import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import {Colors} from 'react-native/Libraries/NewAppScreen';

const HomeScreen: React.FC = () => {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      style={styles.scrollView}>
      <View style={styles.container}>
        <View style={styles.headerContainer}>
          <Text style={[styles.title, {color: isDarkMode ? Colors.white : Colors.black}]}>
            👋 Hello, MyApp!
          </Text>
          <Text style={[styles.subtitle, {color: isDarkMode ? Colors.light : Colors.dark}]}>
            Your React Native Android app is live.
          </Text>
        </View>

        <View style={[styles.card, {backgroundColor: isDarkMode ? Colors.darker : Colors.lighter}]}>
          <Text style={[styles.cardTitle, {color: isDarkMode ? Colors.white : Colors.black}]}>
            🚀 Built on GitHub Actions
          </Text>
          <Text style={[styles.cardBody, {color: isDarkMode ? Colors.light : Colors.dark}]}>
            Every push to your repository triggers a cloud build.
            No local toolchain required.
          </Text>
        </View>

        <View style={[styles.card, {backgroundColor: isDarkMode ? Colors.darker : Colors.lighter}]}>
          <Text style={[styles.cardTitle, {color: isDarkMode ? Colors.white : Colors.black}]}>
            📱 Optimized for Modern Devices
          </Text>
          <Text style={[styles.cardBody, {color: isDarkMode ? Colors.light : Colors.dark}]}>
            Targets Android 14 (API 34). Runs great on Samsung Galaxy
            S24 Ultra, S25 Ultra and all modern flagship devices.
          </Text>
        </View>
      </View>
    </ScrollView>
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
    fontSize: 32,
    fontWeight: '700',
    letterSpacing: -0.5,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 16,
    fontWeight: '400',
    textAlign: 'center',
    opacity: 0.7,
  },
  card: {
    borderRadius: 16,
    padding: 20,
    gap: 8,
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
  cardBody: {
    fontSize: 14,
    lineHeight: 22,
    opacity: 0.75,
  },
});

export default HomeScreen;
