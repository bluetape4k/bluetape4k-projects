#!/usr/bin/env python3
"""Fail-closed validation for the issue #756 Fory follow-up allocation chart."""

from __future__ import annotations

import hashlib
import json
import math
import struct
import xml.etree.ElementTree as ET
from pathlib import Path


RAW_ROOT = Path(__file__).resolve().parent
REPO_ROOT = RAW_ROOT.parents[3]
SOURCE_PATH = REPO_ROOT / "docs/images/readme-charts/issue756-fory-followup-allocation-chart-01-source.json"
SVG_PATH = REPO_ROOT / "docs/images/readme-charts/issue756-fory-followup-allocation-chart-01.svg"
PNG_PATH = REPO_ROOT / "docs/images/readme-charts/issue756-fory-followup-allocation-chart-01.png"
SUMMARY_PATH = REPO_ROOT / "docs/images/readme-charts/issue756-fory-followup-allocation-chart-01-summary.txt"
MANIFEST_PATH = RAW_ROOT / "manifest.json"


def load_json(path: Path):
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


def cell_id(module: str, row: dict) -> str:
    storage = row.get("target", row.get("source"))
    operation = row.get("operation", "encode")
    return f"{module}-{row['backend']}-{storage}-{operation}"


def main() -> None:
    source = load_json(SOURCE_PATH)
    manifest_bytes = MANIFEST_PATH.read_bytes()
    manifest_sha = hashlib.sha256(manifest_bytes).hexdigest()
    manifest = json.loads(manifest_bytes)

    require(source.get("schema_version") == 1, "unsupported chart-source schema")
    require(source.get("manifest_sha256") == manifest_sha, "manifest SHA binding mismatch")
    require(manifest.get("canonical_method_count") == 20, "canonical method count must be 20")
    require(manifest.get("encodeDisposition") == "rejected", "encode disposition must be rejected")
    require(
        source.get("metric")
        == {
            "name": "allocation_reduction",
            "formula": "(1 - candidate_allocation / baseline_allocation) * 100",
            "unit": "%",
            "direction": "higher-is-better",
        },
        "metric contract mismatch",
    )
    require(source.get("runs") == ["canonical-a", "canonical-b"], "canonical run list mismatch")

    raw_by_run: dict[str, dict[str, dict]] = {}
    all_dispositions: dict[str, str] = {}
    for run_key, run_dir in (("canonical_a", "canonical-a"), ("canonical_b", "canonical-b")):
        accepted: dict[str, dict] = {}
        for module in ("lettuce", "redisson"):
            rows = load_json(RAW_ROOT / module / run_dir / "comparison.json")
            for row in rows:
                current_id = cell_id(module, row)
                prior = all_dispositions.setdefault(current_id, row["disposition"])
                require(prior == row["disposition"], f"cross-run disposition drift: {current_id}")
                if row["disposition"] == "accepted":
                    require(row.get("promotable") is True, f"accepted cell is not promotable: {current_id}")
                    accepted[current_id] = row
        raw_by_run[run_key] = accepted

    expected_ids = set(raw_by_run["canonical_a"]) & set(raw_by_run["canonical_b"])
    require(len(expected_ids) == 6, "accepted cell count must be exactly 6")
    require(set(raw_by_run["canonical_a"]) == expected_ids, "canonical-a accepted set mismatch")
    require(set(raw_by_run["canonical_b"]) == expected_ids, "canonical-b accepted set mismatch")

    entries = source.get("accepted_cells")
    require(isinstance(entries, list), "accepted_cells must be a list")
    source_ids = [entry.get("id") for entry in entries]
    require(len(source_ids) == len(set(source_ids)), "duplicate accepted chart cell")
    require(set(source_ids) == expected_ids, "chart accepted set does not match raw evidence")

    for entry in entries:
        current_id = entry["id"]
        require(all_dispositions.get(current_id) == "accepted", f"non-accepted cell plotted: {current_id}")
        for run_key in ("canonical_a", "canonical_b"):
            raw = raw_by_run[run_key][current_id]
            actual = entry[run_key]
            ratio = raw["allocation_ratio"]
            reduction = (1.0 - ratio) * 100.0
            require(math.isfinite(ratio) and ratio > 0.0, f"invalid raw ratio: {current_id}/{run_key}")
            require(
                math.isclose(actual["allocation_ratio"], ratio, rel_tol=0.0, abs_tol=1e-15),
                f"allocation ratio mismatch: {current_id}/{run_key}",
            )
            require(
                math.isclose(actual["allocation_reduction_percent"], reduction, rel_tol=0.0, abs_tol=1e-12),
                f"allocation reduction mismatch: {current_id}/{run_key}",
            )

    excluded = source.get("excluded_dispositions", {})
    declared_excluded = {
        disposition: set(excluded.get(disposition, []))
        for disposition in ("rejected", "fallback", "inconclusive")
    }
    terminal_by_disposition = {
        disposition: {current_id for current_id, actual in all_dispositions.items() if actual == disposition}
        for disposition in ("rejected", "fallback", "inconclusive")
    }
    if manifest["encodeDisposition"] == "rejected":
        terminal_by_disposition["rejected"].update(
            {"redisson-fory-encode", "redisson-fastFory-encode"}
        )
    for disposition, ids in declared_excluded.items():
        require(
            ids == terminal_by_disposition[disposition],
            f"excluded {disposition} set mismatch",
        )

    root = ET.parse(SVG_PATH).getroot()
    require(root.get("data-manifest-sha") == manifest_sha, "SVG manifest SHA mismatch")
    require(root.get("data-source-file") == SOURCE_PATH.name, "SVG chart-source reference mismatch")
    outputs = source["outputs"]
    require(root.get("width") == str(outputs["svg_width"]), "SVG width mismatch")
    require(root.get("height") == str(outputs["svg_height"]), "SVG height mismatch")
    svg_cells = {}
    for element in root.iter():
        current_id = element.get("data-cell-id")
        if current_id is None:
            continue
        require(current_id not in svg_cells, f"duplicate SVG cell: {current_id}")
        svg_cells[current_id] = element
    require(set(svg_cells) == expected_ids, "SVG accepted-cell set mismatch")
    source_by_id = {entry["id"]: entry for entry in entries}
    for current_id, element in svg_cells.items():
        expected = source_by_id[current_id]
        require(
            math.isclose(float(element.get("data-run-a", "nan")), expected["canonical_a"]["allocation_reduction_percent"], rel_tol=0.0, abs_tol=1e-12),
            f"SVG canonical-a value mismatch: {current_id}",
        )
        require(
            math.isclose(float(element.get("data-run-b", "nan")), expected["canonical_b"]["allocation_reduction_percent"], rel_tol=0.0, abs_tol=1e-12),
            f"SVG canonical-b value mismatch: {current_id}",
        )

    png_bytes = PNG_PATH.read_bytes()
    require(png_bytes[:8] == b"\x89PNG\r\n\x1a\n", "rendered PNG signature mismatch")
    width, height = struct.unpack(">II", png_bytes[16:24])
    require((width, height) == (outputs["png_width"], outputs["png_height"]), "rendered PNG dimensions mismatch")

    summary = SUMMARY_PATH.read_text(encoding="utf-8")
    require(f"manifestSha256={manifest_sha}" in summary, "summary manifest SHA mismatch")
    require("acceptedCells=6" in summary and "acceptedOnly=PASS" in summary, "summary accepted-only gate missing")
    require("excludedRejectedCells=4" in summary, "summary rejected-cell count mismatch")
    require("excludedFallbackCells=2" in summary, "summary fallback-cell count mismatch")
    require("excludedInconclusiveCells=0" in summary, "summary inconclusive-cell count mismatch")
    require("status=PASS" in summary, "summary terminal status missing")
    print(f"PASS: accepted_cells=6 manifest_sha256={manifest_sha} png={width}x{height}")


if __name__ == "__main__":
    main()
