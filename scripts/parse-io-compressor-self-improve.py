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

    matches = []
    for row in results:
        params = row.get("params", {})
        if (
            row.get("benchmark") == benchmark
            and params.get("payloadKind") == payload_kind
            and params.get("payloadSize") == payload_size
            and params.get("compressorName") == compressor_name
        ):
            matches.append(row)

    if len(matches) != 1:
        fail(f"expected exactly one primary result, found {len(matches)}", 3)

    primary = matches[0]["primaryMetric"]
    score = float(primary["score"])
    raw_score_error = primary.get("scoreError")
    score_error = float(raw_score_error) if raw_score_error not in (None, "NaN") else None
    if score_error is not None and not math.isfinite(score_error):
        score_error = None
    unit = primary["scoreUnit"]
    if unit != "ops/s":
        fail(f"unexpected score unit: {unit}", 4)

    print(
        json.dumps(
            {
                "primary": score,
                "primary_metric": benchmark,
                "direction": "higher_is_better",
                "score_error": score_error,
                "unit": unit,
                "params": {
                    "payloadKind": payload_kind,
                    "payloadSize": payload_size,
                    "compressorName": compressor_name,
                },
                "raw_json": str(json_path),
            },
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
