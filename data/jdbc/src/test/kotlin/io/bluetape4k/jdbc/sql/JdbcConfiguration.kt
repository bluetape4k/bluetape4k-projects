package io.bluetape4k.jdbc.sql

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Configuration
class JdbcConfiguration {

    @Bean
    fun dataSource() =
        EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .addScript("classpath:database/schema.sql")
            .addScript("classpath:database/data.sql")
            .addScript("classpath:db/schema-h2.sql")
            .addScript("classpath:db/data-h2.sql")
            .build()

    @Bean
    fun jdbcTemplate(): JdbcTemplate = JdbcTemplate(dataSource())

    @Bean
    fun namedParameterJdbcTemplate(): NamedParameterJdbcTemplate =
        NamedParameterJdbcTemplate(dataSource())

    @Bean
    @ConditionalOnMissingBean
    fun transactionManager(): PlatformTransactionManager =
        DataSourceTransactionManager(dataSource())

    @Bean
    @ConditionalOnMissingBean
    fun transactionTemplate(): TransactionTemplate =
        TransactionTemplate(transactionManager())
}
