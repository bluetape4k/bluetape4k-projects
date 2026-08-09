#!/usr/bin/env python3

import re
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).resolve().parents[1]
WORKFLOWS = REPOSITORY / ".github" / "workflows"
CODEQL = (WORKFLOWS / "codeql.yml").read_text(encoding="utf-8")


def named_step(workflow: str, name: str) -> str:
    match = re.search(
        rf"^      - name: {re.escape(name)}\n(?P<body>.*?)(?=^      - (?:name:|uses:)|\Z)",
        workflow,
        re.MULTILINE | re.DOTALL,
    )
    if match is None:
        return ""
    return match.group("body")


class CodeqlWorkflowPolicyTest(unittest.TestCase):

    def test_java_kotlin_uses_the_checked_out_central_catalog_without_rewrite(self) -> None:
        checkout = named_step(CODEQL, "Checkout dependencies catalog")
        pin = named_step(CODEQL, "Pin Kotlin for CodeQL extractor")

        self.assertIn("repository: bluetape4k/bluetape4k-dependencies", checkout)
        self.assertIn("ref: ${{ steps.catalog-ref.outputs.ref }}", checkout)
        self.assertIn("path: .catalog/bluetape4k-dependencies", checkout)
        self.assertIn("BLUETAPE4K_DEPENDENCIES_CATALOG_PATH", CODEQL)
        self.assertEqual("", pin)
        self.assertNotIn("Pin Kotlin for CodeQL extractor", CODEQL)
        self.assertNotIn("2.3.21", CODEQL)

    def test_catalog_ref_is_resolved_from_the_checked_in_settings(self) -> None:
        resolve = named_step(CODEQL, "Resolve dependencies catalog ref")

        self.assertIn('id: catalog-ref', resolve)
        self.assertIn("settings.gradle.kts", resolve)
        self.assertIn('echo "ref=$catalog_ref" >> "$GITHUB_OUTPUT"', resolve)

    def test_testcontainers_exclusion_has_durable_evidence(self) -> None:
        self.assertNotIn("scope: testing-containers", CODEQL)
        self.assertNotIn("roots: testing/testcontainers", CODEQL)
        self.assertIn("actions/runs/29648755583", CODEQL)
        self.assertIn(
            "docs/lessons/2026-07-18-codeql-testcontainers-exclusion.md",
            CODEQL,
        )
        build = named_step(CODEQL, "Build with Gradle")
        self.assertIn("timeout-minutes: 120", build)

    def test_pull_request_ci_runs_this_policy(self) -> None:
        workflow = (WORKFLOWS / "ci.yml").read_text(encoding="utf-8")

        self.assertIn("name: CodeQL Workflow Policy", workflow)
        self.assertIn(
            "python3 -m unittest scripts/test_codeql_workflow_policy.py -v",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
