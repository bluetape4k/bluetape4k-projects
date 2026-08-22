#!/usr/bin/env python3
"""Shared manifest validation and selection for the Testcontainers image gate."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "scripts/testcontainers_image_gate_manifest.json"
EXPECTED_FAMILY_COUNT = 52
REQUIRED_FIELDS = {
    "id",
    "server",
    "source",
    "testSource",
    "image",
    "tag",
    "testPattern",
    "readiness",
    "workload",
    "diagnostics",
    "releaseRequired",
}
KNOWN_TEST_TASKS = {":bluetape4k-testcontainers:k8sTest"}


def _contract_module() -> Any:
    """Load the existing source/README contract without depending on cwd."""

    root_string = str(ROOT)
    if root_string not in sys.path:
        sys.path.insert(0, root_string)
    from scripts.test_testcontainers_contract import kotlin_servers, readme_tag_table

    return kotlin_servers, readme_tag_table


def load_manifest(path: Path = MANIFEST) -> list[dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict) or payload.get("version") != 1:
        raise ValueError(f"unsupported manifest format: {path}")
    entries = payload.get("families")
    if not isinstance(entries, list):
        raise ValueError(f"manifest families must be a list: {path}")
    return [dict(entry) for entry in entries]


def _test_pattern_for(server: str, source: Path) -> str:
    package_path = source.parent.relative_to(ROOT / "testing/testcontainers/src/main/kotlin")
    package = ".".join(package_path.parts)
    return f"{package}.{server}Test"


def _test_source_for(server: str, source: Path) -> Path:
    test_root = ROOT / "testing/testcontainers/src/test/kotlin"
    package_path = source.parent.relative_to(ROOT / "testing/testcontainers/src/main/kotlin")
    return test_root / package_path / f"{server}Test.kt"


def validate_manifest(entries: list[dict[str, Any]], root: Path = ROOT) -> list[str]:
    """Return all source, test, documentation, and schema drift findings."""

    kotlin_servers, readme_tag_table = _contract_module()
    source_servers = kotlin_servers()
    errors: list[str] = []
    if len(entries) != EXPECTED_FAMILY_COUNT:
        errors.append(f"family count {len(entries)} != {EXPECTED_FAMILY_COUNT}")

    names: set[str] = set()
    ids: set[str] = set()
    expected_readme = readme_tag_table(root / "testing/testcontainers/README.md")
    expected_servers = set(source_servers)

    for index, entry in enumerate(entries):
        prefix = f"families[{index}]"
        missing = REQUIRED_FIELDS - set(entry)
        if missing:
            errors.append(f"{prefix} missing fields: {', '.join(sorted(missing))}")
            continue

        server = entry["server"]
        family_id = entry["id"]
        if not isinstance(server, str) or not server:
            errors.append(f"{prefix} server must be a non-empty string")
            continue
        if server in names:
            errors.append(f"duplicate server: {server}")
        names.add(server)
        if not isinstance(family_id, str) or not family_id:
            errors.append(f"{prefix} id must be a non-empty string")
        elif family_id in ids:
            errors.append(f"duplicate id: {family_id}")
        else:
            ids.add(family_id)

        source = root / str(entry["source"])
        details = source_servers.get(server)
        if details is None:
            errors.append(f"unknown server: {server}")
            continue
        if not source.is_file():
            errors.append(f"missing source: {entry['source']}")
        expected_source = Path(details["path"])
        if source.resolve() != expected_source.resolve():
            errors.append(f"source drift for {server}: {entry['source']} != {expected_source}")
        if entry["image"] != details["image"]:
            errors.append(f"image drift for {server}: {entry['image']} != {details['image']}")
        if entry["tag"] != details["tag"]:
            errors.append(f"tag drift for {server}: {entry['tag']} != {details['tag']}")
        if server not in expected_readme:
            errors.append(f"README row missing for {server}")
        elif tuple(expected_readme[server]) != (entry["image"], entry["tag"]):
            errors.append(f"README drift for {server}")

        test_source = root / str(entry["testSource"])
        if not test_source.is_file():
            errors.append(f"missing test source for {server}: {entry['testSource']}")
        expected_pattern = _test_pattern_for(server, source)
        if entry["testPattern"] != expected_pattern:
            errors.append(f"test pattern drift for {server}: {entry['testPattern']} != {expected_pattern}")
        expected_test_source = _test_source_for(server, source)
        if test_source.resolve() != expected_test_source.resolve():
            errors.append(f"test source drift for {server}: {entry['testSource']} != {expected_test_source}")
        if not isinstance(entry["readiness"], str) or not entry["readiness"].strip():
            errors.append(f"readiness is empty for {server}")
        if not isinstance(entry["workload"], str) or not entry["workload"].strip():
            errors.append(f"workload is empty for {server}")
        if not isinstance(entry["diagnostics"], list) or not entry["diagnostics"]:
            errors.append(f"diagnostics is empty for {server}")
        if not isinstance(entry["releaseRequired"], bool):
            errors.append(f"releaseRequired must be boolean for {server}")
        test_task = entry.get("testTask")
        if test_task is not None and (
            not isinstance(test_task, str) or not test_task.startswith(":")
        ):
            errors.append(f"testTask must be a Gradle task path for {server}")
        elif test_task is not None and test_task not in KNOWN_TEST_TASKS:
            errors.append(f"unknown testTask for {server}: {test_task}")

    missing_servers = expected_servers - names
    extra_servers = names - expected_servers
    errors.extend(f"manifest missing server: {name}" for name in sorted(missing_servers))
    errors.extend(f"manifest has unknown server: {name}" for name in sorted(extra_servers))
    return errors


def select_entries(
    entries: list[dict[str, Any]], changed_paths: set[str], scope: str = "changed"
) -> list[dict[str, Any]]:
    """Select changed families deterministically, or all families for a full gate."""

    if scope not in {"changed", "full"}:
        raise ValueError(f"unsupported scope: {scope}")
    if scope == "full":
        return list(entries)
    normalized = {path.replace("\\", "/") for path in changed_paths}
    shared_prefixes = (
        "scripts/testcontainers_image_gate",
        "scripts/run_testcontainers_image_gate.py",
        ".github/workflows/ci.yml",
        ".github/workflows/nightly-tests.yml",
        ".github/workflows/release.yml",
    )
    if any(path.startswith(prefix) for path in normalized for prefix in shared_prefixes):
        return list(entries)
    return [entry for entry in entries if str(entry["source"]) in normalized]
