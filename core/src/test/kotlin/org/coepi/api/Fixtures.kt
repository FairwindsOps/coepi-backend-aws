package org.coepi.api

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.ObjectAssert
import org.coepi.api.v4.crypto.MemoType
import org.coepi.api.v4.crypto.ReportAuthorizationKey
import org.coepi.api.v4.crypto.SignedReport
import org.coepi.api.v4.crypto.createReport
import org.coepi.api.v4.dao.TCNReportRecord
import java.security.SecureRandom

object Fixtures {
     val memoType = MemoType.CoEpiV1
     val memoData = "High Fever".toByteArray()
     val j1 = 20.toUShort()
     val j2 = 40.toUShort()

     fun createSignedReport(memoDataInput: ByteArray = memoData): SignedReport {
          val rak = ReportAuthorizationKey(SecureRandom.getInstanceStrong())
          return rak.createReport(MemoType.CoEpiV1, memoDataInput, j1, j2)
     }
}
