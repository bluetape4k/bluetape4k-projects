#!/usr/bin/env python3
"""Build and run pinned issue 756 Lettuce codec allocation evidence."""

import argparse
import hashlib
import importlib.util
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import warnings
import zipfile
from datetime import datetime, timezone
from pathlib import Path


HERE = Path(__file__).resolve().parent
REPOSITORY_ROOT = HERE.parents[2]
BENCHMARK_CLASS = "io.bluetape4k.redis.lettuce.benchmark.LettuceCodecBenchmark"
PREFLIGHT_CLASS = (
    "io.bluetape4k.redis.lettuce.benchmark.LettuceCodecBenchmarkPreflight"
)
BACKENDS = ("jdk", "kryo", "jackson2", "jackson3")
TARGETS = ("heap", "direct")
BACKEND_CLASSES = {
    "jdk": "io.bluetape4k.io.serializer.JdkBinarySerializer",
    "kryo": "io.bluetape4k.io.serializer.KryoBinarySerializer",
    "jackson2": "io.bluetape4k.jackson.JacksonSerializer",
    "jackson3": "io.bluetape4k.jackson3.JacksonSerializer",
}
PROTOCOL = {
    "forks": 2,
    "warmup_iterations": 3,
    "measurement_iterations": 5,
    "threads": 1,
    "profiler": "gc",
    "mode": "thrpt",
    "throughput_unit": "ops/ms",
}
FIXTURE = {
    "allocator_class": "io.netty.buffer.PooledByteBufAllocator",
    "pooled": True,
    "capacity": 512,
    "max_capacity": 512,
    "reader_index": 3,
    "writer_index": 7,
    "headroom": 505,
}
POOLED_FIXTURE_FIELDS = (
    "heap_allocator_class",
    "direct_allocator_class",
    "heap_buffer_class",
    "direct_buffer_class",
    "num_heap_arenas",
    "num_direct_arenas",
)


def _method_name(backend, target, path):
    suffix = "CopiedBaseline" if path == "baseline" else "Candidate"
    return f"{backend}{target.title()}{suffix}"


def expected_matrix():
    result = []
    for backend in BACKENDS:
        for target in TARGETS:
            baseline = _method_name(backend, target, "baseline")
            candidate = _method_name(backend, target, "candidate")
            result.extend(
                (
                    {
                        "backend": backend,
                        "target": target,
                        "path": "baseline",
                        "method": baseline,
                        "paired_baseline": baseline,
                    },
                    {
                        "backend": backend,
                        "target": target,
                        "path": "candidate",
                        "method": candidate,
                        "paired_baseline": baseline,
                    },
                )
            )
    return result


EXPECTED_MATRIX = tuple(expected_matrix())
EXPECTED_METHODS = tuple(cell["method"] for cell in EXPECTED_MATRIX)
EXPECTED_METHOD_SET = frozenset(EXPECTED_METHODS)


class RunnerError(RuntimeError):
    def __init__(self, reason_code, detail):
        self.reason_code = reason_code
        self.detail = detail
        super().__init__(f"{reason_code}: {detail}")


def fail(reason_code, detail):
    raise RunnerError(reason_code, detail)


def canonical_json_bytes(value):
    return json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=True
    ).encode("utf-8")


def sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def atomic_write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent
    )
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(value, stream, sort_keys=True, indent=2)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    except BaseException:
        try:
            os.unlink(temporary)
        except FileNotFoundError:
            pass
        raise


def normalize_benchmark_jar(source_jar, executable_jar):
    source = Path(source_jar).resolve()
    destination = Path(executable_jar).resolve()
    if not source.is_file():
        fail("ARTIFACT_IDENTITY_MISMATCH", "benchmark source JAR does not exist")
    destination.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{destination.name}.", suffix=".tmp", dir=destination.parent
    )
    os.close(descriptor)
    removed = []
    try:
        with zipfile.ZipFile(source, "r") as input_archive:
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                with zipfile.ZipFile(temporary_name, "w") as output_archive:
                    output_archive.comment = input_archive.comment
                    for entry in input_archive.infolist():
                        normalized_name = entry.filename.upper()
                        is_signature = normalized_name.startswith("META-INF/") and normalized_name.endswith(
                            (".SF", ".RSA", ".DSA")
                        )
                        if is_signature:
                            removed.append(entry.filename)
                            continue
                        output_archive.writestr(entry, input_archive.read(entry))
        with Path(temporary_name).open("rb") as stream:
            os.fsync(stream.fileno())
        os.replace(temporary_name, destination)
    except BaseException:
        try:
            os.unlink(temporary_name)
        except FileNotFoundError:
            pass
        raise
    return {
        "policy": "strip-meta-inf-signatures-v1",
        "source_path": str(source),
        "source_sha256": sha256_file(source),
        "executable_path": str(destination),
        "executable_sha256": sha256_file(destination),
        "removed_entries": sorted(removed),
    }


def command_output(command, *, cwd=REPOSITORY_ROOT, environment=None):
    completed = subprocess.run(
        [str(value) for value in command],
        cwd=cwd,
        env=environment,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        fail(
            "COMMAND_FAILED",
            f"{' '.join(str(value) for value in command)} exited {completed.returncode}: "
            f"{completed.stderr.strip()}",
        )
    return completed.stdout


def git_output(repository_root, *arguments):
    completed = subprocess.run(
        ["git", *arguments],
        cwd=repository_root,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        fail("SOURCE_IDENTITY_MISMATCH", completed.stderr.strip())
    return completed.stdout.strip()


def require_clean_repository(repository_root):
    status = git_output(repository_root, "status", "--porcelain=v1", "--untracked-files=all")
    if status:
        fail("BUILD_INPUT_DIRTY", "benchmark build input contains tracked or untracked changes")
    return "clean"


def require_expected_head(repository_root, expected_head):
    actual = git_output(repository_root, "rev-parse", "HEAD")
    if actual != expected_head:
        fail("SOURCE_IDENTITY_MISMATCH", f"expected HEAD {expected_head}, got {actual}")
    tree = git_output(repository_root, "rev-parse", "HEAD^{tree}")
    if len(actual) != 40 or len(tree) != 40:
        fail("SOURCE_IDENTITY_MISMATCH", "HEAD and tree must be full Git object IDs")
    return {"benchmark_input_sha": actual, "benchmark_input_tree": tree}


def exact_include_regex():
    methods = "|".join(re.escape(method) for method in EXPECTED_METHODS)
    return "^" + re.escape(BENCHMARK_CLASS) + r"\.(?:" + methods + r")$"


def fixed_jmh_argv(benchmark_jar, result_path, classpath=None):
    if classpath is None:
        classpath = [str(benchmark_jar)]
    else:
        classpath = [str(value) for value in classpath]
    return [
        "java",
        "-cp",
        os.pathsep.join(classpath),
        "org.openjdk.jmh.Main",
        "-f",
        "2",
        "-wi",
        "3",
        "-i",
        "5",
        "-t",
        "1",
        "-prof",
        "gc",
        "-rf",
        "json",
        "-rff",
        str(result_path),
        exact_include_regex(),
    ]


def pin_classpath(benchmark_jar, runtime_entries, jackson2_project_jar):
    benchmark = Path(benchmark_jar)
    project = Path(jackson2_project_jar)
    candidates = [Path(value) for value in runtime_entries]
    if not benchmark.is_file() or not benchmark.is_absolute():
        fail("CLASSPATH_INVALID", "benchmark JAR must be an existing absolute file")
    if not project.is_file() or not project.is_absolute():
        fail("CLASSPATH_INVALID", "Jackson 2 project JAR must be an existing absolute file")
    if "jackson2" not in project.name.lower():
        fail("CLASSPATH_INVALID", "Jackson 2 project JAR name does not identify jackson2")
    resolved_candidates = [value.resolve() for value in candidates]
    if any(not value.is_file() for value in resolved_candidates):
        fail("CLASSPATH_INVALID", "runtime classpath entries must be existing files")
    if len(resolved_candidates) != len(set(resolved_candidates)):
        fail("CLASSPATH_INVALID", "runtime classpath contains duplicate entries")
    project_resolved = project.resolve()
    if resolved_candidates.count(project_resolved) != 1:
        fail("CLASSPATH_INVALID", "runtime classpath must contain one Jackson 2 project JAR")
    jackson2_named = [value for value in resolved_candidates if "jackson2" in value.name.lower()]
    if jackson2_named != [project_resolved]:
        fail(
            "CLASSPATH_INVALID",
            f"expected only the pinned Jackson 2 project JAR, got {jackson2_named}",
        )
    ordered = [benchmark.resolve(), project_resolved]
    ordered.extend(
        value
        for value in resolved_candidates
        if value not in {benchmark.resolve(), project_resolved}
    )
    if len(ordered) != len(set(ordered)):
        fail("CLASSPATH_INVALID", "combined classpath contains duplicate entries")
    return [
        {
            "path": str(path),
            "sha256": sha256_file(path),
            "kind": (
                "benchmark"
                if index == 0
                else "jackson2-project"
                if index == 1
                else "runtime"
            ),
        }
        for index, path in enumerate(ordered)
    ]


def parse_preflight_stdout(stdout):
    lines = [line for line in stdout.splitlines() if line.strip()]
    if len(lines) != 1:
        fail("PREFLIGHT_MISMATCH", f"preflight must emit one JSON object line, got {len(lines)}")
    try:
        value = json.loads(lines[0])
    except json.JSONDecodeError as error:
        fail("PREFLIGHT_MISMATCH", f"preflight output is not JSON: {error}")
    if not isinstance(value, dict):
        fail("PREFLIGHT_MISMATCH", "preflight output must be a JSON object")
    if value.get("schema_version") != 1 or value.get("status") != "passed":
        fail("PREFLIGHT_MISMATCH", "preflight schema/status mismatch")
    cells = value.get("cells")
    if not isinstance(cells, list):
        fail("PREFLIGHT_MISMATCH", "preflight cells must be an array")
    methods = [cell.get("method") for cell in cells if isinstance(cell, dict)]
    if len(methods) != 16 or len(set(methods)) != 16 or set(methods) != EXPECTED_METHOD_SET:
        fail("PREFLIGHT_MISMATCH", "preflight method matrix mismatch")
    if not isinstance(value.get("fixture_sha256"), str) or len(value["fixture_sha256"]) != 64:
        fail("PREFLIGHT_MISMATCH", "preflight fixture_sha256 must be 64 characters")
    fixture = value.get("fixture")
    if not isinstance(fixture, dict):
        fail("PREFLIGHT_MISMATCH", "preflight fixture must be an object")
    for field in POOLED_FIXTURE_FIELDS:
        if field not in fixture:
            fail("PREFLIGHT_MISMATCH", f"preflight fixture missing {field}")
    for field in ("heap_allocator_class", "direct_allocator_class"):
        if fixture[field] != "io.netty.buffer.PooledByteBufAllocator":
            fail("PREFLIGHT_MISMATCH", f"preflight fixture {field} is not pooled")
    for field in ("heap_buffer_class", "direct_buffer_class"):
        if not isinstance(fixture[field], str) or ".Pooled" not in fixture[field]:
            fail("PREFLIGHT_MISMATCH", f"preflight fixture {field} is not pooled")
    for field in ("num_heap_arenas", "num_direct_arenas"):
        if not isinstance(fixture[field], int) or fixture[field] <= 0:
            fail("PREFLIGHT_MISMATCH", f"preflight fixture {field} must be positive")
    return value


def parse_benchmark_list(stdout):
    prefix = BENCHMARK_CLASS + "."
    names = [line.strip() for line in stdout.splitlines() if line.strip().startswith(prefix)]
    promotion = [name for name in names if name[len(prefix) :] in EXPECTED_METHOD_SET]
    diagnostics = [name for name in names if name[len(prefix) :].endswith("Diagnostic")]
    unexpected = sorted(set(names) - set(promotion) - set(diagnostics))
    expected_full = [prefix + method for method in EXPECTED_METHODS]
    if len(promotion) != 16 or set(promotion) != set(expected_full) or unexpected:
        fail(
            "MATRIX_EXACT",
            f"benchmark list mismatch: promotion={len(promotion)}, unexpected={unexpected}",
        )
    return {
        "promotion": [name for name in expected_full if name in promotion],
        "diagnostic": diagnostics,
    }


def build_metadata(run_id, source, classpath, preflight, java_runtime):
    fixture = dict(FIXTURE)
    fixture.update(preflight.get("fixture", {}))
    if "payload_sha256" not in fixture:
        cells = preflight.get("cells", [])
        fixture["payload_sha256"] = cells[0].get("payload_sha256") if cells else None
    dispatch = preflight.get("dispatch", {})
    return {
        "schema_version": 1,
        "run_id": run_id,
        "benchmark_input_sha": source["benchmark_input_sha"],
        "benchmark_input_tree": source["benchmark_input_tree"],
        "clean_status": "clean",
        "benchmark_jar": classpath[0],
        "classpath": classpath,
        "protocol": dict(PROTOCOL),
        "fixture": fixture,
        "matrix": list(EXPECTED_MATRIX),
        "preflight": preflight,
        "preflight_sha256": hashlib.sha256(canonical_json_bytes(preflight)).hexdigest(),
        "dispatch": dispatch,
        "java_runtime": java_runtime,
    }


def java_launcher_identity():
    executable = shutil.which("java")
    if executable is None:
        fail("JVM_IDENTITY_MISMATCH", "java launcher is not available")
    resolved = str(Path(executable).resolve())
    completed = subprocess.run(
        [resolved, "-XshowSettings:properties", "-version"],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        fail("JVM_IDENTITY_MISMATCH", "java launcher property probe failed")
    properties = {}
    for line in (completed.stdout + "\n" + completed.stderr).splitlines():
        match = re.match(r"\s*([^=]+?)\s*=\s*(.*)\s*$", line)
        if match:
            properties[match.group(1)] = match.group(2)
    required = ("java.home", "java.vendor", "java.version", "java.vm.name", "java.vm.version")
    missing = [field for field in required if not properties.get(field)]
    if missing:
        fail("JVM_IDENTITY_MISMATCH", f"java launcher properties missing: {missing}")
    java_home = Path(properties["java.home"]).resolve()
    runtime_executable = (java_home / "bin" / "java").resolve()
    if not runtime_executable.is_file():
        fail("JVM_IDENTITY_MISMATCH", "java.home does not contain bin/java")
    return {
        "executable": str(runtime_executable),
        "java_home": str(java_home),
        "vendor": properties["java.vendor"],
        "version": properties["java.version"],
        "vm_name": properties["java.vm.name"],
        "vm_version": properties["java.vm.version"],
    }


def bind_java_runtime(launcher, records, validator):
    observed = validator.jmh_runtime_identity(records)
    for field in ("executable", "version", "vm_name", "vm_version"):
        expected = launcher[field]
        actual = observed[field]
        if field == "executable":
            actual = str(Path(actual).resolve())
        if actual != expected:
            fail("JVM_IDENTITY_MISMATCH", f"JMH {field} differs from resolved launcher")
    return {**launcher, "jvm_args": observed["jvm_args"]}


def _init_script_text():
    return r"""
gradle.beforeProject { candidate ->
    if (candidate.path == ':bluetape4k-jackson2') {
        candidate.tasks.register('issue756PrintRuntimeArtifacts') {
            doLast {
                candidate.configurations.runtimeClasspath.resolvedConfiguration.resolvedArtifacts
                    .collect { it.file.canonicalFile }
                    .sort { a, b -> a.absolutePath <=> b.absolutePath }
                    .each { println('ISSUE756_RUNTIME_ARTIFACT=' + it.absolutePath) }
            }
        }
    }
}
""".strip()


def _single_matching_file(pattern, label):
    matches = sorted(path.resolve() for path in REPOSITORY_ROOT.glob(pattern) if path.is_file())
    if len(matches) != 1:
        fail("CLASSPATH_INVALID", f"expected one {label}, got {len(matches)}: {matches}")
    return matches[0]


def build_pinned_classpath(environment=None):
    descriptor, init_name = tempfile.mkstemp(prefix="issue756-", suffix=".init.gradle")
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            stream.write(_init_script_text())
            stream.write("\n")
        command = [
            "./gradlew",
            ":bluetape4k-jackson2:jar",
            ":bluetape4k-lettuce:benchmarkBenchmarkJar",
            ":bluetape4k-jackson2:issue756PrintRuntimeArtifacts",
            "--init-script",
            init_name,
            "--no-daemon",
            "--no-configuration-cache",
        ]
        output = command_output(command, environment=environment)
    finally:
        try:
            os.unlink(init_name)
        except FileNotFoundError:
            pass
    runtime = [
        Path(line.split("=", 1)[1]).resolve()
        for line in output.splitlines()
        if line.startswith("ISSUE756_RUNTIME_ARTIFACT=")
    ]
    benchmark_source = _single_matching_file(
        "infra/lettuce/build/benchmarks/benchmark/jars/*-JMH.jar", "benchmark JAR"
    )
    normalization = normalize_benchmark_jar(
        benchmark_source,
        REPOSITORY_ROOT / "infra/lettuce/build/issue756/lettuce-codec-benchmark-executable.jar",
    )
    benchmark = Path(normalization["executable_path"])
    jackson_candidates = [
        path.resolve()
        for path in REPOSITORY_ROOT.glob("io/jackson2/build/libs/*.jar")
        if path.is_file()
        and not any(marker in path.name for marker in ("-sources", "-javadoc", "-all"))
    ]
    if len(jackson_candidates) != 1:
        fail(
            "CLASSPATH_INVALID",
            f"expected one Jackson 2 project JAR, got {len(jackson_candidates)}",
        )
    runtime = [jackson_candidates[0], *runtime]
    classpath = pin_classpath(benchmark, runtime, jackson_candidates[0])
    classpath[0]["normalization"] = normalization
    return classpath, command


def _classpath_paths(classpath):
    return [entry["path"] for entry in classpath]


def run_preflight(classpath):
    stdout = command_output(
        ["java", "-cp", os.pathsep.join(_classpath_paths(classpath)), PREFLIGHT_CLASS]
    )
    return parse_preflight_stdout(stdout)


def list_benchmarks(classpath):
    stdout = command_output(
        ["java", "-cp", os.pathsep.join(_classpath_paths(classpath)), "org.openjdk.jmh.Main", "-l"]
    )
    return parse_benchmark_list(stdout)


def _cpu_model():
    if platform.system() == "Darwin" and shutil.which("sysctl"):
        completed = subprocess.run(
            ["sysctl", "-n", "machdep.cpu.brand_string"],
            check=False,
            capture_output=True,
            text=True,
        )
        if completed.returncode == 0 and completed.stdout.strip():
            return completed.stdout.strip()
    return platform.processor() or "unknown"


def environment_document(run_id, source, classpath, build_command, jmh_argv, java_runtime):
    return {
        "schema_version": 1,
        "run_id": run_id,
        "recorded_at": datetime.now(timezone.utc).isoformat(),
        "benchmark_input_sha": source["benchmark_input_sha"],
        "benchmark_input_tree": source["benchmark_input_tree"],
        "os": platform.system(),
        "kernel": platform.release(),
        "architecture": platform.machine(),
        "cpu_model": _cpu_model(),
        "logical_cores": os.cpu_count(),
        "java_home": os.environ.get("JAVA_HOME"),
        "jvm_options": os.environ.get("JAVA_TOOL_OPTIONS", ""),
        "gradle_command": [str(value) for value in build_command],
        "jmh_command": [str(value) for value in jmh_argv],
        "classpath": classpath,
        "protocol": dict(PROTOCOL),
        "java_runtime": java_runtime,
    }


def _load_validator():
    path = HERE / "validate-issue756-jmh.py"
    spec = importlib.util.spec_from_file_location("issue756_validator_for_runner", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _run_canonical(run_id, output_root, source, classpath, build_command, preflight, launcher):
    run_root = output_root / run_id
    run_root.mkdir(parents=True, exist_ok=False)
    result_path = run_root / "jmh.json"
    jmh_argv = fixed_jmh_argv(classpath[0]["path"], result_path, _classpath_paths(classpath))
    started_at = datetime.now(timezone.utc).isoformat()
    with (run_root / "run.log").open("w", encoding="utf-8") as log:
        completed = subprocess.run(
            jmh_argv,
            cwd=REPOSITORY_ROOT,
            check=False,
            stdout=log,
            stderr=subprocess.STDOUT,
            text=True,
        )
    ended_at = datetime.now(timezone.utc).isoformat()
    atomic_write_json(
        run_root / "argv.json",
        {
            "schema_version": 1,
            "argv": jmh_argv,
            "started_at": started_at,
            "ended_at": ended_at,
            "exit_code": completed.returncode,
        },
    )
    if completed.returncode != 0:
        fail("COMMAND_FAILED", f"canonical JMH {run_id} exited {completed.returncode}")
    validator = _load_validator()
    with result_path.open(encoding="utf-8") as stream:
        records = json.load(stream)
    java_runtime = bind_java_runtime(launcher, records, validator)
    metadata = build_metadata(run_id, source, classpath, preflight, java_runtime)
    atomic_write_json(run_root / "metadata.json", metadata)
    atomic_write_json(
        run_root / "environment.json",
        environment_document(run_id, source, classpath, build_command, jmh_argv, java_runtime),
    )
    validation = validator.validate_run_bundle(run_root)
    validator._write_summary(run_root / "summary.csv", validation)
    public = {
        key: value
        for key, value in validation.items()
        if key not in ("rows", "metadata")
    }
    atomic_write_json(run_root / "validation.json", public)
    return validation


def run_two_canonical(output_root, source, classpath, build_command, preflight, launcher):
    output_root = Path(output_root)
    output_root.mkdir(parents=True, exist_ok=True)
    if any((output_root / run_id).exists() for run_id in ("canonical-a", "canonical-b")):
        fail("OUTPUT_EXISTS", "canonical output directories already exist")
    first = _run_canonical(
        "canonical-a", output_root, source, classpath, build_command, preflight, launcher
    )
    second = _run_canonical(
        "canonical-b", output_root, source, classpath, build_command, preflight, launcher
    )
    validator = _load_validator()
    validator.validate_canonical_identity(first["metadata"], second["metadata"])
    comparison = validator.compare_runs(first, second, first["dispatch"])
    validator._write_comparison(output_root / "comparison.csv", comparison)
    validation = {
        "schema_version": 1,
        "status": "passed",
        "benchmark_input_sha": source["benchmark_input_sha"],
        "benchmark_input_tree": source["benchmark_input_tree"],
        "verdicts": {
            f"{row['backend']}/{row['target']}": row["verdict"] for row in comparison
        },
    }
    atomic_write_json(output_root / "validation.json", validation)
    delivery_manifest = {
        "schema_version": 1,
        "status": "passed",
        "benchmark_input_sha": source["benchmark_input_sha"],
        "benchmark_input_tree": source["benchmark_input_tree"],
        "benchmark_jar": classpath[0],
        "classpath": classpath,
        "protocol": dict(PROTOCOL),
        "preflight_sha256": hashlib.sha256(canonical_json_bytes(preflight)).hexdigest(),
        "java_runtime": first["metadata"]["java_runtime"],
        "canonical_runs": ["canonical-a", "canonical-b"],
        "comparison_sha256": sha256_file(output_root / "comparison.csv"),
        "validation_sha256": sha256_file(output_root / "validation.json"),
    }
    atomic_write_json(output_root / "delivery-manifest.json", delivery_manifest)
    return delivery_manifest


def _parser():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expected-head", required=True)
    parser.add_argument("--repository-root", type=Path, default=REPOSITORY_ROOT)
    parser.add_argument(
        "--output",
        "--output-root",
        dest="output_root",
        type=Path,
        default=REPOSITORY_ROOT / "docs/benchmarks/raw/issue-756",
    )
    parser.add_argument(
        "--runs",
        nargs="+",
        default=("canonical-a", "canonical-b"),
        help="Only the fixed canonical-a canonical-b pair is accepted.",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--preflight-only", action="store_true")
    mode.add_argument("--list-benchmarks", action="store_true")
    return parser


def main(argv=None):
    arguments = _parser().parse_args(argv)
    try:
        if tuple(arguments.runs) != ("canonical-a", "canonical-b"):
            fail("PROTOCOL_MISMATCH", "runs must be exactly canonical-a canonical-b")
        require_clean_repository(arguments.repository_root)
        source = require_expected_head(arguments.repository_root, arguments.expected_head)
        launcher = java_launcher_identity()
        classpath, build_command = build_pinned_classpath(os.environ.copy())
        preflight = run_preflight(classpath)
        if arguments.preflight_only:
            result = {
                "status": "passed",
                "mode": "preflight-only",
                "source": source,
                "classpath": classpath,
                "preflight": preflight,
                "java_launcher": launcher,
            }
        elif arguments.list_benchmarks:
            result = {
                "status": "passed",
                "mode": "list-benchmarks",
                "source": source,
                "benchmarks": list_benchmarks(classpath),
                "java_launcher": launcher,
            }
        else:
            result = run_two_canonical(
                arguments.output_root,
                source,
                classpath,
                build_command,
                preflight,
                launcher,
            )
        print(canonical_json_bytes(result).decode("utf-8"))
        return 0
    except RunnerError as error:
        print(
            canonical_json_bytes(
                {"status": "failed", "reason_code": error.reason_code, "detail": error.detail}
            ).decode("utf-8"),
            file=sys.stderr,
        )
        return 2


if __name__ == "__main__":
    sys.exit(main())
