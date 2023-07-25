package com.example.wearablesensordata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.example.wearablesensordata.databinding.ActivityMainBinding
import com.example.wearablesensordata.services.SensorForegroundService
import com.example.wearablesensordata.utils.ServiceUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MainWearActivity : FragmentActivity() {
    private lateinit var binding: ActivityMainBinding

    // Capability vars
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.infoTextview.text = getString(R.string.message_checking)

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

        private const val SENSOR_MESSAGE_PATH = "/sensor"
    }
}
