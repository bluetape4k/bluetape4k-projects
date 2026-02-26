dependencies {
    api(project(":bluetape4k-cache"))

    // Apache Ignite 2.x JCache provider
    api(Libs.ignite_core)
    api(Libs.ignite_clients)
}
