#!/usr/bin/env bash

set -euo pipefail

readonly BASE_SHA="a065a8e88cf246975660c68df2dd78dfb5b6dc4d"
readonly BASE_TREE="50cf7789648c0091b6c16de6cf5eb495c26510f8"
readonly BASE_JAR_SHA="34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1"

ROOT="$(git rev-parse --show-toplevel)"
COMMON_DIR="$(git rev-parse --path-format=absolute --git-common-dir)"
MAIN_ROOT="$(dirname "$COMMON_DIR")"
FIXTURE_ROOT="$ROOT/io/io/src/test/resources/abi/issue-755"
AUTH_DIR="$ROOT/.codex/compat/issue-755/$BASE_SHA"
BASE_WORKTREE="$MAIN_ROOT/.worktrees/compat/issue-755-base"
BASE_JAR="$AUTH_DIR/base/bluetape4k-io-1.12.0.jar"
CURRENT_JAR="$AUTH_DIR/current/bluetape4k-io-1.12.0.jar"
CLASSES="$AUTH_DIR/classes"
INIT_SCRIPT="$AUTH_DIR/issue755-classpaths.init.gradle"
EXPECTED_HEAD=""
BUILD_CURRENT=false
CREATED_BASE_WORKTREE=false

usage() {
    echo "Usage: $0 --build-current --expected-head <full-git-sha>"
}

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

sha256() {
    shasum -a 256 "$1" | awk '{print $1}'
}

cleanup() {
    if [[ "$CREATED_BASE_WORKTREE" == true && -d "$BASE_WORKTREE" ]]; then
        git -C "$MAIN_ROOT" worktree remove --force "$BASE_WORKTREE" >/dev/null 2>&1 || true
    fi
}

trap cleanup EXIT

assert_hash() {
    local path="$1"
    local expected="$2"
    [[ -f "$path" ]] || fail "missing authority artifact: $path"
    local actual
    actual="$(sha256 "$path")"
    [[ "$actual" == "$expected" ]] ||
        fail "checksum mismatch for $path: expected=$expected actual=$actual"
}

assert_clean_abi_paths() {
    local dirty
    dirty="$({
        git -C "$ROOT" diff --name-only
        git -C "$ROOT" diff --cached --name-only
        git -C "$ROOT" ls-files --others --exclude-standard
    } | awk '
        /^io\/io\/src\/main\/kotlin\/io\/bluetape4k\/io\/compressor\// ||
        /^io\/io\/src\/test\/kotlin\/io\/bluetape4k\/io\/compressor\// ||
        /^io\/io\/src\/test\/java\/io\/bluetape4k\/io\/compressor\// ||
        /^io\/io\/src\/test\/resources\/abi\/issue-755\// ||
        $0 == "scripts/check-compressor-buffer-abi.sh" { print }
    ' | sort -u)"
    [[ -z "$dirty" ]] || fail "dirty compressor ABI paths: $dirty"
}

write_init_script() {
    mkdir -p "$AUTH_DIR"
    cat > "$INIT_SCRIPT" <<'GRADLE'
import org.gradle.api.tasks.SourceSetContainer

gradle.afterProject { project, state ->
    if (!state.failure && project.path == ":bluetape4k-io") {
        project.tasks.register("issue755PrintMainRuntimeClasspath") {
            doLast {
                println(project.extensions.getByType(SourceSetContainer).getByName("main").runtimeClasspath.asPath)
            }
        }
        project.tasks.register("issue755PrintKotlinCompilerClasspath") {
            doLast {
                println(project.configurations.getByName("kotlinCompilerClasspath").asPath)
            }
        }
    }
}
GRADLE
}

gradle_value() {
    local worktree="$1"
    local task="$2"
    "$worktree/gradlew" -q -p "$worktree" -I "$INIT_SCRIPT" ":bluetape4k-io:$task" --no-configuration-cache |
        tail -n 1
}

ensure_base_worktree() {
    if [[ ! -d "$BASE_WORKTREE" ]]; then
        mkdir -p "$(dirname "$BASE_WORKTREE")"
        git -C "$MAIN_ROOT" worktree add --detach "$BASE_WORKTREE" "$BASE_SHA" >/dev/null
        CREATED_BASE_WORKTREE=true
    fi
    [[ "$(git -C "$BASE_WORKTREE" rev-parse HEAD)" == "$BASE_SHA" ]] ||
        fail "baseline worktree is not pinned to $BASE_SHA"
    [[ "$(git -C "$BASE_WORKTREE" rev-parse 'HEAD^{tree}')" == "$BASE_TREE" ]] ||
        fail "baseline tree mismatch"
    [[ -z "$(git -C "$BASE_WORKTREE" status --porcelain --untracked-files=no)" ]] ||
        fail "baseline worktree has tracked changes"
}

resolve_single_jar() {
    local directory="$1"
    local candidates
    candidates="$(find "$directory" -maxdepth 1 -type f -name 'bluetape4k-io-*.jar' | sort)"
    [[ "$(printf '%s\n' "$candidates" | sed '/^$/d' | wc -l | tr -d ' ')" == "1" ]] ||
        fail "expected exactly one bluetape4k-io jar in $directory: $candidates"
    printf '%s\n' "$candidates"
}

verify_base_jar() {
    rm -f "$BASE_WORKTREE/io/io/build/libs"/bluetape4k-io-*.jar
    "$BASE_WORKTREE/gradlew" -p "$BASE_WORKTREE" :bluetape4k-io:jar --no-configuration-cache
    local built
    built="$(resolve_single_jar "$BASE_WORKTREE/io/io/build/libs")"
    assert_hash "$built" "$BASE_JAR_SHA"
    mkdir -p "$(dirname "$BASE_JAR")"
    cp "$built" "$BASE_JAR"
}

compile_kotlin() {
    local worktree="$1"
    local destination="$2"
    local classpath="$3"
    shift 3
    local compiler_classpath
    compiler_classpath="$(gradle_value "$worktree" issue755PrintKotlinCompilerClasspath)"
    java -cp "$compiler_classpath" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
        -jvm-target 21 \
        -language-version 2.3 \
        -api-version 2.3 \
        -jvm-default=enable \
        -no-stdlib \
        -no-reflect \
        -classpath "$classpath:$compiler_classpath" \
        -d "$destination" \
        "$@"
}

verify_fixture_manifest() {
    local fixture="$FIXTURE_ROOT/pre-change/legacy-compressor-fixtures.jar"
    local manifest="$FIXTURE_ROOT/pre-change/manifest.json"
    [[ -f "$fixture" ]] || fail "missing frozen legacy fixture jar"
    [[ -f "$manifest" ]] || fail "missing frozen fixture manifest"
    jar tf "$fixture" | grep -q 'io/bluetape4k/io/compressor/Compressor.class' &&
        fail "fixture jar must not contain Compressor.class"
    python3 - "$manifest" "$(sha256 "$fixture")" <<'PY'
import json
import pathlib
import sys

manifest = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
expected = {
    "producer.commit": "a065a8e88cf246975660c68df2dd78dfb5b6dc4d",
    "producer.tree": "50cf7789648c0091b6c16de6cf5eb495c26510f8",
    "compiler.java": "21",
    "compiler.kotlin": "2.4.0",
    "compiler.languageVersion": "2.3",
    "compiler.apiVersion": "2.3",
    "compiler.jvmDefault": "enable",
    "baselineJar.sha256": "34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1",
    "fixtureJar.sha256": sys.argv[2],
}
for dotted, value in expected.items():
    cursor = manifest
    for component in dotted.split("."):
        cursor = cursor[component]
    if cursor != value:
        raise SystemExit(f"manifest mismatch for {dotted}: expected={value} actual={cursor}")
if manifest["fixtureJar"]["containsCompressorClass"] is not False:
    raise SystemExit("fixture manifest must declare containsCompressorClass=false")
print("FIXTURE MANIFEST PASS")
PY
}

compile_legacy_sources() {
    rm -rf "$CLASSES/legacy"
    mkdir -p "$CLASSES/legacy/java" "$CLASSES/legacy/kotlin"
    javac --release 21 \
        -cp "$BASE_JAR" \
        -d "$CLASSES/legacy/java" \
        "$FIXTURE_ROOT/src/java/LegacyCompressorImplementation.java" \
        "$FIXTURE_ROOT/src/java/LegacyCompressorCaller.java"
    compile_kotlin "$BASE_WORKTREE" "$CLASSES/legacy/kotlin" "$BASE_JAR" \
        "$FIXTURE_ROOT/src/kotlin/LegacyCompressorImplementation.kt" \
        "$FIXTURE_ROOT/src/kotlin/LegacyCompressorCaller.kt"
}

build_current_jar() {
    rm -f "$ROOT/io/io/build/libs"/bluetape4k-io-*.jar
    "$ROOT/gradlew" -p "$ROOT" :bluetape4k-io:jar --no-configuration-cache
    local built
    built="$(resolve_single_jar "$ROOT/io/io/build/libs")"
    mkdir -p "$(dirname "$CURRENT_JAR")"
    cp "$built" "$CURRENT_JAR"
}

run_legacy_classfiles() {
    local fixture="$FIXTURE_ROOT/pre-change/legacy-compressor-fixtures.jar"
    local runtime_classpath
    runtime_classpath="$(gradle_value "$ROOT" issue755PrintMainRuntimeClasspath)"
    java -cp "$fixture:$CURRENT_JAR:$runtime_classpath" \
        io.bluetape4k.io.compressor.abi.issue755.java.LegacyCompressorCaller
    java -cp "$fixture:$CURRENT_JAR:$runtime_classpath" \
        io.bluetape4k.io.compressor.abi.issue755.kotlin.LegacyCompressorCaller
}

normalized_ambiguity() {
    local jar="$1"
    local output="$2"
    rm -rf "$CLASSES/ambiguous"
    mkdir -p "$CLASSES/ambiguous"
    if LC_ALL=C javac --release 21 -cp "$jar" -d "$CLASSES/ambiguous" \
        "$FIXTURE_ROOT/src/java/AmbiguousNullCaller.java" >"$output" 2>&1; then
        fail "AmbiguousNullCaller.java unexpectedly compiled against $jar"
    fi
    grep -F 'reference to compress is ambiguous' "$output" >/dev/null ||
        fail "expected ambiguous compress(null) diagnostic against $jar"
    sed -E 's#^.*AmbiguousNullCaller.java:[0-9]+: ##; s#[[:space:]]+# #g' "$output" |
        grep -F 'reference to compress is ambiguous' | head -n 1
}

verify_ambiguous_null() {
    local baseline current
    baseline="$(normalized_ambiguity "$BASE_JAR" "$AUTH_DIR/ambiguous-base.txt")"
    current="$(normalized_ambiguity "$CURRENT_JAR" "$AUTH_DIR/ambiguous-current.txt")"
    [[ "$baseline" == "$current" ]] ||
        fail "compress(null) ambiguity diagnostic drifted: baseline=$baseline current=$current"
    echo "AMBIGUOUS NULL PASS"
}

verify_jvm_defaults() {
    local report="$AUTH_DIR/current-compressor.javap.txt"
    javap -classpath "$CURRENT_JAR" -p -s io.bluetape4k.io.compressor.Compressor >"$report"
    python3 - "$report" <<'PY'
import pathlib
import re
import sys

text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
descriptor = r"\(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;\)I"
for method in ("compress", "decompress"):
    pattern = rf"public default int {method}\(java\.nio\.ByteBuffer, java\.nio\.ByteBuffer\);\s+descriptor: {descriptor}"
    if re.search(pattern, text) is None:
        raise SystemExit(
            f"COMPRESSOR BUFFER ABI RED: missing executable JVM default {method}"
            "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)I"
        )
print("JVM DEFAULT DESCRIPTORS PASS")
PY
}

compile_and_run_new_callers() {
    local fixture="$FIXTURE_ROOT/pre-change/legacy-compressor-fixtures.jar"
    local runtime_classpath
    runtime_classpath="$(gradle_value "$ROOT" issue755PrintMainRuntimeClasspath)"
    rm -rf "$CLASSES/current"
    mkdir -p "$CLASSES/current/java" "$CLASSES/current/kotlin"
    javac --release 21 \
        -cp "$CURRENT_JAR:$fixture" \
        -d "$CLASSES/current/java" \
        "$FIXTURE_ROOT/src/java/NewCompressorBufferCaller.java"
    compile_kotlin "$ROOT" "$CLASSES/current/kotlin" "$CURRENT_JAR:$fixture" \
        "$FIXTURE_ROOT/src/kotlin/NewCompressorBufferCaller.kt"
    java -cp "$CLASSES/current/java:$fixture:$CURRENT_JAR:$runtime_classpath" \
        io.bluetape4k.io.compressor.abi.issue755.java.NewCompressorBufferCaller
    java -cp "$CLASSES/current/kotlin:$fixture:$CURRENT_JAR:$runtime_classpath" \
        io.bluetape4k.io.compressor.abi.issue755.kotlin.NewCompressorBufferCaller
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --build-current)
            BUILD_CURRENT=true
            shift
            ;;
        --expected-head)
            [[ $# -ge 2 ]] || fail "--expected-head requires a commit"
            EXPECTED_HEAD="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            usage >&2
            fail "unknown argument: $1"
            ;;
    esac
done

[[ "$BUILD_CURRENT" == true ]] || fail "--build-current is required"
[[ "$EXPECTED_HEAD" =~ ^[0-9a-f]{40}$ ]] || fail "full --expected-head is required"
[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$EXPECTED_HEAD" ]] || fail "head drift"
assert_clean_abi_paths
command -v javac >/dev/null || fail "javac is required"
command -v java >/dev/null || fail "java is required"
command -v javap >/dev/null || fail "javap is required"

mkdir -p "$AUTH_DIR"
write_init_script
ensure_base_worktree
verify_base_jar
compile_legacy_sources
verify_fixture_manifest
build_current_jar
run_legacy_classfiles
verify_ambiguous_null
verify_jvm_defaults
compile_and_run_new_callers
echo "COMPRESSOR BUFFER ABI PASS head=$EXPECTED_HEAD"
