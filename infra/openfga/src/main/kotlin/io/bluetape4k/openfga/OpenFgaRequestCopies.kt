package io.bluetape4k.openfga

import dev.openfga.sdk.api.model.BatchCheckRequest
import dev.openfga.sdk.api.model.CheckRequest
import dev.openfga.sdk.api.model.ReadRequest
import dev.openfga.sdk.api.model.WriteRequest

internal fun CheckRequest.withAuthorizationModel(scope: OpenFgaScope): CheckRequest =
    CheckRequest(trace)
        .tupleKey(tupleKey)
        .contextualTuples(contextualTuples)
        .authorizationModelId(scope.requireAuthorizationModelId())
        .context(context)
        .consistency(consistency)

internal fun BatchCheckRequest.withAuthorizationModel(scope: OpenFgaScope): BatchCheckRequest =
    BatchCheckRequest()
        .checks(checks)
        .authorizationModelId(scope.requireAuthorizationModelId())
        .consistency(consistency)

internal fun WriteRequest.withAuthorizationModel(scope: OpenFgaScope): WriteRequest =
    WriteRequest()
        .writes(writes)
        .deletes(deletes)
        .authorizationModelId(scope.requireAuthorizationModelId())

internal fun ReadRequest.withPage(pageSize: Int?, continuationToken: String?): ReadRequest =
    ReadRequest()
        .tupleKey(tupleKey)
        .pageSize(pageSize)
        .continuationToken(continuationToken)
        .consistency(consistency)
