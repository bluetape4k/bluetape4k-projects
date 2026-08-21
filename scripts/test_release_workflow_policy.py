#!/usr/bin/env python3

import re
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).resolve().parents[1]
WORKFLOWS = REPOSITORY / ".github" / "workflows"

MAVEN_RELEASE_TASK = re.compile(
    r"(?<![0-9A-Za-z_])nmcpPublishAggregationToCentralPortal(?![0-9A-Za-z_])"
)
MAVEN_SNAPSHOT_TASK = re.compile(
    r"(?<![0-9A-Za-z_])nmcpPublishAggregationToCentralPortalSnapshots(?![0-9A-Za-z_])"
)
GITHUB_RELEASE = re.compile(
    r"(?:gh\s+release(?:\s|$)|softprops/action-gh-release|actions/create-release|"
    r"gh\s+api[^\n]*/releases(?:\s|$))",
    re.IGNORECASE | re.MULTILINE,
)
ISSUE_RELEASE_MACHINERY = re.compile(
    r"(?:issue[-_ ]?754[^\n]*(?:release|tag|settings|candidate)|"
    r"(?:release|tag|settings|candidate)[^\n]*issue[-_ ]?754|"
    r"release[-_ ]hold|create-github-app-token|RELEASE_(?:TAG|SETTINGS)_|"
    r"tag[-_ ]immutability)",
    re.IGNORECASE,
)


def job_ids(workflow: str) -> set[str]:
    jobs = workflow.split("\njobs:\n", 1)
    if len(jobs) != 2:
        return set()
    return set(re.findall(r"^  ([a-z0-9_-]+):\s*$", jobs[1], re.MULTILINE))


def release_policy_errors(workflow: str) -> list[str]:
    errors = []
    if len(MAVEN_RELEASE_TASK.findall(workflow)) != 1 or MAVEN_SNAPSHOT_TASK.search(workflow):
        errors.append("release workflow must invoke the exact Maven release task")
    if GITHUB_RELEASE.search(workflow):
        errors.append("release workflow must not create a GitHub Release")
    if "contents: write" in workflow:
        errors.append("release workflow must not request write access to repository contents")
    if ISSUE_RELEASE_MACHINERY.search(workflow):
        errors.append("release workflow must not contain issue-specific release machinery")
    if job_ids(workflow) != {"resolve-version", "testcontainers-image-gate", "publish"}:
        errors.append("release workflow must contain resolve-version, testcontainers-image-gate, and publish jobs")
    if "needs: [resolve-version, testcontainers-image-gate]" not in workflow:
        errors.append("publish must depend on the full Testcontainers image gate")
    if "--scope full" not in workflow or "coverage=52/52" not in workflow:
        errors.append("release workflow must verify the full 52/52 image gate")
    return errors


def snapshot_policy_errors(workflow: str) -> list[str]:
    errors = []
    if len(MAVEN_SNAPSHOT_TASK.findall(workflow)) != 1:
        errors.append("snapshot workflow must invoke the exact Maven snapshot task")
    if GITHUB_RELEASE.search(workflow) or ISSUE_RELEASE_MACHINERY.search(workflow):
        errors.append("snapshot workflow must not contain release or issue-specific machinery")
    if "contents: write" in workflow:
        errors.append("snapshot workflow must not request write access to repository contents")
    if job_ids(workflow) != {"publish"}:
        errors.append("snapshot workflow must contain only the publish job")
    return errors


def runtime_policy_errors() -> list[str]:
    errors = []
    candidates = list(WORKFLOWS.glob("*.yml")) + list(WORKFLOWS.glob("*.yaml"))
    candidates.extend(
        path
        for path in (REPOSITORY / "scripts").rglob("*")
        if path.is_file() and path.suffix in {".bash", ".py", ".rb", ".sh"}
    )
    for path in candidates:
        if path.resolve() == Path(__file__).resolve():
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if GITHUB_RELEASE.search(text) or ISSUE_RELEASE_MACHINERY.search(text):
            errors.append(path.relative_to(REPOSITORY).as_posix())
    return errors


class ReleaseWorkflowPolicyTest(unittest.TestCase):

    def test_semantic_checker_rejects_snapshot_task_and_release_action(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "nmcpPublishAggregationToCentralPortal\n",
            "nmcpPublishAggregationToCentralPortalSnapshots\n",
        ) + "\n      - uses: softprops/action-gh-release@v2\n"

        errors = release_policy_errors(mutated)

        self.assertIn("release workflow must invoke the exact Maven release task", errors)
        self.assertIn("release workflow must not create a GitHub Release", errors)

    def test_release_workflow_publishes_only_to_maven_central(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")

        self.assertIn("Publish RELEASE to Maven Central Portal", workflow)
        self.assertEqual([], release_policy_errors(workflow))

    def test_release_workflow_blocks_publish_without_full_image_gate(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace("needs: [resolve-version, testcontainers-image-gate]", "needs: resolve-version")
        errors = release_policy_errors(mutated)
        self.assertIn("publish must depend on the full Testcontainers image gate", errors)

    def test_snapshot_workflow_is_not_coupled_to_issue_754_release_state(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("Publish SNAPSHOT to Maven Central", workflow)
        self.assertEqual([], snapshot_policy_errors(workflow))

    def test_runtime_surfaces_have_no_renamed_release_machinery(self) -> None:
        self.assertEqual([], runtime_policy_errors())

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

    def test_pull_request_ci_runs_this_policy(self) -> None:
        workflow = (WORKFLOWS / "ci.yml").read_text(encoding="utf-8")

        self.assertIn("name: Release Workflow Policy", workflow)
        self.assertIn(
            "python3 -m unittest scripts/test_release_workflow_policy.py -v",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
