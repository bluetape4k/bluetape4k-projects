#!/usr/bin/env python3
"""Negative-fixture tests for validate-lock-performance.py."""

import copy
import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("validate-lock-performance.py")
SPEC = importlib.util.spec_from_file_location("validate_lock_performance", SCRIPT)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


def counts(value):
    return {
        "runtimeTasks": value,
        "watchdogs": value,
        "waiters": value,
        "queueEntries": value,
        "requestHolds": value,
    }


def valid_report():
    dispatches = [256] * 39 + [16]
    return {
        "schemaVersion": 1,
        "status": "passed",
        "tag": "coordination-lock-performance",
        "publication": "sibling-temp-fsync-atomic-move",
        "runId": "run-123",
        "generatedAt": "2026-07-26T00:00:00Z",
        "metadata": {
            "redisImage": "redis:8",
            "redisVersion": "8.0",
            "javaVersion": "21",
            "kotlinVersion": "2.3",
            "lettuceVersion": "6.8",
            "osName": "Linux",
            "osVersion": "1",
            "osArch": "aarch64",
            "cpuCount": 8,
        },
        "protocol": {
            "usesDedicatedRedis": True,
            "usesDedicatedConnections": True,
            "usesDedicatedExecutors": True,
            "usesSeparatePingConnection": True,
            "usesSeparatePingExecutor": True,
            "explicitCleanup": True,
        },
        "workload": {
            "warmupAttempts": 10_000,
            "measuredAttempts": 50_000,
            "workers": 8,
            "measurementWindows": 5,
        },
        "commandBudget": {"cold": 2, "warm": 1, "workload": 50_000},
        "fairCleanup": {"configuredBatchCap": 64, "observedBatch": 64, "remainingAfterFirstPass": 1},
        "spin": {
            "configuredMaxAttemptsPerSecond": 100,
            "observedAttempts": 20,
            "elapsedMillis": 1000.0,
            "observedAttemptsPerSecond": 20.0,
            "busyLoopAttempts": 0,
        },
        "latency": {
            "hotLockWaitP50Millis": 0.3,
            "hotLockWaitP95Millis": 1.0,
            "redisCommandP50Millis": 0.3,
            "redisCommandP95Millis": 1.0,
        },
        "responsiveness": {"pingP50Millis": 0.2, "pingP95Millis": 0.8, "sampleCount": 10, "errors": 0},
        "retainedState": {
            "baseline": counts(0),
            "peak": counts(1),
            "final": counts(0),
            "caps": counts(10_000),
            "windows": [counts(1), counts(1), counts(1), counts(1), counts(1)],
            "hasMonotonicGrowth": False,
        },
        "scheduler": {
            "registrationCount": 10_000,
            "maxPerTick": 256,
            "backlogCadenceMillis": 25,
            "tickDispatches": dispatches,
            "due": 10_000,
            "dispatched": 10_000,
            "late": 0,
            "missed": 0,
            "maximumBacklog": 9744,
            "calculatedDrainTicks": 40,
            "calculatedDrainMillis": 975,
            "redisCompletionSafetyMarginMillis": 1000,
            "watchdogTickDispatch": 256,
            "watchdogDue": 256,
            "watchdogDispatched": 256,
            "watchdogLate": 0,
            "watchdogMissed": 0,
            "finalTasks": 0,
            "finalWatchdogs": 0,
        },
        "errors": 0,
        "timeouts": 0,
        "cleanup": {
            "redisStateEntries": 0,
            "runtimeTasks": 0,
            "watchdogs": 0,
            "executorsTerminated": True,
            "connectionsClosed": True,
        },
    }


class ValidatorTest(unittest.TestCase):
    def assert_invalid(self, mutate, reason):
        report = valid_report()
        mutate(report)
        with self.assertRaises(validator.ValidationError) as raised:
            validator.validate_report(report)
        self.assertEqual(reason, raised.exception.reason_code)

    def test_accepts_canonical_report(self):
        self.assertTrue(validator.validate_report(valid_report()))

    def test_rejects_wrong_tag_and_non_atomic_publication(self):
        self.assert_invalid(lambda report: report.update(tag="performance"), "TAG")
        self.assert_invalid(lambda report: report.update(publication="direct-write"), "PUBLICATION")

    def test_rejects_protocol_dimension_drift(self):
        self.assert_invalid(lambda report: report["workload"].update(warmupAttempts=9999), "PROTOCOL")
        self.assert_invalid(lambda report: report["protocol"].update(usesSeparatePingConnection=False), "PROTOCOL")

    def test_rejects_command_budgets(self):
        self.assert_invalid(lambda report: report["commandBudget"].update(warm=2), "COMMAND_BUDGET")
        self.assert_invalid(lambda report: report["commandBudget"].update(cold=3), "COMMAND_BUDGET")

    def test_rejects_fair_cleanup_and_spin_bounds(self):
        self.assert_invalid(lambda report: report["fairCleanup"].update(observedBatch=65), "FAIR_CLEANUP")
        self.assert_invalid(lambda report: report["spin"].update(observedAttemptsPerSecond=101), "SPIN")
        self.assert_invalid(lambda report: report["spin"].update(busyLoopAttempts=1), "SPIN")

    def test_rejects_invalid_latency_and_ping(self):
        self.assert_invalid(lambda report: report["latency"].update(hotLockWaitP50Millis=2.0), "LATENCY")
        self.assert_invalid(lambda report: report["responsiveness"].update(errors=1), "PING")

    def test_rejects_errors_and_timeouts(self):
        self.assert_invalid(lambda report: report.update(errors=1), "WORKLOAD")
        self.assert_invalid(lambda report: report.update(timeouts=1), "WORKLOAD")

    def test_rejects_retained_state_leaks_caps_and_growth(self):
        self.assert_invalid(lambda report: report["retainedState"]["final"].update(queueEntries=1), "RETAINED_STATE")
        self.assert_invalid(lambda report: report["retainedState"]["peak"].update(watchdogs=10_001), "RETAINED_STATE")
        self.assert_invalid(
            lambda report: report["retainedState"].update(
                windows=[counts(1), counts(2), counts(3), counts(4), counts(5)],
                hasMonotonicGrowth=True,
            ),
            "RETAINED_STATE",
        )

    def test_rejects_scheduler_bounds(self):
        self.assert_invalid(lambda report: report["scheduler"].update(maxPerTick=257), "SCHEDULER")
        self.assert_invalid(lambda report: report["scheduler"].update(backlogCadenceMillis=26), "SCHEDULER")
        self.assert_invalid(lambda report: report["scheduler"].update(missed=1), "SCHEDULER")
        self.assert_invalid(lambda report: report["scheduler"].update(watchdogTickDispatch=257), "SCHEDULER")
        self.assert_invalid(lambda report: report["scheduler"].update(watchdogMissed=1), "SCHEDULER")
        self.assert_invalid(lambda report: report["scheduler"].update(redisCompletionSafetyMarginMillis=999), "SCHEDULER")

    def test_rejects_incomplete_cleanup(self):
        self.assert_invalid(lambda report: report["cleanup"].update(redisStateEntries=1), "CLEANUP")
        self.assert_invalid(lambda report: report["cleanup"].update(connectionsClosed=False), "CLEANUP")

    def test_rejects_sensitive_fields_and_values(self):
        self.assert_invalid(lambda report: report["metadata"].update(redisUrl="redis://localhost"), "SENSITIVE_FIELD")
        self.assert_invalid(lambda report: report["metadata"].update(ownerId="opaque-owner"), "SENSITIVE_FIELD")
        self.assert_invalid(lambda report: report.update(env={"PATH": "/bin"}), "SENSITIVE_FIELD")
        self.assert_invalid(
            lambda report: report["metadata"].update(redisVersion="redis://user:pass@example.test"),
            "SENSITIVE_VALUE",
        )

    def test_cli_success_and_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            valid_path = root / "valid.json"
            invalid_path = root / "invalid.json"
            valid_path.write_text(json.dumps(valid_report()), encoding="utf-8")
            invalid = valid_report()
            invalid["timeouts"] = 1
            invalid_path.write_text(json.dumps(invalid), encoding="utf-8")
            success = subprocess.run(
                [sys.executable, str(SCRIPT), str(valid_path)],
                check=False,
                capture_output=True,
                text=True,
            )
            failure = subprocess.run(
                [sys.executable, str(SCRIPT), str(invalid_path)],
                check=False,
                capture_output=True,
                text=True,
            )
            self.assertEqual(0, success.returncode, success.stderr)
            self.assertEqual(1, failure.returncode)
            self.assertIn("WORKLOAD", failure.stderr)


if __name__ == "__main__":
    unittest.main()
