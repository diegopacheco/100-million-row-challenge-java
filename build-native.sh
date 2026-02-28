#!/bin/bash
GRAAL_HOME="$HOME/.sdkman/candidates/java/25.0.1-graal"
NATIVE_IMAGE="$GRAAL_HOME/bin/native-image"

if [ ! -f "$NATIVE_IMAGE" ]; then
  echo "native-image not found at $NATIVE_IMAGE"
  echo "Install GraalVM via: sdk install java 25.0.1-graal"
  exit 1
fi

./build.sh

GC_FLAG=""
if [[ "$(uname)" == "Linux" ]]; then
  GC_FLAG="--gc=G1"
fi

$NATIVE_IMAGE \
  -cp target/classes \
  -o target/challenge-native \
  --no-fallback \
  $GC_FLAG \
  -O3 \
  -march=native \
  -H:+SharedArenaSupport \
  challenge.Main || exit 1

echo "Native image built: target/challenge-native"
