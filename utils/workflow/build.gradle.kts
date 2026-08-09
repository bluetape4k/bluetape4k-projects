dependencies {
    api(project(":bluetape4k-core"))
    implementation(project(":bluetape4k-virtualthread-api"))
    runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
    implementation(project(":bluetape4k-coroutines"))
    testImplementation(project(":bluetape4k-junit5"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
