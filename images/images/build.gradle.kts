configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    // Images
    // https://mvnrepository.com/artifact/com.sksamuel.scrimage/scrimage-core
    api(Libs.scrimage_core)
    api(Libs.scrimage_filters)
    implementation(Libs.scrimage_webp)

    // EXIF metadata (required runtime dependency)
    implementation(Libs.metadata_extractor)

    // TIFF support via TwelveMonkeys ImageIO (auto-registers via SPI)
    api(Libs.twelvemonkeys_imageio_tiff)
    api(Libs.twelvemonkeys_imageio_metadata)

    // SVG rasterization via Apache Batik (opt-in; add to your own dependencies if needed)
    compileOnly(Libs.batik_transcoder)
    compileOnly(Libs.batik_codec)
    testImplementation(Libs.batik_transcoder)
    testImplementation(Libs.batik_codec)

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
