configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    compileOnly(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
    compileOnly(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Serializers
    testImplementation(project(":bluetape4k-io"))
    testImplementation(bt4k.fory.kotlin)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
