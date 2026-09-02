# Test report

All evidence in this report is secret-free. No home IP, camera account, full RTSP
URI, MAC address, adb log or VLC log is committed.

| Area | Status | Evidence |
| --- | --- | --- |
| Phase 0 repository checks | PASS | Secret scan and whitespace check exited 0 |
| JVM unit tests | NOT RUN | Android project not created yet |
| Android lint | NOT RUN | Android project not created yet |
| Debug APK | NOT RUN | Android project not created yet |
| Connected Android tests | NOT RUN | No CLI emulator/device checked yet |
| Target camera discovery | BLOCKED | Requires same LAN and physical cameras |
| Target camera RTSP | BLOCKED | Requires credentials entered only in app |
| Mi Stick acceptance | BLOCKED | Requires paired physical device |
| GitHub authentication | PASS | Account/scopes and repository-name availability verified |
| GitHub Actions | NOT RUN | Workflow and remote not created yet |

## Physical acceptance checklist

Physical runs must record only PASS, FAIL, BLOCKED or NOT RUN plus redacted
evidence for discovery, dual `/stream2` wall playback, `/stream1` fullscreen,
D-pad/focus restoration, 15-minute stability, lifecycle recovery, network
recovery, wrong-password handling, persistence, redacted logs and decoder/memory
behavior.
