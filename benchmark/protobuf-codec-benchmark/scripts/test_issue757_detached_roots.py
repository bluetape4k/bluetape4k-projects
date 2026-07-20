import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("issue757_detached_roots.py")
SPEC = importlib.util.spec_from_file_location("issue757_detached_roots", SCRIPT)
helper = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helper)


class DetachedRootsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.repository = self.root / "repository"
        self.repository.mkdir()
        subprocess.run(["git", "init", "-q", str(self.repository)], check=True)
        subprocess.run(
            ["git", "-C", str(self.repository), "config", "user.email", "test@example.com"],
            check=True,
        )
        subprocess.run(
            ["git", "-C", str(self.repository), "config", "user.name", "Test"],
            check=True,
        )
        (self.repository / "tracked.txt").write_text("baseline\n", encoding="utf-8")
        subprocess.run(["git", "-C", str(self.repository), "add", "tracked.txt"], check=True)
        subprocess.run(["git", "-C", str(self.repository), "commit", "-qm", "baseline"], check=True)
        self.revision = subprocess.check_output(
            ["git", "-C", str(self.repository), "rev-parse", "HEAD"], text=True
        ).strip()
        self.tree = subprocess.check_output(
            ["git", "-C", str(self.repository), "rev-parse", "HEAD^{tree}"], text=True
        ).strip()
        self.evidence = self.root / "evidence"
        self.evidence.mkdir()
        self.selection = self.evidence / "selection.json"

    def begin(self):
        return helper.begin_attempt(
            repository_root=self.repository,
            evidence_root=self.evidence,
            role="candidate",
            revision=self.revision,
            tree=self.tree,
            selection_state=self.selection,
            attempt_id="attempt-001",
        )

    def test_public_api_is_versioned_and_exact(self):
        self.assertEqual(1, helper.PUBLIC_API_VERSION)
        self.assertEqual(
            {
                "begin_attempt",
                "materialize_worktree",
                "materialize_build_root",
                "seal_artifact",
                "cleanup_selected",
            },
            set(helper.PUBLIC_API),
        )

    def test_attempt_materializes_owned_roots_and_preserves_sealed_artifact(self):
        intent = self.begin()
        self.assertEqual(-1, intent["phase"])
        self.assertTrue(self.selection.is_file())

        worktree = helper.materialize_worktree(selection_state=self.selection)
        build_root = helper.materialize_build_root(selection_state=self.selection)
        artifact = helper.seal_artifact(
            selection_state=self.selection,
            relative_path="abi/candidate.struct.txt",
            data=b"immutable\n",
        )

        self.assertEqual(self.revision, subprocess.check_output(
            ["git", "-C", str(worktree), "rev-parse", "HEAD"], text=True
        ).strip())
        self.assertTrue((build_root / helper.OWNER_MARKER).is_file())
        self.assertEqual(b"immutable\n", artifact.read_bytes())
        with self.assertRaises(FileExistsError):
            helper.seal_artifact(
                selection_state=self.selection,
                relative_path="abi/candidate.struct.txt",
                data=b"replacement\n",
            )

        receipt = helper.cleanup_selected(selection_state=self.selection)
        self.assertEqual("cleaned", receipt["status"])
        self.assertFalse(worktree.exists())
        self.assertFalse(build_root.exists())
        self.assertEqual(b"immutable\n", artifact.read_bytes())
        self.assertEqual(
            "cleaned",
            helper.cleanup_selected(selection_state=self.selection)["status"],
        )

    def test_cleanup_rejects_marker_substitution_without_deleting_foreign_root(self):
        self.begin()
        build_root = helper.materialize_build_root(selection_state=self.selection)
        marker = build_root / helper.OWNER_MARKER
        marker.unlink()
        marker.write_text(json.dumps({"foreign": True}), encoding="utf-8")

        with self.assertRaises(helper.OwnershipError):
            helper.cleanup_selected(selection_state=self.selection)

        self.assertTrue(build_root.is_dir())
        self.assertTrue(marker.is_file())

    def test_attempt_and_artifact_paths_reject_escape_and_symlink(self):
        self.begin()
        outside = self.root / "outside"
        outside.mkdir()
        (self.evidence / "attempts" / "attempt-001" / "artifacts" / "link").symlink_to(
            outside,
            target_is_directory=True,
        )

        with self.assertRaises((ValueError, helper.OwnershipError)):
            helper.seal_artifact(
                selection_state=self.selection,
                relative_path="../escape.txt",
                data=b"bad",
            )
        with self.assertRaises((ValueError, helper.OwnershipError)):
            helper.seal_artifact(
                selection_state=self.selection,
                relative_path="link/escape.txt",
                data=b"bad",
            )
        self.assertEqual([], list(outside.iterdir()))

    def test_begin_is_no_clobber_for_selection_and_foreign_final_parent(self):
        self.begin()
        with self.assertRaises(FileExistsError):
            self.begin()

        other_selection = self.evidence / "other-selection.json"
        foreign = self.evidence / "attempts" / "attempt-002"
        foreign.mkdir()
        (foreign / "foreign.txt").write_text("keep\n", encoding="utf-8")
        with self.assertRaises(FileExistsError):
            helper.begin_attempt(
                repository_root=self.repository,
                evidence_root=self.evidence,
                role="baseline",
                revision=self.revision,
                tree=self.tree,
                selection_state=other_selection,
                attempt_id="attempt-002",
            )
        self.assertEqual("keep\n", (foreign / "foreign.txt").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
