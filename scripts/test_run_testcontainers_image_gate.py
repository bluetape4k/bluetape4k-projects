#!/usr/bin/env python3
"""Unit tests for the Docker-backed image gate runner."""

from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

from scripts.run_testcontainers_image_gate import (
    GateRunner,
    classify_failure,
    redact,
    verify_release_summary,
    worst_case_budget_minutes,
)


def entry(server: str = "FlociServer") -> dict[str, object]:
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
        "releaseRequired": True,
    }


def strict_entry() -> dict[str, object]:
    value = entry("Ignite2Server")
    value.update(
        {
            "id": "ignite2",
            "image": "apacheignite/ignite",
            "tag": "2.18.0",
            "testPattern": "io.bluetape4k.testcontainers.storage.Ignite2ServerTest",
            "workloadTestPattern": "io.bluetape4k.testcontainers.storage.Ignite2ServerTest.representativeStartupAndWorkload",
            "executionEvidenceRequired": True,
            "pullEvidenceRequired": True,
            "platforms": [
                {"id": "amd64", "os": "linux", "architecture": "amd64", "tag": "2.18.0", "runner": "ubuntu-24.04"},
            ],
            "defaultPlatformId": "amd64",
            "platformTimeouts": {"amd64": {"testMinutes": 6, "clientConnectSeconds": 30, "clientRequestSeconds": 30}},
        }
    )
    return value


class TestRunTestcontainersImageGate(unittest.TestCase):
    def test_runtime_environment_removes_docker_credentials_and_overrides(self) -> None:
        original = {
            key: os.environ.get(key)
            for key in ("DOCKER_AUTH_CONFIG", "TESTCONTAINERS_REGISTRY_MIRROR", "DOCKER_HOST", "DOCKER_CONTEXT")
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
            return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

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
                command_runner=lambda command, timeout_seconds: SimpleNamespace(
                    returncode=0, stdout=long_success, stderr=""
                ),
                max_attempts=1,
            ).run()
            self.assertEqual("success", summary["results"][0]["status"])
            self.assertTrue(summary["release_gate"])

    def test_secret_redaction_applies_to_artifact_text(self) -> None:
        safe = redact("TOKEN=super-secret password=hunter2 Authorization: Bearer abc123")
        self.assertNotIn("super-secret", safe)
        self.assertNotIn("hunter2", safe)
        self.assertNotIn("abc123", safe)
        self.assertIn("<redacted>", safe)

    def test_summary_json_is_machine_readable(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            summary = GateRunner(
                [entry()],
                Path(directory),
                command_runner=lambda command, timeout_seconds: SimpleNamespace(
                    returncode=0, stdout="BUILD SUCCESSFUL", stderr=""
                ),
                max_attempts=1,
            ).run()
            read_back = json.loads((Path(directory) / "summary.json").read_text())
            self.assertEqual(summary["manifest_digest"], read_back["manifest_digest"])

    def test_strict_family_requires_pull_platform_and_workload_evidence(self) -> None:
        calls: list[list[str]] = []

        def command_runner(command: list[str], timeout_seconds: int, **_: object) -> SimpleNamespace:
            calls.append(command)
            if command[:2] == ["docker", "pull"]:
                return SimpleNamespace(returncode=0, stdout="Status: Downloaded newer image", stderr="")
            if command[:3] == ["docker", "image", "inspect"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps([{"Id": "sha256:" + "a" * 64, "RepoDigests": ["apacheignite/ignite@sha256:" + "b" * 64], "Os": "linux", "Architecture": "amd64"}]),
                    stderr="",
                )
            if command[:3] == ["docker", "context", "show"]:
                return SimpleNamespace(returncode=0, stdout="default\n", stderr="")
            if command[:2] == ["docker", "info"]:
                return SimpleNamespace(returncode=0, stdout=json.dumps({"Architecture": "amd64"}), stderr="")
            if command[:2] == ["uname", "-m"]:
                return SimpleNamespace(returncode=0, stdout="x86_64\n", stderr="")
            if command[:2] == ["docker", "events"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps({
                        "id": "sha256:" + "a" * 64,
                        "from": "apacheignite/ignite:2.18.0",
                        "Actor": {"ID": "sha256:" + "a" * 64, "Attributes": {"name": "apacheignite/ignite:2.18.0"}},
                    }) + "\n",
                    stderr="",
                )
            evidence = next((Path(part.split("=", 1)[1]) for part in command if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")), None)
            assert evidence is not None
            evidence.joinpath("startup.marker").write_text("Ignite node started OK\n", encoding="utf-8")
            evidence.joinpath("workload.image-id").write_text("sha256:" + "a" * 64 + "\n", encoding="utf-8")
            evidence.joinpath("TEST-Ignite2ServerTest.xml").write_text(
                '<testsuite name="io.bluetape4k.testcontainers.storage.Ignite2ServerTest" tests="1" skipped="0" failures="0" errors="0">'
                '<testcase classname="io.bluetape4k.testcontainers.storage.Ignite2ServerTest" name="representativeStartupAndWorkload"/>'
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
            self.assertEqual("apacheignite/ignite:2.18.0", result["pull"]["event_ref"])
            self.assertEqual(1, result["junit"]["workload_tests"])
            self.assertTrue(result["startup"]["ready"])
            self.assertEqual(1, sum(command[:2] == ["docker", "pull"] for command in calls))
            event_command = next(command for command in calls if command[:2] == ["docker", "events"])
            self.assertIn("--since", event_command)
            self.assertIn("--until", event_command)
            since = int(event_command[event_command.index("--since") + 1])
            self.assertGreater(since, 1_000_000_000)
            event_command = next(command for command in calls if command[:2] == ["docker", "events"])
            self.assertIn("--since", event_command)
            self.assertIn("--until", event_command)

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
                    stdout=json.dumps([{"Id": "sha256:" + "a" * 64, "RepoDigests": ["apacheignite/ignite@sha256:" + "b" * 64], "Os": "linux", "Architecture": "amd64"}]),
                    stderr="",
                )
            if command[:3] == ["docker", "context", "show"]:
                return SimpleNamespace(returncode=0, stdout="default\n", stderr="")
            if command[:2] == ["docker", "info"]:
                return SimpleNamespace(returncode=0, stdout=json.dumps({"Architecture": "amd64"}), stderr="")
            if command[:2] == ["uname", "-m"]:
                return SimpleNamespace(returncode=0, stdout="x86_64\n", stderr="")
            if command[:2] == ["docker", "events"]:
                return SimpleNamespace(
                    returncode=0,
                    stdout=json.dumps({
                        "id": "sha256:" + "a" * 64,
                        "from": "apacheignite/ignite:2.18.0",
                        "Actor": {"ID": "sha256:" + "a" * 64, "Attributes": {"name": "apacheignite/ignite:2.18.0"}},
                    }) + "\n",
                    stderr="",
                )
            evidence = next((Path(part.split("=", 1)[1]) for part in command if part.startswith("-Dtestcontainers.image-gate.evidence-dir=")), None)
            assert evidence is not None
            evidence.joinpath("startup.marker").write_text("Ignite node started OK\n", encoding="utf-8")
            evidence.joinpath("workload.image-id").write_text("sha256:" + "a" * 64 + "\n", encoding="utf-8")
            evidence.joinpath("TEST-Ignite2ServerTest.xml").write_text(
                '<testsuite name="io.bluetape4k.testcontainers.storage.Ignite2ServerTest" tests="1" skipped="0" failures="0" errors="0">'
                '<testcase classname="io.bluetape4k.testcontainers.storage.Ignite2ServerTest" name="representativeStartupAndWorkload"/>'
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
            {"id": "arm64", "os": "linux", "architecture": "arm64", "tag": "2.18.0-arm64", "runner": "ubuntu-24.04-arm"},
        ]
        value["defaultPlatformId"] = "arm64"
        value["_selected_platform_id"] = "arm64"
        with tempfile.TemporaryDirectory() as directory:
            runner = GateRunner([value], Path(directory), scope="family")
            command = runner._command(value, Path(directory) / "attempt")
        self.assertEqual("-Dtestcontainers.image-gate.evidence-dir", command[1].split("=", 1)[0])
        self.assertIn("--tests", command)
        self.assertIn("io.bluetape4k.testcontainers.storage.Ignite2ServerTest.representativeStartupAndWorkload", command)
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
            "platforms": [{"platform_id": "arm64", "status": "success", "image_ref": "apacheignite/ignite:2.18.0-arm64", "expected": {"os": "linux", "tag": "2.18.0-arm64", "architecture": "arm64"}, "observed": {"image_tag": "2.18.0-arm64", "image_architecture": "arm64", "image_os": "linux", "runner_os": "linux", "daemon_os": "linux"}, "pull": {}, "junit": {}, "workload_image": {}, "family_artifact": "ignite2.json"}],
        }
        errors = verify_release_summary(summary, expected_coverage="1/1", platform_id="arm64", expected_tag="2.18.0-arm64", expected_architecture="arm64")
        self.assertIn("pull event and digest evidence are required", errors)
        summary["platforms"][0]["observed"].pop("image_tag")
        self.assertIn(
            "expected/observed tag mismatch",
            verify_release_summary(summary, expected_coverage="1/1", platform_id="arm64", expected_tag="2.18.0-arm64", expected_architecture="arm64"),
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
            entries = [entry(f"Family{index}") | {"id": f"family-{index}"} for index in range(52)]
            summary = GateRunner(
                entries,
                Path(directory),
                command_runner=lambda command, timeout_seconds, **_: SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr=""),
                max_attempts=1,
            ).run()
            artifact_size = sum(path.stat().st_size for path in Path(directory).iterdir() if path.is_file())
            self.assertEqual("52/52", summary["coverage"])
            self.assertLessEqual(artifact_size, 8 * 1024 * 1024)


if __name__ == "__main__":
    unittest.main(verbosity=2)
