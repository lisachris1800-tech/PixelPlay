param([switch]$SkipSdk)
$ErrorActionPreference = "Continue"
$Project = "C:\Users\SAAD REHMAN\Desktop\AndroidSecurityProject"
$SdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$GradleVer = "8.5"
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.10.7-hotspot"

Write-Host "=== PixelPlay APK Builder ===" -ForegroundColor Cyan

# Step 1: Download gradle-wrapper.jar if needed
$WrapperJar = "$Project\gradle\wrapper\gradle-wrapper.jar"
$GradlewBat = "$Project\gradlew.bat"
if ((!(Test-Path $WrapperJar)) -or (!(Test-Path $GradlewBat))) {
    Write-Host "[1/5] Setting up Gradle wrapper..." -ForegroundColor Yellow
    if (!(Test-Path "$Project\gradle\wrapper")) { New-Item -ItemType Directory -Path "$Project\gradle\wrapper" -Force | Out-Null }
    
    # Try downloading wrapper jar from GitHub
    $jarOk = $false
    try {
        $jarUrl = "https://raw.githubusercontent.com/gradle/gradle/v$GradleVer/gradle/wrapper/gradle-wrapper.jar"
        Invoke-WebRequest -Uri $jarUrl -OutFile $WrapperJar -UseBasicParsing -ErrorAction Stop
        $jarOk = $true
        Write-Host "  + gradle-wrapper.jar downloaded" -ForegroundColor Green
    } catch {
        Write-Host "  ! GitHub download failed: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    # Fallback: download full Gradle zip and extract wrapper jar
    if (!$jarOk) {
        Write-Host "  Downloading Gradle $GradleVer (120MB) to extract wrapper..." -ForegroundColor Yellow
        $zipUrl = "https://services.gradle.org/distributions/gradle-${GradleVer}-bin.zip"
        $zipOut = "$env:TEMP\gradle.zip"
        Invoke-WebRequest -Uri $zipUrl -OutFile $zipOut -UseBasicParsing
        $extractDir = "$env:TEMP\gradle-extract"
        if (Test-Path $extractDir) { Remove-Item -Recurse -Force $extractDir }
        Expand-Archive -Path $zipOut -DestinationPath $extractDir
        Copy-Item "$extractDir\gradle-$GradleVer\lib\gradle-wrapper-$GradleVer.jar" $WrapperJar
        Remove-Item -Recurse -Force $extractDir, $zipOut
        Write-Host "  + gradle-wrapper.jar extracted" -ForegroundColor Green
    }
    
    # Create gradlew.bat
@"
@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
setlocal
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
"%JAVA_HOME%/bin/java" -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
"@ | Out-File -FilePath $GradlewBat -Encoding ASCII
    Write-Host "  + gradlew.bat created" -ForegroundColor Green
} else {
    Write-Host "[1/5] Gradle wrapper OK" -ForegroundColor Green
}

# Step 2: Debug keystore
$Keystore = "$env:USERPROFILE\.android\debug.keystore"
if (!(Test-Path $Keystore)) {
    Write-Host "[2/5] Creating debug keystore..." -ForegroundColor Yellow
    if (!(Test-Path "$env:USERPROFILE\.android")) { New-Item -ItemType Directory -Path "$env:USERPROFILE\.android" -Force | Out-Null }
    & "$env:JAVA_HOME\bin\keytool" -genkey -v -keystore $Keystore -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" 2>&1 | Out-Null
    if (Test-Path $Keystore) { Write-Host "  + debug.keystore created" -ForegroundColor Green }
} else {
    Write-Host "[2/5] Debug keystore OK" -ForegroundColor Green
}

# Step 3: Android SDK
$SdkManager = "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat"
if (!$SkipSdk) {
    if (!(Test-Path $SdkManager)) {
        Write-Host "[3/5] Downloading Android SDK command-line tools..." -ForegroundColor Yellow
        $sdkUrl = "https://dl.google.com/android/repository/commandlinetools-win-latest.zip"
        $sdkZip = "$env:TEMP\cmdline-tools.zip"
        Invoke-WebRequest -Uri $sdkUrl -OutFile $sdkZip -UseBasicParsing
        $tmpDir = "$SdkDir\cmdline-tools\tmp"
        if (Test-Path $tmpDir) { Remove-Item -Recurse -Force $tmpDir }
        New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null
        Expand-Archive -Path $sdkZip -DestinationPath $tmpDir -Force
        New-Item -ItemType Directory -Path "$SdkDir\cmdline-tools\latest" -Force | Out-Null
        Move-Item "$tmpDir\cmdline-tools\*" "$SdkDir\cmdline-tools\latest\" -Force
        Remove-Item -Recurse -Force $tmpDir, $sdkZip
        Write-Host "  + Command-line tools installed" -ForegroundColor Green
    } else {
        Write-Host "[3/5] SDK tools OK" -ForegroundColor Green
    }
    
    Write-Host "  Accepting licenses..." -ForegroundColor Yellow
    $env:ANDROID_SDK_ROOT = $SdkDir
    $tempFile = "$env:TEMP\sdk_licenses.txt"
    "y" | Out-File $tempFile -Encoding ASCII
    Get-Content $tempFile | & $SdkManager --licenses 2>&1 | Out-Null
    
    Write-Host "  Installing platform android-34 and build-tools..." -ForegroundColor Yellow
    Get-Content $tempFile | & $SdkManager "platforms;android-34" "build-tools;34.0.0"
    Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
    Write-Host "  + SDK ready" -ForegroundColor Green
} else {
    Write-Host "[3/5] SDK setup skipped" -ForegroundColor Yellow
}

# Step 4: local.properties
$LocalProps = "$Project\local.properties"
if (!(Test-Path $LocalProps)) {
    Write-Host "[4/5] Creating local.properties..." -ForegroundColor Yellow
    $escapedSdk = $SdkDir.Replace('\', '\\')
    "sdk.dir=$escapedSdk" | Out-File -FilePath $LocalProps -Encoding ASCII
    Write-Host "  + local.properties created" -ForegroundColor Green
} else {
    Write-Host "[4/5] local.properties OK" -ForegroundColor Green
}

# Step 5: Build APK
$env:ANDROID_SDK_ROOT = $SdkDir
$env:ANDROID_HOME = $SdkDir
Write-Host "[5/5] Building APK..." -ForegroundColor Cyan
Write-Host "  (First build downloads dependencies ~50MB)" -ForegroundColor Yellow

Push-Location $Project
try {
    & .\gradlew.bat assembleDebug --no-daemon 2>&1
    if ($LASTEXITCODE -eq 0) {
        $apk = Get-ChildItem -Recurse -Filter "*.apk" "$Project\app\build\outputs" | Select-Object -First 1
        Write-Host ""
        Write-Host "==============================" -ForegroundColor Cyan
        if ($apk) {
            Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
            Write-Host "  APK: $($apk.FullName)" -ForegroundColor White
            $size = [math]::Round(($apk.Length / 1024))
            Write-Host "  Size: $size KB" -ForegroundColor White
        } else {
            Write-Host "  BUILD SUCCESSFUL (APK not found in expected location)" -ForegroundColor Yellow
        }
        Write-Host "==============================" -ForegroundColor Cyan
    } else {
        Write-Host "  BUILD FAILED (exit: $LASTEXITCODE)" -ForegroundColor Red
    }
} finally {
    Pop-Location
}
Write-Host "Done!" -ForegroundColor Cyan
