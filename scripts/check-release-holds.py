#!/usr/bin/env python3

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SCHEMA_VERSION = 1
RELEASE = "1.12.0"
ISSUE = 754
HOLD_NAME = "release-hold-1.12.0-issue-754"
HOLD_JOB = "release-hold-1-12-0-issue-754"
IMMUTABILITY_JOB = "issue-754-tag-immutability"
SHA_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SLICE_NAMES = [
    "contract",
    "core-serializers",
    "json-serializers",
    "avro-serializers",
    "allocation-proof",
]
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

MANIFEST_FIELDS = {
    "schemaVersion",
    "release",
    "issue",
    "issueState",
    "releaseCandidateSha",
    "testedCodeTreeSha256",
    "slices",
}
SLICE_FIELDS = {
    "name",
    "prNumber",
    "prState",
    "expectedHeadSha",
    "expectedHeadTreeSha",
    "expectedMergeSha",
    "expectedMergeTreeSha",
    "evidence",
    "checksumManifest",
}
EVIDENCE_REF_FIELDS = {"kind", "path", "sha256"}
CHECKSUM_MANIFEST_FIELDS = {"path", "sha256"}
EVIDENCE_REPORT_FIELDS = {
    "schemaVersion",
    "evidenceKind",
    "slice",
    "prNumber",
    "headSha",
    "headTreeSha",
    "mergeSha",
    "mergeTreeSha",
    "releaseCandidateSha",
    "testedCodeTreeSha256",
    "conclusion",
}
ABI_REPORT_FIELDS = {
    "schemaVersion",
    "issue",
    "release",
    "slice",
    "status",
    "producerCommit",
    "producerTree",
    "testedCodeTreeSha256",
    "authority",
    "command",
    "checks",
    "artifacts",
    "textReport",
}


class ManifestError(ValueError):
    pass


@dataclass(frozen=True)
class Decision:
    decision: str
    reasons: list[str]
    release_candidate_sha: str
    manifest_path: str
    tested_code_tree_sha256: str

    def payload(self) -> dict[str, Any]:
        exit_code = 0 if self.decision == "PASS" else 3
        return {
            "schemaVersion": SCHEMA_VERSION,
            "decision": self.decision,
            "exitCode": exit_code,
            "release": RELEASE,
            "issue": ISSUE,
            "releaseCandidateSha": self.release_candidate_sha,
            "testedCodeTreeSha256": self.tested_code_tree_sha256,
            "manifest": self.manifest_path,
            "reasons": self.reasons,
            "exitCodeContract": {"HOLD": 3, "INVALID": 2, "PASS": 0},
        }


def strict_fields(payload: dict[str, Any], expected: set[str], context: str) -> None:
    missing = sorted(expected - payload.keys())
    unknown = sorted(payload.keys() - expected)
    if missing:
        raise ManifestError(f"{context} missing fields: {', '.join(missing)}")
    if unknown:
        label = "unknown evidence fields" if "evidence report" in context.lower() else "unknown fields"
        raise ManifestError(f"{context} {label}: {', '.join(unknown)}")


def require_sha(value: Any, context: str) -> str:
    if not isinstance(value, str) or not SHA_RE.fullmatch(value):
        raise ManifestError(f"{context} must be a lowercase 40-character Git SHA")
    return value


def read_json(path: Path, context: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise ManifestError(f"{context} is missing: {path}") from error
    except json.JSONDecodeError as error:
        raise ManifestError(f"{context} is malformed JSON: {error}") from error
    if not isinstance(value, dict):
        raise ManifestError(f"{context} must be a JSON object")
    return value


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def is_tested_code_path(path: str) -> bool:
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


def git_output(repository: Path, *args: str, text: bool = True) -> Any:
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=repository,
            text=text,
            capture_output=True,
            check=True,
        )
    except subprocess.CalledProcessError as error:
        detail = error.stderr.strip() if isinstance(error.stderr, str) else ""
        raise ManifestError(f"git {' '.join(args)} failed: {detail}") from error
    return result.stdout


def compute_tested_code_tree_sha256(repository: Path, candidate: str) -> str:
    require_sha(candidate, "tested code tree candidate")
    paths = git_output(repository, "ls-tree", "-r", "--name-only", candidate).splitlines()
    digest = hashlib.sha256()
    for path in sorted(path for path in paths if is_tested_code_path(path)):
        content = git_output(repository, "show", f"{candidate}:{path}", text=False)
        digest.update(path.encode("utf-8"))
        digest.update(b"\0")
        digest.update(hashlib.sha256(content).hexdigest().encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest()


def resolve_evidence_path(repository: Path, relative_path: Any) -> Path:
    if not isinstance(relative_path, str) or not relative_path:
        raise ManifestError("evidence path must be a non-empty repository-relative path")
    relative = Path(relative_path)
    if relative.is_absolute() or ".." in relative.parts:
        raise ManifestError(f"evidence path escapes repository: {relative_path}")
    return repository / relative


def require_sha256(value: Any, context: str) -> str:
    if not isinstance(value, str) or not SHA256_RE.fullmatch(value):
        raise ManifestError(f"{context} must be 64 lowercase hex characters")
    return value


def checksum_manifest_path(slice_name: str) -> str:
    return f"docs/evidence/issue-754/{slice_name}/SHA256SUMS"


def expected_checksum_paths(slice_name: str) -> list[str]:
    return sorted(
        [path for _, path in REQUIRED_EVIDENCE[slice_name]]
        + CHECKSUM_EXTRAS.get(slice_name, [])
    )


def validate_checksum_manifest(repository: Path, slice_name: str, reference: dict[str, Any]) -> None:
    strict_fields(reference, CHECKSUM_MANIFEST_FIELDS, f"slice {slice_name} checksum manifest")
    expected_manifest_path = checksum_manifest_path(slice_name)
    manifest_path = resolve_evidence_path(repository, reference["path"])
    if reference["path"] != expected_manifest_path:
        raise ManifestError(
            f"slice {slice_name} checksum manifest path must be {expected_manifest_path}"
        )
    expected_manifest_sha = require_sha256(
        reference["sha256"], f"slice {slice_name} checksum manifest sha256"
    )
    if not manifest_path.is_file():
        raise ManifestError(f"slice {slice_name} checksum manifest is missing: {reference['path']}")
    actual_manifest_sha = file_sha256(manifest_path)
    if actual_manifest_sha != expected_manifest_sha:
        raise ManifestError(
            f"slice {slice_name} checksum manifest checksum mismatch: "
            f"expected {expected_manifest_sha}, actual {actual_manifest_sha}"
        )

    entries: dict[str, str] = {}
    for line_number, line in enumerate(manifest_path.read_text(encoding="utf-8").splitlines(), start=1):
        match = re.fullmatch(r"([0-9a-f]{64})  (\S+)", line)
        if not match:
            raise ManifestError(
                f"slice {slice_name} checksum manifest line {line_number} is malformed"
            )
        checksum, relative_path = match.groups()
        resolve_evidence_path(repository, relative_path)
        if relative_path == reference["path"]:
            raise ManifestError(f"slice {slice_name} checksum manifest must not reference itself")
        if relative_path in entries:
            raise ManifestError(
                f"slice {slice_name} checksum manifest has duplicate path: {relative_path}"
            )
        entries[relative_path] = checksum

    expected_paths = expected_checksum_paths(slice_name)
    actual_paths = sorted(entries)
    missing = sorted(set(expected_paths) - set(actual_paths))
    extra = sorted(set(actual_paths) - set(expected_paths))
    if missing:
        raise ManifestError(
            f"slice {slice_name} checksum manifest missing paths: {', '.join(missing)}"
        )
    if extra:
        raise ManifestError(
            f"slice {slice_name} checksum manifest unexpected paths: {', '.join(extra)}"
        )
    for relative_path in expected_paths:
        resolved = repository / relative_path
        if not resolved.is_file():
            raise ManifestError(
                f"slice {slice_name} checksum manifest path is missing: {relative_path}"
            )
        actual = file_sha256(resolved)
        if entries[relative_path] != actual:
            raise ManifestError(
                f"slice {slice_name} checksum mismatch for {relative_path}: "
                f"expected {entries[relative_path]}, actual {actual}"
            )


def validate_manifest(
    manifest_path: Path,
    repository: Path,
    release_candidate_sha: str,
) -> Decision:
    candidate = require_sha(release_candidate_sha, "release candidate")
    manifest = read_json(manifest_path, "manifest")
    strict_fields(manifest, MANIFEST_FIELDS, "manifest")

    if manifest["schemaVersion"] != SCHEMA_VERSION:
        raise ManifestError(f"manifest schemaVersion must be {SCHEMA_VERSION}")
    if manifest["release"] != RELEASE:
        raise ManifestError(f"manifest release must be {RELEASE}")
    if manifest["issue"] != ISSUE:
        raise ManifestError(f"manifest issue must be {ISSUE}")
    if manifest["issueState"] not in {"open", "closed"}:
        raise ManifestError("manifest issueState must be open or closed")

    manifest_candidate = require_sha(manifest["releaseCandidateSha"], "manifest releaseCandidateSha")
    manifest_tested_digest = require_sha256(
        manifest["testedCodeTreeSha256"], "manifest testedCodeTreeSha256"
    )
    actual_tested_digest = compute_tested_code_tree_sha256(repository, manifest_candidate)
    if actual_tested_digest != manifest_tested_digest:
        raise ManifestError(
            "tested code tree SHA-256 mismatch for exact candidate: "
            f"expected {manifest_tested_digest}, actual {actual_tested_digest}"
        )
    slices = manifest["slices"]
    if not isinstance(slices, list) or len(slices) != len(SLICE_NAMES):
        raise ManifestError("manifest must contain exactly five stack slices")

    names = [entry.get("name") if isinstance(entry, dict) else None for entry in slices]
    if names != SLICE_NAMES:
        raise ManifestError(f"stack slices must be named in order: {', '.join(SLICE_NAMES)}")

    reasons: list[str] = []
    if manifest["issueState"] != "closed":
        reasons.append(f"issue {ISSUE} is open")
    if manifest_candidate != candidate:
        reasons.append(
            f"release candidate SHA does not match manifest: expected {manifest_candidate}, actual {candidate}"
        )

    for entry in slices:
        if not isinstance(entry, dict):
            raise ManifestError("each stack slice must be a JSON object")
        strict_fields(entry, SLICE_FIELDS, f"slice {entry.get('name', '<unknown>')}")
        name = entry["name"]
        state = entry["prState"]
        if state not in {"missing", "open", "merged"}:
            raise ManifestError(f"slice {name} prState must be missing, open, or merged")

        if entry["prNumber"] is None or state == "missing":
            reasons.append(f"{name} PR is missing")
        elif not isinstance(entry["prNumber"], int) or entry["prNumber"] <= 0:
            raise ManifestError(f"slice {name} prNumber must be a positive integer or null")
        elif state != "merged":
            reasons.append(f"{name} PR is not merged")

        sha_fields = (
            "expectedHeadSha",
            "expectedHeadTreeSha",
            "expectedMergeSha",
            "expectedMergeTreeSha",
        )
        complete_identity = state == "merged" and entry["prNumber"] is not None
        if complete_identity:
            for field in sha_fields:
                require_sha(entry[field], f"slice {name} {field}")
        else:
            for field in sha_fields:
                if entry[field] is not None:
                    require_sha(entry[field], f"slice {name} {field}")
            reasons.append(f"{name} exact PR/head/merge identity is incomplete")

        evidence_refs = entry["evidence"]
        if not isinstance(evidence_refs, list):
            raise ManifestError(f"slice {name} evidence must be an array")

        actual_evidence: list[tuple[Any, Any]] = []
        for reference in evidence_refs:
            if not isinstance(reference, dict):
                raise ManifestError(f"slice {name} evidence entry must be an object")
            strict_fields(reference, EVIDENCE_REF_FIELDS, f"slice {name} evidence reference")
            actual_evidence.append((reference["kind"], reference["path"]))
        if len(actual_evidence) != len(set(actual_evidence)):
            raise ManifestError(f"slice {name} has duplicate evidence kind/path entries")
        if actual_evidence != REQUIRED_EVIDENCE[name]:
            expected_rows = ", ".join(f"{kind}:{path}" for kind, path in REQUIRED_EVIDENCE[name])
            raise ManifestError(f"slice {name} required evidence must be exactly: {expected_rows}")

        checksum_reference = entry["checksumManifest"]
        if not isinstance(checksum_reference, dict):
            raise ManifestError(f"slice {name} checksumManifest must be an object")
        checksum_sha = checksum_reference.get("sha256")
        if checksum_sha is None:
            strict_fields(checksum_reference, CHECKSUM_MANIFEST_FIELDS, f"slice {name} checksum manifest")
            if checksum_reference["path"] != checksum_manifest_path(name):
                raise ManifestError(
                    f"slice {name} checksum manifest path must be {checksum_manifest_path(name)}"
                )
            reasons.append(f"{name} checksum manifest is incomplete")
        else:
            validate_checksum_manifest(repository, name, checksum_reference)

        for reference in evidence_refs:
            kind = reference["kind"]
            expected_checksum = reference["sha256"]
            if expected_checksum is None:
                reasons.append(f"{name} {kind} evidence checksum is missing")
                continue
            require_sha256(expected_checksum, f"slice {name} evidence sha256")
            evidence_path = resolve_evidence_path(repository, reference["path"])
            if not evidence_path.is_file():
                reasons.append(f"{name} {kind} evidence is missing")
                continue
            actual_checksum = file_sha256(evidence_path)
            if actual_checksum != expected_checksum:
                raise ManifestError(
                    f"slice {name} evidence checksum mismatch for {reference['path']}: "
                    f"expected {expected_checksum}, actual {actual_checksum}"
                )

            report = read_json(evidence_path, f"slice {name} evidence")
            if kind == "contract-abi":
                strict_fields(report, ABI_REPORT_FIELDS, "ABI evidence report")
                if (
                    report["schemaVersion"] != SCHEMA_VERSION
                    or report["issue"] != ISSUE
                    or report["release"] != RELEASE
                    or report["slice"] != name
                ):
                    raise ManifestError(f"slice {name} ABI evidence identity mismatch")
                require_sha(report["producerCommit"], f"slice {name} ABI producerCommit")
                require_sha(report["producerTree"], f"slice {name} ABI producerTree")
                report_digest = require_sha256(
                    report["testedCodeTreeSha256"], f"slice {name} ABI testedCodeTreeSha256"
                )
                if report_digest != manifest_tested_digest:
                    raise ManifestError(f"slice {name} ABI tested code tree digest mismatch")
                if report["producerCommit"] != manifest_candidate:
                    reasons.append(f"{name} ABI evidence is stale for release candidate SHA")
                if report["status"] != "GREEN":
                    reasons.append(f"{name} ABI evidence is not GREEN")
                checks = report["checks"]
                if not isinstance(checks, dict) or not checks or any(value != "PASS" for value in checks.values()):
                    reasons.append(f"{name} ABI evidence contains a non-PASS check")
                continue

            strict_fields(report, EVIDENCE_REPORT_FIELDS, "evidence report")
            if (
                report["schemaVersion"] != SCHEMA_VERSION
                or report["slice"] != name
                or report["evidenceKind"] != kind
            ):
                raise ManifestError(f"slice {name} evidence identity mismatch")
            report_digest = require_sha256(
                report["testedCodeTreeSha256"], f"slice {name} evidence testedCodeTreeSha256"
            )
            if report_digest != manifest_tested_digest:
                raise ManifestError(f"slice {name} evidence tested code tree digest mismatch")
            if complete_identity:
                comparisons = (
                    ("prNumber", "prNumber", "PR number mismatch"),
                    ("expectedHeadSha", "headSha", "head SHA mismatch"),
                    ("expectedHeadTreeSha", "headTreeSha", "head tree mismatch"),
                    ("expectedMergeSha", "mergeSha", "merge SHA mismatch"),
                    ("expectedMergeTreeSha", "mergeTreeSha", "merge tree mismatch"),
                )
                for manifest_field, report_field, label in comparisons:
                    if entry[manifest_field] != report[report_field]:
                        raise ManifestError(f"slice {name} evidence {label}")
            if report["releaseCandidateSha"] != manifest_candidate:
                reasons.append(f"{name} evidence is stale for release candidate SHA")
            if report["conclusion"] != "PASS":
                reasons.append(f"{name} evidence conclusion is not PASS")

        if complete_identity:
            if entry["expectedHeadTreeSha"] != entry["expectedMergeTreeSha"]:
                reasons.append(f"{name} head and merge trees differ")

    last_merge = slices[-1]["expectedMergeSha"]
    if last_merge is not None and last_merge != manifest_candidate:
        reasons.append("allocation-proof merge SHA is not the manifest release candidate")

    reasons = list(dict.fromkeys(reasons))
    try:
        reported_manifest = manifest_path.resolve().relative_to(repository.resolve()).as_posix()
    except ValueError:
        reported_manifest = str(manifest_path)
    return Decision(
        decision="HOLD" if reasons else "PASS",
        reasons=reasons,
        release_candidate_sha=candidate,
        manifest_path=reported_manifest,
        tested_code_tree_sha256=manifest_tested_digest,
    )


def extract_jobs(workflow: str) -> dict[str, str]:
    lines = workflow.splitlines(keepends=True)
    jobs: dict[str, list[str]] = {}
    in_jobs = False
    current: str | None = None
    for line in lines:
        if line == "jobs:\n" or line == "jobs:\r\n":
            in_jobs = True
            continue
        if not in_jobs:
            continue
        match = re.match(r"^  ([A-Za-z0-9_.-]+):(?:\s.*)?$", line.rstrip("\r\n"))
        if match:
            current = match.group(1)
            jobs[current] = [line]
        elif current is not None:
            jobs[current].append(line)
    return {name: "".join(block) for name, block in jobs.items()}


def job_needs(block: str) -> set[str]:
    inline = re.search(r"^    needs:\s*\[([^]]*)]", block, re.MULTILINE)
    if inline:
        return {item.strip() for item in inline.group(1).split(",") if item.strip()}
    scalar = re.search(r"^    needs:\s*([A-Za-z0-9_.-]+)\s*$", block, re.MULTILINE)
    if scalar:
        return {scalar.group(1)}
    sequence = re.search(r"^    needs:\s*\n((?:      - [^\n]+\n)+)", block, re.MULTILINE)
    if sequence:
        return {line.split("-", 1)[1].strip() for line in sequence.group(1).splitlines()}
    return set()


def named_step(block: str, name_fragment: str) -> str:
    match = re.search(
        rf"^      - name: [^\n]*{re.escape(name_fragment)}[^\n]*\n(.*?)(?=^      - (?:name:|uses:)|\Z)",
        block,
        re.MULTILINE | re.DOTALL,
    )
    return match.group(0) if match else ""


def contains_tag_mutation(block: str) -> bool:
    if re.search(r"\bgit\s+tag\s+(?!--list\b|-l\b)", block):
        return True
    if re.search(r"\bgit\s+push\b", block):
        return True

    ref_api = r"(?:/|\b)git/refs?(?:/|\b)"
    mutating_method = r"(?:--method|-X)\s+(?:POST|PUT|PATCH|DELETE)\b"
    if re.search(rf"{mutating_method}.*?{ref_api}|{ref_api}.*?{mutating_method}", block, re.DOTALL | re.IGNORECASE):
        return True
    return bool(
        re.search(
            rf"\bgh\s+api\b(?=.*?{ref_api})(?=.*?(?:-f|--field)\s+ref=refs/tags/)",
            block,
            re.DOTALL,
        )
    )


def audit_workflows(repository: Path) -> list[str]:
    workflow_dir = repository / ".github/workflows"
    errors: list[str] = []
    if not workflow_dir.is_dir():
        return ["workflow directory is missing"]

    workflows = sorted(workflow_dir.glob("*.y*ml"))
    for path in workflows:
        text = path.read_text(encoding="utf-8")
        if path.name != "release.yml" and "release-tag-1.12.0" in text:
            errors.append(f"{path.name}: release-tag-1.12.0 outside release.yml")

        jobs = extract_jobs(text)
        if path.name in {"publish-snapshot.yml", "release.yml"}:
            hold = jobs.get(HOLD_JOB, "")
            if f"name: {HOLD_NAME}" not in hold:
                errors.append(f"{path.name}: missing exact job name {HOLD_NAME}")

        for job_name, block in jobs.items():
            needs = job_needs(block)
            publication = "nmcpPublish" in block or "gh release create" in block
            tag_mutation = contains_tag_mutation(block)
            protected = "environment: snapshot-publish-1.12.0" in block or "environment: release-tag-1.12.0" in block

            if publication and HOLD_JOB not in needs:
                errors.append(f"{path.name}: {job_name} does not need {HOLD_NAME}")
            if publication and IMMUTABILITY_JOB not in needs:
                errors.append(f"{path.name}: {job_name} does not need {IMMUTABILITY_JOB}")
            if (publication or protected or tag_mutation) and "candidate-sha-guard" not in needs:
                errors.append(f"{path.name}: {job_name} does not need candidate-sha-guard")
            if protected and HOLD_JOB not in needs and job_name != HOLD_JOB:
                errors.append(f"{path.name}: protected job {job_name} is unheld")
            if (
                tag_mutation
                and "GH_TOKEN: ${{ github.token }}" in block
                and "GH_TOKEN: ${{ steps.tag-token.outputs.token }}" not in block
            ):
                errors.append(f"{path.name}: {job_name} uses generic-token tag creation")

    release_path = workflow_dir / "release.yml"
    if release_path.is_file():
        release = release_path.read_text(encoding="utf-8")
        if re.search(r"^  push:\s*$", release, re.MULTILINE):
            errors.append("release.yml: push trigger is forbidden")
        jobs = extract_jobs(release)
        guard = jobs.get("candidate-sha-guard", "")
        if "environment:" in guard or "secrets." in guard:
            errors.append("release.yml: candidate-sha-guard may not use environment or secrets")
        if guard and not (
            "github.sha" in guard
            and "inputs.candidate_sha" in guard
            and ("!=" in guard or " = " in guard or "==" in guard)
        ):
            errors.append("release.yml: candidate-sha-guard does not reject moved refs")

        candidate_validation = jobs.get("candidate-validation", "")
        candidate_requirements = {
            "issue-754-release-candidate-${{ inputs.candidate_validation_request_id }}": "exact candidate artifact name",
            "validate-issue-754-release-candidate.yml": "candidate workflow identity",
            "issue-754-candidate-${{ inputs.candidate_validation_request_id }}": "candidate display identity",
            "issue-754-release-candidate": "candidate check identity",
            "/jobs": "candidate job readback",
            "verify-candidate-artifact": "fail-closed candidate artifact validation",
        }
        for token, label in candidate_requirements.items():
            if candidate_validation and token not in candidate_validation:
                errors.append(f"release.yml: candidate-validation is missing {label}")

        prepare_immutability = jobs.get("issue-754-tag-immutability-prepare", "")
        if prepare_immutability:
            prepare_needs = job_needs(prepare_immutability)
            required_needs = {"candidate-sha-guard", "candidate-validation", HOLD_JOB}
            if not required_needs.issubset(prepare_needs):
                errors.append("release.yml: prepare immutability job has incomplete dependencies")
            if "environment: release-tag-1.12.0" not in prepare_immutability:
                errors.append("release.yml: prepare immutability job must use release-tag-1.12.0")
            permissions = re.search(
                r"^    permissions:\s*\n((?:      [^\n]+\n)+)", prepare_immutability, re.MULTILINE
            )
            expected_permissions = {"actions: read", "contents: write"}
            actual_permissions = {
                line.strip() for line in permissions.group(1).splitlines()
            } if permissions else set()
            if actual_permissions != expected_permissions:
                errors.append(
                    "release.yml: prepare immutability permissions must be exactly actions: read and contents: write"
                )
            if "immutable-closeout" not in prepare_immutability:
                errors.append("release.yml: prepare immutability job must own immutable-closeout")
            if not all(
                token in prepare_immutability
                for token in (
                    "RELEASE_TAG_APP_ID",
                    "RELEASE_TAG_APP_PRIVATE_KEY",
                    "RELEASE_SETTINGS_APP_ID",
                    "RELEASE_SETTINGS_APP_PRIVATE_KEY",
                )
            ):
                errors.append("release.yml: prepare immutability job must bind distinct tag/settings Apps")
            if "issue-754-tag-immutability-${{ inputs.request_id }}" not in prepare_immutability:
                errors.append("release.yml: prepare immutability job has wrong retained artifact name")
        elif "candidate-validation" in jobs or "prepare-tag" in jobs:
            errors.append("release.yml: prepare immutability job must own immutable-closeout")

        immutability = jobs.get(IMMUTABILITY_JOB, "")
        if immutability:
            permissions = re.search(
                r"^    permissions:\s*\n((?:      [^\n]+\n)+)", immutability, re.MULTILINE
            )
            expected = {"actions: read", "contents: read"}
            actual = {
                line.strip() for line in permissions.group(1).splitlines()
            } if permissions else set()
            if actual != expected:
                errors.append(
                    "release.yml: publish-phase immutability permissions must be exactly actions: read and contents: read"
                )
            download_step = named_step(immutability, "Download only")
            if "GH_TOKEN: ${{ github.token }}" not in download_step:
                errors.append("release.yml: immutability cross-run download must use github.token")
            disallowed = re.findall(r"(?:GH_TOKEN|GITHUB_TOKEN):\s*([^\n]+)", download_step)
            if any(value.strip() != "${{ github.token }}" for value in disallowed):
                errors.append("release.yml: immutability cross-run download has excess authentication")
            if "candidate-validation" in jobs:
                for token, label in (
                    ("issue-754-release-candidate-${{ inputs.candidate_validation_request_id }}", "candidate artifact"),
                    ("issue-754-tag-immutability-${{ inputs.request_id }}", "prepare artifact"),
                    ("verify-immutable-closeout", "verify-only closeout"),
                ):
                    if token not in immutability:
                        errors.append(f"release.yml: publish immutability is missing {label}")
            if "environment:" in immutability or "create-github-app-token" in immutability or "secrets." in immutability:
                errors.append("release.yml: publish immutability must remain read-only and unprotected")

    return errors


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate the issue #754 release hold")
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--repository", type=Path, required=True)
    parser.add_argument("--release-candidate")
    parser.add_argument("--report", type=Path)
    parser.add_argument("--audit-workflows", action="store_true")
    parser.add_argument("--print-tested-code-tree-sha256")
    args = parser.parse_args(argv)
    if args.print_tested_code_tree_sha256:
        if args.audit_workflows or args.manifest or args.release_candidate or args.report:
            parser.error("--print-tested-code-tree-sha256 cannot be combined with validation")
    elif args.audit_workflows:
        if args.manifest or args.release_candidate:
            parser.error("--audit-workflows cannot be combined with manifest validation")
    elif not args.manifest or not args.release_candidate:
        parser.error("--manifest and --release-candidate are required")
    return args


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repository = args.repository.resolve()
    if args.print_tested_code_tree_sha256:
        try:
            print(compute_tested_code_tree_sha256(repository, args.print_tested_code_tree_sha256))
        except ManifestError as error:
            print(f"INVALID: {error}", file=sys.stderr)
            return 2
        return 0
    if args.audit_workflows:
        errors = audit_workflows(repository)
        if errors:
            for error in errors:
                print(f"WORKFLOW AUDIT FAIL: {error}", file=sys.stderr)
            return 2
        print("WORKFLOW AUDIT PASS")
        return 0

    try:
        decision = validate_manifest(args.manifest.resolve(), repository, args.release_candidate)
    except ManifestError as error:
        print(f"INVALID: {error}", file=sys.stderr)
        return 2

    payload = decision.payload()
    if args.report:
        write_json(args.report, payload)
    print(json.dumps(payload, sort_keys=True))
    return 0 if decision.decision == "PASS" else 3


if __name__ == "__main__":
    sys.exit(main())
