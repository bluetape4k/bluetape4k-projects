// Umbrella 모듈: bluetape4k-lettuce + bluetape4k-redisson + bluetape4k-spring-data-redis
dependencies {
    api(project(":bluetape4k-lettuce"))
    api(project(":bluetape4k-redisson"))
    implementation(project(":bluetape4k-spring-boot3-redis"))
}
