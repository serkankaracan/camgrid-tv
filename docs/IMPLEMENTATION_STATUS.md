# Implementation status

Last updated: 2026-09-04

## Current candidate status

The `0.1.0-alpha.1` implementation and handoff documentation are present. The
current KARACAM candidate contains the issue #16 playback correction, wall
focus/overlay and fullscreen overscan corrections, plus the single-Live and
panel-free fullscreen layout cleanup. On 2026-09-04 its Windows command-line
quality gate passed: 166 JVM tests passed, both lint variants completed with no
errors, and debug, instrumented-test and minified unsigned release APKs were
assembled. Exact local evidence is recorded in `docs/TEST_REPORT.md`. The
implementation revision is `6a3fc58`; GitHub Actions run `33807717262` passed in
6m 20s.

A user-run pre-fix physical test confirmed that two camera `/stream2` feeds can
play together and exposed the C510W `/stream1` fullscreen reconnect loop. Early
photographs also exposed video covering wall focus/chrome and TV overscan
clipping the camera timestamp. Later `IMG_9754` and `IMG_9755` photographs from
the `e720069` tree (`3a65112` application implementation) verify only two live
wall tiles in one frame, a visible C500 focus frame, and a C500 fullscreen Safe
frame with its timestamp and then-current right-side panel visible. They do not
verify elapsed stability, D-pad movement, the view-mode cycle or C510W fallback.

The current `6a3fc58` source retains the one-way `/stream1` to `/stream2`
compatibility fallback, deterministic embedded-video layering, strong wall focus
and aspect-preserving Safe/Fit/Fill modes. It removes the duplicate bottom Live
overlay, removes the enclosing fullscreen information panel, keeps status/mode
controls at the safe upper right and moves the shadowed camera name to the safe
bottom right. Physical verification of this exact APK remains pending; build and
JVM results never replace it.

## Implemented scope

- Phase 0: public repository, project contract, Apache-2.0 license and repository
  hygiene.
- Phase 1: Android TV shell, Windows command-line build workflow, D-pad-first
  Compose structure, Turkish/English resources and browse/edit text fields where
  OK/Enter opens the IME and Back/Done restores directional navigation.
- Phase 2: camera models, DataStore persistence, credential profiles, AES/GCM
  Android Keystore abstraction, validation and redaction.
- Phase 3: local-network permission policy, ONVIF WS-Discovery probe/parser,
  multicast transport, deduplication, identity matching and discovery repository.
- Phase 4: redesigned control-room discovery/setup, camera selection states,
  shared/per-camera credential readiness and one adaptive Verify → Watch action.
- Phase 5: Media3 RTSP layer, safe URI construction, `/stream2` preview behavior,
  decoder fallback, safe state mapping and fake-player tests.
- Phase 6: redesigned dynamic wall/fullscreen chrome, live counter, conditional
  focusable header rescan, `/stream2` wall, `/stream1` fullscreen with one-way
  `/stream2` compatibility fallback, TextureView-backed embedded chrome/focus,
  one header Live badge per live wall tile, panel-free upper-right fullscreen
  status/mode controls, a bottom-right camera name, Safe/Fit/Fill source-aspect
  modes and resource coordination.
- Phase 7: retry/backoff, connectivity monitoring, lifecycle recovery, decoder
  error isolation, keep-screen-on control and debug diagnostics.
- Phase 8: redacted acceptance matrix and Windows/PowerShell physical-device
  plan. A pre-fix user run and narrow `e720069` photo observations exist; the
  current exact-candidate regression and remaining measured matrix are pending.
- Phase 9: Turkish-first README, architecture/troubleshooting documents,
  changelog, roadmap and third-party notices. No tag or GitHub Release is
  claimed.

## Current verification contract

The PowerShell quality gate now checks formatting, both debug and release lint,
JVM tests, debug APK assembly, debug instrumented-test APK assembly, minified
release assembly, secret hygiene and whitespace:

```powershell
.\scripts\invoke-quality-gate.ps1 `
    -JavaHomePath $env:JAVA_HOME `
    -SdkRootPath "$PWD\.android-sdk"
```

`assembleDebugAndroidTest` proves only that the instrumented-test sources and APK
compile. It does not execute them. With exactly one authorized physical Mi Stick
connected, `connectedDebugAndroidTest` must run before any Camera Account
credential scenario, as described in `docs/MANUAL_TEST_PLAN_TR.md`.

Current integrated evidence:

| Evidence | Status |
| --- | --- |
| Final integrated quality gate | PASS; PowerShell gate exited 0 |
| Debug and release lint | PASS WITH WARNINGS; each variant had 0 fatal, 0 error and 4 warnings |
| Final JVM test count | PASS; 33 suites, 166 tests, 0 failures/errors/skipped |
| Secret hygiene | PASS; 160 candidate files scanned |
| Final debug APK size and SHA-256 | PASS; 17,126,597 bytes, `161557CC66A3E3F76E4751AB75568414AFB1AAE90390528A3DD76FBCEEF6C5E7` |
| APK signing and alignment | PASS; debug and test APKs have one v2 signer; all three APKs pass zip alignment |
| Final implementation revision | PASS; `6a3fc58ba166821d8a12ed6f56c8a85358f270c7` committed and pushed |
| GitHub Actions | PASS; run [33807717262](https://github.com/serkankaracan/camgrid-tv/actions/runs/33807717262) completed successfully in 6m 20s |
| `e720069` physical photographs | PARTIAL; two live wall tiles, one visible focus frame, and C500 Safe/timestamp/old-panel appearance only |
| Physical Mi Stick instrumented tests | BLOCKED; adb inventory contained 0 device rows |
| Current `6a3fc58` camera and Android TV regression | PENDING user retest of the exact APK |

## Historical milestones — not current evidence

The following references describe earlier repository snapshots only:

- Phase 7 commit `7e945c8` had an observed passing GitHub Actions run.
- Phase 8 commit `bdb5d1c` had observed passing GitHub Actions run
  `33707389389` and a temporary debug artifact.
- Pre-interface candidate `42d73d2` passed GitHub Actions run `33729134264`.
- Prior implementation `3a65112` passed GitHub Actions run `33803695693`; its
  `e720069` documentation tree has the limited physical photo observations
  described above, not current-candidate acceptance.
- The exact historical unit-test count, scanned-file count and APK hash retained
  in `docs/TEST_REPORT.md` apply only to that pre-remediation record. They do not
  describe the current working tree.

Do not copy historical hashes, counts or run IDs into current release notes.
Use only output captured after the final integrated build and, for CI, after the
corresponding revision is pushed and observed.

## Remaining external validation

Install the current `6a3fc58` debug APK without clearing app data and follow the
focused wall frame/overlay, single header Live badge, panel-free upper-right
status/mode controls, bottom-right camera name, Safe/Fit/Fill and C510W fallback
scenarios in the manual plan. When adb is available, also authorize exactly one
device and run `connectedDebugAndroidTest` before the remaining credential
scenarios. Do not create a compatibility claim, release tag or GitHub Release
until the exact APK and remaining physical matrix have redacted evidence.
