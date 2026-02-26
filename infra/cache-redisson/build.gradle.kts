dependencies {
    api(project(":bluetape4k-cache"))

    // Redisson JCache provider
    api(Libs.redisson)
    api(Libs.jackson_module_kotlin)
}
