@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@echo off
chcp 65001 >nul

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

@rem Set local scope for the variables with Windows NT shell
if "%OS%"=="Windows_NT" setlocal

set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."
set "APP_BASE_NAME=%~n0"
set "APP_HOME=%DIRNAME%"

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set "APP_HOME=%%~fi"

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

@rem 查找 java.exe

if defined JAVA_HOME goto findJavaFromJavaHome

set "JAVA_EXE=java.exe"
"%JAVA_EXE%" -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto execute

echo.
echo 错误: JAVA_HOME 未设置，并且在 PATH 中找不到 'java' 命令。
echo.
echo 请在环境变量中设置 JAVA_HOME，以匹配你安装的 Java 位置。
echo.
goto fail

:findJavaFromJavaHome
rem 去掉 JAVA_HOME 中的引号
set "JAVA_HOME=%JAVA_HOME:"=%"

set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if exist "%JAVA_EXE%" (
    goto execute
) else (
    echo.
    echo 错误: JAVA_HOME 设置为无效目录：%JAVA_HOME%
    echo.
    goto fail
)

:execute
rem 设置类路径
set "CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"

rem 合并所有 JVM 选项
set "ALL_JVM_OPTS=%DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS%"

rem 执行 Gradle
echo 运行 Gradle...
"%JAVA_EXE%" %ALL_JVM_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

goto end

:fail
rem 如果需要脚本的返回码而不是 cmd.exe 的返回码，请设置 GRADLE_EXIT_CONSOLE
if not ""=="%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:end
rem 清理
if "%OS%"=="Windows_NT" endlocal

:omega
rem 脚本结束
