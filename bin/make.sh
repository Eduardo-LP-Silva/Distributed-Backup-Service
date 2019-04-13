#!/bin/bash

javac app/*.java
javac protocol/handler/*.java
javac protocol/initiator/*.java
javac protocol/listener/*.java
rmiregistry &