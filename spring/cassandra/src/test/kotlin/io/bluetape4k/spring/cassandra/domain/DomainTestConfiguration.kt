package io.bluetape4k.spring.cassandra.domain

import io.bluetape4k.spring.cassandra.domain.model.AllPossibleTypes
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = [AllPossibleTypes::class])
class DomainTestConfiguration: io.bluetape4k.spring.cassandra.AbstractCassandraTestConfiguration() {

    // NOTE: 테스트 대상 Entity가 있는 Package를 지정하거나 @EntityScan 을 사용하세요
    // domain 에 있는 entity를 사용합니다.
    // override fun getEntityBasePackages(): Array<String> = arrayOf(Group::class.java.packageName)

}
