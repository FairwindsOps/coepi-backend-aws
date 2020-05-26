package org.coepi.api.v4.dao

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import java.nio.ByteBuffer
import java.util.*

class TCNReportsDao {
    private val dynamoMapper: DynamoDBMapper

    companion object {
        fun generateReportId(intervalNumber: Long, intervalLength: Long): String =
            "$intervalNumber:$intervalLength"
    }

    init {
        val ddbClient = AmazonDynamoDBClientBuilder.standard().build()
        this.dynamoMapper = DynamoDBMapper(ddbClient)
    }

    fun addReport(reportData: ByteBuffer,
                  intervalNumber: Long,
                  intervalLength: Long,
                  timestamp: Long): TCNReportRecord {
        require(reportData.capacity() > 0) { "reportData cannot be empty" }
        require(intervalNumber > 0) { "intervalNumber should be positive" }
        require(timestamp > 0) { "timestamp needs to be positive" }
        require(intervalLength > 0) { "intervalLength needs to be positive" }

        val reportId = generateReportId(intervalNumber, intervalLength)
        val randomId = UUID.randomUUID().toString()
        val reportRecord = TCNReportRecord(reportId, randomId, timestamp, reportData.array())
        this.dynamoMapper.save(reportRecord)
        return reportRecord
    }

    fun queryReports(intervalNumber: Long, intervalLength: Long): List<TCNReportRecord> {
        require(intervalNumber > 0) { "intervalNumber should be greater than 0." }
        require(intervalLength > 0) { "intervalLength should be greater than 0." }

        val reportId = generateReportId(intervalNumber, intervalLength)
        val queryExpression = DynamoDBQueryExpression<TCNReportRecord>()
        queryExpression.keyConditionExpression = "reportId = :val1"

        val attributeValueMap = HashMap<String, AttributeValue>()
        attributeValueMap[":val1"] = AttributeValue().withS(reportId)
        queryExpression.expressionAttributeValues = attributeValueMap

        val outputList = mutableListOf<TCNReportRecord>()
        var lastEvalKey: Map<String, AttributeValue>? = null

        do {
            queryExpression.exclusiveStartKey = lastEvalKey
            val pageOutput = dynamoMapper.queryPage(TCNReportRecord::class.java, queryExpression)
            outputList.addAll(pageOutput.results)
            lastEvalKey = pageOutput.lastEvaluatedKey
        } while (lastEvalKey != null)

        return outputList
    }
}