import Flutter
import UIKit
import Foundation

@_silgen_name("td_json_client_create")
private func td_json_client_create() -> OpaquePointer?

@_silgen_name("td_json_client_send")
private func td_json_client_send(_ client: OpaquePointer?, _ request: UnsafePointer<CChar>?)

@_silgen_name("td_json_client_receive")
private func td_json_client_receive(_ client: OpaquePointer?, _ timeout: Double) -> UnsafePointer<CChar>?

@_silgen_name("td_json_client_destroy")
private func td_json_client_destroy(_ client: OpaquePointer?)

class TdlibBridge: NSObject {
    private let channel: FlutterMethodChannel
    private let queue = DispatchQueue(label: "spnetgram.tdlib")
    private var client: OpaquePointer?
    private var apiId: Int = 0
    private var apiHash: String = ""
    private var receiving = false
    private var pendingChats: Set<Int64> = []
    private var chatCache: [Int64: [String: Any]] = [:]
    private let databasePath: String

    init(controller: FlutterViewController) {
        self.channel = FlutterMethodChannel(name: "spnetgram/tdlib", binaryMessenger: controller.binaryMessenger)
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        self.databasePath = documents?.appendingPathComponent("tdlib").path ?? "tdlib"
        super.init()
        self.channel.setMethodCallHandler(handle)
    }

    private func handle(call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            if let args = call.arguments as? [String: Any] {
                apiId = args["apiId"] as? Int ?? 0
                apiHash = args["apiHash"] as? String ?? ""
            }
            ensureClient()
            result(true)
        case "sendPhoneNumber":
            if let args = call.arguments as? [String: Any] {
                let phone = args["phone"] as? String ?? ""
                send(["@type": "setAuthenticationPhoneNumber", "phone_number": phone])
            }
            result(true)
        case "sendOtp":
            if let args = call.arguments as? [String: Any] {
                let code = args["code"] as? String ?? ""
                send(["@type": "checkAuthenticationCode", "code": code])
            }
            result(true)
        case "sendPassword":
            if let args = call.arguments as? [String: Any] {
                let password = args["password"] as? String ?? ""
                send(["@type": "checkAuthenticationPassword", "password": password])
            }
            result(true)
        case "registerUser":
            if let args = call.arguments as? [String: Any] {
                let firstName = args["firstName"] as? String ?? ""
                let lastName = args["lastName"] as? String ?? ""
                send(["@type": "registerUser", "first_name": firstName, "last_name": lastName])
            }
            result(true)
        case "fetchChats":
            send(["@type": "getChats", "chat_list": ["@type": "chatListMain"], "limit": 50])
            result(true)
        case "fetchMessages":
            if let args = call.arguments as? [String: Any] {
                let chatId = args["chatId"] as? Int64 ?? 0
                send([
                    "@type": "getChatHistory",
                    "chat_id": chatId,
                    "from_message_id": 0,
                    "offset": 0,
                    "limit": 30,
                    "only_local": false
                ])
            }
            result(true)
        case "sendMessage":
            if let args = call.arguments as? [String: Any] {
                let chatId = args["chatId"] as? Int64 ?? 0
                let text = args["text"] as? String ?? ""
                send([
                    "@type": "sendMessage",
                    "chat_id": chatId,
                    "input_message_content": [
                        "@type": "inputMessageText",
                        "text": ["@type": "formattedText", "text": text],
                        "disable_web_page_preview": true,
                        "clear_draft": true
                    ]
                ])
            }
            result(true)
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    private func ensureClient() {
        if client != nil { return }
        client = td_json_client_create()
        startReceiveLoop()
    }

    private func startReceiveLoop() {
        if receiving { return }
        receiving = true
        queue.async { [weak self] in
            guard let self = self else { return }
            while self.receiving {
                if let response = td_json_client_receive(self.client, 1.0) {
                    let json = String(cString: response)
                    self.handleTdlibResponse(json)
                }
            }
        }
    }

    private func send(_ payload: [String: Any]) {
        guard let client = client else { return }
        if payload["@type"] as? String == "setTdlibParameters" {
            // no-op
        }
        let data = try? JSONSerialization.data(withJSONObject: payload, options: [])
        guard let jsonData = data, let jsonString = String(data: jsonData, encoding: .utf8) else { return }
        jsonString.withCString { ptr in
            td_json_client_send(client, ptr)
        }
    }

    private func handleTdlibResponse(_ json: String) {
        guard let data = json.data(using: .utf8) else { return }
        guard let obj = try? JSONSerialization.jsonObject(with: data, options: []),
              let dict = obj as? [String: Any],
              let type = dict["@type"] as? String else { return }

        switch type {
        case "updateAuthorizationState":
            if let state = dict["authorization_state"] as? [String: Any],
               let stateType = state["@type"] as? String {
                if stateType == "authorizationStateWaitTdlibParameters" {
                    send([
                        "@type": "setTdlibParameters",
                        "parameters": [
                            "@type": "tdlibParameters",
                            "database_directory": databasePath,
                            "use_message_database": true,
                            "use_file_database": true,
                            "use_secret_chats": false,
                            "api_id": apiId,
                            "api_hash": apiHash,
                            "system_language_code": "en",
                            "device_model": UIDevice.current.model,
                            "system_version": UIDevice.current.systemVersion,
                            "application_version": "1.0",
                            "enable_storage_optimizer": true
                        ]
                    ])
                } else if stateType == "authorizationStateWaitPhoneNumber" {
                    emit("onAuthState", ["status": "WAIT_PHONE"])
                } else if stateType == "authorizationStateWaitCode" {
                    emit("onAuthState", ["status": "WAIT_CODE"])
                } else if stateType == "authorizationStateWaitPassword" {
                    emit("onAuthState", ["status": "WAIT_PASSWORD"])
                } else if stateType == "authorizationStateWaitEncryptionKey" {
                    send(["@type": "checkDatabaseEncryptionKey", "encryption_key": ""])
                } else if stateType == "authorizationStateWaitOtherDeviceConfirmation" {
                    let link = state["link"] as? String ?? ""
                    emit("onAuthState", ["status": "WAIT_DEVICE_CONFIRMATION", "message": link])
                } else if stateType == "authorizationStateWaitRegistration" {
                    emit("onAuthState", ["status": "WAIT_REGISTRATION"])
                } else if stateType == "authorizationStateReady" {
                    emit("onAuthState", ["status": "READY"])
                    send(["@type": "getChats", "chat_list": ["@type": "chatListMain"], "limit": 50])
                } else {
                    emit("onAuthState", ["status": "UNKNOWN"])
                }
            }
        case "chats":
            if let ids = dict["chat_ids"] as? [Int64] {
                pendingChats = Set(ids)
                chatCache.removeAll()
                ids.forEach { id in
                    send(["@type": "getChat", "chat_id": id])
                }
                if ids.isEmpty {
                    emit("onChats", [])
                }
            }
        case "chat":
            if let chatId = dict["id"] as? Int64 {
                let title = dict["title"] as? String ?? "Chat"
                let lastMessage = extractLastMessage(dict["last_message"] as? [String: Any])
                let unread = dict["unread_count"] as? Int ?? 0
                chatCache[chatId] = [
                    "id": chatId,
                    "title": title,
                    "lastMessage": lastMessage,
                    "unreadCount": unread
                ]
                pendingChats.remove(chatId)
                if pendingChats.isEmpty {
                    emit("onChats", Array(chatCache.values))
                }
            }
        case "messages":
            if let msgs = dict["messages"] as? [[String: Any]] {
                for msg in msgs.reversed() {
                    emitMessage(msg)
                }
            }
        case "updateNewMessage":
            if let msg = dict["message"] as? [String: Any] {
                emitMessage(msg)
            }
        default:
            break
        }
    }

    private func emitMessage(_ msg: [String: Any]) {
        let chatId = msg["chat_id"] as? Int64 ?? 0
        let id = msg["id"] as? Int64 ?? 0
        let date = msg["date"] as? Int ?? 0
        let isOutgoing = msg["is_outgoing"] as? Bool ?? false
        let sender = msg["sender_id"] as? [String: Any]
        let senderLabel: String
        if let senderType = sender?["@type"] as? String {
            if senderType == "messageSenderUser" {
                senderLabel = "user:\(sender?["user_id"] as? Int ?? 0)"
            } else if senderType == "messageSenderChat" {
                senderLabel = "chat:\(sender?["chat_id"] as? Int ?? 0)"
            } else {
                senderLabel = "unknown"
            }
        } else {
            senderLabel = "unknown"
        }

        let text = extractMessageText(msg["content"] as? [String: Any])
        emit("onMessage", [
            "id": id,
            "chatId": chatId,
            "sender": senderLabel,
            "text": text,
            "timestamp": date,
            "isOutgoing": isOutgoing
        ])
    }

    private func extractLastMessage(_ msg: [String: Any]?) -> String {
        guard let msg = msg else { return "" }
        return extractMessageText(msg["content"] as? [String: Any])
    }

    private func extractMessageText(_ content: [String: Any]?) -> String {
        guard let content = content,
              let type = content["@type"] as? String else { return "" }
        if type == "messageText", let text = content["text"] as? [String: Any] {
            return text["text"] as? String ?? ""
        }
        return ""
    }

    private func emit(_ method: String, _ args: Any?) {
        DispatchQueue.main.async {
            self.channel.invokeMethod(method, arguments: args)
        }
    }
}
