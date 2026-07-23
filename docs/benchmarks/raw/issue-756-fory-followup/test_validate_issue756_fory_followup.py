import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("issue756_aggregate", HERE / "validate-issue756-fory-followup.py")
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class AggregateValidatorTest(unittest.TestCase):
    def fixture(self, root, encode="rejected"):
        modules = {}
        for module, count in (("lettuce", 8), ("redisson", 12 if encode == "rejected" else 16)):
            runs = {}
            for run_name in ("canonical-a", "canonical-b"):
                leaf = root / module / run_name
                leaf.mkdir(parents=True)
                for name, content in (
                    ("jmh.json", "[]"),
                    ("argv.json", "[]"),
                    ("environment.json", "{}"),
                    ("preflight.json", "{}"),
                    ("summary.csv", "header\n"),
                    ("comparison.json", "[]"),
                ):
                    (leaf / name).write_text(content)
                comparisons = []
                if module == "redisson":
                    comparisons = [
                        {"source": "composite", "promotable": False, "disposition": "fallback"}
                    ]
                validation = {
                    "status": "passed",
                    "method_count": count,
                    "comparisons": comparisons,
                }
                if module == "redisson":
                    validation["encodeDisposition"] = encode
                (leaf / "metadata.json").write_text(
                    json.dumps({"jar_sha256": module[0] * 64, "run": run_name})
                )
                validation["hashes"] = {
                    name: validator.sha256_file(leaf / name)
                    for name in (
                        "jmh.json",
                        "argv.json",
                        "environment.json",
                        "metadata.json",
                        "preflight.json",
                        "summary.csv",
                        "comparison.json",
                    )
                }
                (leaf / "validation.json").write_text(
                    json.dumps({**validation, "run_nonce": run_name})
                )
                runs[run_name] = f"{module}/{run_name}"
            modules[module] = {"jar_sha256": module[0] * 64, "runs": runs}
        return {
            "schema_version": 1,
            "encodeDisposition": encode,
            "canonical_method_count": 20 if encode == "rejected" else 24,
            "source": {
                "clean": True,
                "ancestry_verified": True,
                "append_only_raw_verified": True,
                "changed_paths": ["infra/lettuce/example"],
            },
            "modules": modules,
        }

    def test_accepts_rejected_and_implemented_cardinality(self):
        for disposition in ("rejected", "implemented"):
            with self.subTest(disposition=disposition), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                result = validator.validate_manifest(self.fixture(root, disposition), root)
                self.assertEqual(20 if disposition == "rejected" else 24, result["canonical_method_count"])

    def test_rejects_count_missing_run_hash_drift_and_non_promotable_fallback(self):
        mutations = [
            lambda value: value.update(canonical_method_count=24),
            lambda value: value["modules"]["lettuce"]["runs"].pop("canonical-b"),
            lambda value: value["modules"]["redisson"].update(jar_sha256="f" * 64),
        ]
        for mutate in mutations:
            with tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                manifest = self.fixture(root)
                mutate(manifest)
                with self.assertRaises(validator.ValidationError):
                    validator.validate_manifest(manifest, root)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            manifest = self.fixture(root)
            path = root / "redisson/canonical-a/validation.json"
            value = json.loads(path.read_text())
            value["comparisons"][0]["promotable"] = True
            path.write_text(json.dumps(value))
            with self.assertRaises(validator.ValidationError):
                validator.validate_manifest(manifest, root)

    def test_rejects_dirty_ancestry_changed_path_and_sensitive_metadata(self):
        for mutation in (
            lambda value: value["source"].update(clean=False),
            lambda value: value["source"].update(ancestry_verified=False),
            lambda value: value["source"].update(append_only_raw_verified=False),
            lambda value: value["source"].update(changed_paths=["unrelated/file"]),
            lambda value: value.update(secret="not-allowed"),
        ):
            with tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                manifest = self.fixture(root)
                mutation(manifest)
                with self.assertRaises(validator.ValidationError):
                    validator.validate_manifest(manifest, root)


if __name__ == "__main__":
    unittest.main()
