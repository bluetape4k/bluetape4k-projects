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

    def test_projects_ci_pins_exact_catalog_tag_object(self) -> None:
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA: '4919a6f511e57b4349ca6856816500b3a85ace95'",
            self.ci,
        )
        governance = verifier._job_block(self.ci, "catalog-governance")
        self.assertIn(
            'tag_object_sha="$(git -C .gradle/catalog-source/bluetape4k-dependencies '
            'rev-parse "${BLUETAPE4K_DEPENDENCIES_CATALOG_TAG}^{tag}")"',
            governance,
        )
        self.assertIn(
            'test "$tag_object_sha" = "$BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA"',
            governance,
        )

    def test_rejects_catalog_tag_object_guard_bound_to_commit_sha(self) -> None:
        invalid = self.ci.replace(
            'test "$tag_object_sha" = "$BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA"',
            'test "$tag_object_sha" = "$BLUETAPE4K_DEPENDENCIES_CATALOG_SHA"',
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("catalog tag object" in error for error in errors), errors)

    def test_mongodb_container_jobs_use_a_kernel_compatible_runner(self) -> None:
        data_job = verifier._job_block(self.ci, "test-data")
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")

        self.assertIn("runs-on: ubuntu-22.04", data_job)
        self.assertIn(
            "runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            spring_boot_job,
        )

    def test_rejects_latest_runner_for_mongodb_container_tests(self) -> None:
        invalid = self.ci.replace(
            "runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("MongoDB container tests" in error for error in errors), errors)

    def test_rejects_latest_runner_for_data_container_tests(self) -> None:
        data_job = verifier._job_block(self.ci, "test-data")
        invalid_data_job = data_job.replace(
            "runs-on: ubuntu-22.04", "runs-on: ubuntu-latest", 1
        )
        invalid = self.ci.replace(data_job, invalid_data_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Test/Data" in error for error in errors), errors)

    def test_rejects_catalog_tag_as_the_canonical_ref(self) -> None:
        invalid = self.ci.replace(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'dcc7fa7557d3c0548b9e3fc28c48877827ea2f74'",
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'catalog/2026-10-20-01'",
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("catalog REF must be a full commit SHA" in error for error in errors), errors)

    def test_rejects_projects_governance_checkout_at_pull_request_merge_sha(self) -> None:
        invalid = self.ci.replace(
            "ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n          fetch-depth: 0",
            "ref: ${{ github.sha }}\n          fetch-depth: 0",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_rejects_explicit_first_party_repository_checkout_without_expected_sha(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}"
        )
        bypass_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          repository: bluetape4k/bluetape4k-projects\n"
            "          ref: ${{ github.sha }}"
        )
        invalid_test_core = test_core.replace(expected_checkout, bypass_checkout, 1)
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_rejects_github_repository_checkout_without_expected_sha(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}"
        )
        bypass_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          repository: ${{ github.repository }}\n"
            "          ref: ${{ github.sha }}"
        )
        invalid_test_core = test_core.replace(expected_checkout, bypass_checkout, 1)
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_rejects_checkout_v8_without_the_expected_projects_ref(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}"
        )
        invalid_checkout = (
            "      - uses: actions/checkout@v8\n"
            "        with:\n"
            "          ref: ${{ github.sha }}"
        )
        invalid_test_core = test_core.replace(expected_checkout, invalid_checkout, 1)
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_rejects_unpinned_external_repository_checkout(self) -> None:
        manual_job = verifier._job_block(self.ci, "manual-contract")
        invalid_manual_job = manual_job.replace(
            "ref: ${{ env.BLUETAPE4K_MANUAL_REF }}",
            "ref: refs/heads/main",
            1,
        )
        invalid = self.ci.replace(manual_job, invalid_manual_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("external checkout" in error for error in errors), errors
        )

    def test_rejects_unapproved_external_repository_checkout(self) -> None:
        invalid = self.ci.replace(
            "repository: bluetape4k/bluetape4k.github.io",
            "repository: attacker/unreviewed-repository",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("allowlisted external checkout" in error for error in errors),
            errors,
        )

    def test_requires_pull_request_read_permission_for_changed_paths(self) -> None:
        changes_job = verifier._job_block(self.ci, "changes")
        invalid_changes_job = changes_job.replace(
            "    permissions:\n      contents: read\n      pull-requests: read\n",
            "",
            1,
        )
        invalid = self.ci.replace(changes_job, invalid_changes_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertIn("dorny/paths-filter@v4", changes_job)
        self.assertTrue(
            any("pull-requests: read" in error for error in errors), errors
        )

    def test_requires_a_merge_result_contract_job_for_pull_requests(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        invalid = self.ci.replace(merge_job, "", 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("merge-result" in error for error in errors), errors
        )

    def test_merge_result_job_checks_the_synthetic_merge_commit(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        required_contract = (
            "if: ${{ github.event_name == 'pull_request' }}",
            "ref: ${{ github.sha }}",
            'test "$merge_result_sha" = "$GITHUB_SHA"',
            'git merge-base --is-ancestor "$PR_HEAD_SHA" "$merge_result_sha"',
            'git merge-base --is-ancestor "$PR_BASE_SHA" "$merge_result_sha"',
            "python3 -m unittest tests.test_catalog_workflows -v",
        )

        missing = [item for item in required_contract if item not in merge_job]
        self.assertEqual(missing, [])

        status_job = verifier._job_block(self.ci, "ci-status")
        self.assertIn("test-merge-result", status_job)

    def test_rejects_ci_status_that_does_not_require_the_merge_result_job(self) -> None:
        status_job = verifier._job_block(self.ci, "ci-status")
        invalid_status_job = status_job.replace(
            "MERGE_RESULT_RESULT: ${{ needs.test-merge-result.result }}",
            "MERGE_RESULT_RESULT: skipped",
            1,
        )
        invalid = self.ci.replace(status_job, invalid_status_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("require a successful merge-result" in error for error in errors),
            errors,
        )

    def test_product_test_job_checks_out_projects_expected_head(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = "      - uses: actions/checkout@v7\n        with:\n          ref: ${{ env.PROJECTS_EXPECTED_SHA }}"
        self.assertIn(expected_checkout, test_core)

    def test_rejects_product_test_checkout_at_pull_request_merge_sha(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_ref = "ref: ${{ env.PROJECTS_EXPECTED_SHA }}"
        if expected_ref in test_core:
            invalid_test_core = test_core.replace(
                expected_ref,
                "ref: ${{ github.sha }}",
                1,
            )
        else:
            invalid_test_core = test_core.replace(
                "      - uses: actions/checkout@v7\n",
                "      - uses: actions/checkout@v7\n"
                "        with:\n"
                "          ref: ${{ github.sha }}\n",
                1,
            )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_rejects_projects_expected_sha_bound_to_pull_request_merge_commit(self) -> None:
        invalid = self.ci.replace(
            "PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}",
            "PROJECTS_EXPECTED_SHA: ${{ github.sha }}",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Projects PR head" in error for error in errors), errors)

    def test_rejects_merge_sha_even_when_expected_projects_sha_is_in_a_comment(self) -> None:
        invalid = self.ci.replace(
            "  PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}\n",
            "  PROJECTS_EXPECTED_SHA: ${{ github.sha }}\n"
            "  # PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}\n",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Projects PR head" in error for error in errors), errors)

    def test_rejects_merge_result_head_sha_bound_to_github_sha(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        invalid_merge_job = merge_job.replace(
            "PR_HEAD_SHA: ${{ github.event.pull_request.head.sha }}",
            "PR_HEAD_SHA: ${{ github.sha }}",
            1,
        )
        invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("PR_HEAD_SHA" in error for error in errors), errors)

    def test_rejects_merge_result_base_sha_bound_to_github_sha(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        invalid_merge_job = merge_job.replace(
            "PR_BASE_SHA: ${{ github.event.pull_request.base.sha }}",
            "PR_BASE_SHA: ${{ github.sha }}",
            1,
        )
        invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("PR_BASE_SHA" in error for error in errors), errors)

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
