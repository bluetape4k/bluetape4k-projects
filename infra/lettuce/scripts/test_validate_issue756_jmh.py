import copy
import hashlib
import importlib.util
import json
import math
import subprocess
import tempfile
import unittest
from pathlib import Path


HERE = Path(__file__).resolve().parent


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


validator = load_module("validate_issue756_jmh", "validate-issue756-jmh.py")


BACKENDS = ("jdk", "kryo", "jackson2", "jackson3")
TARGETS = ("heap", "direct")


def method_name(backend, target, path):
    suffix = "CopiedBaseline" if path == "baseline" else "Candidate"
    return f"{backend}{target.title()}{suffix}"


def matrix_cells():
    cells = []
    for backend in BACKENDS:
        for target in TARGETS:
            baseline = method_name(backend, target, "baseline")
            candidate = method_name(backend, target, "candidate")
            cells.extend(
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
    return cells


def jmh_record(cell, allocation=100.0, throughput=10.0):
    return {
        "benchmark": validator.BENCHMARK_CLASS + "." + cell["method"],
        "mode": "thrpt",
        "threads": 1,
        "forks": 2,
        "warmupIterations": 3,
        "measurementIterations": 5,
        "primaryMetric": {
            "score": throughput,
            "scoreError": 0.1,
            "scoreUnit": "ops/ms",
        },
        "secondaryMetrics": {
            "gc.alloc.rate.norm": {
                "score": allocation,
                "scoreError": 0.1,
                "scoreUnit": "B/op",
            }
        },
    }


def jmh_records(candidate_allocation=94.0, candidate_throughput=10.0):
    return [
        jmh_record(
            cell,
            allocation=100.0 if cell["path"] == "baseline" else candidate_allocation,
            throughput=10.0 if cell["path"] == "baseline" else candidate_throughput,
        )
        for cell in matrix_cells()
    ]


def classpath_entries(root):
    paths = []
    for index, name in enumerate(("benchmark.jar", "jackson2.jar", "dependency.jar")):
        path = root / name
        path.write_bytes(f"jar-{index}".encode())
        paths.append(
            {
                "path": str(path.resolve()),
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                "kind": "jackson2-project" if name == "jackson2.jar" else "runtime",
            }
        )
    return paths


def preflight_cells(payload_sha="p" * 64):
    cells = []
    for cell in matrix_cells():
        cells.append(
            {
                **cell,
                "backend_class": validator.BACKEND_CLASSES[cell["backend"]],
                "backend_config_sha256": "c" * 64,
                "payload_sha256": payload_sha,
                "target_kind": cell["target"],
                "wire_sha256": "w" * 64,
                "written_count": 128,
                "prefix_preserved": True,
                "baseline_dispatch_count": 1 if cell["path"] == "baseline" else 0,
                "candidate_dispatch_count": 1 if cell["path"] == "candidate" else 0,
                "reset_before": {
                    "capacity": 512,
                    "max_capacity": 512,
                    "reader_index": 3,
                    "writer_index": 7,
                },
                "reset_after": {
                    "capacity": 512,
                    "max_capacity": 512,
                    "reader_index": 3,
                    "writer_index": 7,
                },
            }
        )
    return cells


def metadata(root, run_id="canonical-a"):
    payload_sha = "p" * 64
    classpath = classpath_entries(root)
    matrix = matrix_cells()
    fixture = {
        "payload_sha256": payload_sha,
        "allocator_class": "io.netty.buffer.PooledByteBufAllocator",
        "pooled": True,
        "capacity": 512,
        "max_capacity": 512,
        "reader_index": 3,
        "writer_index": 7,
        "headroom": 505,
    }
    dispatch = {
        backend: {
            "declaring_class": validator.BACKEND_CLASSES[backend],
            "dispatch_kind": "declared-direct",
            "runtime_declaring_class": validator.BACKEND_CLASSES[backend],
            "runtime_dispatch_kind": "declared-direct",
        }
        for backend in BACKENDS
    }
    preflight = {
        "schema_version": 1,
        "status": "passed",
        "fixture_sha256": "",
        "fixture": fixture,
        "cells": preflight_cells(payload_sha),
        "retained_backend_checks": {
            backend: {
                "status": "passed",
                "exception_parity": True,
                "state_preserved": True,
            }
            for backend in BACKENDS
        },
        "dispatch": dispatch,
    }
    preflight["fixture_sha256"] = validator.preflight_fixture_sha256(preflight)
    return {
        "schema_version": 1,
        "run_id": run_id,
        "benchmark_input_sha": "a" * 40,
        "benchmark_input_tree": "b" * 40,
        "clean_status": "clean",
        "benchmark_jar": classpath[0],
        "classpath": classpath,
        "protocol": {
            "forks": 2,
            "warmup_iterations": 3,
            "measurement_iterations": 5,
            "threads": 1,
            "profiler": "gc",
            "mode": "thrpt",
            "throughput_unit": "ops/ms",
        },
        "fixture": fixture,
        "matrix": matrix,
        "preflight": preflight,
        "preflight_sha256": hashlib.sha256(
            json.dumps(preflight, sort_keys=True, separators=(",", ":")).encode()
        ).hexdigest(),
        "dispatch": dispatch,
    }


def write_run(root, run_id="canonical-a", candidate_allocation=94.0):
    run = root / run_id
    run.mkdir()
    (run / "jmh.json").write_text(
        json.dumps(jmh_records(candidate_allocation)), encoding="utf-8"
    )
    (run / "metadata.json").write_text(
        json.dumps(metadata(root, run_id)), encoding="utf-8"
    )
    metadata_value = json.loads((run / "metadata.json").read_text())
    classpath = [entry["path"] for entry in metadata_value["classpath"]]
    result_path = run / "jmh.json"
    argv = [
        "java",
        "-cp",
        ":".join(classpath),
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
        validator.exact_include_regex(),
    ]
    (run / "argv.json").write_text(
        json.dumps({"schema_version": 1, "argv": argv, "exit_code": 0}),
        encoding="utf-8",
    )
    (run / "environment.json").write_text(
        json.dumps(
            {
                "schema_version": 1,
                "run_id": run_id,
                "benchmark_input_sha": metadata_value["benchmark_input_sha"],
                "benchmark_input_tree": metadata_value["benchmark_input_tree"],
                "os": "TestOS",
                "kernel": "test-kernel",
                "architecture": "test-arch",
                "cpu_model": "test-cpu",
                "logical_cores": 1,
                "java_home": "/test/java",
                "jvm_options": "",
                "gradle_command": ["./gradlew", "build"],
                "jmh_command": argv,
                "classpath": metadata_value["classpath"],
                "protocol": metadata_value["protocol"],
            }
        ),
        encoding="utf-8",
    )
    return run


class ValidatorFixtureTest(unittest.TestCase):
    def assert_reason(self, reason_code, callback):
        with self.assertRaises(validator.ValidationError) as caught:
            callback()
        self.assertEqual(reason_code, caught.exception.reason_code, str(caught.exception))

    def test_exact_16_cell_matrix_rejects_missing_duplicate_and_extra(self):
        base = matrix_cells()
        variants = (
            base[:-1],
            base + [copy.deepcopy(base[0])],
            base + [{**base[0], "method": "unexpectedMethod"}],
        )
        for cells in variants:
            with self.subTest(size=len(cells)):
                self.assert_reason(
                    "MATRIX_EXACT", lambda cells=cells: validator.validate_matrix(cells)
                )

    def test_baseline_candidate_pairing_mismatch_is_rejected(self):
        cells = matrix_cells()
        candidate = next(cell for cell in cells if cell["path"] == "candidate")
        candidate["paired_baseline"] = "wrongBaseline"
        self.assert_reason("PAIRING_MISMATCH", lambda: validator.validate_matrix(cells))

    def test_canonical_metadata_identity_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            first = metadata(root, "canonical-a")
            second = metadata(root, "canonical-b")
            second["fixture"]["payload_sha256"] = "x" * 64
            self.assert_reason(
                "CANONICAL_IDENTITY_MISMATCH",
                lambda: validator.validate_canonical_identity(first, second),
            )

    def test_protocol_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            for field, drift in (
                ("forks", 1),
                ("warmup_iterations", 2),
                ("measurement_iterations", 4),
                ("threads", 2),
                ("profiler", "none"),
            ):
                changed = copy.deepcopy(value)
                changed["protocol"][field] = drift
                with self.subTest(field=field):
                    self.assert_reason(
                        "PROTOCOL_MISMATCH",
                        lambda changed=changed: validator.validate_metadata(changed),
                    )

    def test_fixture_target_shape_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            for field, drift in (
                ("payload_sha256", "x" * 64),
                ("allocator_class", "wrong"),
                ("capacity", 511),
                ("max_capacity", 513),
                ("writer_index", 8),
                ("headroom", 504),
            ):
                changed = copy.deepcopy(value)
                changed["preflight"]["cells"][0]["payload_sha256"] = (
                    changed["fixture"]["payload_sha256"]
                )
                changed["fixture"][field] = drift
                with self.subTest(field=field):
                    self.assert_reason(
                        "FIXTURE_MISMATCH",
                        lambda changed=changed: validator.validate_metadata(changed),
                    )

    def test_reset_capacity_or_index_drift_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            for field in ("capacity", "reader_index", "writer_index"):
                changed = copy.deepcopy(value)
                changed["preflight"]["cells"][0]["reset_after"][field] += 1
                with self.subTest(field=field):
                    self.assert_reason(
                        "RESET_DRIFT",
                        lambda changed=changed: validator.validate_metadata(changed),
                    )

    def test_missing_allocation_or_throughput_metric_is_rejected(self):
        records = jmh_records()
        del records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]
        self.assert_reason("METRIC_MISSING", lambda: validator.validate_jmh_records(records))
        records = jmh_records()
        del records[0]["primaryMetric"]
        self.assert_reason("METRIC_MISSING", lambda: validator.validate_jmh_records(records))

    def test_allocation_metric_rejects_unit_non_finite_negative_and_zero_baseline(self):
        corruptions = (
            ("scoreUnit", "KB/op"),
            ("score", math.nan),
            ("score", math.inf),
            ("score", -1.0),
            ("scoreError", -1.0),
            ("scoreError", math.inf),
        )
        for field, value in corruptions:
            records = jmh_records()
            records[0]["secondaryMetrics"]["gc.alloc.rate.norm"][field] = value
            with self.subTest(field=field, value=value):
                self.assert_reason(
                    "ALLOCATION_METRIC_INVALID",
                    lambda records=records: validator.validate_jmh_records(records),
                )
        records = jmh_records()
        records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["score"] = 0.0
        self.assert_reason(
            "ALLOCATION_BASELINE_ZERO",
            lambda: validator.validate_jmh_records(records),
        )

    def test_throughput_metric_rejects_mode_unit_and_invalid_numbers(self):
        mutations = (
            ("mode", "avgt"),
            ("scoreUnit", "ops/s"),
            ("score", 0.0),
            ("score", math.nan),
            ("scoreError", -1.0),
            ("scoreError", math.inf),
        )
        for field, value in mutations:
            records = jmh_records()
            target = records[0] if field == "mode" else records[0]["primaryMetric"]
            target[field] = value
            with self.subTest(field=field, value=value):
                self.assert_reason(
                    "THROUGHPUT_METRIC_INVALID",
                    lambda records=records: validator.validate_jmh_records(records),
                )

    def test_classpath_rejects_missing_duplicate_directory_order_and_hash(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            value = metadata(root)
            missing = copy.deepcopy(value)
            missing["classpath"][1]["path"] = str(root / "missing.jar")
            self.assert_reason(
                "CLASSPATH_INVALID", lambda: validator.validate_classpath(missing)
            )
            duplicate = copy.deepcopy(value)
            duplicate["classpath"].append(copy.deepcopy(duplicate["classpath"][1]))
            self.assert_reason(
                "CLASSPATH_INVALID", lambda: validator.validate_classpath(duplicate)
            )
            directory = copy.deepcopy(value)
            directory["classpath"][1]["path"] = str(root)
            self.assert_reason(
                "CLASSPATH_INVALID", lambda: validator.validate_classpath(directory)
            )
            reordered = copy.deepcopy(value)
            reordered["classpath"] = list(reversed(reordered["classpath"]))
            self.assert_reason(
                "CLASSPATH_IDENTITY_MISMATCH",
                lambda: validator.validate_classpath(reordered),
            )
            wrong_hash = copy.deepcopy(value)
            wrong_hash["classpath"][1]["sha256"] = "0" * 64
            self.assert_reason(
                "ARTIFACT_IDENTITY_MISMATCH",
                lambda: validator.validate_classpath(wrong_hash),
            )

    def test_normalized_benchmark_jar_binds_source_and_executable_hashes(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            value = metadata(root)
            source = root / "benchmark-source.jar"
            source.write_bytes(b"signed-source")
            executable = Path(value["classpath"][0]["path"])
            value["classpath"][0]["normalization"] = {
                "policy": "strip-meta-inf-signatures-v1",
                "source_path": str(source.resolve()),
                "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                "executable_path": str(executable.resolve()),
                "executable_sha256": hashlib.sha256(executable.read_bytes()).hexdigest(),
                "removed_entries": [
                    "META-INF/DEPENDENCY.SF",
                    "META-INF/DEPENDENCY.SF",
                ],
            }
            value["benchmark_jar"] = copy.deepcopy(value["classpath"][0])
            validator.validate_classpath(value)
            value["classpath"][0]["normalization"]["source_sha256"] = "0" * 64
            value["benchmark_jar"] = copy.deepcopy(value["classpath"][0])
            self.assert_reason(
                "ARTIFACT_IDENTITY_MISMATCH",
                lambda: validator.validate_classpath(value),
            )

    def test_preflight_rejects_wiring_wire_count_prefix_target_and_backend_drift(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            corruptions = (
                ("method", "wrongMethod"),
                ("wire_sha256", "x" * 64),
                ("written_count", 127),
                ("prefix_preserved", False),
                ("target_kind", "wrong"),
                ("backend_class", "wrong.Backend"),
                ("backend_config_sha256", "x" * 64),
            )
            for field, drift in corruptions:
                changed = copy.deepcopy(value)
                changed["preflight"]["cells"][0][field] = drift
                with self.subTest(field=field):
                    self.assert_reason(
                        "PREFLIGHT_MISMATCH",
                        lambda changed=changed: validator.validate_metadata(changed),
                    )

    def test_dispatch_metadata_must_match_runtime_reflection(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            for field in ("runtime_declaring_class", "runtime_dispatch_kind"):
                changed = copy.deepcopy(value)
                changed["dispatch"]["jdk"][field] = "wrong"
                with self.subTest(field=field):
                    self.assert_reason(
                        "DISPATCH_MISMATCH",
                        lambda changed=changed: validator.validate_metadata(changed),
                    )

    def test_preflight_and_fixture_hashes_are_bound(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            value["preflight_sha256"] = "0" * 64
            self.assert_reason(
                "PREFLIGHT_BINDING_MISMATCH",
                lambda: validator.validate_metadata(value),
            )
            value = metadata(Path(temporary))
            value["preflight"]["fixture_sha256"] = "0" * 64
            value["preflight_sha256"] = hashlib.sha256(
                json.dumps(
                    value["preflight"], sort_keys=True, separators=(",", ":")
                ).encode()
            ).hexdigest()
            self.assert_reason(
                "PREFLIGHT_BINDING_MISMATCH",
                lambda: validator.validate_metadata(value),
            )

    def test_run_bundle_binds_argv_environment_and_profiler(self):
        with tempfile.TemporaryDirectory() as temporary:
            run = write_run(Path(temporary))
            argv_path = run / "argv.json"
            argv = json.loads(argv_path.read_text())
            profiler_index = argv["argv"].index("-prof")
            argv["argv"][profiler_index + 1] = "none"
            argv_path.write_text(json.dumps(argv))
            self.assert_reason(
                "PROTOCOL_MISMATCH", lambda: validator.validate_run_bundle(run)
            )

        with tempfile.TemporaryDirectory() as temporary:
            run = write_run(Path(temporary))
            environment_path = run / "environment.json"
            environment = json.loads(environment_path.read_text())
            environment["benchmark_input_tree"] = "x" * 40
            environment_path.write_text(json.dumps(environment))
            self.assert_reason(
                "ENVIRONMENT_MISMATCH", lambda: validator.validate_run_bundle(run)
            )

    def test_dirty_build_input_and_source_identity_drift_are_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            value = metadata(Path(temporary))
            value["clean_status"] = "dirty"
            self.assert_reason(
                "BUILD_INPUT_DIRTY", lambda: validator.validate_metadata(value)
            )

            value = metadata(Path(temporary))
            value["benchmark_input_sha"] = "short"
            self.assert_reason(
                "SOURCE_IDENTITY_MISMATCH", lambda: validator.validate_metadata(value)
            )

    def test_allocation_threshold_boundaries_are_decimal_exact(self):
        self.assertEqual(
            "inconclusive",
            validator.cell_verdict("100000", "95001", "10", "10", "declared-direct"),
        )
        self.assertEqual(
            "accepted",
            validator.cell_verdict("100000", "95000", "10", "10", "declared-direct"),
        )

    def test_throughput_threshold_boundaries_are_decimal_exact(self):
        self.assertEqual(
            "accepted",
            validator.cell_verdict("100", "90", "100000", "80001", "declared-direct"),
        )
        self.assertEqual(
            "ineligible",
            validator.cell_verdict("100", "90", "100000", "80000", "declared-direct"),
        )

    def test_inherited_default_is_always_ineligible(self):
        self.assertEqual(
            "ineligible",
            validator.cell_verdict("100", "1", "100", "1000", "inherited-default"),
        )

    def test_two_run_comparison_requires_allocation_threshold_in_both_runs(self):
        first = validator.validate_jmh_records(jmh_records(candidate_allocation=95.0))
        second = validator.validate_jmh_records(jmh_records(candidate_allocation=95.001))
        dispatch = {backend: "declared-direct" for backend in BACKENDS}
        compared = validator.compare_runs(first, second, dispatch)
        self.assertTrue(
            all(row["verdict"] == "inconclusive" for row in compared), compared
        )

    def test_run_bundle_validation_accepts_complete_fixture(self):
        with tempfile.TemporaryDirectory() as temporary:
            run = write_run(Path(temporary))
            result = validator.validate_run_bundle(run)
            self.assertEqual("passed", result["status"])
            self.assertEqual(16, result["method_count"])


class DeliveryValidationTest(unittest.TestCase):

    def test_plan_cli_accepts_benchmark_final_and_working_tree_modes(self):
        benchmark = validator._parser().parse_args(
            [
                "--root",
                "docs/benchmarks/raw/issue-756",
                "--benchmark-input-sha",
                "a" * 40,
                "--post-measurement-working-tree",
            ]
        )
        self.assertEqual("a" * 40, benchmark.benchmark_input_sha)
        self.assertTrue(benchmark.post_measurement_working_tree)

        final = validator._parser().parse_args(
            [
                "--root",
                "docs/benchmarks/raw/issue-756",
                "--final-delivery-sha",
                "b" * 40,
            ]
        )
        self.assertEqual("b" * 40, final.final_delivery_sha)

    def initialize_repository(self, root):
        subprocess.run(["git", "init", "-q"], cwd=root, check=True)
        subprocess.run(
            ["git", "config", "user.email", "issue756@example.invalid"],
            cwd=root,
            check=True,
        )
        subprocess.run(
            ["git", "config", "user.name", "Issue 756 Test"], cwd=root, check=True
        )
        marker = root / "marker.txt"
        marker.write_text("input\n", encoding="utf-8")
        subprocess.run(["git", "add", marker.name], cwd=root, check=True)
        subprocess.run(["git", "commit", "-q", "-m", "input"], cwd=root, check=True)
        return subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=root,
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()

    def git(self, root, *args):
        return subprocess.run(
            ["git", *args],
            cwd=root,
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()

    def test_benchmark_input_must_be_final_delivery_ancestor(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            input_sha = self.initialize_repository(root)
            self.git(root, "switch", "-q", "--orphan", "unrelated")
            marker = root / "marker.txt"
            if marker.exists():
                marker.unlink()
            (root / "other.txt").write_text("other\n", encoding="utf-8")
            self.git(root, "add", "other.txt")
            self.git(root, "commit", "-q", "-m", "unrelated")
            final_sha = self.git(root, "rev-parse", "HEAD")
            with self.assertRaises(validator.ValidationError) as caught:
                validator.validate_delivery_commits(root, input_sha, final_sha)
            self.assertEqual("BENCHMARK_NOT_ANCESTOR", caught.exception.reason_code)

    def test_committed_post_measurement_path_must_be_allowlisted(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            input_sha = self.initialize_repository(root)
            forbidden = root / "infra" / "lettuce" / "src" / "main.kt"
            forbidden.parent.mkdir(parents=True)
            forbidden.write_text("changed\n", encoding="utf-8")
            self.git(root, "add", ".")
            self.git(root, "commit", "-q", "-m", "forbidden")
            final_sha = self.git(root, "rev-parse", "HEAD")
            with self.assertRaises(validator.ValidationError) as caught:
                validator.validate_delivery_commits(root, input_sha, final_sha)
            self.assertEqual("POST_MEASUREMENT_PATH", caught.exception.reason_code)

    def test_allowed_post_measurement_paths_pass(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            input_sha = self.initialize_repository(root)
            allowed = root / "docs" / "benchmarks" / "raw" / "issue-756" / "validation.json"
            allowed.parent.mkdir(parents=True)
            allowed.write_text("{}\n", encoding="utf-8")
            self.git(root, "add", ".")
            self.git(root, "commit", "-q", "-m", "evidence")
            final_sha = self.git(root, "rev-parse", "HEAD")
            result = validator.validate_delivery_commits(root, input_sha, final_sha)
            self.assertEqual([str(allowed.relative_to(root))], result["changed_paths"])

    def test_working_tree_staged_unstaged_untracked_paths_fail_outside_allowlist(self):
        cases = ("staged", "unstaged", "untracked")
        for case in cases:
            with self.subTest(case=case), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                self.initialize_repository(root)
                forbidden = root / f"forbidden-{case}.txt"
                if case == "staged":
                    forbidden.write_text("staged\n", encoding="utf-8")
                    self.git(root, "add", forbidden.name)
                elif case == "unstaged":
                    marker = root / "marker.txt"
                    marker.write_text("changed\n", encoding="utf-8")
                else:
                    forbidden.write_text("untracked\n", encoding="utf-8")
                with self.assertRaises(validator.ValidationError) as caught:
                    validator.validate_post_measurement_working_tree(root)
                self.assertEqual("WORKING_TREE_PATH", caught.exception.reason_code)


if __name__ == "__main__":
    unittest.main()
