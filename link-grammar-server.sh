#!/bin/bash

LIBPATH=./native/linux
CLASSPATH='-classpath lib/link-grammar.jar'
PORT=9000

echo "CLASSPATH=$CLASSPATH"


while [ 1 ]
do
    echo "Link Grammar server on port $PORT"
    $JAVA_HOME/bin/java $VM_OPTS $CLASSPATH -Djava.library.path=$LIBPATH org.linkgrammar.LGService -verbose -threads 5 $PORT ./data/linkparser
    echo "Service exited."
done
