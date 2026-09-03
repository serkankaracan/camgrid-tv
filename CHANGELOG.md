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

### Verification

- Standard local quality gate passed on Windows 11 with JDK 17 and a project-local
  API 37 command-line SDK.
- 116 JVM tests passed with no failures, errors or skips.
- Debug and instrumented-test APKs compiled successfully; instrumented tests were
  not executed because no emulator or adb device was available.
- Secret scan passed for 147 files and `git diff --check` passed.
- Debug APK size: 17,754,065 bytes; SHA-256:
  `033AFA27F691852D11E56BF86C8FF8B4082AE8BF44E47DA097BE7C6A1CCCC0E2`.

### Known limitations

- Physical C500/C510W discovery and playback are not yet run.
- Mi Stick D-pad, 15-minute dual-stream stability, lifecycle, Wi-Fi recovery,
  wrong-password, logcat and decoder/memory acceptance scenarios remain blocked
  until hardware is available.
- No release tag or GitHub Release is published while physical acceptance remains
  blocked; this version stays marked as Unreleased.
