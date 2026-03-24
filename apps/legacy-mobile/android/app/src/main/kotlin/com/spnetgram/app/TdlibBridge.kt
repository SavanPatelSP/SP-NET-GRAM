package com.spnetgram.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicInteger

class TdlibBridge(private val engine: FlutterEngine) : MethodChannel.MethodCallHandler {
    private val channel = MethodChannel(engine.dartExecutor.binaryMessenger, "spnetgram/tdlib")
    private val mainHandler = Handler(Looper.getMainLooper())
    private var client: Client? = null
    private var apiId: Int = 0
    private var apiHash: String = ""
    private var authorizationState: TdApi.AuthorizationState? = null

    fun register() {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                apiId = call.argument<Int>("apiId") ?: 0
                apiHash = call.argument<String>("apiHash") ?: ""
                ensureClient()
                result.success(true)
            }
            "sendPhoneNumber" -> {
                val phone = call.argument<String>("phone") ?: ""
                send(TdApi.SetAuthenticationPhoneNumber(phone, null))
                result.success(true)
            }
            "sendOtp" -> {
                val code = call.argument<String>("code") ?: ""
                send(TdApi.CheckAuthenticationCode(code))
                result.success(true)
            }
            "sendPassword" -> {
                val password = call.argument<String>("password") ?: ""
                send(TdApi.CheckAuthenticationPassword(password))
                result.success(true)
            }
            "registerUser" -> {
                val firstName = call.argument<String>("firstName") ?: ""
                val lastName = call.argument<String>("lastName") ?: ""
                send(TdApi.RegisterUser(firstName, lastName))
                result.success(true)
            }
            "fetchChats" -> {
                fetchChats()
                result.success(true)
            }
            "fetchMessages" -> {
                val chatId = call.argument<Long>("chatId") ?: 0L
                fetchMessages(chatId)
                result.success(true)
            }
            "sendMessage" -> {
                val chatId = call.argument<Long>("chatId") ?: 0L
                val text = call.argument<String>("text") ?: ""
                sendMessage(chatId, text)
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    private fun ensureClient() {
        if (client != null) return
        client = Client.create(updateHandler, null, null)
    }

    private fun send(function: TdApi.Function) {
        client?.send(function, defaultHandler)
    }

    private val defaultHandler = Client.ResultHandler { obj ->
        if (obj is TdApi.Error) {
            Log.e("TDLIB", "TDLib error: ${obj.message}")
        }
    }

    private val updateHandler = Client.ResultHandler { obj ->
        when (obj) {
            is TdApi.UpdateAuthorizationState -> {
                authorizationState = obj.authorizationState
                handleAuthState(obj.authorizationState)
            }
            is TdApi.UpdateNewMessage -> {
                emitMessage(obj.message)
            }
            else -> {
            }
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val params = TdApi.TdlibParameters().apply {
                    databaseDirectory = engine.context.filesDir.absolutePath + "/tdlib"
                    filesDirectory = engine.context.filesDir.absolutePath + "/tdlib_files"
                    useMessageDatabase = true
                    useFileDatabase = true
                    useSecretChats = false
                    apiId = this@TdlibBridge.apiId
                    apiHash = this@TdlibBridge.apiHash
                    systemLanguageCode = "en"
                    deviceModel = android.os.Build.MODEL
                    systemVersion = android.os.Build.VERSION.RELEASE
                    applicationVersion = "1.0"
                    enableStorageOptimizer = true
                }
                send(TdApi.SetTdlibParameters(params))
            }
            is TdApi.AuthorizationStateWaitEncryptionKey -> {
                send(TdApi.CheckDatabaseEncryptionKey(ByteArray(0)))
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> emitAuth("WAIT_PHONE")
            is TdApi.AuthorizationStateWaitCode -> emitAuth("WAIT_CODE")
            is TdApi.AuthorizationStateWaitPassword -> emitAuth("WAIT_PASSWORD")
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> emitAuth(
                "WAIT_DEVICE_CONFIRMATION",
                state.link
            )
            is TdApi.AuthorizationStateWaitRegistration -> emitAuth("WAIT_REGISTRATION")
            is TdApi.AuthorizationStateReady -> {
                emitAuth("READY")
                fetchChats()
            }
            is TdApi.AuthorizationStateLoggingOut -> emitAuth("LOGGING_OUT")
            is TdApi.AuthorizationStateClosed -> emitAuth("CLOSED")
            else -> emitAuth("UNKNOWN")
        }
    }

    private fun fetchChats() {
        val request = TdApi.GetChats(TdApi.ChatListMain(), 50)
        client?.send(request) { obj ->
            if (obj is TdApi.Chats) {
                val ids = obj.chatIds
                if (ids.isEmpty()) {
                    emitChats(emptyList())
                    return@send
                }
                val remaining = AtomicInteger(ids.size)
                val chats = mutableListOf<TdApi.Chat>()
                ids.forEach { chatId ->
                    client?.send(TdApi.GetChat(chatId)) { chatObj ->
                        if (chatObj is TdApi.Chat) {
                            synchronized(chats) { chats.add(chatObj) }
                        }
                        if (remaining.decrementAndGet() == 0) {
                            emitChats(chats)
                        }
                    }
                }
            }
        }
    }

    private fun fetchMessages(chatId: Long) {
        val request = TdApi.GetChatHistory(chatId, 0, 0, 30, false)
        client?.send(request) { obj ->
            if (obj is TdApi.Messages) {
                obj.messages?.reversed()?.forEach { msg ->
                    emitMessage(msg)
                }
            }
        }
    }

    private fun sendMessage(chatId: Long, text: String) {
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, null), false, true)
        val request = TdApi.SendMessage(chatId, 0, 0, null, null, content)
        send(request)
    }

    private fun emitAuth(status: String, message: String? = null) {
        val payload = mutableMapOf<String, Any>("status" to status)
        if (message != null) {
            payload["message"] = message
        }
        emit("onAuthState", payload)
    }

    private fun emitChats(list: List<TdApi.Chat>) {
        val payload = list.map { chat ->
            mapOf(
                "id" to chat.id,
                "title" to chat.title,
                "lastMessage" to lastMessageText(chat.lastMessage),
                "unreadCount" to chat.unreadCount
            )
        }
        emit("onChats", payload)
    }

    private fun emitMessage(message: TdApi.Message) {
        val payload = mapOf(
            "id" to message.id,
            "chatId" to message.chatId,
            "sender" to senderLabel(message.senderId),
            "text" to messageText(message.content),
            "timestamp" to message.date,
            "isOutgoing" to message.isOutgoing
        )
        emit("onMessage", payload)
    }

    private fun emit(method: String, args: Any?) {
        mainHandler.post { channel.invokeMethod(method, args) }
    }

    private fun messageText(content: TdApi.MessageContent?): String {
        return when (content) {
            is TdApi.MessageText -> content.text.text
            else -> ""
        }
    }

    private fun senderLabel(sender: TdApi.MessageSender?): String {
        return when (sender) {
            is TdApi.MessageSenderUser -> "user:${sender.userId}"
            is TdApi.MessageSenderChat -> "chat:${sender.chatId}"
            else -> "unknown"
        }
    }

    private fun lastMessageText(message: TdApi.Message?): String {
        if (message == null) return ""
        return messageText(message.content)
    }
}
