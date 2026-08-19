#!/usr/bin/env bash

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"

readonly GENERAL_CLASS="$ROOT/testing/testcontainers/build/classes/kotlin/main/io/bluetape4k/testcontainers/PropertyExportingServer.class"
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

"$ROOT/gradlew" \
  :bluetape4k-testcontainers:compileKotlin \
  :bluetape4k-virtualthread-jdk21:compileKotlin \
  :bluetape4k-virtualthread-jdk25:compileKotlin \
  --no-daemon --no-configuration-cache

assert_major "$GENERAL_CLASS" 69
assert_major "$JAVA21_CLASS" 65
assert_major "$JAVA25_CLASS" 69
