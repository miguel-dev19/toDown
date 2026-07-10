package com.todown.network.auth

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class JwtAuthenticator {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Host", "auth.todus.cu")
                .header("User-Agent", "ToDus 2.1.2 Auth")
                .header("Content-Type", "application/x-protobuf")
                .header("Connection", "close")
                .removeHeader("Accept-Encoding")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()
    
    suspend fun authenticate(phone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullPhone = if (phone.startsWith("53")) phone else "53$phone"
            val uuid = UUID.randomUUID().toString().replace("-", "")
            val body = buildAuthBody(fullPhone, uuid)
            
            Log.d("AUTH", "Phone: $fullPhone, UUID: $uuid")
            Log.d("AUTH", "Body hex: ${body.joinToString("") { "%02x".format(it) }}")
            
            val request = Request.Builder()
                .url("https://auth.todus.cu/v2/auth/token")
                .post(body.toRequestBody("application/x-protobuf".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d("AUTH", "Response code: ${response.code}")
            Log.d("AUTH", "Response: ${responseBody.take(300)}")
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }
            
            val jwtRegex = Regex("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
            val jwt = jwtRegex.find(responseBody)?.value 
                ?: throw Exception("JWT not found in response")
            
            Log.d("AUTH", "JWT: ${jwt.take(50)}...")
            Result.success(jwt)
            
        } catch (e: Exception) {
            Log.e("AUTH", "Error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun buildAuthBody(phone: String, secret: String): ByteArray {
        val secret32 = secret.take(32)
        val phoneBytes = phone.encodeToByteArray()
        val secretBytes = secret32.encodeToByteArray()
        
        // Formato exacto del bot:
        // bytes([0x0a, len(pb)]) + pb + bytes([0x12, len(sb)]) + sb
        return byteArrayOf(0x0a, phoneBytes.size.toByte()) + 
               phoneBytes +
               byteArrayOf(0x12, secretBytes.size.toByte()) + 
               secretBytes
    }
}
