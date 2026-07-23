#!/usr/bin/env python3
"""Generate checksum-gated cross-version Fory codec compatibility evidence."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import textwrap
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
REPOSITORY_URL = "https://repo.maven.apache.org/maven2"
TARGET_VERSION = "1.12.0"
KNOWN_GOOD_VERSION = "1.11.0"
OUTPUT_ROOT = REPOSITORY_ROOT / "docs/benchmarks/raw/issue-756-fory-followup/release"
BUILD_ROOT = REPOSITORY_ROOT / "infra/redisson/build/issue756-release"

PINNED_ARTIFACTS = (
    {
        "group": "io.github.bluetape4k",
        "artifact": "bluetape4k-io",
        "version": KNOWN_GOOD_VERSION,
        "sha256": "e5d41857bb7196c7fac8ecdfa773deb658f649ccbb78608064807fea1a823ea5",
    },
    {
        "group": "io.github.bluetape4k",
        "artifact": "bluetape4k-lettuce",
        "version": KNOWN_GOOD_VERSION,
        "sha256": "bd38da234b3dcd586d5a5458a95c4996c49585f945146fedb411a8c0810b962a",
    },
    {
        "group": "io.github.bluetape4k",
        "artifact": "bluetape4k-redisson",
        "version": KNOWN_GOOD_VERSION,
        "sha256": "a8018e61ac2c0d3e592efdcf694d2785c709269f22378d00c8f000dfffc628a1",
    },
)

FIXTURE_MATRIX = (
    {"id": "fory-old-write-new-read", "codec": "fory", "writer": "old", "reader": "new"},
    {"id": "fory-new-write-old-read", "codec": "fory", "writer": "new", "reader": "old"},
    {
        "id": "fast-fory-old-write-new-read",
        "codec": "fast-fory",
        "writer": "old",
        "reader": "new",
    },
    {
        "id": "fast-fory-new-write-old-read",
        "codec": "fast-fory",
        "writer": "new",
        "reader": "old",
    },
)

HARNESS_SOURCE = r"""
import io.bluetape4k.redis.redisson.codec.FastForyCodec;
import io.bluetape4k.redis.redisson.codec.ForyCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.redisson.client.codec.Codec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Issue756ForyCompatibilityFixture {
    private static List<Object> payload() {
        ArrayList<Object> value = new ArrayList<>();
        value.add(756L);
        value.add("lettuce-buffer-codec");
        value.add("A".repeat(96));
        return value;
    }

    private static Codec codec(String name) {
        return switch (name) {
            case "fory" -> new ForyCodec();
            case "fast-fory" -> new FastForyCodec();
            default -> throw new IllegalArgumentException("Unknown codec: " + name);
        };
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <write|read|roundtrip> <fory|fast-fory> <fixture>");
        }
        Codec codec = codec(args[1]);
        Path fixture = Path.of(args[2]);
        switch (args[0]) {
            case "write" -> {
                ByteBuf encoded = codec.getValueEncoder().encode(payload());
                try {
                    byte[] bytes = new byte[encoded.readableBytes()];
                    encoded.getBytes(encoded.readerIndex(), bytes);
                    Files.write(fixture, bytes);
                    System.out.println("WRITE_OK bytes=" + bytes.length);
                } finally {
                    encoded.release();
                }
            }
            case "read" -> {
                byte[] bytes = Files.readAllBytes(fixture);
                ByteBuf encoded = Unpooled.wrappedBuffer(bytes);
                Object decoded;
                try {
                    decoded = codec.getValueDecoder().decode(encoded, null);
                } finally {
                    encoded.release();
                }
                if (!payload().equals(decoded)) {
                    throw new IllegalStateException("Decoded payload differs: " + decoded);
                }
                System.out.println("READ_OK bytes=" + bytes.length);
            }
            case "roundtrip" -> {
                ByteBuf encoded = codec.getValueEncoder().encode(payload());
                Object decoded;
                try {
                    decoded = codec.getValueDecoder().decode(encoded, null);
                } finally {
                    encoded.release();
                }
                if (!payload().equals(decoded)) {
                    throw new IllegalStateException("Round-trip payload differs: " + decoded);
                }
                System.out.println("ROUNDTRIP_OK");
            }
            default -> throw new IllegalArgumentException("Unknown action: " + args[0]);
        }
    }
}
""".strip()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def artifact_url(repository_url: str, artifact: dict) -> str:
    group_path = artifact["group"].replace(".", "/")
    filename = f'{artifact["artifact"]}-{artifact["version"]}.jar'
    return (
        f'{repository_url.rstrip("/")}/{group_path}/{artifact["artifact"]}/'
        f'{artifact["version"]}/{filename}'
    )


def download_and_verify(repository_url: str, cache_root: Path) -> dict:
    records = []
    for artifact in PINNED_ARTIFACTS:
        url = artifact_url(repository_url, artifact)
        target = cache_root / f'{artifact["artifact"]}-{artifact["version"]}.jar'
        target.parent.mkdir(parents=True, exist_ok=True)
        try:
            with urllib.request.urlopen(url, timeout=30) as response:
                target.write_bytes(response.read())
        except Exception as failure:
            raise RuntimeError(f'DOWNLOAD_FAILED {artifact["artifact"]}: {failure}') from failure
        actual = sha256(target)
        if actual != artifact["sha256"]:
            target.unlink(missing_ok=True)
            raise RuntimeError(
                f'CHECKSUM_MISMATCH {artifact["artifact"]}: '
                f'expected={artifact["sha256"]} actual={actual}'
            )
        records.append(
            {
                **artifact,
                "coordinate": f'{artifact["group"]}:{artifact["artifact"]}:{artifact["version"]}',
                "repositoryUrl": repository_url,
                "downloadUrl": url,
                "file": target.name,
                "size": target.stat().st_size,
                "verified": True,
            }
        )
    manifest = {"repositoryUrl": repository_url, "artifacts": records}
    validate_artifact_manifest(manifest)
    return manifest


def validate_artifact_manifest(manifest: dict) -> None:
    if manifest.get("repositoryUrl") is None:
        raise ValueError("repository URL is missing")
    expected = {
        f'{artifact["group"]}:{artifact["artifact"]}:{artifact["version"]}'
        for artifact in PINNED_ARTIFACTS
    }
    records = manifest.get("artifacts", [])
    actual = {record.get("coordinate") for record in records}
    if actual != expected:
        raise ValueError(f"artifact coordinates differ: expected={expected}, actual={actual}")
    for record in records:
        if not record.get("verified"):
            raise ValueError(f'{record.get("coordinate")} is not checksum verified')
        expected_sha = next(
            artifact["sha256"]
            for artifact in PINNED_ARTIFACTS
            if artifact["artifact"] == record["artifact"]
        )
        if record.get("sha256") != expected_sha:
            raise ValueError(f'{record.get("coordinate")} has an unpinned checksum')


def gradle_runtime_classpath() -> list[Path]:
    init_script = textwrap.dedent(
        """
        gradle.beforeProject { candidate ->
            if (candidate.path == ':bluetape4k-redisson') {
                candidate.tasks.register('issue756PrintTestRuntimeClasspath') {
                    dependsOn(candidate.tasks.named('testClasses'))
                    doLast {
                        candidate.sourceSets.test.runtimeClasspath.files
                            .collect { it.canonicalFile }
                            .sort { a, b -> a.absolutePath <=> b.absolutePath }
                            .each { println('ISSUE756_RUNTIME_CLASSPATH=' + it.absolutePath) }
                    }
                }
            }
        }
        """
    ).strip()
    descriptor, script_name = tempfile.mkstemp(
        prefix="issue756-compatibility-", suffix=".init.gradle", dir=BUILD_ROOT
    )
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            stream.write(init_script)
            stream.write("\n")
        completed = subprocess.run(
            [
                "./gradlew",
                ":bluetape4k-redisson:issue756PrintTestRuntimeClasspath",
                "--init-script",
                script_name,
                "--no-daemon",
                "--no-configuration-cache",
            ],
            cwd=REPOSITORY_ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
    finally:
        Path(script_name).unlink(missing_ok=True)
    if completed.returncode != 0:
        raise RuntimeError(
            "CLASSPATH_BUILD_FAILED\n" + completed.stdout[-4000:] + completed.stderr[-4000:]
        )
    classpath = [
        Path(line.split("=", 1)[1]).resolve()
        for line in completed.stdout.splitlines()
        if line.startswith("ISSUE756_RUNTIME_CLASSPATH=")
    ]
    if not classpath:
        raise RuntimeError("CLASSPATH_BUILD_FAILED no runtime entries were reported")
    return classpath


def is_replaced_project_output(path: Path) -> bool:
    normalized = path.as_posix()
    replacements = (
        "/io/io/build/",
        "/infra/lettuce/build/",
        "/infra/redisson/build/",
    )
    return normalized.startswith(REPOSITORY_ROOT.as_posix()) and any(
        marker in normalized for marker in replacements
    )


def sanitize_path(path: Path) -> str:
    resolved = path.resolve()
    try:
        return resolved.relative_to(REPOSITORY_ROOT).as_posix()
    except ValueError:
        gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")).resolve()
        try:
            return "$GRADLE_USER_HOME/" + resolved.relative_to(gradle_home).as_posix()
        except ValueError:
            return "$EXTERNAL/" + resolved.name


def classpath_record(path: Path) -> dict:
    record = {
        "path": sanitize_path(path),
        "kind": "directory" if path.is_dir() else "jar" if path.is_file() else "missing",
    }
    if path.is_file():
        record["sha256"] = sha256(path)
        record["size"] = path.stat().st_size
    return record


def build_classpaths(artifact_manifest: dict, cache_root: Path) -> tuple[list[Path], list[Path], dict]:
    current = gradle_runtime_classpath()
    old_artifacts = [
        cache_root / f'{record["artifact"]}-{record["version"]}.jar'
        for record in artifact_manifest["artifacts"]
    ]
    old = old_artifacts + [path for path in current if not is_replaced_project_output(path)]
    manifest = {
        "current": [classpath_record(path) for path in current],
        "knownGood": [classpath_record(path) for path in old],
        "replacementPolicy": {
            "removedCurrentOutputs": [
                "io/io/build",
                "infra/lettuce/build",
                "infra/redisson/build",
            ],
            "prependedPinnedCoordinates": [
                record["coordinate"] for record in artifact_manifest["artifacts"]
            ],
        },
    }
    return current, old, manifest


def run_checked(command: list[str], label: str) -> str:
    completed = subprocess.run(
        command,
        cwd=REPOSITORY_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"{label}_FAILED exit={completed.returncode}\n"
            f"{completed.stdout[-2000:]}\n{completed.stderr[-2000:]}"
        )
    return completed.stdout.strip()


def compile_harness(current_classpath: list[Path]) -> Path:
    source_root = BUILD_ROOT / "harness-src"
    classes_root = BUILD_ROOT / "harness-classes"
    shutil.rmtree(source_root, ignore_errors=True)
    shutil.rmtree(classes_root, ignore_errors=True)
    source_root.mkdir(parents=True)
    classes_root.mkdir(parents=True)
    source = source_root / "Issue756ForyCompatibilityFixture.java"
    source.write_text(HARNESS_SOURCE + "\n", encoding="utf-8")
    run_checked(
        [
            shutil.which("javac") or "javac",
            "-encoding",
            "UTF-8",
            "-classpath",
            os.pathsep.join(str(path) for path in current_classpath),
            "-d",
            str(classes_root),
            str(source),
        ],
        "HARNESS_COMPILE",
    )
    return classes_root


def java_fixture(
    harness: Path,
    classpath: list[Path],
    action: str,
    codec: str,
    fixture: Path,
) -> str:
    return run_checked(
        [
            shutil.which("java") or "java",
            "-classpath",
            os.pathsep.join([str(harness), *(str(path) for path in classpath)]),
            "Issue756ForyCompatibilityFixture",
            action,
            codec,
            str(fixture),
        ],
        f"{action.upper()}_{codec.upper().replace('-', '_')}",
    )


def git_head() -> str:
    return run_checked(["git", "rev-parse", "HEAD"], "GIT_HEAD")


def execute_matrix(
    output_root: Path,
    current: list[Path],
    old: list[Path],
    harness: Path,
) -> list[dict]:
    classpaths = {"new": current, "old": old}
    results = []
    fixtures_root = output_root / "fixtures"
    fixtures_root.mkdir(parents=True, exist_ok=True)
    for cell in FIXTURE_MATRIX:
        fixture = fixtures_root / f'{cell["id"]}.bin'
        write_output = java_fixture(
            harness,
            classpaths[cell["writer"]],
            "write",
            cell["codec"],
            fixture,
        )
        read_output = java_fixture(
            harness,
            classpaths[cell["reader"]],
            "read",
            cell["codec"],
            fixture,
        )
        results.append(
            {
                **cell,
                "status": "passed",
                "fixture": fixture.relative_to(output_root).as_posix(),
                "fixtureSha256": sha256(fixture),
                "fixtureSize": fixture.stat().st_size,
                "writeEvidence": write_output.splitlines()[-1],
                "readEvidence": read_output.splitlines()[-1],
            }
        )
    return results


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository-url", default=REPOSITORY_URL)
    parser.add_argument("--output", type=Path, default=OUTPUT_ROOT)
    parser.add_argument("--verify-only", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    output_root = args.output.resolve()
    cache_root = BUILD_ROOT / "known-good"
    BUILD_ROOT.mkdir(parents=True, exist_ok=True)
    try:
        artifact_manifest = download_and_verify(args.repository_url, cache_root)
        if args.verify_only:
            print("CHECKSUMS_OK")
            return 0
        current, old, classpath_manifest = build_classpaths(artifact_manifest, cache_root)
        harness = compile_harness(current)
        results = execute_matrix(output_root, current, old, harness)
        evidence = {
            "schemaVersion": 1,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "gitHead": git_head(),
            "targetVersion": TARGET_VERSION,
            "knownGoodVersion": KNOWN_GOOD_VERSION,
            "scope": "Fory/FastFory raw Redisson codec cross-version decode",
            "status": "passed",
            "fixtures": results,
            "limitations": [
                "This fixture proves codec-level wire compatibility; it does not publish artifacts.",
                "Compression wrappers are outside the issue #756 follow-up scope.",
            ],
        }
        write_json(output_root / "artifact-manifest.json", artifact_manifest)
        write_json(output_root / "classpath-manifest.json", classpath_manifest)
        write_json(output_root / "compatibility-results.json", evidence)
        print(f"COMPATIBILITY_OK fixtures={len(results)} output={output_root}")
        return 0
    except (RuntimeError, ValueError) as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
