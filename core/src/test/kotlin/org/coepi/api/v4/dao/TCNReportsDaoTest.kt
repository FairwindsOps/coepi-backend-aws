package org.coepi.api.v4.dao

import org.coepi.api.v4.Intervals
import org.coepi.api.v4.generateIntervalForTimestamp
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Instant

@Disabled
class TCNReportsDaoTest {

    private val dao = TCNReportsDao()
    private val reportData = ByteBuffer.wrap("foobar".toByteArray(Charset.defaultCharset()))

    @Test
    fun addReport_sanity() {
        val now = Instant.now()
        val intervalNumber = generateIntervalForTimestamp(now.epochSecond)
        dao.addReport(reportData, intervalNumber, Intervals.INTERVAL_LENGTH, now.toEpochMilli())
        val reports = dao.queryReports(intervalNumber, Intervals.INTERVAL_LENGTH)
        Assertions.assertTrue(reports.isNotEmpty())
        println(reports.joinToString("\n"))
    }
}
