package com.example.a3dmausecad.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getLocalIpAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && !sAddr.contains(':') && !sAddr.startsWith("127.")) {
                            // Returns valid non-loopback IPv4 address
                            return sAddr
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // Ignore network interface enumeration errors
        }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val ip = wifiManager.connectionInfo?.ipAddress ?: 0
                if (ip != 0) {
                    return Formatter.formatIpAddress(ip)
                }
            }
        } catch (e: Throwable) {
            // Ignore wifi manager permission or security errors
        }

        return "192.168.1.x"
    }
}
