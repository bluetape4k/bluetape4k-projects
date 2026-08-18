#!/usr/bin/env python3
"""Kover artifact manifest and aggregation regression tests."""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
AGGREGATOR = ROOT / ".github/scripts/aggregate-kover-coverage.py"


REPORT = """<?xml version="1.0" encoding="UTF-8"?>
<report name="fixture">
  <package name="fixture">
    <class name="fixture/Example">
      <method name="run" desc="()V">
        <counter type="INSTRUCTION" missed="1" covered="3" />
      </method>
    </class>
  </package>
</report>
"""


class AggregateKoverCoverageTest(unittest.TestCase):
    def run_aggregator(self, manifest: str, report: str | None = None, *, extra_args: list[str] | None = None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "coverage-artifacts"
            root.mkdir()
            manifest_path = Path(directory) / "expected-jobs.manifest"
            manifest_path.write_text(manifest, encoding="utf-8")
            if report is not None:
                report_path = root / "coverage-core" / "io" / "fixture" / "build" / "reports" / "kover" / "report.xml"
                report_path.parent.mkdir(parents=True)
                report_path.write_text(report, encoding="utf-8")

            summary_path = Path(directory) / "summary.md"
            environment = {"GITHUB_STEP_SUMMARY": str(summary_path)}
            result = subprocess.run(
                [
                    sys.executable,
                    str(AGGREGATOR),
                    str(root),
                    "--expected-manifest",
                    str(manifest_path),
                    *(extra_args or []),
                ],
                check=False,
                capture_output=True,
                text=True,
                env={**__import__("os").environ, **environment},
            )
            summary = summary_path.read_text(encoding="utf-8") if summary_path.exists() else ""
            return result, summary

    def test_all_skipped_is_explicit_success(self):
        result, summary = self.run_aggregator("test-core=skipped\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("No coverage reports expected", result.stdout)
        self.assertIn("No coverage reports expected", summary)

    def test_expected_success_without_reports_fails(self):
        result, _ = self.run_aggregator("test-core=success\n")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("expected coverage job", result.stderr)

    def test_failed_job_fails_even_when_another_report_exists(self):
        result, _ = self.run_aggregator("test-core=success\ntest-io=failure\n", REPORT)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("failed or cancelled", result.stderr)

    def test_download_failure_fails_even_when_report_exists(self):
        result, _ = self.run_aggregator(
            "test-core=success\n",
            REPORT,
            extra_args=["--download-outcome", "failure"],
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("artifact download", result.stderr)

    def test_all_skipped_with_report_fails_closed(self):
        result, _ = self.run_aggregator("test-core=skipped\n", REPORT)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("all coverage jobs were skipped", result.stderr)

    def test_corrupt_report_fails_closed(self):
        result, _ = self.run_aggregator("test-core=success\n", "<report>")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("invalid Kover report", result.stderr)

    def test_zero_instruction_report_fails_closed(self):
        result, _ = self.run_aggregator("test-core=success\n", "<report />")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("no instruction coverage", result.stderr)

    def test_valid_report_writes_summary(self):
        result, summary = self.run_aggregator("test-core=success\n", REPORT)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("io/fixture", result.stdout)
        self.assertIn("**TOTAL**", summary)

    def test_duplicate_reports_keep_union_semantics(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "coverage-artifacts"
            first = root / "coverage-core" / "io" / "fixture" / "build" / "reports" / "kover" / "report.xml"
            second = root / "coverage-io" / "io" / "fixture" / "build" / "reports" / "kover" / "report.xml"
            first.parent.mkdir(parents=True)
            second.parent.mkdir(parents=True)
            first.write_text(REPORT, encoding="utf-8")
            second.write_text(REPORT.replace('missed="1" covered="3"', 'missed="0" covered="4"'), encoding="utf-8")
            manifest = Path(directory) / "expected-jobs.manifest"
            manifest.write_text("test-core=success\n", encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(AGGREGATOR), str(root), "--expected-manifest", str(manifest)],
                check=False,
                capture_output=True,
                text=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("| 4 | 0 | 100.00% |", result.stdout)


if __name__ == "__main__":
    unittest.main()
