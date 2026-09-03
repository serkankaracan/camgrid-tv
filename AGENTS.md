# KARACAM agent contract

Read `docs/IMPLEMENTATION_STATUS.md` before making changes.

## Invariants

- Product: KARACAM; package/application ID:
  `io.github.serkankaracan.camgridtv`; version `0.1.0-alpha.1` (`versionCode` 1).
- One Android `app` module, Kotlin, Compose for TV, Media3 RTSP, coroutines/Flow,
  ViewModel state and a small manual dependency container. Do not add Hilt.
- The app is Android TV-only, landscape and fully usable with D-pad/OK/Back.
- Discovery uses ONVIF WS-Discovery. Grid uses `/stream2`; fullscreen first uses
  `/stream1` with a one-way `/stream2` compatibility fallback; audio is disabled.
  Do not hard-code a model, camera count or real IP.
- Runtime communication is local-only. No backend, cloud login, analytics,
  crash reporting, recording, PTZ, audio, notifications, boot receiver, mobile UI,
  web UI, reverse engineering or private vendor API.
- Credentials never enter source, BuildConfig, logs, screenshots, fixtures,
  issues or Git. Non-secret settings use DataStore. Secrets use AES/GCM with a
  non-exportable Android Keystore key and never fall back to plaintext.
- Never commit `local.properties`, `.local-test-config*`, logs, APKs, signing
  material, a credential-bearing URI, or private test-environment details.

## Windows quality gate

Run from PowerShell:

```powershell
.\gradlew.bat --no-daemon spotlessCheck lintDebug lintRelease testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease
.\scripts\check-no-secrets.ps1
git diff --check
git status --short
```

Run `.\gradlew.bat connectedDebugAndroidTest` only when a CLI-configured emulator
or device is available. Record commands, exit codes, manual status and blockers
in `docs/TEST_REPORT.md`; fake tests never count as physical camera or TV tests.

## Phase protocol

For each phase: inspect/create issue when GitHub is available, implement, test,
fix, update docs, inspect secrets/diff/staging, commit with Conventional Commits,
push `main`, and watch CI. Never force-push or rewrite published history. Continue
through unaffected work when GitHub auth, physical cameras, credentials or a TV
device are unavailable; mark only those checks BLOCKED.
