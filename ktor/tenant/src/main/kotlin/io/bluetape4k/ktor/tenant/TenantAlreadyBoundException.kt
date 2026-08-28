package io.bluetape4k.ktor.tenant

/** 하나의 `ApplicationCall`에 tenant를 두 번 binding하려 할 때 발생합니다. */
class TenantAlreadyBoundException:
    IllegalStateException("Tenant context is already bound to this call")
