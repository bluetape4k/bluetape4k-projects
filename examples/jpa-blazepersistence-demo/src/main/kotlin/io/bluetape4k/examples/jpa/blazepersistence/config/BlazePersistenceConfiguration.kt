package io.bluetape4k.examples.jpa.blazepersistence.config

import com.blazebit.persistence.Criteria
import com.blazebit.persistence.CriteriaBuilderFactory
import com.blazebit.persistence.view.EntityViewManager
import com.blazebit.persistence.view.EntityViews
import com.blazebit.persistence.view.spi.EntityViewConfiguration
import io.bluetape4k.examples.jpa.blazepersistence.domain.view.MemberSummaryView
import io.bluetape4k.examples.jpa.blazepersistence.domain.view.MemberTeamView
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Local Blaze Persistence wiring for the Spring Boot 4 example.
 */
@Configuration
class BlazePersistenceConfiguration {

    @Bean
    fun criteriaBuilderFactory(entityManagerFactory: EntityManagerFactory): CriteriaBuilderFactory {
        return Criteria.getDefault().createCriteriaBuilderFactory(entityManagerFactory)
    }

    @Bean
    fun entityViewConfiguration(): EntityViewConfiguration {
        return EntityViews.createDefaultConfiguration().apply {
            addEntityView(MemberSummaryView::class.java)
            addEntityView(MemberTeamView::class.java)
        }
    }

    @Bean
    fun entityViewManager(
        criteriaBuilderFactory: CriteriaBuilderFactory,
        entityViewConfiguration: EntityViewConfiguration,
    ): EntityViewManager {
        return entityViewConfiguration.createEntityViewManager(criteriaBuilderFactory)
    }
}
