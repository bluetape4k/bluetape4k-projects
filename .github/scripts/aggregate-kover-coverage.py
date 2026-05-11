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
from dataclasses import dataclass, field
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
        methods: dict[tuple[str, str, str], InstructionCounter] = {}

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
    except Exception:
        return {}, InstructionCounter()


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
    root_dir = sys.argv[1] if len(sys.argv) > 1 else "coverage-artifacts"
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")

    patterns = [
        f"{root_dir}/**/report.xml",
        f"{root_dir}/**/reportJvm.xml",
    ]

    modules: dict[str, ModuleCoverage] = {}
    rows: list[tuple[str, int, int, float, int]] = []
    total_covered = 0
    total_missed = 0
    skipped_zero_total = 0

    for pattern in patterns:
        for xml_path in sorted(glob.glob(pattern, recursive=True)):
            module = module_from_path(xml_path)
            modules.setdefault(module, ModuleCoverage()).add_report(xml_path, module)

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

    lines: list[str] = []
    lines.append("## Kover Coverage Summary")
    lines.append("")
    if not rows:
        lines.append("_No coverage reports found._")
    else:
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
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fp:
            fp.write(output)
    # 로그용 stdout 출력
    print(output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
