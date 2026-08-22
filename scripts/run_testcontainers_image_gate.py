#!/usr/bin/env python3
"""Run the manifest-driven Testcontainers image gate sequentially."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shlex
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Iterable, Sequence

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
if str(REPOSITORY_ROOT) not in sys.path:
    sys.path.insert(0, str(REPOSITORY_ROOT))

from scripts.testcontainers_image_gate import MANIFEST, load_manifest, select_entries, validate_manifest


CommandRunner = Callable[[list[str], int], Any]
DiagnosticRunner = Callable[[dict[str, Any]], dict[str, str]]
MAX_ATTEMPTS = 3
MAX_OUTPUT_CHARS = 12_000
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


def redact(value: str) -> str:
    """Remove common credential values before text enters logs or artifacts."""

    redacted = BEARER_PATTERN.sub(r"\1<redacted>", value)
    redacted = SECRET_PATTERN.sub(r"\1=<redacted>", redacted)
    return redacted


def _bounded(value: object) -> str:
    text = redact(str(value or ""))
    if len(text) <= MAX_OUTPUT_CHARS:
        return text
    return text[:MAX_OUTPUT_CHARS] + "\n...[truncated]"


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


def _subprocess_runner(command: list[str], timeout_seconds: int) -> Any:
    try:
        return subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired as error:
        return type(
            "TimeoutResult",
            (),
            {
                "returncode": None,
                "stdout": error.stdout or "",
                "stderr": (error.stderr or "") + " timeout",
            },
        )()


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
    ) -> None:
        self.entries = [dict(entry) for entry in entries]
        self.report_dir = report_dir
        self.command_runner = command_runner
        self.diagnostic_runner = diagnostic_runner or self._collect_diagnostics
        self.gradle_task = gradle_task
        self.max_attempts = max(1, min(max_attempts, MAX_ATTEMPTS))
        self.timeout_seconds = max(60, timeout_minutes * 60)
        self.manifest_digest = self._digest(manifest_path)

    def _digest(self, manifest_path: Path | None) -> str:
        if manifest_path and manifest_path.is_file():
            return hashlib.sha256(manifest_path.read_bytes()).hexdigest()
        canonical = json.dumps(self.entries, ensure_ascii=False, sort_keys=True).encode("utf-8")
        return hashlib.sha256(canonical).hexdigest()

    def _command(self, entry: dict[str, Any]) -> list[str]:
        command = shlex.split(self.gradle_task)
        if test_task := entry.get("testTask"):
            task_positions = [index for index, part in enumerate(command) if part.startswith(":")]
            if len(task_positions) != 1:
                raise ValueError(f"expected one Gradle task in command: {self.gradle_task}")
            command[task_positions[0]] = str(test_task)
        command.extend(("--tests", str(entry["testPattern"]), "--no-configuration-cache"))
        return command

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
            result = self.command_runner(command, min(self.timeout_seconds, 60))
            diagnostics[label] = _bounded(
                f"exit={getattr(result, 'returncode', None)}\n"
                f"stdout={getattr(result, 'stdout', '')}\n"
                f"stderr={getattr(result, 'stderr', '')}"
            )
        return diagnostics

    def _run_family(self, entry: dict[str, Any]) -> dict[str, Any]:
        attempts: list[dict[str, Any]] = []
        command = self._command(entry)
        final_status = "blocked"
        for attempt in range(1, self.max_attempts + 1):
            started = time.monotonic()
            result = self.command_runner(command, self.timeout_seconds)
            elapsed = round(time.monotonic() - started, 3)
            raw_stdout = getattr(result, "stdout", "")
            raw_stderr = getattr(result, "stderr", "")
            stdout = _bounded(raw_stdout)
            stderr = _bounded(raw_stderr)
            returncode = getattr(result, "returncode", None)
            status = _classify_command_result(returncode, raw_stdout, raw_stderr)
            attempts.append(
                {
                    "attempt": attempt,
                    "command": redact(" ".join(command)),
                    "returncode": returncode,
                    "elapsed_seconds": elapsed,
                    "status": status,
                    "stdout": stdout,
                    "stderr": stderr,
                }
            )
            final_status = status
            if status == "success":
                break

        result: dict[str, Any] = {
            "id": entry["id"],
            "server": entry["server"],
            "image": entry["image"],
            "tag": entry["tag"],
            "test_pattern": entry["testPattern"],
            "release_required": bool(entry["releaseRequired"]),
            "status": final_status,
            "attempts": attempts,
        }
        if final_status != "success":
            try:
                result["diagnostics"] = self.diagnostic_runner(entry)
            except Exception as error:  # diagnostics must not hide the original failure
                result["diagnostics"] = {"diagnostic_error": _bounded(error)}
        return result

    def run(self) -> dict[str, Any]:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        results = []
        for entry in self.entries:
            result = self._run_family(entry)
            results.append(result)
            (self.report_dir / f"{entry['id']}.json").write_text(
                json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
            )

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
            "schema_version": 1,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "manifest_digest": self.manifest_digest,
            "selected": selected,
            "success": counts["success"],
            "product_failure": counts["product_failure"],
            "infrastructure_failure": counts["infrastructure_failure"],
            "blocked": counts["blocked"],
            "coverage": f"{counts['success']}/{selected}",
            "release_gate": release_gate,
            "status": "success" if release_gate else ("skipped" if selected == 0 else "failed"),
            "results": results,
        }
        (self.report_dir / "summary.json").write_text(
            json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
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
        "schema_version": 1,
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
    }
    (report_dir / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return summary


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    parser.add_argument("--scope", choices=("changed", "full"), default="changed")
    parser.add_argument("--changed-path", action="append", default=[])
    parser.add_argument("--changed-path-file", type=Path)
    parser.add_argument("--report-dir", type=Path, default=Path("build/reports/testcontainers-image-gate"))
    parser.add_argument("--gradle-task", default="./gradlew :bluetape4k-testcontainers:test")
    parser.add_argument("--max-attempts", type=int, default=1)
    parser.add_argument("--timeout-minutes", type=int, default=30)
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
    selected = select_entries(entries, changed_paths, scope=args.scope)
    summary = GateRunner(
        selected,
        args.report_dir,
        gradle_task=args.gradle_task,
        max_attempts=args.max_attempts,
        timeout_minutes=args.timeout_minutes,
        manifest_path=args.manifest,
    ).run()
    print(json.dumps({key: summary[key] for key in (
        "status", "coverage", "product_failure", "infrastructure_failure", "blocked", "release_gate"
    )}, ensure_ascii=False))
    return 0 if summary["status"] in {"success", "skipped"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
