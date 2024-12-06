package org.javers.core.model

abstract class AbstractDummyAddress {
    var inheritedInt: Int? = null
}

data class DummyAddress(val city: String, val street: String? = null): AbstractDummyAddress() {

    companion object {
        @JvmField
        var staticInt: Int? = null
    }

    var kind: Kind? = null
    var networkAddress: DummyNetworkAddress? = null

    @Transient
    var someTransientField: Int? = null

    enum class Kind {
        HOME, OFFICE
    }
}

data class DummyNetworkAddress(var address: String? = null) {

    var version: Version? = null

    enum class Version { IPv4, IPv6 }
}
