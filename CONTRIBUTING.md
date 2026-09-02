# Contributing

Use Windows PowerShell and JDK 17. Android Studio is optional; the Gradle wrapper,
Android SDK command-line tools and VS Code are sufficient.

Before a change, read `AGENTS.md` and `docs/IMPLEMENTATION_STATUS.md`. Keep commits
small and use Conventional Commits. Run:

```powershell
.\gradlew.bat --no-daemon spotlessCheck lintDebug testDebugUnitTest assembleDebug
.\scripts\check-no-secrets.ps1
git diff --check
```

Never place camera credentials, private environment addresses, complete RTSP URIs,
signing keys or captured device logs in source, tests, issues or pull requests.
