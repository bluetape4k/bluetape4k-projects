#!/usr/bin/env python3
"""Build once and record two independent issue #756 Fory encode probe runs."""

import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OUTPUT = ROOT / "docs/benchmarks/raw/issue-756-fory-followup/feasibility"
BENCHMARK = (
    "io.bluetape4k.redis.redisson.benchmark."
    "Issue756ForyEncodeFeasibilityBenchmark"
)
PREFLIGHT = (
    "io.bluetape4k.redis.redisson.benchmark."
    "Issue756ForyEncodeFeasibilityPreflight"
)
SENSITIVE = re.compile(r"(?i)(password|passwd|token|secret|api[_-]?key|authorization)")


def run(command, capture=True):
    completed = subprocess.run(
        command,
        cwd=ROOT,
        check=False,
        text=True,
        capture_output=capture,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"command failed ({completed.returncode}): {' '.join(command)}\n"
            f"{completed.stdout}\n{completed.stderr}"
        )
    return completed.stdout if completed.stdout.strip() else completed.stderr


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def sha256_file(path):
    return sha256_bytes(Path(path).read_bytes())


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def normalize_jar(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source, "r") as input_archive:
        with zipfile.ZipFile(destination, "w", allowZip64=True) as output_archive:
            seen = set()
            for info in input_archive.infolist():
                upper = info.filename.upper()
                if upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC")):
                    continue
                if info.filename in seen:
                    continue
                seen.add(info.filename)
                output_archive.writestr(info, input_archive.read(info.filename))


def cpu_model():
    if platform.system() == "Darwin" and shutil.which("sysctl"):
        value = run(["sysctl", "-n", "machdep.cpu.brand_string"]).strip()
        if value:
            return value
    return platform.processor() or "unknown"


def main():
    try:
        dirty = [line for line in run(["git", "status", "--porcelain"]).splitlines() if line]
        commit = run(["git", "rev-parse", "HEAD"]).strip()
        tree = run(["git", "write-tree"]).strip()
        build_command = [
            "./gradlew",
            ":bluetape4k-redisson:benchmarkBenchmarkJar",
            "--no-daemon",
            "--no-configuration-cache",
        ]
        run(build_command)
        jars = sorted(
            ROOT.glob("infra/redisson/build/benchmarks/benchmark/jars/*-JMH.jar")
        )
        if len(jars) != 1:
            raise RuntimeError(f"expected one benchmark JAR, got {jars}")
        executable = ROOT / "infra/redisson/build/issue756/fory-feasibility-executable.jar"
        normalize_jar(jars[0], executable)
        jar_hash = sha256_file(executable)
        preflight_output = run(["java", "-cp", str(executable), PREFLIGHT])
        preflight = json.loads(preflight_output.strip().splitlines()[-1])
        if preflight.get("status") != "passed":
            raise RuntimeError("preflight did not pass")
        java_version = run(["java", "-version"]).strip()
        gradle_version = run(["./gradlew", "--version"])
        gradle_line = next(
            line.split()[-1]
            for line in gradle_version.splitlines()
            if line.startswith("Gradle ")
        )
        base_argv = [
            "java",
            "-jar",
            str(executable),
            f"{BENCHMARK}.*",
            "-t",
            "1",
            "-f",
            "2",
            "-wi",
            "3",
            "-w",
            "1s",
            "-i",
            "5",
            "-r",
            "1s",
            "-prof",
            "gc",
            "-bm",
            "thrpt",
            "-tu",
            "ms",
            "-jvmArgsAppend",
            "-Xms1g -Xmx1g -XX:+UseG1GC",
            "-rf",
            "json",
        ]
        if SENSITIVE.search(" ".join(base_argv)):
            raise RuntimeError("sensitive argv rejected")
        command_hash = sha256_bytes(
            json.dumps(base_argv, sort_keys=True, separators=(",", ":")).encode()
        )
        source_hash = sha256_bytes(
            b"".join(
                path.read_bytes()
                for path in (
                    ROOT / "infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/Issue756ForyEncodeFeasibilityBenchmark.kt",
                    ROOT / "infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/Issue756ForyEncodeFeasibilityPreflight.kt",
                )
            )
        )
        for run_id in ("probe-a", "probe-b"):
            leaf = OUTPUT / run_id
            leaf.mkdir(parents=True, exist_ok=True)
            jmh = leaf / "jmh.json"
            argv = [*base_argv, "-rff", str(jmh)]
            with (leaf / "run.log").open("w", encoding="utf-8") as stream:
                completed = subprocess.run(
                    argv,
                    cwd=ROOT,
                    check=False,
                    text=True,
                    stdout=stream,
                    stderr=subprocess.STDOUT,
                )
            if completed.returncode != 0:
                raise RuntimeError(f"{run_id} JMH failed; see {leaf / 'run.log'}")
            write_json(leaf / "argv.json", {"schemaVersion": 1, "argv": base_argv})
            environment = {
                "schemaVersion": 1,
                "runId": run_id,
                "os": platform.system(),
                "kernel": platform.release(),
                "architecture": platform.machine(),
                "cpuModel": cpu_model(),
                "logicalCores": os.cpu_count(),
                "javaHome": os.environ.get("JAVA_HOME"),
                "javaVersion": java_version.splitlines()[0],
                "javaVendor": "recorded-by-java-version-output",
                "javaVm": java_version.splitlines()[-1],
                "gradleVersion": gradle_line,
                "benchmarkJarSha256": jar_hash,
                "commit": commit,
            }
            write_json(leaf / "environment.json", environment)
            metadata = {
                "schemaVersion": 1,
                "runId": run_id,
                "recordedAt": datetime.now(timezone.utc).isoformat(),
                "commit": commit,
                "tree": tree,
                "repositoryClean": not dirty,
                "dirtyPaths": dirty,
                "benchmarkJarSha256": jar_hash,
                "benchmarkSourceSha256": source_hash,
                "commandSha256": command_hash,
                "buildCommand": build_command,
                "preflight": preflight,
            }
            write_json(leaf / "metadata.json", metadata)
            (leaf / "summary.csv").write_text("", encoding="utf-8")
        validator = HERE / "validate-issue756-fory-feasibility.py"
        completed = subprocess.run([sys.executable, str(validator)], cwd=ROOT)
        return completed.returncode
    except Exception as error:
        print(json.dumps({"status": "failed", "reason": str(error)}, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
