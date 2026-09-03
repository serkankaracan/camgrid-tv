# Implementation status

Last updated: 2026-09-03

## Current candidate status

The `0.1.0-alpha.1` implementation and handoff documentation are present. The
current KARACAM candidate contains the issue #16 playback correction. On
2026-09-03 its Windows command-line quality gate passed: 163 JVM tests passed,
both lint variants completed with no errors, and debug, instrumented-test and
minified unsigned release APKs were assembled. Exact local evidence is recorded
in `docs/TEST_REPORT.md`. The implementation revision and GitHub Actions result
are populated only after this tree is committed, pushed and observed. Earlier
commit `de43abc` and run `33785512583` are historical evidence, not evidence for
this candidate.

A user-run pre-fix physical test confirmed that two camera `/stream2` feeds can
play together and exposed two regressions: the C510W `/stream1` fullscreen feed
looped through reconnects, and fullscreen could distort on a differently shaped
TV. The current source adds a one-way `/stream1` to `/stream2` compatibility
fallback and a Compose-native fitted surface. Physical verification of this
corrected APK remains pending; build and JVM results never replace it.

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
  `/stream2` compatibility fallback, source-aspect fitting and resource
  coordination.
- Phase 7: retry/backoff, connectivity monitoring, lifecycle recovery, decoder
  error isolation, keep-screen-on control and debug diagnostics.
- Phase 8: redacted acceptance matrix and Windows/PowerShell physical-device
  plan. A pre-fix user run exists; corrected-candidate regression and the
  remaining measured matrix are pending.
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
| Final JVM test count | PASS; 32 suites, 163 tests, 0 failures/errors/skipped |
| Final debug APK size and SHA-256 | PASS; 17,125,741 bytes, `612BAE7ED1D87924EA9A2119E2E8C766B33B5E8DBD931460E66AFB9895947A68` |
| Final revision and GitHub Actions run | PENDING commit, push and observation |
| Physical Mi Stick instrumented tests | BLOCKED; no adb-connected device in this run |
| Corrected camera and Android TV regression | PENDING user retest |

## Historical milestones — not current evidence

The following references describe earlier repository snapshots only:

- Phase 7 commit `7e945c8` had an observed passing GitHub Actions run.
- Phase 8 commit `bdb5d1c` had observed passing GitHub Actions run
  `33707389389` and a temporary debug artifact.
- Pre-interface candidate `42d73d2` passed GitHub Actions run `33729134264`.
- The exact historical unit-test count, scanned-file count and APK hash retained
  in `docs/TEST_REPORT.md` apply only to that pre-remediation record. They do not
  describe the current working tree.

Do not copy historical hashes, counts or run IDs into current release notes.
Use only output captured after the final integrated build and, for CI, after the
corresponding revision is pushed and observed.

## Remaining external validation

Install the corrected APK without clearing app data and follow the focused
C510W fullscreen/fallback and aspect-ratio scenarios in the manual plan. When adb
is available, also authorize exactly one device and run `connectedDebugAndroidTest`
before the remaining credential scenarios. Do not create a compatibility claim,
release tag or GitHub Release until the corrected APK and remaining physical
matrix have redacted evidence.
