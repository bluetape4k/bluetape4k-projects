package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityManagerFactory
import org.hibernate.reactive.stage.Stage

fun EntityManagerFactory.asStageSessionFactory(): Stage.SessionFactory {
    return unwrap(Stage.SessionFactory::class.java)
}
