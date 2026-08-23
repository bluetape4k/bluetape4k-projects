#!/usr/bin/env python3
"""Daily CI domain dependency graph regression tests."""

from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"

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


if __name__ == "__main__":
    unittest.main()
