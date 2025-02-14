package ar.caes.velocitypartymanager

import io.javalin.Javalin
import io.javalin.http.Context
import java.util.*

class RestServer(private val manager: VelocityPartyManager, private val port: Int) {
    private var app: Javalin = Javalin.create().start(this.port)

    fun start() {
        /**
         * POST /party/register
         *
         * Registers a new party with the provided leader UUID.
         *
         * Query Parameters:
         * - leaderUuid (String): The UUID of the leader to be registered.
         *
         * Responses:
         * - 201 Created: Returns a JSON object containing the new party's UUID.
         * - 400 Bad Request: If the leaderUuid is missing, invalid, or the leader is already in a party.
         */
        app.post("/party/register") { ctx: Context ->
            try {
                val leaderUuid = UUID.fromString(
                    ctx.queryParam("leaderUuid")
                        ?: throw IllegalArgumentException("Missing parameter: leaderUuid")
                )
                val partyId = manager.registerParty(leaderUuid)
                ctx.status(201).json(mapOf("partyUUID" to partyId.toString()))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * POST /party/join
         *
         * Allows a player to join an existing party.
         *
         * Query Parameters:
         * - partyUUID (String): The UUID of the party to join.
         * - playerUUID (String): The UUID of the player joining the party.
         *
         * Responses:
         * - 200 OK: When the player has successfully joined the party.
         * - 400 Bad Request: If any parameters are missing, invalid, or if the party does not exist
         *   or the player is already in a party.
         */
        app.post("/party/join") { ctx: Context ->
            try {
                val partyUuid = UUID.fromString(
                    ctx.queryParam("partyUUID")
                        ?: throw IllegalArgumentException("Missing parameter: partyUUID")
                )
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                manager.joinParty(partyUuid, playerUuid)
                ctx.status(200).json(mapOf("message" to "Joined party successfully"))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * POST /party/leave
         *
         * Allows a player to leave their current party.
         *
         * Query Parameters:
         * - playerUUID (String): The UUID of the player leaving the party.
         *
         * Behavior:
         * - Removes the player from the party.
         * - If the party becomes empty after leaving, the party is unregistered.
         *
         * Responses:
         * - 200 OK: When the player has successfully left the party.
         * - 400 Bad Request: If the player is not in any party or if an error occurs.
         */
        app.post("/party/leave") { ctx: Context ->
            try {
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                manager.leaveParty(playerUuid)
                ctx.status(200).json(mapOf("message" to "Left party successfully"))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * POST /party/unregister
         *
         * Unregisters a party based on the player's elevated privileges.
         *
         * Query Parameters:
         * - playerUUID (String): The UUID of the player (who must be the leader) unregistering the party.
         *
         * Responses:
         * - 200 OK: When the party is successfully unregistered.
         * - 400 Bad Request: If the player is not the leader of the party or any other error occurs.
         */
        app.post("/party/unregister") { ctx: Context ->
            try {
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                manager.unregisterParty(playerUuid)
                ctx.status(200).json(mapOf("message" to "Party unregistered successfully"))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * POST /party/transfer
         *
         * Transfers the entire party to another server.
         *
         * Query Parameters:
         * - playerUUID (String): The UUID of the party leader initiating the transfer.
         * - serverAlias (String, optional): The alias of the target server.
         *
         * Behavior:
         * - Validates that the player has elevated privileges (is the leader) for the party.
         * - Initiates a connection request for each party member to the target server.
         *
         * Responses:
         * - 200 OK: When the party transfer is initiated successfully.
         * - 400 Bad Request: If required parameters are missing, invalid, or if an error occurs during transfer.
         */
        app.post("/party/transfer") { ctx: Context ->
            try {
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                // serverAlias is optional; it might be null if not provided
                val serverAlias = ctx.queryParam("serverAlias")
                manager.transferParty(playerUuid, serverAlias)
                ctx.status(200).json(mapOf("message" to "Party transfer initiated"))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * POST /party/transferLeader
         *
         * Transfers the leadership of a party to another player.
         *
         * Query Parameters:
         * - playerUUID (String): The UUID of the current leader initiating the transfer.
         * - newLeaderUUID (String): The UUID of the player who will become the new leader.
         *
         * Behavior:
         * - Validates that the player initiating the transfer is the current leader.
         * - Sets the new leader for the party.
         *
         * Responses:
         * - 200 OK: When the leadership transfer is successful.
         * - 400 Bad Request: If required parameters are missing, invalid, or if the initiating player is not the leader.
         */
        app.post("/party/transferLeader") { ctx: Context ->
            try {
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                val newLeaderUuid = UUID.fromString(
                    ctx.queryParam("newLeaderUUID")
                        ?: throw IllegalArgumentException("Missing parameter: newLeaderUUID")
                )
                manager.transferLeader(playerUuid, newLeaderUuid)
                ctx.status(200).json(mapOf("message" to "Party leader transferred successfully"))
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }

        /**
         * GET /party/info
         *
         * Retrieves information about the party that the specified player is a member of.
         *
         * Query Parameters:
         * - playerUUID (String): The UUID of the player whose party information is requested.
         *
         * Responses:
         * - 200 OK: Returns a JSON representation of the party.
         * - 400 Bad Request: If the player is not in any party or if an error occurs.
         *
         * Note: This assumes that the Party class is serializable to JSON.
         */
        app.get("/party/info") { ctx: Context ->
            try {
                val playerUuid = UUID.fromString(
                    ctx.queryParam("playerUUID")
                        ?: throw IllegalArgumentException("Missing parameter: playerUUID")
                )
                val party = manager.partyInfo(playerUuid)
                ctx.status(200).json(party)
            } catch (e: Exception) {
                ctx.status(400).json(mapOf("error" to e.message))
            }
        }
    }

    fun stop() {
        app.stop()
    }
}