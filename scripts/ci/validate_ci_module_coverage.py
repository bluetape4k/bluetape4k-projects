#!/usr/bin/env python3
"""Validate the publishable-module to CI lane contract.

The Gradle settings file discovers two-level modules automatically while
``ProjectPublication.kt`` decides which projects are publishable.  This
validator mirrors those two source-of-truth rules, then checks that every
publishable module is assigned to exactly one regular CI lane or carries an
explicit exemption.
"""

from __future__ import annotations

import argparse
import fnmatch
import json
import re
import sys
from pathlib import Path
from typing import Iterable


EXEMPTIONS = {"nightly-only", "explicit"}


def _bool_argument(arguments: str, name: str, positional_index: int, default: bool) -> bool:
    named = re.search(rf"\b{name}\s*=\s*(true|false)", arguments)
    if named:
        return named.group(1) == "true"
    positional = [item.strip() for item in arguments.split(",") if item.strip()]
    if positional_index < len(positional) and positional[positional_index] in {"true", "false"}:
        return positional[positional_index] == "true"
    return default


def _registration_specs(settings_text: str) -> list[tuple[str, bool, bool]]:
    project_name_match = re.search(r'val projectName\s*=\s*"([^"]+)"', settings_text)
    if project_name_match is None:
        raise ValueError("settings.gradle.kts projectName declaration is missing")
    specs: list[tuple[str, bool, bool]] = []
    for match in re.finditer(r'includeModules\("([^"]+)"([^)]*)\)', settings_text):
        base_dir = match.group(1)
        arguments = match.group(2)
        specs.append(
            (
                base_dir,
                _bool_argument(arguments, "withProjectName", 0, True),
                _bool_argument(arguments, "withBaseDir", 1, True),
            )
        )
    if not specs:
        raise ValueError("settings.gradle.kts has no includeModules declarations")
    return specs


def _publication_exclusions(publication_text: str) -> tuple[set[str], set[str], set[str]]:
    project_exclusions = set(re.findall(r'projectName\s*!=\s*"([^"]+)"', publication_text))
    path_exclusions = set(re.findall(r'normalizedProjectDir\s*!=\s*"([^"]+)"', publication_text))
    path_prefix_exclusions = set(
        re.findall(r'normalizedProjectDir\.startsWith\("([^"]+)"\)', publication_text)
    )
    application_block = re.search(
        r"applicationOnlyProjects\s*=\s*setOf\((.*?)\)",
        publication_text,
        re.DOTALL,
    )
    if application_block:
        project_exclusions.update(re.findall(r'"([^"]+)"', application_block.group(1)))
    return project_exclusions, path_exclusions, path_prefix_exclusions


def project_name(relative_path: str, project_root_name: str, with_project_name: bool, with_base_dir: bool) -> str:
    """Return the project name produced by the parsed ``includeModules`` call."""

    base_dir, leaf = relative_path.rsplit("/", 1)
    base_path = base_dir.replace("/", "-")
    if not with_project_name and not with_base_dir:
        return leaf
    if with_project_name and not with_base_dir:
        return f"{project_root_name}-{leaf}"
    if with_project_name:
        return f"{project_root_name}-{base_path}-{leaf}"
    return f"{base_path}-{leaf}"


def discover_publishable_modules(root: Path) -> dict[str, str]:
    """Discover modules from the actual settings and publication source rules."""

    settings_text = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    publication_text = (root / "buildSrc/src/main/kotlin/ProjectPublication.kt").read_text(encoding="utf-8")
    project_root_name = re.search(r'val projectName\s*=\s*"([^"]+)"', settings_text)
    if project_root_name is None:
        raise ValueError("settings.gradle.kts projectName declaration is missing")
    project_exclusions, path_exclusions, path_prefix_exclusions = _publication_exclusions(publication_text)
    modules: dict[str, str] = {}
    for base_dir, with_project_name, with_base_dir in _registration_specs(settings_text):
        group_dir = root / base_dir
        if not group_dir.is_dir():
            continue
        for module_dir in sorted(group_dir.iterdir()):
            if not module_dir.is_dir() or not (module_dir / "build.gradle.kts").is_file():
                continue
            relative_path = module_dir.relative_to(root).as_posix()
            name = project_name(relative_path, project_root_name.group(1), with_project_name, with_base_dir)
            if name in project_exclusions:
                continue
            if relative_path in path_exclusions or any(
                relative_path.startswith(prefix) for prefix in path_prefix_exclusions
            ):
                continue
            if "-demo" in name or name.endswith("-benchmark"):
                continue
            modules[relative_path] = name
    return modules


def _job_block(workflow: str, job_name: str) -> str | None:
    jobs_start = workflow.find("jobs:\n")
    if jobs_start < 0:
        return None
    jobs = workflow[jobs_start + len("jobs:\n") :]
    match = re.search(rf"^  {re.escape(job_name)}:\n", jobs, re.MULTILINE)
    if match is None:
        return None
    next_job = re.search(r"^  [a-z0-9][a-z0-9-]*:\n", jobs[match.end() :], re.MULTILINE)
    end = match.end() + next_job.start() if next_job else len(jobs)
    return jobs[match.start() : end]


def _path_filters(ci_workflow: str) -> dict[str, list[str]]:
    marker = "          filters: |\n"
    start = ci_workflow.find(marker)
    if start < 0:
        return {}
    filters: dict[str, list[str]] = {}
    current: str | None = None
    for line in ci_workflow[start + len(marker) :].splitlines():
        lane_match = re.fullmatch(r"            ([a-z0-9][a-z0-9-]*):", line)
        path_match = re.fullmatch(r"              - '([^']+)'", line)
        if lane_match:
            current = lane_match.group(1)
            filters[current] = []
        elif path_match and current is not None:
            filters[current].append(path_match.group(1))
        elif line and not line.startswith("              "):
            break
    return filters


def _changes_outputs(changes_block: str) -> tuple[set[str], list[str]]:
    matches = re.findall(
        r"^      ([a-z0-9][a-z0-9-]*): \$\{\{ steps\.filter\.outputs\.([a-z0-9][a-z0-9-]*) \}\}$",
        changes_block,
        re.MULTILINE,
    )
    errors = [
        f"CI changes output {left} references {right}"
        for left, right in matches
        if left != right
    ]
    return {left for left, right in matches if left == right}, errors


def _module_sentinels(path: str) -> tuple[str, ...]:
    return (
        f"{path}/build.gradle.kts",
        f"{path}/src/main/ContractSentinel.kt",
        f"{path}/src/test/ContractSentinel.kt",
    )


def _matches(patterns: Iterable[str], path: str) -> list[str]:
    return [pattern for pattern in patterns if fnmatch.fnmatchcase(path, pattern)]


def _status_guard_present(status_block: str, lane: str) -> bool:
    env_prefix = lane.upper().replace("-", "_")
    changed = f"{env_prefix}_CHANGED"
    result = f"{env_prefix}_RESULT"
    return (
        re.search(rf"^          {re.escape(changed)}: \$\{{\{{ needs\.changes\.outputs(?:\['{re.escape(lane)}'\]|\.{re.escape(lane)}) \}}\}}$", status_block, re.MULTILINE)
        is not None
        and f"{result}: ${{{{ needs.test-{lane}.result }}}}" in status_block
        and f'[[ "${changed}" == "true" && "${result}" == "skipped" ]]' in status_block
    )


def _changes_result_guard_present(status_block: str) -> bool:
    return (
        "CHANGES_RESULT: ${{ needs.changes.result }}" in status_block
        and '[[ "$CHANGES_RESULT" != "success" ]]' in status_block
    )


def _inline_needs(block: str) -> set[str]:
    match = re.search(r"^    needs: \[([^]]+)]$", block, re.MULTILINE)
    if match is None:
        return set()
    return {item.strip() for item in match.group(1).split(",")}


def validate_repository(
    root: Path,
    manifest_path: Path,
    ci_workflow_path: Path,
    nightly_workflow_path: Path,
) -> list[str]:
    """Return contract violations; an empty list means the contract passes."""

    errors: list[str] = []
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return [f"cannot read manifest: {exc}"]
    if not isinstance(manifest, dict) or manifest.get("version") != 1:
        errors.append("manifest version must be 1")
    entries = manifest.get("modules") if isinstance(manifest, dict) else None
    if not isinstance(entries, dict):
        return errors + ["manifest modules must be an object"]

    try:
        discovered = discover_publishable_modules(root)
    except (OSError, ValueError) as exc:
        return errors + [f"cannot discover publishable modules from source rules: {exc}"]
    discovered_paths = set(discovered)
    manifest_paths = set(entries)
    for path in sorted(discovered_paths - manifest_paths):
        errors.append(f"{path} is missing from manifest")
    for path in sorted(manifest_paths - discovered_paths):
        errors.append(f"{path} is not a publishable module")

    try:
        ci_workflow = ci_workflow_path.read_text(encoding="utf-8")
        nightly_workflow = nightly_workflow_path.read_text(encoding="utf-8")
    except OSError as exc:
        return errors + [f"cannot read workflow: {exc}"]

    changes = _job_block(ci_workflow, "changes")
    status = _job_block(ci_workflow, "ci-status")
    if changes is None:
        return errors + ["CI changes job is missing"]
    if status is None:
        errors.append("CI status job is missing")
        status = ""
    filters = _path_filters(ci_workflow)
    outputs, output_errors = _changes_outputs(changes)
    errors.extend(output_errors)
    status_needs = _inline_needs(status)
    if "changes" not in status_needs:
        errors.append("ci-status is missing changes from needs")
    if not _changes_result_guard_present(status):
        errors.append("ci-status changes result guard is missing")
    lane_modules: dict[str, list[str]] = {}
    exemption_modules: dict[str, str] = {}

    for path, entry in sorted(entries.items()):
        if not isinstance(entry, dict):
            errors.append(f"{path}: manifest entry must be an object")
            continue
        has_lane = isinstance(entry.get("lane"), str)
        has_exemption = isinstance(entry.get("exemption"), str)
        if has_lane == has_exemption:
            errors.append(f"{path}: exactly one route (lane or exemption) is required")
            continue
        if has_exemption:
            exemption = entry["exemption"]
            if exemption not in EXEMPTIONS:
                errors.append(f"{path}: unknown exemption {exemption!r}")
            if not str(entry.get("reason", "")).strip():
                errors.append(f"{path}: exemption requires a reason")
            if exemption == "nightly-only" and path in discovered:
                task = f":{discovered[path]}:test"
                if task not in nightly_workflow:
                    errors.append(f"{path}: nightly-only exemption has no nightly test task {task}")
            exemption_modules[path] = exemption
            continue

        lane = entry["lane"]
        lane_modules.setdefault(lane, []).append(path)
        if lane not in outputs:
            errors.append(f"{path}: lane {lane} is missing from CI changes outputs")
        if lane not in filters:
            errors.append(f"{path}: lane {lane} is missing from CI path filters")
        job = _job_block(ci_workflow, f"test-{lane}")
        if job is None:
            errors.append(f"{path}: test-{lane} job is missing")
        elif path in discovered:
            task = f":{discovered[path]}:test"
            kover_task = f":{discovered[path]}:koverXmlReport"
            if task not in job:
                errors.append(f"{path}: {task} is missing from test-{lane}")
            if kover_task not in job:
                errors.append(f"{path}: {kover_task} is missing from test-{lane}")
        if not _status_guard_present(status, lane):
            errors.append(f"{path}: ci-status skip guard is missing for {lane}")
        if f"test-{lane}" not in status_needs:
            errors.append(f"{path}: test-{lane} is missing from ci-status needs")

    for lane, paths in sorted(lane_modules.items()):
        patterns_by_lane = filters.get(lane, [])
        for path in paths:
            if path not in discovered:
                continue
            for sentinel in _module_sentinels(path):
                matching_lanes = [
                    filter_name
                    for filter_name, patterns in filters.items()
                    if _matches(patterns, sentinel)
                ]
                if len(matching_lanes) != 1 or matching_lanes[0] != lane:
                    errors.append(
                        f"{path}: does not cover module-wide source paths: {sentinel} -> {matching_lanes!r}"
                    )
                if not _matches(patterns_by_lane, sentinel):
                    errors.append(
                        f"{path}: lane {lane} path filter does not match module-wide source path {sentinel}"
                    )

    regular_lanes = {
        lane
        for lane in filters
        if lane in outputs and _job_block(ci_workflow, f"test-{lane}") is not None
    }
    for path in sorted(exemption_modules):
        if path not in discovered:
            continue
        for sentinel in _module_sentinels(path):
            matching_regular_lanes = [
                filter_name
                for filter_name in regular_lanes
                if _matches(filters.get(filter_name, []), sentinel)
            ]
            if matching_regular_lanes:
                errors.append(
                    f"{path}: exemption still matches regular CI path lane(s): {matching_regular_lanes!r}"
                )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--manifest", type=Path, default=Path("scripts/ci/ci-module-domain-manifest.json"))
    parser.add_argument("--ci-workflow", type=Path, default=Path(".github/workflows/ci.yml"))
    parser.add_argument("--nightly-workflow", type=Path, default=Path(".github/workflows/nightly-tests.yml"))
    args = parser.parse_args()
    errors = validate_repository(
        args.root,
        args.manifest,
        args.ci_workflow,
        args.nightly_workflow,
    )
    if errors:
        print("CI module coverage contract failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(f"CI module coverage contract passed: {len(discover_publishable_modules(args.root))} publishable modules")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
