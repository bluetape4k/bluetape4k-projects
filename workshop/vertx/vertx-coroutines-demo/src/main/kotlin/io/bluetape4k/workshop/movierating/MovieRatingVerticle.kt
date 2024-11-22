package io.bluetape4k.workshop.movierating

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.asInt
import io.bluetape4k.vertx.web.coHandler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.jdbcclient.JDBCPool
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.jdbcclient.jdbcConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.sqlclient.Tuple

class MovieRatingVerticle: CoroutineVerticle() {

    companion object: KLogging() {
        private val statements = listOf(
            "CREATE TABLE MOVIE (ID VARCHAR(16) PRIMARY KEY, TITLE VARCHAR(256) NOT NULL)",
            "CREATE TABLE RATING (ID INT AUTO_INCREMENT PRIMARY KEY, RATE_VALUE INT, MOVIE_ID VARCHAR(16))",
            "INSERT INTO MOVIE (ID, TITLE) VALUES ('starwars', 'Star Wars')",
            "INSERT INTO MOVIE (ID, TITLE) VALUES ('indianajones', 'Indiana Jones')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (1, 'starwars')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (5, 'starwars')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (9, 'starwars')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (10, 'starwars')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (4, 'indianajones')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (7, 'indianajones')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (3, 'indianajones')",
            "INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (9, 'indianajones')"
        )
    }

    private val pool by lazy {
        val jdbcConnectOptions = jdbcConnectOptionsOf(
            jdbcUrl = "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;",
            user = "sa",
        )
        JDBCPool.pool(vertx, jdbcConnectOptions, poolOptionsOf(maxSize = 16, shared = true))
    }

    override suspend fun start() {
        // 샘플 데이터 추가
        statements.forEach { stmt ->
            log.info { "Execute statement: $stmt" }
            pool.query(stmt).execute().coAwait()
        }

        // Router 설정
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        // ex: http://localhost:8080/movie/starwars
        router.get("/movie/:id").coHandler { getMovie(it) }
        router.post("/rateMovie/:id").coHandler { rateMovie(it) }
        router.get("/getRating/:id").coHandler { getRating(it) }

        // HTTP 서버 시작
        log.debug { "Start HTTP Server. http://localhost:${config.getInteger("http.port", 8080)}" }
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(config.getInteger("http.port", 8080))
            .coAwait()
    }

    private suspend fun getMovie(ctx: RoutingContext) {
        val id = ctx.pathParam("id")
        val rows = pool
            .preparedQuery("SELECT TITLE FROM MOVIE WHERE ID=?")
            .execute(Tuple.of(id))
            .onSuccess {
                log.debug { "Get Movie. movie=${it.firstOrNull()?.deepToString()}" }
            }
            .coAwait()

        if (rows.size() == 1) {
            val row = rows.first()
            val json = json {
                obj(
                    "id" to id,
                    "title" to row.getString("TITLE")
                )
            }
            ctx.response().end(json.encode())
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }

    private suspend fun rateMovie(ctx: RoutingContext) {
        val movieId = ctx.pathParam("id")
        val rating = ctx.queryParam("getRating").firstOrNull().asInt(1)

        val movies = pool.preparedQuery("SELECT TITLE FROM MOVIE WHERE ID=?")
            .execute(Tuple.of(movieId))
            .coAwait()

        if (movies.size() == 1) {
            pool.preparedQuery("INSERT INTO RATING (RATE_VALUE, MOVIE_ID) VALUES (?, ?)")
                .execute(Tuple.of(rating, movieId))
                .coAwait()
            ctx.response().setStatusCode(200).end()
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }

    private suspend fun getRating(ctx: RoutingContext) {
        val movieId = ctx.pathParam("id")

        val rows = pool
            .preparedQuery("SELECT AVG(RATE_VALUE) AS RATE_VALUE FROM RATING WHERE MOVIE_ID=?")
            .execute(Tuple.of(movieId))
            .coAwait()

        if (rows.size() == 1) {
            val json = Json.obj {
                put("id", movieId)
                put("getRating", rows.firstOrNull()?.getDouble("RATE_VALUE"))
            }
            ctx.response().end(json.encode())
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }
}
