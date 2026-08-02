#!/usr/bin/env python3
"""Normalize the JMH heap event counter into an explicit byte metric."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from statistics import fmean
from typing import Any


METRIC_NAME = "jvmUsedHeapBytes"
NORMALIZED_METRIC = "jvm.used_heap"
SAMPLING_POINT = "after_ready_before_shutdown"


def _as_number(value: Any) -> int | float:
    if isinstance(value, (int, float)) and float(value).is_integer():
        return int(value)
    return value


def _samples(raw_data: list[Any]) -> list[int | float]:
    result: list[int | float] = []
    for iteration in raw_data:
        values = iteration if isinstance(iteration, list) else [iteration]
        result.extend(_as_number(value) for value in values)
    return result


def normalize(input_path: Path) -> dict[str, Any]:
    payload = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(payload, list):
        raise ValueError("JMH report must contain a top-level array")

    results: list[dict[str, Any]] = []
    for entry in payload:
        secondary = entry.get("secondaryMetrics", {})
        metric = secondary.get(METRIC_NAME)
        if not metric:
            continue

        samples = _samples(metric.get("rawData", []))
        if not samples:
            raise ValueError(f"{entry.get('benchmark', '<unknown>')} has no heap samples")

        results.append(
            {
                "benchmark": entry["benchmark"],
                "jmhVersion": entry.get("jmhVersion"),
                "jdkVersion": entry.get("jdkVersion"),
                "sampleCount": len(samples),
                "averageBytes": _as_number(fmean(samples)),
                "samples": samples,
            }
        )

    if not results:
        raise ValueError(f"no {METRIC_NAME} secondary metric found in {input_path}")

    return {
        "schemaVersion": 1,
        "metric": NORMALIZED_METRIC,
        "unit": "bytes",
        "samplingPoint": SAMPLING_POINT,
        "sourceArtifact": input_path.name,
        "sourceMetric": METRIC_NAME,
        "results": results,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="JMH JSON report from memoryBenchmark")
    parser.add_argument("output", type=Path, help="normalized JSON output path")
    args = parser.parse_args()

    normalized = normalize(args.input)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(normalized, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
