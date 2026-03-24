package com.example.echo.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        // We use a Set to track multiple simultaneous connections (e.g., Wi-Fi AND Mobile Data)
        val validNetworks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Fired the instant a network with basic internet routing connects
                validNetworks.add(network)
                trySend(validNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                // Fired only when the physical connection drops entirely
                validNetworks.remove(network)
                trySend(validNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Initial check: We loop through ALL networks instead of relying on Android's
        // strict "activeNetwork" (which becomes null during unstable Wi-Fi states).
        val isCurrentlyOnline = connectivityManager.allNetworks.any { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
        trySend(isCurrentlyOnline)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}