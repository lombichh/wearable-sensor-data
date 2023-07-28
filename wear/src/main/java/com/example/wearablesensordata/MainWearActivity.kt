package com.example.wearablesensordata

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.wearablesensordata.databinding.ActivityMainBinding
import com.example.wearablesensordata.services.SensorForegroundService
import com.example.wearablesensordata.utils.ServiceUtils
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainWearActivity : FragmentActivity(), CapabilityClient.OnCapabilityChangedListener {
    private lateinit var binding: ActivityMainBinding

    // Capability vars
    private lateinit var capabilityClient: CapabilityClient
    private var androidPhoneNodeWithApp: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        capabilityClient = Wearable.getCapabilityClient(this)

        // Start sensor service if it is not running.
        if (!ServiceUtils.isServiceRunning(this, SensorForegroundService::class.java)) {
            startService(Intent(this, SensorForegroundService::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        // Register capability listener and check if phone has app manually the first time.
        Wearable.getCapabilityClient(this)
            .addListener(this, CAPABILITY_PHONE_APP)
        lifecycleScope.launch {
            checkIfPhoneHasApp()
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        // There should only ever be one phone in a node set (much less w/ the correct
        // capability), so I am just grabbing the first one (which should be the only one).
        androidPhoneNodeWithApp = capabilityInfo.nodes.firstOrNull()

        checkPairingState()
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
        // Register/unregister sensors based on whether the devices are paired.
        val androidPhoneNodeWithApp = androidPhoneNodeWithApp

        if (androidPhoneNodeWithApp != null) {
            binding.checkingPairedPhoneProgressbar.visibility = View.GONE
            binding.phoneConnectedImageview.visibility = View.VISIBLE

            binding.pairingStateTextview.text = getString(
                R.string.phone_connected,
                androidPhoneNodeWithApp.displayName
            )
            binding.pairingStateTextview.setTextColor(getColor(R.color.light_green))
        } else {
            binding.phoneConnectedImageview.visibility = View.GONE
            binding.checkingPairedPhoneProgressbar.visibility = View.VISIBLE

            binding.pairingStateTextview.text = getString(R.string.looking_for_paired_phone)
            binding.pairingStateTextview.setTextColor(getColor(android.R.color.white))
        }
    }

    companion object {
        // Name of capability listed in Phone app's wear.xml.
        private const val CAPABILITY_PHONE_APP = "verify_remote_example_phone_app"
    }
}
