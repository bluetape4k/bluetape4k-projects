package io.bluetape4k.spring.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.ClassUtils

/**
 * [Retrofit2Client] annotation이 적용된 class를 Retrofit Client로 만들어 Spring Bean으로 등록합니다.
 */
class RetrofitClientsRegistrar: ImportBeanDefinitionRegistrar {

    companion object: KLogging()

    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
    ) {

        log.debug { "Scanning Retrofit2Client ..." }
        val scanner = getScanner()
        scanner.addIncludeFilter(AnnotationTypeFilter(Retrofit2Client::class.java))

        val basePackages = getBasePackages(importingClassMetadata)
        basePackages
            .map(scanner::findCandidateComponents)
            .flatten()
            .forEach { candidate ->
                log.debug { "Found Retrofit2Client candidate=$candidate" }
                val beanDefinition = candidate as AnnotatedBeanDefinition
                val attributes =
                    beanDefinition.metadata.getAnnotationAttributes(Retrofit2Client::class.java.canonicalName)

                if (attributes != null) {
                    val name = attributes["name"].toString()
                    log.debug {
                        "New Retrofit Client BeanDefinition with name=$name, " +
                                "type=${beanDefinition.beanClassName}, baseUrl=${attributes["baseUrl"]}"
                    }

                    val builder = BeanDefinitionBuilder.genericBeanDefinition(RetrofitClientFactoryBean::class.java)
                    builder.addPropertyValue("type", beanDefinition.beanClassName)
                    builder.addPropertyValue("name", name)
                    builder.addPropertyValue("baseUrl", attributes["baseUrl"])

                    registerClientConfiguration(registry, name, attributes["configuration"])

                    log.info { "Register Retrofit Client BeanDefinition. name=$name, definition=${builder.beanDefinition}" }
                    registry.registerBeanDefinition(name, builder.beanDefinition)
                }
            }
    }

    private fun getScanner(): ClassPathScanningCandidateComponentProvider {
        return object: ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return beanDefinition.metadata.isIndependent &&
                        !beanDefinition.metadata.isAnnotation
            }
        }
    }

    private fun registerClientConfiguration(registry: BeanDefinitionRegistry, name: Any?, configuration: Any?) {
        val builder = BeanDefinitionBuilder.genericBeanDefinition(RetrofitClientSpecification::class.java)

        builder.addConstructorArgValue(name)
        builder.addConstructorArgValue(configuration)

        registry.registerBeanDefinition(
            "$name.${RetrofitClientSpecification::class.simpleName}",
            builder.beanDefinition
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBasePackages(importingClassMetadata: AnnotationMetadata): Set<String> {
        val basePackages = mutableSetOf<String>()
        val attributes = importingClassMetadata.getAnnotationAttributes(EnableRetrofitClients::class.java.canonicalName)

        attributes?.let {
            (it["value"] as? Array<String>)?.forEach { pkg ->
                if (pkg.isNotBlank()) {
                    basePackages.add(pkg)
                }
            }
            (it["basePackages"] as? Array<String>)?.forEach { pkg ->
                if (pkg.isNotBlank()) {
                    basePackages.add(pkg)
                }
            }
            (it["basePackageClasses"] as? Array<Class<*>>)?.forEach { clazz ->
                basePackages.add(ClassUtils.getPackageName(clazz))
            }
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.className))
        }

        log.debug { "basePackages for @Retrofit2Client=${basePackages.joinToString()}" }
        return basePackages
    }
}
