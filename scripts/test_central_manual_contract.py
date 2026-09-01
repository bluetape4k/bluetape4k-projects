from __future__ import annotations

import subprocess
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
INVENTORY = REPO_ROOT / "scripts" / "docs-localization-inventory.py"
CI_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "ci.yml"
NIGHTLY_WORKFLOW = REPO_ROOT / ".github" / "workflows" / "nightly-tests.yml"
DOCUMENTATION_TEST = (
    REPO_ROOT
    / "cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache"
    / "NearJCacheDocumentationTest.kt"
)


class CentralManualInventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tempdir = tempfile.TemporaryDirectory()
        self.addCleanup(self.tempdir.cleanup)
        root = Path(self.tempdir.name)
        self.project = root / "project"
        self.manual = root / "manual"
        self.project.mkdir()
        (self.manual / "en").mkdir(parents=True)
        (self.manual / "ko").mkdir(parents=True)
        (self.project / "README.md").write_text("fixture\n", encoding="utf-8")
        subprocess.run(["git", "init", "-b", "develop"], cwd=self.project, check=True, capture_output=True)
        subprocess.run(["git", "add", "README.md"], cwd=self.project, check=True, capture_output=True)

    def write_manual(self, locale: str, relative_path: str) -> None:
        target = self.manual / locale / relative_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(f"# {locale}\n", encoding="utf-8")

    def run_inventory(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                "python3",
                str(INVENTORY),
                "--root",
                str(self.project),
                "--manual-root",
                str(self.manual),
                "--manual-ref",
                "a" * 40,
                "--check",
            ],
            check=False,
            text=True,
            capture_output=True,
        )

    def test_external_manual_pair_passes_and_reports_source_identity(self) -> None:
        self.write_manual("en", "modules/cache.md")
        self.write_manual("ko", "modules/cache.md")

        result = self.run_inventory()

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(f"central manual root: {self.manual.resolve()}", result.stdout)
        self.assertIn(f"central manual ref: {'a' * 40}", result.stdout)

    def test_missing_korean_pair_fails(self) -> None:
        self.write_manual("en", "modules/cache.md")

        result = self.run_inventory()

        self.assertNotEqual(0, result.returncode)
        self.assertIn("manual EN missing KO: 1", result.stdout)

    def test_missing_english_pair_fails(self) -> None:
        self.write_manual("ko", "modules/cache.md")

        result = self.run_inventory()

        self.assertNotEqual(0, result.returncode)
        self.assertIn("manual KO missing EN: 1", result.stdout)

    def test_missing_manual_root_fails_closed(self) -> None:
        missing = self.manual / "missing"
        result = subprocess.run(
            [
                "python3",
                str(INVENTORY),
                "--root",
                str(self.project),
                "--manual-root",
                str(missing),
                "--manual-ref",
                "a" * 40,
                "--check",
            ],
            check=False,
            text=True,
            capture_output=True,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("central manual root is not available", result.stderr)


class CentralManualCiPolicyTest(unittest.TestCase):
    def test_documentation_test_fails_when_central_manual_is_absent(self) -> None:
        source = DOCUMENTATION_TEST.read_text(encoding="utf-8")

        self.assertNotIn("assumeTrue", source)
        self.assertNotIn("repositoryRoot.resolve(\"docs/manual\")", source)
        self.assertIn("BLUETAPE4K_MANUAL_ROOT", source)

    def test_ci_checks_out_and_records_pinned_central_manual(self) -> None:
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")

        self.assertRegex(workflow, r"BLUETAPE4K_MANUAL_REF: '[0-9a-f]{40}'")
        self.assertIn("repository: bluetape4k/bluetape4k.github.io", workflow)
        self.assertIn("ref: ${{ env.BLUETAPE4K_MANUAL_REF }}", workflow)
        self.assertIn("BLUETAPE4K_MANUAL_ROOT", workflow)
        self.assertIn("docs/manual/bluetape4k-projects/manifest.yaml", workflow)
        self.assertIn("tests=6, skipped=0, failures=0, errors=0", workflow)
        self.assertIn(
            "python3 -m unittest scripts/test_central_manual_contract.py -v",
            workflow,
        )

    def test_full_nightly_misc_job_provides_pinned_central_manual(self) -> None:
        workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
        misc_job = workflow.split("\n  test-misc:\n", maxsplit=1)[1].split(
            "\n  test-infra:\n", maxsplit=1
        )[0]

        self.assertRegex(workflow, r"BLUETAPE4K_MANUAL_REF: '[0-9a-f]{40}'")
        self.assertIn("repository: bluetape4k/bluetape4k.github.io", misc_job)
        self.assertIn("ref: ${{ env.BLUETAPE4K_MANUAL_REF }}", misc_job)
        self.assertIn("BLUETAPE4K_MANUAL_ROOT", misc_job)
        self.assertIn("docs/manual/bluetape4k-projects/manifest.yaml", misc_job)


if __name__ == "__main__":
    unittest.main()
