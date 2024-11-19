package io.bluetape4k.math.ml.clustering

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.model.Gender
import io.bluetape4k.math.model.Patient
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ClusteringTest {

    companion object: KLogging()

    private val patients = listOf(
        Patient("John", "Simone", Gender.MALE, LocalDate.of(1989, 1, 7), 4500),
        Patient("Sarah", "Marley", Gender.FEMALE, LocalDate.of(1970, 2, 5), 6700),
        Patient("Jessica", "Arnold", Gender.FEMALE, LocalDate.of(1980, 3, 9), 3400),
        Patient("Sam", "Beasley", Gender.MALE, LocalDate.of(1981, 4, 17), 8800),
        Patient("Dan", "Forney", Gender.MALE, LocalDate.of(1985, 9, 13), 5400),
        Patient("Lauren", "Michaels", Gender.FEMALE, LocalDate.of(1975, 8, 21), 5000),
        Patient("Michael", "Erlich", Gender.MALE, LocalDate.of(1985, 12, 17), 4100),
        Patient("Jason", "Miles", Gender.MALE, LocalDate.of(1991, 11, 1), 3900),
        Patient("Rebekah", "Earley", Gender.FEMALE, LocalDate.of(1985, 2, 18), 4600),
        Patient("James", "Larson", Gender.MALE, LocalDate.of(1974, 4, 10), 5100),
        Patient("Dan", "Ulrech", Gender.MALE, LocalDate.of(1991, 7, 11), 6000),
        Patient("Heather", "Eisner", Gender.FEMALE, LocalDate.of(1994, 3, 6), 6000),
        Patient("Jasper", "Martin", Gender.MALE, LocalDate.of(1971, 7, 1), 6000)
    )

    @Test
    fun `kMeans++ cluster`() {
        val clusters = patients.kMeansCluster(
            k = 3,
            maxIterations = -1,
            xSelector = { it.age.toDouble() },
            ySelector = { it.whiteBloodCellCount.toDouble() }
        )

        clusters.forEachIndexed { index, centroid ->
            println("CENTROID:$index")
            centroid.points.forEach {
                println("\t$it")
            }
        }
        clusters.size shouldBeEqualTo 3
    }

    @Test
    fun `fuzzy kMeans cluster`() {
        val clusters = patients.fuzzyKMeansCluster(
            k = 3,
            fuzziness = 10.0,
            xSelector = { it.age.toDouble() },
            ySelector = { it.whiteBloodCellCount.toDouble() }
        )

        clusters.forEachIndexed { index, centroid ->
            println("CENTROID:$index")
            centroid.points.forEach {
                println("\t$it")
            }
        }
        clusters.size shouldBeEqualTo 3
    }

    @Test
    fun `Multi kMeans cluster`() {
        val clusters = patients.multiKMeansCluster(
            k = 3,
            maxIterations = -1,
            numTrials = 50,
            xSelector = { it.age.toDouble() },
            ySelector = { it.whiteBloodCellCount.toDouble() }
        )

        clusters.forEachIndexed { index, centroid ->
            println("CENTROID:$index")
            centroid.points.forEach {
                println("\t$it")
            }
        }
        clusters.size shouldBeEqualTo 3
    }

    @Test
    fun `dbscan cluster`() {
        val clusters = patients.dbScanCluster(
            maximumRadius = 250.0,
            minPoints = 1,
            xSelector = { it.age.toDouble() },
            ySelector = { it.whiteBloodCellCount.toDouble() }
        )

        clusters.forEachIndexed { index, centroid ->
            println("CENTROID:$index")
            centroid.points.forEach {
                println("\t$it")
            }
        }
        clusters.size shouldBeEqualTo 4
    }
}
