# Implementation status

Last updated: 2026-09-03

## Current phase

Phase 8 physical validation is BLOCKED because no physical C500, C510W, Mi
Stick, emulator or connected adb device was available. The detailed manual plan
and honest test evidence are prepared; Phase 9 documentation is next. Phases
1–7 are implemented, locally verified and pushed.

The Phase 0 public GitHub repository exists. The Phase 7 GitHub Actions quality
gate passed in run `33706625607`. GitHub Actions for the Phase 8 documentation
push and the final Phase 9 push must be observed separately before either is
reported as successful.

## Completed implementation

- Phase 0: public repository, project contract, Apache-2.0 license and repository
  hygiene.
- Phase 1: buildable API 37 Android TV shell, Windows CI definition, D-pad-first
  Compose structure and Turkish/English resources.
- Phase 2: camera models, DataStore persistence, credential profiles, AES/GCM
  Android Keystore abstraction, validation and redaction.
- Phase 3: API 37 local-network permission policy, ONVIF WS-Discovery probe/parser,
  multicast transport, deduplication, identity matching and discovery repository.
- Phase 4: camera discovery/setup/selection states and remote-first UI flow.
- Phase 5: Media3 RTSP layer, safe URI construction, `/stream2` preview behavior,
  safe state mapping and fake-player tests. Real camera playback remains blocked.
- Phase 6: dynamic wall layouts, focus navigation, `/stream2` wall and `/stream1`
  fullscreen resource coordination.
- Phase 7: retry/backoff, connectivity monitoring, lifecycle recovery, decoder
  error isolation, keep-screen-on control and debug diagnostics. Completed by
  commit `7e945c8`; GitHub Actions passed.

## Tests actually run

- Standard PowerShell quality gate — PASS:

  ```powershell
  $env:JAVA_HOME = 'C:\Program Files\Unity\Hub\Editor\6000.3.21f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK'
  $env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
  $env:ANDROID_USER_HOME = "$PWD\.android-user-home"
  .\gradlew.bat --no-daemon spotlessApply spotlessCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest
  ```

  The combined command exited with code 0.
- JVM unit tests — PASS: 116 tests, 0 failures, 0 errors, 0 skipped.
- `assembleDebugAndroidTest` — PASS for APK compilation only;
  `connectedDebugAndroidTest` was not executed.
- `& '.\.android-sdk\platform-tools\adb.exe' devices -l` — exit code 0 with an
  empty device list.
- `.\scripts\check-no-secrets.ps1` — PASS, 147 files checked, exit code 0.
- `git diff --check` — PASS, exit code 0.
- Debug APK — 17,754,065 bytes; SHA-256
  `033AFA27F691852D11E56BF86C8FF8B4082AE8BF44E47DA097BE7C6A1CCCC0E2`.

## Manual and external status

- Windows 11/PowerShell workflow: PASS.
- JDK 17 from the Unity Android support bundle: PASS.
- Project-local API 37 command-line SDK: PASS.
- Emulator/AVD and connected adb device: NOT AVAILABLE; adb returned an empty
  device list.
- Physical C500/C510W discovery and RTSP: BLOCKED.
- Physical Mi Stick D-pad, fullscreen, lifecycle and 15-minute wall tests:
  BLOCKED.
- Real wrong-password, reconnect, decoder/memory and logcat-redaction scenarios:
  BLOCKED.
- Phase 7 GitHub Actions: PASS, run `33706625607`.
- Phase 8 documentation and final Phase 9 GitHub Actions: NOT RUN yet.

No port reachability, build result, fake stream or JVM test has been treated as
evidence of successful physical playback.

## Next

1. Commit the Phase 8 evidence and manual plan, then observe its GitHub Actions
   result.
2. Finish the Phase 9 documentation/diff/secret review.
3. Commit and push the final changes to `main`, then observe GitHub Actions.
4. When hardware is available, execute every Phase 8 scenario from the manual
   test plan and update each BLOCKED/NOT RUN entry with redacted evidence.
