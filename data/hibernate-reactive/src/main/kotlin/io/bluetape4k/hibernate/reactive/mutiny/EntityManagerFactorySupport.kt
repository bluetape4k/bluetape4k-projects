package io.bluetape4k.hibernate.reactive.mutiny

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.mutiny.Mutiny

fun EntityManagerFactory.asMutinySessionFactory(): Mutiny.SessionFactory {
    return unwrap(Mutiny.SessionFactory::class.java)
}
