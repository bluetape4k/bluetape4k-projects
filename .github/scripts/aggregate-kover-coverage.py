#!/usr/bin/env python3
"""
Kover XML 리포트를 집계해서 GitHub Step Summary 에 모듈별 coverage 표를 출력한다.

Usage:
    aggregate-kover-coverage.py <coverage-root>

Each module's report.xml (or reportJvm.xml) is parsed and duplicate reports for
the same module are merged. This matters for nightly matrix jobs where each
matrix entry runs a subset of the same module's tests and uploads a partial
Kover XML report.
"""
from __future__ import annotations

from dataclasses import dataclass, field
import argparse
import fnmatch
import os
import sys
import glob
import xml.etree.ElementTree as ET


@dataclass
class InstructionCounter:
    covered: int = 0
    missed: int = 0

    def merge_union(self, covered: int, missed: int) -> None:
        """Merge another partial report for the same method conservatively."""
        if self.covered or covered:
            self.covered = max(self.covered, covered)
            self.missed = min(self.missed, missed)
        else:
            self.missed = max(self.missed, missed)


@dataclass
class ModuleCoverage:
    reports: int = 0
    methods: dict[tuple[str, str, str], InstructionCounter] = field(default_factory=dict)
    fallback: InstructionCounter = field(default_factory=InstructionCounter)

    def add_report(self, path: str, module: str) -> None:
        self.reports += 1
        methods, fallback = parse_report(path, module)
        if methods:
            for key, counter in methods.items():
                self.methods.setdefault(key, InstructionCounter()).merge_union(
                    counter.covered,
                    counter.missed,
                )
            return

        self.fallback.merge_union(fallback.covered, fallback.missed)

    def totals(self) -> tuple[int, int]:
        if self.methods:
            return (
                sum(counter.covered for counter in self.methods.values()),
                sum(counter.missed for counter in self.methods.values()),
            )
        return self.fallback.covered, self.fallback.missed


MODULE_EXCLUDED_CLASS_PATTERNS: dict[str, tuple[str, ...]] = {
    "testing/testcontainers": (
        "io/bluetape4k/testcontainers/llm/*",
        "*$Launch",
        "*$Launch$*",
        "*$Launcher",
        "*$Launcher$*",
    ),
}


def is_excluded_class(module: str, class_name: str) -> bool:
    return any(
        fnmatch.fnmatch(class_name, pattern)
        for pattern in MODULE_EXCLUDED_CLASS_PATTERNS.get(module, ())
    )


def parse_report(
    path: str,
    module: str,
) -> tuple[dict[tuple[str, str, str], InstructionCounter], InstructionCounter]:
    """Return method-level INSTRUCTION counters, falling back to report root."""
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as error:
        raise ValueError(f"invalid Kover report {path}: {error}") from error

    methods: dict[tuple[str, str, str], InstructionCounter] = {}

    try:
        for pkg in root.findall("package"):
            for klass in pkg.findall("class"):
                class_name = klass.get("name", "")
                if is_excluded_class(module, class_name):
                    continue

                for method in klass.findall("method"):
                    for c in method.findall("counter"):
                        if c.get("type") == "INSTRUCTION":
                            key = (
                                class_name,
                                method.get("name", ""),
                                method.get("desc", ""),
                            )
                            methods[key] = InstructionCounter(
                                covered=int(c.get("covered", "0")),
                                missed=int(c.get("missed", "0")),
                            )

        fallback = InstructionCounter()
        for c in root.findall("counter"):
            if c.get("type") == "INSTRUCTION":
                fallback.missed += int(c.get("missed", "0"))
                fallback.covered += int(c.get("covered", "0"))
        return methods, fallback
    except (TypeError, ValueError) as error:
        raise ValueError(f"invalid Kover report {path}: {error}") from error


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("coverage_root", nargs="?", default="coverage-artifacts")
    parser.add_argument(
        "--expected-manifest",
        help="newline-delimited job=result manifest produced by the workflow",
    )
    parser.add_argument(
        "--download-outcome",
        default="success",
        help="outcome of the artifact download step",
    )
    return parser.parse_args()


def load_expected_manifest(path: str | None) -> dict[str, str]:
    if path is None:
        return {"manual": "success"}

    try:
        with open(path, encoding="utf-8") as manifest_file:
            lines = manifest_file.read().splitlines()
    except OSError as error:
        raise ValueError(f"cannot read expected coverage manifest {path}: {error}") from error

    jobs: dict[str, str] = {}
    allowed_results = {"success", "skipped", "failure", "cancelled"}
    for line_number, line in enumerate(lines, start=1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"invalid expected coverage manifest line {line_number}: {line}")
        name, result = (part.strip() for part in line.split("=", 1))
        if not name or result not in allowed_results:
            raise ValueError(f"invalid expected coverage manifest line {line_number}: {line}")
        if name in jobs:
            raise ValueError(f"duplicate expected coverage job: {name}")
        jobs[name] = result

    if not jobs:
        raise ValueError("expected coverage manifest must contain at least one job")
    return jobs


def write_summary(output: str, summary_path: str | None) -> None:
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fp:
            fp.write(output)
    print(output)


def module_from_path(path: str) -> str:
    # actual path: coverage-artifacts/<artifact>/<group>/<module>/build/reports/kover/report.xml
    # Find the last 'build' segment and return "<group>/<module>" before it.
    parts = path.replace("\\", "/").split("/")
    for i in range(len(parts) - 1, 0, -1):
        if parts[i] == "build":
            if i >= 2:
                return f"{parts[i - 2]}/{parts[i - 1]}"
            return parts[i - 1]
    # fallback: 4 levels up from report.xml
    return os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(path)))))


def main() -> int:
    args = parse_args()
    root_dir = args.coverage_root
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")

    try:
        expected_jobs = load_expected_manifest(args.expected_manifest)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    failed_jobs = sorted(
        name for name, result in expected_jobs.items() if result in {"failure", "cancelled"}
    )
    expected_success_jobs = sorted(
        name for name, result in expected_jobs.items() if result == "success"
    )
    if failed_jobs:
        print(
            "ERROR: expected coverage job(s) failed or cancelled: " + ", ".join(failed_jobs),
            file=sys.stderr,
        )
        return 1
    if expected_success_jobs and args.download_outcome != "success":
        print(
            f"ERROR: coverage artifact download finished with {args.download_outcome}",
            file=sys.stderr,
        )
        return 1

    patterns = [
        f"{root_dir}/**/report.xml",
        f"{root_dir}/**/reportJvm.xml",
    ]

    modules: dict[str, ModuleCoverage] = {}
    rows: list[tuple[str, int, int, float, int]] = []
    total_covered = 0
    total_missed = 0
    skipped_zero_total = 0

    report_paths = sorted(
        {
            xml_path
            for pattern in patterns
            for xml_path in glob.glob(pattern, recursive=True)
        }
    )

    if not expected_success_jobs:
        if report_paths:
            print(
                "ERROR: coverage reports were uploaded even though all coverage jobs were skipped",
                file=sys.stderr,
            )
            return 1
        output = "## Kover Coverage Summary\n\n_No coverage reports expected: all coverage jobs were skipped._\n"
        write_summary(output, summary_path)
        return 0

    if not report_paths:
        print(
            "ERROR: expected coverage job(s) produced no Kover reports: "
            + ", ".join(expected_success_jobs),
            file=sys.stderr,
        )
        return 1

    try:
        for xml_path in report_paths:
            module = module_from_path(xml_path)
            modules.setdefault(module, ModuleCoverage()).add_report(xml_path, module)
    except ValueError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    for module in sorted(modules):
        coverage = modules[module]
        covered, missed = coverage.totals()
        total = covered + missed
        if not total:
            skipped_zero_total += 1
            continue

        pct = covered * 100.0 / total
        rows.append((module, covered, missed, pct, coverage.reports))
        total_covered += covered
        total_missed += missed

    if not rows:
        print("ERROR: Kover reports contain no instruction coverage", file=sys.stderr)
        return 1

    lines: list[str] = []
    lines.append("## Kover Coverage Summary")
    lines.append("")
    lines.append("| Module | Reports | Instruction Covered | Instruction Missed | Coverage |")
    lines.append("|--------|--------:|--------------------:|-------------------:|---------:|")
    for module, covered, missed, pct, report_count in rows:
        lines.append(f"| `{module}` | {report_count} | {covered} | {missed} | {pct:.2f}% |")
    grand_total = total_covered + total_missed
    grand_pct = (total_covered * 100.0 / grand_total) if grand_total else 0.0
    lines.append(f"| **TOTAL** |  | **{total_covered}** | **{total_missed}** | **{grand_pct:.2f}%** |")
    if skipped_zero_total:
        lines.append("")
        lines.append(f"_Skipped {skipped_zero_total} zero-total coverage report(s)._")

    output = "\n".join(lines) + "\n"
    write_summary(output, summary_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
