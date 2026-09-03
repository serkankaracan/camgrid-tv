[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$SdkManagerPath,

    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe') -PathType Leaf })]
    [string]$JavaHomePath,

    [string]$SdkRootPath = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..')) '.android-sdk'),

    [switch]$AcceptLicenses
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$resolvedSdkManager = (Resolve-Path -LiteralPath $SdkManagerPath).Path
$resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHomePath).Path

New-Item -ItemType Directory -Path $SdkRootPath -Force | Out-Null
$resolvedSdkRoot = (Resolve-Path -LiteralPath $SdkRootPath).Path

$env:JAVA_HOME = $resolvedJavaHome
$env:ANDROID_HOME = $resolvedSdkRoot
$env:ANDROID_SDK_ROOT = $resolvedSdkRoot

if ($AcceptLicenses) {
    1..20 | ForEach-Object { 'y' } |
        & $resolvedSdkManager --sdk_root=$resolvedSdkRoot --licenses |
        Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Android SDK license acceptance failed with exit code $LASTEXITCODE."
    }
}

& $resolvedSdkManager `
    --sdk_root=$resolvedSdkRoot `
    'platform-tools' `
    'platforms;android-37.0' `
    'build-tools;36.0.0' `
    'cmdline-tools;latest'
if ($LASTEXITCODE -ne 0) {
    throw "Android SDK package installation failed with exit code $LASTEXITCODE."
}

$portableSdkPath = $resolvedSdkRoot.Replace('\', '/').Replace(':', '\:')
Set-Content `
    -LiteralPath (Join-Path $projectRoot 'local.properties') `
    -Value "sdk.dir=$portableSdkPath" `
    -Encoding UTF8

Write-Output "Android SDK is ready at $resolvedSdkRoot"
Write-Output 'Run .\scripts\invoke-quality-gate.ps1 from PowerShell.'
