#!/usr/bin/env python3
"""Fail-closed validation for issue 757 Protobuf JMH evidence."""

import argparse
import csv
import hashlib
import importlib.util
import json
import math
import os
import stat
import sys
from collections import Counter
from pathlib import Path


CLAIM_THRESHOLD_PERCENT = 5.0
MAX_RUN_LOG_BYTES = 16 * 1024 * 1024
RUN_LOG_TAIL_BYTES = 4096
RUN_LOG_LIMIT_MARKER = b"[runner] output truncated: log size limit exceeded"
RUN_LOG_READ_BYTES = 64 * 1024
EXPECTED_METHODS = {
    "serializerEncodeByteArray",
    "serializerEncodeHeapOptimized",
    "serializerEncodeDirectOptimized",
    "serializerDecodeByteArray",
    "serializerDecodeHeapOptimized",
    "serializerDecodeDirectOptimized",
    "redissonDecodeCopiedByteArray",
    "redissonDecodeContiguousOptimized",
    "redissonDecodeCompositeCompatibility",
    "trustedFallbackEncodeByteArray",
    "trustedFallbackEncodeBufferCompatibility",
    "trustedFallbackDecodeByteArray",
    "trustedFallbackDecodeBufferCompatibility",
}
BASELINES = {
    "serializerEncodeHeapOptimized": "serializerEncodeByteArray",
    "serializerEncodeDirectOptimized": "serializerEncodeByteArray",
    "serializerDecodeHeapOptimized": "serializerDecodeByteArray",
    "serializerDecodeDirectOptimized": "serializerDecodeByteArray",
    "redissonDecodeContiguousOptimized": "redissonDecodeCopiedByteArray",
}
ROLLBACK_DISPATCH_CELLS = {
    "serializer_encode": (
        "serializerEncodeHeapOptimized",
        "serializerEncodeDirectOptimized",
    ),
    "serializer_decode": (
        "serializerDecodeHeapOptimized",
        "serializerDecodeDirectOptimized",
    ),
    "redisson_contiguous": ("redissonDecodeContiguousOptimized",),
}
IDENTITY_FIELDS = (
    "git_commit", "tree_hash", "os", "arch", "cpu", "jvm_vendor", "jvm_version",
    "gradle_version", "jmh_version", "jvm_args", "threads", "forks", "warmups",
    "measurements", "warmup_time", "measurement_time", "profiler", "payload_size",
    "payload_sha256", "config_sha256", "metadata_stdout_sha256", "benchmark_jar_sha256",
    "clean_status", "power_state", "concurrent_heavy_work",
)
CANONICAL_PROFILE = {
    "mode": "thrpt",
    "threads": 1,
    "forks": 2,
    "warmups": 3,
    "measurements": 5,
    "warmup_time": "1 s",
    "measurement_time": "1 s",
    "profiler": "gc",
    "exact_jvm_args": ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"],
}
CONFIG_KEYS = (
    "allowed_class_prefixes", "direct_capacity", "direct_initial_position",
    "heap_capacity", "heap_initial_position", "matrix_version", "methods",
    "payload_identity", "payload_sha256", "redisson_codec_class", "serializer_class",
    "target_headroom", "target_start",
)
OBSERVED_RECORD_FIELDS = {
    "mode": "mode",
    "threads": "threads",
    "forks": "forks",
    "warmups": "warmupIterations",
    "measurements": "measurementIterations",
    "warmup_time": "warmupTime",
    "measurement_time": "measurementTime",
    "jdk_version": "jdkVersion",
    "vm_name": "vmName",
    "vm_version": "vmVersion",
    "jvm_args": "jvmArgs",
}
SUMMARY_FIELDS = (
    "run_id", "method", "throughput_ops_per_second", "allocation_bytes_per_operation",
    "eligible", "eligibility_reason", "observed_config_sha256",
)
COMPARISON_FIELDS = (
    "method", "baseline", "run_a_allocation", "run_a_baseline_allocation",
    "run_a_delta_percent", "run_b_allocation", "run_b_baseline_allocation",
    "run_b_delta_percent", "eligible", "reason", "verdict",
)


def _render(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))


def _fail(path, field, actual, expected, hint):
    raise ValueError(
        "%s: %s: %s != %s; remediation: %s"
        % (path, field, _render(actual), _render(expected), hint)
    )


def _load_json(path):
    path = Path(path)
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        _fail(path, "JSON document", str(error), "readable valid JSON", "regenerate this evidence file")


def _write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n", encoding="utf-8")


def sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def validate_run_log(path):
    path = Path(path)
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    try:
        path_before = path.lstat()
        fd = os.open(str(path), flags)
    except OSError as error:
        _fail(path, "run.log", str(error), "readable non-symlink regular file", "restore the bounded runner log")
    try:
        before = os.fstat(fd)
        identity = (before.st_dev, before.st_ino, before.st_mode, before.st_size)
        path_identity = (path_before.st_dev, path_before.st_ino, path_before.st_mode, path_before.st_size)
        if path_identity != identity:
            _fail(path, "run.log identity", path_identity, identity, "discard the mutable run")
        if not stat.S_ISREG(before.st_mode):
            _fail(path, "run.log type", stat.S_IFMT(before.st_mode), "regular file", "restore the bounded runner log")
        if before.st_size > MAX_RUN_LOG_BYTES:
            _fail(path, "run.log size", before.st_size, "<= %d bytes" % MAX_RUN_LOG_BYTES, "discard the run and repair benchmark logging")
        digest = hashlib.sha256()
        total = 0
        overlap = b""
        tail = b""
        while True:
            chunk = os.read(fd, RUN_LOG_READ_BYTES)
            if not chunk:
                break
            total += len(chunk)
            if total > MAX_RUN_LOG_BYTES:
                _fail(path, "run.log size", total, "<= %d bytes" % MAX_RUN_LOG_BYTES, "discard the run and repair benchmark logging")
            digest.update(chunk)
            scan = overlap + chunk
            if RUN_LOG_LIMIT_MARKER in scan:
                _fail(path, "run.log limit marker", "present", "absent", "discard the truncated run and repair benchmark logging")
            overlap = scan[-(len(RUN_LOG_LIMIT_MARKER) - 1):]
            tail = (tail + chunk)[-RUN_LOG_TAIL_BYTES:]
        after = os.fstat(fd)
        try:
            path_after = path.lstat()
        except OSError as error:
            _fail(path, "run.log identity", str(error), identity, "discard the mutable run")
        after_identity = (after.st_dev, after.st_ino, after.st_mode, after.st_size)
        final_path_identity = (path_after.st_dev, path_after.st_ino, path_after.st_mode, path_after.st_size)
        if after_identity != identity or final_path_identity != identity or total != before.st_size:
            _fail(path, "run.log file identity", (after_identity, final_path_identity, total), identity, "discard the mutable run")
        exact_exit_line = b"exit_code=0\n"
        if tail != exact_exit_line and not tail.endswith(b"\n" + exact_exit_line):
            _fail(path, "run.log tail", tail[-128:].decode("utf-8", "replace"), "exit_code=0\\n", "restore a successful bounded runner log")
    finally:
        os.close(fd)
    return {
        "path": str(path),
        "sha256": digest.hexdigest(),
        "size": total,
        "identity": list(identity),
        "exit_code": 0,
    }


def validate_execution_artifacts(argv_record, environment, log_result, path):
    if not isinstance(argv_record, dict):
        _fail(path, "argv", argv_record, "object", "restore the runner argv record")
    exit_code = argv_record.get("exit_code")
    if isinstance(exit_code, bool) or not isinstance(exit_code, int) or exit_code != 0:
        _fail(path, "exit_code", exit_code, "exact integer 0", "restore successful runner evidence")
    limit_exceeded = argv_record.get("log_limit_exceeded")
    if limit_exceeded is not False:
        _fail(path, "log_limit_exceeded", limit_exceeded, "exact false", "discard bounded or malformed runner evidence")
    if argv_record.get("argv") != environment.get("jmh_argv"):
        _fail(path, "argv", argv_record.get("argv"), environment.get("jmh_argv"), "restore the exact runner command")
    log_exit = log_result.get("exit_code") if isinstance(log_result, dict) else None
    if isinstance(log_exit, bool) or not isinstance(log_exit, int) or log_exit != exit_code:
        _fail(path, "log exit", log_exit, exit_code, "restore matching argv and run.log evidence")
    return True


def _without_hash_fields(value):
    if isinstance(value, dict):
        return {
            key: _without_hash_fields(item)
            for key, item in value.items()
            if key not in ("decision_sha256", "bundle_sha256")
        }
    if isinstance(value, list):
        return [_without_hash_fields(item) for item in value]
    return value


def canonical_sha256(value):
    encoded = json.dumps(
        _without_hash_fields(value), sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def verdict(deltas, eligible):
    if not eligible:
        return "ineligible"
    if all(delta <= -CLAIM_THRESHOLD_PERCENT for delta in deltas):
        return "accepted"
    if all(delta >= CLAIM_THRESHOLD_PERCENT for delta in deltas):
        return "regressed"
    return "inconclusive"


def validate_methods(methods, path="JMH input"):
    if not isinstance(methods, (list, tuple)):
        _fail(path, "methods", methods, "array of benchmark method names", "regenerate the JMH JSON method matrix")
    if any(not isinstance(method, str) for method in methods):
        _fail(path, "methods", methods, "array of benchmark method-name strings", "regenerate the JMH JSON method matrix")
    counts = Counter(methods)
    duplicates = sorted(method for method, count in counts.items() if count != 1)
    if duplicates:
        _fail(path, "duplicate methods", duplicates, [], "rerun JMH with one record per method")
    actual = set(methods)
    missing = sorted(EXPECTED_METHODS - actual)
    unexpected = sorted(actual - EXPECTED_METHODS)
    if missing or unexpected:
        raise ValueError(
            "%s: method matrix actual=%s != expected=%s; missing=%s unexpected=%s; remediation: rebuild the pinned JMH JAR and rerun all methods"
            % (path, sorted(actual), sorted(EXPECTED_METHODS), missing, unexpected)
        )


def validate_score(score, metric, path="JMH input"):
    if isinstance(score, bool) or not isinstance(score, (int, float)):
        _fail(path, metric, score, "finite non-negative number", "rerun JMH and preserve JSON numeric metrics")
    if not math.isfinite(score) or score < 0:
        _fail(path, metric, score, "finite non-negative number", "investigate the profiler failure and rerun JMH")
    return float(score)


def _method_name(record, path):
    benchmark = record.get("benchmark")
    if not isinstance(benchmark, str) or "." not in benchmark:
        _fail(path, "benchmark", benchmark, "fully-qualified benchmark method", "regenerate JMH JSON")
    return benchmark.rsplit(".", 1)[-1]


def _consistent_observed(records, path):
    first = records[0]
    observed = {}
    for output_field, record_field in OBSERVED_RECORD_FIELDS.items():
        if record_field not in first:
            _fail(path, record_field, None, "present in every JMH record", "rerun with JSON output from the pinned JMH JAR")
        expected = first[record_field]
        for index, record in enumerate(records[1:], start=1):
            actual = record.get(record_field)
            if actual != expected:
                _fail("%s record[%d]" % (path, index), record_field, actual, expected, "rerun the complete matrix under one profile")
        observed[output_field] = expected
    observed["profiler"] = "gc"
    return observed


def _parse_params(records, path):
    aliases = {
        "matrix_version": "matrixVersion",
        "target_headroom": "targetHeadroom",
        "target_start": "targetStart",
    }
    result = {}
    for output_field, param_name in aliases.items():
        values = []
        for index, record in enumerate(records):
            params = record.get("params")
            if not isinstance(params, dict) or param_name not in params:
                _fail("%s record[%d]" % (path, index), "params.%s" % param_name, None, "present", "rerun the exact benchmark matrix")
            values.append(params[param_name])
        if any(value != values[0] for value in values[1:]):
            _fail(path, "params.%s" % param_name, values, [values[0]] * len(values), "rerun the complete matrix with one parameter set")
        value = values[0]
        if output_field != "matrix_version":
            try:
                value = int(value)
            except (TypeError, ValueError):
                _fail(path, "params.%s" % param_name, value, "integer", "rerun with the canonical benchmark parameters")
        result[output_field] = value
    return result


def parse_jmh_records(records, path):
    if not isinstance(records, list) or not records:
        _fail(path, "root", records, "non-empty JMH JSON array", "rerun JMH with -rf json")
    if any(not isinstance(record, dict) for record in records):
        _fail(path, "records", records, "JSON objects", "regenerate JMH JSON")
    methods = [_method_name(record, path) for record in records]
    validate_methods(methods, path)
    observed = _consistent_observed(records, path)
    if observed["mode"] != "thrpt":
        _fail(path, "mode", observed["mode"], "thrpt", "run the throughput benchmark profile")
    if observed["jvm_args"] != CANONICAL_PROFILE["exact_jvm_args"]:
        _fail(path, "jvmArgs", observed["jvm_args"], CANONICAL_PROFILE["exact_jvm_args"], "use the exact ordered -jvmArgsAppend list")
    params = _parse_params(records, path)
    observed.update(params)
    rows = {}
    for record, method in zip(records, methods):
        primary = record.get("primaryMetric")
        if not isinstance(primary, dict):
            _fail(path, "%s.primaryMetric" % method, primary, "object", "rerun JMH with primary throughput metrics")
        if primary.get("scoreUnit") != "ops/s":
            _fail(path, "%s.primaryMetric.scoreUnit" % method, primary.get("scoreUnit"), "ops/s", "use throughput mode with seconds output")
        secondary = record.get("secondaryMetrics")
        allocation = secondary.get("gc.alloc.rate.norm") if isinstance(secondary, dict) else None
        if not isinstance(allocation, dict):
            _fail(path, "%s.gc.alloc.rate.norm" % method, allocation, "metric object", "rerun with -prof gc")
        if allocation.get("scoreUnit") != "B/op":
            _fail(path, "%s.gc.alloc.rate.norm.scoreUnit" % method, allocation.get("scoreUnit"), "B/op", "rerun with the JMH GC profiler")
        rows[method] = {
            "throughput": validate_score(primary.get("score"), "%s primary score" % method, path),
            "allocation": validate_score(allocation.get("score"), "%s gc.alloc.rate.norm" % method, path),
            "eligible": method in BASELINES,
            "reason": "candidate" if method in BASELINES else ("baseline" if method in BASELINES.values() else "compatibility_control"),
        }
    observed_sha = canonical_sha256(observed)
    return {
        "rows": rows,
        "observed_config": observed,
        "observed_config_sha256": observed_sha,
        "params": params,
    }


def canonical_config_json(config):
    if not isinstance(config, dict):
        _fail("config", "config_json", config, "object", "regenerate benchmark metadata")
    missing = [key for key in CONFIG_KEYS if key not in config]
    if missing:
        _fail("config", "missing keys", missing, [], "rebuild the benchmark metadata entrypoint")
    canonical = {}
    for key in CONFIG_KEYS:
        value = config[key]
        if isinstance(value, list):
            value = sorted(value)
        canonical[key] = value
    return json.dumps(canonical, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def config_sha256(config):
    return hashlib.sha256(canonical_config_json(config).encode("utf-8")).hexdigest()


def _duration(value):
    if isinstance(value, str) and value.endswith("s") and " " not in value:
        return value[:-1] + " s"
    return value


def _argv_observations(environment, path):
    argv = environment.get("jmh_argv")
    if not isinstance(argv, list):
        return {}
    switches = {
        "-t": ("threads", int), "-f": ("forks", int), "-wi": ("warmups", int),
        "-i": ("measurements", int), "-w": ("warmup_time", _duration),
        "-r": ("measurement_time", _duration), "-prof": ("profiler", str),
        "-jvmArgsAppend": ("jvm_args", lambda value: value.split()),
    }
    result = {"mode": "thrpt"}
    for switch, (field, converter) in switches.items():
        positions = [index for index, token in enumerate(argv) if token == switch]
        if len(positions) != 1 or positions[0] + 1 >= len(argv):
            _fail(path, "jmh_argv %s" % switch, positions, "exactly one value", "regenerate environment.json from the exact recorded argv")
        raw = argv[positions[0] + 1]
        try:
            result[field] = converter(raw)
        except (TypeError, ValueError):
            _fail(path, "jmh_argv %s" % switch, raw, "valid %s" % field, "record the exact JMH invocation")
    return result


def validate_manifest_observations(environment, parsed, path):
    if not isinstance(environment, dict):
        _fail(path, "environment", environment, "JSON object", "regenerate environment.json from the evidence runner")
    if not isinstance(parsed, dict):
        _fail(path, "parsed JMH evidence", parsed, "validated JMH object", "parse the JMH JSON before manifest comparison")
    observed = parsed["observed_config"]
    argv_observed = _argv_observations(environment, path)
    aliases = {
        "threads": "threads", "forks": "forks", "warmups": "warmups",
        "measurements": "measurements", "warmup_time": "warmup_time",
        "measurement_time": "measurement_time", "jvm_args": "jvm_args",
        "jdk_version": "jdk_version", "vm_name": "vm_name", "vm_version": "vm_version",
        "matrix_version": "matrix_version", "target_headroom": "target_headroom",
        "target_start": "target_start",
    }
    nested = environment.get("observed_config", {})
    for env_field, observed_field in aliases.items():
        actual = environment.get(env_field, nested.get(env_field))
        if actual is None:
            actual = argv_observed.get(env_field)
        if actual is None and env_field in ("matrix_version", "target_headroom", "target_start"):
            metadata = environment.get("metadata", {})
            actual = metadata.get(env_field) if isinstance(metadata, dict) else None
        if actual is None and env_field in ("jdk_version", "vm_name", "vm_version"):
            # These fields are still fingerprinted from every JMH record. A runner may
            # additionally provide normalized values for independent manifest binding.
            continue
        expected = observed.get(observed_field)
        if actual != expected:
            _fail(path, env_field, actual, expected, "regenerate environment.json from the observed JMH invocation")
    declared_sha = environment.get("observed_config_sha256")
    if declared_sha is not None and declared_sha != parsed["observed_config_sha256"]:
        _fail(path, "observed_config_sha256", declared_sha, parsed["observed_config_sha256"], "regenerate environment.json from JMH records")

    config = environment.get("config_json")
    if config is None and isinstance(environment.get("metadata"), dict):
        config = environment["metadata"].get("config_json")
    if isinstance(config, str):
        try:
            config = json.loads(config)
        except json.JSONDecodeError as error:
            _fail(path, "config_json", str(error), "valid canonical JSON", "rebuild benchmark metadata")
    if config is not None:
        for field in ("direct_capacity", "direct_initial_position", "heap_capacity", "heap_initial_position", "target_headroom", "target_start"):
            value = config.get(field)
            if isinstance(value, bool) or not isinstance(value, int):
                _fail(path, "config_json.%s" % field, value, "JSON integer", "rebuild canonical benchmark metadata")
        if config.get("methods") != sorted(EXPECTED_METHODS):
            _fail(path, "config_json.methods", config.get("methods"), sorted(EXPECTED_METHODS), "rebuild the exact 13-method benchmark matrix")
        prefixes = config.get("allowed_class_prefixes")
        if not isinstance(prefixes, list) or any(not isinstance(value, str) for value in prefixes):
            _fail(path, "config_json.allowed_class_prefixes", prefixes, "string array", "rebuild canonical benchmark metadata")
        computed = config_sha256(config)
        declared = environment.get("config_sha256")
        if declared is None and isinstance(environment.get("metadata"), dict):
            declared = environment["metadata"].get("config_sha256")
        if declared != computed:
            _fail(path, "config_sha256", declared, computed, "rebuild the pinned JMH JAR and recapture metadata")
        for field in ("matrix_version", "target_headroom", "target_start"):
            if config[field] != parsed["params"][field]:
                _fail(path, "config_json.%s" % field, config[field], parsed["params"][field], "rerun the pinned benchmark matrix")
        metadata = environment.get("metadata", {})
        for field in ("matrix_version", "target_headroom", "target_start", "payload_sha256"):
            declared_value = environment.get(field)
            if declared_value is None and isinstance(metadata, dict):
                declared_value = metadata.get(field)
            expected_value = config.get(field)
            if declared_value != expected_value:
                _fail(path, field, declared_value, expected_value, "recapture metadata from the pinned benchmark JAR")


def _identity_value(environment, field, path):
    if field in environment:
        value = environment[field]
        if field == "power_state" and isinstance(value, dict):
            return value.get("normalized")
        return value
    argv = _argv_observations(environment, path)
    if field in argv:
        return argv[field]
    metadata = environment.get("metadata", {})
    if field in ("payload_size", "payload_sha256", "config_sha256") and isinstance(metadata, dict):
        return metadata.get(field)
    if field in ("jvm_vendor", "jvm_version"):
        return (environment.get("java_version_stdout", "") + environment.get("java_version_stderr", "")).strip() or None
    if field == "gradle_version":
        return (environment.get("gradle_version_stdout", "") + environment.get("gradle_version_stderr", "")).strip() or None
    if field == "clean_status":
        return "clean" if is_clean_environment(environment) else "dirty"
    return None


def validate_identity(first, second, first_path="run-a/environment.json", second_path="run-b/environment.json"):
    if not isinstance(first, dict):
        _fail(first_path, "environment", first, "JSON object", "regenerate the first environment manifest")
    if not isinstance(second, dict):
        _fail(second_path, "environment", second, "JSON object", "regenerate the second environment manifest")
    for field in IDENTITY_FIELDS:
        left = _identity_value(first, field, first_path)
        right = _identity_value(second, field, second_path)
        if left is None or right is None:
            _fail("%s <> %s" % (first_path, second_path), field, {"run_a": left, "run_b": right}, "present in both environments", "recapture the complete normalized environment identity")
        if left != right:
            raise ValueError(
                "%s <> %s: %s: %s != %s; remediation: run both canonical measurements from the same clean head and environment"
                % (first_path, second_path, field, left, right)
            )


def validate_environment_identity(environment, path):
    for field in IDENTITY_FIELDS:
        value = _identity_value(environment, field, path)
        if value is None:
            _fail(path, field, value, "recorded environment identity", "recapture environment.json from the runner before JMH launch")


def _validate_complete_pairs(rows, path):
    for candidate, baseline in BASELINES.items():
        if candidate not in rows or baseline not in rows:
            _fail(path, "baseline pair %s" % candidate, sorted(set((candidate, baseline)) & set(rows)), [baseline, candidate], "regenerate the complete summary")


def _delta(candidate, baseline, path, method):
    if baseline == 0:
        _fail(path, "%s baseline allocation" % method, baseline, "greater than zero", "rerun JMH; a zero allocation baseline cannot produce a percentage")
    return ((candidate - baseline) / baseline) * 100.0


def compare_runs(first, second, ineligible_cells=None, path="comparison"):
    if not isinstance(first, dict):
        _fail(path, "run_a", first, "validated run summary object", "regenerate the first summary.csv")
    if not isinstance(second, dict):
        _fail(path, "run_b", second, "validated run summary object", "regenerate the second summary.csv")
    if first.get("run_id") == second.get("run_id"):
        _fail(path, "duplicate run_id", first.get("run_id"), "two distinct run IDs", "run the canonical profile a second time into a new directory")
    first_sha = first.get("observed_config_sha256")
    second_sha = second.get("observed_config_sha256")
    if first_sha != second_sha:
        _fail(path, "observed_config_sha256", first_sha, second_sha, "rerun both measurements with the same canonical JMH profile")
    for run in (first, second):
        observed = run.get("observed_config")
        if observed is not None:
            for field in ("mode", "threads", "forks", "warmups", "measurements", "warmup_time", "measurement_time", "profiler"):
                if observed.get(field) != CANONICAL_PROFILE[field]:
                    _fail(path, field, observed.get(field), CANONICAL_PROFILE[field], "run the canonical evidence profile")
            if observed.get("jvm_args") != CANONICAL_PROFILE["exact_jvm_args"]:
                _fail(path, "jvm_args", observed.get("jvm_args"), CANONICAL_PROFILE["exact_jvm_args"], "use the exact ordered fork JVM arguments")
    first_rows = first.get("rows", {})
    second_rows = second.get("rows", {})
    if set(first_rows) != set(second_rows):
        _fail(path, "summary methods", sorted(first_rows), sorted(second_rows), "regenerate both complete summaries")
    if set(first_rows) == EXPECTED_METHODS:
        _validate_complete_pairs(first_rows, path)
    ineligible_cells = set(ineligible_cells or ())
    unrelated = ineligible_cells - set(BASELINES)
    if unrelated:
        _fail(path, "ineligible cells", sorted(unrelated), sorted(set(BASELINES)), "remove unrelated rollback decisions")
    result = {}
    for candidate, baseline in BASELINES.items():
        if candidate not in first_rows and candidate not in second_rows:
            continue
        if baseline not in first_rows or baseline not in second_rows:
            _fail(path, "baseline pair %s" % candidate, None, baseline, "regenerate both summaries")
        values = []
        for rows in (first_rows, second_rows):
            values.append(_delta(rows[candidate]["allocation"], rows[baseline]["allocation"], path, candidate))
        eligible = candidate not in ineligible_cells
        reason = "candidate" if eligible else "removed_after_regression"
        result[candidate] = {
            "method": candidate, "baseline": baseline,
            "run_a_allocation": first_rows[candidate]["allocation"],
            "run_a_baseline_allocation": first_rows[baseline]["allocation"],
            "run_a_delta_percent": values[0],
            "run_b_allocation": second_rows[candidate]["allocation"],
            "run_b_baseline_allocation": second_rows[baseline]["allocation"],
            "run_b_delta_percent": values[1],
            "eligible": eligible, "reason": reason,
            "verdict": verdict(values, eligible),
        }
    for method in sorted(set(first_rows) - set(BASELINES)):
        reason = "baseline" if method in BASELINES.values() else "compatibility_control"
        result[method] = {
            "method": method, "baseline": "",
            "run_a_allocation": first_rows[method]["allocation"],
            "run_a_baseline_allocation": "", "run_a_delta_percent": "",
            "run_b_allocation": second_rows[method]["allocation"],
            "run_b_baseline_allocation": "", "run_b_delta_percent": "",
            "eligible": False, "reason": reason, "verdict": "ineligible",
        }
    return result


def _jar_identity(path):
    stat = path.stat()
    return (stat.st_dev, stat.st_ino, stat.st_size, stat.st_mtime_ns)


def _validated_jar(jar, environment, environment_path):
    jar = Path(jar)
    try:
        resolved = jar.resolve(strict=True)
    except OSError as error:
        _fail(jar, "jar path", str(error), "existing absolute regular file", "build and resolve exactly one JMH JAR")
    if not resolved.is_file():
        _fail(jar, "jar type", "not a regular file", "regular file", "rebuild the JMH JAR")
    if jar != resolved and not jar.is_absolute():
        _fail(jar, "jar path", str(jar), str(resolved), "pass --jar as an absolute canonical path")
    declared_path = environment.get("benchmark_jar_path")
    if declared_path != str(resolved):
        _fail(environment_path, "benchmark_jar_path", declared_path, str(resolved), "regenerate environment.json from the pinned JAR state")
    digest = sha256_file(resolved)
    if environment.get("benchmark_jar_sha256") != digest:
        _fail(environment_path, "benchmark_jar_sha256", environment.get("benchmark_jar_sha256"), digest, "re-resolve the JMH JAR and rerun evidence")
    return resolved, digest, _jar_identity(resolved)


def _is_clean(value):
    return value is True or value in ("", "clean")


def is_clean_environment(environment):
    if not isinstance(environment, dict):
        return False
    if "clean_status" in environment and not _is_clean(environment.get("clean_status")):
        return False
    for field in ("initial_clean_status", "prelaunch_clean_status"):
        if field in environment:
            gate = environment.get(field)
            if isinstance(gate, dict):
                clean = gate.get("stdout") == ""
            else:
                clean = _is_clean(gate)
            if not clean:
                return False
    for field in ("initial_clean_observation", "prelaunch_clean_observation"):
        if field in environment:
            observation = environment.get(field)
            if not isinstance(observation, dict) or observation.get("stdout") != "":
                return False
    return "clean_status" in environment or all(
        field in environment for field in ("initial_clean_status", "prelaunch_clean_status")
    )


def write_summary(path, run_id, parsed):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=SUMMARY_FIELDS)
        writer.writeheader()
        for method in sorted(parsed["rows"]):
            row = parsed["rows"][method]
            writer.writerow({
                "run_id": run_id, "method": method,
                "throughput_ops_per_second": format(row["throughput"], ".17g"),
                "allocation_bytes_per_operation": format(row["allocation"], ".17g"),
                "eligible": "true" if row["eligible"] else "false",
                "eligibility_reason": row["reason"],
                "observed_config_sha256": parsed["observed_config_sha256"],
            })


def read_summary(path):
    path = Path(path)
    try:
        with path.open(encoding="utf-8", newline="") as stream:
            records = list(csv.DictReader(stream))
    except OSError as error:
        _fail(path, "summary.csv", str(error), "readable validated CSV", "rerun per-run validation")
    if not records:
        _fail(path, "rows", 0, len(EXPECTED_METHODS), "rerun per-run validation")
    run_ids = {record.get("run_id") for record in records}
    observed_shas = {record.get("observed_config_sha256") for record in records}
    if len(run_ids) != 1 or len(observed_shas) != 1:
        _fail(path, "summary identity", {
            "run_ids": sorted(run_ids, key=_render),
            "observed": sorted(observed_shas, key=_render),
        }, "one consistent identity", "regenerate summary.csv")
    rows = {}
    for record in records:
        method = record.get("method")
        if method in rows:
            _fail(path, "duplicate method", method, "unique methods", "regenerate summary.csv")
        rows[method] = {
            "throughput": validate_score(_csv_number(record.get("throughput_ops_per_second")), "%s throughput" % method, path),
            "allocation": validate_score(_csv_number(record.get("allocation_bytes_per_operation")), "%s allocation" % method, path),
            "eligible": record.get("eligible") == "true",
            "reason": record.get("eligibility_reason"),
        }
    validate_methods(list(rows), str(path))
    for method, row in rows.items():
        expected_eligible = method in BASELINES
        expected_reason = "candidate" if expected_eligible else ("baseline" if method in BASELINES.values() else "compatibility_control")
        if row["eligible"] != expected_eligible:
            _fail(path, "%s.eligible" % method, row["eligible"], expected_eligible, "regenerate summary.csv; rollback eligibility is applied only during compare")
        if row["reason"] != expected_reason:
            _fail(path, "%s.eligibility_reason" % method, row["reason"], expected_reason, "regenerate summary.csv from the authoritative matrix")
    return {"run_id": next(iter(run_ids)), "observed_config_sha256": next(iter(observed_shas)), "rows": rows}


def _csv_number(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return value


def validate_run(jar, input_path, environment_path, summary_path, validation_path):
    environment_path = Path(environment_path)
    environment = _load_json(environment_path)
    if not isinstance(environment, dict):
        _fail(environment_path, "root", environment, "object", "regenerate environment.json")
    log_result = validate_run_log(environment_path.parent / "run.log")
    validate_execution_artifacts(
        _load_json(environment_path.parent / "argv.json"), environment, log_result, environment_path.parent,
    )
    resolved_jar, first_hash, first_identity = _validated_jar(Path(jar), environment, environment_path)
    if not is_clean_environment(environment):
        _fail(environment_path, "clean_status", {
            "clean_status": environment.get("clean_status"),
            "initial_clean_status": environment.get("initial_clean_status"),
            "prelaunch_clean_status": environment.get("prelaunch_clean_status"),
        }, "clean at initial and pre-launch gates", "commit or remove every tracked and untracked change, then rerun")
    records = _load_json(input_path)
    parsed = parse_jmh_records(records, str(input_path))
    validate_manifest_observations(environment, parsed, str(environment_path))
    validate_environment_identity(environment, str(environment_path))
    run_id = environment.get("run_id")
    if not isinstance(run_id, str) or not run_id:
        _fail(environment_path, "run_id", run_id, "non-empty unique string", "rerun into a newly generated run directory")
    write_summary(summary_path, run_id, parsed)
    second_hash = sha256_file(resolved_jar)
    second_identity = _jar_identity(resolved_jar)
    if second_hash != first_hash or second_identity != first_identity:
        _fail(resolved_jar, "JAR double-hash/file identity", {"hash": second_hash, "identity": second_identity}, {"hash": first_hash, "identity": first_identity}, "discard the run and rebuild the pinned JMH JAR")
    result = {
        "schema_version": 1, "status": "passed", "mode": "run", "run_id": run_id,
        "input_path": str(Path(input_path).resolve()),
        "environment_path": str(environment_path.resolve()),
        "summary_path": str(Path(summary_path).resolve()),
        "benchmark_jar_path": str(resolved_jar),
        "benchmark_jar_sha256": second_hash,
        "config_sha256": environment.get("config_sha256", environment.get("metadata", {}).get("config_sha256") if isinstance(environment.get("metadata"), dict) else None),
        "observed_config": parsed["observed_config"],
        "observed_config_sha256": parsed["observed_config_sha256"],
        "method_count": len(parsed["rows"]),
    }
    _write_json(validation_path, result)
    return result


def _read_comparison_verdicts(path):
    try:
        with Path(path).open(encoding="utf-8", newline="") as stream:
            return {row.get("method"): row.get("verdict") for row in csv.DictReader(stream)}
    except OSError as error:
        _fail(path, "archived comparison", str(error), "readable comparison.csv", "restore the immutable rollback archive")


def validate_rollback_bundle(path):
    path = Path(path).resolve()
    bundle = _load_json(path)
    if bundle.get("schema_version") != 2 or bundle.get("kind") != "rollback_bundle":
        remediation = "v1 rollback artifacts must be recreated with record-rollback then finalize-rollback" if bundle.get("schema_version") == 1 else "use a finalized v2 rollback bundle, not a preparation"
        _fail(path, "finalized v2 rollback", {"schema_version": bundle.get("schema_version"), "kind": bundle.get("kind")}, {"schema_version": 2, "kind": "rollback_bundle"}, remediation)
    runner_path = Path(__file__).resolve().with_name("run-evidence.py")
    spec = importlib.util.spec_from_file_location("issue757_rollback_runner", runner_path)
    rollback_runner = importlib.util.module_from_spec(spec); spec.loader.exec_module(rollback_runner)
    chain = rollback_runner.authenticate_rollback_bundle_chain(path)
    authenticated = chain[-1][1]
    ineligible = {cell for decision in authenticated["decisions"] for cell in decision["removed_cells"]}
    return {
        "path": str(path), "sha256": rollback_runner.sha256_file(path), "decisions": authenticated["decisions"],
        "ineligible_cells": ineligible, "bundle": authenticated,
        "chain_paths": [str(bundle_path) for bundle_path, _ in chain],
    }
    if not isinstance(bundle, dict) or not isinstance(bundle.get("decisions"), list) or not bundle["decisions"]:
        _fail(path, "decisions", bundle.get("decisions") if isinstance(bundle, dict) else None, "non-empty ordered array", "record rollback from a regressed comparison")
    if bundle.get("schema_version") != 1:
        _fail(path, "schema_version", bundle.get("schema_version"), 1, "recreate the rollback bundle with this validator contract")
    declared_bundle_hash = bundle.get("bundle_sha256")
    if declared_bundle_hash is not None:
        computed_bundle_hash = canonical_sha256(bundle)
        if declared_bundle_hash != computed_bundle_hash:
            _fail(path, "bundle_sha256", declared_bundle_hash, computed_bundle_hash, "restore the immutable authenticated bundle")
        expected_name = "rollback-bundle-g%s-%s.json" % (bundle.get("generation"), declared_bundle_hash)
        if path.name != expected_name:
            _fail(path, "bundle filename", path.name, expected_name, "restore the canonical immutable bundle filename")
    seen_dispatches = set()
    ineligible = set()
    previous_post_commit = None
    previous_generation = 0
    seen_generations = set()
    generation_dispatches = {}
    generation_verdicts = {}
    generation_identity = {}
    dispatch_order = {name: index for index, name in enumerate(ROLLBACK_DISPATCH_CELLS)}
    previous_dispatch_order = -1
    for index, decision in enumerate(bundle["decisions"], start=1):
        if not isinstance(decision, dict):
            _fail(path, "decision[%d]" % index, decision, "object", "restore the rollback bundle")
        generation = decision.get("generation")
        if isinstance(generation, bool) or not isinstance(generation, int) or generation < 1:
            _fail(path, "decision[%d].generation" % index, generation, "positive integer", "restore every ordered bundle generation")
        if generation < previous_generation or generation > previous_generation + 1:
            _fail(path, "decision[%d].generation" % index, generation, previous_generation + 1, "restore contiguous ordered bundle generations")
        if generation != previous_generation:
            previous_dispatch_order = -1
        seen_generations.add(generation)
        if bundle.get("generation") is not None:
            for identity_field in ("old_commit", "old_tree", "post_rollback_commit", "timestamp"):
                identity_value = decision.get(identity_field)
                if not isinstance(identity_value, str) or not identity_value:
                    _fail(path, "decision[%d].%s" % (index, identity_field), identity_value, "non-empty lineage value", "restore the complete rollback decision lineage")
            lineage_parent = decision.get("lineage_parent_commit")
            if generation == 1 and lineage_parent is not None:
                _fail(path, "decision[%d].lineage_parent_commit" % index, lineage_parent, None, "restore the root rollback generation")
            identity = (decision["old_commit"], decision["old_tree"], decision["post_rollback_commit"], lineage_parent)
            existing_identity = generation_identity.setdefault(generation, identity)
            if identity != existing_identity:
                _fail(path, "generation %d source lineage" % generation, identity, existing_identity, "bind simultaneous rollback decisions to one source lineage")
        dispatch = decision.get("dispatch", decision.get("dispatch_unit"))
        if not isinstance(dispatch, str) or dispatch not in ROLLBACK_DISPATCH_CELLS:
            _fail(path, "dispatch", dispatch, sorted(ROLLBACK_DISPATCH_CELLS), "record only a mapped optimized dispatch")
        if dispatch in seen_dispatches:
            _fail(path, "duplicate dispatch", dispatch, "one decision per dispatch", "remove the conflicting decision and recreate a new immutable bundle")
        seen_dispatches.add(dispatch)
        if dispatch_order[dispatch] <= previous_dispatch_order:
            _fail(path, "decision order", dispatch, list(ROLLBACK_DISPATCH_CELLS), "sort simultaneous decisions by fixed dispatch order")
        previous_dispatch_order = dispatch_order[dispatch]
        generation_dispatches.setdefault(generation, set()).add(dispatch)
        expected_cells = sorted(ROLLBACK_DISPATCH_CELLS[dispatch])
        cells = decision.get("cells", decision.get("regressed_cells"))
        if cells != expected_cells:
            _fail(path, "decision[%d].cells" % index, cells, expected_cells, "record the complete mapped simultaneous regression set in fixed order")
        declared_decision_hash = decision.get("decision_sha256")
        computed_decision_hash = canonical_sha256(decision)
        if declared_decision_hash != computed_decision_hash:
            _fail(path, "decision[%d].decision_sha256" % index, declared_decision_hash, computed_decision_hash, "restore the immutable decision payload")
        comparison_name = decision.get("comparison_path")
        if comparison_name is not None:
            if not isinstance(comparison_name, str) or Path(comparison_name).is_absolute():
                _fail(path, "decision[%d].comparison_path" % index, comparison_name, "archive-relative path", "recreate the rollback archive with relative paths")
            comparison_path = (path.parent / comparison_name).resolve()
            expected_comparison_hash = decision.get("comparison_sha256")
        else:
            archive_root = decision.get("archive_root")
            if not isinstance(archive_root, str) or Path(archive_root).is_absolute() or ".." in Path(archive_root).parts:
                _fail(path, "decision[%d].archive_root" % index, archive_root, "bundle-relative directory", "restore the canonical rollback archive")
            artifacts = decision.get("artifacts")
            if not isinstance(artifacts, list) or not artifacts:
                _fail(path, "decision[%d].artifacts" % index, artifacts, "non-empty array", "restore every archived evidence artifact")
            comparison_path = None
            expected_comparison_hash = None
            for artifact_index, artifact in enumerate(artifacts):
                if not isinstance(artifact, dict):
                    _fail(path, "decision[%d].artifacts[%d]" % (index, artifact_index), artifact, "path/hash object", "restore the canonical archive manifest")
                relative = artifact.get("path")
                if not isinstance(relative, str) or Path(relative).is_absolute() or ".." in Path(relative).parts:
                    _fail(path, "decision[%d].artifacts[%d].path" % (index, artifact_index), relative, "archive-relative path", "restore the canonical archive manifest")
                artifact_path = (path.parent / archive_root / relative).resolve()
                expected_hash = artifact.get("sha256")
                actual_hash = sha256_file(artifact_path) if artifact_path.is_file() else None
                if actual_hash != expected_hash:
                    _fail(path, "decision[%d].artifacts[%d].sha256" % (index, artifact_index), actual_hash, expected_hash, "restore the immutable archived bytes")
                if Path(relative).name == "comparison.csv":
                    if comparison_path is not None:
                        _fail(path, "decision[%d].comparison artifacts" % index, relative, "exactly one comparison.csv", "restore the unambiguous rollback archive")
                    comparison_path = artifact_path
                    expected_comparison_hash = expected_hash
            if comparison_path is None:
                _fail(path, "decision[%d].comparison artifact" % index, None, "comparison.csv", "archive the state-bound regressed comparison")
        try:
            comparison_path.relative_to(path.parent)
        except ValueError:
            _fail(path, "decision[%d].comparison_path" % index, str(comparison_path), "path inside bundle archive", "restore the immutable archive layout")
        actual_hash = sha256_file(comparison_path) if comparison_path.is_file() else None
        if actual_hash != expected_comparison_hash:
            _fail(path, "decision[%d].comparison_sha256" % index, actual_hash, expected_comparison_hash, "restore the archived comparison bytes")
        verdicts = _read_comparison_verdicts(comparison_path)
        existing_verdicts = generation_verdicts.setdefault(generation, verdicts)
        if verdicts != existing_verdicts:
            _fail(path, "generation %d comparison lineage" % generation, verdicts, existing_verdicts, "bind simultaneous decisions to one archived comparison")
        for cell in expected_cells:
            if verdicts.get(cell) != "regressed":
                _fail(comparison_path, cell, verdicts.get(cell), "regressed", "do not remove a retained or non-regressed candidate")
        if previous_post_commit is not None and generation != previous_generation:
            lineage_parent = decision.get("lineage_parent_commit")
            if lineage_parent != previous_post_commit:
                _fail(path, "decision[%d].lineage_parent_commit" % index, lineage_parent, previous_post_commit, "record a complete descending rollback lineage")
        previous_post_commit = decision.get("post_rollback_commit")
        previous_generation = generation
        ineligible.update(expected_cells)
    if seen_generations != set(range(1, max(seen_generations) + 1)):
        _fail(path, "bundle generations", sorted(seen_generations), list(range(1, max(seen_generations) + 1)), "restore every immutable generation")
    if bundle.get("generation") is not None and bundle.get("generation") != max(seen_generations):
        _fail(path, "bundle generation", bundle.get("generation"), max(seen_generations), "restore the complete latest generation")
    predecessor = bundle.get("predecessor_bundle_sha256")
    if max(seen_generations) == 1 and predecessor is not None:
        _fail(path, "predecessor_bundle_sha256", predecessor, None, "restore the first-generation lineage root")
    if max(seen_generations) > 1 and (not isinstance(predecessor, str) or len(predecessor) != 64 or any(char not in "0123456789abcdef" for char in predecessor)):
        _fail(path, "predecessor_bundle_sha256", predecessor, "64 lowercase hexadecimal characters", "restore the authenticated predecessor lineage")
    for generation, verdicts in generation_verdicts.items():
        regressed_dispatches = {
            dispatch for dispatch, cells in ROLLBACK_DISPATCH_CELLS.items()
            if all(verdicts.get(cell) == "regressed" for cell in cells)
        }
        if generation_dispatches[generation] != regressed_dispatches:
            _fail(path, "generation %d dispatches" % generation, sorted(generation_dispatches[generation]), sorted(regressed_dispatches), "record every simultaneously regressed dispatch or record none")
    chain_paths = [str(path)]
    if max(seen_generations) > 1:
        candidates = []
        for candidate in path.parent.glob("rollback-bundle-g%d-*.json" % (max(seen_generations) - 1)):
            if candidate.is_file() and sha256_file(candidate) == predecessor:
                candidates.append(candidate.resolve())
        if len(candidates) != 1:
            _fail(path, "predecessor bundle matches", [str(candidate) for candidate in candidates], "exactly one generation-%d file with sha256 %s" % (max(seen_generations) - 1, predecessor), "restore the complete immutable rollback bundle chain")
        previous_result = validate_rollback_bundle(candidates[0])
        previous_decisions = previous_result["bundle"]["decisions"]
        inherited = bundle["decisions"][:len(previous_decisions)]
        if inherited != previous_decisions:
            _fail(path, "inherited decision prefix", inherited, previous_decisions, "restore the exact authenticated predecessor decisions")
        chain_paths = previous_result["chain_paths"] + [str(path)]
    return {
        "path": str(path), "sha256": sha256_file(path), "decisions": bundle["decisions"],
        "ineligible_cells": ineligible, "bundle": bundle, "chain_paths": chain_paths,
    }


def write_comparison(path, comparison):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=COMPARISON_FIELDS)
        writer.writeheader()
        for method in sorted(comparison):
            row = dict(comparison[method])
            row["eligible"] = "true" if row["eligible"] else "false"
            writer.writerow(row)


def validate_compare(run_paths, environment_paths, output_path, validation_path, rollback_bundle=None):
    if len(run_paths) != 2 or len(environment_paths) != 2:
        _fail("compare CLI", "input count", {"runs": len(run_paths), "environments": len(environment_paths)}, {"runs": 2, "environments": 2}, "pass exactly two canonical runs and environments")
    runs = [read_summary(path) for path in run_paths]
    environments = [_load_json(path) for path in environment_paths]
    validate_identity(environments[0], environments[1], str(environment_paths[0]), str(environment_paths[1]))
    for run, environment, path in zip(runs, environments, environment_paths):
        observed = environment.get("observed_config")
        if observed is not None:
            observed_sha = canonical_sha256(observed)
            if run["observed_config_sha256"] != observed_sha:
                _fail(path, "observed_config_sha256", run["observed_config_sha256"], observed_sha, "regenerate the summary from this environment")
            run["observed_config"] = observed
        else:
            argv_profile = _argv_observations(environment, str(path))
            profile = {
                "mode": environment.get("mode", argv_profile.get("mode")),
                "threads": environment.get("threads", argv_profile.get("threads")),
                "forks": environment.get("forks", argv_profile.get("forks")),
                "warmups": environment.get("warmups", argv_profile.get("warmups")),
                "measurements": environment.get("measurements", argv_profile.get("measurements")),
                "warmup_time": environment.get("warmup_time", argv_profile.get("warmup_time")),
                "measurement_time": environment.get("measurement_time", argv_profile.get("measurement_time")),
                "profiler": environment.get("profiler", argv_profile.get("profiler")),
                "jvm_args": environment.get("jvm_args", argv_profile.get("jvm_args")),
            }
            run["observed_config"] = profile
    rollback = validate_rollback_bundle(rollback_bundle) if rollback_bundle else None
    for environment, path in zip(environments, environment_paths):
        declared = environment.get("rollback_bundle_sha256")
        expected = rollback["sha256"] if rollback else None
        if declared != expected:
            _fail(path, "rollback_bundle_sha256", declared, expected, "use the exact authenticated imported rollback bundle")
    comparison = compare_runs(runs[0], runs[1], rollback["ineligible_cells"] if rollback else None, str(output_path))
    write_comparison(output_path, comparison)
    result = {
        "schema_version": 1, "status": "passed", "mode": "compare",
        "run_ids": [runs[0]["run_id"], runs[1]["run_id"]],
        "observed_config_sha256": runs[0]["observed_config_sha256"],
        "comparison_path": str(Path(output_path).resolve()),
        "comparison_sha256": sha256_file(output_path),
        "verdicts": {method: row["verdict"] for method, row in sorted(comparison.items())},
        "reasons": {method: row["reason"] for method, row in sorted(comparison.items())},
        "rollback_bundle_sha256": rollback["sha256"] if rollback else None,
    }
    _write_json(validation_path, result)
    return result


def build_parser():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    run = commands.add_parser("run")
    run.add_argument("--jar", required=True, type=Path)
    run.add_argument("--input", required=True, type=Path)
    run.add_argument("--environment", required=True, type=Path)
    run.add_argument("--summary", required=True, type=Path)
    run.add_argument("--validation", required=True, type=Path)
    compare = commands.add_parser("compare")
    compare.add_argument("--run", required=True, action="append", type=Path)
    compare.add_argument("--environment", required=True, action="append", type=Path)
    compare.add_argument("--output", required=True, type=Path)
    compare.add_argument("--validation", required=True, type=Path)
    compare.add_argument("--rollback-bundle", type=Path)
    return parser


def main(argv=None):
    args = build_parser().parse_args(argv)
    try:
        if args.command == "run":
            validate_run(args.jar, args.input, args.environment, args.summary, args.validation)
        else:
            validate_compare(args.run, args.environment, args.output, args.validation, args.rollback_bundle)
    except ValueError as error:
        _write_json(args.validation, {
            "schema_version": 1,
            "status": "failed",
            "mode": args.command,
            "diagnostic": str(error),
        })
        print("validation failed: %s" % error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
