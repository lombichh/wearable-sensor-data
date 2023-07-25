package com.example.wearablesensordata.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.wearablesensordata.R
import com.example.wearablesensordata.data.SensorData
import com.example.wearablesensordata.notifications.Notifications
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SensorForegroundService : Service(), CapabilityClient.OnCapabilityChangedListener,
    SensorEventListener {
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    // Capability vars
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper

    private var androidPhoneNodeWithApp: Node? = null

    // Sensor vars
    private lateinit var sensorManager: SensorManager

    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var lightSensor: Sensor? = null

    private var lastAccelerometerUpdateTimestamp: Long? = null
    private var lastGyroscopeUpdateTimestamp: Long? = null
    private var lastTemperatureUpdateTimestamp: Long? = null
    private var lastLightUpdateTimestamp: Long? = null

    override fun onCreate() {
        super.onCreate()

        initMobileAPIs()
        initSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create notification and start foreground service
        createNotificationChannel()

        val notification: Notification = NotificationCompat
            .Builder(this, Notifications.SENSOR_CHANNEL_ID)
            .setContentTitle("${getString(R.string.app_name)} Foreground Service")
            .setContentText("Listening to sensor changes")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(Notifications.SENSOR_NOTIFICATION_ID, notification)

        // Register capability listener and check if phone has app manually the first time.
        Wearable.getCapabilityClient(this)
            .addListener(this, CAPABILITY_PHONE_APP)
        serviceScope.launch {
            checkIfPhoneHasApp()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
     * When sensor values changes.
     */
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent != null) {
            // Check sensor type to correctly create byte array message
            // and send it to the phone.
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Check if last sensor update was sent more than INTERVAL second ago.
                    val actualTimestamp = sensorEvent.timestamp

                    if (lastAccelerometerUpdateTimestamp == null ||
                        actualTimestamp - lastAccelerometerUpdateTimestamp!!
                        >= SensorData.SENSOR_MESSAGE_MINIMUM_INTERVAL
                    ) {
                        val sensorMessage = SensorData.accelerometerValuesToSensorMessage(
                            sensorEvent.values[0],
                            sensorEvent.values[1],
                            sensorEvent.values[2]
                        )
                        sendSensorMessageToPhone(sensorMessage)

                        lastAccelerometerUpdateTimestamp = actualTimestamp
                    }
                }

                Sensor.TYPE_GYROSCOPE -> {
                    // Check if last sensor update was sent more than INTERVAL second ago.
                    val actualTime = sensorEvent.timestamp

                    if (lastGyroscopeUpdateTimestamp == null ||
                        actualTime - lastGyroscopeUpdateTimestamp!!
                        >= SensorData.SENSOR_MESSAGE_MINIMUM_INTERVAL
                    ) {
                        val sensorMessage = SensorData.gyroscopeValuesToSensorMessage(
                            sensorEvent.values[0],
                            sensorEvent.values[1],
                            sensorEvent.values[2]
                        )
                        sendSensorMessageToPhone(sensorMessage)

                        lastGyroscopeUpdateTimestamp = actualTime
                    }
                }

                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    // Check if last sensor update was sent more than INTERVAL second ago.
                    val actualTime = sensorEvent.timestamp

                    if (lastTemperatureUpdateTimestamp == null ||
                        actualTime - lastTemperatureUpdateTimestamp!!
                        >= SensorData.SENSOR_MESSAGE_MINIMUM_INTERVAL
                    ) {
                        val sensorMessage = SensorData.temperatureValueToSensorMessage(sensorEvent.values[0])
                        sendSensorMessageToPhone(sensorMessage)

                        lastTemperatureUpdateTimestamp = actualTime
                    }
                }

                Sensor.TYPE_LIGHT -> {
                    // Check if last sensor update was sent more than INTERVAL second ago.
                    val actualTime = sensorEvent.timestamp

                    if (lastLightUpdateTimestamp == null ||
                        actualTime - lastLightUpdateTimestamp!!
                        >= SensorData.SENSOR_MESSAGE_MINIMUM_INTERVAL
                    ) {
                        val sensorMessage = SensorData.lightValueToSensorMessage(sensorEvent.values[0])
                        sendSensorMessageToPhone(sensorMessage)

                        lastLightUpdateTimestamp = actualTime
                    }
                }
            }
        }
    }

    /*
     * When sensor accuracy changes.
     */
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Notifications.SENSOR_CHANNEL_ID,
            "Sensor Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun initMobileAPIs() {
        capabilityClient = Wearable.getCapabilityClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
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
            registerSensorListeners()
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    private fun registerSensorListeners() {
        accelerometerSensor?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        gyroscopeSensor?.also { gyroscope ->
            sensorManager.registerListener(
                this, gyroscope,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        temperatureSensor?.also { temperature ->
            sensorManager.registerListener(
                this,
                temperature,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        lightSensor?.also { light ->
            sensorManager.registerListener(
                this,
                light,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun sendSensorMessageToPhone(sensorMessage: ByteArray) {
        androidPhoneNodeWithApp?.id?.also { nodeId ->
            Wearable.getMessageClient(this).sendMessage(
                nodeId,
                SENSOR_MESSAGE_PATH,
                sensorMessage
            ).apply {
                addOnSuccessListener {
                    // Message sent successfully with no errors
                    Log.d("lombichh", "Message sent")
                }
                addOnFailureListener { exception ->
                    // Message failed to send
                    Log.d("lombichh", "Message failed: $exception")
                }
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
