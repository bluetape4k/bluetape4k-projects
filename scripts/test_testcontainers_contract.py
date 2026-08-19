#!/usr/bin/env python3
"""Validate the Testcontainers image-tag and documentation contract."""

from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KOTLIN_ROOT = ROOT / "testing/testcontainers/src/main/kotlin"
README_EN = ROOT / "testing/testcontainers/README.md"
README_KO = ROOT / "testing/testcontainers/README.ko.md"
MINISTACK_SOURCE = KOTLIN_ROOT / "io/bluetape4k/testcontainers/aws/MiniStackServer.kt"
HTTP_READMES = (ROOT / "io/http/README.md", ROOT / "io/http/README.ko.md")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def kotlin_servers() -> dict[str, dict[str, str | Path]]:
    servers: dict[str, dict[str, str | Path]] = {}
    image_pattern = re.compile(r"\bconst\s+val\s+IMAGE\s*(?::\s*String)?\s*=\s*\"([^\"]+)\"")
    tag_pattern = re.compile(r"\bconst\s+val\s+TAG\s*(?::\s*String)?\s*=\s*\"([^\"]+)\"")
    class_pattern = re.compile(r"\bclass\s+([A-Za-z_]\w*Server)\b")

    for path in sorted(KOTLIN_ROOT.rglob("*Server.kt")):
        source = read(path)
        class_match = class_pattern.search(source)
        image_match = image_pattern.search(source)
        tag_match = tag_pattern.search(source)
        if not class_match:
            continue
        if bool(image_match) != bool(tag_match):
            raise AssertionError(f"IMAGE/TAG must be declared together: {path}")
        if not image_match:
            continue

        name = class_match.group(1)
        if name in servers:
            raise AssertionError(f"duplicate server declaration: {name}")
        servers[name] = {
            "image": image_match.group(1),
            "tag": tag_match.group(1),
            "path": path,
            "source": source,
        }
    return servers


def readme_tag_table(path: Path) -> dict[str, tuple[str, str]]:
    lines = read(path).splitlines()
    heading = "## Default Docker Image Tags" if path == README_EN else "## 기본 Docker 이미지 태그"
    try:
        start = next(index for index, line in enumerate(lines) if line.strip() == heading)
    except StopIteration as error:
        raise AssertionError(f"missing tag table heading in {path}") from error

    row_pattern = re.compile(
        r"^\|\s*[^|]+\|\s*`(?P<server>[^`]+)`\s*\|\s*`(?P<image>[^`]+)`\s*\|\s*`(?P<tag>[^`]+)`"
    )
    rows: dict[str, tuple[str, str]] = {}
    for line in lines[start + 1 :]:
        if line.startswith("## "):
            break
        match = row_pattern.match(line)
        if not match:
            continue
        server = match.group("server")
        if server in rows:
            raise AssertionError(f"duplicate README row for {server}: {path}")
        rows[server] = (match.group("image"), match.group("tag"))
    if not rows:
        raise AssertionError(f"empty tag table in {path}")
    return rows


class TestTestcontainersContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.sources = kotlin_servers()
        cls.en_rows = readme_tag_table(README_EN)
        cls.ko_rows = readme_tag_table(README_KO)

    def test_readme_tables_match_each_other_and_kotlin_constants(self) -> None:
        self.assertEqual(self.en_rows, self.ko_rows, "EN/KO default-tag tables drifted")
        expected = {
            name: (str(details["image"]), str(details["tag"]))
            for name, details in self.sources.items()
        }
        self.assertEqual(expected, self.en_rows, "README defaults drifted from Kotlin IMAGE/TAG")

    def test_kdoc_defaults_do_not_pin_stale_tags(self) -> None:
        default_pattern = re.compile(
            r"@param\s+tag[^\n]*\(기본:\s*(?:`([^`]+)`|\[([^\]]+)\])\)"
        )
        literal_patterns = (
            re.compile(r'withTag\("([^"]+)"\)'),
            re.compile(r'\btag\s*=\s*"([^"]+)"'),
        )
        stale: list[str] = []
        for name, details in self.sources.items():
            source = str(details["source"])
            expected_tag = str(details["tag"])
            for match in default_pattern.finditer(source):
                value = match.group(1) or match.group(2)
                if value not in {"TAG", expected_tag}:
                    stale.append(f"{name}: @param tag default {value!r}")
            for pattern in literal_patterns:
                for match in pattern.finditer(source):
                    value = match.group(1)
                    if value != expected_tag:
                        stale.append(f"{name}: literal tag {value!r}")
        self.assertEqual([], stale, "stale KDoc tag literals: " + "; ".join(stale))

    def test_jdk_baseline_is_25_in_both_http_readmes(self) -> None:
        for path in HTTP_READMES:
            content = read(path)
            self.assertIn("JDK 25", content, path.as_posix())
            self.assertNotIn("JDK 21", content, path.as_posix())

    def test_ministack_grant_limitation_is_explicit(self) -> None:
        source = read(MINISTACK_SOURCE)
        for action in ("CreateGrant", "ListGrants", "RevokeGrant"):
            self.assertIn(action, source)
        self.assertIn("미지원", source)
        self.assertNotIn("KMS 전 기능 지원", source)

        for path in (README_EN, README_KO):
            content = read(path)
            for action in ("CreateGrant", "ListGrants", "RevokeGrant"):
                self.assertIn(action, content, path.as_posix())
            if path == README_EN:
                self.assertIn("not supported", content)
            else:
                self.assertIn("지원하지 않", content)


if __name__ == "__main__":
    result = unittest.main(verbosity=2, exit=False)
    if result.result.wasSuccessful():
        print(f"PASS: {len(kotlin_servers())} Kotlin server TAG contracts match EN/KO README tables")
    raise SystemExit(not result.result.wasSuccessful())
