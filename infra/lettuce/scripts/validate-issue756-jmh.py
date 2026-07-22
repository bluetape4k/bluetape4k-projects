#!/usr/bin/env python3
"""Fail-closed validator for issue 756 Lettuce codec JMH evidence."""

import argparse
import csv
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from decimal import Decimal, InvalidOperation
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
BENCHMARK_CLASS = "io.bluetape4k.redis.lettuce.benchmark.LettuceCodecBenchmark"
BACKENDS = ("jdk", "kryo", "jackson2", "jackson3")
TARGETS = ("heap", "direct")
BACKEND_CLASSES = {
    "jdk": "io.bluetape4k.io.serializer.JdkBinarySerializer",
    "kryo": "io.bluetape4k.io.serializer.KryoBinarySerializer",
    "jackson2": "io.bluetape4k.jackson.JacksonSerializer",
    "jackson3": "io.bluetape4k.jackson3.JacksonSerializer",
}
EXPECTED_PROTOCOL = {
    "forks": 2,
    "warmup_iterations": 3,
    "measurement_iterations": 5,
    "threads": 1,
    "profiler": "gc",
    "mode": "thrpt",
    "throughput_unit": "ops/ms",
}
EXPECTED_FIXTURE_SHAPE = {
    "allocator_class": "io.netty.buffer.PooledByteBufAllocator",
    "pooled": True,
    "capacity": 512,
    "max_capacity": 512,
    "reader_index": 3,
    "writer_index": 7,
    "headroom": 505,
}
ALLOCATION_THRESHOLD_PERCENT = Decimal("5.000")
THROUGHPUT_FLOOR_PERCENT = Decimal("-20.000")
ALLOWED_POST_MEASUREMENT_EXACT = frozenset(
    {
        "docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md",
        "io/io/README.md",
        "io/io/README.ko.md",
        "io/json/README.md",
        "io/json/README.ko.md",
        "io/jackson2/README.md",
        "io/jackson2/README.ko.md",
        "io/jackson3/README.md",
        "io/jackson3/README.ko.md",
        "infra/lettuce/README.md",
        "infra/lettuce/README.ko.md",
    }
)
ALLOWED_POST_MEASUREMENT_PREFIX = "docs/benchmarks/raw/issue-756/"


class ValidationError(ValueError):
    def __init__(self, reason_code, detail):
        self.reason_code = reason_code
        self.detail = detail
        super().__init__(f"{reason_code}: {detail}")


def fail(reason_code, detail):
    raise ValidationError(reason_code, detail)


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


def exact_include_regex():
    methods = "|".join(re.escape(method) for method in EXPECTED_METHODS)
    return "^" + re.escape(BENCHMARK_CLASS) + r"\.(?:" + methods + r")$"


def _canonical_json(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def _reset_snapshot_identity(snapshot):
    return (
        "ResetSnapshot("
        f"capacity={snapshot['capacity']}, "
        f"maxCapacity={snapshot['max_capacity']}, "
        f"readerIndex={snapshot['reader_index']}, "
        f"writerIndex={snapshot['writer_index']}"
        ")"
    )


def preflight_fixture_sha256(preflight):
    fixture = preflight["fixture"]
    fixture_json = (
        "{"
        f'"payload_sha256":{json.dumps(fixture["payload_sha256"])},'
        f'"allocator_class":{json.dumps(fixture["allocator_class"])},'
        f'"pooled":{str(fixture["pooled"]).lower()},'
        f'"capacity":{fixture["capacity"]},'
        f'"max_capacity":{fixture["max_capacity"]},'
        f'"reader_index":{fixture["reader_index"]},'
        f'"writer_index":{fixture["writer_index"]},'
        f'"headroom":{fixture["headroom"]}'
        "}"
    )
    identity = [fixture_json]
    for cell in preflight["cells"]:
        values = (
            cell["backend"],
            cell["target"],
            cell["path"],
            cell["method"],
            cell["paired_baseline"],
            cell["backend_class"],
            cell["backend_config_sha256"],
            cell["payload_sha256"],
            cell["target_kind"],
            cell["wire_sha256"],
            str(cell["written_count"]),
            str(cell["prefix_preserved"]).lower(),
            str(cell["baseline_dispatch_count"]),
            str(cell["candidate_dispatch_count"]),
            _reset_snapshot_identity(cell["reset_before"]),
            _reset_snapshot_identity(cell["reset_after"]),
        )
        identity.append("|" + "|".join(values))
    for backend in BACKENDS:
        report = preflight["dispatch"][backend]
        identity.append(
            "|"
            + backend
            + "|"
            + "|".join(
                (
                    report["declaring_class"],
                    report["dispatch_kind"],
                    report["runtime_declaring_class"],
                    report["runtime_dispatch_kind"],
                )
            )
        )
    return hashlib.sha256("".join(identity).encode("utf-8")).hexdigest()


def _sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _require_mapping(value, reason_code, label):
    if not isinstance(value, dict):
        fail(reason_code, f"{label} must be a JSON object")
    return value


def _require_list(value, reason_code, label):
    if not isinstance(value, list):
        fail(reason_code, f"{label} must be a JSON array")
    return value


def _require_keys(value, required, reason_code, label):
    missing = sorted(set(required) - set(value))
    if missing:
        fail(reason_code, f"{label} missing fields: {', '.join(missing)}")


def _decimal(value, reason_code, label, *, positive=False, non_negative=False):
    if isinstance(value, bool):
        fail(reason_code, f"{label} must be numeric")
    try:
        result = Decimal(str(value))
    except (InvalidOperation, ValueError, TypeError):
        fail(reason_code, f"{label} must be numeric")
    if not result.is_finite():
        fail(reason_code, f"{label} must be finite")
    if positive and result <= 0:
        fail(reason_code, f"{label} must be positive")
    if non_negative and result < 0:
        fail(reason_code, f"{label} must be non-negative")
    return result


def validate_matrix(cells):
    cells = _require_list(cells, "MATRIX_EXACT", "matrix")
    if len(cells) != 16:
        fail("MATRIX_EXACT", f"expected 16 cells, got {len(cells)}")
    normalized = []
    for index, raw in enumerate(cells):
        cell = _require_mapping(raw, "MATRIX_EXACT", f"matrix[{index}]")
        _require_keys(
            cell,
            ("backend", "target", "path", "method", "paired_baseline"),
            "MATRIX_EXACT",
            f"matrix[{index}]",
        )
        normalized.append(
            {
                key: cell[key]
                for key in ("backend", "target", "path", "method", "paired_baseline")
            }
        )
    methods = [cell["method"] for cell in normalized]
    if len(set(methods)) != len(methods):
        fail("MATRIX_EXACT", "duplicate benchmark method")
    expected_by_method = {cell["method"]: cell for cell in EXPECTED_MATRIX}
    actual_by_method = {cell["method"]: cell for cell in normalized}
    if set(actual_by_method) != set(expected_by_method):
        missing = sorted(set(expected_by_method) - set(actual_by_method))
        extra = sorted(set(actual_by_method) - set(expected_by_method))
        fail("MATRIX_EXACT", f"missing={missing}; extra={extra}")
    for method in EXPECTED_METHODS:
        actual = actual_by_method[method]
        expected = expected_by_method[method]
        if actual["paired_baseline"] != expected["paired_baseline"]:
            fail(
                "PAIRING_MISMATCH",
                f"{method} paired_baseline expected {expected['paired_baseline']}, "
                f"got {actual['paired_baseline']}",
            )
        if actual != expected:
            fail("MATRIX_EXACT", f"{method} cell identity mismatch")
    return normalized


def validate_classpath(metadata):
    metadata = _require_mapping(metadata, "CLASSPATH_INVALID", "metadata")
    entries = _require_list(metadata.get("classpath"), "CLASSPATH_INVALID", "classpath")
    if not entries:
        fail("CLASSPATH_INVALID", "classpath is empty")
    benchmark_jar = _require_mapping(
        metadata.get("benchmark_jar"), "CLASSPATH_INVALID", "benchmark_jar"
    )
    paths = []
    project_indexes = []
    for index, raw in enumerate(entries):
        entry = _require_mapping(raw, "CLASSPATH_INVALID", f"classpath[{index}]")
        _require_keys(entry, ("path", "sha256", "kind"), "CLASSPATH_INVALID", f"classpath[{index}]")
        path = Path(entry["path"])
        if not path.is_absolute() or not path.exists() or not path.is_file():
            fail("CLASSPATH_INVALID", f"classpath[{index}] is not an existing absolute file")
        resolved = str(path.resolve())
        if resolved in paths:
            fail("CLASSPATH_INVALID", f"duplicate classpath entry: {resolved}")
        paths.append(resolved)
        if entry["kind"] == "jackson2-project":
            project_indexes.append(index)
        actual_hash = _sha256_file(path)
        if actual_hash != entry["sha256"]:
            fail("ARTIFACT_IDENTITY_MISMATCH", f"classpath[{index}] sha256 mismatch")
    if len(project_indexes) != 1:
        fail("CLASSPATH_INVALID", f"expected one Jackson 2 project JAR, got {len(project_indexes)}")
    if project_indexes[0] != 1:
        fail("CLASSPATH_IDENTITY_MISMATCH", "Jackson 2 project JAR must be classpath entry 1")
    if paths[0] != str(Path(benchmark_jar.get("path", "")).resolve()):
        fail("CLASSPATH_IDENTITY_MISMATCH", "benchmark JAR must be classpath entry 0")
    if benchmark_jar.get("sha256") != entries[0]["sha256"]:
        fail("CLASSPATH_IDENTITY_MISMATCH", "benchmark JAR metadata differs from classpath entry 0")
    if _canonical_json(benchmark_jar) != _canonical_json(entries[0]):
        fail("CLASSPATH_IDENTITY_MISMATCH", "benchmark JAR metadata must equal classpath entry 0")
    normalization = entries[0].get("normalization")
    if normalization is not None:
        normalization = _require_mapping(
            normalization, "ARTIFACT_IDENTITY_MISMATCH", "benchmark normalization"
        )
        required = (
            "policy",
            "source_path",
            "source_sha256",
            "executable_path",
            "executable_sha256",
            "removed_entries",
        )
        _require_keys(
            normalization,
            required,
            "ARTIFACT_IDENTITY_MISMATCH",
            "benchmark normalization",
        )
        if normalization["policy"] != "strip-meta-inf-signatures-v1":
            fail("ARTIFACT_IDENTITY_MISMATCH", "unexpected benchmark normalization policy")
        source = Path(normalization["source_path"])
        if not source.is_absolute() or not source.is_file():
            fail("ARTIFACT_IDENTITY_MISMATCH", "benchmark normalization source is missing")
        if _sha256_file(source) != normalization["source_sha256"]:
            fail("ARTIFACT_IDENTITY_MISMATCH", "benchmark normalization source hash mismatch")
        if normalization["executable_path"] != entries[0]["path"]:
            fail("ARTIFACT_IDENTITY_MISMATCH", "normalized executable path mismatch")
        if normalization["executable_sha256"] != entries[0]["sha256"]:
            fail("ARTIFACT_IDENTITY_MISMATCH", "normalized executable hash mismatch")
        removed = normalization["removed_entries"]
        if not isinstance(removed, list) or removed != sorted(removed):
            fail("ARTIFACT_IDENTITY_MISMATCH", "removed signature entries are not canonical")
        for name in removed:
            normalized = name.upper()
            if not normalized.startswith("META-INF/") or not normalized.endswith(
                (".SF", ".RSA", ".DSA")
            ):
                fail("ARTIFACT_IDENTITY_MISMATCH", f"non-signature entry removed: {name}")
    return entries


def _validate_protocol(metadata):
    protocol = _require_mapping(metadata.get("protocol"), "PROTOCOL_MISMATCH", "protocol")
    if protocol != EXPECTED_PROTOCOL:
        fail("PROTOCOL_MISMATCH", f"expected {_canonical_json(EXPECTED_PROTOCOL)}, got {_canonical_json(protocol)}")


def _validate_fixture(metadata):
    fixture = _require_mapping(metadata.get("fixture"), "FIXTURE_MISMATCH", "fixture")
    _require_keys(fixture, ("payload_sha256", *EXPECTED_FIXTURE_SHAPE), "FIXTURE_MISMATCH", "fixture")
    for field, expected in EXPECTED_FIXTURE_SHAPE.items():
        if fixture[field] != expected:
            fail("FIXTURE_MISMATCH", f"fixture.{field} expected {expected}, got {fixture[field]}")
    payload_sha = fixture["payload_sha256"]
    if not isinstance(payload_sha, str) or len(payload_sha) != 64:
        fail("FIXTURE_MISMATCH", "fixture.payload_sha256 must be 64 characters")
    return fixture


def _validate_preflight(metadata, fixture, matrix):
    preflight = _require_mapping(metadata.get("preflight"), "PREFLIGHT_MISMATCH", "preflight")
    _require_keys(
        preflight,
        ("schema_version", "status", "fixture_sha256", "cells", "retained_backend_checks"),
        "PREFLIGHT_MISMATCH",
        "preflight",
    )
    if preflight["schema_version"] != 1 or preflight["status"] != "passed":
        fail("PREFLIGHT_MISMATCH", "preflight schema/status mismatch")
    cells = _require_list(preflight["cells"], "PREFLIGHT_MISMATCH", "preflight.cells")
    if len(cells) != 16:
        fail("PREFLIGHT_MISMATCH", f"expected 16 preflight cells, got {len(cells)}")
    expected_by_method = {cell["method"]: cell for cell in matrix}
    actual_by_method = {}
    for index, raw in enumerate(cells):
        cell = _require_mapping(raw, "PREFLIGHT_MISMATCH", f"preflight.cells[{index}]")
        required = (
            "backend",
            "target",
            "path",
            "method",
            "paired_baseline",
            "backend_class",
            "backend_config_sha256",
            "payload_sha256",
            "target_kind",
            "wire_sha256",
            "written_count",
            "prefix_preserved",
            "baseline_dispatch_count",
            "candidate_dispatch_count",
            "reset_before",
            "reset_after",
        )
        _require_keys(cell, required, "PREFLIGHT_MISMATCH", f"preflight.cells[{index}]")
        method = cell["method"]
        if method in actual_by_method:
            fail("PREFLIGHT_MISMATCH", f"duplicate preflight method: {method}")
        actual_by_method[method] = cell
        expected = expected_by_method.get(method)
        if expected is None:
            fail("PREFLIGHT_MISMATCH", f"unexpected preflight method: {method}")
        for field in ("backend", "target", "path", "paired_baseline"):
            if cell[field] != expected[field]:
                fail("PREFLIGHT_MISMATCH", f"{method}.{field} mismatch")
        if cell["backend_class"] != BACKEND_CLASSES[cell["backend"]]:
            fail("PREFLIGHT_MISMATCH", f"{method}.backend_class mismatch")
        if cell["payload_sha256"] != fixture["payload_sha256"]:
            fail("FIXTURE_MISMATCH", f"{method}.payload_sha256 mismatch")
        if cell["target_kind"] != cell["target"]:
            fail("PREFLIGHT_MISMATCH", f"{method}.target_kind mismatch")
        if not isinstance(cell["backend_config_sha256"], str) or len(cell["backend_config_sha256"]) != 64:
            fail("PREFLIGHT_MISMATCH", f"{method}.backend_config_sha256 invalid")
        if not isinstance(cell["wire_sha256"], str) or len(cell["wire_sha256"]) != 64:
            fail("PREFLIGHT_MISMATCH", f"{method}.wire_sha256 invalid")
        if not isinstance(cell["written_count"], int) or cell["written_count"] <= 0:
            fail("PREFLIGHT_MISMATCH", f"{method}.written_count invalid")
        if cell["prefix_preserved"] is not True:
            fail("PREFLIGHT_MISMATCH", f"{method}.prefix_preserved mismatch")
        expected_baseline_count = 1 if cell["path"] == "baseline" else 0
        expected_candidate_count = 1 if cell["path"] == "candidate" else 0
        if (
            cell["baseline_dispatch_count"] != expected_baseline_count
            or cell["candidate_dispatch_count"] != expected_candidate_count
        ):
            fail("PREFLIGHT_MISMATCH", f"{method} dispatch count mismatch")
        before = _require_mapping(cell["reset_before"], "RESET_DRIFT", f"{method}.reset_before")
        after = _require_mapping(cell["reset_after"], "RESET_DRIFT", f"{method}.reset_after")
        reset_expected = {
            field: fixture[field]
            for field in ("capacity", "max_capacity", "reader_index", "writer_index")
        }
        if before != reset_expected or after != reset_expected:
            fail("RESET_DRIFT", f"{method} reset capacity/index drift")
    if set(actual_by_method) != EXPECTED_METHOD_SET:
        fail("PREFLIGHT_MISMATCH", "preflight method set mismatch")
    for backend in BACKENDS:
        for target in TARGETS:
            baseline = actual_by_method[_method_name(backend, target, "baseline")]
            candidate = actual_by_method[_method_name(backend, target, "candidate")]
            for field in (
                "backend_class",
                "backend_config_sha256",
                "payload_sha256",
                "wire_sha256",
                "written_count",
                "reset_before",
                "reset_after",
            ):
                if baseline[field] != candidate[field]:
                    fail("PREFLIGHT_MISMATCH", f"{backend}/{target} paired {field} mismatch")
    retained = _require_mapping(
        preflight["retained_backend_checks"],
        "PREFLIGHT_MISMATCH",
        "preflight.retained_backend_checks",
    )
    if set(retained) != set(BACKENDS):
        fail("PREFLIGHT_MISMATCH", "retained backend check set mismatch")
    for backend, check in retained.items():
        expected = {"status": "passed", "exception_parity": True, "state_preserved": True}
        if check != expected:
            fail("PREFLIGHT_MISMATCH", f"{backend} retained backend check failed")


def _validate_dispatch(metadata):
    dispatch = _require_mapping(metadata.get("dispatch"), "DISPATCH_MISMATCH", "dispatch")
    if set(dispatch) != set(BACKENDS):
        fail("DISPATCH_MISMATCH", "dispatch backend set mismatch")
    result = {}
    for backend in BACKENDS:
        entry = _require_mapping(dispatch[backend], "DISPATCH_MISMATCH", f"dispatch.{backend}")
        required = (
            "declaring_class",
            "dispatch_kind",
            "runtime_declaring_class",
            "runtime_dispatch_kind",
        )
        _require_keys(entry, required, "DISPATCH_MISMATCH", f"dispatch.{backend}")
        if entry["dispatch_kind"] not in ("declared-direct", "inherited-default"):
            fail("DISPATCH_MISMATCH", f"dispatch.{backend}.dispatch_kind invalid")
        if entry["declaring_class"] != entry["runtime_declaring_class"]:
            fail("DISPATCH_MISMATCH", f"dispatch.{backend}.declaring_class runtime mismatch")
        if entry["dispatch_kind"] != entry["runtime_dispatch_kind"]:
            fail("DISPATCH_MISMATCH", f"dispatch.{backend}.dispatch_kind runtime mismatch")
        result[backend] = entry["dispatch_kind"]
    return result


def validate_metadata(metadata, *, validate_files=True):
    metadata = _require_mapping(metadata, "SOURCE_IDENTITY_MISMATCH", "metadata")
    required = (
        "schema_version",
        "run_id",
        "benchmark_input_sha",
        "benchmark_input_tree",
        "clean_status",
        "benchmark_jar",
        "classpath",
        "protocol",
        "fixture",
        "matrix",
        "preflight",
        "preflight_sha256",
        "dispatch",
    )
    _require_keys(metadata, required, "SOURCE_IDENTITY_MISMATCH", "metadata")
    if metadata["schema_version"] != 1:
        fail("SOURCE_IDENTITY_MISMATCH", "metadata schema_version must be 1")
    if metadata["run_id"] not in ("canonical-a", "canonical-b"):
        fail("SOURCE_IDENTITY_MISMATCH", "run_id must be canonical-a or canonical-b")
    if metadata["clean_status"] != "clean":
        fail("BUILD_INPUT_DIRTY", "benchmark input must be clean")
    for field in ("benchmark_input_sha", "benchmark_input_tree"):
        value = metadata[field]
        if not isinstance(value, str) or len(value) != 40:
            fail("SOURCE_IDENTITY_MISMATCH", f"{field} must be a 40-character Git object ID")
    _validate_protocol(metadata)
    fixture = _validate_fixture(metadata)
    matrix = validate_matrix(metadata["matrix"])
    _validate_preflight(metadata, fixture, matrix)
    dispatch = _validate_dispatch(metadata)
    expected_fixture_sha = preflight_fixture_sha256(metadata["preflight"])
    if metadata["preflight"].get("fixture_sha256") != expected_fixture_sha:
        fail("PREFLIGHT_BINDING_MISMATCH", "preflight fixture_sha256 mismatch")
    expected_preflight_sha = hashlib.sha256(
        _canonical_json(metadata["preflight"]).encode("utf-8")
    ).hexdigest()
    if metadata["preflight_sha256"] != expected_preflight_sha:
        fail("PREFLIGHT_BINDING_MISMATCH", "preflight document sha256 mismatch")
    if validate_files:
        validate_classpath(metadata)
    return dispatch


def _jmh_method(record):
    benchmark = record.get("benchmark")
    prefix = BENCHMARK_CLASS + "."
    if not isinstance(benchmark, str) or not benchmark.startswith(prefix):
        fail("MATRIX_EXACT", f"unexpected benchmark class: {benchmark}")
    return benchmark[len(prefix) :]


def validate_jmh_records(records):
    records = _require_list(records, "MATRIX_EXACT", "JMH result")
    rows = {}
    for index, raw in enumerate(records):
        record = _require_mapping(raw, "MATRIX_EXACT", f"JMH[{index}]")
        method = _jmh_method(record)
        if method in rows:
            fail("MATRIX_EXACT", f"duplicate JMH method: {method}")
        if method not in EXPECTED_METHOD_SET:
            fail("MATRIX_EXACT", f"unexpected JMH method: {method}")
        protocol_fields = {
            "forks": record.get("forks"),
            "warmup_iterations": record.get("warmupIterations"),
            "measurement_iterations": record.get("measurementIterations"),
            "threads": record.get("threads"),
        }
        for field, actual in protocol_fields.items():
            if actual != EXPECTED_PROTOCOL[field]:
                fail("PROTOCOL_MISMATCH", f"{method}.{field} mismatch")
        if "primaryMetric" not in record:
            fail("METRIC_MISSING", f"{method} missing primaryMetric")
        secondary = record.get("secondaryMetrics")
        if not isinstance(secondary, dict) or "gc.alloc.rate.norm" not in secondary:
            fail("METRIC_MISSING", f"{method} missing gc.alloc.rate.norm")
        primary = _require_mapping(
            record["primaryMetric"], "THROUGHPUT_METRIC_INVALID", f"{method}.primaryMetric"
        )
        allocation = _require_mapping(
            secondary["gc.alloc.rate.norm"],
            "ALLOCATION_METRIC_INVALID",
            f"{method}.gc.alloc.rate.norm",
        )
        if record.get("mode") != "thrpt" or primary.get("scoreUnit") != "ops/ms":
            fail("THROUGHPUT_METRIC_INVALID", f"{method} throughput mode/unit mismatch")
        throughput_score = _decimal(
            primary.get("score"), "THROUGHPUT_METRIC_INVALID", f"{method}.throughput.score", positive=True
        )
        throughput_error = _decimal(
            primary.get("scoreError"),
            "THROUGHPUT_METRIC_INVALID",
            f"{method}.throughput.scoreError",
            non_negative=True,
        )
        if allocation.get("scoreUnit") != "B/op":
            fail("ALLOCATION_METRIC_INVALID", f"{method} allocation unit must be B/op")
        allocation_score = _decimal(
            allocation.get("score"),
            "ALLOCATION_METRIC_INVALID",
            f"{method}.allocation.score",
            non_negative=True,
        )
        allocation_error = _decimal(
            allocation.get("scoreError"),
            "ALLOCATION_METRIC_INVALID",
            f"{method}.allocation.scoreError",
            non_negative=True,
        )
        cell = next(cell for cell in EXPECTED_MATRIX if cell["method"] == method)
        if cell["path"] == "baseline" and allocation_score == 0:
            fail("ALLOCATION_BASELINE_ZERO", f"{method} allocation baseline must be positive")
        rows[method] = {
            "allocation": allocation_score,
            "allocation_error": allocation_error,
            "throughput": throughput_score,
            "throughput_error": throughput_error,
        }
    if set(rows) != EXPECTED_METHOD_SET:
        missing = sorted(EXPECTED_METHOD_SET - set(rows))
        fail("MATRIX_EXACT", f"missing JMH methods: {missing}")
    return {"rows": rows}


def allocation_delta_percent(baseline, candidate):
    baseline = _decimal(baseline, "ALLOCATION_METRIC_INVALID", "baseline allocation", positive=True)
    candidate = _decimal(candidate, "ALLOCATION_METRIC_INVALID", "candidate allocation", non_negative=True)
    return (baseline - candidate) * Decimal("100") / baseline


def throughput_delta_percent(baseline, candidate):
    baseline = _decimal(baseline, "THROUGHPUT_METRIC_INVALID", "baseline throughput", positive=True)
    candidate = _decimal(candidate, "THROUGHPUT_METRIC_INVALID", "candidate throughput", positive=True)
    return (candidate - baseline) * Decimal("100") / baseline


def cell_verdict(
    baseline_allocation,
    candidate_allocation,
    baseline_throughput,
    candidate_throughput,
    dispatch_kind,
):
    if dispatch_kind == "inherited-default":
        return "ineligible"
    if dispatch_kind != "declared-direct":
        fail("DISPATCH_MISMATCH", f"unsupported dispatch kind: {dispatch_kind}")
    throughput_delta = throughput_delta_percent(baseline_throughput, candidate_throughput)
    if throughput_delta <= THROUGHPUT_FLOOR_PERCENT:
        return "ineligible"
    allocation_delta = allocation_delta_percent(baseline_allocation, candidate_allocation)
    return "accepted" if allocation_delta >= ALLOCATION_THRESHOLD_PERCENT else "inconclusive"


def compare_runs(first, second, dispatch):
    first_rows = first.get("rows", {})
    second_rows = second.get("rows", {})
    if set(first_rows) != EXPECTED_METHOD_SET or set(second_rows) != EXPECTED_METHOD_SET:
        fail("MATRIX_EXACT", "both canonical runs must contain the exact method matrix")
    results = []
    for backend in BACKENDS:
        dispatch_kind = dispatch.get(backend)
        if dispatch_kind not in ("declared-direct", "inherited-default"):
            fail("DISPATCH_MISMATCH", f"missing dispatch for {backend}")
        for target in TARGETS:
            baseline_method = _method_name(backend, target, "baseline")
            candidate_method = _method_name(backend, target, "candidate")
            per_run = []
            allocation_deltas = []
            throughput_deltas = []
            for run_rows in (first_rows, second_rows):
                baseline = run_rows[baseline_method]
                candidate = run_rows[candidate_method]
                per_run.append(
                    cell_verdict(
                        baseline["allocation"],
                        candidate["allocation"],
                        baseline["throughput"],
                        candidate["throughput"],
                        dispatch_kind,
                    )
                )
                allocation_deltas.append(
                    allocation_delta_percent(baseline["allocation"], candidate["allocation"])
                )
                throughput_deltas.append(
                    throughput_delta_percent(baseline["throughput"], candidate["throughput"])
                )
            if dispatch_kind == "inherited-default" or "ineligible" in per_run:
                verdict = "ineligible"
            elif per_run == ["accepted", "accepted"]:
                verdict = "accepted"
            else:
                verdict = "inconclusive"
            results.append(
                {
                    "backend": backend,
                    "target": target,
                    "baseline_method": baseline_method,
                    "candidate_method": candidate_method,
                    "dispatch_kind": dispatch_kind,
                    "canonical_a_allocation_delta_percent": allocation_deltas[0],
                    "canonical_b_allocation_delta_percent": allocation_deltas[1],
                    "canonical_a_throughput_delta_percent": throughput_deltas[0],
                    "canonical_b_throughput_delta_percent": throughput_deltas[1],
                    "verdict": verdict,
                }
            )
    return results


IDENTITY_FIELDS = (
    "benchmark_input_sha",
    "benchmark_input_tree",
    "benchmark_jar",
    "classpath",
    "protocol",
    "fixture",
    "matrix",
    "preflight",
    "preflight_sha256",
    "dispatch",
)


def validate_canonical_identity(first, second):
    for field in IDENTITY_FIELDS:
        if _canonical_json(first.get(field)) != _canonical_json(second.get(field)):
            fail("CANONICAL_IDENTITY_MISMATCH", f"canonical metadata field differs: {field}")
    if first.get("run_id") != "canonical-a" or second.get("run_id") != "canonical-b":
        fail("CANONICAL_IDENTITY_MISMATCH", "run IDs must be canonical-a and canonical-b")


def _read_json(path, reason_code):
    try:
        with Path(path).open(encoding="utf-8") as stream:
            return json.load(stream)
    except (OSError, json.JSONDecodeError) as error:
        fail(reason_code, f"cannot read {path}: {error}")


def validate_run_bundle(run_dir):
    run_dir = Path(run_dir)
    metadata = _read_json(run_dir / "metadata.json", "SOURCE_IDENTITY_MISMATCH")
    dispatch = validate_metadata(metadata)
    _validate_run_artifacts(run_dir, metadata)
    records = _read_json(run_dir / "jmh.json", "MATRIX_EXACT")
    parsed = validate_jmh_records(records)
    return {
        "schema_version": 1,
        "status": "passed",
        "run_id": metadata["run_id"],
        "method_count": len(parsed["rows"]),
        "dispatch": dispatch,
        "rows": parsed["rows"],
        "metadata": metadata,
    }


def _expected_jmh_argv(run_dir, metadata):
    classpath = os.pathsep.join(entry["path"] for entry in metadata["classpath"])
    return [
        "java",
        "-cp",
        classpath,
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
        str((Path(run_dir) / "jmh.json").resolve()),
        exact_include_regex(),
    ]


def _validate_run_artifacts(run_dir, metadata):
    argv_document = _read_json(Path(run_dir) / "argv.json", "PROTOCOL_MISMATCH")
    if not isinstance(argv_document, dict):
        fail("PROTOCOL_MISMATCH", "argv.json must contain an object")
    _require_keys(
        argv_document,
        ("schema_version", "argv", "exit_code"),
        "PROTOCOL_MISMATCH",
        "argv.json",
    )
    if argv_document["schema_version"] != 1 or argv_document["exit_code"] != 0:
        fail("PROTOCOL_MISMATCH", "argv schema/exit mismatch")
    actual_argv = argv_document["argv"]
    expected_argv = _expected_jmh_argv(run_dir, metadata)
    if isinstance(actual_argv, list) and "-rff" in actual_argv:
        actual_argv = list(actual_argv)
        result_index = actual_argv.index("-rff") + 1
        if result_index < len(actual_argv):
            actual_argv[result_index] = str(Path(actual_argv[result_index]).resolve())
    if actual_argv != expected_argv:
        fail("PROTOCOL_MISMATCH", "JMH argv differs from the canonical protocol")

    environment = _read_json(Path(run_dir) / "environment.json", "ENVIRONMENT_MISMATCH")
    if not isinstance(environment, dict):
        fail("ENVIRONMENT_MISMATCH", "environment.json must contain an object")
    required = (
        "schema_version",
        "run_id",
        "benchmark_input_sha",
        "benchmark_input_tree",
        "os",
        "kernel",
        "architecture",
        "cpu_model",
        "logical_cores",
        "java_home",
        "jvm_options",
        "gradle_command",
        "jmh_command",
        "classpath",
        "protocol",
    )
    _require_keys(environment, required, "ENVIRONMENT_MISMATCH", "environment.json")
    if environment["schema_version"] != 1:
        fail("ENVIRONMENT_MISMATCH", "environment schema_version must be 1")
    bindings = {
        "run_id": metadata["run_id"],
        "benchmark_input_sha": metadata["benchmark_input_sha"],
        "benchmark_input_tree": metadata["benchmark_input_tree"],
        "jmh_command": argv_document["argv"],
        "classpath": metadata["classpath"],
        "protocol": metadata["protocol"],
    }
    for field, expected in bindings.items():
        if environment[field] != expected:
            fail("ENVIRONMENT_MISMATCH", f"environment.{field} metadata mismatch")
    for field in ("os", "kernel", "architecture", "cpu_model"):
        if not isinstance(environment[field], str) or not environment[field]:
            fail("ENVIRONMENT_MISMATCH", f"environment.{field} must be non-empty")
    if not isinstance(environment["logical_cores"], int) or environment["logical_cores"] <= 0:
        fail("ENVIRONMENT_MISMATCH", "environment.logical_cores must be positive")
    if not isinstance(environment["gradle_command"], list) or not environment["gradle_command"]:
        fail("ENVIRONMENT_MISMATCH", "environment.gradle_command must be non-empty")


def _git(root, *arguments):
    completed = subprocess.run(
        ["git", *arguments],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        fail("SOURCE_IDENTITY_MISMATCH", f"git {' '.join(arguments)} failed: {completed.stderr.strip()}")
    return completed.stdout.strip()


def _path_allowed(path):
    normalized = path.replace(os.sep, "/")
    return normalized in ALLOWED_POST_MEASUREMENT_EXACT or normalized.startswith(
        ALLOWED_POST_MEASUREMENT_PREFIX
    )


def validate_delivery_commits(repository_root, benchmark_input_sha, final_delivery_sha):
    root = Path(repository_root)
    completed = subprocess.run(
        ["git", "merge-base", "--is-ancestor", benchmark_input_sha, final_delivery_sha],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        fail("BENCHMARK_NOT_ANCESTOR", "benchmark input is not an ancestor of final delivery")
    output = _git(
        root,
        "diff",
        "--name-only",
        "--diff-filter=ACDMRTUXB",
        f"{benchmark_input_sha}..{final_delivery_sha}",
    )
    paths = sorted(path for path in output.splitlines() if path)
    forbidden = [path for path in paths if not _path_allowed(path)]
    if forbidden:
        fail("POST_MEASUREMENT_PATH", f"paths outside allowlist: {forbidden}")
    return {"status": "passed", "changed_paths": paths}


def validate_post_measurement_working_tree(repository_root):
    root = Path(repository_root)
    commands = (
        ("staged", ("diff", "--cached", "--name-only", "--diff-filter=ACDMRTUXB")),
        ("unstaged", ("diff", "--name-only", "--diff-filter=ACDMRTUXB")),
        ("untracked", ("ls-files", "--others", "--exclude-standard")),
    )
    observed = []
    for state, arguments in commands:
        for path in _git(root, *arguments).splitlines():
            if path:
                observed.append({"state": state, "path": path})
    forbidden = [entry for entry in observed if not _path_allowed(entry["path"])]
    if forbidden:
        fail("WORKING_TREE_PATH", f"working-tree paths outside allowlist: {forbidden}")
    return {"status": "passed", "paths": observed}


def _atomic_write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(value, stream, sort_keys=True, indent=2, default=str)
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


def _write_summary(path, validation):
    fields = (
        "method",
        "allocation_b_per_op",
        "allocation_error",
        "throughput_ops_per_ms",
        "throughput_error",
    )
    rows = []
    for method in EXPECTED_METHODS:
        row = validation["rows"][method]
        rows.append(
            {
                "method": method,
                "allocation_b_per_op": str(row["allocation"]),
                "allocation_error": str(row["allocation_error"]),
                "throughput_ops_per_ms": str(row["throughput"]),
                "throughput_error": str(row["throughput_error"]),
            }
        )
    _atomic_write_csv(path, fields, rows)


def _write_comparison(path, rows):
    fields = tuple(rows[0])
    normalized = [
        {key: str(value) for key, value in row.items()}
        for row in rows
    ]
    _atomic_write_csv(path, fields, normalized)


def _atomic_write_csv(path, fields, rows):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent
    )
    try:
        with os.fdopen(descriptor, "w", newline="", encoding="utf-8") as stream:
            writer = csv.DictWriter(stream, fieldnames=fields)
            writer.writeheader()
            writer.writerows(rows)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    except BaseException:
        try:
            os.unlink(temporary)
        except FileNotFoundError:
            pass
        raise


def validate_evidence_root(root, expected_benchmark_input_sha=None):
    root = Path(root)
    first = validate_run_bundle(root / "canonical-a")
    second = validate_run_bundle(root / "canonical-b")
    validate_canonical_identity(first["metadata"], second["metadata"])
    benchmark_input_sha = first["metadata"]["benchmark_input_sha"]
    if (
        expected_benchmark_input_sha is not None
        and benchmark_input_sha != expected_benchmark_input_sha
    ):
        fail(
            "SOURCE_IDENTITY_MISMATCH",
            f"expected benchmark input {expected_benchmark_input_sha}, got {benchmark_input_sha}",
        )

    comparison = compare_runs(first, second, first["dispatch"])
    root.mkdir(parents=True, exist_ok=True)
    _write_summary(root / "canonical-a" / "summary.csv", first)
    _write_summary(root / "canonical-b" / "summary.csv", second)
    _write_comparison(root / "comparison.csv", comparison)
    validation = {
        "schema_version": 1,
        "status": "passed",
        "benchmark_input_sha": benchmark_input_sha,
        "benchmark_input_tree": first["metadata"]["benchmark_input_tree"],
        "verdicts": {
            f"{row['backend']}/{row['target']}": row["verdict"] for row in comparison
        },
    }
    _atomic_write_json(root / "validation.json", validation)
    manifest = {
        "schema_version": 1,
        "status": "passed",
        "benchmark_input_sha": benchmark_input_sha,
        "benchmark_input_tree": first["metadata"]["benchmark_input_tree"],
        "benchmark_jar": first["metadata"]["benchmark_jar"],
        "classpath": first["metadata"]["classpath"],
        "protocol": first["metadata"]["protocol"],
        "preflight_sha256": first["metadata"]["preflight_sha256"],
        "canonical_runs": ["canonical-a", "canonical-b"],
        "comparison_sha256": _sha256_file(root / "comparison.csv"),
        "validation_sha256": _sha256_file(root / "validation.json"),
    }
    _atomic_write_json(root / "delivery-manifest.json", manifest)
    return {
        "schema_version": 1,
        "status": "passed",
        "benchmark_input_sha": benchmark_input_sha,
        "benchmark_input_tree": first["metadata"]["benchmark_input_tree"],
        "method_count_per_run": 16,
        "verdicts": validation["verdicts"],
    }


def _parser():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--repository-root", type=Path, default=REPOSITORY_ROOT)
    identity = parser.add_mutually_exclusive_group(required=True)
    identity.add_argument("--benchmark-input-sha")
    identity.add_argument("--final-delivery-sha")
    parser.add_argument("--post-measurement-working-tree", action="store_true")
    return parser


def main(argv=None):
    arguments = _parser().parse_args(argv)
    try:
        result = validate_evidence_root(arguments.root, arguments.benchmark_input_sha)
        final_delivery_sha = arguments.final_delivery_sha
        if arguments.post_measurement_working_tree and final_delivery_sha is None:
            final_delivery_sha = _git(arguments.repository_root, "rev-parse", "HEAD")
        if final_delivery_sha is not None:
            result["delivery"] = validate_delivery_commits(
                arguments.repository_root,
                result["benchmark_input_sha"],
                final_delivery_sha,
            )
        if arguments.post_measurement_working_tree:
            result["working_tree"] = validate_post_measurement_working_tree(
                arguments.repository_root
            )
        print(_canonical_json(result))
        return 0
    except ValidationError as error:
        print(
            _canonical_json(
                {"status": "failed", "reason_code": error.reason_code, "detail": error.detail}
            ),
            file=sys.stderr,
        )
        return 2


if __name__ == "__main__":
    sys.exit(main())
