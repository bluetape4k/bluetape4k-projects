#!/usr/bin/env python3
"""Inventory Korean localization scope for single-language docs and Kotlin KDoc."""

from __future__ import annotations

import argparse
import re
import subprocess
from collections import Counter
from pathlib import Path

DOC_SUFFIXES = {".md", ".mdx", ".adoc", ".rst", ".txt"}
KOTLIN_SUFFIXES = {".kt", ".kts"}
OPERATING_PARTS = {
    ".codex",
    ".omc",
    ".omx",
    "prompts",
    "hooks",
}
OPERATING_NAMES = {
    "AGENTS.md",
    "CLAUDE.md",
    "SKILL.md",
    "copilot-instructions.md",
    "git-commit-instructions.md",
    "pull_request_template.md",
}
PUBLIC_ENGLISH_NAMES = {
    "CHANGELOG.md",
    "SECURITY.md",
}
MANUAL_EN = Path("docs/manual/en")
MANUAL_KO = Path("docs/manual/ko")
KDOC_BLOCK = re.compile(r"/\\*\\*.*?\\*/", re.DOTALL)
ENGLISH_KDOC_POLICY = [
    re.compile(r"KDoc\\s+in\\s+English", re.IGNORECASE),
    re.compile(r"English\\s+KDoc", re.IGNORECASE),
    re.compile(r"Write\\s+KDoc\\s+in\\s+English", re.IGNORECASE),
]


def git_files(root: Path) -> list[Path]:
    output = subprocess.check_output(
        ["git", "ls-files"], cwd=root, text=True, encoding="utf-8"
    )
    return [Path(line) for line in output.splitlines() if line]


def has_part(path: Path, parts: set[str]) -> bool:
    return any(part in parts for part in path.parts)


def is_under(path: Path, parent: Path) -> bool:
    try:
        path.relative_to(parent)
        return True
    except ValueError:
        return False


def doc_bucket(path: Path) -> str:
    if len(path.parts) == 1:
        return "root"
    if path.parts[0] == "docs" and len(path.parts) >= 2:
        return f"docs/{path.parts[1]}"
    return path.parts[0]


def kotlin_bucket(path: Path) -> str:
    if len(path.parts) >= 2 and path.parts[0] in {
        "bluetape4k",
        "io",
        "data",
        "infra",
        "cache",
        "ktor",
        "spring-boot",
        "testing",
        "utils",
        "virtualthread",
        "examples",
    }:
        return f"{path.parts[0]}/{path.parts[1]}"
    return path.parts[0] if path.parts else "root"


def classify_doc(path: Path) -> str:
    name = path.name
    if name.upper().startswith("README"):
        return "excluded-readme"
    if name in PUBLIC_ENGLISH_NAMES:
        return "excluded-public-english"
    if name in OPERATING_NAMES or has_part(path, OPERATING_PARTS):
        return "excluded-operating"
    if is_under(path, MANUAL_EN) or is_under(path, MANUAL_KO):
        return "manual-pair-parity-only"
    if path.suffix.lower() in DOC_SUFFIXES:
        return "in-scope-doc"
    return "not-doc"


def count_kdoc_blocks(root: Path, files: list[Path]) -> Counter[str]:
    counts: Counter[str] = Counter()
    for path in files:
        if path.suffix.lower() not in KOTLIN_SUFFIXES:
            continue
        try:
            text = (root / path).read_text(encoding="utf-8")
        except UnicodeDecodeError:
            text = (root / path).read_text(encoding="utf-8", errors="ignore")
        blocks = len(KDOC_BLOCK.findall(text))
        if blocks:
            counts[kotlin_bucket(path)] += blocks
    return counts


def find_english_kdoc_policy_drift(root: Path, files: list[Path]) -> list[tuple[Path, int, str]]:
    findings: list[tuple[Path, int, str]] = []
    for path in files:
        if path.suffix.lower() not in DOC_SUFFIXES:
            continue
        if classify_doc(path) == "excluded-readme":
            continue
        try:
            text = (root / path).read_text(encoding="utf-8")
        except UnicodeDecodeError:
            text = (root / path).read_text(encoding="utf-8", errors="ignore")
        for line_no, line in enumerate(text.splitlines(), start=1):
            if any(pattern.search(line) for pattern in ENGLISH_KDOC_POLICY):
                findings.append((path, line_no, line.strip()))
    return findings


def manual_relative_set(files: list[Path], root_dir: Path) -> set[Path]:
    rels: set[Path] = set()
    for path in files:
        if path.suffix.lower() not in DOC_SUFFIXES:
            continue
        if is_under(path, root_dir):
            rels.add(path.relative_to(root_dir))
    return rels


def render_counter(title: str, counter: Counter[str], limit: int | None = None) -> list[str]:
    lines = [f"### {title}", "", "| Bucket | Count |", "|---|---:|"]
    items = counter.most_common(limit)
    lines.extend(f"| `{bucket}` | {count} |" for bucket, count in items)
    lines.append("")
    return lines


def render_sample(title: str, paths: list[Path], limit: int = 40) -> list[str]:
    lines = [f"### {title}", ""]
    lines.extend(f"- `{path}`" for path in paths[:limit])
    if len(paths) > limit:
        lines.append(f"- ... {len(paths) - limit} more")
    lines.append("")
    return lines


def inventory(root: Path) -> dict[str, object]:
    files = git_files(root)
    doc_classes = Counter()
    doc_buckets: Counter[str] = Counter()
    excluded_buckets: Counter[str] = Counter()
    in_scope_docs: list[Path] = []
    parity_only: list[Path] = []
    excluded_docs: list[Path] = []

    for path in files:
        if path.suffix.lower() not in DOC_SUFFIXES:
            continue
        cls = classify_doc(path)
        doc_classes[cls] += 1
        if cls == "in-scope-doc":
            in_scope_docs.append(path)
            doc_buckets[doc_bucket(path)] += 1
        elif cls == "manual-pair-parity-only":
            parity_only.append(path)
        else:
            excluded_docs.append(path)
            excluded_buckets[cls] += 1

    kotlin_files = [path for path in files if path.suffix.lower() in KOTLIN_SUFFIXES]
    kotlin_buckets = Counter(kotlin_bucket(path) for path in kotlin_files)
    kdoc_buckets = count_kdoc_blocks(root, kotlin_files)

    en_manual = manual_relative_set(files, MANUAL_EN)
    ko_manual = manual_relative_set(files, MANUAL_KO)
    missing_ko = sorted(en_manual - ko_manual)
    missing_en = sorted(ko_manual - en_manual)
    policy_drift = find_english_kdoc_policy_drift(root, files)

    return {
        "files": files,
        "doc_classes": doc_classes,
        "doc_buckets": doc_buckets,
        "excluded_buckets": excluded_buckets,
        "in_scope_docs": in_scope_docs,
        "parity_only": parity_only,
        "excluded_docs": excluded_docs,
        "kotlin_files": kotlin_files,
        "kotlin_buckets": kotlin_buckets,
        "kdoc_buckets": kdoc_buckets,
        "missing_ko": missing_ko,
        "missing_en": missing_en,
        "policy_drift": policy_drift,
    }


def build_report(root: Path) -> str:
    data = inventory(root)
    files = data["files"]
    doc_classes = data["doc_classes"]
    doc_buckets = data["doc_buckets"]
    excluded_buckets = data["excluded_buckets"]
    in_scope_docs = data["in_scope_docs"]
    parity_only = data["parity_only"]
    excluded_docs = data["excluded_docs"]
    kotlin_files = data["kotlin_files"]
    kotlin_buckets = data["kotlin_buckets"]
    kdoc_buckets = data["kdoc_buckets"]
    missing_ko = data["missing_ko"]
    missing_en = data["missing_en"]
    policy_drift = data["policy_drift"]

    lines = [
        "# Korean Docs And KDoc Localization Inventory",
        "",
        "Issue: #1093",
        "",
        "## Scope Rules",
        "",
        "- In scope: Git-tracked non-README, single-language documentation and Kotlin/KTS KDoc surfaces.",
        "- Korean rewrite target: prose in in-scope docs, public/internal KDoc, and meaningful internal/data-class property contracts.",
        "- Preserve exactly: code identifiers, API names, commands, URLs, exact error text, external product names, issue/PR numbers, and measured values.",
        "- Excluded from rewrite: README files, LLM-facing operating guidance, generated workflow state, CHANGELOG, SECURITY, GitHub metadata, release notes, and pushed commit text.",
        "- Parity-only: `docs/manual/en` and `docs/manual/ko` bilingual manual pairs.",
        "",
        "## Current Inventory",
        "",
        f"- Git-tracked files scanned: {len(files)}",
        f"- In-scope single-language docs: {len(in_scope_docs)}",
        f"- Bilingual manual parity-only docs: {len(parity_only)}",
        f"- Excluded docs: {len(excluded_docs)}",
        f"- Kotlin/KTS files for KDoc follow-up: {len(kotlin_files)}",
        f"- KDoc blocks found in Kotlin/KTS files: {sum(kdoc_buckets.values())}",
        f"- Manual EN files missing KO pair: {len(missing_ko)}",
        f"- Manual KO files missing EN pair: {len(missing_en)}",
        f"- English-KDoc policy drift findings: {len(policy_drift)}",
        "",
    ]
    lines += render_counter("Document Classification", doc_classes)
    lines += render_counter("In-Scope Document Buckets", doc_buckets)
    lines += render_counter("Excluded Document Buckets", excluded_buckets)
    lines += render_counter("Kotlin/KTS Buckets", kotlin_buckets)
    lines += render_counter("Existing KDoc Blocks By Bucket", kdoc_buckets)
    lines += render_sample("In-Scope Document Sample", sorted(in_scope_docs))
    lines += render_sample("Manual Pair Missing KO", missing_ko)
    lines += render_sample("Manual Pair Missing EN", missing_en)
    lines += render_sample(
        "English-KDoc Policy Drift",
        [Path(f"{path}:{line_no}") for path, line_no, _ in policy_drift],
    )
    lines += [
        "## Follow-Up Partition",
        "",
        "- #1094 owns repeatable guardrails based on this inventory.",
        "- #1095-#1100 own documentation buckets and manual parity verification.",
        "- #1101-#1108 own Kotlin KDoc buckets by module group.",
        "- #1109 owns the final repository-wide audit after child PRs land.",
        "",
        "## Reproduction",
        "",
        "```bash",
        "python3 scripts/docs-localization-inventory.py",
        "python3 scripts/docs-localization-inventory.py --check",
        "```",
        "",
    ]
    return "\n".join(lines)


def run_check(root: Path) -> int:
    data = inventory(root)
    missing_ko = data["missing_ko"]
    missing_en = data["missing_en"]
    policy_drift = data["policy_drift"]
    print("Korean localization guardrail")
    print(f"- manual EN missing KO: {len(missing_ko)}")
    print(f"- manual KO missing EN: {len(missing_en)}")
    print(f"- English-KDoc policy drift: {len(policy_drift)}")
    for path, line_no, line in policy_drift[:20]:
        print(f"  - {path}:{line_no}: {line}")
    return 1 if missing_ko or missing_en or policy_drift else 0


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd(),
        help="Repository root. Defaults to the current directory.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail when manual parity or English-KDoc policy drift is detected.",
    )
    args = parser.parse_args()
    if args.check:
        raise SystemExit(run_check(args.root.resolve()))
    print(build_report(args.root.resolve()))


if __name__ == "__main__":
    main()
