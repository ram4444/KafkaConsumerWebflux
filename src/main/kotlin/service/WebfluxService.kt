package main.kotlin.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class WebfluxService {
    //fun fetchTestEntity(name: String): Flux<TestEntity> = fetch("testentity/$name").bodyToFlux(TestEntity::class.java)

    //fun fetchPosts(): Flux<Post> = fetch("/posts").bodyToFlux(Post::class.java)
}