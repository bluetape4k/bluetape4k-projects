#!/usr/bin/env python3
"""Build once and run the dedicated issue #756 Lettuce Fory canonical A/B evidence."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
BENCHMARK_CLASS = "io.bluetape4k.redis.lettuce.benchmark.Issue756ForyCodecBenchmark"
PREFLIGHT_CLASS = "io.bluetape4k.redis.lettuce.benchmark.Issue756ForyCodecBenchmarkPreflight"
EXPECTED_METHODS = tuple(
    f"{backend}{target.title()}{suffix}"
    for backend in ("fory", "fastFory")
    for target in ("heap", "direct")
    for suffix in ("CopiedBaseline", "Candidate")
)


class RunnerError(RuntimeError):
    pass


def exact_include_regex() -> str:
    names = "|".join(re.escape(name) for name in EXPECTED_METHODS)
    return rf"^{re.escape(BENCHMARK_CLASS)}\.({names})$"


def fixed_jmh_argv(jar: Path, result: Path) -> list[str]:
    return [
        "java", "-Xms1g", "-Xmx1g", "-XX:+UseG1GC", "-jar", str(jar),
        "-f", "2", "-wi", "3", "-w", "1s", "-i", "5", "-r", "1s",
        "-t", "1", "-prof", "gc", "-bm", "thrpt", "-tu", "ms",
        "-rf", "json", "-rff", str(result), exact_include_regex(),
    ]


def require_clean_repository() -> str:
    status = subprocess.check_output(["git", "status", "--porcelain"], cwd=REPO, text=True).strip()
    if status:
        raise RunnerError("canonical evidence requires a clean repository")
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip()


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")

def normalize_jar(source: Path, destination: Path) -> Path:
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
    return destination


def validator_module():
    spec = importlib.util.spec_from_file_location("issue756_fory_lettuce_validator", HERE / "validate-issue756-fory-evidence.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def build_jar() -> Path:
    subprocess.run(
        ["./gradlew", ":bluetape4k-lettuce:benchmarkBenchmarkJar", "--no-daemon", "--no-configuration-cache"],
        cwd=REPO,
        check=True,
    )
    jars = sorted((REPO / "infra/lettuce/build/benchmarks/benchmark/jars").glob("*-JMH.jar"))
    if len(jars) != 1:
        raise RunnerError(f"expected one benchmark jar, found {len(jars)}")
    return normalize_jar(
        jars[0],
        REPO / "infra/lettuce/build/issue756/fory-canonical-executable.jar",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("docs/benchmarks/raw/issue-756-fory-followup/lettuce"))
    args = parser.parse_args()
    head = require_clean_repository()
    jar = build_jar()
    jar_sha = sha256_file(jar)
    preflight_run = subprocess.run(
        ["java", "-cp", str(jar), PREFLIGHT_CLASS],
        cwd=REPO,
        check=True,
        text=True,
        capture_output=True,
    )
    preflight = json.loads(preflight_run.stdout.strip().splitlines()[-1])
    validator = validator_module()
    validator.validate_preflight(preflight)
    for run_name in ("canonical-a", "canonical-b"):
        root = REPO / args.output / run_name
        root.mkdir(parents=True, exist_ok=False)
        write_json(root / "preflight.json", preflight)
        argv = fixed_jmh_argv(jar, root / "jmh.json")
        write_json(root / "argv.json", argv)
        write_json(
            root / "environment.json",
            {"java_home": os.environ.get("JAVA_HOME", ""), "os_name": os.uname().sysname, "arch": os.uname().machine},
        )
        write_json(root / "metadata.json", {"head": head, "jar_sha256": jar_sha, "run": run_name})
        subprocess.run(argv, cwd=REPO, check=True)
        write_json(root / "jmh.json", json.loads((root / "jmh.json").read_text()))
        validator.validate_leaf(root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
