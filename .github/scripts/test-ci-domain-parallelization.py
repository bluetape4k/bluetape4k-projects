#!/usr/bin/env python3
"""Daily CI domain dependency graph regression tests."""

from __future__ import annotations

import fnmatch
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

SHARED_FILTER_PATHS = (
    "'buildSrc/**'",
    "'build.gradle.kts'",
    "'settings.gradle.kts'",
    "'gradle/**'",
    "'gradle.properties'",
    "'.github/workflows/**'",
    "'.github/scripts/**'",
    "'.github/actions/**'",
    "'scripts/validate-ci-*.rb'",
)

DOMAIN_JOBS = (
    "test-core",
    "test-io",
    "test-io-http",
    "test-testcontainers-spring",
    "test-ktor",
    "test-key-utils",
    "test-infra",
    "test-search-messaging",
    "test-kafka-infra",
    "test-telemetry-infra",
    "test-data",
    "test-spring-boot",
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
        self.assertIn("test-spring-boot", status_needs)
        self.assertNotIn("testcontainers-image-gate", status_needs)

    def test_coverage_report_still_waits_for_every_coverage_job(self):
        coverage_jobs = set(DOMAIN_JOBS)
        coverage_needs = inline_needs(job_block(self.workflow, "coverage-report"))
        self.assertSetEqual(coverage_needs, coverage_jobs)
        self.assertNotIn("build", coverage_needs)

    def test_changes_job_runs_this_contract(self):
        changes = job_block(self.workflow, "changes")
        self.assertIn("Validate CI domain dependency graph", changes)
        self.assertIn("python3 .github/scripts/test-ci-domain-parallelization.py -v", changes)

    def test_shared_filter_covers_ci_orchestration_inputs(self):
        shared_filter = path_filter_block(self.workflow, "shared")
        for path_pattern in SHARED_FILTER_PATHS:
            with self.subTest(path_pattern=path_pattern):
                self.assertIn(path_pattern, shared_filter)

    def test_spring_boot_fixture_routes_to_the_spring_boot_test_job(self):
        changes = job_block(self.workflow, "changes")
        self.assertIn(
            "spring-boot: ${{ steps.filter.outputs.spring-boot }}",
            changes,
        )
        spring_filter = path_filter_block(self.workflow, "spring-boot")
        self.assertIn("'spring-boot/**'", spring_filter)
        fixture = (
            "spring-boot/cassandra/src/test/kotlin/"
            "io/bluetape4k/spring/cassandra/convert/CassandraTypeMappingTest.kt"
        )
        filter_patterns = re.findall(r"^              - '([^']+)'$", spring_filter, re.MULTILINE)
        self.assertTrue(any(fnmatch.fnmatchcase(fixture, pattern) for pattern in filter_patterns))

        spring_job = job_block(self.workflow, "test-spring-boot")
        self.assertIn(
            "needs.changes.outputs['spring-boot'] == 'true'",
            spring_job,
        )
        self.assertIn(
            ":bluetape4k-spring-boot-cassandra:test",
            spring_job,
        )
        self.assertIn(
            ":bluetape4k-spring-boot-cassandra:koverXmlReport",
            spring_job,
        )

    def test_spring_boot_skip_is_not_accepted_when_changes_are_detected(self):
        status = job_block(self.workflow, "ci-status")
        self.assertIn(
            "SPRING_BOOT_CHANGED: ${{ needs.changes.outputs['spring-boot'] }}",
            status,
        )
        self.assertIn(
            "SPRING_BOOT_RESULT: ${{ needs.test-spring-boot.result }}",
            status,
        )
        self.assertIn(
            '[[ "$SPRING_BOOT_CHANGED" == "true" && "$SPRING_BOOT_RESULT" == "skipped" ]]',
            status,
        )

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

    def test_ci_excludes_image_gate_and_keeps_spring_bridge(self):
        self.assertNotIn("testcontainers-image-gate", self.workflow)
        self.assertIn("test-testcontainers-spring:", self.workflow)

    def test_http_domain_tests_cover_both_local_mock_server_images(self):
        io_http_filter = path_filter_block(self.workflow, "io-http")
        self.assertIn("'testing/mock-web-server/**'", io_http_filter)
        self.assertIn("'testing/mock-webflux-server/**'", io_http_filter)

    def test_nightly_keeps_full_testcontainers_and_image_gate_verification(self):
        nightly = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        image_gate = job_block(nightly, "test-testcontainers-image-gate")
        self.assertIn(
            "if: ${{ needs.plan.outputs.scope == 'full' }}",
            image_gate,
        )
        self.assertIn("--scope full", image_gate)
        self.assertIn("test-testcontainers:", nightly)
        self.assertIn("test-testcontainers-spring:", nightly)

    def test_nightly_spring_boot_matrix_is_full_scope_only(self):
        nightly = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        spring_job = job_block(nightly, "test-spring-docker")
        self.assertIn(
            "if: ${{ needs.plan.outputs.run_standard == 'true' }}",
            spring_job,
        )


if __name__ == "__main__":
    unittest.main()
