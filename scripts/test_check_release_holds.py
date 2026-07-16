#!/usr/bin/env python3

import copy
import hashlib
import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path


REPOSITORY = Path(__file__).resolve().parents[1]
VALIDATOR = REPOSITORY / "scripts" / "check-release-holds.py"
SECOND_SHA = "2" * 40
THIRD_SHA = "3" * 40

REQUIRED_EVIDENCE = {
    "contract": [
        ("contract-abi", "docs/evidence/issue-754/contract/abi-report.json"),
    ],
    "core-serializers": [
        ("core-rolling-compatibility", "docs/evidence/issue-754/core-serializers/rolling-compatibility.json"),
    ],
    "json-serializers": [
        ("json-security-compatibility", "docs/evidence/issue-754/json-serializers/security-compatibility.json"),
    ],
    "avro-serializers": [
        ("avro-security-ocf", "docs/evidence/issue-754/avro-serializers/security-ocf.json"),
    ],
    "allocation-proof": [
        ("proof-docs", "docs/evidence/issue-754/allocation-proof/docs-report.json"),
        ("proof-allocation", "docs/evidence/issue-754/allocation-proof/allocation-report.json"),
    ],
}

CHECKSUM_EXTRAS = {
    "contract": [
        "docs/evidence/issue-754/contract/baseline-manifest.json",
        "docs/evidence/issue-754/contract/release-hold-report.json",
    ],
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(root: Path, *args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=root, text=True, capture_output=True, check=True
    ).stdout.strip()


def tested_path(path: str) -> bool:
    return (
        path.startswith(("io/io/src/", "io/json/src/", "io/avro/src/", "benchmark/serializer-bytebuffer-benchmark/"))
        or path.startswith(("buildSrc/", "gradle/"))
        or path in {"build.gradle.kts", "settings.gradle.kts", "gradle.properties"}
        or path.endswith("/build.gradle.kts")
        or (path.startswith("scripts/") and (
            path == "scripts/check-serializer-buffer-abi.sh"
            or path == "scripts/check-release-holds.py"
            or path == "scripts/issue-754-github-settings.py"
            or path.startswith("scripts/check-issue-754-")
        ))
        or (path.startswith(".github/workflows/") and path.endswith((".yml", ".yaml")))
    )


def tested_code_digest(root: Path, commit: str) -> str:
    digest = hashlib.sha256()
    paths = git(root, "ls-tree", "-r", "--name-only", commit).splitlines()
    for path in sorted(path for path in paths if tested_path(path)):
        content = subprocess.run(
            ["git", "show", f"{commit}:{path}"], cwd=root, capture_output=True, check=True
        ).stdout
        digest.update(path.encode("utf-8"))
        digest.update(b"\0")
        digest.update(hashlib.sha256(content).hexdigest().encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest()


class ReleaseHoldFixture:
    SLICE_NAMES = [
        "contract",
        "core-serializers",
        "json-serializers",
        "avro-serializers",
        "allocation-proof",
    ]

    def __init__(self, root: Path, complete: bool = True):
        self.root = root
        self._initialize_candidate_repository()
        self.manifest_path = root / ".github/release-holds/1.12.0-issue-754.json"
        self.manifest_path.parent.mkdir(parents=True, exist_ok=True)
        self.candidate_sha = git(root, "rev-parse", "HEAD")
        self.candidate_tree = git(root, "rev-parse", "HEAD^{tree}")
        self.tested_code_tree_sha256 = tested_code_digest(root, self.candidate_sha)
        self.manifest = {
            "schemaVersion": 1,
            "release": "1.12.0",
            "issue": 754,
            "issueState": "closed" if complete else "open",
            "releaseCandidateSha": self.candidate_sha,
            "testedCodeTreeSha256": self.tested_code_tree_sha256,
            "slices": [],
        }
        for index, name in enumerate(self.SLICE_NAMES, start=1):
            head_sha = f"{index:x}" * 40
            merge_sha = self.candidate_sha if index == len(self.SLICE_NAMES) else f"{index + 5:x}" * 40
            tree_sha = f"{index + 10:x}" * 40
            evidence_refs = []
            for kind, evidence_name in REQUIRED_EVIDENCE[name]:
                evidence_path = Path(evidence_name)
                absolute_evidence = root / evidence_path
                absolute_evidence.parent.mkdir(parents=True, exist_ok=True)
                if kind == "contract-abi":
                    evidence = {
                        "schemaVersion": 1,
                        "issue": 754,
                        "release": "1.12.0",
                        "slice": name,
                        "status": "GREEN",
                        "producerCommit": self.candidate_sha,
                        "producerTree": self.candidate_tree,
                        "testedCodeTreeSha256": self.tested_code_tree_sha256,
                        "authority": {"commit": SECOND_SHA, "tree": THIRD_SHA},
                        "command": "fixture",
                        "checks": {"abi": "PASS"},
                        "artifacts": [],
                        "textReport": "fixture.txt",
                    }
                else:
                    evidence = {
                        "schemaVersion": 1,
                        "evidenceKind": kind,
                        "slice": name,
                        "prNumber": 800 + index,
                        "headSha": head_sha,
                        "headTreeSha": tree_sha,
                        "mergeSha": merge_sha,
                        "mergeTreeSha": tree_sha,
                        "releaseCandidateSha": self.candidate_sha,
                        "testedCodeTreeSha256": self.tested_code_tree_sha256,
                        "conclusion": "PASS",
                    }
                absolute_evidence.write_text(json.dumps(evidence, sort_keys=True) + "\n", encoding="utf-8")
                evidence_refs.append(
                    {"kind": kind, "path": evidence_path.as_posix(), "sha256": sha256(absolute_evidence)}
                )

            if name == "contract":
                for extra in CHECKSUM_EXTRAS[name]:
                    extra_path = root / extra
                    extra_path.parent.mkdir(parents=True, exist_ok=True)
                    extra_path.write_text(json.dumps({"fixture": extra}, sort_keys=True) + "\n", encoding="utf-8")

            checksum_path = Path(f"docs/evidence/issue-754/{name}/SHA256SUMS")
            self.manifest["slices"].append(
                {
                    "name": name,
                    "prNumber": 800 + index if complete else None,
                    "prState": "merged" if complete else "missing",
                    "expectedHeadSha": head_sha if complete else None,
                    "expectedHeadTreeSha": tree_sha if complete else None,
                    "expectedMergeSha": merge_sha if complete else None,
                    "expectedMergeTreeSha": tree_sha if complete else None,
                    "evidence": evidence_refs,
                    "checksumManifest": {"path": checksum_path.as_posix(), "sha256": None},
                }
            )
            self.refresh_checksum_manifest(index - 1)
        self.write()

    def _initialize_candidate_repository(self):
        git(self.root, "init", "-q")
        git(self.root, "config", "user.name", "Fixture")
        git(self.root, "config", "user.email", "fixture@example.invalid")
        files = {
            "io/io/src/main/kotlin/Serializer.kt": "interface Serializer\n",
            "io/json/src/main/kotlin/JsonSerializer.kt": "interface JsonSerializer\n",
            "io/avro/src/main/kotlin/AvroSerializer.kt": "interface AvroSerializer\n",
            "benchmark/serializer-bytebuffer-benchmark/build.gradle.kts": "plugins {}\n",
            "scripts/check-release-holds.py": "# fixture validator\n",
            ".github/workflows/release.yml": "name: Release\n",
            "build.gradle.kts": "plugins {}\n",
            "docs/README.md": "excluded\n",
        }
        for name, content in files.items():
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        git(self.root, "add", *files.keys())
        git(self.root, "commit", "--allow-empty", "-qm", "fixture candidate")

    def write(self):
        self.manifest_path.write_text(
            json.dumps(self.manifest, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )

    def evidence(self, index: int, evidence_index: int = 0) -> tuple[Path, dict]:
        entry = self.manifest["slices"][index]["evidence"][evidence_index]
        path = self.root / entry["path"]
        return path, json.loads(path.read_text(encoding="utf-8"))

    def rewrite_evidence(self, index: int, payload: dict, update_checksum: bool = True, evidence_index: int = 0):
        path, _ = self.evidence(index, evidence_index)
        path.write_text(json.dumps(payload, sort_keys=True) + "\n", encoding="utf-8")
        if update_checksum:
            self.manifest["slices"][index]["evidence"][evidence_index]["sha256"] = sha256(path)
            self.refresh_checksum_manifest(index)
        self.write()

    def refresh_checksum_manifest(self, index: int):
        slice_entry = self.manifest["slices"][index]
        name = slice_entry["name"]
        paths = [entry["path"] for entry in slice_entry["evidence"]] + CHECKSUM_EXTRAS.get(name, [])
        checksum_path = self.root / slice_entry["checksumManifest"]["path"]
        checksum_path.parent.mkdir(parents=True, exist_ok=True)
        checksum_path.write_text(
            "".join(f"{sha256(self.root / path)}  {path}\n" for path in sorted(paths)),
            encoding="utf-8",
        )
        slice_entry["checksumManifest"]["sha256"] = sha256(checksum_path)

    def commit_change(self, path: str, content: str) -> str:
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        git(self.root, "add", path)
        git(self.root, "commit", "-qm", f"change {path}")
        return git(self.root, "rev-parse", "HEAD")

    def retarget_candidate(self, candidate: str, *, refresh_digest: bool):
        digest = tested_code_digest(self.root, candidate) if refresh_digest else self.manifest["testedCodeTreeSha256"]
        self.manifest["releaseCandidateSha"] = candidate
        self.candidate_sha = candidate
        self.manifest["testedCodeTreeSha256"] = digest
        candidate_tree = git(self.root, "rev-parse", f"{candidate}^{{tree}}")
        for index, entry in enumerate(self.manifest["slices"]):
            if index == len(self.SLICE_NAMES) - 1:
                entry["expectedMergeSha"] = candidate
            for evidence_index, reference in enumerate(entry["evidence"]):
                path, payload = self.evidence(index, evidence_index)
                payload["testedCodeTreeSha256"] = digest
                if reference["kind"] == "contract-abi":
                    payload["producerCommit"] = candidate
                    payload["producerTree"] = candidate_tree
                else:
                    payload["releaseCandidateSha"] = candidate
                    if index == len(self.SLICE_NAMES) - 1:
                        payload["mergeSha"] = candidate
                path.write_text(json.dumps(payload, sort_keys=True) + "\n", encoding="utf-8")
                reference["sha256"] = sha256(path)
            self.refresh_checksum_manifest(index)
        self.write()


class ReleaseHoldValidatorTest(unittest.TestCase):
    def run_validator(self, fixture: ReleaseHoldFixture, candidate=None):
        return subprocess.run(
            [
                "python3",
                str(VALIDATOR),
                "--manifest",
                str(fixture.manifest_path),
                "--repository",
                str(fixture.root),
                "--release-candidate",
                candidate or fixture.candidate_sha,
            ],
            text=True,
            capture_output=True,
            check=False,
        )

    def assert_invalid(self, result: subprocess.CompletedProcess, message: str):
        self.assertEqual(2, result.returncode, result.stdout + result.stderr)
        self.assertIn(message, result.stdout + result.stderr)

    def assert_hold(self, result: subprocess.CompletedProcess, message: str):
        self.assertEqual(3, result.returncode, result.stdout + result.stderr)
        self.assertIn("HOLD", result.stdout)
        self.assertIn(message, result.stdout)

    def test_open_issue_holds(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory), complete=False)
            self.assert_hold(self.run_validator(fixture), "issue 754 is open")

    def test_missing_or_unmerged_pr_holds(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            fixture.manifest["slices"][2]["prState"] = "open"
            fixture.write()
            self.assert_hold(self.run_validator(fixture), "json-serializers PR is not merged")

            fixture.manifest["slices"][2]["prNumber"] = None
            fixture.manifest["slices"][2]["prState"] = "missing"
            fixture.write()
            self.assert_hold(self.run_validator(fixture), "json-serializers PR is missing")

    def test_missing_and_unknown_manifest_fields_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            del fixture.manifest["releaseCandidateSha"]
            fixture.write()
            self.assert_invalid(self.run_validator(fixture), "missing fields: releaseCandidateSha")

            fixture = ReleaseHoldFixture(Path(directory))
            fixture.manifest["bypass"] = True
            fixture.write()
            self.assert_invalid(self.run_validator(fixture), "unknown fields: bypass")

    def test_unknown_slice_and_evidence_fields_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            fixture.manifest["slices"][0]["manualOverride"] = False
            fixture.write()
            self.assert_invalid(self.run_validator(fixture), "unknown fields: manualOverride")

            fixture = ReleaseHoldFixture(Path(directory))
            path, payload = fixture.evidence(0)
            payload["unexpected"] = "value"
            fixture.rewrite_evidence(0, payload)
            self.assert_invalid(self.run_validator(fixture), "unknown evidence fields: unexpected")

    def test_checksum_mismatch_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            path, payload = fixture.evidence(0)
            payload["conclusion"] = "HOLD"
            fixture.rewrite_evidence(0, payload, update_checksum=False)
            self.assert_invalid(self.run_validator(fixture), "checksum mismatch")

    def test_head_and_tree_mismatch_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            _, payload = fixture.evidence(1)
            payload["headSha"] = THIRD_SHA
            fixture.rewrite_evidence(1, payload)
            self.assert_invalid(self.run_validator(fixture), "head SHA mismatch")

            fixture = ReleaseHoldFixture(Path(directory))
            _, payload = fixture.evidence(1)
            payload["headTreeSha"] = SECOND_SHA
            fixture.rewrite_evidence(1, payload)
            self.assert_invalid(self.run_validator(fixture), "head tree mismatch")

    def test_merge_and_tree_mismatch_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            _, payload = fixture.evidence(3)
            payload["mergeSha"] = SECOND_SHA
            fixture.rewrite_evidence(3, payload)
            self.assert_invalid(self.run_validator(fixture), "merge SHA mismatch")

            fixture = ReleaseHoldFixture(Path(directory))
            _, payload = fixture.evidence(3)
            payload["mergeTreeSha"] = THIRD_SHA
            fixture.rewrite_evidence(3, payload)
            self.assert_invalid(self.run_validator(fixture), "merge tree mismatch")

    def test_squash_merge_tree_mismatch_holds(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            fixture.manifest["slices"][4]["expectedMergeTreeSha"] = SECOND_SHA
            for evidence_index in range(len(fixture.manifest["slices"][4]["evidence"])):
                _, payload = fixture.evidence(4, evidence_index)
                payload["mergeTreeSha"] = SECOND_SHA
                fixture.rewrite_evidence(4, payload, evidence_index=evidence_index)
            self.assert_hold(self.run_validator(fixture), "head and merge trees differ")

    def test_stale_release_candidate_holds(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            self.assert_hold(
                self.run_validator(fixture, candidate=SECOND_SHA),
                "release candidate SHA does not match",
            )

    def test_complete_manifest_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            result = self.run_validator(fixture)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertIn("PASS", result.stdout)

    def test_covered_candidate_tree_drift_fails_but_excluded_evidence_drift_does_not_change_digest(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            original_digest = fixture.tested_code_tree_sha256
            covered_candidate = fixture.commit_change(
                "io/io/src/main/kotlin/Serializer.kt", "interface Serializer { fun changed(): Unit }\n"
            )
            self.assertNotEqual(original_digest, tested_code_digest(fixture.root, covered_candidate))
            fixture.retarget_candidate(covered_candidate, refresh_digest=False)
            self.assert_invalid(self.run_validator(fixture), "tested code tree SHA-256 mismatch")

        with tempfile.TemporaryDirectory() as directory:
            fixture = ReleaseHoldFixture(Path(directory))
            original_digest = fixture.tested_code_tree_sha256
            docs_candidate = fixture.commit_change(
                "docs/evidence/issue-754/notes.md", "excluded evidence-only change\n"
            )
            self.assertEqual(original_digest, tested_code_digest(fixture.root, docs_candidate))
            fixture.retarget_candidate(docs_candidate, refresh_digest=True)
            result = self.run_validator(fixture)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_required_slice_evidence_rejects_omitted_duplicate_and_wrong_kind_or_path(self):
        mutations = []

        def omitted(fixture):
            fixture.manifest["slices"][2]["evidence"] = []

        mutations.append((omitted, "required evidence"))

        def duplicate(fixture):
            fixture.manifest["slices"][1]["evidence"].append(
                copy.deepcopy(fixture.manifest["slices"][1]["evidence"][0])
            )

        mutations.append((duplicate, "duplicate evidence"))

        def wrong_kind(fixture):
            fixture.manifest["slices"][3]["evidence"][0]["kind"] = "avro-compat"

        mutations.append((wrong_kind, "required evidence"))

        def wrong_path(fixture):
            fixture.manifest["slices"][0]["evidence"][0]["path"] = "docs/evidence/issue-754/contract/wrong.json"

        mutations.append((wrong_path, "required evidence"))

        for mutation, message in mutations:
            with self.subTest(message=message), tempfile.TemporaryDirectory() as directory:
                fixture = ReleaseHoldFixture(Path(directory))
                mutation(fixture)
                fixture.write()
                self.assert_invalid(self.run_validator(fixture), message)

    def test_checksum_manifests_reject_missing_extra_tampered_escape_and_self_reference(self):
        cases = []

        def missing(fixture):
            del fixture.manifest["slices"][0]["checksumManifest"]

        cases.append((missing, "missing fields: checksumManifest"))

        def extra(fixture):
            entry = fixture.manifest["slices"][1]
            path = fixture.root / entry["checksumManifest"]["path"]
            path.write_text(path.read_text() + f"{'0' * 64}  docs/extra.json\n", encoding="utf-8")
            entry["checksumManifest"]["sha256"] = sha256(path)

        cases.append((extra, "unexpected paths"))

        def tampered(fixture):
            path, payload = fixture.evidence(2)
            payload["conclusion"] = "HOLD"
            path.write_text(json.dumps(payload, sort_keys=True) + "\n", encoding="utf-8")

        cases.append((tampered, "checksum mismatch"))

        def escape(fixture):
            entry = fixture.manifest["slices"][2]
            entry["checksumManifest"]["path"] = "../SHA256SUMS"

        cases.append((escape, "escapes repository"))

        def self_reference(fixture):
            entry = fixture.manifest["slices"][3]
            path = fixture.root / entry["checksumManifest"]["path"]
            path.write_text(
                path.read_text() + f"{'0' * 64}  {entry['checksumManifest']['path']}\n",
                encoding="utf-8",
            )
            entry["checksumManifest"]["sha256"] = sha256(path)

        cases.append((self_reference, "must not reference itself"))

        for mutation, message in cases:
            with self.subTest(message=message), tempfile.TemporaryDirectory() as directory:
                fixture = ReleaseHoldFixture(Path(directory))
                mutation(fixture)
                fixture.write()
                self.assert_invalid(self.run_validator(fixture), message)


class WorkflowHoldAuditTest(unittest.TestCase):
    def run_audit(self, repository: Path):
        return subprocess.run(
            ["python3", str(VALIDATOR), "--audit-workflows", "--repository", str(repository)],
            text=True,
            capture_output=True,
            check=False,
        )

    def test_repository_workflows_have_exact_hold_and_side_effect_dependencies(self):
        result = self.run_audit(REPOSITORY)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("WORKFLOW AUDIT PASS", result.stdout)

        snapshot = (REPOSITORY / ".github/workflows/publish-snapshot.yml").read_text()
        release = (REPOSITORY / ".github/workflows/release.yml").read_text()
        for workflow in (snapshot, release):
            self.assertIn("name: release-hold-1.12.0-issue-754", workflow)
        self.assertNotIn("push:\n    tags:", release)
        self.assertIn("run-name: 'issue-754-release-${{ inputs.phase }}-${{ inputs.request_id }}'", release)
        self.assertIn("version:", release)
        self.assertIn("request_id:", release)
        self.assertIn("candidate-sha-guard:", release)
        self.assertIn("issue-754-tag-immutability:", release)
        self.assertIn("issue-754-tag-immutability-prepare:", release)
        self.assertIn("environment: snapshot-publish-1.12.0", snapshot)
        snapshot_guard = snapshot.split("  candidate-sha-guard:", 1)[1].split(
            "\n  release-hold-1-12-0-issue-754:", 1
        )[0]
        self.assertIn("EVENT_NAME: ${{ github.event_name }}", snapshot_guard)
        self.assertIn('if [[ "$EVENT_NAME" == "workflow_dispatch"', snapshot_guard)
        self.assertIn("environment: release-tag-1.12.0", release)
        self.assertIn("issue-754-release-candidate-${{ inputs.candidate_validation_request_id }}", release)
        self.assertIn("issue-754-tag-immutability-${{ inputs.request_id }}", release)
        self.assertIn("validate-issue-754-release-candidate.yml", release)
        self.assertIn("issue-754-candidate-${{ inputs.candidate_validation_request_id }}", release)
        self.assertIn("issue-754-release-candidate", release)
        self.assertIn("/jobs", release)
        self.assertIn("verify-candidate-artifact", release)
        self.assertIn("immutable-closeout", release)
        self.assertIn("verify-immutable-closeout", release)

        prepare_job = release.split("  issue-754-tag-immutability-prepare:", 1)[1].split(
            "\n  issue-754-tag-immutability:", 1
        )[0]
        self.assertIn("RELEASE_TAG_APP_ID", prepare_job)
        self.assertIn("RELEASE_SETTINGS_APP_ID", prepare_job)
        self.assertIn("immutable-closeout", prepare_job)
        prepare_permissions = prepare_job.split("    permissions:\n", 1)[1].split("    steps:\n", 1)[0]
        self.assertIn("contents: write", prepare_permissions)
        self.assertNotIn("gh api --method POST", prepare_job)
        self.assertLess(prepare_job.index("immutable-closeout"), prepare_job.index("upload-artifact"))

        publish_immutability = release.split("  issue-754-tag-immutability:", 1)[1].split("\n  publish:", 1)[0]
        self.assertIn("GH_TOKEN: ${{ github.token }}", publish_immutability)
        self.assertNotIn("create-github-app-token", publish_immutability)
        self.assertNotIn("environment:", publish_immutability)
        self.assertNotIn("immutable-closeout --", publish_immutability)

    def write_workflows(self, root: Path, snapshot: str, release: str, extra=None):
        workflows = root / ".github/workflows"
        workflows.mkdir(parents=True)
        (workflows / "publish-snapshot.yml").write_text(snapshot, encoding="utf-8")
        (workflows / "release.yml").write_text(release, encoding="utf-8")
        if extra:
            (workflows / "extra.yml").write_text(extra, encoding="utf-8")

    def test_audit_rejects_release_environment_outside_release_workflow(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(root, "jobs: {}\n", "jobs: {}\n", "jobs:\n  x:\n    environment: release-tag-1.12.0\n")
            result = self.run_audit(root)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("release-tag-1.12.0 outside release.yml", result.stdout + result.stderr)

    def test_audit_accepts_read_only_tag_lookup_with_generic_download_token(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(
                root,
                """jobs:
  release-hold-1-12-0-issue-754:
    name: release-hold-1.12.0-issue-754
""",
                """on: workflow_dispatch
jobs:
  candidate-sha-guard:
    steps:
      - run: test "${{ github.sha }}" = "${{ inputs.candidate_sha }}"
  release-hold-1-12-0-issue-754:
    name: release-hold-1.12.0-issue-754
  issue-754-tag-immutability:
    needs: [candidate-sha-guard, release-hold-1-12-0-issue-754]
    permissions:
      actions: read
      contents: read
    steps:
      - name: Download only retained artifact
        run: gh run download 123
        env:
          GH_TOKEN: ${{ github.token }}
      - name: Read tag target
        run: git ls-remote origin refs/tags/1.12.0
""",
            )
            result = self.run_audit(root)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_audit_rejects_generic_token_tag_creation(self):
        commands = [
            "git tag 1.12.0",
            "git push origin refs/tags/1.12.0",
            "gh api --method POST repos/example/project/git/refs -f ref=refs/tags/1.12.0",
        ]
        for command in commands:
            with self.subTest(command=command), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                self.write_workflows(
                    root,
                    "jobs: {}\n",
                    f"jobs:\n  create-tag:\n    steps:\n      - run: {command}\n        env:\n          GH_TOKEN: ${{{{ github.token }}}}\n",
                )
                result = self.run_audit(root)
                self.assertNotEqual(0, result.returncode)
                self.assertIn("generic-token tag creation", result.stdout + result.stderr)

    def test_audit_rejects_unheld_publication_path(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(
                root,
                "jobs:\n  publish:\n    steps:\n      - run: ./gradlew nmcpPublishAggregationToCentralPortalSnapshots\n",
                "jobs: {}\n",
            )
            result = self.run_audit(root)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("publish does not need release-hold-1.12.0-issue-754", result.stdout + result.stderr)

    def test_audit_rejects_prepare_tag_completion_without_protected_immutable_closeout(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(
                root,
                "jobs: {}\n",
                """on: workflow_dispatch
jobs:
  candidate-sha-guard:
    steps:
      - run: test "${{ github.sha }}" = "${{ inputs.candidate_sha }}"
  release-hold-1-12-0-issue-754:
    name: release-hold-1.12.0-issue-754
  issue-754-tag-immutability-prepare:
    name: issue-754-tag-immutability
    needs: [candidate-sha-guard, release-hold-1-12-0-issue-754]
    environment: release-tag-1.12.0
    steps:
      - run: git tag 1.12.0
  issue-754-tag-immutability:
    permissions:
      actions: read
      contents: read
    steps:
      - name: Download only authority artifact
        run: gh run download 1
        env:
          GH_TOKEN: ${{ github.token }}
""",
            )
            result = self.run_audit(root)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("prepare immutability job must own immutable-closeout", result.stdout + result.stderr)

    def test_audit_rejects_candidate_guard_without_moved_ref_check(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(
                root,
                "jobs: {}\n",
                """on: workflow_dispatch
jobs:
  candidate-sha-guard:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
        with:
          ref: ${{ inputs.candidate_sha }}
  release-hold-1-12-0-issue-754:
    name: release-hold-1.12.0-issue-754
    runs-on: ubuntu-latest
  issue-754-tag-immutability:
    permissions:
      actions: read
      contents: read
    steps:
      - run: gh run download 1
        env:
          GH_TOKEN: ${{ github.token }}
""",
            )
            result = self.run_audit(root)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("candidate-sha-guard does not reject moved refs", result.stdout + result.stderr)

    def test_audit_rejects_excess_immutability_permissions_and_authentication(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.write_workflows(
                root,
                "jobs: {}\n",
                """on: workflow_dispatch
jobs:
  candidate-sha-guard:
    steps:
      - run: test \"${{ github.sha }}\" = \"${{ inputs.candidate_sha }}\"
  release-hold-1-12-0-issue-754:
    name: release-hold-1.12.0-issue-754
    runs-on: ubuntu-latest
  issue-754-tag-immutability:
    permissions:
      actions: write
      contents: read
    steps:
      - name: Download only authority artifact
        run: gh run download 1
        env:
          GH_TOKEN: ${{ secrets.RELEASE_TOKEN }}
""",
            )
            result = self.run_audit(root)
            output = result.stdout + result.stderr
            self.assertNotEqual(0, result.returncode)
            self.assertIn("permissions must be exactly actions: read and contents: read", output)
            self.assertIn("excess authentication", output)


if __name__ == "__main__":
    unittest.main()
