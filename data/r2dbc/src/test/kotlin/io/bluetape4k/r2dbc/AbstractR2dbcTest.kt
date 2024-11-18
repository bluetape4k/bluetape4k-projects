package io.bluetape4k.r2dbc

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.r2dbc.core.execute
import io.bluetape4k.support.uninitialized
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.awaitRowsUpdated

@DataR2dbcTest
abstract class AbstractR2dbcTest {

    companion object: KLogging()

    @Autowired
    protected val client: R2dbcClient = uninitialized()

    @BeforeEach
    fun beforeEach() {
        runSuspendIO {
            client.execute(
                """
                    CREATE TABLE IF NOT EXISTS users (
                      user_id serial NOT NULL,
                      username varchar(255) NOT NULL,
                      password varchar(255) NOT NULL,
                      name varchar(255) NOT NULL,
                      description varchar(255),                    
                      created_at TIMESTAMP WITH TIME ZONE,
                      active BOOLEAN,
                      PRIMARY KEY (user_id),
                      UNIQUE(username)
                    );
                    INSERT INTO users (username, password, name, description, created_at, active) 
                    VALUES ('jsmith', 'pass', 'John Smith', 'A test user', CURRENT_TIMESTAMP, true);
                    CREATE TABLE IF NOT EXISTS logs (
                      logs_id bigserial NOT NULL,
                      description varchar(255) NOT NULL,
                      PRIMARY KEY (logs_id)
                    );
                """.trimIndent()
            ).fetch().awaitRowsUpdated()
        }
    }

    @AfterEach
    fun afterEach() {
        runSuspendIO {
            client.execute(
                """
                    DROP TABLE users;
                    DROP TABLE logs;
                """.trimIndent()
            ).fetch().awaitRowsUpdated()
        }
    }

}
