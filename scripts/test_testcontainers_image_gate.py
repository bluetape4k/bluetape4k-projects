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

    def test_changed_scope_is_deterministic_and_full_scope_is_complete(self) -> None:
        changed = select_entries(self.entries, {"testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/aws/FlociServer.kt"})
        self.assertEqual(["FlociServer"], [entry["server"] for entry in changed])
        self.assertEqual(self.entries, select_entries(self.entries, set(), scope="full"))

    def test_invalid_manifest_reports_drift_without_running_docker(self) -> None:
        invalid = [dict(self.entries[0], image="wrong/image")]
        self.assertIn("image drift", " ".join(validate_manifest(invalid, self.root)))


if __name__ == "__main__":
    unittest.main(verbosity=2)
