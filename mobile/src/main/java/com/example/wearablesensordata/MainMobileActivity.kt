package com.example.wearablesensordata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.wearablesensordata.databinding.ActivityMainBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityClient.OnCapabilityChangedListener
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainMobileActivity : AppCompatActivity(), OnCapabilityChangedListener,
    OnMessageReceivedListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var wearNodesWithApp: Set<Node>? = null
    private var allConnectedNodes: List<Node>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initWearAPIs()

        // Perform the initial update of the UI
        updateUI()

        initialRequestForWearDevices()
    }

    override fun onPause() {
        super.onPause()

        // Remove listeners
        capabilityClient.removeListener(this, CAPABILITY_WEAR_APP)
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onResume() {
        super.onResume()

        // Add listeners
        capabilityClient.addListener(this, CAPABILITY_WEAR_APP)
        Wearable.getMessageClient(this).addListener(this)
    }

    /*
     * When capabilities change (install/uninstall wear app).
     */
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        wearNodesWithApp = capabilityInfo.nodes

        lifecycleScope.launch {
            // Because we have an updated list of devices with/without our app, we need to also update
            // our list of active Wear devices.
            findAllWearDevices()
        }
    }

    // When message is received from MessageClient.
    override fun onMessageReceived(p0: MessageEvent) {
        Log.d("lombichh", "Message received: $p0")
        if (p0.path == SENSOR_MESSAGE_PATH) {
            binding.sensorTextview.text = String(p0.data)
        }
    }

    private fun initWearAPIs() {
        capabilityClient = Wearable.getCapabilityClient(this)
        nodeClient = Wearable.getNodeClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)
    }

    private fun initialRequestForWearDevices() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    // Initial request for devices with our capability, aka, our Wear app installed.
                    findWearDevicesWithApp()
                }
                launch {
                    // Initial request for all Wear devices connected (with or without our capability).
                    findAllWearDevices()
                }
            }
        }
    }

    private suspend fun findWearDevicesWithApp() {
        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                .await()

            withContext(Dispatchers.Main) {
                wearNodesWithApp = capabilityInfo.nodes
                updateUI()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
            throw cancellationException
        } catch (throwable: Throwable) {
            // Capability request failed to return any results
        }
    }

    private suspend fun findAllWearDevices() {
        try {
            val connectedNodes = nodeClient.connectedNodes.await()

            withContext(Dispatchers.Main) {
                allConnectedNodes = connectedNodes
                updateUI()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            // Node request failed to return any results
        }
    }

    private fun updateUI() {
        val wearNodesWithApp = wearNodesWithApp
        val allConnectedNodes = allConnectedNodes

        when {
            wearNodesWithApp == null || allConnectedNodes == null -> {
                // Waiting on Results for both connected nodes and nodes with app
                binding.infoTextview.text = getString(R.string.message_checking)
            }
            allConnectedNodes.isEmpty() -> {
                // No devices connected
                binding.infoTextview.text = getString(R.string.message_checking)
            }
            wearNodesWithApp.isEmpty() -> {
                // Missing on all devices
                binding.infoTextview.text = getString(R.string.message_missing_all)
            }
            wearNodesWithApp.size < allConnectedNodes.size -> {
                // Installed on some devices
                // TODO: Add code to communicate with the wear app via Wear APIs
                //       (MessageClient, DataClient, etc.)
                binding.infoTextview.text =
                    getString(R.string.message_some_installed, wearNodesWithApp.toString())
            }
            else -> {
                // TODO: Add code to communicate with the wear app via Wear APIs
                //       (MessageClient, DataClient, etc.)
                binding.infoTextview.text =
                    getString(R.string.message_all_installed, wearNodesWithApp.toString())
            }
        }
    }

    private fun openPlayStoreOnWearDevicesWithoutApp() {
        val wearNodesWithApp = wearNodesWithApp ?: return
        val allConnectedNodes = allConnectedNodes ?: return

        // Determine the list of nodes (wear devices) that don't have the app installed yet.
        val nodesWithoutApp = allConnectedNodes - wearNodesWithApp

        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(PLAY_STORE_APP_URI))

        // In parallel, start remote activity requests for all wear devices that don't have the app installed yet.
        nodesWithoutApp.forEach { node ->
            lifecycleScope.launch {
                try {
                    remoteActivityHelper.startRemoteActivity(
                        targetIntent = intent,
                        targetNodeId = node.id
                    ).await()

                    Toast.makeText(
                        this@MainMobileActivity,
                        getString(R.string.store_request_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (cancellationException: CancellationException) {
                    // Request was cancelled normally
                } catch (throwable: Throwable) {
                    Toast.makeText(
                        this@MainMobileActivity,
                        getString(R.string.store_request_unsuccessful),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        // Name of capability listed in Wear app's wear.xml.
        private const val CAPABILITY_WEAR_APP = "verify_remote_example_wear_app"

        // Links to Wear app (Play Store).
        // TODO: Replace with actual link.
        private const val PLAY_STORE_APP_URI =
            "market://details?id=com.example.wearablesensordata"

        // Sensor message path
        private const val SENSOR_MESSAGE_PATH = "/sensor"
    }
}
