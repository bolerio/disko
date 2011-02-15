REM set JAVA_HOME if you don't have it your environment
rem set JAVA_HOME=c:/java6_32bit
set LIBPATH=./native/windows
set CLASSPATH=-classpath lib/link-grammar.jar
set PORT=9000

REM on window, dependent DLLs are found only when set in the global path var
set PATH=%LIBPATH%;%PATH%

:RUNLP
%JAVA_HOME%\bin\java %CLASSPATH%  org.linkgrammar.LGService -verbose -threads 5 %PORT% ./data/linkparser
goto RUNLP
