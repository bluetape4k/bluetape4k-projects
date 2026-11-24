package io.bluetape4k.io.digest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestersJavaContractTest {

    @Test
    void javaCallerUsesDigestersAndBase64Overloads() {
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] expectedDigest = HexFormat.of().parseHex(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
        byte[] codecInput = new byte[] {(byte) 0xfb, (byte) 0xff};

        String defaultBase64 = Digesters.SHA256.digestBase64(input);
        String explicitBase64 = Digesters.SHA256.digestBase64(input, Base64Format.URL_SAFE_UNPADDED);
        String codecDefault = Base64Codec.encode(codecInput);
        byte[] codecDecoded = Base64Codec.decode("-_8=");
        String codecExplicit = Base64Codec.encode(codecInput, Base64Format.STANDARD_UNPADDED);
        byte[] codecExplicitDecoded = Base64Codec.decode("+/8", Base64Format.STANDARD_UNPADDED);

        assertArrayEquals(expectedDigest, Digesters.SHA256.digest(input));
        assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0=", defaultBase64);
        assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0", explicitBase64);
        assertEquals("-_8=", codecDefault);
        assertArrayEquals(codecInput, codecDecoded);
        assertEquals("+/8", codecExplicit);
        assertArrayEquals(codecInput, codecExplicitDecoded);

        DigestSession session = Digesters.SHA256.newSession();
        assertArrayEquals(expectedDigest, session.update(input, 0, 1).update(input, 1, input.length - 1).digest());
    }

    @Test
    void javaFacadeExposesStaticFieldAndOverloadDescriptors() throws ReflectiveOperationException {
        assertEquals(Digester.class, Digesters.class.getField("SHA256").getType());
        assertEquals(String.class, Base64Codec.class.getMethod("encode", byte[].class).getReturnType());
        assertEquals(
                String.class,
                Base64Codec.class.getMethod("encode", byte[].class, Base64Format.class).getReturnType()
        );
        assertEquals(byte[].class, Base64Codec.class.getMethod("decode", String.class).getReturnType());
        assertEquals(
                byte[].class,
                Base64Codec.class.getMethod("decode", String.class, Base64Format.class).getReturnType()
        );
    }
}
