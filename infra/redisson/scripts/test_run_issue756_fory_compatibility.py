#!/usr/bin/env python3

import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock


SCRIPT = Path(__file__).with_name("run-issue756-fory-compatibility.py")
ROLLBACK_SCRIPT = Path(__file__).with_name("run-issue756-fory-rollback-smoke.py")


def load_runner():
    spec = importlib.util.spec_from_file_location("issue756_fory_compatibility", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_rollback_runner():
    spec = importlib.util.spec_from_file_location("issue756_fory_rollback", ROLLBACK_SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Issue756ForyCompatibilityRunnerTest(unittest.TestCase):

    def test_pins_known_good_maven_central_artifacts(self):
        runner = load_runner()

        self.assertEqual("https://repo.maven.apache.org/maven2", runner.REPOSITORY_URL)
        self.assertEqual(
            {
                "bluetape4k-io": "e5d41857bb7196c7fac8ecdfa773deb658f649ccbb78608064807fea1a823ea5",
                "bluetape4k-lettuce": "bd38da234b3dcd586d5a5458a95c4996c49585f945146fedb411a8c0810b962a",
                "bluetape4k-redisson": "a8018e61ac2c0d3e592efdcf694d2785c709269f22378d00c8f000dfffc628a1",
            },
            {artifact["artifact"]: artifact["sha256"] for artifact in runner.PINNED_ARTIFACTS},
        )
        self.assertTrue(
            all(artifact["group"] == "io.github.bluetape4k" for artifact in runner.PINNED_ARTIFACTS)
        )
        self.assertTrue(all(artifact["version"] == "1.11.0" for artifact in runner.PINNED_ARTIFACTS))

    def test_checksum_mismatch_stops_before_classpath_construction(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            repository = root / "repository"
            output = root / "output"
            for artifact in ("bluetape4k-io", "bluetape4k-lettuce", "bluetape4k-redisson"):
                jar = (
                    repository
                    / "io/github/bluetape4k"
                    / artifact
                    / "1.11.0"
                    / f"{artifact}-1.11.0.jar"
                )
                jar.parent.mkdir(parents=True, exist_ok=True)
                with zipfile.ZipFile(jar, "w") as archive:
                    archive.writestr("fixture.txt", artifact)

            completed = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--repository-url",
                    repository.as_uri(),
                    "--output",
                    str(output),
                    "--verify-only",
                ],
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertNotEqual(0, completed.returncode)
            self.assertIn("CHECKSUM_MISMATCH", completed.stderr)
            self.assertFalse((output / "classpath-manifest.json").exists())
            self.assertFalse((output / "compatibility-results.json").exists())

    def test_fixture_matrix_covers_both_cross_version_directions(self):
        runner = load_runner()

        self.assertEqual(
            {
                ("fory", "old", "new"),
                ("fory", "new", "old"),
                ("fast-fory", "old", "new"),
                ("fast-fory", "new", "old"),
            },
            {
                (fixture["codec"], fixture["writer"], fixture["reader"])
                for fixture in runner.FIXTURE_MATRIX
            },
        )

    def test_release_manifest_rejects_unverified_artifacts(self):
        runner = load_runner()
        manifest = {
            "repositoryUrl": runner.REPOSITORY_URL,
            "artifacts": [
                {
                    **artifact,
                    "coordinate": (
                        f'{artifact["group"]}:{artifact["artifact"]}:{artifact["version"]}'
                    ),
                    "verified": True,
                }
                for artifact in runner.PINNED_ARTIFACTS
            ],
        }
        manifest["artifacts"][1]["verified"] = False

        with self.assertRaisesRegex(ValueError, "not checksum verified"):
            runner.validate_artifact_manifest(manifest)

    def test_input_state_rejects_dirty_paths_outside_generated_release_output(self):
        runner = load_runner()

        with mock.patch.object(
            runner,
            "run_checked",
            side_effect=["commit", "tree", " M infra/redisson/src/main/kotlin/Codec.kt"],
        ):
            with self.assertRaisesRegex(RuntimeError, "DIRTY_INPUT_TREE"):
                runner.git_input_state()

    def test_input_state_excludes_only_generated_release_output(self):
        runner = load_runner()
        generated = runner.RELEASE_OUTPUT_RELATIVE

        with mock.patch.object(
            runner,
            "run_checked",
            side_effect=[
                "commit",
                "tree",
                f" M {generated}/compatibility-results.json\n"
                f"?? {generated}/fixtures/new.bin",
            ],
        ):
            self.assertEqual(
                {
                    "inputCommit": "commit",
                    "inputTree": "tree",
                    "sourceRelevantClean": True,
                    "excludedGeneratedOutput": generated,
                },
                runner.git_input_state(),
            )

    def test_directory_classpath_record_has_content_hash(self):
        runner = load_runner()

        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "nested").mkdir()
            (root / "nested" / "fixture.class").write_bytes(b"compiled")

            record = runner.classpath_record(root)

            self.assertEqual("directory", record["kind"])
            self.assertEqual(1, record["fileCount"])
            self.assertEqual(8, record["size"])
            self.assertEqual(64, len(record["sha256"]))

    def test_rollback_codec_only_result_blocks_publication(self):
        rollback = load_rollback_runner()

        self.assertEqual(("passed", "passed"), rollback.classify_smoke("redis"))
        self.assertEqual(("limited", "blocked"), rollback.classify_smoke("codec-level"))


if __name__ == "__main__":
    unittest.main()
