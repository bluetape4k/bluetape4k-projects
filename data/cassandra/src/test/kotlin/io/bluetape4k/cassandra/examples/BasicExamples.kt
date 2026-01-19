package io.bluetape4k.cassandra.examples

import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.CassandraAdmin
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class BasicExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val DROP_TABLE_SONG = "DROP TABLE IF EXISTS songs;"

        private val CREATE_TABLE_SONG =
            """
            |CREATE TABLE IF NOT EXISTS songs (
            |id uuid PRIMARY KEY,
            |title text,
            |album text,
            |artist text,
            |tags set<text>,
            |data blob
            |);
            """.trimMargin()

        private const val DROP_TABLE_PLAYLISTS = "DROP TABLE IF EXISTS playlists;"

        private val CREATE_TABLE_PLAYLISTS =
            """
            |CREATE TABLE IF NOT EXISTS playlists (
            |id uuid,
            |title text,
            |album text,
            |artist text,
            |song_id uuid,
            |PRIMARY KEY (id, title, album, artist)
            |);
            """.trimMargin()

        private const val SongId = "756716f7-2e54-4715-9f00-91dcbea6cf50"
        private const val PlayListId = "2cc9ccb7-6221-4ccb-8387-f22b6a1b354d"

        private val INSERT_SONGS =
            """
            |INSERT INTO songs (id, title, album, artist, tags) 
            |VALUES (
            |    $SongId,
            |    'La Petite Tonkinoise',
            |    'Bye Bye Blackbird',
            |    'Joséphine Baker',
            |    {'jazz', '2013'}
            |)
            """.trimMargin()


        private val INSERT_PLAYLISTS =
            """
            |INSERT INTO playlists (id, song_id, title, album, artist) 
            |VALUES (
            |   $PlayListId,
            |   $SongId,
            |   'La Petite Tonkinoise',
            |   'Bye Bye Blackbird',
            |   'Joséphine Baker'
            |)
            """.trimMargin()

        private fun querySelectPlaylists(playListId: String): String =
            """
            SELECT * FROM examples.playlists WHERE id = $playListId
            """.trimIndent()
    }

    @Test
    fun `read scylla version`() {
        val version = CassandraAdmin.getReleaseVersion(session)
        version.shouldNotBeNull()
    }

    @Test
    fun `read topology and schema metadata`() {
        log.info { "Connected session=${session.name}" }

        val metadata = session.metadata

        // Node
        metadata.nodes.values.forEach { node ->
            println("Datacenter: ${node.datacenter}, Host: ${node.endPoint}, Rack: ${node.rack}")
        }

        // Keyspace
        metadata.keyspaces.values.forEach { keyspace ->
            keyspace.tables.values.forEach { table ->
                println("Keyspace: ${keyspace.name}, Table: ${table.name}")
            }
        }
    }

    @Test
    fun `테이블생성 데이터 입력 및 조회`() {
        createSchema(session)
        loadData(session)
        queryPlaylists(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute(DROP_TABLE_SONG)
        session.execute(DROP_TABLE_PLAYLISTS)
        session.execute(CREATE_TABLE_SONG).wasApplied().shouldBeTrue()
        session.execute(CREATE_TABLE_PLAYLISTS).wasApplied().shouldBeTrue()
    }

    private fun loadData(session: CqlSession) {
        session.execute(INSERT_SONGS).wasApplied().shouldBeTrue()
        session.execute(INSERT_PLAYLISTS).wasApplied().shouldBeTrue()
    }

    private fun queryPlaylists(session: CqlSession) {
        val query = querySelectPlaylists(PlayListId)
        val rs = session.execute(query)
        rs.wasApplied().shouldBeTrue()
        rs.availableWithoutFetching shouldBeEqualTo 1

        rs.forEach { row ->
            val title = row.getString("title")
            val album = row.getString("album")
            val artist = row.getString("artist")
            log.info { "title=$title, album=$album, artist=$artist" }
        }
    }
}
