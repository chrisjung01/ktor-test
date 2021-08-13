package de.coli

import com.google.gson.Gson
import io.ktor.application.*
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
                    uuid = UUID.randomUUID().toString(),
                    username = "Username",
                    email = "E-Mail"
                )
                call.respond(Gson().toJson(user))
            }

            // read the json from the body and put this into the dataclass
            post {
                val user = call.receive<User>()
                println(user)
            }
        }
    }
}

data class User(val uuid: String, val username: String, val email: String)

data class MySession(val count: Int = 0)
