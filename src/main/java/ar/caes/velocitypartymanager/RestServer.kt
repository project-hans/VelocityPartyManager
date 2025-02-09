package ar.caes.velocitypartymanager

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Handler

class RestServer(private val manager: VelocityPartyManager, private val port: Int) {
    private var app: Javalin? = null

    fun start() {
        // Create a Javalin instance on port 7000 (or any other port)
        app = Javalin.create { config: JavalinConfig? -> }.start(this.port)

        // Define a simple endpoint
        app.get("/hello", Handler { ctx: Context -> ctx.result("Hello, World!") })

        // You can add more endpoints as needed
    }

    fun stop() {
        if (app != null) {
            app!!.stop()
        }
    }
}
