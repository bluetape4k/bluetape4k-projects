#!/usr/bin/env python3
"""Shared manifest validation and selection for the Testcontainers image gate."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "scripts/testcontainers_image_gate_manifest.json"
EXPECTED_FAMILY_COUNT = 52
EXPECTED_RELEASE_FAMILY_COUNT = 48
NON_RELEASE_RUNTIME_SERVERS = frozenset(
    {"ChromaDBServer", "OllamaServer", "RedpandaServer", "Ignite3Server"}
)
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
ALLOWED_PLATFORM_IDS = {"amd64", "arm64"}
ALLOWED_RUNNERS = {"ubuntu-24.04", "ubuntu-24.04-arm"}
IMAGE_PATTERN = re.compile(
    r"^(?:[a-z0-9]+(?:[._-][a-z0-9]+)*(?:\.[a-z]{2,})?(?::[0-9]{1,5})?/)?"
    r"[a-z0-9]+(?:[._/-][a-z0-9]+)*$"
)
TAG_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
ID_PATTERN = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}$")
WORKLOAD_PATTERN = re.compile(r"^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+\.[A-Za-z_$][\w$]*(?:\(\))?$")
TEST_METHOD_DECLARATION_PATTERN = re.compile(
    r"(?ms)^\s*@Test(?:\s*\([^)]*\))?\s*(?:\r?\n\s*)+"
    r"fun\s+(`(?P<quoted>[^`]+)`|(?P<identifier>[A-Za-z_][A-Za-z0-9_]*))\s*\("
)
TEST_SELECTOR_WILDCARDS = frozenset("*?[]{}")


class SelectionError(ValueError):
    """Raised when a gate selector is not exact and unambiguous."""


def _contract_module() -> Any:
    """Load the existing source/README contract without depending on cwd."""

    root_string = str(ROOT)
    if root_string not in sys.path:
        sys.path.insert(0, root_string)
    from scripts.test_testcontainers_contract import kotlin_servers, readme_tag_table

    return kotlin_servers, readme_tag_table


def load_manifest(path: Path = MANIFEST) -> list[dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict) or payload.get("version") not in {1, 2}:
        raise ValueError(f"unsupported manifest format: {path}")
    entries = payload.get("families")
    if not isinstance(entries, list) or not all(isinstance(entry, dict) for entry in entries):
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


def _safe_string(value: object, pattern: re.Pattern[str]) -> bool:
    return isinstance(value, str) and bool(pattern.fullmatch(value)) and "\x00" not in value


def _test_method_names(test_source: Path) -> set[str]:
    """Return concrete JUnit @Test method names declared in a Kotlin source file."""

    try:
        text = test_source.read_text(encoding="utf-8")
    except OSError:
        return set()
    return {
        match.group("quoted") or match.group("identifier")
        for match in TEST_METHOD_DECLARATION_PATTERN.finditer(text)
    }


def _validate_test_selector(
    entry: dict[str, Any],
    prefix: str,
    test_source: Path,
    errors: list[str],
) -> None:
    selector = entry.get("testSelector")
    if selector is None:
        return
    pattern = entry.get("testPattern")
    if (
        not isinstance(selector, str)
        or not selector.strip()
        or selector != selector.strip()
        or not selector.isprintable()
        or "\x00" in selector
        or not isinstance(pattern, str)
        or not selector.startswith(f"{pattern}.")
    ):
        errors.append(f"{prefix}.testSelector must target a method under testPattern")
        return

    method_name = selector[len(pattern) + 1 :]
    if not method_name or any(character in method_name for character in TEST_SELECTOR_WILDCARDS):
        errors.append(f"{prefix}.testSelector must name one concrete @Test method")
        return
    if test_source.is_file() and method_name not in _test_method_names(test_source):
        errors.append(f"{prefix}.testSelector must match an @Test method in {entry['testSource']}")


def canonical_architecture(value: object) -> str | None:
    """Normalize Docker/runner architecture aliases to the gate vocabulary."""

    if not isinstance(value, str):
        return None
    normalized = value.strip().lower()
    return {
        "x86_64": "amd64",
        "amd64": "amd64",
        "aarch64": "arm64",
        "arm64": "arm64",
    }.get(normalized)


def platform_for_entry(
    entry: dict[str, Any],
    platform_id: str | None = None,
    *,
    default_platform_id: str = "amd64",
) -> dict[str, Any] | None:
    """Return the selected platform metadata, or ``None`` for generic families."""

    platforms = entry.get("platforms")
    if platforms is None:
        if platform_id is not None:
            raise SelectionError(f"family has no platform metadata: {entry.get('id', '<unknown>')}")
        return None
    if not isinstance(platforms, list):
        raise SelectionError(f"platforms must be a list: {entry.get('id', '<unknown>')}")
    selected_id = platform_id or str(entry.get("defaultPlatformId") or default_platform_id)
    matches = [item for item in platforms if isinstance(item, dict) and item.get("id") == selected_id]
    if len(matches) != 1:
        raise SelectionError(
            f"platform selection must match exactly one entry: {entry.get('id', '<unknown>')} / {selected_id}"
        )
    selected = dict(matches[0])
    selected["id"] = selected_id
    return selected


def _validate_platforms(entry: dict[str, Any], prefix: str, errors: list[str]) -> None:
    strict = entry.get("executionEvidenceRequired", False)
    if not isinstance(strict, bool):
        errors.append(f"{prefix} executionEvidenceRequired must be boolean")
        strict = False
    pull_required = entry.get("pullEvidenceRequired", False)
    if not isinstance(pull_required, bool):
        errors.append(f"{prefix} pullEvidenceRequired must be boolean")
        pull_required = False
    if not strict and "platforms" not in entry:
        return

    platforms = entry.get("platforms")
    if not isinstance(platforms, list) or not platforms:
        errors.append(f"{prefix} platforms must be a non-empty list")
        return
    seen: set[str] = set()
    for platform_index, platform in enumerate(platforms):
        platform_prefix = f"{prefix}.platforms[{platform_index}]"
        if not isinstance(platform, dict):
            errors.append(f"{platform_prefix} must be an object")
            continue
        platform_id = platform.get("id")
        if not _safe_string(platform_id, ID_PATTERN) or platform_id not in ALLOWED_PLATFORM_IDS:
            errors.append(f"{platform_prefix}.id is unsupported: {platform_id!r}")
        elif platform_id in seen:
            errors.append(f"duplicate platform id: {platform_id}")
        else:
            seen.add(platform_id)
        if platform.get("os") != "linux":
            errors.append(f"{platform_prefix}.os must be linux")
        architecture = canonical_architecture(platform.get("architecture"))
        if architecture is None or architecture != platform_id:
            errors.append(f"{platform_prefix}.architecture does not match id")
        if not _safe_string(platform.get("tag"), TAG_PATTERN):
            errors.append(f"{platform_prefix}.tag is invalid")
        if platform.get("runner") not in ALLOWED_RUNNERS:
            errors.append(f"{platform_prefix}.runner is unsupported")
        if platform_id == "amd64" and platform.get("runner") != "ubuntu-24.04":
            errors.append(f"{platform_prefix}.runner must be ubuntu-24.04")
        if platform_id == "arm64" and platform.get("runner") != "ubuntu-24.04-arm":
            errors.append(f"{platform_prefix}.runner must be ubuntu-24.04-arm")

    default_platform_id = entry.get("defaultPlatformId")
    if default_platform_id != "amd64":
        errors.append(f"{prefix}.defaultPlatformId must be amd64")
    if default_platform_id not in seen:
        errors.append(f"{prefix}.defaultPlatformId is not present in platforms")
    if strict:
        if not _safe_string(entry.get("workloadTestPattern"), WORKLOAD_PATTERN):
            errors.append(f"{prefix}.workloadTestPattern is invalid")
        timeouts = entry.get("platformTimeouts")
        if not isinstance(timeouts, dict):
            errors.append(f"{prefix}.platformTimeouts must be an object")
        else:
            for platform_id in seen:
                timeout = timeouts.get(platform_id)
                if not isinstance(timeout, dict):
                    errors.append(f"{prefix}.platformTimeouts.{platform_id} must be an object")
                    continue
                for field in ("testMinutes", "clientConnectSeconds", "clientRequestSeconds"):
                    if not isinstance(timeout.get(field), int) or timeout[field] <= 0:
                        errors.append(f"{prefix}.platformTimeouts.{platform_id}.{field} must be positive")
        if not pull_required:
            errors.append(f"{prefix}.pullEvidenceRequired must be true for strict execution")
    if "image" in entry and not _safe_string(entry.get("image"), IMAGE_PATTERN):
        errors.append(f"{prefix}.image is invalid")
    if "tag" in entry and not _safe_string(entry.get("tag"), TAG_PATTERN):
        errors.append(f"{prefix}.tag is invalid")


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
        if not _safe_string(family_id, ID_PATTERN):
            errors.append(f"{prefix} id is invalid")
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
        _validate_test_selector(entry, prefix, test_source, errors)
        _validate_platforms(entry, prefix, errors)
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
    release_servers = {
        entry["server"] for entry in entries if entry.get("releaseRequired") is True
    }
    if len(release_servers) != EXPECTED_RELEASE_FAMILY_COUNT:
        errors.append(
            f"release family count {len(release_servers)} != {EXPECTED_RELEASE_FAMILY_COUNT}"
        )
    for server in sorted(NON_RELEASE_RUNTIME_SERVERS):
        entry = next((item for item in entries if item.get("server") == server), None)
        if entry is not None and entry.get("releaseRequired") is not False:
            errors.append(f"disabled family must be support inventory only: {server}")
    return errors


def select_entries(
    entries: list[dict[str, Any]],
    changed_paths: set[str] | None = None,
    scope: str = "changed",
    *,
    family_id: str | None = None,
    platform_id: str | None = None,
    require_selection: bool = False,
    default_platform_id: str = "amd64",
) -> list[dict[str, Any]]:
    """Select changed families deterministically, or all families for a full gate."""

    if scope not in {"changed", "full", "family"}:
        raise ValueError(f"unsupported scope: {scope}")
    if scope == "family":
        if not family_id or not _safe_string(family_id, ID_PATTERN):
            raise SelectionError("family scope requires a safe --family-id")
        matches = [entry for entry in entries if entry.get("id") == family_id]
        if len(matches) != 1:
            raise SelectionError(f"family selection must match exactly one entry: {family_id}")
        selected = dict(matches[0])
        selected_platform = platform_for_entry(
            selected, platform_id, default_platform_id=default_platform_id
        )
        if require_selection and selected_platform is None:
            raise SelectionError(f"family has no selectable platform: {family_id}")
        if platform_id is not None and selected_platform is None:
            raise SelectionError(f"family has no platform metadata: {family_id}")
        if selected_platform is not None:
            selected["_selected_platform_id"] = selected_platform["id"]
        return [selected]
    if scope == "full":
        # Full scope keeps manifest entries canonical; the runner resolves a
        # strict family's default platform at execution time.
        return list(entries)
    normalized = {(path or "").replace("\\", "/") for path in (changed_paths or set())}
    shared_prefixes = (
        "scripts/testcontainers_image_gate",
        "scripts/run_testcontainers_image_gate.py",
        ".github/workflows/ci.yml",
        ".github/workflows/nightly-tests.yml",
        ".github/workflows/release.yml",
    )
    if any(path.startswith(prefix) for path in normalized for prefix in shared_prefixes):
        return list(entries)
    selected = [dict(entry) for entry in entries if str(entry["source"]) in normalized]
    for entry in selected:
        selected_platform = platform_for_entry(
            entry, platform_id, default_platform_id=default_platform_id
        )
        if selected_platform is not None:
            entry["_selected_platform_id"] = selected_platform["id"]
    return selected
