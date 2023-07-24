package com.example.wearablesensordata.data

class SensorData(location: FloatArray?, accelerometer: FloatArray?, gyroscope: FloatArray?,
                 temperature: Float?, light: Float?) {

    var location: FloatArray? = location
    var accelerometer: FloatArray? = accelerometer
    var gyroscope: FloatArray? = gyroscope
    var temperature: Float? = temperature
    var light: Float? = light

    companion object {
        /*
         * Create byteArray from SensorData object
         */
        fun toByteArray(sensorData: SensorData): ByteArray {
            return byteArrayOf(
                sensorData.location?.get(0)?.toInt()?.toByte() ?: 0,
                sensorData.location?.get(1)?.toInt()?.toByte() ?: 0,
                if (sensorData.location?.get(0) == null) 0 else 1,

                sensorData.accelerometer?.get(0)?.toInt()?.toByte() ?: 0,
                sensorData.accelerometer?.get(1)?.toInt()?.toByte() ?: 0,
                sensorData.accelerometer?.get(2)?.toInt()?.toByte() ?: 0,
                if (sensorData.accelerometer?.get(0) == null) 0 else 1,

                sensorData.gyroscope?.get(0)?.toInt()?.toByte() ?: 0,
                sensorData.gyroscope?.get(1)?.toInt()?.toByte() ?: 0,
                sensorData.gyroscope?.get(2)?.toInt()?.toByte() ?: 0,
                if (sensorData.gyroscope?.get(0) == null) 0 else 1,

                sensorData.temperature?.toInt()?.toByte() ?: 0,
                if (sensorData.temperature == null) 0 else 1,

                sensorData.light?.toInt()?.toByte() ?: 0,
                if (sensorData.light == null) 0 else 1
            )
        }

        /*
         * Create SensorData object from byteArray values
         */
        fun fromByteArray(byteArray: ByteArray): SensorData {

            var location: FloatArray? = null

            if (byteArray.get(2) == 1.toByte()) {
                location = FloatArray(2)
                location[0] = byteArray.get(0).toFloat()
                location[1] = byteArray.get(1).toFloat()
            }

            var accelerometer: FloatArray? = null

            if (byteArray.get(6) == 1.toByte()) {
                accelerometer = FloatArray(3)
                accelerometer[0] = byteArray.get(3).toFloat()
                accelerometer[1] = byteArray.get(4).toFloat()
                accelerometer[2] = byteArray.get(5).toFloat()
            }

            var gyroscope: FloatArray? = null

            if (byteArray.get(10) == 1.toByte()) {
                gyroscope = FloatArray(3)
                gyroscope[0] = byteArray.get(7).toFloat()
                gyroscope[1] = byteArray.get(8).toFloat()
                gyroscope[2] = byteArray.get(9).toFloat()
            }

            var temperature: Float? = null

            if (byteArray.get(12) == 1.toByte())
                temperature = byteArray.get(11).toFloat()

            var light: Float? = null

            if (byteArray.get(14) == 1.toByte())
                light = byteArray.get(13).toFloat()

            return SensorData(location, accelerometer, gyroscope, temperature, light)
        }
    }

}
