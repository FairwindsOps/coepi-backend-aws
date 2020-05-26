package org.coepi.api.v4.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.coepi.api.TCNClientException
import org.coepi.api.UnexpectedIntervalLengthException
import org.coepi.api.common.base64Decode
import org.coepi.api.common.toByteBuffer
import org.coepi.api.v4.Intervals
import org.coepi.api.v4.Intervals.MIN_REPORT_DATE
import org.coepi.api.v4.crypto.InvalidReportIndex
import org.coepi.api.v4.crypto.OversizeMemo
import org.coepi.api.v4.crypto.ReportVerificationFailed
import org.coepi.api.v4.crypto.SignedReport
import org.coepi.api.v4.crypto.UnknownMemoType
import org.coepi.api.v4.dao.TCNReportsDao
import org.coepi.api.v4.toInterval
import org.slf4j.LoggerFactory
import java.nio.BufferOverflowException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.time.Clock
import java.time.Instant

class TCNHttpHandler(
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
    private val reportsDao: TCNReportsDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TCNHttpHandler::class.java)

        const val INTERVAL_NUMBER_KEY = "intervalNumber"
        const val INTERVAL_LENGTH_KEY = "intervalLength"
    }

    fun getReport(parameters: Map<String, String>): HttpResponse {
        return try {
            val (maybeInterval, maybeIntervalLength) = parseQueryParameters(parameters)

            val now = clock.instant()
            val intervalNumber = maybeInterval ?: now.toInterval()
            val intervalLength = maybeIntervalLength ?: Intervals.INTERVAL_LENGTH

            logger.info("Querying reports with intervalNumber: $intervalNumber and $intervalLength")

            val reports = reportsDao.queryReports(intervalNumber, intervalLength).map { it.report }

            logger.info("Number of reports retrieved successfully: ${reports.size}")

            Ok(objectMapper.writeValueAsBytes(reports).toByteBuffer())
        } catch (ex: TCNClientException) {
            logger.info("Failed to retrieve report due to client error", ex)

            BadRequest(ex.message.orEmpty())
        } catch (ex: UnexpectedIntervalLengthException) {
            logger.info("Failed to retrieve report due to bad intervalLength used by client", ex)
            Unauthorized(ex.message.orEmpty())
        }
    }

    fun postReport(body: ByteBuffer): HttpResponse =
        try {
            val now = clock.instant()
            val reportData = body.base64Decode()

            val report = SignedReport.fromByteBuffer(reportData)
            report.verify()

            val savedReport = reportsDao.addReport(
                    reportData = reportData,
                    intervalNumber = now.toInterval(),
                    intervalLength = Intervals.INTERVAL_LENGTH,
                    timestamp = now.toEpochMilli()
            )
            logger.info("Successfully added report ${savedReport.reportId}")

            Ok()
        } catch (ex: ReportVerificationFailed) {
            logger.info("Failed to put report due to illegal TCN Signature", ex)
            Unauthorized(ex.message.orEmpty())
        } catch (ex: Exception) {
            logger.info("Failed to put report due to client error", ex)

            when(ex) {
                is UnknownMemoType, is IllegalArgumentException, is InvalidReportIndex,
                is BufferUnderflowException, is OversizeMemo -> {
                    logger.info("Bad Request. ${ex.message.orEmpty()}")
                    BadRequest(ex.message.orEmpty())
                }
                else -> throw ex
            }
        }

    private fun parseQueryParameters(
            parameters: Map<String, String>
    ): Pair<Long?, Long?> {
        var intervalNumber: Long? = null
        var intervalLength: Long? = null

        try {
            if (parameters.containsKey(INTERVAL_NUMBER_KEY)) {
                intervalNumber = parameters[INTERVAL_NUMBER_KEY]?.toLong()

                if (!parameters.containsKey(INTERVAL_LENGTH_KEY)) {
                    throw TCNClientException(
                            "$INTERVAL_LENGTH_KEY query parameter is required if " +
                                    "$INTERVAL_NUMBER_KEY is provided"
                    )
                }
                val intervalLength = parameters[INTERVAL_LENGTH_KEY]?.toLong()

                if (intervalNumber == null || intervalLength == null) {
                    throw TCNClientException("intervalNumber or intervalLength cannot be null")
                }

                if (intervalNumber <= 0 || intervalLength <= 0) {
                    throw TCNClientException("intervalNumber or intervalLength cannot be less than or equal to 0")
                }

                if (intervalLength != Intervals.INTERVAL_LENGTH) {
                    throw UnexpectedIntervalLengthException(
                            "$intervalLength is invalid. " +
                                    "Please use ${Intervals.INTERVAL_LENGTH} to calculate $INTERVAL_NUMBER_KEY"
                    )
                }

                if (Instant.ofEpochSecond(intervalNumber * intervalLength)
                                .isBefore(Intervals.MIN_REPORT_DATE)) {
                    throw TCNClientException("No reports or keys exist before $MIN_REPORT_DATE for this " +
                            "combination of $intervalNumber and $intervalLength")
                }
            }
        } catch (ex: NumberFormatException) {
            throw TCNClientException(
                    "$INTERVAL_NUMBER_KEY or $INTERVAL_LENGTH_KEY in " +
                            "illegal number format.", ex
            )
        }
        return Pair(intervalNumber, intervalLength)
    }
}
