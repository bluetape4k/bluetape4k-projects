plugins {
    alias(libs.plugins.avro.plugin)
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

    api(bt4k.avro)
    api(libs.avro.kotlin)

    // Compressor
    runtimeOnly(libs.snappy.java)
    runtimeOnly(libs.lz4.java)
    runtimeOnly(bt4k.zstd.jni)
    runtimeOnly(libs.xz)
}
