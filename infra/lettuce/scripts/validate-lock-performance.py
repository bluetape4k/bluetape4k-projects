#!/usr/bin/env python3
"""Validate the coordination lock characterization report without third-party packages."""

import argparse
import json
import math
import re
import sys
from pathlib import Path


SCHEMA_VERSION = 1
EXPECTED_TAG = "coordination-lock-performance"
STATE_FIELDS = ("runtimeTasks", "watchdogs", "waiters", "queueEntries", "requestHolds")
SENSITIVE_KEY = re.compile(
    r"(^env$|api.?key|access.?key|authorization|cookie|credential|environment|identity|"
    r"lock.?key|owner.?id|password|private.?key|redis.?key|request.?id|secret|session|token|url|uri)",
    re.IGNORECASE,
)
SENSITIVE_VALUE = re.compile(
    r"(://[^/\s:@]+:[^/\s@]+@|Bearer\s+\S+|AKIA[0-9A-Z]{16}|"
    r"-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|"
    r"(?:password|secret|token|credential|api[_-]?key|access[_-]?key)\s*[=:])",
    re.IGNORECASE,
)


class ValidationError(ValueError):
    def __init__(self, reason_code, detail):
        self.reason_code = reason_code
        self.detail = detail
        super().__init__(f"{reason_code}: {detail}")


def fail(reason_code, detail):
    raise ValidationError(reason_code, detail)


def require_mapping(value, label, reason="SCHEMA"):
    if not isinstance(value, dict):
        fail(reason, f"{label} must be an object")
    return value


def require_keys(value, keys, label, reason="SCHEMA"):
    missing = sorted(set(keys) - set(value))
    if missing:
        fail(reason, f"{label} missing fields: {', '.join(missing)}")


def require_integer(value, label, minimum=0, reason="SCHEMA"):
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        fail(reason, f"{label} must be an integer >= {minimum}")
    return value


def require_number(value, label, *, positive=False, reason="SCHEMA"):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        fail(reason, f"{label} must be finite")
    if positive and value <= 0:
        fail(reason, f"{label} must be positive")
    return float(value)


def forbid_sensitive(value, path="$"):
    if isinstance(value, dict):
        for key, child in value.items():
            if not isinstance(key, str):
                fail("SENSITIVE_FIELD", f"{path} contains a non-string key")
            if SENSITIVE_KEY.search(key):
                fail("SENSITIVE_FIELD", f"{path}.{key} is forbidden")
            forbid_sensitive(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            forbid_sensitive(child, f"{path}[{index}]")
    elif isinstance(value, str) and SENSITIVE_VALUE.search(value):
        fail("SENSITIVE_VALUE", f"{path} contains a credential-like value")


def state_counts(value, label):
    value = require_mapping(value, label, "RETAINED_STATE")
    require_keys(value, STATE_FIELDS, label, "RETAINED_STATE")
    return {
        field: require_integer(value[field], f"{label}.{field}", reason="RETAINED_STATE")
        for field in STATE_FIELDS
    }


def validate_metadata(report):
    require_keys(
        report,
        (
            "schemaVersion",
            "status",
            "tag",
            "publication",
            "runId",
            "generatedAt",
            "metadata",
            "protocol",
            "workload",
            "commandBudget",
            "fairCleanup",
            "spin",
            "latency",
            "responsiveness",
            "retainedState",
            "scheduler",
            "errors",
            "timeouts",
            "cleanup",
        ),
        "report",
    )
    if report["schemaVersion"] != SCHEMA_VERSION:
        fail("SCHEMA", f"schemaVersion must be {SCHEMA_VERSION}")
    if report["status"] != "passed":
        fail("STATUS", "status must be passed")
    if report["tag"] != EXPECTED_TAG:
        fail("TAG", f"tag must be {EXPECTED_TAG}")
    if report["publication"] != "sibling-temp-fsync-atomic-move":
        fail("PUBLICATION", "report must use sibling temp, fsync, and atomic move")
    for field in ("runId", "generatedAt"):
        if not isinstance(report[field], str) or not report[field]:
            fail("SCHEMA", f"{field} must be non-empty")

    metadata = require_mapping(report["metadata"], "metadata")
    require_keys(
        metadata,
        (
            "redisImage",
            "redisVersion",
            "javaVersion",
            "kotlinVersion",
            "lettuceVersion",
            "osName",
            "osVersion",
            "osArch",
            "cpuCount",
        ),
        "metadata",
    )
    for field in (
        "redisImage",
        "redisVersion",
        "javaVersion",
        "kotlinVersion",
        "lettuceVersion",
        "osName",
        "osVersion",
        "osArch",
    ):
        if not isinstance(metadata[field], str) or not metadata[field]:
            fail("SCHEMA", f"metadata.{field} must be non-empty")
    require_integer(metadata["cpuCount"], "metadata.cpuCount", minimum=1)


def validate_protocol(report):
    protocol = require_mapping(report["protocol"], "protocol", "PROTOCOL")
    require_keys(
        protocol,
        (
            "usesDedicatedRedis",
            "usesDedicatedConnections",
            "usesDedicatedExecutors",
            "usesSeparatePingConnection",
            "usesSeparatePingExecutor",
            "explicitCleanup",
        ),
        "protocol",
        "PROTOCOL",
    )
    for field, enabled in protocol.items():
        if not isinstance(enabled, bool) or not enabled:
            fail("PROTOCOL", f"protocol.{field} must be true")

    workload = require_mapping(report["workload"], "workload", "PROTOCOL")
    expected = {
        "warmupAttempts": 10_000,
        "measuredAttempts": 50_000,
        "workers": 8,
        "measurementWindows": 5,
    }
    require_keys(workload, expected, "workload", "PROTOCOL")
    for field, value in expected.items():
        if workload[field] != value:
            fail("PROTOCOL", f"workload.{field} must be {value}")


def validate_command_and_rate_bounds(report):
    budget = require_mapping(report["commandBudget"], "commandBudget", "COMMAND_BUDGET")
    require_keys(budget, ("cold", "warm", "workload"), "commandBudget", "COMMAND_BUDGET")
    if budget["cold"] != 2 or budget["warm"] != 1:
        fail("COMMAND_BUDGET", "cold/warm acquire budgets must be exactly 2/1")
    if budget["workload"] != 50_000:
        fail("COMMAND_BUDGET", "measured workload must issue exactly one script command per attempt")

    fair = require_mapping(report["fairCleanup"], "fairCleanup", "FAIR_CLEANUP")
    require_keys(fair, ("configuredBatchCap", "observedBatch", "remainingAfterFirstPass"), "fairCleanup", "FAIR_CLEANUP")
    if fair["configuredBatchCap"] != 64 or fair["observedBatch"] != 64 or fair["remainingAfterFirstPass"] != 1:
        fail("FAIR_CLEANUP", "fair cleanup must remove exactly the default 64-item batch")

    spin = require_mapping(report["spin"], "spin", "SPIN")
    require_keys(
        spin,
        (
            "configuredMaxAttemptsPerSecond",
            "observedAttempts",
            "elapsedMillis",
            "observedAttemptsPerSecond",
            "busyLoopAttempts",
        ),
        "spin",
        "SPIN",
    )
    maximum = require_number(
        spin["configuredMaxAttemptsPerSecond"],
        "spin.configuredMaxAttemptsPerSecond",
        positive=True,
        reason="SPIN",
    )
    observed = require_number(
        spin["observedAttemptsPerSecond"],
        "spin.observedAttemptsPerSecond",
        positive=True,
        reason="SPIN",
    )
    require_integer(spin["observedAttempts"], "spin.observedAttempts", minimum=1, reason="SPIN")
    require_number(spin["elapsedMillis"], "spin.elapsedMillis", positive=True, reason="SPIN")
    if observed > maximum or maximum > 100:
        fail("SPIN", "spin attempt rate exceeded 100 attempts/second")
    if spin["busyLoopAttempts"] != 0:
        fail("SPIN", "busy-loop attempts must be zero")


def validate_latency_and_responsiveness(report):
    latency = require_mapping(report["latency"], "latency", "LATENCY")
    require_keys(
        latency,
        (
            "hotLockWaitP50Millis",
            "hotLockWaitP95Millis",
            "redisCommandP50Millis",
            "redisCommandP95Millis",
        ),
        "latency",
        "LATENCY",
    )
    wait50 = require_number(latency["hotLockWaitP50Millis"], "latency.hotLockWaitP50Millis", positive=True, reason="LATENCY")
    wait95 = require_number(latency["hotLockWaitP95Millis"], "latency.hotLockWaitP95Millis", positive=True, reason="LATENCY")
    command50 = require_number(latency["redisCommandP50Millis"], "latency.redisCommandP50Millis", positive=True, reason="LATENCY")
    command95 = require_number(latency["redisCommandP95Millis"], "latency.redisCommandP95Millis", positive=True, reason="LATENCY")
    if wait50 > wait95 or command50 > command95:
        fail("LATENCY", "p50 must not exceed p95")

    ping = require_mapping(report["responsiveness"], "responsiveness", "PING")
    require_keys(ping, ("pingP50Millis", "pingP95Millis", "sampleCount", "errors"), "responsiveness", "PING")
    ping50 = require_number(ping["pingP50Millis"], "responsiveness.pingP50Millis", positive=True, reason="PING")
    ping95 = require_number(ping["pingP95Millis"], "responsiveness.pingP95Millis", positive=True, reason="PING")
    if ping50 > ping95:
        fail("PING", "PING p50 must not exceed p95")
    require_integer(ping["sampleCount"], "responsiveness.sampleCount", minimum=1, reason="PING")
    if ping["errors"] != 0:
        fail("PING", "PING errors must be zero")
    if report["errors"] != 0 or report["timeouts"] != 0:
        fail("WORKLOAD", "workload errors and timeouts must be zero")


def validate_retained_state(report):
    retained = require_mapping(report["retainedState"], "retainedState", "RETAINED_STATE")
    require_keys(
        retained,
        ("baseline", "peak", "final", "caps", "windows", "hasMonotonicGrowth"),
        "retainedState",
        "RETAINED_STATE",
    )
    baseline = state_counts(retained["baseline"], "retainedState.baseline")
    peak = state_counts(retained["peak"], "retainedState.peak")
    final = state_counts(retained["final"], "retainedState.final")
    caps = state_counts(retained["caps"], "retainedState.caps")
    if final != baseline:
        fail("RETAINED_STATE", "final counts must equal baseline")
    for field in STATE_FIELDS:
        if peak[field] > caps[field]:
            fail("RETAINED_STATE", f"peak {field} exceeded its cap")

    windows = retained["windows"]
    if not isinstance(windows, list) or len(windows) != 5:
        fail("RETAINED_STATE", "exactly five measurement windows are required")
    windows = [state_counts(window, f"retainedState.windows[{index}]") for index, window in enumerate(windows)]
    monotonic = any(
        all(windows[index + 1][field] > windows[index][field] for index in range(4))
        for field in STATE_FIELDS
    )
    if monotonic or retained["hasMonotonicGrowth"] is not False:
        fail("RETAINED_STATE", "retained state must not grow monotonically across all five windows")


def validate_scheduler_and_cleanup(report):
    scheduler = require_mapping(report["scheduler"], "scheduler", "SCHEDULER")
    require_keys(
        scheduler,
        (
            "registrationCount",
            "maxPerTick",
            "backlogCadenceMillis",
            "tickDispatches",
            "due",
            "dispatched",
            "late",
            "missed",
            "maximumBacklog",
            "calculatedDrainTicks",
            "calculatedDrainMillis",
            "redisCompletionSafetyMarginMillis",
            "watchdogTickDispatch",
            "watchdogDue",
            "watchdogDispatched",
            "watchdogLate",
            "watchdogMissed",
            "finalTasks",
            "finalWatchdogs",
        ),
        "scheduler",
        "SCHEDULER",
    )
    if scheduler["registrationCount"] != 10_000 or scheduler["maxPerTick"] > 256:
        fail("SCHEDULER", "registration or per-tick cap is invalid")
    if scheduler["backlogCadenceMillis"] > 25:
        fail("SCHEDULER", "backlog cadence exceeded 25 ms")
    dispatches = scheduler["tickDispatches"]
    if not isinstance(dispatches, list) or not dispatches:
        fail("SCHEDULER", "tickDispatches must be non-empty")
    if any(require_integer(value, "scheduler.tickDispatches", reason="SCHEDULER") > 256 for value in dispatches):
        fail("SCHEDULER", "a scheduler tick dispatched more than 256 registrations")
    if scheduler["due"] != scheduler["dispatched"] or scheduler["dispatched"] != sum(dispatches):
        fail("SCHEDULER", "due and dispatched registrations must match")
    if scheduler["late"] != 0 or scheduler["missed"] != 0:
        fail("SCHEDULER", "late and missed counts must be zero")
    expected_ticks = math.ceil(scheduler["registrationCount"] / scheduler["maxPerTick"])
    if scheduler["calculatedDrainTicks"] != expected_ticks or len(dispatches) != expected_ticks:
        fail("SCHEDULER", "calculated drain tick count is inconsistent")
    maximum_envelope = scheduler["registrationCount"] - scheduler["maxPerTick"]
    if scheduler["maximumBacklog"] > maximum_envelope:
        fail("SCHEDULER", "maximum backlog exceeded the calculated service envelope")
    if scheduler["calculatedDrainMillis"] > (expected_ticks - 1) * 25:
        fail("SCHEDULER", "calculated drain duration exceeded the cadence envelope")
    if scheduler["redisCompletionSafetyMarginMillis"] < 1000:
        fail("SCHEDULER", "Redis completion safety margin must be at least one second")
    if scheduler["watchdogTickDispatch"] > 256:
        fail("SCHEDULER", "a watchdog tick dispatched more than 256 renewals")
    if scheduler["watchdogDue"] != scheduler["watchdogDispatched"]:
        fail("SCHEDULER", "due and dispatched watchdog renewals must match")
    if scheduler["watchdogLate"] != 0 or scheduler["watchdogMissed"] != 0:
        fail("SCHEDULER", "late and missed watchdog renewals must be zero")
    if scheduler["finalTasks"] != 0 or scheduler["finalWatchdogs"] != 0:
        fail("SCHEDULER", "scheduler retained work after cleanup")

    cleanup = require_mapping(report["cleanup"], "cleanup", "CLEANUP")
    require_keys(
        cleanup,
        ("redisStateEntries", "runtimeTasks", "watchdogs", "executorsTerminated", "connectionsClosed"),
        "cleanup",
        "CLEANUP",
    )
    if cleanup["redisStateEntries"] != 0 or cleanup["runtimeTasks"] != 0 or cleanup["watchdogs"] != 0:
        fail("CLEANUP", "retained Redis or runtime state remains")
    if cleanup["executorsTerminated"] is not True or cleanup["connectionsClosed"] is not True:
        fail("CLEANUP", "connections and executors must be closed")


def validate_report(report):
    report = require_mapping(report, "report")
    forbid_sensitive(report)
    validate_metadata(report)
    validate_protocol(report)
    validate_command_and_rate_bounds(report)
    validate_latency_and_responsiveness(report)
    validate_retained_state(report)
    validate_scheduler_and_cleanup(report)
    return True


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    args = parser.parse_args(argv)
    try:
        with args.report.open(encoding="utf-8") as stream:
            validate_report(json.load(stream))
    except (OSError, json.JSONDecodeError, ValidationError) as error:
        print(f"validation failed: {error}", file=sys.stderr)
        return 1
    print(f"validation passed: {args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
