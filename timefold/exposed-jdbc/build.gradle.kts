configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.timefold_solver_bom))

    api(project(":bluetape4k-timefold-core"))
    api(Libs.timefold_solver_persistence_common)
    testImplementation(Libs.timefold_solver_test)

    api(project(":bluetape4k-exposed"))
    api(project(":bluetape4k-exposed-tests"))
    testImplementation(project(":bluetape4k-junit5"))
}
