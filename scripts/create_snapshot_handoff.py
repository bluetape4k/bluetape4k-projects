#!/usr/bin/env python3

from __future__ import annotations

import argparse
import errno
import hashlib
import json
import re
import socket
import ssl
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


SCHEMA = "bluetape.snapshot-handoff/v1"
LAST_GOOD_SCHEMA = "bluetape.last-good-manifest/v1"
DEFAULT_BASE_URL = "https://central.sonatype.com/repository/maven-snapshots"
SAFE_TOKEN = re.compile(r"^[A-Za-z0-9_.-]+$")
TRANSIENT_NETWORK_ERRNOS = {
    errno.ECONNABORTED,
    errno.ECONNREFUSED,
    errno.ECONNRESET,
    errno.EHOSTUNREACH,
    errno.ENETDOWN,
    errno.ENETRESET,
    errno.ENETUNREACH,
    errno.ETIMEDOUT,
}


class SnapshotHandoffError(RuntimeError):
    pass


class TransientSnapshotHandoffError(SnapshotHandoffError):
    pass


def is_transient_url_error(error: urllib.error.URLError) -> bool:
    reason = error.reason
    if isinstance(reason, ssl.SSLError):
        return False
    if isinstance(reason, (TimeoutError, socket.timeout, ConnectionError)):
        return True
    if isinstance(reason, socket.gaierror):
        return reason.errno == socket.EAI_AGAIN
    return isinstance(reason, OSError) and reason.errno in TRANSIENT_NETWORK_ERRNOS


def fetch(url: str, timeout: float = 20.0) -> bytes:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            if response.status != 200:
                raise SnapshotHandoffError(f"HTTP {response.status} for {url}")
            return response.read()
    except urllib.error.HTTPError as error:
        error_type = (
            TransientSnapshotHandoffError
            if error.code in {404, 408, 429, 500, 502, 503, 504}
            else SnapshotHandoffError
        )
        raise error_type(f"Failed to fetch {url}: {error}") from error
    except urllib.error.URLError as error:
        error_type = (
            TransientSnapshotHandoffError
            if is_transient_url_error(error)
            else SnapshotHandoffError
        )
        raise error_type(f"Failed to fetch {url}: {error}") from error
    except TimeoutError as error:
        raise TransientSnapshotHandoffError(f"Failed to fetch {url}: {error}") from error


def metadata_url(base_url: str, group: str, artifact: str, base_version: str) -> str:
    group_path = group.replace(".", "/")
    return (
        f"{base_url.rstrip('/')}/{group_path}/{artifact}/"
        f"{base_version}-SNAPSHOT/maven-metadata.xml"
    )


def parse_metadata(
    document: bytes,
    expected_group: str,
    expected_artifact: str,
    base_version: str,
) -> dict:
    try:
        root = ET.fromstring(document)
    except ET.ParseError as error:
        raise SnapshotHandoffError("Invalid Maven metadata XML") from error

    actual = (
        root.findtext("groupId"),
        root.findtext("artifactId"),
        root.findtext("version"),
    )
    expected = (expected_group, expected_artifact, f"{base_version}-SNAPSHOT")
    if actual != expected:
        raise SnapshotHandoffError(
            f"Maven metadata coordinate mismatch: expected={expected}, actual={actual}"
        )

    timestamp = root.findtext("versioning/snapshot/timestamp")
    build_number = root.findtext("versioning/snapshot/buildNumber")
    last_updated = root.findtext("versioning/lastUpdated")
    if not timestamp or not build_number or not last_updated:
        raise SnapshotHandoffError("Maven metadata is missing snapshot identity fields")
    if (
        not re.fullmatch(r"[0-9]{8}\.[0-9]{6}", timestamp)
        or not re.fullmatch(r"[1-9][0-9]*", build_number)
        or not re.fullmatch(r"[0-9]{14}", last_updated)
    ):
        raise SnapshotHandoffError("Maven metadata has invalid snapshot identity fields")

    versions = {}
    expected_snapshot_version = f"{base_version}-{timestamp}-{build_number}"
    for item in root.findall("versioning/snapshotVersions/snapshotVersion"):
        extension = item.findtext("extension")
        classifier = item.findtext("classifier") or ""
        value = item.findtext("value")
        if extension and value:
            if value != expected_snapshot_version:
                raise SnapshotHandoffError(
                    "Maven metadata snapshot version mismatch: "
                    f"expected={expected_snapshot_version}, actual={value}"
                )
            versions[(extension, classifier)] = value

    return {
        "group": expected_group,
        "artifact": expected_artifact,
        "base_version": base_version,
        "timestamp": timestamp,
        "build_number": build_number,
        "last_updated": last_updated,
        "versions": versions,
    }


def read_metadata_set(
    base_url: str,
    group: str,
    base_version: str,
    artifacts: list[str],
) -> dict[str, dict]:
    return {
        artifact: parse_metadata(
            fetch(metadata_url(base_url, group, artifact, base_version)),
            group,
            artifact,
            base_version,
        )
        for artifact in artifacts
    }


def validate_snapshot_identity(metadata_set: dict[str, dict], receipt_artifact: str) -> dict:
    canonical = metadata_set.get(receipt_artifact)
    if canonical is None:
        raise SnapshotHandoffError("Receipt artifact metadata was not requested")
    identity = (
        canonical["timestamp"],
        canonical["build_number"],
        canonical["last_updated"],
    )
    for artifact, item in metadata_set.items():
        candidate = (item["timestamp"], item["build_number"], item["last_updated"])
        if candidate != identity:
            raise SnapshotHandoffError(
                f"Snapshot metadata identity mismatch for {artifact}: {candidate} != {identity}"
            )
    return canonical


def resource_url(
    base_url: str,
    group: str,
    artifact: str,
    base_version: str,
    timestamped_version: str,
    extension: str,
) -> str:
    group_path = group.replace(".", "/")
    return (
        f"{base_url.rstrip('/')}/{group_path}/{artifact}/{base_version}-SNAPSHOT/"
        f"{artifact}-{timestamped_version}.{extension}"
    )


def fetch_resources(
    base_url: str,
    group: str,
    base_version: str,
    resource_specs: list[tuple[str, str]],
    metadata_set: dict[str, dict],
) -> list[dict]:
    resources = []
    for artifact, extension in resource_specs:
        timestamped_version = metadata_set[artifact]["versions"].get((extension, ""))
        if not timestamped_version:
            raise SnapshotHandoffError(
                f"Maven metadata has no {extension} snapshot version for {artifact}"
            )
        url = resource_url(
            base_url,
            group,
            artifact,
            base_version,
            timestamped_version,
            extension,
        )
        content = fetch(url)
        resources.append({"url": url, "sha256": hashlib.sha256(content).hexdigest()})
    return resources


def validate_tokens(group: str, artifact: str, resource_specs: list[tuple[str, str]]) -> None:
    if not group or any(not SAFE_TOKEN.fullmatch(part) for part in group.split(".")):
        raise SnapshotHandoffError("Invalid Maven group")
    tokens = [artifact]
    tokens.extend(value for spec in resource_specs for value in spec)
    if any(not SAFE_TOKEN.fullmatch(token) for token in tokens):
        raise SnapshotHandoffError("Invalid Maven artifact or extension")


def write_json(output: Path, value: dict) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def create_verified_receipt(
    *,
    repository: str,
    merge_sha: str,
    verified_ci_run_id: str,
    publication_run_id: str,
    handoff_issue_number: str,
    base_url: str,
    group: str,
    artifact: str,
    base_version: str,
    resource_specs: list[tuple[str, str]],
    output: Path,
) -> dict:
    validate_tokens(group, artifact, resource_specs)
    if not re.fullmatch(r"[0-9a-f]{40}", merge_sha):
        raise SnapshotHandoffError("merge_sha must be a full lowercase Git SHA")
    if not verified_ci_run_id.isdigit() or not publication_run_id.isdigit():
        raise SnapshotHandoffError("workflow run IDs must be numeric")
    if not handoff_issue_number.isdigit() or int(handoff_issue_number) < 1:
        raise SnapshotHandoffError("handoff issue number must be positive")
    if not resource_specs:
        raise SnapshotHandoffError("At least one resource is required")

    artifacts = list(dict.fromkeys(item[0] for item in resource_specs))
    first_metadata = read_metadata_set(base_url, group, base_version, artifacts)
    canonical = validate_snapshot_identity(first_metadata, artifact)
    first_resources = fetch_resources(
        base_url, group, base_version, resource_specs, first_metadata
    )

    second_metadata = read_metadata_set(base_url, group, base_version, artifacts)
    if second_metadata != first_metadata:
        raise SnapshotHandoffError("Maven metadata changed during public read-back")
    validate_snapshot_identity(second_metadata, artifact)
    second_resources = fetch_resources(
        base_url, group, base_version, resource_specs, second_metadata
    )
    if second_resources != first_resources:
        raise SnapshotHandoffError("Public resource checksum changed during read-back")

    receipt = {
        "schema": SCHEMA,
        "repository": repository,
        "merge_sha": merge_sha,
        "verified_ci_run_id": verified_ci_run_id,
        "publication_run_id": publication_run_id,
        "handoff_issue_number": handoff_issue_number,
        "group": group,
        "artifact": artifact,
        "base_version": base_version,
        "timestamp": canonical["timestamp"],
        "build_number": canonical["build_number"],
        "last_updated": canonical["last_updated"],
        "resources": first_resources,
        "catalog_commit_sha": None,
        "created_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "status": "verified",
        "supersedes": None,
    }
    write_json(output, receipt)
    return receipt


def create_rejected_receipt(source: Path, output: Path) -> dict:
    if source.resolve() == output.resolve():
        raise SnapshotHandoffError("Rejected receipt must be append-only")
    original = source.read_bytes()
    try:
        receipt = json.loads(original)
    except json.JSONDecodeError as error:
        raise SnapshotHandoffError("Invalid source receipt JSON") from error
    if receipt.get("schema") != SCHEMA or receipt.get("status") != "verified":
        raise SnapshotHandoffError("Only a verified handoff receipt can be rejected")
    receipt["status"] = "rejected"
    receipt["supersedes"] = hashlib.sha256(original).hexdigest()
    receipt["created_at"] = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    write_json(output, receipt)
    return receipt


def create_last_good_manifest(
    *,
    receipt_path: Path,
    catalog_commit_sha: str | None,
    validation_command: str,
    validation_run_id: str,
    output: Path,
) -> dict:
    if receipt_path.resolve() == output.resolve():
        raise SnapshotHandoffError("Last-good manifest must not replace its receipt")
    if catalog_commit_sha is not None and not re.fullmatch(
        r"[0-9a-f]{40}", catalog_commit_sha
    ):
        raise SnapshotHandoffError("catalog_commit_sha must be a full lowercase Git SHA")
    if not validation_command.strip() or not validation_run_id.strip():
        raise SnapshotHandoffError("Validation command and run ID are required")

    original = receipt_path.read_bytes()
    try:
        receipt = json.loads(original)
    except json.JSONDecodeError as error:
        raise SnapshotHandoffError("Invalid handoff receipt JSON") from error

    required = (
        "merge_sha",
        "handoff_issue_number",
        "group",
        "artifact",
        "base_version",
        "timestamp",
        "build_number",
        "last_updated",
        "resources",
    )
    if receipt.get("schema") != SCHEMA or receipt.get("status") != "verified":
        raise SnapshotHandoffError("Last-good manifest requires a verified handoff receipt")
    if any(not receipt.get(field) for field in required):
        raise SnapshotHandoffError("Verified handoff receipt is incomplete")
    if not re.fullmatch(r"[0-9a-f]{40}", receipt["merge_sha"]):
        raise SnapshotHandoffError("Verified handoff receipt has an invalid merge SHA")

    manifest = {
        "schema": LAST_GOOD_SCHEMA,
        "base_sha": receipt["merge_sha"],
        "dependency": {
            "coordinate": (
                f"{receipt['group']}:{receipt['artifact']}:"
                f"{receipt['base_version']}-SNAPSHOT"
            ),
            "timestamp": receipt["timestamp"],
            "build_number": receipt["build_number"],
            "last_updated": receipt["last_updated"],
        },
        "resources": receipt["resources"],
        "catalog_commit_sha": catalog_commit_sha,
        "validation": {
            "command": validation_command,
            "run_id": validation_run_id,
        },
        "receipt_sha256": hashlib.sha256(original).hexdigest(),
        "created_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }
    write_json(output, manifest)
    return manifest


def parse_resource(value: str) -> tuple[str, str]:
    parts = value.split(":")
    if len(parts) != 2 or not all(parts):
        raise argparse.ArgumentTypeError("resource must be ARTIFACT:EXTENSION")
    return parts[0], parts[1]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Create an immutable Maven SNAPSHOT handoff receipt."
    )
    parser.add_argument("--repository", required=True)
    parser.add_argument("--merge-sha", required=True)
    parser.add_argument("--verified-ci-run-id", required=True)
    parser.add_argument("--publication-run-id", required=True)
    parser.add_argument("--handoff-issue-number", required=True)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--group", required=True)
    parser.add_argument("--artifact", required=True)
    parser.add_argument("--base-version", required=True)
    parser.add_argument("--resource", action="append", type=parse_resource, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    try:
        receipt = create_verified_receipt(
            repository=args.repository,
            merge_sha=args.merge_sha,
            verified_ci_run_id=args.verified_ci_run_id,
            publication_run_id=args.publication_run_id,
            handoff_issue_number=args.handoff_issue_number,
            base_url=args.base_url,
            group=args.group,
            artifact=args.artifact,
            base_version=args.base_version,
            resource_specs=args.resource,
            output=args.output,
        )
    except TransientSnapshotHandoffError as error:
        print(f"error: {error}", file=sys.stderr)
        return 75
    except SnapshotHandoffError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    print(f"timestamp={receipt['timestamp']}")
    print(f"build_number={receipt['build_number']}")
    print(f"last_updated={receipt['last_updated']}")
    print(f"receipt_sha256={hashlib.sha256(args.output.read_bytes()).hexdigest()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
