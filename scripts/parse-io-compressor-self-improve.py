#!/usr/bin/env python3
import json
import math
import sys
from pathlib import Path


def fail(message: str, code: int = 1) -> None:
    print(json.dumps({"error": message}, sort_keys=True))
    raise SystemExit(code)


def main() -> None:
    if len(sys.argv) != 6:
        fail(
            "usage: parse-io-compressor-self-improve.py "
            "<jmh-json> <benchmark> <payloadKind> <payloadSize> <compressorName>",
            2,
        )

    json_path = Path(sys.argv[1])
    benchmark, payload_kind, payload_size, compressor_name = sys.argv[2:]
    results = json.loads(json_path.read_text())

    compressor_names = {name.strip() for name in compressor_name.split(",") if name.strip()}
    if not compressor_names:
        fail("at least one compressorName is required", 2)

    matches = []
    for row in results:
        params = row.get("params", {})
        if (
            row.get("benchmark") == benchmark
            and (payload_kind == "*" or params.get("payloadKind") == payload_kind)
            and (payload_size == "*" or params.get("payloadSize") == payload_size)
            and params.get("compressorName") in compressor_names
        ):
            matches.append(row)

    if not matches:
        fail("expected at least one primary result, found 0", 3)

    scores = []
    score_errors = []
    for row in matches:
        primary = row["primaryMetric"]
        score = float(primary["score"])
        if score <= 0 or not math.isfinite(score):
            fail(f"non-positive or non-finite score: {score}", 4)

        unit = primary["scoreUnit"]
        if unit != "ops/s":
            fail(f"unexpected score unit: {unit}", 5)

        raw_score_error = primary.get("scoreError")
        score_error = float(raw_score_error) if raw_score_error not in (None, "NaN") else None
        if score_error is not None and math.isfinite(score_error):
            score_errors.append(score_error)
        scores.append(score)

    score = math.exp(sum(math.log(value) for value in scores) / len(scores))
    score_error = None
    if len(score_errors) == len(scores):
        score_error = math.exp(sum(math.log(value) for value in score_errors) / len(score_errors))

    print(
        json.dumps(
            {
                "primary": score,
                "primary_aggregation": "geometric_mean" if len(matches) > 1 else "single",
                "primary_metric": benchmark,
                "direction": "higher_is_better",
                "sample_count": len(matches),
                "score_error": score_error,
                "unit": "ops/s",
                "params": {
                    "payloadKind": payload_kind,
                    "payloadSize": payload_size,
                    "compressorName": sorted(compressor_names),
                },
                "raw_json": str(json_path),
            },
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
