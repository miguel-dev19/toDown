package com.todown.network.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class JwtAuthenticator {
    
    private val client = OkHttpClient()
    
    suspend fun authenticate(phone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uuid = UUID.randomUUID().toString().replace("-", "")
            val body = buildAuthBody(phone, uuid)
            
            val request = Request.Builder()
                .url("https://auth.todus.cu/v2/auth/token")
                .post(body.toRequestBody("application/x-protobuf".toMediaType()))
                .addHeader("User-Agent", "ToDus 2.1.2 Auth")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            val jwtRegex = Regex("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
            val jwt = jwtRegex.find(responseBody)?.value 
                ?: throw Exception("JWT not found")
            
            Result.success(jwt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildAuthBody(phone: String, secret: String): ByteArray {
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
