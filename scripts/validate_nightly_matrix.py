from __future__ import annotations

import json
import sys
from collections import Counter
from collections.abc import Iterable
from pathlib import Path
from typing import Any

REQUIRED_JOB_NAMES = frozenset(
    {
        "Plan Nightly Scope",
        "Build",
        "Detekt",
        "Test / Core",
        "Test / IO",
        "Test / IO HTTP",
        "Test / Ktor",
        "Test / Utils",
        "Test / Misc",
        "Test / Testcontainers image startup-workload gate",
        "Test / Testcontainers Ignite2 arm64 image gate",
        "Test / Testcontainers Spring bridge",
        "Coverage Report",
        "Nightly Status",
    }
)


def expected_matrix_names(contract: dict[str, Any]) -> tuple[set[str], tuple[str, ...]]:
    if contract.get("schema_version") != 1:
        raise ValueError("unsupported schema version")
    if contract.get("workflow") != ".github/workflows/nightly-tests.yml":
        raise ValueError("unexpected workflow")

    matrix_jobs = contract.get("matrix_jobs")
    if not isinstance(matrix_jobs, dict) or not matrix_jobs:
        raise ValueError("matrix_jobs must be a non-empty object")

    expected_names: set[str] = set()
    prefixes: list[str] = []
    for prefix, groups in matrix_jobs.items():
        if (
            not isinstance(prefix, str)
            or not prefix
            or not isinstance(groups, list)
            or not groups
        ):
            raise ValueError("matrix prefix and groups must be non-empty")
        prefixes.append(f"{prefix} (")
        for group in groups:
            if not isinstance(group, str) or not group:
                raise ValueError("matrix group must be a non-empty string")
            name = f"{prefix} ({group})"
            if name in expected_names:
                raise ValueError(f"duplicate matrix job: {name}")
            expected_names.add(name)
    return expected_names, tuple(prefixes)


def matrix_contract_errors(
    jobs: Iterable[dict[str, Any]], contract: dict[str, Any]
) -> list[str]:
    jobs = list(jobs)
    try:
        expected_names, prefixes = expected_matrix_names(contract)
    except ValueError as error:
        return [f"invalid Nightly matrix contract: {error}"]

    names = [job.get("name") for job in jobs if isinstance(job, dict)]
    observed_names = {
        name for name in names if isinstance(name, str) and name.startswith(prefixes)
    }
    errors = [
        f"missing matrix job: {name}"
        for name in sorted(expected_names - observed_names)
    ]
    errors.extend(
        f"unexpected matrix job: {name}"
        for name in sorted(observed_names - expected_names)
    )

    name_counts = Counter(
        name for name in names if isinstance(name, str) and name.startswith(prefixes)
    )
    errors.extend(
        f"matrix job appears {count} times: {name}"
        for name, count in sorted(name_counts.items())
        if count != 1
    )
    for name in sorted(expected_names):
        matching_jobs = [job for job in jobs if job.get("name") == name]
        if len(matching_jobs) == 1 and matching_jobs[0].get("conclusion") != "success":
            errors.append(f"non-success matrix job: {name}")
    return errors


def validation_errors(
    run: dict[str, Any],
    jobs: Iterable[dict[str, Any]],
    expected_head_sha: str,
    contract: dict[str, Any],
) -> tuple[str, list[str]]:
    jobs = list(jobs)
    names = {job.get("name") for job in jobs if isinstance(job, dict)}
    errors: list[str] = []
    if run.get("status") != "completed":
        errors.append("run is not completed")
    if run.get("conclusion") != "success":
        errors.append("run did not succeed")
    if run.get("path") != ".github/workflows/nightly-tests.yml":
        errors.append("run is not Nightly")
    if run.get("head_branch") != "develop":
        errors.append("run is not on develop")
    head_sha = run.get("head_sha", "")
    if not isinstance(head_sha, str) or not head_sha:
        errors.append("run has no head SHA")
    if expected_head_sha and head_sha != expected_head_sha:
        errors.append("head SHA mismatch")
    errors.extend(f"missing job: {name}" for name in sorted(REQUIRED_JOB_NAMES - names))
    errors.extend(matrix_contract_errors(jobs, contract))
    errors.extend(
        f"non-success job: {job.get('name')}"
        for job in jobs
        if job.get("conclusion") != "success"
    )
    return head_sha, errors


def _load_json(path: str) -> Any:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def main(argv: list[str]) -> int:
    if len(argv) != 4:
        print(
            "usage: validate_nightly_matrix.py RUN_JSON JOBS_JSON EXPECTED_HEAD_SHA CONTRACT_JSON",
            file=sys.stderr,
        )
        return 2

    run = _load_json(argv[0])
    pages = _load_json(argv[1])
    contract = _load_json(argv[3])
    if isinstance(pages, dict):
        pages = [pages]
    jobs = [job for page in pages for job in page.get("jobs", [])]
    head_sha, errors = validation_errors(run, jobs, argv[2], contract)
    print(f"head_sha={head_sha}")
    print(f"publish_eligible={'true' if not errors else 'false'}")
    for error in errors:
        print(f"validation_error={error}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
