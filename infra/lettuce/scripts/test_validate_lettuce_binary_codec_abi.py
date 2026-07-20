import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("validate-lettuce-binary-codec-abi.py")
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


def render_javap(*, class_final=True, members=None, reverse=False):
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
        f"{class_modifiers} class io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec<V> "
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
            f"method encodeValue {TARGET_DESCRIPTOR} effective final expected false, got true",
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

    def test_rejected_rejects_target_final_removal(self):
        baseline_members_open = retained_members()
        baseline_members_open = replace_member(
            baseline_members_open,
            TARGET_DESCRIPTOR,
            member_name="encodeValue",
            declaration="public final void encodeValue(V, io.netty.buffer.ByteBuf);",
        )
        candidate_members = replace_member(
            baseline_members_open,
            TARGET_DESCRIPTOR,
            member_name="encodeValue",
            declaration="public void encodeValue(V, io.netty.buffer.ByteBuf);",
        )

        self.assert_invalid(
            "rejected",
            render_javap(class_final=False, members=baseline_members_open),
            render_javap(class_final=False, members=candidate_members),
            f"method encodeValue {TARGET_DESCRIPTOR} raw final expected true, got false",
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

    def test_cli_requires_baseline_candidate_and_mode(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT)],
            capture_output=True,
            text=True,
            check=False,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("--baseline", result.stderr)
        self.assertIn("--candidate", result.stderr)
        self.assertIn("--mode", result.stderr)

    def test_cli_validates_files(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            baseline = Path(temp_dir) / "baseline.javap"
            candidate = Path(temp_dir) / "candidate.javap"
            baseline.write_text(render_javap(), encoding="utf-8")
            candidate.write_text(
                render_javap(
                    class_final=False,
                    members=retained_members(),
                    reverse=True,
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--baseline",
                    str(baseline),
                    "--candidate",
                    str(candidate),
                    "--mode",
                    "retained",
                ],
                capture_output=True,
                text=True,
                check=False,
            )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("retained: ABI validation passed\n", result.stdout)


if __name__ == "__main__":
    unittest.main()
