package com.spnetgram.app

import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class TdlibBridge(private val engine: FlutterEngine) : MethodChannel.MethodCallHandler {
    private val channel = MethodChannel(engine.dartExecutor.binaryMessenger, "spnetgram/tdlib")

    fun register() {
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                val apiId = call.argument<Int>("apiId")
                val apiHash = call.argument<String>("apiHash")
                Log.d("TDLIB", "Init TDLib apiId=$apiId")
                // TODO: Initialize TDLib client.
                result.success(true)
            }
            "sendPhoneNumber" -> {
                val phone = call.argument<String>("phone")
                Log.d("TDLIB", "Phone number $phone")
                // TODO: td_send for phone number.
                result.success(true)
            }
            "sendOtp" -> {
                val code = call.argument<String>("code")
                Log.d("TDLIB", "OTP $code")
                // TODO: td_send for OTP.
                result.success(true)
            }
            "sendMessage" -> {
                val chatId = call.argument<Long>("chatId")
                val text = call.argument<String>("text")
                Log.d("TDLIB", "Send message chat=$chatId")
                // TODO: td_send for new message.
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }
}
