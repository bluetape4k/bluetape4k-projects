import hashlib
import importlib.util
import json
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path
from unittest import mock


HERE = Path(__file__).resolve().parent


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runner = load_module("run_issue756_evidence", "run-issue756-evidence.py")


class RunnerContractTest(unittest.TestCase):
    def assert_reason(self, reason_code, callback):
        with self.assertRaises(runner.RunnerError) as caught:
            callback()
        self.assertEqual(reason_code, caught.exception.reason_code, str(caught.exception))

    def test_include_regex_names_exact_16_methods(self):
        pattern = runner.exact_include_regex()
        self.assertTrue(pattern.startswith("^" + runner.BENCHMARK_CLASS.replace(".", "\\.")))
        self.assertEqual(16, len(runner.EXPECTED_METHODS))
        for method in runner.EXPECTED_METHODS:
            self.assertRegex(runner.BENCHMARK_CLASS + "." + method, pattern)
        self.assertNotRegex(runner.BENCHMARK_CLASS + ".growthDiagnostic", pattern)

    def test_fixed_jmh_argv_contains_exact_protocol_and_result_path(self):
        result_path = Path("canonical-a/jmh.json")
        argv = runner.fixed_jmh_argv(Path("benchmark.jar"), result_path)
        expected_pairs = {
            "-f": "2",
            "-wi": "3",
            "-i": "5",
            "-t": "1",
            "-prof": "gc",
            "-rf": "json",
            "-rff": str(result_path),
        }
        for option, expected in expected_pairs.items():
            index = argv.index(option)
            self.assertEqual(expected, argv[index + 1])
        self.assertEqual(runner.exact_include_regex(), argv[-1])

    def test_plan_cli_accepts_output_and_exact_run_pair(self):
        arguments = runner._parser().parse_args(
            [
                "--expected-head",
                "a" * 40,
                "--output",
                "docs/benchmarks/raw/issue-756",
                "--runs",
                "canonical-a",
                "canonical-b",
            ]
        )
        self.assertEqual(Path("docs/benchmarks/raw/issue-756"), arguments.output_root)
        self.assertEqual(["canonical-a", "canonical-b"], arguments.runs)

    def test_clean_status_is_required_before_build(self):
        with mock.patch.object(runner, "git_output", return_value="?? dirty.txt"):
            self.assert_reason(
                "BUILD_INPUT_DIRTY", lambda: runner.require_clean_repository(Path("repo"))
            )

    def test_expected_head_and_tree_are_bound(self):
        outputs = iter(("a" * 40, "b" * 40))
        with mock.patch.object(runner, "git_output", side_effect=lambda *_: next(outputs)):
            state = runner.require_expected_head(Path("repo"), "a" * 40)
        self.assertEqual("a" * 40, state["benchmark_input_sha"])
        self.assertEqual("b" * 40, state["benchmark_input_tree"])

        with mock.patch.object(runner, "git_output", return_value="c" * 40):
            self.assert_reason(
                "SOURCE_IDENTITY_MISMATCH",
                lambda: runner.require_expected_head(Path("repo"), "a" * 40),
            )

    def test_classpath_rejects_directory_missing_duplicate_and_multiple_project_jars(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            benchmark = root / "benchmark.jar"
            jackson = root / "jackson2.jar"
            dependency = root / "dependency.jar"
            for path in (benchmark, jackson, dependency):
                path.write_bytes(path.name.encode())

            valid = runner.pin_classpath(benchmark, [jackson, dependency], jackson)
            self.assertEqual(3, len(valid))
            self.assertEqual("jackson2-project", valid[1]["kind"])

            stale_jackson = root / "stale-jackson2.jar"
            stale_jackson.write_bytes(b"stale")
            self.assert_reason(
                "CLASSPATH_INVALID",
                lambda: runner.pin_classpath(
                    benchmark, [jackson, stale_jackson], jackson
                ),
            )

            for entries, expected_project in (
                ([root], jackson),
                ([root / "missing.jar"], jackson),
                ([jackson, jackson], jackson),
                ([jackson, dependency], dependency),
            ):
                with self.subTest(entries=entries):
                    self.assert_reason(
                        "CLASSPATH_INVALID",
                        lambda entries=entries, expected_project=expected_project: runner.pin_classpath(
                            benchmark, entries, expected_project
                        ),
                    )

    def test_atomic_json_write_replaces_complete_document(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "value.json"
            runner.atomic_write_json(path, {"first": True})
            runner.atomic_write_json(path, {"second": True})
            self.assertEqual({"second": True}, json.loads(path.read_text()))
            self.assertEqual([], list(path.parent.glob(".*.tmp")))

    def test_benchmark_jar_normalization_removes_only_signatures_deterministically(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "source-jmh.jar"
            first = root / "first.jar"
            second = root / "second.jar"
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                with zipfile.ZipFile(source, "w") as archive:
                    archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
                    archive.writestr("META-INF/DEPENDENCY.SF", "signature-one")
                    archive.writestr("META-INF/DEPENDENCY.SF", "signature-two")
                    archive.writestr("META-INF/DEPENDENCY.RSA", "signature")
                    archive.writestr("META-INF/OTHER.DSA", "signature")
                    archive.writestr("META-INF/services/example.Service", "example.One\n")
                    archive.writestr("META-INF/services/example.Service", "example.Two\n")
                    archive.writestr("example/Main.class", b"class-bytes")

            first_result = runner.normalize_benchmark_jar(source, first)
            second_result = runner.normalize_benchmark_jar(source, second)

            self.assertEqual(first_result["source_sha256"], runner.sha256_file(source))
            self.assertEqual(first_result["executable_sha256"], runner.sha256_file(first))
            self.assertEqual(first_result["executable_sha256"], second_result["executable_sha256"])
            with zipfile.ZipFile(first) as archive:
                self.assertEqual(
                    [
                        "META-INF/MANIFEST.MF",
                        "META-INF/services/example.Service",
                        "META-INF/services/example.Service",
                        "example/Main.class",
                    ],
                    archive.namelist(),
                )
            self.assertEqual(
                [
                    "META-INF/DEPENDENCY.RSA",
                    "META-INF/DEPENDENCY.SF",
                    "META-INF/DEPENDENCY.SF",
                    "META-INF/OTHER.DSA",
                ],
                first_result["removed_entries"],
            )

    def test_preflight_output_requires_exact_schema_and_passed_status(self):
        fixture = {
            "schema_version": 1,
            "status": "passed",
            "fixture_sha256": "f" * 64,
            "cells": [{"method": method} for method in runner.EXPECTED_METHODS],
            "dispatch": {
                backend: {
                    "declaring_class": runner.BACKEND_CLASSES[backend],
                    "dispatch_kind": "declared-direct",
                    "runtime_declaring_class": runner.BACKEND_CLASSES[backend],
                    "runtime_dispatch_kind": "declared-direct",
                }
                for backend in runner.BACKENDS
            },
        }
        parsed = runner.parse_preflight_stdout(json.dumps(fixture))
        self.assertEqual("passed", parsed["status"])
        for changed in (
            {**fixture, "status": "failed"},
            {**fixture, "cells": fixture["cells"][:-1]},
            {**fixture, "schema_version": 2},
        ):
            self.assert_reason(
                "PREFLIGHT_MISMATCH",
                lambda changed=changed: runner.parse_preflight_stdout(json.dumps(changed)),
            )

    def test_list_output_requires_exact_matrix_and_allows_named_diagnostics(self):
        promotion = [runner.BENCHMARK_CLASS + "." + method for method in runner.EXPECTED_METHODS]
        output = "Benchmarks:\n" + "\n".join(
            promotion + [runner.BENCHMARK_CLASS + ".growthDiagnostic"]
        )
        result = runner.parse_benchmark_list(output)
        self.assertEqual(promotion, result["promotion"])
        self.assertEqual(
            [runner.BENCHMARK_CLASS + ".growthDiagnostic"], result["diagnostic"]
        )
        self.assert_reason(
            "MATRIX_EXACT",
            lambda: runner.parse_benchmark_list("Benchmarks:\n" + "\n".join(promotion[:-1])),
        )
        self.assert_reason(
            "MATRIX_EXACT",
            lambda: runner.parse_benchmark_list(
                output + "\n" + runner.BENCHMARK_CLASS + ".unexpected"
            ),
        )

    def test_metadata_records_ordered_classpath_hashes_and_preflight_binding(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            benchmark = root / "benchmark.jar"
            jackson = root / "jackson2.jar"
            benchmark.write_bytes(b"benchmark")
            jackson.write_bytes(b"jackson")
            classpath = runner.pin_classpath(benchmark, [jackson], jackson)
            preflight = {
                "schema_version": 1,
                "status": "passed",
                "fixture_sha256": "f" * 64,
                "cells": [{"method": method} for method in runner.EXPECTED_METHODS],
                "dispatch": {},
            }
            metadata = runner.build_metadata(
                "canonical-a",
                {"benchmark_input_sha": "a" * 40, "benchmark_input_tree": "b" * 40},
                classpath,
                preflight,
            )
            self.assertEqual(classpath, metadata["classpath"])
            expected_hash = hashlib.sha256(
                json.dumps(preflight, sort_keys=True, separators=(",", ":")).encode()
            ).hexdigest()
            self.assertEqual(expected_hash, metadata["preflight_sha256"])


if __name__ == "__main__":
    unittest.main()
