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

    val GZipJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.GZip) }
    val DeflateJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Deflate) }
    val LZ4Jdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Zstd) }

    val Kryo by unsafeLazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val GZipKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.GZip) }
    val DeflateKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val LZ4Kryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Zstd) }

    val Fury by unsafeLazy { BinaryJaversCodec(BinarySerializers.Fury) }

    val GZipFury by unsafeLazy { CompressableBinaryJaversCodec(Fury, Compressors.GZip) }
    val DeflateFury by unsafeLazy { CompressableBinaryJaversCodec(Fury, Compressors.Deflate) }
    val LZ4Fury by unsafeLazy { CompressableBinaryJaversCodec(Fury, Compressors.LZ4) }
    val SnappyFury by unsafeLazy { CompressableBinaryJaversCodec(Fury, Compressors.Snappy) }
    val ZstdFury by unsafeLazy { CompressableBinaryJaversCodec(Fury, Compressors.Zstd) }

    // Map
    val Map by unsafeLazy { MapJaversCodec() }
}
