/**
 * Type definitions for native modules
 */

declare module 'react-native' {
  interface NativeModulesStatic {
    LeodgeWidgetModule: {
      /**
       * Update the widget with new portfolio data
       */
      updateWidget(
        totalValue: string,
        cash: string,
        invested: string,
        updated: string
      ): Promise<boolean>;

      /**
       * Start the background service for continuous widget updates
       * @param apiKey Trading 212 API key
       * @param apiSecret Trading 212 API secret
       */
      startBackgroundService(apiKey: string, apiSecret: string): Promise<boolean>;

      /**
       * Stop the background service
       */
      stopBackgroundService(): Promise<boolean>;

      /**
       * Check if the background service is currently running
       */
      isBackgroundServiceRunning(): Promise<boolean>;
    };

    LeodgeLoggerModule: {
      /**
       * Get the path to the log file
       */
      getLogFilePath(): Promise<string>;
    };
  }
}
