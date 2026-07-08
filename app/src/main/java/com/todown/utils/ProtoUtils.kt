package com.todown.utils

object ProtoUtils {
    
    fun buildAuthBody(phone: String, secret: String): ByteArray {
        val secret32 = secret.take(32)
        val phoneBytes = phone.encodeToByteArray()
        val secretBytes = secret32.encodeToByteArray()
        
        return byteArrayOf(0x0a) +
                byteArrayOf(phoneBytes.size.toByte()) +
                phoneBytes +
                byteArrayOf(0x12) +
                byteArrayOf(secretBytes.size.toByte()) +
                secretBytes
    }
}
