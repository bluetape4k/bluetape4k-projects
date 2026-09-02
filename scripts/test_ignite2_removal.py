#!/usr/bin/env python3
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Ignite2RemovalTest(unittest.TestCase):
    def test_runtime_source_and_test_are_removed(self) -> None:
        storage = ROOT / "testing/testcontainers/src"
        self.assertFalse((storage / "main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt").exists())
        self.assertFalse((storage / "test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt").exists())

    def test_image_manifest_and_workflows_have_no_ignite2_gate(self) -> None:
        manifest = json.loads((ROOT / "scripts/testcontainers_image_gate_manifest.json").read_text(encoding="utf-8"))
        self.assertNotIn("ignite2", {family["id"] for family in manifest["families"]})
        for relative in (".github/workflows/nightly-tests.yml", ".github/workflows/release.yml"):
            workflow = (ROOT / relative).read_text(encoding="utf-8")
            self.assertNotIn("ignite2", workflow.lower(), relative)

    def test_build_contract_has_no_ignite2_runtime_dependency_or_jvm_options(self) -> None:
        root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        module_build = (ROOT / "testing/testcontainers/build.gradle.kts").read_text(encoding="utf-8")
        catalog = (ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8")
        self.assertNotIn("org.apache.ignite:ignite-", root_build)
        self.assertNotIn("bt4k.ignite.core", module_build)
        self.assertNotIn("--add-opens=java.base/java.nio", module_build)
        self.assertNotIn("--add-opens=java.base/java.util", module_build)
        for alias in (
            "ignite-clients",
            "ignite-aop",
            "ignite-aws",
            "ignite-compress",
            "ignite-indexing",
            "ignite-slf4j",
            "ignite-spring",
            "ignite-tools",
            "ignite-zookeeper",
        ):
            self.assertNotIn(f"{alias} =", catalog)

    def test_current_docs_record_removal_and_keep_2_0_history(self) -> None:
        for relative in (
            "testing/testcontainers/README.md",
            "testing/testcontainers/README.ko.md",
            "cache/cache-core/README.ko.md",
        ):
            self.assertNotIn("Ignite2Server", (ROOT / relative).read_text(encoding="utf-8"), relative)
        for relative in ("CHANGELOG.md", "WIP.md"):
            current = (ROOT / relative).read_text(encoding="utf-8")
            self.assertIn("Ignite 2", current, relative)
            self.assertIn("3.0.0", current, relative)
        history = ROOT / "docs/release/2.0.0-ignite2-migration.md"
        self.assertTrue(history.is_file())
        self.assertIn("Ignite2Server", history.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
