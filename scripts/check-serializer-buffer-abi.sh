#!/usr/bin/env bash

set -euo pipefail

readonly BASE_SHA="90b267871e9154f242e6de7ee9fd0539f83e509e"
readonly BASE_TREE="f40ccbda16ddf56d4b7770c01e9b0b2cb07cedba"
readonly IO_JAR_SHA="ddf283bdb3a17267a5a275bcc78f5cbbee7510c35a512d5fabe6a34d2c39063e"
readonly JSON_JAR_SHA="e8a9930e0c7ca2aecd6088f55b15243ad3c3b490bbc97e67b9c4452d569c1329"
readonly AVRO_JAR_SHA="876396647c0f3d37b18fcfbee5648ef8764655bb4a5c2c902d70d98b81422e60"

ROOT="$(git rev-parse --show-toplevel)"
COMMON_DIR="$(git rev-parse --path-format=absolute --git-common-dir)"
MAIN_ROOT="$(dirname "$COMMON_DIR")"
BASE_WORKTREE="$MAIN_ROOT/.worktrees/compat/issue-754-base"
AUTH_DIR="$ROOT/.codex/compat/issue-754/$BASE_SHA"
BASE_JARS="$AUTH_DIR/jars"
CLASSES="$AUTH_DIR/classes"
REPORT="$AUTH_DIR/abi-report.txt"
INIT_SCRIPT="$AUTH_DIR/print-classpath.init.gradle"
FIXTURE_ROOT="$ROOT/io/io/src/test/resources/compat/issue-754/pre-change"
BUILD_CURRENT=false
EXPECTED_HEAD=""

usage() {
    echo "Usage: $0 --build-current --expected-head <commit>"
}

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

sha256() {
    shasum -a 256 "$1" | awk '{print $1}'
}

assert_hash() {
    local path="$1"
    local expected="$2"
    [[ -f "$path" ]] || fail "missing authority artifact: $path"
    local actual
    actual="$(sha256 "$path")"
    [[ "$actual" == "$expected" ]] || fail "checksum mismatch for $path: expected=$expected actual=$actual"
}

resolve_single_jar() {
    local directory="$1"
    local prefix="$2"
    local candidates
    local count
    candidates="$(find "$directory" -maxdepth 1 -type f -name "$prefix-*.jar" | sort)"
    count="$(printf '%s\n' "$candidates" | sed '/^$/d' | wc -l | tr -d ' ')"
    [[ "$count" == "1" ]] || fail "expected exactly one $prefix jar in $directory, found $count: $candidates"
    printf '%s\n' "$candidates"
}

write_classpath_init_script() {
    mkdir -p "$AUTH_DIR"
    cat > "$INIT_SCRIPT" <<'GRADLE'
import org.gradle.api.tasks.SourceSetContainer

gradle.afterProject { project, state ->
    if (!state.failure && project.path in [":bluetape4k-io", ":bluetape4k-json", ":bluetape4k-avro"]) {
        project.tasks.register("issue754PrintRuntimeClasspath") {
            doLast {
                println(project.extensions.getByType(SourceSetContainer).getByName("main").runtimeClasspath.asPath)
            }
        }
        project.tasks.register("issue754PrintTestRuntimeClasspath") {
            doLast {
                println(project.extensions.getByType(SourceSetContainer).getByName("test").runtimeClasspath.asPath)
            }
        }
    }
}
GRADLE
}

runtime_classpath() {
    local worktree="$1"
    local project="$2"
    local source_set="$3"
    local task="issue754PrintRuntimeClasspath"
    if [[ "$source_set" == "test" ]]; then
        task="issue754PrintTestRuntimeClasspath"
    fi
    "$worktree/gradlew" -q -p "$worktree" -I "$INIT_SCRIPT" ":$project:$task" --no-configuration-cache |
        tail -n 1
}

ensure_base_worktree() {
    if [[ ! -d "$BASE_WORKTREE" ]]; then
        git -C "$MAIN_ROOT" worktree add --detach "$BASE_WORKTREE" "$BASE_SHA"
    fi
    [[ "$(git -C "$BASE_WORKTREE" rev-parse HEAD)" == "$BASE_SHA" ]] ||
        fail "baseline worktree is not pinned to $BASE_SHA: $BASE_WORKTREE"
    [[ "$(git -C "$BASE_WORKTREE" rev-parse 'HEAD^{tree}')" == "$BASE_TREE" ]] ||
        fail "baseline tree mismatch: $BASE_WORKTREE"
    [[ -z "$(git -C "$BASE_WORKTREE" status --porcelain --untracked-files=no)" ]] ||
        fail "baseline worktree has tracked changes: $BASE_WORKTREE"
}

build_base_jars() {
    ensure_base_worktree
    mkdir -p "$BASE_JARS"
    "$BASE_WORKTREE/gradlew" -p "$BASE_WORKTREE" \
        :bluetape4k-io:jar :bluetape4k-json:jar :bluetape4k-avro:jar \
        --no-configuration-cache

    local io_jar json_jar avro_jar
    io_jar="$(resolve_single_jar "$BASE_WORKTREE/io/io/build/libs" "bluetape4k-io")"
    json_jar="$(resolve_single_jar "$BASE_WORKTREE/io/json/build/libs" "bluetape4k-json")"
    avro_jar="$(resolve_single_jar "$BASE_WORKTREE/io/avro/build/libs" "bluetape4k-avro")"
    cp "$io_jar" "$BASE_JARS/"
    cp "$json_jar" "$BASE_JARS/"
    cp "$avro_jar" "$BASE_JARS/"
}

verify_base_jars() {
    local io_jar="$BASE_JARS/bluetape4k-io-1.12.0.jar"
    local json_jar="$BASE_JARS/bluetape4k-json-1.12.0.jar"
    local avro_jar="$BASE_JARS/bluetape4k-avro-1.12.0.jar"
    if [[ ! -f "$io_jar" || ! -f "$json_jar" || ! -f "$avro_jar" ]]; then
        build_base_jars
    fi
    assert_hash "$io_jar" "$IO_JAR_SHA"
    assert_hash "$json_jar" "$JSON_JAR_SHA"
    assert_hash "$avro_jar" "$AVRO_JAR_SHA"
}

verify_frozen_fixtures() {
    assert_hash "$FIXTURE_ROOT/binary/jdk-simple-data.bin" \
        "90b5df96eb70b575cf2dd2cd31956c14b90f0b701f50f3008e53535a74e77870"
    assert_hash "$FIXTURE_ROOT/binary/kryo-default-simple-data.bin" \
        "18ddb15377e1066848cc9485fe2d7a8efa11ec9d3fb190c44d0d3dd4efd4f8a7"
    assert_hash "$FIXTURE_ROOT/binary/kryo-fast-simple-data.bin" \
        "e3bd2de6c7d95f7c248d9a3ca36bcd8edfc23414f1ecc260b2ed2be19be9d365"
    assert_hash "$FIXTURE_ROOT/binary/fory-default-simple-data.bin" \
        "b01f1635f860bd269ffd05b43396d22534df6196ca2f165d4b19e33a0edead3c"
    assert_hash "$FIXTURE_ROOT/binary/fory-fast-simple-data.bin" \
        "7196b4b07fdf9f22454286ce0333f67b094f7f2c57040369136ffb0a5fe1032d"
}

avro_api_jar() {
    local classpath="$1"
    local result
    result="$(printf '%s' "$classpath" | tr ':' '\n' | grep '/org.apache.avro/avro/1.12.1/.*/avro-1.12.1.jar$' | head -n 1)"
    [[ -n "$result" ]] || fail "could not resolve Apache Avro 1.12.1 from the Gradle classpath"
    printf '%s\n' "$result"
}

compile_legacy_fixtures() {
    local avro_jar="$1"
    rm -rf "$CLASSES/legacy"
    mkdir -p "$CLASSES/legacy/binary-java" "$CLASSES/legacy/binary-kotlin"
    mkdir -p "$CLASSES/legacy/json-java" "$CLASSES/legacy/json-kotlin"
    mkdir -p "$CLASSES/legacy/avro-java" "$CLASSES/legacy/avro-kotlin"

    javac -source 21 -target 21 \
        -cp "$BASE_JARS/bluetape4k-io-1.12.0.jar" \
        -d "$CLASSES/legacy/binary-java" \
        "$ROOT/io/io/src/test/resources/compat/issue-754/src/java/LegacyBinaryCaller.java" \
        "$ROOT/io/io/src/test/resources/compat/issue-754/src/java/LegacyBinaryImplementation.java"
    kotlinc -jvm-target 21 \
        -classpath "$BASE_JARS/bluetape4k-io-1.12.0.jar" \
        -d "$CLASSES/legacy/binary-kotlin" \
        "$ROOT/io/io/src/test/resources/compat/issue-754/src/kotlin/LegacyBinaryCaller.kt" \
        "$ROOT/io/io/src/test/resources/compat/issue-754/src/kotlin/LegacyBinaryImplementation.kt"

    javac -source 21 -target 21 \
        -cp "$BASE_JARS/bluetape4k-json-1.12.0.jar" \
        -d "$CLASSES/legacy/json-java" \
        "$ROOT/io/json/src/test/resources/compat/issue-754/src/java/LegacyJsonCaller.java" \
        "$ROOT/io/json/src/test/resources/compat/issue-754/src/java/LegacyJsonImplementation.java"
    kotlinc -jvm-target 21 \
        -classpath "$BASE_JARS/bluetape4k-json-1.12.0.jar" \
        -d "$CLASSES/legacy/json-kotlin" \
        "$ROOT/io/json/src/test/resources/compat/issue-754/src/kotlin/LegacyJsonCaller.kt" \
        "$ROOT/io/json/src/test/resources/compat/issue-754/src/kotlin/LegacyJsonImplementation.kt"

    javac -source 21 -target 21 \
        -cp "$BASE_JARS/bluetape4k-avro-1.12.0.jar:$avro_jar" \
        -d "$CLASSES/legacy/avro-java" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroCaller.java" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroReflectImplementation.java" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroGenericRecordImplementation.java" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroSpecificRecordImplementation.java"
    kotlinc -jvm-target 21 \
        -classpath "$BASE_JARS/bluetape4k-avro-1.12.0.jar:$avro_jar" \
        -d "$CLASSES/legacy/avro-kotlin" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroCaller.kt" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroReflectImplementation.kt" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroGenericRecordImplementation.kt" \
        "$ROOT/io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroSpecificRecordImplementation.kt"
}

regenerate_and_compare_fixtures() {
    local base_test_cp="$1"
    local generated="$AUTH_DIR/regenerated-fixtures"
    rm -rf "$CLASSES/generator" "$generated"
    mkdir -p "$CLASSES/generator" "$generated"
    javac -source 21 -target 21 \
        -cp "$BASE_JARS/bluetape4k-io-1.12.0.jar:$CLASSES/legacy/binary-java:$base_test_cp" \
        -d "$CLASSES/generator" \
        "$ROOT/io/io/src/test/resources/compat/issue-754/src/java/GenerateBinaryFixtures.java"
    java -cp "$CLASSES/generator:$CLASSES/legacy/binary-java:$BASE_JARS/bluetape4k-io-1.12.0.jar:$base_test_cp" \
        io.bluetape4k.io.serializer.compat.issue754.java.GenerateBinaryFixtures "$generated"

    local name
    for name in jdk-simple-data.bin kryo-default-simple-data.bin kryo-fast-simple-data.bin \
        fory-default-simple-data.bin fory-fast-simple-data.bin; do
        cmp "$generated/$name" "$FIXTURE_ROOT/binary/$name" || fail "baseline fixture is not reproducible: $name"
    done
}

build_current_jars() {
    rm -f "$ROOT/io/io/build/libs"/bluetape4k-io-*.jar
    rm -f "$ROOT/io/json/build/libs"/bluetape4k-json-*.jar
    rm -f "$ROOT/io/avro/build/libs"/bluetape4k-avro-*.jar
    "$ROOT/gradlew" -p "$ROOT" \
        :bluetape4k-io:jar :bluetape4k-json:jar :bluetape4k-avro:jar \
        --no-configuration-cache
}

compile_new_caller() {
    local label="$1"
    local output="$2"
    local classpath="$3"
    shift 3
    if javac -source 21 -target 21 -cp "$classpath" -d "$output" "$@" >> "$REPORT" 2>&1; then
        echo "$label-new-caller-compilation=PASS" | tee -a "$REPORT"
        return 0
    fi
    echo "$label-new-caller-compilation=FAIL" | tee -a "$REPORT"
    return 1
}

assert_default_method() {
    local jar="$1"
    local class_name="$2"
    local method="$3"
    javap -classpath "$jar" "$class_name" | grep -Eq " default .*${method}\\(" ||
        fail "$class_name.$method is not an executable JVM default"
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
[[ -n "$EXPECTED_HEAD" ]] || fail "--expected-head is required"
[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$EXPECTED_HEAD" ]] ||
    fail "current HEAD does not match --expected-head"
command -v javac >/dev/null || fail "javac is required"
command -v java >/dev/null || fail "java is required"
command -v javap >/dev/null || fail "javap is required"
command -v kotlinc >/dev/null || fail "kotlinc is required"

mkdir -p "$AUTH_DIR"
: > "$REPORT"
write_classpath_init_script
ensure_base_worktree
verify_base_jars
verify_frozen_fixtures

BASE_AVRO_TEST_CP="$(runtime_classpath "$BASE_WORKTREE" "bluetape4k-avro" "test")"
AVRO_API_JAR="$(avro_api_jar "$BASE_AVRO_TEST_CP")"
compile_legacy_fixtures "$AVRO_API_JAR"
BASE_IO_TEST_CP="$(runtime_classpath "$BASE_WORKTREE" "bluetape4k-io" "test")"
regenerate_and_compare_fixtures "$BASE_IO_TEST_CP"

echo "authority-commit=$BASE_SHA" | tee -a "$REPORT"
echo "authority-tree=$BASE_TREE" | tee -a "$REPORT"
echo "legacy-java-compilation=PASS" | tee -a "$REPORT"
echo "legacy-kotlin-compilation=PASS" | tee -a "$REPORT"
echo "java-null-literal-compilation=PASS" | tee -a "$REPORT"
echo "frozen-fixture-reproduction=PASS" | tee -a "$REPORT"

build_current_jars
CURRENT_IO_JAR="$(resolve_single_jar "$ROOT/io/io/build/libs" "bluetape4k-io")"
CURRENT_JSON_JAR="$(resolve_single_jar "$ROOT/io/json/build/libs" "bluetape4k-json")"
CURRENT_AVRO_JAR="$(resolve_single_jar "$ROOT/io/avro/build/libs" "bluetape4k-avro")"
CURRENT_HEAD="$(git -C "$ROOT" rev-parse HEAD)"
CURRENT_TREE="$(git -C "$ROOT" rev-parse 'HEAD^{tree}')"

echo "current-head=$CURRENT_HEAD" | tee -a "$REPORT"
echo "current-head-tree=$CURRENT_TREE" | tee -a "$REPORT"
echo "current-io-jar=$CURRENT_IO_JAR sha256=$(sha256 "$CURRENT_IO_JAR")" | tee -a "$REPORT"
echo "current-json-jar=$CURRENT_JSON_JAR sha256=$(sha256 "$CURRENT_JSON_JAR")" | tee -a "$REPORT"
echo "current-avro-jar=$CURRENT_AVRO_JAR sha256=$(sha256 "$CURRENT_AVRO_JAR")" | tee -a "$REPORT"

rm -rf "$CLASSES/current"
mkdir -p "$CLASSES/current/binary" "$CLASSES/current/json" "$CLASSES/current/avro"
new_failures=0
compile_new_caller binary "$CLASSES/current/binary" \
    "$CURRENT_IO_JAR:$CLASSES/legacy/binary-java:$CLASSES/legacy/binary-kotlin" \
    "$ROOT/io/io/src/test/resources/compat/issue-754/src/java/NewBinaryBufferCaller.java" || new_failures=$((new_failures + 1))
compile_new_caller json "$CLASSES/current/json" \
    "$CURRENT_JSON_JAR:$CLASSES/legacy/json-java:$CLASSES/legacy/json-kotlin" \
    "$ROOT/io/json/src/test/resources/compat/issue-754/src/java/NewJsonBufferCaller.java" || new_failures=$((new_failures + 1))
compile_new_caller avro "$CLASSES/current/avro" \
    "$CURRENT_AVRO_JAR:$CURRENT_IO_JAR:$AVRO_API_JAR:$CLASSES/legacy/avro-java:$CLASSES/legacy/avro-kotlin" \
    "$ROOT/io/avro/src/test/resources/compat/issue-754/src/java/NewAvroBufferCaller.java" || new_failures=$((new_failures + 1))

if [[ "$new_failures" -ne 0 ]]; then
    echo "buffer-default-abi=RED ($new_failures new caller compilations failed)" | tee -a "$REPORT"
    echo "ABI report: $REPORT" >&2
    exit 1
fi

assert_default_method "$CURRENT_IO_JAR" "io.bluetape4k.io.serializer.BinarySerializer" "serializeTo"
assert_default_method "$CURRENT_IO_JAR" "io.bluetape4k.io.serializer.BinarySerializer" "deserializeFrom"
assert_default_method "$CURRENT_JSON_JAR" "io.bluetape4k.json.JsonSerializer" "serializeTo"
assert_default_method "$CURRENT_JSON_JAR" "io.bluetape4k.json.JsonSerializer" "deserializeFrom"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroReflectSerializer" "serializeTo"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroReflectSerializer" "deserializeFrom"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroGenericRecordSerializer" "serializeTo"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroGenericRecordSerializer" "deserializeFrom"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroSpecificRecordSerializer" "serializeTo"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroSpecificRecordSerializer" "deserializeFrom"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroSpecificRecordSerializer" "serializeListTo"
assert_default_method "$CURRENT_AVRO_JAR" "io.bluetape4k.avro.AvroSpecificRecordSerializer" "deserializeListFrom"
javap -classpath "$CURRENT_IO_JAR" io.bluetape4k.io.serializer.BinarySerializerSupportKt |
    grep -Eq 'deserialize\(io\.bluetape4k\.io\.serializer\.BinarySerializer, java\.nio\.ByteBuffer\)' ||
    fail "legacy BinarySerializer.deserialize(ByteBuffer) static JVM symbol is missing"

CURRENT_IO_CP="$(runtime_classpath "$ROOT" "bluetape4k-io" "main")"
CURRENT_JSON_CP="$(runtime_classpath "$ROOT" "bluetape4k-json" "main")"
CURRENT_AVRO_CP="$(runtime_classpath "$ROOT" "bluetape4k-avro" "main")"

java -cp "$CLASSES/current/binary:$CLASSES/legacy/binary-java:$CLASSES/legacy/binary-kotlin:$CURRENT_IO_JAR:$CURRENT_IO_CP" \
    io.bluetape4k.io.serializer.compat.issue754.java.NewBinaryBufferCaller | tee -a "$REPORT"
java -cp "$CLASSES/current/json:$CLASSES/legacy/json-java:$CLASSES/legacy/json-kotlin:$CURRENT_JSON_JAR:$CURRENT_JSON_CP" \
    io.bluetape4k.json.compat.issue754.java.NewJsonBufferCaller | tee -a "$REPORT"
java -cp "$CLASSES/current/avro:$CLASSES/legacy/avro-java:$CLASSES/legacy/avro-kotlin:$CURRENT_AVRO_JAR:$CURRENT_IO_JAR:$CURRENT_AVRO_CP" \
    io.bluetape4k.avro.compat.issue754.java.NewAvroBufferCaller | tee -a "$REPORT"

echo "buffer-default-abi=PASS" | tee -a "$REPORT"
echo "ABI report: $REPORT"
