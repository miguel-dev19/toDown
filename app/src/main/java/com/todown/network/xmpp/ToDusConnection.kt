package com.todown.network.xmpp

import android.util.Base64
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ToDusConnection {
    
    companion object {
        const val HOST = "ws.todus.cu"
        const val PORT = 1756
        const val CONNECT_TIMEOUT = 10000
        const val READ_TIMEOUT = 30000
    }
    
    private var socket: SSLSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    var jid: String = ""
    
    suspend fun handshake(phone: String, jwt: String, resource: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Crear socket TLS sin verificar certificado
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAll, SecureRandom())
            
            socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket?.connect(InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT)
            socket?.soTimeout = READ_TIMEOUT
            
            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(socket!!.outputStream))
            
            // 2. Stream inicial
            sendRaw("<?xml version='1.0'?><stream:stream to='im.todus.cu' xmlns='jc' xmlns:stream='x1' version='1.0'>")
            readUntil { it.contains("stream:features") }
            
            // 3. Auth SASL PLAIN
            val authString = "\u0000$phone\u0000$jwt"
            val authB64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
            sendRaw("<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>$authB64</auth>")
            readUntil { it.contains("<ok") || it.contains("<success") }
            
            // 4. Reiniciar stream
            sendRaw("<?xml version='1.0'?><stream:stream to='im.todus.cu' xmlns='jc' xmlns:stream='x1' version='1.0'>")
            readUntil { it.contains("stream:features") }
            
            // 5. Bind resource
            val bindId = UUID.randomUUID().toString().replace("-", "").take(8)
            sendRaw("<iq type='set' id='$bindId'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>$resource</resource></bind></iq>")
            val bindResp = readUntil { it.contains("jid") }
            jid = Regex("jid='([^']+)'").find(bindResp)?.groupValues?.get(1) ?: "$phone@im.todus.cu/$resource"
            
            // 6. Session
            val sessionId = UUID.randomUUID().toString().replace("-", "").take(8)
            sendRaw("<iq type='set' id='$sessionId'><session xmlns='urn:ietf:params:xml:ns:xmpp-session'/></iq>")
            readUntil { it.contains("result") || it.contains("error") }
            
            // 7. Presence
            sendRaw("<presence/>")
            
            Result.success(jid)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun sendRaw(xml: String) {
        try {
            writer?.write(xml)
            writer?.flush()
        } catch (e: Exception) {
            throw e
        }
    }
    
    fun readRaw(): String? {
        return try {
            socket?.soTimeout = 1000
            val sb = StringBuilder()
            val buf = CharArray(8192)
            val read = reader?.read(buf) ?: -1
            if (read > 0) sb.append(buf, 0, read)
            sb.toString().ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun readUntil(predicate: (String) -> Boolean): String {
        val sb = StringBuilder()
        val buf = CharArray(4096)
        socket?.soTimeout = 5000
        var totalRead = 0
        while (totalRead < 50000) {
            val read = reader?.read(buf) ?: break
            if (read == -1) break
            sb.append(buf, 0, read)
            totalRead += read
            val str = sb.toString()
            if (predicate(str)) break
        }
        return sb.toString()
    }
    
    fun close() {
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
    }
}
