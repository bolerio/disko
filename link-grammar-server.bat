set DEBUG_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9100
set VM_OPTS=%DEBUG_OPTS% -Djava.library.path=native/windows -Drelex.linkparserpath=data/linkparser/ 
set JAVA_HOME=c:\java5

:RUNLP
%JAVA_HOME%\bin\java  %VM_OPTS% -cp lib\relex.jar;lib\link-grammar.jar relex.parser.LinkParserServer %1
goto RUNLP
