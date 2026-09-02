# Security policy

Report vulnerabilities privately through GitHub's security-advisory feature once
the public repository is available. Do not open a public issue containing camera
credentials, network addresses, full RTSP URIs, device identifiers or logs.

CamGrid TV accepts only local-network camera targets and stores credential material
encrypted on the Android device with a Keystore-held key. It intentionally has no
cloud service, analytics or telemetry. Never expose camera ports to the internet.
