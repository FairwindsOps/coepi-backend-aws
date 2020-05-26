package org.coepi.api.v4.http

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.coepi.api.Fixtures.createSignedReport
import org.coepi.api.Fixtures.j1
import org.coepi.api.Fixtures.j2
import org.coepi.api.Fixtures.memoData
import org.coepi.api.Fixtures.memoType
import org.coepi.api.v4.crypto.ReportVerificationFailed
import org.coepi.api.v4.crypto.SignedReport
import org.coepi.api.v4.dao.TCNReportsDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.time.Clock
import java.util.*

class SignedReportTest {

    val dao = mockk<TCNReportsDao>(relaxed = true)

    val tcnHttpHandler = TCNHttpHandler(
        objectMapper = ObjectMapper(),
        clock = Clock.systemUTC(),
        reportsDao = dao
    )

    @Test
    fun testPost_validSignature() {
        val signedReport = createSignedReport()
        val body = Base64.getEncoder().encode(signedReport.toByteArray())
        val response = tcnHttpHandler.postReport(ByteBuffer.wrap(body))
        assertEquals(200, response.status)
    }

    @Test
    fun testPost_invalidSignature() {
        val signedReport1 = createSignedReport()
        val signedReport2 = createSignedReport("foo".toByteArray())

        val newReport = SignedReport(signedReport1.report, signedReport2.signature)
        val body = Base64.getEncoder().encode(newReport.toByteArray())
        val response = tcnHttpHandler.postReport(ByteBuffer.wrap(body))
        assertEquals(401, response.status)
    }

    @Test
    fun testPost_invalidReportData_tooFewBytes() {
        val body = Base64.getEncoder().encode("foobar".toByteArray())
        val response = tcnHttpHandler.postReport(ByteBuffer.wrap(body))
        assertEquals(400, response.status)
    }

    @Test
    fun testPost_invalidReportData_invalidPrefix() {
        val signedReport = createSignedReport()
        val extra = "foo"
        val buff = ByteBuffer.allocate(signedReport.toByteArray().size + extra.length)
        buff.put(extra.toByteArray())
        buff.put(signedReport.toByteArray())
        val body = Base64.getEncoder().encode(buff)
        val response = tcnHttpHandler.postReport(body)

        assertEquals(400, response.status)
    }

    @Test
    fun testPost_invalidReportData_invalidSuffix() {
        val signedReport = createSignedReport()
        val extra = "foo"
        val buff = ByteBuffer.allocate(signedReport.toByteArray().size + extra.length)
        buff.put(signedReport.toByteArray())
        buff.put(extra.toByteArray())
        val body = Base64.getEncoder().encode(buff)
        val response = tcnHttpHandler.postReport(body)

        assertEquals(400, response.status)
    }

    @Test
    fun testSignatureVerificationSuccessful() {
        val signedReport = createSignedReport()
        val report = signedReport.verify()
        assertEquals(memoData, report.memoData)
        assertEquals(memoType, report.memoType)
        assertEquals(j1, report.j1.uShort)
        assertEquals(j2, report.j2.uShort)
    }

    @Test
    fun testInvalidSignature() {
        val signedReport1 = createSignedReport()
        val signedReport2 = createSignedReport("foo".toByteArray())

        val newReport = SignedReport(signedReport1.report, signedReport2.signature)
        assertThrows(ReportVerificationFailed::class.java) {
            newReport.verify()
        }
    }
}