#!/usr/bin/env python3

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MARKER = "issue-1335-java25-semver"
EXPECTED_ISLAND = {
    "bluetape4k-assertions",
    "bluetape4k-junit5",
    "bluetape4k-logging",
    "bluetape4k-virtualthread-api",
    "bluetape4k-virtualthread-jdk21",
}
COMMON_TOKENS = ("2.0.0", "1.13.x", "Java 25", "Java 21")


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def block(text: str, marker: str = MARKER) -> str:
    pattern = rf"<!-- {re.escape(marker)}:start -->(.*?)<!-- {re.escape(marker)}:end -->"
    match = re.search(pattern, text, re.DOTALL)
    if match is None:
        raise AssertionError(f"missing marker block: {marker}")
    return match.group(1)


class JvmReleaseContractTest(unittest.TestCase):
    def test_version_source_is_2_0_0_release_ready(self) -> None:
        properties = read("gradle.properties")
        self.assertRegex(properties, r"(?m)^baseVersion=2\.0\.0$")
        self.assertRegex(properties, r"(?m)^snapshotVersion=$")

    def test_java_21_island_and_default_target_are_explicit(self) -> None:
        build = read("build.gradle.kts")
        match = re.search(
            r"private val java21CompatibilityProjects = setOf\((.*?)\n\)",
            build,
            re.DOTALL,
        )
        self.assertIsNotNone(match)
        modules = set(re.findall(r'"([^"]+)"', match.group(1)))
        self.assertEqual(EXPECTED_ISLAND, modules)
        self.assertIn(
            "val javaCompatibilityVersion = if (project.name in java21CompatibilityProjects) 21 else 25",
            build,
        )
        self.assertIn(
            "val kotlinJvmTarget = if (javaCompatibilityVersion == 21) JvmTarget.JVM_21 else JvmTarget.JVM_25",
            build,
        )
        self.assertIn("options.release.set(javaCompatibilityVersion)", build)

    def test_local_mock_server_image_tags_follow_release_version(self) -> None:
        properties = read("gradle.properties")
        version = re.search(r"(?m)^baseVersion=([^\n]+)$", properties)
        self.assertIsNotNone(version)
        release_version = version.group(1)
        for source_path in (
            "testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/BluetapeHttpServer.kt",
            "testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/BluetapeWebfluxServer.kt",
        ):
            source = read(source_path)
            self.assertRegex(
                source,
                rf'(?m)^\s*const val TAG = "{re.escape(release_version)}"$',
                msg=f"stale mock-server image tag in {source_path}",
            )

    def test_mock_server_jib_keeps_snapshot_and_default_tags_available(self) -> None:
        expected_tag_expression = 'tags = setOf("latest", baseVersionTag, project.version.toString())'
        for source_path in (
            "testing/mock-web-server/build.gradle.kts",
            "testing/mock-webflux-server/build.gradle.kts",
        ):
            source = read(source_path)
            self.assertIn(
                'val baseVersionTag = providers.gradleProperty("baseVersion").get()',
                source,
                msg=f"missing baseVersion image tag source in {source_path}",
            )
            self.assertIn(expected_tag_expression, source, msg=f"snapshot tag drift in {source_path}")

    def test_ci_builds_and_inspects_both_mock_server_images(self) -> None:
        workflow = read(".github/workflows/ci.yml")
        for image in ("mock-web-server", "mock-webflux-server"):
            self.assertIn(f":bluetape4k-{image}:jibDockerBuild", workflow)
            self.assertIn(f"bluetape4k/{image}:2.0.0", workflow)

    def test_testcontainers_docs_follow_release_image_tags(self) -> None:
        for relative in ("testing/testcontainers/README.md", "testing/testcontainers/README.ko.md"):
            source = read(relative)
            self.assertIn("`bluetape4k/mock-web-server` | `2.0.0`", source)
            self.assertIn("`bluetape4k/mock-webflux-server` | `2.0.0`", source)
            self.assertNotIn("`bluetape4k/mock-web-server` | `1.13.0`", source)
            self.assertNotIn("`bluetape4k/mock-webflux-server` | `1.13.0`", source)

    def test_readme_locales_share_the_migration_contract(self) -> None:
        english = block(read("README.md"))
        korean = block(read("README.ko.md"))
        for token in COMMON_TOKENS:
            self.assertIn(token, english)
            self.assertIn(token, korean)
        self.assertIn("baseVersion=2.0.0", read("README.md"))
        self.assertIn("baseVersion=2.0.0", read("README.ko.md"))
        self.assertNotIn("baseVersion=1.11.0", read("README.md"))
        self.assertNotIn("baseVersion=1.11.0", read("README.ko.md"))

    def test_changelog_records_unreleased_compatibility_change(self) -> None:
        changelog = block(read("CHANGELOG.md"))
        self.assertIn("## [Unreleased]", changelog)
        self.assertIn("#1335", changelog)
        for token in COMMON_TOKENS:
            self.assertIn(token, changelog)

    def test_ci_and_release_workflows_run_both_contract_checks(self) -> None:
        for workflow in (read(".github/workflows/ci.yml"), read(".github/workflows/release.yml")):
            self.assertIn("scripts/test_jvm_release_contract.py", workflow)
            self.assertIn("scripts/check-jvm-release-contract.sh", workflow)


if __name__ == "__main__":
    unittest.main()
