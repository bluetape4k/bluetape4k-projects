#!/usr/bin/env python3

import unittest
from pathlib import Path


REPOSITORY = Path(__file__).resolve().parents[1]
WORKFLOWS = REPOSITORY / ".github" / "workflows"


class ReleaseWorkflowPolicyTest(unittest.TestCase):

    def test_release_workflow_publishes_only_to_maven_central(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")

        self.assertIn("Publish RELEASE to Maven Central Portal", workflow)
        self.assertIn("nmcpPublishAggregationToCentralPortal", workflow)
        self.assertNotIn("github-release:", workflow)
        self.assertNotIn("gh release create", workflow)
        self.assertNotIn("issue-754", workflow)
        self.assertNotIn("create-github-app-token", workflow)
        self.assertNotIn("release-hold", workflow)

    def test_snapshot_workflow_is_not_coupled_to_issue_754_release_state(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("Publish SNAPSHOT to Maven Central", workflow)
        self.assertIn("nmcpPublishAggregationToCentralPortalSnapshots", workflow)
        self.assertNotIn("issue-754", workflow)
        self.assertNotIn("release-hold", workflow)
        self.assertNotIn("tag-immutability", workflow)

    def test_issue_specific_release_machinery_is_absent(self) -> None:
        forbidden = (
            ".github/release-holds/1.12.0-github-settings.json",
            ".github/release-holds/1.12.0-issue-754.json",
            ".github/workflows/release-generic.yml",
            "scripts/check-release-holds.py",
            "scripts/issue-754-github-settings.py",
            "scripts/test_check_release_holds.py",
            "scripts/test_issue_754_github_settings.py",
        )

        existing = [path for path in forbidden if (REPOSITORY / path).exists()]
        self.assertEqual([], existing)

    def test_serializer_abi_check_is_release_policy_independent(self) -> None:
        script = (REPOSITORY / "scripts" / "check-serializer-buffer-abi.sh").read_text(
            encoding="utf-8"
        )

        self.assertNotIn("check-release-holds.py", script)
        self.assertNotIn("release-hold", script)


if __name__ == "__main__":
    unittest.main()
