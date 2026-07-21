import importlib.util
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("validate-lettuce-binary-codec-abi.py")
HELPER = (
    Path(__file__).parents[3]
    / "benchmark/protobuf-codec-benchmark/scripts/issue757_detached_roots.py"
)
CLASS_NAME = "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec"
CONSTRUCTOR_DESCRIPTOR = "(Lio/bluetape4k/io/serializer/BinarySerializer;)V"
TARGET_DESCRIPTOR = "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V"
BRIDGE_MEMBERS = {
    ("encodeKey", "(Ljava/lang/Object;)Ljava/nio/ByteBuffer;"),
    ("encodeKey", "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V"),
    ("decodeKey", "(Ljava/nio/ByteBuffer;)Ljava/lang/Object;"),
}

SPEC = importlib.util.spec_from_file_location(
    "validate_lettuce_binary_codec_abi",
    SCRIPT,
)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


def render_javap(*, class_final=True, class_name=CLASS_NAME, members=None, reverse=False):
    class_modifiers = "public final" if class_final else "public"
    declarations = members or baseline_members()
    if reverse:
        declarations = list(reversed(declarations))
    body = "\n\n".join(
        f"  {declaration}\n    descriptor: {descriptor}"
        for declaration, descriptor in declarations
    )
    return (
        'Compiled from "LettuceBinaryCodec.kt"\n'
        f"{class_modifiers} class {class_name}<V> "
        "implements java.io.Serializable {\n"
        f"{body}\n"
        "}\n"
    )


def baseline_members():
    return [
        (
            "private final io.bluetape4k.io.serializer.BinarySerializer serializer;",
            "Lio/bluetape4k/io/serializer/BinarySerializer;",
        ),
        (
            "public io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec("
            "io.bluetape4k.io.serializer.BinarySerializer);",
            "(Lio/bluetape4k/io/serializer/BinarySerializer;)V",
        ),
        (
            "public final io.bluetape4k.io.serializer.BinarySerializer getSerializer();",
            "()Lio/bluetape4k/io/serializer/BinarySerializer;",
        ),
        (
            "public java.nio.ByteBuffer encodeKey(java.lang.String);",
            "(Ljava/lang/String;)Ljava/nio/ByteBuffer;",
        ),
        (
            "public void encodeKey(java.lang.String, io.netty.buffer.ByteBuf);",
            "(Ljava/lang/String;Lio/netty/buffer/ByteBuf;)V",
        ),
        (
            "public void encodeValue(V, io.netty.buffer.ByteBuf);",
            TARGET_DESCRIPTOR,
        ),
        (
            "public V decodeValue(java.nio.ByteBuffer);",
            "(Ljava/nio/ByteBuffer;)Ljava/lang/Object;",
        ),
        (
            "public java.lang.String decodeKey(java.nio.ByteBuffer);",
            "(Ljava/nio/ByteBuffer;)Ljava/lang/String;",
        ),
        (
            "public int estimateSize(java.lang.Object);",
            "(Ljava/lang/Object;)I",
        ),
        ("public java.lang.String toString();", "()Ljava/lang/String;"),
        (
            "public java.nio.ByteBuffer encodeKey(java.lang.Object);",
            "(Ljava/lang/Object;)Ljava/nio/ByteBuffer;",
        ),
        (
            "public void encodeKey(java.lang.Object, io.netty.buffer.ByteBuf);",
            "(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V",
        ),
        (
            "public java.lang.Object decodeKey(java.nio.ByteBuffer);",
            "(Ljava/nio/ByteBuffer;)Ljava/lang/Object;",
        ),
    ]


def retained_members():
    result = []
    for declaration, descriptor in baseline_members():
        member_name = declaration.split("(", 1)[0].split()[-1]
        if "(" in declaration and "LettuceBinaryCodec(" not in declaration:
            if (
                (member_name, descriptor) != ("encodeValue", TARGET_DESCRIPTOR)
                and (member_name, descriptor) not in BRIDGE_MEMBERS
                and " final " not in declaration
            ):
                declaration = declaration.replace("public ", "public final ", 1)
        result.append((declaration, descriptor))
    return result


def replace_member(
    members,
    descriptor,
    *,
    member_name=None,
    declaration=None,
    new_descriptor=None,
):
    result = []
    for current_declaration, current_descriptor in members:
        if current_descriptor == descriptor and (
            member_name is None or member_name in current_declaration
        ):
            current_declaration = declaration or current_declaration
            current_descriptor = new_descriptor or current_descriptor
        result.append((current_declaration, current_descriptor))
    return result


class AbiValidatorTest(unittest.TestCase):
    def assert_valid(self, mode, baseline, candidate):
        valid, diagnostic = validator.validate_text(baseline, candidate, mode)
        self.assertTrue(valid, diagnostic)
        self.assertEqual(f"{mode}: ABI validation passed", diagnostic)

    def assert_invalid(self, mode, baseline, candidate, first_mismatch):
        valid, diagnostic = validator.validate_text(baseline, candidate, mode)
        self.assertFalse(valid, diagnostic)
        self.assertTrue(diagnostic.startswith(f"{mode}: "), diagnostic)
        self.assertIn(first_mismatch, diagnostic)

    def mode_candidate(self, mode, *, members=None, class_final=None, class_name=CLASS_NAME):
        return render_javap(
            class_final=(mode == "rejected") if class_final is None else class_final,
            class_name=class_name,
            members=(baseline_members() if mode == "rejected" else retained_members())
            if members is None
            else members,
        )

    def test_modes_reject_wrong_class_before_comparison(self):
        wrong_class = "example.WrongCodec"
        for mode in ("retained", "rejected"):
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(class_name=wrong_class),
                    self.mode_candidate(mode, class_name=wrong_class),
                    f"baseline class name expected {CLASS_NAME}, got {wrong_class}",
                )

    def test_modes_reject_missing_required_constructor(self):
        for mode in ("retained", "rejected"):
            baseline = [m for m in baseline_members() if m[1] != CONSTRUCTOR_DESCRIPTOR]
            candidate = [m for m in self._mode_members(mode) if m[1] != CONSTRUCTOR_DESCRIPTOR]
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(members=baseline),
                    self.mode_candidate(mode, members=candidate),
                    f"baseline missing constructor {CLASS_NAME} "
                    f"{CONSTRUCTOR_DESCRIPTOR}",
                )

    def test_modes_reject_non_public_required_constructor(self):
        declaration = (
            f"protected {CLASS_NAME}(io.bluetape4k.io.serializer.BinarySerializer);"
        )
        for mode in ("retained", "rejected"):
            baseline = replace_member(
                baseline_members(),
                CONSTRUCTOR_DESCRIPTOR,
                declaration=declaration,
            )
            candidate = replace_member(
                self._mode_members(mode),
                CONSTRUCTOR_DESCRIPTOR,
                declaration=declaration,
            )
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(members=baseline),
                    self.mode_candidate(mode, members=candidate),
                    f"baseline constructor {CLASS_NAME} {CONSTRUCTOR_DESCRIPTOR} "
                    "access expected public, got protected",
                )

    def test_modes_reject_missing_required_target(self):
        def without_target(members):
            return [
                member
                for member in members
                if not ("encodeValue" in member[0] and member[1] == TARGET_DESCRIPTOR)
            ]

        for mode in ("retained", "rejected"):
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(members=without_target(baseline_members())),
                    self.mode_candidate(mode, members=without_target(self._mode_members(mode))),
                    f"baseline missing method encodeValue {TARGET_DESCRIPTOR}",
                )

    def test_modes_reject_non_final_baseline_class(self):
        for mode in ("retained", "rejected"):
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(class_final=False),
                    self.mode_candidate(mode, class_final=False),
                    "baseline class final expected true, got false",
                )

    def test_modes_reject_raw_final_baseline_target(self):
        target_final = "public final void encodeValue(V, io.netty.buffer.ByteBuf);"
        baseline = replace_member(
            baseline_members(),
            TARGET_DESCRIPTOR,
            member_name="encodeValue",
            declaration=target_final,
        )
        for mode in ("retained", "rejected"):
            candidate = (
                retained_members()
                if mode == "retained"
                else replace_member(
                    baseline_members(),
                    TARGET_DESCRIPTOR,
                    member_name="encodeValue",
                    declaration=target_final,
                )
            )
            with self.subTest(mode=mode):
                self.assert_invalid(
                    mode,
                    render_javap(members=baseline),
                    self.mode_candidate(mode, members=candidate),
                    f"baseline method encodeValue {TARGET_DESCRIPTOR} raw final "
                    "expected false, got true",
                )

    @staticmethod
    def _mode_members(mode):
        return baseline_members() if mode == "rejected" else retained_members()

    def test_retained_accepts_only_class_and_target_effective_final_removal(self):
        candidate = render_javap(
            class_final=False,
            members=retained_members(),
            reverse=True,
        )

        self.assert_valid("retained", render_javap(), candidate)

    def test_retained_rejects_constructor_descriptor_change(self):
        constructor = "(Lio/bluetape4k/io/serializer/BinarySerializer;)V"
        candidate_members = replace_member(
            retained_members(),
            constructor,
            new_descriptor="()V",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "missing constructor io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec "
            "(Lio/bluetape4k/io/serializer/BinarySerializer;)V",
        )

    def test_retained_rejects_constructor_access_change(self):
        constructor = "(Lio/bluetape4k/io/serializer/BinarySerializer;)V"
        candidate_members = replace_member(
            retained_members(),
            constructor,
            declaration=(
                "protected io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec("
                "io.bluetape4k.io.serializer.BinarySerializer);"
            ),
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "constructor io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec "
            "(Lio/bluetape4k/io/serializer/BinarySerializer;)V access expected public, "
            "got protected",
        )

    def test_retained_rejects_property_access_change(self):
        getter = "()Lio/bluetape4k/io/serializer/BinarySerializer;"
        candidate_members = replace_member(
            retained_members(),
            getter,
            declaration=(
                "protected final io.bluetape4k.io.serializer.BinarySerializer "
                "getSerializer();"
            ),
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "method getSerializer ()Lio/bluetape4k/io/serializer/BinarySerializer; "
            "access expected public, got protected",
        )

    def test_retained_rejects_property_descriptor_change(self):
        getter = "()Lio/bluetape4k/io/serializer/BinarySerializer;"
        candidate_members = replace_member(
            retained_members(),
            getter,
            new_descriptor="()Ljava/lang/Object;",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "missing method getSerializer "
            "()Lio/bluetape4k/io/serializer/BinarySerializer;",
        )

    def test_retained_rejects_other_method_descriptor_change(self):
        candidate_members = replace_member(
            retained_members(),
            "(Ljava/lang/Object;)I",
            new_descriptor="(Ljava/lang/String;)I",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "missing method estimateSize (Ljava/lang/Object;)I",
        )

    def test_retained_rejects_other_method_access_change(self):
        candidate_members = replace_member(
            retained_members(),
            "(Ljava/lang/Object;)I",
            declaration="protected final int estimateSize(java.lang.Object);",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            "method estimateSize (Ljava/lang/Object;)I access expected public, got protected",
        )

    def test_retained_rejects_member_removal(self):
        candidate_members = [
            member
            for member in retained_members()
            if not (
                "encodeValue" in member[0]
                and member[1] == TARGET_DESCRIPTOR
            )
        ]

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=candidate_members),
            f"missing method encodeValue {TARGET_DESCRIPTOR}",
        )

    def test_retained_rejects_missing_class_final_removal(self):
        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=True, members=retained_members()),
            "class final expected false, got true",
        )

    def test_retained_rejects_missing_target_final_removal(self):
        target_final = replace_member(
            retained_members(),
            TARGET_DESCRIPTOR,
            member_name="encodeValue",
            declaration="public final void encodeValue(V, io.netty.buffer.ByteBuf);",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=target_final),
            f"candidate method encodeValue {TARGET_DESCRIPTOR} raw final expected false, "
            "got true",
        )

    def test_retained_rejects_unrelated_final_removal(self):
        key_descriptor = "(Ljava/lang/String;)Ljava/nio/ByteBuffer;"
        unrelated_open = replace_member(
            retained_members(),
            key_descriptor,
            declaration="public java.nio.ByteBuffer encodeKey(java.lang.String);",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=unrelated_open),
            f"method encodeKey {key_descriptor} effective final expected true, got false",
        )

    def test_rejected_accepts_normalized_exact_abi_equality(self):
        self.assert_valid(
            "rejected",
            render_javap(),
            render_javap(members=baseline_members(), reverse=True),
        )

    def test_rejected_rejects_class_final_removal(self):
        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(class_final=False, members=retained_members()),
            "class final expected true, got false",
        )

    def test_rejected_rejects_target_raw_final_addition(self):
        candidate_members = replace_member(
            baseline_members(),
            TARGET_DESCRIPTOR,
            member_name="encodeValue",
            declaration="public final void encodeValue(V, io.netty.buffer.ByteBuf);",
        )

        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(members=candidate_members),
            f"candidate method encodeValue {TARGET_DESCRIPTOR} raw final expected false, "
            "got true",
        )

    def test_rejected_rejects_descriptor_change(self):
        candidate_members = replace_member(
            baseline_members(),
            "(Ljava/lang/Object;)I",
            new_descriptor="(Ljava/lang/String;)I",
        )

        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(members=candidate_members),
            "missing method estimateSize (Ljava/lang/Object;)I",
        )

    def test_rejected_rejects_access_change(self):
        candidate_members = replace_member(
            baseline_members(),
            "()Ljava/lang/String;",
            declaration="protected java.lang.String toString();",
        )

        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(members=candidate_members),
            "method toString ()Ljava/lang/String; access expected public, got protected",
        )

    def test_rejected_rejects_non_target_raw_final_addition_inside_final_class(self):
        candidate_members = replace_member(
            baseline_members(),
            "(Ljava/lang/Object;)I",
            declaration="public final int estimateSize(java.lang.Object);",
        )

        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(members=candidate_members),
            "method estimateSize (Ljava/lang/Object;)I raw final expected false, got true",
        )

    def test_rejected_rejects_non_target_raw_final_removal_inside_final_class(self):
        getter = "()Lio/bluetape4k/io/serializer/BinarySerializer;"
        candidate_members = replace_member(
            baseline_members(),
            getter,
            declaration=(
                "public io.bluetape4k.io.serializer.BinarySerializer getSerializer();"
            ),
        )

        self.assert_invalid(
            "rejected",
            render_javap(),
            render_javap(members=candidate_members),
            f"method getSerializer {getter} raw final expected true, got false",
        )

    def test_diagnostic_reports_the_first_structural_mismatch(self):
        changed = replace_member(
            retained_members(),
            "()Lio/bluetape4k/io/serializer/BinarySerializer;",
            declaration=(
                "protected final io.bluetape4k.io.serializer.BinarySerializer "
                "getSerializer();"
            ),
        )
        changed = replace_member(
            changed,
            "()Ljava/lang/String;",
            declaration="protected final java.lang.String toString();",
        )

        self.assert_invalid(
            "retained",
            render_javap(),
            render_javap(class_final=False, members=changed),
            "method getSerializer ()Lio/bluetape4k/io/serializer/BinarySerializer; "
            "access expected public, got protected",
        )

    def test_cli_requires_subcommand_and_rejects_legacy_two_file_surface(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT)],
            capture_output=True,
            text=True,
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("validate", result.stderr)

        legacy = subprocess.run(
            [
                sys.executable,
                str(SCRIPT),
                "--baseline",
                "baseline.txt",
                "--candidate",
                "candidate.txt",
                "--mode",
                "retained",
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertNotEqual(0, legacy.returncode)

    def test_cli_validates_manifest_bound_files(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            baseline_root = root / "baseline-root"
            candidate_root = root / "candidate-root"
            baseline_root.mkdir()
            candidate_root.mkdir()
            baseline = baseline_root / "baseline.struct.txt"
            candidate = candidate_root / "candidate.struct.txt"
            baseline.write_text(render_javap(), encoding="utf-8")
            candidate.write_text(
                render_javap(
                    class_final=False,
                    members=retained_members(),
                    reverse=True,
                ),
                encoding="utf-8",
            )
            manifest = root / "manifest.json"
            manifest.write_text(
                json.dumps(
                    {
                        "schema": "issue757-lettuce-abi-v1",
                        "mode": "retained",
                        "class_name": CLASS_NAME,
                        "authority": {
                            "baseline_revision": "4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88",
                            "baseline_tree": "086f83baa7eec0cd68e68fff132542ef6db0f200",
                        },
                        "helper": {
                            "path": str(HELPER.resolve()),
                            "sha256": hashlib.sha256(HELPER.read_bytes()).hexdigest(),
                            "api_version": 1,
                        },
                        "baseline": {
                            "checkout_root": str(baseline_root),
                            "structural": {
                                "path": str(baseline),
                                "sha256": hashlib.sha256(baseline.read_bytes()).hexdigest(),
                            },
                        },
                        "candidate": {
                            "checkout_root": str(candidate_root),
                            "structural": {
                                "path": str(candidate),
                                "sha256": hashlib.sha256(candidate.read_bytes()).hexdigest(),
                            },
                        },
                    }
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "validate",
                    "--manifest",
                    str(manifest),
                ],
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("retained: ABI validation passed\n", result.stdout)

    def test_manifest_rejects_wrong_authority_hash_and_shared_root(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            structural = root / "structural.txt"
            structural.write_text(render_javap(), encoding="utf-8")
            payload = {
                "schema": "issue757-lettuce-abi-v1",
                "mode": "rejected",
                "class_name": CLASS_NAME,
                "authority": {
                    "baseline_revision": "wrong",
                    "baseline_tree": "wrong",
                },
                "helper": {
                    "path": str(HELPER.resolve()),
                    "sha256": "0" * 64,
                    "api_version": 1,
                },
                "baseline": {
                    "checkout_root": str(root),
                    "structural": {"path": str(structural), "sha256": "0" * 64},
                },
                "candidate": {
                    "checkout_root": str(root),
                    "structural": {"path": str(structural), "sha256": "0" * 64},
                },
            }
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps(payload), encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), "validate", "--manifest", str(manifest)],
                capture_output=True,
                text=True,
                check=False,
            )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("baseline authority", result.stderr)


if __name__ == "__main__":
    unittest.main()
