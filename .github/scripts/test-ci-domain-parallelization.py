#!/usr/bin/env python3
"""Daily CI domain dependency graph regression tests."""

from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"
NIGHTLY_WORKFLOW = ROOT / ".github/workflows/nightly-tests.yml"
ROOT_BUILD = ROOT / "build.gradle.kts"

BENCHMARK_TESTS = (
    ROOT / "infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceThroughputBenchmark.kt",
    ROOT / "infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonConcurrencyBenchmark.kt",
    ROOT / "io/http/src/test/kotlin/io/bluetape4k/http/benchmark/HttpClientBenchmarkTest.kt",
    ROOT / "utils/workflow/src/test/kotlin/io/bluetape4k/workflow/examples/OrderProcessingExecutionModelBenchmarkTest.kt",
)

BENCHMARK_PROJECTS = (
    "protobuf-codec-benchmark",
    "serializer-benchmark",
    "web-framework-benchmark",
)

DOMAIN_JOBS = (
    "test-core",
    "test-io",
    "test-io-http",
    "test-testcontainers-spring",
    "testcontainers-image-gate",
    "test-ktor",
    "test-key-utils",
    "test-infra",
    "test-search-messaging",
    "test-kafka-infra",
    "test-telemetry-infra",
    "test-data",
)


def job_block(workflow: str, job_name: str) -> str:
    jobs = workflow[workflow.index("jobs:\n") + len("jobs:\n") :]
    header = re.compile(rf"^  {re.escape(job_name)}:\n", re.MULTILINE)
    match = header.search(jobs)
    if match is None:
        raise AssertionError(f"job not found: {job_name}")

    next_job = re.search(r"^  [a-z0-9][a-z0-9-]*:\n", jobs[match.end() :], re.MULTILINE)
    end = match.end() + next_job.start() if next_job else len(jobs)
    return jobs[match.start() : end]


def inline_needs(block: str) -> set[str]:
    match = re.search(r"^    needs: \[([^]]+)]$", block, re.MULTILINE)
    if match is None:
        raise AssertionError("inline needs declaration not found")
    return {item.strip() for item in match.group(1).split(",")}


def path_filter_block(workflow: str, filter_name: str) -> str:
    marker = f"            {filter_name}:\n"
    start = workflow.index(marker)
    next_filter = re.search(
        r"^            [a-z0-9][a-z0-9-]*:\n",
        workflow[start + len(marker) :],
        re.MULTILINE,
    )
    end = start + len(marker) + next_filter.start() if next_filter else len(workflow)
    return workflow[start:end]


class CiDomainParallelizationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = CI_WORKFLOW.read_text(encoding="utf-8")

    def test_domain_jobs_run_after_gates_without_waiting_for_full_build(self):
        expected_needs = {"changes", "catalog-governance"}
        for job_name in DOMAIN_JOBS:
            with self.subTest(job=job_name):
                self.assertSetEqual(inline_needs(job_block(self.workflow, job_name)), expected_needs)

    def test_full_build_remains_an_independent_required_check(self):
        self.assertSetEqual(
            inline_needs(job_block(self.workflow, "build")),
            {"validate-wrapper", "catalog-governance"},
        )
        status_needs = inline_needs(job_block(self.workflow, "ci-status"))
        self.assertIn("build", status_needs)
        self.assertIn("coverage-report", status_needs)
        self.assertIn("testcontainers-image-gate", status_needs)

    def test_coverage_report_still_waits_for_every_coverage_job(self):
        coverage_jobs = set(DOMAIN_JOBS) - {"testcontainers-image-gate"}
        coverage_needs = inline_needs(job_block(self.workflow, "coverage-report"))
        self.assertSetEqual(coverage_needs, coverage_jobs)
        self.assertNotIn("build", coverage_needs)

    def test_changes_job_runs_this_contract(self):
        changes = job_block(self.workflow, "changes")
        self.assertIn("Validate CI domain dependency graph", changes)
        self.assertIn("python3 .github/scripts/test-ci-domain-parallelization.py -v", changes)

    def test_ci_and_nightly_exclude_benchmark_tag(self):
        for workflow_path in (CI_WORKFLOW, NIGHTLY_WORKFLOW):
            with self.subTest(workflow=workflow_path.name):
                workflow = workflow_path.read_text(encoding="utf-8")
                self.assertIn("ORG_GRADLE_PROJECT_excludeBenchmarks: 'true'", workflow)

    def test_ci_and_nightly_compile_builds_exclude_benchmark_projects(self):
        for workflow_path in (CI_WORKFLOW, NIGHTLY_WORKFLOW):
            build = job_block(workflow_path.read_text(encoding="utf-8"), "build")
            for project_name in BENCHMARK_PROJECTS:
                with self.subTest(workflow=workflow_path.name, project=project_name):
                    self.assertIn(f"-x :{project_name}:build", build)

    def test_root_test_tasks_honor_benchmark_exclusion_property(self):
        root_build = ROOT_BUILD.read_text(encoding="utf-8")
        self.assertIn('gradleProperty("excludeBenchmarks")', root_build)
        self.assertIn('excludeTags("benchmark")', root_build)

    def test_junit_benchmarks_use_the_benchmark_tag(self):
        for benchmark_test in BENCHMARK_TESTS:
            with self.subTest(test=benchmark_test.relative_to(ROOT)):
                source = benchmark_test.read_text(encoding="utf-8")
                self.assertIn("import org.junit.jupiter.api.Tag", source)
                self.assertIn('@Tag("benchmark")', source)

    def test_daily_image_gate_runs_only_for_relevant_changes_or_manual_dispatch(self):
        gate = job_block(self.workflow, "testcontainers-image-gate")
        self.assertIn(
            "if: ${{ needs.changes.outputs['testcontainers-image-gate'] == 'true' || "
            "github.event_name == 'workflow_dispatch' }}",
            gate,
        )
        self.assertNotIn("needs.changes.outputs.shared", gate)

    def test_daily_image_gate_filter_excludes_generic_workflow_and_contract_test_changes(self):
        image_gate_filter = path_filter_block(self.workflow, "testcontainers-image-gate")
        for required_path in (
            "'testing/testcontainers/**'",
            "'scripts/testcontainers_image_gate_manifest.json'",
            "'scripts/testcontainers_image_gate.py'",
            "'scripts/run_testcontainers_image_gate.py'",
        ):
            with self.subTest(required_path=required_path):
                self.assertIn(required_path, image_gate_filter)

        for excluded_path in (
            "'.github/workflows/ci.yml'",
            "'.github/workflows/nightly-tests.yml'",
            "'.github/workflows/release.yml'",
            "'scripts/test_testcontainers_contract.py'",
            "'scripts/test_testcontainers_image_gate.py'",
            "'scripts/test_run_testcontainers_image_gate.py'",
        ):
            with self.subTest(excluded_path=excluded_path):
                self.assertNotIn(excluded_path, image_gate_filter)

    def test_http_domain_tests_cover_both_local_mock_server_images(self):
        io_http_filter = path_filter_block(self.workflow, "io-http")
        self.assertIn("'testing/mock-web-server/**'", io_http_filter)
        self.assertIn("'testing/mock-webflux-server/**'", io_http_filter)

    def test_nightly_keeps_full_testcontainers_and_image_gate_verification(self):
        nightly = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        image_gate = job_block(nightly, "test-testcontainers-image-gate")
        self.assertIn(
            "if: ${{ needs.plan.outputs.scope == 'full' || needs.plan.outputs.scope == 'testcontainers' }}",
            image_gate,
        )
        self.assertIn("--scope full", image_gate)
        self.assertIn("test-testcontainers:", nightly)
        self.assertIn("test-testcontainers-spring:", nightly)


if __name__ == "__main__":
    unittest.main()
