package com.example.wearablesensordata.data

import com.example.wearablesensordata.utils.DataConverter

class SensorData {

    companion object {
        const val SENSOR_MESSAGE_MINIMUM_INTERVAL = 100000000 // 0.1 sec

        const val ACCELEROMETER: Byte = 0
        const val GYROSCOPE: Byte = 1
        const val MAGNETOMETER: Byte = 2
        const val HEART_RATE: Byte = 3
        const val LIGHT: Byte = 4
        const val TEMPERATURE: Byte = 5
        const val HUMIDITY: Byte = 6
        const val PROXIMITY: Byte = 7
        const val PRESSURE: Byte = 8

        // Sensor values (Float) to sensor message (ByteArray)
        fun accelerometerValuesToSensorMessage(
            accelerometerX: Float,
            accelerometerY: Float,
            accelerometerZ: Float
        ): ByteArray {
            val accelerometerXByteArray = DataConverter.floatToByteArray(accelerometerX)
            val accelerometerYByteArray = DataConverter.floatToByteArray(accelerometerY)
            val accelerometerZByteArray = DataConverter.floatToByteArray(accelerometerZ)

            return byteArrayOf(
                ACCELEROMETER,
                accelerometerXByteArray[0],
                accelerometerXByteArray[1],
                accelerometerXByteArray[2],
                accelerometerXByteArray[3],
                accelerometerYByteArray[0],
                accelerometerYByteArray[1],
                accelerometerYByteArray[2],
                accelerometerYByteArray[3],
                accelerometerZByteArray[0],
                accelerometerZByteArray[1],
                accelerometerZByteArray[2],
                accelerometerZByteArray[3]
            )
        }

        fun gyroscopeValuesToSensorMessage(
            gyroscopeX: Float,
            gyroscopeY: Float,
            gyroscopeZ: Float
        ): ByteArray {
            val gyroscopeXByteArray = DataConverter.floatToByteArray(gyroscopeX)
            val gyroscopeYByteArray = DataConverter.floatToByteArray(gyroscopeY)
            val gyroscopeZByteArray = DataConverter.floatToByteArray(gyroscopeZ)

            return byteArrayOf(
                GYROSCOPE,
                gyroscopeXByteArray[0],
                gyroscopeXByteArray[1],
                gyroscopeXByteArray[2],
                gyroscopeXByteArray[3],
                gyroscopeYByteArray[0],
                gyroscopeYByteArray[1],
                gyroscopeYByteArray[2],
                gyroscopeYByteArray[3],
                gyroscopeZByteArray[0],
                gyroscopeZByteArray[1],
                gyroscopeZByteArray[2],
                gyroscopeZByteArray[3]
            )
        }

        fun magnetometerValuesToSensorMessage(
            magnetometerX: Float,
            magnetometerY: Float,
            magnetometerZ: Float
        ): ByteArray {
            val magnetometerXByteArray = DataConverter.floatToByteArray(magnetometerX)
            val magnetometerYByteArray = DataConverter.floatToByteArray(magnetometerY)
            val magnetometerZByteArray = DataConverter.floatToByteArray(magnetometerZ)

            return byteArrayOf(
                MAGNETOMETER,
                magnetometerXByteArray[0],
                magnetometerXByteArray[1],
                magnetometerXByteArray[2],
                magnetometerXByteArray[3],
                magnetometerYByteArray[0],
                magnetometerYByteArray[1],
                magnetometerYByteArray[2],
                magnetometerYByteArray[3],
                magnetometerZByteArray[0],
                magnetometerZByteArray[1],
                magnetometerZByteArray[2],
                magnetometerZByteArray[3]
            )
        }

        fun heartRateValueToSensorMessage(heartRate: Float): ByteArray {
            val heartRateByteArray = DataConverter.floatToByteArray(heartRate)

            return byteArrayOf(
                HEART_RATE,
                heartRateByteArray[0],
                heartRateByteArray[1],
                heartRateByteArray[2],
                heartRateByteArray[3]
            )
        }

        fun lightValueToSensorMessage(light: Float): ByteArray {
            val lightByteArray = DataConverter.floatToByteArray(light)

            return byteArrayOf(
                LIGHT,
                lightByteArray[0],
                lightByteArray[1],
                lightByteArray[2],
                lightByteArray[3]
            )
        }

        fun temperatureValueToSensorMessage(temperature: Float): ByteArray {
            val temperatureByteArray = DataConverter.floatToByteArray(temperature)

            return byteArrayOf(
                TEMPERATURE,
                temperatureByteArray[0],
                temperatureByteArray[1],
                temperatureByteArray[2],
                temperatureByteArray[3]
            )
        }

        fun humidityValueToSensorMessage(humidity: Float): ByteArray {
            val humidityByteArray = DataConverter.floatToByteArray(humidity)

            return byteArrayOf(
                HUMIDITY,
                humidityByteArray[0],
                humidityByteArray[1],
                humidityByteArray[2],
                humidityByteArray[3]
            )
        }

        fun proximityValueToSensorMessage(proximity: Float): ByteArray {
            val proximityByteArray = DataConverter.floatToByteArray(proximity)

            return byteArrayOf(
                PROXIMITY,
                proximityByteArray[0],
                proximityByteArray[1],
                proximityByteArray[2],
                proximityByteArray[3]
            )
        }

        fun pressureValueToSensorMessage(pressure: Float): ByteArray {
            val pressureByteArray = DataConverter.floatToByteArray(pressure)

            return byteArrayOf(
                PRESSURE,
                pressureByteArray[0],
                pressureByteArray[1],
                pressureByteArray[2],
                pressureByteArray[3]
            )
        }

        // Sensor message (ByteArray) to sensor values (Float)
        fun sensorMessageToAccelerometerValues(sensorMessage: ByteArray): FloatArray {
            val accelerometerX =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))
            val accelerometerY =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(5, 9))
            val accelerometerZ =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(9, 13))

            return floatArrayOf(accelerometerX, accelerometerY, accelerometerZ)
        }

        fun sensorMessageToGyroscopeValues(sensorMessage: ByteArray): FloatArray {
            val gyroscopeX =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))
            val gyroscopeY =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(5, 9))
            val gyroscopeZ =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(9, 13))

            return floatArrayOf(gyroscopeX, gyroscopeY, gyroscopeZ)
        }

        fun sensorMessageToMagnetometerValues(sensorMessage: ByteArray): FloatArray {
            val magnetometerX =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))
            val magnetometerY =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(5, 9))
            val magnetometerZ =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(9, 13))

            return floatArrayOf(magnetometerX, magnetometerY, magnetometerZ)
        }

        fun sensorMessageToHeartRateValue(sensorMessage: ByteArray): Float {
            val heartRateValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return heartRateValue
        }

        fun sensorMessageToLightValue(sensorMessage: ByteArray): Float {
            val lightValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return lightValue
        }

        fun sensorMessageToTemperatureValue(sensorMessage: ByteArray): Float {
            val temperatureValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return temperatureValue
        }

        fun sensorMessageToHumidityValue(sensorMessage: ByteArray): Float {
            val humidityValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return humidityValue
        }

        fun sensorMessageToProximityValue(sensorMessage: ByteArray): Float {
            val proximityValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return proximityValue
        }

        fun sensorMessageToPressureValue(sensorMessage: ByteArray): Float {
            val pressureValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return pressureValue
        }
    }

}
