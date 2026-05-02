plugins {
    kotlin("kapt")
}

kapt {
    correctErrorTypes = true
    showProcessorStats = true
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)
    kaptTest(libs.mapstruct.processor)

    implementation(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
}
