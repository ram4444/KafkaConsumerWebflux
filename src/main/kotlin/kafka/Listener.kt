package main.kotlin.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import main.kotlin.config.KakfaConfig
import main.kotlin.pojo.AvroSchema.Node
import mu.KotlinLogging
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.*
import java.io.File

@Service
class Listener {
    var currentValue = 0F

    private val logger = KotlinLogging.logger {}
    val objectMapper = ObjectMapper().registerModule(KotlinModule())

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, GenericRecord>

    lateinit var openFlux: Flux<GenericRecord>
    var cnt = 0
    //val schema_open = Schema.Parser().parse(File("src/main/resources/avsc/node-src-open.avsc"))

    @KafkaListener(topics = ["src-node-open"], groupId = "src")
    fun listen(message: GenericRecord) {
        currentValue=message.get("val") as Float

        openFlux = Flux.generate{sink ->
            sink.next(message)
            cnt++
            if (cnt>1) {
                sink.complete()
            }
        }
    }

    fun subscriptOpenFlux():Flux<GenericRecord>{
        println("Listener subscribe")
        return openFlux
    }

    fun getLatestFromOpenFlux():Node{
        var lastElement = openFlux.blockLast().toString()
        val rtn = objectMapper.readValue<Node>(lastElement)
        return rtn
    }

}
