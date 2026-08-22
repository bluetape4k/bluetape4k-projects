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
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"
NIGHTLY_WORKFLOW = ROOT / ".github/workflows/nightly-tests.yml"


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

    def test_missing_expected_module_fails_closed(self):
        result, _ = self.run_aggregator(
            "test-core=success\n",
            REPORT,
            extra_args=["--expected-module", "infra/lettuce"],
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("expected coverage module", result.stderr)

    def test_empty_expected_module_fails_closed(self):
        result, _ = self.run_aggregator(
            "test-core=success\n",
            """<?xml version="1.0" encoding="UTF-8"?>
<report name="empty"><counter type="INSTRUCTION" missed="0" covered="0" /></report>
""",
            extra_args=["--expected-module", "io/fixture"],
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("empty Kover report", result.stderr)

    def test_expected_module_is_accepted_when_report_has_instructions(self):
        result, _ = self.run_aggregator(
            "test-core=success\n",
            REPORT,
            extra_args=["--expected-module", "io/fixture"],
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_duplicate_expected_module_fails_closed(self):
        result, _ = self.run_aggregator(
            "test-core=success\n",
            REPORT,
            extra_args=[
                "--expected-module",
                "io/fixture",
                "--expected-module",
                "io/fixture",
            ],
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("duplicate expected coverage module", result.stderr)

    def test_ci_separates_raw_and_aggregate_artifacts(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("pattern: coverage-*", workflow)
        self.assertIn("name: aggregate-coverage-all", workflow)
        self.assertNotIn("name: coverage-all", workflow)

    def test_ci_requires_the_infra_module_inventory(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("Verify Infra coverage inventory", workflow)
        for module in (
            "infra/lettuce",
            "cache/cache-lettuce",
            "infra/redisson",
            "cache/cache-redisson",
        ):
            self.assertIn(module, workflow)
        self.assertIn("--expected-module", workflow)

    def test_ci_routes_search_messaging_changes_to_nats_and_elasticsearch(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("search-messaging: ${{ steps.filter.outputs.search-messaging }}", workflow)
        self.assertIn("'infra/elasticsearch/**'", workflow)
        self.assertIn("'infra/nats/**'", workflow)
        self.assertIn("test-search-messaging:", workflow)
        self.assertIn(":bluetape4k-elasticsearch:test --max-workers=1", workflow)
        self.assertIn(":bluetape4k-nats:test --max-workers=1", workflow)
        self.assertIn(":bluetape4k-elasticsearch:koverXmlReport --max-workers=1", workflow)
        self.assertIn(":bluetape4k-nats:koverXmlReport --max-workers=1", workflow)
        self.assertIn("test-search-messaging=${{ needs.test-search-messaging.result }}", workflow)
        self.assertIn("'infra/elasticsearch'", workflow)
        self.assertIn("'infra/nats'", workflow)
        self.assertIn("test-search-messaging, test-kafka-infra", workflow)

    def test_ci_only_uploads_coveralls_after_successful_aggregation(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("id: aggregate-coverage", workflow)
        self.assertIn(
            "if: ${{ steps.aggregate-coverage.outcome == 'success' && hashFiles('coverage-artifacts/**/reports/kover/report.xml') != '' }}",
            workflow,
        )
        self.assertIn(
            "if: ${{ steps.aggregate-coverage.outcome == 'success' && steps.coveralls-files.outcome == 'success' }}",
            workflow,
        )

    def test_nightly_separates_raw_and_aggregate_artifacts(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("pattern: nightly-coverage-*", workflow)
        self.assertIn("name: aggregate-nightly-coverage-all", workflow)
        self.assertNotIn("name: nightly-coverage-all", workflow)

    def test_nightly_requires_the_infra_module_inventory(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("expected-modules.manifest", workflow)
        manifest_start = workflow.index("          if grep -q '^test-infra=success$'")
        manifest_end = workflow.index("          fi", manifest_start)
        manifest = workflow[manifest_start:manifest_end]
        actual_modules = {
            line.strip().removesuffix("\\").strip().strip("'")
            for line in manifest.splitlines()
            if line.strip().startswith("'")
        }
        expected_modules = {
            "infra/redisson",
            "infra/lettuce",
            "cache/cache-lettuce",
            "cache/cache-redisson",
            "infra/kafka",
            "infra/kafka4",
            "infra/resilience4j",
            "infra/bucket4j",
            "infra/micrometer",
            "cache/cache-hazelcast",
            "infra/elasticsearch",
            "infra/nats",
        }
        self.assertSetEqual(actual_modules, expected_modules)
        self.assertIn("expected_module_args+=(--expected-module \"$module\")", workflow)

    def test_nightly_excludes_code_free_redis_umbrella_from_coverage(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        redis_start = workflow.index("          - group: redis")
        redis_end = workflow.index("          - group: kafka-resilience", redis_start)
        redis_group = workflow[redis_start:redis_end]
        self.assertNotIn(":bluetape4k-redis:test", redis_group)
        self.assertNotIn(":bluetape4k-redis:koverXmlReport", redis_group)

        manifest_start = workflow.index("          if grep -q '^test-infra=success$'")
        manifest_end = workflow.index("          fi", manifest_start)
        expected_modules = workflow[manifest_start:manifest_end]
        self.assertNotIn("'infra/redis'", expected_modules)

    def test_nightly_keeps_redis_characterization_out_of_coverage(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        for task in (
            "fencingLeaseTopologyRecoveryTest",
            "coordinationLockTopologyRecoveryTest",
            "multiKeyLeasePerformanceTest",
            "coordinationLockPerformanceTest",
        ):
            self.assertIn(f"-x :bluetape4k-lettuce:{task}", workflow)

    def test_nightly_kover_failures_are_not_ignored(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        self.assertNotIn("continue-on-error: true", workflow)

    def test_nightly_spring_demos_skip_coverage_upload_without_kover_tasks(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        spring_docker_start = workflow.index("  test-spring-docker:")
        spring_docker_end = workflow.index("\n  # ── Testcontainers", spring_docker_start)
        spring_docker = workflow[spring_docker_start:spring_docker_end]
        self.assertIn('kover_tasks: ""', spring_docker)
        coverage_upload = spring_docker[spring_docker.index("      - name: Upload coverage report") :]
        self.assertIn("if: ${{ always() && matrix.kover_tasks != '' }}", coverage_upload)

    def test_ci_kover_failures_are_not_ignored(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertNotIn("continue-on-error: true", workflow)

    def test_nightly_overwrites_aggregate_artifact_on_rerun(self):
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        aggregate_name = workflow.index("name: aggregate-nightly-coverage-all")
        overwrite = workflow.index("overwrite: true", aggregate_name)
        self.assertLess(overwrite, workflow.index("path: coverage-artifacts/", aggregate_name))

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
