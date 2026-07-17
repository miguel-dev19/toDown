package com.todown.network.xmpp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import java.security.MessageDigest

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED
}

data class VideoData(
    val url: String, val fileName: String, val size: Long,
    val duration: Int, val thumbnailUrl: String, val title: String
)

enum class BotState {
    ANALYZING, DOWNLOADING, PROCESSING, UPLOADING, READY, ERROR
}

class XmppClient(private val context: Context? = null) {
    
    private val connection = ToDusConnection()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _videoMessages = MutableStateFlow<VideoData?>(null)
    val videoMessages: StateFlow<VideoData?> = _videoMessages
    
    private val _botState = MutableStateFlow<BotState?>(null)
    val botState: StateFlow<BotState?> = _botState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private var phone = ""
    private var jwt = ""
    private var running = false
    private var reconnectAttempts = 0
    private val botPhone = "5351417372"
    private val processedMessages = mutableSetOf<String>()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readerJob: Job? = null
    private var reconnectJob: Job? = null
    
    private fun md5(s: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    suspend fun connect(phone: String, jwt: String) {
        this.phone = phone
        this.jwt = jwt
        
        _connectionState.value = ConnectionState.CONNECTING
        
        val resource = md5(phone) + "_Android"
        
        connection.handshake(phone, jwt, resource)
            .onSuccess { jid ->
                Log.d("XMPP", "Conectado: $jid")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                running = true
                startMessageReader()
                startReconnectWatcher()
            }
            .onFailure { e ->
                Log.e("XMPP", "Error: ${e.message}")
                _connectionState.value = ConnectionState.FAILED
            }
    }
    
    private fun startMessageReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = StringBuilder()
            while (running) {
                val data = connection.readRaw()
                if (!data.isNullOrBlank()) {
                    buffer.append(data)
                    val text = buffer.toString()
                    val messages = Regex("<m\\b.*?</m>", RegexOption.DOT_MATCHES_ALL).findAll(text).toList()
                    if (messages.isNotEmpty()) {
                        buffer.clear()
                        messages.forEach { handleMessage(it.value) }
                    }
                }
                delay(100)
            }
        }
    }
    
    private fun startReconnectWatcher() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (running) {
                delay(3000)
                try {
                    connection.sendRaw(" ")
                } catch (_: Exception) {
                    if (running && phone.isNotEmpty()) attemptReconnect()
                }
            }
        }
    }
    
    private suspend fun attemptReconnect() {
        reconnectAttempts++
        if (reconnectAttempts > 10) {
            _connectionState.value = ConnectionState.FAILED
            return
        }
        
        _connectionState.value = ConnectionState.RECONNECTING
        
        val delay = 2000L * (1L shl minOf(reconnectAttempts - 1, 5))
        delay(delay)
        
        try {
            connection.close()
            val resource = md5(phone) + "_Android"
            connection.handshake(phone, jwt, resource)
                .onSuccess {
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0
                    running = true
                    startMessageReader()
                }
                .onFailure {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
        } catch (_: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    private fun handleMessage(xml: String) {
        if (xml.contains("<rd ") || xml.contains("<dd ")) return
        
        val msgId = Regex("i='([^']+)'").find(xml)?.groupValues?.get(1) ?: return
        if (msgId in processedMessages) return
        processedMessages.add(msgId)
        if (processedMessages.size > 500) processedMessages.clear()
        
        val from = Regex("f='([^']+)'").find(xml)?.groupValues?.get(1)
        val sender = from?.split("@")?.first()
        if (sender != botPhone) return
        
        val body = Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1) ?: return
        
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
                connection.sendRaw(xml)
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
        val tag = Regex("""<video\s+xmlns="video:n"[^>]*>""").find(xml)?.value ?: return null
        val url = attr(tag, "url")?.replace("&amp;", "&") ?: return null
        return VideoData(url, attr(tag, "n") ?: "video.mp4",
            attr(tag, "s")?.toLongOrNull() ?: 0L, attr(tag, "d")?.toIntOrNull() ?: 0,
            attr(tag, "tnail") ?: "", body.trim())
    }
    
    private fun attr(xml: String, name: String) = Regex("""$name="([^"]*)"""").find(xml)?.groupValues?.get(1)
    
    fun disconnect() {
        running = false
        readerJob?.cancel()
        reconnectJob?.cancel()
        connection.close()
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun clearVideoData() {
        _videoMessages.value = null
        _botState.value = null
        _errorMessage.value = null
    }
}
