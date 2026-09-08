package io.bluetape4k.science.shapefile

import io.bluetape4k.science.projection.transform
import org.geotools.api.data.Transaction as GeoToolsTransaction
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.io.File
import java.io.Serializable
import java.nio.file.Path

/** EPSG:3857 파일을 만들어 reader와 DB 임포트 회귀 테스트에서 재사용합니다. */
internal fun createWebMercatorPointShapefile(dir: Path, lon: Double, lat: Double): File {
    val shpFile = dir.resolve("web-mercator-point.shp").toFile()
    val sourceCrs = CRS.decode("EPSG:3857", true)
    val (x, y) = transform("EPSG:4326", "EPSG:3857", lon, lat)

    val typeBuilder = SimpleFeatureTypeBuilder().apply {
        setName("web_mercator_points")
        setCRS(sourceCrs)
        add("the_geom", Point::class.java)
        add("NAME", String::class.java)
    }
    val featureType = typeBuilder.buildFeatureType()
    val params = mapOf<String, Serializable>("url" to shpFile.toURI().toURL())

    val dataStore = ShapefileDataStoreFactory().createNewDataStore(params) as ShapefileDataStore
    try {
        dataStore.createSchema(featureType)
        dataStore.forceSchemaCRS(sourceCrs)
        dataStore.getFeatureWriterAppend(GeoToolsTransaction.AUTO_COMMIT).use { writer ->
            val feature = writer.next()
            feature.setAttribute("the_geom", GeometryFactory().createPoint(Coordinate(x, y)))
            feature.setAttribute("NAME", "Seoul")
            writer.write()
        }
    } finally {
        dataStore.dispose()
    }
    return shpFile
}

