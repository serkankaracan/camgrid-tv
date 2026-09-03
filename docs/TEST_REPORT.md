# Test report

Last updated: 2026-09-04

All evidence in this report must be secret-free. Do not record a home IP, camera
account, complete credential-bearing RTSP URI, MAC address, adb serial, pairing
code, raw adb/VLC log or private test-environment detail. A successful build,
port check, fake or emulator test is not physical camera or Mi Stick evidence.

## Current candidate verification

This section is the only source of truth for the current candidate. Local
evidence was measured on 2026-09-04 after the fullscreen fallback, embedded
surface/focus layering, overscan-safe viewport, single-Live wall treatment and
panel-free fullscreen layout changes. The exact implementation tree was
committed, pushed and observed in GitHub Actions. Do not copy a number, hash or
run ID from the historical section.

| Check | Status | Current evidence |
| --- | --- | --- |
| Repository revision | PASS | Implementation commit `6a3fc58ba166821d8a12ed6f56c8a85358f270c7` |
| Standard PowerShell quality gate | PASS | `invoke-quality-gate.ps1` exited 0; the required Gradle tasks, secret scan and diff check completed |
| Debug and release lint | PASS WITH WARNINGS | Each variant: 0 fatal, 0 error, 4 warnings: two upgrade notices plus unused and missing-density advisories from the unreferenced legacy banner |
| JVM unit tests | PASS | 33 suites; 166 tests; 0 failures, 0 errors, 0 skipped |
| Debug APK build | PASS | `assembleDebug` completed |
| Debug instrumented-test APK build | PASS | `assembleDebugAndroidTest` completed; compilation is not physical execution |
| Minified release APK build | PASS | `assembleRelease` completed; artifact is deliberately unsigned and is not a release claim |
| Secret hygiene | PASS | `Secret hygiene check passed for 160 files.` |
| Whitespace/diff validation | PASS | `git diff --check` exited 0; the `.gitignore` LF/CRLF message is a conversion warning, not a diff error |
| Debug APK size and SHA-256 | PASS | 17,126,597 bytes; `161557CC66A3E3F76E4751AB75568414AFB1AAE90390528A3DD76FBCEEF6C5E7` |
| APK identity, signing and alignment | PASS | Debug package is `io.github.serkankaracan.camgridtv.debug`, target SDK 37, Leanback launcher present; debug and test APKs verify with one v2 signer; all three APKs pass zip alignment |
| Product identity and TV banner | PASS | Packaged application label is `KARACAM`; manifest points to the new density-independent KARACAM vector banner; stable package ID is retained for in-place updates |
| adb inventory | BLOCKED | 0 connected rows; 0 authorized, unauthorized or offline devices; `connectedDebugAndroidTest` was not run |
| GitHub Actions | PASS | Android quality gate run [33807717262](https://github.com/serkankaracan/camgrid-tv/actions/runs/33807717262) passed in 6m 20s for the implementation commit; every recorded step completed successfully |

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

The user supplied an initial physical run of a pre-remediation APK: two camera
`/stream2` feeds rendered together, while the identified C510W entered a
reconnect loop only after fullscreen selected `/stream1`; fullscreen also
distorted on a TV whose display ratio differs from the stream. Photographs from
that stage showed that video could cover the left wall tile's app chrome/focus
treatment and that the camera's own top-left timestamp was clipped by the TV
edge. Input still selected the left tile despite the missing visual frame.

Later photographs from the `e720069` tree (`3a65112` application implementation)
provide narrow physical evidence for the intervening surface/focus and Safe-view
corrections. They predate the current `6a3fc58` single-Live, panel-free and
bottom-right-name layout, so they are not a physical pass for the current APK.
This development environment still has no adb-connected device; current-build
device automation and the exact-revision physical regression remain blocked or
not run as recorded below.

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

### `e720069` physical photo observations — superseded UI candidate

The raw `IMG_9754` and `IMG_9755` files are not committed because they contain
private environment details. Only these redacted, revision-scoped observations
are retained:

| Observation | Result for `e720069` | Evidence boundary |
| --- | --- | --- |
| Two live wall tiles in one frame | PASS | `IMG_9754` shows both camera images and their live UI state at the same instant; it does not measure duration |
| Visible wall focus treatment | PASS | `IMG_9754` shows a high-contrast focus frame around C500; it does not show a D-pad movement sequence or Back focus restoration |
| C500 fullscreen Safe frame | PASS | `IMG_9755` shows C500 rendering in the default Safe viewport; it does not identify the selected stream path or first-frame time |
| Camera timestamp inside the TV edge | PASS | `IMG_9755` shows the camera's top-left timestamp; it does not exercise Fit or Fill |
| Then-current app chrome | OBSERVED, SUPERSEDED | `IMG_9754` contains duplicate per-tile Live treatments and `IMG_9755` contains the old right-side panel; neither validates the current single-Live, panel-free, bottom-right-name UI |

The still photographs do not prove 15-minute stability, D-pad traversal,
Safe/Fit/Fill cycling, player continuity during mode changes, or the C510W
`/stream1` to `/stream2` compatibility fallback.

### Current `6a3fc58` candidate

| Scenario | Status | Reason |
| --- | --- | --- |
| Connect and authorize exactly one Mi Stick over adb | BLOCKED | No device is connected to this development environment |
| Run `connectedDebugAndroidTest` on the Mi Stick before credentials | BLOCKED | No adb-connected device is available in this development run |
| Install the current `6a3fc58` debug APK on the Mi Stick | BLOCKED | No device is connected to this development environment; the photographed `e720069` APK is not this candidate |
| Verify measured clean start after explicitly approved data clear | BLOCKED | No adb-connected device is available; no `pm clear` was run |
| Discover the two physical cameras over ONVIF | NOT RUN | The current `6a3fc58` APK has not yet been installed; earlier discovery is versioned evidence only |
| Validate C500 `/stream2` | NOT RUN | Prescribed corrected-candidate scenario was not reported |
| Validate C510W `/stream2` | NOT RUN | Earlier `/stream2` observations do not validate the current exact APK |
| Run two `/stream2` feeds together for 15 minutes | NOT RUN | `IMG_9754` is one instant, not a measured stability run |
| Change wall focus with the physical D-pad | NOT RUN | `IMG_9754` shows one visible frame but not the prescribed left/right movement sequence |
| Show each live wall tile's name and one header Live badge above video, with no duplicate bottom Live overlay | NOT RUN | `IMG_9754` predates the single-Live cleanup and visibly contains the superseded duplicate treatment |
| Verify browse-mode arrows, OK-to-edit, Back/IME Done and password masking | NOT RUN | Prescribed physical IME sequence was not reported |
| Verify adaptive Verify connection → N cameras action with real feeds | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Verify conditional header rescan, D-pad reachability and fresh discovery | NOT RUN | Prescribed corrected-candidate sequence was not reported |
| Verify redesigned UI safe areas/focus at 960x540 and 1280x720 | NOT RUN | Those two viewport measurements were not reported |
| Open C500 `/stream1` fullscreen with OK | NOT RUN | `IMG_9755` shows a prior-candidate C500 frame but not the current APK, stream path or first-frame time |
| Open C510W `/stream1` fullscreen with OK | NOT RUN | The current APK has not yet been installed; see the pre-fix failure above |
| Verify current C510W `/stream1` to `/stream2` fullscreen fallback | BLOCKED | The current APK has not yet been installed on the physical device |
| Preserve source aspect ratio in fullscreen | BLOCKED | `IMG_9755` shows only the prior candidate's Safe frame; exact-candidate physical mode coverage is pending |
| Keep the camera's top-left timestamp visible in Safe mode | NOT RUN | `IMG_9755` passed this visual subcriterion only on the prior `e720069` candidate |
| Show playback status and the view-mode control at upper right without an enclosing information panel | NOT RUN | The panel-free `6a3fc58` layout has not been checked on the physical TV |
| Show the shadowed camera name at bottom right with no persistent Back hint | NOT RUN | The bottom-right-name `6a3fc58` layout has not been checked on the physical TV |
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
tests also prescribe left-to-right wall focus movement, a single header Live
badge without the duplicate bottom overlay, centered 5% Safe margins, fullscreen
status/mode controls in the upper half, the camera name in the lower-right half,
full-viewport Fit and remote mode switching. Instrumented sources compile but
were not executed without a connected device; they do not prove real TextureView
layering, SurfaceView overscan or player continuity. None replaces the current
APK physical scenarios.
