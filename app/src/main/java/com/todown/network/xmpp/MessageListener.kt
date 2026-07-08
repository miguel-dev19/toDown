package com.todown.network.xmpp

import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Message

class MessageListener(
    private val onMessageReceived: (Message) -> Unit
) {
    fun setupListener(chatManager: ChatManager) {
        chatManager.addIncomingListener { _, message, _ ->
            onMessageReceived(message)
        }
    }
}
