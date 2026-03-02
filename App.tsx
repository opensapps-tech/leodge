// App.tsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  StatusBar,
  Alert,
  ActivityIndicator,
  NativeModules,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { LeodgeWidgetModule } = NativeModules;

const STORAGE_KEY_API_KEY = '@leodge_api_key';
const STORAGE_KEY_API_SECRET = '@leodge_api_secret';
const POLL_INTERVAL_MS = 60_000;

// ─── Trading 212 API ──────────────────────────────────────────────────────────
// Docs: https://docs.trading212.com/api
// Endpoint: GET /api/v0/equity/account/summary
// Returns: { cash: { total }, equity: { total } }
// We use Authorization: <apiKey> header (Trading 212 uses API key only, no secret in v0)
async function fetchPortfolio(apiKey: string): Promise<number> {
  const response = await fetch(
    'https://live.trading212.com/api/v0/equity/account/summary',
    {
      method: 'GET',
      headers: {
        Authorization: apiKey,
        'Content-Type': 'application/json',
      },
    },
  );

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`API error ${response.status}: ${body}`);
  }

  const data = await response.json();

  // Total portfolio = cash + invested equity
  // data.cash.freeForStocks or data.cash.total covers uninvested cash
  // data.equity.total covers invested positions
  const cash: number = data?.cash?.total ?? 0;
  const equity: number = data?.equity?.total ?? 0;

  return cash + equity;
}

// ─── Component ────────────────────────────────────────────────────────────────
export default function App() {
  const [apiKey, setApiKey] = useState('');
  const [apiSecret, setApiSecret] = useState(''); // stored but not used in v0 calls
  const [credentialsSaved, setCredentialsSaved] = useState(false);
  const [portfolioValue, setPortfolioValue] = useState<number | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [loading, setLoading] = useState(false);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Load saved credentials on mount
  useEffect(() => {
    (async () => {
      try {
        const savedKey = await AsyncStorage.getItem(STORAGE_KEY_API_KEY);
        const savedSecret = await AsyncStorage.getItem(STORAGE_KEY_API_SECRET);
        if (savedKey) {
          setApiKey(savedKey);
          setApiSecret(savedSecret ?? '');
          setCredentialsSaved(true);
        }
      } catch (e) {
        console.warn('Failed to load credentials', e);
      }
    })();
  }, []);

  // Start/stop polling based on credentialsSaved
  useEffect(() => {
    if (credentialsSaved && apiKey) {
      startPolling(apiKey);
    }
    return () => stopPolling();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [credentialsSaved, apiKey]);

  const poll = useCallback(
    async (key: string) => {
      try {
        setError(null);
        const value = await fetchPortfolio(key);
        setPortfolioValue(value);
        setLastUpdated(new Date());

        // Update Android widget
        if (LeodgeWidgetModule?.updateWidget) {
          LeodgeWidgetModule.updateWidget(value.toFixed(2));
        }
      } catch (e: any) {
        setError(e?.message ?? 'Unknown error');
      }
    },
    [],
  );

  const startPolling = useCallback(
    (key: string) => {
      stopPolling();
      setPolling(true);
      poll(key); // immediate first call
      intervalRef.current = setInterval(() => poll(key), POLL_INTERVAL_MS);
    },
    [poll],
  );

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setPolling(false);
  }, []);

  const handleSave = async () => {
    if (!apiKey.trim()) {
      Alert.alert('Missing API Key', 'Please enter your Trading 212 API Key.');
      return;
    }
    setLoading(true);
    try {
      await AsyncStorage.setItem(STORAGE_KEY_API_KEY, apiKey.trim());
      await AsyncStorage.setItem(STORAGE_KEY_API_SECRET, apiSecret.trim());
      setCredentialsSaved(true);
    } catch (e) {
      Alert.alert('Error', 'Failed to save credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleClear = async () => {
    stopPolling();
    await AsyncStorage.multiRemove([STORAGE_KEY_API_KEY, STORAGE_KEY_API_SECRET]);
    setApiKey('');
    setApiSecret('');
    setCredentialsSaved(false);
    setPortfolioValue(null);
    setLastUpdated(null);
    setError(null);
  };

  const formatValue = (v: number) =>
    `£${v.toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  const formatTime = (d: Date) =>
    d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor="#0a0a0f" />

      {/* ── Header ── */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>LEODGE</Text>
        <Text style={styles.headerSub}>Portfolio Monitor</Text>
      </View>

      {/* ── Portfolio Value ── */}
      <View style={styles.valueCard}>
        {portfolioValue !== null ? (
          <>
            <Text style={styles.valueLabel}>TOTAL VALUE</Text>
            <Text style={styles.valueAmount}>{formatValue(portfolioValue)}</Text>
            {lastUpdated && (
              <Text style={styles.valueTime}>Updated {formatTime(lastUpdated)}</Text>
            )}
          </>
        ) : (
          <Text style={styles.valuePlaceholder}>
            {credentialsSaved ? 'Fetching...' : 'Enter credentials below'}
          </Text>
        )}

        {error ? (
          <Text style={styles.errorText}>⚠ {error}</Text>
        ) : null}
      </View>

      {/* ── Status Bar ── */}
      <View style={styles.statusRow}>
        <View style={[styles.statusDot, polling ? styles.dotActive : styles.dotInactive]} />
        <Text style={styles.statusText}>
          {polling ? 'Polling every 60s' : 'Not polling'}
        </Text>
      </View>

      {/* ── Credentials ── */}
      <View style={styles.section}>
        <Text style={styles.sectionLabel}>API KEY</Text>
        <TextInput
          style={styles.input}
          value={apiKey}
          onChangeText={setApiKey}
          placeholder="Enter your Trading 212 API Key"
          placeholderTextColor="#444"
          autoCapitalize="none"
          autoCorrect={false}
          editable={!credentialsSaved}
        />

        <Text style={styles.sectionLabel}>API SECRET</Text>
        <TextInput
          style={styles.input}
          value={apiSecret}
          onChangeText={setApiSecret}
          placeholder="Enter your API Secret (optional)"
          placeholderTextColor="#444"
          secureTextEntry
          autoCapitalize="none"
          autoCorrect={false}
          editable={!credentialsSaved}
        />

        {!credentialsSaved ? (
          <TouchableOpacity
            style={styles.button}
            onPress={handleSave}
            disabled={loading}>
            {loading ? (
              <ActivityIndicator color="#0a0a0f" />
            ) : (
              <Text style={styles.buttonText}>SAVE & START</Text>
            )}
          </TouchableOpacity>
        ) : (
          <TouchableOpacity style={styles.buttonSecondary} onPress={handleClear}>
            <Text style={styles.buttonSecondaryText}>CLEAR CREDENTIALS</Text>
          </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#0a0a0f',
  },
  header: {
    paddingTop: 24,
    paddingHorizontal: 24,
    paddingBottom: 8,
  },
  headerTitle: {
    fontSize: 32,
    fontWeight: '900',
    color: '#e8f4e8',
    letterSpacing: 6,
  },
  headerSub: {
    fontSize: 11,
    color: '#4caf72',
    letterSpacing: 3,
    marginTop: 2,
    textTransform: 'uppercase',
  },
  valueCard: {
    margin: 24,
    backgroundColor: '#111118',
    borderRadius: 16,
    padding: 28,
    borderWidth: 1,
    borderColor: '#1e1e2e',
    alignItems: 'center',
  },
  valueLabel: {
    fontSize: 10,
    color: '#4caf72',
    letterSpacing: 4,
    marginBottom: 8,
  },
  valueAmount: {
    fontSize: 42,
    fontWeight: '800',
    color: '#e8f4e8',
    letterSpacing: -1,
  },
  valueTime: {
    marginTop: 8,
    fontSize: 12,
    color: '#555',
    letterSpacing: 1,
  },
  valuePlaceholder: {
    fontSize: 16,
    color: '#444',
    letterSpacing: 1,
  },
  errorText: {
    marginTop: 12,
    fontSize: 12,
    color: '#ff6b6b',
    textAlign: 'center',
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 24,
    marginBottom: 16,
    gap: 8,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  dotActive: {
    backgroundColor: '#4caf72',
  },
  dotInactive: {
    backgroundColor: '#333',
  },
  statusText: {
    fontSize: 12,
    color: '#555',
    letterSpacing: 1,
  },
  section: {
    paddingHorizontal: 24,
    gap: 8,
  },
  sectionLabel: {
    fontSize: 10,
    color: '#4caf72',
    letterSpacing: 3,
    marginTop: 8,
  },
  input: {
    backgroundColor: '#111118',
    borderWidth: 1,
    borderColor: '#1e1e2e',
    borderRadius: 10,
    paddingHorizontal: 16,
    paddingVertical: 14,
    color: '#e8f4e8',
    fontSize: 14,
    letterSpacing: 0.5,
  },
  button: {
    marginTop: 16,
    backgroundColor: '#4caf72',
    borderRadius: 10,
    paddingVertical: 16,
    alignItems: 'center',
  },
  buttonText: {
    color: '#0a0a0f',
    fontWeight: '800',
    fontSize: 14,
    letterSpacing: 3,
  },
  buttonSecondary: {
    marginTop: 16,
    borderWidth: 1,
    borderColor: '#ff6b6b',
    borderRadius: 10,
    paddingVertical: 16,
    alignItems: 'center',
  },
  buttonSecondaryText: {
    color: '#ff6b6b',
    fontWeight: '700',
    fontSize: 13,
    letterSpacing: 2,
  },
});
