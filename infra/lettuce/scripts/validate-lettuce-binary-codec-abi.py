#!/usr/bin/env python3
import argparse
import hashlib
import importlib.util
import json
import re
import stat
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, FrozenSet, Optional, Tuple


ACCESS_MODIFIERS = frozenset({"public", "protected", "private"})
DECLARATION_MODIFIERS = frozenset(
    {
        "public",
        "protected",
        "private",
        "static",
        "final",
        "abstract",
        "synchronized",
        "native",
        "strictfp",
        "transient",
        "volatile",
        "default",
    }
)
CLASS_NAME = "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec"
MANIFEST_SCHEMA = "issue757-lettuce-abi-v1"
BASELINE_REVISION = "4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88"
BASELINE_TREE = "086f83baa7eec0cd68e68fff132542ef6db0f200"
HELPER_API_VERSION = 1
HELPER_PUBLIC_API = frozenset(
    {
        "begin_attempt",
        "materialize_worktree",
        "materialize_build_root",
        "seal_artifact",
        "cleanup_selected",
    }
)
REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
EXPECTED_HELPER = (
    REPOSITORY_ROOT
    / "benchmark/protobuf-codec-benchmark/scripts/issue757_detached_roots.py"
)
CONSTRUCTOR_DESCRIPTOR = "(Lio/bluetape4k/io/serializer/BinarySerializer;)V"
TARGET_NAME = "encodeValue"
TARGET_DESCRIPTOR = "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V"
CONSTRUCTOR_KEY = ("constructor", CLASS_NAME, CONSTRUCTOR_DESCRIPTOR)
TARGET_KEY = ("method", TARGET_NAME, TARGET_DESCRIPTOR)
COMPILER_BRIDGE_KEYS = frozenset(
    {
        ("method", "encodeKey", "(Ljava/lang/Object;)Ljava/nio/ByteBuffer;"),
        (
            "method",
            "encodeKey",
            "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V",
        ),
        ("method", "decodeKey", "(Ljava/nio/ByteBuffer;)Ljava/lang/Object;"),
    }
)


class AbiParseError(ValueError):
    pass


class ManifestError(ValueError):
    pass


@dataclass(frozen=True)
class Member:
    kind: str
    name: str
    descriptor: str
    access: str
    final: bool
    modifiers: FrozenSet[str]

    @property
    def key(self) -> Tuple[str, str, str]:
        return self.kind, self.name, self.descriptor

    @property
    def label(self) -> str:
        return f"{self.kind} {self.name} {self.descriptor}"


@dataclass(frozen=True)
class AbiClass:
    kind: str
    name: str
    access: str
    final: bool
    modifiers: FrozenSet[str]
    hierarchy: str
    members: Dict[Tuple[str, str, str], Member]

    def effective_final(self, member: Member) -> bool:
        overrideable = (
            member.kind == "method"
            and member.access != "private"
            and "static" not in member.modifiers
            and member.key not in COMPILER_BRIDGE_KEYS
        )
        return member.final or (self.final and overrideable)


def _access(modifiers: FrozenSet[str]) -> str:
    found = sorted(modifiers & ACCESS_MODIFIERS)
    if len(found) > 1:
        raise AbiParseError(f"conflicting access modifiers: {', '.join(found)}")
    return found[0] if found else "package"


def _split_modifiers(declaration: str) -> Tuple[FrozenSet[str], str]:
    tokens = declaration.split()
    index = 0
    while index < len(tokens) and tokens[index] in DECLARATION_MODIFIERS:
        index += 1
    return frozenset(tokens[:index]), " ".join(tokens[index:])


def _normalize_class_line(line: str) -> str:
    without_generics = re.sub(r"<[^<>]*>", "", line)
    normalized_commas = re.sub(r"\s*,\s*", ",", without_generics)
    return " ".join(normalized_commas.split())


def _parse_member(declaration: str, descriptor: str, class_name: str) -> Member:
    declaration = declaration.rstrip(";").strip()
    modifiers, remainder = _split_modifiers(declaration)
    access = _access(modifiers)
    stable_modifiers = frozenset(modifiers - ACCESS_MODIFIERS - {"final"})

    if remainder == "{}" and "static" in modifiers:
        kind = "initializer"
        name = "<clinit>"
    elif "(" in remainder:
        name = remainder.split("(", 1)[0].split()[-1]
        kind = "constructor" if name == class_name else "method"
    else:
        tokens = remainder.split()
        if len(tokens) < 2:
            raise AbiParseError(f"unrecognized member declaration: {declaration}")
        kind = "field"
        name = tokens[-1]

    return Member(
        kind=kind,
        name=name,
        descriptor=descriptor,
        access=access,
        final="final" in modifiers,
        modifiers=stable_modifiers,
    )


def parse_javap(text: str) -> AbiClass:
    class_kind: Optional[str] = None
    class_name: Optional[str] = None
    class_access: Optional[str] = None
    class_final = False
    class_modifiers: FrozenSet[str] = frozenset()
    class_hierarchy = ""
    members: Dict[Tuple[str, str, str], Member] = {}
    pending_declaration: Optional[str] = None
    inside_class = False

    for raw_line in text.splitlines():
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("Compiled from "):
            continue

        if not inside_class:
            normalized = _normalize_class_line(stripped)
            match = re.match(
                r"^(?P<prefix>.*?)\b(?P<kind>class|interface|enum)\s+"
                r"(?P<name>[^\s{]+)(?P<hierarchy>.*?)\{$",
                normalized,
            )
            if not match:
                raise AbiParseError(f"unrecognized class declaration: {stripped}")
            modifiers, leftover = _split_modifiers(match.group("prefix").strip())
            if leftover:
                raise AbiParseError(f"unrecognized class modifiers: {leftover}")
            class_kind = match.group("kind")
            class_name = match.group("name")
            class_access = _access(modifiers)
            class_final = "final" in modifiers
            class_modifiers = frozenset(modifiers - ACCESS_MODIFIERS - {"final"})
            class_hierarchy = match.group("hierarchy").strip()
            inside_class = True
            continue

        if stripped == "}":
            if pending_declaration is not None:
                raise AbiParseError(
                    f"missing descriptor for member: {pending_declaration.strip()}"
                )
            inside_class = False
            continue

        descriptor_match = re.match(r"^descriptor:\s*(\S+)$", stripped)
        if descriptor_match:
            if pending_declaration is None:
                raise AbiParseError(f"descriptor without member: {stripped}")
            member = _parse_member(
                pending_declaration,
                descriptor_match.group(1),
                class_name,
            )
            if member.key in members:
                raise AbiParseError(f"duplicate member: {member.label}")
            members[member.key] = member
            pending_declaration = None
            continue

        if pending_declaration is not None:
            raise AbiParseError(
                f"missing descriptor for member: {pending_declaration.strip()}"
            )
        pending_declaration = stripped

    if inside_class:
        raise AbiParseError("missing closing class brace")
    if class_name is None:
        raise AbiParseError("missing class declaration")
    return AbiClass(
        kind=class_kind,
        name=class_name,
        access=class_access,
        final=class_final,
        modifiers=class_modifiers,
        hierarchy=class_hierarchy,
        members=members,
    )


def _class_mismatch(baseline: AbiClass, candidate: AbiClass) -> Optional[str]:
    for attribute in ("kind", "name", "access", "modifiers", "hierarchy"):
        expected = getattr(baseline, attribute)
        actual = getattr(candidate, attribute)
        if expected != actual:
            return f"class {attribute} expected {expected}, got {actual}"
    return None


def _required_invariant_mismatch(
    abi: AbiClass,
    role: str,
    *,
    expected_class_final: bool,
    expected_target_effective_final: bool,
) -> Optional[str]:
    if abi.name != CLASS_NAME:
        return f"{role} class name expected {CLASS_NAME}, got {abi.name}"

    constructor = abi.members.get(CONSTRUCTOR_KEY)
    if constructor is None:
        return f"{role} missing constructor {CLASS_NAME} {CONSTRUCTOR_DESCRIPTOR}"
    if constructor.access != "public":
        return (
            f"{role} {constructor.label} access expected public, "
            f"got {constructor.access}"
        )

    target = abi.members.get(TARGET_KEY)
    if target is None:
        return f"{role} missing method {TARGET_NAME} {TARGET_DESCRIPTOR}"
    if target.access != "public":
        return f"{role} {target.label} access expected public, got {target.access}"

    if abi.final != expected_class_final:
        return (
            f"{role} class final expected {str(expected_class_final).lower()}, "
            f"got {str(abi.final).lower()}"
        )
    if target.final:
        return f"{role} {target.label} raw final expected false, got true"

    target_effective_final = abi.effective_final(target)
    if target_effective_final != expected_target_effective_final:
        return (
            f"{role} {target.label} effective final expected "
            f"{str(expected_target_effective_final).lower()}, "
            f"got {str(target_effective_final).lower()}"
        )
    return None


def _member_set_mismatch(baseline: AbiClass, candidate: AbiClass) -> Optional[str]:
    missing = sorted(set(baseline.members) - set(candidate.members))
    if missing:
        member = baseline.members[missing[0]]
        return f"missing {member.label}"
    unexpected = sorted(set(candidate.members) - set(baseline.members))
    if unexpected:
        member = candidate.members[unexpected[0]]
        return f"unexpected {member.label}"
    return None


def _member_mismatch(
    baseline: AbiClass,
    candidate: AbiClass,
    *,
    allow_target_final_removal: bool,
) -> Optional[str]:
    for key in sorted(baseline.members):
        expected = baseline.members[key]
        actual = candidate.members[key]
        if expected.access != actual.access:
            return (
                f"{expected.label} access expected {expected.access}, got {actual.access}"
            )
        if expected.modifiers != actual.modifiers:
            return (
                f"{expected.label} modifiers expected {sorted(expected.modifiers)}, "
                f"got {sorted(actual.modifiers)}"
            )

        if allow_target_final_removal:
            expected_final = baseline.effective_final(expected)
            actual_final = candidate.effective_final(actual)
            is_target = expected.kind == "method" and (
                expected.name,
                expected.descriptor,
            ) == (TARGET_NAME, TARGET_DESCRIPTOR)
            if is_target:
                if not expected_final:
                    return (
                        f"{expected.label} baseline effective final expected true, "
                        "got false"
                    )
                if actual_final:
                    return f"{expected.label} effective final expected false, got true"
            elif expected_final != actual_final:
                return (
                    f"{expected.label} effective final expected "
                    f"{str(expected_final).lower()}, got {str(actual_final).lower()}"
                )
        elif expected.final != actual.final:
            return (
                f"{expected.label} raw final expected "
                f"{str(expected.final).lower()}, got {str(actual.final).lower()}"
            )
    return None


def validate_text(baseline_text: str, candidate_text: str, mode: str):
    if mode not in {"retained", "rejected"}:
        return False, f"{mode}: unsupported mode"
    try:
        baseline = parse_javap(baseline_text)
        candidate = parse_javap(candidate_text)
    except AbiParseError as error:
        return False, f"{mode}: parse error: {error}"

    mismatch = _required_invariant_mismatch(
        baseline,
        "baseline",
        expected_class_final=True,
        expected_target_effective_final=True,
    )
    if mismatch:
        return False, f"{mode}: {mismatch}"
    mismatch = _required_invariant_mismatch(
        candidate,
        "candidate",
        expected_class_final=mode == "rejected",
        expected_target_effective_final=mode == "rejected",
    )
    if mismatch:
        return False, f"{mode}: {mismatch}"

    mismatch = _class_mismatch(baseline, candidate)
    if mismatch:
        return False, f"{mode}: {mismatch}"

    mismatch = _member_set_mismatch(baseline, candidate)
    if mismatch:
        return False, f"{mode}: {mismatch}"
    mismatch = _member_mismatch(
        baseline,
        candidate,
        allow_target_final_removal=mode == "retained",
    )
    if mismatch:
        return False, f"{mode}: {mismatch}"
    return True, f"{mode}: ABI validation passed"


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _regular_file(path_value: str, label: str) -> Path:
    path = Path(path_value)
    try:
        status = path.lstat()
    except OSError as error:
        raise ManifestError(f"{label} is unavailable: {error}") from error
    if not stat.S_ISREG(status.st_mode) or status.st_nlink != 1:
        raise ManifestError(f"{label} must be a single-link regular file")
    return path.resolve(strict=True)


def _checkout_root(path_value: str, label: str) -> Path:
    path = Path(path_value)
    try:
        status = path.lstat()
    except OSError as error:
        raise ManifestError(f"{label} is unavailable: {error}") from error
    if not stat.S_ISDIR(status.st_mode):
        raise ManifestError(f"{label} must be a directory")
    return path.resolve(strict=True)


def _bound_evidence(role: dict, label: str, checkout_root: Path) -> Path:
    try:
        evidence = role["structural"]
        expected_hash = evidence["sha256"]
        path = _regular_file(evidence["path"], f"{label} structural evidence")
    except (KeyError, TypeError) as error:
        raise ManifestError(f"{label} structural evidence binding is incomplete") from error
    if path != checkout_root and checkout_root not in path.parents:
        raise ManifestError(f"{label} structural evidence escapes checkout root")
    if _sha256(path) != expected_hash:
        raise ManifestError(f"{label} structural evidence hash mismatch")
    return path


def _validate_helper(binding: dict) -> None:
    try:
        path = _regular_file(binding["path"], "detached-root helper")
        expected_hash = binding["sha256"]
        api_version = binding["api_version"]
    except (KeyError, TypeError) as error:
        raise ManifestError("detached-root helper binding is incomplete") from error
    if path != EXPECTED_HELPER.resolve(strict=True):
        raise ManifestError("detached-root helper path mismatch")
    if _sha256(path) != expected_hash:
        raise ManifestError("detached-root helper hash mismatch")
    spec = importlib.util.spec_from_file_location("issue757_detached_roots", path)
    if spec is None or spec.loader is None:
        raise ManifestError("detached-root helper cannot be loaded")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    if api_version != HELPER_API_VERSION or module.PUBLIC_API_VERSION != api_version:
        raise ManifestError("detached-root helper API version mismatch")
    if frozenset(module.PUBLIC_API) != HELPER_PUBLIC_API:
        raise ManifestError("detached-root helper public API mismatch")


def validate_manifest(manifest_path: Path):
    manifest_path = _regular_file(str(manifest_path), "ABI manifest")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ManifestError(f"ABI manifest is unreadable: {error}") from error
    if not isinstance(manifest, dict) or manifest.get("schema") != MANIFEST_SCHEMA:
        raise ManifestError("ABI manifest schema mismatch")
    mode = manifest.get("mode")
    if mode not in {"retained", "rejected"}:
        raise ManifestError("ABI manifest mode mismatch")
    if manifest.get("class_name") != CLASS_NAME:
        raise ManifestError("ABI manifest class name mismatch")
    authority = manifest.get("authority")
    if not isinstance(authority, dict) or authority != {
        "baseline_revision": BASELINE_REVISION,
        "baseline_tree": BASELINE_TREE,
    }:
        raise ManifestError("baseline authority mismatch")
    _validate_helper(manifest.get("helper"))
    try:
        baseline_role = manifest["baseline"]
        candidate_role = manifest["candidate"]
        baseline_root = _checkout_root(
            baseline_role["checkout_root"], "baseline checkout root"
        )
        candidate_root = _checkout_root(
            candidate_role["checkout_root"], "candidate checkout root"
        )
    except (KeyError, TypeError) as error:
        raise ManifestError("checkout root binding is incomplete") from error
    if baseline_root == candidate_root:
        raise ManifestError("baseline and candidate checkout roots must be distinct")
    baseline_path = _bound_evidence(baseline_role, "baseline", baseline_root)
    candidate_path = _bound_evidence(candidate_role, "candidate", candidate_root)
    try:
        baseline_text = baseline_path.read_text(encoding="utf-8")
        candidate_text = candidate_path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as error:
        raise ManifestError(f"ABI evidence is unreadable: {error}") from error
    return validate_text(baseline_text, candidate_text, mode)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate the normalized LettuceBinaryCodec javap ABI."
    )
    subparsers = parser.add_subparsers(dest="command", required=True)
    validate_parser = subparsers.add_parser(
        "validate", help="validate evidence bound by a capture manifest"
    )
    validate_parser.add_argument("--manifest", required=True, type=Path)
    arguments = parser.parse_args()

    try:
        valid, diagnostic = validate_manifest(arguments.manifest)
    except ManifestError as error:
        print(f"manifest: {error}", file=sys.stderr)
        return 2
    print(diagnostic, file=sys.stdout if valid else sys.stderr)
    return 0 if valid else 1


if __name__ == "__main__":
    sys.exit(main())
