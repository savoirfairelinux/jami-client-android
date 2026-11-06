/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.HardwareService
import net.jami.services.PreferencesService
import java.net.InetAddress

internal class DaemonNetworkMonitor(
    private val context: Context,
    private val isDaemonReady: () -> Boolean,
    private val accounts: AccountService,
    private val calls: CallService,
    private val hardware: HardwareService,
    private val preferences: PreferencesService,
    private val onConnectivityChanged: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false
    private var networkCallbackRegistered = false
    private var networkDeactivationScheduled = false
    private val connectivityChecker = Runnable { updateConnectivityState() }
    private val deactivateAfterNetworkLoss = Runnable {
        networkDeactivationScheduled = false
        if (receiverRegistered && isDaemonReady() && !preferences.hasNetworkConnected())
            accounts.setAccountsActive(false)
    }

    private val monitor = object : ConnectivityManager.NetworkCallback() {
        private var defaultNetwork: Network? = null
        private var defaultAddresses: Set<InetAddress> = emptySet()

        override fun onAvailable(network: Network) {
            if (!receiverRegistered) return
            if (defaultNetwork != network) {
                Log.i(TAG, "Default network changed to $network")
                defaultNetwork = network
                defaultAddresses = emptySet()
            }
            cancelNetworkDeactivation()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            if (!receiverRegistered || network != defaultNetwork) return
            val addresses = linkProperties.linkAddresses.map { it.address }.toSet()
            if (addresses.isEmpty() || addresses == defaultAddresses) return
            defaultAddresses = addresses
            updateConnectivityState(true)
            if (receiverRegistered && isDaemonReady()) hardware.networkInterfaceChanged()
        }

        override fun onLost(network: Network) {
            if (!receiverRegistered || network != defaultNetwork) return
            Log.i(TAG, "Default network lost: $network")
            defaultNetwork = null
            defaultAddresses = emptySet()
            updateConnectivityState(false)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> updateConnectivityState()
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    connectivityChecker.run()
                    handler.postDelayed(connectivityChecker, 100)
                }
            }
        }
    }

    fun start() {
        if (receiverRegistered || !isDaemonReady()) return
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        })
        receiverRegistered = true
        updateConnectivityState()
        try {
            context.getSystemService(ConnectivityManager::class.java)?.let {
                it.registerDefaultNetworkCallback(monitor, handler)
                networkCallbackRegistered = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't register network callback", e)
        }
    }

    fun stop() {
        if (receiverRegistered) {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
        if (networkCallbackRegistered) {
            try {
                context.getSystemService(ConnectivityManager::class.java)
                    ?.unregisterNetworkCallback(monitor)
            } catch (e: Exception) {
                Log.w(TAG, "Can't unregister network callback", e)
            } finally {
                networkCallbackRegistered = false
            }
        }
        cancelNetworkDeactivation()
        handler.removeCallbacks(connectivityChecker)
    }

    private fun cancelNetworkDeactivation() {
        handler.removeCallbacks(deactivateAfterNetworkLoss)
        networkDeactivationScheduled = false
    }

    private fun updateConnectivityState(isConnected: Boolean = preferences.hasNetworkConnected()) {
        if (!receiverRegistered || !isDaemonReady()) return
        if (isConnected) {
            cancelNetworkDeactivation()
            accounts.setAccountsActive(true)
        } else if (calls.hasActiveCalls()) {
            if (!networkDeactivationScheduled) {
                networkDeactivationScheduled = true
                handler.postDelayed(deactivateAfterNetworkLoss, CALL_RECOVERY_TIMEOUT_MS)
            }
        } else {
            cancelNetworkDeactivation()
            accounts.setAccountsActive(false)
        }
        hardware.connectivityChanged(isConnected)
        onConnectivityChanged()
    }

    companion object {
        private const val TAG = "DaemonNetworkMonitor"
        private const val CALL_RECOVERY_TIMEOUT_MS = 20_000L
    }
}
