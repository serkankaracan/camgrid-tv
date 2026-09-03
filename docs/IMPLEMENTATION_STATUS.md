# Implementation status

Last updated: 2026-09-03

## Current candidate status

The `0.1.0-alpha.1` implementation and handoff documentation are present. The
final integrated source tree passed the Windows command-line quality gate on
2026-09-03: 151 JVM tests passed, both lint variants completed without errors,
and debug, instrumented-test and minified unsigned release APKs were assembled.
The exact local evidence is recorded in `docs/TEST_REPORT.md`. Implementation
commit `de43abc` passed GitHub Actions run `33785512583` in 7m 56s.

Physical validation remains **BLOCKED** because no C500, C510W, Mi Stick,
emulator or connected adb device is available. Build, JVM, fake-player and port
reachability results are never treated as physical playback evidence.

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
  safe state mapping and fake-player tests. Real camera playback remains blocked.
- Phase 6: redesigned dynamic wall/fullscreen chrome, live counter, conditional
  focusable header rescan, `/stream2` wall and `/stream1` fullscreen resource
  coordination.
- Phase 7: retry/backoff, connectivity monitoring, lifecycle recovery, decoder
  error isolation, keep-screen-on control and debug diagnostics.
- Phase 8: redacted acceptance matrix and Windows/PowerShell physical-device
  plan. Hardware execution remains blocked.
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
| Final JVM test count | PASS; 151 tests, 0 failures/errors/skipped |
| Final debug APK size and SHA-256 | PASS; 17,088,767 bytes, `8656B1A0DD234133EBDE923BD23FF02D4B9A0F7D42A2A4FF9D23E3729E2B5737` |
| Final revision and GitHub Actions run | PASS; implementation `de43abc`, run `33785512583` |
| Physical Mi Stick instrumented tests | BLOCKED |
| Physical camera and Android TV acceptance | BLOCKED |

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

When hardware is available, follow the manual plan in order: connect and
authorize a single Mi Stick, run `connectedDebugAndroidTest` before entering
credentials, explicitly acknowledge any uninstall/`pm clear` data loss, close
other RTSP clients, and execute every physical scenario with redacted evidence.
Until then, do not create a compatibility claim, release tag or GitHub Release
based on build, fake-player or port-reachability evidence alone.
