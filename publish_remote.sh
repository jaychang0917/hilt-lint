#!/bin/bash

# Build and upload the artifacts to 'mavenCentral'.
./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon --no-parallel
