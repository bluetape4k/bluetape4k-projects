package io.bluetape4k.testcontainers

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer

/**
 * Docker Container의 exposed port를 지정한 port로 expose 하도록 합니다.
 * 이렇게 하지 않으면 Docker가 임의의 port number로 expose 합니다.
 *
 * @param T Server type
 * @param exposedPorts port numbers to exposed, 아무것도 지정하지 않으면 기본적인 exposedPorts 를 이용합니다.
 */
fun <T: GenericContainer<T>> GenericContainer<T>.exposeCustomPorts(vararg exposedPorts: Int) {
    val portsToExpose = exposedPorts + this.exposedPorts
    if (portsToExpose.isNotEmpty()) {
        val bindings = portsToExpose.toSet().map { PortBinding(Ports.Binding.bindPort(it), ExposedPort(it)) }
        if (bindings.isNotEmpty()) {
            withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig?.withPortBindings(bindings)
            }
        }
    }
}

/**
 * Docker Container의 exposed port를 지정한 port로 expose 하도록 합니다.
 * 이렇게 하지 않으면 Docker가 임의의 port number로 expose 합니다.
 *
 * @param T Server type
 * @param exposedPorts port numbers to exposed, 아무것도 지정하지 않으면 기본적인 exposedPorts 를 이용합니다.
 */
@JvmName("exposeCustomPortsIntArray")
fun <T: GenericContainer<T>> GenericContainer<T>.exposeCustomPorts(exposedPorts: Array<Int>) {
    val portsToExpose = exposedPorts + this.exposedPorts
    if (portsToExpose.isNotEmpty()) {
        val bindings = portsToExpose.toSet().map { PortBinding(Ports.Binding.bindPort(it), ExposedPort(it)) }
        if (bindings.isNotEmpty()) {
            withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig?.withPortBindings(bindings)
            }
        }
    }
}
