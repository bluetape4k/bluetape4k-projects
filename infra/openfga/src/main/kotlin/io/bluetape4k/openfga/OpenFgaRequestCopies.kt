package io.bluetape4k.openfga

import dev.openfga.sdk.api.model.BatchCheckRequest
import dev.openfga.sdk.api.model.CheckRequest
import dev.openfga.sdk.api.model.ReadRequest
import dev.openfga.sdk.api.model.WriteRequest

internal fun CheckRequest.withAuthorizationModel(scope: OpenFgaScope): CheckRequest =
    CheckRequest(getTrace())
        .tupleKey(getTupleKey())
        .contextualTuples(getContextualTuples())
        .authorizationModelId(scope.requireAuthorizationModelId())
        .context(getContext())
        .consistency(getConsistency())

internal fun BatchCheckRequest.withAuthorizationModel(scope: OpenFgaScope): BatchCheckRequest =
    BatchCheckRequest()
        .checks(getChecks())
        .authorizationModelId(scope.requireAuthorizationModelId())
        .consistency(getConsistency())

internal fun WriteRequest.withAuthorizationModel(scope: OpenFgaScope): WriteRequest =
    WriteRequest()
        .writes(getWrites())
        .deletes(getDeletes())
        .authorizationModelId(scope.requireAuthorizationModelId())

internal fun ReadRequest.withPage(pageSize: Int?, continuationToken: String?): ReadRequest =
    ReadRequest()
        .tupleKey(getTupleKey())
        .pageSize(pageSize)
        .continuationToken(continuationToken)
        .consistency(getConsistency())
