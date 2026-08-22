#!/usr/bin/env python3
"""Test the manifest-driven Testcontainers image gate contract."""

from __future__ import annotations

import unittest
from pathlib import Path

from scripts.testcontainers_image_gate import (
    EXPECTED_FAMILY_COUNT,
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

    def test_k3s_family_uses_the_existing_tagged_test_task(self) -> None:
        k3s = next(entry for entry in self.entries if entry["server"] == "K3sServer")
        self.assertEqual(":bluetape4k-testcontainers:k8sTest", k3s["testTask"])
        build_script = (
            self.root / "testing/testcontainers/build.gradle.kts"
        ).read_text(encoding="utf-8")
        self.assertIn('tasks.register<Test>("k8sTest")', build_script)

    def test_changed_scope_is_deterministic_and_full_scope_is_complete(self) -> None:
        changed = select_entries(self.entries, {"testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt"})
        self.assertEqual(["FlociServer"], [entry["server"] for entry in changed])
        self.assertEqual(self.entries, select_entries(self.entries, set(), scope="full"))

        shared = select_entries(self.entries, {"scripts/run_testcontainers_image_gate.py"})
        self.assertEqual(EXPECTED_FAMILY_COUNT, len(shared))

    def test_invalid_manifest_reports_drift_without_running_docker(self) -> None:
        invalid = [dict(self.entries[0], image="wrong/image")]
        self.assertIn("image drift", " ".join(validate_manifest(invalid, self.root)))

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

    def test_ci_workflow_runs_changed_gate_and_uploads_evidence(self) -> None:
        workflow = (self.root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
        self.assertIn("testcontainers-image-gate:", workflow)
        self.assertIn("--scope changed", workflow)
        self.assertIn("--changed-path-file", workflow)
        self.assertIn("build/reports/testcontainers-image-gate/", workflow)
        self.assertIn("testcontainers-image-gate, test-ktor", workflow)

    def test_nightly_runs_full_gate_sequentially_before_spring_bridge(self) -> None:
        workflow = (self.root / ".github/workflows/nightly-tests.yml").read_text(encoding="utf-8")
        self.assertIn("test-testcontainers-image-gate:", workflow)
        self.assertIn("--scope full", workflow)
        self.assertIn("TESTCONTAINERS_IMAGE_GATE_MAX_PARALLEL: '1'", workflow)
        self.assertIn("needs: [test-testcontainers, test-testcontainers-image-gate, plan]", workflow)
        self.assertIn("- test-testcontainers-image-gate", workflow)

    def test_release_publish_depends_on_full_gate_summary(self) -> None:
        workflow = (self.root / ".github/workflows/release.yml").read_text(encoding="utf-8")
        self.assertIn("testcontainers-image-gate:", workflow)
        self.assertIn("needs: [resolve-version, testcontainers-image-gate]", workflow)
        self.assertIn("--scope full", workflow)
        self.assertIn("coverage=52/52", workflow)


if __name__ == "__main__":
    unittest.main(verbosity=2)
