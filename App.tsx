import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  Button,
  FlatList,
  TouchableOpacity,
  Alert,
} from 'react-native';

import NativeLocalStorage from './specs/NativeLocalStorage';

const EMPTY = '<empty>';

function App(): React.JSX.Element {
  const [value, setValue] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState<string | null>(null);
  const [scanningDevices, setScanningDevices] = useState<any[]>([]); // State to hold scanned devices
  const [healthData, setHealthData] = useState<any>(null); // State to hold health data

  // Fetch the stored value on mount
  useEffect(() => {
    const storedValue = NativeLocalStorage?.getItem('myKey');
    setValue(storedValue ?? '');
  }, []);

  // Save value to native storage
  function saveValue() {
    NativeLocalStorage?.setItem(editingValue ?? EMPTY, 'myKey');
    setValue(editingValue);
  }

  // Connect to the device using MAC address
  async function connectToDevice(macAddress: string) {
    try {
      const result = await NativeLocalStorage.connectDevice(macAddress);
      console.log('Device connected successfully:', result);
    } catch (error: any) {
      console.error('Connection failed:', error.message);
    }
  }

  // Disconnect from the device
  async function disconnectDevice() {
    try {
      const result = await NativeLocalStorage.disconnectDevice();
      console.log('Device disconnected successfully:', result);
    } catch (error: any) {
      console.error('Disconnection failed:', error.message);
    }
  }

  // Fetch health data (e.g., heart rate, steps, etc.)
  async function getHealthData(dataType: string) {
    try {
      const data = await NativeLocalStorage.fetchHealthData(dataType);
      console.log(`Fetched health data (${dataType}):`, data);
      setHealthData(data);
    } catch (error: any) {
      console.error(`Error fetching health data for ${dataType}:`, error.message);
    }
  }

  // Start scanning for BLE devices
  async function performScan() {
    try {
      const devices = await NativeLocalStorage.scanDevice(10); // 10 seconds duration
      console.log('Scanned Devices:', devices);
      setScanningDevices(devices); // Save scanned devices to state
    } catch (error) {
      console.error('Error during scan:', error);
    }
  }
  async function reconnectBle() {
    try {
      const devices = await NativeLocalStorage.reconnectBle(); // 10 seconds duration
      console.log('found:', devices);
      // setScanningDevices(devices); // Save scanned devices to state
    } catch (error) {
      console.error('Error during scan:', error);
    }
  }

  // Clear all values in local storage
  function clearAll() {
    NativeLocalStorage?.clear();
    setValue('');
  }

  // Remove a specific value from local storage
  function deleteValue() {
    NativeLocalStorage?.removeItem(editingValue ?? EMPTY);
    setValue('');
  }

  // Function to handle when a device is clicked
  function handleDeviceClick(macAddress: string) {
    connectToDevice(macAddress)
  }

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.text}>
        Current stored value is: {value ?? 'No Value'}
      </Text>

      <TextInput
        placeholder="Enter the text you want to store"
        style={styles.textInput}
        onChangeText={setEditingValue}
        value={editingValue ?? ''}
      />
      
      <Button title="Save" onPress={saveValue} />
      <Button title="Scan for Devices" onPress={performScan} />
      <Button title="Delete" onPress={deleteValue} />
      <Button title="Clear All" onPress={clearAll} />
      
      <Button title="Connect" onPress={() => connectToDevice('ED:F2:F9:63:AF:17')} />
      <Button title="Disconnect" onPress={disconnectDevice} />
      
      <Button title="Fetch Heart Rate" onPress={() => getHealthData('heartRate')} />
      <Button title="Fetch Sleep Data" onPress={() => getHealthData('sleep')} />
      <Button title="Fetch Sync Data" onPress={() => getHealthData('sync')} />
      <Button title="Fetch Temperature" onPress={() => getHealthData('temperature')} />
      <Button title="Fetch SPO2" onPress={() => getHealthData('spo2')} />
      <Button title="Fetch Fall Data" onPress={() => getHealthData('fall')} />
      <Button title="reconnect" onPress={() => reconnectBle()} />

      {/* Displaying scanned devices */}
      <Text style={styles.subHeaderText}>Scanned Devices:</Text>
      <FlatList
        data={scanningDevices}
        keyExtractor={(item, index) => index.toString()}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.deviceItem}
            onPress={() => handleDeviceClick(item.macAddress)}
          >
            <Text style={styles.deviceText}>{item.deviceName || 'Unknown Device'}</Text>
            <Text style={styles.deviceText}>{item.macAddress || 'Unknown MAC'}</Text>
          </TouchableOpacity>
        )}
      />

      {/* Displaying fetched health data */}
      <Text style={styles.subHeaderText}>
        Fetched Health Data: {healthData ? JSON.stringify(healthData) : 'No data fetched'}
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  text: {
    margin: 10,
    fontSize: 20,
  },
  textInput: {
    margin: 10,
    height: 40,
    borderColor: 'black',
    borderWidth: 1,
    paddingLeft: 5,
    paddingRight: 5,
    borderRadius: 5,
  },
  subHeaderText: {
    marginTop: 20,
    fontSize: 18,
    fontWeight: 'bold',
  },
  deviceItem: {
    marginVertical: 5,
    padding: 10,
    backgroundColor: '#f0f0f0',
    borderRadius: 5,
  },
  deviceText: {
    fontSize: 16,
    color: '#333',
  },
});

export default App;
