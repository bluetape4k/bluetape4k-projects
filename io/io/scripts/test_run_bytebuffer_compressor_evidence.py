import copy
import hashlib
import importlib.util
import json
import math
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).with_name("run-bytebuffer-compressor-evidence.py")
SPEC = importlib.util.spec_from_file_location("issue755_evidence", SCRIPT)
evidence = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(evidence)


def metric(score=100.0, error=1.0, unit="B/op"):
    return {"score": score, "scoreError": error, "scoreUnit": unit}


def record(codec, payload, storage, method, *, allocation=None, throughput=10_000.0, throughput_error=10.0):
    payload_bytes = evidence.PAYLOAD_BYTES[payload]
    if allocation is None:
        allocation = 128.0 if method.endswith("CallerOwned") else payload_bytes + 1_000.0
    return {
        "jmhVersion": "1.37",
        "jvm": "/java",
        "jdkVersion": "21.0.8+9",
        "vmName": "OpenJDK 64-Bit Server VM",
        "vmVersion": "21.0.8+9",
        "jvmArgs": list(evidence.JVM_ARGS),
        "threads": 1,
        "forks": 2,
        "warmupIterations": 3,
        "warmupTime": "1 s",
        "measurementIterations": 5,
        "measurementTime": "1 s",
        "mode": "thrpt",
        "benchmark": "io.bluetape4k.io.benchmark.CallerOwnedByteBufferCompressorBenchmark." + method,
        "params": {"compressorName": codec, "payloadSize": payload, "storagePath": storage},
        "primaryMetric": metric(throughput, throughput_error, "ops/s"),
        "secondaryMetrics": {"gc.alloc.rate.norm": metric(allocation)},
    }


def matrix():
    return [
        record(codec, payload, storage, method)
        for codec in evidence.CODECS
        for payload in evidence.PAYLOAD_BYTES
        for storage in evidence.STORAGE_PATHS
        for method in evidence.METHODS
    ]


def metadata(identifier):
    return {
        "schemaVersion": 1,
        "runId": identifier,
        "profile": "canonical",
        "commit": "a" * 40,
        "tree": "b" * 40,
        "jarSha256": "c" * 64,
        "jmhVersion": "1.37",
        "stateScope": "Thread",
        "dependenciesSha256": hashlib.sha256(b"dependencies\n").hexdigest(),
        "jdk": "21.0.8+9",
        "jvm": "OpenJDK 64-Bit Server VM",
        "vmVersion": "21.0.8+9",
        "jvmExecutable": "/java",
        "actualJvmArgs": list(evidence.JVM_ARGS),
        "gc": "G1",
        "os": "macOS",
        "cpu": "Apple M4 Max",
    }


def write_run(parent, identifier, records=None):
    root = parent / identifier
    root.mkdir()
    records = records or matrix()
    (root / "jmh.json").write_text(json.dumps(records))
    (root / "metadata.json").write_text(json.dumps(metadata(identifier)))
    argv = [
        "java", "-jar", "/benchmark.jar", ".*CallerOwnedByteBufferCompressorBenchmark.*",
        *evidence.PROFILE_ARGS["canonical"], "-rff", str(root / "jmh.json"),
        "-jvmArgsAppend", " ".join(evidence.JVM_ARGS),
    ]
    (root / "argv.json").write_text(json.dumps({"argv": argv, "exitCode": 0, "logBytes": 0}))
    (root / "environment.json").write_text(json.dumps({
        "profileArgs": evidence.PROFILE_ARGS["canonical"],
        "jvmArgs": evidence.JVM_ARGS,
        "javaVersion": "21.0.8+9",
        "vmName": "OpenJDK 64-Bit Server VM",
        "vmVersion": "21.0.8+9",
        "jvmExecutable": "/java",
        "actualJvmArgs": list(evidence.JVM_ARGS),
    }))
    (root / "dependencies.txt").write_text("dependencies\n")
    (root / "run.log").write_bytes(b"")
    (root / "summary.csv").write_text("summary\n")
    (root / "source-inspection.json").write_text(json.dumps(evidence.source_inspection(evidence.repo_root())))
    (root / "validation.json").write_text(json.dumps({"schemaVersion": 1, "status": "PASS", "records": len(records)}))
    return root


class MetricValidationTest(unittest.TestCase):
    def test_finite_rejects_boolean_string_and_non_finite_values(self):
        for value in (True, "1.0", math.nan, math.inf):
            with self.subTest(value=value):
                with self.assertRaises(ValueError):
                    evidence.finite(value, "value")

    def test_allocation_requires_b_op_and_non_negative_scores(self):
        with self.assertRaisesRegex(ValueError, "B/op"):
            evidence.metric_interval(metric(unit="KB/op"), "gc.alloc.rate.norm", "B/op")
        with self.assertRaisesRegex(ValueError, "out of range"):
            evidence.metric_interval(metric(score=-1), "gc.alloc.rate.norm", "B/op")

    def test_allocation_gate_requires_five_percent_and_non_overlapping_intervals(self):
        self.assertTrue(evidence.allocation_accepted(metric(1_000, 10), metric(900, 10)))
        self.assertFalse(evidence.allocation_accepted(metric(1_000, 100), metric(900, 100)))
        self.assertFalse(evidence.allocation_accepted(metric(1_000, 1), metric(960, 1)))

    def test_throughput_gate_requires_twenty_percent_and_non_overlap(self):
        self.assertTrue(evidence.throughput_regressed(metric(10_000, 10, "ops/s"), metric(7_900, 10, "ops/s")))
        self.assertFalse(evidence.throughput_regressed(metric(10_000, 10, "ops/s"), metric(8_100, 10, "ops/s")))

    def test_backend_eligibility_excludes_native_mixed_storage(self):
        self.assertTrue(evidence.eligible("lz4", "heapToDirect"))
        self.assertTrue(evidence.eligible("deflate", "directToHeap"))
        self.assertFalse(evidence.eligible("snappy", "heapToDirect"))
        self.assertFalse(evidence.eligible("zstd", "directToHeap"))


class JmhValidationTest(unittest.TestCase):
    def test_complete_canonical_matrix_is_accepted(self):
        self.assertEqual(288, len(evidence.validate_jmh(matrix(), require_matrix=True)))

    def test_missing_and_duplicate_cells_fail_closed(self):
        records = matrix()
        with self.assertRaisesRegex(ValueError, "matrix mismatch"):
            evidence.validate_jmh(records[:-1], require_matrix=True)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            evidence.validate_jmh(records + [copy.deepcopy(records[0])], require_matrix=True)

    def test_missing_allocation_metric_and_wrong_unit_fail_closed(self):
        records = matrix()
        del records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]
        with self.assertRaisesRegex(ValueError, "gc.alloc.rate.norm"):
            evidence.validate_jmh(records, require_matrix=True)
        records = matrix()
        records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] = "KB/op"
        with self.assertRaisesRegex(ValueError, "B/op"):
            evidence.validate_jmh(records, require_matrix=True)

    def test_unknown_payload_does_not_invent_a_byte_count(self):
        records = [record("lz4", "small", "heap", "compressCallerOwned")]
        records[0]["params"]["payloadSize"] = "extra-large"
        with self.assertRaisesRegex(ValueError, "unexpected JMH params"):
            evidence.validate_jmh(records, require_matrix=False)

    def test_smoke_may_accept_nan_error_but_canonical_may_not(self):
        records = [record("lz4", "small", "heap", "compressCallerOwned")]
        records[0].update({
            "forks": 1, "warmupIterations": 1, "warmupTime": "100 ms",
            "measurementIterations": 1, "measurementTime": "100 ms",
        })
        records[0]["primaryMetric"]["scoreError"] = "NaN"
        records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreError"] = "NaN"
        evidence.validate_jmh(records, require_matrix=False, allow_unstable_error=True, expected_profile="smoke")
        with self.assertRaisesRegex(ValueError, "JSON number"):
            evidence.validate_jmh(records, require_matrix=False)

    def test_runtime_and_profile_drift_are_rejected(self):
        records = matrix()
        records[0]["forks"] = 1
        with self.assertRaisesRegex(ValueError, "identity drift|profile mismatch"):
            evidence.validate_jmh(records, require_matrix=True, expected_profile="canonical")
        records = matrix()
        with self.assertRaisesRegex(ValueError, "runtime identity"):
            evidence.validate_jmh(
                records,
                require_matrix=True,
                expected_profile="canonical",
                expected_authority={"jdk": "22", "vmName": "OpenJDK 64-Bit Server VM", "vmVersion": "21.0.8+9", "jvmExecutable": "/java"},
            )


class ComparisonTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temporary.name)

    def tearDown(self):
        self.temporary.cleanup()

    def two_runs(self, first_records=None, second_records=None):
        first = write_run(self.root, "run-20260721T120000Z-00000001", first_records)
        second = write_run(self.root, "run-20260721T121000Z-00000002", second_records)
        return first, second

    def test_two_matching_runs_accept_eligible_cells_and_mark_fallback_ineligible(self):
        rows = evidence.comparison_rows(list(self.two_runs()))
        accepted = [row for row in rows if row["codec"] == "lz4" and row["storage"] == "heap"]
        ineligible = [row for row in rows if row["codec"] == "snappy" and row["storage"] == "heapToDirect"]
        self.assertEqual({"accepted"}, {row["verdict"] for row in accepted})
        self.assertEqual({"ineligible"}, {row["verdict"] for row in ineligible})
        self.assertEqual(96, len(rows))

    def test_identity_mismatch_is_rejected(self):
        first, second = self.two_runs()
        changed = json.loads((second / "metadata.json").read_text())
        changed["jarSha256"] = "0" * 64
        (second / "metadata.json").write_text(json.dumps(changed))
        with self.assertRaisesRegex(ValueError, "identity"):
            evidence.comparison_rows([first, second])

    def test_both_run_throughput_regressions_require_design_review(self):
        records = matrix()
        for row in records:
            if row["params"] == {"compressorName": "lz4", "payloadSize": row["params"]["payloadSize"], "storagePath": "heap"}:
                if row["benchmark"].endswith("compressCallerOwned"):
                    row["primaryMetric"] = metric(7_500, 10, "ops/s")
        rows = evidence.comparison_rows(list(self.two_runs(records, copy.deepcopy(records))))
        verdicts = {row["verdict"] for row in rows if row["codec"] == "lz4" and row["storage"] == "heap" and row["operation"] == "compress"}
        self.assertEqual({"design-review-required"}, verdicts)

    def test_one_run_throughput_regression_does_not_block_allocation_claim(self):
        first_records = matrix()
        for row in first_records:
            if row["params"]["compressorName"] == "lz4" and row["params"]["storagePath"] == "heap" and row["benchmark"].endswith("compressCallerOwned"):
                row["primaryMetric"] = metric(7_500, 10, "ops/s")
        rows = evidence.comparison_rows(list(self.two_runs(first_records, matrix())))
        verdicts = {row["verdict"] for row in rows if row["codec"] == "lz4" and row["storage"] == "heap" and row["operation"] == "compress"}
        self.assertEqual({"accepted"}, verdicts)

    def test_one_payload_regression_in_both_runs_requires_design_review(self):
        records = matrix()
        for row in records:
            if (
                row["params"] == {"compressorName": "lz4", "payloadSize": "medium", "storagePath": "heap"}
                and row["benchmark"].endswith("decompressCallerOwned")
            ):
                row["primaryMetric"] = metric(7_500, 10, "ops/s")
        rows = evidence.comparison_rows(list(self.two_runs(records, copy.deepcopy(records))))
        verdicts = {
            row["verdict"] for row in rows
            if row["codec"] == "lz4" and row["storage"] == "heap" and row["operation"] == "decompress"
        }
        self.assertEqual({"design-review-required"}, verdicts)

    def test_five_percent_gate_without_payload_scaling_is_not_demonstrated(self):
        records = matrix()
        for row in records:
            if row["params"]["compressorName"] == "lz4" and row["params"]["storagePath"] == "heap" and row["benchmark"].endswith("compressCallerOwned"):
                baseline = evidence.PAYLOAD_BYTES[row["params"]["payloadSize"]] + 1_000.0
                row["secondaryMetrics"]["gc.alloc.rate.norm"] = metric(baseline * 0.90)
        rows = evidence.comparison_rows(list(self.two_runs(records, copy.deepcopy(records))))
        verdicts = {row["verdict"] for row in rows if row["codec"] == "lz4" and row["storage"] == "heap" and row["operation"] == "compress"}
        self.assertEqual({"not-demonstrated"}, verdicts)

    def test_extra_file_and_source_hash_drift_are_rejected(self):
        first, second = self.two_runs()
        (first / "extra.txt").write_text("unexpected")
        with self.assertRaisesRegex(ValueError, "file set"):
            evidence.comparison_rows([first, second])
        (first / "extra.txt").unlink()
        inspection = json.loads((first / "source-inspection.json").read_text())
        inspection[0]["sha256"] = "0" * 64
        (first / "source-inspection.json").write_text(json.dumps(inspection))
        with self.assertRaisesRegex(ValueError, "source hash"):
            evidence.comparison_rows([first, second])

    def test_comparison_output_is_no_clobber(self):
        rows = evidence.comparison_rows(list(self.two_runs()))
        output = self.root / "comparison.csv"
        evidence.write_comparison(output, rows)
        original = output.read_bytes()
        with self.assertRaises(FileExistsError):
            evidence.write_comparison(output, rows)
        self.assertEqual(original, output.read_bytes())


class RunnerContractTest(unittest.TestCase):
    def test_canonical_profile_is_exact(self):
        self.assertEqual(
            ["-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
            evidence.PROFILE_ARGS["canonical"],
        )

    def test_run_id_shape_is_strict(self):
        self.assertIsNotNone(evidence.RUN_ID_PATTERN.fullmatch(evidence.run_id()))
        self.assertIsNone(evidence.RUN_ID_PATTERN.fullmatch("run-latest"))

    def test_source_inspection_covers_every_codec_operation_and_storage(self):
        rows = evidence.source_inspection(evidence.repo_root())
        self.assertEqual(32, len(rows))
        evidence.validate_source_inspection(rows, evidence.repo_root())


if __name__ == "__main__":
    unittest.main()
