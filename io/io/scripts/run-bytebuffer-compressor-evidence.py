#!/usr/bin/env python3
"""Capture and validate immutable JMH allocation evidence for issue #755."""

from __future__ import annotations

import argparse
import csv
import ctypes
import datetime as dt
import errno
import fcntl
import hashlib
import json
import math
import os
import pathlib
import platform
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import uuid
import warnings
import zipfile

SCHEMA_VERSION = 1
MAX_RUN_LOG_BYTES = 16 * 1024 * 1024
JVM_ARGS = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"]
PROFILE_ARGS = {
    "smoke": ["-t", "1", "-f", "1", "-wi", "1", "-i", "1", "-w", "100ms", "-r", "100ms", "-prof", "gc", "-rf", "json"],
    "canonical": ["-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
}
PAYLOAD_BYTES = {"small": 1147, "medium": 65718, "large": 524349}
CODECS = ("lz4", "deflate", "snappy", "zstd")
STORAGE_PATHS = ("heap", "direct", "heapToDirect", "directToHeap")
OPERATIONS = ("compress", "decompress")
METHODS = (
    "compressByteArrayBaseline",
    "compressByteBufferBaseline",
    "compressCallerOwned",
    "decompressByteArrayBaseline",
    "decompressByteBufferBaseline",
    "decompressCallerOwned",
)
REQUIRED_RUN_FILES = {
    "jmh.json", "metadata.json", "argv.json", "environment.json", "dependencies.txt",
    "run.log", "summary.csv", "source-inspection.json", "validation.json",
}
SOURCE_PATHS = {
    "lz4": "io/io/src/main/kotlin/io/bluetape4k/io/compressor/LZ4Compressor.kt",
    "deflate": "io/io/src/main/kotlin/io/bluetape4k/io/compressor/DeflateCompressor.kt",
    "snappy": "io/io/src/main/kotlin/io/bluetape4k/io/compressor/SnappyCompressor.kt",
    "zstd": "io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZstdCompressor.kt",
}
DELIVERY_ALLOWLIST = (
    "docs/benchmarks/raw/issue-755/",
    "docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md",
    "docs/benchmarks/README.md",
    "io/io/Benchmark.md",
    "io/io/README.md",
    "io/io/README.ko.md",
    "CHANGELOG.md",
    "docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md",
)
RUN_ID_PATTERN = re.compile(r"run-[0-9]{8}T[0-9]{6}Z-[0-9a-f]{8}")


def repo_root() -> pathlib.Path:
    return pathlib.Path(__file__).resolve().parents[3]


def command(*argv: str, cwd: pathlib.Path | None = None) -> str:
    result = subprocess.run(argv, cwd=cwd, check=True, text=True, capture_output=True)
    return result.stdout.strip()


def java_identity(root: pathlib.Path) -> dict[str, str]:
    result = subprocess.run(
        ["java", "-XshowSettings:properties", "-version"],
        cwd=root,
        check=True,
        text=True,
        capture_output=True,
    )
    output = result.stderr + result.stdout
    version = re.search(r"(?m)^\s*java\.version = (.+)$", output)
    vm_name = re.search(r"(?m)^\s*java\.vm\.name = (.+)$", output)
    vm_version = re.search(r"(?m)^\s*java\.vm\.version = (.+)$", output)
    java_home = re.search(r"(?m)^\s*java\.home = (.+)$", output)
    if not version or not vm_name or not vm_version or not java_home:
        raise ValueError("Java runtime identity is incomplete")
    executable = pathlib.Path(java_home.group(1).strip()) / "bin/java"
    return {
        "jdk": version.group(1).strip(),
        "vmName": vm_name.group(1).strip(),
        "vmVersion": vm_version.group(1).strip(),
        "jvmExecutable": str(executable.resolve(strict=True)),
    }


def cpu_identity() -> str:
    if sys.platform == "darwin":
        result = subprocess.run(
            ["sysctl", "-n", "machdep.cpu.brand_string"],
            check=False,
            text=True,
            capture_output=True,
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    return platform.processor() or command("uname", "-m")


def environment_authority(root: pathlib.Path) -> dict[str, str]:
    return {**java_identity(root), "os": platform.platform(), "cpu": cpu_identity()}


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonicalize_jar(source: pathlib.Path, destination: pathlib.Path) -> pathlib.Path:
    """Repack a fat JAR deterministically without changing its entry-byte multiset."""
    source = source.resolve(strict=True)
    if source.is_symlink() or not source.is_file():
        raise ValueError("source JMH JAR must be a regular file")
    entries = []
    with zipfile.ZipFile(source) as archive:
        for ordinal, info in enumerate(archive.infolist()):
            data = archive.read(info)
            entries.append((info.filename, hashlib.sha256(data).hexdigest(), ordinal, data, info.is_dir()))
    entries.sort(key=lambda item: (item[0] != "META-INF/MANIFEST.MF", item[0], item[1], item[2]))
    destination.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary_name = tempfile.mkstemp(prefix=f".{destination.name}.", dir=destination.parent)
    os.close(fd)
    temporary = pathlib.Path(temporary_name)
    try:
        with warnings.catch_warnings():
            warnings.filterwarnings("ignore", message="Duplicate name:.*", category=UserWarning)
            with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output:
                for name, _, _, data, is_directory in entries:
                    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                    info.compress_type = zipfile.ZIP_STORED if is_directory else zipfile.ZIP_DEFLATED
                    info.create_system = 3
                    info.external_attr = (0o40755 if is_directory else 0o100644) << 16
                    output.writestr(info, data, compress_type=info.compress_type, compresslevel=9)
        os.replace(temporary, destination)
    finally:
        if temporary.exists():
            temporary.unlink()
    return destination.resolve(strict=True)


def atomic_json(path: pathlib.Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as output:
            json.dump(value, output, indent=2, sort_keys=True)
            output.write("\n")
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def finite(value: object, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label} must be a JSON number")
    number = float(value)
    if not math.isfinite(number):
        raise ValueError(f"{label} must be finite")
    return number


def metric_interval(metric: dict, label: str, unit: str, positive: bool = False) -> tuple[float, float]:
    if metric.get("scoreUnit") != unit:
        raise ValueError(f"{label} unit must be {unit}")
    score = finite(metric.get("score"), f"{label} score")
    error = finite(metric.get("scoreError"), f"{label} scoreError")
    if error < 0 or (positive and score <= 0) or (not positive and score < 0):
        raise ValueError(f"{label} score/error out of range")
    return score - error, score + error


def allocation_accepted(baseline: dict, candidate: dict) -> bool:
    baseline_low, _ = metric_interval(baseline, "gc.alloc.rate.norm", "B/op")
    _, candidate_high = metric_interval(candidate, "gc.alloc.rate.norm", "B/op")
    return finite(candidate["score"], "candidate allocation") <= finite(
        baseline["score"], "baseline allocation"
    ) * 0.95 and candidate_high < baseline_low


def throughput_regressed(baseline: dict, candidate: dict) -> bool:
    baseline_low, _ = metric_interval(baseline, "throughput", "ops/s", positive=True)
    _, candidate_high = metric_interval(candidate, "throughput", "ops/s", positive=True)
    return finite(candidate["score"], "candidate throughput") <= finite(
        baseline["score"], "baseline throughput"
    ) * 0.80 and candidate_high < baseline_low


def eligible(codec: str, storage_path: str) -> bool:
    return codec in {"lz4", "deflate"} or storage_path in {"heap", "direct"}


def scaling_demonstrated(per_payload: dict[str, tuple[dict, dict]]) -> bool:
    if set(per_payload) != set(PAYLOAD_BYTES):
        raise ValueError("small, medium, and large matched pairs are required")
    saved = {
        payload: finite(baseline["score"], f"{payload} baseline allocation")
        - finite(candidate["score"], f"{payload} candidate allocation")
        for payload, (baseline, candidate) in per_payload.items()
    }
    return (
        saved["small"] < saved["medium"] < saved["large"]
        and saved["medium"] / PAYLOAD_BYTES["medium"] >= 0.50
        and saved["large"] / PAYLOAD_BYTES["large"] >= 0.50
    )


def split_benchmark(record: dict) -> str:
    name = record.get("benchmark")
    if not isinstance(name, str) or not name.startswith(
        "io.bluetape4k.io.benchmark.CallerOwnedByteBufferCompressorBenchmark."
    ):
        raise ValueError(f"unexpected benchmark: {name}")
    method = name.rsplit(".", 1)[-1]
    if method not in METHODS:
        raise ValueError(f"unexpected benchmark method: {method}")
    return method


def validate_jmh(
    records: object,
    *,
    require_matrix: bool,
    allow_unstable_error: bool = False,
    expected_profile: str | None = None,
    expected_authority: dict | None = None,
) -> dict[tuple[str, str, str, str], dict]:
    if not isinstance(records, list) or not records:
        raise ValueError("jmh.json must contain a non-empty array")
    indexed: dict[tuple[str, str, str, str], dict] = {}
    execution_identity = None
    profile_expectations = {
        "smoke": (1, 1, 1, "100 ms", 1, "100 ms"),
        "canonical": (1, 2, 3, "1 s", 5, "1 s"),
    }
    for record in records:
        if not isinstance(record, dict):
            raise ValueError("every JMH row must be an object")
        method = split_benchmark(record)
        current_identity = (
            record.get("jmhVersion"), record.get("jvm"), record.get("jdkVersion"), record.get("vmName"),
            record.get("vmVersion"), tuple(record.get("jvmArgs", [])), record.get("threads"), record.get("forks"),
            record.get("warmupIterations"), record.get("warmupTime"), record.get("measurementIterations"),
            record.get("measurementTime"), record.get("mode"),
        )
        if execution_identity is None:
            execution_identity = current_identity
        elif current_identity != execution_identity:
            raise ValueError("JMH execution identity drift within one run")
        if record.get("mode") != "thrpt":
            raise ValueError("JMH mode must be throughput")
        actual_jvm_args = record.get("jvmArgs")
        if not isinstance(actual_jvm_args, list) or actual_jvm_args[-len(JVM_ARGS):] != JVM_ARGS:
            raise ValueError("JMH JVM args do not contain the exact required suffix")
        if any(actual_jvm_args.count(argument) != 1 for argument in JVM_ARGS):
            raise ValueError("required JMH JVM args must occur exactly once")
        if expected_profile:
            actual_profile = (
                record.get("threads"), record.get("forks"), record.get("warmupIterations"),
                record.get("warmupTime"), record.get("measurementIterations"), record.get("measurementTime"),
            )
            if actual_profile != profile_expectations[expected_profile]:
                raise ValueError(f"{expected_profile} JMH profile mismatch: {actual_profile}")
        if expected_authority:
            pairs = {
                "jdkVersion": "jdk", "vmName": "vmName", "vmVersion": "vmVersion", "jvm": "jvmExecutable",
            }
            for record_key, authority_key in pairs.items():
                if record.get(record_key) != expected_authority.get(authority_key):
                    raise ValueError(f"JMH runtime identity mismatch: {record_key}")
        params = record.get("params")
        if not isinstance(params, dict) or set(params) != {"compressorName", "payloadSize", "storagePath"}:
            raise ValueError("JMH params must be compressorName, payloadSize, and storagePath")
        codec, payload, storage = (params["compressorName"], params["payloadSize"], params["storagePath"])
        if codec not in CODECS or payload not in PAYLOAD_BYTES or storage not in STORAGE_PATHS:
            raise ValueError(f"unexpected JMH params: {params}")
        primary = record.get("primaryMetric")
        secondary = record.get("secondaryMetrics")
        if not isinstance(primary, dict):
            raise ValueError("primaryMetric is required")
        if allow_unstable_error and primary.get("scoreError") == "NaN":
            primary_for_validation = {**primary, "scoreError": 0.0}
        else:
            primary_for_validation = primary
        metric_interval(primary_for_validation, "throughput", "ops/s", positive=True)
        allocation = secondary.get("gc.alloc.rate.norm") if isinstance(secondary, dict) else None
        if not isinstance(allocation, dict):
            raise ValueError("gc.alloc.rate.norm is required")
        if allow_unstable_error and allocation.get("scoreError") == "NaN":
            allocation_for_validation = {**allocation, "scoreError": 0.0}
        else:
            allocation_for_validation = allocation
        metric_interval(allocation_for_validation, "gc.alloc.rate.norm", "B/op")
        key = (codec, payload, storage, method)
        if key in indexed:
            raise ValueError(f"duplicate JMH cell: {key}")
        indexed[key] = record
    if require_matrix:
        expected = {
            (codec, payload, storage, method)
            for codec in CODECS for payload in PAYLOAD_BYTES for storage in STORAGE_PATHS for method in METHODS
        }
        missing, extra = expected - set(indexed), set(indexed) - expected
        if missing or extra:
            raise ValueError(f"canonical JMH matrix mismatch: missing={len(missing)}, extra={len(extra)}")
    return indexed


def source_inspection(root: pathlib.Path) -> list[dict]:
    rows = []
    for codec, relative in SOURCE_PATHS.items():
        path = root / relative
        if path.is_symlink() or not path.is_file():
            raise ValueError(f"production source must be a regular file: {relative}")
        text = path.read_text(encoding="utf-8")
        for operation in OPERATIONS:
            signature = f"override fun {operation}(source: ByteBuffer, target: ByteBuffer): Int"
            if signature not in text:
                raise ValueError(f"missing production symbol: {relative}::{signature}")
            for storage in STORAGE_PATHS:
                rows.append({
                    "codec": codec,
                    "operation": operation,
                    "storagePath": storage,
                    "path": relative,
                    "symbol": f"{operation}(ByteBuffer, ByteBuffer)",
                    "sha256": sha256(path),
                    "payloadIntermediateFree": eligible(codec, storage),
                })
    return rows


def validate_source_inspection(rows: object, root: pathlib.Path) -> None:
    expected = {(c, o, s) for c in CODECS for o in OPERATIONS for s in STORAGE_PATHS}
    if not isinstance(rows, list):
        raise ValueError("source inspection must be an array")
    actual = set()
    for row in rows:
        key = (row.get("codec"), row.get("operation"), row.get("storagePath"))
        actual.add(key)
        relative = row.get("path")
        if not isinstance(relative, str) or relative != SOURCE_PATHS.get(key[0]):
            raise ValueError("source inspection path mismatch")
        path = root / relative
        if path.is_symlink() or sha256(path) != row.get("sha256"):
            raise ValueError("production source hash mismatch")
        if row.get("payloadIntermediateFree") is not eligible(key[0], key[2]):
            raise ValueError("source inspection eligibility mismatch")
    if actual != expected:
        raise ValueError("source inspection matrix mismatch")


def git_identity(root: pathlib.Path) -> tuple[str, str]:
    return command("git", "rev-parse", "HEAD", cwd=root), command("git", "rev-parse", "HEAD^{tree}", cwd=root)


def clean_paths(root: pathlib.Path) -> list[str]:
    output = command("git", "status", "--porcelain=v1", "--untracked-files=all", cwd=root)
    return [line[3:] for line in output.splitlines() if line]


def ensure_real_directory(path: pathlib.Path, label: str) -> pathlib.Path:
    if path.is_symlink() or not path.exists() or not stat.S_ISDIR(os.lstat(path).st_mode):
        raise ValueError(f"{label} must be a real directory")
    return path.resolve(strict=True)


def ensure_authority_root(root: pathlib.Path) -> pathlib.Path:
    expected = repo_root() / "docs/benchmarks/raw/issue-755"
    if root.absolute() != expected.absolute():
        raise ValueError("canonical output root must be docs/benchmarks/raw/issue-755")
    parent = ensure_real_directory(expected.parent, "raw benchmark parent")
    parent_fd = os.open(parent, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        try:
            os.mkdir(expected.name, 0o755, dir_fd=parent_fd)
        except FileExistsError:
            pass
        child_stat = os.stat(expected.name, dir_fd=parent_fd, follow_symlinks=False)
        if not stat.S_ISDIR(child_stat.st_mode):
            raise ValueError("issue-755 authority root must be a real directory")
        child_fd = os.open(expected.name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent_fd)
        try:
            os.fsync(child_fd)
            os.fsync(parent_fd)
        finally:
            os.close(child_fd)
    finally:
        os.close(parent_fd)
    return ensure_real_directory(expected, "issue-755 authority root")


def dependencies(root: pathlib.Path) -> str:
    return command(
        "./gradlew", ":bluetape4k-io:dependencies", "--configuration", "testRuntimeClasspath",
        "--no-configuration-cache", cwd=root,
    ) + "\n"


def jmh_version(jar: pathlib.Path) -> str:
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.endswith("META-INF/maven/org.openjdk.jmh/jmh-core/pom.properties"):
                match = re.search(r"(?m)^version=(.+)$", archive.read(name).decode())
                if match:
                    return match.group(1)
    raise ValueError("JMH version is missing from benchmark JAR")


def prepare(args: argparse.Namespace) -> None:
    root = repo_root()
    head, tree = git_identity(root)
    if head != args.expected_head:
        raise ValueError("expected HEAD mismatch")
    dirty = clean_paths(root)
    if dirty:
        raise ValueError(f"prepare requires a clean tree: {dirty}")
    raw_jar = pathlib.Path(args.jar).resolve(strict=True)
    receipt_path = pathlib.Path(args.receipt)
    jar = canonicalize_jar(raw_jar, receipt_path.parent / "canonical-jmh.jar")
    output = ensure_authority_root(pathlib.Path(args.output_root))
    if any(output.iterdir()):
        raise ValueError("authority root must be empty at prepare")
    dep_text = dependencies(root)
    receipt = {
        "schemaVersion": SCHEMA_VERSION,
        "commit": head,
        "tree": tree,
        "jar": str(jar),
        "jarSha256": sha256(jar),
        "rawJar": str(raw_jar),
        "rawJarSha256": sha256(raw_jar),
        "jmhVersion": jmh_version(jar),
        "dependencies": dep_text,
        "dependenciesSha256": hashlib.sha256(dep_text.encode()).hexdigest(),
        "sourceInspection": source_inspection(root),
        "environmentAuthority": environment_authority(root),
        "outputRoot": str(output),
        "runs": [],
    }
    atomic_json(receipt_path, receipt)


def load_receipt(path: pathlib.Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if value.get("schemaVersion") != SCHEMA_VERSION:
        raise ValueError("unsupported receipt schema")
    return value


def validate_receipt(receipt: dict, root: pathlib.Path, *, canonical: bool) -> pathlib.Path:
    head, tree = git_identity(root)
    if (head, tree) != (receipt.get("commit"), receipt.get("tree")):
        raise ValueError("commit/tree mismatch")
    jar = pathlib.Path(receipt["jar"])
    if jar.is_symlink() or not jar.is_file() or sha256(jar) != receipt.get("jarSha256"):
        raise ValueError("JAR identity mismatch")
    validate_source_inspection(receipt.get("sourceInspection"), root)
    if receipt.get("environmentAuthority") != environment_authority(root):
        raise ValueError("runtime environment changed after prepare")
    if canonical:
        allowed = {
            f"docs/benchmarks/raw/issue-755/{run_id}/{name}"
            for run_id in receipt.get("runs", []) for name in REQUIRED_RUN_FILES
        }
        dirty = clean_paths(root)
        unexpected = [path for path in dirty if path not in allowed]
        if unexpected:
            raise ValueError(f"unexpected dirty paths: {unexpected}")
    return jar


def run_id() -> str:
    timestamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    return f"run-{timestamp}-{uuid.uuid4().hex[:8]}"


def write_summary(path: pathlib.Path, indexed: dict) -> None:
    with path.open("x", encoding="utf-8", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(["codec", "payload", "storage", "method", "throughput_ops_s", "throughput_error", "allocation_b_op", "allocation_error"])
        for key, record in sorted(indexed.items()):
            primary = record["primaryMetric"]
            allocation = record["secondaryMetrics"]["gc.alloc.rate.norm"]
            writer.writerow([*key, primary["score"], primary["scoreError"], allocation["score"], allocation["scoreError"]])


def create_staging(final_root: pathlib.Path, identifier: str) -> pathlib.Path:
    if not RUN_ID_PATTERN.fullmatch(identifier):
        raise ValueError("invalid run id")
    ensure_real_directory(final_root, "final root")
    staging_root = final_root.parent / ".issue-755-staging"
    staging_root.mkdir(mode=0o700, exist_ok=True)
    ensure_real_directory(staging_root, "staging root")
    pending = staging_root / f"{identifier}.pending"
    pending.mkdir(mode=0o700, exist_ok=False)
    return pending


def rename_noreplace(source: pathlib.Path, destination: pathlib.Path) -> None:
    libc = ctypes.CDLL(None, use_errno=True)
    at_fdcwd = -2
    if sys.platform == "darwin":
        result = libc.renameatx_np(at_fdcwd, os.fsencode(source), at_fdcwd, os.fsencode(destination), 0x00000004)
    elif sys.platform.startswith("linux"):
        result = libc.renameat2(at_fdcwd, os.fsencode(source), at_fdcwd, os.fsencode(destination), 0x00000001)
    else:
        raise RuntimeError("atomic no-replace rename is unsupported")
    if result != 0:
        error = ctypes.get_errno()
        raise OSError(error, os.strerror(error), destination)


def publish(staging: pathlib.Path, final_root: pathlib.Path, identifier: str) -> pathlib.Path:
    lock_root = repo_root() / "io/io/build/issue-755-evidence/locks"
    lock_root.mkdir(mode=0o700, parents=True, exist_ok=True)
    ensure_real_directory(lock_root, "lock root")
    lock_path = lock_root / f".{identifier}.lock"
    fd = os.open(lock_path, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
    final = final_root / identifier
    try:
        fcntl.flock(fd, fcntl.LOCK_EX)
        if final.exists() or final.is_symlink():
            raise FileExistsError(final)
        if staging.name != f"{identifier}.pending":
            raise ValueError("staging run ID mismatch")
        validate_run_directory(staging, require_matrix=True, expected_run_id=identifier)
        rename_noreplace(staging, final)
        parent_fd = os.open(final_root, os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(parent_fd)
        finally:
            os.close(parent_fd)
    finally:
        fcntl.flock(fd, fcntl.LOCK_UN)
        os.close(fd)
    try:
        staging.parent.rmdir()
    except OSError as failure:
        if failure.errno not in {errno.ENOTEMPTY, errno.ENOENT}:
            warnings.warn(f"published run; staging cleanup skipped: {failure}")
    return final


def execute_jmh(args: argparse.Namespace, profile: str, receipt: dict, receipt_path: pathlib.Path | None) -> pathlib.Path:
    root = repo_root()
    canonical = profile == "canonical"
    jar = validate_receipt(receipt, root, canonical=canonical)
    identifier = run_id()
    requested_output_root = pathlib.Path(args.output_root)
    if requested_output_root.is_symlink():
        raise ValueError("output root must not be a symlink")
    output_root = requested_output_root.resolve()
    if canonical:
        output_root = ensure_authority_root(output_root)
        if str(output_root) != receipt.get("outputRoot"):
            raise ValueError("canonical output root mismatch")
        staging = create_staging(output_root, identifier)
    else:
        output_root.mkdir(parents=True, exist_ok=True)
        staging = output_root / identifier
        staging.mkdir(exist_ok=False)
    result_path = staging / "jmh.json"
    include = args.include
    parameter_args = []
    for parameter in args.param or []:
        if not re.fullmatch(r"(?:compressorName|payloadSize|storagePath)=[A-Za-z0-9,]+", parameter):
            raise ValueError(f"invalid JMH parameter: {parameter}")
        parameter_args.extend(["-p", parameter])
    jmh_args = [include, *parameter_args, *PROFILE_ARGS[profile], "-rff", str(result_path), "-jvmArgsAppend", " ".join(JVM_ARGS)]
    argv = ["java", "-jar", str(jar), *jmh_args]
    completed = subprocess.run(argv, cwd=root, shell=False, capture_output=True)
    log = completed.stdout + completed.stderr
    if len(log) > MAX_RUN_LOG_BYTES:
        raise ValueError("JMH log exceeds 16 MiB")
    (staging / "run.log").write_bytes(log)
    atomic_json(staging / "argv.json", {"argv": argv, "exitCode": completed.returncode, "logBytes": len(log)})
    if completed.returncode != 0:
        raise RuntimeError(f"JMH failed with exit code {completed.returncode}")
    records = json.loads(result_path.read_text(encoding="utf-8"))
    indexed = validate_jmh(
        records,
        require_matrix=canonical,
        allow_unstable_error=not canonical,
        expected_profile=profile,
        expected_authority=receipt["environmentAuthority"],
    )
    runtime = receipt["environmentAuthority"]
    first_record = records[0]
    environment = {
        "javaVersion": runtime["jdk"],
        "vmName": runtime["vmName"],
        "vmVersion": runtime["vmVersion"],
        "jvmExecutable": runtime["jvmExecutable"],
        "actualJvmArgs": first_record["jvmArgs"],
        "jmhVersion": receipt["jmhVersion"],
        "jvmArgs": JVM_ARGS,
        "profileArgs": PROFILE_ARGS[profile],
        "os": runtime["os"],
        "cpu": runtime["cpu"],
    }
    atomic_json(staging / "environment.json", environment)
    (staging / "dependencies.txt").write_text(receipt["dependencies"], encoding="utf-8")
    atomic_json(staging / "source-inspection.json", receipt["sourceInspection"])
    metadata = {
        "schemaVersion": SCHEMA_VERSION,
        "runId": identifier,
        "profile": profile,
        "commit": receipt["commit"],
        "tree": receipt["tree"],
        "jarSha256": receipt["jarSha256"],
        "jmhVersion": receipt["jmhVersion"],
        "stateScope": "Thread",
        "dependenciesSha256": receipt["dependenciesSha256"],
        "jdk": environment["javaVersion"],
        "jvm": environment["vmName"],
        "vmVersion": environment["vmVersion"],
        "jvmExecutable": environment["jvmExecutable"],
        "actualJvmArgs": environment["actualJvmArgs"],
        "gc": "G1",
        "os": environment["os"],
        "cpu": environment["cpu"],
    }
    atomic_json(staging / "metadata.json", metadata)
    write_summary(staging / "summary.csv", indexed)
    atomic_json(staging / "validation.json", {"schemaVersion": SCHEMA_VERSION, "status": "PASS", "records": len(indexed)})
    if canonical:
        final = publish(staging, output_root, identifier)
        receipt["runs"].append(identifier)
        if receipt_path is None:
            raise ValueError("canonical run requires an input receipt")
        atomic_json(receipt_path, receipt)
        return final
    return staging


def validate_regular_tree(path: pathlib.Path) -> None:
    if path.is_symlink() or not path.is_dir():
        raise ValueError(f"run must be a real directory: {path}")
    names = {child.name for child in path.iterdir()}
    if names != REQUIRED_RUN_FILES:
        raise ValueError(f"run file set mismatch: missing={REQUIRED_RUN_FILES - names}, extra={names - REQUIRED_RUN_FILES}")
    for child in path.iterdir():
        if child.is_symlink() or not child.is_file():
            raise ValueError(f"run artifact must be a regular file: {child}")


def validate_run_directory(
    path: pathlib.Path,
    *,
    require_matrix: bool,
    expected_run_id: str | None = None,
) -> tuple[dict, dict]:
    validate_regular_tree(path)
    metadata = json.loads((path / "metadata.json").read_text(encoding="utf-8"))
    if metadata.get("stateScope") != "Thread":
        raise ValueError("benchmark state must be Scope.Thread")
    if expected_run_id is None:
        identifier = path.name
    elif path.name == f"{expected_run_id}.pending":
        identifier = expected_run_id
    else:
        raise ValueError("staging run ID mismatch")
    if metadata.get("runId") != identifier or not RUN_ID_PATTERN.fullmatch(identifier):
        raise ValueError("run ID mismatch")
    required_identity = (
        "commit", "tree", "jarSha256", "jmhVersion", "jdk", "jvm", "vmVersion", "jvmExecutable",
        "gc", "os", "cpu", "dependenciesSha256"
    )
    if any(not isinstance(metadata.get(key), str) or not metadata[key] for key in required_identity):
        raise ValueError("run identity metadata is incomplete")
    if hashlib.sha256((path / "dependencies.txt").read_bytes()).hexdigest() != metadata.get("dependenciesSha256"):
        raise ValueError("dependency identity mismatch")
    argv = json.loads((path / "argv.json").read_text(encoding="utf-8"))
    if argv.get("exitCode") != 0 or argv.get("logBytes") != (path / "run.log").stat().st_size:
        raise ValueError("run log/exit metadata mismatch")
    if (path / "run.log").stat().st_size > MAX_RUN_LOG_BYTES:
        raise ValueError("run log exceeds size limit")
    environment = json.loads((path / "environment.json").read_text(encoding="utf-8"))
    if require_matrix and environment.get("profileArgs") != PROFILE_ARGS["canonical"]:
        raise ValueError("canonical profile arguments mismatch")
    if environment.get("jvmArgs") != JVM_ARGS:
        raise ValueError("JVM arguments mismatch")
    if (
        environment.get("javaVersion") != metadata.get("jdk")
        or environment.get("vmName") != metadata.get("jvm")
        or environment.get("vmVersion") != metadata.get("vmVersion")
        or environment.get("jvmExecutable") != metadata.get("jvmExecutable")
        or environment.get("actualJvmArgs") != metadata.get("actualJvmArgs")
    ):
        raise ValueError("Java environment identity mismatch")
    recorded_argv = argv.get("argv")
    if not isinstance(recorded_argv, list) or not all(isinstance(item, str) for item in recorded_argv):
        raise ValueError("JMH argv must be a string array")
    if len(recorded_argv) < 6 or recorded_argv[:2] != ["java", "-jar"]:
        raise ValueError("JMH argv prefix mismatch")
    if recorded_argv.count("-rff") != 1 or recorded_argv.count("-jvmArgsAppend") != 1:
        raise ValueError("JMH output/JVM switches must occur exactly once")
    result_arg = pathlib.Path(recorded_argv[recorded_argv.index("-rff") + 1])
    if result_arg.name != "jmh.json" or result_arg.parent.resolve() != path.resolve():
        raise ValueError("JMH result path mismatch")
    if recorded_argv[recorded_argv.index("-jvmArgsAppend") + 1] != " ".join(JVM_ARGS):
        raise ValueError("recorded JVM arguments mismatch")
    if require_matrix:
        profile_start = next(
            (index for index in range(len(recorded_argv)) if recorded_argv[index:index + len(PROFILE_ARGS["canonical"])] == PROFILE_ARGS["canonical"]),
            None,
        )
        if profile_start is None:
            raise ValueError("canonical argv order mismatch")
    records = json.loads((path / "jmh.json").read_text(encoding="utf-8"))
    expected_authority = {
        "jdk": metadata["jdk"], "vmName": metadata["jvm"], "vmVersion": metadata["vmVersion"],
        "jvmExecutable": metadata["jvmExecutable"],
    }
    indexed = validate_jmh(
        records,
        require_matrix=require_matrix,
        allow_unstable_error=not require_matrix,
        expected_profile=metadata.get("profile"),
        expected_authority=expected_authority,
    )
    validation = json.loads((path / "validation.json").read_text(encoding="utf-8"))
    if validation.get("status") != "PASS" or validation.get("records") != len(indexed):
        raise ValueError("validation receipt mismatch")
    validate_source_inspection(json.loads((path / "source-inspection.json").read_text()), repo_root())
    return metadata, indexed


def metrics(record: dict) -> tuple[dict, dict]:
    return record["primaryMetric"], record["secondaryMetrics"]["gc.alloc.rate.norm"]


def comparison_rows(run_paths: list[pathlib.Path]) -> list[dict]:
    if len(run_paths) != 2 or run_paths[0].resolve() == run_paths[1].resolve():
        raise ValueError("exactly two unique runs are required")
    runs = [validate_run_directory(path, require_matrix=True) for path in run_paths]
    identity_keys = (
        "commit", "tree", "jarSha256", "jmhVersion", "jdk", "jvm", "vmVersion", "jvmExecutable",
        "actualJvmArgs", "gc", "os", "cpu", "dependenciesSha256",
    )
    if any(runs[0][0].get(key) != runs[1][0].get(key) for key in identity_keys):
        raise ValueError("run identity mismatch")
    rows = []
    for codec in CODECS:
        for storage in STORAGE_PATHS:
            for operation in OPERATIONS:
                baseline_method = f"{operation}ByteBufferBaseline"
                candidate_method = f"{operation}CallerOwned"
                per_run_payloads = []
                throughput_by_run = []
                allocation_passes = []
                for _, indexed in runs:
                    payloads = {}
                    run_regressions = []
                    run_allocations = []
                    for payload in PAYLOAD_BYTES:
                        baseline = indexed[(codec, payload, storage, baseline_method)]
                        candidate = indexed[(codec, payload, storage, candidate_method)]
                        base_throughput, base_alloc = metrics(baseline)
                        cand_throughput, cand_alloc = metrics(candidate)
                        payloads[payload] = (base_alloc, cand_alloc)
                        run_allocations.append(allocation_accepted(base_alloc, cand_alloc))
                        run_regressions.append(throughput_regressed(base_throughput, cand_throughput))
                    per_run_payloads.append(payloads)
                    allocation_passes.append(all(run_allocations))
                    throughput_by_run.append(run_regressions)
                is_eligible = eligible(codec, storage)
                scaling = is_eligible and all(scaling_demonstrated(payloads) for payloads in per_run_payloads)
                if not is_eligible:
                    verdict = "ineligible"
                elif any(all(run[payload_index] for run in throughput_by_run) for payload_index in range(len(PAYLOAD_BYTES))):
                    verdict = "design-review-required"
                elif all(allocation_passes) and scaling:
                    verdict = "accepted"
                else:
                    verdict = "not-demonstrated"
                for payload, payload_bytes in PAYLOAD_BYTES.items():
                    row = {"codec": codec, "operation": operation, "storage": storage, "payload": payload,
                           "payload_bytes": payload_bytes, "verdict": verdict, "scaling_demonstrated": str(scaling).lower()}
                    for index, (_, indexed) in enumerate(runs, start=1):
                        baseline = indexed[(codec, payload, storage, baseline_method)]
                        candidate = indexed[(codec, payload, storage, candidate_method)]
                        base_t, base_a = metrics(baseline)
                        cand_t, cand_a = metrics(candidate)
                        saved = finite(base_a["score"], "baseline allocation") - finite(cand_a["score"], "candidate allocation")
                        row.update({
                            f"run{index}_baseline_b_op": base_a["score"], f"run{index}_candidate_b_op": cand_a["score"],
                            f"run{index}_saved_b_op": saved, f"run{index}_saved_ratio": saved / payload_bytes,
                            f"run{index}_baseline_ops_s": base_t["score"], f"run{index}_candidate_ops_s": cand_t["score"],
                        })
                    rows.append(row)
    return rows


def write_comparison(path: pathlib.Path, rows: list[dict]) -> None:
    if path.exists() or path.is_symlink():
        raise FileExistsError(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)


def validate_delivery(args: argparse.Namespace) -> None:
    root = repo_root()
    requested_paths = [pathlib.Path(value) for value in args.run]
    if any(path.is_symlink() for path in requested_paths):
        raise ValueError("run path must not be a symlink")
    paths = [path.resolve(strict=True) for path in requested_paths]
    rows = comparison_rows(paths)
    requested_comparison = pathlib.Path(args.comparison)
    if requested_comparison.is_symlink():
        raise ValueError("comparison must not be a symlink")
    comparison = requested_comparison.resolve(strict=True)
    with tempfile.TemporaryDirectory() as temporary:
        expected = pathlib.Path(temporary) / "comparison.csv"
        write_comparison(expected, rows)
        if comparison.read_bytes() != expected.read_bytes():
            raise ValueError("comparison.csv does not match raw evidence")
    evidence_head = json.loads((paths[0] / "metadata.json").read_text())["commit"]
    if evidence_head != args.expected_head:
        raise ValueError("evidence HEAD mismatch")
    delivery_head = args.delivery_head or command("git", "rev-parse", "HEAD", cwd=root)
    subprocess.run(["git", "merge-base", "--is-ancestor", evidence_head, delivery_head], cwd=root, check=True)
    changed = command("git", "diff", "--name-only", f"{evidence_head}..{delivery_head}", cwd=root).splitlines()
    for path in changed:
        if not any(path == allowed or (allowed.endswith("/") and path.startswith(allowed)) for allowed in DELIVERY_ALLOWLIST):
            raise ValueError(f"delivery changed non-allowlisted path: {path}")
    jars = list((root / "io/io/build/benchmarks/test/jars").glob("*-JMH.jar"))
    if len(jars) != 1:
        raise ValueError("exactly one rebuilt JMH JAR is required")
    rebuilt_canonical = canonicalize_jar(
        jars[0], root / "io/io/build/issue-755-evidence/delivery-canonical-jmh.jar"
    )
    if sha256(rebuilt_canonical) != json.loads((paths[0] / "metadata.json").read_text())["jarSha256"]:
        raise ValueError("rebuilt JMH JAR SHA mismatch")
    current_runtime = environment_authority(root)
    recorded_metadata = json.loads((paths[0] / "metadata.json").read_text())
    for current_key, recorded_key in (
        ("jdk", "jdk"), ("vmName", "jvm"), ("vmVersion", "vmVersion"),
        ("jvmExecutable", "jvmExecutable"), ("os", "os"), ("cpu", "cpu"),
    ):
        if current_runtime[current_key] != recorded_metadata[recorded_key]:
            raise ValueError(f"delivery runtime identity mismatch: {current_key}")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser()
    sub = result.add_subparsers(dest="command", required=True)
    prepare_parser = sub.add_parser("prepare")
    prepare_parser.add_argument("--jar", required=True)
    prepare_parser.add_argument("--expected-head", required=True)
    prepare_parser.add_argument("--output-root", required=True)
    prepare_parser.add_argument("--receipt", required=True)
    for name in ("smoke", "run"):
        run_parser = sub.add_parser(name)
        run_parser.add_argument("--input-receipt")
        run_parser.add_argument("--jar")
        run_parser.add_argument("--profile", choices=PROFILE_ARGS, default="smoke" if name == "smoke" else "canonical")
        run_parser.add_argument("--output-root", required=True)
        run_parser.add_argument("--include", required=True)
        run_parser.add_argument("--param", action="append")
    compare_parser = sub.add_parser("compare")
    compare_parser.add_argument("--run", action="append", required=True)
    compare_parser.add_argument("--output", required=True)
    delivery_parser = sub.add_parser("validate-delivery")
    delivery_parser.add_argument("--run", action="append", required=True)
    delivery_parser.add_argument("--comparison", required=True)
    delivery_parser.add_argument("--expected-head", required=True)
    delivery_parser.add_argument("--delivery-head")
    return result


def direct_smoke_receipt(jar: pathlib.Path) -> dict:
    root = repo_root()
    head, tree = git_identity(root)
    dep_text = dependencies(root)
    raw_jar = jar.resolve(strict=True)
    canonical_jar = canonicalize_jar(
        raw_jar, root / "io/io/build/issue-755-evidence/smoke-canonical-jmh.jar"
    )
    return {
        "schemaVersion": SCHEMA_VERSION, "commit": head, "tree": tree, "jar": str(canonical_jar),
        "jarSha256": sha256(canonical_jar), "rawJar": str(raw_jar), "rawJarSha256": sha256(raw_jar),
        "jmhVersion": jmh_version(canonical_jar), "dependencies": dep_text,
        "dependenciesSha256": hashlib.sha256(dep_text.encode()).hexdigest(), "sourceInspection": source_inspection(root),
        "environmentAuthority": environment_authority(root), "runs": [], "outputRoot": "",
    }


def main() -> None:
    args = parser().parse_args()
    if args.command == "prepare":
        prepare(args)
    elif args.command in {"smoke", "run"}:
        receipt_path = pathlib.Path(args.input_receipt).resolve(strict=True) if args.input_receipt else None
        if receipt_path:
            receipt = load_receipt(receipt_path)
        elif args.command == "smoke" and args.jar:
            receipt = direct_smoke_receipt(pathlib.Path(args.jar))
        else:
            raise ValueError("run requires --input-receipt; smoke requires --input-receipt or --jar")
        path = execute_jmh(args, args.profile, receipt, receipt_path)
        print(path.resolve())
    elif args.command == "compare":
        requested_paths = [pathlib.Path(value) for value in args.run]
        if any(path.is_symlink() for path in requested_paths):
            raise ValueError("run path must not be a symlink")
        paths = [path.resolve(strict=True) for path in requested_paths]
        write_comparison(pathlib.Path(args.output), comparison_rows(paths))
    elif args.command == "validate-delivery":
        validate_delivery(args)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, RuntimeError, subprocess.CalledProcessError) as failure:
        print(f"ERROR: {failure}", file=sys.stderr)
        raise SystemExit(1)
