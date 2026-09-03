# Changelog

All notable changes follow Keep a Changelog principles.

## [0.1.0-alpha.1] - Unreleased

### Added

- Android TV-only, D-pad-first Compose application shell with Turkish and English
  resources.
- Local ONVIF WS-Discovery with API 37 permission coordination, hardened XML
  parsing, deduplication and saved-camera identity matching.
- Camera setup, naming, selection and versioned DataStore persistence.
- Shared and per-camera credential profiles backed by AES/GCM and Android
  Keystore abstractions.
- Media3 RTSP playback coordination using `/stream2` for the wall and `/stream1`
  for fullscreen, with audio disabled.
- Dynamic, centered camera wall layouts, fullscreen navigation and focus restore.
- Connectivity monitoring, bounded retry/backoff, app lifecycle recovery and
  tile-local decoder error handling.
- Windows/PowerShell build, test, secret-hygiene and GitHub Actions workflows.

### Security

- Runtime camera traffic is restricted to literal local addresses.
- Credential-bearing URIs and sensitive error details are redacted.
- Credential ciphertext uses a fresh AES/GCM IV and a non-exportable Android
  Keystore key; no plaintext fallback is provided.
- App backup is disabled and secret/signing/build artifacts are excluded from
  Git.

### Fixed

- Pin ONVIF service addresses to the literal UDP response source so a mismatched
  `XAddr` cannot redirect local requests to another host.
- Stop discovery and playback immediately when API 37 local-network permission
  is revoked, while preserving the correct rationale/settings state on resume.
- Map Media3 unsupported-format and reclaimed-decoder errors to actionable,
  non-retrying UI states.
- Keep the tested `/stream2` visible briefly in setup, retain D-pad focus during
  the test, and keep TV overlays within the overscan-safe area.

### Verification

- The current Windows/PowerShell quality gate covers debug and release lint, JVM
  tests, debug APK assembly, debug instrumented-test APK assembly, minified
  release assembly, secret hygiene and whitespace.
- Final candidate counts, artifact digest and CI run are recorded only after the
  integrated build; earlier snapshot evidence is isolated as historical in
  `docs/TEST_REPORT.md`.
- Instrumented-test compilation is not device execution. Physical Mi Stick
  execution remains blocked until a device is available.

### Known limitations

- Physical C500/C510W discovery and playback are not yet run.
- Mi Stick D-pad, 15-minute dual-stream stability, lifecycle, API 37 permission
  revocation, Wi-Fi recovery, wrong-password, logcat and decoder/memory
  acceptance scenarios remain blocked until hardware is available.
- No release tag or GitHub Release is published while physical acceptance remains
  blocked; this version stays marked as Unreleased.
