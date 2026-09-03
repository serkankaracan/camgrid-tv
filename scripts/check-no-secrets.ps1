[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
Push-Location -LiteralPath $repositoryRoot

try {
    function ConvertTo-RepositoryRelativePath {
        param([Parameter(Mandatory = $true)][string]$Path)

        $fullPath = [System.IO.Path]::GetFullPath($Path)
        $rootPrefix = $repositoryRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar) +
            [System.IO.Path]::DirectorySeparatorChar
        if (-not $fullPath.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Scan result is outside the repository root."
        }

        return $fullPath.Substring($rootPrefix.Length).Replace('\', '/')
    }

    $candidateFiles = @(
        git ls-files --cached --others --exclude-standard |
            Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) }
    )

    $problems = [System.Collections.Generic.List[string]]::new()
    $forbiddenFilePatterns = @(
        '(?i)(^|/)(local\.properties|\.local-test-config(?:\..*)?)$',
        '(?i)\.(jks|keystore|p12|pem|key|apk|aab|apks|log)$',
        '(?i)(^|/)(adb-logcat|vlc-log)'
    )

    foreach ($relativePath in $candidateFiles) {
        $normalizedPath = $relativePath -replace '\\', '/'
        foreach ($pattern in $forbiddenFilePatterns) {
            if ($normalizedPath -match $pattern) {
                $problems.Add("forbidden file: $normalizedPath")
                break
            }
        }
    }

    $textPatterns = @{
        'credential-bearing RTSP URI' = '(?i)rtsp://[^\s/:@]+:[^\s@]+@'
        'private key block' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
        'GitHub token shape' = '(?i)(?:gh[opusr]_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,})'
    }

    foreach ($entry in $textPatterns.GetEnumerator()) {
        $matchingFiles = @(
            Select-String -LiteralPath $candidateFiles -Pattern $entry.Value -List -ErrorAction SilentlyContinue |
                ForEach-Object { $_.Path } |
                Sort-Object -Unique
        )
        foreach ($matchingFile in $matchingFiles) {
            $relativeMatch = ConvertTo-RepositoryRelativePath -Path $matchingFile
            $problems.Add("$($entry.Key): $relativeMatch")
        }
    }

    $localForbiddenTokens = @(
        $env:CAMGRID_FORBIDDEN_TOKENS -split ';' |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ }
    )
    foreach ($token in $localForbiddenTokens) {
        $matchingFiles = @(
            Select-String -LiteralPath $candidateFiles -SimpleMatch -Pattern $token -List -ErrorAction SilentlyContinue |
                ForEach-Object { $_.Path } |
                Sort-Object -Unique
        )
        foreach ($matchingFile in $matchingFiles) {
            $relativeMatch = ConvertTo-RepositoryRelativePath -Path $matchingFile
            $problems.Add("local forbidden token: $relativeMatch")
        }
    }

    if ($problems.Count -gt 0) {
        Write-Error ("Secret hygiene check failed:`n - " + ($problems -join "`n - "))
    }

    Write-Output "Secret hygiene check passed for $($candidateFiles.Count) files."
}
finally {
    Pop-Location
}
