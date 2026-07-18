import importlib.util
import contextlib
import io
import json
import os
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


HERE = Path(__file__).resolve().parent


def load_module(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runner = load_module("run_evidence", "run-evidence.py")
validator = load_module("validate_jmh_for_runner", "validate-jmh.py")


def complete_jmh_records():
    records = []
    for method in sorted(validator.EXPECTED_METHODS):
        records.append({
            "benchmark": "io.example.ProtobufCodecBenchmark." + method,
            "mode": "thrpt", "threads": 1, "forks": 2,
            "warmupIterations": 3, "measurementIterations": 5,
            "warmupTime": "1 s", "measurementTime": "1 s",
            "jdkVersion": "21.0.1", "vmName": "VM", "vmVersion": "21+1",
            "jvmArgs": list(runner.JVM_ARGS),
            "params": {"matrixVersion": "v1", "targetHeadroom": "2", "targetStart": "1"},
            "primaryMetric": {"score": 10.0, "scoreUnit": "ops/s"},
            "secondaryMetrics": {"gc.alloc.rate.norm": {"score": 100.0, "scoreUnit": "B/op"}},
        })
    return records


def benchmark_metadata():
    config = {
        "allowed_class_prefixes": ["io.example"], "direct_capacity": 20,
        "direct_initial_position": 0, "heap_capacity": 20, "heap_initial_position": 0,
        "matrix_version": "v1", "methods": sorted(validator.EXPECTED_METHODS),
        "payload_identity": "fixture", "payload_sha256": "payload",
        "redisson_codec_class": "R", "serializer_class": "S",
        "target_headroom": 2, "target_start": 1,
    }
    return {"schema_version": 1, "matrix_version": "v1", "target_headroom": 2, "target_start": 1,
            "payload_size": 12, "payload_sha256": "payload", "config_sha256": validator.config_sha256(config),
            "config_json": validator.canonical_config_json(config)}


def build_complete_delivery(root):
    root = Path(root); evidence = root / "docs" / "raw"; evidence.mkdir(parents=True)
    metadata = benchmark_metadata(); metadata_stdout = json.dumps(metadata, separators=(",", ":")) + "\n"
    parsed = validator.parse_jmh_records(complete_jmh_records(), "fixture")
    summaries = []; environments = []; commands = []
    jar_path = root / "build" / "bench-JMH.jar"; jar_hash = "a" * 64
    for run_id in ("run-a", "run-b"):
        run_root = evidence / run_id; run_root.mkdir()
        jmh_path = run_root / "jmh.json"; jmh_path.write_text(json.dumps(complete_jmh_records()))
        runner.atomic_write_json(run_root / "metadata.json", metadata)
        argv = ["java", "-jar", str(jar_path), "-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json", "-rff", str(jmh_path), "-jvmArgsAppend", " ".join(runner.JVM_ARGS)]
        clean_a = {"phase": "initial", "stdout": "", "sha256": runner.sha256_bytes(b"")}
        clean_b = {"phase": "pre-launch", "stdout": "", "sha256": runner.sha256_bytes(b"")}
        environment = {"schema_version": 1, "run_id": run_id, "git_commit": "measure", "tree_hash": "tree",
                       "os": "Darwin", "arch": "arm64", "cpu": "cpu", "jvm_vendor": "Vendor", "jvm_version": "21.0.1",
                       "gradle_version": "9.1", "jmh_version": "1.37", "benchmark_jar_path": str(jar_path),
                       "benchmark_jar_sha256": jar_hash, "metadata": metadata, "metadata_stdout": metadata_stdout,
                       "metadata_stdout_sha256": runner.sha256_bytes(metadata_stdout.encode()),
                       "metadata_prelaunch_stdout_sha256": runner.sha256_bytes(metadata_stdout.encode()),
                       "clean_status": "clean", "initial_clean_status": clean_a, "prelaunch_clean_status": clean_b,
                       "power_state": "ac", "concurrent_heavy_work": "absent", "payload_size": 12,
                       "payload_sha256": "payload", "config_sha256": metadata["config_sha256"], "config_json": metadata["config_json"],
                       "matrix_version": "v1", "target_headroom": 2, "target_start": 1, "rollback_bundle_sha256": None,
                       "jdk_version": "21.0.1", "vm_name": "VM", "vm_version": "21+1", "jmh_argv": argv}
        environment.update(runner.normalized_profile("canonical"))
        runner.atomic_write_json(run_root / "environment.json", environment)
        runner.atomic_write_json(run_root / "argv.json", {"schema_version": 1, "argv": argv, "started_at": "s", "ended_at": "e", "exit_code": 0})
        (run_root / "run.log").write_text("exit_code=0\n")
        summary = run_root / "summary.csv"; validator.write_summary(summary, run_id, parsed); summaries.append(validator.read_summary(summary))
        runner.atomic_write_json(run_root / "validation.json", {"schema_version": 1, "status": "passed", "mode": "run", "run_id": run_id,
            "benchmark_jar_sha256": jar_hash, "config_sha256": metadata["config_sha256"], "observed_config": parsed["observed_config"],
            "observed_config_sha256": parsed["observed_config_sha256"], "method_count": len(parsed["rows"])})
        environments.append(environment); commands.append(runner.manifest_command(argv, environment, run_root, root))
    comparison = validator.compare_runs(summaries[0], summaries[1]); comparison_path = evidence / "comparison.csv"
    validator.write_comparison(comparison_path, comparison)
    verdicts = {method: row["verdict"] for method, row in sorted(comparison.items())}
    reasons = {method: row["reason"] for method, row in sorted(comparison.items())}
    runner.atomic_write_json(evidence / "validation.json", {"schema_version": 1, "status": "passed", "mode": "compare",
        "run_ids": ["run-a", "run-b"], "comparison_sha256": runner.sha256_file(comparison_path), "verdicts": verdicts,
        "reasons": reasons, "rollback_bundle_sha256": None})
    results = []
    for index, summary in enumerate(summaries):
        for method, metrics in summary["rows"].items():
            row = comparison[method]
            results.append({"method": method, "run": ["run-a", "run-b"][index], "allocation_b_per_op": str(metrics["allocation"]),
                            "throughput_ops_per_s": str(metrics["throughput"]),
                            "delta_percent": row["run_{}_delta_percent".format("a" if index == 0 else "b")],
                            "verdict": row["verdict"], "reason": row["reason"]})
    manifest = runner.create_delivery_manifest(root, evidence, "measure", "tree", verdicts, {"decisions": []}, commands, results, "delivery", jar_hash, reasons)
    manifest_path = evidence / "delivery-manifest.json"; runner.atomic_write_json(manifest_path, manifest)
    return manifest_path


def rewrite_manifest(manifest_path, mutate):
    manifest = json.loads(Path(manifest_path).read_text())
    mutate(manifest)
    report_keys = ("measurement", "delivery", "final_verdicts", "final_reasons", "rollback", "commands", "results")
    manifest["report_input_sha256"] = runner.sha256_bytes(
        runner.payload_json_bytes({key: manifest[key] for key in report_keys})
    )
    runner.atomic_write_json(manifest_path, manifest)


class EvidenceRunnerTest(unittest.TestCase):
    def test_resolve_jar_requires_exactly_one_and_state_is_no_clobber(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            jars = root / "jars"; jars.mkdir()
            state = root / "state.json"
            with self.assertRaisesRegex(ValueError, "exactly one"):
                runner.resolve_jar(jars, state)
            jar = jars / "benchmark-JMH.jar"; jar.write_bytes(b"jar")
            result = runner.resolve_jar(jars, state)
            self.assertEqual(jar.resolve(), Path(result["benchmark_jar_path"]))
            self.assertEqual(runner.sha256_file(jar), result["benchmark_jar_sha256"])
            with self.assertRaisesRegex(ValueError, "exists"):
                runner.resolve_jar(jars, state)
            (jars / "other-JMH.jar").write_bytes(b"other")
            with self.assertRaisesRegex(ValueError, "exactly one"):
                runner.resolve_jar(jars, root / "other-state.json")

    def test_atomic_json_is_deterministic_and_fail_if_exists(self):
        with tempfile.TemporaryDirectory() as td:
            target = Path(td) / "value.json"
            runner.atomic_write_json(target, {"b": 2, "a": 1}, fail_if_exists=True)
            self.assertEqual('{\n  "a": 1,\n  "b": 2\n}\n', target.read_text())
            with self.assertRaises(FileExistsError):
                runner.atomic_write_json(target, {}, fail_if_exists=True)

    def test_append_canonical_run_rejects_duplicates_and_third_run(self):
        state = {"canonical_runs": []}
        runner.append_canonical_run(state, {"run_id": "a", "absolute_path": "/a"})
        runner.append_canonical_run(state, {"run_id": "b", "absolute_path": "/b"})
        with self.assertRaisesRegex(ValueError, "duplicate"):
            runner.append_canonical_run(state, {"run_id": "a", "absolute_path": "/c"})
        with self.assertRaisesRegex(ValueError, "exactly two"):
            runner.append_canonical_run(state, {"run_id": "c", "absolute_path": "/c"})

    def test_promote_is_no_clobber_and_manifest_rejects_absolute_paths(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); source = root / "source"; source.mkdir()
            (source / "a.txt").write_text("a")
            destination = root / "dest"
            runner.atomic_promote(source, destination)
            self.assertEqual("a", (destination / "a.txt").read_text())
            with self.assertRaisesRegex(ValueError, "exists"):
                runner.atomic_promote(source, destination)
            with self.assertRaisesRegex(ValueError, "absolute"):
                runner.validate_manifest({"files": [{"path": "/tmp/a", "sha256": "x"}]}, root / "manifest.json")

    def test_replace_restores_old_destination_on_second_rename_failure(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); old = root / "dest"; old.mkdir(); (old / "old").write_text("old")
            new = root / "new"; new.mkdir(); (new / "new").write_text("new")
            backup_root = root / "backups"
            original = runner._rename
            calls = []
            def fail_second(src, dst):
                calls.append((src, dst))
                if len(calls) == 2:
                    raise OSError("boom")
                return original(src, dst)
            runner._rename = fail_second
            try:
                with self.assertRaises(OSError):
                    runner.atomic_replace_promoted(new, old, backup_root)
            finally:
                runner._rename = original
            self.assertTrue((old / "old").is_file())
            self.assertFalse(any(backup_root.iterdir()))

    def test_replace_preserves_unique_backup_when_primary_and_restore_renames_fail(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); destination = root / "dest"; destination.mkdir(); (destination / "old").write_text("old")
            source = root / "new"; source.mkdir(); (source / "new").write_text("new")
            backup_root = root / "backups"
            original = runner._rename; calls = []
            def fail_replace_and_restore(src, dst):
                calls.append((Path(src), Path(dst)))
                if len(calls) == 2:
                    destination.mkdir(); (destination / "intruder").write_text("intruder")
                    raise OSError("primary rename failed")
                if len(calls) == 3:
                    raise OSError("restore rename failed")
                return original(src, dst)
            runner._rename = fail_replace_and_restore
            try:
                with self.assertRaisesRegex(ValueError, "primary rename failed.*restore rename failed.*backup preserved"):
                    runner.atomic_replace_promoted(source, destination, backup_root)
            finally:
                runner._rename = original
            backups = list(backup_root.iterdir())
            self.assertEqual(1, len(backups))
            self.assertEqual("old", (backups[0] / "old").read_text())
            self.assertEqual("intruder", (destination / "intruder").read_text())

    def test_cleanup_only_removes_exact_recorded_backup(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); backup_root = root / "backups"; backup_root.mkdir()
            recorded = backup_root / "backup-run-20260718T120000.000000Z-1234abcd"; recorded.mkdir(); (recorded / "x").write_text("x")
            other = backup_root / "backup-run-20260718T120001.000000Z-1234abcd"; other.mkdir()
            state = {"replacement_backup_path": str(recorded.resolve())}
            runner.cleanup_recorded_backup(state, recorded, backup_root)
            self.assertFalse(recorded.exists()); self.assertTrue(other.exists())
            with self.assertRaisesRegex(ValueError, "recorded backup"):
                runner.cleanup_recorded_backup(state, other, backup_root)

    def test_cleanup_refuses_root_nested_sibling_and_symlink_targets(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); backup_root = root / "backups"; backup_root.mkdir()
            nested_parent = backup_root / "nested"; nested_parent.mkdir()
            nested = nested_parent / "backup-run-20260718T120000.000000Z-1234abcd"; nested.mkdir()
            sibling = root / "backup-run-20260718T120001.000000Z-1234abcd"; sibling.mkdir()
            real = backup_root / "backup-run-20260718T120002.000000Z-1234abcd"; real.mkdir()
            linked = backup_root / "backup-run-20260718T120003.000000Z-1234abcd"; linked.symlink_to(real, target_is_directory=True)
            for requested in (backup_root, nested, sibling, linked):
                with self.subTest(requested=requested), self.assertRaisesRegex(ValueError, "recorded backup"):
                    runner.cleanup_recorded_backup({"replacement_backup_path": str(requested)}, requested, backup_root)
            self.assertTrue(backup_root.is_dir())
            self.assertTrue(real.is_dir())

    def test_clean_gate_reports_dirty_output_and_remediation(self):
        def dirty(*_args, **_kwargs):
            return subprocess.CompletedProcess([], 0, stdout=b"?? stray.txt\n", stderr=b"")
        with self.assertRaisesRegex(ValueError, r"stray\.txt.*git status"):
            runner.require_clean_tree(Path("/repo"), "initial", command_runner=dirty)

    def test_metadata_is_captured_twice_and_must_be_byte_identical(self):
        outputs = iter((b'{"matrix_version":"v1"}\n', b'{"matrix_version":"v2"}\n'))
        def command(*_args, **_kwargs):
            return subprocess.CompletedProcess([], 0, stdout=next(outputs), stderr=b"")
        jar = Path("/benchmark-JMH.jar")
        first = runner.capture_metadata(jar, command_runner=command)
        with self.assertRaisesRegex(ValueError, r"metadata.*v1.*v2.*rebuild"):
            runner.capture_metadata(jar, expected=first, command_runner=command)

    def test_failed_process_preserves_argv_log_timestamps_and_exit(self):
        with tempfile.TemporaryDirectory() as td:
            run_dir = Path(td)
            def failed(*_args, **_kwargs):
                return subprocess.CompletedProcess([], 7, stdout=b"out\n", stderr=b"err\n")
            with self.assertRaisesRegex(ValueError, "exit_code=7"):
                runner.execute_logged(["java", "-jar", "x"], run_dir, command_runner=failed)
            argv = json.loads((run_dir / "argv.json").read_text())
            self.assertEqual(7, argv["exit_code"])
            self.assertIn("started_at", argv); self.assertIn("ended_at", argv)
            self.assertIn("out\nerr\nexit_code=7", (run_dir / "run.log").read_text())

    def test_power_state_and_canonical_heavy_work_are_fail_closed(self):
        def unavailable(*_args, **_kwargs):
            return subprocess.CompletedProcess([], 127, stdout=b"", stderr=b"missing")
        power = runner.capture_power_state("Darwin", command_runner=unavailable)
        self.assertEqual("unknown", power["normalized"])
        self.assertIn("missing", power["stderr"])
        runner.validate_heavy_work("canonical", "absent", Path("state.json"))
        with self.assertRaisesRegex(ValueError, "concurrent_heavy_work"):
            runner.validate_heavy_work("canonical", "present", Path("state.json"))

    def test_manifest_verifies_hashes_without_build_state_and_detects_tamper(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            manifest_path = build_complete_delivery(root)
            data = manifest_path.parent / "comparison.csv"
            runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)
            data.write_text("tampered")
            with self.assertRaisesRegex(ValueError, r"comparison\.csv.*sha256"):
                runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)
            with self.assertRaisesRegex(ValueError, "absolute/build token"):
                runner.validate_manifest({"schema_version": 1, "files": [{"path": "docs/a", "sha256": "0" * 64}], "commands": [["java", "-jar", "/tmp/build/x.jar"]]}, manifest_path)

    def test_state_binds_exact_complete_run_file_set_and_hashes(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jar = root / "bench-JMH.jar"; jar.write_bytes(b"jar")
            jar_stat = jar.stat()
            state = {"benchmark_jar_path": str(jar.resolve()), "benchmark_jar_sha256": runner.sha256_file(jar),
                     "benchmark_jar_stat": [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size], "canonical_runs": []}
            for run_id in ("a", "b"):
                run_root = root / run_id; run_root.mkdir()
                for name in runner.REQUIRED_RUN_FILES:
                    (run_root / name).write_text(run_id + name)
                state["canonical_runs"].append({"run_id": run_id, "absolute_path": str(run_root), "files": {name: runner.sha256_file(run_root / name) for name in runner.REQUIRED_RUN_FILES}})
            comparison = root / "comparison.csv"; comparison.write_text("comparison")
            validation = root / "validation.json"; validation.write_text("{}")
            state.update({"comparison_path": str(comparison), "comparison_sha256": runner.sha256_file(comparison),
                          "comparison_validation_path": str(validation), "comparison_validation_sha256": runner.sha256_file(validation)})
            runner.verify_state_inputs(state, root / "state.json")
            (root / "a" / "extra.txt").write_text("extra")
            with self.assertRaisesRegex(ValueError, "file set"):
                runner.verify_state_inputs(state, root / "state.json")

    def test_successful_replacement_records_one_backup_and_old_manifest_drift_fails(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); destination = root / "dest"; destination.mkdir()
            (destination / "old").write_text("old")
            new = root / "new"; new.mkdir(); (new / "new").write_text("new")
            backup_root = root / "backups"
            backup = runner.atomic_replace_promoted(new, destination, backup_root)
            self.assertTrue((destination / "new").is_file())
            self.assertTrue((backup / "old").is_file())
            with self.assertRaisesRegex(ValueError, "sha256"):
                runner.verify_manifest_files(
                    {"schema_version": 1, "files": [{"path": "dest/new", "sha256": "0" * 64}]}, root, root / "manifest.json"
                )

    def test_rollback_bundle_generations_are_immutable_ordered_and_authenticated(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            old = archive / "comparison.csv"
            old.write_text("method,verdict\nserializerEncodeDirectOptimized,regressed\nserializerEncodeHeapOptimized,regressed\n")
            decision = runner.make_rollback_decision(
                "serializer_encode", ["serializerEncodeDirectOptimized", "serializerEncodeHeapOptimized"], "c", "t", archive, [old], 1, "now", "post", None, "post-tree"
            )
            bundle_path = runner.write_rollback_bundle(root, [decision], predecessor=None)
            loaded = runner.authenticate_rollback_bundle(bundle_path)
            self.assertEqual(1, loaded["generation"])
            self.assertEqual("serializer_encode", loaded["decisions"][0]["dispatch"])
            with self.assertRaisesRegex(ValueError, "duplicate"):
                runner.write_rollback_bundle(root, [decision, decision], predecessor=None)
            payload = json.loads(bundle_path.read_text()); payload["decisions"][0]["dispatch"] = "serializer_decode"
            bundle_path.write_text(json.dumps(payload))
            with self.assertRaisesRegex(ValueError, "sha256"):
                runner.authenticate_rollback_bundle(bundle_path)

    def test_normalized_profiles_match_validator_facing_contract(self):
        canonical = runner.normalized_profile("canonical")
        self.assertEqual(2, canonical["forks"])
        self.assertEqual(3, canonical["warmups"])
        self.assertEqual(5, canonical["measurements"])
        self.assertEqual("1 s", canonical["warmup_time"])
        self.assertEqual(["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"], canonical["jvm_args"])
        smoke = runner.normalized_profile("smoke")
        self.assertEqual((1, 1, 1), (smoke["forks"], smoke["warmups"], smoke["measurements"]))

    def test_chained_rollback_bundle_requires_every_immutable_generation(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            first_archive = root / "archive-one"; first_archive.mkdir()
            first_csv = first_archive / "comparison.csv"
            first_csv.write_text("method,verdict\nserializerEncodeDirectOptimized,regressed\nserializerEncodeHeapOptimized,regressed\n")
            first_decision = runner.make_rollback_decision(
                "serializer_encode", list(runner.DISPATCH_CELLS["serializer_encode"]),
                "c1", "t1", first_archive, [first_csv], 1, "one", "p1", None, "pt1",
            )
            first_bundle = runner.write_rollback_bundle(root, [first_decision])
            second_archive = root / "archive-two"; second_archive.mkdir()
            second_csv = second_archive / "comparison.csv"
            second_csv.write_text("method,verdict\nserializerDecodeDirectOptimized,regressed\nserializerDecodeHeapOptimized,regressed\n")
            second_decision = runner.make_rollback_decision(
                "serializer_decode", list(runner.DISPATCH_CELLS["serializer_decode"]),
                "c2", "t2", second_archive, [second_csv], 2, "two", "p2", "p1", "pt2",
            )
            second_bundle = runner.write_rollback_bundle(root, [second_decision], predecessor=first_bundle)
            chain = runner.authenticate_rollback_bundle_chain(second_bundle)
            self.assertEqual([1, 2], [bundle["generation"] for _, bundle in chain])
            first_bundle.unlink()
            with self.assertRaisesRegex(ValueError, "predecessor"):
                runner.authenticate_rollback_bundle_chain(second_bundle)

    def test_run_captures_validator_schema_and_rejects_run_directory_collision(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); (root / "jars").mkdir(); (root / "gradlew").write_text("")
            jar = root / "jars" / "bench-JMH.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("META-INF/maven/org.openjdk.jmh/jmh-core/pom.properties", "version=1.37\n")
            state_path = root / "state.json"; runner.resolve_jar(root / "jars", state_path)
            config = {
                "allowed_class_prefixes": ["io.example"], "direct_capacity": 20,
                "direct_initial_position": 0, "heap_capacity": 20, "heap_initial_position": 0,
                "matrix_version": "v1", "methods": sorted(validator.EXPECTED_METHODS),
                "payload_identity": "fixture", "payload_sha256": "payload",
                "redisson_codec_class": "R", "serializer_class": "S",
                "target_headroom": 2, "target_start": 1,
            }
            metadata = {
                "schema_version": 1, "matrix_version": "v1", "target_headroom": 2, "target_start": 1,
                "payload_size": 12, "payload_sha256": "payload",
                "config_sha256": validator.config_sha256(config),
                "config_json": validator.canonical_config_json(config),
            }
            metadata_bytes = (json.dumps(metadata, separators=(",", ":")) + "\n").encode()
            jar_derived_paths = []
            metadata_swap_probed = []

            def command(argv, **_kwargs):
                if argv[:2] == ["git", "status"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[:2] == ["git", "rev-parse"]:
                    value = b"tree\n" if argv[-1] == "HEAD^{tree}" else b"commit\n"
                    return subprocess.CompletedProcess(argv, 0, stdout=value, stderr=b"")
                if argv[:2] == ["java", "-cp"] and argv[3:] == [runner.METADATA_MAIN, "--json"]:
                    jar_derived_paths.append(argv[2])
                    stable_hash = runner.sha256_file(Path(argv[2]))
                    if not metadata_swap_probed:
                        held = root / "held-pinned.jar"; attacker = root / "attacker.jar"
                        attacker.write_bytes(b"attacker metadata")
                        jar.rename(held); attacker.rename(jar)
                        try:
                            self.assertNotEqual(runner.sha256_file(jar), stable_hash)
                            self.assertEqual(json.loads(state_path.read_text())["benchmark_jar_sha256"], stable_hash)
                        finally:
                            jar.unlink(); held.rename(jar)
                        metadata_swap_probed.append(True)
                    self.assertEqual(runner.sha256_file(jar), stable_hash)
                    return subprocess.CompletedProcess(argv, 0, stdout=metadata_bytes, stderr=b"")
                if argv[:2] == ["java", "-XshowSettings:properties"]:
                    settings = b"    java.vendor = Vendor\n    java.version = 21.0.1\n    java.vm.name = VM\n    java.vm.version = 21+1\n"
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=settings)
                if argv[0].endswith("gradlew"):
                    return subprocess.CompletedProcess(argv, 0, stdout=b"Gradle 9.1.0\n", stderr=b"")
                if argv[:3] == ["pmset", "-g", "batt"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"Now drawing from 'AC Power'\n", stderr=b"")
                if argv[:2] == ["java", "-jar"]:
                    jar_derived_paths.append(argv[2])
                    Path(argv[argv.index("-rff") + 1]).write_text(json.dumps(complete_jmh_records()))
                    return subprocess.CompletedProcess(argv, 0, stdout=b"jmh out\n", stderr=b"")
                if len(argv) > 2 and argv[2] in ("run", "compare") and argv[1].endswith("validate-jmh.py"):
                    return subprocess.run(argv, **_kwargs)
                return subprocess.CompletedProcess(argv, 127, stdout=b"", stderr=("unexpected " + repr(argv)).encode())

            with contextlib.redirect_stdout(io.StringIO()):
                run_dir = runner.run_benchmark(
                    state_path, "canonical", root / "output", "run-fixed", "absent",
                    command_runner=command, repo_root=root,
                )
            environment = json.loads((run_dir / "environment.json").read_text())
            self.assertEqual("1.37", environment["jmh_version"])
            self.assertEqual("Vendor", environment["jvm_vendor"])
            self.assertEqual("9.1.0", environment["gradle_version"])
            self.assertEqual("clean", environment["clean_status"])
            self.assertEqual({"phase": "initial", "stdout": "", "sha256": runner.sha256_bytes(b"")}, environment["initial_clean_status"])
            self.assertEqual({"phase": "pre-launch", "stdout": "", "sha256": runner.sha256_bytes(b"")}, environment["prelaunch_clean_status"])
            self.assertEqual("absent", environment["concurrent_heavy_work"])
            self.assertEqual(2, environment["forks"])
            self.assertEqual(runner.JVM_ARGS, environment["jvm_args"])
            self.assertEqual(runner.sha256_bytes(metadata_bytes), environment["metadata_stdout_sha256"])
            self.assertEqual(0, json.loads((run_dir / "argv.json").read_text())["exit_code"])
            self.assertIn("exit_code=0", (run_dir / "run.log").read_text())
            self.assertTrue(Path(environment["executed_jar_path"]).is_absolute())
            self.assertNotIn("/dev/fd/", environment["executed_jar_path"])
            self.assertEqual(str(jar.resolve()), environment["benchmark_jar_path"])
            self.assertEqual(json.loads(state_path.read_text())["benchmark_jar_stat"], environment["benchmark_jar_stat"])
            self.assertTrue(jar_derived_paths)
            self.assertEqual([True], metadata_swap_probed)
            self.assertEqual(1, len(set(jar_derived_paths)))
            self.assertFalse(any(run_dir.glob(".pinned-execution-*")))
            with self.assertRaisesRegex(ValueError, "run directory exists"):
                with contextlib.redirect_stdout(io.StringIO()):
                    runner.run_benchmark(
                        state_path, "canonical", root / "output", "run-fixed", "absent",
                        command_runner=command, repo_root=root,
                    )
            with contextlib.redirect_stdout(io.StringIO()):
                runner.run_benchmark(
                    state_path, "canonical", root / "output", "run-second", "absent",
                    command_runner=command, repo_root=root,
                )
            comparison = root / "comparison.csv"; comparison_validation = root / "comparison-validation.json"
            runner.compare_state(state_path, comparison, comparison_validation, command_runner=command)
            destination = root / "docs" / "raw"
            runner.promote_state(state_path, destination)
            runner.verify_promoted(state_path, destination, repo_root=root, command_runner=command)
            manifest_path = destination / "delivery-manifest.json"
            runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)
            self.assertTrue(json.loads(state_path.read_text())["promotion_status"] == "verified")

    def test_report_is_deterministic_and_positive_language_requires_accepted(self):
        manifest = {
            "schema_version": 1, "measurement": {"git_commit": "m", "tree_hash": "t"},
            "delivery": {"git_commit": "d"}, "commands": [["java", "-jar", "bench.jar"]],
            "results": [
                {"method": "good", "run": "run-a", "allocation_b_per_op": 94.0,
                 "throughput_ops_per_s": 10.0, "delta_percent": -6.0,
                 "verdict": "accepted", "reason": "threshold_met"},
                {"method": "mixed", "run": "run-a", "allocation_b_per_op": 99.0,
                 "throughput_ops_per_s": 9.0, "delta_percent": -1.0,
                 "verdict": "inconclusive", "reason": "mixed"},
            ],
            "rollback": {"decisions": []},
            "final_verdicts": {"good": "accepted", "mixed": "inconclusive"},
            "final_reasons": {"good": "threshold_met", "mixed": "mixed"},
        }
        first = runner.render_report_text(manifest)
        self.assertEqual(first, runner.render_report_text(manifest))
        self.assertIn("measured allocation reduction", first)
        self.assertIn("No positive reduction claim", first)
        with self.assertRaisesRegex(ValueError, "positive reduction language"):
            runner.validate_positive_language("mixed measured allocation reduction", manifest, Path("report.md"))
        manifest["rollback"] = {"decisions": [{"regressed_cells": ["good"]}]}
        with self.assertRaisesRegex(ValueError, "removed/ineligible"):
            runner.validate_positive_language(first, manifest, Path("report.md"))

    def test_run_exception_removes_private_execution_directory(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jars = root / "jars"; jars.mkdir()
            (jars / "bench-JMH.jar").write_bytes(b"jar")
            state_path = root / "state.json"; runner.resolve_jar(jars, state_path)
            observed_jar = []
            def command(argv, **_kwargs):
                if argv[:2] == ["git", "status"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[:2] == ["java", "-cp"]:
                    observed_jar.append(Path(argv[2]))
                    raise OSError("metadata probe failure")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            with self.assertRaisesRegex(OSError, "metadata probe failure"):
                runner.run_benchmark(
                    state_path, "smoke", root / "output", "failed-run", "absent",
                    command_runner=command, repo_root=root,
                )
            self.assertEqual(1, len(observed_jar))
            self.assertFalse(observed_jar[0].exists())
            self.assertFalse(any((root / "output" / "failed-run").glob(".pinned-execution-*")))

    def test_cleanup_requires_verified_state_bound_committed_manifest_and_current_head(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            manifest_path = build_complete_delivery(root)
            backup_root = root / "backups"; backup = backup_root / "backup-run-20260718T120000.000000Z-1234abcd"; backup.mkdir(parents=True)
            state_path = root / "state.json"
            state = {"promotion_status": "verified", "delivery_manifest_path": str(manifest_path.resolve()),
                     "delivery_manifest_sha256": runner.sha256_file(manifest_path),
                     "replacement_backup_path": str(backup.resolve())}
            runner.atomic_write_json(state_path, state)
            def git(argv, **_kwargs):
                if argv[:3] == ["git", "rev-parse", "--show-toplevel"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=(str(root) + "\n").encode(), stderr=b"")
                if argv[:2] == ["git", "show"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=manifest_path.read_bytes(), stderr=b"")
                if argv[:2] == ["git", "rev-parse"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"head\n", stderr=b"")
                if argv[:3] == ["git", "merge-base", "--is-ancestor"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            runner.cleanup_replacement_backup(state_path, manifest_path, "HEAD", backup_root, command_runner=git)
            self.assertFalse(backup.exists())

    def test_complete_delivery_rejects_mutated_provenance_commands_and_results(self):
        mutations = (
            ("measurement commit", lambda manifest: manifest["measurement"].update(git_commit="other")),
            ("measurement tree", lambda manifest: manifest["measurement"].update(tree_hash="other")),
            ("normalized commands", lambda manifest: manifest["commands"][0].append("--mutated")),
            ("final verdict", lambda manifest: manifest["final_verdicts"].update({next(iter(manifest["final_verdicts"])): "regressed"})),
            ("result verdict", lambda manifest: manifest["results"][0].update(verdict="regressed")),
        )
        for label, mutate in mutations:
            with self.subTest(label=label), tempfile.TemporaryDirectory() as td:
                root = Path(td); manifest_path = build_complete_delivery(root)
                runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)
                rewrite_manifest(manifest_path, mutate)
                with self.assertRaises(ValueError):
                    runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)

    def test_complete_delivery_requires_exactly_two_environment_files(self):
        for count in (1, 2):
            with self.subTest(removed=count), tempfile.TemporaryDirectory() as td:
                root = Path(td); manifest_path = build_complete_delivery(root)
                original = json.loads(manifest_path.read_text())
                for environment in sorted(manifest_path.parent.glob("run-*/environment.json"))[:count]:
                    environment.unlink()
                replacement = runner.create_delivery_manifest(
                    root, manifest_path.parent,
                    original["measurement"]["git_commit"], original["measurement"]["tree_hash"],
                    original["final_verdicts"], original["rollback"], original["commands"], original["results"],
                    original["delivery"]["git_commit"], original["measurement"]["benchmark_jar_sha256"],
                    original["final_reasons"],
                )
                runner.atomic_write_json(manifest_path, replacement)
                with self.assertRaisesRegex(ValueError, "environment count"):
                    runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)

    def test_committed_delivery_head_must_be_in_current_head_lineage(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); manifest_path = build_complete_delivery(root)
            def git(argv, **_kwargs):
                if argv[:2] == ["git", "show"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=manifest_path.read_bytes(), stderr=b"")
                if argv[:3] == ["git", "rev-parse", "HEAD"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"head\n", stderr=b"")
                if argv[:3] == ["git", "merge-base", "--is-ancestor"]:
                    return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"not ancestor")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            with self.assertRaisesRegex(ValueError, "not an ancestor"):
                runner.validate_committed(manifest_path, repo_root=root, command_runner=git)

    def test_fresh_rollback_resolution_requires_real_changed_post_removal_head(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            comparison = archive / "comparison.csv"
            comparison.write_text("method,verdict\nserializerEncodeDirectOptimized,regressed\nserializerEncodeHeapOptimized,regressed\n")
            decision = runner.make_rollback_decision(
                "serializer_encode", list(runner.DISPATCH_CELLS["serializer_encode"]),
                "old", "old-tree", archive, [comparison], 1, "now", "post", None, "post-tree",
            )
            bundle = runner.write_rollback_bundle(root, [decision])
            jars = root / "jars"; jars.mkdir(); (jars / "bench-JMH.jar").write_bytes(b"jar")

            def non_git(argv, **_kwargs):
                return subprocess.CompletedProcess(argv, 128, stdout=b"", stderr=b"not a git repository")
            with self.assertRaises(ValueError):
                runner.resolve_jar(jars, root / "non-git.json", bundle, command_runner=non_git, repo_root=root)

            def unchanged(argv, **_kwargs):
                if argv[-1] == "HEAD":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"old\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"post-tree\n", stderr=b"")
                return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
            with self.assertRaisesRegex(ValueError, "pre-removal head"):
                runner.resolve_jar(jars, root / "unchanged.json", bundle, command_runner=unchanged, repo_root=root)

            def changed(argv, **_kwargs):
                if argv[-1] == "HEAD":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"post\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"post-tree\n", stderr=b"")
                if argv[:3] == ["git", "merge-base", "--is-ancestor"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            state = runner.resolve_jar(jars, root / "changed.json", bundle, command_runner=changed, repo_root=root)
            self.assertEqual(("post", "post-tree"), (state["source_commit"], state["source_tree"]))

    def test_pinned_jar_stat_rejects_same_bytes_inode_swap_before_execution(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jars = root / "jars"; jars.mkdir()
            jar = jars / "bench-JMH.jar"; jar.write_bytes(b"same bytes")
            state_path = root / "state.json"; state = runner.resolve_jar(jars, state_path)
            replacement = root / "replacement.jar"; replacement.write_bytes(b"same bytes")
            original = root / "original.jar"; jar.rename(original); replacement.rename(jar)
            with self.assertRaisesRegex(ValueError, "JAR stat"):
                runner.prepare_private_execution_jar(state, state_path, root)

    def test_private_execution_path_rejects_swap_removal_and_permission_drift(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jar = root / "bench-JMH.jar"; jar.write_bytes(b"pinned")
            jar_stat = jar.stat()
            state = {"benchmark_jar_path": str(jar.resolve()), "benchmark_jar_sha256": runner.sha256_file(jar),
                     "benchmark_jar_stat": [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size]}
            private, identity = runner.prepare_private_execution_jar(state, root / "state.json", root)
            self.assertTrue(private.is_file()); self.assertNotIn("/dev/fd/", str(private))
            self.assertEqual(0o400, private.stat().st_mode & 0o777)
            self.assertEqual(0o500, private.parent.stat().st_mode & 0o777)
            held = root / "held.jar"; attacker = root / "attacker.jar"; attacker.write_bytes(b"attacker")
            try:
                os.chmod(private.parent, 0o700)
                private.rename(held); attacker.rename(private)
                os.chmod(private, 0o400); os.chmod(private.parent, 0o500)
                with self.assertRaisesRegex(ValueError, "identity"):
                    runner.verify_private_execution_jar(identity, root / "state.json")
            finally:
                runner.cleanup_private_execution_jar(identity)
                held.unlink(missing_ok=True)

            private, identity = runner.prepare_private_execution_jar(state, root / "state.json", root)
            os.chmod(private.parent, 0o700); private.unlink(); os.chmod(private.parent, 0o500)
            with self.assertRaisesRegex(ValueError, "lstat failed"):
                runner.verify_private_execution_jar(identity, root / "state.json")
            runner.cleanup_private_execution_jar(identity)

            private, identity = runner.prepare_private_execution_jar(state, root / "state.json", root)
            os.chmod(private, 0o600)
            with self.assertRaisesRegex(ValueError, "mode=600 expected=400"):
                runner.verify_private_execution_jar(identity, root / "state.json")
            runner.cleanup_private_execution_jar(identity)

    def test_private_execution_prepare_cleans_directory_and_fd_on_hash_or_open_failure(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); jar = root / "bench-JMH.jar"; jar.write_bytes(b"pinned")
            jar_stat = jar.stat()
            state = {"benchmark_jar_path": str(jar.resolve()), "benchmark_jar_sha256": runner.sha256_file(jar),
                     "benchmark_jar_stat": [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size]}
            original_hash = runner.sha256_file
            original_open = runner.os.open
            opened = []
            def tracking_open(*args, **kwargs):
                fd = original_open(*args, **kwargs); opened.append(fd); return fd
            def mismatched_private_hash(path):
                path = Path(path)
                if path.name == "benchmark-JMH.jar" and path.parent.name.startswith(".pinned-execution-"):
                    return "0" * 64
                return original_hash(path)
            runner.sha256_file = mismatched_private_hash
            runner.os.open = tracking_open
            try:
                with self.assertRaisesRegex(ValueError, "sha256"):
                    runner.prepare_private_execution_jar(state, root / "state.json", root)
            finally:
                runner.sha256_file = original_hash
                runner.os.open = original_open
            self.assertFalse(any(root.glob(".pinned-execution-*")))
            self.assertGreaterEqual(len(opened), 2)
            for fd in opened:
                with self.assertRaises(OSError):
                    os.fstat(fd)

            calls = []
            def fail_private_open(*args, **kwargs):
                calls.append(args[0])
                if len(calls) == 2:
                    raise OSError("forced private open failure")
                return original_open(*args, **kwargs)
            runner.os.open = fail_private_open
            try:
                with self.assertRaisesRegex(OSError, "forced private open failure"):
                    runner.prepare_private_execution_jar(state, root / "state.json", root)
            finally:
                runner.os.open = original_open
            self.assertFalse(any(root.glob(".pinned-execution-*")))

    def test_rollback_decision_rejects_symlink_to_external_artifact(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            external = root / "external.csv"
            external.write_text("method,verdict\nserializerEncodeDirectOptimized,regressed\nserializerEncodeHeapOptimized,regressed\n")
            linked = archive / "comparison.csv"; linked.symlink_to(external)
            with self.assertRaisesRegex(ValueError, "symlink"):
                runner.make_rollback_decision(
                    "serializer_encode", list(runner.DISPATCH_CELLS["serializer_encode"]),
                    "old", "old-tree", archive, [linked], 1, "now", "post", None, "post-tree",
                )

    def test_cli_failure_is_concise_without_traceback(self):
        with tempfile.TemporaryDirectory() as td:
            result = subprocess.run([sys.executable, str(HERE / "run-evidence.py"), "resolve-jar", "--jar-dir", td, "--state", str(Path(td) / "state.json")], capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("remediation:", result.stderr)
            self.assertNotIn("Traceback", result.stderr)

    def test_generated_run_ids_are_unique_and_match_contract(self):
        first = runner.generate_run_id(); second = runner.generate_run_id()
        self.assertNotEqual(first, second)
        self.assertRegex(first, r"^run-\d{8}T\d{6}\.\d{6}Z-[0-9a-f]{8}$")


if __name__ == "__main__":
    unittest.main()
