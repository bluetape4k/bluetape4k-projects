package io.bluetape4k.testcontainers

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer

internal fun resolvePortBindings(ports: Iterable<Int>): List<PortBinding> {
    val uniquePorts = ports.distinct()
    require(uniquePorts.all { it > 0 }) { "All ports must be positive numbers." }
    return uniquePorts.map { PortBinding(Ports.Binding.bindPort(it), ExposedPort(it)) }
}

/**
 * Docker Container의 exposed port를 지정한 port로 expose 하도록 합니다.
 * 이렇게 하지 않으면 Docker가 임의의 port number로 expose 합니다.
 *
 * ## 동작/계약
 * - 전달한 포트와 컨테이너 기존 exposed port를 합쳐 중복 제거 후 바인딩합니다.
 * - 포트 값이 0 이하이면 [IllegalArgumentException]이 발생합니다.
 * - 컨테이너 설정(hostConfig)에 포트 바인딩을 추가하며, hostConfig가 비어 있으면 새로 생성해 적용합니다.
 * - 컨테이너 시작 전 호출을 권장합니다.
 *
 * ```kotlin
 * val container = GenericContainer("redis:7").withExposedPorts(6379)
 * container.exposeCustomPorts(6379)
 * // host의 6379 포트로 고정 매핑 시도
 * ```
 *
 * @param T Server type
 * @param exposedPorts port numbers to exposed, 아무것도 지정하지 않으면 기본적인 exposedPorts 를 이용합니다.
 */
fun <T: GenericContainer<T>> GenericContainer<T>.exposeCustomPorts(vararg exposedPorts: Int) {
    val bindings = resolvePortBindings(exposedPorts.asIterable() + this.exposedPorts)
    if (bindings.isNotEmpty()) {
        withCreateContainerCmdModifier { cmd ->
            val hostConfig = cmd.hostConfig ?: HostConfig.newHostConfig()
            hostConfig.withPortBindings(bindings)
            cmd.withHostConfig(hostConfig)
        }
    }
}

/**
 * Docker Container의 exposed port를 지정한 port로 expose 하도록 합니다.
 * 이렇게 하지 않으면 Docker가 임의의 port number로 expose 합니다.
 *
 * ## 동작/계약
 * - [Array] 오버로드로 전달된 포트를 기존 exposed port와 합쳐 바인딩합니다.
 * - 포트 값 검증/중복 제거 규칙은 vararg 버전과 동일합니다.
 * - 수신 컨테이너 설정을 변경하며 새 컨테이너를 생성하지 않습니다.
 * - hostConfig가 비어 있으면 내부에서 생성하여 포트 바인딩을 보장합니다.
 *
 * ```kotlin
 * val ports = arrayOf(5432, 8080)
 * container.exposeCustomPorts(ports)
 * // 지정 포트 고정 매핑 시도
 * ```
 *
 * @param T Server type
 * @param exposedPorts port numbers to exposed, 아무것도 지정하지 않으면 기본적인 exposedPorts 를 이용합니다.
 */
@JvmName("exposeCustomPortsIntArray")
fun <T: GenericContainer<T>> GenericContainer<T>.exposeCustomPorts(exposedPorts: Array<Int>) {
    val bindings = resolvePortBindings(exposedPorts.asIterable() + this.exposedPorts)
    if (bindings.isNotEmpty()) {
        withCreateContainerCmdModifier { cmd ->
            val hostConfig = cmd.hostConfig ?: HostConfig.newHostConfig()
            hostConfig.withPortBindings(bindings)
            cmd.withHostConfig(hostConfig)
        }
    }
}
