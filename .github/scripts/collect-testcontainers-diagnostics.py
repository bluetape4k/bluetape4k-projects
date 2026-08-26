#!/usr/bin/env python3
"""Collect bounded, redacted Testcontainers diagnostics and test reports."""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from collections.abc import Iterable
from pathlib import Path
from typing import Any, NamedTuple

DEFAULT_MAX_BYTES = 2_000_000
DEFAULT_MAX_REPORT_FILES = 200
MAX_REPORT_FILE_BYTES = 2_000_000
MAX_REPORT_ENTRIES = 50_000
ALLOWLIST = {
    "confluentinc/cp-kafka@sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130",
    "redis@sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a",
}
SHA40 = re.compile(r"^[0-9a-fA-F]{40}$")
USES_KEY = re.compile(r"(?i)(?:^|[,{])\s*(?:-\s*)?(?:[\"']uses[\"']|uses)\s*:")
URI = re.compile(
    r"\b(?:https?|amqps?|redis|kafka|postgres(?:ql)?|mongodb(?:\+srv)?|mysql|file|jdbc:[a-z0-9+.-]+)://[^\s<>\"']+"
)
AUTHORIZATION_LINE = re.compile(r"(?im)^(\s*[\"']?authorization[\"']?\s*[:=]\s*).*$")
SENSITIVE_KEY = (
    r"(?:payload|message|body|value|authorization|access[_-]?token|access[_-]?key(?:[_-]?id)?|token|password|secret[_-]?key|secret|"
    r"private[_-]?key|api[_-]?key|"
    r"[a-z][a-z0-9_.-]*(?:authorization|access[_-]?token|access[_-]?key(?:[_-]?id)?|token|password|secret|"
    r"private[_-]?key|api[_-]?key)[a-z0-9_.-]*)"
)
SENSITIVE_ASSIGNMENT_KEY = re.compile(
    rf'''(?is)(?<![a-z0-9_.-])([\"']?{SENSITIVE_KEY}[\"']?\s*[:=]\s*)'''
)
XML_SENSITIVE = re.compile(
    rf"(?is)(<\s*(?P<tag>{SENSITIVE_KEY}|failure|error)\b[^>]*>).*?(</\s*(?P=tag)\s*>)"
)
SENSITIVE_ENV_LINE = re.compile(
    r"(?im)^\s*[A-Z][A-Z0-9_]*(?:AUTHORIZATION|ACCESS[_-]?TOKEN|ACCESS[_-]?KEY(?:[_-]?ID)?|"
    r"TOKEN|PASSWORD|SECRET|PRIVATE[_-]?KEY|API[_-]?KEY|CREDENTIALS?)[A-Z0-9_-]*\s*[:=].*$"
)
SENSITIVE_LINE = re.compile(r"(?im)^\s*(?:payload|message|body|value)\s*[:=].*$")
EXCEPTION_MESSAGE = re.compile(r"(?im)^([^\r\n]*(?:Exception|Error))\s*:\s*[^\r\n]*$")


class DockerLogsResult(NamedTuple):
    returncode: int
    stdout: str
    truncated: bool


def _assignment_value_end(value: str, start: int) -> int:
    """Find the end of a scalar, quoted, or balanced object assignment."""

    index = start
    while index < len(value) and value[index].isspace():
        index += 1
    if index >= len(value):
        return index

    opening = value[index]
    if opening in "\"'":
        quote = opening
        index += 1
        escaped = False
        while index < len(value):
            character = value[index]
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == quote:
                return index + 1
            index += 1
        return index

    if opening in "[{":
        closing = {"[": "]", "{": "}"}
        stack = [opening]
        index += 1
        quote: str | None = None
        escaped = False
        while index < len(value) and stack:
            character = value[index]
            if quote is not None:
                if escaped:
                    escaped = False
                elif character == "\\":
                    escaped = True
                elif character == quote:
                    quote = None
            elif character in "\"'":
                quote = character
            elif character in "[{":
                stack.append(character)
            elif character in "]}" and character == closing[stack[-1]]:
                stack.pop()
            index += 1
        return index

    while index < len(value) and value[index] not in " \t,;&}]\r\n":
        index += 1
    return index


def redact_sensitive_assignments(value: str) -> str:
    """Replace complete sensitive assignment values, including nested objects."""

    spans: list[tuple[int, int]] = []
    cursor = 0
    while match := SENSITIVE_ASSIGNMENT_KEY.search(value, cursor):
        start = match.end()
        end = _assignment_value_end(value, start)
        value_start = start
        while value_start < end and value[value_start].isspace():
            value_start += 1
        spans.append((value_start, end))
        cursor = max(end, match.end() + 1)

    if not spans:
        return value
    output: list[str] = []
    cursor = 0
    for start, end in spans:
        if start < cursor:
            continue
        output.extend((value[cursor:start], "[REDACTED]"))
        cursor = end
    output.append(value[cursor:])
    return "".join(output)


def sanitize(value: str) -> str:
    """Redact credentials, endpoints, payloads, and exception messages."""

    value = AUTHORIZATION_LINE.sub(r"\1[REDACTED]", value)
    value = redact_sensitive_assignments(value)
    value = URI.sub("[REDACTED]", value)
    value = XML_SENSITIVE.sub(r"\1[REDACTED]\3", value)
    value = SENSITIVE_ENV_LINE.sub("[REDACTED]", value)
    value = SENSITIVE_LINE.sub("[REDACTED]", value)
    value = EXCEPTION_MESSAGE.sub(r"\1: [REDACTED]", value)
    return value


def bounded_bytes(value: str, limit: int) -> tuple[bytes, bool]:
    encoded = value.encode("utf-8", errors="replace")
    if len(encoded) <= limit:
        return encoded, False
    # Drop an incomplete trailing code point rather than emitting invalid UTF-8.
    bounded = encoded[:limit].decode("utf-8", errors="ignore").encode("utf-8")
    return bounded, True


def run_docker(task_name: str, args: list[str]) -> subprocess.CompletedProcess[str]:
    try:
        return subprocess.run(
            ["docker", *args],
            check=False,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
    except OSError as error:
        raise RuntimeError(f"{task_name}: docker command unavailable") from error


def run_docker_logs(task_name: str, container_id: str, max_bytes: int) -> DockerLogsResult:
    command = ["docker", "logs", "--tail", "200", container_id]
    try:
        process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    except OSError as error:
        raise RuntimeError(f"{task_name}: docker command unavailable") from error
    output = process.stdout.read(max_bytes + 1) if process.stdout is not None else b""
    output_truncated = len(output) > max_bytes
    if output_truncated:
        try:
            process.kill()
        except ProcessLookupError:
            pass
    process.wait()
    return DockerLogsResult(
        returncode=0 if output_truncated else process.returncode,
        stdout=output.decode("utf-8", errors="replace"),
        truncated=output_truncated,
    )


def workflow_action_refs(path: Path) -> list[str]:
    refs: list[str] = []
    with path.open("rb") as handle:
        workflow_bytes = handle.read(MAX_REPORT_FILE_BYTES + 1)
    if len(workflow_bytes) > MAX_REPORT_FILE_BYTES:
        raise ValueError(f"{path}: workflow file exceeds size limit")
    for line_number, original_line in enumerate(
        workflow_bytes.decode("utf-8", errors="replace").splitlines(), start=1
    ):
        if original_line.lstrip().startswith("#"):
            continue
        line = original_line.split("#", 1)[0]
        for match in USES_KEY.finditer(line):
            tail = line[match.end():].lstrip()
            if not tail:
                raise ValueError(f"{path}:{line_number}: missing workflow action reference")
            if tail[0] in "\"'":
                quote = tail[0]
                end = tail.find(quote, 1)
                if end < 0:
                    raise ValueError(f"{path}:{line_number}: malformed workflow action reference")
                reference = tail[1:end]
            else:
                reference = re.split(r"[\s,}]+", tail, maxsplit=1)[0]
            if "@" not in reference:
                raise ValueError(f"{path}:{line_number}: mutable workflow action reference")
            action, ref = reference.rsplit("@", 1)
            if not action or not SHA40.fullmatch(ref):
                raise ValueError(f"{path}:{line_number}: workflow action is not pinned to a commit SHA")
            normalized = f"{action}@{ref.lower()}"
            if normalized not in refs:
                refs.append(normalized)
    return refs


def inspect_container(task_name: str, container_id: str) -> dict[str, Any]:
    inspected = run_docker(task_name, ["inspect", container_id])
    if inspected.returncode != 0:
        raise RuntimeError(f"{task_name}: docker inspect failed")
    try:
        payload = json.loads(inspected.stdout)[0]
    except (IndexError, json.JSONDecodeError, TypeError) as error:
        raise RuntimeError(f"{task_name}: invalid docker inspect response") from error

    config = payload.get("Config") or {}
    image = str(config.get("Image") or payload.get("Image") or "")
    repo_digests = payload.get("RepoDigests") or []
    if not repo_digests:
        if not image:
            raise RuntimeError(f"{task_name}: container image is missing")
        image_inspected = run_docker(task_name, ["image", "inspect", image])
        if image_inspected.returncode != 0:
            raise RuntimeError(f"{task_name}: docker image inspect failed")
        try:
            image_payload = json.loads(image_inspected.stdout)[0]
        except (IndexError, json.JSONDecodeError, TypeError) as error:
            raise RuntimeError(f"{task_name}: invalid docker image inspect response") from error
        repo_digests = image_payload.get("RepoDigests") or []
    if not repo_digests:
        raise RuntimeError(f"{task_name}: container image has no immutable repo digest")
    resolved = next((item for item in repo_digests if item in ALLOWLIST), None)
    if resolved is None:
        raise RuntimeError(f"{task_name}: container image is outside the diagnostics allowlist")

    name = str(payload.get("Name") or container_id).lstrip("/")
    record = {
        "id": container_id,
        "name": name,
        "image": image,
        "image_id": payload.get("Image"),
        "image_digest": resolved,
        "created": payload.get("Created"),
    }
    return record


def collect_container_logs(task_name: str, container_id: str, max_log_bytes: int) -> tuple[str, bool]:
    """Read and sanitize one container's bounded log stream."""

    logs_result = run_docker_logs(task_name, container_id, max_log_bytes)
    if logs_result.returncode != 0:
        raise RuntimeError(f"{task_name}: docker logs failed")
    return sanitize(logs_result.stdout), logs_result.truncated


CONTAINER_ID = re.compile(r"^[0-9a-fA-F]{12,64}$")


def normalized_path(path: Path) -> Path:
    """Normalize lexical dot segments without following symlinks."""

    return Path(os.path.abspath(path))


def reject_symlink_components(path: Path, stop_at: Path | None = None) -> None:
    """Reject a path or any existing parent component that is a symlink."""

    current = path.absolute()
    boundary = stop_at.absolute() if stop_at is not None else None
    while True:
        if boundary is not None and current == boundary:
            return
        if current.is_symlink():
            raise ValueError(f"path contains a symlink: {path}")
        if current.parent == current:
            return
        current = current.parent


def require_relative(path: Path, root: Path, description: str) -> None:
    try:
        path.relative_to(root)
    except ValueError as error:
        raise ValueError(f"{description} is outside repository: {path}") from error


def matching_report_files(root: Path, max_files: int, repo_root: Path) -> tuple[list[Path], bool]:
    reject_symlink_components(root, repo_root)
    if root.is_symlink():
        raise ValueError(f"report path is a symlink: {root}")
    if not root.is_dir():
        raise ValueError(f"report path does not exist: {root}")
    allowed_suffixes = {".xml", ".html", ".css", ".js"}
    files: list[Path] = []
    directories = [root]
    entries_seen = 0
    while directories:
        directory = directories.pop()
        entries: list[os.DirEntry[str]] = []
        try:
            with os.scandir(directory) as scanner:
                for entry in scanner:
                    entries_seen += 1
                    if entries_seen > MAX_REPORT_ENTRIES:
                        return sorted(files), True
                    entries.append(entry)
        except OSError as error:
            raise ValueError(f"report path cannot be read: {directory}") from error
        entries.sort(key=lambda entry: entry.name)
        for entry in entries:
            path = Path(entry.path)
            if entry.is_symlink():
                raise ValueError(f"report path contains a symlink: {path}")
            if entry.is_dir(follow_symlinks=False):
                directories.append(path)
            elif entry.is_file(follow_symlinks=False) and path.suffix.lower() in allowed_suffixes:
                files.append(path)
                if len(files) > max_files:
                    return sorted(files), True
    return sorted(files), False


def sanitize_reports(
    report_paths: Iterable[Path],
    destination: Path,
    repo_root: Path,
    max_files: int,
    max_total_bytes: int,
) -> tuple[list[dict[str, Any]], bool]:
    all_files: list[tuple[Path, Path]] = []
    truncated = False
    for root in report_paths:
        remaining_files = max_files - len(all_files)
        if remaining_files <= 0:
            truncated = True
            break
        files, files_truncated = matching_report_files(root, remaining_files + 1, repo_root)
        truncated = truncated or files_truncated
        if not files:
            raise ValueError(f"report path has no supported files: {root}")
        try:
            relative_root = root.relative_to(repo_root)
        except ValueError as error:
            raise ValueError(f"report path is outside repository: {root}") from error
        if len(files) > remaining_files:
            truncated = True
            files = files[:remaining_files]
        all_files.extend((path, destination / relative_root / path.relative_to(root)) for path in files)

    written: list[dict[str, Any]] = []
    used = 0
    for source, target in all_files:
        remaining = max_total_bytes - used
        if remaining <= 0:
            truncated = True
            break
        per_file_limit = min(MAX_REPORT_FILE_BYTES, remaining)
        with source.open("rb") as handle:
            raw_bytes = handle.read(per_file_limit + 1)
        raw = raw_bytes.decode("utf-8", errors="replace")
        raw_truncated = len(raw_bytes) > per_file_limit
        sanitized = sanitize(raw)
        data, was_truncated = bounded_bytes(sanitized, per_file_limit)
        was_truncated = was_truncated or raw_truncated
        if was_truncated:
            truncated = True
        reject_symlink_components(target.parent, repo_root)
        target.parent.mkdir(parents=True, exist_ok=True)
        reject_symlink_components(target.parent, repo_root)
        if target.is_symlink():
            raise ValueError(f"sanitized report target is a symlink: {target}")
        target.write_bytes(data)
        used += len(data)
        written.append(
            {
                "source": str(source.relative_to(repo_root)),
                "destination": str(target.relative_to(destination)),
                "bytes": len(data),
            }
        )
        if was_truncated:
            break
    if len(written) < len(all_files):
        truncated = True
    return written, truncated


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--task-name", required=True)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--workflow-file", required=True, type=Path)
    parser.add_argument("--max-total-bytes", type=int, default=DEFAULT_MAX_BYTES)
    parser.add_argument("--container-id", action="append", default=[])
    parser.add_argument("--sanitized-report-dir", type=Path)
    parser.add_argument("--report-path", action="append", default=[], type=Path)
    parser.add_argument("--max-report-files", type=int, default=DEFAULT_MAX_REPORT_FILES)
    parser.add_argument("--max-report-total-bytes", type=int, default=DEFAULT_MAX_BYTES)
    return parser.parse_args()


def write_manifest(path: Path, manifest: dict[str, Any], max_bytes: int, repo_root: Path | None = None) -> None:
    encoded = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
    if len(encoded) > max_bytes:
        # The manifest is deliberately small; if a caller supplies an unusable cap, fail closed.
        raise ValueError("manifest exceeds max-total-bytes")
    if repo_root is not None:
        reject_symlink_components(path.parent, repo_root)
    if path.is_symlink():
        raise ValueError(f"manifest target is a symlink: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(encoded)


def main() -> int:
    args = parse_args()
    if args.max_total_bytes <= 0 or args.max_report_files <= 0 or args.max_report_total_bytes <= 0:
        print(f"{args.task_name}: diagnostic limits must be positive", file=sys.stderr)
        return 1

    args.workflow_file = normalized_path(args.workflow_file)
    args.output_dir = normalized_path(args.output_dir)
    args.report_path = [normalized_path(path) for path in args.report_path]
    if args.sanitized_report_dir is not None:
        args.sanitized_report_dir = normalized_path(args.sanitized_report_dir)
    try:
        repo_root = args.workflow_file.parents[2]
        reject_symlink_components(args.workflow_file, repo_root)
        reject_symlink_components(args.output_dir, repo_root)
        require_relative(args.workflow_file, repo_root, "workflow file")
        require_relative(args.output_dir, repo_root, "output directory")
        if args.sanitized_report_dir is not None:
            reject_symlink_components(args.sanitized_report_dir, repo_root)
            require_relative(args.sanitized_report_dir, repo_root, "sanitized report directory")
        for report_path in args.report_path:
            reject_symlink_components(report_path, repo_root)
            require_relative(report_path, repo_root, "report path")
        args.output_dir.mkdir(parents=True, exist_ok=True)
    except (OSError, ValueError) as error:
        print(str(error) if str(error).startswith(args.task_name) else f"{args.task_name}: diagnostics failed", file=sys.stderr)
        return 1

    manifest: dict[str, Any] = {
        "task_name": args.task_name,
        "workflow_action_refs": [],
        "containers": [],
        "truncated": False,
        "sanitized_reports": [],
        "report_truncated": False,
    }
    try:
        manifest["workflow_action_refs"] = workflow_action_refs(args.workflow_file)
        container_ids = sorted(set(args.container_id))
        for container_id in container_ids:
            if not CONTAINER_ID.fullmatch(container_id):
                raise ValueError(f"{args.task_name}: invalid container id")
            manifest["containers"].append(inspect_container(args.task_name, container_id))

        # Reserve space for the manifest before writing bounded log files.
        provisional = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
        log_budget = max(0, args.max_total_bytes - len(provisional))
        log_used = 0
        for container_id in container_ids:
            logs, logs_truncated = collect_container_logs(
                args.task_name, container_id, args.max_total_bytes
            )
            manifest["truncated"] = manifest["truncated"] or logs_truncated
            remaining = log_budget - log_used
            data, was_truncated = bounded_bytes(logs, remaining)
            if was_truncated:
                manifest["truncated"] = True
            log_path = args.output_dir / f"{container_id}.log"
            if log_path.is_symlink():
                raise ValueError(f"{args.task_name}: log target is a symlink")
            log_path.write_bytes(data)
            log_used += len(data)
            if was_truncated:
                break

        if args.report_path:
            if args.sanitized_report_dir is None:
                raise ValueError("sanitized-report-dir is required with report-path")
            reports, report_truncated = sanitize_reports(
                args.report_path,
                args.sanitized_report_dir,
                repo_root,
                args.max_report_files,
                args.max_report_total_bytes,
            )
            manifest["sanitized_reports"] = reports
            manifest["report_truncated"] = report_truncated

        write_manifest(args.output_dir / "manifest.json", manifest, args.max_total_bytes, repo_root)
    except (OSError, RuntimeError, ValueError) as error:
        # Do not echo Docker output or report contents; the task name is the only diagnostic detail.
        print(str(error) if str(error).startswith(args.task_name) else f"{args.task_name}: diagnostics failed", file=sys.stderr)
        try:
            write_manifest(args.output_dir / "manifest.json", manifest, args.max_total_bytes, repo_root)
        except (OSError, ValueError):
            pass
        return 1
    return 1 if manifest["report_truncated"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
