package io.bluetape4k.redis.redisson

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.ForyCodec
import io.bluetape4k.redis.redisson.codec.FuryCodec
import io.bluetape4k.redis.redisson.codec.GzipCodec
import io.bluetape4k.redis.redisson.codec.Lz4Codec
import io.bluetape4k.redis.redisson.codec.ProtobufCodec
import io.bluetape4k.redis.redisson.codec.ZstdCodec
import org.redisson.client.codec.Codec
import org.redisson.client.codec.DoubleCodec
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.Kryo5Codec
import org.redisson.codec.SerializationCodec
import org.redisson.codec.SnappyCodecV2

/**
 * Redisson 용 Codec.
 * Redisson에서 제공하는 Codec 보다 성능을 빠르게 하는 것들을 포함하고 있습니다.
 */
object RedissonCodecs: KLogging() {

    /**
     * Redisson 의 기본 Codec (Kryo5) 입니다.
     */
    @JvmStatic
    val Default: Codec by lazy { Fury }

    val Int: Codec by lazy { IntegerCodec() }
    val Long: Codec by lazy { LongCodec() }
    val Double: Codec by lazy { DoubleCodec() }
    val String: Codec by lazy { StringCodec() }

    val Kryo5: Codec by lazy { Kryo5Codec() }
    val Protobuf: Codec by lazy { ProtobufCodec() }

    /**
     * Apache Fury Serialization Codec
     */
    @Deprecated("Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 Fory 사용을 권장합니다.", ReplaceWith("Fory"))
    val Fury: Codec by lazy { FuryCodec() }

    /**
     * Apache Fory Serialization Codec
     */
    val Fory: Codec by lazy { ForyCodec() }

    /**
     * JDK Serialization Codec
     */
    val Jdk: Codec by lazy { SerializationCodec() }

    val Kryo5Composite: Codec by lazy { CompositeCodec(String, Kryo5, Kryo5) }
    val ProtobufComposite: Codec by lazy { CompositeCodec(String, Protobuf, Protobuf) }

    @Deprecated(
        "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyComposite 사용을 권장합니다.",
        ReplaceWith("ForyComposite")
    )
    val FuryComposite: Codec by lazy { CompositeCodec(String, Fury, Fury) }
    val ForyComposite: Codec by lazy { CompositeCodec(String, Fory, Fory) }
    val JdkComposite: Codec by lazy { CompositeCodec(String, Jdk, Jdk) }

    val GzipKryo5: Codec by lazy { GzipCodec(Kryo5) }
    val GzipProtobuf: Codec by lazy { GzipCodec(Protobuf) }

    @Deprecated("Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 Fory 사용을 권장합니다.", ReplaceWith("GzipFory"))
    val GzipFury: Codec by lazy { GzipCodec(Fury) }
    val GzipFory: Codec by lazy { GzipCodec(Fory) }
    val GzipJdk: Codec by lazy { GzipCodec(Jdk) }

    val GzipKryo5Composite: Codec by lazy { CompositeCodec(String, GzipKryo5, GzipKryo5) }
    val GzipProtobufComposite: Codec by lazy { CompositeCodec(String, GzipProtobuf, GzipProtobuf) }

    @Deprecated(
        "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyComposite 사용을 권장합니다.",
        ReplaceWith("GzipForyComposite")
    )
    val GzipFuryComposite: Codec by lazy { CompositeCodec(String, GzipFury, GzipFury) }
    val GzipForyComposite: Codec by lazy { CompositeCodec(String, GzipFory, GzipFory) }
    val GzipJdkComposite: Codec by lazy { CompositeCodec(String, GzipJdk, GzipJdk) }

    val LZ4Kryo5: Codec by lazy { Lz4Codec(Kryo5) }
    val LZ4Protobuf: Codec by lazy { Lz4Codec(Protobuf) }

    @Deprecated("Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 Fory 사용을 권장합니다.", ReplaceWith("LZ4Fory"))
    val LZ4Fury: Codec by lazy { Lz4Codec(Fury) }
    val LZ4Fory: Codec by lazy { Lz4Codec(Fory) }
    val LZ4Jdk: Codec by lazy { Lz4Codec(Jdk) }

    val LZ4Kryo5Composite: Codec by lazy { CompositeCodec(String, LZ4Kryo5, LZ4Kryo5) }
    val LZ4ProtobufComposite: Codec by lazy { CompositeCodec(String, LZ4Protobuf, LZ4Protobuf) }

    @Deprecated(
        "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyComposite 사용을 권장합니다.",
        ReplaceWith("LZ4ForyComposite")
    )
    val LZ4FuryComposite: Codec by lazy { CompositeCodec(String, LZ4Fury, LZ4Fury) }
    val LZ4ForyComposite: Codec by lazy { CompositeCodec(String, LZ4Fory, LZ4Fory) }
    val LZ4JdkComposite: Codec by lazy { CompositeCodec(String, LZ4Jdk, LZ4Jdk) }

    val SnappyKryo5: Codec by lazy { SnappyCodecV2(Kryo5) }
    val SnappyProtobuf: Codec by lazy { SnappyCodecV2(Protobuf) }

    @Deprecated("Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 Fory 사용을 권장합니다.", ReplaceWith("SnappyFory"))
    val SnappyFury: Codec by lazy { SnappyCodecV2(Fury) }
    val SnappyFory: Codec by lazy { SnappyCodecV2(Fory) }
    val SnappyJdk: Codec by lazy { SnappyCodecV2(Jdk) }

    val SnappyKryo5Composite: Codec by lazy { CompositeCodec(String, SnappyKryo5, SnappyKryo5) }
    val SnappyProtobufComposite: Codec by lazy { CompositeCodec(String, SnappyProtobuf, SnappyProtobuf) }

    @Deprecated(
        "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyComposite 사용을 권장합니다.",
        ReplaceWith("SnappyForyComposite")
    )
    val SnappyFuryComposite: Codec by lazy { CompositeCodec(String, SnappyFury, SnappyFury) }
    val SnappyForyComposite: Codec by lazy { CompositeCodec(String, SnappyFory, SnappyFory) }
    val SnappyJdkComposite: Codec by lazy { CompositeCodec(String, SnappyJdk, SnappyJdk) }

    val ZstdKryo5: Codec by lazy { ZstdCodec(Kryo5) }
    val ZstdProtobuf: Codec by lazy { ZstdCodec(Protobuf) }

    @Deprecated("Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 Fory 사용을 권장합니다.", ReplaceWith("ZstdFory"))
    val ZstdFury: Codec by lazy { ZstdCodec(Fury) }
    val ZstdFory: Codec by lazy { ZstdCodec(Fory) }
    val ZstdJdk: Codec by lazy { ZstdCodec(Jdk) }

    val ZstdKryo5Composite: Codec by lazy { CompositeCodec(String, ZstdKryo5, ZstdKryo5) }
    val ZstdProtobufComposite: Codec by lazy { CompositeCodec(String, ZstdProtobuf, ZstdProtobuf) }

    @Deprecated(
        "Apache Fury 프로젝트가 중단되어 유지보수가 되지 않음에 따라 사용을 권장하지 않습니다. 대신 ForyComposite 사용을 권장합니다.",
        ReplaceWith("ZstdForyComposite")
    )
    val ZstdFuryComposite: Codec by lazy { CompositeCodec(String, ZstdFury, ZstdFury) }
    val ZstdForyComposite: Codec by lazy { CompositeCodec(String, ZstdFory, ZstdFory) }
    val ZstdJdkComposite: Codec by lazy { CompositeCodec(String, ZstdJdk, ZstdJdk) }

}
