package com.todown.network.xmpp

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.*
import java.net.Socket
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

sealed class XMPPConnectionState {
    object Connecting : XMPPConnectionState()
    object Connected : XMPPConnectionState()
    object Disconnected : XMPPConnectionState()
    data class Error(val message: String) : XMPPConnectionState()
}

data class VideoData(
    val url: String,
    val fileName: String,
    val size: Long,
    val duration: Int,
    val thumbnailUrl: String,
    val title: String
)

enum class BotState {
    ANALYZING, DOWNLOADING, PROCESSING, UPLOADING, READY, ERROR
}

class XMPPManager {
    
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private val _connectionState = MutableStateFlow<XMPPConnectionState>(XMPPConnectionState.Disconnected)
    val connectionState: StateFlow<XMPPConnectionState> = _connectionState
    
    private val _videoMessages = MutableStateFlow<VideoData?>(null)
    val videoMessages: StateFlow<VideoData?> = _videoMessages
    
    private val _botState = MutableStateFlow<BotState?>(null)
    val botState: StateFlow<BotState?> = _botState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private var keepAliveJob: Job? = null
    private var listenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processedMessages = mutableSetOf<String>()
    private var currentPhone: String = ""
    private val botPhone = "5350155246"
    
    fun connect(phone: String, jwt: String) {
        currentPhone = phone
        scope.launch {
            try {
                _connectionState.value = XMPPConnectionState.Connecting
                
                // Conectar igual que el bot
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, SecureRandom())
                val factory = sslContext.socketFactory
                
                socket = factory.createSocket("ws.todus.cu", 5222)
                writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                
                // Stream inicial
                sendRaw("<?xml version='1.0'?><stream:stream to='im.todus.cu' xmlns='jc' xmlns:stream='x1' version='1.0'>")
                val resp1 = readResponse()
                Log.d("XMPP", "Stream open: ${resp1.take(100)}")
                
                // SASL PLAIN
                val authString = "\u0000$phone\u0000$jwt"
                val authB64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
                sendRaw("<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>$authB64</auth>")
                val resp2 = readResponse()
                Log.d("XMPP", "SASL: ${resp2.take(100)}")
                
                // Nuevo stream
                sendRaw("<?xml version='1.0'?><stream:stream to='im.todus.cu' xmlns='jc' xmlns:stream='x1' version='1.0'>")
                val resp3 = readResponse()
                Log.d("XMPP", "Stream 2: ${resp3.take(100)}")
                
                // Bind
                val bindId = UUID.randomUUID().toString().replace("-", "").take(8)
                sendRaw("<iq type='set' id='$bindId'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>toDown</resource></bind></iq>")
                val resp4 = readResponse()
                Log.d("XMPP", "Bind: ${resp4.take(100)}")
                
                // Session
                val sessionId = UUID.randomUUID().toString().replace("-", "").take(8)
                sendRaw("<iq type='set' id='$sessionId'><session xmlns='urn:ietf:params:xml:ns:xmpp-session'/></iq>")
                val resp5 = readResponse()
                Log.d("XMPP", "Session: ${resp5.take(100)}")
                
                // Presence
                sendRaw("<presence/>")
                
                _connectionState.value = XMPPConnectionState.Connected
                Log.d("XMPP", "Conectado!")
                
                // Iniciar listener y keepalive
                startListening()
                startKeepAlive()
                
            } catch (e: Exception) {
                Log.e("XMPP", "Error conexion: ${e.message}", e)
                _connectionState.value = XMPPConnectionState.Error(e.message ?: "Error")
            }
        }
    }
    
    private fun sendRaw(xml: String) {
        try {
            writer?.write(xml)
            writer?.flush()
            Log.d("XMPP", "SEND: ${xml.take(100)}")
        } catch (e: Exception) {
            Log.e("XMPP", "Error send: ${e.message}")
        }
    }
    
    private fun readResponse(): String {
        val sb = StringBuilder()
        try {
            socket?.soTimeout = 5000
            val buf = CharArray(4096)
            var totalRead = 0
            while (totalRead < 50000) {
                val read = reader?.read(buf) ?: -1
                if (read == -1) break
                sb.append(buf, 0, read)
                totalRead += read
                val str = sb.toString()
                if (str.contains("</stream:stream>") || str.contains("<ok") || 
                    str.contains("<success") || str.contains("</iq>")) break
            }
        } catch (e: Exception) {
            Log.d("XMPP", "Read done: ${e.message}")
        }
        return sb.toString()
    }
    
    private fun startListening() {
        listenJob?.cancel()
        listenJob = scope.launch {
            val buffer = StringBuilder()
            while (isActive && socket?.isConnected == true) {
                try {
                    socket?.soTimeout = 1000
                    val buf = CharArray(8192)
                    val read = reader?.read(buf) ?: -1
                    if (read == -1) continue
                    buffer.append(buf, 0, read)
                    
                    val text = buffer.toString()
                    
                    // Interceptar mensajes
                    val messages = Regex("<m\\b.*?</m>", RegexOption.DOT_MATCHES_ALL).findAll(text).toList()
                    if (messages.isNotEmpty()) {
                        buffer.clear()
                    }
                    
                    for (m in messages) {
                        handleMessage(m.value)
                    }
                    
                    if (buffer.length > 10000) buffer.clear()
                    
                } catch (e: Exception) {
                    if (e !is java.net.SocketTimeoutException) {
                        Log.e("XMPP", "Listen error: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun handleMessage(xml: String) {
        // Ignorar ACKs
        if (xml.contains("<rd ") || xml.contains("<dd ")) return
        
        val msgId = Regex("i='([^']+)'").find(xml)?.groupValues?.get(1) ?: return
        if (msgId in processedMessages) return
        processedMessages.add(msgId)
        if (processedMessages.size > 500) processedMessages.clear()
        
        val from = Regex("f='([^']+)'").find(xml)?.groupValues?.get(1)
        val sender = from?.split("@")?.first()
        if (sender != botPhone) return
        
        val body = Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1) ?: return
        
        Log.d("XMPP", "Bot msg: $body")
        
        when {
            body.contains("Analizando") -> { _botState.value = BotState.ANALYZING; _errorMessage.value = null }
            body.contains("Descargando") -> _botState.value = BotState.DOWNLOADING
            body.contains("Procesando") -> _botState.value = BotState.PROCESSING
            body.contains("Subiendo") -> _botState.value = BotState.UPLOADING
            body.contains("Listo") -> _botState.value = BotState.READY
            body.startsWith("\u274C") -> { _botState.value = BotState.ERROR; _errorMessage.value = body.removePrefix("\u274C").trim() }
        }
        
        if (xml.contains("<video")) {
            parseVideo(xml, body)?.let {
                _videoMessages.value = it
                _botState.value = BotState.READY
            }
        }
    }
    
    fun sendUrlToBot(url: String) {
        scope.launch {
            try {
                val msgId = UUID.randomUUID().toString().replace("-", "").take(16)
                val xml = "<m to=\"${botPhone}@im.todus.cu\" t=\"c\" i=\"$msgId\" xmlns=\"jc\"><k xmlns=\"x8\"/><b>$url</b></m>"
                sendRaw(xml)
                
                _videoMessages.value = null
                _errorMessage.value = null
                _botState.value = BotState.ANALYZING
            } catch (e: Exception) {
                _botState.value = BotState.ERROR
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }
    
    private fun parseVideo(xml: String, body: String): VideoData? {
        val videoTag = Regex("""<video\s+xmlns="video:n"[^>]*>""").find(xml)?.value ?: return null
        val url = extract(videoTag, "url")?.replace("&amp;", "&") ?: return null
        val fileName = extract(videoTag, "n") ?: "video.mp4"
        val size = extract(videoTag, "s")?.toLongOrNull() ?: 0L
        val duration = extract(videoTag, "d")?.toIntOrNull() ?: 0
        val thumbnailUrl = extract(videoTag, "tnail") ?: ""
        return VideoData(url, fileName, size, duration, thumbnailUrl, body.trim())
    }
    
    private fun extract(xml: String, attr: String): String? {
        return Regex("""$attr="([^"]*)"""").find(xml)?.groupValues?.get(1)
    }
    
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && socket?.isConnected == true) {
                try { sendRaw(" ") } catch (_: Exception) { 
                    _connectionState.value = XMPPConnectionState.Disconnected 
                }
                delay(30000)
            }
        }
    }
    
    fun disconnect() {
        scope.launch {
            keepAliveJob?.cancel()
            listenJob?.cancel()
            try { sendRaw("</stream:stream>") } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            _connectionState.value = XMPPConnectionState.Disconnected
        }
    }
    
    fun clearVideoData() {
        _videoMessages.value = null
        _botState.value = null
        _errorMessage.value = null
    }
}
