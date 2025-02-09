package ar.caes.velocitypartymanager

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.*

class RestServer(private val manager: VelocityPartyManager, private val port: Int) {
    private var app: Javalin = Javalin.create().start(this.port)

    fun start() {
        /**
         * GET /player/:playerUUID/partyInfo
         *
         * Retrieves the party information associated with a given player's UUID.
         *
         * Path Parameter:
         * - **playerUUID**: The UUID of the player for whom to retrieve party information.
         *
         * Successful Response:
         * - **HTTP 200 OK**: Returns the party object in JSON format.
         *
         * Error Response:
         * - **HTTP 400 Bad Request**: Returns an error message if the party information cannot be retrieved.
         *
         * Example Request:
         * GET /player/1a2b3c4d-5e6f-7890-abcd-ef1234567890/partyInfo
         *
         * @param ctx The Javalin Context object that holds the HTTP request and response.
         */
        app.get("/player/:playerUUID/partyInfo") { ctx: Context ->
            val playerUUID = UUID.fromString(ctx.pathParam("playerUUID"))
            try {
                val party = manager.partyInfo(playerUUID)
                ctx.status(200).json(party)
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error while fetching party info for UUID $playerUUID")
            }
        }

        /**
         * Transfer a party’s members to a different server.
         *
         * Expects:
         *   - path param "partyUUID": the party UUID.
         *   - query param "serverAlias": the target server alias.
         *
         * POST /parties/{partyUUID}/transfer?serverAlias=serverA
         */
        app.post("/parties/:partyUUID/transfer") { ctx: Context ->
            val partyUUID = UUID.fromString(ctx.pathParam("partyUUID"))
            val serverAlias = ctx.queryParam("serverAlias") // May be null
            try {
                manager.transferParty(partyUUID, serverAlias)
                ctx.status(200).result("Party transferred successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error transferring party")
            }
        }

        /**
         * Add a player to a party.
         *
         * Expects:
         *   - path param "partyUUID": the party UUID.
         *   - form (or body) parameter "playerUUID": the player UUID.
         *
         * POST /parties/{partyUUID}/join
         */
        app.post("/parties/:partyUUID/join") { ctx: Context ->
            val partyUUID = UUID.fromString(ctx.pathParam("partyUUID"))
            val playerUUID = UUID.fromString(
                ctx.formParam("playerUUID") ?: throw IllegalArgumentException("Missing playerUUID")
            )
            try {
                manager.joinParty(partyUUID, playerUUID)
                ctx.status(200).result("Player joined party successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error joining party")
            }
        }

        /**
         * Remove a player from their party.
         *
         * We assume that the player's UUID is enough to find the party.
         *
         * DELETE /parties/members/{playerUUID}
         */
        app.delete("/parties/members/:playerUUID") { ctx: Context ->
            val playerUUID = UUID.fromString(ctx.pathParam("playerUUID"))
            try {
                manager.leaveParty(playerUUID)
                ctx.status(200).result("Player left party successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error leaving party")
            }
        }

        /**
         * Create a new party (i.e. register a party).
         *
         * Expects:
         *   - form (or body) parameter "leaderUUID": the UUID of the party leader.
         *
         * POST /parties
         */
        app.post("/parties") { ctx: Context ->
            val leaderUUID = UUID.fromString(
                ctx.formParam("leaderUUID") ?: throw IllegalArgumentException("Missing leaderUUID")
            )
            try {
                manager.registerParty(leaderUUID)
                ctx.status(201).result("Party registered successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error registering party")
            }
        }

        /**
         * Delete (unregister) an existing party.
         *
         * Expects:
         *   - path parameter "partyUUID": the party UUID.
         *
         * DELETE /parties/{partyUUID}
         */
        app.delete("/parties/:partyUUID") { ctx: Context ->
            val partyUUID = UUID.fromString(ctx.pathParam("partyUUID"))
            try {
                manager.unregisterParty(partyUUID)
                ctx.status(200).result("Party unregistered successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error unregistering party")
            }
        }

        /**
         * Transfer the leadership of a party.
         *
         * The player specified must already be in a party.
         *
         * Expects:
         *   - form (or body) parameter "playerUUID": the new leader’s UUID.
         *
         * POST /parties/transfer-leader
         */
        app.post("/parties/transfer-leader") { ctx: Context ->
            val playerUUID = UUID.fromString(
                ctx.formParam("playerUUID") ?: throw IllegalArgumentException("Missing playerUUID")
            )
            try {
                manager.transferLeader(playerUUID)
                ctx.status(200).result("Party leader transferred successfully")
            } catch (e: Exception) {
                ctx.status(400).result(e.message ?: "Error transferring leader")
            }
        }

        // You can add more endpoints as needed
    }

    fun stop() {
        app.stop()
    }
}
