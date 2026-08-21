#!/usr/bin/env python3
"""Unit tests for the Docker-backed image gate runner."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

from scripts.run_testcontainers_image_gate import (
    GateRunner,
    classify_failure,
    redact,
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


class TestRunTestcontainersImageGate(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
