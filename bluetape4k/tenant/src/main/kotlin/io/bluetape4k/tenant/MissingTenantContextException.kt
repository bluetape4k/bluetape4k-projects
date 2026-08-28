package io.bluetape4k.tenant

/** 필수 tenant context가 binding되지 않았음을 나타냅니다. */
class MissingTenantContextException: IllegalStateException("Tenant context is not bound")
