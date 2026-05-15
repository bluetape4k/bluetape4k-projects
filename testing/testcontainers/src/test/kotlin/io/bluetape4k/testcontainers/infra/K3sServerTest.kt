package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("k8s")
class K3sServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val k3s: K3sServer by lazy { K3sServer.Launcher.k3s }
    }

    @AfterEach
    fun cleanup() {
        runCatching {
            k3s.kubernetesClient().use { it.namespaces().withName("test-ns").delete() }
        }
        runCatching {
            k3s.kubernetesClient().use { it.leases().inNamespace("default").withName("test-lease").delete() }
        }
    }

    @Test
    fun `launch k3s server`() {
        k3s.isRunning.shouldBeTrue()
    }

    @Test
    fun `api server reachable at mapped port`() {
        (k3s.port in 1..65535).shouldBeTrue()
        k3s.port shouldBeEqualTo k3s.getMappedPort(K3sServer.API_PORT)
    }

    @Test
    fun `kubeConfig returns valid yaml with patched server url`() {
        val yaml = k3s.kubeConfig
        yaml.shouldNotBeNull()
        yaml.contains("https://").shouldBeTrue()         // protocol is HTTPS
        yaml.contains(":${k3s.port}").shouldBeTrue()    // mapped port is patched in
    }

    @Test
    fun `kubernetesClient can list nodes with at least one ready`() {
        k3s.kubernetesClient().use { client ->
            val nodes = client.nodes().list().items
            (nodes.size >= 1).shouldBeTrue()
            val allReady = nodes.all { node ->
                node.status.conditions.any { it.type == "Ready" && it.status == "True" }
            }
            allReady.shouldBeTrue()
        }
    }

    @Test
    fun `create and delete namespace`() {
        k3s.kubernetesClient().use { client ->
            val ns = NamespaceBuilder()
                .withNewMetadata().withName("test-ns").endMetadata()
                .build()
            client.namespaces().resource(ns).create()
            try {
                client.namespaces().withName("test-ns").get().shouldNotBeNull()
            } finally {
                client.namespaces().withName("test-ns").delete()
            }
        }
    }

    @Test
    fun `create get and delete Lease object`() {
        k3s.kubernetesClient().use { client ->
            val lease = LeaseBuilder()
                .withNewMetadata()
                .withName("test-lease")
                .withNamespace("default")
                .endMetadata()
                .build()
            client.leases().inNamespace("default").resource(lease).create()
            try {
                client.leases().inNamespace("default").withName("test-lease").get().shouldNotBeNull()
            } finally {
                client.leases().inNamespace("default").withName("test-lease").delete()
            }
        }
    }
}
