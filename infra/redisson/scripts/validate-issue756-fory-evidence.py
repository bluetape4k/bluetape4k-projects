#!/usr/bin/env python3
"""Validate issue #756 Redisson Fory decode evidence and conditional encode cells."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
from pathlib import Path

BENCHMARK_CLASS = "io.bluetape4k.redis.redisson.benchmark.Issue756ForyRedissonBenchmark"
BACKENDS = ("fory", "fastFory")
SOURCES = ("heap", "direct", "composite")


class ValidationError(ValueError):
    pass


def decode_method(backend: str, source: str, path: str) -> str:
    suffix = "CopiedBaseline" if path == "baseline" else "Candidate"
    return f"{backend}{source.title()}Decode{suffix}"


def encode_method(backend: str, path: str) -> str:
    suffix = "CopiedBaseline" if path == "baseline" else "Candidate"
    return f"{backend}Encode{suffix}"


def matrix(encode_disposition: str) -> list[dict[str, object]]:
    if encode_disposition not in ("implemented", "rejected"):
        raise ValidationError("encodeDisposition must be implemented or rejected")
    cells = [
        {
            "backend": backend,
            "operation": "decode",
            "source": source,
            "baseline": decode_method(backend, source, "baseline"),
            "candidate": decode_method(backend, source, "candidate"),
            "promotable": source != "composite",
        }
        for backend in BACKENDS
        for source in SOURCES
    ]
    if encode_disposition == "implemented":
        cells.extend(
            {
                "backend": backend,
                "operation": "encode",
                "source": "owned-output",
                "baseline": encode_method(backend, "baseline"),
                "candidate": encode_method(backend, "candidate"),
                "promotable": True,
            }
            for backend in BACKENDS
        )
    return cells


def expected_methods(encode_disposition: str) -> tuple[str, ...]:
    return tuple(name for cell in matrix(encode_disposition) for name in (cell["baseline"], cell["candidate"]))


def _metric(record: dict, secondary: bool) -> tuple[float, float, str]:
    metric = record["secondaryMetrics"]["gc.alloc.rate.norm"] if secondary else record["primaryMetric"]
    score, error, unit = float(metric["score"]), float(metric["scoreError"]), metric["scoreUnit"]
    if not math.isfinite(score) or not math.isfinite(error) or score <= 0 or error < 0:
        raise ValidationError("non-finite or non-positive metric")
    return score, error, unit


def validate_records(records: list[dict], encode_disposition: str) -> list[dict]:
    expected = expected_methods(encode_disposition)
    by_method = {}
    for record in records:
        prefix = BENCHMARK_CLASS + "."
        benchmark = record.get("benchmark", "")
        if not benchmark.startswith(prefix):
            raise ValidationError("unexpected benchmark class")
        name = benchmark[len(prefix):]
        if name in by_method:
            raise ValidationError("duplicate method")
        by_method[name] = record
    if set(by_method) != set(expected):
        raise ValidationError("method cardinality mismatch")

    result = []
    for cell in matrix(encode_disposition):
        baseline, candidate = by_method[cell["baseline"]], by_method[cell["candidate"]]
        for record in (baseline, candidate):
            if (
                record.get("mode") != "thrpt"
                or record.get("threads") != 1
                or record.get("forks") != 2
                or record.get("warmupIterations") != 3
                or record.get("measurementIterations") != 5
            ):
                raise ValidationError("JMH protocol mismatch")
        b_alloc, b_error, b_unit = _metric(baseline, True)
        c_alloc, c_error, c_unit = _metric(candidate, True)
        b_thrpt, _, b_thrpt_unit = _metric(baseline, False)
        c_thrpt, _, c_thrpt_unit = _metric(candidate, False)
        if (b_unit, c_unit) != ("B/op", "B/op") or (b_thrpt_unit, c_thrpt_unit) != ("ops/ms", "ops/ms"):
            raise ValidationError("metric unit mismatch")
        allocation_ratio = c_alloc / b_alloc
        throughput_delta = c_thrpt / b_thrpt - 1.0
        separated = c_alloc + c_error < b_alloc - b_error
        if not cell["promotable"]:
            disposition = "fallback"
        elif allocation_ratio <= 0.95 and separated and throughput_delta > -0.20:
            disposition = "accepted"
        elif allocation_ratio <= 0.95 and not separated:
            disposition = "inconclusive"
        else:
            disposition = "rejected"
        result.append(
            {
                **cell,
                "allocation_ratio": allocation_ratio,
                "throughput_delta": throughput_delta,
                "error_intervals_separated": separated,
                "disposition": disposition,
            }
        )
    return result


def validate_preflight(preflight: dict, encode_disposition: str) -> None:
    if preflight.get("schema_version") != 1 or preflight.get("status") != "passed":
        raise ValidationError("preflight status mismatch")
    if preflight.get("encode_disposition") != encode_disposition:
        raise ValidationError("encode disposition mismatch")
    actual = [entry.get("method") for entry in preflight.get("methods", [])]
    decode_expected = list(expected_methods("rejected"))
    if actual != decode_expected:
        raise ValidationError("preflight decode matrix mismatch")
    for entry in preflight["methods"]:
        expected_promotable = entry["source"] != "composite"
        if entry.get("promotable") != expected_promotable or not entry.get("state_preserved"):
            raise ValidationError("preflight fallback/state mismatch")


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_leaf(root: Path, encode_disposition: str) -> dict:
    records = json.loads((root / "jmh.json").read_text())
    preflight = json.loads((root / "preflight.json").read_text())
    validate_preflight(preflight, encode_disposition)
    comparisons = validate_records(records, encode_disposition)
    (root / "comparison.json").write_text(json.dumps(comparisons, indent=2) + "\n")
    with (root / "summary.csv").open("w", newline="") as stream:
        fields = ("backend", "operation", "source", "allocation_ratio", "throughput_delta", "disposition")
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        for row in comparisons:
            writer.writerow({name: row[name] for name in fields})
    validation = {
        "schema_version": 1,
        "status": "passed",
        "encodeDisposition": encode_disposition,
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
    parser.add_argument("--encode-disposition", required=True, choices=("implemented", "rejected"))
    args = parser.parse_args()
    validate_leaf(args.root, args.encode_disposition)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
