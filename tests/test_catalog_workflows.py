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

    def test_rejects_duplicate_projects_expected_sha_definitions(self) -> None:
        expected = "  PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}\n"
        invalid = self.ci.replace(
            expected,
            expected + "  PROJECTS_EXPECTED_SHA: ${{ github.sha }}\n",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "exactly one top-level PROJECTS_EXPECTED_SHA" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_quoted_projects_expected_sha_keys(self) -> None:
        expected = "  PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}\n"
        invalid_duplicate = self.ci.replace(
            expected,
            expected + '  "PROJECTS_EXPECTED_SHA": ${{ github.sha }}\n',
            1,
        )
        test_core = verifier._job_block(self.ci, "test-core")
        invalid_test_core = test_core.replace(
            "  test-core:\n",
            '  test-core:\n    env:\n      "PROJECTS_EXPECTED_SHA": ${{ github.sha }}\n',
            1,
        )
        invalid_override = self.ci.replace(test_core, invalid_test_core, 1)
        invalid_test_core = test_core.replace(
            "  test-core:\n",
            "  test-core:\n"
            "    env:\n"
            "      <<: &merged_environment\n"
            "        PROJECTS_EXPECTED_SHA: ${{ github.sha }}\n",
            1,
        )
        invalid_merge_override = self.ci.replace(test_core, invalid_test_core, 1)

        for description, invalid in (
            ("quoted duplicate", invalid_duplicate),
            ("quoted override", invalid_override),
            ("merged override", invalid_merge_override),
        ):
            with self.subTest(description=description):
                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "PROJECTS_EXPECTED_SHA" in error
                        and ("exactly one" in error or "overridden" in error)
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_job_level_projects_expected_sha_override(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        invalid_test_core = test_core.replace(
            "  test-core:\n",
            "  test-core:\n    env:\n      PROJECTS_EXPECTED_SHA: ${{ github.sha }}\n",
            1,
        )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "PROJECTS_EXPECTED_SHA must not be overridden" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_checkout_step_projects_expected_sha_override(self) -> None:
        expected_ref = "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        invalid = self.ci.replace(
            expected_ref,
            expected_ref
            + "        env:\n"
            + "          PROJECTS_EXPECTED_SHA: ${{ github.sha }}\n",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "PROJECTS_EXPECTED_SHA must not be overridden" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_ancestry_commands_outside_the_named_verification_step(
        self,
    ) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        ancestry_commands = (
            '          git merge-base --is-ancestor "$PR_HEAD_SHA" "$merge_result_sha"\n',
            '          git merge-base --is-ancestor "$PR_BASE_SHA" "$merge_result_sha"\n',
        )
        invalid_verify_step = verify_step
        for command in ancestry_commands:
            invalid_verify_step = invalid_verify_step.replace(command, "", 1)
        disabled_step = (
            "\n      - name: Disabled ancestry check\n"
            "        if: false\n"
            "        run: |\n" + "".join(ancestry_commands)
        )
        invalid_merge_job = merge_job.replace(verify_step, invalid_verify_step, 1)
        invalid_merge_job = invalid_merge_job.replace(
            "      - uses: actions/setup-python@v7\n",
            disabled_step + "      - uses: actions/setup-python@v7\n",
            1,
        )
        invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("named merge-result verification step" in error for error in errors),
            errors,
        )

    def test_rejects_disabled_nonfatal_or_folded_merge_verification(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        invalid_steps = (
            (
                "if: false",
                verify_step.replace(
                    f"      - name: {step_name}\n",
                    f"      - name: {step_name}\n        if: false\n",
                    1,
                ),
            ),
            (
                "continue-on-error: true",
                verify_step.replace(
                    f"      - name: {step_name}\n",
                    f"      - name: {step_name}\n        continue-on-error: true\n",
                    1,
                ),
            ),
            (
                "folded run scalar",
                verify_step.replace("        run: |\n", "        run: >\n", 1),
            ),
        )

        for description, invalid_verify_step in invalid_steps:
            with self.subTest(description=description):
                invalid_merge_job = merge_job.replace(
                    verify_step, invalid_verify_step, 1
                )
                invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "named merge-result verification step" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_fail_open_or_pr_sha_reassignment_shell_commands(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        bypasses = (
            "set +e",
            "trap 'exit 0' ERR",
            "git() { return 0; }",
            'printf -v PR_HEAD_SHA "%s" "$merge_result_sha"',
            'read -r PR_BASE_SHA <<< "$merge_result_sha"',
            "source <(printf 'PR_HEAD_SHA=%s\\n' \"$merge_result_sha\")",
        )

        for bypass in bypasses:
            with self.subTest(bypass=bypass):
                invalid_verify_step = verify_step.replace(
                    "          set -euo pipefail\n",
                    f"          set -euo pipefail\n          {bypass}\n",
                    1,
                )
                invalid_merge_job = merge_job.replace(
                    verify_step, invalid_verify_step, 1
                )
                invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any("exact fail-closed allowlist" in error for error in errors),
                    errors,
                )

    def test_detects_checkout_actions_when_uses_follows_step_name(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        checkout = (
            "      - name: Additional first-party checkout\n"
            "        uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ github.sha }}\n"
        )
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        invalid_test_core = test_core.replace(
            expected_checkout, expected_checkout + checkout, 1
        )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_detects_quoted_checkout_uses_key_after_step_name(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        quoted_checkout = (
            "      - name: Additional quoted checkout\n"
            '        "uses": actions/checkout@v7\n'
            "        with:\n"
            "          ref: ${{ github.sha }}\n"
        )
        invalid_test_core = test_core.replace(
            expected_checkout, expected_checkout + quoted_checkout, 1
        )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("first-party Projects checkout" in error for error in errors),
            errors,
        )

    def test_detects_quoted_checkout_action_values_after_step_name(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )

        for quote in ('"', "'"):
            with self.subTest(quote=quote):
                quoted_checkout = (
                    "      - name: Additional quoted checkout value\n"
                    f"        uses: {quote}actions/checkout@v7{quote}\n"
                    "        with:\n"
                    "          ref: ${{ github.sha }}\n"
                )
                invalid_test_core = test_core.replace(
                    expected_checkout, expected_checkout + quoted_checkout, 1
                )
                invalid = self.ci.replace(test_core, invalid_test_core, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any("first-party Projects checkout" in error for error in errors),
                    errors,
                )

    def test_rejects_escaped_checkout_uses_key(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        projects_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        escaped_checkout = (
            "      - name: Escaped-key checkout\n"
            '        "\\u0075ses": actions/checkout@v7\n'
            "        with:\n"
            "          ref: ${{ github.sha }}\n"
        )
        invalid_test_core = test_core.replace(
            projects_checkout, projects_checkout + escaped_checkout, 1
        )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("escaped YAML mapping keys" in error for error in errors), errors
        )

    def test_rejects_quoted_external_checkout_repository_key(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        projects_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        external_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            '          "repository": attacker/foreign-repository\n'
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        invalid_test_core = test_core.replace(projects_checkout, external_checkout, 1)
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("external checkout" in error for error in errors), errors
        )

    def test_rejects_duplicate_checkout_repository_keys(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        projects_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        duplicate_repository = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          repository: bluetape4k/bluetape4k-projects\n"
            '          "repository": attacker/foreign-repository\n'
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        invalid_test_core = test_core.replace(
            projects_checkout, duplicate_repository, 1
        )
        invalid = self.ci.replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("duplicate repository/ref/path" in error for error in errors),
            errors,
        )

    def test_rejects_duplicate_checkout_action_keys(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        invalid_merge_job = merge_job.replace(
            "      - uses: actions/checkout@v7\n",
            "      - uses: actions/checkout@v7\n"
            "        uses: attacker/untrusted-action@v1\n",
            1,
        )
        invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("duplicate uses keys" in error for error in errors), errors)

    def test_rejects_aliased_checkout_action(self) -> None:
        test_core = verifier._job_block(self.ci, "test-core")
        expected_checkout = (
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
        )
        alias_declaration = "  CHECKOUT_ACTION: &checkout_action actions/checkout@v7\n"
        aliased_checkout = (
            "      - name: Additional aliased checkout\n"
            "        uses: *checkout_action\n"
            "        with:\n"
            "          ref: ${{ github.sha }}\n"
        )
        invalid_test_core = test_core.replace(
            expected_checkout, expected_checkout + aliased_checkout, 1
        )
        invalid = self.ci.replace(
            "  GITLEAKS_VERSION: 'v8.30.1'\n",
            alias_declaration + "  GITLEAKS_VERSION: 'v8.30.1'\n",
            1,
        ).replace(test_core, invalid_test_core, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("YAML anchors and aliases" in error for error in errors), errors
        )

    def test_rejects_shell_environment_and_runner_overrides(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        top_level_env = "  PROJECTS_EXPECTED_SHA: ${{ github.event.pull_request.head.sha || github.sha }}\n"
        invalid_workflows = (
            (
                "top-level BASH_ENV",
                self.ci.replace(
                    top_level_env,
                    "  BASH_ENV: /tmp/fail-open.sh\n" + top_level_env,
                    1,
                ),
            ),
            (
                "job PATH override",
                self.ci.replace(
                    merge_job,
                    merge_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      PATH: /tmp/attacker-bin\n    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "step BASH_ENV override",
                self.ci.replace(
                    merge_job,
                    merge_job.replace(
                        verify_step,
                        verify_step.replace(
                            "          PR_BASE_SHA: ${{ github.event.pull_request.base.sha }}\n",
                            "          PR_BASE_SHA: ${{ github.event.pull_request.base.sha }}\n"
                            "          BASH_ENV: /tmp/fail-open.sh\n",
                            1,
                        ),
                        1,
                    ),
                    1,
                ),
            ),
            (
                "step shell override",
                self.ci.replace(
                    merge_job,
                    merge_job.replace(
                        verify_step,
                        verify_step.replace(
                            f"      - name: {step_name}\n",
                            f"      - name: {step_name}\n        shell: bash -c 'set +e; source \"$1\"' -- {{0}}\n",
                            1,
                        ),
                        1,
                    ),
                    1,
                ),
            ),
            (
                "workflow shell default",
                self.ci.replace(
                    "jobs:\n",
                    "defaults:\n  run:\n    shell: bash {0}\n\njobs:\n",
                    1,
                ),
            ),
            (
                "pre-verification step",
                self.ci.replace(
                    verify_step,
                    "      - name: Prepare shell environment\n"
                    "        run: echo '/tmp/attacker-bin' >> \"$GITHUB_PATH\"\n"
                    + verify_step,
                    1,
                ),
            ),
        )

        for description, invalid in invalid_workflows:
            with self.subTest(description=description):
                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "merge-result execution environment" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_quoted_environment_and_shell_override_keys(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        quoted_job_env = self.ci.replace(
            merge_job,
            merge_job.replace(
                "    runs-on: ubuntu-latest\n",
                '    "env":\n      PATH: /tmp/attacker-bin\n    runs-on: ubuntu-latest\n',
                1,
            ),
            1,
        )
        quoted_step_shell = self.ci.replace(
            merge_job,
            merge_job.replace(
                verify_step,
                verify_step.replace(
                    f"      - name: {step_name}\n",
                    f'      - name: {step_name}\n        "shell": bash -c \'set +e; source "$1"\' -- {{0}}\n',
                    1,
                ),
                1,
            ),
            1,
        )
        quoted_workflow_shell = self.ci.replace(
            "jobs:\n",
            '"defaults":\n  run:\n    "shell": bash {0}\n\njobs:\n',
            1,
        )

        for description, invalid in (
            ("quoted job env", quoted_job_env),
            ("quoted step shell", quoted_step_shell),
            ("quoted workflow shell default", quoted_workflow_shell),
        ):
            with self.subTest(description=description):
                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "merge-result execution environment" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_ambiguous_or_tolerated_merge_result_job(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        current_condition = "    if: ${{ github.event_name == 'pull_request' }}\n"
        invalid_jobs = (
            (
                "duplicate job condition",
                merge_job.replace(
                    current_condition,
                    current_condition + "    if: false\n",
                    1,
                ),
                "one exact pull_request condition",
            ),
            (
                "continue-on-error",
                merge_job.replace(
                    "    runs-on: ubuntu-latest\n",
                    "    continue-on-error: true\n    runs-on: ubuntu-latest\n",
                    1,
                ),
                "must not use continue-on-error",
            ),
        )

        for description, invalid_merge_job, expected_error in invalid_jobs:
            with self.subTest(description=description):
                invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(expected_error in error for error in errors),
                    errors,
                )

    def test_rejects_pr_sha_shell_reassignment(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)
        assignments = (
            'PR_HEAD_SHA="$merge_result_sha"',
            'export PR_BASE_SHA="$merge_result_sha"',
        )

        for assignment in assignments:
            with self.subTest(assignment=assignment):
                invalid_verify_step = verify_step.replace(
                    "          set -euo pipefail\n",
                    f"          set -euo pipefail\n          {assignment}\n",
                    1,
                )
                invalid_merge_job = merge_job.replace(
                    verify_step, invalid_verify_step, 1
                )
                invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "PR_HEAD_SHA and PR_BASE_SHA must not be reassigned" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_pr_sha_writes_to_github_env(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        step_name = "Verify synthetic merge result contains the current PR commits"
        verify_step = verifier._named_step_block(merge_job, step_name)

        for variable in ("PR_HEAD_SHA", "PR_BASE_SHA"):
            with self.subTest(variable=variable):
                assignment = f'echo "{variable}=$merge_result_sha" >> "$GITHUB_ENV"'
                invalid_verify_step = verify_step.replace(
                    "          set -euo pipefail\n",
                    f"          set -euo pipefail\n          {assignment}\n",
                    1,
                )
                invalid_merge_job = merge_job.replace(
                    verify_step, invalid_verify_step, 1
                )
                invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any(
                        "must not write PR SHA values to GITHUB_ENV" in error
                        for error in errors
                    ),
                    errors,
                )

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

    def test_rejects_escaped_checkout_action_value_with_compensating_checkout(
        self,
    ) -> None:
        invalid = self.ci.replace(
            "      - uses: actions/checkout@v7\n",
            '      - uses: "actions/checkout\\u0040v7"\n',
            1,
        )
        status_job = verifier._job_block(invalid, "ci-status")
        invalid_status_job = status_job.replace(
            "      - name: Check all jobs\n",
            "      - uses: actions/checkout@v7\n"
            "        with:\n"
            "          ref: ${{ env.PROJECTS_EXPECTED_SHA }}\n"
            "      - name: Check all jobs\n",
            1,
        )
        invalid = invalid.replace(status_job, invalid_status_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("escaped YAML scalars" in error for error in errors), errors
        )

    def test_rejects_flow_style_defaults_that_override_run_shell(self) -> None:
        invalid = self.ci.replace(
            "jobs:\n",
            "defaults: {run: {shell: 'bash -c \"true\" -- {0}'}}\n\njobs:\n",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("defaults" in error for error in errors), errors)

    def test_rejects_commented_out_ci_status_governance_gate(self) -> None:
        invalid = self.ci.replace(
            '          if [[ "$CATALOG_GOVERNANCE_RESULT" != "success" ]]; then\n'
            '            echo "Catalog governance must succeed; got $CATALOG_GOVERNANCE_RESULT"\n'
            "            exit 1\n"
            "          fi",
            '          # if [[ "$CATALOG_GOVERNANCE_RESULT" != "success" ]]; then\n'
            '          #   echo "Catalog governance must succeed; got $CATALOG_GOVERNANCE_RESULT"\n'
            "          #   exit 1\n"
            "          # fi",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "ci-status must fail unless catalog-governance" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_ci_status_job_condition_that_skips_pull_requests(self) -> None:
        status_job = verifier._job_block(self.ci, "ci-status")
        invalid_status_job = status_job.replace(
            "    if: always()",
            "    if: ${{ github.event_name == 'workflow_dispatch' }}",
            1,
        )
        invalid = self.ci.replace(status_job, invalid_status_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("ci-status must run for every event" in error for error in errors),
            errors,
        )

    def test_rejects_submission_before_sha_verification_with_comment_decoys(
        self,
    ) -> None:
        submission_job = verifier._job_block(self.submission, "dependency-submission")
        verification_step = verifier._named_step_block(
            submission_job, "Verify dependency submission commit"
        )
        graph_step = verifier._named_step_block(
            submission_job, "Generate and submit dependency graph"
        )
        invalid_job = submission_job.replace(graph_step, "", 1).replace(
            verification_step,
            '      # test "$CHECKOUT_SHA" = "$GITHUB_SHA"\n'
            "      # submission_graph_commit_sha=$CHECKOUT_SHA\n"
            f"{graph_step}\n"
            f"{verification_step}",
            1,
        )
        invalid = self.submission.replace(submission_job, invalid_job, 1)

        errors = verifier.validate_workflows(self.ci, invalid)

        self.assertTrue(
            any("steps must preserve checkout" in error for error in errors),
            errors,
        )

    def test_rejects_parser_contract_command_that_is_only_a_comment(self) -> None:
        command = (
            "bundle exec ruby .gradle/catalog-source/bluetape4k-dependencies/"
            "tests/fixtures/project-dependabot-catalog/dependabot-parser-contract.rb"
        )
        invalid = self.ci.replace(f"          {command}", f"          # {command}", 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("parser contract step must execute" in error for error in errors),
            errors,
        )

    def test_rejects_exit_before_catalog_verification_commands(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        catalog_step = verifier._named_step_block(
            governance, "Verify generated catalog and parser contract"
        )
        invalid_step = catalog_step.replace(
            "          set -euo pipefail\n",
            "          set -euo pipefail\n          exit 0\n",
            1,
        )
        invalid_governance = governance.replace(catalog_step, invalid_step, 1)
        invalid = self.ci.replace(governance, invalid_governance, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "catalog verification step must execute its exact script" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_shell_overrides_on_fail_closed_steps(self) -> None:
        def with_custom_shell(step: str) -> str:
            return step.replace(
                "        run: |\n",
                "        shell: bash -c 'exit 0' -- {0}\n        run: |\n",
                1,
            )

        ci_cases = (
            (
                "parser contract",
                "catalog-governance",
                "Run pinned Dependabot Gradle parser contract",
            ),
            (
                "catalog verification",
                "catalog-governance",
                "Verify generated catalog and parser contract",
            ),
            ("ci-status", "ci-status", "Check all jobs"),
        )
        for description, job_name, step_name in ci_cases:
            with self.subTest(step=description):
                job = verifier._job_block(self.ci, job_name)
                step = verifier._named_step_block(job, step_name)
                invalid_job = job.replace(step, with_custom_shell(step), 1)
                invalid_ci = self.ci.replace(job, invalid_job, 1)

                errors = verifier.validate_workflows(invalid_ci, self.submission)

                self.assertTrue(
                    any(
                        "must not override the fail-closed shell" in error
                        for error in errors
                    ),
                    errors,
                )

        submission_job = verifier._job_block(self.submission, "dependency-submission")
        verification_step = verifier._named_step_block(
            submission_job, "Verify dependency submission commit"
        )
        invalid_job = submission_job.replace(
            verification_step, with_custom_shell(verification_step), 1
        )
        invalid_submission = self.submission.replace(submission_job, invalid_job, 1)

        errors = verifier.validate_workflows(self.ci, invalid_submission)

        self.assertTrue(
            any("must not override the fail-closed shell" in error for error in errors),
            errors,
        )

    def test_rejects_job_level_continue_on_error_for_fail_closed_jobs(self) -> None:
        cases = (
            ("CI catalog-governance", self.ci, "catalog-governance", "ci"),
            ("CI ci-status", self.ci, "ci-status", "ci"),
            (
                "dependency-submission",
                self.submission,
                "dependency-submission",
                "submission",
            ),
        )
        for description, source, job_name, workflow in cases:
            with self.subTest(job=description):
                job = verifier._job_block(source, job_name)
                invalid_job = job.replace(
                    f"  {job_name}:\n",
                    f"  {job_name}:\n    continue-on-error: true\n",
                    1,
                )
                invalid_source = source.replace(job, invalid_job, 1)
                if workflow == "ci":
                    invalid_ci, invalid_submission = invalid_source, self.submission
                else:
                    invalid_ci, invalid_submission = self.ci, invalid_source

                errors = verifier.validate_workflows(invalid_ci, invalid_submission)

                self.assertTrue(
                    any(
                        f"{job_name} must not use job-level continue-on-error" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_ci_status_gate_environment_overrides_and_duplicates(self) -> None:
        status_job = verifier._job_block(self.ci, "ci-status")
        status_step = verifier._named_step_block(status_job, "Check all jobs")
        mutations = (
            (
                "RESULTS override",
                "          RESULTS: ${{ join(needs.*.result, ',') }}\n",
                "          RESULTS: success\n",
            ),
            (
                "catalog result override",
                "          CATALOG_GOVERNANCE_RESULT: ${{ needs.catalog-governance.result }}\n",
                "          CATALOG_GOVERNANCE_RESULT: success\n",
            ),
            (
                "event name override",
                "          EVENT_NAME: ${{ github.event_name }}\n",
                "          EVENT_NAME: push\n",
            ),
            (
                "duplicate event name",
                "          EVENT_NAME: ${{ github.event_name }}\n",
                (
                    "          EVENT_NAME: ${{ github.event_name }}\n"
                    "          EVENT_NAME: push\n"
                ),
            ),
        )

        for description, expected, replacement in mutations:
            with self.subTest(mutation=description):
                invalid_step = status_step.replace(expected, replacement, 1)
                invalid_job = status_job.replace(status_step, invalid_step, 1)
                invalid_ci = self.ci.replace(status_job, invalid_job, 1)

                errors = verifier.validate_workflows(invalid_ci, self.submission)

                self.assertTrue(
                    any(
                        "Check all jobs step must use the exact gate environment"
                        in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_dependency_submission_workflow_shell_defaults(self) -> None:
        invalid = (
            "defaults:\n"
            "  run:\n"
            "    shell: bash -c 'exit 0' -- {0}\n\n" + self.submission
        )

        errors = verifier.validate_workflows(self.ci, invalid)

        self.assertTrue(any("defaults" in error for error in errors), errors)

    def test_rejects_dependabot_pin_step_shell_and_script_overrides(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        step_name = "Read pinned Dependabot Core commit"
        pin_step = verifier._named_step_block(governance, step_name)
        invalid_steps = (
            (
                "custom shell",
                pin_step.replace(
                    "        shell: bash\n",
                    "        shell: bash -c 'exit 0' -- {0}\n",
                    1,
                ),
            ),
            (
                "working-directory override",
                pin_step.replace(
                    "        shell: bash\n",
                    "        working-directory: injected\n        shell: bash\n",
                    1,
                ),
            ),
            (
                "attacker output before pin parser",
                pin_step.replace(
                    "        run: |\n",
                    '        run: |\n          echo "commit=attacker" >> "$GITHUB_OUTPUT"\n',
                    1,
                ),
            ),
            (
                "early successful exit",
                pin_step.replace(
                    "        run: |\n",
                    "        run: |\n          exit 0\n",
                    1,
                ),
            ),
            (
                "conditional step",
                pin_step.replace(
                    f"      - name: {step_name}\n",
                    f"      - name: {step_name}\n        if: false\n",
                    1,
                ),
            ),
            (
                "step BASH_ENV override",
                pin_step.replace(
                    "        run: |\n",
                    "        env:\n          BASH_ENV: /tmp/fail-open.sh\n"
                    "        run: |\n",
                    1,
                ),
            ),
            (
                "duplicate output id",
                pin_step.replace(
                    "        id: dependabot-pin\n",
                    "        id: dependabot-pin\n        id: attacker\n",
                    1,
                ),
            ),
        )

        for description, invalid_step in invalid_steps:
            with self.subTest(mutation=description):
                invalid_job = governance.replace(pin_step, invalid_step, 1)
                invalid_ci = self.ci.replace(governance, invalid_job, 1)

                errors = verifier.validate_workflows(invalid_ci, self.submission)

                self.assertTrue(
                    any(
                        "pinned Dependabot Core commit step" in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_unsafe_environment_overrides_on_protected_workflows(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        status_job = verifier._job_block(self.ci, "ci-status")
        submission_job = verifier._job_block(self.submission, "dependency-submission")
        verification_step = verifier._named_step_block(
            submission_job, "Verify dependency submission commit"
        )
        invalid_workflows = (
            (
                "CI workflow BASH_ENV",
                self.ci.replace(
                    "  JAVA_VERSION: '25'\n",
                    "  \"BASH_ENV\": /tmp/fail-open.sh\n  JAVA_VERSION: '25'\n",
                    1,
                ),
                self.submission,
            ),
            (
                "CI workflow RUBYOPT",
                self.ci.replace(
                    "  JAVA_VERSION: '25'\n",
                    "  RUBYOPT: '-r./.github/evil'\n  JAVA_VERSION: '25'\n",
                    1,
                ),
                self.submission,
            ),
            (
                "CI workflow BUNDLE_GEMFILE",
                self.ci.replace(
                    "  JAVA_VERSION: '25'\n",
                    "  BUNDLE_GEMFILE: ${{ github.workspace }}/Gemfile\n"
                    "  JAVA_VERSION: '25'\n",
                    1,
                ),
                self.submission,
            ),
            (
                "catalog-governance job BUNDLE_PATH",
                self.ci.replace(
                    governance,
                    governance.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      BUNDLE_PATH: /tmp/attacker-gems\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
                self.submission,
            ),
            (
                "catalog-governance job GIT_CONFIG_GLOBAL",
                self.ci.replace(
                    governance,
                    governance.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      GIT_CONFIG_GLOBAL: /tmp/attacker.gitconfig\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
                self.submission,
            ),
            (
                "ci-status job PATH",
                self.ci.replace(
                    status_job,
                    status_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      PATH: /tmp/attacker-bin\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
                self.submission,
            ),
            (
                "ci-status inline flow PATH",
                self.ci.replace(
                    status_job,
                    status_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env: { 'PATH': /tmp/attacker-bin }\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
                self.submission,
            ),
            (
                "Dependency Submission workflow ENV",
                self.ci,
                self.submission.replace(
                    "  JAVA_VERSION: '25'\n",
                    "  ENV: /tmp/fail-open.sh\n  JAVA_VERSION: '25'\n",
                    1,
                ),
            ),
            (
                "Dependency Submission job BASH_ENV",
                self.ci,
                self.submission.replace(
                    submission_job,
                    submission_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      BASH_ENV: /tmp/fail-open.sh\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "Dependency Submission job PYTHONPATH",
                self.ci,
                self.submission.replace(
                    submission_job,
                    submission_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    env:\n      PYTHONPATH: /tmp/attacker-modules\n"
                        "    runs-on: ubuntu-latest\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "Dependency Submission step PATH",
                self.ci,
                self.submission.replace(
                    verification_step,
                    verification_step.replace(
                        "          EXPECTED_SHA: ${{ github.sha }}\n",
                        "          EXPECTED_SHA: ${{ github.sha }}\n"
                        "          PATH: /tmp/attacker-bin\n",
                        1,
                    ),
                    1,
                ),
            ),
        )

        for description, invalid_ci, invalid_submission in invalid_workflows:
            with self.subTest(scope=description):
                errors = verifier.validate_workflows(invalid_ci, invalid_submission)

                self.assertTrue(
                    any(
                        (
                            "BUNDLE_GEMFILE only in the pinned parser contract"
                            if description == "CI workflow BUNDLE_GEMFILE"
                            else "unsafe execution environment variable"
                        )
                        in error
                        for error in errors
                    ),
                    errors,
                )

    def test_rejects_environment_aliases_in_dependency_submission(self) -> None:
        invalid_submission = self.submission.replace(
            "  dependency-submission:\n",
            "  env-source:\n"
            "    runs-on: ubuntu-latest\n"
            "    env: &unsafe_env\n"
            "      BASH_ENV: /tmp/fail-open.sh\n"
            "    steps:\n"
            "      - run: echo no-op\n"
            "  dependency-submission:\n"
            "    env: *unsafe_env\n",
            1,
        )

        errors = verifier.validate_workflows(self.ci, invalid_submission)

        self.assertTrue(any("environment mapping" in error for error in errors), errors)

    def test_rejects_environment_aliases_in_ci(self) -> None:
        invalid_ci = self.ci.replace(
            "  catalog-governance:\n",
            "  env-source:\n"
            "    runs-on: ubuntu-latest\n"
            "    env: &unused_env\n"
            "      CATALOG_CACHE_DIR: /tmp/catalog-cache\n"
            "    steps:\n"
            "      - run: echo no-op\n"
            "  catalog-governance:\n",
            1,
        )

        errors = verifier.validate_workflows(invalid_ci, self.submission)

        self.assertTrue(any("environment mapping" in error for error in errors), errors)

    def test_pins_bundle_gemfile_to_dependabot_parser_checkout(self) -> None:
        invalid_ci = self.ci.replace(
            "          BUNDLE_GEMFILE: ${{ github.workspace }}/.gradle/dependabot-core/Gemfile\n",
            "          BUNDLE_GEMFILE: ${{ github.workspace }}/Gemfile\n",
            1,
        )

        errors = verifier.validate_workflows(invalid_ci, self.submission)

        self.assertTrue(
            any("parser contract step environment" in error for error in errors),
            errors,
        )


if __name__ == "__main__":
    unittest.main()
