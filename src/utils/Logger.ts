import { Platform, NativeModules } from 'react-native';

// Get the native file module for writing to app documents directory
const { LeodgeLoggerModule } = NativeModules;

// Log levels
export enum LogLevel {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
}

// Singleton logger instance
class Logger {
  private static instance: Logger;
  private logFilePath: string = 'leodge.log';
  private isEnabled: boolean = true;
  private maxLogSize: number = 1000000; // 1MB max file size
  private buffer: string[] = [];
  private flushInterval: number | null = null;

  private constructor() {
    // Start periodic flush every 5 seconds
    this.flushInterval = setInterval(() => this.flush(), 5000);
  }

  static getInstance(): Logger {
    if (!Logger.instance) {
      Logger.instance = new Logger();
    }
    return Logger.instance;
  }

  private getTimestamp(): string {
    return new Date().toISOString();
  }

  private formatMessage(level: LogLevel, tag: string, message: string): string {
    return `[${this.getTimestamp()}] [${level}] [${tag}] ${message}\n`;
  }

  private async writeToNativeFile(content: string): Promise<void> {
    try {
      if (Platform.OS === 'android' && LeodgeLoggerModule) {
        await LeodgeLoggerModule.appendToLog(content);
      }
    } catch (error) {
      // Fallback to console if native module fails
      console.log('Native log write failed:', error);
    }
  }

  async log(level: LogLevel, tag: string, message: string, data?: any): Promise<void> {
    if (!this.isEnabled) return;

    let formattedMessage = this.formatMessage(level, tag, message);
    
    // Add data if provided
    if (data !== undefined) {
      if (data instanceof Error) {
        formattedMessage += `  Stack: ${data.stack}\n`;
        formattedMessage += `  Message: ${data.message}\n`;
      } else if (typeof data === 'object') {
        try {
          formattedMessage += `  Data: ${JSON.stringify(data, null, 2)}\n`;
        } catch (e) {
          formattedMessage += `  Data: [Object cannot be stringified]\n`;
        }
      } else {
        formattedMessage += `  Data: ${String(data)}\n`;
      }
    }

    // Add to buffer
    this.buffer.push(formattedMessage);

    // Also log to console
    console.log(formattedMessage);

    // Write to native file
    await this.writeToNativeFile(formattedMessage);
  }

  async flush(): Promise<void> {
    if (this.buffer.length === 0) return;

    const content = this.buffer.join('');
    this.buffer = [];

    await this.writeToNativeFile(content);
  }

  async debug(tag: string, message: string, data?: any): Promise<void> {
    await this.log(LogLevel.DEBUG, tag, message, data);
  }

  async info(tag: string, message: string, data?: any): Promise<void> {
    await this.log(LogLevel.INFO, tag, message, data);
  }

  async warn(tag: string, message: string, data?: any): Promise<void> {
    await this.log(LogLevel.WARN, tag, message, data);
  }

  async error(tag: string, message: string, data?: any): Promise<void> {
    await this.log(LogLevel.ERROR, tag, message, data);
  }

  // Lifecycle methods
  async onAppStart(): Promise<void> {
    await this.info('APP', '========================================');
    await this.info('APP', 'LEODGE Application Started');
    await this.info('APP', `Platform: ${Platform.OS}`);
    await this.info('APP', `Version: ${Platform.Version}`);
    await this.info('APP', '========================================');
  }

  async onAppError(error: Error, context: string): Promise<void> {
    await this.error('APP', `Error in ${context}`, error);
  }

  async onApiCall(url: string, method: string, headers: any): Promise<void> {
    await this.info('API', `>>> ${method} ${url}`);
    await this.debug('API', 'Headers', headers);
  }

  async onApiResponse(status: number, statusText: string, body?: any): Promise<void> {
    await this.info('API', `<<< Response: ${status} ${statusText}`);
    if (body) {
      await this.debug('API', 'Response body', body);
    }
  }

  async onApiError(error: Error, url: string): Promise<void> {
    await this.error('API', `API Error for ${url}`, error);
  }

  async onWidgetUpdate(value: string): Promise<void> {
    await this.info('WIDGET', `Updating widget with value: £${value}`);
  }

  async onCredentialsSaved(): Promise<void> {
    await this.info('CRED', 'Credentials saved to AsyncStorage');
  }

  async onCredentialsLoaded(): Promise<void> {
    await this.info('CRED', 'Credentials loaded from AsyncStorage');
  }

  async onPollingStart(): Promise<void> {
    await this.info('POLL', 'Starting portfolio polling (60s interval)');
  }

  async onPollingStop(): Promise<void> {
    await this.info('POLL', 'Stopping portfolio polling');
  }

  async onPollingTick(): Promise<void> {
    await this.debug('POLL', 'Polling tick - fetching portfolio');
  }

  async destroy(): Promise<void> {
    if (this.flushInterval) {
      clearInterval(this.flushInterval);
    }
    await this.flush();
    await this.info('APP', 'Logger destroyed');
  }
}

export const logger = Logger.getInstance();
export default logger;
