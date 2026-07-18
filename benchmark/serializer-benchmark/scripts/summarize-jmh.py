#!/usr/bin/env python3

import argparse
import csv
import json
from pathlib import Path


CLAIM_THRESHOLD_PERCENT = 5.0
INELIGIBLE_TOKENS = ("Compatibility", "Fallback")
BASELINE_SUFFIXES = (
    "FallbackReadOnly",
    "FallbackDirect",
    "OptimizedHeap",
    "Compatibility",
    "Optimized",
    "Fallback",
)
RUN_FIELDS = (
    "run_id",
    "benchmark",
    "method",
    "throughput_score",
    "throughput_error",
    "throughput_unit",
    "gc_alloc_rate_norm_b_op",
    "gc_alloc_rate_mb_s",
    "gc_count",
    "eligible",
)
COMPARISON_FIELDS = (
    "method",
    "baseline",
    "eligible",
    "run_1_id",
    "run_1_baseline_b_op",
    "run_1_candidate_b_op",
    "run_1_delta_pct",
    "run_2_id",
    "run_2_baseline_b_op",
    "run_2_candidate_b_op",
    "run_2_delta_pct",
    "verdict",
)


def load_jmh(path):
    source = Path(path)
    try:
        payload = json.loads(source.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError("Invalid JMH JSON in {}: {}".format(source, exc)) from exc
    if not isinstance(payload, list):
        raise ValueError("JMH JSON top level must be a list: {}".format(source))
    return payload


def extract_rows(entries, run_id):
    rows = []
    for entry in entries:
        benchmark = _required_mapping(entry, "benchmark")
        primary = _required_mapping(entry, "primaryMetric")
        secondary = _required_mapping(entry, "secondaryMetrics")
        allocation = secondary.get("gc.alloc.rate.norm")
        if not isinstance(allocation, dict) or "score" not in allocation:
            raise ValueError("{} is missing gc.alloc.rate.norm".format(benchmark))
        method = str(benchmark).rsplit(".", 1)[-1]
        rows.append(
            {
                "run_id": run_id,
                "benchmark": benchmark,
                "method": method,
                "throughput_score": _required_score(primary, "primaryMetric", benchmark),
                "throughput_error": primary.get("scoreError", "N/A"),
                "throughput_unit": primary.get("scoreUnit", "N/A"),
                "gc_alloc_rate_norm_b_op": _required_score(
                    allocation, "gc.alloc.rate.norm", benchmark
                ),
                "gc_alloc_rate_mb_s": _optional_score(secondary.get("gc.alloc.rate")),
                "gc_count": _optional_score(secondary.get("gc.count")),
                "eligible": claim_eligible(method),
            }
        )
    return sorted(rows, key=lambda row: row["method"])


def baseline_name(method):
    for suffix in BASELINE_SUFFIXES:
        if method.endswith(suffix):
            return method[: -len(suffix)] + "ByteArray"
    return method


def claim_eligible(method):
    return "Optimized" in method and not any(token in method for token in INELIGIBLE_TOKENS)


def compare_runs(runs):
    if len(runs) != 2:
        raise ValueError("Comparison requires exactly two runs")
    run_ids = [_single_run_id(rows) for rows in runs]
    if len(set(run_ids)) != 2:
        raise ValueError("Comparison requires two unique run IDs")
    indexed = [{row["method"]: row for row in rows} for rows in runs]
    if set(indexed[0]) != set(indexed[1]):
        raise ValueError("Comparison runs must contain the same benchmark methods")

    compared = []
    for method in sorted(indexed[0]):
        baseline = baseline_name(method)
        if baseline == method:
            continue
        if baseline not in indexed[0] or baseline not in indexed[1]:
            raise ValueError("{} lacks matching baseline {}".format(method, baseline))
        eligible = claim_eligible(method)
        deltas = []
        baselines = []
        candidates = []
        for run_index in range(2):
            baseline_value = float(indexed[run_index][baseline]["gc_alloc_rate_norm_b_op"])
            candidate_value = float(indexed[run_index][method]["gc_alloc_rate_norm_b_op"])
            if baseline_value <= 0.0:
                raise ValueError("{} has non-positive baseline allocation".format(baseline))
            baselines.append(baseline_value)
            candidates.append(candidate_value)
            deltas.append(round((candidate_value - baseline_value) / baseline_value * 100.0, 6))
        verdict = "ineligible"
        if eligible:
            verdict = (
                "accepted"
                if all(delta <= -CLAIM_THRESHOLD_PERCENT for delta in deltas)
                else "inconclusive"
            )
        compared.append(
            {
                "method": method,
                "baseline": baseline,
                "eligible": eligible,
                "run_1_id": run_ids[0],
                "run_1_baseline_b_op": baselines[0],
                "run_1_candidate_b_op": candidates[0],
                "run_1_delta_pct": deltas[0],
                "run_2_id": run_ids[1],
                "run_2_baseline_b_op": baselines[1],
                "run_2_candidate_b_op": candidates[1],
                "run_2_delta_pct": deltas[1],
                "verdict": verdict,
            }
        )
    return compared


def write_csv(rows, path, fieldnames):
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow({key: _csv_value(row.get(key, "N/A")) for key in fieldnames})


def load_summary(path):
    source = Path(path)
    try:
        with source.open(encoding="utf-8", newline="") as input_file:
            rows = list(csv.DictReader(input_file))
    except OSError as exc:
        raise ValueError("Cannot read summary CSV {}: {}".format(source, exc)) from exc
    if not rows:
        raise ValueError("Summary CSV is empty: {}".format(source))
    for row in rows:
        row["eligible"] = str(row.get("eligible", "false")).lower() == "true"
    return rows


def _single_run_id(rows):
    if not rows:
        raise ValueError("Comparison run is empty")
    run_ids = {row["run_id"] for row in rows}
    if len(run_ids) != 1:
        raise ValueError("Each summary must contain exactly one run ID")
    return next(iter(run_ids))


def _required_mapping(entry, key):
    value = entry.get(key) if isinstance(entry, dict) else None
    if not isinstance(value, dict) and key != "benchmark":
        raise ValueError("JMH entry is missing {}".format(key))
    if key == "benchmark" and not isinstance(value, str):
        raise ValueError("JMH entry is missing benchmark")
    return value


def _required_score(metric, name, benchmark):
    if "score" not in metric:
        raise ValueError("{} is missing {} score".format(benchmark, name))
    return metric["score"]


def _optional_score(metric):
    if not isinstance(metric, dict) or "score" not in metric:
        return "N/A"
    return metric["score"]


def _csv_value(value):
    if isinstance(value, bool):
        return str(value).lower()
    return value


def _parser():
    parser = argparse.ArgumentParser(description="Summarize JMH allocation evidence")
    commands = parser.add_subparsers(dest="command", required=True)

    run = commands.add_parser("run", help="Extract one JMH JSON run")
    run.add_argument("--input", required=True)
    run.add_argument("--output", required=True)

    compare = commands.add_parser("compare", help="Compare exactly two summary CSV files")
    compare.add_argument("--run", action="append", required=True)
    compare.add_argument("--output", required=True)
    return parser


def main():
    args = _parser().parse_args()
    if args.command == "run":
        source = Path(args.input)
        rows = extract_rows(load_jmh(source), source.parent.name)
        write_csv(rows, args.output, RUN_FIELDS)
        return
    if len(args.run) != 2 or len(set(args.run)) != 2:
        raise ValueError("Comparison requires exactly two unique run files")
    rows = compare_runs([load_summary(path) for path in args.run])
    write_csv(rows, args.output, COMPARISON_FIELDS)


if __name__ == "__main__":
    main()
