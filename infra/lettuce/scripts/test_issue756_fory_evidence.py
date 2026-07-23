import importlib.util
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runner = load("issue756_fory_lettuce_runner", "run-issue756-fory-evidence.py")
validator = load("issue756_fory_lettuce_validator", "validate-issue756-fory-evidence.py")


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


class LettuceHarnessTest(unittest.TestCase):
    def records(self):
        return [
            record(name, 100.0 if name.endswith("CopiedBaseline") else 90.0)
            for name in validator.EXPECTED_METHODS
        ]

    def test_exact_eight_method_regex_and_protocol(self):
        self.assertEqual(8, len(runner.EXPECTED_METHODS))
        regex = runner.exact_include_regex()
        for name in runner.EXPECTED_METHODS:
            self.assertRegex(runner.BENCHMARK_CLASS + "." + name, regex)
        argv = runner.fixed_jmh_argv(Path("benchmark.jar"), Path("jmh.json"))
        for option, value in (("-f", "2"), ("-wi", "3"), ("-i", "5"), ("-t", "1"), ("-prof", "gc")):
            self.assertEqual(value, argv[argv.index(option) + 1])

    def test_accepted_matrix(self):
        comparisons = validator.validate_records(self.records())
        self.assertEqual(4, len(comparisons))
        self.assertEqual({"accepted"}, {cell["disposition"] for cell in comparisons})

    def test_rejects_missing_duplicate_unexpected_and_bad_unit(self):
        records = self.records()
        for changed in (
            records[:-1],
            records + [records[0]],
            [{**records[0], "benchmark": validator.BENCHMARK_CLASS + ".unexpected"}] + records[1:],
        ):
            with self.assertRaises(validator.ValidationError):
                validator.validate_records(changed)
        records[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] = "KB/op"
        with self.assertRaises(validator.ValidationError):
            validator.validate_records(records)

    def test_error_overlap_is_inconclusive(self):
        records = self.records()
        for item in records:
            item["primaryMetric"]["scoreError"] = 20.0
            item["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreError"] = 20.0
        self.assertEqual(
            {"inconclusive"},
            {cell["disposition"] for cell in validator.validate_records(records)},
        )

    def test_preflight_requires_exact_state_checked_matrix(self):
        fixture = {
            "schema_version": 1,
            "status": "passed",
            "methods": [
                {"method": name, "prefix_preserved": True, "state_preserved": True}
                for name in validator.EXPECTED_METHODS
            ],
        }
        validator.validate_preflight(fixture)
        fixture["methods"][0]["state_preserved"] = False
        with self.assertRaises(validator.ValidationError):
            validator.validate_preflight(fixture)


if __name__ == "__main__":
    unittest.main()
