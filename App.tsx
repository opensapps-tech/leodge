/**
 * LEODGE - Trading 212 Portfolio Monitor
 * @format
 */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Modal,
  ScrollView,
  Platform,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { NativeModules } from 'react-native';
import logger from './src/utils/Logger';

// Native module imports
const { LeodgeWidgetModule, LeodgeLoggerModule } = NativeModules;

// Storage keys
const STORAGE_KEYS = {
  API_KEY: '@leodge_api_key',
  API_SECRET: '@leodge_api_secret',
};

// Trading 212 API configuration
const API_BASE_URL = 'https://live.trading212.com';

// Helper function to create Basic Auth header
function createBasicAuthHeader(apiKey: string, apiSecret: string): string {
  const credentials = `${apiKey}:${apiSecret}`;
  const base64Credentials = btoa(credentials);
  return `Basic ${base64Credentials}`;
}

// Fetch portfolio data from Trading 212 API
async function fetchPortfolio(apiKey: string, apiSecret: string): Promise<{ total: number; rawJson: string }> {
  const url = `${API_BASE_URL}/api/v0/equity/account/summary`;
  const authHeader = createBasicAuthHeader(apiKey, apiSecret);
  
  await logger.info('API', `>>> FETCH: GET ${url}`);
  await logger.info('API', `Auth: Basic ${apiKey.substring(0, 4)}...`);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': authHeader,
        'Content-Type': 'application/json',
      },
      signal: controller.signal,
    });

    clearTimeout(timeoutId);
    
    const statusInfo = `Status: ${response.status} ${response.statusText}`;
    await logger.info('API', `<<< RESPONSE: ${statusInfo}`);
    await logger.info('API', `Headers: ${JSON.stringify(Object.fromEntries(response.headers.entries()))}`);

    if (!response.ok) {
      const errorText = await response.text();
      await logger.error('API', `HTTP Error: ${response.status}`, { body: errorText });
      throw new Error(`HTTP ${response.status}: ${response.statusText} - ${errorText}`);
    }

    // Get raw response text first
    const rawText = await response.text();
    await logger.info('API', `Raw response raw: [${rawText.substring(0, 100)}]`);
    await logger.info('API', `Raw response (${rawText.length} chars): ${rawText.substring(0, 500)}`);
    const data = JSON.parse(rawText);
    await logger.debug('API', 'Parsed JSON data', data);
    // Trading 212 account summary returns: { total: number, cash: {availableToTrade}, investments: {currentValue} }
    const total = data?.totalValue ?? data?.total ?? data?.balance ?? 0;
    const cash = data?.cash?.availableToTrade ?? 0;
    const invested = data?.investments?.currentValue ?? data?.investments?.totalCost ?? 0;
    await logger.info('API', `Portfolio extracted: total=${total}, cash=${cash}, invested=${invested}`);
    return { total, rawJson: rawText };
  } catch (error: any) {
    clearTimeout(timeoutId);
    if (error.name === 'AbortError') {
      await logger.error('API', 'Request timeout after 15 seconds');
      throw new Error('Request timeout - API did not respond within 15 seconds');
    }
    await logger.error('API', 'Fetch failed', error);
    throw error;
  }
}

// Update Android widget
async function updateWidget(totalValue: string, cash: string, invested: string, updated: string): Promise<void> {
  await logger.info('WIDGET', `Updating widget with value: £${totalValue}`);
  if (Platform.OS === 'android' && LeodgeWidgetModule) {
    try {
      await LeodgeWidgetModule.updateWidget(totalValue, cash, invested, updated);
    } catch (error: any) {
      await logger.error('WIDGET', 'Widget update failed', error);
    }
  }
}

// Get log file path
async function getLogFilePath(): Promise<string> {
  try {
    if (LeodgeLoggerModule) {
      return await LeodgeLoggerModule.getLogFilePath();
    }
  } catch (error) {
    await logger.error('LOG', 'Failed to get log file path', error);
  }
  return 'Unknown';
}

function App(): React.JSX.Element {
  const [apiKey, setApiKey] = useState('');
  const [apiSecret, setApiSecret] = useState('');
  const [portfolioValue, setPortfolioValue] = useState<number | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [pollingStatus, setPollingStatus] = useState<'idle' | 'polling' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showErrorModal, setShowErrorModal] = useState(false);
  const [detailedError, setDetailedError] = useState('');
  const [rawJson, setRawJson] = useState('');
  
  const pollingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const isPollingStarted = useRef(false);
  const credentialsLoaded = useRef(false);

  // Initialize logger on mount
  useEffect(() => {
    const init = async () => {
      await logger.onAppStart();
      const logPath = await getLogFilePath();
      await logger.info('LOG', `Log file: ${logPath}`);
    };
    init();
  }, []);

  // Load saved credentials on mount
  useEffect(() => {
    const loadSaved = async () => {
      await logger.info('CRED', 'Loading saved credentials...');
      try {
        const savedApiKey = await AsyncStorage.getItem(STORAGE_KEYS.API_KEY);
        const savedApiSecret = await AsyncStorage.getItem(STORAGE_KEYS.API_SECRET);
        if (savedApiKey && savedApiSecret) {
          setApiKey(savedApiKey);
          setApiSecret(savedApiSecret);
          await logger.info('CRED', 'Credentials loaded from storage');
          credentialsLoaded.current = true;
        } else {
          await logger.info('CRED', 'No saved credentials found');
        }
      } catch (error: any) {
        await logger.error('CRED', 'Failed to load credentials', error);
      }
    };
    loadSaved();
  }, []);

  // Start polling when credentials become available
  useEffect(() => {
    const startPolling = async () => {
      if (apiKey && apiSecret && !isPollingStarted.current) {
        isPollingStarted.current = true;
        await logger.info('POLL', `Starting polling - API key: ${apiKey.substring(0, 4)}...`);
        
        // Initial fetch
        fetchPortfolioData();
        
        // Start interval
        pollingIntervalRef.current = setInterval(fetchPortfolioData, 60000);
        await logger.info('POLL', 'Polling interval started (60s)');
      }
    };
    
    startPolling();
    
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
        logger.info('POLL', 'Polling stopped');
      }
    };
  }, [apiKey, apiSecret]);

  const saveCredentials = async () => {
    await logger.info('CRED', 'Saving credentials...');
    
    if (!apiKey.trim() || !apiSecret.trim()) {
      const errMsg = 'Please enter both API Key and API Secret';
      await logger.warn('CRED', errMsg);
      showDetailedError('Validation Error', errMsg);
      return;
    }
    
    try {
      await AsyncStorage.setItem(STORAGE_KEYS.API_KEY, apiKey.trim());
      await AsyncStorage.setItem(STORAGE_KEYS.API_SECRET, apiSecret.trim());
      await logger.onCredentialsSaved();
      // Force re-render to trigger polling start
      setApiKey(apiKey.trim());
      setApiSecret(apiSecret.trim());
    } catch (error: any) {
      await logger.error('CRED', 'Failed to save credentials', error);
      showDetailedError('Storage Error', `Failed: ${error.message}`);
    }
  };

  const fetchPortfolioData = useCallback(async () => {
    await logger.info('POLL', '=== Fetch cycle started ===');
    
    if (!apiKey || !apiSecret) {
      await logger.warn('POLL', 'No credentials, skipping fetch');
      return;
    }

    setPollingStatus('polling');
    setErrorMessage(null);

    try {
      const { total, rawJson } = await fetchPortfolio(apiKey, apiSecret);
      
      await logger.info('POLL', `Success! Total: £${total}`);
      
      setPortfolioValue(total);
      setRawJson(rawJson);
      setLastUpdated(new Date());
      setPollingStatus('polling');
      setErrorMessage(null);
      
      // Update widget
      await updateWidget(
      total.toFixed(2),
      cash.toFixed(2),
      invested.toFixed(2),
      new Date().toLocaleTimeString()
    );
      
      await logger.info('POLL', '=== Fetch cycle complete ===');
    } catch (error: any) {
      await logger.error('POLL', 'Fetch failed', error);
      
      const errorDetails = await formatErrorDetails(error);
      showDetailedError('API Error', errorDetails);
      
      setPollingStatus('error');
      setErrorMessage(error.message || 'Failed');
      await logger.info('POLL', '=== Fetch cycle failed ===');
    }
  }, [apiKey, apiSecret]);

  const formatErrorDetails = async (error: any): Promise<string> => {
    let details = '';
    
    details += `Message: ${error.message || 'Unknown error'}\n\n`;
    
    if (error.stack) {
      details += `Stack Trace:\n${error.stack}\n\n`;
    }
    
    details += `Context:\n`;
    details += `- API URL: ${API_BASE_URL}/api/v0/equity/account/summary\n`;
    details += `- Auth: Basic ${apiKey.substring(0, 8)}...\n`;
    details += `- Timestamp: ${new Date().toISOString()}\n`;
    details += `- Platform: ${Platform.OS}\n`;
    details += `- Log: ${await getLogFilePath()}\n`;
    
    return details;
  };

  const showDetailedError = (title: string, details: string) => {
    setDetailedError(details);
    setShowErrorModal(true);
  };

  const formatCurrency = (value: number): string => {
    return `£${value.toLocaleString('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const formatTime = (date: Date): string => {
    return date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#1a1a2e" />
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Title */}
        <Text style={styles.title}>LEODGE</Text>
        <Text style={styles.subtitle}>Trading 212 Portfolio Monitor</Text>

        {/* Credentials Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>API Credentials</Text>
          
          <TextInput
            style={styles.input}
            placeholder="API Key"
            placeholderTextColor="#666"
            value={apiKey}
            onChangeText={setApiKey}
            autoCapitalize="none"
            autoCorrect={false}
          />
          
          <TextInput
            style={styles.input}
            placeholder="API Secret"
            placeholderTextColor="#666"
            value={apiSecret}
            onChangeText={setApiSecret}
            secureTextEntry
            autoCapitalize="none"
            autoCorrect={false}
          />
          
          <TouchableOpacity style={styles.saveButton} onPress={saveCredentials}>
            <Text style={styles.saveButtonText}>Save Credentials</Text>
          </TouchableOpacity>
        </View>

        {/* Portfolio Value Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Portfolio Value</Text>
          
          <Text style={styles.portfolioValue}>
            {portfolioValue !== null ? formatCurrency(portfolioValue) : '£--'}
          </Text>
          
          <View style={styles.statusRow}>
            <View style={styles.statusItem}>
              <Text style={styles.statusLabel}>Last Updated</Text>
              <Text style={styles.statusValue}>
                {lastUpdated ? formatTime(lastUpdated) : '--'}
              </Text>
            </View>
            
            <View style={styles.statusItem}>
              <Text style={styles.statusLabel}>Status</Text>
              <View style={styles.statusIndicator}>
                <View style={[
                  styles.statusDot,
                  pollingStatus === 'polling' && styles.statusDotActive,
                  pollingStatus === 'error' && styles.statusDotError,
                ]} />
                <Text style={[
                  styles.statusValue,
                  pollingStatus === 'error' && styles.statusValueError,
                ]}>
                  {pollingStatus === 'idle' ? 'Ready' : 
                   pollingStatus === 'polling' ? 'Polling' : 'Error'}
                </Text>
              </View>
            </View>
          </View>

          {errorMessage && (
            <TouchableOpacity 
              style={styles.errorButton}
              onPress={() => setShowErrorModal(true)}
            >
              <Text style={styles.errorButtonText}>View Error Details</Text>
            </TouchableOpacity>
          )}
        </View>

        {/* Raw JSON Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Raw API Response</Text>
          <ScrollView style={styles.rawJsonContainer}>
            <Text style={styles.rawJsonText}>
              {rawJson || 'No data yet - press refresh to fetch'}
            </Text>
          </ScrollView>
        </View>

        {/* Manual Refresh Button */}
        <TouchableOpacity 
          style={[styles.refreshButton, pollingStatus === 'polling' && styles.refreshButtonDisabled]}
          onPress={fetchPortfolioData}
          disabled={pollingStatus === 'polling' || !apiKey || !apiSecret}
        >
          <Text style={styles.refreshButtonText}>
            {pollingStatus === 'polling' ? 'Fetching...' : 'Refresh Now'}
          </Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Error Modal */}
      <Modal
        visible={showErrorModal}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowErrorModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Error Details</Text>
            <ScrollView style={styles.errorScrollView}>
              <Text style={styles.errorText}>{detailedError}</Text>
            </ScrollView>
            <TouchableOpacity
              style={styles.modalCloseButton}
              onPress={() => setShowErrorModal(false)}
            >
              <Text style={styles.modalCloseButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#1a1a2e' },
  scrollContent: { padding: 20, paddingTop: 40 },
  title: { fontSize: 36, fontWeight: 'bold', color: '#00d4aa', textAlign: 'center' },
  subtitle: { fontSize: 14, color: '#888', textAlign: 'center', marginTop: 4, marginBottom: 30 },
  section: { backgroundColor: '#16213e', borderRadius: 12, padding: 16, marginBottom: 16 },
  sectionTitle: { fontSize: 16, fontWeight: '600', color: '#fff', marginBottom: 12 },
  input: { backgroundColor: '#0f0f23', borderRadius: 8, padding: 12, color: '#fff', fontSize: 14, marginBottom: 12, borderWidth: 1, borderColor: '#333' },
  saveButton: { backgroundColor: '#00d4aa', borderRadius: 8, padding: 14, alignItems: 'center', marginTop: 4 },
  saveButtonText: { color: '#1a1a2e', fontSize: 16, fontWeight: '600' },
  portfolioValue: { fontSize: 42, fontWeight: 'bold', color: '#00d4aa', textAlign: 'center', marginVertical: 16 },
  statusRow: { flexDirection: 'row', justifyContent: 'space-around', marginTop: 8 },
  statusItem: { alignItems: 'center' },
  statusLabel: { fontSize: 12, color: '#888', marginBottom: 4 },
  statusValue: { fontSize: 14, color: '#fff' },
  statusIndicator: { flexDirection: 'row', alignItems: 'center' },
  statusDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#666', marginRight: 6 },
  statusDotActive: { backgroundColor: '#00d4aa' },
  statusDotError: { backgroundColor: '#ff4444' },
  statusValueError: { color: '#ff4444' },
  errorButton: { backgroundColor: '#ff4444', borderRadius: 8, padding: 10, alignItems: 'center', marginTop: 12 },
  errorButtonText: { color: '#fff', fontSize: 14, fontWeight: '600' },
  rawJsonContainer: { backgroundColor: '#0f0f23', borderRadius: 8, padding: 12, maxHeight: 150 },
  rawJsonText: { fontSize: 11, color: '#00ff00', fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace' },
  refreshButton: { backgroundColor: '#333', borderRadius: 8, padding: 14, alignItems: 'center', marginTop: 8 },
  refreshButtonDisabled: { opacity: 0.5 },
  refreshButtonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0, 0, 0, 0.8)', justifyContent: 'center', alignItems: 'center', padding: 20 },
  modalContent: { backgroundColor: '#16213e', borderRadius: 12, padding: 20, width: '100%', maxHeight: '80%' },
  modalTitle: { fontSize: 20, fontWeight: 'bold', color: '#ff4444', marginBottom: 16, textAlign: 'center' },
  errorScrollView: { maxHeight: 400 },
  errorText: { fontSize: 12, color: '#ccc', fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace', lineHeight: 18 },
  modalCloseButton: { backgroundColor: '#00d4aa', borderRadius: 8, padding: 14, alignItems: 'center', marginTop: 16 },
  modalCloseButtonText: { color: '#1a1a2e', fontSize: 16, fontWeight: '600' },
});

export default App;
