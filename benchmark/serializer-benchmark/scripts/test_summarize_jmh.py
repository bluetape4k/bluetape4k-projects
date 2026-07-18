import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("summarize-jmh.py")
SPEC = importlib.util.spec_from_file_location("summarize_jmh", SCRIPT)
summarize_jmh = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(summarize_jmh)


def entry(method, allocation, allocation_rate=12.5, gc_count=1.0, throughput=100.0):
    secondary = {
        "gc.alloc.rate.norm": {
            "score": allocation,
            "scoreError": 0.1,
            "scoreUnit": "B/op",
        },
        "gc.alloc.rate": {
            "score": allocation_rate,
            "scoreError": 0.2,
            "scoreUnit": "MB/sec",
        },
    }
    if gc_count is not None:
        secondary["gc.count"] = {
            "score": gc_count,
            "scoreError": 0.0,
            "scoreUnit": "counts",
        }
    return {
        "benchmark": f"io.bluetape4k.benchmark.serializer.BinarySerializerAllocationBenchmark.{method}",
        "mode": "thrpt",
        "primaryMetric": {
            "score": throughput,
            "scoreError": 1.0,
            "scoreUnit": "ops/s",
        },
        "secondaryMetrics": secondary,
    }


def rows(run_id, baseline, candidate, candidate_method="jdkSerializeOptimized"):
    baseline_method = summarize_jmh.baseline_name(candidate_method)
    return summarize_jmh.extract_rows(
        [
            entry(baseline_method, baseline),
            entry(candidate_method, candidate),
        ],
        run_id,
    )


class SummarizeJmhTest(unittest.TestCase):
    def test_extracts_gc_and_diagnostic_metrics(self):
        extracted = summarize_jmh.extract_rows(
            [entry("jdkSerializeOptimized", 940.0, allocation_rate=7.5, gc_count=3.0, throughput=123.0)],
            "run-a",
        )

        self.assertEqual(1, len(extracted))
        self.assertEqual("jdkSerializeOptimized", extracted[0]["method"])
        self.assertEqual(940.0, extracted[0]["gc_alloc_rate_norm_b_op"])
        self.assertEqual(7.5, extracted[0]["gc_alloc_rate_mb_s"])
        self.assertEqual(3.0, extracted[0]["gc_count"])
        self.assertEqual(123.0, extracted[0]["throughput_score"])

    def test_marks_two_runs_below_five_percent_inconclusive(self):
        compared = summarize_jmh.compare_runs(
            [rows("run-a", 1000.0, 960.0), rows("run-b", 1000.0, 960.0)]
        )

        self.assertEqual("inconclusive", compared[0]["verdict"])

    def test_accepts_two_claim_eligible_runs_at_or_above_five_percent(self):
        compared = summarize_jmh.compare_runs(
            [rows("run-a", 1000.0, 940.0), rows("run-b", 1000.0, 930.0)]
        )

        self.assertEqual("accepted", compared[0]["verdict"])
        self.assertEqual(-6.0, compared[0]["run_1_delta_pct"])
        self.assertEqual(-7.0, compared[0]["run_2_delta_pct"])

    def test_rejects_mixed_direction_runs(self):
        compared = summarize_jmh.compare_runs(
            [rows("run-a", 1000.0, 940.0), rows("run-b", 1000.0, 1010.0)]
        )

        self.assertEqual("inconclusive", compared[0]["verdict"])

    def test_never_accepts_compatibility_or_fallback_cells(self):
        for method in ("jdkSerializeCompatibility", "forySerializeFallback"):
            with self.subTest(method=method):
                compared = summarize_jmh.compare_runs(
                    [
                        rows("run-a", 1000.0, 100.0, method),
                        rows("run-b", 1000.0, 100.0, method),
                    ]
                )
                self.assertEqual(False, compared[0]["eligible"])
                self.assertEqual("ineligible", compared[0]["verdict"])

    def test_records_missing_allocation_count_as_na(self):
        extracted = summarize_jmh.extract_rows(
            [entry("jdkSerializeOptimized", 940.0, gc_count=None)],
            "run-a",
        )

        self.assertEqual("N/A", extracted[0]["gc_count"])

    def test_fails_when_gc_alloc_rate_norm_is_missing(self):
        invalid = entry("jdkSerializeOptimized", 940.0)
        del invalid["secondaryMetrics"]["gc.alloc.rate.norm"]

        with self.assertRaisesRegex(ValueError, "gc.alloc.rate.norm"):
            summarize_jmh.extract_rows([invalid], "run-a")


if __name__ == "__main__":
    unittest.main()
