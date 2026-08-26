#!/usr/bin/env python3
"""Collect bounded, redacted Testcontainers diagnostics and test reports."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any, Iterable


DEFAULT_MAX_BYTES = 2_000_000
DEFAULT_MAX_REPORT_FILES = 200
ALLOWLIST = {
    "confluentinc/cp-kafka@sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130",
    "redis@sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a",
}
SHA40 = re.compile(r"^[0-9a-fA-F]{40}$")
USES = re.compile(r"^\s*(?:-\s*)?uses:\s*([^\s#]+)", re.MULTILINE)
URI = re.compile(r"\b(?:https?|amqps?|redis|kafka|postgres(?:ql)?|file)://[^\s<>\"']+")
AUTHORIZATION_LINE = re.compile(r"(?im)^(\s*[\"']?authorization[\"']?\s*[:=]\s*).*$")
SECRET_KEY_QUOTED = re.compile(
    r"(?i)([\"']?\b(?:authorization|token|password|secret|api[_-]?key)\b[\"']?\s*[:=]\s*)(\"|')([^\"']*)(\2)"
)
SECRET_KEY_UNQUOTED = re.compile(
    r"(?i)([\"']?\b(?:authorization|token|password|secret|api[_-]?key)\b[\"']?\s*[:=]\s*)(?!\[REDACTED\])([^\"'\s,;&}\]\r\n]+)"
)
INLINE_SENSITIVE = re.compile(
    r'''(?is)([\"']?(?:payload|message|body|value)[\"']?\s*[:=]\s*)(\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'|\{.*?\}|\[.*?\]|[^\s,;&}\]\r\n]+)'''
)
XML_SENSITIVE = re.compile(
    r"(?is)(<\s*(?:payload|message|body|value)\b[^>]*>).*?(</\s*(?:payload|message|body|value)\s*>)"
)
UPPER_ENV_LINE = re.compile(r"(?m)^\s*[A-Z][A-Z0-9_]*\s*[:=].*$")
LOWER_SENSITIVE_ENV_LINE = re.compile(
    r"(?im)^\s*[a-z][a-z0-9_]*(?:secret|token|password|api[_-]?key)[a-z0-9_]*\s*[:=].*$"
)
SENSITIVE_LINE = re.compile(r"(?im)^\s*(?:payload|message|body|value)\s*[:=].*$")
EXCEPTION_MESSAGE = re.compile(r"(?im)^([^\r\n]*(?:Exception|Error))\s*:\s*[^\r\n]*$")


def sanitize(value: str) -> str:
    """Redact credentials, endpoints, payloads, and exception messages."""

    value = AUTHORIZATION_LINE.sub(r"\1[REDACTED]", value)
    value = SECRET_KEY_QUOTED.sub(r"\1\2[REDACTED]\4", value)
    value = SECRET_KEY_UNQUOTED.sub(r"\1[REDACTED]", value)
    value = URI.sub("[REDACTED]", value)
    value = INLINE_SENSITIVE.sub(r"\1[REDACTED]", value)
    value = XML_SENSITIVE.sub(r"\1[REDACTED]\2", value)
    value = UPPER_ENV_LINE.sub("[REDACTED]", value)
    value = LOWER_SENSITIVE_ENV_LINE.sub("[REDACTED]", value)
    value = SENSITIVE_LINE.sub("[REDACTED]", value)
    value = EXCEPTION_MESSAGE.sub(r"\1: [REDACTED]", value)
    return value


def bounded_bytes(value: str, limit: int) -> tuple[bytes, bool]:
    encoded = value.encode("utf-8", errors="replace")
    if len(encoded) <= limit:
        return encoded, False
    return encoded[:limit], True


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


def workflow_action_refs(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    refs: list[str] = []
    for reference in USES.findall(text):
        if "@" not in reference:
            raise ValueError(f"{path}: mutable workflow action reference")
        action, ref = reference.rsplit("@", 1)
        if not SHA40.fullmatch(ref):
            raise ValueError(f"{path}: workflow action is not pinned to a commit SHA")
        normalized = f"{action}@{ref.lower()}"
        if normalized not in refs:
            refs.append(normalized)
    return refs


def inspect_container(task_name: str, container_id: str) -> tuple[dict[str, Any], str | None]:
    inspected = run_docker(task_name, ["inspect", container_id])
    if inspected.returncode != 0:
        raise RuntimeError(f"{task_name}: docker inspect failed")
    try:
        payload = json.loads(inspected.stdout)[0]
    except (IndexError, json.JSONDecodeError, TypeError) as error:
        raise RuntimeError(f"{task_name}: invalid docker inspect response") from error

    config = payload.get("Config") or {}
    image = str(config.get("Image") or payload.get("Config", {}).get("Image") or "")
    repo_digests = payload.get("RepoDigests") or []
    if not repo_digests:
        image_reference = image or str(payload.get("Image") or "")
        image_inspected = run_docker(task_name, ["image", "inspect", image_reference])
        if image_inspected.returncode != 0:
            raise RuntimeError(f"{task_name}: docker image inspect failed")
        try:
            image_payload = json.loads(image_inspected.stdout)[0]
            repo_digests = image_payload.get("RepoDigests") or []
        except (IndexError, json.JSONDecodeError, TypeError) as error:
            raise RuntimeError(f"{task_name}: invalid docker image inspect response") from error
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
    logs_result = run_docker(task_name, ["logs", "--tail", "200", container_id])
    if logs_result.returncode != 0:
        return record, None
    return record, sanitize(logs_result.stdout + logs_result.stderr)


def matching_report_files(root: Path) -> list[Path]:
    if root.is_symlink():
        raise ValueError(f"report path is a symlink: {root}")
    if not root.is_dir():
        raise ValueError(f"report path does not exist: {root}")
    allowed_suffixes = {".xml", ".html", ".css", ".js"}
    files: list[Path] = []
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"report path contains a symlink: {path}")
        if path.is_file() and path.suffix.lower() in allowed_suffixes:
            files.append(path)
    return sorted(files)


def sanitize_reports(
    report_paths: Iterable[Path],
    destination: Path,
    repo_root: Path,
    max_files: int,
    max_total_bytes: int,
) -> tuple[list[dict[str, Any]], bool]:
    all_files: list[tuple[Path, Path]] = []
    for root in report_paths:
        files = matching_report_files(root)
        if not files:
            raise ValueError(f"report path has no supported files: {root}")
        try:
            relative_root = root.relative_to(repo_root)
        except ValueError as error:
            raise ValueError(f"report path is outside repository: {root}") from error
        all_files.extend((path, destination / relative_root / path.relative_to(root)) for path in files)

    truncated = len(all_files) > max_files
    selected = all_files[:max_files]
    written: list[dict[str, Any]] = []
    used = 0
    for source, target in selected:
        raw = source.read_text(encoding="utf-8", errors="replace")
        sanitized = sanitize(raw)
        remaining = max_total_bytes - used
        if remaining <= 0:
            truncated = True
            break
        data, was_truncated = bounded_bytes(sanitized, min(2_000_000, remaining))
        if was_truncated:
            truncated = True
        target.parent.mkdir(parents=True, exist_ok=True)
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


def write_manifest(path: Path, manifest: dict[str, Any], max_bytes: int) -> None:
    encoded = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
    if len(encoded) > max_bytes:
        # The manifest is deliberately small; if a caller supplies an unusable cap, fail closed.
        raise ValueError("manifest exceeds max-total-bytes")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(encoded)


def main() -> int:
    args = parse_args()
    if args.max_total_bytes <= 0 or args.max_report_files <= 0 or args.max_report_total_bytes <= 0:
        print(f"{args.task_name}: diagnostic limits must be positive", file=sys.stderr)
        return 1

    args.output_dir.mkdir(parents=True, exist_ok=True)
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
        container_logs: list[tuple[str, str]] = []
        for container_id in sorted(set(args.container_id)):
            record, logs = inspect_container(args.task_name, container_id)
            manifest["containers"].append(record)
            if logs:
                container_logs.append((container_id, logs))

        # Reserve space for the manifest before writing bounded log files.
        provisional = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
        log_budget = max(0, args.max_total_bytes - len(provisional))
        log_used = 0
        for container_id, logs in container_logs:
            remaining = log_budget - log_used
            data, was_truncated = bounded_bytes(logs, remaining)
            if was_truncated:
                manifest["truncated"] = True
            log_path = args.output_dir / f"{container_id}.log"
            log_path.write_bytes(data)
            log_used += len(data)
            if was_truncated:
                break

        if args.report_path:
            if args.sanitized_report_dir is None:
                raise ValueError("sanitized-report-dir is required with report-path")
            repo_root = args.workflow_file.resolve().parents[2]
            reports, report_truncated = sanitize_reports(
                [path.resolve() for path in args.report_path],
                args.sanitized_report_dir.resolve(),
                repo_root,
                args.max_report_files,
                args.max_report_total_bytes,
            )
            manifest["sanitized_reports"] = reports
            manifest["report_truncated"] = report_truncated

        write_manifest(args.output_dir / "manifest.json", manifest, args.max_total_bytes)
    except (OSError, RuntimeError, ValueError) as error:
        # Do not echo Docker output or report contents; the task name is the only diagnostic detail.
        print(str(error) if str(error).startswith(args.task_name) else f"{args.task_name}: diagnostics failed", file=sys.stderr)
        try:
            write_manifest(args.output_dir / "manifest.json", manifest, args.max_total_bytes)
        except (OSError, ValueError):
            pass
        return 1
    return 1 if manifest["report_truncated"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
