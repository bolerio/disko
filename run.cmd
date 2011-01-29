@echo off
set GATE_HOME=c:\tools\gate
set DISCO_HOME=.
set JAVA_HOME=c:\java5
set CLASSPATH=""
set DISCO_NATIVE=native/windows
set JAVA_EXEC="%JAVA_HOME%\bin\java"

set PATH=%DISCO_NATIVE%;%PATH%

set LIB_JARS=
echo set LIB_JARS=%%~1;%%LIB_JARS%%>append.bat
dir /s/b lib\*.jar > tmpList.txt
FOR /F "usebackq tokens=1* delims=" %%i IN (tmpList.txt) do (call append.bat "%%i")
del append.bat
del tmpList.txt
set CLASSPATH=bin;%LIB_JARS%;
set CLASSPATH=%GATE_HOME%/bin/gate.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/xercesImpl.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/jasper-compiler-jdt.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/stax-api-1.0.1.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/nekohtml-0.9.5.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/PDFBox-0.7.2.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/jdom.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/ontotext.jar;%CLASSPATH%
set CLASSPATH=%GATE_HOME%/lib/wstx-lgpl-2.0.6.jar;%CLASSPATH%

echo Using native libraries at %DISCO_NATIVE%
set JAVA_FLAGS=-Xms1024m -Xmx1500m -Djava.library.path=%DISCO_NATIVE%
set JAVA_FLAGS=%JAVA_FLAGS% -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n
set JAVA_FLAGS=%JAVA_FLAGS% -Drelex.morphy.Morphy=org.disco.relex.MorphyHGDB -Dmorphy.hgdb.location=%1
set CMD_LINE=%JAVA_FLAGS% -cp %CLASSPATH% %1 %2 %3 %4 %5 %6 %7 %8 %9
echo Command line: %CMD_LINE%
%JAVA_EXEC% %CMD_LINE%