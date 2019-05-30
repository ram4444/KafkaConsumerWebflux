package main.kotlin.controller

import graphql.ExecutionResult
import graphql.schema.DataFetcher
import graphql.schema.StaticDataFetcher
import main.kotlin.graphql.GraphQLHandler
import main.kotlin.graphql.GraphQLRequest
import main.kotlin.kafka.Listener
import main.kotlin.service.WebfluxService
import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.*


import reactor.core.scheduler.Schedulers
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct

@RestController
class GraphQLController() {

    @Autowired
    val webFluxDBService: WebfluxService = WebfluxService()

    @Autowired
    val listenerService: Listener = Listener()

    private val topicprocessor = TopicProcessor.share<GenericRecord>("shared", 1024)

    private val logger = KotlinLogging.logger {}

    //Initiate schema from somewhere
    val schema ="""
            type Query{
                query_funca: Int
            }
            type Subscription{
                sub_listener: String
            }
            type GenericRecord{
                id: String
                val: String
            }
            schema {
              query: Query
              subscription: Subscription
            }"""

    lateinit var fetchers: Map<String, List<Pair<String, DataFetcher<out Any>>>>
    lateinit var handler:GraphQLHandler

    @PostConstruct
    fun init() {

        //initialize Fetchers
        fetchers = mapOf(
                "Subscription" to
                        listOf(
                                "sub_listener" to DataFetcher { listenerService.getLatestFromOpenFlux().toMono()}
                        )
        )

        handler = GraphQLHandler(schema, fetchers)
    }

    @RequestMapping("/")
    suspend fun pingcheck():String {
        println("ping")
        logger.debug { "Debugging" }
        return "success"
    }
    @CrossOrigin(origins = arrayOf("http://localhost:3000"))
    @PostMapping("/graphql", produces = arrayOf(MediaType.APPLICATION_STREAM_JSON_VALUE))
    fun executeGraphQL(@RequestBody request:GraphQLRequest): Flux<ExecutionResult> {
        val result = handler.execute_subscription(request.query, request.params, request.operationName, ctx = null)
        val resultStream: Publisher<ExecutionResult> = result.getData()
        //var rtn = resultStream.toFlux().publish()
        val subscriptionRef = AtomicReference<Subscription>()
        class OvrSubscriber: Subscriber<ExecutionResult> {

            override fun onSubscribe(s: Subscription) {
                println("OvrSubscriber detect subscription")
                subscriptionRef.set(s);
                s.request(1);
            }

            override fun onNext(er: ExecutionResult) {
                //
                // ask the publisher for one more item please
                //
                println("When on next")
                println("When on next:"+er.getData<ExecutionResult>())
                //session.send(topicprocessor.map { ev -> session.textMessage(er.getData<ExecutionResult>().toString()) })
                //rtn.mergeWith(topicprocessor.map { ev ->er.getData<ExecutionResult>() })
                //er.getData<ExecutionResult>().toString()
                subscriptionRef.get().request(1)
            }

            override fun onError(t: Throwable) {
                //
                // The upstream publishing data source has encountered an error
                // and the subscription is now terminated.  Real production code needs
                // to decide on a error handling strategy.
                //
                println("error")
            }

            override fun onComplete() {
                //
                // the subscription has completed.  There is not more data
                //
                println("completed")
            }

        }
        resultStream.subscribe(OvrSubscriber())

        return resultStream.toFlux()
    }


}
