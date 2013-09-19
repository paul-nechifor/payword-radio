#!/bin/bash

java -classpath `echo ../liburi/*.jar | tr ' ' ':'`:../build/classes si_t6.Main --user user.ser
