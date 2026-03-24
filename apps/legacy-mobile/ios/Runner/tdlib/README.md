Place the TDLib dynamic library for iOS in this folder.

Expected filename: `libtdjson.dylib` or `libtdjson.framework/libtdjson`

Steps:
1. Build TDLib for iOS (arm64 + simulator) and create a universal binary.
2. Copy the binary into this folder.
3. Add it to the Xcode project:
   - Runner target > Build Phases > Link Binary With Libraries.
   - Ensure the binary is embedded (if using a framework).

The Swift bridge loads TDLib symbols dynamically at runtime.
