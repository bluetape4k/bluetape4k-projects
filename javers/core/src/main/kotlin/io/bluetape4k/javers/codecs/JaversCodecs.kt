package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.support.unsafeLazy


object JaversCodecs {

    val Default by unsafeLazy { String }

    // String Codecs

    val String by unsafeLazy { StringJaversCodec() }

    val GZipString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.GZip) }
    val DeflateString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Deflate) }
    val LZ4String by unsafeLazy { CompressableStringJaversCodec(String, Compressors.LZ4) }
    val SnappyString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Snappy) }
    val ZstdString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Zstd) }

    // Binary Codecs

    val Jdk by unsafeLazy { BinaryJaversCodec(BinarySerializers.Jdk) }

    val DeflateJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Deflate) }
    val GZipJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.GZip) }
    val LZ4Jdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Zstd) }

    val Kryo by unsafeLazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val DeflateKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val GZipKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.GZip) }
    val LZ4Kryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Zstd) }

    val Fory by unsafeLazy { BinaryJaversCodec(BinarySerializers.Fory) }

    val DeflateFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Deflate) }
    val GZipFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.GZip) }
    val LZ4Fory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.LZ4) }
    val SnappyFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Snappy) }
    val ZstdFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Zstd) }

    // Map
    val Map by unsafeLazy { MapJaversCodec() }
}
