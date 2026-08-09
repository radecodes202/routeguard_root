@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=-Xmx64m

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
exit /b 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
exit /b 2

:init
@rem Get command-line arguments, handling Windows variants
if not "%OS%"=="Windows_NT" goto win9xME_args
if "%@eval[2+2]"=="4" goto 4NT_args

:win9xME_args
@rem Slurp the zero and first arguments off the command line
set "GET_ARGS=%*"
shift
shift
set "EVARGS=%GET_ARGS%"
goto initVmOptions

:4NT_args
@rem Get arguments from the 4NT shell from JP Software
set "GET_ARGS=%*"
shift
shift
set "EVARGS=%GET_ARGS%"
goto initVmOptions

:initVmOptions
@rem Set JVM_OPTS
set JVM_OPTS=%DEFAULT_JVM_OPTS%

@rem Collect all arguments for the java command
set "SET_CMD_LINE_ARGS=%*"

:runJava
if not "%DEBUG%"=="" echo "%JAVACMD% %JVM_OPTS% %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %SET_CMD_LINE_ARGS%"
if not "%DEBUG%"=="" echo %CLASSPATH%
if not "%DEBUG%"=="" echo %SET_CMD_LINE_ARGS%
if not "%DEBUG%"=="" echo %JAVACMD%

"%JAVACMD%" %JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %SET_CMD_LINE_ARGS%
@endlocal