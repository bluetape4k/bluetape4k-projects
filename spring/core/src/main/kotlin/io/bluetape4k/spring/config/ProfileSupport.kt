package io.bluetape4k.spring.config

import org.springframework.context.annotation.Profile

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("local")
annotation class LocalProfile(val name: String = "local")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("dev", "develop", "development")
annotation class DevelopProfile(val name: String = "development")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("feature")
annotation class FeatureProfile(val name: String = "feature")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("test", "testing")
annotation class TestProfile(val name: String = "test")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("qa")
annotation class QaProfile(val name: String = "qa")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("stage", "staging")
annotation class StageProfile(val name: String = "staging")

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Profile("prod", "product", "production")
annotation class ProductionProfile(val name: String = "production")
