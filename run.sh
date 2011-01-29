#!/bin/bash

NATIVE=native/linux/x86_64

VM_OPTS="-Djava.library.path=$NATIVE"

CLASSPATH="-classpath bin"
for filename in `find . -name '*.jar'`; do CLASSPATH=$CLASSPATH:$filename; done;

echo "java $VM_OPTS $CLASSPATH disko.Disko diskorun.properties $@"

java $VM_OPTS $CLASSPATH disko.Disko diskorun.properties "$@"
