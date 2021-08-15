package de.coli

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.sessions.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()


    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = myRealm
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build())

            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }


    routing {
        get("/") {
            // set custom header
            call.response.headers.append("Token", "123123123123")

            // show body data
            call.respondText("HELLO WORLD!!", contentType = ContentType.Text.Plain)
        }

        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }

        route("/user") {
            get {
                val user = User(
                    username = "username",
                    password = "password"
                )
                call.respond(Gson().toJson(user))
            }

            // read the json from the body and put this into the dataclass
            post {
                val user = call.receive<User>()
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("username", user.username)
                    .withClaim("password", user.password)
                    .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                    .sign(Algorithm.HMAC256(secret))
                call.respond(hashMapOf("token" to token))
            }
        }

        // need a valid token to get to this page
        authenticate("auth-jwt") {
            route("/hello") {
                get{
                    call.respondText("Hello world")
                }
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal!!.payload.getClaim("username").asString()
                    val user = principal!!.payload.getClaim("password").asString()
                    call.respondText("Hello, $username! -> user : $user")
                }
            }
        }
    }
}

data class User(val username: String, val password: String)

data class MySession(val count: Int = 0)
