#!/usr/bin/env python3
"""Unit tests for the Nightly image-gate shard aggregate contract."""

from __future__ import annotations

import unittest

from scripts.aggregate_testcontainers_image_gate import (
    AggregationError,
    aggregate_summaries,
)


def _result(family_id: str, *, release_required: bool = True) -> dict[str, object]:
    return {
        "id": family_id,
        "server": f"{family_id.title()}Server",
        "release_required": release_required,
        "status": "success",
        "attempts": [],
    }


def _summary(
    index: int, results: list[dict[str, object]], *, count: int = 2
) -> dict[str, object]:
    release_results = [
        result for result in results if result.get("release_required") is True
    ]
    release_success = sum(
        result.get("status") == "success" for result in release_results
    )
    success = sum(result.get("status") == "success" for result in results)
    product_failure = sum(
        result.get("status") == "product_failure" for result in results
    )
    infrastructure_failure = sum(
        result.get("status") == "infrastructure_failure" for result in results
    )
    blocked = sum(result.get("status") == "blocked" for result in results)
    return {
        "schema_version": 2,
        "generated_at": "2026-08-29T00:00:00+00:00",
        "manifest_digest": "manifest-digest",
        "workflow_run_id": "123",
        "commit": "a" * 40,
        "ref": "refs/heads/develop",
        "scope": "full",
        "status": "success" if success == len(results) else "failed",
        "selected": len(results),
        "success": success,
        "product_failure": product_failure,
        "infrastructure_failure": infrastructure_failure,
        "blocked": blocked,
        "coverage": f"{release_success}/{len(release_results)}",
        "release_coverage": f"{release_success}/{len(release_results)}",
        "selected_coverage": f"{success}/{len(results)}",
        "release_required_selected": len(release_results),
        "release_required_success": release_success,
        "release_gate": bool(release_results)
        and release_success == len(release_results),
        "results": results,
        "platforms": [],
        "shard": {
            "index": index,
            "count": count,
            "family_ids": [result["id"] for result in results],
        },
    }


class TestAggregateTestcontainersImageGate(unittest.TestCase):
    def test_aggregate_preserves_all_families_and_release_coverage(self) -> None:
        entries = [
            {"id": "alpha", "releaseRequired": True},
            {"id": "beta", "releaseRequired": True},
            {"id": "support", "releaseRequired": False},
        ]
        aggregate = aggregate_summaries(
            [
                _summary(0, [_result("alpha")]),
                _summary(
                    1, [_result("beta"), _result("support", release_required=False)]
                ),
            ],
            entries,
            expected_shard_count=2,
        )
        self.assertEqual("3/3", aggregate["selected_coverage"])
        self.assertEqual("2/2", aggregate["coverage"])
        self.assertTrue(aggregate["release_gate"])
        self.assertEqual(
            ["alpha", "beta", "support"], [item["id"] for item in aggregate["results"]]
        )
        self.assertEqual([0, 1], [item["index"] for item in aggregate["shards"]])

    def test_aggregate_rejects_missing_or_duplicate_family(self) -> None:
        entries = [
            {"id": "alpha", "releaseRequired": True},
            {"id": "beta", "releaseRequired": True},
        ]
        with self.assertRaisesRegex(AggregationError, "missing family ids"):
            aggregate_summaries(
                [_summary(0, [_result("alpha")], count=1)],
                entries,
                expected_shard_count=1,
            )
        with self.assertRaisesRegex(AggregationError, "duplicate family id"):
            aggregate_summaries(
                [_summary(0, [_result("alpha")]), _summary(1, [_result("alpha")])],
                entries,
                expected_shard_count=2,
            )

    def test_aggregate_rejects_shard_counter_drift(self) -> None:
        entries = [{"id": "alpha", "releaseRequired": True}]
        summary = _summary(0, [_result("alpha")], count=1)
        summary["blocked"] = 1
        with self.assertRaisesRegex(AggregationError, "counter mismatch"):
            aggregate_summaries([summary], entries, expected_shard_count=1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
