#!/usr/bin/env python3
"""Aggregate fail-closed Testcontainers image-gate shard evidence."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from collections.abc import Iterable, Sequence
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
if str(REPOSITORY_ROOT) not in sys.path:
    sys.path.insert(0, str(REPOSITORY_ROOT))

from scripts.run_testcontainers_image_gate import verify_release_summary
from scripts.testcontainers_image_gate import (
    MANIFEST,
    load_manifest,
    validate_manifest,
)


class AggregationError(ValueError):
    """Raised when shard evidence cannot form one canonical release summary."""


def _manifest_digest(manifest_path: Path) -> str:
    return hashlib.sha256(manifest_path.read_bytes()).hexdigest()


def _metadata(summary: dict[str, Any], key: str) -> str:
    value = summary.get(key)
    if not isinstance(value, str) or not value:
        raise AggregationError(f"shard summary is missing {key}")
    return value


def aggregate_summaries(
    shard_summaries: Iterable[dict[str, Any]],
    manifest_entries: Sequence[dict[str, Any]],
    *,
    expected_shard_count: int,
    expected_manifest_digest: str | None = None,
) -> dict[str, Any]:
    """Merge shard summaries while rejecting overlap, omission, and drift."""

    if expected_shard_count < 1:
        raise AggregationError("expected shard count must be positive")
    expected_by_id = {str(entry["id"]): entry for entry in manifest_entries}
    if len(expected_by_id) != len(manifest_entries):
        raise AggregationError("manifest contains duplicate family ids")

    summaries = list(shard_summaries)
    if len(summaries) != expected_shard_count:
        raise AggregationError(
            f"expected {expected_shard_count} shard summaries, found {len(summaries)}"
        )

    seen_shards: set[int] = set()
    results_by_id: dict[str, dict[str, Any]] = {}
    platforms: list[dict[str, Any]] = []
    shard_rows: list[dict[str, Any]] = []
    shared_metadata: dict[str, str] | None = None

    for summary in summaries:
        if summary.get("schema_version") != 2:
            raise AggregationError("shard summary schema_version must be 2")
        if summary.get("scope") != "full":
            raise AggregationError("shard summary scope must be full")
        shard = summary.get("shard")
        if not isinstance(shard, dict):
            raise AggregationError("shard identity is required")
        index = shard.get("index")
        count = shard.get("count")
        if (
            not isinstance(index, int)
            or isinstance(index, bool)
            or not isinstance(count, int)
            or isinstance(count, bool)
            or count != expected_shard_count
            or index < 0
            or index >= expected_shard_count
        ):
            raise AggregationError("invalid shard identity")
        if index in seen_shards:
            raise AggregationError(f"duplicate shard index: {index}")
        seen_shards.add(index)

        metadata = {
            key: _metadata(summary, key)
            for key in ("manifest_digest", "workflow_run_id", "commit", "ref")
        }
        if (
            expected_manifest_digest is not None
            and metadata["manifest_digest"] != expected_manifest_digest
        ):
            raise AggregationError("manifest digest mismatch")
        if shared_metadata is None:
            shared_metadata = metadata
        elif metadata != shared_metadata:
            raise AggregationError("shard metadata mismatch")

        results = summary.get("results")
        if not isinstance(results, list) or not results:
            raise AggregationError(f"shard {index} has no family results")
        family_ids = shard.get("family_ids")
        result_ids = [
            result.get("id") for result in results if isinstance(result, dict)
        ]
        if family_ids != result_ids:
            raise AggregationError(
                f"shard {index} family identity does not match results"
            )
        for result in results:
            if not isinstance(result, dict):
                raise AggregationError(f"shard {index} contains a non-object result")
            family_id = result.get("id")
            if not isinstance(family_id, str) or family_id not in expected_by_id:
                raise AggregationError(f"unknown family id: {family_id}")
            if family_id in results_by_id:
                raise AggregationError(f"duplicate family id: {family_id}")
            expected_release = bool(expected_by_id[family_id].get("releaseRequired"))
            if result.get("release_required") is not expected_release:
                raise AggregationError(f"releaseRequired drift: {family_id}")
            results_by_id[family_id] = result

        allowed_statuses = (
            "success",
            "product_failure",
            "infrastructure_failure",
            "blocked",
        )
        if any(result.get("status") not in allowed_statuses for result in results):
            raise AggregationError(f"shard {index} contains an unknown result status")
        shard_counts = {
            status: sum(result.get("status") == status for result in results)
            for status in allowed_statuses
        }
        release_results = [
            result for result in results if result.get("release_required") is True
        ]
        release_success = sum(
            result.get("status") == "success" for result in release_results
        )
        expected_summary = {
            "selected": len(results),
            "success": shard_counts["success"],
            "product_failure": shard_counts["product_failure"],
            "infrastructure_failure": shard_counts["infrastructure_failure"],
            "blocked": shard_counts["blocked"],
            "coverage": f"{release_success}/{len(release_results)}",
            "release_coverage": f"{release_success}/{len(release_results)}",
            "selected_coverage": f"{shard_counts['success']}/{len(results)}",
            "release_required_selected": len(release_results),
            "release_required_success": release_success,
            "release_gate": bool(release_results)
            and release_success == len(release_results),
            "status": "success"
            if shard_counts["success"] == len(results)
            else "failed",
        }
        for key, expected in expected_summary.items():
            if summary.get(key) != expected:
                raise AggregationError(f"shard {index} counter mismatch: {key}")

        shard_platforms = summary.get("platforms")
        if not isinstance(shard_platforms, list):
            raise AggregationError(f"shard {index} platforms must be a list")
        if not all(isinstance(item, dict) for item in shard_platforms):
            raise AggregationError(f"shard {index} contains an invalid platform record")
        platforms.extend(shard_platforms)
        shard_rows.append(
            {
                "index": index,
                "count": count,
                "family_ids": list(result_ids),
            }
        )

    missing_shards = set(range(expected_shard_count)) - seen_shards
    if missing_shards:
        values = ", ".join(str(index) for index in sorted(missing_shards))
        raise AggregationError(f"missing shard indexes: {values}")
    missing_ids = set(expected_by_id) - set(results_by_id)
    if missing_ids:
        raise AggregationError(f"missing family ids: {', '.join(sorted(missing_ids))}")

    ordered_results = [results_by_id[str(entry["id"])] for entry in manifest_entries]
    counts = {
        status: sum(result.get("status") == status for result in ordered_results)
        for status in (
            "success",
            "product_failure",
            "infrastructure_failure",
            "blocked",
        )
    }
    release_results = [
        result for result in ordered_results if result.get("release_required") is True
    ]
    release_success = sum(
        result.get("status") == "success" for result in release_results
    )
    selected = len(ordered_results)
    release_selected = len(release_results)
    if shared_metadata is None:
        raise AggregationError("no shard metadata found")
    return {
        "schema_version": 2,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "manifest_digest": shared_metadata["manifest_digest"],
        "workflow_run_id": shared_metadata["workflow_run_id"],
        "commit": shared_metadata["commit"],
        "ref": shared_metadata["ref"],
        "scope": "full",
        "selected": selected,
        "success": counts["success"],
        "product_failure": counts["product_failure"],
        "infrastructure_failure": counts["infrastructure_failure"],
        "blocked": counts["blocked"],
        "coverage": f"{release_success}/{release_selected}",
        "release_coverage": f"{release_success}/{release_selected}",
        "selected_coverage": f"{counts['success']}/{selected}",
        "release_required_selected": release_selected,
        "release_required_success": release_success,
        "release_gate": release_selected > 0 and release_success == release_selected,
        "status": "success"
        if selected > 0 and counts["success"] == selected
        else "failed",
        "results": ordered_results,
        "platforms": platforms,
        "shards": sorted(shard_rows, key=lambda item: item["index"]),
    }


def _load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AggregationError(f"invalid JSON: {path}") from error
    if not isinstance(value, dict):
        raise AggregationError(f"JSON object required: {path}")
    return value


def _regular(path: Path) -> bool:
    return path.is_file() and not path.is_symlink()


def _write_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# Testcontainers image gate",
        "",
        f"- 상태: `{summary['status']}`",
        f"- release 증거 coverage: `{summary['coverage']}`",
        f"- 전체 선택/성공: `{summary['selected_coverage']}`",
        f"- 제품 실패: `{summary['product_failure']}`",
        f"- 인프라 실패: `{summary['infrastructure_failure']}`",
        f"- 차단: `{summary['blocked']}`",
        f"- stable release gate: `{str(summary['release_gate']).lower()}`",
        f"- manifest digest: `{summary['manifest_digest']}`",
        "",
        "| family | image | tag | status | attempts |",
        "|---|---|---|---|---:|",
    ]
    lines.extend(
        f"| `{result.get('server', result.get('id'))}` | `{result.get('image', '')}` | "
        f"`{result.get('tag', '')}` | `{result.get('status')}` | "
        f"{len(result.get('attempts', []))} |"
        for result in summary["results"]
    )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def aggregate_reports(
    input_dir: Path,
    output_dir: Path,
    *,
    manifest_path: Path = MANIFEST,
    expected_shard_count: int,
    platform_id: str,
    expected_tag: str,
    expected_architecture: str,
) -> dict[str, Any]:
    """Load shard artifacts, create canonical artifacts, and verify the release gate."""

    manifest_entries = load_manifest(manifest_path)
    errors = validate_manifest(manifest_entries, REPOSITORY_ROOT)
    if errors:
        raise AggregationError("manifest contract drift: " + "; ".join(errors))
    if not input_dir.is_dir() or input_dir.is_symlink():
        raise AggregationError(f"shard artifact directory is missing: {input_dir}")
    shard_dirs = [
        path
        for path in sorted(input_dir.iterdir())
        if path.is_dir() and not path.is_symlink()
    ]
    if len(shard_dirs) != expected_shard_count:
        raise AggregationError(
            f"expected {expected_shard_count} shard artifact directories, found {len(shard_dirs)}"
        )

    shard_summaries: list[dict[str, Any]] = []
    family_sources: dict[str, Path] = {}
    expected_ids = {str(entry["id"]) for entry in manifest_entries}
    for shard_dir in shard_dirs:
        summary_path = shard_dir / "summary.json"
        if not _regular(summary_path):
            raise AggregationError(f"missing shard summary: {summary_path}")
        summary = _load_json(summary_path)
        shard_summaries.append(summary)
        results = summary.get("results")
        if not isinstance(results, list):
            raise AggregationError(f"shard results must be a list: {summary_path}")
        for result in results:
            if not isinstance(result, dict):
                raise AggregationError(
                    f"shard result must be an object: {summary_path}"
                )
            family_id = result.get("id")
            if family_id not in expected_ids:
                raise AggregationError(f"unknown family artifact: {family_id}")
            source = shard_dir / f"{family_id}.json"
            if not _regular(source):
                raise AggregationError(f"missing family artifact: {source}")
            payload = _load_json(source)
            if payload.get("id") != family_id:
                raise AggregationError(f"family artifact identity mismatch: {source}")
            if payload.get("status") != result.get("status"):
                raise AggregationError(f"family artifact status mismatch: {source}")
            if family_id in family_sources:
                raise AggregationError(f"duplicate family artifact: {family_id}")
            family_sources[family_id] = source

    summary = aggregate_summaries(
        shard_summaries,
        manifest_entries,
        expected_shard_count=expected_shard_count,
        expected_manifest_digest=_manifest_digest(manifest_path),
    )
    output_dir.mkdir(parents=True, exist_ok=True)
    if any(output_dir.iterdir()):
        raise AggregationError(f"aggregate output directory is not empty: {output_dir}")
    for entry in manifest_entries:
        family_id = str(entry["id"])
        source = family_sources.get(family_id)
        if source is None:
            raise AggregationError(f"missing family ids: {family_id}")
        shutil.copy2(source, output_dir / source.name)
    summary_path = output_dir / "summary.json"
    summary_path.write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    _write_markdown(output_dir / "summary.md", summary)

    expected_release_count = sum(
        bool(entry.get("releaseRequired")) for entry in manifest_entries
    )
    verification_errors = verify_release_summary(
        summary,
        expected_coverage=f"{expected_release_count}/{expected_release_count}",
        platform_id=platform_id,
        expected_tag=expected_tag,
        expected_architecture=expected_architecture,
        report_dir=output_dir,
    )
    if verification_errors:
        raise AggregationError("; ".join(verification_errors))
    return summary


def _write_blocked_summary(
    output_dir: Path, errors: Sequence[str], manifest_path: Path
) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    summary = {
        "schema_version": 2,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "manifest_digest": _manifest_digest(manifest_path)
        if manifest_path.is_file()
        else "",
        "workflow_run_id": "local",
        "commit": "local",
        "ref": "local",
        "scope": "full",
        "selected": 0,
        "success": 0,
        "product_failure": 0,
        "infrastructure_failure": 0,
        "blocked": 1,
        "coverage": "0/0",
        "release_coverage": "0/0",
        "selected_coverage": "0/0",
        "release_required_selected": 0,
        "release_required_success": 0,
        "release_gate": False,
        "status": "blocked",
        "errors": list(errors),
        "results": [],
        "platforms": [],
        "shards": [],
    }
    (output_dir / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    _write_markdown(output_dir / "summary.md", summary)


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--expected-shards", type=int, required=True)
    parser.add_argument("--platform-id", default="amd64")
    parser.add_argument("--expected-tag", default="2.18.0")
    parser.add_argument("--expected-architecture", default="amd64")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        summary = aggregate_reports(
            args.input_dir,
            args.output_dir,
            manifest_path=args.manifest,
            expected_shard_count=args.expected_shards,
            platform_id=args.platform_id,
            expected_tag=args.expected_tag,
            expected_architecture=args.expected_architecture,
        )
    except (AggregationError, OSError, ValueError) as error:
        _write_blocked_summary(args.output_dir, [str(error)], args.manifest)
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                key: summary[key]
                for key in (
                    "status",
                    "coverage",
                    "selected_coverage",
                    "product_failure",
                    "infrastructure_failure",
                    "blocked",
                    "release_gate",
                )
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
