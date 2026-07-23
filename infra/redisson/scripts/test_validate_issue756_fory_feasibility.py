import importlib.util
import json
import math
import tempfile
import unittest
from pathlib import Path


HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location(
    "issue756_fory_validator",
    HERE / "validate-issue756-fory-feasibility.py",
)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


def record(method, allocation, allocation_error=0.1, throughput=10.0):
    return {
        "benchmark": f"{validator.BENCHMARK_CLASS}.{method}",
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
                "scoreError": allocation_error,
                "scoreUnit": "B/op",
            },
        },
    }


def records():
    return [
        record("foryBaseline", 100.0),
        record("foryCandidate", 90.0),
        record("fastForyBaseline", 100.0),
        record("fastForyCandidate", 90.0),
    ]


class FeasibilityValidatorTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        for run_id in ("probe-a", "probe-b"):
            self.write_leaf(run_id)

    def tearDown(self):
        self.temporary.cleanup()

    def write_json(self, path, value):
        path.write_text(json.dumps(value, allow_nan=True), encoding="utf-8")

    def write_leaf(self, run_id, jmh=None, clean=True, jar_hash="a" * 64):
        leaf = self.root / run_id
        leaf.mkdir(parents=True, exist_ok=True)
        self.write_json(leaf / "jmh.json", records() if jmh is None else jmh)
        self.write_json(leaf / "argv.json", {"argv": ["java", "-jar", "benchmark.jar"]})
        environment = {key: "value" for key in validator.ENVIRONMENT_KEYS}
        environment.update(
            {
                "schemaVersion": 1,
                "runId": run_id,
                "logicalCores": 8,
                "benchmarkJarSha256": jar_hash,
            },
        )
        self.write_json(leaf / "environment.json", environment)
        self.write_json(
            leaf / "metadata.json",
            {
                "runId": run_id,
                "repositoryClean": clean,
                "benchmarkJarSha256": jar_hash,
                "commandSha256": "b" * 64,
                "preflight": {"status": "passed"},
            },
        )
        (leaf / "summary.csv").write_text("", encoding="utf-8")

    def assert_invalid(self, mutate):
        mutate()
        with self.assertRaises(validator.ValidationError):
            validator.validate(self.root)

    def test_accepts_complete_evidence(self):
        disposition = validator.validate(self.root)
        self.assertEqual("accepted", disposition["probeDisposition"])
        self.assertEqual("pending", disposition["encodeDisposition"])

    def test_rejects_missing_method(self):
        self.assert_invalid(
            lambda: self.write_leaf("probe-a", records()[:-1]),
        )

    def test_rejects_duplicate_method(self):
        self.assert_invalid(
            lambda: self.write_leaf("probe-a", records() + [records()[0]]),
        )

    def test_rejects_unexpected_method(self):
        broken = records()
        broken[-1]["benchmark"] = f"{validator.BENCHMARK_CLASS}.unexpected"
        self.assert_invalid(lambda: self.write_leaf("probe-a", broken))

    def test_rejects_missing_allocation_metric(self):
        broken = records()
        broken[0]["secondaryMetrics"] = {}
        self.assert_invalid(lambda: self.write_leaf("probe-a", broken))

    def test_rejects_invalid_allocation_unit(self):
        broken = records()
        broken[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] = "KB/op"
        self.assert_invalid(lambda: self.write_leaf("probe-a", broken))

    def test_rejects_nan(self):
        broken = records()
        broken[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["score"] = math.nan
        self.assert_invalid(lambda: self.write_leaf("probe-a", broken))

    def test_overlap_and_dirty_tree_are_terminal_rejections(self):
        overlap = records()
        overlap[1]["secondaryMetrics"]["gc.alloc.rate.norm"].update(
            {"score": 94.0, "scoreError": 10.0},
        )
        self.write_leaf("probe-a", overlap, clean=False)
        disposition = validator.validate(self.root)
        self.assertEqual("rejected", disposition["probeDisposition"])
        self.assertEqual("rejected", disposition["encodeDisposition"])

    def test_rejects_hash_drift(self):
        self.write_leaf("probe-b", jar_hash="c" * 64)
        with self.assertRaises(validator.ValidationError):
            validator.validate(self.root)

    def test_rejects_malformed_provenance(self):
        metadata = json.loads((self.root / "probe-a/metadata.json").read_text())
        metadata["preflight"] = {"status": "failed"}
        self.write_json(self.root / "probe-a/metadata.json", metadata)
        with self.assertRaises(validator.ValidationError):
            validator.validate(self.root)

    def test_rejects_process_environment_capture(self):
        environment = json.loads((self.root / "probe-a/environment.json").read_text())
        environment["PATH"] = "/secret"
        self.write_json(self.root / "probe-a/environment.json", environment)
        with self.assertRaises(validator.ValidationError):
            validator.validate(self.root)

    def test_rejects_sensitive_argv(self):
        self.write_json(
            self.root / "probe-a/argv.json",
            {"argv": ["java", "-Dpassword=secret", "https://user:pass@example.test"]},
        )
        with self.assertRaises(validator.ValidationError):
            validator.validate(self.root)


if __name__ == "__main__":
    unittest.main()
