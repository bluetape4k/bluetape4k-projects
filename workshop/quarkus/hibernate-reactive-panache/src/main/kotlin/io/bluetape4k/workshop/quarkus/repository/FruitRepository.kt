package io.bluetape4k.workshop.quarkus.repository

import io.bluetape4k.workshop.quarkus.model.Fruit
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@WithTransaction
class FruitRepository: PanacheRepositoryBase<Fruit, Long> {

    // NOTE: `@Transactional` 이 Mutiny 만 지원한다. Coroutines 는 서비스에서 쓰는 걸로 ㅠ.ㅠ
    //

    fun findByName(name: String): Uni<Fruit> {
        return find("name", name).firstResult()
    }
}
