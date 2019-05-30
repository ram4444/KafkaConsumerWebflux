package main.kotlin.config

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetcher
import graphql.schema.StaticDataFetcher
import main.kotlin.graphql.GraphQLHandler
import main.kotlin.graphql.GraphQLRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import main.kotlin.handler.WSGraphQLHandler
import main.kotlin.service.WebfluxService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.file.dsl.Files
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer


@Configuration
@EnableWebFlux
@ComponentScan(value = ["org.zupzup.kotlinwebfluxdemo"])
class WebSocketConfig(
        val graphQLHandler: WSGraphQLHandler
) {
    @Bean
    fun websocketHandlerAdapter() = WebSocketHandlerAdapter()

    @Bean
    fun handlerMapping() : HandlerMapping {
        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.urlMap = mapOf(
                "/ws/graphql" to graphQLHandler
        )
        handlerMapping.order = 1
        return handlerMapping
    }


    @Autowired
    val webFluxDBService: WebfluxService = WebfluxService()

    var testrtn : Flux<Int> = Flux.just(42)
    //Initiate schema from somewhere
    val schema ="""
            type Query{
                query_funca: Int
            }
            type Subscription{
                query_func1: Int
                query_func2: [TestEntity]
            }
            type TestEntity{
                id: String
                name: String
            }"""

    //initialize Fetchers
    val fetchers = mapOf(
        "Subscription" to
        listOf(
        "query_func1" to StaticDataFetcher(testrtn)

        )
    )

    val handler: GraphQLHandler = GraphQLHandler(schema, fetchers)

}

data class FileEvent(val sessionId: String, val path:String)