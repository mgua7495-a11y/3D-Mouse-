package com.example.a3dmausecad.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

enum class TransportMode {
    WIFI_UDP,
    USB_TCP,
    BLUETOOTH_HID
}

enum class NetworkStatus {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED,
    ERROR
}

class Network3DMouseManager(private val context: Context? = null) {

    private val TAG = "Network3DMouseManager"

    private val prefs = context?.applicationContext?.getSharedPreferences("3d_mouse_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _status = MutableStateFlow(NetworkStatus.DISCONNECTED)
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _autoDiscoveredPcName = MutableStateFlow<String?>(null)
    val autoDiscoveredPcName: StateFlow<String?> = _autoDiscoveredPcName.asStateFlow()

    var activeMode = TransportMode.WIFI_UDP

    private val _activePreset = MutableStateFlow(prefs?.getString("active_preset", "fusion360") ?: "fusion360")
    val activePresetFlow: StateFlow<String> = _activePreset.asStateFlow()

    var activePreset: String
        get() = _activePreset.value
        set(value) {
            _activePreset.value = value
            prefs?.edit()?.putString("active_preset", value)?.apply()
        }

    // Network target parameters (Persisted in SharedPreferences)
    var pcIpAddress: String
        get() = prefs?.getString("pc_ip", "127.0.0.1") ?: "127.0.0.1"
        set(value) { prefs?.edit()?.putString("pc_ip", value)?.apply() }

    var pcPort: Int
        get() = prefs?.getInt("pc_port", 9876) ?: 9876
        set(value) { prefs?.edit()?.putInt("pc_port", value)?.apply() }

    fun clearError() {
        _lastError.value = null
        if (_status.value == NetworkStatus.ERROR) {
            _status.value = NetworkStatus.DISCONNECTED
        }
    }

    // Helper to get Wi-Fi / Hotspot Gateway IP address (e.g. 192.168.137.1 for Hotspot)
    @Suppress("DEPRECATION")
    fun getGatewayIpAddress(): String? {
        try {
            val wifiManager = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifiManager?.dhcpInfo ?: return null
            if (dhcp.gateway == 0) return null
            val ipInt = dhcp.gateway
            return String.format(
                java.util.Locale.US,
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            return null
        }
    }

    // UDP Socket
    private var udpSocket: DatagramSocket? = null
    private var discoveryJob: Job? = null

    // TCP Socket (For USB / Direct TCP)
    private var tcpSocket: Socket? = null
    private var tcpOutputStream: OutputStream? = null

    fun startAutoDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            try {
                _status.value = NetworkStatus.SEARCHING
                Log.d(TAG, "Starting Fast Auto-Discovery for PC...")

                val socket = DatagramSocket().apply { broadcast = true }
                udpSocket = socket

                val discoverMsg = JSONObject().apply {
                    put("type", "3d_mouse_discover")
                }.toString().toByteArray(Charsets.UTF_8)

                val targets = mutableListOf<String>()
                if (pcIpAddress.isNotEmpty() && pcIpAddress != "127.0.0.1") {
                    targets.add(pcIpAddress)
                }
                getGatewayIpAddress()?.let { if (!targets.contains(it)) targets.add(it) }
                targets.add("255.255.255.255")

                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)

                socket.soTimeout = 250 // Fast 250ms timeout for instant discovery

                var attempts = 0
                while (isActive && _status.value != NetworkStatus.CONNECTED && attempts < 5) {
                    for (targetIp in targets) {
                        try {
                            val addr = InetAddress.getByName(targetIp)
                            val packet = DatagramPacket(discoverMsg, discoverMsg.size, addr, pcPort)
                            socket.send(packet)
                            socket.receive(responsePacket)

                            val respText = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                            val respJson = JSONObject(respText)

                            if (respJson.optString("type") == "3d_mouse_pc_beacon") {
                                val pcIp = responsePacket.address.hostAddress ?: ""
                                val pcName = respJson.optString("name", "PC Receiver")
                                if (pcIp.isNotEmpty()) {
                                    // Prioritize keeping the user's saved IP address if it responded
                                    if (pcIpAddress.isEmpty() || pcIpAddress == "127.0.0.1" || targetIp == pcIpAddress) {
                                        pcIpAddress = pcIp
                                    }
                                    _autoDiscoveredPcName.value = pcName
                                    _status.value = NetworkStatus.CONNECTED
                                    Log.d(TAG, "Fast Auto-Discovered PC: $pcName at $pcIp")
                                    break
                                }
                            }
                        } catch (_: Exception) {
                            // Timeout - retry next target
                        }
                    }
                    attempts++
                    delay(150)
                }

                if (_status.value != NetworkStatus.CONNECTED) {
                    connect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-Discovery Error", e)
                connect()
            }
        }
    }

    fun connect() {
        scope.launch {
            try {
                _status.value = NetworkStatus.CONNECTING
                _lastError.value = null

                when (activeMode) {
                    TransportMode.WIFI_UDP -> {
                        if (udpSocket == null || udpSocket?.isClosed == true) {
                            udpSocket = DatagramSocket()
                        }
                        _status.value = NetworkStatus.CONNECTED
                        Log.d(TAG, "UDP Socket initialized for $pcIpAddress:$pcPort")
                    }
                    TransportMode.USB_TCP -> {
                        closeTcp()
                        try {
                            val socket = Socket(pcIpAddress, pcPort)
                            socket.tcpNoDelay = true
                            tcpSocket = socket
                            tcpOutputStream = socket.getOutputStream()
                            _status.value = NetworkStatus.CONNECTED
                            Log.d(TAG, "TCP Connected to $pcIpAddress:$pcPort")
                        } catch (e: Exception) {
                            Log.e(TAG, "TCP Connection failed for USB mode", e)
                            _status.value = NetworkStatus.ERROR
                            _lastError.value = "USB TCP Failed. Run 'adb forward tcp:9876 tcp:9876'"
                        }
                    }
                    TransportMode.BLUETOOTH_HID -> {
                        // Handled by Bluetooth3DMouseManager
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _status.value = NetworkStatus.ERROR
                _lastError.value = e.localizedMessage
            }
        }
    }

    fun disconnect() {
        discoveryJob?.cancel()
        closeUdp()
        closeTcp()
        _status.value = NetworkStatus.DISCONNECTED
    }

    private fun closeUdp() {
        try {
            udpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing UDP socket", e)
        }
        udpSocket = null
    }

    private fun closeTcp() {
        try {
            tcpOutputStream?.close()
            tcpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TCP socket", e)
        }
        tcpOutputStream = null
        tcpSocket = null
    }

    private fun sendPayloadUdp(payload: ByteArray) {
        val socket = udpSocket ?: DatagramSocket().also { udpSocket = it }

        // Target 1: Configured pcIpAddress
        try {
            val address1 = InetAddress.getByName(pcIpAddress)
            val packet1 = DatagramPacket(payload, payload.size, address1, pcPort)
            socket.send(packet1)
        } catch (_: Exception) {}

        // Target 2: Hotspot / Wi-Fi Gateway IP (e.g. 192.168.137.1 for Windows Hotspot)
        val gatewayIp = getGatewayIpAddress()
        if (gatewayIp != null && gatewayIp != pcIpAddress && gatewayIp != "0.0.0.0" && gatewayIp != "127.0.0.1") {
            try {
                val address2 = InetAddress.getByName(gatewayIp)
                val packet2 = DatagramPacket(payload, payload.size, address2, pcPort)
                socket.send(packet2)
            } catch (_: Exception) {}
        }

        if (_status.value != NetworkStatus.CONNECTED) {
            _status.value = NetworkStatus.CONNECTED
        }
    }

    fun send3DData(
        x: Short = 0,
        y: Short = 0,
        z: Short = 0,
        rx: Short = 0,
        ry: Short = 0,
        rz: Short = 0,
        isTouchDown: Boolean = true
    ) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "3d_mouse")
                    put("touch_down", isTouchDown)
                    put("x", x.toInt())
                    put("y", y.toInt())
                    put("z", z.toInt())
                    put("rx", rx.toInt())
                    put("ry", ry.toInt())
                    put("rz", rz.toInt())
                    put("preset", activePreset)
                }
                val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)

                when (activeMode) {
                    TransportMode.WIFI_UDP -> {
                        sendPayloadUdp(payload)
                    }
                    TransportMode.USB_TCP -> {
                        val out = tcpOutputStream
                        if (out != null) {
                            try {
                                out.write(payload)
                                out.flush()
                            } catch (e: Exception) {
                                sendPayloadUdp(payload)
                            }
                        } else {
                            sendPayloadUdp(payload)
                        }
                    }
                    TransportMode.BLUETOOTH_HID -> {
                        // Bluetooth handled separately
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending 3D data", e)
                _status.value = NetworkStatus.ERROR
                _lastError.value = e.localizedMessage
            }
        }
    }

    fun sendZeroData() {
        send3DData(0, 0, 0, 0, 0, 0, isTouchDown = false)
    }

    fun sendHomeCommand() {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "3d_mouse_home")
                    put("preset", activePreset)
                }
                val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
                sendPayloadUdp(payload)
                Log.d(TAG, "Sent Home/Reset command")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending home command", e)
            }
        }
    }

    fun sendUndoCommand() {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "3d_mouse_undo")
                    put("preset", activePreset)
                }
                val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
                sendPayloadUdp(payload)
                Log.d(TAG, "Sent Undo (Ctrl+Z) command")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending undo command", e)
            }
        }
    }

    fun sendRedoCommand() {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "3d_mouse_redo")
                    put("preset", activePreset)
                }
                val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
                sendPayloadUdp(payload)
                Log.d(TAG, "Sent Redo (Ctrl+Y) command")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending redo command", e)
            }
        }
    }

    fun sendViewPlaneCommand(plane: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "3d_mouse_view_plane")
                    put("plane", plane.lowercase())
                    put("preset", activePreset)
                }
                val payload = (json.toString() + "\n").toByteArray(Charsets.UTF_8)
                sendPayloadUdp(payload)
                Log.d(TAG, "Sent View Plane command for plane: $plane")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending view plane command", e)
            }
        }
    }
}
