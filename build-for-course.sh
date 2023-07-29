#!/bin/bash

echo "Building bb4-optimization for discrete-optimization course"

# create bb4-optimizations-1.8-SNAPSHOT.zip in /build/distributions dir
./gradlew

cd build/distributions
rm -rf bb4-optimization-1.8.SNAPSHOT
echo "Unzipping bb4-optimization-1.8-SNAPSHOT.zip"
unzip bb4-optimization-1.8-SNAPSHOT.zip

exit