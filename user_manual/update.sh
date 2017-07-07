#!/bin/bash

mvn package -Psingle
cd target
chmod +x tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar
cp tsat-0.0.1-SNAPSHOT-jar-with-dependencies.jar ~/Desktop/TSAT.jar
cd ..
cd user_manual
cp User\ Manual.pdf ~/Desktop/
cp datasets ~/Desktop/datasets
