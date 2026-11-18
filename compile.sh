#!/bin/zsh

sdk use java 21.0.2-graal

mvn clean package

cp target/checker-2.2.jar .
