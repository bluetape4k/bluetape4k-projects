#!/usr/bin/env python3
"""Validate the non-promotable issue #756 Redisson Fory encode probe."""

import csv
import hashlib
import json
import math
import re
import sys
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
EVIDENCE_ROOT = ROOT / "docs/benchmarks/raw/issue-756-fory-followup/feasibility"
BENCHMARK_CLASS = (
    "io.bluetape4k.redis.redisson.benchmark."
    "Issue756ForyEncodeFeasibilityBenchmark"
)
METHODS = {
    "fory": ("foryBaseline", "foryCandidate"),
    "fastFory": ("fastForyBaseline", "fastForyCandidate"),
}
EXPECTED_METHODS = {method for pair in METHODS.values() for method in pair}
ENVIRONMENT_KEYS = {
    "schemaVersion",
    "runId",
    "os",
    "kernel",
    "architecture",
    "cpuModel",
    "logicalCores",
    "javaHome",
    "javaVersion",
    "javaVendor",
    "javaVm",
    "gradleVersion",
    "benchmarkJarSha256",
    "commit",
}
SENSITIVE = re.compile(
    r"(?i)(password|passwd|token|secret|api[_-]?key|authorization|"
    r"https?://[^/\s:@]+:[^@\s]+@)"
)


class ValidationError(RuntimeError):
    pass


def fail(message):
    raise ValidationError(message)


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        fail(f"{path}: invalid JSON: {error}")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def finite_positive(value, label):
    if not isinstance(value, (int, float)) or not math.isfinite(value) or value <= 0:
        fail(f"{label} must be finite and positive")
    return float(value)


def method_name(record):
    prefix = BENCHMARK_CLASS + "."
    benchmark = record.get("benchmark")
    if not isinstance(benchmark, str) or not benchmark.startswith(prefix):
        fail(f"unexpected benchmark: {benchmark}")
    return benchmark[len(prefix):]


def metric(record, metric_name, expected_unit):
    source = record["primaryMetric"] if metric_name == "throughput" else (
        record.get("secondaryMetrics", {}).get(metric_name)
    )
    if not isinstance(source, dict):
        fail(f"{method_name(record)} missing {metric_name}")
    if source.get("scoreUnit") != expected_unit:
        fail(f"{method_name(record)} {metric_name} unit must be {expected_unit}")
    return (
        finite_positive(source.get("score"), f"{method_name(record)} {metric_name} score"),
        finite_positive(source.get("scoreError"), f"{method_name(record)} {metric_name} error"),
    )


def validate_argv(value):
    encoded = json.dumps(value, sort_keys=True)
    if SENSITIVE.search(encoded):
        fail("argv contains a sensitive token or credential-bearing URL")


def validate_leaf(evidence_root, run_id):
    leaf = Path(evidence_root) / run_id
    required = {
        "jmh.json",
        "argv.json",
        "environment.json",
        "metadata.json",
        "summary.csv",
    }
    missing = sorted(name for name in required if not (leaf / name).is_file())
    if missing:
        fail(f"{run_id} missing files: {missing}")
    records = read_json(leaf / "jmh.json")
    if not isinstance(records, list):
        fail(f"{run_id} jmh.json must be an array")
    names = [method_name(record) for record in records]
    if len(names) != len(set(names)):
        fail(f"{run_id} contains duplicate methods")
    if set(names) != EXPECTED_METHODS:
        fail(f"{run_id} method set mismatch: {sorted(names)}")
    by_name = dict(zip(names, records))
    argv = read_json(leaf / "argv.json")
    validate_argv(argv)
    environment = read_json(leaf / "environment.json")
    if set(environment) != ENVIRONMENT_KEYS:
        fail(f"{run_id} environment allowlist mismatch")
    metadata = read_json(leaf / "metadata.json")
    if metadata.get("runId") != run_id:
        fail(f"{run_id} metadata run id mismatch")
    if metadata.get("benchmarkJarSha256") != environment.get("benchmarkJarSha256"):
        fail(f"{run_id} benchmark JAR hash drift")
    if metadata.get("preflight", {}).get("status") != "passed":
        fail(f"{run_id} preflight failed")
    results = {}
    rows = []
    for backend, (baseline_name, candidate_name) in METHODS.items():
        baseline = by_name[baseline_name]
        candidate = by_name[candidate_name]
        for record in (baseline, candidate):
            if record.get("mode") != "thrpt":
                fail(f"{method_name(record)} mode must be thrpt")
            if record.get("threads") != 1 or record.get("forks") != 2:
                fail(f"{method_name(record)} thread/fork profile mismatch")
            if record.get("warmupIterations") != 3 or record.get("measurementIterations") != 5:
                fail(f"{method_name(record)} iteration profile mismatch")
        baseline_alloc, baseline_alloc_error = metric(
            baseline, "gc.alloc.rate.norm", "B/op"
        )
        candidate_alloc, candidate_alloc_error = metric(
            candidate, "gc.alloc.rate.norm", "B/op"
        )
        baseline_thrpt, _ = metric(baseline, "throughput", "ops/ms")
        candidate_thrpt, _ = metric(candidate, "throughput", "ops/ms")
        allocation_ratio = candidate_alloc / baseline_alloc
        allocation_interval_separated = (
            candidate_alloc + candidate_alloc_error
            < baseline_alloc - baseline_alloc_error
        )
        throughput_delta = candidate_thrpt / baseline_thrpt - 1.0
        accepted = (
            allocation_ratio <= 0.95
            and allocation_interval_separated
            and throughput_delta > -0.20
        )
        reason = "accepted" if accepted else (
            f"allocationRatio={allocation_ratio:.6f}, "
            f"allocationIntervalSeparated={allocation_interval_separated}, "
            f"throughputDelta={throughput_delta:.6f}"
        )
        results[backend] = {
            "accepted": accepted,
            "reason": reason,
            "baselineAllocation": baseline_alloc,
            "candidateAllocation": candidate_alloc,
            "allocationRatio": allocation_ratio,
            "throughputDelta": throughput_delta,
        }
        rows.extend(
            (
                [backend, "baseline", baseline_alloc, baseline_alloc_error, baseline_thrpt],
                [backend, "candidate", candidate_alloc, candidate_alloc_error, candidate_thrpt],
            )
        )
    with (leaf / "summary.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("backend", "path", "allocation_B_per_op", "allocation_error", "throughput_ops_per_ms"))
        writer.writerows(rows)
    comparison = {"schemaVersion": 1, "runId": run_id, "results": results}
    (leaf / "comparison.json").write_text(
        json.dumps(comparison, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    validation = {"schemaVersion": 1, "runId": run_id, "status": "passed"}
    (leaf / "validation.json").write_text(
        json.dumps(validation, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    hashes = {
        name: sha256(leaf / name)
        for name in (
            "jmh.json",
            "argv.json",
            "environment.json",
            "metadata.json",
            "summary.csv",
            "comparison.json",
            "validation.json",
        )
    }
    (leaf / "sha256.json").write_text(
        json.dumps(hashes, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    return metadata, results


def validate(root=EVIDENCE_ROOT):
    evidence_root = Path(root)
    metadata_a, results_a = validate_leaf(evidence_root, "probe-a")
    metadata_b, results_b = validate_leaf(evidence_root, "probe-b")
    if metadata_a.get("benchmarkJarSha256") != metadata_b.get("benchmarkJarSha256"):
        fail("probe A/B benchmark JAR hash mismatch")
    if metadata_a.get("commandSha256") != metadata_b.get("commandSha256"):
        fail("probe A/B command hash mismatch")
    clean = metadata_a.get("repositoryClean") is True and metadata_b.get("repositoryClean") is True
    accepted = clean and all(
        results[backend]["accepted"]
        for results in (results_a, results_b)
        for backend in METHODS
    )
    reasons = []
    if not clean:
        reasons.append("repository was not clean before the pinned build")
    for run_id, results in (("probe-a", results_a), ("probe-b", results_b)):
        for backend, result in results.items():
            if not result["accepted"]:
                reasons.append(f"{run_id}/{backend}: {result['reason']}")
    disposition = {
        "schemaVersion": 1,
        "probeDisposition": "accepted" if accepted else "rejected",
        "encodeDisposition": "pending" if accepted else "rejected",
        "reason": "all gates passed" if accepted else "; ".join(reasons),
        "benchmarkJarSha256": metadata_a["benchmarkJarSha256"],
        "runs": ["probe-a", "probe-b"],
    }
    (evidence_root / "disposition.json").write_text(
        json.dumps(disposition, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    return disposition


def main():
    try:
        print(json.dumps(validate(), sort_keys=True))
        return 0
    except ValidationError as error:
        print(json.dumps({"status": "failed", "reason": str(error)}, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
