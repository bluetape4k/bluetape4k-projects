plugins {
    kotlin("kapt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(Libs.mapstruct)
    kapt(Libs.mapstruct_processor)
    kaptTest(Libs.mapstruct_processor)

    implementation(project(":bluetape4k-io"))
    testImplementation(project(":bluetape4k-junit5"))
}
