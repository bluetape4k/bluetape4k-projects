package io.bluetape4k.tenant

/**
 * 동기 MVC/Servlet 호출을 위한 `ThreadLocal` 기반 [TenantContext]입니다.
 *
 * application singleton으로 생성하고 [withTenant] lexical scope만 사용해야 합니다.
 */
class ThreadLocalTenantContext: TenantContext {

    private val currentTenant = ThreadLocal<TenantId>()

    override fun currentOrNull(): TenantId? = currentTenant.get()

    override fun requireCurrent(): TenantId =
        currentOrNull() ?: throw MissingTenantContextException()

    override fun <T> withTenant(tenantId: TenantId, block: () -> T): T {
        val previous = currentTenant.get()
        currentTenant.set(tenantId)

        return try {
            block()
        } finally {
            if (previous == null) {
                currentTenant.remove()
            } else {
                currentTenant.set(previous)
            }
        }
    }
}
