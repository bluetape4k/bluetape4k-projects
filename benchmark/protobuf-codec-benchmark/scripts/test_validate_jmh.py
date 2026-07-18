import csv
import contextlib
import hashlib
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path


HERE = Path(__file__).resolve().parent


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


validator = load_module("validate_jmh", "validate-jmh.py")
runner = load_module("run_evidence_for_validator", "run-evidence.py")


def write_v2_bundle(root, dispatch, verdicts, old="old", post="post"):
    root = Path(root); archive = root / "archive"; archive.mkdir()
    comparison = archive / "comparison.csv"
    with comparison.open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=("method", "verdict")); writer.writeheader()
        for method, verdict_value in verdicts.items(): writer.writerow({"method": method, "verdict": verdict_value})
    regressed = [cell for cell in runner.DISPATCH_CELLS[dispatch] if verdicts.get(cell) == "regressed"]
    decision = runner.make_rollback_decision(dispatch, regressed, old, old + "-tree", archive, [comparison], 1, "now", post, None, post + "-tree")
    return runner.write_rollback_bundle(root, [decision])


def comparison_run(run_id, baseline=1000.0, candidate=940.0):
    return {
        "run_id": run_id,
        "observed_config_sha256": "observed",
        "rows": {
            "serializerEncodeByteArray": {"allocation": baseline, "throughput": 10.0, "eligible": False},
            "serializerEncodeHeapOptimized": {"allocation": candidate, "throughput": 11.0, "eligible": True},
        },
    }


def environment(run_id="run-a", tree_hash="aaa"):
    value = {field: "same" for field in validator.IDENTITY_FIELDS}
    value.update({"run_id": run_id, "tree_hash": tree_hash})
    return value


class ValidateJmhTest(unittest.TestCase):
    def test_v2_direct_only_decode_regression_removes_both_mapped_cells(self):
        with tempfile.TemporaryDirectory() as td:
            bundle_path = write_v2_bundle(Path(td), "serializer_decode", {
                "serializerDecodeHeapOptimized": "inconclusive",
                "serializerDecodeDirectOptimized": "regressed",
            })
            result = validator.validate_rollback_bundle(bundle_path)
            self.assertEqual(set(runner.DISPATCH_CELLS["serializer_decode"]), result["ineligible_cells"])
            decision = result["decisions"][0]
            self.assertEqual(["serializerDecodeDirectOptimized"], decision["regressed_cells"])
            self.assertEqual(sorted(runner.DISPATCH_CELLS["serializer_decode"]), decision["removed_cells"])
            first = comparison_run("a"); second = comparison_run("b")
            for value in (first, second):
                value["rows"].update({
                    "serializerDecodeByteArray": {"allocation": 1000.0, "throughput": 10.0, "eligible": False},
                    "serializerDecodeHeapOptimized": {"allocation": 990.0, "throughput": 10.0, "eligible": True},
                    "serializerDecodeDirectOptimized": {"allocation": 1100.0, "throughput": 10.0, "eligible": True},
                })
            compared = validator.compare_runs(first, second, result["ineligible_cells"])
            for cell in runner.DISPATCH_CELLS["serializer_decode"]:
                self.assertEqual(("ineligible", "removed_after_regression"), (compared[cell]["verdict"], compared[cell]["reason"]))

            preparation = Path(td) / result["bundle"]["preparation_path"]
            with self.assertRaisesRegex(ValueError, "finalized v2"):
                validator.validate_rollback_bundle(preparation)
            legacy = Path(td) / "legacy.json"; legacy.write_text(json.dumps({"schema_version": 1, "decisions": []}))
            with self.assertRaisesRegex(ValueError, "v1"):
                validator.validate_rollback_bundle(legacy)

    def test_rollback_bundle_non_object_json_fails_concisely(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            for index, value in enumerate(([], 7, None)):
                path = root / ("non-object-{}.json".format(index)); path.write_text(json.dumps(value))
                with self.assertRaisesRegex(ValueError, "JSON object") as caught:
                    validator.validate_rollback_bundle(path)
                self.assertIn(str(path), str(caught.exception))
                self.assertIn("remediation:", str(caught.exception))
    def assert_diagnostic(self, error, path):
        message = str(error)
        self.assertIn(str(path), message)
        self.assertIn("!=", message)
        self.assertIn("remediation:", message)

    def test_complete_two_run_matrix_is_accepted(self):
        result = validator.compare_runs(
            comparison_run("run-a", candidate=940.0),
            comparison_run("run-b", candidate=930.0),
        )
        self.assertEqual("accepted", result["serializerEncodeHeapOptimized"]["verdict"])

    def test_missing_and_unexpected_methods_fail(self):
        with self.assertRaisesRegex(ValueError, "missing=.*serializerEncodeByteArray"):
            validator.validate_methods([])
        with self.assertRaisesRegex(ValueError, "unexpected=.*driftedMethod"):
            validator.validate_methods(list(validator.EXPECTED_METHODS) + ["driftedMethod"])

    def test_invalid_metrics_fail_closed(self):
        for score in (float("nan"), float("inf"), -1.0, "not-a-number"):
            with self.subTest(score=score), self.assertRaises(ValueError):
                validator.validate_score(score, "gc.alloc.rate.norm")

    def test_verdict_matrix(self):
        self.assertEqual("accepted", validator.verdict([-6.0, -5.0], True))
        self.assertEqual("regressed", validator.verdict([6.0, 5.0], True))
        self.assertEqual("inconclusive", validator.verdict([-6.0, -4.0], True))
        self.assertEqual("ineligible", validator.verdict([-99.0, -99.0], False))

    def test_duplicate_run_id_and_identity_mismatch_fail(self):
        with self.assertRaisesRegex(ValueError, "duplicate run_id"):
            validator.compare_runs(comparison_run("same"), comparison_run("same"))
        with self.assertRaisesRegex(ValueError, "tree_hash: aaa != bbb"):
            validator.validate_identity(environment(tree_hash="aaa"), environment(run_id="run-b", tree_hash="bbb"))

    def test_canonical_config_has_exact_fields_and_fixed_sha(self):
        config = {
            "allowed_class_prefixes": ["z", "a"], "direct_capacity": 8,
            "direct_initial_position": 0, "heap_capacity": 7, "heap_initial_position": 1,
            "matrix_version": "v1", "methods": ["b", "a"], "payload_identity": "fixture",
            "payload_sha256": "p", "redisson_codec_class": "R", "serializer_class": "S",
            "target_headroom": 2, "target_start": 1, "ignored": "x",
        }
        expected = '{"allowed_class_prefixes":["a","z"],"direct_capacity":8,"direct_initial_position":0,"heap_capacity":7,"heap_initial_position":1,"matrix_version":"v1","methods":["a","b"],"payload_identity":"fixture","payload_sha256":"p","redisson_codec_class":"R","serializer_class":"S","target_headroom":2,"target_start":1}'
        self.assertEqual(expected, validator.canonical_config_json(config))
        self.assertEqual("6f2abfb1ae7a91ac52673cad9fe2a04b6154a919c07d898e07c3f09ba37bd122", validator.config_sha256(config))
        for key in validator.CONFIG_KEYS:
            changed = dict(config)
            changed[key] = ["changed"] if isinstance(config[key], list) else "changed"
            self.assertNotEqual(validator.config_sha256(config), validator.config_sha256(changed), key)

    def test_jmh_records_require_exact_matrix_units_and_observed_profile(self):
        records = make_records()
        parsed = validator.parse_jmh_records(records, "fixture.json")
        self.assertEqual(set(validator.EXPECTED_METHODS), set(parsed["rows"]))
        self.assertEqual(validator.CANONICAL_PROFILE["exact_jvm_args"], parsed["observed_config"]["jvm_args"])
        bad = make_records()
        bad[0]["primaryMetric"]["scoreUnit"] = "ms/op"
        with self.assertRaisesRegex(ValueError, "ops/s"):
            validator.parse_jmh_records(bad, "fixture.json")
        bad = make_records()
        bad[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["score"] = float("nan")
        with self.assertRaises(ValueError):
            validator.parse_jmh_records(bad, "fixture.json")
        bad = make_records()
        bad[0]["jvmArgs"] = list(reversed(validator.CANONICAL_PROFILE["exact_jvm_args"]))
        with self.assertRaisesRegex(ValueError, "jvmArgs"):
            validator.parse_jmh_records(bad, "fixture.json")

    def test_duplicate_method_wrong_gc_unit_and_every_jvm_arg_drift_fail(self):
        duplicate = make_records()
        duplicate.append(dict(duplicate[0]))
        with self.assertRaisesRegex(ValueError, "duplicate methods") as caught:
            validator.parse_jmh_records(duplicate, "duplicate.json")
        self.assert_diagnostic(caught.exception, "duplicate.json")

        wrong_unit = make_records()
        wrong_unit[0]["secondaryMetrics"]["gc.alloc.rate.norm"]["scoreUnit"] = "KB/op"
        with self.assertRaisesRegex(ValueError, "B/op") as caught:
            validator.parse_jmh_records(wrong_unit, "unit.json")
        self.assert_diagnostic(caught.exception, "unit.json")

        canonical = validator.CANONICAL_PROFILE["exact_jvm_args"]
        variants = {
            "extra": canonical + ["-Dextra=true"],
            "missing": canonical[:-1],
            "duplicate": canonical + [canonical[-1]],
            "reordered": list(reversed(canonical)),
        }
        for name, args in variants.items():
            with self.subTest(name=name):
                records = make_records()
                for record in records:
                    record["jvmArgs"] = args
                with self.assertRaisesRegex(ValueError, "jvmArgs") as caught:
                    validator.parse_jmh_records(records, name + ".json")
                self.assert_diagnostic(caught.exception, name + ".json")

    def test_every_observed_setting_is_consistent_and_manifest_bound(self):
        for field, replacement in (
            ("threads", 9), ("forks", 9), ("warmupIterations", 9),
            ("measurementIterations", 9), ("warmupTime", "9 s"),
            ("measurementTime", "9 s"), ("jdkVersion", "99"),
            ("vmName", "other"), ("vmVersion", "99"),
        ):
            with self.subTest(field=field):
                records = make_records()
                records[-1][field] = replacement
                with self.assertRaisesRegex(ValueError, field):
                    validator.parse_jmh_records(records, "fixture.json")

        parsed = validator.parse_jmh_records(make_records(), "fixture.json")
        manifest = manifest_for(parsed)
        for field in ("threads", "forks", "warmups", "measurements", "warmup_time", "measurement_time", "jvm_args"):
            with self.subTest(field=field):
                changed = dict(manifest)
                changed[field] = ["-Xmx2g"] if field == "jvm_args" else "different"
                with self.assertRaisesRegex(ValueError, field):
                    validator.validate_manifest_observations(changed, parsed, "environment.json")

    def test_every_canonical_config_field_mutation_is_rejected_by_manifest_binding(self):
        parsed = validator.parse_jmh_records(make_records(), "fixture.json")
        base = manifest_for(parsed)
        base["config_json"] = fixture_config()
        base["config_sha256"] = validator.config_sha256(fixture_config())
        for field in validator.CONFIG_KEYS:
            with self.subTest(field=field):
                changed = json.loads(json.dumps(base))
                value = changed["config_json"][field]
                if isinstance(value, list):
                    changed["config_json"][field] = value + ["drift"]
                elif isinstance(value, int):
                    changed["config_json"][field] = value + 1
                else:
                    changed["config_json"][field] = value + "-drift"
                with self.assertRaises(ValueError) as caught:
                    validator.validate_manifest_observations(changed, parsed, "config-environment.json")
                self.assert_diagnostic(caught.exception, "config-environment.json")

    def test_run_validation_binds_config_clean_tree_and_jar_twice(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            jar = root / "benchmark-JMH.jar"; jar.write_bytes(b"jar")
            input_path = root / "jmh.json"; input_path.write_text(json.dumps(make_records()))
            parsed = validator.parse_jmh_records(make_records(), str(input_path))
            env = manifest_for(parsed)
            env.update({
                "benchmark_jar_path": str(jar.resolve()),
                "benchmark_jar_sha256": hashlib.sha256(b"jar").hexdigest(),
                "run_id": "run-a", "clean_status": "clean",
                "config_json": fixture_config(),
                "config_sha256": validator.config_sha256(fixture_config()),
                "payload_sha256": "payload", "payload_size": 1,
            })
            environment_path = root / "environment.json"
            environment_path.write_text(json.dumps(env))
            (root / "run.log").write_text("exit_code=0\n")
            (root / "argv.json").write_text(json.dumps({"argv": env.get("jmh_argv"), "exit_code": 0, "log_limit_exceeded": False}))
            result = validator.validate_run(jar, input_path, environment_path, root / "summary.csv", root / "validation.json")
            self.assertEqual("passed", result["status"])
            self.assertEqual(env["benchmark_jar_sha256"], result["benchmark_jar_sha256"])
            self.assertTrue((root / "summary.csv").is_file())
            self.assertTrue((root / "validation.json").is_file())

            canonical = dict(env)
            canonical.pop("benchmark_jar_path")
            canonical_ref = "<PINNED_JAR_SHA256:{}>".format(env["benchmark_jar_sha256"])
            canonical["benchmark_jar_ref"] = canonical_ref
            canonical["executed_jar_ref"] = canonical_ref
            environment_path.write_text(json.dumps(canonical))
            canonical_result = validator.validate_run(
                jar, input_path, environment_path, root / "canonical.csv", root / "canonical.json",
            )
            self.assertEqual("passed", canonical_result["status"])
            tampered_ref = dict(canonical); tampered_ref["executed_jar_ref"] = "<PINNED_JAR_SHA256:{}>".format("0" * 64)
            environment_path.write_text(json.dumps(tampered_ref))
            with self.assertRaisesRegex(ValueError, "executed_jar_ref"):
                validator.validate_run(jar, input_path, environment_path, root / "bad-ref.csv", root / "bad-ref.json")

            bad = dict(env); bad["benchmark_jar_sha256"] = "0" * 64
            environment_path.write_text(json.dumps(bad))
            with self.assertRaisesRegex(ValueError, "benchmark_jar_sha256"):
                validator.validate_run(jar, input_path, environment_path, root / "bad.csv", root / "bad.json")
            bad = dict(env); bad["clean_status"] = " M source.kt"
            environment_path.write_text(json.dumps(bad))
            with self.assertRaisesRegex(ValueError, "clean_status"):
                validator.validate_run(jar, input_path, environment_path, root / "bad.csv", root / "bad.json")

            environment_path.write_text(json.dumps(env))
            with (root / "run.log").open("wb") as stream:
                stream.truncate(validator.MAX_RUN_LOG_BYTES + 1)
            with self.assertRaisesRegex(ValueError, "run.log.*size"):
                validator.validate_run(jar, input_path, environment_path, root / "oversized.csv", root / "oversized.json")

    def test_run_log_rejects_limit_marker_anywhere_including_chunk_boundary(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "run.log"
            marker = b"[runner] output truncated: log size limit exceeded"
            for prefix in (b"ordinary\n", b"x" * (64 * 1024 - len(marker) // 2)):
                with self.subTest(prefix_size=len(prefix)):
                    path.write_bytes(prefix + marker + b"\nexit_code=0\n")
                    with self.assertRaisesRegex(ValueError, "limit marker"):
                        validator.validate_run_log(path)

    def test_run_log_requires_an_exact_success_exit_line(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "run.log"
            path.write_bytes(b"ordinary\nnot_exit_code=0\n")
            with self.assertRaisesRegex(ValueError, "run.log tail"):
                validator.validate_run_log(path)

    def test_run_log_detects_same_size_path_inode_swap_during_read(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            path = root / "run.log"
            payload = b"ordinary\nexit_code=0\n"
            path.write_bytes(payload)
            replacement = root / "replacement.log"
            replacement.write_bytes(payload)
            original = validator.os.read
            swapped = []

            def swap_after_read(fd, size):
                data = original(fd, size)
                if data and not swapped:
                    validator.os.replace(replacement, path)
                    swapped.append(True)
                return data

            validator.os.read = swap_after_read
            try:
                with self.assertRaisesRegex(ValueError, "identity"):
                    validator.validate_run_log(path)
            finally:
                validator.os.read = original

    def test_execution_artifacts_reject_bool_exit_limit_flag_and_log_mismatch(self):
        environment = {"jmh_argv": ["java", "-jar", "benchmark.jar"]}
        valid_argv = {"argv": environment["jmh_argv"], "exit_code": 0, "log_limit_exceeded": False}
        log_result = {"exit_code": 0}
        cases = (
            ({**valid_argv, "exit_code": False}, log_result, "exit_code"),
            ({**valid_argv, "log_limit_exceeded": 0}, log_result, "log_limit_exceeded"),
            (valid_argv, {"exit_code": 1}, "log exit"),
        )
        for argv, observed_log, message in cases:
            with self.subTest(message=message), self.assertRaisesRegex(ValueError, message):
                validator.validate_execution_artifacts(argv, environment, observed_log, "run")

    def test_runner_argv_and_clean_gate_objects_are_manifest_observations(self):
        parsed = validator.parse_jmh_records(make_records(), "fixture.json")
        environment = {
            "jmh_argv": ["java", "-jar", "/x.jar", "-t", "1", "-f", "2", "-wi", "3",
                         "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json",
                         "-rff", "jmh.json", "-jvmArgsAppend", "-Xms1g -Xmx1g -XX:+UseG1GC"],
            "metadata": {"matrix_version": "v1", "target_headroom": 2, "target_start": 1},
        }
        validator.validate_manifest_observations(environment, parsed, "environment.json")
        self.assertTrue(validator.is_clean_environment({
            "initial_clean_status": {"stdout": ""}, "prelaunch_clean_status": {"stdout": ""},
        }))

    def test_runner_clean_observation_dicts_are_authenticated(self):
        clean = {
            "clean_status": "clean", "initial_clean_status": "clean", "prelaunch_clean_status": "clean",
            "initial_clean_observation": {"stdout": "", "sha256": "empty"},
            "prelaunch_clean_observation": {"stdout": "", "sha256": "empty"},
        }
        self.assertTrue(validator.is_clean_environment(clean))
        for field in ("initial_clean_observation", "prelaunch_clean_observation"):
            with self.subTest(field=field):
                dirty = json.loads(json.dumps(clean))
                dirty[field]["stdout"] = "?? untracked"
                self.assertFalse(validator.is_clean_environment(dirty))

    def test_runner_emitted_clean_dicts_pass_real_run_validation(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jar, input_path, environment_path = write_valid_run_fixture(root)
            environment_value = json.loads(environment_path.read_text())
            environment_value.update({
                "initial_clean_status": {"phase": "initial", "stdout": "", "sha256": "empty"},
                "prelaunch_clean_status": {"phase": "pre-launch", "stdout": "", "sha256": "empty"},
            })
            environment_path.write_text(json.dumps(environment_value))
            result = validator.validate_run(
                jar, input_path, environment_path, root / "summary.csv", root / "validation.json",
            )
            self.assertEqual("passed", result["status"])

    def test_compare_csv_and_validation_include_observed_sha(self):
        parsed = validator.parse_jmh_records(make_records(), "fixture.json")
        first = {"run_id": "a", "rows": parsed["rows"], "observed_config_sha256": parsed["observed_config_sha256"]}
        second = {"run_id": "b", "rows": parsed["rows"], "observed_config_sha256": parsed["observed_config_sha256"]}
        comparison = validator.compare_runs(first, second)
        self.assertEqual(13, len(comparison))
        self.assertEqual("inconclusive", comparison["serializerEncodeHeapOptimized"]["verdict"])
        self.assertEqual("ineligible", comparison["trustedFallbackEncodeBufferCompatibility"]["verdict"])
        self.assertEqual("compatibility_control", comparison["trustedFallbackEncodeBufferCompatibility"]["reason"])
        changed = dict(second); changed["observed_config_sha256"] = "different"
        with self.assertRaisesRegex(ValueError, "observed_config_sha256"):
            validator.compare_runs(first, changed)

    def test_rollback_bundle_authenticates_hash_cells_and_regressed_archive(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); bundle_path = write_v2_bundle(root, "serializer_encode", {
                "serializerEncodeHeapOptimized": "regressed", "serializerEncodeDirectOptimized": "regressed",
            })
            bundle = validator.validate_rollback_bundle(bundle_path)
            self.assertEqual(set(runner.DISPATCH_CELLS["serializer_encode"]), bundle["ineligible_cells"])
            comparison = root / "archive" / "comparison.csv"; comparison.write_text("tampered")
            with self.assertRaisesRegex(ValueError, "sha256"):
                validator.validate_rollback_bundle(bundle_path)

    def test_rollback_bundle_accepts_archive_artifact_schema(self):
        with tempfile.TemporaryDirectory() as td:
            bundle_path = write_v2_bundle(Path(td), "serializer_decode", {cell: "regressed" for cell in runner.DISPATCH_CELLS["serializer_decode"]})
            result = validator.validate_rollback_bundle(bundle_path)
            self.assertEqual(set(runner.DISPATCH_CELLS["serializer_decode"]), result["ineligible_cells"])

    def test_non_regressed_rollback_and_unrelated_ineligible_cell_are_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            with self.assertRaisesRegex(ValueError, "regressed") as caught:
                write_v2_bundle(Path(td), "redisson_contiguous", {"redissonDecodeContiguousOptimized": "accepted"})

        with self.assertRaisesRegex(ValueError, "ineligible cells") as caught:
            validator.compare_runs(
                comparison_run("a"), comparison_run("b"),
                {"trustedFallbackEncodeBufferCompatibility"}, "unrelated.csv",
            )
        self.assert_diagnostic(caught.exception, "unrelated.csv")

    def test_chained_rollback_requires_exact_predecessor_file(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); bundles = []
            for generation, dispatch in ((1, "serializer_encode"), (2, "serializer_decode")):
                archive = root / ("archive-g" + str(generation)); archive.mkdir()
                comparison = archive / "comparison.csv"
                comparison.write_text("method,verdict\n" + "".join("{},regressed\n".format(cell) for cell in runner.DISPATCH_CELLS[dispatch]))
                decision = runner.make_rollback_decision(dispatch, list(runner.DISPATCH_CELLS[dispatch]), "old-" + str(generation), "tree-" + str(generation), archive, [comparison], generation, "now", "post-" + str(generation), None if generation == 1 else "post-1", "post-tree-" + str(generation))
                bundles.append(runner.write_rollback_bundle(root, [decision], predecessor=bundles[-1] if bundles else None))
            bundle_path = bundles[-1]; bundles[0].unlink()
            with self.assertRaisesRegex(ValueError, "predecessor") as caught:
                validator.validate_rollback_bundle(bundle_path)
            self.assertIn("remediation:", str(caught.exception))

    def test_jar_file_identity_replacement_between_hashes_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            jar, input_path, environment_path = write_valid_run_fixture(root)
            original = validator._jar_identity
            calls = []
            def changing_identity(path):
                value = original(path)
                calls.append(value)
                return value if len(calls) == 1 else value[:-1] + (value[-1] + 1,)
            validator._jar_identity = changing_identity
            try:
                with self.assertRaisesRegex(ValueError, "file identity") as caught:
                    validator.validate_run(jar, input_path, environment_path, root / "summary.csv", root / "validation.json")
            finally:
                validator._jar_identity = original
            self.assert_diagnostic(caught.exception, jar)

    def test_malformed_cli_input_has_one_diagnostic_without_traceback(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); environment_path = root / "environment.json"
            environment_path.write_text("{broken")
            validation_path = root / "validation.json"
            stderr = io.StringIO()
            with contextlib.redirect_stderr(stderr):
                exit_code = validator.main([
                    "run", "--jar", str(root / "missing.jar"), "--input", str(root / "jmh.json"),
                    "--environment", str(environment_path), "--summary", str(root / "summary.csv"),
                    "--validation", str(validation_path),
                ])
            self.assertEqual(1, exit_code)
            diagnostic = stderr.getvalue()
            self.assertEqual(1, diagnostic.count("validation failed:"))
            self.assertNotIn("Traceback", diagnostic)
            self.assertIn(str(environment_path), diagnostic)
            self.assertIn("!=", diagnostic)
            self.assertIn("remediation:", diagnostic)
            self.assertEqual("failed", json.loads(validation_path.read_text())["status"])

    def test_malformed_public_inputs_are_diagnostic_value_errors(self):
        parsed = validator.parse_jmh_records(make_records(), "fixture.json")
        cases = (
            ("methods.json", lambda: validator.validate_methods(None, "methods.json")),
            ("environment.json", lambda: validator.validate_manifest_observations(None, parsed, "environment.json")),
            ("run-a.json", lambda: validator.validate_identity(None, {}, "run-a.json", "run-b.json")),
            ("comparison.csv", lambda: validator.compare_runs(None, {}, path="comparison.csv")),
        )
        for path, operation in cases:
            with self.subTest(path=path):
                with self.assertRaises(ValueError) as caught:
                    operation()
                self.assert_diagnostic(caught.exception, path)


def fixture_config():
    return {
        "allowed_class_prefixes": ["a"], "direct_capacity": 8,
        "direct_initial_position": 0, "heap_capacity": 7, "heap_initial_position": 1,
        "matrix_version": "v1", "methods": sorted(validator.EXPECTED_METHODS),
        "payload_identity": "fixture", "payload_sha256": "payload",
        "redisson_codec_class": "R", "serializer_class": "S",
        "target_headroom": 2, "target_start": 1,
    }


def write_valid_run_fixture(root):
    jar = root / "benchmark-JMH.jar"; jar.write_bytes(b"jar")
    input_path = root / "jmh.json"; input_path.write_text(json.dumps(make_records()))
    parsed = validator.parse_jmh_records(make_records(), str(input_path))
    env = manifest_for(parsed)
    env.update({
        "benchmark_jar_path": str(jar.resolve()),
        "benchmark_jar_sha256": validator.sha256_file(jar), "run_id": "run-a",
        "config_json": fixture_config(), "config_sha256": validator.config_sha256(fixture_config()),
        "payload_sha256": "payload", "payload_size": 1,
    })
    environment_path = root / "environment.json"; environment_path.write_text(json.dumps(env))
    (root / "run.log").write_text("exit_code=0\n")
    (root / "argv.json").write_text(json.dumps({"argv": env.get("jmh_argv"), "exit_code": 0, "log_limit_exceeded": False}))
    return jar, input_path, environment_path


def manifest_for(parsed):
    observed = parsed["observed_config"]
    result = {
        "threads": observed["threads"], "forks": observed["forks"],
        "warmups": observed["warmups"], "measurements": observed["measurements"],
        "warmup_time": observed["warmup_time"], "measurement_time": observed["measurement_time"],
        "jvm_args": observed["jvm_args"], "jdk_version": observed["jdk_version"],
        "vm_name": observed["vm_name"], "vm_version": observed["vm_version"],
        "matrix_version": "v1", "target_headroom": 2, "target_start": 1,
        "observed_config_sha256": parsed["observed_config_sha256"],
    }
    result.update({
        "git_commit": "commit", "tree_hash": "tree", "os": "os", "arch": "arch", "cpu": "cpu",
        "jvm_vendor": "vendor", "jvm_version": "version", "gradle_version": "gradle",
        "jmh_version": "jmh", "payload_size": 1, "payload_sha256": "payload",
        "config_sha256": "config", "metadata_stdout_sha256": "metadata",
        "benchmark_jar_sha256": "jar", "clean_status": "clean", "power_state": "ac",
        "concurrent_heavy_work": "absent", "profiler": "gc",
    })
    return result


def make_records():
    records = []
    for method in sorted(validator.EXPECTED_METHODS):
        records.append({
            "benchmark": "io.example.ProtobufCodecBenchmark." + method,
            "mode": "thrpt", "threads": 1, "forks": 2,
            "warmupIterations": 3, "measurementIterations": 5,
            "warmupTime": "1 s", "measurementTime": "1 s",
            "jdkVersion": "21", "vmName": "OpenJDK", "vmVersion": "21.0.1",
            "jvmArgs": list(validator.CANONICAL_PROFILE["exact_jvm_args"]),
            "params": {"matrixVersion": "v1", "targetHeadroom": "2", "targetStart": "1"},
            "primaryMetric": {"score": 10.0, "scoreUnit": "ops/s"},
            "secondaryMetrics": {"gc.alloc.rate.norm": {"score": 100.0, "scoreUnit": "B/op"}},
        })
    return records


if __name__ == "__main__":
    unittest.main()
