#!/usr/bin/env python3
"""Unit tests for the Docker-backed image gate runner."""

from __future__ import annotations

import json
import os
import sys
import tempfile
import time
import unittest
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace

from scripts.run_testcontainers_image_gate import (
    GateRunner,
    MAX_OUTPUT_CHARS,
    MAX_OUTPUT_LINES,
    _classify_command_result,
    _subprocess_runner,
    classify_failure,
    redact,
    register_secret,
    verify_release_summary,
    worst_case_budget_minutes,
)


def entry(server: str = "FlociServer", *, release_required: bool = True) -> dict[str, object]:
    return {
        "id": server.lower(),
        "server": server,
        "source": "testing/testcontainers/src/main/kotlin/example/Server.kt",
        "testSource": "testing/testcontainers/src/test/kotlin/example/ServerTest.kt",
        "image": "example/image",
        "tag": "1.0",
        "testPattern": f"example.{server}Test",
        "readiness": "testcontainers_wait_strategy_and_workload_readiness",
        "workload": f"example.{server}Test:representative_test",
        "diagnostics": ["docker_inspect", "docker_logs", "docker_events"],
        "releaseRequired": release_required,
    }


def strict_entry() -> dict[str, object]:
    value = entry("StrictServer")
    value.update(
        {
            "id": "strict-family",
            "image": "example/strict",
            "tag": "1.0",
            "testPattern": "io.bluetape4k.testcontainers.storage.StrictServerTest",
            "workloadTestPattern": "io.bluetape4k.testcontainers.storage.StrictServerTest.representativeStartupAndWorkload",
            "executionEvidenceRequired": True,
            "pullEvidenceRequired": True,
            "platforms": [
                {"id": "amd64", "os": "linux", "architecture": "amd64", "tag": "1.0", "runner": "ubuntu-24.04"},
            ],
            "defaultPlatformId": "amd64",
            "platformTimeouts": {"amd64": {"testMinutes": 6, "clientConnectSeconds": 30, "clientRequestSeconds": 30}},
        }
    )
    return value


def _evidence_dir(command: list[str]) -> Path:
    evidence = next(
        (
            Path(part.split("=", 1)[1])
            for part in command
            if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")
        ),
        None,
    )
    if evidence is None:
        raise AssertionError("generic release evidence directory is missing")
    return evidence


def _write_generic_junit(
    command: list[str],
    *,
    tests: int,
    skipped: int,
    failures: int = 0,
    errors: int = 0,
    suite_name: str | None = None,
) -> None:
    evidence = _evidence_dir(command)
    pattern = suite_name or command[command.index("--tests") + 1]
    evidence.joinpath("TEST-generic.xml").write_text(
        f'<testsuite name="{pattern}" tests="{tests}" skipped="{skipped}" '
        f'failures="{failures}" errors="{errors}">'
        '<testcase classname="example.FamilyTest" name="representative_test"/>'
        "</testsuite>",
        encoding="utf-8",
    )


def _generic_success(command: list[str], timeout_seconds: int) -> SimpleNamespace:
    _write_generic_junit(command, tests=1, skipped=0)
    return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")


class TestRunTestcontainersImageGate(unittest.TestCase):
    def test_runtime_environment_removes_docker_credentials_and_overrides(self) -> None:
        original = {
            key: os.environ.get(key)
            for key in ("DOCKER_AUTH_CONFIG", "DOCKER_CONFIG", "TESTCONTAINERS_REGISTRY_MIRROR", "DOCKER_HOST", "DOCKER_CONTEXT")
        }
        try:
            for key in original:
                os.environ[key] = "secret-or-override"
            sanitized = GateRunner._sanitized_runtime_env()
            for key in original:
                self.assertNotIn(key, sanitized)
        finally:
            for key, value in original.items():
                if value is None:
                    os.environ.pop(key, None)
                else:
                    os.environ[key] = value

    def test_runtime_environment_preserves_only_managed_local_unix_socket(self) -> None:
        original = {
            key: os.environ.get(key)
            for key in ("DOCKER_HOST", "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE")
        }
        try:
            os.environ["DOCKER_HOST"] = "unix:///Users/test/.colima/default/docker.sock"
            os.environ["TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE"] = "/var/run/docker.sock"
            sanitized = GateRunner._sanitized_runtime_env()
            self.assertEqual(
                "unix:///Users/test/.colima/default/docker.sock",
                sanitized.get("DOCKER_HOST"),
            )
        finally:
            for key, value in original.items():
                if value is None:
                    os.environ.pop(key, None)
                else:
                    os.environ[key] = value

    def test_successful_family_records_command_and_coverage(self) -> None:
        calls: list[list[str]] = []

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            calls.append(command)
            return _generic_success(command, timeout_seconds)

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()
            self.assertEqual("success", summary["results"][0]["status"])
            self.assertEqual("1/1", summary["coverage"])
            self.assertTrue(summary["release_gate"])
            self.assertIn("--tests", calls[0])
            self.assertTrue((Path(directory) / "summary.json").is_file())

    def test_release_command_uses_explicit_test_selector(self) -> None:
        selected = entry()
        selected["testSelector"] = "example.FlociServerTest.representative_test"

        with tempfile.TemporaryDirectory() as directory:
            command = GateRunner([selected], Path(directory))._command(
                selected,
                Path(directory) / "evidence",
            )

        self.assertEqual(
            "example.FlociServerTest.representative_test",
            command[command.index("--tests") + 1],
        )

    def test_explicit_selector_produces_successful_junit_release_evidence(self) -> None:
        selected = entry()
        selected["testSelector"] = "example.FlociServerTest.representative_test"
        calls: list[list[str]] = []

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            calls.append(command)
            _write_generic_junit(
                command,
                tests=1,
                skipped=0,
                suite_name=str(selected["testPattern"]),
            )
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [selected],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()

        result = summary["results"][0]
        self.assertEqual("success", result["status"])
        self.assertEqual("1/1", summary["coverage"])
        self.assertTrue(summary["release_gate"])
        self.assertEqual(1, result["junit"]["tests"])
        self.assertEqual(0, result["junit"]["skipped"])
        self.assertEqual(
            selected["testSelector"],
            calls[0][calls[0].index("--tests") + 1],
        )

    def test_release_required_generic_family_rejects_all_skipped_junit(self) -> None:
        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            _write_generic_junit(command, tests=12, skipped=12)
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()

        result = summary["results"][0]
        self.assertEqual("blocked", result["status"])
        self.assertIn(
            "JUnit suite has no successful execution evidence",
            result["attempts"][0]["evidence_error"],
        )
        self.assertEqual("0/1", summary["coverage"])
        self.assertFalse(summary["release_gate"])

    def test_release_required_generic_family_rejects_empty_junit(self) -> None:
        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            _write_generic_junit(command, tests=0, skipped=0)
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            result = GateRunner(
                [entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()["results"][0]

        self.assertEqual("blocked", result["status"])

    def test_support_inventory_is_excluded_from_release_evidence_coverage(self) -> None:
        support = entry("SupportServer", release_required=False)

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            if any(part.startswith("-Dtestcontainers.image-gate.evidence-dir=") for part in command):
                _write_generic_junit(command, tests=1, skipped=0)
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry(), support],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()

        self.assertEqual("1/1", summary["coverage"])
        self.assertEqual("2/2", summary["selected_coverage"])
        self.assertEqual(1, summary["release_required_selected"])
        self.assertTrue(summary["release_gate"])

    def test_infrastructure_failure_retries_and_collects_diagnostics(self) -> None:
        calls: list[list[str]] = []
        diagnostics: list[str] = []

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            calls.append(command)
            return SimpleNamespace(returncode=1, stdout="toomanyrequests: rate limit", stderr="")

        def diagnostic_runner(family: dict[str, object]) -> dict[str, str]:
            diagnostics.append(str(family["server"]))
            return {"docker_logs": "<redacted>"}

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=command_runner,
                diagnostic_runner=diagnostic_runner,
                max_attempts=2,
            ).run()
            result = summary["results"][0]
            self.assertEqual("infrastructure_failure", result["status"])
            self.assertEqual(2, len(result["attempts"]))
            self.assertEqual(["FlociServer"], diagnostics)
            self.assertFalse(summary["release_gate"])

    def test_product_failure_and_timeout_are_distinct(self) -> None:
        self.assertEqual("product_failure", classify_failure(1, "AssertionError: expected 200", ""))
        self.assertEqual("infrastructure_failure", classify_failure(1, "connection refused", ""))
        self.assertEqual("infrastructure_failure", classify_failure(None, "", "timeout"))

    def test_product_failure_does_not_retry(self) -> None:
        calls: list[list[str]] = []

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            calls.append(command)
            return SimpleNamespace(
                returncode=1,
                stdout="",
                stderr="AssertionError: expected 200",
            )

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=2,
            ).run()

        self.assertEqual("product_failure", summary["results"][0]["status"])
        self.assertEqual(1, len(summary["results"][0]["attempts"]))
        gradle_calls = [command for command in calls if command[0] == "./gradlew"]
        self.assertEqual(1, len(gradle_calls))

    def test_test_discovery_failure_wins_over_unrelated_docker_output(self) -> None:
        stdout = "building image to Docker daemon\ntimeout while preparing unrelated diagnostics\n"
        stderr = (
            "Execution failed for task ':bluetape4k-testcontainers:test'.\n"
            "> No tests found for given includes: "
            "[io.bluetape4k.testcontainers.infra.K3sServerTest](--tests filter)"
        )

        self.assertEqual("product_failure", classify_failure(1, stdout, stderr))

    def test_test_discovery_words_without_gradle_failure_context_do_not_override_infrastructure(self) -> None:
        stdout = "building image to Docker daemon\n"
        stderr = "diagnostic note: No tests found for given includes may be reported later"

        self.assertEqual("infrastructure_failure", classify_failure(1, stdout, stderr))

    def test_family_specific_test_task_overrides_the_default_task(self) -> None:
        calls: list[list[str]] = []
        k3s = entry("K3sServer")
        k3s["testTask"] = ":bluetape4k-testcontainers:k8sTest"

        def command_runner(command: list[str], timeout_seconds: int) -> SimpleNamespace:
            calls.append(command)
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            GateRunner(
                [k3s],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
            ).run()

        self.assertEqual(
            ["./gradlew", ":bluetape4k-testcontainers:k8sTest"],
            calls[0][:2],
        )

    def test_zero_exit_without_gradle_success_evidence_is_blocked(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=lambda command, timeout_seconds: SimpleNamespace(
                    returncode=0, stdout="completed without the Gradle result", stderr=""
                ),
                max_attempts=1,
            ).run()
            self.assertEqual("blocked", summary["results"][0]["status"])
            self.assertFalse(summary["release_gate"])

    def test_success_after_long_gradle_output_is_not_blocked(self) -> None:
        long_success = ("progress\n" * 4000) + "BUILD SUCCESSFUL\n"
        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=lambda command, timeout_seconds: (
                    _write_generic_junit(command, tests=1, skipped=0)
                    or SimpleNamespace(returncode=0, stdout=long_success, stderr="")
                ),
                max_attempts=1,
            ).run()
            self.assertEqual("success", summary["results"][0]["status"])
            self.assertTrue(summary["release_gate"])

    def test_stdout_overflow_with_zero_exit_is_blocked(self) -> None:
        command = [
            sys.executable,
            "-c",
            f"import sys; sys.stdout.write('x' * {MAX_OUTPUT_CHARS + 1}); sys.stdout.flush()",
        ]

        result = _subprocess_runner(command, timeout_seconds=5)

        self.assertEqual(0, result.returncode)
        self.assertTrue(result.stdout_overflow)
        self.assertFalse(result.stderr_overflow)
        self.assertLessEqual(len(result.stdout.encode("utf-8")), MAX_OUTPUT_CHARS)
        self.assertEqual(
            "blocked",
            _classify_command_result(
                result.returncode,
                result.stdout,
                result.stderr,
                stdout_overflow=result.stdout_overflow,
                stderr_overflow=result.stderr_overflow,
            ),
        )

    def test_stderr_overflow_with_zero_exit_is_blocked(self) -> None:
        command = [
            sys.executable,
            "-c",
            f"import sys; sys.stdout.write('BUILD SUCCESSFUL\\n'); sys.stderr.write('x' * {MAX_OUTPUT_CHARS + 1}); sys.stderr.flush()",
        ]

        result = _subprocess_runner(command, timeout_seconds=5)

        self.assertEqual(0, result.returncode)
        self.assertFalse(result.stdout_overflow)
        self.assertTrue(result.stderr_overflow)
        self.assertLessEqual(len(result.stderr.encode("utf-8")), MAX_OUTPUT_CHARS)
        self.assertEqual(
            "blocked",
            _classify_command_result(
                result.returncode,
                result.stdout,
                result.stderr,
                stdout_overflow=result.stdout_overflow,
                stderr_overflow=result.stderr_overflow,
            ),
        )

    def test_stdout_line_overflow_with_zero_exit_is_blocked(self) -> None:
        command = [
            sys.executable,
            "-c",
            f"import sys; sys.stdout.write('x\\n' * {MAX_OUTPUT_LINES + 1}); sys.stdout.write('BUILD SUCCESSFUL\\n'); sys.stdout.flush()",
        ]

        result = _subprocess_runner(command, timeout_seconds=5)

        self.assertEqual(0, result.returncode)
        self.assertTrue(result.stdout_overflow)
        self.assertFalse(result.stderr_overflow)
        self.assertIn("...[line limit]", result.stdout)
        self.assertEqual(
            "blocked",
            _classify_command_result(
                result.returncode,
                result.stdout,
                result.stderr,
                stdout_overflow=result.stdout_overflow,
                stderr_overflow=result.stderr_overflow,
            ),
        )

    def test_stderr_line_overflow_with_zero_exit_is_blocked(self) -> None:
        command = [
            sys.executable,
            "-c",
            f"import sys; sys.stdout.write('BUILD SUCCESSFUL\\n'); sys.stderr.write('x\\n' * {MAX_OUTPUT_LINES + 1}); sys.stderr.flush()",
        ]

        result = _subprocess_runner(command, timeout_seconds=5)

        self.assertEqual(0, result.returncode)
        self.assertFalse(result.stdout_overflow)
        self.assertTrue(result.stderr_overflow)
        self.assertIn("...[line limit]", result.stderr)
        self.assertEqual(
            "blocked",
            _classify_command_result(
                result.returncode,
                result.stdout,
                result.stderr,
                stdout_overflow=result.stdout_overflow,
                stderr_overflow=result.stderr_overflow,
            ),
        )

    def test_secret_redaction_applies_to_artifact_text(self) -> None:
        safe = redact("TOKEN=super-secret password=hunter2 Authorization: Bearer abc123")
        self.assertNotIn("super-secret", safe)
        self.assertNotIn("hunter2", safe)
        self.assertNotIn("abc123", safe)
        self.assertIn("<redacted>", safe)

    def test_nested_docker_auth_values_are_registered_for_redaction(self) -> None:
        register_secret(
            json.dumps(
                {
                    "auths": {
                        "registry.example": {
                            "auth": "dXNlcjpwYXNz",
                            "username": "user",
                            "password": "pass",
                        }
                    }
                }
            )
        )
        safe = redact("auth=dXNlcjpwYXNz username=user password=pass user:pass")
        self.assertNotIn("dXNlcjpwYXNz", safe)
        self.assertNotIn("user", safe)
        self.assertNotIn("pass", safe)

    def test_summary_json_is_machine_readable(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=_generic_success,
                max_attempts=1,
            ).run()
            read_back = json.loads((Path(directory) / "summary.json").read_text())
            self.assertEqual(summary["manifest_digest"], read_back["manifest_digest"])

    def test_shard_summary_records_partition_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry("AlphaServer"), entry("BetaServer")],
                Path(directory),
                command_runner=_generic_success,
                max_attempts=1,
                shard_index=1,
                shard_count=4,
            ).run()
        self.assertEqual({"index": 1, "count": 4, "family_ids": ["alphaserver", "betaserver"]}, summary["shard"])

    def test_strict_family_requires_pull_platform_and_workload_evidence(self) -> None:
        calls: list[list[str]] = []

        def command_runner(command: list[str], timeout_seconds: int, **_: object) -> SimpleNamespace:
            calls.append(command)
            if command[:2] == ["docker", "pull"]:
                return SimpleNamespace(returncode=0, stdout="Status: Downloaded newer image", stderr="")
            if command[:3] == ["docker", "image", "inspect"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps([{"Id": "sha256:" + "a" * 64, "RepoDigests": ["example/strict@sha256:" + "b" * 64], "Os": "linux", "Architecture": "amd64"}]),
                    stderr="",
                )
            if command[:3] == ["docker", "context", "show"]:
                return SimpleNamespace(returncode=0, stdout="default\n", stderr="")
            if command[:2] == ["docker", "info"]:
                return SimpleNamespace(returncode=0, stdout=json.dumps({"Architecture": "amd64", "OSType": "linux"}), stderr="")
            if command[:2] == ["uname", "-m"]:
                return SimpleNamespace(returncode=0, stdout="x86_64\n", stderr="")
            if command[:2] == ["uname", "-s"]:
                return SimpleNamespace(returncode=0, stdout="Linux\n", stderr="")
            if command[:2] == ["docker", "events"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps({
                        "timeNano": str(time.time_ns()),
                        "id": "sha256:" + "a" * 64,
                        "from": "example/strict:1.0",
                        "Actor": {"ID": "sha256:" + "a" * 64, "Attributes": {"name": "example/strict:1.0"}},
                    }) + "\n",
                    stderr="",
                )
            evidence = next((Path(part.split("=", 1)[1]) for part in command if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")), None)
            assert evidence is not None
            evidence.joinpath("startup.marker").write_text("Container workload ready\n", encoding="utf-8")
            evidence.joinpath("workload.image-id").write_text("sha256:" + "a" * 64 + "\n", encoding="utf-8")
            evidence.joinpath("TEST-StrictServerTest.xml").write_text(
                '<testsuite name="io.bluetape4k.testcontainers.storage.StrictServerTest" tests="1" skipped="0" failures="0" errors="0">'
                '<testcase classname="io.bluetape4k.testcontainers.storage.StrictServerTest" name="representativeStartupAndWorkload"/>'
                '</testsuite>',
                encoding="utf-8",
            )
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [strict_entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
                scope="family",
            ).run()
            result = summary["results"][0]
            self.assertEqual("success", result["status"])
            self.assertEqual("amd64", result["platform_id"])
            self.assertEqual("sha256:" + "a" * 64, result["pull"]["event_id"])
            self.assertEqual("example/strict:1.0", result["pull"]["event_ref"])
            self.assertEqual(1, result["junit"]["workload_tests"])
            self.assertTrue(result["startup"]["ready"])
            self.assertEqual(1, sum(command[:2] == ["docker", "pull"] for command in calls))
            self.assertEqual(
                [],
                verify_release_summary(
                    summary,
                    expected_coverage="1/1",
                    platform_id="amd64",
                    expected_tag="1.0",
                    expected_architecture="amd64",
                    report_dir=Path(directory),
                ),
            )
            event_command = next(command for command in calls if command[:2] == ["docker", "events"])
            self.assertIn("--since", event_command)
            self.assertIn("--until", event_command)
            since = int(event_command[event_command.index("--since") + 1])
            self.assertGreater(since, 1_000_000_000)
            event_command = next(command for command in calls if command[:2] == ["docker", "events"])
            self.assertIn("--since", event_command)
            self.assertIn("--until", event_command)

    def test_strict_family_accepts_text_pull_event_bound_to_post_pull_inspect(self) -> None:
        """Hosted Docker may emit text pull events without an event/image ID."""

        def command_runner(command: list[str], timeout_seconds: int, **_: object) -> SimpleNamespace:
            if command[:2] == ["docker", "pull"]:
                return SimpleNamespace(returncode=0, stdout="Status: Downloaded newer image", stderr="")
            if command[:3] == ["docker", "image", "inspect"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps(
                        [
                            {
                                "Id": "sha256:" + "a" * 64,
                                "RepoDigests": ["example/strict@sha256:" + "b" * 64],
                                "Os": "linux",
                                "Architecture": "amd64",
                            }
                        ]
                    ),
                    stderr="",
                )
            if command[:3] == ["docker", "context", "show"]:
                return SimpleNamespace(returncode=0, stdout="default\n", stderr="")
            if command[:2] == ["docker", "info"]:
                return SimpleNamespace(returncode=0, stdout=json.dumps({"Architecture": "amd64", "OSType": "linux"}), stderr="")
            if command[:2] == ["uname", "-m"]:
                return SimpleNamespace(returncode=0, stdout="x86_64\n", stderr="")
            if command[:2] == ["uname", "-s"]:
                return SimpleNamespace(returncode=0, stdout="Linux\n", stderr="")
            if command[:2] == ["docker", "events"]:
                timestamp = datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")
                return SimpleNamespace(
                    returncode=0,
                    stdout=(
                        f"{timestamp} image pull example/strict:1.0 "
                        "(name=example/strict, org.opencontainers.image.ref.name=ubuntu)\n"
                    ),
                    stderr="",
                )
            evidence = next(
                (
                    Path(part.split("=", 1)[1])
                    for part in command
                    if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")
                ),
                None,
            )
            assert evidence is not None
            evidence.joinpath("startup.marker").write_text("Container workload ready\n", encoding="utf-8")
            evidence.joinpath("workload.image-id").write_text("sha256:" + "a" * 64 + "\n", encoding="utf-8")
            evidence.joinpath("TEST-StrictServerTest.xml").write_text(
                '<testsuite name="io.bluetape4k.testcontainers.storage.StrictServerTest" tests="1" skipped="0" failures="0" errors="0">'
                '<testcase classname="io.bluetape4k.testcontainers.storage.StrictServerTest" name="representativeStartupAndWorkload"/>'
                "</testsuite>",
                encoding="utf-8",
            )
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [strict_entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=1,
                scope="family",
            ).run()

        result = summary["results"][0]
        self.assertEqual("success", result["status"])
        self.assertEqual("example/strict:1.0", result["pull"]["event_ref"])
        self.assertEqual("post_pull_inspect", result["pull"]["event_image_id_source"])
        self.assertEqual(
            [],
            verify_release_summary(
                summary,
                expected_coverage="1/1",
                platform_id="amd64",
                expected_tag="1.0",
                expected_architecture="amd64",
            ),
        )

    def test_strict_family_without_marker_is_blocked(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = GateRunner(
                [strict_entry()],
                Path(directory),
                command_runner=lambda command, timeout_seconds, **_: SimpleNamespace(
                    returncode=0, stdout="BUILD SUCCESSFUL", stderr=""
                ),
                max_attempts=1,
            ).run()["results"][0]
            self.assertEqual("blocked", result["status"])

    def test_strict_pull_infrastructure_failure_retries_the_complete_pull_chain(self) -> None:
        calls: list[list[str]] = []
        pull_attempts = 0

        def command_runner(command: list[str], timeout_seconds: int, **_: object) -> SimpleNamespace:
            nonlocal pull_attempts
            calls.append(command)
            if command[:2] == ["docker", "pull"]:
                pull_attempts += 1
                if pull_attempts == 1:
                    return SimpleNamespace(returncode=1, stdout="", stderr="connection refused")
                return SimpleNamespace(returncode=0, stdout="Status: Downloaded newer image", stderr="")
            if command[:3] == ["docker", "image", "inspect"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps([{"Id": "sha256:" + "a" * 64, "RepoDigests": ["example/strict@sha256:" + "b" * 64], "Os": "linux", "Architecture": "amd64"}]),
                    stderr="",
                )
            if command[:3] == ["docker", "context", "show"]:
                return SimpleNamespace(returncode=0, stdout="default\n", stderr="")
            if command[:2] == ["docker", "info"]:
                return SimpleNamespace(returncode=0, stdout=json.dumps({"Architecture": "amd64", "OSType": "linux"}), stderr="")
            if command[:2] == ["uname", "-m"]:
                return SimpleNamespace(returncode=0, stdout="x86_64\n", stderr="")
            if command[:2] == ["uname", "-s"]:
                return SimpleNamespace(returncode=0, stdout="Linux\n", stderr="")
            if command[:2] == ["docker", "events"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps({
                        "timeNano": str(time.time_ns()),
                        "id": "sha256:" + "a" * 64,
                        "from": "example/strict:1.0",
                        "Actor": {"ID": "sha256:" + "a" * 64, "Attributes": {"name": "example/strict:1.0"}},
                    }) + "\n",
                    stderr="",
                )
            evidence = next((Path(part.split("=", 1)[1]) for part in command if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")), None)
            assert evidence is not None
            evidence.joinpath("startup.marker").write_text("Container workload ready\n", encoding="utf-8")
            evidence.joinpath("workload.image-id").write_text("sha256:" + "a" * 64 + "\n", encoding="utf-8")
            evidence.joinpath("TEST-StrictServerTest.xml").write_text(
                '<testsuite name="io.bluetape4k.testcontainers.storage.StrictServerTest" tests="1" skipped="0" failures="0" errors="0">'
                '<testcase classname="io.bluetape4k.testcontainers.storage.StrictServerTest" name="representativeStartupAndWorkload"/>'
                '</testsuite>',
                encoding="utf-8",
            )
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

        with tempfile.TemporaryDirectory() as directory:
            result = GateRunner(
                [strict_entry()],
                Path(directory),
                command_runner=command_runner,
                max_attempts=2,
                scope="family",
            ).run()["results"][0]

        self.assertEqual("success", result["status"])
        self.assertEqual(2, pull_attempts)
        self.assertEqual(2, len(result["attempts"]))
        self.assertEqual("infrastructure_failure", result["attempts"][0]["status"])
        self.assertEqual("success", result["attempts"][1]["status"])
        self.assertEqual(
            1,
            sum(
                command and command[0] == "./gradlew" and any(
                    part.startswith("-Dtestcontainers.image-gate.evidence-dir=") for part in command
                )
                for command in calls
            ),
        )

    def test_arm64_strict_command_is_fixed_and_excludes_mock_jib(self) -> None:
        value = strict_entry()
        value["platforms"] = [
            {"id": "arm64", "os": "linux", "architecture": "arm64", "tag": "1.0-arm64", "runner": "ubuntu-24.04-arm"},
        ]
        value["defaultPlatformId"] = "arm64"
        value["_selected_platform_id"] = "arm64"
        with tempfile.TemporaryDirectory() as directory:
            runner = GateRunner([value], Path(directory), scope="family")
            command = runner._command(value, Path(directory) / "attempt")
        self.assertEqual("-Dtestcontainers.image-gate.evidence-dir", command[1].split("=", 1)[0])
        self.assertIn("--tests", command)
        self.assertIn("io.bluetape4k.testcontainers.storage.StrictServerTest.representativeStartupAndWorkload", command)
        self.assertIn("--rerun-tasks", command)
        self.assertEqual(2, command.count("-x"))
        self.assertNotIn("jibDockerBuild", command[:3])

    def test_release_verifier_rejects_partial_or_missing_evidence(self) -> None:
        summary = {
            "schema_version": 2,
            "coverage": "1/1",
            "release_gate": True,
            "status": "success",
            "selected": 1,
            "blocked": 0,
            "product_failure": 0,
            "infrastructure_failure": 0,
            "platforms": [{"platform_id": "arm64", "status": "success", "image_ref": "example/strict:1.0-arm64", "expected": {"os": "linux", "tag": "1.0-arm64", "architecture": "arm64"}, "observed": {"image_tag": "1.0-arm64", "image_architecture": "arm64", "image_os": "linux", "runner_os": "linux", "daemon_os": "linux"}, "pull": {}, "junit": {}, "workload_image": {}, "family_artifact": "strict-family.json"}],
        }
        errors = verify_release_summary(summary, expected_coverage="1/1", platform_id="arm64", expected_tag="1.0-arm64", expected_architecture="arm64")
        self.assertIn("pull event and digest evidence are required", errors)
        self.assertIn("expected runner label mismatch", errors)
        self.assertIn("runner/daemon architecture evidence is required", errors)
        summary["platforms"][0]["observed"].pop("image_tag")
        self.assertIn(
            "expected/observed tag mismatch",
            verify_release_summary(summary, expected_coverage="1/1", platform_id="arm64", expected_tag="1.0-arm64", expected_architecture="arm64"),
        )

    def test_budget_formula_and_52_family_artifact_guard(self) -> None:
        self.assertEqual(
            318,
            worst_case_budget_minutes(
                generic_families=51,
                generic_attempts=1,
                generic_pull_minutes=1,
                generic_test_minutes=4,
                generic_diagnostic_minutes=0.5,
                strict_families=1,
                strict_attempts=1,
                strict_pull_minutes=1,
                strict_test_minutes=6,
                strict_diagnostic_minutes=0.5,
                setup_slack_minutes=30,
            ),
        )
        self.assertEqual(
            84,
            worst_case_budget_minutes(
                generic_families=0,
                generic_attempts=1,
                generic_pull_minutes=0,
                generic_test_minutes=0,
                generic_diagnostic_minutes=0,
                strict_families=1,
                strict_attempts=2,
                strict_pull_minutes=5,
                strict_test_minutes=30,
                strict_diagnostic_minutes=2,
                setup_slack_minutes=10,
            ),
        )
        with tempfile.TemporaryDirectory() as directory:
            entries = [
                entry(f"Family{index}", release_required=False) | {"id": f"family-{index}"}
                for index in range(52)
            ]
            summary = GateRunner(
                entries,
                Path(directory),
                command_runner=lambda command, timeout_seconds, **_: SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr=""),
                max_attempts=1,
            ).run()
            artifact_size = sum(path.stat().st_size for path in Path(directory).iterdir() if path.is_file())
            self.assertEqual("52/52", summary["selected_coverage"])
            self.assertLessEqual(artifact_size, 8 * 1024 * 1024)


if __name__ == "__main__":
    unittest.main(verbosity=2)
