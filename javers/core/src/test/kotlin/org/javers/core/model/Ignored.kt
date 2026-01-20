package org.javers.core.model

import org.javers.core.metamodel.annotation.DiffIgnore
import java.io.Serializable

@DiffIgnore
open class DummyIgnoredType: Serializable

open class IgnoredSubType: DummyIgnoredType()
