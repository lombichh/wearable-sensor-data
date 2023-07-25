package com.example.wearablesensordata.data

import com.example.wearablesensordata.utils.DataConverter

class SensorData {

    companion object {
        const val SENSOR_MESSAGE_MINIMUM_INTERVAL = 50000000

        const val LOCATION: Byte = 0
        const val ACCELEROMETER: Byte = 1
        const val GYROSCOPE: Byte = 2
        const val TEMPERATURE: Byte = 3
        const val LIGHT: Byte = 4

        // Sensor values (Float) to sensor message (ByteArray)
        fun locationValuesToSensorMessage(longitude: Float, latitude: Float): ByteArray {
            val longitudeByteArray = DataConverter.floatToByteArray(longitude)
            val latitudeByteArray = DataConverter.floatToByteArray(latitude)

            return byteArrayOf(
                LOCATION,
                longitudeByteArray[0],
                longitudeByteArray[1],
                longitudeByteArray[2],
                longitudeByteArray[3],
                latitudeByteArray[0],
                latitudeByteArray[1],
                latitudeByteArray[2],
                latitudeByteArray[3]
            )
        }

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

        // Sensor message (ByteArray) to sensor values (Float)
        fun sensorMessageToLocationValues(sensorMessage: ByteArray): FloatArray {
            val longitudeValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))
            val latitudeValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(5, 9))

            return floatArrayOf(longitudeValue, latitudeValue)
        }

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

        fun sensorMessageToTemperatureValue(sensorMessage: ByteArray): Float {
            val temperatureValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return temperatureValue
        }

        fun sensorMessageToLightValues(sensorMessage: ByteArray): Float {
            val lightValue =
                DataConverter.byteArrayToFloat(sensorMessage.copyOfRange(1, 5))

            return lightValue
        }
    }

}
