package io.bluetape4k.io.serializer

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers.Jdk

/**
 * 다양한 [BinarySerializer]를 제공합니다.
 *
 * JDK 기본 직렬화, Kryo, Fory 직렬화기와
 * BZip2·Deflate·GZip·LZ4·Snappy·Zstd 압축 조합을 싱글톤으로 제공합니다.
 *
 * 예제:
 * ```kotlin
 * data class User(val id: Long, val name: String) : Serializable
 *
 * val user = User(1L, "debop")
 *
 * // JDK 기본 직렬화
 * val jdkBytes = BinarySerializers.Jdk.serialize(user)
 * val jdkRestored = BinarySerializers.Jdk.deserialize<User>(jdkBytes)
 * // jdkRestored == user
 *
 * // Kryo 직렬화 (빠름, 권장)
 * val kryoBytes = BinarySerializers.Kryo.serialize(user)
 * val kryoRestored = BinarySerializers.Kryo.deserialize<User>(kryoBytes)
 * // kryoRestored == user
 *
 * // Fory 직렬화
 * val foryBytes = BinarySerializers.Fory.serialize(user)
 * val foryRestored = BinarySerializers.Fory.deserialize<User>(foryBytes)
 * // foryRestored == user
 *
 * // LZ4 + Kryo 압축 직렬화 (대용량 객체에 유리)
 * val compressedBytes = BinarySerializers.LZ4Kryo.serialize(user)
 * val compressedRestored = BinarySerializers.LZ4Kryo.deserialize<User>(compressedBytes)
 * // compressedRestored == user
 * ```
 */
object BinarySerializers {

    /**
     * 기본 [BinarySerializer]. 현재 [Jdk]를 사용합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.Default.serialize(myObject)
     * val restored = BinarySerializers.Default.deserialize<MyClass>(bytes)
     * ```
     */
    val Default: BinarySerializer by lazy { Jdk }

    /**
     * JDK 표준 직렬화를 사용하는 [BinarySerializer].
     *
     * [java.io.Serializable]을 구현한 모든 객체를 직렬화할 수 있습니다.
     *
     * 예제:
     * ```kotlin
     * data class Item(val id: Long, val name: String) : Serializable
     *
     * val item = Item(42L, "hello")
     * val bytes = BinarySerializers.Jdk.serialize(item)
     * val restored = BinarySerializers.Jdk.deserialize<Item>(bytes)
     * // restored == item
     * ```
     */
    val Jdk: JdkBinarySerializer by lazy { JdkBinarySerializer() }

    /**
     * Kryo 라이브러리를 사용하는 [BinarySerializer].
     *
     * JDK 직렬화보다 빠르고 출력 크기가 작습니다. 일반적으로 권장하는 직렬화기입니다.
     *
     * 예제:
     * ```kotlin
     * data class Item(val id: Long, val name: String) : Serializable
     *
     * val item = Item(42L, "hello")
     * val bytes = BinarySerializers.Kryo.serialize(item)
     * val restored = BinarySerializers.Kryo.deserialize<Item>(bytes)
     * // restored == item
     * ```
     */
    val Kryo: KryoBinarySerializer by lazy { KryoBinarySerializer() }

    /**
     * Fory 라이브러리를 사용하는 [BinarySerializer].
     *
     * 높은 성능의 직렬화를 제공합니다.
     * 단, `BigDecimal`/`BigInteger` 타입은 지원하지 않습니다.
     *
     * 예제:
     * ```kotlin
     * data class Item(val id: Long, val name: String) : Serializable
     *
     * val item = Item(42L, "hello")
     * val bytes = BinarySerializers.Fory.serialize(item)
     * val restored = BinarySerializers.Fory.deserialize<Item>(bytes)
     * // restored == item
     * ```
     */
    val Fory: ForyBinarySerializer by lazy { ForyBinarySerializer() }

    /**
     * JDK 직렬화 후 BZip2 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.BZip2Jdk.serialize(largeObject)
     * val restored = BinarySerializers.BZip2Jdk.deserialize<MyClass>(bytes)
     * ```
     */
    val BZip2Jdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.BZip2)
    }

    /**
     * JDK 직렬화 후 Deflate 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.DeflateJdk.serialize(largeObject)
     * val restored = BinarySerializers.DeflateJdk.deserialize<MyClass>(bytes)
     * ```
     */
    val DeflateJdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.Deflate)
    }

    /**
     * JDK 직렬화 후 GZip 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.GZipJdk.serialize(largeObject)
     * val restored = BinarySerializers.GZipJdk.deserialize<MyClass>(bytes)
     * ```
     */
    val GZipJdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.GZip)
    }

    /**
     * JDK 직렬화 후 LZ4 알고리즘으로 압축하는 [BinarySerializer].
     *
     * LZ4는 압축/해제 속도가 매우 빠르며 실시간 처리에 적합합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.LZ4Jdk.serialize(largeObject)
     * val restored = BinarySerializers.LZ4Jdk.deserialize<MyClass>(bytes)
     * ```
     */
    val LZ4Jdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.LZ4)
    }

    /**
     * JDK 직렬화 후 Snappy 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.SnappyJdk.serialize(largeObject)
     * val restored = BinarySerializers.SnappyJdk.deserialize<MyClass>(bytes)
     * ```
     */
    val SnappyJdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.Snappy)
    }

    /**
     * JDK 직렬화 후 Zstd 알고리즘으로 압축하는 [BinarySerializer].
     *
     * Zstd는 높은 압축률과 빠른 속도를 동시에 제공합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.ZstdJdk.serialize(largeObject)
     * val restored = BinarySerializers.ZstdJdk.deserialize<MyClass>(bytes)
     * ```
     */
    val ZstdJdk: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Jdk, Compressors.Zstd)
    }

    /**
     * Kryo 직렬화 후 BZip2 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.BZip2Kryo.serialize(largeObject)
     * val restored = BinarySerializers.BZip2Kryo.deserialize<MyClass>(bytes)
     * ```
     */
    val BZip2Kryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.BZip2)
    }

    /**
     * Kryo 직렬화 후 Deflate 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.DeflateKryo.serialize(largeObject)
     * val restored = BinarySerializers.DeflateKryo.deserialize<MyClass>(bytes)
     * ```
     */
    val DeflateKryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.Deflate)
    }

    /**
     * Kryo 직렬화 후 GZip 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.GZipKryo.serialize(largeObject)
     * val restored = BinarySerializers.GZipKryo.deserialize<MyClass>(bytes)
     * ```
     */
    val GZipKryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.GZip)
    }

    /**
     * Kryo 직렬화 후 LZ4 알고리즘으로 압축하는 [BinarySerializer].
     *
     * Kryo의 빠른 직렬화와 LZ4의 고속 압축을 조합한 권장 설정입니다.
     *
     * 예제:
     * ```kotlin
     * data class Item(val id: Long, val name: String) : Serializable
     *
     * val item = Item(1L, "debop")
     * val bytes = BinarySerializers.LZ4Kryo.serialize(item)
     * val restored = BinarySerializers.LZ4Kryo.deserialize<Item>(bytes)
     * // restored == item
     * ```
     */
    val LZ4Kryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.LZ4)
    }

    /**
     * Kryo 직렬화 후 Snappy 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.SnappyKryo.serialize(largeObject)
     * val restored = BinarySerializers.SnappyKryo.deserialize<MyClass>(bytes)
     * ```
     */
    val SnappyKryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.Snappy)
    }

    /**
     * Kryo 직렬화 후 Zstd 알고리즘으로 압축하는 [BinarySerializer].
     *
     * Kryo 직렬화와 Zstd 압축의 조합으로 높은 압축률을 제공합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.ZstdKryo.serialize(largeObject)
     * val restored = BinarySerializers.ZstdKryo.deserialize<MyClass>(bytes)
     * ```
     */
    val ZstdKryo: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Kryo, Compressors.Zstd)
    }

    /**
     * Fory 직렬화 후 BZip2 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.BZip2Fory.serialize(largeObject)
     * val restored = BinarySerializers.BZip2Fory.deserialize<MyClass>(bytes)
     * ```
     */
    val BZip2Fory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.BZip2)
    }

    /**
     * Fory 직렬화 후 Deflate 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.DeflateFory.serialize(largeObject)
     * val restored = BinarySerializers.DeflateFory.deserialize<MyClass>(bytes)
     * ```
     */
    val DeflateFory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.Deflate)
    }

    /**
     * Fory 직렬화 후 GZip 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.GZipFory.serialize(largeObject)
     * val restored = BinarySerializers.GZipFory.deserialize<MyClass>(bytes)
     * ```
     */
    val GZipFory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.GZip)
    }

    /**
     * Fory 직렬화 후 LZ4 알고리즘으로 압축하는 [BinarySerializer].
     *
     * Fory의 고성능 직렬화와 LZ4의 고속 압축을 조합합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.LZ4Fory.serialize(largeObject)
     * val restored = BinarySerializers.LZ4Fory.deserialize<MyClass>(bytes)
     * ```
     */
    val LZ4Fory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.LZ4)
    }

    /**
     * Fory 직렬화 후 Snappy 알고리즘으로 압축하는 [BinarySerializer].
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.SnappyFory.serialize(largeObject)
     * val restored = BinarySerializers.SnappyFory.deserialize<MyClass>(bytes)
     * ```
     */
    val SnappyFory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.Snappy)
    }

    /**
     * Fory 직렬화 후 Zstd 알고리즘으로 압축하는 [BinarySerializer].
     *
     * Fory 직렬화와 Zstd 압축의 조합으로 높은 압축률을 제공합니다.
     *
     * 예제:
     * ```kotlin
     * val bytes = BinarySerializers.ZstdFory.serialize(largeObject)
     * val restored = BinarySerializers.ZstdFory.deserialize<MyClass>(bytes)
     * ```
     */
    val ZstdFory: CompressableBinarySerializer by lazy {
        CompressableBinarySerializer(Fory, Compressors.Zstd)
    }

}
