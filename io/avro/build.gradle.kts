plugins {
    id(Plugins.avro) version Plugins.Versions.avro
}

avro {
    isCreateSetters.set(true)
    isCreateOptionalGetters.set(false)
    isGettersReturnOptional.set(false)
    fieldVisibility.set("PUBLIC")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
    templateDirectory.set(null as String?)
    isEnableDecimalLogicalType.set(true)
}

// Build script 에 아래와 같이 compile 전에 avro 를 generate 하도록 해주면 Kotlin 에서도 사용이 가능합니다.
tasks.compileKotlin { dependsOn(tasks.generateAvroJava) }
tasks.compileTestKotlin { dependsOn(tasks.generateTestAvroJava) }
//tasks["compileKotlin"].dependsOn(tasks["generateAvroJava"])
//tasks["compileTestKotlin"].dependsOn(tasks["generateTestAvroJava"])

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))

    api(Libs.avro)
    api(Libs.avro_kotlin)

    // Compressor
    testImplementation(Libs.snappy_java)
    testImplementation(Libs.lz4_java)
    testImplementation(Libs.zstd_jni)
    testImplementation(Libs.xz)
}
