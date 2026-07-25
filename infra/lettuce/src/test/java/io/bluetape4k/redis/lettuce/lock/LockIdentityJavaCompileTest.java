package io.bluetape4k.redis.lettuce.lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LockIdentityJavaCompileTest {

    @Test
    void identityFactoriesAreStaticCallableFromJava() {
        assertNotNull(LockOwnerId.random());
        assertNotNull(LockOwnerId.from("owner"));
        assertNotNull(LockRequestId.random());
        assertNotNull(LockRequestId.from("request"));
    }
}
