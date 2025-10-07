configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.timefold_solver_bom))

    api(Libs.timefold_solver_core)
    testImplementation(Libs.timefold_solver_test)

    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))


}
