package com.example.wearablesensordata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.example.wearablesensordata.databinding.ActivityMainBinding
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

class MainWearActivity : FragmentActivity(), CapabilityClient.OnCapabilityChangedListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var capabilityClient: CapabilityClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var androidPhoneNodeWithApp: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMobileAPIs()

        binding.mainTextview.text = getString(R.string.message_checking)
    }

    override fun onPause() {
        super.onPause()

        Wearable.getCapabilityClient(this).removeListener(this, CAPABILITY_PHONE_APP)
    }

    override fun onResume() {
        super.onResume()

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
        updateUi()
    }

    private fun initMobileAPIs() {
        capabilityClient = Wearable.getCapabilityClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)
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
                updateUi()
            }
        } catch (cancellationException: CancellationException) {
            // Request was cancelled normally
        } catch (throwable: Throwable) {
            // Capability request failed to return any results
        }
    }

    private fun updateUi() {
        val androidPhoneNodeWithApp = androidPhoneNodeWithApp

        if (androidPhoneNodeWithApp != null) {
            // App is installed on remote node
            // TODO: Add your code to communicate with the phone app via
            //       Wear APIs (MessageClient, DataClient, etc.)
            binding.mainTextview.text =
                getString(R.string.message_installed, androidPhoneNodeWithApp.displayName)
        } else {
            // App is missing on remote node
            binding.mainTextview.text = getString(R.string.message_missing)
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
    }
}