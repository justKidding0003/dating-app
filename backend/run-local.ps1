$ErrorActionPreference = "Stop"

$backendRoot = $PSScriptRoot
$envFile = Join-Path $backendRoot ".env.local"
$mavenWrapper = Join-Path $backendRoot "mvnw.cmd"
$fallbackMaven = Join-Path $HOME ".m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd"

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing .env.local. Copy backend\.env.example to backend\.env.local and replace placeholder values."
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()

    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -lt 1) {
        throw "Invalid .env.local line: '$line'. Expected KEY=VALUE."
    }

    $name = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    $value = $value.Replace("\n", "`n")
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if ([string]::IsNullOrWhiteSpace($env:SPRING_PROFILES_ACTIVE)) {
    $env:SPRING_PROFILES_ACTIVE = "local"
}

$placeholderVariables = @(
    "IDENTITY_DB_PASSWORD",
    "IDENTITY_JWT_PRIVATE_KEY_PEM",
    "IDENTITY_JWT_PUBLIC_KEY_PEM",
    "IDENTITY_OTP_HMAC_SECRET",
    "IDENTITY_PHONE_HMAC_SECRET",
    "IDENTITY_REFRESH_TOKEN_HMAC_SECRET"
)

$unresolvedPlaceholders = $placeholderVariables | Where-Object {
    $value = [Environment]::GetEnvironmentVariable($_, "Process")
    [string]::IsNullOrWhiteSpace($value) -or $value.Contains("replace-with-local")
}

if ($unresolvedPlaceholders.Count -gt 0) {
    throw "Replace placeholder values in backend\.env.local before starting Spring Boot: $($unresolvedPlaceholders -join ', ')"
}

$mavenCommand = $mavenWrapper
if (-not (Test-Path -LiteralPath $mavenCommand)) {
    throw "Maven wrapper not found at $mavenCommand"
}

Push-Location $backendRoot
try {
    & $mavenCommand spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"

    if ($LASTEXITCODE -ne 0 -and (Test-Path -LiteralPath $fallbackMaven)) {
        Write-Warning "Maven wrapper failed. Retrying with the Maven distribution already installed by the wrapper."
        & $fallbackMaven spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"
    }

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
