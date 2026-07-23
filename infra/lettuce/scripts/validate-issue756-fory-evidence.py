#!/usr/bin/env python3
"""Validate the dedicated issue #756 Fory Lettuce canonical JMH matrix."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
from pathlib import Path

BENCHMARK_CLASS = "io.bluetape4k.redis.lettuce.benchmark.Issue756ForyCodecBenchmark"
BACKENDS = ("fory", "fastFory")
TARGETS = ("heap", "direct")


class ValidationError(ValueError):
    pass


def method(backend: str, target: str, path: str) -> str:
    suffix = "CopiedBaseline" if path == "baseline" else "Candidate"
    return f"{backend}{target.title()}{suffix}"


def matrix() -> list[dict[str, object]]:
    return [
        {
            "backend": backend,
            "target": target,
            "baseline": method(backend, target, "baseline"),
            "candidate": method(backend, target, "candidate"),
            "promotable": True,
        }
        for backend in BACKENDS
        for target in TARGETS
    ]


EXPECTED_METHODS = tuple(
    name for cell in matrix() for name in (cell["baseline"], cell["candidate"])
)


def _metric(record: dict, metric_name: str) -> tuple[float, float, str]:
    metric = record["primaryMetric"] if metric_name == "throughput" else record["secondaryMetrics"]["gc.alloc.rate.norm"]
    score = float(metric["score"])
    error = float(metric["scoreError"])
    unit = metric["scoreUnit"]
    if not math.isfinite(score) or not math.isfinite(error) or score <= 0 or error < 0:
        raise ValidationError(f"invalid {metric_name} metric")
    return score, error, unit


def validate_records(records: list[dict]) -> list[dict]:
    by_method: dict[str, dict] = {}
    for record in records:
        full_name = record.get("benchmark", "")
        prefix = BENCHMARK_CLASS + "."
        if not full_name.startswith(prefix):
            raise ValidationError(f"unexpected benchmark: {full_name}")
        name = full_name[len(prefix):]
        if name in by_method:
            raise ValidationError(f"duplicate method: {name}")
        by_method[name] = record
    if set(by_method) != set(EXPECTED_METHODS):
        raise ValidationError("method cardinality mismatch")

    comparisons = []
    for cell in matrix():
        baseline = by_method[cell["baseline"]]
        candidate = by_method[cell["candidate"]]
        for record in (baseline, candidate):
            if (
                record.get("mode") != "thrpt"
                or record.get("threads") != 1
                or record.get("forks") != 2
                or record.get("warmupIterations") != 3
                or record.get("measurementIterations") != 5
            ):
                raise ValidationError("JMH protocol mismatch")
        b_alloc, b_alloc_error, b_alloc_unit = _metric(baseline, "allocation")
        c_alloc, c_alloc_error, c_alloc_unit = _metric(candidate, "allocation")
        b_thrpt, _, b_thrpt_unit = _metric(baseline, "throughput")
        c_thrpt, _, c_thrpt_unit = _metric(candidate, "throughput")
        if b_alloc_unit != "B/op" or c_alloc_unit != "B/op":
            raise ValidationError("allocation unit must be B/op")
        if b_thrpt_unit != "ops/ms" or c_thrpt_unit != "ops/ms":
            raise ValidationError("throughput unit must be ops/ms")
        allocation_ratio = c_alloc / b_alloc
        throughput_delta = c_thrpt / b_thrpt - 1.0
        separated = c_alloc + c_alloc_error < b_alloc - b_alloc_error
        disposition = (
            "accepted"
            if allocation_ratio <= 0.95 and separated and throughput_delta > -0.20
            else "inconclusive"
            if allocation_ratio <= 0.95 and not separated
            else "rejected"
        )
        comparisons.append(
            {
                **cell,
                "allocation_ratio": allocation_ratio,
                "throughput_delta": throughput_delta,
                "error_intervals_separated": separated,
                "disposition": disposition,
            }
        )
    return comparisons


def validate_preflight(preflight: dict) -> None:
    if preflight.get("schema_version") != 1 or preflight.get("status") != "passed":
        raise ValidationError("preflight status mismatch")
    methods = [cell.get("method") for cell in preflight.get("methods", [])]
    if methods != list(EXPECTED_METHODS):
        raise ValidationError("preflight method matrix mismatch")
    for cell in preflight["methods"]:
        if not cell.get("prefix_preserved") or not cell.get("state_preserved"):
            raise ValidationError("preflight state mismatch")


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_leaf(root: Path) -> dict:
    records = json.loads((root / "jmh.json").read_text())
    preflight = json.loads((root / "preflight.json").read_text())
    validate_preflight(preflight)
    comparisons = validate_records(records)
    (root / "comparison.json").write_text(json.dumps(comparisons, indent=2) + "\n")
    with (root / "summary.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(
            stream,
            fieldnames=("backend", "target", "allocation_ratio", "throughput_delta", "disposition"),
            lineterminator="\n",
        )
        writer.writeheader()
        for row in comparisons:
            writer.writerow({name: row[name] for name in writer.fieldnames})
    validation = {
        "schema_version": 1,
        "status": "passed",
        "method_count": len(records),
        "comparisons": comparisons,
        "hashes": {
            "jmh.json": sha256_file(root / "jmh.json"),
            "preflight.json": sha256_file(root / "preflight.json"),
            "argv.json": sha256_file(root / "argv.json"),
            "environment.json": sha256_file(root / "environment.json"),
            "metadata.json": sha256_file(root / "metadata.json"),
            "comparison.json": sha256_file(root / "comparison.json"),
            "summary.csv": sha256_file(root / "summary.csv"),
        },
    }
    (root / "validation.json").write_text(json.dumps(validation, indent=2) + "\n")
    return validation


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    validate_leaf(args.root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
