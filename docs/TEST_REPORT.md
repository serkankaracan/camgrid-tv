# Test report

Last updated: 2026-09-03

All evidence in this report is secret-free. No home IP, camera account, complete
credential-bearing RTSP URI, MAC address, adb log or VLC log is committed. A
successful build or fake test is not counted as physical camera or Mi Stick
validation.

## Environment

- Host: Windows 11 with PowerShell, Visual Studio Code and Codex.
- Java: JDK 17 from the installed Unity Android support bundle.
- Android SDK: project-local command-line SDK with stable API 37; Android Studio
  was not assumed or required.
- Emulator/AVD: unavailable.
- Connected adb device: unavailable; the project-local `adb devices -l` command
  completed with exit code 0 and returned an empty device list.
- Physical C500, C510W and Mi Stick access: unavailable during this run.

## Automated and repository checks

| Area | Status | Evidence |
| --- | --- | --- |
| Standard local quality gate | PASS | `spotlessApply spotlessCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest` completed with exit code 0 |
| JVM unit tests | PASS | 116 tests; 0 failures, 0 errors, 0 skipped |
| Formatting | PASS | Included in the standard quality gate |
| Android lint | PASS | Included in the standard quality gate |
| Debug APK build | PASS | Included in the standard quality gate |
| Instrumented test APK build | PASS | `assembleDebugAndroidTest` compiled the test APK |
| Instrumented test execution | BLOCKED | `connectedDebugAndroidTest` was not run because no emulator or connected adb device was available |
| adb device availability | PASS (check only) | Project-local `adb.exe devices -l` exited 0; the device list was empty |
| Secret hygiene | PASS | `check-no-secrets.ps1` scanned 147 candidate files and exited 0 |
| Whitespace/diff validation | PASS | `git diff --check` exited successfully |
| Public GitHub repository | PASS | Public repository was created during Phase 0 |
| Most recent GitHub Actions run | PASS | Phase 7 quality gate run `33706625607` completed successfully; Phase 8 documentation push is pending |

Commands actually run from PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Unity\Hub\Editor\6000.3.21f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
$env:ANDROID_USER_HOME = "$PWD\.android-user-home"
.\gradlew.bat --no-daemon spotlessApply spotlessCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest
& '.\.android-sdk\platform-tools\adb.exe' devices -l
.\scripts\check-no-secrets.ps1
git diff --check
Get-FileHash -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk' -Algorithm SHA256
```

Each command above exited with code 0. The adb command printed only the header
`List of devices attached`; it did not list a serial or transport.

The Android test APK was compiled only. `connectedDebugAndroidTest` was not run
and no instrumented test is reported as executed.

## Debug APK

- Local path: `app\build\outputs\apk\debug\app-debug.apk`
- Size: 17,754,065 bytes
- SHA-256:
  `033AFA27F691852D11E56BF86C8FF8B4082AE8BF44E47DA097BE7C6A1CCCC0E2`
- Distribution status: local debug/test artifact; not a release-signed build and
  not committed to Git.

## Physical and real-network acceptance

| Scenario | Status | Reason |
| --- | --- | --- |
| Discover C500 and C510W over ONVIF | BLOCKED | Physical cameras and their LAN were unavailable |
| Validate C500 `/stream2` | BLOCKED | The C500 and an in-app Camera Account entry were unavailable |
| Validate C510W `/stream2` | BLOCKED | The C510W and an in-app Camera Account entry were unavailable |
| Run two `/stream2` feeds together for 15 minutes | BLOCKED | Physical cameras and Mi Stick were unavailable |
| Change wall focus with the physical D-pad | BLOCKED | Mi Stick/remote was unavailable |
| Open C500 `/stream1` fullscreen with OK | BLOCKED | The C500 and Mi Stick were unavailable |
| Open C510W `/stream1` fullscreen with OK | BLOCKED | The C510W and Mi Stick were unavailable |
| Return to the wall and restore focus with Back | BLOCKED | Mi Stick/remote was unavailable |
| Background/foreground lifecycle recovery | BLOCKED | No emulator or physical Android TV device was available |
| Wi-Fi loss and reconnection recovery | BLOCKED | No physical Android TV/network test setup was available |
| Wrong-password behavior with a real camera | BLOCKED | No physical camera or in-app credentials were available |
| Selection persistence on a physical device | BLOCKED | No emulator or physical Android TV device was available |
| Logcat credential/URI audit during real playback | BLOCKED | No connected adb device or real stream was available |
| Decoder and memory behavior under real concurrent streams | BLOCKED | Mi Stick and physical streams were unavailable |
| Three-stream physical/fake-device grid observation | BLOCKED | No executable emulator or physical device was available |

Automated tests cover pure discovery parsing/deduplication, secure configuration,
URI encoding/redaction, retry/state reducers, layout calculations and lifecycle
coordination with fakes. Those results do not replace the blocked physical
acceptance scenarios above.
