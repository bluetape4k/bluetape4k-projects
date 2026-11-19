#!/usr/bin/env python3
"""Publishable CI module inventory contract tests."""

from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / "scripts/ci/validate_ci_module_coverage.py"
MANIFEST = ROOT / "scripts/ci/ci-module-domain-manifest.json"


class CiModuleCoverageContractTest(unittest.TestCase):
    def run_validator(self, root: Path, manifest: Path, ci: Path, nightly: Path):
        return subprocess.run(
            [
                sys.executable,
                str(VALIDATOR),
                "--root",
                str(root),
                "--manifest",
                str(manifest),
                "--ci-workflow",
                str(ci),
                "--nightly-workflow",
                str(nightly),
            ],
            check=False,
            capture_output=True,
            text=True,
        )

    def write_fixture(self, directory: Path, *, manifest: dict, ci: str | None = None, nightly: str = ""):
        root = directory / "repo"
        (root / "io/avro").mkdir(parents=True)
        (root / "io/avro/build.gradle.kts").write_text("plugins {}\n", encoding="utf-8")
        (root / "buildSrc/src/main/kotlin").mkdir(parents=True)
        (root / "settings.gradle.kts").write_text(
            'val projectName = "bluetape4k"\nincludeModules("io", withBaseDir = false)\n',
            encoding="utf-8",
        )
        (root / "buildSrc/src/main/kotlin/ProjectPublication.kt").write_text(
            'private val applicationOnlyProjects = setOf<String>()\n'
            'fun isPublishableLibraryProject(relativeProjectDir: String, projectName: String): Boolean =\n'
            '    projectName != "bluetape4k-bom" &&\n'
            '    normalizedProjectDir != "workshop" &&\n'
            '    !normalizedProjectDir.startsWith("workshop/") &&\n'
            '    normalizedProjectDir != "examples" &&\n'
            '    !normalizedProjectDir.startsWith("examples/")\n',
            encoding="utf-8",
        )
        manifest_path = directory / "manifest.json"
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        ci_path = directory / "ci.yml"
        ci_text = (ci or self.fixture_ci()).replace(
            "    needs: [test-io]\n",
            "    needs: [changes, test-io]\n",
            1,
        )
        ci_text = ci_text.replace(
            '          IO_CHANGED: ${{ needs.changes.outputs.io }}\n',
            '          CHANGES_RESULT: ${{ needs.changes.result }}\n'
            '          IO_CHANGED: ${{ needs.changes.outputs.io }}\n',
            1,
        )
        ci_text = ci_text.replace(
            '          if [[ "$IO_CHANGED" == "true" && "$IO_RESULT" == "skipped" ]]; then exit 1; fi\n',
            '          if [[ "$CHANGES_RESULT" != "success" ]]; then exit 1; fi\n'
            '          if [[ "$IO_CHANGED" == "true" && "$IO_RESULT" == "skipped" ]]; then exit 1; fi\n',
            1,
        )
        ci_path.write_text(ci_text, encoding="utf-8")
        nightly_path = directory / "nightly.yml"
        nightly_path.write_text(nightly, encoding="utf-8")
        return root, manifest_path, ci_path, nightly_path

    @staticmethod
    def fixture_ci() -> str:
        return """jobs:\n  changes:\n    outputs:\n      io: ${{ steps.filter.outputs.io }}\n    steps:\n      - uses: dorny/paths-filter@v4\n        id: filter\n        with:\n          filters: |\n            io:\n              - 'io/**'\n  test-io:\n    needs: [changes, catalog-governance]\n    if: ${{ needs.changes.outputs.io == 'true' }}\n    steps:\n      - run: ./gradlew :bluetape4k-avro:test :bluetape4k-avro:koverXmlReport\n  ci-status:\n    needs: [test-io]\n    steps:\n      - env:\n          IO_CHANGED: ${{ needs.changes.outputs.io }}\n          IO_RESULT: ${{ needs.test-io.result }}\n        run: |\n          if [[ \"$IO_CHANGED\" == \"true\" && \"$IO_RESULT\" == \"skipped\" ]]; then exit 1; fi\n"""

    def test_current_repository_has_a_complete_contract(self):
        result = self.run_validator(ROOT, MANIFEST, ROOT / ".github/workflows/ci.yml", ROOT / ".github/workflows/nightly-tests.yml")
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_issue_paths_are_regular_product_lanes(self):
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.assertEqual(manifest["modules"]["io/avro"]["lane"], "io")
        for path in ("utils/geo", "utils/javatimes", "utils/money"):
            self.assertEqual(manifest["modules"][path]["lane"], "utils")

    def test_new_publishable_module_must_be_added_to_manifest(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
            )
            (root / "io/new-module").mkdir()
            (root / "io/new-module/build.gradle.kts").write_text("plugins {}\n", encoding="utf-8")
            result = self.run_validator(root, manifest, ci, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("missing from manifest", result.stderr)

    def test_publication_path_exclusions_are_not_discovered(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
            )
            for excluded in ("examples/cache", "workshop/cache"):
                path = root / excluded
                path.mkdir(parents=True)
                (path / "build.gradle.kts").write_text("plugins {}\n", encoding="utf-8")
            settings = root / "settings.gradle.kts"
            settings.write_text(
                settings.read_text(encoding="utf-8")
                + 'includeModules("examples", withProjectName = true, withBaseDir = true)\n'
                + 'includeModules("workshop", withProjectName = true, withBaseDir = true)\n',
                encoding="utf-8",
            )
            result = self.run_validator(root, manifest, ci, nightly)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_module_cannot_match_two_ci_path_lanes(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            ci = self.fixture_ci().replace(
                "            io:\n              - 'io/**'\n",
                "            io:\n              - 'io/**'\n            shared-all:\n              - '**'\n",
            )
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
                ci=ci,
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("does not cover module-wide source paths", result.stderr)

    def test_missing_ci_output_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            ci = self.fixture_ci().replace(
                "      io: ${{ steps.filter.outputs.io }}\n",
                "",
                1,
            )
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
                ci=ci,
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("missing from CI changes outputs", result.stderr)

    def test_mismatched_ci_output_reference_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            ci = self.fixture_ci().replace(
                "      io: ${{ steps.filter.outputs.io }}\n",
                "      io: ${{ steps.filter.outputs.other }}\n",
                1,
            )
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
                ci=ci,
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("output io references other", result.stderr)

    def test_nightly_exemption_cannot_still_match_a_regular_filter(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"exemption": "explicit", "reason": "fixture"}}},
            )
            result = self.run_validator(root, manifest, ci, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("exemption still matches regular CI path lane", result.stderr)

    def test_regular_filter_must_cover_module_source_paths(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            ci = self.fixture_ci().replace(
                "              - 'io/**'",
                "              - 'io/avro/build.gradle.kts'",
                1,
            )
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
                ci=ci,
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("does not cover module-wide source paths", result.stderr)

    def test_missing_ci_status_dependency_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            ci = self.fixture_ci().replace("    needs: [test-io]\n", "    needs: []\n", 1)
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
                ci=ci,
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("missing from ci-status needs", result.stderr)

    def test_missing_changes_dependency_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
            )
            ci_path.write_text(
                ci_path.read_text(encoding="utf-8").replace(
                    "    needs: [changes, test-io]\n",
                    "    needs: [test-io]\n",
                    1,
                ),
                encoding="utf-8",
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("ci-status is missing changes from needs", result.stderr)

    def test_changes_skip_guard_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci_path, nightly = self.write_fixture(
                directory,
                manifest={"version": 1, "modules": {"io/avro": {"lane": "io"}}},
            )
            ci_path.write_text(
                ci_path.read_text(encoding="utf-8")
                .replace("  changes:\n", "  changes:\n    if: ${{ false }}\n", 1)
                .replace("          CHANGES_RESULT: ${{ needs.changes.result }}\n", "", 1)
                .replace(
                    '          if [[ "$CHANGES_RESULT" != "success" ]]; then exit 1; fi\n',
                    "",
                    1,
                ),
                encoding="utf-8",
            )
            result = self.run_validator(root, manifest, ci_path, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("ci-status changes result guard is missing", result.stderr)

    def test_exemption_requires_a_reason_and_cannot_also_have_a_lane(self):
        with tempfile.TemporaryDirectory() as directory_name:
            directory = Path(directory_name)
            root, manifest, ci, nightly = self.write_fixture(
                directory,
                manifest={
                    "version": 1,
                    "modules": {"io/avro": {"lane": "io", "exemption": "nightly-only"}},
                },
            )
            result = self.run_validator(root, manifest, ci, nightly)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("exactly one route", result.stderr)


if __name__ == "__main__":
    unittest.main()
