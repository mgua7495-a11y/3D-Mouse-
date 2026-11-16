package com.example.a3dmausecad.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

enum class ConnectionStatus {
    NO_BLUETOOTH,
    DISCONNECTED,
    REGISTERED,
    CONNECTING,
    CONNECTED
}

class Bluetooth3DMouseManager(private val context: Context) {

    private val TAG = "Bluetooth3DMouseManager"

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID Device Profile Connected")
                bluetoothHidDevice = proxy as? BluetoothHidDevice
                registerHidApp()
                updatePairedDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID Device Profile Disconnected")
                bluetoothHidDevice = null
                _status.value = ConnectionStatus.DISCONNECTED
                _connectedDevice.value = null
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            if (registered) {
                if (_status.value != ConnectionStatus.CONNECTED) {
                    _status.value = ConnectionStatus.REGISTERED
                }
            } else {
                _status.value = ConnectionStatus.DISCONNECTED
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: device=${device?.name}, state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice.value = device
                    _status.value = ConnectionStatus.CONNECTED
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _status.value = ConnectionStatus.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_DISCONNECTING -> {
                    if (_connectedDevice.value == device) {
                        _connectedDevice.value = null
                    }
                    if (bluetoothHidDevice != null) {
                        _status.value = ConnectionStatus.REGISTERED
                    } else {
                        _status.value = ConnectionStatus.DISCONNECTED
                    }
                }
            }
        }
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = try { bluetoothAdapter?.isEnabled == true } catch (e: Throwable) { false }

    @SuppressLint("MissingPermission")
    fun initialize() {
        try {
            if (bluetoothAdapter == null) {
                _status.value = ConnectionStatus.NO_BLUETOOTH
                return
            }
            if (!bluetoothAdapter.isEnabled) {
                _status.value = ConnectionStatus.DISCONNECTED
                return
            }

            bluetoothAdapter.getProfileProxy(
                context.applicationContext,
                serviceListener,
                BluetoothProfile.HID_DEVICE
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Bluetooth HID Profile Proxy", e)
            _status.value = ConnectionStatus.NO_BLUETOOTH
        }
    }

    @SuppressLint("MissingPermission")
    fun updatePairedDevices() {
        try {
            if (bluetoothAdapter?.isEnabled == true) {
                _pairedDevices.value = bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update paired devices", e)
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        try {
            val hidDevice = bluetoothHidDevice ?: return

            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "3D Mouse Controller",
                "Android 3D Mouse CAD Controller",
                "3Dconnexion Compatible Mouse",
                0.toByte(),
                HidDescriptors.THREE_D_MOUSE_DESCRIPTOR
            )

            val qosSettings = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                900,
                0,
                0,
                10000
            )

            hidDevice.registerApp(
                sdpSettings,
                qosSettings,
                qosSettings,
                executor,
                hidCallback
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register HID App", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        try {
            val hidDevice = bluetoothHidDevice ?: return
            _status.value = ConnectionStatus.CONNECTING
            hidDevice.connect(device)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to connect to device", e)
            _status.value = ConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        try {
            val hidDevice = bluetoothHidDevice ?: return
            val device = _connectedDevice.value ?: return
            hidDevice.disconnect(device)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to disconnect device", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun send3DReport(
        x: Short = 0,
        y: Short = 0,
        z: Short = 0,
        rx: Short = 0,
        ry: Short = 0,
        rz: Short = 0
    ): Boolean {
        try {
            val hidDevice = bluetoothHidDevice ?: return false
            val device = _connectedDevice.value ?: return false

            val reportData = HidDescriptors.createReport(x, y, z, rx, ry, rz)
            return hidDevice.sendReport(
                device,
                HidDescriptors.REPORT_ID_3D_MOUSE.toInt(),
                reportData
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error sending 3D report", e)
            return false
        }
    }

    fun sendRotation(rx: Short, ry: Short, rz: Short): Boolean {
        return send3DReport(rx = rx, ry = ry, rz = rz)
    }

    fun sendZeroReport(): Boolean {
        return send3DReport(0, 0, 0, 0, 0, 0)
    }

    fun cleanup() {
        try {
            bluetoothHidDevice?.let { hid ->
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error cleaning up HID profile proxy", e)
        }
        bluetoothHidDevice = null
        _status.value = ConnectionStatus.DISCONNECTED
    }
}
