from __future__ import annotations

import subprocess
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SCRIPT = REPO_ROOT / "scripts" / "ci" / "resolve_release_target.py"
WORKFLOW = REPO_ROOT / ".github" / "workflows" / "release.yml"
CI_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "ci.yml"


class ResolveReleaseTargetCliTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tempdir = tempfile.TemporaryDirectory()
        self.addCleanup(self.tempdir.cleanup)
        root = Path(self.tempdir.name)
        self.remote = root / "remote.git"
        self.repository = root / "repository"

        self.run_git(root, "init", "--bare", str(self.remote))
        self.run_git(root, "init", "-b", "develop", str(self.repository))
        self.run_git(self.repository, "config", "user.name", "Release Test")
        self.run_git(self.repository, "config", "user.email", "release@example.com")
        self.run_git(self.repository, "config", "commit.gpgSign", "false")
        self.run_git(self.repository, "config", "tag.gpgSign", "false")
        self.run_git(self.repository, "remote", "add", "origin", str(self.remote))
        (self.repository / "release.txt").write_text("candidate\n", encoding="utf-8")
        self.run_git(self.repository, "add", "release.txt")
        self.run_git(self.repository, "commit", "-m", "release candidate")
        self.run_git(self.repository, "push", "-u", "origin", "develop")
        self.candidate_sha = self.git_output(self.repository, "rev-parse", "HEAD")

    def run_git(self, cwd: Path, *args: str) -> None:
        subprocess.run(
            ["git", *args],
            cwd=cwd,
            check=True,
            text=True,
            capture_output=True,
        )

    def git_output(self, cwd: Path, *args: str) -> str:
        return subprocess.run(
            ["git", *args],
            cwd=cwd,
            check=True,
            text=True,
            capture_output=True,
        ).stdout.strip()

    def create_tag(self, *, annotated: bool) -> None:
        args = ["tag"]
        if annotated:
            args.extend(["-a", "-m", "2.0.0"])
        args.append("2.0.0")
        self.run_git(self.repository, *args)
        self.run_git(self.repository, "push", "origin", "refs/tags/2.0.0")

    def run_script(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                "python3",
                str(SCRIPT),
                *args,
                "--repository",
                str(self.repository),
            ],
            check=False,
            text=True,
            capture_output=True,
        )

    def assert_resolved_candidate(
        self, result: subprocess.CompletedProcess[str]
    ) -> None:
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("version=2.0.0", result.stdout)
        self.assertIn("tag=refs/tags/2.0.0", result.stdout)
        self.assertIn(f"target_sha={self.candidate_sha}", result.stdout)

    def test_resolve_peels_annotated_tag_to_commit(self) -> None:
        self.create_tag(annotated=True)

        result = self.run_script(
            "resolve",
            "--version",
            "2.0.0",
            "--event-name",
            "workflow_dispatch",
        )

        self.assert_resolved_candidate(result)

    def test_resolve_accepts_lightweight_tag(self) -> None:
        self.create_tag(annotated=False)

        result = self.run_script(
            "resolve",
            "--version",
            "2.0.0",
            "--event-name",
            "push",
            "--event-sha",
            self.candidate_sha,
        )

        self.assert_resolved_candidate(result)

    def test_resolve_rejects_push_event_sha_mismatch(self) -> None:
        self.create_tag(annotated=False)

        result = self.run_script(
            "resolve",
            "--version",
            "2.0.0",
            "--event-name",
            "push",
            "--event-sha",
            "0" * 40,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("does not match push event SHA", result.stderr)

    def test_verify_rejects_tag_moved_after_resolution(self) -> None:
        self.create_tag(annotated=True)
        (self.repository / "release.txt").write_text("moved\n", encoding="utf-8")
        self.run_git(self.repository, "add", "release.txt")
        self.run_git(self.repository, "commit", "-m", "move release tag")
        self.run_git(self.repository, "tag", "-f", "-a", "2.0.0", "-m", "moved")
        self.run_git(self.repository, "push", "--force", "origin", "refs/tags/2.0.0")

        result = self.run_script(
            "verify",
            "--version",
            "2.0.0",
            "--expected-sha",
            self.candidate_sha,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("moved from expected commit", result.stderr)

    def test_resolve_rejects_missing_tag(self) -> None:
        result = self.run_script(
            "resolve",
            "--version",
            "2.0.0",
            "--event-name",
            "workflow_dispatch",
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("does not exist on origin", result.stderr)

    def test_resolve_preserves_hyphenated_prerelease_version_support(self) -> None:
        self.run_git(self.repository, "tag", "2.0.0-rc-build.1")
        self.run_git(
            self.repository,
            "push",
            "origin",
            "refs/tags/2.0.0-rc-build.1",
        )

        result = self.run_script(
            "resolve",
            "--version",
            "2.0.0-rc-build.1",
            "--event-name",
            "workflow_dispatch",
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("version=2.0.0-rc-build.1", result.stdout)


class ReleaseWorkflowPolicyTest(unittest.TestCase):
    def test_release_jobs_checkout_and_verify_one_immutable_target(self) -> None:
        workflow = WORKFLOW.read_text(encoding="utf-8")

        self.assertIn("target_sha: ${{ steps.resolve.outputs.target_sha }}", workflow)
        self.assertGreaterEqual(
            workflow.count("ref: ${{ needs.resolve-version.outputs.target_sha }}"),
            5,
        )
        self.assertNotIn("needs.resolve-version.outputs.ref", workflow)
        self.assertGreaterEqual(workflow.count("resolve_release_target.py verify"), 5)
        self.assertIn("Source commit", workflow)

    def test_ci_runs_immutable_release_target_contract(self) -> None:
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")

        self.assertIn(
            "python3 -m unittest scripts/ci/resolve_release_target_test.py -v",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
