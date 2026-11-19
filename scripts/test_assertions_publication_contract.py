"""Regression checks for the published FlowAssertions coroutines dependency."""

from __future__ import annotations

import json
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
PUBLICATION_ROOT = REPOSITORY_ROOT / "testing/assertions/build/publications/Bluetape4k"
POM_PATH = PUBLICATION_ROOT / "pom-default.xml"
MODULE_PATH = PUBLICATION_ROOT / "module.json"


class AssertionsPublicationContractTests(unittest.TestCase):
    def test_coroutines_core_is_compile_dependency_in_pom(self):
        root = ET.parse(POM_PATH).getroot()
        dependencies = root.findall(".//{*}dependency")
        matches = [
            dependency
            for dependency in dependencies
            if dependency.findtext("{*}groupId") == "org.jetbrains.kotlinx"
            and dependency.findtext("{*}artifactId") == "kotlinx-coroutines-core"
            and dependency.findtext("{*}scope") == "compile"
        ]

        self.assertEqual(len(matches), 1)
        self.assertEqual(matches[0].findtext("{*}scope"), "compile")

    def test_coroutines_core_is_in_api_elements_module_metadata(self):
        module = json.loads(MODULE_PATH.read_text(encoding="utf-8"))
        api_elements = next(variant for variant in module["variants"] if variant["name"] == "apiElements")
        self.assertIn(
            {
                "group": "org.jetbrains.kotlinx",
                "module": "kotlinx-coroutines-core",
                "version": {"requires": "1.11.0"},
            },
            api_elements["dependencies"],
        )


if __name__ == "__main__":
    unittest.main()
