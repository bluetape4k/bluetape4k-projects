import importlib.util
import contextlib
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import time
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
        runner.atomic_write_json(run_root / "argv.json", {"schema_version": 1, "argv": argv, "started_at": "s", "ended_at": "e", "exit_code": 0, "log_limit_exceeded": False})
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


def build_rollback_state(root, comparison_rows, environments=(("old", "old-tree"), ("old", "old-tree"))):
    root = Path(root); evidence = root / "evidence"; evidence.mkdir()
    jar = root / "bench-JMH.jar"; jar.write_bytes(b"jar"); jar_stat = jar.stat()
    runs = []
    for index, (commit, tree) in enumerate(environments):
        run = evidence / ("run-" + chr(ord("a") + index)); run.mkdir()
        for name in runner.REQUIRED_RUN_FILES: (run / name).write_text(name)
        runner.atomic_write_json(run / "environment.json", {"git_commit": commit, "tree_hash": tree})
        runs.append({"absolute_path": str(run), "files": {name: runner.sha256_file(run / name) for name in runner.REQUIRED_RUN_FILES}})
    comparison = evidence / "comparison.csv"; comparison.write_text("method,verdict\n" + "".join("{},{}\n".format(*row) for row in comparison_rows))
    validation = evidence / "validation.json"; validation.write_text("{}")
    state_path = evidence / "state.json"
    runner.atomic_write_json(state_path, {
        "schema_version": 1, "promotable": True, "canonical_runs": runs,
        "benchmark_jar_path": str(jar.resolve()), "benchmark_jar_sha256": runner.sha256_file(jar),
        "benchmark_jar_stat": [jar_stat.st_dev, jar_stat.st_ino, jar_stat.st_size],
        "comparison_path": str(comparison), "comparison_sha256": runner.sha256_file(comparison),
        "comparison_validation_path": str(validation), "comparison_validation_sha256": runner.sha256_file(validation),
    })
    return state_path, comparison, validation


class EvidenceRunnerTest(unittest.TestCase):
    def test_record_rollback_prepares_actual_regressed_subset_then_finalize_is_idempotent(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            state_path, comparison, validation = build_rollback_state(root, (
                ("serializerDecodeHeapOptimized", "inconclusive"),
                ("serializerDecodeDirectOptimized", "regressed"),
            ))

            def git_before(argv, **_kwargs):
                if argv[1:3] == ["status", "--porcelain=v1"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[-1] == "HEAD":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"old\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"old-tree\n", stderr=b"")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")

            preparation_path = runner.record_rollback(
                state_path, ["serializer_decode"], root / "rollback", command_runner=git_before, repo_root=root,
            )
            preparation = runner.authenticate_rollback_preparation(preparation_path)
            decision = preparation["decisions"][0]
            self.assertEqual(2, preparation["schema_version"])
            self.assertEqual(["serializerDecodeDirectOptimized"], decision["regressed_cells"])
            self.assertEqual(sorted(runner.DISPATCH_CELLS["serializer_decode"]), decision["removed_cells"])
            state = json.loads(state_path.read_text())
            self.assertEqual("prepared", state["rollback_status"])
            self.assertFalse(state["promotable"])
            self.assertEqual(preparation_path, runner.record_rollback(
                state_path, ["serializer_decode"], root / "rollback", command_runner=git_before, repo_root=root,
            ))

            def git_after(argv, **_kwargs):
                if argv[1] == "show":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"class ProtobufSerializer : BinarySerializer", stderr=b"")
                if argv[1:3] == ["status", "--porcelain=v1"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[-1] == "HEAD":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"post\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}":
                    return subprocess.CompletedProcess(argv, 0, stdout=b"post-tree\n", stderr=b"")
                if argv[1:3] == ["merge-base", "--is-ancestor"]:
                    return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")

            bundle_path = runner.finalize_rollback(
                preparation_path, command_runner=git_after, repo_root=root,
                removal_verifier=lambda *_args, **_kwargs: True,
            )
            self.assertEqual(bundle_path, runner.finalize_rollback(
                preparation_path, command_runner=git_after, repo_root=root,
                removal_verifier=lambda *_args, **_kwargs: True,
            ))
            bundle = runner.authenticate_rollback_bundle(bundle_path)
            self.assertEqual(runner.sha256_file(preparation_path), bundle["preparation_sha256"])
            self.assertEqual(sorted(runner.DISPATCH_CELLS["serializer_decode"]), bundle["decisions"][0]["removed_cells"])
            tampered = json.loads(preparation_path.read_text()); tampered["decisions"][0]["timestamp"] = "tampered"
            preparation_path.write_text(json.dumps(tampered))
            with self.assertRaisesRegex(ValueError, "preparation sha256"):
                runner.authenticate_rollback_bundle(bundle_path)

    def test_record_rollback_rejects_environment_and_state_hash_drift(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); state_path, comparison, _ = build_rollback_state(
                root, (("redissonDecodeContiguousOptimized", "regressed"),),
                environments=(("old", "tree"), ("other", "tree")),
            )
            with self.assertRaisesRegex(ValueError, "measurement identity"):
                runner.record_rollback(state_path, ["redisson_contiguous"], root / "rollback", repo_root=root)
            state = json.loads(state_path.read_text()); state["canonical_runs"][1] = state["canonical_runs"][0]
            runner.atomic_write_json(state_path, state)
            comparison.write_text("method,verdict\nredissonDecodeContiguousOptimized,inconclusive\n")
            with self.assertRaisesRegex(ValueError, "state-bound sha256"):
                runner.record_rollback(state_path, ["redisson_contiguous"], root / "rollback", repo_root=root)

    def test_record_rollback_requires_clean_exact_measurement_head(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); state, _, _ = build_rollback_state(
                root, (("redissonDecodeContiguousOptimized", "regressed"),),
                environments=(("old", "tree"), ("old", "tree")),
            )
            def dirty(argv, **_kwargs):
                if argv[1] == "status": return subprocess.CompletedProcess(argv, 0, stdout=b" M dirty\n", stderr=b"")
                return subprocess.CompletedProcess(argv, 0, stdout=b"other\n", stderr=b"")
            with self.assertRaisesRegex(ValueError, "clean"):
                runner.record_rollback(state, ["redisson_contiguous"], root / "rollback", command_runner=dirty, repo_root=root)

            preparation = root / ("rollback-preparation-g1-" + "0" * 64 + ".json")
            preparation.write_text(json.dumps({"schema_version": 2, "kind": "rollback_preparation"}))
            with self.assertRaisesRegex(ValueError, "finalized v2"):
                runner.authenticate_rollback_bundle(preparation)
            legacy = root / "rollback-bundle-g1-legacy.json"
            legacy.write_text(json.dumps({"schema_version": 1, "decisions": []}))
            with self.assertRaisesRegex(ValueError, "v1 artifacts must be recreated"):
                runner.authenticate_rollback_bundle(legacy)
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

    def test_metadata_rejects_debug_and_known_fallback_noise_but_allows_benign_stderr(self):
        jar = Path("/benchmark-JMH.jar")
        payload = b'{"matrix_version":"v1"}\n'

        for stderr in (
            b"12:00:00.000 DEBUG logger - noisy diagnostic\n",
            b"Protobuf deserialization failed; delegating to the trusted fallback serializer.\n",
        ):
            with self.subTest(stderr=stderr), self.assertRaisesRegex(ValueError, "metadata stderr"):
                runner.capture_metadata(
                    jar,
                    command_runner=lambda *_args, **_kwargs: subprocess.CompletedProcess([], 0, stdout=payload, stderr=stderr),
                )

        captured = runner.capture_metadata(
            jar,
            command_runner=lambda *_args, **_kwargs: subprocess.CompletedProcess(
                [], 0, stdout=payload, stderr=b"OpenJDK warning: CDS is disabled\n",
            ),
        )
        self.assertIn("CDS is disabled", captured["stderr"])

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

    def test_execute_logged_caps_output_and_terminates_the_owned_process(self):
        with tempfile.TemporaryDirectory() as td:
            run_dir = Path(td)
            limit = getattr(runner, "MAX_RUN_LOG_BYTES", 8 * 1024 * 1024)
            script = "import os,time; os.write(1, b'x' * {}); time.sleep(2)".format(limit + 65536)
            started = time.monotonic()
            with self.assertRaisesRegex(ValueError, "log size limit"):
                runner.execute_logged([sys.executable, "-c", script], run_dir)
            self.assertLess(time.monotonic() - started, 1.5)
            self.assertLessEqual((run_dir / "run.log").stat().st_size, limit)
            argv = json.loads((run_dir / "argv.json").read_text())
            self.assertTrue(argv["log_limit_exceeded"])
            self.assertEqual(getattr(runner, "RUNNER_FAILURE_EXIT_CODE", 125), argv["exit_code"])
            log_tail = (run_dir / "run.log").read_bytes()[-256:]
            self.assertIn(runner.RUN_LOG_LIMIT_MARKER.strip(), log_tail)
            self.assertIn(b"exit_code=", log_tail)
            self.assertFalse(any(path.name.startswith(".run.log.") for path in run_dir.iterdir()))

    def test_execute_logged_publishes_bounded_evidence_before_reraising_anchor_protocol_failure(self):
        original = runner._execute_with_anchor
        protocol_error_type = getattr(runner, "AnchorProtocolError", ValueError)
        for detail in (
            "anchor status pipe closed before target exit status",
            "anchor target exit status is malformed",
            "anchor target exit status exceeded 32 bytes",
        ):
            with self.subTest(detail=detail), tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                cause = RuntimeError("anchor cleanup completed")
                try:
                    raise protocol_error_type(detail) from cause
                except protocol_error_type as failure:
                    protocol_failure = failure

                def fail_after_cleanup(*_args, **_kwargs):
                    raise protocol_failure.with_traceback(protocol_failure.__traceback__)

                runner._execute_with_anchor = fail_after_cleanup
                try:
                    with self.assertRaises(protocol_error_type) as raised:
                        runner.execute_logged([sys.executable, "-c", "pass"], run_dir)
                finally:
                    runner._execute_with_anchor = original

                self.assertIs(protocol_failure, raised.exception)
                self.assertIs(cause, raised.exception.__cause__)
                argv = json.loads((run_dir / "argv.json").read_text())
                expected_exit = getattr(runner, "RUNNER_PROTOCOL_FAILURE_EXIT_CODE", 126)
                self.assertEqual(expected_exit, argv["exit_code"])
                self.assertFalse(argv["log_limit_exceeded"])
                log = (run_dir / "run.log").read_bytes()
                self.assertLessEqual(len(log), runner.MAX_RUN_LOG_BYTES)
                self.assertIn(detail.encode("utf-8"), log)
                self.assertTrue(log.endswith("exit_code={}\n".format(expected_exit).encode("ascii")))
                self.assertFalse(any(path.name.startswith(".run.log.") for path in run_dir.iterdir()))

    def test_execute_logged_publishes_bounded_evidence_before_reraising_owned_oserror(self):
        original = runner.os.pipe
        cause = RuntimeError("anchor pipe setup failed")
        try:
            raise OSError("anchor selector read failed") from cause
        except OSError as failure:
            owned_failure = failure

        def fail_owned_pipe():
            raise owned_failure.with_traceback(owned_failure.__traceback__)

        runner.os.pipe = fail_owned_pipe
        try:
            with tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                with self.assertRaises(runner.AnchorExecutionError) as raised:
                    runner.execute_logged([sys.executable, "-c", "pass"], run_dir)

                self.assertIs(owned_failure, raised.exception.__cause__)
                self.assertIs(cause, raised.exception.__cause__.__cause__)
                argv = json.loads((run_dir / "argv.json").read_text())
                self.assertEqual(127, argv["exit_code"])
                self.assertFalse(argv["log_limit_exceeded"])
                log = (run_dir / "run.log").read_bytes()
                self.assertLessEqual(len(log), runner.MAX_RUN_LOG_BYTES)
                self.assertIn(b"anchor selector read failed", log)
                self.assertTrue(log.endswith(b"exit_code=127\n"))
                self.assertFalse(any(path.name.startswith(".run.log.") for path in run_dir.iterdir()))
        finally:
            runner.os.pipe = original

    def test_execute_logged_never_classifies_log_write_permission_errors_as_owned(self):
        for persistent in (False, True):
            with self.subTest(persistent=persistent), tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                original_fdopen = runner.os.fdopen
                fdopen_calls = []
                cause = RuntimeError("artifact filesystem denied the write")
                try:
                    raise PermissionError("run.log write denied") from cause
                except PermissionError as failure:
                    denied = failure

                class FailingLogStream:
                    def __init__(self, raw):
                        self.raw = raw
                        self.failed = False

                    def __enter__(self):
                        return self

                    def __exit__(self, *args):
                        return self.raw.__exit__(*args)

                    def __getattr__(self, name):
                        return getattr(self.raw, name)

                    def write(self, payload):
                        if persistent or not self.failed:
                            self.failed = True
                            raise denied.with_traceback(denied.__traceback__)
                        return self.raw.write(payload)

                def fail_only_run_log(fd, mode):
                    raw = original_fdopen(fd, mode)
                    fdopen_calls.append(fd)
                    return FailingLogStream(raw) if len(fdopen_calls) == 1 else raw

                runner.os.fdopen = fail_only_run_log
                try:
                    with self.assertRaises(PermissionError) as raised:
                        runner.execute_logged([sys.executable, "-c", "print('payload')"], run_dir)
                finally:
                    runner.os.fdopen = original_fdopen

                self.assertIs(denied, raised.exception)
                self.assertIs(cause, raised.exception.__cause__)
                self.assertFalse((run_dir / "run.log").exists())
                self.assertFalse((run_dir / "argv.json").exists())
                self.assertFalse(any(path.name.startswith(".run.log.") for path in run_dir.iterdir()))

    def test_execute_logged_does_not_capture_unowned_value_errors(self):
        original = runner._execute_with_anchor

        def fail_unrelated(*_args, **_kwargs):
            raise ValueError("unrelated caller defect")

        runner._execute_with_anchor = fail_unrelated
        try:
            with tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                with self.assertRaisesRegex(ValueError, "unrelated caller defect"):
                    runner.execute_logged([sys.executable, "-c", "pass"], run_dir)
                self.assertFalse((run_dir / "run.log").exists())
                self.assertFalse((run_dir / "argv.json").exists())
        finally:
            runner._execute_with_anchor = original

    def test_execute_logged_does_not_mask_artifact_permission_errors(self):
        original = runner._publish_temporary_no_clobber
        denied = PermissionError("diagnostic publication denied")

        def reject_publish(*_args, **_kwargs):
            raise denied

        runner._publish_temporary_no_clobber = reject_publish
        try:
            with tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                completed = lambda *_args, **_kwargs: subprocess.CompletedProcess([], 0, stdout=b"", stderr=b"")
                with self.assertRaises(PermissionError) as raised:
                    runner.execute_logged(["java"], run_dir, command_runner=completed)
                self.assertIs(denied, raised.exception)
                self.assertFalse((run_dir / "argv.json").exists())
                self.assertFalse(any(path.name.startswith(".run.log.") for path in run_dir.iterdir()))
        finally:
            runner._publish_temporary_no_clobber = original

    def test_anchor_status_parser_classifies_owned_protocol_failures(self):
        cases = (
            (b"", "closed before target exit status"),
            (b"invalid\n", "status is malformed"),
            (b"1" * 33, "exceeded 32 bytes"),
        )
        for payload, message in cases:
            with self.subTest(message=message):
                status_read, status_write = os.pipe()
                if payload:
                    os.write(status_write, payload)
                os.close(status_write)

                class Key:
                    fd = status_read
                    fileobj = status_read
                    data = "status"

                class Selector:
                    def select(self, _timeout):
                        return [(Key(), None)]

                    def unregister(self, _fileobj):
                        pass

                state = {
                    "written": 0,
                    "limit_exceeded": False,
                    "status": bytearray(),
                    "target_exit_code": None,
                    "status_fd_open": True,
                }
                try:
                    with self.assertRaisesRegex(runner.AnchorProtocolError, message):
                        runner._consume_anchor_events(Selector(), io.BytesIO(), 1024, state)
                finally:
                    if state["status_fd_open"]:
                        os.close(status_read)

    def test_termination_signals_group_before_reaping_anchor(self):
        class Anchor:
            pid = 4321
            returncode = 0

            def poll(self):
                events.append("poll")
                return None

            def wait(self, timeout=None):
                events.append("wait")
                return self.returncode

        events = []
        original = runner.os.killpg

        def record_killpg(process_group_id, requested_signal):
            events.append((process_group_id, requested_signal))
            if requested_signal == 0:
                return None

        runner.os.killpg = record_killpg
        try:
            runner._terminate_owned_process(Anchor())
        finally:
            runner.os.killpg = original
        self.assertEqual(
            [(4321, runner.signal.SIGTERM), (4321, runner.signal.SIGKILL), "wait"],
            events,
        )

    def test_normal_anchor_drain_failure_never_resignals_reaped_group(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

            def wait(self, timeout=None):
                events.append("wait")
                return 0

        events = []
        original_popen = runner.subprocess.Popen
        original_killpg = runner.os.killpg
        original_consume = runner._consume_anchor_events
        original_drain = runner._drain_anchor_events

        def complete_target(_selector, _stream, _limit, state, timeout=None, allow_missing_status=False):
            state["target_exit_code"] = 0
            return True

        def fail_drain(*_args, **_kwargs):
            raise RuntimeError("post-reap drain failed")

        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner.os.killpg = lambda pgid, requested_signal: events.append((pgid, requested_signal))
        runner._consume_anchor_events = complete_target
        runner._drain_anchor_events = fail_drain
        try:
            with self.assertRaisesRegex(RuntimeError, "post-reap drain failed"):
                runner._execute_with_anchor(["target"], io.BytesIO(), 1024, ())
        finally:
            runner.subprocess.Popen = original_popen
            runner.os.killpg = original_killpg
            runner._consume_anchor_events = original_consume
            runner._drain_anchor_events = original_drain

        self.assertEqual([(4321, runner.signal.SIGKILL), "wait"], events)

    def test_persistent_anchor_read_failure_uses_one_cleanup_and_preserves_first_cause(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

            def wait(self, timeout=None):
                events.append("wait")
                return 0

        first_cause = OSError("first selector read failure")
        repeated_cause = OSError("repeated selector read failure")
        causes = iter((first_cause, repeated_cause))
        events = []
        original_popen = runner.subprocess.Popen
        original_killpg = runner.os.killpg
        original_consume = runner._consume_anchor_events

        def persistent_read_failure(*_args, **_kwargs):
            cause = next(causes)
            raise runner.AnchorExecutionError(str(cause)) from cause

        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner.os.killpg = lambda pgid, requested_signal: events.append((pgid, requested_signal))
        runner._consume_anchor_events = persistent_read_failure
        try:
            with self.assertRaises(runner.AnchorExecutionError) as raised:
                runner._execute_with_anchor(["target"], io.BytesIO(), 1024, ())
        finally:
            runner.subprocess.Popen = original_popen
            runner.os.killpg = original_killpg
            runner._consume_anchor_events = original_consume

        self.assertIs(first_cause, raised.exception.__cause__)
        self.assertEqual(
            [(4321, runner.signal.SIGTERM), (4321, runner.signal.SIGKILL), "wait"],
            events,
        )

    def test_primary_anchor_failure_survives_cleanup_failure_without_retry(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

        first_cause = OSError("first selector read failure")
        cleanup_calls = []
        original_popen = runner.subprocess.Popen
        original_consume = runner._consume_anchor_events
        original_terminate = runner._terminate_owned_process

        def primary_failure(*_args, **_kwargs):
            raise runner.AnchorExecutionError(str(first_cause)) from first_cause

        def cleanup_failure(*_args, **_kwargs):
            cleanup_calls.append("cleanup")
            raise RuntimeError("cleanup failed")

        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner._consume_anchor_events = primary_failure
        runner._terminate_owned_process = cleanup_failure
        try:
            with self.assertRaises(runner.AnchorExecutionError) as raised:
                runner._execute_with_anchor(["target"], io.BytesIO(), 1024, ())
        finally:
            runner.subprocess.Popen = original_popen
            runner._consume_anchor_events = original_consume
            runner._terminate_owned_process = original_terminate

        self.assertIs(first_cause, raised.exception.__cause__)
        self.assertEqual(["cleanup"], cleanup_calls)

    def test_protocol_failure_drain_error_still_kills_and_reaps_anchor_once(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

            def wait(self, timeout=None):
                events.append("wait")
                return 0

        cause = RuntimeError("malformed status source")
        try:
            raise runner.AnchorProtocolError("anchor target exit status is malformed") from cause
        except runner.AnchorProtocolError as failure:
            protocol_failure = failure

        events = []
        original_popen = runner.subprocess.Popen
        original_killpg = runner.os.killpg
        original_consume = runner._consume_anchor_events
        original_drain = runner._drain_anchor_events

        def fail_protocol(*_args, **_kwargs):
            raise protocol_failure.with_traceback(protocol_failure.__traceback__)

        def fail_drain(*_args, **_kwargs):
            raise RuntimeError("cleanup drain failed")

        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner.os.killpg = lambda pgid, requested_signal: events.append((pgid, requested_signal))
        runner._consume_anchor_events = fail_protocol
        runner._drain_anchor_events = fail_drain
        try:
            with self.assertRaises(runner.AnchorProtocolError) as raised:
                runner._execute_with_anchor(["target"], io.BytesIO(), 1024, ())
        finally:
            runner.subprocess.Popen = original_popen
            runner.os.killpg = original_killpg
            runner._consume_anchor_events = original_consume
            runner._drain_anchor_events = original_drain

        self.assertIs(protocol_failure, raised.exception)
        self.assertIs(cause, raised.exception.__cause__)
        self.assertEqual(
            [(4321, runner.signal.SIGTERM), (4321, runner.signal.SIGKILL), "wait"],
            events,
        )

    def test_primary_anchor_failure_survives_selector_close_failure(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

            def wait(self, timeout=None):
                events.append("wait")
                return 0

        close_failure = OSError("selector close failed")
        first_cause = OSError("first selector read failure")
        events = []

        class Selector:
            def register(self, *_args):
                pass

            def close(self):
                raise close_failure

        original_selector = runner.selectors.DefaultSelector
        original_popen = runner.subprocess.Popen
        original_killpg = runner.os.killpg
        original_consume = runner._consume_anchor_events

        def primary_failure(*_args, **_kwargs):
            raise runner.AnchorExecutionError(str(first_cause)) from first_cause

        runner.selectors.DefaultSelector = Selector
        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner.os.killpg = lambda pgid, requested_signal: events.append((pgid, requested_signal))
        runner._consume_anchor_events = primary_failure
        try:
            with self.assertRaises(runner.AnchorExecutionError) as raised:
                runner._execute_with_anchor(["target"], io.BytesIO(), 1024, ())
        finally:
            runner.selectors.DefaultSelector = original_selector
            runner.subprocess.Popen = original_popen
            runner.os.killpg = original_killpg
            runner._consume_anchor_events = original_consume

        self.assertIs(first_cause, raised.exception.__cause__)
        self.assertEqual(
            [(4321, runner.signal.SIGTERM), (4321, runner.signal.SIGKILL), "wait"],
            events,
        )

    def test_selector_close_oserror_is_owned_when_no_primary_failure_exists(self):
        read_fd, write_fd = os.pipe()
        os.close(write_fd)

        class Anchor:
            pid = 4321
            stdout = os.fdopen(read_fd, "rb", buffering=0)

            def wait(self, timeout=None):
                events.append("wait")
                return 0

        close_failure = OSError("selector close failed")
        events = []

        class Selector:
            def register(self, *_args):
                pass

            def close(self):
                raise close_failure

        original_selector = runner.selectors.DefaultSelector
        original_popen = runner.subprocess.Popen
        original_killpg = runner.os.killpg
        original_consume = runner._consume_anchor_events
        original_drain = runner._drain_anchor_events

        def complete_target(_selector, _stream, _limit, state, timeout=None, allow_missing_status=False):
            state["target_exit_code"] = 0
            return True

        runner.selectors.DefaultSelector = Selector
        runner.subprocess.Popen = lambda *_args, **_kwargs: Anchor()
        runner.os.killpg = lambda pgid, requested_signal: events.append((pgid, requested_signal))
        runner._consume_anchor_events = complete_target
        runner._drain_anchor_events = lambda *_args, **_kwargs: None
        try:
            with tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                with self.assertRaises(runner.AnchorExecutionError) as raised:
                    runner.execute_logged(["target"], run_dir)
                self.assertIs(close_failure, raised.exception.__cause__)
                argv = json.loads((run_dir / "argv.json").read_text())
                self.assertEqual(127, argv["exit_code"])
        finally:
            runner.selectors.DefaultSelector = original_selector
            runner.subprocess.Popen = original_popen
            runner.os.killpg = original_killpg
            runner._consume_anchor_events = original_consume
            runner._drain_anchor_events = original_drain

        self.assertEqual([(4321, runner.signal.SIGKILL), "wait"], events)

    def test_selector_creation_failure_survives_status_descriptor_close_failures(self):
        status_read, status_write = os.pipe()
        original_pipe = runner.os.pipe
        original_close = runner.os.close
        original_selector = runner.selectors.DefaultSelector
        root_cause = RuntimeError("selector backend unavailable")
        try:
            raise OSError("selector creation failed") from root_cause
        except OSError as failure:
            selector_failure = failure
        descriptor_failure = OSError("status descriptor close failed")

        def owned_pipe():
            return status_read, status_write

        def fail_selector_creation():
            raise selector_failure.with_traceback(selector_failure.__traceback__)

        def fail_status_close(fd):
            if fd in (status_read, status_write):
                raise descriptor_failure
            return original_close(fd)

        runner.os.pipe = owned_pipe
        runner.os.close = fail_status_close
        runner.selectors.DefaultSelector = fail_selector_creation
        try:
            with tempfile.TemporaryDirectory() as td:
                run_dir = Path(td)
                with self.assertRaises(runner.AnchorExecutionError) as raised:
                    runner.execute_logged(["target"], run_dir)
                self.assertIs(selector_failure, raised.exception.__cause__)
                self.assertIs(root_cause, raised.exception.__cause__.__cause__)
                argv = json.loads((run_dir / "argv.json").read_text())
                self.assertEqual(127, argv["exit_code"])
                self.assertFalse(argv["log_limit_exceeded"])
                self.assertEqual({"argv.json", "run.log"}, {path.name for path in run_dir.iterdir()})
        finally:
            runner.os.pipe = original_pipe
            runner.os.close = original_close
            runner.selectors.DefaultSelector = original_selector
            original_close(status_read)
            original_close(status_write)

    def test_anchor_returns_target_exit_without_waiting_for_stdout_descendant(self):
        with tempfile.TemporaryDirectory() as td:
            run_dir = Path(td)
            child = (
                "import subprocess,sys; "
                "p=subprocess.Popen([sys.executable,'-c','import time; time.sleep(2)']); "
                "print('descendant_pid=%d' % p.pid, flush=True)"
            )
            started = time.monotonic()
            record = runner.execute_logged([sys.executable, "-c", child], run_dir)
            self.assertLess(time.monotonic() - started, 1.0)
            self.assertEqual(0, record["exit_code"])
            match = re.search(rb"descendant_pid=(\d+)", (run_dir / "run.log").read_bytes())
            self.assertIsNotNone(match)
            descendant_pid = int(match.group(1))
            for _ in range(50):
                try:
                    os.kill(descendant_pid, 0)
                except ProcessLookupError:
                    break
                time.sleep(0.01)
            else:
                self.fail("stdout-inheriting descendant remained alive")

    def test_anchor_preserves_requested_pass_fds(self):
        with tempfile.TemporaryDirectory() as td:
            read_fd, write_fd = os.pipe()
            try:
                child = "import os; os.write({}, b'passed')".format(write_fd)
                runner.execute_logged(
                    [sys.executable, "-c", child], Path(td), pass_fds=(write_fd,),
                )
            finally:
                os.close(write_fd)
            try:
                self.assertEqual(b"passed", os.read(read_fd, 16))
            finally:
                os.close(read_fd)

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

    def test_committed_validation_rejects_oversized_run_log_before_tail_read(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            manifest_path = build_complete_delivery(root)
            run_log = manifest_path.parent / "run-a" / "run.log"
            with run_log.open("wb") as stream:
                stream.truncate(validator.MAX_RUN_LOG_BYTES + 1)
            hashed = []
            original = runner.sha256_file

            def recording_sha256(path):
                hashed.append(Path(path).resolve())
                return original(path)

            runner.sha256_file = recording_sha256
            try:
                with self.assertRaisesRegex(ValueError, "run.log.*size"):
                    runner.validate_committed(manifest_path, repo_root=root, require_git_commit=False)
            finally:
                runner.sha256_file = original
            self.assertNotIn(run_log.resolve(), hashed)

    def test_runner_and_validator_share_the_exact_log_size_limit(self):
        self.assertEqual(runner.MAX_RUN_LOG_BYTES, validator.MAX_RUN_LOG_BYTES)

    def test_delivery_manifest_uses_validated_run_log_digest_without_hash_reopen(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            evidence = root / "docs" / "raw"
            evidence.mkdir(parents=True)
            run_log = evidence / "run.log"
            run_log.write_text("exit_code=0\n")
            other = evidence / "other.txt"
            other.write_text("bounded")
            original = runner.sha256_file

            def reject_run_log(path):
                if Path(path) == run_log:
                    raise AssertionError("run.log was reopened for hashing")
                return original(path)

            runner.sha256_file = reject_run_log
            try:
                manifest = runner.create_delivery_manifest(root, evidence, "c", "t")
            finally:
                runner.sha256_file = original
            entry = next(item for item in manifest["files"] if item["path"].endswith("run.log"))
            self.assertEqual(validator.validate_run_log(run_log)["sha256"], entry["sha256"])

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
            empty = dict(loaded); empty["decisions"] = []
            empty["bundle_sha256"] = runner.sha256_bytes(runner.payload_json_bytes(runner._bundle_payload(empty)))
            empty_path = root / ("rollback-bundle-g1-" + empty["bundle_sha256"] + ".json")
            runner.atomic_write_json(empty_path, empty)
            with self.assertRaisesRegex(ValueError, "non-empty"):
                runner.authenticate_rollback_bundle(empty_path)
            with self.assertRaisesRegex(ValueError, "duplicate"):
                runner.write_rollback_bundle(root, [decision, decision], predecessor=None)
            payload = json.loads(bundle_path.read_text()); payload["decisions"][0]["dispatch"] = "serializer_decode"
            bundle_path.write_text(json.dumps(payload))
            with self.assertRaisesRegex(ValueError, "sha256"):
                runner.authenticate_rollback_bundle(bundle_path)

    def test_preparation_requires_complete_simultaneous_dispatch_set(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            comparison = archive / "comparison.csv"
            comparison.write_text(
                "method,verdict\nserializerDecodeDirectOptimized,regressed\n"
                "redissonDecodeContiguousOptimized,regressed\n"
            )
            decision = runner.make_rollback_decision(
                "serializer_decode", ["serializerDecodeDirectOptimized"], "old", "tree",
                archive, [comparison], 1, "now",
            )
            preparation = runner.write_rollback_preparation(root, [decision], 1)
            with self.assertRaisesRegex(ValueError, "simultaneous"):
                runner.authenticate_rollback_preparation(preparation)

            second = runner.make_rollback_decision(
                "redisson_contiguous", ["redissonDecodeContiguousOptimized"], "different-old", "different-tree",
                archive, [comparison], 1, "now",
            )
            mixed = runner.write_rollback_preparation(root, [decision, second], 1)
            with self.assertRaisesRegex(ValueError, "old commit/tree"):
                runner.authenticate_rollback_preparation(mixed)

    def test_finalized_generation_rejects_mixed_post_lineage_and_unsafe_preparation_path(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            comparison = archive / "comparison.csv"
            comparison.write_text(
                "method,verdict\nserializerDecodeDirectOptimized,regressed\n"
                "redissonDecodeContiguousOptimized,regressed\n"
            )
            decisions = [
                runner.make_rollback_decision("serializer_decode", ["serializerDecodeDirectOptimized"], "old", "tree", archive, [comparison], 1, "now", "post-a", None, "post-tree-a"),
                runner.make_rollback_decision("redisson_contiguous", ["redissonDecodeContiguousOptimized"], "old", "tree", archive, [comparison], 1, "now", "post-b", None, "post-tree-b"),
            ]
            with self.assertRaisesRegex(ValueError, "shared post"):
                runner.write_rollback_bundle(root, decisions)
            for stale in root.glob("rollback-preparation-g1-*.json"):
                stale.unlink()

            decisions[1] = runner.make_rollback_decision("redisson_contiguous", ["redissonDecodeContiguousOptimized"], "old", "tree", archive, [comparison], 1, "now", "post-a", None, "post-tree-a")
            bundle_path = runner.write_rollback_bundle(root, decisions)
            payload = json.loads(bundle_path.read_text())
            for unsafe in (str((root / "absolute.json").resolve()), "../traversal.json"):
                malicious = dict(payload); malicious["preparation_path"] = unsafe
                malicious["bundle_sha256"] = runner.sha256_bytes(runner.payload_json_bytes(runner._bundle_payload(malicious)))
                path = root / ("rollback-bundle-g1-" + malicious["bundle_sha256"] + ".json")
                runner.atomic_write_json(path, malicious)
                with self.assertRaisesRegex(ValueError, "unsafe preparation_path"):
                    runner.authenticate_rollback_bundle(path)

    def test_dispatch_removal_predicates_read_committed_head_and_reject_retained_symbols(self):
        removed = {
            "serializer_encode": "class ProtobufSerializer : BinarySerializer { }",
            "serializer_decode": "class ProtobufSerializer : BinarySerializer { }",
            "redisson_contiguous": "private fun decodeProtobuf(buf: ByteBuf): Any { return AnyMessage.parseFrom(buf.getBytes(copy = true)) }",
        }
        paths_seen = []
        def git_show(argv, **_kwargs):
            dispatch = git_show.dispatch
            paths_seen.append(argv[-1])
            return subprocess.CompletedProcess(argv, 0, stdout=removed[dispatch].encode(), stderr=b"")
        for dispatch in runner.DISPATCH_ORDER:
            git_show.dispatch = dispatch
            runner.verify_dispatch_source_removals(Path("/repo"), "post", [dispatch], git_show)
        self.assertTrue(all(value.startswith("post:") for value in paths_seen))

        retained = dict(removed)
        retained["serializer_encode"] = "override fun serializeTo(graph: Any?, target: ByteBuffer): Int = packMessageTo(graph, target)"
        retained["serializer_decode"] = "override fun <T: Any> deserializeFrom(source: ByteBuffer): T? = decodeWithTrustedFallback(source)"
        retained["redisson_contiguous"] = "if (buf.nioBufferCount() == 1) AnyMessage.parseFrom(buf.nioBuffer()) else AnyMessage.parseFrom(buf.getBytes(copy = true))"
        for dispatch in runner.DISPATCH_ORDER:
            git_show.dispatch = dispatch; removed[dispatch] = retained[dispatch]
            with self.assertRaisesRegex(ValueError, "removal predicate"):
                runner.verify_dispatch_source_removals(Path("/repo"), "post", [dispatch], git_show)

        adversarial = {
            "serializer_encode": '''class ProtobufSerializer {
                // packMessageTo(graph, target)
                // serializeTo(graph, target)
                val diagnostic = "serializeTo and packMessageTo(graph, target)"
            }''',
            "serializer_decode": '''class ProtobufSerializer {
                // override fun deserializeFrom(source: ByteBuffer) = decodeWithTrustedFallback(source)
                val diagnostic = "deserializeFrom(source: ByteBuffer)"
            }''',
            "redisson_contiguous": '''class Codec { private fun decodeProtobuf(buf: ByteBuf): Any {
                // buf.nioBufferCount(); AnyMessage.parseFrom(buf.nioBuffer())
                val diagnostic = "getBytes(copy = true)"
                return AnyMessage.parseFrom(buf.getBytes( copy = true ))
            }}''',
        }
        removed.update(adversarial)
        for dispatch in runner.DISPATCH_ORDER:
            git_show.dispatch = dispatch
            runner.verify_dispatch_source_removals(Path("/repo"), "post", [dispatch], git_show)
        rejected = {
            "serializer_encode": ["val `serializeTo` = 1", "val ref = serializer :: serializeTo"],
            "serializer_decode": ["val `deserializeFrom` = 1", "val ref = serializer :: deserializeFrom"],
            "redisson_contiguous": [
                "private fun decodeProtobuf(buf: ByteBuf): Any =\n AnyMessage.parseFrom(buf.getBytes(copy = true))",
                "private fun decodeProtobuf(buf: ByteBuf) = helper(buf)\nprivate fun helper(buf: ByteBuf) = AnyMessage.parseFrom(buf.getBytes(copy = true))",
                "private fun decodeProtobuf(buf: ByteBuf): Any { return AnyMessage.parseFrom(buf.getBytes(copy = true)) }\nprivate fun decodeProtobuf(buf: Other): Any { return AnyMessage.parseFrom(buf.getBytes(copy = true)) }",
                "private fun decodeProtobuf(buf: ByteBuf): Any { val view = buf.internalNioBuffer(); return AnyMessage.parseFrom(buf.getBytes(copy = true)) }",
                "private fun decodeProtobuf(buf: ByteBuf): Any { val `nioBufferAlias` = 1; return AnyMessage.parseFrom(buf.getBytes(copy = true)) }",
            ],
        }
        for dispatch, cases in rejected.items():
            for source in cases:
                removed[dispatch] = source; git_show.dispatch = dispatch
                with self.assertRaisesRegex(ValueError, "canonical"):
                    runner.verify_dispatch_source_removals(Path("/repo"), "post", [dispatch], git_show)

    def test_prepared_retry_rejects_refreshed_environment_lineage(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); state_path, _, _ = build_rollback_state(root, (("redissonDecodeContiguousOptimized", "regressed"),))
            def git(argv, **_kwargs):
                if argv[1] == "status": return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[-1] == "HEAD": return subprocess.CompletedProcess(argv, 0, stdout=b"old\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}": return subprocess.CompletedProcess(argv, 0, stdout=b"old-tree\n", stderr=b"")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            runner.record_rollback(state_path, ["redisson_contiguous"], root / "rollback", command_runner=git, repo_root=root)
            state = json.loads(state_path.read_text())
            for run in state["canonical_runs"]:
                environment = Path(run["absolute_path"]) / "environment.json"
                runner.atomic_write_json(environment, {"git_commit": "new", "tree_hash": "new-tree"})
                run["files"]["environment.json"] = runner.sha256_file(environment)
            runner.atomic_write_json(state_path, state)
            with self.assertRaisesRegex(ValueError, "stale preparation"):
                runner.record_rollback(state_path, ["redisson_contiguous"], root / "rollback", command_runner=git, repo_root=root)

    def test_expected_promotion_includes_every_authenticated_preparation(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td); archive = root / "archive"; archive.mkdir()
            comparison = archive / "comparison.csv"
            comparison.write_text("method,verdict\nredissonDecodeContiguousOptimized,regressed\n")
            decision = runner.make_rollback_decision("redisson_contiguous", ["redissonDecodeContiguousOptimized"], "old", "tree", archive, [comparison], 1, "now", "post", None, "post-tree")
            bundle_path = runner.write_rollback_bundle(root, [decision])
            bundle = runner.authenticate_rollback_bundle(bundle_path)
            state = {"canonical_runs": [], "rollback_bundle_path": str(bundle_path), "rollback_bundle": bundle}
            expected = runner.expected_promoted_files(state)
            self.assertIn(bundle["preparation_path"], expected)
            preparation = root / bundle["preparation_path"]
            preparation.write_text("tampered")
            with self.assertRaises(ValueError):
                runner.expected_promoted_files(state)

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

            next_root = root / "next"; next_root.mkdir()
            state_path, _, _ = build_rollback_state(next_root, (("serializerDecodeDirectOptimized", "regressed"),), environments=(("c2", "t2"), ("c2", "t2")))
            state = json.loads(state_path.read_text())
            state.update({"rollback_bundle_path": str(first_bundle), "rollback_bundle_sha256": runner.sha256_file(first_bundle),
                          "rollback_bundle": runner.authenticate_rollback_bundle(first_bundle)})
            runner.atomic_write_json(state_path, state)
            def non_descendant(argv, **_kwargs):
                if argv[1] == "status": return subprocess.CompletedProcess(argv, 0, stdout=b"", stderr=b"")
                if argv[-1] == "HEAD": return subprocess.CompletedProcess(argv, 0, stdout=b"c2\n", stderr=b"")
                if argv[-1] == "HEAD^{tree}": return subprocess.CompletedProcess(argv, 0, stdout=b"t2\n", stderr=b"")
                if argv[1:3] == ["merge-base", "--is-ancestor"]: return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"no")
                return subprocess.CompletedProcess(argv, 1, stdout=b"", stderr=b"unexpected")
            with self.assertRaisesRegex(ValueError, "does not descend from predecessor"):
                runner.record_rollback(state_path, ["serializer_decode"], root, command_runner=non_descendant, repo_root=root)
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
        manifest["rollback"] = {"decisions": [{"regressed_cells": ["good"], "removed_cells": ["good"]}]}
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
