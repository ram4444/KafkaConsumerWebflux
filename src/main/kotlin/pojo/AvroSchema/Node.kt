package main.kotlin.pojo.AvroSchema

import org.apache.kafka.common.utils.Bytes
import java.math.BigDecimal
import java.util.*


data class Node(
        val id: String,
        val value: Float
    )