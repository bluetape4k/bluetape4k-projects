#!/usr/bin/env python3
"""Run the manifest-driven Testcontainers image gate sequentially."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shlex
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
_KNOWN_SECRETS: set[str] = set()


class CommandResult:
    """Bounded subprocess result shared by real and fake command runners."""

    def __init__(self, returncode: int | None, stdout: str = "", stderr: str = "", elapsed: float = 0.0):
        self.returncode = returncode
        self.stdout = stdout
        self.stderr = stderr
        self.elapsed_seconds = elapsed


def redact(value: str) -> str:
    """Remove common credential values before text enters logs or artifacts."""

    redacted = value
    for secret in sorted((item for item in _KNOWN_SECRETS if item), key=len, reverse=True):
        redacted = redacted.replace(secret, "<redacted>")
    redacted = BASIC_AUTH_URL_PATTERN.sub(r"\1<redacted>:<redacted>@", redacted)
    redacted = BEARER_PATTERN.sub(r"\1<redacted>", redacted)
    redacted = SECRET_PATTERN.sub(r"\1=<redacted>", redacted)
    return redacted


def _bounded(value: object) -> str:
    text = redact(str(value or ""))
    lines = text.splitlines()
    if len(lines) > MAX_OUTPUT_LINES:
        text = "\n".join(lines[:MAX_OUTPUT_LINES]) + "\n...[line limit]"
    if len(text.encode("utf-8")) <= MAX_OUTPUT_CHARS:
        return text
    encoded = text.encode("utf-8")[:MAX_OUTPUT_CHARS]
    return encoded.decode("utf-8", errors="ignore") + "\n...[byte limit]"


def register_secret(value: object) -> None:
    """Register raw/decoded credentials for every report boundary."""

    if isinstance(value, str) and value:
        _KNOWN_SECRETS.add(value)
        try:
            import base64

            decoded = base64.b64decode(value, validate=True).decode("utf-8")
        except Exception:
            decoded = ""
        if decoded:
            _KNOWN_SECRETS.add(decoded)


def _read_bounded(path: Path, limit: int) -> str:
    with path.open("rb") as stream:
        data = stream.read(limit + 1)
    if len(data) > limit:
        raise ValueError(f"output exceeds {limit} bytes: {path.name}")
    return redact(data.decode("utf-8", errors="replace"))


def classify_failure(returncode: int | None, stdout: str, stderr: str) -> str:
    """Classify a command result without conflating registry/daemon failures with product bugs."""

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


def _classify_command_result(returncode: int | None, stdout: str, stderr: str) -> str:
    status = classify_failure(returncode, stdout, stderr)
    if status == "success" and not re.search(r"\bBUILD SUCCESSFUL\b", f"{stdout}\n{stderr}"):
        return "blocked"
    return status


def _subprocess_runner(command: list[str], timeout_seconds: int, *, env: dict[str, str] | None = None) -> Any:
    started = time.monotonic()
    stdout_file = tempfile.TemporaryFile()
    stderr_file = tempfile.TemporaryFile()
    process: subprocess.Popen[bytes] | None = None
    try:
        process = subprocess.Popen(
            command,
            stdout=stdout_file,
            stderr=stderr_file,
            env=env,
            start_new_session=True,
        )
        try:
            process.wait(timeout=timeout_seconds)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGTERM)
                process.wait(timeout=5)
            except (OSError, subprocess.TimeoutExpired):
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except OSError:
                    pass
                process.wait()
            return CommandResult(None, _bounded(_read_tail(stdout_file)), _bounded(_read_tail(stderr_file) + " timeout"), time.monotonic() - started)
        return CommandResult(
            process.returncode,
            _bounded(_read_tail(stdout_file)),
            _bounded(_read_tail(stderr_file)),
            time.monotonic() - started,
        )
    finally:
        stdout_file.close()
        stderr_file.close()


def _read_tail(file_object: Any, limit: int = MAX_OUTPUT_CHARS) -> str:
    file_object.seek(0, os.SEEK_END)
    size = file_object.tell()
    file_object.seek(max(0, size - limit - 1))
    return file_object.read(limit + 1).decode("utf-8", errors="replace")


def verify_release_summary(
    summary: dict[str, Any],
    *,
    expected_coverage: str,
    platform_id: str,
    expected_tag: str,
    expected_architecture: str,
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
        if expected.get("tag") != expected_tag or observed.get("image_tag") not in {expected_tag, None}:
            errors.append("expected/observed tag mismatch")
        if expected.get("architecture") != expected_architecture or observed.get("image_architecture") not in {expected_architecture, None}:
            errors.append("expected/observed architecture mismatch")
        if not pull.get("event_id") or not str(pull.get("digest", "")).startswith("sha256:"):
            errors.append("pull event and digest evidence are required")
        if junit.get("workload_tests") != 1 or junit.get("skipped", 1) != 0 or junit.get("failures", 1) != 0 or junit.get("errors", 1) != 0:
            errors.append("successful workload JUnit evidence is required")
        if result.get("family_artifact") in {None, ""}:
            errors.append("family artifact reference is required")
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
    ) -> None:
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
        self.manifest_digest = self._digest(manifest_path)
        self._staging_root: Path | None = None
        self._elapsed_seconds = 0.0

    def _digest(self, manifest_path: Path | None) -> str:
        if manifest_path and manifest_path.is_file():
            return hashlib.sha256(manifest_path.read_bytes()).hexdigest()
        canonical = json.dumps(self.entries, ensure_ascii=False, sort_keys=True).encode("utf-8")
        return hashlib.sha256(canonical).hexdigest()

    def _invoke(self, command: list[str], timeout_seconds: int, env: dict[str, str] | None = None) -> Any:
        if env is None:
            return self.command_runner(command, timeout_seconds)
        try:
            return self.command_runner(command, timeout_seconds, env=env)
        except TypeError:
            # Existing fake runners intentionally expose the old two-argument contract.
            return self.command_runner(command, timeout_seconds)

    @staticmethod
    def _sanitized_runtime_env() -> dict[str, str]:
        """Keep registry credentials and Docker overrides out of Gradle and diagnostics."""

        env = os.environ.copy()
        for key in (
            "DOCKER_AUTH_CONFIG",
            "TESTCONTAINERS_REGISTRY_MIRROR",
            "DOCKER_HOST",
            "DOCKER_CONTEXT",
        ):
            env.pop(key, None)
        return env

    def _command(self, entry: dict[str, Any], evidence_dir: Path | None = None) -> list[str]:
        strict = bool(entry.get("executionEvidenceRequired"))
        if strict and entry.get("_selected_platform_id") == "arm64" and evidence_dir is not None:
            workload = str(entry.get("workloadTestPattern", entry["testPattern"])).removesuffix("()")
            return [
                "./gradlew",
                f"-Dtestcontainers.image-gate.evidence-dir={evidence_dir}",
                ":bluetape4k-testcontainers:test",
                "--tests",
                workload,
                "--no-configuration-cache",
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
        if "--tests" not in command:
            selector = entry.get("testPattern")
            command.extend(("--tests", str(selector)))
        if "--no-configuration-cache" not in command:
            command.append("--no-configuration-cache")
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
        image_ref = f"{entry['image']}:{entry['tag']}"
        commands: list[tuple[str, list[str]]] = [
            (
                "docker_ps",
                ["docker", "ps", "-a", "--no-trunc", "--filter", f"ancestor={image_ref}"],
            ),
            ("docker_inspect", ["docker", "inspect", image_ref]),
            ("docker_events", ["timeout", "10s", "docker", "events", "--since", "5m"]),
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
                self._sanitized_runtime_env(),
            )
            diagnostics[label] = self._diagnostic_bounded(
                f"exit={getattr(result, 'returncode', None)}\n"
                f"stdout={getattr(result, 'stdout', '')}\n"
                f"stderr={getattr(result, 'stderr', '')}"
            )
        return diagnostics

    @staticmethod
    def _is_regular(path: Path) -> bool:
        return path.is_file() and not path.is_symlink()

    @staticmethod
    def _canonical_testcase(classname: str, name: str) -> str:
        return f"{classname}.{name[:-2] if name.endswith('()') else name}"

    def _xml_candidates(self, started_ns: int, evidence_dir: Path) -> list[Path]:
        roots = [
            REPOSITORY_ROOT / "testing/testcontainers/build/test-results/test",
            evidence_dir,
        ]
        candidates: list[Path] = []
        for root in roots:
            if not root.is_dir() or root.is_symlink():
                continue
            for path in root.glob("*.xml"):
                try:
                    if self._is_regular(path) and path.stat().st_mtime_ns >= started_ns:
                        candidates.append(path)
                except OSError:
                    continue
        return candidates[:MAX_XML_FILES]

    def _parse_junit(self, entry: dict[str, Any], files: list[Path]) -> dict[str, Any]:
        if not files:
            raise ValueError("JUnit evidence is missing")
        expected_suite = str(entry["testPattern"])
        expected_workload = str(entry.get("workloadTestPattern", "")).replace("()", "")
        matched: tuple[Path, ET.Element] | None = None
        for path in files:
            if path.stat().st_size > MAX_XML_BYTES:
                raise ValueError(f"JUnit XML exceeds {MAX_XML_BYTES} bytes")
            raw = path.read_bytes()
            if b"<!DOCTYPE" in raw or b"<!ENTITY" in raw:
                raise ValueError("DTD/ENTITY is not allowed in JUnit XML")
            try:
                root = ET.fromstring(raw)
            except ET.ParseError as error:
                raise ValueError(f"malformed JUnit XML: {path.name}") from error
            suite_name = root.attrib.get("name", "")
            if suite_name == expected_suite or suite_name.endswith(expected_suite):
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
        if tests <= 0 or tests <= skipped or failures or errors:
            raise ValueError("JUnit suite has no successful execution evidence")
        workload_matches = []
        for testcase in testcases:
            canonical = self._canonical_testcase(testcase.attrib.get("classname", ""), testcase.attrib.get("name", ""))
            if canonical == expected_workload:
                workload_matches.append(testcase)
        if len(workload_matches) != 1:
            raise ValueError(f"workload testcase mismatch: {expected_workload}")
        workload = workload_matches[0]
        if list(workload) or workload.attrib.get("status") == "skipped":
            raise ValueError("workload testcase failed or was skipped")
        return {
            "suite": expected_suite,
            "tests": tests,
            "skipped": skipped,
            "failures": failures,
            "errors": errors,
            "workload_testcase": expected_workload,
            "workload_tests": 1,
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
        if content != "Ignite node started OK":
            raise ValueError("startup marker content mismatch")
        return {"ready": True, "marker": content, "marker_source": "bounded_attempt_marker"}

    def _pull_evidence(self, entry: dict[str, Any], platform: dict[str, Any], attempt_root: Path, attempt: int) -> tuple[str, dict[str, Any]]:
        image_ref = f"{entry['image']}:{platform['tag']}"
        architecture = str(platform["id"])
        auth_config = os.environ.get("DOCKER_AUTH_CONFIG", "")
        register_secret(auth_config)
        env = os.environ.copy()
        env.pop("DOCKER_AUTH_CONFIG", None)
        env.pop("TESTCONTAINERS_REGISTRY_MIRROR", None)
        config_dir: Path | None = None
        try:
            if auth_config:
                config_dir = attempt_root / "docker-config"
                config_dir.mkdir(mode=0o700)
                config_file = config_dir / "config.json"
                config_file.write_text(auth_config, encoding="utf-8")
                config_file.chmod(0o600)
                env["DOCKER_CONFIG"] = str(config_dir)
            pull_started = time.monotonic()
            pull = self._invoke(
                ["docker", "pull", "--platform", f"linux/{architecture}", image_ref],
                self.pull_timeout_seconds,
                env,
            )
            pull_status = classify_failure(getattr(pull, "returncode", None), getattr(pull, "stdout", ""), getattr(pull, "stderr", ""))
            if getattr(pull, "returncode", None) != 0:
                return pull_status, {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": pull_status}
            inspect = self._invoke(["docker", "image", "inspect", image_ref], self.pull_timeout_seconds, env)
            try:
                payload = json.loads(getattr(inspect, "stdout", ""))
                image = payload[0] if isinstance(payload, list) else payload
                image_id = str(image.get("Id", ""))
                digests = image.get("RepoDigests", [])
                digest = next((item.split("@", 1)[1] for item in digests if "@sha256:" in item), "")
                observed_os = str(image.get("Os", ""))
                observed_arch = canonical_architecture(image.get("Architecture"))
            except (ValueError, IndexError, AttributeError, TypeError, json.JSONDecodeError) as error:
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": _bounded(error)}
            if not image_id.startswith("sha256:") or not digest.startswith("sha256:") or observed_os != "linux" or observed_arch != architecture:
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "image platform or digest mismatch"}
            context = self._invoke(["docker", "context", "show"], self.pull_timeout_seconds, env)
            info = self._invoke(["docker", "info", "--format", "{{json .}}"], self.pull_timeout_seconds, env)
            uname = self._invoke(["uname", "-m"], self.pull_timeout_seconds, env)
            if getattr(context, "returncode", 1) != 0 or getattr(context, "stdout", "").strip() != "default":
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "non-default Docker context"}
            try:
                info_payload = json.loads(getattr(info, "stdout", "{}"))
            except json.JSONDecodeError:
                info_payload = {}
            daemon_arch = canonical_architecture(info_payload.get("Architecture"))
            runner_arch = canonical_architecture(getattr(uname, "stdout", "").strip())
            if daemon_arch != architecture or runner_arch != architecture:
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "daemon or runner architecture mismatch"}
            event = self._invoke(
                ["docker", "events", "--since", "1s", "--filter", "type=image", "--filter", "event=pull", "--format", "{{json .}}"],
                self.pull_timeout_seconds,
                env,
            )
            event_id = ""
            for line in getattr(event, "stdout", "").splitlines():
                try:
                    item = json.loads(line)
                    event_id = str(item.get("id") or item.get("Actor", {}).get("ID") or "")
                except json.JSONDecodeError:
                    continue
            if not event_id:
                return "blocked", {"requested_ref": f"docker.io/{image_ref}", "attempts": attempt, "status": "blocked", "error": "pull event correlation is missing"}
            return "success", {
                "requested_ref": f"docker.io/{image_ref}",
                "attempts": attempt,
                "status": "success",
                "cache": "up_to_date" if "already exists" in getattr(pull, "stdout", "").lower() else "pulled",
                "event_id": event_id,
                "image_id": image_id,
                "digest": digest,
                "elapsed_seconds": round(time.monotonic() - pull_started, 3),
                "observed": {
                    "runner_os": "linux",
                    "runner_architecture": runner_arch,
                    "daemon_os": str(info_payload.get("OperatingSystem", "linux")).lower() or "linux",
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
        platform = self._strict_platform(entry)
        if strict and platform is None:
            return {"id": entry.get("id"), "server": entry.get("server"), "status": "blocked", "attempts": [], "error": "strict family has no platform"}
        if platform is not None:
            entry["_selected_platform_id"] = platform["id"]
        if self._staging_root is None:
            raise RuntimeError("runner staging root is not initialized")
        final_status = "blocked"
        pull_evidence: dict[str, Any] | None = None
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
                        shutil.rmtree(evidence_dir, ignore_errors=True)
                        continue
                    break
            command = self._command(entry, evidence_dir if strict else None)
            test_timeout = self.timeout_seconds
            if platform:
                test_timeout = int(entry.get("platformTimeouts", {}).get(platform["id"], {}).get("testMinutes", self.timeout_seconds // 60)) * 60
            result = self._invoke(command, test_timeout, self._sanitized_runtime_env())
            elapsed = round(time.monotonic() - attempt_started, 3)
            self._elapsed_seconds += elapsed
            raw_stdout = getattr(result, "stdout", "")
            raw_stderr = getattr(result, "stderr", "")
            returncode = getattr(result, "returncode", None)
            status = _classify_command_result(returncode, raw_stdout, raw_stderr)
            attempt_result: dict[str, Any] = {"attempt": attempt, "command": redact(" ".join(command)), "returncode": returncode, "elapsed_seconds": elapsed, "status": status}
            if strict and status == "success":
                try:
                    junit = self._parse_junit(entry, self._xml_candidates(attempt_started_ns, evidence_dir))
                    startup = self._marker(evidence_dir, attempt_started_ns)
                    attempt_result["junit"] = junit
                    attempt_result["startup"] = startup
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
                    "provenance": {"commit": self.commit, "ref": self.ref, "workflow_run_id": self.workflow_run_id},
                }
            )
            successful_attempt = next((item for item in attempts if item.get("status") == "success"), None)
            if successful_attempt:
                result["junit"] = successful_attempt.get("junit")
                result["startup"] = successful_attempt.get("startup")
        if final_status != "success":
            try:
                result["diagnostics"] = self.diagnostic_runner(entry)
            except Exception as error:  # diagnostics must not hide the original failure
                result["diagnostics"] = {"diagnostic_error": _bounded(error)}
        return result

    def run(self) -> dict[str, Any]:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        self._staging_root = Path(tempfile.mkdtemp(prefix=".image-gate-", dir=self.report_dir))
        results = []
        try:
            for entry in self.entries:
                result = self._run_family(entry)
                results.append(result)
                payload = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
                if len(payload.encode("utf-8")) > MAX_FAMILY_JSON_BYTES:
                    result = {"id": entry.get("id"), "server": entry.get("server"), "status": "blocked", "error": "family artifact limit exceeded", "attempts": []}
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
        release_gate = selected > 0 and counts["success"] == selected and all(
            result["release_required"] for result in results
        )
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
            "coverage": f"{counts['success']}/{selected}",
            "release_gate": release_gate,
            "status": "success" if release_gate else ("skipped" if selected == 0 else "failed"),
            "results": results,
            "platforms": [
                {
                    "platform_id": result.get("platform_id"),
                    "status": result.get("status"),
                    "family_artifact": f"{result.get('id')}.json",
                    "expected": result.get("expected", {}),
                    "observed": result.get("observed", {}),
                    "pull": result.get("pull", {}),
                    "junit": result.get("junit", {}),
                    "startup": result.get("startup", {}),
                }
                for result in results
            ],
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
            f"- 선택/성공: `{summary['coverage']}`",
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
    except (ValueError, SelectionError) as error:
        _blocked_summary(args.report_dir, [str(error)], args.manifest)
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2
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
    ).run()
    print(json.dumps({key: summary[key] for key in (
        "status", "coverage", "product_failure", "infrastructure_failure", "blocked", "release_gate"
    )}, ensure_ascii=False))
    return 0 if summary["status"] in {"success", "skipped"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
