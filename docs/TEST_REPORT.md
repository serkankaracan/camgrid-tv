# Test report

Last updated: 2026-09-03

All evidence in this report must be secret-free. Do not record a home IP, camera
account, complete credential-bearing RTSP URI, MAC address, adb serial, pairing
code, raw adb/VLC log or private test-environment detail. A successful build,
port check, fake or emulator test is not physical camera or Mi Stick evidence.

## Current candidate verification

This section is the only source of truth for the current candidate. Local
evidence was measured on 2026-09-03 after the fullscreen fallback, embedded
surface/focus layering, overscan-safe viewport and KARACAM branding changes.
The exact implementation tree was committed, pushed and observed in GitHub
Actions. Do not copy a number, hash or run ID from the historical section.

| Check | Status | Current evidence |
| --- | --- | --- |
| Repository revision | PASS | Implementation commit `3a65112746ff5db9fd73dcca4ce1342924b1265a` |
| Standard PowerShell quality gate | PASS | `invoke-quality-gate.ps1` exited 0; the required Gradle tasks, secret scan and diff check completed |
| Debug and release lint | PASS WITH WARNINGS | Each variant: 0 fatal, 0 error, 4 warnings: two upgrade notices plus unused and missing-density advisories from the unreferenced legacy banner |
| JVM unit tests | PASS | 33 suites; 166 tests; 0 failures, 0 errors, 0 skipped |
| Debug APK build | PASS | `assembleDebug` completed |
| Debug instrumented-test APK build | PASS | `assembleDebugAndroidTest` completed; compilation is not physical execution |
| Minified release APK build | PASS | `assembleRelease` completed; artifact is deliberately unsigned and is not a release claim |
| Secret hygiene | PASS | `Secret hygiene check passed for 160 files.` |
| Whitespace/diff validation | PASS | `git diff --check` exited 0; the `.gitignore` LF/CRLF message is a conversion warning, not a diff error |
| Debug APK size and SHA-256 | PASS | 17,126,597 bytes; `431CAAF98497B93E2A4410B63DA90BC31B09F38149EE3813DA3CBA5244E59002` |
| APK identity, signing and alignment | PASS | Debug package is `io.github.serkankaracan.camgridtv.debug`, target SDK 37, Leanback launcher present; debug and test APKs verify with one v2 signer; all three APKs pass zip alignment |
| Product identity and TV banner | PASS | Packaged application label is `KARACAM`; manifest points to the new density-independent KARACAM vector banner; stable package ID is retained for in-place updates |
| adb inventory | BLOCKED | 0 connected rows; 0 authorized, unauthorized or offline devices; `connectedDebugAndroidTest` was not run |
| GitHub Actions | PASS | Android quality gate run [33803695693](https://github.com/serkankaracan/camgrid-tv/actions/runs/33803695693) passed in 6m 1s for the implementation commit |

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

The user supplied a physical run of the previous APK: two camera `/stream2`
feeds rendered together, while the identified C510W entered a reconnect loop
only after fullscreen selected `/stream1`; fullscreen also distorted on a TV
whose display ratio differs from the stream. Later photographs of that APK show
that video could cover the left wall tile's app chrome/focus treatment and that
the camera's own top-left timestamp was clipped by the physical TV edge in
fullscreen. Input still selected the left tile despite the missing visual frame.
These are valid, secret-free pre-fix observations and explain the current work,
but they are not evidence that the corrected APK passes. This development
environment still has no adb-connected device, so current-build device
automation and regression checks remain blocked.

### Pre-fix user observations

| Observation | Result |
| --- | --- |
| ONVIF discovery | Two cameras were found; C510W was explicitly identified |
| Concurrent wall playback | Both `/stream2` feeds displayed together; duration was not measured |
| C510W wall playback | `/stream2` displayed normally |
| C510W fullscreen | `/stream1` cycled through connecting/reconnecting and did not show live video |
| Fullscreen geometry | Video was vertically distorted on the user's differently shaped TV |
| Wall focus and app chrome | The left tile remained selectable, but its visible focus frame, camera name and status chrome were missing in the supplied photograph |
| Fullscreen edge visibility | The camera's own top-left date/time was clipped at the physical TV edge; KARACAM camera/status text was still on the left |

### Current corrected candidate

| Scenario | Status | Reason |
| --- | --- | --- |
| Connect and authorize exactly one Mi Stick over adb | BLOCKED | No device is connected to this development environment |
| Run `connectedDebugAndroidTest` on the Mi Stick before credentials | BLOCKED | No adb-connected device is available in this development run |
| Install the current corrected debug APK on the Mi Stick | BLOCKED | No device is connected to this development environment; user retest pending |
| Verify measured clean start after explicitly approved data clear | BLOCKED | No adb-connected device is available; no `pm clear` was run |
| Discover the two physical cameras over ONVIF | NOT RUN | Corrected APK has not yet been installed; see pre-fix observation above |
| Validate C500 `/stream2` | NOT RUN | Prescribed corrected-candidate scenario was not reported |
| Validate C510W `/stream2` | NOT RUN | Corrected APK has not yet been installed; pre-fix `/stream2` passed |
| Run two `/stream2` feeds together for 15 minutes | NOT RUN | Pre-fix feeds displayed together, but no measured 15-minute run was reported |
| Change wall focus with the physical D-pad | NOT RUN | Corrected APK must show exactly one thick frame as focus moves left and right; pre-fix input-only selection is insufficient |
| Keep both wall tiles' camera name/status above video | NOT RUN | Corrected TextureView-backed wall has not been checked on the physical TV |
| Verify browse-mode arrows, OK-to-edit, Back/IME Done and password masking | NOT RUN | Prescribed physical IME sequence was not reported |
| Verify adaptive Verify connection → N cameras action with real feeds | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Verify conditional header rescan, D-pad reachability and fresh discovery | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Verify redesigned UI safe areas/focus at 960x540 and 1280x720 | NOT RUN | Those two viewport measurements were not reported |
| Open C500 `/stream1` fullscreen with OK | NOT RUN | Prescribed corrected-candidate scenario was not reported |
| Open C510W `/stream1` fullscreen with OK | NOT RUN | Corrected APK has not yet been installed; see pre-fix failure above |
| Verify current C510W `/stream1` to `/stream2` fullscreen fallback | BLOCKED | Corrected APK has not yet been installed on the physical device |
| Preserve source aspect ratio in fullscreen | BLOCKED | Corrected Compose surface retest is pending; pre-fix geometry failed |
| Keep the camera's top-left timestamp visible in Safe mode | NOT RUN | The centered 90% viewport requires a physical overscan retest |
| Show KARACAM camera/status/mode chrome at upper right with no Back hint | NOT RUN | Corrected fullscreen panel has not been checked on the physical TV |
| Cycle Safe/Fit/Fill with Left, Right and OK without reconnecting | NOT RUN | The D-pad sequence and live-player continuity require a physical retest |
| Return to the wall and restore focus with Back | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Background/foreground lifecycle recovery | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Revoke and restore API 37 local-network permission during playback | BLOCKED | Requires an adb-connected API 37 device |
| Wi-Fi loss and reconnection recovery | NOT RUN | Prescribed controlled-network sequence was not reported |
| Wrong-password behavior with a real camera | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Force-stop/relaunch timing and selection persistence | BLOCKED | Requires an adb-connected device for measured evidence |
| Endpoint UUID rematch after a DHCP address change | NOT RUN | A controlled DHCP-change sequence was not reported |
| Logcat credential/URI audit during real playback | BLOCKED | Requires an adb-connected device and current real stream |
| Decoder and memory behavior under real concurrent streams | BLOCKED | Requires an adb-connected device for measured evidence |
| Three-stream physical/fake-device grid observation | NOT RUN | A three-stream physical scenario was not reported |

Automated tests cover discovery parsing/deduplication, secure configuration, URI
encoding/redaction, retry/state reducers, layout calculations, lifecycle
coordination, setup primary-action policy, fake UI focus/state behavior, the
one-way fullscreen fallback and the Safe/Fit/Fill state cycle. They verify
eligible primary failures switch to secondary, authentication/route failures do
not, fallback retries stay secondary, Back cancels pending fallback work, and a
new fullscreen session tries primary again. The compiled Compose instrumented
test also prescribes left-to-right wall focus movement, centered 5% Safe margins,
full-viewport Fit and remote mode switching. Instrumented sources compile but
were not executed without a connected device; they do not prove real TextureView
layering, SurfaceView overscan or player continuity. None replaces the corrected
APK physical scenarios.
