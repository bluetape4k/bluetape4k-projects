package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.support.unsafeLazy


object GsonCodecs {

    val Default by unsafeLazy { String }

    // String Codecs

    val String by unsafeLazy { StringGsonCodec() }

    val GZipString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.GZip) }
    val DeflateString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Deflate) }
    val LZ4String by unsafeLazy { CompressableStringGsonCodec(String, Compressors.LZ4) }
    val SnappyString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Snappy) }
    val ZstdString by unsafeLazy { CompressableStringGsonCodec(String, Compressors.Zstd) }

    // Binary Codecs

    val Jdk by unsafeLazy { BinaryGsonCodec(BinarySerializers.Jdk) }

    val GZipJdk by unsafeLazy { CompressableBinaryGsonCodec(Jdk, Compressors.GZip) }
    val DeflateJdk by unsafeLazy { CompressableBinaryGsonCodec(Jdk, Compressors.Deflate) }
    val LZ4Jdk by unsafeLazy { CompressableBinaryGsonCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by unsafeLazy { CompressableBinaryGsonCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by unsafeLazy { CompressableBinaryGsonCodec(Jdk, Compressors.Zstd) }

    val Kryo by unsafeLazy { BinaryGsonCodec(BinarySerializers.Kryo) }

    val GZipKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.GZip) }
    val DeflateKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Deflate) }
    val LZ4Kryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by unsafeLazy { CompressableBinaryGsonCodec(Kryo, Compressors.Zstd) }

    val Fury by unsafeLazy { BinaryGsonCodec(BinarySerializers.Fury) }

    val GZipFury by unsafeLazy { CompressableBinaryGsonCodec(Fury, Compressors.GZip) }
    val DeflateFury by unsafeLazy { CompressableBinaryGsonCodec(Fury, Compressors.Deflate) }
    val LZ4Fury by unsafeLazy { CompressableBinaryGsonCodec(Fury, Compressors.LZ4) }
    val SnappyFury by unsafeLazy { CompressableBinaryGsonCodec(Fury, Compressors.Snappy) }
    val ZstdFury by unsafeLazy { CompressableBinaryGsonCodec(Fury, Compressors.Zstd) }

    // Map
    val Map by unsafeLazy { MapGsonCodec() }
}
