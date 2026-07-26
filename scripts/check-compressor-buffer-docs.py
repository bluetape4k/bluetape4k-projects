#!/usr/bin/env python3

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README_PATHS = {
    "en": ROOT / "io/io/README.md",
    "ko": ROOT / "io/io/README.ko.md",
}
CHANGELOG = ROOT / "CHANGELOG.md"
MARKERS = (
    "issue-755-contract",
    "issue-755-storage-matrix",
    "issue-755-kotlin-example",
    "issue-755-java-example",
    "issue-755-sizing-retry",
    "issue-755-resource-bound",
    "issue-755-telemetry",
)


def fail(message: str) -> None:
    raise SystemExit(f"ERROR: {message}")


def extract_marker(text: str, marker: str, path: Path) -> str:
    start = f"<!-- {marker}:start -->"
    end = f"<!-- {marker}:end -->"
    if text.count(start) != 1 or text.count(end) != 1:
        fail(f"{path}: {marker} markers must occur exactly once")
    start_index = text.index(start) + len(start)
    end_index = text.index(end)
    if start_index >= end_index:
        fail(f"{path}: invalid marker order for {marker}")
    return text[start_index:end_index].strip()


def require_tokens(section: str, tokens: tuple[str, ...], label: str) -> None:
    normalized = " ".join(section.split())
    missing = [token for token in tokens if token not in normalized]
    if missing:
        fail(f"{label}: missing required text: {', '.join(missing)}")


def extract_code_block(section: str, language: str, label: str) -> str:
    matches = re.findall(rf"```{re.escape(language)}\n(.*?)\n```", section, flags=re.DOTALL)
    if len(matches) != 1:
        fail(f"{label}: expected exactly one {language} code block")
    return matches[0].strip()


def parse_matrix(section: str, label: str) -> dict[str, tuple[str, ...]]:
    rows: dict[str, tuple[str, ...]] = {}
    for line in section.splitlines():
        if not line.startswith("|"):
            continue
        cells = tuple(cell.strip() for cell in line.strip().strip("|").split("|"))
        if not cells or cells[0] == "Codec" or set(cells[0]) <= {"-", ":"}:
            continue
        if len(cells) != 5:
            fail(f"{label}: expected five matrix columns: {line}")
        if cells[0] in rows:
            fail(f"{label}: duplicate codec row {cells[0]}")
        rows[cells[0]] = cells[1:]
    expected = {"LZ4", "Deflate", "Snappy", "Zstd", "Other codecs"}
    if set(rows) != expected:
        fail(f"{label}: matrix keys differ: expected={sorted(expected)} actual={sorted(rows)}")
    return rows


def validate_readmes() -> None:
    texts = {locale: path.read_text(encoding="utf-8") for locale, path in README_PATHS.items()}
    sections: dict[str, dict[str, str]] = {}
    for locale, text in texts.items():
        positions = [text.index(f"<!-- {marker}:start -->") for marker in MARKERS]
        if positions != sorted(positions):
            fail(f"{README_PATHS[locale]}: issue-755 marker order drift")
        sections[locale] = {
            marker: extract_marker(text, marker, README_PATHS[locale]) for marker in MARKERS
        }

    en_matrix = parse_matrix(sections["en"]["issue-755-storage-matrix"], "README.md matrix")
    ko_matrix = parse_matrix(sections["ko"]["issue-755-storage-matrix"], "README.ko.md matrix")
    if en_matrix != ko_matrix:
        fail("README storage matrix row/status parity drift")
    for marker, language in (
            ("issue-755-kotlin-example", "kotlin"),
            ("issue-755-java-example", "java"),
    ):
        en_code = extract_code_block(sections["en"][marker], language, f"English {marker}")
        ko_code = extract_code_block(sections["ko"][marker], language, f"Korean {marker}")
        if en_code != ko_code:
            fail(f"README {language} example locale parity drift")
    expected_matrix = {
        "LZ4": ("compatibility fallback", "compatibility fallback", "compatibility fallback", "none in the core slice"),
        "Deflate": ("compatibility fallback", "compatibility fallback", "compatibility fallback",
                    "none in the core slice"),
        "Snappy": ("compatibility fallback", "compatibility fallback", "compatibility fallback",
                   "none in the core slice"),
        "Zstd": ("compatibility fallback", "compatibility fallback", "compatibility fallback",
                 "none in the core slice"),
        "Other codecs": ("compatibility fallback", "compatibility fallback", "compatibility fallback", "ineligible"),
    }
    if en_matrix != expected_matrix:
        fail(f"core storage matrix drift: expected={expected_matrix} actual={en_matrix}")

    shared_contract = (
        "position",
        "limit",
        "mark",
        "byte order",
        "ReadOnlyBufferException",
        "IllegalArgumentException",
        "direct",
        "read-only",
        "thread",
        "one-argument",
        "two-argument",
        "erased signature",
        "fallback",
    )
    for locale in README_PATHS:
        require_tokens(
            sections[locale]["issue-755-contract"],
            shared_contract,
            f"{locale} contract",
        )
        require_tokens(
            sections[locale]["issue-755-kotlin-example"],
            (
                "Compressors.LZ4.compress(source, target)",
                "ByteBuffer.allocate(64 * 1024).apply { position(16) }",
                "val start = target.position()",
                "position(start)",
                "limit(start + written)",
                "}.slice()",
            ),
            f"{locale} Kotlin example",
        )
        require_tokens(
            sections[locale]["issue-755-java-example"],
            (
                "compressor.compress(source, target)",
                "Compressors.INSTANCE.getLZ4()",
                "target.position(16)",
                "int start = target.position()",
                "compressed.position(start).limit(start + written)",
                "compressed = compressed.slice()",
            ),
            f"{locale} Java example",
        )
        examples = sections[locale]["issue-755-kotlin-example"] + sections[locale]["issue-755-java-example"]
        for forbidden in ("ByteArray(written)", ".flip()"):
            if forbidden in examples:
                fail(f"{locale} examples: forbidden allocating or unbounded pattern: {forbidden}")
        require_tokens(
            sections[locale]["issue-755-telemetry"],
            ("runtime dispatch telemetry", "privacy-safe", "override", "allocating API", "fallback storage"),
            f"{locale} telemetry",
        )

    require_tokens(
        sections["en"]["issue-755-contract"],
        ("explicit override", "reusable target"),
        "English migration boundary",
    )
    require_tokens(
        sections["ko"]["issue-755-contract"],
        ("명시적 override", "재사용 가능한 target"),
        "Korean migration boundary",
    )

    require_tokens(
        sections["en"]["issue-755-sizing-retry"],
        ("BufferOverflowException", "required size", "retry"),
        "English sizing/retry",
    )
    require_tokens(
        sections["ko"]["issue-755-sizing-retry"],
        ("BufferOverflowException", "required size", "재시도"),
        "Korean sizing/retry",
    )
    require_tokens(
        sections["en"]["issue-755-resource-bound"],
        ("payload-sized", "final-write bound", "resource bound", "untrusted"),
        "English resource bound",
    )
    require_tokens(
        sections["ko"]["issue-755-resource-bound"],
        ("payload-sized", "final-write bound", "resource bound", "신뢰할 수 없는"),
        "Korean resource bound",
    )


def validate_changelog() -> None:
    text = CHANGELOG.read_text(encoding="utf-8")
    migration = extract_marker(text, "issue-755-migration", CHANGELOG)
    rollback = extract_marker(text, "issue-755-rollback", CHANGELOG)
    if text.index("<!-- issue-755-migration:start -->") > text.index("<!-- issue-755-rollback:start -->"):
        fail("CHANGELOG issue-755 marker order drift")
    require_tokens(
        migration,
        (
            "one-argument `ByteBuffer`",
            "consume the source position",
            "two-argument methods preserve all source state",
            "erased-signature-equivalent",
            "explicit override",
            "fallback pairings are correctness-only",
        ),
        "CHANGELOG migration",
    )
    require_tokens(
        rollback,
        (
            "reverts only that override",
            "compatibility fallback",
            "allocating API",
            "fallback storage pairing",
            "no runtime feature flag",
        ),
        "CHANGELOG rollback",
    )


def main() -> None:
    validate_readmes()
    validate_changelog()
    print("COMPRESSOR BUFFER DOCS PASS")


if __name__ == "__main__":
    main()
