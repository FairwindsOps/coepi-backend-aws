package org.coepi.api.v4

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import org.coepi.api.common.decodeToString
import org.coepi.api.common.toByteBuffer
import org.coepi.api.v4.dao.TCNReportsDao
import org.coepi.api.v4.http.HttpResponse
import org.coepi.api.v4.http.TCNHttpHandler
import org.slf4j.LoggerFactory

class TCNCloudAPIHandler(
    private val handler: TCNHttpHandler
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(TCNCloudAPIHandler::class.java)
    }

    /**
     * Zero-arg constructor to initialize the class in Lambda.
     */
    constructor() : this(
        handler = TCNHttpHandler(
            objectMapper = ObjectMapper(),
            clock = Clock.systemUTC(),
            reportsDao = TCNReportsDao()
        )
    )

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        logger.info("Processing request: ${context.awsRequestId}. " +
                "Query params: ${input.queryStringParameters}. " +
                "Body: ${input.body}")
        try {
            if (input.httpMethod == "GET") {
                logger.info("Handling GET Request for :${input.path}. ReqId: ${context.awsRequestId}")
                return handleGetReport(input)
            }
            logger.info("Handling POST Request for :${input.path}. ReqId: ${context.awsRequestId}")
            return handlePostReport(input)
        } catch (ex: Exception) {
            logger.info("Failed to serve request: ${context.awsRequestId}. Cause: ${ex.message}")
            return APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("CoEpi Service Internal Failure")
        }
    }

    fun handleGetReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent =
        handler
            .getReport(input.queryStringParameters ?: emptyMap())
            .toAPIGatewayProxyResponseEvent()

    fun handlePostReport(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent =
        handler
            .postReport(input.body.toByteBuffer())
            .toAPIGatewayProxyResponseEvent()
}

private fun HttpResponse.toAPIGatewayProxyResponseEvent(): APIGatewayProxyResponseEvent =
    APIGatewayProxyResponseEvent()
        .withStatusCode(status)
        .withBody(body?.decodeToString())