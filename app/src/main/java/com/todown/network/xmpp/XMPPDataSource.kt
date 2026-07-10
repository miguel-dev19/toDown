package com.todown.network.xmpp

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jivesoftware.smack.*
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.provider.ProviderManager
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.ping.PingManager
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.*
import javax.net.ssl.SSLContext

class XMPPDataSource {
    
    private var connection: XMPPTCPConnection? = null
    private var pingManager: PingManager? = null
    private var reconnectionManager: ReconnectionManager? = null
    
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
    
    init {
        registerProviders()
    }
    
    private fun registerProviders() {
        // Video extension (lo que recibimos del bot)
        ProviderManager.addExtensionProvider("video", "video:n", object : ExtensionElementProvider<ExtensionElement> {
            override fun parse(parser: XmlPullParser, initialDepth: Int, depth: Int): ExtensionElement {
                return object : ExtensionElement {
                    override fun getNamespace(): String = "video:n"
                    override fun getElementName(): String = "video"
                    override fun toXML(): CharSequence = ""
                }
            }
        })
    }
    
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
                
                // Presence
                connection?.sendStanza(Presence(Presence.Type.available))
                
                // Ping Manager (14s como ToDus)
                pingManager = PingManager.getInstanceFor(connection)
                pingManager?.pingInterval = 14
                
                // Reconnection Manager
                reconnectionManager = ReconnectionManager.getInstanceFor(connection)
                reconnectionManager?.enableAutomaticReconnection()
                reconnectionManager?.setFixedDelay(3)
                
                setupListeners()
                
                _connectionState.value = XMPPConnectionState.Connected
                Log.d("XMPP", "Conectado!")
                
            } catch (e: Exception) {
                Log.e("XMPP", "Error: ${e.message}", e)
                _connectionState.value = XMPPConnectionState.Error(e.message ?: "Error")
            }
        }
    }
    
    private fun setupListeners() {
        connection?.addStanzaListener(
            { stanza -> handleStanza(stanza) },
            StanzaFilter { stanza -> stanza is Message }
        )
    }
    
    private fun handleStanza(stanza: Stanza) {
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
        
        // Solo mensajes del bot
        if (sender != botPhone) return
        
        // Extraer cuerpo
        val body = Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1) ?: return
        
        Log.d("XMPP", "Bot: $body")
        
        when {
            body.contains("Analizando") -> { _botState.value = BotState.ANALYZING; _errorMessage.value = null }
            body.contains("Descargando") -> _botState.value = BotState.DOWNLOADING
            body.contains("Procesando") -> _botState.value = BotState.PROCESSING
            body.contains("Subiendo") -> _botState.value = BotState.UPLOADING
            body.contains("Listo") -> _botState.value = BotState.READY
            body.startsWith("\u274C") -> { _botState.value = BotState.ERROR; _errorMessage.value = body.removePrefix("\u274C").trim() }
        }
        
        // Parsear video
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
                val msg = Message()
                msg.stanzaId = msgId
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
    
    private fun parseVideoMessage(xml: String, body: String): VideoData? {
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
