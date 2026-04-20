#!/usr/bin/env python3
"""
Kover XML 리포트를 집계해서 GitHub Step Summary 에 모듈별 coverage 표를 출력한다.

Usage:
    aggregate-kover-coverage.py <coverage-root>

Each module's report.xml (or reportJvm.xml) is parsed for report-level
INSTRUCTION counter. The module name is extracted from the grandparent
directory of the report file.
"""
import os
import sys
import glob
import xml.etree.ElementTree as ET


def parse_report(path: str) -> tuple[int, int]:
    """Return (covered, missed) for INSTRUCTION counter at report root."""
    try:
        root = ET.parse(path).getroot()
        missed = 0
        covered = 0
        for c in root.findall("counter"):
            if c.get("type") == "INSTRUCTION":
                missed += int(c.get("missed", "0"))
                covered += int(c.get("covered", "0"))
        return covered, missed
    except Exception:
        return 0, 0


def module_from_path(path: str) -> str:
    # path: coverage-artifacts/<artifact>/build/<module>/reports/kover/report.xml
    # 3-level dirname climbs from report.xml -> kover -> reports -> <module>
    return os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(path))))


def main() -> int:
    root_dir = sys.argv[1] if len(sys.argv) > 1 else "coverage-artifacts"
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")

    patterns = [
        f"{root_dir}/**/report.xml",
        f"{root_dir}/**/reportJvm.xml",
    ]

    rows: list[tuple[str, int, int, float]] = []
    total_covered = 0
    total_missed = 0

    seen_modules: set[str] = set()
    for pattern in patterns:
        for xml_path in sorted(glob.glob(pattern, recursive=True)):
            module = module_from_path(xml_path)
            if module in seen_modules:
                continue
            seen_modules.add(module)

            covered, missed = parse_report(xml_path)
            total = covered + missed
            pct = (covered * 100.0 / total) if total else 0.0
            rows.append((module, covered, missed, pct))
            total_covered += covered
            total_missed += missed

    lines: list[str] = []
    lines.append("## Kover Coverage Summary")
    lines.append("")
    if not rows:
        lines.append("_No coverage reports found._")
    else:
        lines.append("| Module | Instruction Covered | Instruction Missed | Coverage |")
        lines.append("|--------|--------------------:|-------------------:|---------:|")
        for module, covered, missed, pct in rows:
            lines.append(f"| `{module}` | {covered} | {missed} | {pct:.2f}% |")
        grand_total = total_covered + total_missed
        grand_pct = (total_covered * 100.0 / grand_total) if grand_total else 0.0
        lines.append(f"| **TOTAL** | **{total_covered}** | **{total_missed}** | **{grand_pct:.2f}%** |")

    output = "\n".join(lines) + "\n"
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fp:
            fp.write(output)
    # 로그용 stdout 출력
    print(output)
    return 0


if __name__ == "__main__":
    sys.exit(main())
