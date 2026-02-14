package io.bluetape4k.io.serializer

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.unsafeLazy

/**
 * 다양한 [BinarySerializer]를 제공합니다.
 */
/**
 * `BinarySerializers` 싱글톤/유틸리티입니다.
 */
object BinarySerializers {

    /** Default BinarySerializer */
    val Default: BinarySerializer by unsafeLazy { Jdk }

    /**
     * Jdk BinarySerializer
     */
    val Jdk: JdkBinarySerializer by unsafeLazy { JdkBinarySerializer() }

    /**
     * Kryo BinarySerializer
     */
    val Kryo: KryoBinarySerializer by unsafeLazy { KryoBinarySerializer() }

    /**
     * Fory BinarySerializer
     */
    val Fory: ForyBinarySerializer by unsafeLazy { ForyBinarySerializer() }

    /**
     * Jdk 직렬화 후 BZip2 알고리즘으로 압축하는 BinarySerializer
     */
    val BZip2Jdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.BZip2)
    }

    /**
     * Jdk 직렬화 후 Deflate 알고리즘으로 압축하는 BinarySerializer
     */
    val DeflateJdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.Deflate)
    }

    /**
     * Jdk 직렬화 후 GZip 알고리즘으로 압축하는 BinarySerializer
     */
    val GZipJdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.GZip)
    }

    /**
     * Jdk 직렬화 후 LZ4 알고리즘으로 압축하는 BinarySerializer
     */
    val LZ4Jdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.LZ4)
    }

    /**
     * Jdk 직렬화 후 Snappy 알고리즘으로 압축하는 BinarySerializer
     */
    val SnappyJdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.Snappy)
    }

    /**
     * Jdk 직렬화 후 Zstd 알고리즘으로 압축하는 BinarySerializer
     */
    val ZstdJdk: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Jdk, Compressors.Zstd)
    }

    /**
     * Kryo 직렬화 후 BZip2 알고리즘으로 압축하는 BinarySerializer
     */
    val BZip2Kryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.BZip2)
    }

    /**
     * Kryo 직렬화 후 Deflate 알고리즘으로 압축하는 BinarySerializer
     */
    val DeflateKryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.Deflate)
    }

    /**
     * Kryo 직렬화 후 GZip 알고리즘으로 압축하는 BinarySerializer
     */
    val GZipKryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.GZip)
    }

    /**
     * Kryo 직렬화 후 LZ4 알고리즘으로 압축하는 BinarySerializer
     */
    val LZ4Kryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.LZ4)
    }

    /**
     * Kryo 직렬화 후 Snappy 알고리즘으로 압축하는 BinarySerializer
     */
    val SnappyKryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.Snappy)
    }

    /**
     * Kryo 직렬화 후 Zstd 알고리즘으로 압축하는 BinarySerializer
     */
    val ZstdKryo: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Kryo, Compressors.Zstd)
    }

    /**
     * Fory 직렬화 후 BZip2 알고리즘으로 압축하는 BinarySerializer
     */
    val BZip2Fory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.BZip2)
    }

    /**
     * Fory 직렬화 후 Deflate 알고리즘으로 압축하는 BinarySerializer
     */
    val DeflateFory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.Deflate)
    }

    /**
     * Fory 직렬화 후 GZip 알고리즘으로 압축하는 BinarySerializer
     */
    val GZipFory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.GZip)
    }

    /**
     *  Fory 직렬화 후 LZ4 알고리즘으로 압축하는 BinarySerializer
     */
    val LZ4Fory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.LZ4)
    }

    /**
     * Fory 직렬화 후 Snappy 알고리즘으로 압축하는 BinarySerializer
     */
    val SnappyFory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.Snappy)
    }

    /**
     * Fory 직렬화 후 Zstd 알고리즘으로 압축하는 BinarySerializer
     */
    val ZstdFory: CompressableBinarySerializer by unsafeLazy {
        CompressableBinarySerializer(Fory, Compressors.Zstd)
    }

}
