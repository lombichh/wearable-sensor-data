package com.example.wearablesensordata

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    override fun onMessageReceived(p0: MessageEvent) {
        if (p0.path == SENSOR_MESSAGE_PATH) {
            val sensorType = p0.data[0]

            when (sensorType) {
                /*SensorData.ACCELEROMETER -> {
                    val accelerometerValues =
                        SensorData.sensorMessageToAccelerometerValues(p0.data)
                    binding.accelerometerTextview.text =
                        "AccelerometerX: ${accelerometerValues[0]}\nAccelerometerY: ${accelerometerValues[1]}\nAccelerometerZ: ${accelerometerValues[2]}"
                }

                SensorData.GYROSCOPE -> {
                    val gyroscopeValues =
                        SensorData.sensorMessageToGyroscopeValues(p0.data)
                    binding.gyroscopeTextview.text =
                        "GyroscopeX: ${gyroscopeValues[0]}\nGyroscopeY: ${gyroscopeValues[1]}\nGyroscopeZ: ${gyroscopeValues[2]}"
                }

                SensorData.TEMPERATURE -> {
                    val temperatureValue =
                        SensorData.sensorMessageToTemperatureValue(p0.data)
                    binding.temperatureTextview.text = "Temperature: $temperatureValue"
                }

                SensorData.LIGHT -> {
                    val lightValue =
                        SensorData.sensorMessageToLightValue(p0.data)
                    binding.lightTextview.text = "Light: $lightValue"
                }*/
            }
        }
    }

    private fun initUi() {
        setTitleStyle()
    }

    private fun setTitleStyle() {
        /*// Find index of substring "sensor" to apply a different style
        val startSubstring = getString(R.string.title_main_activity).indexOf("sensor")
        val endSubstring = startSubstring + "sensor".length

        val spannable = SpannableStringBuilder(getString(R.string.title_main_activity))

        // Apply different style to "sensor" substring
        spannable.setSpan(
            ForegroundColorSpan(getColor(android.R.color.white)),
            startSubstring,
            endSubstring,
            0
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            startSubstring,
            endSubstring,
            0
        )

        // Aggiungi la Drawable di sfondo dietro alla parola "sensor"
        val drawable: Drawable? = ResourcesCompat.getDrawable(resources, R.drawable.rounded_rectangle, null)
        drawable?.setBounds(0, 0, 350, 100)

        // Usa BackgroundColorSpan per impostare lo sfondo colorato
        spannable.setSpan(BackgroundColorSpan(ContextCompat.getColor(this, android.R.color.black)), startSubstring, endSubstring, 0)

        // Usa ImageSpan per sovrapporre la Drawable come sfondo colorato
        spannable.setSpan(drawable?.let { ImageSpan(it, ImageSpan.ALIGN_BOTTOM) }, startSubstring, endSubstring, 0)

        // Imposta il testo formattato nella TextView
        binding.titleTextview.text = spannable*/
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
            /*connectedNodesWithApp == null || allConnectedNodes == null -> {
                // Waiting on Results for both connected nodes and nodes with app
                binding.infoTextview.text = getString(R.string.message_checking)
            }

            allConnectedNodes.isEmpty() -> {
                // No devices connected
                binding.infoTextview.text = getString(R.string.message_checking)

                Wearable.getMessageClient(this).removeListener(this)
            }

            connectedNodesWithApp.isEmpty() -> {
                // Missing on all devices
                binding.infoTextview.text = getString(R.string.message_missing_all)

                Wearable.getMessageClient(this).removeListener(this)
            }

            connectedNodesWithApp.size < allConnectedNodes.size -> {
                // Installed on some devices
                binding.infoTextview.text =
                    getString(R.string.message_some_installed, connectedNodesWithApp.toString())

                Wearable.getMessageClient(this).addListener(this)
            }

            else -> {
                // Installed on all devices
                binding.infoTextview.text =
                    getString(R.string.message_all_installed, connectedNodesWithApp.toString())

                Wearable.getMessageClient(this).addListener(this)
            }*/
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
