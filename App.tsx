import React, {useState, useEffect, useRef} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
  NativeModules,
  Alert,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const {LeodgeWidgetModule} = NativeModules;

const App: React.FC = () => {
  const isDarkMode = useColorScheme() === 'dark';

  const [apiKey, setApiKey] = useState('');
  const [apiSecret, setApiSecret] = useState('');
  const [portfolioValue, setPortfolioValue] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);
  const [pollingStatus, setPollingStatus] = useState<
    'idle' | 'polling' | 'error'
  >('idle');
  const [error, setError] = useState<string | null>(null);

  const pollingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(
    null,
  );

  useEffect(() => {
    loadCredentials();
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, []);

  const loadCredentials = async () => {
    try {
      const storedApiKey = await AsyncStorage.getItem('apiKey');
      const storedApiSecret = await AsyncStorage.getItem('apiSecret');

      if (storedApiKey && storedApiSecret) {
        setApiKey(storedApiKey);
        setApiSecret(storedApiSecret);
        fetchPortfolio(storedApiKey, storedApiSecret);
        startPolling(storedApiKey, storedApiSecret);
      }
    } catch (e) {
      console.error('Failed to load credentials:', e);
    }
  };

  const saveCredentials = async () => {
    if (!apiKey.trim() || !apiSecret.trim()) {
      Alert.alert('Error', 'Please enter both API Key and API Secret');
      return;
    }

    try {
      await AsyncStorage.setItem('apiKey', apiKey);
      await AsyncStorage.setItem('apiSecret', apiSecret);

      fetchPortfolio(apiKey, apiSecret);
      startPolling(apiKey, apiSecret);
    } catch (e) {
      console.error('Failed to save credentials:', e);
      setError('Failed to save credentials');
    }
  };

  const startPolling = (key: string, secret: string) => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
    }

    pollingIntervalRef.current = setInterval(() => {
      fetchPortfolio(key, secret);
    }, 60000);
  };

  const fetchPortfolio = async (key?: string, secret?: string) => {
    const useKey = key ?? apiKey;
    const useSecret = secret ?? apiSecret;

    if (!useKey || !useSecret) {
      return;
    }

    try {
      const response = await fetch(
        'https://api.trading212.com/v1/portfolio',
        {
          method: 'GET',
          headers: {
            'X-Api-Key': useKey,
            'X-Api-Secret': useSecret,
          },
        },
      );

      if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
      }

      const data = await response.json();

      const cash = parseFloat(data.cash) || 0;
      const equity = parseFloat(data.equity) || 0;
      const total = cash + equity;

      setPortfolioValue(total.toFixed(2));
      setLastUpdated(new Date().toLocaleTimeString());
      setPollingStatus('polling');
      setError(null);

      try {
        LeodgeWidgetModule?.updateWidget(total.toFixed(2));
      } catch (widgetError) {
        console.log('Widget update not available:', widgetError);
      }
    } catch (e) {
      const errorMessage =
        e instanceof Error ? e.message : 'Failed to fetch portfolio';
      setError(errorMessage);
      setPollingStatus('error');
    }
  };

  const backgroundStyle = {
    backgroundColor: isDarkMode ? '#121212' : '#FFFFFF',
    flex: 1,
  };

  const textColor = isDarkMode ? '#FFFFFF' : '#000000';
  const secondaryTextColor = isDarkMode ? '#AAAAAA' : '#666666';
  const inputBackgroundColor = isDarkMode ? '#1E1E1E' : '#F5F5F5';
  const cardBackgroundColor = isDarkMode ? '#1E1E1E' : '#F5F5F5';

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={styles.scrollView}>
        <View style={styles.container}>
          <View style={styles.headerContainer}>
            <Text style={[styles.title, {color: textColor}]}>LEODGE</Text>
            <Text style={[styles.subtitle, {color: secondaryTextColor}]}>
              Trading 212 Portfolio Monitor
            </Text>
          </View>

          <View style={[styles.card, {backgroundColor: cardBackgroundColor}]}>
            <Text style={[styles.label, {color: textColor}]}>API Key</Text>
            <TextInput
              style={[
                styles.input,
                {backgroundColor: inputBackgroundColor, color: textColor},
              ]}
              placeholder="Enter API Key"
              placeholderTextColor={secondaryTextColor}
              value={apiKey}
              onChangeText={setApiKey}
              autoCapitalize="none"
              autoCorrect={false}
            />

            <Text style={[styles.label, {color: textColor}]}>API Secret</Text>
            <TextInput
              style={[
                styles.input,
                {backgroundColor: inputBackgroundColor, color: textColor},
              ]}
              placeholder="Enter API Secret"
              placeholderTextColor={secondaryTextColor}
              value={apiSecret}
              onChangeText={setApiSecret}
              secureTextEntry
              autoCapitalize="none"
              autoCorrect={false}
            />

            <TouchableOpacity
              style={styles.button}
              onPress={saveCredentials}>
              <Text style={styles.buttonText}>Save Credentials</Text>
            </TouchableOpacity>
          </View>

          {portfolioValue !== null && (
            <View style={[styles.card, {backgroundColor: cardBackgroundColor}]}>
              <Text style={[styles.cardTitle, {color: textColor}]}>
                Portfolio Value
              </Text>
              <Text style={[styles.portfolioValue, {color: '#4CAF50'}]}>
                £{portfolioValue}
              </Text>

              {lastUpdated && (
                <Text style={[styles.infoText, {color: secondaryTextColor}]}>
                  Last Updated: {lastUpdated}
                </Text>
              )}

              <Text style={[styles.statusText, {color: '#4CAF50'}]}>
                Status: {pollingStatus === 'polling' ? 'Polling' : 'Active'}
              </Text>
            </View>
          )}

          {error && (
            <View style={[styles.card, {backgroundColor: '#3D1E1E'}]}>
              <Text style={[styles.errorText, {color: '#FF6B6B'}]}>
                Error: {error}
              </Text>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
  },
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 40,
    paddingBottom: 32,
    gap: 20,
  },
  headerContainer: {
    alignItems: 'center',
    paddingVertical: 32,
    gap: 8,
  },
  title: {
    fontSize: 40,
    fontWeight: '700',
    letterSpacing: 4,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 14,
    fontWeight: '400',
    textAlign: 'center',
  },
  card: {
    borderRadius: 16,
    padding: 20,
    gap: 12,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  cardTitle: {
    fontSize: 17,
    fontWeight: '600',
    letterSpacing: -0.2,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    marginTop: 8,
  },
  input: {
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    marginTop: 4,
  },
  button: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 12,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  portfolioValue: {
    fontSize: 36,
    fontWeight: '700',
  },
  infoText: {
    fontSize: 14,
    marginTop: 8,
  },
  statusText: {
    fontSize: 14,
    marginTop: 4,
  },
  errorText: {
    fontSize: 14,
  },
});

export default App;
