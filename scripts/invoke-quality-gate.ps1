[CmdletBinding()]
param(
    [string]$JavaHomePath = $env:JAVA_HOME,
    [string]$SdkRootPath = $env:ANDROID_HOME
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

if ($JavaHomePath) {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHomePath).Path
}
if ($SdkRootPath) {
    $resolvedSdkRoot = (Resolve-Path -LiteralPath $SdkRootPath).Path
    $env:ANDROID_HOME = $resolvedSdkRoot
    $env:ANDROID_SDK_ROOT = $resolvedSdkRoot
}

Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat `
        --no-daemon `
        spotlessCheck `
        lintDebug `
        lintRelease `
        testDebugUnitTest `
        assembleDebug `
        assembleDebugAndroidTest `
        assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle quality gate failed with exit code $LASTEXITCODE."
    }

    & .\scripts\check-no-secrets.ps1
    if ($LASTEXITCODE -ne 0) {
        throw "Secret hygiene check failed with exit code $LASTEXITCODE."
    }

    git diff --check
    if ($LASTEXITCODE -ne 0) {
        throw "Git whitespace check failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Write-Output 'CamGrid TV quality gate passed.'
