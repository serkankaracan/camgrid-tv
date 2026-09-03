# Roadmap

- [x] Phase 0 — public repository and project contract
- [x] Phase 1 — buildable Android TV shell and CI definition
- [x] Phase 2 — models, DataStore and encrypted credentials
- [x] Phase 3 — ONVIF WS-Discovery and local-network infrastructure
- [x] Phase 4 — camera setup and selection
- [x] Phase 5 — Media3 RTSP integration and automated proof
- [x] Phase 6 — dynamic wall and fullscreen
- [x] Phase 7 — lifecycle, connectivity and recovery hardening
- [ ] Phase 8 — physical C500/C510W and Mi Stick validation (IN PROGRESS: corrected
  wall/C500 still frames observed; latest chrome and measured matrix pending)
- [x] Phase 9 — documentation and alpha handoff

Phases 1–7 have passed the local automated quality gate. Phase 5's checked state
means the implementation and fake/automated tests are complete; it does not claim
successful playback from a physical camera. An initial user-run physical pass
confirmed two concurrent wall streams and identified a C510W primary-stream
fullscreen reconnect/playback failure. Later `e720069` photographs show two
simultaneous wall frames, one visible focus frame, and a C500 Safe fullscreen
frame with its timestamp visible; they do not prove timed stability, D-pad focus
movement, the view-mode cycle or the C510W fallback. Phase 8 remains required for
the latest panel-free, bottom-right-name APK and the full acceptance matrix before
compatibility can be reported as verified. The alpha remains unreleased until
that regression pass is complete.
