import Flutter
import UIKit

class TdlibBridge: NSObject {
    private let channel: FlutterMethodChannel

    init(controller: FlutterViewController) {
        self.channel = FlutterMethodChannel(name: "spnetgram/tdlib", binaryMessenger: controller.binaryMessenger)
        super.init()
        self.channel.setMethodCallHandler(handle)
    }

    private func handle(call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initialize":
            result(true)
        case "sendPhoneNumber":
            result(true)
        case "sendOtp":
            result(true)
        case "sendMessage":
            result(true)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}
