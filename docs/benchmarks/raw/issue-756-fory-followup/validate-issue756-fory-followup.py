#!/usr/bin/env python3
"""Aggregate validator for issue #756 Fory/FastFory canonical evidence."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

SENSITIVE_KEYS = {"token", "password", "secret", "credential", "authorization"}
ALLOWED_CHANGED_PREFIXES = (
    "infra/lettuce/",
    "infra/redisson/",
    "io/io/",
    "docs/benchmarks/",
    "docs/images/readme-charts/",
    "docs/superpowers/",
)


class ValidationError(ValueError):
    pass


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _reject_sensitive(value: object, path: str = "") -> None:
    if isinstance(value, dict):
        for key, nested in value.items():
            if key.lower() in SENSITIVE_KEYS:
                raise ValidationError(f"sensitive metadata key: {path}{key}")
            _reject_sensitive(nested, f"{path}{key}.")
    elif isinstance(value, list):
        for nested in value:
            _reject_sensitive(nested, path)


def validate_manifest(manifest: dict, root: Path) -> dict:
    if manifest.get("schema_version") != 1:
        raise ValidationError("manifest schema mismatch")
    encode_disposition = manifest.get("encodeDisposition")
    if encode_disposition not in ("implemented", "rejected"):
        raise ValidationError("terminal encodeDisposition required")
    expected_total = 24 if encode_disposition == "implemented" else 20
    if manifest.get("canonical_method_count") != expected_total:
        raise ValidationError("canonical method count conflicts with encodeDisposition")
    source = manifest.get("source", {})
    if (
        not source.get("clean")
        or not source.get("ancestry_verified")
        or not source.get("append_only_raw_verified")
    ):
        raise ValidationError("clean tree, ancestry, and append-only raw proof required")
    changed_paths = source.get("changed_paths", [])
    if any(not path.startswith(ALLOWED_CHANGED_PREFIXES) for path in changed_paths):
        raise ValidationError("changed path outside allowlist")
    _reject_sensitive(manifest)

    modules = manifest.get("modules", {})
    if set(modules) != {"lettuce", "redisson"}:
        raise ValidationError("exact lettuce/redisson module set required")
    observed_counts = {}
    for module, expected_count in (
        ("lettuce", 8),
        ("redisson", 16 if encode_disposition == "implemented" else 12),
    ):
        entry = modules[module]
        jar_sha = entry.get("jar_sha256")
        if not isinstance(jar_sha, str) or len(jar_sha) != 64:
            raise ValidationError("pinned module jar SHA required")
        runs = entry.get("runs", {})
        if set(runs) != {"canonical-a", "canonical-b"}:
            raise ValidationError("exact A/B runs required")
        run_hashes = set()
        for run_name, relative in runs.items():
            leaf = root / relative
            required_files = {
                "jmh.json",
                "argv.json",
                "environment.json",
                "metadata.json",
                "preflight.json",
                "summary.csv",
                "comparison.json",
                "validation.json",
            }
            if any(not (leaf / name).is_file() for name in required_files):
                raise ValidationError("canonical leaf is incomplete")
            validation_path = leaf / "validation.json"
            metadata_path = leaf / "metadata.json"
            validation = json.loads(validation_path.read_text())
            metadata = json.loads(metadata_path.read_text())
            if validation.get("status") != "passed" or validation.get("method_count") != expected_count:
                raise ValidationError("leaf validation mismatch")
            if metadata.get("jar_sha256") != jar_sha or metadata.get("run") != run_name:
                raise ValidationError("leaf jar/run binding mismatch")
            if module == "redisson" and validation.get("encodeDisposition") != encode_disposition:
                raise ValidationError("Redisson encode disposition drift")
            declared_hashes = validation.get("hashes", {})
            hashable_files = required_files - {"validation.json"}
            if set(declared_hashes) != hashable_files:
                raise ValidationError("leaf hash cardinality mismatch")
            if any(declared_hashes[name] != sha256_file(leaf / name) for name in hashable_files):
                raise ValidationError("leaf hash drift")
            for cell in validation.get("comparisons", []):
                if cell.get("source") == "composite" and (
                    cell.get("promotable") is not False or cell.get("disposition") != "fallback"
                ):
                    raise ValidationError("composite fallback is non-promotable")
            run_hashes.add(sha256_file(validation_path))
        if len(run_hashes) != 2:
            raise ValidationError("A/B validation artifacts must be distinct")
        observed_counts[module] = expected_count
    return {
        "schema_version": 1,
        "status": "passed",
        "encodeDisposition": encode_disposition,
        "canonical_method_count": sum(observed_counts.values()),
        "module_method_counts": observed_counts,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent)
    args = parser.parse_args()
    manifest_path = args.root / "manifest.json"
    result = validate_manifest(json.loads(manifest_path.read_text()), args.root)
    (args.root / "validation.json").write_text(json.dumps(result, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
