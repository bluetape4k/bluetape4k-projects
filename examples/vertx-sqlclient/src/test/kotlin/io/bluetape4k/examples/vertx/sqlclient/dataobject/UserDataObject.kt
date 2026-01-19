package io.bluetape4k.examples.vertx.sqlclient.dataobject

import io.vertx.codegen.annotations.DataObject
import io.vertx.codegen.format.SnakeCase
import io.vertx.sqlclient.templates.annotations.RowMapped
import java.io.Serializable

/**
 * Mapping with Vert.x data objects
 *
 * 1. `io.vertx:vertx-codegen:4.3.1:processor` 를 dependency 에 추가한다
 * ```
 * compileOnly(Libs.vertx_codegen)
 * kapt(Libs.vertx_codegen)
 * kaptTest(Libs.vertx_codegen)
 * ```
 *
 * 2. module에 package-info.java 를 추가한다
 *
 * [package-info.java](src/main/java/io/bluetape4k/workshop/sqlclient/package-info.java
 *
 * ```java
 * @ModuleGen(name = "sql-client-examples", groupPackage = "io.bluetape4k.workshop.sqlclient")
 * package io.bluetape4k.workshop.sqlclient;
 *
 * import io.vertx.codegen.annotations.ModuleGen;
 * ```
 *
 * [Mapping with Vert.x data objects](https://vertx.io/docs/vertx-sql-client-templates/java/#_mapping_with_vert_x_data_objects)
 */
@DataObject
@RowMapped(formatter = SnakeCase::class)

// FIXME: ParametersMapped 는 제대로 kapt 로 생성되지 않습니다. JSON constructor 문제로 봐서는 Kotlin 이어서 생긴 문제
// 차라리 USER_TUPLE_MAPPER 처럼 사용하는 게 나을 듯 하다
// @ParametersMapped(formatter = LowerCamelCase::class)
open class UserDataObject: Serializable {
    var id: Long? = null
    var firstName: String? = null
    var lastName: String? = null
}
