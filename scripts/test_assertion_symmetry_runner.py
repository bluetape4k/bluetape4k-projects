"""Contract tests for the bounded, fixed-command Assertions/Core runner."""

from __future__ import annotations

import importlib.util
import json
import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
RUNNER_PATH = REPOSITORY_ROOT / "scripts" / "assertion_symmetry_runner.py"


def load_runner():
    if not RUNNER_PATH.is_file():
        return None
    spec = importlib.util.spec_from_file_location("assertion_symmetry_runner", RUNNER_PATH)
    if spec is None or spec.loader is None:
        return None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


runner = load_runner()


class RunnerAvailabilityTests(unittest.TestCase):
    def test_runner_module_exists(self):
        self.assertTrue(RUNNER_PATH.is_file(), "the fixed-command runner is not implemented yet")


class RunnerContractTests(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(runner, "the fixed-command runner is not implemented yet")

    def test_request_validation_pins_run_id_alias_and_phase(self):
        runner.validate_request("baseline-assertions", "20261011T180000Z-a1b2c3d4", "baseline")

        for alias, phase, run_id in (
            ("baseline-assertions", "final", "20261011T180000Z-a1b2c3d4"),
            ("test-core", "baseline", "20261011T180000Z-a1b2c3d4"),
            ("arbitrary", "final", "20261011T180000Z-a1b2c3d4"),
            ("test-core", "final", "../20261011T180000Z-a1b2c3d4"),
        ):
            with self.subTest(alias=alias, phase=phase, run_id=run_id):
                with self.assertRaises(ValueError):
                    runner.validate_request(alias, phase, run_id)

    def test_alias_builds_fixed_argv_without_shell_strings(self):
        steps = runner.build_steps("baseline-assertions", "baseline", REPOSITORY_ROOT)

        self.assertEqual(len(steps), 1)
        self.assertEqual(
            steps[0].argv,
            (
                "./gradlew",
                "--no-daemon",
                "--no-configuration-cache",
                "--no-build-cache",
                "-Pkotlin.compiler.execution.strategy=in-process",
                ":bluetape4k-assertions:clean",
                ":bluetape4k-assertions:test",
            ),
        )
        self.assertIsInstance(steps[0].argv, tuple)
        self.assertFalse(any(isinstance(argument, str) and " && " in argument for argument in steps[0].argv))

    def test_detekt_alias_keeps_rerun_option_outside_project_task_path(self):
        steps = runner.build_steps("baseline-detekt-assertions", "baseline", REPOSITORY_ROOT)

        self.assertEqual(
            steps[0].argv[-2:],
            (":bluetape4k-assertions:detekt", "--rerun-tasks"),
        )

    def test_final_test_alias_requires_all_changed_contract_test_classes(self):
        expected = {
            "assertions": (
                "BasicTest",
                "ArraysTest",
                "CharSequencesTest",
                "CollectionsTest",
                "DateTimesTest$InstantAssertions",
                "DateTimesTest$ZonedDateTimeAssertions",
                "DateTimesTest$OffsetDateTimeAssertions",
                "DateTimesTest$LocalDateTimeAssertions",
                "DateTimesTest$LocalDateAssertions",
                "DateTimesTest$LocalTimeAssertions",
                "DateTimesTest$JavaUtilDateAssertions",
                "MapsTest",
                "NumericalTest",
                "DurationAssertionsTest",
                "AssertionSymmetryTest",
            ),
            "core": (
                "RequireSupportTest",
                "CheckSupportTest",
                "DurationSupportTest",
                "KotlinDurationSupportTest",
            ),
        }

        for module, required_classes in expected.items():
            alias = f"test-{module}"
            with self.subTest(module=module):
                steps = runner.build_steps(alias, "final", REPOSITORY_ROOT)
                self.assertEqual(steps[0].required_classes, required_classes)

    def test_baseline_test_alias_requires_only_baseline_test_classes(self):
        for module, required_classes in runner.BASELINE_TEST_CLASSES.items():
            alias = f"baseline-{module}"
            with self.subTest(module=module):
                steps = runner.build_steps(alias, "baseline", REPOSITORY_ROOT)
                self.assertEqual(steps[0].required_classes, required_classes)

        self.assertNotIn("DurationAssertionsTest", runner.BASELINE_TEST_CLASSES["assertions"])
        self.assertNotIn("AssertionSymmetryTest", runner.BASELINE_TEST_CLASSES["assertions"])

    def test_attempt_directory_is_created_below_repository_root(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            repository_root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                repository_root,
                "20261011T180000Z-a1b2c3d4",
                "final",
                "compile-core",
                1,
            )

            self.assertTrue(attempt.is_dir())
            self.assertEqual(attempt.name, "attempt-1")
            self.assertTrue(attempt.resolve().is_relative_to(repository_root.resolve()))

    def test_attempt_directory_rejects_symlink_escape(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            repository_root = Path(temporary_directory) / "repo"
            outside = Path(temporary_directory) / "outside"
            repository_root.mkdir()
            outside.mkdir()
            (repository_root / "build").symlink_to(outside, target_is_directory=True)

            with self.assertRaises(ValueError):
                runner.create_attempt_directory(
                    repository_root,
                    "20261011T180000Z-a1b2c3d4",
                    "final",
                    "compile-core",
                    1,
                )

            self.assertEqual(list(outside.iterdir()), [])

    def test_raw_logs_grow_while_child_is_running(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "stream", 1
            )
            child = "import time; print('ready', flush=True); time.sleep(0.35); print('done', flush=True)"
            results = []
            worker = threading.Thread(
                target=lambda: results.append(
                    runner.execute_command(
                        (sys.executable, "-c", child),
                        root,
                        attempt,
                        timeout_seconds=2,
                        idle_timeout_seconds=1,
                        poll_interval=0.01,
                        grace_periods=(0.05, 0.05, 0.1),
                    )
                )
            )
            worker.start()
            stdout_log = attempt / "stdout.log"
            deadline = time.monotonic() + 1
            while time.monotonic() < deadline:
                if stdout_log.exists() and stdout_log.stat().st_size > 0:
                    break
                time.sleep(0.01)

            self.assertTrue(stdout_log.exists())
            self.assertGreater(stdout_log.stat().st_size, 0)
            self.assertTrue(worker.is_alive(), "the child exited before the live log was observed")
            worker.join(timeout=2)

            self.assertFalse(worker.is_alive())
            self.assertEqual(results[0].exit_code, 0)
            self.assertIn("done", stdout_log.read_text())

    def test_idle_warning_does_not_terminate_child(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "idle", 1
            )
            child = "import time; time.sleep(0.25); print('still alive', flush=True)"

            result = runner.execute_command(
                (sys.executable, "-c", child),
                root,
                attempt,
                timeout_seconds=2,
                idle_timeout_seconds=0.05,
                poll_interval=0.01,
                grace_periods=(0.05, 0.05, 0.1),
            )

            self.assertEqual(result.exit_code, 0)
            self.assertTrue(result.metadata["idle_warnings"])
            self.assertEqual(result.metadata["termination_reason"], "completed")
            self.assertIn("still alive", (attempt / "stdout.log").read_text())

    @unittest.skipUnless(hasattr(signal, "SIGKILL") and hasattr(os, "killpg"), "requires POSIX process groups")
    def test_timeout_escalates_signals_and_terminates_the_process_group(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "timeout", 1
            )
            child_code = (
                "import signal, time; "
                "signal.signal(signal.SIGINT, signal.SIG_IGN); "
                "signal.signal(signal.SIGTERM, signal.SIG_IGN); "
                "time.sleep(30)"
            )
            parent_code = (
                "import signal, subprocess, sys, time; "
                "signal.signal(signal.SIGINT, signal.SIG_IGN); "
                "signal.signal(signal.SIGTERM, signal.SIG_IGN); "
                f"subprocess.Popen([sys.executable, '-c', {child_code!r}]); "
                "time.sleep(30)"
            )

            result = runner.execute_command(
                (sys.executable, "-c", parent_code),
                root,
                attempt,
                timeout_seconds=0.2,
                idle_timeout_seconds=5,
                poll_interval=0.01,
                grace_periods=(0.05, 0.05, 0.5),
            )

            self.assertNotEqual(result.exit_code, 0)
            self.assertEqual(result.metadata["termination_reason"], "timeout")
            self.assertEqual(
                result.metadata["signals_sent"], ["SIGINT", "SIGTERM", "SIGKILL"]
            )
            self.assertTrue(result.metadata["process_group_terminated"])

    def test_interrupt_event_is_recorded_and_stops_child(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "interrupt", 1
            )
            interrupt_event = threading.Event()
            timer = threading.Timer(0.1, interrupt_event.set)
            timer.start()
            try:
                result = runner.execute_command(
                    (sys.executable, "-c", "import time; time.sleep(30)"),
                    root,
                    attempt,
                    timeout_seconds=3,
                    idle_timeout_seconds=5,
                    poll_interval=0.01,
                    grace_periods=(0.1, 0.1, 0.2),
                    interrupt_event=interrupt_event,
                )
            finally:
                timer.cancel()

            self.assertNotEqual(result.exit_code, 0)
            self.assertEqual(result.metadata["termination_reason"], "interrupted")
            self.assertTrue(result.metadata["signals_sent"])
            self.assertTrue(result.metadata["process_group_terminated"])

    def test_failed_step_is_not_retried_or_followed_by_later_steps(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            marker = root / "later-step-ran"
            steps = (
                runner.CommandSpec(
                    "fails-once",
                    (sys.executable, "-c", "raise SystemExit(7)"),
                    timeout_seconds=2,
                    idle_timeout_seconds=1,
                ),
                runner.CommandSpec(
                    "must-not-run",
                    (sys.executable, "-c", f"from pathlib import Path; Path({str(marker)!r}).touch()"),
                    timeout_seconds=2,
                    idle_timeout_seconds=1,
                ),
            )

            results = runner.run_steps(
                root,
                "20261011T180000Z-a1b2c3d4",
                "final",
                steps,
                poll_interval=0.01,
                grace_periods=(0.05, 0.05, 0.1),
            )

            self.assertEqual(len(results), 1)
            self.assertEqual(results[0].exit_code, 7)
            self.assertFalse(marker.exists())
            step_root = (
                root
                / "build/workflow/assertion-symmetry/20261011T180000Z-a1b2c3d4/final/fails-once"
            )
            self.assertTrue((step_root / "attempt-1/metadata.json").is_file())
            self.assertFalse((step_root / "attempt-2").exists())

    def test_existing_result_cannot_be_automatically_retried(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "finished", 1
            )
            (attempt / "metadata.json").write_text("{}")

            with self.assertRaises(ValueError):
                runner.create_attempt_directory(
                    root, "20261011T180000Z-a1b2c3d4", "final", "finished", 1
                )

            self.assertFalse((attempt.parent / "attempt-2").exists())

    def test_only_an_incomplete_external_attempt_can_resume_once(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            exited = subprocess.Popen(
                (sys.executable, "-c", "pass"), start_new_session=True
            )
            exited.wait()
            first_attempt = runner.create_attempt_directory(
                root, "20261011T180000Z-a1b2c3d4", "final", "resume", 1
            )
            (first_attempt / "launch.json").write_text(
                json.dumps({"pid": exited.pid, "pgid": exited.pid})
            )
            step = runner.CommandSpec(
                "resume",
                (sys.executable, "-c", "print('recovered')"),
                timeout_seconds=2,
                idle_timeout_seconds=1,
            )

            results = runner.run_steps(
                root,
                "20261011T180000Z-a1b2c3d4",
                "final",
                (step,),
                poll_interval=0.01,
                grace_periods=(0.05, 0.05, 0.1),
            )

            second_attempt = first_attempt.parent / "attempt-2"
            self.assertEqual(results[0].exit_code, 0)
            self.assertTrue((first_attempt / "launch.json").is_file())
            self.assertTrue((second_attempt / "metadata.json").is_file())
            self.assertEqual(results[0].metadata["resumed_from_incomplete_attempt"], 1)
            with self.assertRaises(ValueError):
                runner.run_steps(
                    root,
                    "20261011T180000Z-a1b2c3d4",
                    "final",
                    (step,),
                    poll_interval=0.01,
                    grace_periods=(0.05, 0.05, 0.1),
                )

    def test_junit_verifier_requires_fresh_test_task_and_non_skipped_cases(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            report_dir = root / "testing" / "assertions" / "build/test-results/test"
            report_dir.mkdir(parents=True)
            report = report_dir / "TEST-AssertionsTest.xml"
            report.write_text(
                '<testsuite name="AssertionsTest" tests="1" failures="0" errors="0" skipped="0">'
                '<testcase classname="io.bluetape4k.AssertionsTest" name="positive" />'
                "</testsuite>"
            )
            task_log = "> Task :bluetape4k-assertions:clean\n> Task :bluetape4k-assertions:test\n"

            result = runner.verify_junit_reports(
                root, "assertions", task_log, required_classes=("AssertionsTest",)
            )

            self.assertEqual(result["testcases"], 1)
            self.assertEqual(len(result["reports"]), 1)
            with self.assertRaises(ValueError):
                runner.verify_junit_reports(root, "assertions", "BUILD SUCCESSFUL", required_classes=())

            report.write_text(
                '<testsuite name="AssertionsTest" tests="1" failures="0" errors="0" skipped="1">'
                '<testcase classname="io.bluetape4k.AssertionsTest" name="skipped"><skipped /></testcase>'
                "</testsuite>"
            )
            with self.assertRaises(ValueError):
                runner.verify_junit_reports(root, "assertions", task_log, required_classes=())


if __name__ == "__main__":
    unittest.main()
