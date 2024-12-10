import React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  Button,
} from 'react-native';

import NativeLocalStorage from './specs/NativeLocalStorage';

const EMPTY = '<empty>';

function App(): React.JSX.Element {
  const [value, setValue] = React.useState<string | null>(null);

  const [editingValue, setEditingValue] = React.useState<
    string | null
  >(null);

  React.useEffect(() => {
    const storedValue = NativeLocalStorage?.getItem('myKey');
    setValue(storedValue ?? '');
  }, []);

  function saveValue() {
    NativeLocalStorage?.setItem(editingValue ?? EMPTY, 'myKey');
    setValue(editingValue);
  }
  async function connectToDevice(macAddress:any) {
    try {
        const result = await NativeLocalStorage.connectDevice(macAddress);
        console.log(result); // Device connected successfully
    } catch (error:any) {
        console.error('Connection failed:', error.message);
    }
}

async function disconnectDevice() {
    try {
        const result = await NativeLocalStorage.disconnectDevice();
        console.log(result); // Device disconnected successfully
    } catch (error:any) {
        console.error('Disconnection failed:', error.message);
    }
}
async function getHealthData(dataType:any) {
  try {
      const data = await NativeLocalStorage.fetchHealthData(dataType);
      console.log('Fetched health data:', data);
  } catch (error:any) {
      console.error('Error fetching health data:', error.message);
  }
}
  async function performScan() {
    try {
      const devices = await NativeLocalStorage.scanDevice(10); // 10 seconds duration
      console.log('Scanned Devices:', devices);
    } catch (error) {
      console.error('Error during scan:', error);
    }
  }
  function clearAll() {
    NativeLocalStorage?.clear();
    setValue('');
  }

  function deleteValue() {
    NativeLocalStorage?.removeItem(editingValue ?? EMPTY);
    setValue('');
  }

  return (
    <SafeAreaView style={{flex: 1}}>
      <Text style={styles.text}>
        Current stored value is: {value ?? 'No Value'}
      </Text>
      <TextInput
        placeholder="Enter the text you want to store"
        style={styles.textInput}
        onChangeText={setEditingValue}
      />
      <Button title="Save" onPress={saveValue} />
      <Button title="scan" onPress={performScan} />
      <Button title="Delete" onPress={deleteValue} />
      <Button title="Clear" onPress={clearAll} />
      <Button title="Connect" onPress={()=>connectToDevice("07:19:00:00:07:11")} />
      <Button title="disconnect" onPress={disconnectDevice} />
      <Button title="donnect" onPress={()=>getHealthData("heartRate")} />
      <Button title="sleep" onPress={()=>getHealthData("sleep")} />
      <Button title="sync" onPress={()=>getHealthData("sync")} />
      <Button title="temperature" onPress={()=>getHealthData("temperature")} />
      <Button title="spo2" onPress={()=>getHealthData("spo2")} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
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
});

export default App;