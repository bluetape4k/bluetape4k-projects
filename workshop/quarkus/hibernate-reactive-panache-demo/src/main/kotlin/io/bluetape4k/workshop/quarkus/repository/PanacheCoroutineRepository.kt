package io.bluetape4k.workshop.quarkus.repository

import io.quarkus.hibernate.reactive.panache.PanacheRepository
import io.smallrye.mutiny.coroutines.awaitSuspending

interface PanacheCoroutineRepository<T>: PanacheRepository<T> {

    // NOTE: `@Transactional` 이 Mutiny 만 지원한다. ㅠ.ㅠ

    suspend fun coPersist(entity: T): T {
        return persist(entity).awaitSuspending()
    }

}
