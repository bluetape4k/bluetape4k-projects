#!/usr/bin/env python3
"""Run the non-publishing issue #756 rollback smoke and record its evidence."""

from __future__ import annotations

import importlib.util
import json
import os
import socket
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse


SCRIPT_ROOT = Path(__file__).resolve().parent
COMPATIBILITY_SCRIPT = SCRIPT_ROOT / "run-issue756-fory-compatibility.py"


def load_compatibility_runner():
    spec = importlib.util.spec_from_file_location(
        "issue756_fory_compatibility",
        COMPATIBILITY_SCRIPT,
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def redis_command(stream, *arguments: bytes) -> bytes | None:
    request = bytearray(f"*{len(arguments)}\r\n".encode())
    for argument in arguments:
        request.extend(f"${len(argument)}\r\n".encode())
        request.extend(argument)
        request.extend(b"\r\n")
    stream.write(request)
    stream.flush()
    prefix = stream.read(1)
    if prefix == b"+":
        return stream.readline().rstrip(b"\r\n")
    if prefix == b":":
        return stream.readline().rstrip(b"\r\n")
    if prefix == b"$":
        length = int(stream.readline().rstrip(b"\r\n"))
        if length == -1:
            return None
        value = stream.read(length)
        if stream.read(2) != b"\r\n":
            raise RuntimeError("REDIS_PROTOCOL_FAILED incomplete bulk response")
        return value
    if prefix == b"-":
        raise RuntimeError(f"REDIS_COMMAND_FAILED {stream.readline().decode().strip()}")
    raise RuntimeError(f"REDIS_PROTOCOL_FAILED unknown response prefix={prefix!r}")


def redis_endpoint() -> tuple[str, int]:
    configured = os.environ.get("ISSUE756_REDIS_URL", "redis://127.0.0.1:6379")
    parsed = urlparse(configured)
    if parsed.scheme != "redis" or parsed.username or parsed.password:
        raise ValueError("ISSUE756_REDIS_URL must be an unauthenticated redis://host:port URL")
    return parsed.hostname or "127.0.0.1", parsed.port or 6379


def live_redis_available(host: str, port: int) -> bool:
    try:
        with socket.create_connection((host, port), timeout=0.5) as connection:
            connection.settimeout(1)
            with connection.makefile("rwb") as stream:
                return redis_command(stream, b"PING") == b"PONG"
    except (OSError, RuntimeError):
        return False


def run_live_redis_smoke(runner, output_root: Path, old_classpath, harness, host, port):
    checks = []
    for codec in ("fory", "fast-fory"):
        fixture = output_root / "fixtures" / f"rollback-{codec}.bin"
        fixture.parent.mkdir(parents=True, exist_ok=True)
        write_evidence = runner.java_fixture(harness, old_classpath, "write", codec, fixture)
        payload = fixture.read_bytes()
        key = f"issue756:rollback:{codec}:{uuid.uuid4().hex}".encode()
        with socket.create_connection((host, port), timeout=2) as connection:
            connection.settimeout(2)
            with connection.makefile("rwb") as stream:
                if redis_command(stream, b"SET", key, payload) != b"OK":
                    raise RuntimeError(f"REDIS_SET_FAILED codec={codec}")
                try:
                    fetched = redis_command(stream, b"GET", key)
                finally:
                    redis_command(stream, b"DEL", key)
        if fetched != payload:
            raise RuntimeError(f"REDIS_PAYLOAD_MISMATCH codec={codec}")
        fixture.write_bytes(fetched)
        read_evidence = runner.java_fixture(harness, old_classpath, "read", codec, fixture)
        checks.append(
            {
                "codec": codec,
                "status": "passed",
                "fixture": fixture.relative_to(output_root).as_posix(),
                "fixtureSha256": runner.sha256(fixture),
                "writeEvidence": write_evidence.splitlines()[-1],
                "readEvidence": read_evidence.splitlines()[-1],
                "redisSetGetDelete": "passed",
            }
        )
    return checks


def run_codec_level_smoke(runner, old_classpath, harness):
    checks = []
    for codec in ("fory", "fast-fory"):
        evidence = runner.java_fixture(
            harness,
            old_classpath,
            "roundtrip",
            codec,
            Path("unused"),
        )
        checks.append(
            {
                "codec": codec,
                "status": "passed",
                "roundTripEvidence": evidence.splitlines()[-1],
            }
        )
    return checks


def main() -> int:
    runner = load_compatibility_runner()
    output_root = runner.OUTPUT_ROOT
    cache_root = runner.BUILD_ROOT / "known-good"
    try:
        artifact_manifest = runner.download_and_verify(runner.REPOSITORY_URL, cache_root)
        current, old, _ = runner.build_classpaths(artifact_manifest, cache_root)
        harness = runner.compile_harness(current)
        host, port = redis_endpoint()
        if live_redis_available(host, port):
            mode = "redis"
            checks = run_live_redis_smoke(
                runner,
                output_root,
                old,
                harness,
                host,
                port,
            )
            limitation = None
        else:
            mode = "codec-level"
            checks = run_codec_level_smoke(runner, old, harness)
            limitation = (
                f"Redis was unavailable at redis://{host}:{port}; this deterministic fallback "
                "proves known-good Fory/FastFory codec round trips but not networked Redis SET/GET."
            )
        evidence = {
            "schemaVersion": 1,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "gitHead": runner.git_head(),
            "status": "passed",
            "mode": mode,
            "publishingPerformed": False,
            "knownGoodVersion": runner.KNOWN_GOOD_VERSION,
            "repositoryUrl": runner.REPOSITORY_URL,
            "pinnedCoordinates": [
                record["coordinate"] for record in artifact_manifest["artifacts"]
            ],
            "checks": checks,
            "limitation": limitation,
        }
        runner.write_json(output_root / "rollback-smoke.json", evidence)
        release_files = sorted(
            path
            for path in output_root.rglob("*")
            if path.is_file() and path.name != "release-manifest.json"
        )
        runner.write_json(
            output_root / "release-manifest.json",
            {
                "schemaVersion": 1,
                "files": [
                    {
                        "path": path.relative_to(output_root).as_posix(),
                        "sha256": runner.sha256(path),
                        "size": path.stat().st_size,
                    }
                    for path in release_files
                ],
            },
        )
        print(f"ROLLBACK_SMOKE_OK mode={mode} checks={len(checks)} output={output_root}")
        return 0
    except (OSError, RuntimeError, ValueError) as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
