package com.example.wearablesensordata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.wearablesensordata.data.SensorData
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

    private var connectedNodesWithApp: Set<Node>? = null
    private var allConnectedNodes: List<Node>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()

        initWearAPIs()

        initialRequestForWearDevices()
    }

    override fun onPause() {
        super.onPause()

        capabilityClient.removeListener(this, CAPABILITY_WEAR_APP)
    }

    override fun onResume() {
        super.onResume()

        capabilityClient.addListener(this, CAPABILITY_WEAR_APP)
    }

    /*
     * When capabilities change (install/uninstall wear app).
     */
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        connectedNodesWithApp = capabilityInfo.nodes

        lifecycleScope.launch {
            // Because we have an updated list of devices with/without our app,
            // we need to also update our list of active Wear devices.
            updateAllWearDevices()
        }
    }

    /*
     * When message is received from MessageClient.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == SENSOR_MESSAGE_PATH) {
            val sensorType = messageEvent.data[0]

            when (sensorType) {
                SensorData.ACCELEROMETER -> {
                    val accelerometerValues =
                        SensorData.sensorMessageToAccelerometerValues(messageEvent.data)

                    binding.accelerometerXaxisValueTextview.text =
                        String.format("%.2f", accelerometerValues[0])
                    binding.accelerometerYaxisValueTextview.text =
                        String.format("%.2f", accelerometerValues[1])
                    binding.accelerometerZaxisValueTextview.text =
                        String.format("%.2f", accelerometerValues[2])
                }

                SensorData.GYROSCOPE -> {
                    val gyroscopeValues =
                        SensorData.sensorMessageToGyroscopeValues(messageEvent.data)

                    binding.gyroscopeXaxisValueTextview.text =
                        String.format("%.2f", gyroscopeValues[0])
                    binding.gyroscopeYaxisValueTextview.text =
                        String.format("%.2f", gyroscopeValues[1])
                    binding.gyroscopeZaxisValueTextview.text =
                        String.format("%.2f", gyroscopeValues[2])
                }

                SensorData.MAGNETOMETER -> {
                    val magnetometerValues =
                        SensorData.sensorMessageToMagnetometerValues(messageEvent.data)

                    binding.magnetometerXaxisValueTextview.text =
                        String.format("%.2f", magnetometerValues[0])
                    binding.magnetometerYaxisValueTextview.text =
                        String.format("%.2f", magnetometerValues[1])
                    binding.magnetometerZaxisValueTextview.text =
                        String.format("%.2f", magnetometerValues[2])
                }

                SensorData.HEART_RATE -> {
                    val heartRateValue =
                        SensorData.sensorMessageToHeartRateValue(messageEvent.data)

                    binding.heartRateValueTextview.text = heartRateValue.toString()
                }

                SensorData.LIGHT -> {
                    val lightValue =
                        SensorData.sensorMessageToLightValue(messageEvent.data)

                    binding.lightValueTextview.text = String.format("%.1f", lightValue)
                }

                SensorData.TEMPERATURE -> {
                    val temperatureValue =
                        SensorData.sensorMessageToTemperatureValue(messageEvent.data)

                    binding.temperatureValueTextview.text = String.format("%.1f", temperatureValue)
                }

                SensorData.HUMIDITY -> {
                    val humidityValue =
                        SensorData.sensorMessageToHumidityValue(messageEvent.data)

                    binding.humidityValueTextview.text = String.format("%.1f", humidityValue)
                }

                SensorData.PROXIMITY -> {
                    val proximityValue =
                        SensorData.sensorMessageToProximityValue(messageEvent.data)

                    binding.proximityValueTextview.text = String.format("%.1f", proximityValue)
                }

                SensorData.PRESSURE -> {
                    val pressureValue =
                        SensorData.sensorMessageToPressureValue(messageEvent.data)

                    binding.pressureValueTextview.text = String.format("%.1f", pressureValue)
                }
            }
        }
    }

    private fun initUi() {
        binding.checkingPairedWearablesLayout.setOnClickListener {
            // Remove possibility to scroll dashboard while checking pairing state
            binding.dashboardScrollview.requestDisallowInterceptTouchEvent(true)
        }

        binding.installAppButton.setOnClickListener {
            openPlayStoreOnWearDevicesWithoutApp()
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
                    updateWearDevicesWithApp()
                }
                launch {
                    // Initial request for all Wear devices connected (with or without our capability).
                    updateAllWearDevices()
                }
            }
        }
    }

    private suspend fun updateWearDevicesWithApp() {
        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL)
                .await()

            withContext(Dispatchers.Main) {
                connectedNodesWithApp = capabilityInfo.nodes
                checkPairingState()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
            throw cancellationException
        } catch (throwable: Throwable) {
            // Capability request failed to return any results
        }
    }

    private suspend fun updateAllWearDevices() {
        try {
            val connectedNodes = nodeClient.connectedNodes.await()

            withContext(Dispatchers.Main) {
                allConnectedNodes = connectedNodes
                checkPairingState()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            // Node request failed to return any results
        }
    }

    private fun checkPairingState() {
        val connectedNodesWithApp = connectedNodesWithApp
        val allConnectedNodes = allConnectedNodes

        when {
            connectedNodesWithApp == null || allConnectedNodes == null -> {
                // Waiting on Results for both connected nodes and nodes with app
                binding.deviceConnectedLayout.visibility = View.GONE

                binding.connectedDeviceWithoutAppTextview.visibility = View.GONE
                binding.installAppButton.visibility = View.GONE
                binding.checkingPairedWearablesProgressbar.visibility = View.VISIBLE
                binding.checkingPairedWearablesTextview.visibility = View.VISIBLE
                binding.checkingPairedWearablesLayout.visibility = View.VISIBLE
            }

            allConnectedNodes.isEmpty() -> {
                // No devices connected
                Wearable.getMessageClient(this).removeListener(this)

                // Update ui
                binding.deviceConnectedLayout.visibility = View.GONE

                binding.connectedDeviceWithoutAppTextview.visibility = View.GONE
                binding.installAppButton.visibility = View.GONE
                binding.checkingPairedWearablesProgressbar.visibility = View.VISIBLE
                binding.checkingPairedWearablesTextview.visibility = View.VISIBLE
                binding.checkingPairedWearablesLayout.visibility = View.VISIBLE
            }

            connectedNodesWithApp.isEmpty() -> {
                // Missing on all devices
                Wearable.getMessageClient(this).removeListener(this)

                // Update ui
                binding.deviceConnectedLayout.visibility = View.GONE

                binding.connectedDeviceWithoutAppTextview.text =
                    getString(
                        R.string.wearable_connected_without_app,
                        allConnectedNodes.firstOrNull()?.displayName
                    )
                binding.checkingPairedWearablesProgressbar.visibility = View.GONE
                binding.checkingPairedWearablesTextview.visibility = View.GONE
                binding.connectedDeviceWithoutAppTextview.visibility = View.VISIBLE
                binding.installAppButton.visibility = View.VISIBLE
                binding.checkingPairedWearablesLayout.visibility = View.VISIBLE
            }

            connectedNodesWithApp.size < allConnectedNodes.size -> {
                // Installed on some devices
                Wearable.getMessageClient(this).addListener(this)

                // Update ui
                connectedNodesWithApp.firstOrNull()?.let {
                    binding.deviceConnectedTextview.text = getString(
                        R.string.message_device_connected,
                        it.displayName
                    )
                    binding.deviceConnectedLayout.visibility = View.VISIBLE
                }

                binding.checkingPairedWearablesLayout.visibility = View.GONE
            }

            else -> {
                // Installed on all devices
                Wearable.getMessageClient(this).addListener(this)

                // Update connected device textview
                connectedNodesWithApp.firstOrNull()?.let {
                    binding.deviceConnectedTextview.text = getString(
                        R.string.message_device_connected,
                        it.displayName
                    )
                    binding.deviceConnectedLayout.visibility = View.VISIBLE
                }

                binding.checkingPairedWearablesLayout.visibility = View.GONE
            }
        }
    }

    private fun openPlayStoreOnWearDevicesWithoutApp() {
        val wearNodesWithApp = connectedNodesWithApp ?: return
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
                    // Request failed to start remote activity
                }
            }
        }
    }

    companion object {
        // Name of capability listed in Wear app's wear.xml.
        private const val CAPABILITY_WEAR_APP = "verify_remote_example_wear_app"

        // Links to Wear app (Play Store).
        private const val PLAY_STORE_APP_URI =
            "market://details?id=com.example.wearablesensordata"

        // Sensor message path
        private const val SENSOR_MESSAGE_PATH = "/sensor"
    }
}
