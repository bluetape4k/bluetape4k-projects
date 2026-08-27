#!/usr/bin/env python3
"""Test the manifest-driven Testcontainers image gate contract."""

from __future__ import annotations

import re
import unittest
from pathlib import Path

from scripts.testcontainers_image_gate import (
    EXPECTED_FAMILY_COUNT,
    EXPECTED_RELEASE_FAMILY_COUNT,
    NON_RELEASE_RUNTIME_SERVERS,
    SelectionError,
    load_manifest,
    select_entries,
    validate_manifest,
)


class TestTestcontainersImageGate(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.root = Path(__file__).resolve().parents[1]
        cls.manifest_path = cls.root / "scripts/testcontainers_image_gate_manifest.json"
        cls.entries = load_manifest(cls.manifest_path)

    def test_manifest_covers_every_image_family_and_has_required_fields(self) -> None:
        self.assertEqual(EXPECTED_FAMILY_COUNT, len(self.entries))
        self.assertEqual([], validate_manifest(self.entries, self.root))

    def test_disabled_families_are_support_inventory_not_release_runtime(self) -> None:
        disabled = set(NON_RELEASE_RUNTIME_SERVERS)
        release_required = {
            entry["server"] for entry in self.entries if entry["releaseRequired"]
        }
        self.assertEqual(EXPECTED_RELEASE_FAMILY_COUNT, len(release_required))
        self.assertEqual(disabled, {entry["server"] for entry in self.entries if not entry["releaseRequired"]})

    def test_method_selectors_exclude_intentionally_disabled_tests(self) -> None:
        selectors = {
            entry["server"]: entry["testSelector"]
            for entry in self.entries
            if "testSelector" in entry
        }
        self.assertEqual(
            {
                "LocalStackServer": "io.bluetape4k.testcontainers.aws.LocalStackServerTest.run S3 Service",
                "RedisClusterServer": "io.bluetape4k.testcontainers.storage.RedisClusterServerTest.create redis cluster server",
            },
            selectors,
        )

    def test_readmes_document_release_required_coverage(self) -> None:
        coverage = f"{EXPECTED_RELEASE_FAMILY_COUNT}/{EXPECTED_RELEASE_FAMILY_COUNT}"
        for readme_name in ("README.md", "README.ko.md"):
            with self.subTest(readme=readme_name):
                readme = (self.root / "testing/testcontainers" / readme_name).read_text(encoding="utf-8")
                self.assertIn(coverage, readme)
                self.assertNotIn("Stable publication requires `52/52`", readme)
                self.assertNotIn("안정 버전 배포는 `52/52`", readme)

    def test_k3s_family_uses_the_existing_tagged_test_task(self) -> None:
        k3s = next(entry for entry in self.entries if entry["server"] == "K3sServer")
        self.assertEqual(":bluetape4k-testcontainers:k8sTest", k3s["testTask"])
        build_script = (
            self.root / "testing/testcontainers/build.gradle.kts"
        ).read_text(encoding="utf-8")
        self.assertIn('tasks.register<Test>("k8sTest")', build_script)

    def test_java25_ignite_options_are_module_scoped_and_minimal(self) -> None:
        root_build = (self.root / "build.gradle.kts").read_text(encoding="utf-8")
        module_build = (
            self.root / "testing/testcontainers/build.gradle.kts"
        ).read_text(encoding="utf-8")
        self.assertIn("tasks.withType<Test>().configureEach", module_build)
        self.assertIn('systemProperty("testcontainers.image-gate.evidence-dir", it)', module_build)
        for option in (
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
        ):
            self.assertIn(option, module_build)
            self.assertNotIn(option, root_build)

    def test_ignite_workload_receipt_matches_the_long_arm_timeout_contract(self) -> None:
        source = (
            self.root
            / "testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt"
        ).read_text(encoding="utf-8")
        self.assertIn('Files.writeString(root.resolve("workload.image-id")', source)
        self.assertIn("@Timeout(value = 30, unit = TimeUnit.MINUTES)", source)

    def test_changed_scope_is_deterministic_and_full_scope_is_complete(self) -> None:
        changed = select_entries(self.entries, {"testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt"})
        self.assertEqual(["FlociServer"], [entry["server"] for entry in changed])
        self.assertEqual(self.entries, select_entries(self.entries, set(), scope="full"))

        shared = select_entries(self.entries, {"scripts/run_testcontainers_image_gate.py"})
        self.assertEqual(EXPECTED_FAMILY_COUNT, len(shared))

    def test_family_selector_is_exact_and_requires_platform_when_requested(self) -> None:
        ignite = dict(self.entries[0])
        ignite["id"] = "ignite2"
        ignite["platforms"] = [
            {"id": "amd64", "os": "linux", "architecture": "amd64", "tag": "2.18.0", "runner": "ubuntu-24.04"},
            {"id": "arm64", "os": "linux", "architecture": "arm64", "tag": "2.18.0-arm64", "runner": "ubuntu-24.04-arm"},
        ]
        ignite["defaultPlatformId"] = "amd64"
        selected = select_entries([ignite], set(), scope="family", family_id="ignite2", platform_id="arm64", require_selection=True)
        self.assertEqual("arm64", selected[0]["_selected_platform_id"])
        with self.assertRaises(SelectionError):
            select_entries([ignite], set(), scope="family", family_id="missing", platform_id="arm64", require_selection=True)

    def test_strict_platform_contract_rejects_unsafe_runner_and_workload(self) -> None:
        invalid = dict(self.entries[0])
        invalid.update(
            {
                "executionEvidenceRequired": True,
                "pullEvidenceRequired": True,
                "defaultPlatformId": "amd64",
                "workloadTestPattern": "../unsafe",
                "platforms": [
                    {"id": "amd64", "os": "linux", "architecture": "amd64", "tag": "2.18.0", "runner": "self-hosted"},
                ],
                "platformTimeouts": {"amd64": {"testMinutes": 6, "clientConnectSeconds": 30, "clientRequestSeconds": 30}},
            }
        )
        errors = validate_manifest([invalid], self.root)
        self.assertIn("runner is unsupported", " ".join(errors))
        self.assertIn("workloadTestPattern is invalid", " ".join(errors))

    def test_invalid_manifest_reports_drift_without_running_docker(self) -> None:
        invalid = [dict(self.entries[0], image="wrong/image")]
        self.assertIn("image drift", " ".join(validate_manifest(invalid, self.root)))

    def test_manifest_rejects_test_selector_outside_test_class(self) -> None:
        invalid = [
            dict(
                self.entries[0],
                testSelector="example.OtherServerTest.representative_test",
            )
        ]
        self.assertIn(
            "testSelector must target a method under testPattern",
            " ".join(validate_manifest(invalid, self.root)),
        )

    def test_manifest_rejects_non_concrete_or_unknown_test_selector(self) -> None:
        pattern = self.entries[0]["testPattern"]
        for selector in (
            f"{pattern}.",
            f"{pattern}.*",
            f"{pattern}.missing.method",
            f"{pattern}.missing\u2028method",
        ):
            with self.subTest(selector=selector):
                errors = validate_manifest(
                    [dict(self.entries[0], testSelector=selector)],
                    self.root,
                )
                self.assertTrue(
                    any("testSelector" in error for error in errors),
                    errors,
                )

    def test_manifest_rejects_unsafe_family_id(self) -> None:
        invalid = [dict(self.entries[0], id="../escape")]
        self.assertIn("id is invalid", " ".join(validate_manifest(invalid, self.root)))

    def test_invalid_family_specific_test_task_reports_drift(self) -> None:
        invalid = [dict(self.entries[0], testTask="test")]
        self.assertIn(
            "testTask must be a Gradle task path",
            " ".join(validate_manifest(invalid, self.root)),
        )

    def test_unknown_family_specific_test_task_reports_drift(self) -> None:
        invalid = [dict(self.entries[0], testTask=":not-existing-task")]
        self.assertIn(
            "unknown testTask",
            " ".join(validate_manifest(invalid, self.root)),
        )

    def test_ci_workflow_excludes_image_gate(self) -> None:
        workflow = (self.root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
        self.assertNotIn("testcontainers-image-gate", workflow)
        self.assertIn("test-testcontainers-spring:", workflow)

    def test_ci_and_release_run_manifest_contract_before_docker(self) -> None:
        ci_workflow = (self.root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
        ci_match = re.search(
            r"^  jvm-release-contract:\n.*?(?=^  [a-z0-9-]+:\n)",
            ci_workflow,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(ci_match, "CI JVM release contract job is missing")
        self.assertIn(
            "python3 -m unittest scripts/test_testcontainers_image_gate.py -v",
            ci_match.group(0),
        )

        release_workflow = (self.root / ".github/workflows/release.yml").read_text(encoding="utf-8")
        for workflow in (ci_workflow, release_workflow):
            if "testcontainers-image-gate" in workflow:
                if "testcontainers-ignite2-arm64-image-gate" in workflow:
                    self.assertIn("build/reports/testcontainers-image-gate-artifact", workflow)
                    self.assertNotIn("build/reports/testcontainers-image-gate/*.json", workflow)
                else:
                    self.assertIn("build/reports/testcontainers-image-gate/*.json", workflow)
                    self.assertIn("build/reports/testcontainers-image-gate/summary.md", workflow)
                self.assertNotIn("path: build/reports/testcontainers-image-gate/", workflow)
        manifest_match = re.search(
            r"^  testcontainers-manifest-contract:\n.*?(?=^  [a-z0-9-]+:\n)",
            release_workflow,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(manifest_match, "Release manifest contract job is missing")
        self.assertIn(
            "python3 -m unittest scripts/test_testcontainers_image_gate.py -v",
            manifest_match.group(0),
        )
        self.assertNotRegex(manifest_match.group(0), r"(?i)\bdocker\b")
        self.assertNotIn("jibDockerBuild", manifest_match.group(0))
        self.assertNotIn("run_testcontainers_image_gate.py", manifest_match.group(0))
        self.assertIn(
            "needs: [resolve-version, testcontainers-manifest-contract]",
            release_workflow,
        )
        if "testcontainers-ignite2-arm64-image-gate:" in release_workflow:
            self.assertIn(
                "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]",
                release_workflow,
            )
        else:
            self.assertIn(
                "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate]",
                release_workflow,
            )

        publish_match = re.search(
            r"^  publish:\n.*\Z",
            release_workflow,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(publish_match, "Release publish job is missing")
        self.assertNotIn("scripts/test_testcontainers_image_gate.py", publish_match.group(0))

    def test_ci_required_contract_covers_runner_and_its_shared_paths(self) -> None:
        workflow = (self.root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
        ci_match = re.search(
            r"^  jvm-release-contract:\n.*?(?=^  [a-z0-9-]+:\n)",
            workflow,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(ci_match, "CI JVM release contract job is missing")
        self.assertIn(
            "python3 -m unittest scripts/test_run_testcontainers_image_gate.py -v",
            ci_match.group(0),
        )
        shared_match = re.search(
            r"^            shared:\n.*?(?=^            [a-z0-9-]+:\n)",
            workflow,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(shared_match, "CI shared path filter is missing")
        for path in (
            "scripts/run_testcontainers_image_gate.py",
            "scripts/test_run_testcontainers_image_gate.py",
            "scripts/testcontainers_image_gate.py",
            "scripts/testcontainers_image_gate_manifest.json",
        ):
            self.assertIn(f"'{path}'", shared_match.group(0))

    def test_nightly_runs_full_gate_only_before_spring_bridge(self) -> None:
        workflow = (self.root / ".github/workflows/nightly-tests.yml").read_text(encoding="utf-8")
        self.assertIn("test-testcontainers-image-gate:", workflow)
        self.assertIn("if: ${{ needs.plan.outputs.scope == 'full' }}", workflow)
        self.assertIn("--scope full", workflow)
        self.assertIn("TESTCONTAINERS_IMAGE_GATE_MAX_PARALLEL: '1'", workflow)
        if "test-testcontainers-ignite2-arm64-image-gate:" in workflow:
            self.assertIn("runs-on: ubuntu-24.04", workflow)
            self.assertIn("runs-on: ubuntu-24.04-arm", workflow)
            self.assertIn("--default-platform-id amd64", workflow)
            self.assertIn("--job-budget-minutes 360", workflow)
            self.assertIn("--family-id ignite2", workflow)
            self.assertIn("--platform-id arm64", workflow)
            self.assertIn("--job-budget-minutes 90", workflow)
            self.assertIn(
                "needs: [test-testcontainers, test-testcontainers-image-gate, test-testcontainers-ignite2-arm64-image-gate, plan]",
                workflow,
            )
            self.assertIn("needs.test-testcontainers-image-gate.result == 'skipped'", workflow)
            self.assertIn("needs.test-testcontainers-ignite2-arm64-image-gate.result == 'skipped'", workflow)
            self.assertIn("- test-testcontainers-ignite2-arm64-image-gate", workflow)
            self.assertNotIn("io.bluetape4k.testcontainers.storage.Ignite2ServerTest\",", workflow)
            self.assertIn("name: nightly-testcontainers-image-gate-${{ github.run_id }}-amd64", workflow)
            self.assertIn("name: nightly-testcontainers-image-gate-${{ github.run_id }}-arm64", workflow)
        else:
            self.assertIn("needs: [test-testcontainers, test-testcontainers-image-gate, plan]", workflow)
            self.assertIn("needs.test-testcontainers-image-gate.result == 'skipped'", workflow)

    def test_release_publish_depends_on_full_gate_summary(self) -> None:
        workflow = (self.root / ".github/workflows/release.yml").read_text(encoding="utf-8")
        self.assertIn("testcontainers-manifest-contract:", workflow)
        self.assertIn("testcontainers-image-gate:", workflow)
        if "testcontainers-ignite2-arm64-image-gate:" in workflow:
            self.assertIn(
                "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]",
                workflow,
            )
        else:
            self.assertIn(
                "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate]",
                workflow,
            )
        self.assertIn("--scope full", workflow)
        if "testcontainers-ignite2-arm64-image-gate:" in workflow:
            self.assertIn("runs-on: ubuntu-24.04", workflow)
            self.assertIn("runs-on: ubuntu-24.04-arm", workflow)
            self.assertIn('expected_coverage="48/48"', workflow)
            self.assertIn('expected_coverage="1/1"', workflow)
            self.assertIn("name: release-testcontainers-image-gate-${{ github.run_id }}-amd64", workflow)
            self.assertIn("name: release-testcontainers-image-gate-${{ github.run_id }}-arm64", workflow)
        else:
            self.assertIn("coverage=52/52", workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
