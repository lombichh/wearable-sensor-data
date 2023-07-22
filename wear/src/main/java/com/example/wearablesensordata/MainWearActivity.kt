package com.example.wearablesensordata

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.example.wearablesensordata.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainWearActivity : FragmentActivity(), CapabilityClient.OnCapabilityChangedListener,
    SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    // Capability vars
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var androidPhoneNodeWithApp: Node? = null

    // Sensor vars
    private lateinit var sensorManager: SensorManager
    private var mLight: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMobileAPIs()
        initSensor()

        binding.infoTextview.text = getString(R.string.message_checking)
    }

    override fun onPause() {
        super.onPause()

        Wearable.getCapabilityClient(this).removeListener(this, CAPABILITY_PHONE_APP)
    }

    override fun onResume() {
        super.onResume()

        // Register capability listener and check if phone has app manually the first time.
        Wearable.getCapabilityClient(this).addListener(this, CAPABILITY_PHONE_APP)
        lifecycleScope.launch {
            checkIfPhoneHasApp()
        }
    }

    /*
     * When capabilities change (install/uninstall phone app).
     */
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        // There should only ever be one phone in a node set (much less w/ the correct
        // capability), so I am just grabbing the first one (which should be the only one).
        androidPhoneNodeWithApp = capabilityInfo.nodes.firstOrNull()

        checkPairingState()
    }

    /*
     * When sensor data changes.
     */
    override fun onSensorChanged(p0: SensorEvent?) {
        // Send sensor data to phone through MessageClient
        androidPhoneNodeWithApp?.id?.also { nodeId ->
            val sendTask: Task<*> = Wearable.getMessageClient(this).sendMessage(
                nodeId,
                SENSOR_MESSAGE_PATH,
                p0?.values?.get(0).toString()?.toByteArray()
            ).apply {
                addOnSuccessListener {
                    // Message sent successfully with no errors
                    Log.d("lombichh", "Message sent")
                }
                addOnFailureListener {
                    // Message failed to send
                    Log.d("lombichh", "Message failed")
                }
            }
        }

        binding.sensorTextview.text = p0?.values?.get(0).toString()
    }

    /*
     * When sensor accuracy changes.
     */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun initMobileAPIs() {
        capabilityClient = Wearable.getCapabilityClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)
    }

    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    private suspend fun checkIfPhoneHasApp() {
        try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_ALL)
                .await()

            withContext(Dispatchers.Main) {
                // There should only ever be one phone in a node set (much less w/ the correct
                // capability), so I am just grabbing the first one (which should be the only one).
                androidPhoneNodeWithApp = capabilityInfo.nodes.firstOrNull()
                checkPairingState()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            // Capability request failed to return any results
        }
    }

    private fun checkPairingState() {
        val androidPhoneNodeWithApp = androidPhoneNodeWithApp

        if (androidPhoneNodeWithApp != null) {
            // App is installed on remote node
            binding.infoTextview.text =
                getString(R.string.message_installed, androidPhoneNodeWithApp.displayName)

            registerSensorListeners()
        } else {
            // App is missing on remote node
            binding.infoTextview.text = getString(R.string.message_missing)

            sensorManager.unregisterListener(this)
        }
    }

    private fun registerSensorListeners() {
        mLight?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun openAppInStoreOnPhone() {
        // Create Remote Intent to open Play Store listing of app on remote device.
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(PLAY_STORE_APP_URI))

        lifecycleScope.launch {
            try {
                remoteActivityHelper.startRemoteActivity(intent).await()

                ConfirmationOverlay().showOn(this@MainWearActivity)
            } catch (cancellationException: CancellationException) {
                // Request was cancelled normally
                throw cancellationException
            } catch (throwable: Throwable) {
                ConfirmationOverlay()
                    .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                    .showOn(this@MainWearActivity)
            }
        }
    }

    companion object {
        // Name of capability listed in Phone app's wear.xml.
        private const val CAPABILITY_PHONE_APP = "verify_remote_example_phone_app"

        // Links to Android mobile app (Play Store).
        // TODO: Replace with actual link.
        private const val PLAY_STORE_APP_URI =
            "market://details?id=com.example.wearablesensordata"

        // Sensor message path
        private const val SENSOR_MESSAGE_PATH = "/sensor"
    }
}
