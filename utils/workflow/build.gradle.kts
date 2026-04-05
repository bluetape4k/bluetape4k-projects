dependencies {
    api(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk21"))
    implementation(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
