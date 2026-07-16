package io.bluetape4k.avro.compat.issue754.java;

import io.bluetape4k.avro.AvroGenericRecordSerializer;
import io.bluetape4k.avro.AvroReflectSerializer;
import io.bluetape4k.avro.AvroSpecificRecordSerializer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificRecord;

public final class NewAvroBufferCaller {
    private static final Schema SCHEMA = new Schema.Parser().parse(
        "{\"type\":\"record\",\"name\":\"Issue754Record\",\"fields\":[{\"name\":\"value\",\"type\":\"string\"}]}"
    );

    private NewAvroBufferCaller() {
    }

    public static void main(String[] args) throws Exception {
        verifyReflectDefaults();
        verifyGenericDefaults();
        verifySpecificDefaults();
        verifyKotlinDefaults();
        System.out.println("avro-default-dispatch=PASS");
    }

    private static void verifyKotlinDefaults() throws Exception {
        AvroReflectSerializer reflect = (AvroReflectSerializer) Class.forName(
            "io.bluetape4k.avro.compat.issue754.kotlin.LegacyAvroReflectImplementation"
        ).getDeclaredConstructor().newInstance();
        ByteBuffer reflectTarget = ByteBuffer.allocate(32);
        require(reflect.serializeTo("value", reflectTarget) > 0, "Kotlin reflect default did not write");
        require("reflect".equals(reflect.deserializeFrom(ByteBuffer.wrap("reflect".getBytes(StandardCharsets.UTF_8)), String.class)),
            "unexpected Kotlin reflect value");

        AvroGenericRecordSerializer generic = (AvroGenericRecordSerializer) Class.forName(
            "io.bluetape4k.avro.compat.issue754.kotlin.LegacyAvroGenericRecordImplementation"
        ).getDeclaredConstructor().newInstance();
        GenericData.Record record = new GenericData.Record(SCHEMA);
        record.put("value", "generic");
        require(generic.serializeTo(SCHEMA, record, ByteBuffer.allocate(64)) > 0,
            "Kotlin generic default did not write");
        require(generic.deserializeFrom(SCHEMA, ByteBuffer.allocate(0)) == null,
            "unexpected Kotlin generic value");

        AvroSpecificRecordSerializer specific = (AvroSpecificRecordSerializer) Class.forName(
            "io.bluetape4k.avro.compat.issue754.kotlin.LegacyAvroSpecificRecordImplementation"
        ).getDeclaredConstructor().newInstance();
        DummySpecificRecord specificRecord = new DummySpecificRecord();
        require(specific.serializeTo(specificRecord, ByteBuffer.allocate(64)) > 0,
            "Kotlin specific default did not write");
        require(specific.deserializeFrom(ByteBuffer.allocate(0), DummySpecificRecord.class) == null,
            "unexpected Kotlin specific value");
        require(specific.serializeListTo(List.of(specificRecord), ByteBuffer.allocate(128)) > 0,
            "Kotlin specific-list default did not write");
        require(specific.deserializeListFrom(ByteBuffer.allocate(0), DummySpecificRecord.class).isEmpty(),
            "unexpected Kotlin specific-list value");
    }

    private static void verifyReflectDefaults() {
        LegacyAvroReflectImplementation serializer = new LegacyAvroReflectImplementation();
        expectNullPointer(() -> serializer.serializeTo("value", null));
        require(serializer.serializeCalls == 0, "reflect null target invoked legacy serialization");

        ByteBuffer target = ByteBuffer.allocate(32);
        int count = serializer.serializeTo("value", target);
        require(count == 7, "unexpected reflect count");

        expectNullPointer(() -> serializer.deserializeFrom(null, Object.class));
        require(serializer.deserializeCalls == 0, "reflect null source invoked legacy deserialization");
        ByteBuffer source = ByteBuffer.wrap("reflect".getBytes(StandardCharsets.UTF_8));
        int position = source.position();
        Object restored = serializer.deserializeFrom(source, Object.class);
        require("reflect".equals(restored), "unexpected reflect value");
        require(source.position() == position, "reflect source position changed");
    }

    private static void verifyGenericDefaults() {
        LegacyAvroGenericRecordImplementation serializer = new LegacyAvroGenericRecordImplementation();
        GenericData.Record graph = new GenericData.Record(SCHEMA);
        graph.put("value", "generic");

        expectNullPointer(() -> serializer.serializeTo(SCHEMA, graph, null));
        require(serializer.serializeCalls == 0, "generic null target invoked legacy serialization");
        ByteBuffer target = ByteBuffer.allocate(32);
        require(serializer.serializeTo(SCHEMA, graph, target) == 7, "unexpected generic count");

        expectNullPointer(() -> serializer.deserializeFrom(SCHEMA, null));
        require(serializer.deserializeCalls == 0, "generic null source invoked legacy deserialization");
        ByteBuffer source = ByteBuffer.wrap("generic".getBytes(StandardCharsets.UTF_8));
        int position = source.position();
        GenericData.Record restored = serializer.deserializeFrom(SCHEMA, source);
        require("generic".equals(restored.get("value")), "unexpected generic value");
        require(source.position() == position, "generic source position changed");
    }

    private static void verifySpecificDefaults() {
        LegacyAvroSpecificRecordImplementation serializer = new LegacyAvroSpecificRecordImplementation();
        DummySpecificRecord graph = new DummySpecificRecord();

        expectNullPointer(() -> serializer.serializeTo(graph, null));
        require(serializer.serializeCalls == 0, "specific null target invoked legacy serialization");
        ByteBuffer target = ByteBuffer.allocate(64);
        require(serializer.serializeTo(graph, target) == 8, "unexpected specific count");

        expectNullPointer(() -> serializer.deserializeFrom(null, DummySpecificRecord.class));
        require(serializer.deserializeCalls == 0, "specific null source invoked legacy deserialization");
        require(serializer.deserializeFrom(ByteBuffer.allocate(0), DummySpecificRecord.class) == null,
            "unexpected specific value");

        expectNullPointer(() -> serializer.serializeListTo(List.of(graph), null));
        require(serializer.serializeListCalls == 0, "specific-list null target invoked legacy serialization");
        require(serializer.serializeListTo(List.of(graph), target) == 13, "unexpected specific-list count");

        expectNullPointer(() -> serializer.deserializeListFrom(null, DummySpecificRecord.class));
        require(serializer.deserializeListCalls == 0, "specific-list null source invoked legacy deserialization");
        require(serializer.deserializeListFrom(ByteBuffer.allocate(0), DummySpecificRecord.class).isEmpty(),
            "unexpected specific-list value");
    }

    private static void expectNullPointer(Runnable block) {
        try {
            block.run();
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static final class DummySpecificRecord implements SpecificRecord {
        private String value = "specific";

        @Override
        public void put(int field, Object value) {
            if (field != 0) {
                throw new IndexOutOfBoundsException(field);
            }
            this.value = value == null ? null : value.toString();
        }

        @Override
        public Object get(int field) {
            if (field != 0) {
                throw new IndexOutOfBoundsException(field);
            }
            return value;
        }

        @Override
        public Schema getSchema() {
            return SCHEMA;
        }
    }
}
