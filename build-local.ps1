param([string[]]$Tasks = @(':app:assembleDebug'))

$ErrorActionPreference = 'Stop'
# Reuse this computer's existing Android toolchain and dependency cache.
$toolsRoot = 'F:\qqrobot\.build-tools'
$env:JAVA_HOME = Join-Path $toolsRoot 'java\jdk-17.0.20.1+1'
$env:GRADLE_USER_HOME = Join-Path $toolsRoot 'gradle-cache'
$env:ANDROID_USER_HOME = Join-Path $toolsRoot 'android-user'
$env:ANDROID_HOME = Join-Path $toolsRoot 'android-sdk'
$gradle = Join-Path $PSScriptRoot '.local-tools\gradle-9.5.0\bin\gradle.bat'

& $gradle -p $PSScriptRoot @Tasks --console=plain
exit $LASTEXITCODE
