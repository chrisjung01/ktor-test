package de.coli.routes.customer

import io.ktor.application.*
import io.ktor.routing.*

fun Route.customerById() {
    get("/customer/{id}") {
        println("customerById")
        val id = call.parameters["id"]
        if (id != "") {
            println("ID: $id")
        }
    }
}

fun Route.createCustomer() {
    post("/customer") {
        println("createCustomer")
    }
}

fun Route.customer() {
    createCustomer()
    customerById()
}