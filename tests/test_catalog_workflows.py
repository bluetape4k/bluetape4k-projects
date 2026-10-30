from __future__ import annotations

import importlib.util
import subprocess
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
        self.nightly = (
            ROOT / ".github" / "workflows" / "nightly-tests.yml"
        ).read_text()
        self.submission = (
            ROOT / ".github" / "workflows" / "dependency-submission.yml"
        ).read_text()
        self.security = (ROOT / ".github" / "workflows" / "security.yml").read_text()
        self.image_gate_manifest = (
            ROOT / "scripts" / "testcontainers_image_gate_manifest.json"
        ).read_text()

    def test_repository_workflows_satisfy_catalog_contract(self) -> None:
        self.assertEqual(verifier.validate_workflows(self.ci, self.submission), [])

    def test_projects_ci_pins_catalog_tag_to_peeled_commit(self) -> None:
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: '9bd4fa16e6d64677641b1babf4629ae5b4ef6d80'",
            self.ci,
        )
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG: 'catalog/2026-10-22-01'",
            self.ci,
        )
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: '9bd4fa16e6d64677641b1babf4629ae5b4ef6d80'",
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
            '.orElse("9bd4fa16e6d64677641b1babf4629ae5b4ef6d80")',
            settings,
        )

    def test_projects_ci_pins_exact_catalog_tag_object(self) -> None:
        self.assertIn(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA: 'eb4f1aca48229ca74bd9968e310ca0f8bfe0d43b'",
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

    def test_dependabot_parser_requires_the_approved_core_sha(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")

        self.assertIn(
            "BLUETAPE4K_DEPENDABOT_CORE_SHA: 'de71a59f11cc8373d39bbf5b7af5e8b73f45ca0a'",
            self.ci,
        )
        self.assertIn('import os', governance)
        self.assertIn(
            'expected = os.environ["BLUETAPE4K_DEPENDABOT_CORE_SHA"]',
            governance,
        )
        self.assertIn("if commit != expected:", governance)
        self.assertIn(
            "ref: ${{ env.BLUETAPE4K_DEPENDABOT_CORE_SHA }}",
            governance,
        )
        self.assertNotIn(
            "ref: ${{ steps.dependabot-pin.outputs.commit }}",
            governance,
        )

    def test_dependabot_parser_does_not_write_shared_gradle_or_bundler_cache(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")

        self.assertIn("cache-read-only: true", governance)
        self.assertIn("bundler-cache: false", governance)
        self.assertIn("BUNDLE_PATH: ${{ runner.temp }}/dependabot-bundle", governance)
        self.assertNotIn("bundler-cache: true", governance)

    def test_rejects_dynamic_dependabot_checkout_ref(self) -> None:
        invalid = self.ci.replace(
            "ref: ${{ env.BLUETAPE4K_DEPENDABOT_CORE_SHA }}",
            "ref: ${{ steps.dependabot-pin.outputs.commit }}",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "Dependabot Core" in error or "external checkout" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_writable_dependabot_parser_caches(self) -> None:
        invalid = self.ci.replace(
            "cache-read-only: true",
            "cache-read-only: false",
            1,
        ).replace(
            "bundler-cache: false",
            "bundler-cache: true",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Gradle cache must be read-only" in error for error in errors), errors)
        self.assertTrue(
            any("shared Bundler cache" in error for error in errors), errors
        )

    def test_pull_request_trigger_cannot_skip_required_status_checks(self) -> None:
        invalid = self.ci.replace(
            "  pull_request:\n    branches: [develop, main]\n",
            "  pull_request:\n    branches: [develop, main]\n    paths-ignore:\n      - 'docs/**'\n",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("path filters" in error for error in errors), errors)

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
        for diagnostic in (
            'echo "ImageOS=${ImageOS:-<unset>}"',
            'echo "ImageVersion=${ImageVersion:-<unset>}"',
            "cat /etc/os-release",
            "uname -r",
        ):
            with self.subTest(diagnostic=diagnostic):
                self.assertIn(diagnostic, data_job)
                self.assertIn(diagnostic, spring_boot_job)

    def test_rejects_latest_runner_for_mongodb_container_tests(self) -> None:
        invalid = self.ci.replace(
            "runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("MongoDB container tests" in error for error in errors), errors
        )

    def test_rejects_latest_runner_for_data_container_tests(self) -> None:
        data_job = verifier._job_block(self.ci, "test-data")
        invalid_data_job = data_job.replace(
            "runs-on: ubuntu-22.04",
            "runs-on: ubuntu-latest",
            1,
        )
        invalid = self.ci.replace(data_job, invalid_data_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Test/Data" in error for error in errors), errors)

    def test_rejects_comment_decoy_for_data_runner(self) -> None:
        data_job = verifier._job_block(self.ci, "test-data")
        invalid_data_job = data_job.replace(
            "    runs-on: ubuntu-22.04",
            "    runs-on: ubuntu-latest\n    # expected: runs-on: ubuntu-22.04",
            1,
        )
        invalid = self.ci.replace(data_job, invalid_data_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("Test/Data" in error for error in errors), errors)

    def test_rejects_comment_decoy_for_spring_boot_runner(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        expected_runner = (
            "    runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' "
            "|| 'ubuntu-latest' }}"
        )
        invalid_spring_boot_job = spring_boot_job.replace(
            expected_runner,
            "    runs-on: ubuntu-latest\n    # expected: " + expected_runner.strip(),
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("Spring Boot batch-nosql" in error for error in errors), errors
        )

    def test_rejects_mongodb_test_task_outside_batch_nosql(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        moved_mongodb_test = spring_boot_job.replace(
            "              :bluetape4k-spring-boot-mongodb:test",
            "              :bluetape4k-spring-boot-cassandra:test",
            1,
        ).replace(
            "          - group: demos\n            test_tasks: >-\n"
            "              :bluetape4k-examples-cassandra-demo:test",
            "          - group: demos\n            test_tasks: >-\n"
            "              :bluetape4k-spring-boot-mongodb:test",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, moved_mongodb_test, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("MongoDB test task" in error for error in errors), errors)

    def test_mongodb_matrix_test_tasks_reject_gradle_skip_options(self) -> None:
        cases = (
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                "batch-nosql",
                ":bluetape4k-spring-boot-mongodb:test",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Test/Data",
                self.nightly,
                "test-data",
                "nosql",
                ":bluetape4k-mongodb:test",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                "batch-nosql",
                ":bluetape4k-spring-boot-mongodb:test",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        for label, workflow, job_name, group_name, mongodb_task, validate in cases:
            job = verifier._job_block(workflow, job_name)
            group = dict(verifier._matrix_group_blocks(job))[group_name]
            for option in (
                f"-x {mongodb_task}",
                f"--exclude-task={mongodb_task}",
                "--dry-run",
            ):
                with self.subTest(job=label, option=option):
                    invalid_group = group.replace(
                        f"              {mongodb_task}\n",
                        f"              {mongodb_task}\n              {option}\n",
                        1,
                    )
                    self.assertNotEqual(group, invalid_group)
                    invalid_job = job.replace(group, invalid_group, 1)
                    invalid_workflow = workflow.replace(job, invalid_job, 1)

                    errors = validate(invalid_workflow)

                    self.assertTrue(
                        any(
                            "test_tasks must contain only Gradle task paths" in error
                            for error in errors
                        ),
                        errors,
                    )

    def test_rejects_spring_boot_test_command_that_bypasses_matrix_tasks(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        invalid_spring_boot_job = spring_boot_job.replace(
            '            read -ra tasks <<< "$TEST_TASKS"\n'
            '            ./gradlew "${tasks[@]}" --max-workers=2 --no-configuration-cache',
            "            ./gradlew :bluetape4k-spring-boot-mongodb:test "
            "--max-workers=2 --no-configuration-cache",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("Spring Boot MongoDB test step" in error for error in errors), errors
        )

    def test_rejects_spring_boot_test_tasks_mapped_to_kover_matrix(self) -> None:
        invalid = self.ci.replace(
            "          TEST_TASKS: ${{ matrix.test_tasks }}",
            "          TEST_TASKS: ${{ matrix.kover_tasks }}",
            1,
        )

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("Spring Boot MongoDB test step" in error for error in errors), errors
        )

    def test_requires_ci_data_test_step_to_execute_the_exact_task_list(self) -> None:
        data_job = verifier._job_block(self.ci, "test-data")
        step_name = "Test data modules"
        step = verifier._named_step_block(data_job, step_name)
        self.assertIsNotNone(step)
        assert step is not None
        invalid_steps = (
            ("missing", ""),
            ("renamed", step.replace(step_name, "Run database tests", 1)),
            (
                "MongoDB task replaced",
                step.replace(":bluetape4k-mongodb:test", ":bluetape4k-jdbc:test", 1),
            ),
            (
                "retry action replaced",
                step.replace("nick-fields/retry@v4", "actions/upload-artifact@v7", 1),
            ),
        )

        for description, invalid_step in invalid_steps:
            with self.subTest(description=description):
                invalid_job = data_job.replace(step, invalid_step, 1)
                invalid = self.ci.replace(data_job, invalid_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any("Test/Data MongoDB test step" in error for error in errors),
                    errors,
                )

    def test_rejects_mongodb_test_invocation_in_demos_step(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        extra_step = (
            "      - name: Run MongoDB test in demos group\n"
            "        if: ${{ matrix.group == 'demos' }}\n"
            "        run: ./gradlew :bluetape4k-spring-boot-mongodb:test"
        )
        invalid_spring_boot_job = spring_boot_job.replace(
            "      - name: Generate Kover XML report",
            extra_step + "\n\n      - name: Generate Kover XML report",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("MongoDB test invocation" in error for error in errors), errors
        )

    def test_rejects_mongodb_test_invocation_in_folded_demos_run_script(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        extra_step = (
            "      - name: Run folded MongoDB test in demos group\n"
            "        if: ${{ matrix.group == 'demos' }}\n"
            "        run: >- # folded YAML script\n"
            "          echo running MongoDB\n"
            "          ./gradlew :bluetape4k-spring-boot-mongodb:test"
        )
        invalid_spring_boot_job = spring_boot_job.replace(
            "      - name: Generate Kover XML report",
            extra_step + "\n\n      - name: Generate Kover XML report",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("MongoDB test invocation" in error for error in errors), errors
        )

    def test_rejects_mongodb_test_invocation_in_folded_retry_command(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        extra_step = (
            "      - name: Retry a MongoDB test in demos group\n"
            "        if: ${{ matrix.group == 'demos' }}\n"
            "        uses: nick-fields/retry@v4\n"
            "        with:\n"
            "          timeout_minutes: 10\n"
            "          command: >- # folded retry script\n"
            "            echo running MongoDB\n"
            "            ./gradlew :bluetape4k-spring-boot-mongodb:test"
        )
        invalid_spring_boot_job = spring_boot_job.replace(
            "      - name: Generate Kover XML report",
            extra_step + "\n\n      - name: Generate Kover XML report",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("MongoDB test invocation" in error for error in errors), errors
        )

    def test_detects_folded_mongodb_commands_after_a_blank_line(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        for script_key, header in (
            ("run", "      - name: Run folded MongoDB test in demos group\n"),
            (
                "command",
                (
                    "      - name: Retry folded MongoDB test in demos group\n"
                    "        uses: nick-fields/retry@v4\n"
                    "        with:\n"
                    "          timeout_minutes: 10\n"
                ),
            ),
        ):
            with self.subTest(script_key=script_key):
                extra_step = (
                    header
                    + "        if: ${{ matrix.group == 'demos' }}\n"
                    + ("        " if script_key == "run" else "          ")
                    + f"{script_key}: >-\n"
                    + ("          " if script_key == "run" else "            ")
                    + "# shell comment\n\n"
                    + ("          " if script_key == "run" else "            ")
                    + "./gradlew :bluetape4k-spring-boot-mongodb:test"
                )
                invalid_job = spring_boot_job.replace(
                    "      - name: Generate Kover XML report",
                    extra_step + "\n\n      - name: Generate Kover XML report",
                    1,
                )
                invalid = self.ci.replace(spring_boot_job, invalid_job, 1)

                errors = verifier.validate_workflows(invalid, self.submission)

                self.assertTrue(
                    any("MongoDB test invocation" in error for error in errors),
                    errors,
                )

    def test_requires_nightly_mongodb_test_steps_to_execute_the_matrix_tasks(
        self,
    ) -> None:
        cases = (
            (
                "Test/Data",
                "test-data",
                "Test data modules (${{ matrix.group }})",
                ":bluetape4k-mongodb:test",
            ),
            (
                "Spring Boot",
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
                ":bluetape4k-spring-boot-mongodb:test",
            ),
        )
        for label, job_name, step_name, mongodb_task in cases:
            job = verifier._job_block(self.nightly, job_name)
            step = verifier._named_step_block(job, step_name)
            self.assertIsNotNone(step)
            assert step is not None
            invalid_steps = (
                ("missing", ""),
                ("renamed", step.replace(step_name, "Run JDBC tests", 1)),
                (
                    "command bypasses matrix",
                    step.replace(
                        '            read -ra tasks <<< "$TEST_TASKS"\n'
                        '            ./gradlew "${tasks[@]}" --max-workers=2 '
                        "--no-configuration-cache",
                        "            ./gradlew :bluetape4k-jdbc:test --max-workers=2 "
                        "--no-configuration-cache",
                        1,
                    ),
                ),
                (
                    "TEST_TASKS bypasses matrix",
                    step.replace(
                        "TEST_TASKS: ${{ matrix.test_tasks }}",
                        "TEST_TASKS: ${{ matrix.kover_tasks }}",
                        1,
                    ),
                ),
            )
            for description, invalid_step in invalid_steps:
                with self.subTest(
                    job=label, task=mongodb_task, description=description
                ):
                    invalid_job = job.replace(step, invalid_step, 1)
                    invalid_nightly = self.nightly.replace(job, invalid_job, 1)

                    errors = verifier.validate_nightly_mongodb_workflow(invalid_nightly)

                    self.assertTrue(
                        any(
                            f"Nightly {label} MongoDB test step" in error
                            for error in errors
                        ),
                        errors,
                    )

    def test_ci_mongodb_test_steps_cannot_be_skipped_or_mask_failures(self) -> None:
        cases = (
            (
                "Test/Data",
                "test-data",
                "Test data modules",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Spring Boot",
                "test-spring-boot",
                "Test Spring Boot modules (${{ matrix.group }})",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
        )
        mutations = (
            ("step if", "if: false"),
            ("step continue-on-error", "continue-on-error: true"),
        )
        for label, job_name, step_name, validate in cases:
            job = verifier._job_block(self.ci, job_name)
            step = verifier._named_step_block(job, step_name)
            self.assertTrue(step)
            for description, guard in mutations:
                with self.subTest(job=label, mutation=description):
                    invalid_step = step.replace(
                        f"      - name: {step_name}\n",
                        f"      - name: {step_name}\n        {guard}\n",
                        1,
                    )
                    invalid_job = job.replace(step, invalid_step, 1)
                    invalid = self.ci.replace(job, invalid_job, 1)

                    errors = validate(invalid)

                    self.assertTrue(
                        any(
                            "skip" in error or "continue-on-error" in error
                            for error in errors
                        ),
                        errors,
                    )

    def test_nightly_mongodb_test_steps_cannot_be_skipped_or_mask_failures(
        self,
    ) -> None:
        cases = (
            (
                "Test/Data",
                "test-data",
                "Test data modules (${{ matrix.group }})",
            ),
            (
                "Spring Boot",
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
            ),
        )
        mutations = (
            ("step if", "if: false"),
            ("step continue-on-error", "continue-on-error: true"),
        )
        for label, job_name, step_name in cases:
            job = verifier._job_block(self.nightly, job_name)
            step = verifier._named_step_block(job, step_name)
            self.assertTrue(step)
            for description, guard in mutations:
                with self.subTest(job=label, mutation=description):
                    invalid_step = step.replace(
                        f"      - name: {step_name}\n",
                        f"      - name: {step_name}\n        {guard}\n",
                        1,
                    )
                    invalid_job = job.replace(step, invalid_step, 1)
                    invalid = self.nightly.replace(job, invalid_job, 1)

                    errors = verifier.validate_nightly_mongodb_workflow(invalid)

                    self.assertTrue(
                        any(
                            "skip" in error or "continue-on-error" in error
                            for error in errors
                        ),
                        errors,
                    )

    def test_mongodb_test_steps_reject_unsafe_execution_environment(self) -> None:
        cases = (
            (
                "CI Test/Data",
                self.ci,
                "test-data",
                "Test data modules",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                "Test Spring Boot modules (${{ matrix.group }})",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Test/Data",
                self.nightly,
                "test-data",
                "Test data modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        for label, workflow, job_name, step_name, validate in cases:
            job = verifier._job_block(workflow, job_name)
            step = verifier._named_step_block(job, step_name)
            for variable in ("BASH_ENV", "NODE_OPTIONS", "PATH"):
                mutations = (
                    (
                        "workflow environment",
                        workflow.replace(
                            "  JAVA_VERSION: '25'\n",
                            "  JAVA_VERSION: '25'\n"
                            f"  {variable}: '/tmp/issue-1839-fail-open'\n",
                            1,
                        ),
                    ),
                    (
                        "job environment",
                        workflow.replace(
                            job,
                            job.replace(
                                "    steps:\n",
                                "    env:\n"
                                f"      {variable}: '/tmp/issue-1839-fail-open'\n"
                                "    steps:\n",
                                1,
                            ),
                            1,
                        ),
                    ),
                    (
                        "test step environment",
                        workflow.replace(
                            job,
                            job.replace(
                                step,
                                step.replace(
                                    "        env:\n",
                                    "        env:\n"
                                    f"          {variable}: '/tmp/issue-1839-fail-open'\n",
                                    1,
                                ),
                                1,
                            ),
                            1,
                        ),
                    ),
                )
                for scope, invalid in mutations:
                    with self.subTest(job=label, variable=variable, scope=scope):
                        self.assertNotEqual(invalid, workflow)

                        errors = validate(invalid)

                        self.assertTrue(
                            any(
                                "unsafe execution environment" in error
                                for error in errors
                            ),
                            errors,
                        )

    def test_mongodb_test_step_environment_is_allowlisted(self) -> None:
        cases = (
            (
                "CI Test/Data",
                self.ci,
                "test-data",
                "Test data modules",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                "Test Spring Boot modules (${{ matrix.group }})",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Test/Data",
                self.nightly,
                "test-data",
                "Test data modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        for label, workflow, job_name, step_name, validate in cases:
            job = verifier._job_block(workflow, job_name)
            step = verifier._named_step_block(job, step_name)
            mutations = (
                (
                    "unexpected variable",
                    step.replace(
                        "        env:\n",
                        "        env:\n          CUSTOM_FLAG: '1'\n",
                        1,
                    ),
                ),
                (
                    "changed Gradle options",
                    step.replace(
                        'GRADLE_OPTS: "-Dorg.gradle.daemon=false"',
                        'GRADLE_OPTS: "-Dorg.gradle.daemon=false -javaagent:/tmp/fail-open.jar"',
                        1,
                    ),
                ),
            )
            for description, invalid_step in mutations:
                with self.subTest(job=label, mutation=description):
                    invalid = workflow.replace(
                        job, job.replace(step, invalid_step, 1), 1
                    )

                    errors = validate(invalid)

                    self.assertTrue(
                        any("environment allowlist" in error for error in errors),
                        errors,
                    )

    def test_mongodb_retry_steps_reject_failure_masking_action_inputs(self) -> None:
        cases = (
            (
                "CI Data",
                self.ci,
                "test-data",
                "Test data modules",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                "Test Spring Boot modules (${{ matrix.group }})",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Data",
                self.nightly,
                "test-data",
                "Test data modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        masking_inputs = (
            ("continue_on_error", "true"),
            ("continue-on-error", "true"),
            ("new_command_on_retry", "true"),
        )

        for label, workflow, job_name, step_name, validate in cases:
            with self.subTest(job=label):
                job = verifier._job_block(workflow, job_name)
                step = verifier._named_step_block(job, step_name)
                self.assertTrue(step)
                for input_name, input_value in masking_inputs:
                    with self.subTest(action_input=input_name):
                        invalid_step = step.replace(
                            "        with:\n",
                            f"        with:\n          {input_name}: {input_value}\n",
                            1,
                        )
                        self.assertNotEqual(invalid_step, step)
                        invalid_job = job.replace(step, invalid_step, 1)
                        invalid_workflow = workflow.replace(job, invalid_job, 1)

                        errors = validate(invalid_workflow)

                        self.assertTrue(
                            any("retry action inputs" in error for error in errors),
                            errors,
                        )

    def test_nightly_rejects_unicode_escaped_mapping_keys(self) -> None:
        cases = (
            (
                "Test/Data step if",
                "test-data",
                "Test data modules (${{ matrix.group }})",
                r'        "\u0069f": false',
            ),
            (
                "Test/Data step continue-on-error",
                "test-data",
                "Test data modules (${{ matrix.group }})",
                r'        "continue\u002don\u002derror": true',
            ),
            (
                "Spring Boot step continue-on-error",
                "test-spring-docker",
                "Test spring-boot modules (${{ matrix.group }})",
                r'        "continue\u002don\u002derror": true',
            ),
            (
                "Test/Data runner diagnostics if",
                "test-data",
                "Report runner image and kernel",
                r'        "\u0069f": false',
            ),
        )
        for label, job_name, step_name, escaped_key in cases:
            with self.subTest(mutation=label):
                job = verifier._job_block(self.nightly, job_name)
                step = verifier._named_step_block(job, step_name)
                self.assertTrue(step)
                step_header = f"      - name: {step_name}\n"
                invalid_step = step.replace(
                    step_header, step_header + escaped_key + "\n", 1
                )
                invalid_job = job.replace(step, invalid_step, 1)
                invalid_nightly = self.nightly.replace(job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(invalid_nightly)

                self.assertTrue(
                    any("escaped YAML mapping keys" in error for error in errors),
                    errors,
                )

    def test_nightly_rejects_unicode_escaped_job_failure_masking(self) -> None:
        for job_name in ("test-data", "test-spring-docker"):
            with self.subTest(job=job_name):
                job = verifier._job_block(self.nightly, job_name)
                invalid_job = job.replace(
                    f"  {job_name}:\n",
                    f'  {job_name}:\n    "continue\\u002don\\u002derror": true\n',
                    1,
                )
                invalid_nightly = self.nightly.replace(job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(invalid_nightly)

                self.assertTrue(
                    any("escaped YAML mapping keys" in error for error in errors),
                    errors,
                )

    def test_mongodb_jobs_keep_execution_guards_and_cannot_mask_failures(
        self,
    ) -> None:
        cases = (
            (
                "CI Test/Data",
                self.ci,
                "test-data",
                "    if: ${{ needs.changes.outputs.data == 'true' || needs.changes.outputs.shared == 'true' || github.event_name == 'workflow_dispatch' }}",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                "    if: ${{ needs.changes.outputs['spring-boot'] == 'true' || needs.changes.outputs.shared == 'true' || github.event_name == 'workflow_dispatch' }}",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Test/Data",
                self.nightly,
                "test-data",
                "    if: ${{ needs.plan.outputs.run_standard == 'true' }}",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                "    if: ${{ needs.plan.outputs.run_standard == 'true' }}",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        for label, workflow, job_name, expected_if, validate in cases:
            job = verifier._job_block(workflow, job_name)
            mutations = (
                (
                    "job if narrows execution",
                    job.replace(expected_if, "    if: false", 1),
                ),
                (
                    "job continue-on-error",
                    job.replace(
                        f"  {job_name}:\n",
                        f"  {job_name}:\n    continue-on-error: true\n",
                        1,
                    ),
                ),
            )
            for description, invalid_job in mutations:
                with self.subTest(job=label, mutation=description):
                    invalid = workflow.replace(job, invalid_job, 1)

                    errors = validate(invalid)

                    self.assertTrue(
                        any(
                            "skip" in error or "continue-on-error" in error
                            for error in errors
                        ),
                        errors,
                    )

    def test_rejects_runner_diagnostics_outside_batch_nosql(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        invalid_spring_boot_job = spring_boot_job.replace(
            "if: ${{ matrix.group == 'batch-nosql' }}",
            "if: ${{ matrix.group == 'demos' }}",
            1,
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("runner diagnostics" in error for error in errors), errors)

    def test_nightly_data_diagnostics_names_the_nosql_group(self) -> None:
        data_job = verifier._job_block(self.nightly, "test-data")
        diagnostics = verifier._named_step_block(
            data_job, "Report runner image and kernel"
        )
        invalid_diagnostics = diagnostics.replace(
            "if: ${{ matrix.group == 'nosql' }}",
            "if: ${{ matrix.group == 'database-core' }}",
            1,
        )
        invalid = self.nightly.replace(
            data_job, data_job.replace(diagnostics, invalid_diagnostics, 1), 1
        )

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any("expected matrix group" in error for error in errors), errors
        )
        self.assertFalse(any("batch-nosql" in error for error in errors), errors)

    def test_nightly_mongodb_jobs_use_supported_runners_and_report_image_kernel(
        self,
    ) -> None:
        errors = verifier.validate_nightly_mongodb_workflow(self.nightly)

        self.assertEqual(errors, [])

    def test_nightly_testcontainers_mongodb_group_uses_supported_runner_and_reports_kernel(
        self,
    ) -> None:
        errors = verifier.validate_nightly_mongodb_workflow(self.nightly)

        self.assertEqual(errors, [])
        job = verifier._job_block(self.nightly, "test-testcontainers")
        diagnostics = verifier._named_step_block(job, "Report runner image and kernel")
        self.assertEqual(
            verifier._job_values(job, "runs-on"),
            [
                "${{ matrix.group == 'storage-cache' && 'ubuntu-22.04' || 'ubuntu-latest' }}"
            ],
        )
        self.assertEqual(
            verifier._step_values(diagnostics, "if"),
            ["${{ matrix.group == 'storage-cache' }}"],
        )

    def test_rejects_unsupported_runner_for_nightly_testcontainers_mongodb_group(
        self,
    ) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers")
        invalid_job = job.replace(
            "runs-on: ${{ matrix.group == 'storage-cache' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )
        invalid = self.nightly.replace(job, invalid_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(any("test-testcontainers" in error for error in errors), errors)

    def test_nightly_storage_cache_testcontainers_diagnostics_fail_closed_before_tests(
        self,
    ) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers")
        diagnostics = verifier._named_step_block(job, "Report runner image and kernel")
        invalid_diagnostics = diagnostics.replace(
            "        shell: bash\n",
            "        continue-on-error: true\n        shell: bash\n",
            1,
        )
        invalid_job = job.replace(diagnostics, invalid_diagnostics, 1)
        invalid = self.nightly.replace(job, invalid_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any("test-testcontainers runner diagnostics" in error for error in errors),
            errors,
        )

    def test_nightly_testcontainers_mongodb_test_cannot_mask_failures(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers")
        test_step = verifier._named_step_block(
            job, "Test testcontainers (${{ matrix.group }})"
        )
        invalid_step = test_step.replace(
            "      - name: Test testcontainers (${{ matrix.group }})\n",
            "      - name: Test testcontainers (${{ matrix.group }})\n"
            "        continue-on-error: true\n",
            1,
        )
        step_mutation = self.nightly.replace(
            job, job.replace(test_step, invalid_step, 1), 1
        )
        job_mutation = self.nightly.replace(
            job,
            job.replace(
                "    needs: [build, plan]\n",
                "    needs: [build, plan]\n    continue-on-error: true\n",
                1,
            ),
            1,
        )

        for invalid in (step_mutation, job_mutation):
            with self.subTest(mutation="failure masking"):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("test-testcontainers" in error for error in errors), errors
                )

    def test_nightly_storage_cache_job_cannot_be_conditionally_skipped(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers")
        invalid_job = job.replace(
            "    needs: [build, plan]\n",
            "    needs: [build, plan]\n    if: ${{ false }}\n",
            1,
        )
        invalid = self.nightly.replace(job, invalid_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any(
                "test-testcontainers job must not be conditionally skipped" in error
                for error in errors
            ),
            errors,
        )

    def test_nightly_mongodb_testcontainers_scopes_reject_unsafe_environment(self):
        mutations = (
            self.nightly.replace(
                "env:\n  JAVA_VERSION:",
                "env:\n  BASH_ENV: /tmp/disable-failure-checks\n  JAVA_VERSION:",
                1,
            ),
            self.nightly.replace(
                "  test-testcontainers:\n    name:",
                "  test-testcontainers:\n    env:\n      PATH: /tmp/masked-bin\n    name:",
                1,
            ),
            self.nightly.replace(
                "  test-testcontainers-image-gate-shard:\n    name:",
                "  test-testcontainers-image-gate-shard:\n"
                "    env:\n      NODE_OPTIONS: --require=/tmp/mask.js\n"
                "    name:",
                1,
            ),
            self.nightly.replace(
                "  test-testcontainers-image-gate:\n    name:",
                "  test-testcontainers-image-gate:\n"
                "    env:\n      BASH_ENV: /tmp/disable-failure-checks\n"
                "    name:",
                1,
            ),
            self.nightly.replace(
                "  plan:\n    name:",
                "  plan:\n"
                "    env:\n      BASH_ENV: /tmp/disable-plan-checks\n"
                "    name:",
                1,
            ),
            self.nightly.replace(
                "  plan:\n    name:",
                "  plan:\n    env:\n      PATH: /tmp/masked-plan-bin\n    name:",
                1,
            ),
            self.nightly.replace(
                "  plan:\n    name:",
                "  plan:\n    env:\n      PYTHONPATH: /tmp/attacker-modules\n    name:",
                1,
            ),
            self.nightly.replace(
                "  nightly-status:\n    name:",
                "  nightly-status:\n"
                "    env:\n      BASH_ENV: /tmp/disable-status-checks\n"
                "    name:",
                1,
            ),
            self.nightly.replace(
                "  nightly-status:\n    name:",
                "  nightly-status:\n"
                "    env:\n      PATH: /tmp/masked-status-bin\n"
                "    name:",
                1,
            ),
            self.nightly.replace(
                "  nightly-status:\n    name:",
                "  nightly-status:\n"
                "    env:\n      PYTHONPATH: /tmp/attacker-modules\n"
                "    name:",
                1,
            ),
        )

        for invalid in mutations:
            with self.subTest(mutation="unsafe execution environment"):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("unsafe execution environment" in error for error in errors),
                    errors,
                )

    def test_nightly_rejects_environment_mapping_aliases_and_merges(self) -> None:
        anchored_nightly = self.nightly.replace(
            "env:\n  JAVA_VERSION: '25'\n",
            "env: &nightly_environment\n  JAVA_VERSION: '25'\n",
            1,
        )
        plan = verifier._job_block(anchored_nightly, "plan")
        status_step = verifier._named_step_block(
            verifier._job_block(anchored_nightly, "nightly-status"), "Check all jobs"
        )
        self.assertTrue(plan)
        self.assertTrue(status_step)

        mutations = (
            (
                "plan job environment alias",
                anchored_nightly.replace(
                    plan,
                    plan.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    runs-on: ubuntu-latest\n    env: *nightly_environment\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "nightly status step environment merge",
                anchored_nightly.replace(
                    status_step,
                    status_step.replace(
                        "        env:\n",
                        "        env:\n          <<: *nightly_environment\n",
                        1,
                    ),
                    1,
                ),
            ),
        )

        for description, invalid in mutations:
            with self.subTest(mutation=description):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("environment mappings" in error for error in errors), errors
                )

    def test_nightly_plan_and_status_environments_are_explicitly_pinned(self) -> None:
        plan = verifier._job_block(self.nightly, "plan")
        plan_step = next(
            step
            for step in verifier._step_blocks(plan)
            if verifier._step_values(step, "id") == ["plan"]
        )
        status_job = verifier._job_block(self.nightly, "nightly-status")
        status_step = verifier._named_step_block(status_job, "Check all jobs")
        mutations = (
            (
                "plan job environment added",
                self.nightly.replace(
                    plan,
                    plan.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    runs-on: ubuntu-latest\n"
                        "    env:\n      UNPINNED_FLAG: true\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "plan step environment added",
                self.nightly.replace(
                    plan,
                    plan.replace(
                        plan_step,
                        plan_step.replace(
                            "      - id: plan\n",
                            "      - id: plan\n"
                            "        env:\n          UNPINNED_FLAG: true\n",
                            1,
                        ),
                        1,
                    ),
                    1,
                ),
            ),
            (
                "nightly status job environment added",
                self.nightly.replace(
                    status_job,
                    status_job.replace(
                        "    runs-on: ubuntu-latest\n",
                        "    runs-on: ubuntu-latest\n"
                        "    env:\n      UNPINNED_FLAG: true\n",
                        1,
                    ),
                    1,
                ),
            ),
            (
                "nightly status step environment expanded",
                self.nightly.replace(
                    status_step,
                    status_step.replace(
                        "          COVERAGE_REPORT_RESULT: ${{ needs.coverage-report.result }}\n",
                        "          COVERAGE_REPORT_RESULT: ${{ needs.coverage-report.result }}\n"
                        "          UNPINNED_FLAG: true\n",
                        1,
                    ),
                    1,
                ),
            ),
        )

        for description, invalid in mutations:
            with self.subTest(mutation=description):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("environment" in error.lower() for error in errors), errors
                )

    def test_rejects_missing_testcontainers_scope_for_storage_cache(self) -> None:
        invalid = self.nightly.replace(
            '"testcontainers": [group["group"] for group in all_groups],',
            '"testcontainers": [],',
            1,
        )

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any("selected by the testcontainers scope" in error for error in errors),
            errors,
        )

    def test_rejects_mongodb_test_pattern_outside_nightly_storage_cache_group(
        self,
    ) -> None:
        invalid = self.nightly.replace(
            '"io.bluetape4k.testcontainers.storage.MongoDBServerTest",',
            '"io.bluetape4k.testcontainers.storage.MariaDBServerTest",',
            1,
        )

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(any("storage-cache" in error for error in errors), errors)

    def test_rejects_comments_that_decoy_storage_cache_plan_contract(self) -> None:
        pattern = '"io.bluetape4k.testcontainers.storage.MongoDBServerTest",'
        scope_mapping = '"full": [group["group"] for group in all_groups],'
        mutations = (
            self.nightly.replace(pattern, f"# {pattern}", 1),
            self.nightly.replace(
                scope_mapping,
                '"full": [\n              # [group["group"] for group in all_groups],\n          ],',
                1,
            ),
        )

        for invalid in mutations:
            with self.subTest(mutation="comment decoy"):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("storage-cache" in error for error in errors), errors
                )

    def test_rejects_mutations_after_validated_testcontainers_plan_assignments(
        self,
    ) -> None:
        selected_assignment = "          selected = set(groups_by_scope[scope])\n"
        mutations = (
            '          selected.discard("storage-cache")\n',
            "          selected.clear()\n",
            '          all_groups.pop(next(index for index, group in enumerate(all_groups) if group["group"] == "storage-cache"))\n',
        )

        for mutation in mutations:
            with self.subTest(mutation=mutation.strip()):
                invalid = self.nightly.replace(
                    selected_assignment,
                    selected_assignment + mutation,
                    1,
                )

                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("storage-cache" in error for error in errors), errors
                )

    def test_testcontainers_plan_matrix_output_is_connected_to_the_job(self) -> None:
        plan_job = verifier._job_block(self.nightly, "plan")
        mutations = (
            (
                "hardcoded matrix output",
                plan_job.replace(
                    'echo "testcontainers_matrix=$matrix_json"',
                    "echo 'testcontainers_matrix={\"include\":[]}'",
                    1,
                ),
            ),
            (
                "matrix overwrite",
                plan_job.replace(
                    '          )"\n\n          {\n',
                    '          )"\n\n          matrix_json=\'{"include":[]}\'\n\n          {\n',
                    1,
                ),
            ),
            (
                "wrong output file",
                plan_job.replace('} >> "$GITHUB_OUTPUT"', '} >> "$GITHUB_ENV"', 1),
            ),
            (
                "wrong matrix job output",
                plan_job.replace(
                    "testcontainers_matrix: ${{ steps.plan.outputs.testcontainers_matrix }}",
                    "testcontainers_matrix: ${{ steps.plan.outputs.scope }}",
                    1,
                ),
            ),
            (
                "wrong scope job output",
                plan_job.replace(
                    "      scope: ${{ steps.plan.outputs.scope }}",
                    "      scope: ${{ steps.plan.outputs.run_smoke }}",
                    1,
                ),
            ),
            (
                "plan job skipped",
                plan_job.replace("  plan:\n", "  plan:\n    if: false\n", 1),
            ),
            (
                "plan job failure masked",
                plan_job.replace(
                    "  plan:\n", "  plan:\n    continue-on-error: true\n", 1
                ),
            ),
            (
                "plan step skipped",
                plan_job.replace(
                    "      - id: plan\n", "      - id: plan\n        if: false\n", 1
                ),
            ),
            (
                "plan step failure masked",
                plan_job.replace(
                    "      - id: plan\n",
                    "      - id: plan\n        continue-on-error: true\n",
                    1,
                ),
            ),
            (
                "plan error guard removed",
                plan_job.replace("          set -euo pipefail\n", "", 1),
            ),
        )

        for name, invalid_job in mutations:
            with self.subTest(mutation=name):
                self.assertNotEqual(invalid_job, plan_job, name)
                invalid = self.nightly.replace(plan_job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(
                    any("Testcontainers plan" in error for error in errors), errors
                )

    def test_testcontainers_plan_rejects_python_function_shadowing(self) -> None:
        plan_job = verifier._job_block(self.nightly, "plan")
        assignment = '          matrix_json="$(SCOPE="$scope" python3 <<\'PY\'\n'
        shadow = (
            "          python3() {\n"
            "            printf '%s\\n' '{\"include\":[]}'\n"
            "          }\n\n"
        )
        invalid_job = plan_job.replace(assignment, shadow + assignment, 1)
        self.assertNotEqual(invalid_job, plan_job)

        invalid = self.nightly.replace(plan_job, invalid_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )

        self.assertTrue(any("plan shell prelude" in error for error in errors), errors)

    def test_nightly_plan_job_outputs_match_the_validated_script_values(self) -> None:
        mappings = (
            ("scope", "scope", "run_smoke"),
            ("run_smoke", "run_smoke", "run_standard"),
            ("run_standard", "run_standard", "run_smoke"),
            (
                "run_testcontainers_coverage",
                "run_testcontainers_coverage",
                "run_standard",
            ),
            (
                "testcontainers_matrix",
                "testcontainers_matrix",
                "scope",
            ),
        )
        for output_name, source_name, wrong_source in mappings:
            with self.subTest(output=output_name):
                valid_mapping = (
                    f"      {output_name}: ${{{{ steps.plan.outputs.{source_name} }}}}"
                )
                invalid_mapping = (
                    f"      {output_name}: ${{{{ steps.plan.outputs.{wrong_source} }}}}"
                )
                invalid = self.nightly.replace(valid_mapping, invalid_mapping, 1)
                self.assertNotEqual(invalid, self.nightly)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(any("plan output" in error for error in errors), errors)

    def test_nightly_status_script_rejects_skipped_required_jobs_for_each_scope(
        self,
    ) -> None:
        status_job = verifier._job_block(self.nightly, "nightly-status")
        status_step = verifier._named_step_block(status_job, "Check all jobs")
        script = verifier._step_run_script(status_step)
        self.assertIsNotNone(script)

        cases = (
            (
                "full standard job skipped",
                {
                    "EXPECTED_SCOPE": "full",
                    "PLAN_RESULT": "success",
                    "RESULTS": "success,skipped,success",
                    "SCOPE": "full",
                    "TEST_DATA_RESULT": "success",
                    "TEST_SPRING_DOCKER_RESULT": "success",
                    "TESTCONTAINERS_RESULT": "success",
                    "TESTCONTAINERS_SPRING_RESULT": "success",
                    "X64_GATE_RESULT": "success",
                    "COVERAGE_REPORT_RESULT": "success",
                },
            ),
            (
                "testcontainers bridge skipped",
                {
                    "EXPECTED_SCOPE": "testcontainers",
                    "PLAN_RESULT": "success",
                    "RESULTS": "success,skipped,success",
                    "SCOPE": "testcontainers",
                    "TEST_DATA_RESULT": "success",
                    "TEST_SPRING_DOCKER_RESULT": "success",
                    "TESTCONTAINERS_RESULT": "success",
                    "TESTCONTAINERS_SPRING_RESULT": "skipped",
                    "X64_GATE_RESULT": "skipped",
                    "COVERAGE_REPORT_RESULT": "success",
                },
            ),
        )
        for label, environment in cases:
            with self.subTest(scope=label):
                result = subprocess.run(
                    ["bash", "-c", "\n".join(script or [])],
                    check=False,
                    capture_output=True,
                    env=environment,
                    text=True,
                )
                self.assertNotEqual(result.returncode, 0, result.stdout)

    def test_nightly_status_requires_testcontainers_success_for_its_scope(self) -> None:
        status_job = verifier._job_block(self.nightly, "nightly-status")
        mutations = (
            (
                "Testcontainers dependency removed",
                status_job.replace("      - test-testcontainers\n", "", 1),
            ),
            (
                "plan dependency removed",
                status_job.replace("      - plan\n", "", 1),
            ),
            (
                "data dependency removed",
                status_job.replace("      - test-data\n", "", 1),
            ),
            (
                "Spring Docker dependency removed",
                status_job.replace("      - test-spring-docker\n", "", 1),
            ),
            (
                "Testcontainers Spring dependency removed",
                status_job.replace("      - test-testcontainers-spring\n", "", 1),
            ),
            (
                "coverage report dependency removed",
                status_job.replace("      - coverage-report\n", "", 1),
            ),
            (
                "Testcontainers result removed",
                status_job.replace(
                    "          TESTCONTAINERS_RESULT: ${{ needs.test-testcontainers.result }}\n",
                    "",
                    1,
                ),
            ),
            (
                "Testcontainers Spring result removed",
                status_job.replace(
                    "          TESTCONTAINERS_SPRING_RESULT: ${{ needs.test-testcontainers-spring.result }}\n",
                    "",
                    1,
                ),
            ),
            (
                "plan result removed",
                status_job.replace(
                    "          PLAN_RESULT: ${{ needs.plan.result }}\n", "", 1
                ),
            ),
            (
                "expected scope changed",
                status_job.replace(
                    "          EXPECTED_SCOPE: ${{ github.event_name == 'workflow_dispatch' && inputs.scope || github.event.schedule == '0 18 * * 0' && 'full' || 'smoke' }}\n",
                    "          EXPECTED_SCOPE: ${{ inputs.scope }}\n",
                    1,
                ),
            ),
            (
                "scope guard weakened",
                status_job.replace(
                    'if [ "$TESTCONTAINERS_RESULT" != success ]; then',
                    'if [ "$SCOPE" = full ] && [ "$TESTCONTAINERS_RESULT" != success ]; then',
                    1,
                ),
            ),
            (
                "Testcontainers skip allowed",
                status_job.replace(
                    'if [ "$TESTCONTAINERS_RESULT" != success ]; then',
                    'if [ "$TESTCONTAINERS_RESULT" = failure ]; then',
                    1,
                ),
            ),
            (
                "plan failure allowed",
                status_job.replace(
                    'if [ "$PLAN_RESULT" != success ]; then',
                    'if [ "$PLAN_RESULT" = failure ]; then',
                    1,
                ),
            ),
            (
                "full data failure allowed",
                status_job.replace(
                    'if [ "$SCOPE" = full ] && [ "$TEST_DATA_RESULT" != success ]; then',
                    'if [ "$SCOPE" = never ] && [ "$TEST_DATA_RESULT" != success ]; then',
                    1,
                ),
            ),
            (
                "full Spring Docker failure allowed",
                status_job.replace(
                    'if [ "$SCOPE" = full ] && [ "$TEST_SPRING_DOCKER_RESULT" != success ]; then',
                    'if [ "$SCOPE" = never ] && [ "$TEST_SPRING_DOCKER_RESULT" != success ]; then',
                    1,
                ),
            ),
            (
                "status job skip condition weakened",
                status_job.replace("    if: always()\n", "    if: success()\n", 1),
            ),
            (
                "status job failure masked",
                status_job.replace(
                    "  nightly-status:\n",
                    "  nightly-status:\n    continue-on-error: true\n",
                    1,
                ),
            ),
            (
                "status step skipped",
                status_job.replace(
                    "      - name: Check all jobs\n",
                    "      - name: Check all jobs\n        if: false\n",
                    1,
                ),
            ),
            (
                "status step failure masked",
                status_job.replace(
                    "      - name: Check all jobs\n",
                    "      - name: Check all jobs\n        continue-on-error: true\n",
                    1,
                ),
            ),
            (
                "status shell wraps and masks failure",
                status_job.replace(
                    "      - name: Check all jobs\n",
                    "      - name: Check all jobs\n"
                    "        shell: \"bash -c 'bash {0}; exit 0'\"\n",
                    1,
                ),
            ),
            (
                "full skipped result allowed",
                status_job.replace(
                    'if [ "$result" != success ]; then',
                    'if [ "$result" = failure ]; then',
                    1,
                ),
            ),
            (
                "Testcontainers Spring skipped result allowed",
                status_job.replace(
                    'if { [ "$SCOPE" = full ] || [ "$SCOPE" = testcontainers ]; } && [ "$TESTCONTAINERS_SPRING_RESULT" != success ]; then',
                    'if { [ "$SCOPE" = full ] || [ "$SCOPE" = testcontainers ]; } && [ "$TESTCONTAINERS_SPRING_RESULT" = failure ]; then',
                    1,
                ),
            ),
        )

        for name, invalid_job in mutations:
            with self.subTest(mutation=name):
                self.assertNotEqual(invalid_job, status_job, name)
                invalid = self.nightly.replace(status_job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(
                    any("Nightly status" in error for error in errors), errors
                )

    def test_nightly_required_scope_jobs_cannot_be_disabled(self) -> None:
        cases = (
            ("detekt", "smoke"),
            ("test-core", "smoke"),
            ("test-io", "standard"),
            ("test-ktor", "standard"),
            ("test-data", "standard"),
            ("test-testcontainers-spring", "Testcontainers bridge"),
            ("coverage-report", "Nightly coverage"),
        )
        for job_name, scope in cases:
            with self.subTest(job=job_name):
                job = verifier._job_block(self.nightly, job_name)
                invalid_job = job.replace("    if:", "    if: false #", 1)
                self.assertNotEqual(invalid_job, job)
                invalid = self.nightly.replace(job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(errors, errors)

    def test_nightly_testcontainers_spring_bridge_test_step_is_fail_closed(
        self,
    ) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-spring")
        step = verifier._named_step_block(job, "Test Testcontainers Spring bridge")
        self.assertTrue(step)

        step_mutations = (
            (
                "bridge test step skipped",
                step.replace(
                    "      - name: Test Testcontainers Spring bridge\n",
                    "      - name: Test Testcontainers Spring bridge\n"
                    "        if: false\n",
                    1,
                ),
            ),
            (
                "bridge test step failure ignored",
                step.replace(
                    "      - name: Test Testcontainers Spring bridge\n",
                    "      - name: Test Testcontainers Spring bridge\n"
                    "        continue-on-error: true\n",
                    1,
                ),
            ),
            (
                "bridge test step shell masks failure",
                step.replace(
                    "      - name: Test Testcontainers Spring bridge\n",
                    "      - name: Test Testcontainers Spring bridge\n"
                    "        shell: \"bash -c 'bash {0}; exit 0'\"\n",
                    1,
                ),
            ),
            (
                "bridge test command masks failure",
                step.replace(
                    "run: ./gradlew :bluetape4k-testcontainers-spring:test --no-configuration-cache",
                    "run: ./gradlew :bluetape4k-testcontainers-spring:test --no-configuration-cache || true",
                    1,
                ),
            ),
            (
                "bridge test environment broadened",
                step.replace(
                    '          GRADLE_OPTS: "-Dorg.gradle.daemon=false"\n',
                    '          GRADLE_OPTS: "-Dorg.gradle.daemon=false"\n'
                    "          BASH_ENV: /tmp/ignored.sh\n",
                    1,
                ),
            ),
        )
        mutations = [
            (name, job.replace(step, invalid_step, 1))
            for name, invalid_step in step_mutations
        ]
        mutations.append(
            (
                "bridge job failure ignored",
                job.replace(
                    "  test-testcontainers-spring:\n",
                    "  test-testcontainers-spring:\n    continue-on-error: true\n",
                    1,
                ),
            )
        )

        for name, invalid_job in mutations:
            with self.subTest(mutation=name):
                self.assertNotEqual(invalid_job, job, name)
                invalid = self.nightly.replace(job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(
                    any("Testcontainers Spring bridge" in error for error in errors),
                    errors,
                )

    def test_nightly_status_rejects_skipped_coverage_report(self) -> None:
        status_job = verifier._job_block(self.nightly, "nightly-status")
        status_step = verifier._named_step_block(status_job, "Check all jobs")
        script = verifier._step_run_script(status_step)
        self.assertIsNotNone(script)

        result = subprocess.run(
            ["bash", "-c", "\n".join(script or [])],
            check=False,
            capture_output=True,
            env={
                "EXPECTED_SCOPE": "smoke",
                "PLAN_RESULT": "success",
                "RESULTS": "success,success,success",
                "SCOPE": "smoke",
                "DETEKT_RESULT": "success",
                "TEST_CORE_RESULT": "success",
                "TEST_DATA_RESULT": "skipped",
                "TEST_SPRING_DOCKER_RESULT": "skipped",
                "TESTCONTAINERS_RESULT": "success",
                "TESTCONTAINERS_SPRING_RESULT": "skipped",
                "X64_GATE_RESULT": "skipped",
                "COVERAGE_REPORT_RESULT": "skipped",
            },
            text=True,
        )

        self.assertNotEqual(result.returncode, 0, result.stdout)

    def test_nightly_image_gate_mongodb_job_pins_runner_and_reports_kernel(
        self,
    ) -> None:
        errors = verifier.validate_nightly_mongodb_workflow(
            self.nightly, self.image_gate_manifest
        )

        self.assertEqual(errors, [])
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate-shard")
        self.assertEqual(verifier._job_values(job, "runs-on"), ["ubuntu-22.04"])
        diagnostics = verifier._named_step_block(job, "Report runner image and kernel")
        self.assertTrue(diagnostics)

    def test_nightly_image_gate_requires_all_four_shards(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate-shard")
        invalid_job = job.replace(
            "        shard: [0, 1, 2, 3]\n",
            "        shard: [0, 1, 2]\n",
            1,
        )
        invalid = self.nightly.replace(job, invalid_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )

        self.assertTrue(
            any(
                "image-gate shard matrix must include exactly shards 0 through 3"
                in error
                for error in errors
            ),
            errors,
        )

    def test_image_gate_aggregate_cannot_mask_missing_or_failed_shards(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate")
        step = verifier._named_step_block(
            job, "Aggregate and verify image gate evidence"
        )
        mutations = (
            step.replace(
                '          test "$SHARD_RESULT" = success\n',
                '          test "$SHARD_RESULT" = success || true\n',
                1,
            ),
            step.replace(
                '          test "$aggregate_exit" -eq 0\n',
                '          test "$aggregate_exit" -eq 0 || true\n',
                1,
            ),
            step.replace(
                "      - name: Aggregate and verify image gate evidence\n",
                "      - name: Aggregate and verify image gate evidence\n"
                "        continue-on-error: true\n",
                1,
            ),
            step.replace("        if: always()\n", "        if: false\n", 1),
            step.replace(
                "      - name: Aggregate and verify image gate evidence\n",
                "      - name: Aggregate and verify image gate evidence\n"
                "        shell: \"bash -c 'bash {0}; exit 0'\"\n",
                1,
            ),
        )

        for invalid_step in mutations:
            with self.subTest(mutation="aggregate failure masking or skip"):
                invalid_job = job.replace(step, invalid_step, 1)
                invalid = self.nightly.replace(job, invalid_job, 1)

                errors = verifier.validate_nightly_mongodb_workflow(
                    invalid, self.image_gate_manifest
                )

                self.assertTrue(
                    any("image-gate aggregate" in error for error in errors), errors
                )

        invalid_job = job.replace(
            "    if: ${{ always() && needs.plan.outputs.scope == 'full' }}\n",
            "    if: false\n",
            1,
        )
        invalid = self.nightly.replace(job, invalid_job, 1)
        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )
        self.assertTrue(
            any("image-gate aggregate" in error for error in errors), errors
        )

    def test_rejects_unsupported_runner_for_nightly_mongodb_image_gate(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate-shard")
        invalid = self.nightly.replace(
            job, job.replace("runs-on: ubuntu-22.04", "runs-on: ubuntu-24.04", 1), 1
        )

        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )

        self.assertTrue(any("image-gate" in error for error in errors), errors)

    def test_rejects_mongodb_image_gate_without_runner_kernel_diagnostics(
        self,
    ) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate-shard")
        diagnostics = verifier._named_step_block(job, "Report runner image and kernel")
        invalid = self.nightly.replace(job, job.replace(diagnostics, "", 1), 1)

        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )

        self.assertTrue(
            any("image-gate runner diagnostics" in error for error in errors), errors
        )

    def test_mongodb_image_gate_execution_cannot_mask_failures(self) -> None:
        job = verifier._job_block(self.nightly, "test-testcontainers-image-gate-shard")
        run_step = verifier._named_step_block(
            job, "Run image family gate shard sequentially"
        )
        invalid_step = run_step.replace(
            "      - name: Run image family gate shard sequentially\n",
            "      - name: Run image family gate shard sequentially\n"
            "        continue-on-error: true\n",
            1,
        )
        invalid = self.nightly.replace(job, job.replace(run_step, invalid_step, 1), 1)

        errors = verifier.validate_nightly_mongodb_workflow(
            invalid, self.image_gate_manifest
        )

        self.assertTrue(any("propagate failures" in error for error in errors), errors)

        masked_command = run_step.replace(
            "--job-budget-minutes 120",
            "--job-budget-minutes 120 || true",
            1,
        )
        invalid_command = self.nightly.replace(
            job, job.replace(run_step, masked_command, 1), 1
        )
        command_errors = verifier.validate_nightly_mongodb_workflow(
            invalid_command, self.image_gate_manifest
        )
        self.assertTrue(
            any(
                "image-gate execution must run full scope" in error
                for error in command_errors
            ),
            command_errors,
        )

        invalid_shell_step = run_step.replace(
            "      - name: Run image family gate shard sequentially\n",
            "      - name: Run image family gate shard sequentially\n"
            "        shell: \"bash -c 'bash {0}; exit 0'\"\n",
            1,
        )
        invalid_shell = self.nightly.replace(
            job, job.replace(run_step, invalid_shell_step, 1), 1
        )
        shell_errors = verifier.validate_nightly_mongodb_workflow(
            invalid_shell, self.image_gate_manifest
        )
        self.assertTrue(
            any(
                "image-gate execution must run full scope" in error
                for error in shell_errors
            ),
            shell_errors,
        )

        invalid_environment_step = run_step.replace(
            "          DOCKER_AUTH_CONFIG: ${{ secrets.TESTCONTAINERS_DOCKER_AUTH_CONFIG }}\n",
            "          DOCKER_AUTH_CONFIG: ${{ secrets.TESTCONTAINERS_DOCKER_AUTH_CONFIG }}\n"
            "          UNPINNED_FLAG: true\n",
            1,
        )
        invalid_environment = self.nightly.replace(
            job, job.replace(run_step, invalid_environment_step, 1), 1
        )
        environment_errors = verifier.validate_nightly_mongodb_workflow(
            invalid_environment, self.image_gate_manifest
        )
        self.assertTrue(
            any(
                "image-gate execution must run full scope" in error
                for error in environment_errors
            ),
            environment_errors,
        )

    def test_image_gate_manifest_keeps_the_mongodb_workload(self) -> None:
        errors = verifier.validate_mongodb_image_gate_manifest(self.image_gate_manifest)

        self.assertEqual(errors, [])

        invalid_manifest = self.image_gate_manifest.replace(
            '"id": "mongo-d-b"', '"id": "mongo-db"', 1
        )
        errors = verifier.validate_mongodb_image_gate_manifest(invalid_manifest)

        self.assertTrue(any("mongo-d-b" in error for error in errors), errors)

    def test_rejects_latest_runner_for_nightly_mongodb_data_tests(self) -> None:
        data_job = verifier._job_block(self.nightly, "test-data")
        invalid_data_job = data_job.replace(
            "runs-on: ${{ matrix.group == 'nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )
        invalid = self.nightly.replace(data_job, invalid_data_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(any("compatible runner" in error for error in errors), errors)

    def test_nightly_pins_only_the_nosql_data_group_to_supported_runner(self) -> None:
        data_job = verifier._job_block(self.nightly, "test-data")
        diagnostic = verifier._named_step_block(
            data_job, "Report runner image and kernel"
        )

        self.assertEqual(
            verifier._job_values(data_job, "runs-on"),
            ["${{ matrix.group == 'nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}"],
        )
        self.assertEqual(
            verifier._step_values(diagnostic, "if"),
            ["${{ matrix.group == 'nosql' }}"],
        )

        invalid_job = data_job.replace(
            "runs-on: ${{ matrix.group == 'nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )
        invalid = self.nightly.replace(data_job, invalid_job, 1)
        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(any("nosql group" in error for error in errors), errors)

    def test_rejects_latest_runner_for_nightly_spring_boot_mongodb_tests(self) -> None:
        spring_job = verifier._job_block(self.nightly, "test-spring-docker")
        invalid_spring_job = spring_job.replace(
            "runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}",
            "runs-on: ubuntu-latest",
            1,
        )
        invalid = self.nightly.replace(spring_job, invalid_spring_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any("compatible runner" in error for error in errors),
            errors,
        )

    def test_rejects_nightly_mongodb_test_task_outside_expected_matrix_group(
        self,
    ) -> None:
        spring_job = verifier._job_block(self.nightly, "test-spring-docker")
        invalid_spring_job = spring_job.replace(
            "              :bluetape4k-spring-boot-mongodb:test",
            "              :bluetape4k-spring-boot-cassandra:test",
            1,
        ).replace(
            "          - group: demos\n            test_tasks: >-\n"
            "              :bluetape4k-examples-cassandra-demo:test",
            "          - group: demos\n            test_tasks: >-\n"
            "              :bluetape4k-spring-boot-mongodb:test",
            1,
        )
        invalid = self.nightly.replace(spring_job, invalid_spring_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(any("MongoDB test task" in error for error in errors), errors)

    def test_rejects_nightly_runner_diagnostics_after_mongodb_tests(self) -> None:
        data_job = verifier._job_block(self.nightly, "test-data")
        diagnostic = verifier._named_step_block(
            data_job, "Report runner image and kernel"
        )
        self.assertTrue(diagnostic)
        invalid_data_job = data_job.replace(diagnostic, "", 1)
        invalid_data_job = invalid_data_job.replace(
            "      - name: Generate Kover XML report",
            diagnostic + "\n\n      - name: Generate Kover XML report",
            1,
        )
        invalid = self.nightly.replace(data_job, invalid_data_job, 1)

        errors = verifier.validate_nightly_mongodb_workflow(invalid)

        self.assertTrue(
            any("diagnostics must precede" in error for error in errors), errors
        )

    def test_rejects_comment_decoy_for_runner_diagnostics(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        diagnostics_step = verifier._named_step_block(
            spring_boot_job, "Report runner image and kernel"
        )
        invalid_step = diagnostics_step
        for command in (
            'echo "ImageOS=${ImageOS:-<unset>}"',
            'echo "ImageVersion=${ImageVersion:-<unset>}"',
            "cat /etc/os-release",
            "uname -r",
        ):
            invalid_step = invalid_step.replace(
                f"          {command}", f"          # {command}", 1
            )
        invalid_spring_boot_job = spring_boot_job.replace(
            diagnostics_step, invalid_step, 1
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(any("runner diagnostics" in error for error in errors), errors)

    def test_rejects_runner_diagnostics_shell_and_failure_masking(self) -> None:
        cases = (
            (
                "CI Test/Data",
                self.ci,
                "test-data",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "CI Spring Boot",
                self.ci,
                "test-spring-boot",
                lambda workflow: verifier.validate_workflows(workflow, self.submission),
            ),
            (
                "Nightly Test/Data",
                self.nightly,
                "test-data",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Spring Boot",
                self.nightly,
                "test-spring-docker",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly Testcontainers storage-cache",
                self.nightly,
                "test-testcontainers",
                verifier.validate_nightly_mongodb_workflow,
            ),
            (
                "Nightly image-gate shard",
                self.nightly,
                "test-testcontainers-image-gate-shard",
                verifier.validate_nightly_mongodb_workflow,
            ),
        )
        mutations = (
            (
                "shell override",
                "        shell: bash -c 'set +e' -- {0}\n",
            ),
            ("continue-on-error", "        continue-on-error: true\n"),
            ("early successful exit", "          exit 0\n"),
        )
        for label, workflow, job_name, validate in cases:
            with self.subTest(job=label):
                job = verifier._job_block(workflow, job_name)
                step = verifier._named_step_block(job, "Report runner image and kernel")
                self.assertTrue(step)
                for mutation, inserted in mutations:
                    with self.subTest(mutation=mutation):
                        if mutation == "early successful exit":
                            invalid_step = step.replace(
                                '          echo "ImageOS=',
                                inserted + '          echo "ImageOS=',
                                1,
                            )
                        else:
                            invalid_step = step.replace(
                                "        run: |\n", inserted + "        run: |\n", 1
                            )
                        self.assertNotEqual(invalid_step, step)
                        invalid_job = job.replace(step, invalid_step, 1)
                        invalid_workflow = workflow.replace(job, invalid_job, 1)

                        errors = validate(invalid_workflow)

                        self.assertTrue(
                            any("runner diagnostics" in error for error in errors),
                            errors,
                        )

    def test_nightly_rejects_workflow_and_job_shell_defaults(self) -> None:
        job = verifier._job_block(self.nightly, "test-data")
        mutations = (
            (
                "workflow defaults",
                self.nightly.replace(
                    "jobs:\n",
                    "defaults:\n  run:\n    shell: bash -c 'exit 0' -- {0}\n\njobs:\n",
                    1,
                ),
            ),
            (
                "job defaults",
                self.nightly.replace(
                    job,
                    job.replace(
                        "    steps:\n",
                        "    defaults:\n"
                        "      run:\n"
                        "        shell: bash -c 'exit 0' -- {0}\n"
                        "    steps:\n",
                        1,
                    ),
                    1,
                ),
            ),
        )
        for description, invalid in mutations:
            with self.subTest(scope=description):
                errors = verifier.validate_nightly_mongodb_workflow(invalid)

                self.assertTrue(
                    any("custom run shell defaults" in error for error in errors),
                    errors,
                )

    def test_rejects_runner_diagnostics_after_spring_boot_test_step(self) -> None:
        spring_boot_job = verifier._job_block(self.ci, "test-spring-boot")
        diagnostics = verifier._named_step_block(
            spring_boot_job, "Report runner image and kernel"
        )
        test_step = verifier._named_step_block(
            spring_boot_job, "Test Spring Boot modules (${{ matrix.group }})"
        )
        without_diagnostics = spring_boot_job.replace(diagnostics, "", 1)
        test_step = verifier._named_step_block(
            without_diagnostics, "Test Spring Boot modules (${{ matrix.group }})"
        )
        invalid_spring_boot_job = without_diagnostics.replace(
            test_step, test_step + "\n\n" + diagnostics, 1
        )
        invalid = self.ci.replace(spring_boot_job, invalid_spring_boot_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any(
                "Spring Boot runner diagnostics must precede the test step" in error
                for error in errors
            ),
            errors,
        )

    def test_rejects_catalog_tag_as_the_canonical_ref(self) -> None:
        invalid = self.ci.replace(
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: '9bd4fa16e6d64677641b1babf4629ae5b4ef6d80'",
            "BLUETAPE4K_DEPENDENCIES_CATALOG_REF: 'catalog/2026-10-22-01'",
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
            'test -n "$PR_BASE_REF"',
            'git fetch --no-tags origin "refs/heads/$PR_BASE_REF:refs/remotes/origin/$PR_BASE_REF"',
            'git merge-base --is-ancestor "$current_base_sha" "$PR_HEAD_SHA"',
            "python3 -m unittest tests.test_catalog_workflows -v",
        )

        missing = [item for item in required_contract if item not in merge_job]
        self.assertEqual(missing, [])

        status_job = verifier._job_block(self.ci, "ci-status")
        self.assertIn("test-merge-result", status_job)

    def test_rejects_merge_result_job_without_current_base_ref(self) -> None:
        merge_job = verifier._job_block(self.ci, "test-merge-result")
        invalid_merge_job = merge_job.replace(
            "          PR_BASE_REF: ${{ github.base_ref }}\n",
            "",
            1,
        )
        invalid = self.ci.replace(merge_job, invalid_merge_job, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("exact merge-result execution environment" in error for error in errors),
            errors,
        )

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

    def test_projects_ci_verifies_and_uploads_all_catalog_provenance_evidence(self) -> None:
        governance = verifier._job_block(self.ci, "catalog-governance")
        verification = verifier._named_step_block(
            governance, "Verify catalog provenance evidence"
        )
        upload = verifier._named_step_block(
            governance, "Upload catalog provenance evidence"
        )

        self.assertEqual(
            verifier._step_run_script(verification),
            list(verifier.EXPECTED_CATALOG_EVIDENCE_VERIFICATION_SCRIPT),
        )
        self.assertEqual(verifier._step_values(verification, "if"), ["always()"])
        self.assertEqual(verifier._step_values(verification, "shell"), ["bash"])
        self.assertEqual(
            verifier._step_values(upload, "uses"), ["actions/upload-artifact@v7"]
        )
        self.assertEqual(verifier._step_values(upload, "if"), ["always()"])
        self.assertEqual(
            verifier._step_with_input_keys(upload),
            list(verifier.EXPECTED_CATALOG_EVIDENCE_ARTIFACT_INPUTS),
        )
        self.assertLess(
            verifier._named_step_index(governance, "Verify catalog provenance evidence"),
            verifier._named_step_index(governance, "Upload catalog provenance evidence"),
        )
        self.assertIn("path: .gradle/issue-1832-evidence/**", upload)
        self.assertIn("if-no-files-found: warn", upload)

    def test_rejects_missing_catalog_provenance_upload(self) -> None:
        upload = verifier._named_step_block(
            verifier._job_block(self.ci, "catalog-governance"),
            "Upload catalog provenance evidence",
        )
        invalid = self.ci.replace(upload, "", 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("must upload catalog provenance evidence" in error for error in errors),
            errors,
        )

    def test_rejects_narrow_catalog_provenance_artifact_path(self) -> None:
        upload = verifier._named_step_block(
            verifier._job_block(self.ci, "catalog-governance"),
            "Upload catalog provenance evidence",
        )
        invalid_upload = upload.replace(
            "path: .gradle/issue-1832-evidence/**",
            "path: .gradle/issue-1832-evidence/parser-receipt.json",
            1,
        )
        invalid = self.ci.replace(upload, invalid_upload, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("complete evidence directory" in error for error in errors),
            errors,
        )

    def test_rejects_selective_catalog_provenance_validation(self) -> None:
        verification = verifier._named_step_block(
            verifier._job_block(self.ci, "catalog-governance"),
            "Verify catalog provenance evidence",
        )
        invalid_verification = verification.replace(
            "candidate-lock.json",
            "parser-receipt.json",
            1,
        )
        invalid = self.ci.replace(verification, invalid_verification, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("require all four regular files" in error for error in errors),
            errors,
        )

    def test_rejects_optional_catalog_provenance_artifact_missing_policy(self) -> None:
        upload = verifier._named_step_block(
            verifier._job_block(self.ci, "catalog-governance"),
            "Upload catalog provenance evidence",
        )
        invalid_upload = upload.replace(
            "if-no-files-found: warn",
            "if-no-files-found: ignore",
            1,
        )
        invalid = self.ci.replace(upload, invalid_upload, 1)

        errors = verifier.validate_workflows(invalid, self.submission)

        self.assertTrue(
            any("partial failure evidence" in error for error in errors),
            errors,
        )

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
            "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA: '9bd4fa16e6d64677641b1babf4629ae5b4ef6d80'",
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
                            "BUNDLE_GEMFILE only in the pinned parser install and contract"
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
        parser_step = verifier._named_step_block(
            verifier._job_block(self.ci, "catalog-governance"),
            "Run pinned Dependabot Gradle parser contract",
        )
        invalid_parser_step = parser_step.replace(
            "          BUNDLE_GEMFILE: ${{ github.workspace }}/.gradle/dependabot-core/Gemfile\n",
            "          BUNDLE_GEMFILE: ${{ github.workspace }}/Gemfile\n",
            1,
        )
        invalid_ci = self.ci.replace(parser_step, invalid_parser_step, 1)

        errors = verifier.validate_workflows(invalid_ci, self.submission)

        self.assertTrue(
            any("parser contract step environment" in error for error in errors),
            errors,
        )

    def test_security_dependency_scan_failure_is_not_masked(self) -> None:
        dependency_check = verifier._job_block(self.security, "dependency-check")
        step = verifier._named_step_block(
            dependency_check, "Run OWASP Dependency Check"
        )

        self.assertTrue(step)
        self.assertNotIn("continue-on-error: true", step)

    def test_security_report_and_sarif_upload_failures_are_not_masked(self) -> None:
        dependency_check = verifier._job_block(self.security, "dependency-check")
        artifact_step = verifier._named_step_block(
            dependency_check, "Upload Dependency Check report"
        )
        sarif_step = verifier._named_step_block(
            dependency_check, "Upload SARIF to GitHub Security tab"
        )

        self.assertEqual(verifier._step_values(artifact_step, "if"), ["always()"])
        self.assertIn("if-no-files-found: error", artifact_step)
        self.assertEqual(verifier._step_values(sarif_step, "if"), ["always()"])
        self.assertNotIn("continue-on-error: true", artifact_step)
        self.assertNotIn("continue-on-error: true", sarif_step)

    def test_security_no_report_path_is_an_explicit_failure(self) -> None:
        dependency_check = verifier._job_block(self.security, "dependency-check")
        step = verifier._named_step_block(
            dependency_check, "Verify Dependency Check reports"
        )
        script = verifier._step_script_source(step, "run") or ""

        self.assertEqual(verifier._step_values(step, "if"), ["always()"])
        self.assertIn("dependency-check-report.", script)
        self.assertIn("exit 1", script)

    def test_security_status_aggregates_scan_and_upload_results(self) -> None:
        status_job = verifier._job_block(self.security, "security-status")
        status_step = verifier._named_step_block(status_job, "Check all security jobs")
        script = verifier._step_script_source(status_step, "run") or ""

        self.assertTrue(status_job)
        self.assertIn("needs:", status_job)
        self.assertIn("- gitleaks", status_job)
        self.assertIn("- dependency-check", status_job)
        self.assertEqual(verifier._job_values(status_job, "if"), ["always()"])
        self.assertFalse(verifier._job_values(status_job, "continue-on-error"))
        self.assertIn("needs.gitleaks.result", status_job)
        self.assertIn("needs.dependency-check.result", status_job)
        self.assertIn("exit 1", script)


if __name__ == "__main__":
    unittest.main()
