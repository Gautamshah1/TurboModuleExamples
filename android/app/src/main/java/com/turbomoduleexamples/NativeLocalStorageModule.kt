package com.turbomoduleexamples

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.nativelocalstorage.NativeLocalStorageSpec
import com.yucheng.ycbtsdk.Constants
import com.yucheng.ycbtsdk.YCBTClient
import com.yucheng.ycbtsdk.bean.ScanDeviceBean
import com.yucheng.ycbtsdk.response.BleConnectResponse
import com.yucheng.ycbtsdk.response.BleDataResponse
import com.yucheng.ycbtsdk.response.BleScanResponse
import com.yucheng.ycbtsdk.utils.SPUtil
import com.yucheng.ycbtsdk.utils.YCBTLog
import java.util.concurrent.Executors


class NativeLocalStorageModule(reactContext: ReactApplicationContext) : NativeLocalStorageSpec(reactContext) {

    private val executorService = Executors.newSingleThreadExecutor()
    private var isScanning = false

    override fun getName() = NAME

    override fun setItem(value: String, key: String) {
        val sharedPref = reactApplicationContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(key, value)
        editor.apply()
    }

    override fun getItem(key: String): String? {
        val sharedPref = reactApplicationContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString(key, null)
        return username.toString()
    }

    override fun removeItem(key: String) {
        val sharedPref = reactApplicationContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.remove(key)
        editor.apply()
    }

    override fun clear() {
        val sharedPref = reactApplicationContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()
    }

    override fun scanDevice(duration: Double, promise: Promise?) {
        if (isScanning) {
            promise?.reject("SCAN_IN_PROGRESS", "Scanning is already in progress")
            return
        }

        isScanning = true
        val devices: WritableArray = Arguments.createArray()

        executorService.execute {
            try {
                YCBTClient.startScanBle(object : BleScanResponse {
                    override fun onScanResponse(code: Int, scanDevice: ScanDeviceBean?) {
                        if (scanDevice != null) {
                            val device = Arguments.createMap()
                            device.putString("macAddress", scanDevice.deviceMac ?: "Unknown MAC")
                            device.putString("deviceName", scanDevice.deviceName ?: "Unknown Device")
                            device.putInt("rssi", scanDevice.deviceRssi)
                            devices.pushMap(device)
                        }
                    }
                }, duration.toInt())

                // Simulate scan duration
                Thread.sleep((duration * 1000).toLong())
                isScanning = false

                // Resolve with scanned devices
                promise?.resolve(devices)
            } catch (e: InterruptedException) {
                isScanning = false
                promise?.reject("SCAN_INTERRUPTED", "Scan was interrupted: ${e.message}")
            } catch (e: Exception) {
                isScanning = false
                promise?.reject("SCAN_FAILED", "Scan failed due to an unexpected error: ${e.message}")
            }
        }
    }

    override fun connectDevice(macAddress: String?, promise: Promise?) {
        if (macAddress.isNullOrEmpty()) {
            promise?.reject("INVALID_MAC", "MAC address is empty")
            return
        }

        try {
            YCBTLog.e("Stopping BLE scanning before connection")
            YCBTClient.stopScanBle()

            YCBTLog.e("Attempting to connect to MAC: $macAddress")

            YCBTClient.connectBle(macAddress, object : BleConnectResponse {
                override fun onConnectResponse(code: Int) {
                    YCBTLog.e("Connection response code: $code")

                    when (code) {
                        0 -> {
                            val result = Arguments.createMap()
                            result.putString("message", "Device connected successfully")
                            result.putInt("code", code)
                            promise?.resolve(result)
                            emitBluetoothStateChange("Connected")

                        }
                        Constants.BLEState.Disconnect -> {
                            promise?.reject("DISCONNECTED", "Device disconnected unexpectedly")
                        }
                        Constants.BLEState.TimeOut -> {
                            promise?.reject("TIMEOUT", "Connection timed out")
                        }
                        Constants.BLEState.Connecting -> {
                            promise?.reject("CONNECTING", "Device is still connecting")
                        }
                        else -> {
                            promise?.reject("CONNECT_FAILED", "Failed to connect. State code: $code")
                        }
                    }
                }
            })
        } catch (e: Throwable) {
            YCBTLog.e("Connection error: ${e.message}")
            promise?.reject("CONNECTION_ERROR", "Error occurred during connection: ${e.stackTraceToString()}")
        }
    }
   override fun reconnectBle(promise: Promise) {
        try {
            // Retrieve the stored MAC address
            val macAddress = SPUtil.getBindedDeviceMac()
            val deviceName = SPUtil.getBindedDeviceName()

            if (macAddress != null && !macAddress.isEmpty()) {
                // Try to reconnect using the MAC address and device name
                YCBTClient.connectBle(macAddress, object : BleConnectResponse {
                    override fun onConnectResponse(code: Int) {
                        YCBTLog.e("Connection response code: $code")

                        when (code) {
                            0 -> {
                                val result = Arguments.createMap()
                                result.putString("message", "Device connected successfully")
                                result.putInt("code", code)
                                result.putString("deviceName", deviceName)
                                result.putString("macAddress", macAddress)
                                promise.resolve(result)
                                emitBluetoothStateChange("Connected")

                            }
                            Constants.BLEState.Disconnect -> {
                                promise.reject("DISCONNECTED", "Device disconnected unexpectedly")
                            }
                            Constants.BLEState.TimeOut -> {
                                promise.reject("TIMEOUT", "Connection timed out")
                            }
                            Constants.BLEState.Connecting -> {
                                promise.reject("CONNECTING", "Device is still connecting")
                            }
                            else -> {
                                promise.reject("CONNECT_FAILED", "Failed to connect. State code: $code")
                            }
                        }
                    }
                })


            } else {
                promise.reject("NO_DEVICE", "No device found to reconnect.")
            }
        } catch (e: java.lang.Exception) {
            promise.reject("ERROR", e.message)
        }
    }


    override fun disconnectDevice(promise: Promise?) {
        try {
            val connectState = YCBTClient.connectState()
            if (connectState != Constants.BLEState.ReadWriteOK) {
                promise?.reject("DEVICE_NOT_CONNECTED", "Device is not connected or ready for disconnection")
                return
            }

            YCBTClient.disconnectBle()

            if (reactApplicationContext.hasActiveCatalystInstance()) {
                emitBluetoothStateChange("Disconnected")
            } else {
                YCBTLog.e("React Native context is not active; skipping state emission")
            }

            promise?.resolve("Device disconnected successfully")
        } catch (e: IllegalStateException) {
            promise?.reject("ILLEGAL_STATE_ERROR", "Illegal state encountered during disconnection: ${e.localizedMessage}")
        } catch (e: SecurityException) {
            promise?.reject("SECURITY_ERROR", "Permission issue encountered during disconnection: ${e.localizedMessage}")
        } catch (e: Exception) {
            promise?.reject("GENERAL_ERROR", "An error occurred during disconnection: ${e.localizedMessage}")
        } catch (e: Throwable) {
            promise?.reject("CRITICAL_ERROR", "A critical error occurred during disconnection: ${e.stackTraceToString()}")
        } finally {
            YCBTLog.e("Disconnection process completed")
        }
    }

    override fun fetchHealthData(dataType: String?, promise: Promise?) {
        val dataTypeCode = dataType?.let { mapHealthDataType(it) }
        if (dataTypeCode == null) {
            promise?.reject("INVALID_DATA_TYPE", "Invalid data type provided: $dataType")
            return
        }

        if (YCBTClient.connectState() != Constants.BLEState.ReadWriteOK) {
            promise?.reject("DEVICE_NOT_CONNECTED", "Device is not connected or ready")
            return
        }

        try {
            YCBTClient.healthHistoryData(dataTypeCode, object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.e("fetchHealthData", "Code: $code, Result: $resultMap")

                    if (code == 0 && resultMap != null) {
                        val result = Arguments.createMap()
                        result.putString("message", "$resultMap")
                        promise?.resolve(result)
                    } else if (code == 0 && resultMap.isNullOrEmpty()) {
                        promise?.reject("NO_DATA", "No data available for the requested type")
                    } else {
                        promise?.reject("DATA_FETCH_ERROR", "Error fetching health history data: $code")
                    }
                }
            })
        } catch (e: Throwable) {
            promise?.reject("ERROR", "Exception during data fetch: ${e.localizedMessage}")
        }
    }

    override fun deleteHealthData(dataType: String, promise: Promise) {

        val dataTypeCode = dataType.let { mapHealthDataType(it) }
        if (dataTypeCode == null) {
            promise.reject("INVALID_DATA_TYPE", "Invalid data type provided: $dataType")
            return
        }

        if (YCBTClient.connectState() != Constants.BLEState.ReadWriteOK) {
            promise.reject("DEVICE_NOT_CONNECTED", "Device is not connected or ready")
            return
        }
        try {
            YCBTClient.deleteHealthHistoryData(dataTypeCode ,object : BleDataResponse {
                override fun onDataResponse(code: Int, ratio: Float, resultMap: HashMap<*, *>?) {
                    Log.e("fetchHealthData", "Code: $code, Result: $resultMap")

                    if (code == 0 && resultMap != null) {
                        val result = Arguments.createMap()
                        result.putString("message", "$resultMap")
                        promise.resolve(result)
                    } else if (code == 0 && resultMap.isNullOrEmpty()) {
                        promise.reject("NO_DATA", "No data available for the requested type")
                    } else {
                        promise.reject("DATA_FETCH_ERROR", "Error fetching health history data: $code")
                    }
                }
            })
        } catch (e: Throwable) {
            promise.reject("ERROR", "Exception during data deletion: ${e.localizedMessage}")
    }
    }

    private fun mapHealthDataType(dataType: String): Int {
        return when (dataType) {
            "heartRate" -> Constants.DATATYPE.Health_HistoryHeart
            "bloodPressure" -> Constants.DATATYPE.Health_HistoryBlood
            "spo2" -> Constants.DATATYPE.Health_HistoryBloodOxygen
            "sleep" -> Constants.DATATYPE.Health_HistorySleep
            "sync" -> Constants.DATATYPE.Health_HistoryAll
            "temperature" -> Constants.DATATYPE.Health_HistoryComprehensiveMeasureData
            "steps" -> Constants.DATATYPE.Health_HistorySport
            "Uric" -> Constants.DATATYPE.Health_HistoryComprehensiveMeasureData
            else -> {
                YCBTLog.e("Invalid health data type: $dataType")
                -1
            }
        }
    }

    private fun registerBluetoothStateListener() {
        YCBTClient.registerBleStateChange(object : BleConnectResponse {
            override fun onConnectResponse(code: Int) {
                val state = when (code) {
                    Constants.BLEState.Disconnect -> "Disconnected"
                    Constants.BLEState.ReadWriteOK -> "Connected"
                    Constants.BLEState.Connecting -> "Connecting"
                    Constants.BLEState.TimeOut -> "Connection Timeout"
                    else -> "Unknown"
                }
                emitBluetoothStateChange(state)
            }
        })
    }

    private fun emitBluetoothStateChange(state: String) {
        reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("BluetoothStateChange", state)
    }

    // Handle window focus changes for React Native
    fun onWindowFocusChanged(hasFocus: Boolean) {
        try {
            if (reactApplicationContext.hasActiveCatalystInstance()) {
                if (hasFocus) {
                    YCBTLog.e("Window focus gained.")
                    // React to focus gain here, for example, restarting a scan or refreshing data
                } else {
                    YCBTLog.e("Window focus lost.")
                    // React to focus loss here, for example, pausing background tasks
                }
            } else {
                YCBTLog.e("React Native context is not ready, skipping window focus change")
            }
        } catch (e: Exception) {
            YCBTLog.e("Unexpected error during window focus change: ${e.message}")
        }
    }

    init {
        registerBluetoothStateListener()
    }

    companion object {
        const val NAME = "NativeLocalStorage"
    }
}
