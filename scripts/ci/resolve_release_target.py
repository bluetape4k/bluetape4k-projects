from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

VERSION_PATTERN = re.compile(
    r"^[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"
)
SHA_PATTERN = re.compile(r"^[0-9a-fA-F]{40,64}$")


class ReleaseTargetError(RuntimeError):
    pass


def require_version(version: str) -> str:
    if not VERSION_PATTERN.fullmatch(version):
        raise ReleaseTargetError(f"Invalid release version: {version}")
    return version


def require_sha(value: str, *, field: str) -> str:
    if not SHA_PATTERN.fullmatch(value):
        raise ReleaseTargetError(f"Invalid {field}: {value or '<empty>'}")
    return value.lower()


def resolve_remote_tag(repository: Path, version: str) -> tuple[str, str]:
    tag = f"refs/tags/{require_version(version)}"
    peeled_tag = f"{tag}^{{}}"
    result = subprocess.run(
        ["git", "ls-remote", "--exit-code", "origin", tag, peeled_tag],
        cwd=repository,
        check=False,
        text=True,
        capture_output=True,
    )
    if result.returncode == 2:
        raise ReleaseTargetError(f"Release tag {tag} does not exist on origin")
    if result.returncode != 0:
        detail = result.stderr.strip() or "unknown git ls-remote failure"
        raise ReleaseTargetError(f"Could not read {tag} from origin: {detail}")

    refs = {}
    for line in result.stdout.splitlines():
        sha, ref = line.split("\t", 1)
        refs[ref] = require_sha(sha, field=f"SHA for {ref}")

    target_sha = refs.get(peeled_tag) or refs.get(tag)
    if target_sha is None:
        raise ReleaseTargetError(f"Release tag {tag} did not resolve to a commit")
    return tag, target_sha


def resolve_command(args: argparse.Namespace) -> None:
    tag, target_sha = resolve_remote_tag(args.repository, args.version)
    if args.event_name == "push":
        event_sha = require_sha(args.event_sha, field="push event SHA")
        if target_sha != event_sha:
            raise ReleaseTargetError(
                f"Release tag {tag} target {target_sha} does not match push event SHA {event_sha}"
            )
    elif args.event_name != "workflow_dispatch":
        raise ReleaseTargetError(f"Unsupported release event: {args.event_name}")

    print(f"version={args.version}")
    print(f"tag={tag}")
    print(f"target_sha={target_sha}")


def verify_command(args: argparse.Namespace) -> None:
    tag, target_sha = resolve_remote_tag(args.repository, args.version)
    expected_sha = require_sha(args.expected_sha, field="expected release SHA")
    if target_sha != expected_sha:
        raise ReleaseTargetError(
            f"Release tag {tag} moved from expected commit {expected_sha} to {target_sha}"
        )
    print(f"Verified {tag} -> {target_sha}")


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(
        description="Resolve and verify an immutable stable release target"
    )
    commands = root.add_subparsers(dest="command", required=True)

    resolve = commands.add_parser("resolve")
    resolve.add_argument("--repository", type=Path, default=Path.cwd())
    resolve.add_argument("--version", required=True)
    resolve.add_argument("--event-name", required=True)
    resolve.add_argument("--event-sha", default="")
    resolve.set_defaults(handler=resolve_command)

    verify = commands.add_parser("verify")
    verify.add_argument("--repository", type=Path, default=Path.cwd())
    verify.add_argument("--version", required=True)
    verify.add_argument("--expected-sha", required=True)
    verify.set_defaults(handler=verify_command)
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        args.handler(args)
    except ReleaseTargetError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
