@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@echo off

set MAVEN_PROJECTBASEDIR=%~dp0
if not "%JAVA_HOME%"=="" goto javaHomeSet
echo Error: JAVA_HOME not set. >&2
exit /b 1
:javaHomeSet

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

for /f "usebackq tokens=1,2 delims==" %%a in ("%WRAPPER_PROPERTIES%") do (
    if "%%a"=="distributionUrl" set DISTRIBUTION_URL=%%b
)

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

echo Downloading Maven 3.9.9...
powershell -Command "& { Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%TEMP%\maven.zip' }"
powershell -Command "& { Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\' -Force }"
move "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\apache-maven-3.9.9" "%MAVEN_HOME%"
del "%TEMP%\maven.zip"

:runMaven
set PATH=%MAVEN_HOME%\bin;%PATH%
mvn %*
