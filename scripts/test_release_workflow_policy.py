#!/usr/bin/env python3

import json
import re
import unittest
from pathlib import Path

from scripts.validate_nightly_matrix import (
    REQUIRED_JOB_NAMES,
    expected_matrix_names,
    matrix_contract_errors,
    validation_errors,
)

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
    expected_jobs = {
        "resolve-version",
        "testcontainers-manifest-contract",
        "testcontainers-image-gate",
        "testcontainers-ignite2-arm64-image-gate",
        "publish",
    }
    if job_ids(workflow) != expected_jobs:
        errors.append(
            "release workflow must contain resolve-version, testcontainers-manifest-contract, "
            "testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate, and publish jobs"
        )
    if "needs: [resolve-version, testcontainers-manifest-contract]" not in workflow:
        errors.append("full Testcontainers image gate must wait for the manifest contract")
    if (
        "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]"
        not in workflow
    ):
        errors.append("publish must depend on the manifest contract and full image gate")
    if "--scope full" not in workflow or not (
        "coverage=48/48" in workflow or "expected_coverage=\"48/48\"" in workflow
    ):
        errors.append("release workflow must verify the full 48/48 release evidence gate")
    arm_contract = (
        "testcontainers-ignite2-arm64-image-gate" in workflow
        and "--scope family" in workflow
        and "--family-id ignite2" in workflow
        and "--platform-id arm64" in workflow
        and "expected_coverage=\"1/1\"" in workflow
    )
    if not arm_contract:
        errors.append("release workflow must verify the exact Ignite2 arm64 1/1 image gate")
    return errors


def snapshot_policy_errors(workflow: str) -> list[str]:
    errors = []
    if len(MAVEN_SNAPSHOT_TASK.findall(workflow)) != 1:
        errors.append("snapshot workflow must invoke the exact Maven snapshot task")
    if GITHUB_RELEASE.search(workflow) or ISSUE_RELEASE_MACHINERY.search(workflow):
        errors.append("snapshot workflow must not contain release or issue-specific machinery")
    if "contents: write" in workflow:
        errors.append("snapshot workflow must not request write access to repository contents")
    if job_ids(workflow) != {"validate-full-nightly", "publish"}:
        errors.append("snapshot workflow must contain validation and publish jobs")
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

    @staticmethod
    def _nightly_matrix_contract() -> dict:
        return json.loads(
            (REPOSITORY / "scripts" / "nightly_matrix_contract.json").read_text(
                encoding="utf-8"
            )
        )

    def _successful_matrix_jobs(self) -> list[dict[str, str]]:
        expected_names, _ = expected_matrix_names(self._nightly_matrix_contract())
        return [
            {"name": name, "conclusion": "success"}
            for name in sorted(expected_names)
        ]

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
        mutated = workflow.replace(
            "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]",
            "needs: resolve-version",
        )
        errors = release_policy_errors(mutated)
        self.assertIn("publish must depend on the manifest contract and full image gate", errors)

    def test_release_workflow_blocks_image_gate_without_manifest_contract(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "needs: [resolve-version, testcontainers-manifest-contract]",
            "needs: resolve-version",
        )
        errors = release_policy_errors(mutated)
        self.assertIn("full Testcontainers image gate must wait for the manifest contract", errors)

    def test_release_workflow_blocks_arm_gate_without_exact_selector(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace("--platform-id arm64", "--platform-id amd64")
        errors = release_policy_errors(mutated)
        self.assertIn("release workflow must verify the exact Ignite2 arm64 1/1 image gate", errors)

    def test_snapshot_workflow_is_not_coupled_to_issue_754_release_state(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("Publish SNAPSHOT to Maven Central", workflow)
        self.assertEqual([], snapshot_policy_errors(workflow))

    def test_snapshot_workflow_requires_full_nightly_validation_before_publish(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("actions: read", workflow)
        self.assertIn("validate-full-nightly:", workflow)
        self.assertIn("needs: validate-full-nightly", workflow)
        self.assertIn(
            "needs.validate-full-nightly.outputs.publish_eligible == 'true'",
            workflow,
        )
        self.assertIn('validation_run_id:', workflow)
        self.assertIn("required: true", workflow)
        self.assertIn("gh api", workflow)
        self.assertIn("actions/runs/${validation_run_id}", workflow)
        self.assertIn("actions/runs/${validation_run_id}/jobs", workflow)
        self.assertIn(
            "contents/scripts/nightly_matrix_contract.json?ref=${validation_head_sha}",
            workflow,
        )
        self.assertIn(
            "contents/scripts/validate_nightly_matrix.py?ref=${validation_head_sha}",
            workflow,
        )
        self.assertIn("Accept: application/vnd.github.raw", workflow)
        self.assertIn("valid head SHA", workflow)
        self.assertIn('python3 "$validator_script"', workflow)
        self.assertIn("publish_eligible=", workflow)
        self.assertNotIn("override_full_validation", workflow)

    def test_nightly_matrix_contract_matches_current_workflow_groups(self) -> None:
        workflow = (WORKFLOWS / "nightly-tests.yml").read_text(encoding="utf-8")
        contract = self._nightly_matrix_contract()
        expected_groups = {
            group
            for groups in contract["matrix_jobs"].values()
            for group in groups
        }
        workflow_groups = set(
            re.findall(r"^\s*-\s+group:\s*([a-z0-9-]+)", workflow, re.MULTILINE)
        )
        workflow_groups.update(
            re.findall(r'"group":\s*"([a-z0-9-]+)"', workflow)
        )
        self.assertEqual(expected_groups, workflow_groups)
        self.assertEqual(29, len(expected_matrix_names(contract)[0]))

    def test_nightly_matrix_contract_accepts_current_expected_set(self) -> None:
        contract = self._nightly_matrix_contract()
        errors = matrix_contract_errors(self._successful_matrix_jobs(), contract)
        self.assertEqual([], errors)

    def test_nightly_matrix_contract_rejects_missing_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            job
            for job in self._successful_matrix_jobs()
            if job["name"] != "Test / Infra (redis)"
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("missing matrix job: Test / Infra (redis)", errors)

    def test_nightly_matrix_contract_rejects_additional_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = self._successful_matrix_jobs() + [
            {"name": "Test / Infra (unexpected)", "conclusion": "success"}
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("unexpected matrix job: Test / Infra (unexpected)", errors)

    def test_nightly_matrix_contract_rejects_renamed_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            {
                "name": (
                    "Test / Infra (renamed)"
                    if job["name"] == "Test / Infra (redis)"
                    else job["name"]
                ),
                "conclusion": job["conclusion"],
            }
            for job in self._successful_matrix_jobs()
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("missing matrix job: Test / Infra (redis)", errors)
        self.assertIn("unexpected matrix job: Test / Infra (renamed)", errors)

    def test_nightly_matrix_contract_rejects_non_success_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = self._successful_matrix_jobs()
        jobs[0]["conclusion"] = "failure"
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn(f"non-success matrix job: {jobs[0]['name']}", errors)

    def test_nightly_matrix_validation_accepts_complete_successful_run(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            *({"name": name, "conclusion": "success"} for name in REQUIRED_JOB_NAMES),
            *self._successful_matrix_jobs(),
        ]
        head_sha, errors = validation_errors(
            {
                "status": "completed",
                "conclusion": "success",
                "path": ".github/workflows/nightly-tests.yml",
                "head_branch": "develop",
                "head_sha": "a" * 40,
            },
            jobs,
            "a" * 40,
            contract,
        )
        self.assertEqual("a" * 40, head_sha)
        self.assertEqual([], errors)

    def test_snapshot_checkout_uses_validated_nightly_head(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("head_sha", workflow)
        self.assertIn(
            "ref: ${{ needs.validate-full-nightly.outputs.head_sha }}",
            workflow,
        )

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
