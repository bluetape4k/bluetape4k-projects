#!/usr/bin/env python3
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class Ignite2CompatibilityBoundaryTest(unittest.TestCase):
    def test_deprecated_facade_and_compatibility_test_remain_until_3_0(self) -> None:
        storage = ROOT / "testing/testcontainers/src"
        facade = storage / "main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt"
        compatibility_test = storage / "test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerApiCompatibilityTest.kt"
        self.assertTrue(facade.is_file())
        self.assertTrue(compatibility_test.is_file())
        self.assertFalse((storage / "test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt").exists())
        source = facade.read_text(encoding="utf-8")
        self.assertIn("@Deprecated", source)
        self.assertIn("3.0.0", source)

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

    def test_current_docs_record_2_1_facade_and_3_0_removal_boundary(self) -> None:
        for relative in ("testing/testcontainers/README.md", "testing/testcontainers/README.ko.md"):
            current = (ROOT / relative).read_text(encoding="utf-8")
            self.assertIn("Ignite2Server", current, relative)
            self.assertIn("3.0.0", current, relative)
        self.assertNotIn(
            "Ignite2Server",
            (ROOT / "cache/cache-core/README.ko.md").read_text(encoding="utf-8"),
        )
        for relative in ("CHANGELOG.md", "WIP.md"):
            current = (ROOT / relative).read_text(encoding="utf-8")
            self.assertIn("Ignite 2", current, relative)
            self.assertIn("2.1.0", current, relative)
            self.assertIn("Ignite2Server", current, relative)
            self.assertIn("3.0.0", current, relative)
        history = ROOT / "docs/release/2.0.0-ignite2-migration.md"
        self.assertTrue(history.is_file())
        self.assertIn("Ignite2Server", history.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
