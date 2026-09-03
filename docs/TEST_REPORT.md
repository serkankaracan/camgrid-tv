# Test report

Last updated: 2026-09-03

All evidence in this report must be secret-free. Do not record a home IP, camera
account, complete credential-bearing RTSP URI, MAC address, adb serial, pairing
code, raw adb/VLC log or private test-environment detail. A successful build,
port check, fake or emulator test is not physical camera or Mi Stick evidence.

## Current candidate verification

This section is the only source of truth for the current integrated working
tree. Local evidence was measured on 2026-09-03 from the final source tree, then
the exact implementation tree was committed and observed in GitHub Actions. Do
not copy a number, hash or run ID from the historical section.

| Check | Status | Current evidence |
| --- | --- | --- |
| Repository revision | PASS | Implementation commit `42d73d210c0cf06d4ba7a172715d44ba59f53abe` |
| Standard PowerShell quality gate | PASS | `scripts\invoke-quality-gate.ps1` exited 0; final run completed in 4m 7s |
| Debug and release lint | PASS WITH WARNINGS | Each variant: 0 fatal, 0 error, 3 warnings; two dependency-update notices and the expected xhdpi-only TV banner density advisory |
| JVM unit tests | PASS | 31 suites; 145 tests; 0 failures, 0 errors, 0 skipped |
| Debug APK build | PASS | `assembleDebug` completed |
| Debug instrumented-test APK build | PASS | `assembleDebugAndroidTest` completed; compilation is not physical execution |
| Minified release APK build | PASS | `assembleRelease` completed; artifact is deliberately unsigned and is not a release claim |
| Secret hygiene | PASS | `Secret hygiene check passed for 153 files.` |
| Whitespace/diff validation | PASS | `git diff --check` exited 0; the `.gitignore` LF/CRLF message is a conversion warning, not a diff error |
| Debug APK size and SHA-256 | PASS | 17,805,949 bytes; `8C7317B0EE7B20801ECE0CBC936AE46E5D9AD814A2965756ED7F34848F4FAE9A` |
| APK identity, signing and alignment | PASS | Debug package is `io.github.serkankaracan.camgridtv.debug`, target SDK 37, Leanback launcher present; debug and test APKs verify with one v2 signer; all three APKs pass zip alignment |
| TV banner asset | PASS | 320x180, 32-bit PNG, 79,262 bytes; packaged as the application banner |
| adb inventory | BLOCKED | 0 connected rows; 0 authorized, unauthorized or offline devices; `connectedDebugAndroidTest` was not run |
| GitHub Actions | PASS | Android quality gate run [33729134264](https://github.com/serkankaracan/camgrid-tv/actions/runs/33729134264) passed in 7m 34s for the implementation commit |

Run the final local evidence from Windows PowerShell, without assuming Android
Studio:

```powershell
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath "$PWD\.android-sdk"
Get-Item -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk' |
    Select-Object -Property Length, LastWriteTimeUtc
Get-FileHash -LiteralPath '.\app\build\outputs\apk\debug\app-debug.apk' -Algorithm SHA256
git status --short
```

Record each command, exit code and generated test result. A release APK produced
by `assembleRelease` is an unsigned/local verification artifact unless a real
release signing and publication process is separately completed.

## Historical baseline — not current evidence

The following snapshot is retained only for traceability. It predates the
current fixes and cannot be used to claim that the present working tree passes.

| Historical item | Historical evidence |
| --- | --- |
| Scope | 2026-09-03 pre-remediation alpha snapshot |
| Local gate | `spotlessApply spotlessCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest` exited 0 |
| JVM result | 116 tests; 0 failures, 0 errors, 0 skipped |
| Secret scan | 147 candidate files; exit code 0 |
| Debug APK | 17,754,065 bytes; SHA-256 `033AFA27F691852D11E56BF86C8FF8B4082AE8BF44E47DA097BE7C6A1CCCC0E2` |
| CI milestone | Commit `bdb5d1c`, GitHub Actions run `33707389389`; historical debug artifact with 14-day retention |
| Device availability | `adb devices -l` returned an empty device list |

That historical gate did not include the current release lint/release assembly
requirements. Its test count, scanned-file count, artifact digest and CI run are
deliberately isolated here so they cannot be mistaken for final candidate data.

## Physical and real-network acceptance

No connected Mi Stick, physical C500/C510W or usable real camera LAN was
available for the current work. Every physical result therefore remains
`BLOCKED`; no result below may be promoted from build or fake evidence.

| Scenario | Status | Reason |
| --- | --- | --- |
| Connect and authorize exactly one Mi Stick over adb | BLOCKED | Mi Stick was unavailable |
| Run `connectedDebugAndroidTest` on the Mi Stick before credentials | BLOCKED | No connected physical Mi Stick was available |
| Install the current debug APK on the Mi Stick | BLOCKED | Mi Stick was unavailable |
| Verify measured clean start after explicitly approved data clear | BLOCKED | Mi Stick was unavailable; no `pm clear` was run |
| Discover C500 and C510W over ONVIF | BLOCKED | Physical cameras and their LAN were unavailable |
| Validate C500 `/stream2` | BLOCKED | The C500 and an in-app Camera Account entry were unavailable |
| Validate C510W `/stream2` | BLOCKED | The C510W and an in-app Camera Account entry were unavailable |
| Run two `/stream2` feeds together for 15 minutes | BLOCKED | Physical cameras and Mi Stick were unavailable |
| Change wall focus with the physical D-pad | BLOCKED | Mi Stick/remote was unavailable |
| Open C500 `/stream1` fullscreen with OK | BLOCKED | The C500 and Mi Stick were unavailable |
| Open C510W `/stream1` fullscreen with OK | BLOCKED | The C510W and Mi Stick were unavailable |
| Return to the wall and restore focus with Back | BLOCKED | Mi Stick/remote was unavailable |
| Background/foreground lifecycle recovery | BLOCKED | No physical Android TV device was available |
| Revoke and restore API 37 local-network permission during playback | BLOCKED | No API 37 device or real local stream was available |
| Wi-Fi loss and reconnection recovery | BLOCKED | No physical Android TV/network test setup was available |
| Wrong-password behavior with a real camera | BLOCKED | No physical camera or in-app credentials were available |
| Force-stop/relaunch timing and selection persistence | BLOCKED | No physical Android TV device was available |
| Endpoint UUID rematch after a DHCP address change | BLOCKED | Physical cameras and a controllable LAN were unavailable |
| Logcat credential/URI audit during real playback | BLOCKED | No connected adb device or real stream was available |
| Decoder and memory behavior under real concurrent streams | BLOCKED | Mi Stick and physical streams were unavailable |
| Three-stream physical/fake-device grid observation | BLOCKED | No executable physical device was available |

Automated tests may cover discovery parsing/deduplication, secure configuration,
URI encoding/redaction, retry/state reducers, layout calculations and lifecycle
coordination with fakes. Record their actual final result above, but do not use
them to replace any blocked physical scenario.
