package io.bluetape4k.testcontainers.storage

import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.TransactionOptions
import com.mongodb.WriteConcern
import com.mongodb.client.MongoClients
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.bson.Document
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MongoDBServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val DATABASE_NAME = "testcontainers-database"
    }

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch mongodb 8 server`() {
            MongoDBServer(databaseName = DATABASE_NAME).use { mongo ->
                mongo.start()
                mongo.isRunning.shouldBeTrue()
                mongo.replicaSetUrl.shouldNotBeNullOrBlank()

                verifyMongo(mongo)
                crudWithTransaction(mongo)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch mongodb 8 server with default port`() {
            MongoDBServer(useDefaultPort = true, databaseName = DATABASE_NAME).use { mongo ->
                mongo.start()
                mongo.isRunning.shouldBeTrue()
                mongo.replicaSetUrl.shouldNotBeNullOrBlank()

                verifyMongo(mongo)
                crudWithTransaction(mongo)
            }
        }
    }

    private fun verifyMongo(mongo: MongoDBServer) {
        MongoClients.create(mongo.url).use { client ->
            val db = client.getDatabase(DATABASE_NAME)
            val customers = db.getCollection("customers")

            val document = Document().apply {
                put("name", "Debop")
                put("commany", "Self Employeed")
            }
            val result = customers.insertOne(document)
            result.insertedId.shouldNotBeNull()

            val loaded = customers.find().toFastList()
            loaded shouldContain document
        }
    }

    private fun crudWithTransaction(mongo: MongoDBServer) {
        MongoClients.create(mongo.replicaSetUrl).use { client ->
            client.getDatabase("mydb1")
                .getCollection("foo")
                .withWriteConcern(WriteConcern.MAJORITY)
                .insertOne(Document("abc", 0))

            client.getDatabase("mydb2")
                .getCollection("bar")
                .withWriteConcern(WriteConcern.MAJORITY)
                .insertOne(Document("xyz", 0))

            client.startSession().use { clientSession ->
                val txOptions = TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.LOCAL)
                    .writeConcern(WriteConcern.MAJORITY)
                    .build()

                val txResult = "Inserted into collections in different databases"

                val txBody: () -> String = {
                    val coll1 = client.getDatabase("mydb1").getCollection("foo")
                    val coll2 = client.getDatabase("mydb2").getCollection("bar")

                    coll1.insertOne(clientSession, Document("abc", 1))
                    coll2.insertOne(clientSession, Document("xyz", 999))
                    txResult
                }

                try {
                    val txResultActual = clientSession.withTransaction(txBody, txOptions)
                    txResultActual shouldBeEqualTo txResult
                } catch (re: RuntimeException) {
                    throw IllegalStateException(re.message, re)
                }
            }
        }
    }
}
