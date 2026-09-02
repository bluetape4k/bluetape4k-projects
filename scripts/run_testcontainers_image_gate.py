#!/usr/bin/env python3
"""Run the manifest-driven Testcontainers image gate sequentially."""

from __future__ import annotations

import argparse
import hashlib
import inspect
import json
import os
import re
import shlex
import selectors
import signal
import shutil
import tempfile
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Iterable, Sequence

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
if str(REPOSITORY_ROOT) not in sys.path:
    sys.path.insert(0, str(REPOSITORY_ROOT))

from scripts.testcontainers_image_gate import (
    MANIFEST,
    SelectionError,
    canonical_architecture,
    load_manifest,
    platform_for_entry,
    select_entries,
    select_shard_entries,
    validate_manifest,
)


CommandRunner = Callable[[list[str], int], Any]
DiagnosticRunner = Callable[[dict[str, Any]], dict[str, str]]
MAX_ATTEMPTS = 3
MAX_OUTPUT_CHARS = 64 * 1024
MAX_OUTPUT_LINES = 1_000
MAX_DIAGNOSTIC_CHARS = 64 * 1024
MAX_DIAGNOSTIC_LINES = 500
MAX_FAMILY_JSON_BYTES = 128 * 1024
MAX_SUMMARY_BYTES = 1 * 1024 * 1024
MAX_ARTIFACT_BYTES = 8 * 1024 * 1024
MAX_XML_BYTES = 4 * 1024 * 1024
MAX_XML_FILES = 8
MAX_XML_TESTCASES = 2_048
MAX_XML_COUNTER = 10_000
EXPECTED_RUNNERS = {
    "amd64": "ubuntu-24.04",
    "arm64": "ubuntu-24.04-arm",
}
SAFE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}$")
SAFE_TAG = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
INFRASTRUCTURE_MARKERS = (
    "toomanyrequests",
    "rate limit",
    "pull access denied",
    "manifest unknown",
    "connection refused",
    "cannot connect to the docker daemon",
    "docker daemon",
    "no space left on device",
    "timed out",
    "timeout",
    "connection reset",
    "eof",
)
TEST_DISCOVERY_FAILURE_PATTERN = re.compile(
    r"execution failed for task ['\"][^'\"]+['\"].*"
    r"no tests found for given includes",
    re.IGNORECASE | re.DOTALL,
)
SECRET_PATTERN = re.compile(
    r"(?i)(password|passwd|token|secret|authorization|api[_-]?key)\s*[:=]\s*([^\s,;]+)"
)
BEARER_PATTERN = re.compile(r"(?i)(bearer\s+)[^\s,;]+")
BASIC_AUTH_URL_PATTERN = re.compile(r"(?i)(https?://)([^/@:\s]+):([^/@\s]+)@")
DOCKER_TEXT_PULL_EVENT_PATTERN = re.compile(
    r"^(?P<timestamp>\S+)\s+image\s+pull\s+(?P<ref>\S+)(?:\s|$)"
)
_KNOWN_SECRETS: set[str] = set()


class CommandResult:
    """Bounded subprocess result shared by real and fake command runners."""

    def __init__(
        self,
        returncode: int | None,
        stdout: str = "",
        stderr: str = "",
        elapsed: float = 0.0,
        *,
        stdout_overflow: bool = False,
        stderr_overflow: bool = False,
    ):
        self.returncode = returncode
        self.stdout = stdout
        self.stderr = stderr
        self.elapsed_seconds = elapsed
        self.stdout_overflow = stdout_overflow
        self.stderr_overflow = stderr_overflow


def _has_output_overflow(result: object) -> bool:
    return bool(
        getattr(result, "stdout_overflow", False)
        or getattr(result, "stderr_overflow", False)
    )


def redact(value: str) -> str:
    """Remove common credential values before text enters logs or artifacts."""

    redacted = BASIC_AUTH_URL_PATTERN.sub(r"\1<redacted>:<redacted>@", value)
    redacted = BEARER_PATTERN.sub(r"\1<redacted>", redacted)
    redacted = SECRET_PATTERN.sub(r"\1=<redacted>", redacted)
    for secret in sorted((item for item in _KNOWN_SECRETS if item), key=len, reverse=True):
        redacted = redacted.replace(secret, "<redacted>")
    return redacted


def _bounded_with_overflow(value: object) -> tuple[str, bool]:
    text = redact(str(value or ""))
    lines = text.splitlines()
    overflow = len(lines) > MAX_OUTPUT_LINES
    if len(lines) > MAX_OUTPUT_LINES:
        text = "\n".join(lines[:MAX_OUTPUT_LINES]) + "\n...[line limit]"
    if len(text.encode("utf-8")) <= MAX_OUTPUT_CHARS:
        return text, overflow
    encoded = text.encode("utf-8")[:MAX_OUTPUT_CHARS]
    return encoded.decode("utf-8", errors="ignore") + "\n...[byte limit]", True


def _bounded(value: object) -> str:
    return _bounded_with_overflow(value)[0]


def register_secret(value: object) -> None:
    """Register raw/decoded credentials for every report boundary."""

    if isinstance(value, (dict, list, tuple)):
        for item in value:
            register_secret(item)
        if isinstance(value, dict):
            for item in value.values():
                register_secret(item)
        return
    if isinstance(value, str) and value:
        _KNOWN_SECRETS.add(value)
        if value.lstrip().startswith(("{", "[")):
            try:
                register_secret(json.loads(value))
            except (TypeError, ValueError, json.JSONDecodeError):
                pass
        try:
            import base64

            decoded = base64.b64decode(value, validate=True).decode("utf-8")
        except Exception:
            decoded = ""
        if decoded:
            _KNOWN_SECRETS.add(decoded)


def _parse_iso_timestamp_ns(value: object) -> int:
    """Parse Docker's ISO event timestamp without losing nanosecond precision."""

    text = str(value or "").strip()
    match = re.fullmatch(
        r"(?P<date>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})"
        r"(?:\.(?P<fraction>\d{1,9}))?(?P<zone>Z|[+-]\d{2}:\d{2})",
        text,
    )
    if not match:
        return 0
    try:
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
        seconds = int(parsed.timestamp())
    except ValueError:
        return 0
    fraction = int((match.group("fraction") or "").ljust(9, "0") or "0")
    return seconds * 1_000_000_000 + fraction


def _parse_text_pull_event(line: str) -> tuple[str, int] | None:
    """Parse a Docker text event into its requested reference and timestamp."""

    match = DOCKER_TEXT_PULL_EVENT_PATTERN.match(line.strip())
    if not match:
        return None
    timestamp_ns = _parse_iso_timestamp_ns(match.group("timestamp"))
    if timestamp_ns <= 0:
        return None
    return match.group("ref"), timestamp_ns


def _read_bounded(path: Path, limit: int) -> str:
    with path.open("rb") as stream:
        data = stream.read(limit + 1)
    if len(data) > limit:
        raise ValueError(f"output exceeds {limit} bytes: {path.name}")
    return redact(data.decode("utf-8", errors="replace"))


def classify_failure(
    returncode: int | None,
    stdout: str,
    stderr: str,
    *,
    stdout_overflow: bool = False,
    stderr_overflow: bool = False,
) -> str:
    """Classify a command result without conflating registry/daemon failures with product bugs."""

    if stdout_overflow or stderr_overflow:
        return "blocked"
    if returncode == 0:
        return "success"
    haystack = f"{stdout}\n{stderr}".lower()
    if returncode is None:
        return "infrastructure_failure"
    if TEST_DISCOVERY_FAILURE_PATTERN.search(haystack):
        return "product_failure"
    if any(marker in haystack for marker in INFRASTRUCTURE_MARKERS):
        return "infrastructure_failure"
    return "product_failure"


def _classify_command_result(
    returncode: int | None,
    stdout: str,
    stderr: str,
    *,
    stdout_overflow: bool = False,
    stderr_overflow: bool = False,
) -> str:
    if stdout_overflow or stderr_overflow:
        return "blocked"
    if "job budget exceeded" in f"{stdout}\n{stderr}".lower():
        return "blocked"
    status = classify_failure(
        returncode,
        stdout,
        stderr,
        stdout_overflow=stdout_overflow,
        stderr_overflow=stderr_overflow,
    )
    if status == "success" and not re.search(r"\bBUILD SUCCESSFUL\b", f"{stdout}\n{stderr}"):
        return "blocked"
    return status


def _terminate_process(process: subprocess.Popen[bytes]) -> None:
    try:
        os.killpg(process.pid, signal.SIGTERM)
        process.wait(timeout=5)
    except (OSError, subprocess.TimeoutExpired):
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except OSError:
            pass
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            pass


def _subprocess_runner(command: list[str], timeout_seconds: int, *, env: dict[str, str] | None = None) -> Any:
    """Run a command while continuously draining capped stdout/stderr pipes."""

    started = time.monotonic()
    process = subprocess.Popen(
        command,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        start_new_session=True,
    )
    selector = selectors.DefaultSelector()
    captures: dict[str, bytearray] = {"stdout": bytearray(), "stderr": bytearray()}
    overflows = {"stdout": False, "stderr": False}
    for name, stream in (("stdout", process.stdout), ("stderr", process.stderr)):
        if stream is not None:
            selector.register(stream, selectors.EVENT_READ, name)
    deadline = started + max(1, timeout_seconds)
    timed_out = False
    try:
        while selector.get_map():
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                timed_out = True
                _terminate_process(process)
                break
            events = selector.select(min(remaining, 0.25))
            for key, _ in events:
                stream = key.fileobj
                chunk = os.read(stream.fileno(), 8192)
                if not chunk:
                    selector.unregister(stream)
                    stream.close()
                    continue
                capture = captures[key.data]
                capture.extend(chunk)
                if len(capture) > MAX_OUTPUT_CHARS:
                    overflows[key.data] = True
                    del capture[:-MAX_OUTPUT_CHARS]
        if not timed_out:
            process.wait(timeout=max(1, deadline - time.monotonic()))
    except subprocess.TimeoutExpired:
        timed_out = True
        _terminate_process(process)
    finally:
        selector.close()
        for stream in (process.stdout, process.stderr):
            if stream is not None and not stream.closed:
                stream.close()
    stdout = bytes(captures["stdout"]).decode("utf-8", errors="replace")
    stderr = bytes(captures["stderr"]).decode("utf-8", errors="replace")
    bounded_stdout, stdout_bounded_overflow = _bounded_with_overflow(stdout)
    bounded_stderr, stderr_bounded_overflow = _bounded_with_overflow(stderr)
    if timed_out:
        bounded_stderr = f"{bounded_stderr}\n...[timeout]"
    return CommandResult(
        None if timed_out else process.returncode,
        bounded_stdout,
        bounded_stderr,
        time.monotonic() - started,
        stdout_overflow=overflows["stdout"] or stdout_bounded_overflow,
        stderr_overflow=overflows["stderr"] or stderr_bounded_overflow,
    )


def verify_release_summary(
    summary: dict[str, Any],
    *,
    expected_coverage: str,
    platform_id: str,
    expected_tag: str,
    expected_architecture: str,
    report_dir: Path | None = None,
) -> list[str]:
    """Return fail-closed Release verifier findings for one native platform gate."""

    errors: list[str] = []
    if summary.get("schema_version") != 2:
        errors.append("summary schema_version must be 2")
    if summary.get("coverage") != expected_coverage:
        errors.append(f"coverage must be {expected_coverage}")
    if summary.get("release_gate") is not True:
        errors.append("release_gate must be true")
    if summary.get("status") != "success":
        errors.append("summary status must be success")
    if summary.get("selected", 0) <= 0 or summary.get("blocked", 0) or summary.get("product_failure", 0) or summary.get("infrastructure_failure", 0):
        errors.append("summary contains incomplete or failed execution")
    release_results = [
        result for result in summary.get("results", []) if result.get("release_required") is True
    ]
    if summary.get("release_required_selected") != len(release_results):
        errors.append("release-required result count is inconsistent")
    if summary.get("release_required_success") != sum(
        result.get("status") == "success" for result in release_results
    ):
        errors.append("release-required success count is inconsistent")
    for result in release_results:
        junit = result.get("junit", {})
        if (
            not isinstance(junit, dict)
            or junit.get("tests", 0) <= 0
            or junit.get("skipped", 1) != 0
            or junit.get("failures", 1) != 0
            or junit.get("errors", 1) != 0
        ):
            errors.append(f"successful JUnit evidence is required: {result.get('id')}")
    platform_results = [item for item in summary.get("platforms", []) if item.get("platform_id") == platform_id]
    if len(platform_results) != 1:
        errors.append(f"exactly one {platform_id} platform result is required")
    else:
        result = platform_results[0]
        if result.get("status") != "success":
            errors.append("platform result is not successful")
        expected = result.get("expected", {})
        observed = result.get("observed", {})
        pull = result.get("pull", {})
        junit = result.get("junit", {})
        image_ref = str(result.get("image_ref", ""))
        if expected.get("tag") != expected_tag or observed.get("image_tag") != expected_tag:
            errors.append("expected/observed tag mismatch")
        if expected.get("architecture") != expected_architecture or observed.get("image_architecture") != expected_architecture:
            errors.append("expected/observed architecture mismatch")
        if expected.get("runner") != EXPECTED_RUNNERS.get(platform_id):
            errors.append("expected runner label mismatch")
        if (
            observed.get("runner_architecture") != expected_architecture
            or observed.get("daemon_architecture") != expected_architecture
        ):
            errors.append("runner/daemon architecture evidence is required")
        if expected.get("os") != "linux" or observed.get("image_os") != "linux":
            errors.append("expected/observed OS mismatch")
        if observed.get("runner_os") != "linux" or observed.get("daemon_os") != "linux":
            errors.append("runner/daemon OS evidence is required")
        if pull.get("status") != "success":
            errors.append("pull status must be success")
        if pull.get("event_ref") not in {image_ref, f"docker.io/{image_ref}"}:
            errors.append("pull event requested-ref correlation is required")
        expected_repository = image_ref.rsplit(":", 1)[0]
        digest = str(pull.get("digest", ""))
        if pull.get("digest_ref") not in {
            f"{expected_repository}@{digest}",
            f"docker.io/{expected_repository}@{digest}",
        }:
            errors.append("pull digest must belong to the requested repository")
        event_id_source = pull.get("event_id_source")
        event_image_id_source = pull.get("event_image_id_source")
        if event_id_source not in {"docker_event", "ref_timestamp_receipt"}:
            errors.append("pull event ID source is required")
        if event_image_id_source not in {"docker_event", "post_pull_inspect"}:
            errors.append("pull event image ID source is required")
        if event_id_source == "ref_timestamp_receipt" and pull.get("event_id") != (
            f"ref_timestamp:{pull.get('event_timestamp_ns')}:{pull.get('event_ref')}"
        ):
            errors.append("pull text event receipt is invalid")
        if event_image_id_source == "post_pull_inspect" and pull.get("event_image_id") != pull.get("image_id"):
            errors.append("post-pull inspect image ID binding is required")
        if (
            not pull.get("event_id")
            or pull.get("event_image_id") not in {pull.get("image_id"), pull.get("digest")}
            or not pull.get("event_timestamp_ns")
            or not str(pull.get("image_id", "")).startswith("sha256:")
            or not re.fullmatch(r"sha256:[0-9a-f]{64}", digest)
        ):
            errors.append("pull event and digest evidence are required")
        workload_image = result.get("workload_image", {})
        if workload_image.get("image_id") != pull.get("image_id"):
            errors.append("workload image must match pulled image")
        if junit.get("workload_tests") != 1 or junit.get("skipped", 1) != 0 or junit.get("failures", 1) != 0 or junit.get("errors", 1) != 0:
            errors.append("successful workload JUnit evidence is required")
        artifact = result.get("family_artifact")
        family_id = str(result.get("family_id", ""))
        if not SAFE_ID.fullmatch(family_id) or artifact != f"{family_id}.json":
            errors.append("family artifact reference is required")
        elif report_dir is not None:
            report_root = report_dir.resolve()
            artifact_path = report_dir / artifact
            try:
                artifact_path.resolve().relative_to(report_root)
            except ValueError:
                errors.append("family artifact escapes report directory")
            else:
                if artifact_path.is_symlink() or not artifact_path.is_file():
                    errors.append("family artifact file is missing or not regular")
    return errors


def worst_case_budget_minutes(
    *,
    generic_families: int,
    generic_attempts: int,
    generic_pull_minutes: int,
    generic_test_minutes: int,
    generic_diagnostic_minutes: float,
    strict_families: int,
    strict_attempts: int,
    strict_pull_minutes: int,
    strict_test_minutes: int,
    strict_diagnostic_minutes: float,
    setup_slack_minutes: int,
) -> float:
    """Calculate the bounded workflow budget used by the Nightly/Release contract."""

    return (
        generic_families * generic_attempts * (generic_pull_minutes + generic_test_minutes + generic_diagnostic_minutes)
        + strict_families * strict_attempts * (strict_pull_minutes + strict_test_minutes + strict_diagnostic_minutes)
        + setup_slack_minutes
    )


class GateRunner:
    """Execute selected families one by one and persist fail-closed evidence."""

    def __init__(
        self,
        entries: Iterable[dict[str, Any]],
        report_dir: Path,
        *,
        command_runner: CommandRunner = _subprocess_runner,
        diagnostic_runner: DiagnosticRunner | None = None,
        gradle_task: str = "./gradlew :bluetape4k-testcontainers:test",
        max_attempts: int = 1,
        timeout_minutes: int = 30,
        manifest_path: Path | None = None,
        pull_timeout_seconds: int = 60,
        diagnostic_timeout_seconds: int = 30,
        job_budget_minutes: int | None = None,
        scope: str = "full",
        workflow_run_id: str | None = None,
        commit: str | None = None,
        ref: str | None = None,
        shard_index: int | None = None,
        shard_count: int | None = None,
    ) -> None:
        if (shard_index is None) != (shard_count is None):
            raise ValueError("shard index and count must be provided together")
        if shard_index is not None and shard_count is not None:
            if shard_count < 1:
                raise ValueError("shard count must be positive")
            if shard_index < 0 or shard_index >= shard_count:
                raise ValueError("shard index must be within shard count")
        self.entries = [dict(entry) for entry in entries]
        self.report_dir = report_dir
        self.command_runner = command_runner
        self.diagnostic_runner = diagnostic_runner or self._collect_diagnostics
        self.gradle_task = gradle_task
        self.max_attempts = max(1, min(max_attempts, MAX_ATTEMPTS))
        self.timeout_seconds = max(60, timeout_minutes * 60)
        self.pull_timeout_seconds = max(1, pull_timeout_seconds)
        self.diagnostic_timeout_seconds = max(1, diagnostic_timeout_seconds)
        self.job_budget_seconds = (job_budget_minutes * 60) if job_budget_minutes else None
        self.scope = scope
        self.workflow_run_id = workflow_run_id or os.environ.get("GITHUB_RUN_ID", "local")
        self.commit = commit or os.environ.get("GITHUB_SHA", "local")
        self.ref = ref or os.environ.get("GITHUB_REF", "local")
        self.shard_index = shard_index
        self.shard_count = shard_count
        self.manifest_digest = self._digest(manifest_path)
        self._staging_root: Path | None = None
        self._elapsed_seconds = 0.0
        self._deadline: float | None = None

    def _digest(self, manifest_path: Path | None) -> str:
        if manifest_path and manifest_path.is_file():
            return hashlib.sha256(manifest_path.read_bytes()).hexdigest()
        canonical = json.dumps(self.entries, ensure_ascii=False, sort_keys=True).encode("utf-8")
        return hashlib.sha256(canonical).hexdigest()

    def _invoke(self, command: list[str], timeout_seconds: int, env: dict[str, str] | None = None) -> Any:
        effective_timeout = timeout_seconds
        if self._deadline is not None:
            remaining = int(self._deadline - time.monotonic())
            if remaining <= 0:
                return CommandResult(None, "", "job budget exceeded")
            effective_timeout = min(timeout_seconds, remaining)
        if env is None:
            return self.command_runner(command, effective_timeout)
        try:
            parameters = inspect.signature(self.command_runner).parameters.values()
            accepts_env = any(
                parameter.name == "env" or parameter.kind is inspect.Parameter.VAR_KEYWORD
                for parameter in parameters
            )
        except (TypeError, ValueError):
            accepts_env = False
        if accepts_env:
            return self.command_runner(command, effective_timeout, env=env)
        return self.command_runner(command, effective_timeout)

    @staticmethod
    def _sanitized_runtime_env() -> dict[str, str]:
        """Keep registry credentials and Docker overrides out of Gradle and diagnostics."""

        env = os.environ.copy()
        for key in (
            "DOCKER_AUTH_CONFIG",
            "DOCKER_CONFIG",
            "TESTCONTAINERS_REGISTRY_MIRROR",
            "DOCKER_HOST",
            "DOCKER_CONTEXT",
        ):
            env.pop(key, None)
        # Local Colima is the only approved host override: it is a Unix socket
        # paired with Testcontainers' canonical in-container socket. Never
        # carry TCP endpoints or arbitrary context selections into the gate.
        local_socket = os.environ.get("DOCKER_HOST", "")
        if (
            local_socket.startswith("unix://")
            and os.environ.get("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") == "/var/run/docker.sock"
        ):
            env["DOCKER_HOST"] = local_socket
        return env

    def _isolated_runtime_env(self) -> dict[str, str]:
        env = self._sanitized_runtime_env()
        if self._staging_root is None:
            return env
        config_dir = self._staging_root / "runtime-docker-config"
        config_dir.mkdir(mode=0o700, exist_ok=True)
        config_file = config_dir / "config.json"
        if not config_file.exists():
            config_file.write_text("{}\n", encoding="utf-8")
            config_file.chmod(0o600)
        env["DOCKER_CONFIG"] = str(config_dir)
        return env

    def _command(self, entry: dict[str, Any], evidence_dir: Path | None = None) -> list[str]:
        strict = bool(entry.get("executionEvidenceRequired"))
        junit_required = bool(entry.get("releaseRequired"))
        if strict and evidence_dir is not None:
            workload = str(entry.get("workloadTestPattern", entry["testPattern"])).removesuffix("()")
            test_task = str(entry.get("testTask", ":bluetape4k-testcontainers:test"))
            return [
                "./gradlew",
                f"-Dtestcontainers.image-gate.evidence-dir={evidence_dir}",
                test_task,
                "--tests",
                workload,
                "--no-configuration-cache",
                # Strict workload evidence must be produced by this attempt;
                # never accept a previously cached Gradle test result.
                "--rerun-tasks",
                "-x",
                ":bluetape4k-mock-web-server:jibDockerBuild",
                "-x",
                ":bluetape4k-mock-webflux-server:jibDockerBuild",
            ]
        command = shlex.split(self.gradle_task)
        if test_task := entry.get("testTask"):
            task_positions = [
                index
                for index, part in enumerate(command)
                if part.startswith(":") and (index == 0 or command[index - 1] not in {"-x", "--exclude-task"})
            ]
            if len(task_positions) != 1:
                raise ValueError(f"expected one Gradle task in command: {self.gradle_task}")
            command[task_positions[0]] = str(test_task)
        if strict and evidence_dir is not None and not any(
            part.startswith("-Dtestcontainers.image-gate.evidence-dir=") for part in command
        ):
            task_index = next((index for index, part in enumerate(command) if part.startswith(":")), 1)
            command.insert(task_index, f"-Dtestcontainers.image-gate.evidence-dir={evidence_dir}")
        elif junit_required and evidence_dir is not None and not any(
            part.startswith("-Dtestcontainers.image-gate.evidence-dir=") for part in command
        ):
            task_index = next((index for index, part in enumerate(command) if part.startswith(":")), len(command) - 1)
            command.insert(task_index + 1, f"-Dtestcontainers.image-gate.evidence-dir={evidence_dir}")
        if "--tests" not in command:
            selector = entry.get("testSelector", entry.get("testPattern"))
            command.extend(("--tests", str(selector)))
        if "--no-configuration-cache" not in command:
            command.append("--no-configuration-cache")
        if junit_required and "--rerun-tasks" not in command:
            # Release evidence must be produced by this invocation, never by a
            # previously cached Gradle test result.
            command.append("--rerun-tasks")
        return command

    @staticmethod
    def _diagnostic_bounded(value: object) -> str:
        text = redact(str(value or ""))
        lines = text.splitlines()
        if len(lines) > MAX_DIAGNOSTIC_LINES:
            text = "\n".join(lines[:MAX_DIAGNOSTIC_LINES]) + "\n...[line limit]"
        encoded = text.encode("utf-8")
        if len(encoded) > MAX_DIAGNOSTIC_CHARS:
            text = encoded[:MAX_DIAGNOSTIC_CHARS].decode("utf-8", errors="ignore") + "\n...[byte limit]"
        return text

    def _collect_diagnostics(self, entry: dict[str, Any]) -> dict[str, str]:
        platform = self._strict_platform(entry)
        selected_tag = str(platform["tag"]) if platform else str(entry["tag"])
        image_ref = f"{entry['image']}:{selected_tag}"
        commands: list[tuple[str, list[str]]] = [
            (
                "docker_ps",
                ["docker", "ps", "-aq", "--no-trunc", "--filter", f"ancestor={image_ref}"],
            ),
            ("docker_inspect", ["docker", "inspect", image_ref]),
            # _invoke already bounds and process-group-terminates diagnostics; do
            # not depend on GNU `timeout`, which is absent on macOS runners.
            ("docker_events", ["docker", "events", "--since", "5m"]),
        ]
        if entry["server"] == "K3sServer":
            commands.extend(
                (
                    ("kubectl_pods", ["kubectl", "get", "pods", "--all-namespaces", "-o", "wide"]),
                    (
                        "kubectl_events",
                        ["kubectl", "get", "events", "--all-namespaces", "--sort-by=.lastTimestamp"],
                    ),
                    (
                        "kubectl_logs",
                        ["kubectl", "logs", "--all-namespaces", "--all-containers", "--tail=200"],
                    ),
                )
            )
        diagnostics: dict[str, str] = {}
        for label, command in commands:
            result = self._invoke(
                command,
                min(self.diagnostic_timeout_seconds, 60),
                self._isolated_runtime_env(),
            )
            diagnostics[label] = self._diagnostic_bounded(
                f"exit={getattr(result, 'returncode', None)}\n"
                f"stdout={getattr(result, 'stdout', '')}\n"
                f"stderr={getattr(result, 'stderr', '')}"
            )
            if label == "docker_ps":
                container_ids = [
                    line.strip()
                    for line in str(getattr(result, "stdout", "")).splitlines()
                    if re.fullmatch(r"[0-9a-f]{12,64}", line.strip())
                ][:5]
                log_chunks: list[str] = []
                for container_id in container_ids:
                    log_result = self._invoke(
                        ["docker", "logs", "--tail=200", container_id],
                        min(self.diagnostic_timeout_seconds, 60),
                        self._isolated_runtime_env(),
                    )
                    log_chunks.append(
                        f"container={container_id}\n"
                        f"exit={getattr(log_result, 'returncode', None)}\n"
                        f"stdout={getattr(log_result, 'stdout', '')}\n"
                        f"stderr={getattr(log_result, 'stderr', '')}"
                    )
                diagnostics["docker_logs"] = self._diagnostic_bounded(
                    "\n---\n".join(log_chunks) if log_chunks else "no matching containers"
                )
        return diagnostics

    @staticmethod
    def _is_regular(path: Path) -> bool:
        return path.is_file() and not path.is_symlink()

    @staticmethod
    def _canonical_testcase(classname: str, name: str) -> str:
        return f"{classname}.{name[:-2] if name.endswith('()') else name}"

    def _xml_candidates(
        self,
        started_ns: int,
        evidence_dir: Path,
        entry: dict[str, Any],
    ) -> list[Path]:
        test_task = str(entry.get("testTask") or ":bluetape4k-testcontainers:test")
        task_name = test_task.rsplit(":", 1)[-1] or "test"
        roots = [
            REPOSITORY_ROOT / "testing/testcontainers/build/test-results" / task_name,
            REPOSITORY_ROOT / "testing/testcontainers/build/test-results/test",
            evidence_dir,
        ]
        candidates: list[Path] = []
        seen_roots: set[Path] = set()
        for root in roots:
            if root in seen_roots:
                continue
            seen_roots.add(root)
            if not root.is_dir() or root.is_symlink():
                continue
            for path in root.glob("*.xml"):
                try:
                    if self._is_regular(path) and path.stat().st_mtime_ns >= started_ns:
                        candidates.append(path)
                except OSError:
                    continue
        if len(candidates) > MAX_XML_FILES:
            raise ValueError("JUnit XML file limit exceeded")
        return candidates

    def _parse_junit(self, entry: dict[str, Any], files: list[Path]) -> dict[str, Any]:
        if not files:
            raise ValueError("JUnit evidence is missing")
        expected_suite = str(entry["testPattern"])
        expected_workload = str(entry.get("workloadTestPattern", "")).replace("()", "")
        matched: tuple[Path, ET.Element] | None = None
        for path in files:
            try:
                flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
                fd = os.open(path, flags)
                with os.fdopen(fd, "rb") as stream:
                    raw = stream.read(MAX_XML_BYTES + 1)
            except OSError as error:
                raise ValueError(f"JUnit XML cannot be opened safely: {path.name}") from error
            if len(raw) > MAX_XML_BYTES:
                raise ValueError(f"JUnit XML exceeds {MAX_XML_BYTES} bytes")
            if b"<!DOCTYPE" in raw or b"<!ENTITY" in raw:
                raise ValueError("DTD/ENTITY is not allowed in JUnit XML")
            try:
                root = ET.fromstring(raw)
            except ET.ParseError as error:
                raise ValueError(f"malformed JUnit XML: {path.name}") from error
            suite_name = root.attrib.get("name", "")
            if suite_name == expected_suite:
                matched = (path, root)
                break
        if matched is None:
            raise ValueError(f"JUnit suite is missing: {expected_suite}")
        path, root = matched
        testcases = list(root.iter("testcase"))
        if len(testcases) > MAX_XML_TESTCASES:
            raise ValueError("JUnit testcase limit exceeded")
        def count(name: str) -> int:
            try:
                value = int(root.attrib.get(name, "0"))
            except ValueError as error:
                raise ValueError(f"invalid JUnit counter: {name}") from error
            if value < 0 or value > MAX_XML_COUNTER:
                raise ValueError(f"JUnit counter limit exceeded: {name}")
            return value
        tests = count("tests")
        skipped = count("skipped")
        failures = count("failures")
        errors = count("errors")
        if tests <= 0 or skipped != 0 or failures or errors:
            raise ValueError("JUnit suite has no successful execution evidence")
        def failed_or_skipped(testcase: ET.Element) -> bool:
            return testcase.attrib.get("status") == "skipped" or any(
                child.tag.rsplit("}", 1)[-1] in {"failure", "error", "skipped"}
                for child in testcase
            )

        if any(failed_or_skipped(testcase) for testcase in testcases):
            raise ValueError("JUnit testcase failed or was skipped")
        workload_tests = tests
        if expected_workload:
            workload_matches = []
            for testcase in testcases:
                canonical = self._canonical_testcase(testcase.attrib.get("classname", ""), testcase.attrib.get("name", ""))
                if canonical == expected_workload:
                    workload_matches.append(testcase)
            if len(workload_matches) != 1:
                raise ValueError(f"workload testcase mismatch: {expected_workload}")
            workload = workload_matches[0]
            if failed_or_skipped(workload):
                raise ValueError("workload testcase failed or was skipped")
            workload_tests = 1
        return {
            "suite": expected_suite,
            "tests": tests,
            "skipped": skipped,
            "failures": failures,
            "errors": errors,
            "workload_testcase": expected_workload,
            "workload_tests": workload_tests,
            "evidence_type": "junit",
            "source": path.name,
        }

    def _marker(self, evidence_dir: Path, started_ns: int) -> dict[str, Any]:
        marker = evidence_dir / "startup.marker"
        if not self._is_regular(marker):
            raise ValueError("startup marker is missing")
        stat = marker.stat()
        if stat.st_mtime_ns < started_ns or stat.st_size > 1024:
            raise ValueError("startup marker is stale or oversized")
        content = marker.read_text(encoding="utf-8").strip()
        if content != "Container workload ready":
            raise ValueError("startup marker content mismatch")
        return {"ready": True, "marker": content, "marker_source": "bounded_attempt_marker"}

    def _workload_image(self, evidence_dir: Path) -> dict[str, Any]:
        receipt = evidence_dir / "workload.image-id"
        if not self._is_regular(receipt) or receipt.stat().st_size > 256:
            raise ValueError("workload image receipt is missing or oversized")
        image_id = receipt.read_text(encoding="utf-8").strip()
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", image_id):
            raise ValueError("workload image receipt is not a Docker image ID")
        return {"image_id": image_id, "source": receipt.name}

    def _pull_evidence(self, entry: dict[str, Any], platform: dict[str, Any], attempt_root: Path, attempt: int) -> tuple[str, dict[str, Any]]:
        image_ref = f"{entry['image']}:{platform['tag']}"
        architecture = str(platform["id"])
        auth_config = os.environ.get("DOCKER_AUTH_CONFIG", "")
        local_socket = os.environ.get("DOCKER_HOST", "")
        register_secret(auth_config)
        env = os.environ.copy()
        for key in (
            "DOCKER_AUTH_CONFIG",
            "DOCKER_CONFIG",
            "TESTCONTAINERS_REGISTRY_MIRROR",
            "DOCKER_HOST",
            "DOCKER_CONTEXT",
        ):
            env.pop(key, None)
        # 호출자가 선택한 context는 무시하고 저장소가 허용한 기본 Unix socket 경계만 사용한다.
        env["DOCKER_CONTEXT"] = "default"
        if (
            local_socket.startswith("unix://")
            and os.environ.get("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") == "/var/run/docker.sock"
        ):
            env["DOCKER_HOST"] = local_socket
        config_dir: Path | None = attempt_root / "docker-config"
        try:
            config_dir.mkdir(mode=0o700)
            config_file = config_dir / "config.json"
            config_file.write_text(auth_config or "{}\n", encoding="utf-8")
            config_file.chmod(0o600)
            env["DOCKER_CONFIG"] = str(config_dir)
            pull_started_wall = time.time()
            pull_started = time.monotonic()
            pull = self._invoke(
                ["docker", "pull", "--platform", f"linux/{architecture}", image_ref],
                self.pull_timeout_seconds,
                env,
            )
            if "job budget exceeded" in f"{getattr(pull, 'stdout', '')}\n{getattr(pull, 'stderr', '')}".lower():
                return "blocked", {
                    "requested_ref": f"docker.io/{image_ref}",
                    "attempts": attempt,
                    "status": "blocked",
                    "error": "job budget exceeded",
                }
            pull_status = classify_failure(
                getattr(pull, "returncode", None),
                getattr(pull, "stdout", ""),
                getattr(pull, "stderr", ""),
                stdout_overflow=bool(getattr(pull, "stdout_overflow", False)),
                stderr_overflow=bool(getattr(pull, "stderr_overflow", False)),
            )
            if pull_status != "success":
                error = "pull command output overflow" if _has_output_overflow(pull) else None
                evidence = {
                    "requested_ref": f"docker.io/{image_ref}",
                    "attempts": attempt,
                    "status": pull_status,
                }
                if error:
                    evidence["error"] = error
                return pull_status, evidence
            inspect = self._invoke(["docker", "image", "inspect", image_ref], self.pull_timeout_seconds, env)
            if _has_output_overflow(inspect):
                return "blocked", {
                    "requested_ref": f"docker.io/{image_ref}",
                    "attempts": attempt,
                    "status": "blocked",
                    "error": "image inspect output overflow",
                }
            try:
                payload = json.loads(getattr(inspect, "stdout", ""))
                image = payload[0] if isinstance(payload, list) else payload
                image_id = str(image.get("Id", ""))
                digests = image.get("RepoDigests", [])
                repository = image_ref.rsplit(":", 1)[0]
                repository_names = {repository, f"docker.io/{repository}"}
                digest_ref = next(
                    (
                        item
                        for item in digests
                        if isinstance(item, str)
                        and "@sha256:" in item
                        and item.split("@", 1)[0] in repository_names
                    ),
                    "",
                )
                digest = digest_ref.split("@", 1)[1] if digest_ref else ""
                observed_os = str(image.get("Os", ""))
                observed_arch = canonical_architecture(image.get("Architecture"))
            except (ValueError, IndexError, AttributeError, TypeError, json.JSONDecodeError) as error:
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": _bounded(error)}
            if (
                not image_id.startswith("sha256:")
                or not digest.startswith("sha256:")
                or not digest_ref
                or observed_os != "linux"
                or observed_arch != architecture
            ):
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "image platform or digest mismatch"}
            context = self._invoke(["docker", "context", "show"], self.pull_timeout_seconds, env)
            info = self._invoke(["docker", "info", "--format", "{{json .}}"], self.pull_timeout_seconds, env)
            uname = self._invoke(["uname", "-m"], self.pull_timeout_seconds, env)
            uname_os = self._invoke(["uname", "-s"], self.pull_timeout_seconds, env)
            if any(_has_output_overflow(item) for item in (context, info, uname, uname_os)):
                return "blocked", {
                    "requested_ref": f"docker.io/{image_ref}",
                    "attempts": attempt,
                    "status": "blocked",
                    "error": "Docker environment output overflow",
                }
            if (
                getattr(context, "returncode", 1) != 0
                or getattr(context, "stdout", "").strip() != "default"
                or getattr(uname_os, "returncode", 1) != 0
            ):
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "non-default Docker context"}
            try:
                info_payload = json.loads(getattr(info, "stdout", "{}"))
            except json.JSONDecodeError:
                info_payload = {}
            daemon_arch = canonical_architecture(info_payload.get("Architecture"))
            runner_arch = canonical_architecture(getattr(uname, "stdout", "").strip())
            daemon_os = str(info_payload.get("OSType", "")).strip().lower()
            runner_os = str(getattr(uname_os, "stdout", "")).strip().lower()
            if (
                daemon_arch != architecture
                or runner_arch != architecture
                or daemon_os != "linux"
                or runner_os != "linux"
            ):
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "daemon or runner architecture mismatch"}
            event_since = str(max(0, int(pull_started_wall)))
            event_until = str(int(time.time()) + 2)
            event = self._invoke(
                [
                    "docker",
                    "events",
                    "--since",
                    event_since,
                    "--until",
                    event_until,
                    "--filter",
                    "type=image",
                    "--filter",
                    "event=pull",
                    "--format",
                    "{{json .}}",
                ],
                self.pull_timeout_seconds,
                env,
            )
            if _has_output_overflow(event):
                return "blocked", {
                    "requested_ref": f"docker.io/{image_ref}",
                    "attempts": attempt,
                    "status": "blocked",
                    "error": "Docker event output overflow",
                }
            event_id = ""
            event_id_source = ""
            event_ref = ""
            event_image_id = ""
            event_image_id_source = ""
            event_timestamp_ns = 0
            expected_refs = {image_ref, f"docker.io/{image_ref}"}
            expected_ids = {image_id, digest}
            for line in getattr(event, "stdout", "").splitlines():
                try:
                    item = json.loads(line)
                    if not isinstance(item, dict):
                        continue
                    actor = item.get("Actor", {})
                    attributes = actor.get("Attributes", {}) if isinstance(actor, dict) else {}
                    actor_id = str(actor.get("ID") or "") if isinstance(actor, dict) else ""
                    refs = {
                        str(item.get("from") or ""),
                        str(attributes.get("name") or ""),
                        str(attributes.get("image") or ""),
                        actor_id,
                    }
                    ids = {
                        str(item.get("id") or ""),
                        actor_id,
                    }
                    matching_ref = next((value for value in refs if value in expected_refs), "")
                    matching_id = next((value for value in ids if value in expected_ids), "")
                    raw_timestamp = item.get("timeNano")
                    if raw_timestamp is None and item.get("time") is not None:
                        raw_timestamp = int(float(item["time"]) * 1_000_000_000)
                    try:
                        candidate_timestamp_ns = int(raw_timestamp)
                    except (TypeError, ValueError):
                        candidate_timestamp_ns = 0
                    if (
                        not matching_ref
                        or candidate_timestamp_ns < int(pull_started_wall * 1_000_000_000)
                    ):
                        continue
                    event_ref = matching_ref
                    event_timestamp_ns = candidate_timestamp_ns
                    if matching_id:
                        event_id = str(item.get("id") or actor_id)
                        event_id_source = "docker_event"
                        event_image_id = matching_id
                        event_image_id_source = "docker_event"
                    else:
                        event_id = f"ref_timestamp:{event_timestamp_ns}:{event_ref}"
                        event_id_source = "ref_timestamp_receipt"
                        event_image_id = image_id
                        event_image_id_source = "post_pull_inspect"
                    break
                except json.JSONDecodeError:
                    text_event = _parse_text_pull_event(line)
                    if text_event is None:
                        continue
                    text_ref, candidate_timestamp_ns = text_event
                    matching_ref = text_ref if text_ref in expected_refs else ""
                    if (
                        not matching_ref
                        or candidate_timestamp_ns < int(pull_started_wall * 1_000_000_000)
                    ):
                        continue
                    event_ref = matching_ref
                    event_timestamp_ns = candidate_timestamp_ns
                    event_id = f"ref_timestamp:{event_timestamp_ns}:{event_ref}"
                    event_id_source = "ref_timestamp_receipt"
                    event_image_id = image_id
                    event_image_id_source = "post_pull_inspect"
                    break
            if (
                getattr(event, "returncode", 0) != 0
                or not event_id
                or not event_ref
                or not event_image_id
                or not event_id_source
                or not event_image_id_source
            ):
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "pull event correlation is missing"}
            return "success", {
                "requested_ref": f"docker.io/{image_ref}",
                "attempts": attempt,
                "status": "success",
                "cache": "up_to_date" if "already exists" in getattr(pull, "stdout", "").lower() else "pulled",
                "event_id": event_id,
                "event_id_source": event_id_source,
                "event_ref": event_ref or None,
                "event_image_id": event_image_id or None,
                "event_image_id_source": event_image_id_source,
                "event_timestamp_ns": event_timestamp_ns,
                "image_id": image_id,
                "digest": digest,
                "digest_ref": digest_ref,
                "elapsed_seconds": round(time.monotonic() - pull_started, 3),
                "observed": {
                    "runner_os": runner_os,
                    "runner_architecture": runner_arch,
                    # Docker's OperatingSystem field is a distro description
                    # (for example, "Ubuntu 24.04.4 LTS"), not an OS family.
                    # Keep the contract's normalized family value here and
                    # preserve the raw description separately for diagnostics.
                    "daemon_os": daemon_os,
                    "daemon_os_description": str(info_payload.get("OperatingSystem", "")).lower(),
                    "daemon_architecture": daemon_arch,
                    "image_os": observed_os,
                    "image_tag": str(platform["tag"]),
                    "image_architecture": observed_arch,
                },
            }
        finally:
            if config_dir:
                shutil.rmtree(config_dir, ignore_errors=True)

    def _strict_platform(self, entry: dict[str, Any]) -> dict[str, Any] | None:
        selected_id = entry.get("_selected_platform_id")
        return platform_for_entry(entry, selected_id) if entry.get("platforms") else None

    def _run_family(self, entry: dict[str, Any]) -> dict[str, Any]:
        attempts: list[dict[str, Any]] = []
        strict = bool(entry.get("executionEvidenceRequired"))
        junit_required = bool(entry.get("releaseRequired"))
        platform = self._strict_platform(entry)
        if strict and platform is None:
            return {
                "id": entry.get("id"),
                "server": entry.get("server"),
                "release_required": bool(entry.get("releaseRequired")),
                "status": "blocked",
                "attempts": [],
                "error": "strict family has no platform",
            }
        if platform is not None:
            entry["_selected_platform_id"] = platform["id"]
        if self._staging_root is None:
            raise RuntimeError("runner staging root is not initialized")
        final_status = "blocked"
        pull_evidence: dict[str, Any] | None = None
        workload_image: dict[str, Any] | None = None
        selected_tag = str(platform.get("tag")) if platform else str(entry["tag"])
        selected_image = str(entry["image"])
        for attempt in range(1, self.max_attempts + 1):
            attempt_started = time.monotonic()
            attempt_started_ns = time.time_ns()
            evidence_dir = self._staging_root / f"{entry['id']}-{platform['id'] if platform else 'generic'}-{attempt}"
            evidence_dir.mkdir(parents=True, exist_ok=False)
            pull_status = "success"
            if strict and pull_evidence is None:
                pull_status, pull_evidence = self._pull_evidence(entry, platform, evidence_dir, attempt)
                if pull_status != "success":
                    attempts.append({"attempt": attempt, "status": pull_status, "pull": pull_evidence})
                    final_status = pull_status
                    self._elapsed_seconds += time.monotonic() - attempt_started
                    if pull_status == "infrastructure_failure" and attempt < self.max_attempts:
                        # A failed pull is not reusable evidence for the next
                        # attempt; require a fresh pull/inspect/event chain.
                        pull_evidence = None
                        shutil.rmtree(evidence_dir, ignore_errors=True)
                        continue
                    break
            command = self._command(entry, evidence_dir if strict or junit_required else None)
            test_timeout = self.timeout_seconds
            if platform:
                test_timeout = int(entry.get("platformTimeouts", {}).get(platform["id"], {}).get("testMinutes", self.timeout_seconds // 60)) * 60
            result = self._invoke(command, test_timeout, self._isolated_runtime_env())
            elapsed = round(time.monotonic() - attempt_started, 3)
            self._elapsed_seconds += elapsed
            raw_stdout = getattr(result, "stdout", "")
            raw_stderr = getattr(result, "stderr", "")
            returncode = getattr(result, "returncode", None)
            stdout_overflow = bool(getattr(result, "stdout_overflow", False))
            stderr_overflow = bool(getattr(result, "stderr_overflow", False))
            status = _classify_command_result(
                returncode,
                raw_stdout,
                raw_stderr,
                stdout_overflow=stdout_overflow,
                stderr_overflow=stderr_overflow,
            )
            if status == "success" and self._deadline is not None and time.monotonic() >= self._deadline:
                status = "blocked"
                raw_stderr = f"{raw_stderr}\njob budget exceeded"
            attempt_result: dict[str, Any] = {"attempt": attempt, "command": redact(" ".join(command)), "returncode": returncode, "elapsed_seconds": elapsed, "status": status}
            if stdout_overflow:
                attempt_result["stdout_overflow"] = True
            if stderr_overflow:
                attempt_result["stderr_overflow"] = True
            if (strict or junit_required) and status == "success":
                try:
                    junit = self._parse_junit(entry, self._xml_candidates(attempt_started_ns, evidence_dir, entry))
                    attempt_result["junit"] = junit
                    if strict:
                        startup = self._marker(evidence_dir, attempt_started_ns)
                        workload_image = self._workload_image(evidence_dir)
                        if pull_evidence and workload_image["image_id"] != pull_evidence.get("image_id"):
                            raise ValueError("workload image does not match pulled image")
                        attempt_result["startup"] = startup
                        attempt_result["workload_image"] = workload_image
                except (OSError, ValueError) as error:
                    status = "blocked"
                    attempt_result["status"] = status
                    attempt_result["evidence_error"] = _bounded(error)
            final_status = status
            if status == "success":
                if self.job_budget_seconds and self._elapsed_seconds > self.job_budget_seconds:
                    final_status = status = "blocked"
                    attempt_result["status"] = status
                    attempt_result["evidence_error"] = "job budget exceeded"
                attempts.append(attempt_result)
                break
            attempts.append(attempt_result)
            if status not in {"infrastructure_failure"} or attempt >= self.max_attempts:
                break
            shutil.rmtree(evidence_dir, ignore_errors=True)

        result: dict[str, Any] = {
            "id": entry["id"],
            "server": entry["server"],
            "image": entry["image"],
            "tag": selected_tag,
            "test_pattern": entry["testPattern"],
            "release_required": bool(entry["releaseRequired"]),
            "status": final_status,
            "attempts": attempts,
        }
        if platform:
            result.update(
                {
                    "schema_version": 2,
                    "family_id": entry["id"],
                    "platform_id": platform["id"],
                    "image_ref": f"{selected_image}:{selected_tag}",
                    "expected": {"os": platform.get("os"), "tag": selected_tag, "architecture": platform.get("architecture"), "runner": platform.get("runner")},
                    "pull": pull_evidence or {"status": "blocked"},
                    "observed": (pull_evidence or {}).get("observed", {}),
                    "workload_image": workload_image or {},
                    "provenance": {"commit": self.commit, "ref": self.ref, "workflow_run_id": self.workflow_run_id},
                }
            )
            successful_attempt = next((item for item in attempts if item.get("status") == "success"), None)
            if successful_attempt:
                result["junit"] = successful_attempt.get("junit")
                result["startup"] = successful_attempt.get("startup")
        else:
            successful_attempt = next((item for item in attempts if item.get("status") == "success"), None)
            if successful_attempt and successful_attempt.get("junit"):
                result["junit"] = successful_attempt["junit"]
        if final_status != "success":
            try:
                result["diagnostics"] = self.diagnostic_runner(entry)
            except Exception as error:  # diagnostics must not hide the original failure
                result["diagnostics"] = {"diagnostic_error": _bounded(error)}
        return result

    def run(self) -> dict[str, Any]:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        invalid_ids = [str(entry.get("id", "")) for entry in self.entries if not SAFE_ID.fullmatch(str(entry.get("id", "")))]
        if invalid_ids:
            raise ValueError(f"unsafe family id: {', '.join(invalid_ids)}")
        allowed_names = {f"{entry['id']}.json" for entry in self.entries} | {"summary.json", "summary.md"}
        unexpected = [
            path.name
            for path in self.report_dir.iterdir()
            if path.is_symlink() or (path.is_file() and path.name not in allowed_names)
        ]
        if unexpected:
            raise RuntimeError(
                f"report directory contains unexpected artifacts: {', '.join(sorted(unexpected))}"
            )
        self._deadline = (
            time.monotonic() + self.job_budget_seconds
            if self.job_budget_seconds is not None
            else None
        )
        # Secret-bearing Docker config lives outside the artifact tree, including on timeout paths.
        self._staging_root = Path(tempfile.mkdtemp(prefix=".image-gate-"))
        results = []
        try:
            for entry in self.entries:
                result = self._run_family(entry)
                results.append(result)
                payload = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
                if len(payload.encode("utf-8")) > MAX_FAMILY_JSON_BYTES:
                    result = {
                        "id": entry.get("id"),
                        "server": entry.get("server"),
                        "release_required": bool(entry.get("releaseRequired")),
                        "status": "blocked",
                        "error": "family artifact limit exceeded",
                        "attempts": [],
                    }
                    results[-1] = result
                    payload = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
                (self.report_dir / f"{entry['id']}.json").write_text(payload, encoding="utf-8")
        finally:
            shutil.rmtree(self._staging_root, ignore_errors=True)
            self._staging_root = None

        counts = {status: sum(result["status"] == status for result in results) for status in (
            "success",
            "product_failure",
            "infrastructure_failure",
            "blocked",
        )}
        selected = len(results)
        release_results = [result for result in results if result["release_required"]]
        release_selected = len(release_results)
        release_success = sum(result["status"] == "success" for result in release_results)
        selected_coverage = f"{counts['success']}/{selected}"
        release_coverage = f"{release_success}/{release_selected}"
        release_gate = release_selected > 0 and release_success == release_selected
        summary: dict[str, Any] = {
            "schema_version": 2,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "manifest_digest": self.manifest_digest,
            "workflow_run_id": self.workflow_run_id,
            "commit": self.commit,
            "ref": self.ref,
            "scope": self.scope,
            "selected": selected,
            "success": counts["success"],
            "product_failure": counts["product_failure"],
            "infrastructure_failure": counts["infrastructure_failure"],
            "blocked": counts["blocked"],
            "coverage": release_coverage,
            "release_coverage": release_coverage,
            "selected_coverage": selected_coverage,
            "release_required_selected": release_selected,
            "release_required_success": release_success,
            "release_gate": release_gate,
            "status": "skipped" if selected == 0 else ("success" if counts["success"] == selected else "failed"),
            "results": results,
            "platforms": [
                {
                    "family_id": result.get("id"),
                    "platform_id": result.get("platform_id"),
                    "status": result.get("status"),
                    "image_ref": result.get("image_ref"),
                    "family_artifact": f"{result.get('id')}.json",
                    "expected": result.get("expected", {}),
                    "observed": result.get("observed", {}),
                    "pull": result.get("pull", {}),
                    "junit": result.get("junit", {}),
                    "startup": result.get("startup", {}),
                    "workload_image": result.get("workload_image", {}),
                }
                for result in results
            ],
        }
        if self.shard_index is not None and self.shard_count is not None:
            summary["shard"] = {
                "index": self.shard_index,
                "count": self.shard_count,
                "family_ids": [result.get("id") for result in results],
            }
        summary_text = json.dumps(summary, ensure_ascii=False, indent=2) + "\n"
        if len(summary_text.encode("utf-8")) > MAX_SUMMARY_BYTES:
            summary["status"] = "blocked"
            summary["blocked"] = max(1, summary["blocked"])
            summary["results"] = [{"id": item.get("id"), "status": item.get("status")} for item in results]
            summary["platforms"] = [{"platform_id": item.get("platform_id"), "status": item.get("status")} for item in results]
            summary_text = json.dumps(summary, ensure_ascii=False, indent=2) + "\n"
        (self.report_dir / "summary.json").write_text(summary_text, encoding="utf-8")
        artifact_bytes = sum(path.stat().st_size for path in self.report_dir.iterdir() if path.is_file())
        if artifact_bytes > MAX_ARTIFACT_BYTES:
            summary["status"] = "blocked"
            summary["blocked"] = max(1, summary["blocked"])
            summary["artifact_limit"] = MAX_ARTIFACT_BYTES
            (self.report_dir / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        self._write_markdown(summary)
        return summary

    def _write_markdown(self, summary: dict[str, Any]) -> None:
        lines = [
            "# Testcontainers image gate",
            "",
            f"- 상태: `{summary['status']}`",
            f"- release 증거 coverage: `{summary['coverage']}`",
            f"- 전체 선택/성공: `{summary['selected_coverage']}`",
            f"- 제품 실패: `{summary['product_failure']}`",
            f"- 인프라 실패: `{summary['infrastructure_failure']}`",
            f"- 차단: `{summary['blocked']}`",
            f"- stable release gate: `{str(summary['release_gate']).lower()}`",
            f"- manifest digest: `{summary['manifest_digest']}`",
            "",
            "| family | image | tag | status | attempts |",
            "|---|---|---|---|---:|",
        ]
        lines.extend(
            f"| `{result['server']}` | `{result['image']}` | `{result['tag']}` | "
            f"`{result['status']}` | {len(result['attempts'])} |"
            for result in summary["results"]
        )
        (self.report_dir / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def _blocked_summary(report_dir: Path, errors: Sequence[str], manifest_path: Path) -> dict[str, Any]:
    report_dir.mkdir(parents=True, exist_ok=True)
    summary = {
        "schema_version": 2,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "manifest_digest": hashlib.sha256(manifest_path.read_bytes()).hexdigest()
        if manifest_path.is_file()
        else "",
        "selected": 0,
        "success": 0,
        "product_failure": 0,
        "infrastructure_failure": 0,
        "blocked": 1,
        "coverage": "0/0",
        "release_coverage": "0/0",
        "selected_coverage": "0/0",
        "release_required_selected": 0,
        "release_required_success": 0,
        "release_gate": False,
        "status": "blocked",
        "errors": list(errors),
        "results": [],
        "workflow_run_id": os.environ.get("GITHUB_RUN_ID", "local"),
        "commit": os.environ.get("GITHUB_SHA", "local"),
        "ref": os.environ.get("GITHUB_REF", "local"),
        "scope": "blocked",
        "platforms": [],
    }
    (report_dir / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return summary


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--scope", choices=("changed", "full", "family"), default="changed")
    parser.add_argument("--family-id")
    parser.add_argument("--platform-id")
    parser.add_argument("--default-platform-id", default="amd64")
    parser.add_argument("--require-selection", action="store_true")
    parser.add_argument("--changed-path", action="append", default=[])
    parser.add_argument("--changed-path-file", type=Path)
    parser.add_argument("--report-dir", type=Path, default=Path("build/reports/testcontainers-image-gate"))
    parser.add_argument("--gradle-task", default="./gradlew :bluetape4k-testcontainers:test")
    parser.add_argument("--max-attempts", type=int, default=1)
    parser.add_argument("--pull-timeout-seconds", type=int, default=60)
    parser.add_argument("--timeout-minutes", type=int, default=30)
    parser.add_argument("--diagnostic-timeout-seconds", type=int, default=30)
    parser.add_argument("--job-budget-minutes", type=int)
    parser.add_argument("--shard-index", type=int)
    parser.add_argument("--shard-count", type=int)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        entries = load_manifest(args.manifest)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        _blocked_summary(args.report_dir, [str(error)], args.manifest)
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2
    errors = validate_manifest(entries)
    if errors:
        _blocked_summary(args.report_dir, errors, args.manifest)
        print("BLOCKED: manifest contract drift", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 2
    changed_paths = set(args.changed_path)
    if args.changed_path_file and args.changed_path_file.is_file():
        changed_paths.update(
            line.strip() for line in args.changed_path_file.read_text(encoding="utf-8").splitlines() if line.strip()
        )
    try:
        selected = select_entries(
            entries,
            changed_paths,
            scope=args.scope,
            family_id=args.family_id,
            platform_id=args.platform_id,
            require_selection=args.require_selection,
            default_platform_id=args.default_platform_id,
        )
        if args.shard_index is not None or args.shard_count is not None:
            if args.scope != "full":
                raise SelectionError("shards require full scope")
            if args.shard_index is None or args.shard_count is None:
                raise SelectionError("shard index and count must be provided together")
            selected = select_shard_entries(
                selected,
                shard_index=args.shard_index,
                shard_count=args.shard_count,
            )
    except (ValueError, SelectionError) as error:
        _blocked_summary(args.report_dir, [str(error)], args.manifest)
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2
    try:
        summary = GateRunner(
            selected,
            args.report_dir,
            gradle_task=args.gradle_task,
            max_attempts=args.max_attempts,
            timeout_minutes=args.timeout_minutes,
            manifest_path=args.manifest,
            pull_timeout_seconds=args.pull_timeout_seconds,
            diagnostic_timeout_seconds=args.diagnostic_timeout_seconds,
            job_budget_minutes=args.job_budget_minutes,
            scope=args.scope,
            shard_index=args.shard_index,
            shard_count=args.shard_count,
        ).run()
    except (OSError, RuntimeError, ValueError) as error:
        _blocked_summary(args.report_dir, [str(error)], args.manifest)
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 1
    print(json.dumps({key: summary[key] for key in (
        "status", "coverage", "selected_coverage", "product_failure",
        "infrastructure_failure", "blocked", "release_gate"
    )}, ensure_ascii=False))
    return 0 if summary["status"] in {"success", "skipped"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
