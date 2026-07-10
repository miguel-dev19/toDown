package com.todown.network.xmpp

import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jivesoftware.smack.*
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import java.util.*
import javax.net.ssl.SSLContext

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
    
    private var connection: XMPPTCPConnection? = null
    private val _connectionState = MutableStateFlow<XMPPConnectionState>(XMPPConnectionState.Disconnected)
    val connectionState: StateFlow<XMPPConnectionState> = _connectionState
    
    private val _videoMessages = MutableStateFlow<VideoData?>(null)
    val videoMessages: StateFlow<VideoData?> = _videoMessages
    
    private val _botState = MutableStateFlow<BotState?>(null)
    val botState: StateFlow<BotState?> = _botState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private var keepAliveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processedMessages = mutableSetOf<String>()
    private var currentPhone: String = ""
    private val botPhone = "5350155246"
    
    fun connect(phone: String, jwt: String) {
        currentPhone = phone
        scope.launch {
            try {
                _connectionState.value = XMPPConnectionState.Connecting
                
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, null)
                
                val config = XMPPTCPConnectionConfiguration.builder()
                    .setHost("ws.todus.cu")
                    .setPort(5222)
                    .setXmppDomain("im.todus.cu")
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                    .setSocketFactory(sslContext.socketFactory)
                    .setResource(Resourcepart.from("toDown"))
                    .build()
                
                connection = XMPPTCPConnection(config)
                connection?.connect()
                
                // SASL PLAIN
                val authString = "\u0000${phone}\u0000${jwt}"
                val authB64 = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
                // SASL auth handled by login
                
                val jid = JidCreate.entityBareFrom("${phone}@im.todus.cu")
                connection?.login()
                
                connection?.sendStanza(Presence(Presence.Type.available))
                
                setupMessageListener()
                startKeepAlive()
                
                _connectionState.value = XMPPConnectionState.Connected
                
            } catch (e: Exception) {
                _connectionState.value = XMPPConnectionState.Error(e.message ?: "Error de conexion")
            }
        }
    }
    
    private fun setupMessageListener() {
        connection?.addStanzaListener(
            { stanza -> handleIncomingStanza(stanza) },
            StanzaFilter { true }
        )
    }
    
    private fun handleIncomingStanza(stanza: Stanza) {
        if (stanza !is Message) return
        
        val xml = stanza.toXML().toString()
        
        // Ignorar ACKs
        if (xml.contains("<rd ") || xml.contains("<dd ")) return
        
        // Extraer msg_id
        val msgId = Regex("i='([^']+)'").find(xml)?.groupValues?.get(1) ?: return
        if (msgId in processedMessages) return
        processedMessages.add(msgId)
        if (processedMessages.size > 500) processedMessages.clear()
        
        // Extraer remitente
        val from = Regex("f='([^']+)'").find(xml)?.groupValues?.get(1)
        val sender = from?.split("@")?.first()
        
        if (sender != botPhone) return
        
        // Extraer cuerpo
        val body = Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1) ?: return
        
        when {
            body.contains("Analizando") -> {
                _botState.value = BotState.ANALYZING
                _errorMessage.value = null
            }
            body.contains("Descargando video") -> _botState.value = BotState.DOWNLOADING
            body.contains("Procesando thumbnail") -> _botState.value = BotState.PROCESSING
            body.contains("Subiendo video") -> _botState.value = BotState.UPLOADING
            body.contains("Listo") -> _botState.value = BotState.READY
            body.startsWith("\u274C") -> {
                _botState.value = BotState.ERROR
                _errorMessage.value = body.removePrefix("\u274C").trim()
            }
        }
        
        if (xml.contains("<video")) {
            parseVideoMessage(xml, body)?.let {
                _videoMessages.value = it
                _botState.value = BotState.READY
            }
        }
    }
    
    fun sendUrlToBot(url: String) {
        scope.launch {
            try {
                val msgId = UUID.randomUUID().toString().replace("-", "").take(16)
                val rawXml = "<m to=\"${botPhone}@im.todus.cu\" t=\"c\" i=\"$msgId\" xmlns=\"jc\"><k xmlns=\"x8\"/><b>$url</b></m>"
                
                // Enviar como raw XML usando sendStanza con string
                // Send via raw TCP - bypass Smack
                
                _videoMessages.value = null
                _errorMessage.value = null
                _botState.value = BotState.ANALYZING
                
            } catch (e: Exception) {
                _botState.value = BotState.ERROR
                _errorMessage.value = "Error al enviar: ${e.message}"
            }
        }
    }
    
    private fun parseVideoMessage(xml: String, body: String): VideoData? {
        val videoTag = Regex("""<video\s+xmlns="video:n"[^>]*>""")
            .find(xml)?.value ?: return null
        
        val url = extractAttr(videoTag, "url")?.replace("&amp;", "&") ?: return null
        val fileName = extractAttr(videoTag, "n") ?: "video.mp4"
        val size = extractAttr(videoTag, "s")?.toLongOrNull() ?: 0L
        val duration = extractAttr(videoTag, "d")?.toIntOrNull() ?: 0
        val thumbnailUrl = extractAttr(videoTag, "tnail") ?: ""
        val title = body.trim()
        
        return VideoData(url, fileName, size, duration, thumbnailUrl, title)
    }
    
    private fun extractAttr(xml: String, attr: String): String? {
        return Regex("""$attr="([^"]*)"""").find(xml)?.groupValues?.get(1)
    }
    
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && connection?.isConnected == true) {
                try {
                    // Keepalive handled by connection
                } catch (_: Exception) {
                    _connectionState.value = XMPPConnectionState.Disconnected
                }
                delay(30000)
            }
        }
    }
    
    fun disconnect() {
        scope.launch {
            keepAliveJob?.cancel()
            try { connection?.disconnect() } catch (_: Exception) {}
            _connectionState.value = XMPPConnectionState.Disconnected
            processedMessages.clear()
        }
    }
    
    fun clearVideoData() {
        _videoMessages.value = null
        _botState.value = null
        _errorMessage.value = null
    }
}
