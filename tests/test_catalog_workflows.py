from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = ROOT / "scripts" / "verify-catalog-workflows.py"
SPEC = importlib.util.spec_from_file_location("verify_catalog_workflows", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load workflow verifier at {SCRIPT_PATH}")
verifier = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verifier)


class CatalogWorkflowsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.ci = (ROOT / ".github" / "workflows" / "ci.yml").read_text()
        self.submission = (
            ROOT / ".github" / "workflows" / "dependency-submission.yml"
        ).read_text()

    def test_repository_workflows_satisfy_catalog_contract(self) -> None:
        self.assertEqual(verifier.validate_workflows(self.ci, self.submission), [])

    def test_projects_ci_creates_pinned_parser_receipt_before_catalog_check(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        parser_contract = governance.find("dependabot-parser-contract.rb")
        catalog_check = governance.find("project-dependabot-catalog.py")

        self.assertGreaterEqual(parser_contract, 0, "Projects CI must run the parser contract")
        self.assertLess(parser_contract, catalog_check, "parser receipt must precede catalog check")
        self.assertIn("dependabot_core_commit", governance)
        self.assertIn("parser-receipt.json", governance)

    def test_dokka_freemarker_uses_central_patched_version(self) -> None:
        build_gradle = (ROOT / "build.gradle.kts").read_text()

        required_contract = [
            'configurations.matching { it.name.startsWith("dokka") }.configureEach',
            'val dokkaFreemarkerVersion = bt4kVersion("freemarker")',
            'require(isAtLeastDottedRelease(dokkaFreemarkerVersion, "2.3.35"))',
            '"org.freemarker" to "freemarker"',
            'useVersion(dokkaFreemarkerVersion)',
        ]
        missing_contract = [
            contract for contract in required_contract if contract not in build_gradle
        ]

        self.assertEqual(missing_contract, [])

    def test_rejects_catalog_ref_without_exact_commit_pin(self) -> None:
        invalid = self.ci.replace(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: 'e9946b18bdb0022f52cd5dc8994d859d470e7bab'",
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: 'develop'",
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("catalog SHA" in error for error in errors), errors)

    def test_rejects_checkout_of_a_moving_submission_ref(self) -> None:
        invalid = self.submission.replace(
            "ref: ${{ github.sha }}", "ref: refs/heads/develop"
        )

        errors = verifier.validate_workflows(self.ci, invalid)

        self.assertTrue(any("github.sha" in error for error in errors), errors)

    def test_rejects_skipped_catalog_governance(self) -> None:
        invalid = self.ci.replace(
            'if [[ "$CATALOG_GOVERNANCE_RESULT" != "success" ]]; then',
            'if [[ "$CATALOG_GOVERNANCE_RESULT" == "skipped" ]]; then',
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("catalog-governance" in error for error in errors), errors)

    def test_rejects_concurrency_that_cancels_or_merges_distinct_shas(self) -> None:
        invalid = self.submission.replace(
            "group: dependency-submission-${{ github.sha }}",
            "group: dependency-submission-${{ github.ref }}",
        ).replace("cancel-in-progress: false", "cancel-in-progress: true")

        errors = verifier.validate_workflows(self.ci, invalid)

        self.assertTrue(any("concurrency" in error for error in errors), errors)

    def test_rejects_dependency_submission_checkout_head_mismatch(self) -> None:
        invalid = self.submission.replace(
            'test "$CHECKOUT_SHA" = "$GITHUB_SHA"',
            'test "$CHECKOUT_SHA" = "$GITHUB_REF"',
        )

        errors = verifier.validate_workflows(self.ci, invalid)

        self.assertTrue(any("checkout HEAD" in error for error in errors), errors)


if __name__ == "__main__":
    unittest.main()
