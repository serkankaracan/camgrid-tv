# Architecture

KARACAM is a single-activity, single-module Android TV application with no
backend or background/foreground service. Discovery and playback are explicitly
foreground-gated; lightweight ViewModel state and connectivity collections may
remain registered while the activity is stopped.

```text
Compose TV screens
        |
immutable UI state + actions
        |
ViewModel / application container
   |          |             |
DataStore   WS-Discovery   PlaybackCoordinator
   |          |             |
Keystore   UDP + lock      Media3 RTSP players
```

## Package boundaries

- `model`: persistent non-secret domain values.
- `data`: DataStore codecs and repository implementations.
- `security`: credential validation/storage/recovery, redaction and discovery
  local-literal admission.
- `discovery`: hardened WS-Discovery probes, XML parser, deduplication and Android
  multicast transport.
- `playback`: credential-safe RTSP URI values, Media3 wrapper, state reducer,
  retry policy and centralized player ownership.
- `util`: RFC 1918 and active on-link route admission, socket-factory selection
  and RFC 3986 user-info encoding.
- `ui`: TV-specific screens, state/actions, lifecycle observation, focus behavior,
  wall layout and navigation.
- `app`: the manual dependency container, Application bootstrap and debug
  StrictMode configuration.

## Trust boundaries

Discovery datagrams and every advertised field are untrusted. Parsing is bounded,
namespace-aware and rejects DTD/external entities. XAddr and RTSP targets must pass
the local-address policy. A credential-bearing URI can be revealed only at the
Media3 boundary; its `toString` is redacted.

DataStore contains camera metadata and encrypted secret envelopes. Usernames and
passwords are serialized only into an AES/GCM payload with a fresh IV; the key is
non-exportable in Android Keystore. Keystore loss requires explicit recovery and
never permits plaintext fallback. Android backup is disabled.

## Player ownership

The playback coordinator owns at most one engine for each requested wall tile.
Switching to fullscreen releases the entire wall and creates one `/stream1`
player. If that primary stream is unsupported, cannot obtain a decoder, ends, or
otherwise fails, the same fullscreen session switches once to that camera's
`/stream2`; retries then remain on the fallback instead of oscillating between
qualities. Authentication and missing local-route failures do not trigger the
quality fallback. Returning recreates selected `/stream2` wall players and the
next fullscreen session tries `/stream1` again.

Players enable Media3's lower-priority decoder fallback. All video is hosted by
Media3's Compose-native `ContentFrame`, but the surface type follows the UI role.
The embedded setup preview and wall tiles use `TextureView` so Compose clipping,
rounded containers, status overlays and the five-dp focus border remain above the
video. A live wall tile renders its per-camera Live state once in the tile header;
only non-live states use the video overlay. The wall header's live count is a
separate aggregate. Fullscreen alone uses `SurfaceView`.

Fullscreen view state starts at `SAFE`: `ContentScale.Fit` inside a centered
viewport whose width and height are each 90% of the logical screen. `FIT` uses the
full logical viewport with `ContentScale.Fit`; `FILL` uses the full viewport with
`ContentScale.Crop`. D-pad Right or OK advances
`SAFE → FIT → FILL → SAFE`, while Left moves in reverse. Every mode preserves the
source pixel aspect ratio; only `FILL` crops edges. Playback status and the mode
control occupy the overscan-safe upper right without a shared enclosing panel.
The shadowed camera name is independently anchored at the overscan-safe bottom
right. Back navigation remains active without a persistent on-screen return hint.

Backgrounding, leaving the screen, or losing connectivity releases players and
cancels retries. Transient failures use bounded exponential backoff with jitter;
decoder exhaustion remains isolated to its slot.

## Local-network permission

The app targets API 37. On Android 17/API 37 and newer it declares and requests
`ACCESS_LOCAL_NETWORK` before opening discovery or RTSP sockets. API 36 and older
use the platform's `INTERNET`-permission compatibility behavior and are not shown a
nonexistent runtime permission prompt.
