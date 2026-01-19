package io.bluetape4k.examples.redisson.coroutines.readwritethrough

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class JdbcConfiguration {

    @Bean
    fun dataSource(): DataSource =
        EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .addScript("classpath:database/schema.sql")
            .addScript("classpath:database/data.sql")
            .build()

    @Bean
    fun jdbcTemplate(): JdbcTemplate = JdbcTemplate(dataSource())

    @Bean
    @ConditionalOnMissingBean
    fun transactionManager(): PlatformTransactionManager =
        DataSourceTransactionManager(dataSource())

}
