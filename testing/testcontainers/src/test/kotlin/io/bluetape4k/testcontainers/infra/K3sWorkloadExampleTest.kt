package io.bluetape4k.testcontainers.infra

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

@Tag("k8s")
class K3sWorkloadExampleTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val NAMESPACE = "default"
        private const val APP_LABEL_KEY = "app"
        private const val APP_LABEL_VALUE = "k3s-workload-example"
        private const val CONFIG_MAP_NAME = "k3s-example-config"
        private const val DEPLOYMENT_NAME = "k3s-example-deployment"
        private const val SERVICE_NAME = "k3s-example-service"
        private const val SECRET_NAME = "k3s-example-secret"

        private val k3s: K3sServer by lazy { K3sServer.Launcher.k3s }
    }

    @Test
    fun `deploy ConfigMap and read updated value`() {
        k3s.kubernetesClient().use { client ->
            deleteConfigMapIfExists(client)
            val configMap = ConfigMapBuilder()
                .withNewMetadata()
                .withName(CONFIG_MAP_NAME)
                .withNamespace(NAMESPACE)
                .endMetadata()
                .addToData("feature.enabled", "true")
                .build()

            client.configMaps().inNamespace(NAMESPACE).resource(configMap).create()
            try {
                val found = client.configMaps().inNamespace(NAMESPACE).withName(CONFIG_MAP_NAME).get()

                found.shouldNotBeNull()
                found.data["feature.enabled"] shouldBeEqualTo "true"

                client.configMaps().inNamespace(NAMESPACE).withName(CONFIG_MAP_NAME).edit { current ->
                    ConfigMapBuilder(current)
                        .addToData("refresh.interval", "30s")
                        .build()
                }

                val updated = client.configMaps().inNamespace(NAMESPACE).withName(CONFIG_MAP_NAME).get()
                updated.shouldNotBeNull()
                updated.data["refresh.interval"] shouldBeEqualTo "30s"
            } finally {
                deleteConfigMapIfExists(client)
            }
        }
    }

    @Test
    fun `deploy Deployment and Service and verify a running pod backs the service`() {
        k3s.kubernetesClient().use { client ->
            deleteServiceIfExists(client)
            deleteDeploymentIfExists(client)
            val deployment = DeploymentBuilder()
                .withNewMetadata()
                .withName(DEPLOYMENT_NAME)
                .withNamespace(NAMESPACE)
                .addToLabels(APP_LABEL_KEY, APP_LABEL_VALUE)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .addToMatchLabels(APP_LABEL_KEY, APP_LABEL_VALUE)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(APP_LABEL_KEY, APP_LABEL_VALUE)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("app")
                .withImage("busybox:1.36")
                .withCommand("sh", "-c", "sleep 3600")
                .addNewPort()
                .withContainerPort(8080)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build()
            val service = ServiceBuilder()
                .withNewMetadata()
                .withName(SERVICE_NAME)
                .withNamespace(NAMESPACE)
                .endMetadata()
                .withNewSpec()
                .addToSelector(APP_LABEL_KEY, APP_LABEL_VALUE)
                .addNewPort()
                .withName("http")
                .withPort(80)
                .withTargetPort(IntOrString(8080))
                .endPort()
                .endSpec()
                .build()

            var deploymentCreated = false
            var serviceCreated = false
            try {
                client.apps().deployments().inNamespace(NAMESPACE).resource(deployment).create()
                deploymentCreated = true
                client.services().inNamespace(NAMESPACE).resource(service).create()
                serviceCreated = true

                awaitDeploymentReady(client, DEPLOYMENT_NAME).shouldBeTrue()

                val foundService = client.services().inNamespace(NAMESPACE).withName(SERVICE_NAME).get()
                foundService.shouldNotBeNull()
                foundService.spec.selector[APP_LABEL_KEY] shouldBeEqualTo APP_LABEL_VALUE

                awaitServiceEndpointReady(client, SERVICE_NAME).shouldBeTrue()
            } finally {
                if (serviceCreated) {
                    deleteServiceIfExists(client)
                }
                if (deploymentCreated) {
                    deleteDeploymentIfExists(client)
                }
            }
        }
    }

    @Test
    fun `create Secret and read decoded value`() {
        k3s.kubernetesClient().use { client ->
            deleteSecretIfExists(client)
            val token = "k3s-secret-token"
            val secret = SecretBuilder()
                .withNewMetadata()
                .withName(SECRET_NAME)
                .withNamespace(NAMESPACE)
                .endMetadata()
                .withType("Opaque")
                .addToData("api-token", Base64.getEncoder().encodeToString(token.toByteArray(UTF_8)))
                .build()

            client.secrets().inNamespace(NAMESPACE).resource(secret).create()
            try {
                val found = client.secrets().inNamespace(NAMESPACE).withName(SECRET_NAME).get()
                found.shouldNotBeNull()

                val decoded = String(Base64.getDecoder().decode(found.data["api-token"]), UTF_8)
                decoded shouldBeEqualTo token
            } finally {
                deleteSecretIfExists(client)
            }
        }
    }

    private fun awaitDeploymentReady(client: KubernetesClient, name: String): Boolean {
        repeat(90) {
            val deployment = client.apps().deployments().inNamespace(NAMESPACE).withName(name).get()
            val readyReplicas = deployment?.status?.readyReplicas ?: 0
            val availableReplicas = deployment?.status?.availableReplicas ?: 0
            if (readyReplicas >= 1 && availableReplicas >= 1) {
                return true
            }
            sleepOneSecond()
        }
        return false
    }

    private fun awaitServiceEndpointReady(client: KubernetesClient, name: String): Boolean {
        repeat(90) {
            val endpoints = client.endpoints().inNamespace(NAMESPACE).withName(name).get()
            val hasReadyAddress = endpoints?.subsets.orEmpty().any { subset ->
                subset.addresses.orEmpty().isNotEmpty()
            }
            if (hasReadyAddress) {
                return true
            }
            sleepOneSecond()
        }
        return false
    }

    private fun sleepOneSecond() {
        try {
            Thread.sleep(1_000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        }
    }

    private fun deleteConfigMapIfExists(client: KubernetesClient) {
        deleteIfExists(
            kind = "ConfigMap",
            name = CONFIG_MAP_NAME,
            delete = { client.configMaps().inNamespace(NAMESPACE).withName(CONFIG_MAP_NAME).delete() },
            exists = { client.configMaps().inNamespace(NAMESPACE).withName(CONFIG_MAP_NAME).get() != null },
        )
    }

    private fun deleteServiceIfExists(client: KubernetesClient) {
        deleteIfExists(
            kind = "Service",
            name = SERVICE_NAME,
            delete = { client.services().inNamespace(NAMESPACE).withName(SERVICE_NAME).delete() },
            exists = { client.services().inNamespace(NAMESPACE).withName(SERVICE_NAME).get() != null },
        )
    }

    private fun deleteDeploymentIfExists(client: KubernetesClient) {
        deleteIfExists(
            kind = "Deployment",
            name = DEPLOYMENT_NAME,
            delete = { client.apps().deployments().inNamespace(NAMESPACE).withName(DEPLOYMENT_NAME).delete() },
            exists = { client.apps().deployments().inNamespace(NAMESPACE).withName(DEPLOYMENT_NAME).get() != null },
        )
    }

    private fun deleteSecretIfExists(client: KubernetesClient) {
        deleteIfExists(
            kind = "Secret",
            name = SECRET_NAME,
            delete = { client.secrets().inNamespace(NAMESPACE).withName(SECRET_NAME).delete() },
            exists = { client.secrets().inNamespace(NAMESPACE).withName(SECRET_NAME).get() != null },
        )
    }

    private fun deleteIfExists(
        kind: String,
        name: String,
        delete: () -> Unit,
        exists: () -> Boolean,
    ) {
        runCatching {
            delete()
            repeat(30) {
                if (!exists()) {
                    return
                }
                sleepOneSecond()
            }
            log.warn { "K3s pre-cleanup timed out. kind=$kind, name=$name" }
        }.onFailure { e ->
            log.warn(e) { "K3s pre-cleanup failed. kind=$kind, name=$name" }
        }
    }
}
