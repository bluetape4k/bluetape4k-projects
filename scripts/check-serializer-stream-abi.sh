#!/usr/bin/env bash

# Issue 756 stream compatibility matrix. The stable buffer ABI entrypoint dispatches here for --scope runs.

set -euo pipefail

readonly RELEASE_SHA="6187173b58e8b4c5c435c145e00e94708f31ef75"
readonly RELEASE_TREE="daa12f3cfb185926fe2ff09e571288059953d85c"
readonly BASE_SHA="b00cc5440e47ad803e5aac21528b560fdd3b0474"
readonly BASE_TREE="48f9dee849a0c3de0a89c3b05ff5c827c9233fce"

ROOT="$(git rev-parse --show-toplevel)"
COMMON_DIR="$(git rev-parse --path-format=absolute --git-common-dir)"
MAIN_ROOT="$(dirname "$COMMON_DIR")"
AUTH_DIR="$ROOT/.codex/compat/issue-756"
JAR_DIR="$AUTH_DIR/jars"
CLASS_DIR="$AUTH_DIR/classes"
REPORT="$AUTH_DIR/abi-report.txt"
INIT_SCRIPT="$AUTH_DIR/issue756-classpaths.init.gradle"
BINARY_FIXTURES="$ROOT/io/io/src/test/resources/compat/issue-756/src"
JSON_FIXTURES="$ROOT/io/json/src/test/resources/compat/issue-756/src"
RELEASE_WORKTREE=""
BASE_WORKTREE=""
EXPECTED_HEAD=""
EXPECTED_TREE=""
SCOPE=""
BUILD_CURRENT=false
REQUIRE_DIRECT=""
CREATED_WORKTREES=()
JDK_JAVA="${JAVA_HOME:-}/bin/java"
GRADLE_TOOLCHAIN_ARGS=(
    --no-daemon
    --no-configuration-cache
    -Dorg.gradle.java.installations.auto-detect=false
    -Dorg.gradle.java.installations.auto-download=false
    "-Dorg.gradle.java.installations.paths=${JAVA_HOME:-}"
    --console=plain
)

usage() {
    echo "Usage: $0 --scope <interface|full> --build-current --expected-head <full-git-sha> [--require-direct-candidates jdk,kryo,jackson2,jackson3]"
}

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

sha256() {
    shasum -a 256 "$1" | awk '{print $1}'
}

javac() {
    "$JDK_JAVA" -m jdk.compiler/com.sun.tools.javac.Main "$@"
}

javap() {
    "$JDK_JAVA" -m jdk.jdeps/com.sun.tools.javap.Main "$@"
}

cleanup() {
    local worktree
    local cleanup_failed=false
    [[ "${#CREATED_WORKTREES[@]}" -gt 0 ]] || return 0
    for worktree in "${CREATED_WORKTREES[@]}"; do
        if [[ -d "$worktree" ]]; then
            if ! git -C "$MAIN_ROOT" worktree remove --force "$worktree" >/dev/null 2>&1; then
                echo "ERROR: failed to remove compatibility worktree: $worktree" >&2
                cleanup_failed=true
            fi
        else
            if ! git -C "$MAIN_ROOT" worktree prune --expire now >/dev/null 2>&1; then
                echo "ERROR: failed to prune missing compatibility worktree metadata: $worktree" >&2
                cleanup_failed=true
            fi
        fi
        if git -C "$MAIN_ROOT" worktree list --porcelain | grep -Fx "worktree $worktree" >/dev/null; then
            echo "ERROR: compatibility worktree metadata remains registered: $worktree" >&2
            cleanup_failed=true
        fi
    done
    CREATED_WORKTREES=()
    [[ "$cleanup_failed" == false ]]
}

finish() {
    local exit_code=$?
    trap - EXIT
    if ! cleanup; then
        exit_code=1
    fi
    exit "$exit_code"
}

trap finish EXIT

assert_clean_inputs() {
    python3 - "$ROOT" <<'PY'
import pathlib
import subprocess
import sys

root = pathlib.Path(sys.argv[1])

def in_scope(path: str) -> bool:
    return (
        path.startswith((
            "io/io/src/main/",
            "io/json/src/main/",
            "io/jackson2/src/main/",
            "io/jackson3/src/main/",
            "io/io/src/test/resources/compat/issue-756/",
            "io/json/src/test/resources/compat/issue-756/",
            "buildSrc/",
            "gradle/",
        ))
        or path in {
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            "io/io/build.gradle.kts",
            "io/json/build.gradle.kts",
            "io/jackson2/build.gradle.kts",
            "io/jackson3/build.gradle.kts",
            "scripts/check-serializer-buffer-abi.sh",
            "scripts/check-serializer-stream-abi.sh",
        }
    )

dirty = set()
for command in (
    ("diff", "--name-only", "-z"),
    ("diff", "--cached", "--name-only", "-z"),
    ("ls-files", "--others", "--exclude-standard", "-z"),
):
    output = subprocess.run(["git", *command], cwd=root, check=True, capture_output=True).stdout
    dirty.update(
        raw.decode("utf-8", errors="surrogateescape")
        for raw in output.split(b"\0")
        if raw and in_scope(raw.decode("utf-8", errors="surrogateescape"))
    )

if dirty:
    raise SystemExit("dirty serializer source/fixture/script inputs: " + ", ".join(sorted(dirty)))
print("SERIALIZER ABI INPUTS CLEAN")
PY
}

write_init_script() {
    mkdir -p "$AUTH_DIR"
    cat > "$INIT_SCRIPT" <<'GRADLE'
import org.gradle.api.tasks.SourceSetContainer

gradle.afterProject { project, state ->
    if (!state.failure && project.path in [
        ":bluetape4k-io",
        ":bluetape4k-json",
        ":bluetape4k-jackson2",
        ":bluetape4k-jackson3",
    ]) {
        project.tasks.register("issue756PrintRuntimeClasspath") {
            doLast {
                def main = project.extensions.getByType(SourceSetContainer).getByName("main")
                def consumerClasspath = project.files(main.compileClasspath, main.runtimeClasspath)
                println("ISSUE756_CONSUMER_CLASSPATH=" + consumerClasspath.asPath)
            }
        }
    }
}
GRADLE
}

runtime_classpath() {
    local worktree="$1"
    local project="$2"
    local output classpath marker_count
    output="$(
        "$worktree/gradlew" -q -p "$worktree" -I "$INIT_SCRIPT" \
            ":$project:issue756PrintRuntimeClasspath" "${GRADLE_TOOLCHAIN_ARGS[@]}"
    )" || fail "failed to resolve consumer classpath for $project in $worktree"
    classpath="$(printf '%s\n' "$output" | sed -n 's/^ISSUE756_CONSUMER_CLASSPATH=//p')"
    marker_count="$(printf '%s\n' "$output" | grep -c '^ISSUE756_CONSUMER_CLASSPATH=' || true)"
    [[ "$marker_count" == "1" && -n "$classpath" ]] ||
        fail "expected one consumer classpath marker for $project in $worktree, found $marker_count"
    printf '%s\n' "$classpath"
}

create_authority_worktree() {
    local variable="$1"
    local label="$2"
    local commit="$3"
    local tree="$4"
    local parent="$MAIN_ROOT/.worktrees/compat"
    local worktree
    mkdir -p "$parent"
    worktree="$(mktemp -d "$parent/issue-756-$label.XXXXXX")"
    rmdir "$worktree"
    git -C "$MAIN_ROOT" worktree add --detach "$worktree" "$commit" >/dev/null
    CREATED_WORKTREES+=("$worktree")
    [[ "$(git -C "$worktree" rev-parse HEAD)" == "$commit" ]] || fail "$label authority commit mismatch"
    [[ "$(git -C "$worktree" rev-parse 'HEAD^{tree}')" == "$tree" ]] || fail "$label authority tree mismatch"
    [[ -z "$(git -C "$worktree" status --porcelain --untracked-files=all)" ]] || fail "$label authority worktree is dirty"
    printf -v "$variable" '%s' "$worktree"
}

resolve_single_jar() {
    local directory="$1"
    local prefix="$2"
    local candidates count
    candidates="$(find "$directory" -maxdepth 1 -type f -name "$prefix-*.jar" | sort)"
    count="$(printf '%s\n' "$candidates" | sed '/^$/d' | wc -l | tr -d ' ')"
    [[ "$count" == "1" ]] || fail "expected one $prefix jar in $directory, found $count: $candidates"
    printf '%s\n' "$candidates"
}

build_authority_jars() {
    local label="$1"
    local worktree="$2"
    rm -f "$worktree/io/io/build/libs"/bluetape4k-io-*.jar
    rm -f "$worktree/io/json/build/libs"/bluetape4k-json-*.jar
    "$worktree/gradlew" -p "$worktree" :bluetape4k-io:jar :bluetape4k-json:jar \
        "${GRADLE_TOOLCHAIN_ARGS[@]}"
    cp "$(resolve_single_jar "$worktree/io/io/build/libs" bluetape4k-io)" "$JAR_DIR/$label-io.jar"
    cp "$(resolve_single_jar "$worktree/io/json/build/libs" bluetape4k-json)" "$JAR_DIR/$label-json.jar"
}

build_current_jars() {
    rm -f "$ROOT/io/io/build/libs"/bluetape4k-io-*.jar
    rm -f "$ROOT/io/json/build/libs"/bluetape4k-json-*.jar
    local tasks=(:bluetape4k-io:jar :bluetape4k-json:jar)
    if [[ "$SCOPE" == "full" ]]; then
        rm -f "$ROOT/io/jackson2/build/libs"/bluetape4k-jackson2-*.jar
        rm -f "$ROOT/io/jackson3/build/libs"/bluetape4k-jackson3-*.jar
        tasks+=(:bluetape4k-jackson2:jar :bluetape4k-jackson3:jar)
    fi
    "$ROOT/gradlew" -p "$ROOT" "${tasks[@]}" "${GRADLE_TOOLCHAIN_ARGS[@]}"
    cp "$(resolve_single_jar "$ROOT/io/io/build/libs" bluetape4k-io)" "$JAR_DIR/current-io.jar"
    cp "$(resolve_single_jar "$ROOT/io/json/build/libs" bluetape4k-json)" "$JAR_DIR/current-json.jar"
    if [[ "$SCOPE" == "full" ]]; then
        cp "$(resolve_single_jar "$ROOT/io/jackson2/build/libs" bluetape4k-jackson2)" "$JAR_DIR/current-jackson2.jar"
        cp "$(resolve_single_jar "$ROOT/io/jackson3/build/libs" bluetape4k-jackson3)" "$JAR_DIR/current-jackson3.jar"
    fi
}

compile_kotlin() {
    local output="$1"
    local classpath="$2"
    shift 2
    mkdir -p "$output"
    kotlinc -jvm-target 21 -classpath "$classpath" -d "$output" "$@"
}

compile_authority_fixtures() {
    local label="$1"
    local worktree="$2"
    local io_jar="$JAR_DIR/$label-io.jar"
    local json_jar="$JAR_DIR/$label-json.jar"
    local output="$CLASS_DIR/$label"
    local io_cp json_cp
    io_cp="$(runtime_classpath "$worktree" bluetape4k-io)"
    json_cp="$(runtime_classpath "$worktree" bluetape4k-json)"
    mkdir -p "$output"/{binary-java,binary-kotlin,json-java,json-kotlin,dual-java,dual-kotlin}

    javac --release 21 -cp "$io_jar:$io_cp" -d "$output/binary-java" \
        "$BINARY_FIXTURES/java/LegacyBinaryImplementation.java" \
        "$BINARY_FIXTURES/java/LegacyBinaryStreamCaller.java"
    compile_kotlin "$output/binary-kotlin" "$io_jar:$io_cp" \
        "$BINARY_FIXTURES/kotlin/LegacyBinaryStreamCaller.kt"
    javac --release 21 -cp "$json_jar:$json_cp" -d "$output/json-java" \
        "$JSON_FIXTURES/java/LegacyJsonImplementation.java" \
        "$JSON_FIXTURES/java/LegacyJsonStreamCaller.java"
    compile_kotlin "$output/json-kotlin" "$json_jar:$json_cp" \
        "$JSON_FIXTURES/kotlin/LegacyJsonStreamCaller.kt"
    javac --release 21 -cp "$io_jar:$json_jar:$io_cp:$json_cp" -d "$output/dual-java" \
        "$JSON_FIXTURES/java/LegacyDualSerializer.java"
    compile_kotlin "$output/dual-kotlin" "$io_jar:$json_jar:$io_cp:$json_cp" \
        "$JSON_FIXTURES/kotlin/LegacyDualSerializer.kt"

    if [[ "$SCOPE" == "full" ]]; then
        mkdir -p "$output/decorator-java" "$output/decorator-kotlin"
        javac --release 21 -cp "$io_jar:$io_cp" -d "$output/decorator-java" \
            "$BINARY_FIXTURES/java/LegacyBinaryDecorator.java"
        compile_kotlin "$output/decorator-kotlin" "$io_jar:$io_cp" \
            "$BINARY_FIXTURES/kotlin/LegacyBinaryDecorator.kt"
    fi
}

compile_current_fixtures() {
    local output="$CLASS_DIR/current"
    local current_cp="$JAR_DIR/current-io.jar:$JAR_DIR/current-json.jar"
    local io_cp json_cp jackson2_cp jackson3_cp
    io_cp="$(runtime_classpath "$ROOT" bluetape4k-io)"
    json_cp="$(runtime_classpath "$ROOT" bluetape4k-json)"
    current_cp="$current_cp:$io_cp:$json_cp"
    mkdir -p "$output"/{dual-java,dual-kotlin,caller,null-base,null-current}
    javac --release 21 -cp "$current_cp" -d "$output/dual-java" \
        "$JSON_FIXTURES/java/LegacyDualSerializer.java"
    compile_kotlin "$output/dual-kotlin" "$current_cp" \
        "$JSON_FIXTURES/kotlin/LegacyDualSerializer.kt"
    javac --release 21 -cp "$current_cp" -d "$output/caller" \
        "$BINARY_FIXTURES/java/ConcreteSerializerStreamCaller.java"

    if [[ "$SCOPE" == "full" ]]; then
        jackson2_cp="$(runtime_classpath "$ROOT" bluetape4k-jackson2)"
        jackson3_cp="$(runtime_classpath "$ROOT" bluetape4k-jackson3)"
        mkdir -p "$output/direct" "$output/decorator-java" "$output/decorator-kotlin"
        javac --release 21 \
            -cp "$current_cp:$JAR_DIR/current-jackson2.jar:$JAR_DIR/current-jackson3.jar:$jackson2_cp:$jackson3_cp" \
            -d "$output/direct" \
            "$BINARY_FIXTURES/java/ConcreteDirectSerializerStreamCaller.java"
        javac --release 21 -cp "$current_cp" -d "$output/decorator-java" \
            "$BINARY_FIXTURES/java/LegacyBinaryDecorator.java"
        compile_kotlin "$output/decorator-kotlin" "$current_cp" \
            "$BINARY_FIXTURES/kotlin/LegacyBinaryDecorator.kt"
        echo "candidate-old-decorator-source-recompile=PASS" | tee -a "$REPORT"
    fi

    cat > "$AUTH_DIR/OldNullSerializeToCaller.java" <<'JAVA'
import io.bluetape4k.io.serializer.BinarySerializer;
import io.bluetape4k.json.JsonSerializer;

final class OldNullSerializeToCaller {
    static void call(BinarySerializer binary, JsonSerializer json) {
        binary.serializeTo("binary", null);
        json.serializeTo("json", null);
    }
}
JAVA
    javac --release 21 -cp "$JAR_DIR/base-io.jar:$JAR_DIR/base-json.jar" \
        -d "$output/null-base" "$AUTH_DIR/OldNullSerializeToCaller.java"
    javac --release 21 -cp "$current_cp" -d "$output/null-current" \
        "$AUTH_DIR/OldNullSerializeToCaller.java"
    echo "old-null-literal-base-current=PASS" | tee -a "$REPORT"
}

run_authority_fixtures() {
    local label="$1"
    local output="$CLASS_DIR/$label"
    local current="$JAR_DIR/current-io.jar:$JAR_DIR/current-json.jar"
    local io_cp json_cp
    io_cp="$(runtime_classpath "$ROOT" bluetape4k-io)"
    json_cp="$(runtime_classpath "$ROOT" bluetape4k-json)"
    "$JDK_JAVA" -cp "$output/binary-java:$current:$io_cp" \
        io.bluetape4k.io.serializer.compat.issue756.java.LegacyBinaryStreamCaller | tee -a "$REPORT"
    "$JDK_JAVA" -cp "$output/binary-kotlin:$current:$io_cp" \
        io.bluetape4k.io.serializer.compat.issue756.kotlin.LegacyBinaryStreamCallerKt | tee -a "$REPORT"
    "$JDK_JAVA" -cp "$output/json-java:$current:$json_cp" \
        io.bluetape4k.json.compat.issue756.java.LegacyJsonStreamCaller | tee -a "$REPORT"
    "$JDK_JAVA" -cp "$output/json-kotlin:$current:$json_cp" \
        io.bluetape4k.json.compat.issue756.kotlin.LegacyJsonStreamCallerKt | tee -a "$REPORT"
    "$JDK_JAVA" -cp "$CLASS_DIR/current/caller:$output/dual-java:$output/dual-kotlin:$current:$io_cp:$json_cp" \
        io.bluetape4k.io.serializer.compat.issue756.java.ConcreteSerializerStreamCaller dual | tee -a "$REPORT"
    echo "$label-old-callers-implementors=PASS" | tee -a "$REPORT"
    echo "$label-dual-source-defaults=PASS" | tee -a "$REPORT"
}

run_current_dual_fixture() {
    local current="$JAR_DIR/current-io.jar:$JAR_DIR/current-json.jar"
    local io_cp json_cp
    io_cp="$(runtime_classpath "$ROOT" bluetape4k-io)"
    json_cp="$(runtime_classpath "$ROOT" bluetape4k-json)"
    "$JDK_JAVA" -cp "$CLASS_DIR/current/caller:$CLASS_DIR/current/dual-java:$CLASS_DIR/current/dual-kotlin:$current:$io_cp:$json_cp" \
        io.bluetape4k.io.serializer.compat.issue756.java.ConcreteSerializerStreamCaller dual | tee -a "$REPORT"
    echo "candidate-null-literal-distinct-streams=PASS" | tee -a "$REPORT"
    echo "reflection-interface-defaults=PASS" | tee -a "$REPORT"
}

assert_default_throws() {
    local jar="$1"
    local class_name="$2"
    local method="$3"
    local report
    report="$AUTH_DIR/$(printf '%s-%s' "$class_name" "$method" | tr '. ' '--').javap.txt"
    javap -classpath "$jar" -p "$class_name" > "$report"
    grep -F "public default int $method(java.lang.Object, java.io.OutputStream) throws java.io.IOException;" "$report" >/dev/null ||
        fail "$class_name.$method is not a default exposing throws java.io.IOException"
}

assert_declared_throws() {
    local classpath="$1"
    local class_name="$2"
    local method="$3"
    local report
    report="$AUTH_DIR/$(printf '%s-%s' "$class_name" "$method" | tr '. ' '--').javap.txt"
    javap -classpath "$classpath" -p "$class_name" > "$report"
    grep -F "public int $method(java.lang.Object, java.io.OutputStream) throws java.io.IOException;" "$report" >/dev/null ||
        fail "$class_name.$method is not directly declared with throws java.io.IOException"
}

verify_interface_javap() {
    assert_default_throws "$JAR_DIR/current-io.jar" \
        io.bluetape4k.io.serializer.BinarySerializer serializeBinaryToStream
    assert_default_throws "$JAR_DIR/current-json.jar" \
        io.bluetape4k.json.JsonSerializer serializeJsonToStream
    echo "interface-method-is-default=PASS" | tee -a "$REPORT"
    echo "interface-checked-ioexception=PASS" | tee -a "$REPORT"
}

candidate_spec() {
    case "$1" in
        jdk) echo "$JAR_DIR/current-io.jar|io.bluetape4k.io.serializer.JdkBinarySerializer|serializeBinaryToStream" ;;
        kryo) echo "$JAR_DIR/current-io.jar|io.bluetape4k.io.serializer.KryoBinarySerializer|serializeBinaryToStream" ;;
        jackson2) echo "$JAR_DIR/current-jackson2.jar:$JAR_DIR/current-json.jar|io.bluetape4k.jackson.JacksonSerializer|serializeJsonToStream" ;;
        jackson3) echo "$JAR_DIR/current-jackson3.jar:$JAR_DIR/current-json.jar|io.bluetape4k.jackson3.JacksonSerializer|serializeJsonToStream" ;;
        *) fail "unrecognized direct candidate: $1" ;;
    esac
}

is_required_direct() {
    local name="$1"
    [[ ",${REQUIRE_DIRECT}," == *",$name,"* ]]
}

verify_full_scope() {
    local current="$JAR_DIR/current-io.jar:$JAR_DIR/current-json.jar"
    local io_cp json_cp jackson2_cp jackson3_cp
    io_cp="$(runtime_classpath "$ROOT" bluetape4k-io)"
    json_cp="$(runtime_classpath "$ROOT" bluetape4k-json)"
    jackson2_cp="$(runtime_classpath "$ROOT" bluetape4k-jackson2)"
    jackson3_cp="$(runtime_classpath "$ROOT" bluetape4k-jackson3)"
    assert_declared_throws "$JAR_DIR/current-io.jar" \
        io.bluetape4k.io.serializer.BinarySerializerDecorator serializeBinaryToStream
    assert_declared_throws "$JAR_DIR/current-io.jar" \
        io.bluetape4k.io.serializer.CompressableBinarySerializer serializeBinaryToStream

    local label output
    for label in release base current; do
        output="$CLASS_DIR/$label"
        "$JDK_JAVA" -cp "$CLASS_DIR/current/caller:$output/decorator-java:$output/decorator-kotlin:$current:$io_cp:$json_cp" \
            io.bluetape4k.io.serializer.compat.issue756.java.ConcreteSerializerStreamCaller decorator | tee -a "$REPORT"
    done

    local name spec classpath class_name method javap_report
    for name in jdk kryo jackson2 jackson3; do
        spec="$(candidate_spec "$name")"
        IFS='|' read -r classpath class_name method <<< "$spec"
        javap_report="$AUTH_DIR/direct-$name.javap.txt"
        javap -classpath "$classpath" -p "$class_name" > "$javap_report" || fail "missing concrete candidate: $name"
        if grep -F "public int $method(java.lang.Object, java.io.OutputStream) throws java.io.IOException;" "$javap_report" >/dev/null; then
            echo "direct-candidate-$name=DECLARED" | tee -a "$REPORT"
        else
            echo "direct-candidate-$name=INHERITED" | tee -a "$REPORT"
            is_required_direct "$name" && fail "required direct candidate inherits instead of declaring stream dispatch: $name"
        fi
    done
    "$JDK_JAVA" \
        -cp "$CLASS_DIR/current/direct:$current:$JAR_DIR/current-jackson2.jar:$JAR_DIR/current-jackson3.jar:$io_cp:$json_cp:$jackson2_cp:$jackson3_cp" \
        io.bluetape4k.io.serializer.compat.issue756.java.ConcreteDirectSerializerStreamCaller | tee -a "$REPORT"
    echo "concrete-type-java-callers=PASS" | tee -a "$REPORT"
    echo "decorator-concrete-stream-abi=PASS" | tee -a "$REPORT"
}

write_hash_report() {
    local current_tree
    current_tree="$(git -C "$ROOT" rev-parse 'HEAD^{tree}')"
    [[ "$current_tree" == "$EXPECTED_TREE" ]] || fail "candidate tree changed before hash reporting"
    {
        echo "scope=$SCOPE"
        echo "release-commit=$RELEASE_SHA"
        echo "release-tree=$RELEASE_TREE"
        echo "release-io-jar=$JAR_DIR/release-io.jar sha256=$(sha256 "$JAR_DIR/release-io.jar")"
        echo "release-json-jar=$JAR_DIR/release-json.jar sha256=$(sha256 "$JAR_DIR/release-json.jar")"
        echo "base-commit=$BASE_SHA"
        echo "base-tree=$BASE_TREE"
        echo "base-io-jar=$JAR_DIR/base-io.jar sha256=$(sha256 "$JAR_DIR/base-io.jar")"
        echo "base-json-jar=$JAR_DIR/base-json.jar sha256=$(sha256 "$JAR_DIR/base-json.jar")"
        echo "current-commit=$EXPECTED_HEAD"
        echo "current-tree=$current_tree"
        echo "current-io-jar=$JAR_DIR/current-io.jar sha256=$(sha256 "$JAR_DIR/current-io.jar")"
        echo "current-json-jar=$JAR_DIR/current-json.jar sha256=$(sha256 "$JAR_DIR/current-json.jar")"
    } >> "$REPORT"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --scope)
            [[ $# -ge 2 ]] || fail "--scope requires interface or full"
            SCOPE="$2"
            shift 2
            ;;
        --build-current)
            BUILD_CURRENT=true
            shift
            ;;
        --expected-head)
            [[ $# -ge 2 ]] || fail "--expected-head requires a commit"
            EXPECTED_HEAD="$2"
            shift 2
            ;;
        --require-direct-candidates)
            [[ $# -ge 2 ]] || fail "--require-direct-candidates requires a comma-separated list"
            REQUIRE_DIRECT="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            usage >&2
            fail "unrecognized argument: $1"
            ;;
    esac
done

[[ "$SCOPE" == "interface" || "$SCOPE" == "full" ]] || fail "unrecognized scope: $SCOPE"
[[ "$BUILD_CURRENT" == true ]] || fail "--build-current is required"
[[ "$EXPECTED_HEAD" =~ ^[0-9a-f]{40}$ ]] || fail "full lowercase --expected-head is required"
[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$EXPECTED_HEAD" ]] || fail "current HEAD does not match --expected-head"
EXPECTED_TREE="$(git -C "$ROOT" rev-parse "$EXPECTED_HEAD^{tree}")"
[[ "$SCOPE" == "full" || -z "$REQUIRE_DIRECT" ]] || fail "--require-direct-candidates requires --scope full"
if [[ -n "$REQUIRE_DIRECT" ]]; then
    [[ "$REQUIRE_DIRECT" =~ ^(jdk|kryo|jackson2|jackson3)(,(jdk|kryo|jackson2|jackson3))*$ ]] ||
        fail "invalid --require-direct-candidates list: $REQUIRE_DIRECT"
fi

[[ -n "${JAVA_HOME:-}" ]] || fail "JAVA_HOME is required"
[[ -x "$JDK_JAVA" ]] || fail "JAVA_HOME does not provide an executable java launcher"
for tool in git kotlinc python3 shasum awk find sed grep; do
    command -v "$tool" >/dev/null || fail "$tool is required"
done
"$JDK_JAVA" -m jdk.compiler/com.sun.tools.javac.Main -version >/dev/null
"$JDK_JAVA" -m jdk.jdeps/com.sun.tools.javap.Main -version >/dev/null
assert_clean_inputs

rm -rf "$CLASS_DIR" "$JAR_DIR"
mkdir -p "$CLASS_DIR" "$JAR_DIR"
: > "$REPORT"
write_init_script
create_authority_worktree RELEASE_WORKTREE release "$RELEASE_SHA" "$RELEASE_TREE"
create_authority_worktree BASE_WORKTREE base "$BASE_SHA" "$BASE_TREE"
build_authority_jars release "$RELEASE_WORKTREE"
build_authority_jars base "$BASE_WORKTREE"
build_current_jars
write_hash_report
compile_authority_fixtures release "$RELEASE_WORKTREE"
compile_authority_fixtures base "$BASE_WORKTREE"
compile_current_fixtures
run_authority_fixtures release
run_authority_fixtures base
run_current_dual_fixture
verify_interface_javap

if [[ "$SCOPE" == "full" ]]; then
    verify_full_scope
else
    echo "decorator-concrete-stream-abi=DEFERRED" | tee -a "$REPORT"
fi

[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$EXPECTED_HEAD" ]] || fail "current HEAD changed during ABI evidence generation"
[[ "$(git -C "$ROOT" rev-parse 'HEAD^{tree}')" == "$EXPECTED_TREE" ]] || fail "current tree changed during ABI evidence generation"
assert_clean_inputs
cleanup
trap - EXIT
echo "SERIALIZER STREAM ABI PASS scope=$SCOPE head=$EXPECTED_HEAD" | tee -a "$REPORT"
echo "ABI report: $REPORT"
