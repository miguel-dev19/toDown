package com.todown.data.repository

import com.todown.network.xmpp.XMPPConnectionState
import com.todown.network.xmpp.XMPPManager
import kotlinx.coroutines.flow.StateFlow

class XMPPRepository(private val xmppManager: XMPPManager) {
    
    val connectionState: StateFlow<XMPPConnectionState> = xmppManager.connectionState
    
    fun connect(phone: String, jwt: String) {
        xmppManager.connect(phone, jwt)
    }
    
    fun disconnect() {
        xmppManager.disconnect()
    }
}
