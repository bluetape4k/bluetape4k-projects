package io.bluetape4k.spring.cassandra.model

import org.springframework.data.domain.Persistable
import org.springframework.data.util.ProxyUtils
import java.io.Serializable

/**
 * [Persistable]을 구현하는 최상휘 추상화 클래스입니다.
 *
 * @param PK entity의 primary key 수형
 */
abstract class AbstractCassandraPersistable<PK: Any>: Persistable<PK>, Serializable {

    abstract fun setId(id: PK)

    override fun isNew(): Boolean {
        return id == null
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (this.javaClass != ProxyUtils.getUserClass(other)) {
            return false
        }
        return other is AbstractCassandraPersistable<*> && id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: System.identityHashCode(this)
    }

    override fun toString(): String {
        return "Entity of type ${javaClass.simpleName} with id: $id"
    }
}
