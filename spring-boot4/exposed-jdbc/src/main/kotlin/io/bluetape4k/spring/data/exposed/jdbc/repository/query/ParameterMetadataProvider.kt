package io.bluetape4k.spring.data.exposed.jdbc.repository.query

import io.bluetape4k.logging.KLogging
import org.springframework.data.repository.query.ParameterAccessor
import org.springframework.data.repository.query.Parameters
import org.springframework.data.repository.query.ParametersParameterAccessor

/**
 * 메서드 파라미터에서 [ParameterAccessor]를 제공합니다.
 * Spring Data 4.x [AbstractQueryCreator]는 [ParameterAccessor]를 필요로 합니다.
 */
class ParameterMetadataProvider(
    val accessor: ParameterAccessor,
) {

    companion object: KLogging() {
        fun of(parameters: Parameters<*, *>, values: Array<Any?>): ParameterMetadataProvider =
            ParameterMetadataProvider(ParametersParameterAccessor(parameters, values))
    }
}
