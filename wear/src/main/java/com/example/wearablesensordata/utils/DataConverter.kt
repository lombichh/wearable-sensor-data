package com.example.wearablesensordata.utils

import java.nio.ByteBuffer

class DataConverter {

    companion object {
        fun floatToByteArray(value: Float): ByteArray {
            val buffer = ByteBuffer.allocate(4)
            buffer.putFloat(value)
            return buffer.array()
        }

        fun byteArrayToFloat(byteArray: ByteArray): Float {
            val buffer = ByteBuffer.wrap(byteArray)
            return buffer.float
        }
    }

}
