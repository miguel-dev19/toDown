package com.todown.network.xmpp

import android.util.Log
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
import org.jivesoftware.smackx.ping.PingManager
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import java.util.*
import javax.net.ssl.SSLContext

// Clases de datos compartidas
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

class XMPPDataSource {
    
    private var connection: XMPPTCPConnection? = null
    private var pingManager: PingManager? = null
    
    private val _connectionState = MutableStateFlow<XMPPConnectionState>(XMPPConnectionState.Disconnected)
    val connectionState: StateFlow<XMPPConnectionState> = _connectionState
    
    private val _videoMessages = MutableStateFlow<VideoData?>(null)
    val videoMessages: StateFlow<VideoData?> = _videoMessages
    
    private val _botState = MutableStateFlow<BotState?>(null)
    val botState: StateFlow<BotState?> = _botState
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
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
                    .setResource(Resourcepart.from("Android"))
                    .setUsernameAndPassword(phone, jwt)
                    .build()
                
                connection = XMPPTCPConnection(config)
                connection?.connect()
                connection?.login()
                
                connection?.sendStanza(Presence(Presence.Type.available))
                
                pingManager = PingManager.getInstanceFor(connection)
                pingManager?.pingInterval = 14
                
                ReconnectionManager.getInstanceFor(connection).apply {
                    enableAutomaticReconnection()
                    setFixedDelay(3)
                }
                
                setupListeners()
                
                _connectionState.value = XMPPConnectionState.Connected
                Log.d("XMPP", "Conectado como Android")
                
            } catch (e: Exception) {
                Log.e("XMPP", "Error: ${e.message}", e)
                _connectionState.value = XMPPConnectionState.Error(e.message ?: "Error")
            }
        }
    }
    
    private fun setupListeners() {
        connection?.addStanzaListener(
            { stanza -> handleStanza(stanza) },
            StanzaFilter { it is Message }
        )
    }
    
    private fun handleStanza(stanza: Stanza) {
        if (stanza !is Message) return
        val xml = stanza.toXML().toString()
        
        if (xml.contains("<rd ") || xml.contains("<dd ")) return
        
        val msgId = Regex("i='([^']+)'").find(xml)?.groupValues?.get(1) ?: return
        if (msgId in processedMessages) return
        processedMessages.add(msgId)
        if (processedMessages.size > 500) processedMessages.clear()
        
        val from = Regex("f='([^']+)'").find(xml)?.groupValues?.get(1)
        val sender = from?.split("@")?.first()
        if (sender != botPhone) return
        
        val body = Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1) ?: return
        
        Log.d("XMPP", "Bot: $body")
        
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
                val msg = Message()
                msg.stanzaId = UUID.randomUUID().toString().replace("-", "").take(16)
                msg.to = JidCreate.bareFrom("${botPhone}@im.todus.cu")
                msg.body = url
                connection?.sendStanza(msg)
                
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
        return VideoData(
            url = url,
            fileName = attr(tag, "n") ?: "video.mp4",
            size = attr(tag, "s")?.toLongOrNull() ?: 0L,
            duration = attr(tag, "d")?.toIntOrNull() ?: 0,
            thumbnailUrl = attr(tag, "tnail") ?: "",
            title = body.trim()
        )
    }
    
    private fun attr(xml: String, name: String) = Regex("""$name="([^"]*)"""").find(xml)?.groupValues?.get(1)
    
    fun disconnect() {
        scope.launch {
            try { connection?.disconnect() } catch (_: Exception) {}
            _connectionState.value = XMPPConnectionState.Disconnected
        }
    }
    
    fun clearVideoData() {
        _videoMessages.value = null
        _botState.value = null
        _errorMessage.value = null
    }
}
