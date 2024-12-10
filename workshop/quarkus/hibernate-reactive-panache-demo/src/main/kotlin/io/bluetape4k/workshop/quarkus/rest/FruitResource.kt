package io.bluetape4k.workshop.quarkus.rest

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.quarkus.model.Fruit
import io.bluetape4k.workshop.quarkus.repository.FruitRepository
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class FruitResource(
    private val repository: FruitRepository,
) {
    companion object: KLogging()

    @GET
    @WithTransaction
    fun getAll(): Uni<List<Fruit>> {
        return repository.listAll()
    }

    @GET
    @Path("/{name}")
    @WithTransaction
    fun findByName(name: String): Uni<Fruit> {
        return repository.findByName(name)
    }

    /**
     * NOTE: `@ReactiveTransactional` 을 사용하기 위해 suspend 함수보다는 Mutiny 객체를 사용하여 반환합니다.
     * NOTE: `@ReactiveTransactional` 이 suspend 함수를 지원하지 않는다 !!!
     */
    @POST
    @WithTransaction
    fun addFruit(@Valid fruit: Fruit): Uni<Fruit> {
        log.debug("add fruit. {}", fruit)
        return repository.persist(fruit)
    }
}
