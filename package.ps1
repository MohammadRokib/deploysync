$ErrorActionPreference = "Stop"

$appModule  = "deploysync-app"
$mainClass  = "com.deploysync.app.Launcher"
$appName    = "DeploySync"

# Detect OS for naming and icons
if ($IsWindows) {
    $osSuffix = "win64"
    $iconArgs = @("--icon", "images/icon.ico")
} elseif ($IsMacOS) {
    $osSuffix = "mac64"
    # macOS requires .icns, omitting for now unless you create one
    $iconArgs = @()
} else {
    $osSuffix = "linux64"
    $iconArgs = @("--icon", "images/app-icon-256.png")
}

Write-Host "==> Reading app version from pom.xml"
$appVersion = (mvn -q -f pom.xml help:evaluate "-Dexpression=project.version" -DforceStdout).Trim()
if ([string]::IsNullOrWhiteSpace($appVersion)) { throw "Failed to read project.version from pom.xml" }
Write-Host "    version: $appVersion"
$mainJar    = "$appModule-$appVersion.jar"

$inputDir   = "$appModule/target/app-image-input"
$runtimeDir = "$appModule/target/runtime"
$distDir    = "$appModule/target/dist"

function Remove-DirectoryRobust($path) {
    # Freshly written .exe files are briefly locked by Windows Defender's real-time
    # scan right after jpackage creates them. A plain Remove-Item can lose that race -
    # retry with a short backoff instead of failing on the first transient lock.
    if (-not (Test-Path $path)) { return }
    for ($i = 0; $i -lt 6; $i++) {
        try {
            Remove-Item -Recurse -Force $path -ErrorAction Stop
            return
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    Remove-Item -Recurse -Force $path
}

function Invoke-Traced {
    param(
        [Parameter(Mandatory = $true)][string]$Exe,
        [Parameter(Mandatory = $false)][string[]]$ExeArgs = @()
    )
    Write-Host "$Exe $($ExeArgs -join ' ')" -ForegroundColor Green
    & $Exe @ExeArgs
}

# Clear last run's app-image BEFORE "mvn clean" walks the same target tree - otherwise
# a locked leftover DeploySync.exe makes maven-clean-plugin fail outright.
Write-Host "==> Clearing previous app-image"
Remove-DirectoryRobust $distDir

Write-Host "==> mvn clean install"
Invoke-Traced -Exe "mvn" -ExeArgs @("clean", "install")
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "==> Detecting required JDK modules via jdeps"
$jars = (Get-ChildItem "$inputDir/*.jar").FullName
$cp = $jars -join (([IO.Path]::PathSeparator).ToString())
$jdepsArgs = @("--multi-release", "25", "--ignore-missing-deps", "--print-module-deps", "--class-path", $cp) + $jars
$detected = (Invoke-Traced -Exe "jdeps" -ExeArgs $jdepsArgs 2>$null | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($detected)) { throw "jdeps failed to compute module list" }

# jdk.zipfs is never detected by jdeps: FileSystems.newFileSystem(Path) resolves the zip
# filesystem provider dynamically at runtime (via ServiceLoader), which static bytecode
# analysis cannot see. This app opens .ear/.war/.jar archives via that exact API, so
# jdk.zipfs is a hard requirement - always append it, regardless of what jdeps reports.
$moduleList = "$detected,jdk.zipfs"
Write-Host "    modules: $moduleList"

$javaExePath = (Get-Command java).Source
$javaHome = Split-Path (Split-Path $javaExePath -Parent) -Parent
$jmodsDir = Join-Path $javaHome "jmods"

Write-Host "==> jlink"
Remove-DirectoryRobust $runtimeDir
Invoke-Traced -Exe "jlink" -ExeArgs @(
    "--module-path", $jmodsDir,
    "--add-modules", $moduleList,
    "--output", $runtimeDir,
    "--strip-debug", "--no-header-files", "--no-man-pages"
)
if ($LASTEXITCODE -ne 0) { throw "jlink failed" }

Write-Host "==> jpackage"
Remove-DirectoryRobust $distDir
$jpackageArgs = @(
    "--type", "app-image",
    "--name", $appName,
    "--app-version", $appVersion,
    "--input", $inputDir,
    "--main-jar", $mainJar,
    "--main-class", $mainClass,
    "--runtime-image", $runtimeDir,
    "--dest", $distDir
) + $iconArgs

Invoke-Traced -Exe "jpackage" -ExeArgs $jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host "==> Creating release zip"
$zipPath = "$distDir/$appName-v$appVersion-$osSuffix.zip"
$zipSource = "$distDir/$appName"
if ($IsMacOS) { $zipSource += ".app" } # macOS outputs a .app bundle

Write-Host "Compress-Archive -Path $zipSource -DestinationPath $zipPath -Force" -ForegroundColor Green
Compress-Archive -Path $zipSource -DestinationPath $zipPath -Force

Write-Host "==> Release zip: $zipPath"
