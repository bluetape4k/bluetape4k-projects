package org.javers.core.model

import org.javers.core.metamodel.annotation.DiffIgnore

@DiffIgnore
open class DummyIgnoredType

open class IgnoredSubType: DummyIgnoredType()