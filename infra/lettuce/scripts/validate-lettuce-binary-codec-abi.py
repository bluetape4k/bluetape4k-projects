#!/usr/bin/env python3
import argparse
import re
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
TARGET_NAME = "encodeValue"
TARGET_DESCRIPTOR = "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V"
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

    mismatch = _class_mismatch(baseline, candidate)
    if mismatch:
        return False, f"{mode}: {mismatch}"

    if mode == "retained":
        if not baseline.final:
            return False, f"{mode}: baseline class final expected true, got false"
        if candidate.final:
            return False, f"{mode}: class final expected false, got true"
    elif baseline.final != candidate.final:
        return (
            False,
            f"{mode}: class final expected {str(baseline.final).lower()}, "
            f"got {str(candidate.final).lower()}",
        )

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


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate the normalized LettuceBinaryCodec javap ABI."
    )
    parser.add_argument("--baseline", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--mode", required=True, choices=("retained", "rejected"))
    arguments = parser.parse_args()

    try:
        baseline_text = arguments.baseline.read_text(encoding="utf-8")
        candidate_text = arguments.candidate.read_text(encoding="utf-8")
    except OSError as error:
        print(f"{arguments.mode}: unable to read ABI input: {error}", file=sys.stderr)
        return 2

    valid, diagnostic = validate_text(
        baseline_text,
        candidate_text,
        arguments.mode,
    )
    print(diagnostic, file=sys.stdout if valid else sys.stderr)
    return 0 if valid else 1


if __name__ == "__main__":
    sys.exit(main())
