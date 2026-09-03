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
  for fullscreen, with audio disabled and a per-camera `/stream2` compatibility
  fallback when a primary stream cannot play.
- Dynamic, centered camera wall layouts, fullscreen navigation and focus restore.
- Connectivity monitoring, bounded retry/backoff, app lifecycle recovery and
  tile-local decoder error handling.
- Windows/PowerShell build, test, secret-hygiene and GitHub Actions workflows.
- Dark control-room discovery, setup, live-wall and fullscreen presentation with
  explicit focus, selection, readiness and live-state treatments.

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
- Keep TV text fields in D-pad browse mode until OK/Enter explicitly opens edit
  mode and the software keyboard; Back or IME Done returns to browse mode.
- Replace the inactive start control with one adaptive Verify connection → Watch
  N cameras action, including deterministic failure-first targeting and focus
  restoration after asynchronous verification.
- Keep conditional wall rescanning in a reachable, non-overlapping header action.
- Stop primary fullscreen streams that fail playback from looping forever by
  enabling decoder fallback and switching that session from `/stream1` to its
  known-working `/stream2` stream.
- Replace `PlayerView`/`AndroidView` interop with Media3's Compose-native fitted
  video surface so non-16:9 displays preserve the camera's source aspect ratio.
- Use `TextureView` for embedded setup/wall feeds so video cannot cover Compose
  overlays or the stronger wall focus border; retain `SurfaceView` for fullscreen.
- Move fullscreen camera/status information to an overscan-safe upper-right panel,
  remove the persistent Back hint, and add a D-pad-operated Safe 90% → Fit → Fill
  cycle. Every mode preserves aspect ratio and only Fill crops edges.
- Rename the Gradle project and user-visible product/launcher branding to KARACAM
  while retaining the package and persistence identifiers for in-place updates.

### Verification

- The current Windows/PowerShell quality gate covers debug and release lint, JVM
  tests, debug APK assembly, debug instrumented-test APK assembly, minified
  release assembly, secret hygiene and whitespace.
- Final candidate counts, artifact digest and CI run are recorded only after the
  integrated build; earlier snapshot evidence is isolated as historical in
  `docs/TEST_REPORT.md`.
- Instrumented-test compilation is not device execution. Current automated
  device execution remains blocked until one Mi Stick is connected over adb.

### Known limitations

- A user-run pre-fix physical test confirmed two concurrent wall streams and
  exposed a C510W `/stream1` fullscreen reconnect loop plus display-ratio
  distortion. Supplied photographs show the APK before the latest surface,
  focus, information-panel and scale-control changes; the corrected APK still
  requires a physical regression run.
- Instrumented tests, API 37 permission revocation, measured logcat and
  decoder/memory evidence require an adb-connected run. The 15-minute stability,
  lifecycle, Wi-Fi recovery and wrong-password scenarios also remain pending on
  the corrected APK.
- No release tag or GitHub Release is published while physical acceptance remains
  blocked; this version stays marked as Unreleased.
