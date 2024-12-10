import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {
  setItem(value: string, key: string): void;
  getItem(key: string): string | null;
  removeItem(key: string): void;
  clear(): void;
  scanDevice(duration: number): Promise<any>;
  connectDevice(macAddress: string): Promise<string>;
  disconnectDevice(): Promise<string>;
  fetchHealthData(dataType: string): Promise<any>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'NativeLocalStorage',
);