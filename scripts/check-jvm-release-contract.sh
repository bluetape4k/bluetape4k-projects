#!/usr/bin/env bash

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"

readonly GENERAL_CLASS="$ROOT/testing/testcontainers/build/classes/kotlin/main/io/bluetape4k/testcontainers/PropertyExportingServer.class"
readonly TESTCONTAINERS_CLASSES="$ROOT/testing/testcontainers/build/classes/kotlin/main"
readonly IGNITE2_CLASS="io.bluetape4k.testcontainers.storage.Ignite2Server"
readonly IGNITE2_COMPANION_CLASS='io.bluetape4k.testcontainers.storage.Ignite2Server$Companion'
readonly IGNITE2_BASELINE_VERSION="1.12.1"
readonly IGNITE2_BASELINE_SHA256="525f21ea6addeece8cb9d9eded2e9461aeda8693e2a2617e82e2357a476d99ee"
readonly IGNITE2_BASELINE_URL="https://repo.maven.apache.org/maven2/io/github/bluetape4k/bluetape4k-testcontainers/$IGNITE2_BASELINE_VERSION/bluetape4k-testcontainers-$IGNITE2_BASELINE_VERSION.jar"
readonly IGNITE2_BASELINE_DIR="$ROOT/testing/testcontainers/build/jvm-release-contract"
readonly IGNITE2_BASELINE_JAR="$IGNITE2_BASELINE_DIR/bluetape4k-testcontainers-$IGNITE2_BASELINE_VERSION.jar"
readonly JAVA21_CLASS="$ROOT/virtualthread/jdk21/build/classes/kotlin/main/io/bluetape4k/concurrent/virtualthread/jdk21/Jdk21StructuredTaskScopeProvider.class"
readonly JAVA25_CLASS="$ROOT/virtualthread/jdk25/build/classes/kotlin/main/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProvider.class"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

assert_major() {
  local class_file="$1"
  local expected="$2"
  [[ -f "$class_file" ]] || fail "missing classfile: $class_file"
  local actual
  actual="$(javap -verbose "$class_file" | awk '/major version:/{print $3; exit}')"
  [[ "$actual" == "$expected" ]] ||
    fail "classfile major mismatch: file=$class_file expected=$expected actual=$actual"
  printf 'OK: %s major=%s\n' "${class_file#"$ROOT"/}" "$actual"
}

descriptor_for() {
  local classpath="$1"
  local class_name="$2"
  local signature="$3"
  local report
  report="$(javap -classpath "$classpath" -public -s "$class_name")"
  printf '%s\n' "$report" |
    grep -F -A1 -- "$signature" |
    awk '/descriptor:/{print $2; exit}'
}

assert_descriptor_matches_baseline() {
  local class_name="$1"
  local signature="$2"
  local expected_descriptor="$3"
  local baseline_descriptor
  local current_descriptor
  baseline_descriptor="$(descriptor_for "$IGNITE2_BASELINE_JAR" "$class_name" "$signature")"
  current_descriptor="$(descriptor_for "$TESTCONTAINERS_CLASSES" "$class_name" "$signature")"
  [[ "$baseline_descriptor" == "$expected_descriptor" ]] ||
    fail "published baseline descriptor mismatch: class=$class_name expected=$expected_descriptor actual=$baseline_descriptor"
  [[ "$current_descriptor" == "$baseline_descriptor" ]] ||
    fail "JVM descriptor drift: class=$class_name baseline=$baseline_descriptor current=$current_descriptor"
  printf 'OK: %s baseline=%s current=%s\n' "$class_name" "$baseline_descriptor" "$current_descriptor"
}

download_ignite2_baseline() {
  mkdir -p "$IGNITE2_BASELINE_DIR"
  local baseline_tmp
  local actual_sha256
  baseline_tmp="$(mktemp "$IGNITE2_BASELINE_DIR/.ignite2-baseline.XXXXXX")"
  trap 'rm -f "$baseline_tmp"' EXIT
  curl --fail --location --silent --show-error \
    --output "$baseline_tmp" \
    "$IGNITE2_BASELINE_URL"
  actual_sha256="$(shasum -a 256 "$baseline_tmp" | awk '{print $1}')"
  [[ "$actual_sha256" == "$IGNITE2_BASELINE_SHA256" ]] ||
    fail "published baseline checksum mismatch: expected=$IGNITE2_BASELINE_SHA256 actual=$actual_sha256"
  mv "$baseline_tmp" "$IGNITE2_BASELINE_JAR"
  trap - EXIT
  printf 'OK: published baseline %s sha256=%s\n' "$IGNITE2_BASELINE_VERSION" "$actual_sha256"
}

"$ROOT/gradlew" \
  :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-virtualthread-jdk21:compileKotlin \
  :bluetape4k-virtualthread-jdk25:compileKotlin \
  --no-daemon --no-configuration-cache

assert_major "$GENERAL_CLASS" 69
assert_major "$JAVA21_CLASS" 65
assert_major "$JAVA25_CLASS" 69
download_ignite2_baseline

# Published baseline:
# io.github.bluetape4k:bluetape4k-testcontainers:1.12.1
# SHA-256: 525f21ea6addeece8cb9d9eded2e9461aeda8693e2a2617e82e2357a476d99ee
readonly DOCKER_IMAGE_DESCRIPTOR='(Lorg/testcontainers/utility/DockerImageName;ZZ)Lio/bluetape4k/testcontainers/storage/Ignite2Server;'
readonly STRING_IMAGE_DESCRIPTOR='(Ljava/lang/String;Ljava/lang/String;ZZ)Lio/bluetape4k/testcontainers/storage/Ignite2Server;'

assert_descriptor_matches_baseline "$IGNITE2_CLASS" \
  'public static final io.bluetape4k.testcontainers.storage.Ignite2Server invoke(org.testcontainers.utility.DockerImageName, boolean, boolean);' \
  "$DOCKER_IMAGE_DESCRIPTOR"
assert_descriptor_matches_baseline "$IGNITE2_CLASS" \
  'public static final io.bluetape4k.testcontainers.storage.Ignite2Server invoke(java.lang.String, java.lang.String, boolean, boolean);' \
  "$STRING_IMAGE_DESCRIPTOR"
assert_descriptor_matches_baseline "$IGNITE2_COMPANION_CLASS" \
  'public final io.bluetape4k.testcontainers.storage.Ignite2Server invoke(org.testcontainers.utility.DockerImageName, boolean, boolean);' \
  "$DOCKER_IMAGE_DESCRIPTOR"
assert_descriptor_matches_baseline "$IGNITE2_COMPANION_CLASS" \
  'public final io.bluetape4k.testcontainers.storage.Ignite2Server invoke(java.lang.String, java.lang.String, boolean, boolean);' \
  "$STRING_IMAGE_DESCRIPTOR"
