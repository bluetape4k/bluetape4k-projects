import importlib.util
import math
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runner = load("issue756_fory_redisson_runner", "run-issue756-fory-evidence.py")
validator = load("issue756_fory_redisson_validator", "validate-issue756-fory-evidence.py")


def record(method, allocation, throughput=10.0, error=0.1):
    return {
        "benchmark": validator.BENCHMARK_CLASS + "." + method,
        "mode": "thrpt",
        "threads": 1,
        "forks": 2,
        "warmupIterations": 3,
        "measurementIterations": 5,
        "primaryMetric": {"score": throughput, "scoreError": error, "scoreUnit": "ops/ms"},
        "secondaryMetrics": {
            "gc.alloc.rate.norm": {"score": allocation, "scoreError": error, "scoreUnit": "B/op"}
        },
    }


class RedissonHarnessTest(unittest.TestCase):
    def records(self, disposition="rejected"):
        return [
            record(name, 100.0 if name.endswith("CopiedBaseline") else 90.0)
            for name in validator.expected_methods(disposition)
        ]

    def test_rejected_encode_has_twelve_methods_and_implemented_has_sixteen(self):
        self.assertEqual(12, len(validator.expected_methods("rejected")))
        self.assertEqual(16, len(validator.expected_methods("implemented")))
        for disposition in ("rejected", "implemented"):
            regex = runner.exact_include_regex(disposition)
            for name in validator.expected_methods(disposition):
                self.assertRegex(runner.BENCHMARK_CLASS + "." + name, regex)

    def test_protocol_and_fallback_dispositions(self):
        comparisons = validator.validate_records(self.records(), "rejected")
        self.assertEqual(6, len(comparisons))
        self.assertEqual(
            {"fallback"},
            {cell["disposition"] for cell in comparisons if cell["source"] == "composite"},
        )
        self.assertEqual(
            {"accepted"},
            {cell["disposition"] for cell in comparisons if cell["source"] != "composite"},
        )
        argv = runner.fixed_jmh_argv(Path("benchmark.jar"), Path("jmh.json"), "rejected")
        for option, value in (("-f", "2"), ("-wi", "3"), ("-i", "5"), ("-t", "1"), ("-prof", "gc")):
            self.assertEqual(value, argv[argv.index(option) + 1])

    def test_rejects_cardinality_duplicate_unexpected_unit_nan_and_bad_disposition(self):
        records = self.records()
        cases = (
            records[:-1],
            records + [records[0]],
            [{**records[0], "benchmark": validator.BENCHMARK_CLASS + ".unexpected"}] + records[1:],
        )
        for changed in cases:
            with self.assertRaises(validator.ValidationError):
                validator.validate_records(changed, "rejected")
        records = self.records()
        records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] = "KB/op"
        with self.assertRaises(validator.ValidationError):
            validator.validate_records(records, "rejected")
        records = self.records()
        records[0]["primaryMetric"]["score"] = math.nan
        with self.assertRaises(validator.ValidationError):
            validator.validate_records(records, "rejected")
        with self.assertRaises(validator.ValidationError):
            validator.matrix("unknown")

    def test_preflight_rejects_promotable_composite_and_disposition_mismatch(self):
        methods = []
        for cell in validator.matrix("rejected"):
            methods.extend(
                {
                    "method": name,
                    "source": cell["source"],
                    "promotable": cell["promotable"],
                    "state_preserved": True,
                }
                for name in (cell["baseline"], cell["candidate"])
            )
        fixture = {
            "schema_version": 1,
            "status": "passed",
            "encode_disposition": "rejected",
            "methods": methods,
        }
        validator.validate_preflight(fixture, "rejected")
        composite = next(cell for cell in methods if cell["source"] == "composite")
        composite["promotable"] = True
        with self.assertRaises(validator.ValidationError):
            validator.validate_preflight(fixture, "rejected")
        fixture["encode_disposition"] = "implemented"
        with self.assertRaises(validator.ValidationError):
            validator.validate_preflight(fixture, "rejected")


if __name__ == "__main__":
    unittest.main()
