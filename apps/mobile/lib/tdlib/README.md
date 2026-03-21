# TDLib Bridge (Flutter)

This folder is the Dart-facing interface for TDLib. The native Android/iOS implementations will expose TDLib through platform channels.

Next steps:
- Add native TDLib binaries to the Android project.
- Implement platform channel methods that map to TDLib auth + chat APIs.
- Wire `TdlibBridge` streams into the app state.

Native channel stubs live in `apps/mobile/native_bridge/`.
