# Privacy

CamGrid TV is local-only. Camera discovery packets, settings, credentials and video
stay between the Android TV device and cameras on the trusted LAN. The application
contains no backend, advertising, analytics, crash reporting or telemetry SDK.

Non-secret configuration is kept in Android DataStore. Camera account secrets are
encrypted with AES/GCM using a non-exportable key held by Android Keystore. Android
backup is disabled, and there is no plaintext fallback if key recovery fails.
