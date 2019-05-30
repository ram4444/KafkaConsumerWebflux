package main.kotlin.kafka

import main.kotlin.config.KakfaConfig
import mu.KotlinLogging
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.*
import java.io.File
import java.lang.Math.abs
import java.lang.System.out
import java.util.*

@Service
class Listener {
    var currentValue = 0F

    private val logger = KotlinLogging.logger {}

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, GenericRecord>

    lateinit var openFlux: Flux<GenericRecord>
    var openMono: Mono<GenericRecord> = Mono.empty()
    var cnt = 0
    val schema_open = Schema.Parser().parse(File("src/main/resources/avsc/node-src-open.avsc"))

    @KafkaListener(topics = ["src-node-open"], groupId = "src")
    fun listen(message: GenericRecord) {
        currentValue=message.get("val") as Float
        //logger.debug { currentValue }
        //println(currentValue)
        //openMono = currentValue.toMono()
        //println(message)

        openFlux = Flux.generate{sink ->
            sink.next(message)
            cnt++
            if (cnt>1) {
                sink.complete()
            }
        }
        //openMono = message.toMono()
        //Flux.generate{sink -> sink.next(message)}
        //openFlux.mergeWith(message.toMono())
        //openFlux.doOnNext(message)
    }

    fun subscriptOpenMono():Flux<GenericRecord>{
        println("Listener subscribe")
        //openFlux.subscribe(out::println)
        return openFlux
    }

    fun getLatestFromOpenFlux():String{
        var rtn = openFlux.blockLast().toString()
        return rtn
    }

}
