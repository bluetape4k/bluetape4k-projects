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

    def test_projects_ci_pins_catalog_tag_to_peeled_commit(self) -> None:
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'dcc7fa7557d3c0548b9e3fc28c48877827ea2f74'",
            self.ci,
        )
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG: 'catalog/2026-10-20-01'",
            self.ci,
        )
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: 'dcc7fa7557d3c0548b9e3fc28c48877827ea2f74'",
            self.ci,
        )
        governance = verifier._job_block(self.ci, "catalog-governance")
        self.assertIn(
            "ref: ${{ env.BLUETAPE4K_DEPENDENCIES_CATALOG_TAG }}",
            governance,
        )
        self.assertIn(
            '"${BLUETAPE4K_DEPENDENCIES_CATALOG_TAG}^{commit}"',
            governance,
        )
        self.assertNotIn("export BLUETAPE4K_DEPENDENCIES_CATALOG_REF=", governance)
        settings = (ROOT / "settings.gradle.kts").read_text()
        self.assertIn(
            '.orElse("dcc7fa7557d3c0548b9e3fc28c48877827ea2f74")',
            settings,
        )

    def test_rejects_catalog_tag_as_the_canonical_ref(self) -> None:
        invalid = self.ci.replace(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'dcc7fa7557d3c0548b9e3fc28c48877827ea2f74'",
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'catalog/2026-10-20-01'",
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("catalog REF must be a full commit SHA" in error for error in errors), errors)

    def test_projects_ci_creates_pinned_parser_receipt_before_catalog_check(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        parser_contract = governance.find("dependabot-parser-contract.rb")
        catalog_check = governance.find("project-dependabot-catalog.py")

        self.assertGreaterEqual(parser_contract, 0, "Projects CI must run the parser contract")
        self.assertLess(parser_contract, catalog_check, "parser receipt must precede catalog check")
        self.assertIn("dependabot_core_commit", governance)
        self.assertIn("parser-receipt.json", governance)

    def test_gradle_catalog_tag_resolves_with_its_pinned_commit(self) -> None:
        settings = (ROOT / "settings.gradle.kts").read_text()

        required_contract = [
            'providers.gradleProperty("bluetape4kDependenciesCatalogSha")',
            'providers.environmentVariable("BLUETAPE4K_DEPENDENCIES_CATALOG_SHA")',
            'Regex("catalog/[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}")',
            'val bluetape4kDependenciesCatalogCacheSha = bluetape4kDependenciesCatalogSha',
            '"https://raw.githubusercontent.com/bluetape4k/bluetape4k-dependencies/$bluetape4kDependenciesCatalogSha/gradle"',
        ]
        missing_contract = [
            contract for contract in required_contract if contract not in settings
        ]

        self.assertEqual(missing_contract, [])

    def test_parser_contract_runs_inside_pinned_dependabot_bundle(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")

        self.assertIn(
            "BUNDLE_GEMFILE: ${{ github.workspace }}/.gradle/dependabot-core/Gemfile",
            governance,
        )
        self.assertIn(
            "bundle exec ruby .gradle/catalog-source/bluetape4k-dependencies/tests/fixtures/project-dependabot-catalog/dependabot-parser-contract.rb",
            governance,
        )

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
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: 'dcc7fa7557d3c0548b9e3fc28c48877827ea2f74'",
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
