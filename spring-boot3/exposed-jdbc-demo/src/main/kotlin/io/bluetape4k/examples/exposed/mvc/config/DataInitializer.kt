package io.bluetape4k.examples.exposed.mvc.config

import io.bluetape4k.examples.exposed.mvc.domain.ProductEntity
import io.bluetape4k.examples.exposed.mvc.domain.Products
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 컴포넌트.
 */
@Component
class DataInitializer: ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        transaction {
            MigrationUtils.statementsRequiredForDatabaseMigration(Products).forEach { exec(it) }

            if (ProductEntity.count() == 0L) {
                ProductEntity.new {
                    name = "Kotlin Programming Book"
                    price = BigDecimal("39.99")
                    stock = 100
                }
                ProductEntity.new {
                    name = "Spring Boot Guide"
                    price = BigDecimal("49.99")
                    stock = 50
                }
                ProductEntity.new {
                    name = "Exposed ORM Tutorial"
                    price = BigDecimal("29.99")
                    stock = 200
                }
            }
        }
    }
}
