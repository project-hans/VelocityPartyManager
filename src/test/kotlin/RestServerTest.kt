package ar.caes.velocitypartymanager

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.*

class RestServerTest {

    // Obtain a free port for testing.
    private val port: Int = ServerSocket(0).use { it.localPort }

    // Base URL for the server.
    private val baseUrl = "http://localhost:$port"

    // Create a mock for the manager dependency.
    private val manager: VelocityPartyManager = mockk(relaxed = true)
    private lateinit var restServer: RestServer
    private val client = OkHttpClient()

    @BeforeEach
    fun setup() {
        // Create the RestServer and register the routes.
        restServer = RestServer(manager, port)
        restServer.start()
    }

    @AfterEach
    fun tearDown() {
        restServer.stop()
    }

    // Helper function to send a POST request with an empty body.
    private fun post(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody(null))
            .build()
        return client.newCall(request).execute()
    }

    // --- Tests for /party/register ---
    @Test
    fun `register party success`() {
        val leaderUuid = UUID.randomUUID()
        val partyUUID = UUID.randomUUID()
        every { manager.registerParty(leaderUuid) } returns partyUUID

        val url = "$baseUrl/party/register?leaderUuid=$leaderUuid"
        client.newCall(
            Request.Builder()
                .url(url)
                .post(ByteArray(0).toRequestBody(null))
                .build()
        ).execute().use { response ->
            assertEquals(201, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals(partyUUID.toString(), json.getString("partyUUID"))
        }
    }

    @Test
    fun `register party missing leaderUuid`() {
        val url = "$baseUrl/party/register"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter"))
        }
    }

    @Test
    fun `register party invalid leaderUuid`() {
        val url = "$baseUrl/party/register?leaderUuid=invalid-uuid"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            // The error message comes from UUID.fromString – we just check that an error is reported.
            assertTrue(json.getString("error").isNotEmpty())
        }
    }

    // --- Tests for /party/join ---
    @Test
    fun `join party success`() {
        val partyUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()
        // For a successful join, we simply allow the call.
        justRun { manager.joinParty(partyUuid, playerUuid) }

        val url = "$baseUrl/party/join?partyUUID=$partyUuid&playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Joined party successfully", json.getString("message"))
        }
    }

    @Test
    fun `join party missing parameters`() {
        // Missing the 'playerUUID' parameter.
        val url = "$baseUrl/party/join?partyUUID=${UUID.randomUUID()}"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: playerUUID"))
        }
    }

    @Test
    fun `join party invalid parameters`() {
        val url = "$baseUrl/party/join?partyUUID=invalid&playerUUID=invalid"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            // We expect an error message about the invalid UUID string.
            assertTrue(json.getString("error").isNotEmpty())
        }
    }

    @Test
    fun `join party manager throws exception`() {
        val partyUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()
        every { manager.joinParty(partyUuid, playerUuid) } throws IllegalStateException("Player already in party")

        val url = "$baseUrl/party/join?partyUUID=$partyUuid&playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Player already in party"))
        }
    }

    // --- Tests for /party/leave ---
    @Test
    fun `leave party success`() {
        val playerUuid = UUID.randomUUID()
        justRun { manager.leaveParty(playerUuid) }

        val url = "$baseUrl/party/leave?playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Left party successfully", json.getString("message"))
        }
    }

    @Test
    fun `leave party missing parameter`() {
        val url = "$baseUrl/party/leave"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: playerUUID"))
        }
    }

    // --- Tests for /party/unregister ---
    @Test
    fun `unregister party success`() {
        val playerUuid = UUID.randomUUID()
        justRun { manager.unregisterParty(playerUuid) }

        val url = "$baseUrl/party/unregister?playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Party unregistered successfully", json.getString("message"))
        }
    }

    @Test
    fun `unregister party missing parameter`() {
        val url = "$baseUrl/party/unregister"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: playerUUID"))
        }
    }

    // --- Tests for /party/transfer ---
    @Test
    fun `transfer party with serverAlias success`() {
        val playerUuid = UUID.randomUUID()
        val serverAlias = "server1"
        justRun { manager.transferParty(playerUuid, serverAlias) }

        val url = "$baseUrl/party/transfer?playerUUID=$playerUuid&serverAlias=$serverAlias"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Party transfer initiated", json.getString("message"))
        }
    }

    @Test
    fun `transfer party without serverAlias success`() {
        val playerUuid = UUID.randomUUID()
        // When serverAlias is not provided, no transfer is initiated but the endpoint still returns success.
        val url = "$baseUrl/party/transfer?playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Party transfer initiated", json.getString("message"))
        }
    }

    @Test
    fun `transfer party missing playerUUID`() {
        val url = "$baseUrl/party/transfer"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: playerUUID"))
        }
    }

    // --- Tests for /party/transferLeader ---
    @Test
    fun `transfer leader success`() {
        val playerUuid = UUID.randomUUID()
        val newLeaderUuid = UUID.randomUUID()
        justRun { manager.transferLeader(playerUuid, newLeaderUuid) }

        val url = "$baseUrl/party/transferLeader?playerUUID=$playerUuid&newLeaderUUID=$newLeaderUuid"
        post(url).use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertEquals("Party leader transferred successfully", json.getString("message"))
        }
    }

    @Test
    fun `transfer leader missing parameter`() {
        val playerUuid = UUID.randomUUID()
        // newLeaderUUID is missing.
        val url = "$baseUrl/party/transferLeader?playerUUID=$playerUuid"
        post(url).use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: newLeaderUUID"))
        }
    }

    // --- Tests for /party/info ---
    @Test
    fun `party info success`() {
        val playerUuid = UUID.randomUUID()
        // Create a Party instance as expected to be returned.
        val party = Party(playerUuid)
        party.name = "Test Party"
        every { manager.partyInfo(playerUuid) } returns party

        val url = "$baseUrl/party/info?playerUUID=$playerUuid"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            // Verify the party's UUID and leader.
            assertEquals(party.uuid.toString(), json.getString("uuid"))
            assertEquals(party.leader.toString(), json.getString("leader"))
            // Verify the members array contains the leader.
            val membersJson = json.getJSONArray("members")
            assertEquals(1, membersJson.length())
            assertEquals(playerUuid.toString(), membersJson.getString(0))
            // Optionally verify the party name.
            if (party.name != null) {
                assertEquals(party.name, json.getString("name"))
            }
        }
    }

    @Test
    fun `party info missing parameter`() {
        val url = "$baseUrl/party/info"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            assertEquals(400, response.code)
            val json = JSONObject(response.body?.string() ?: "")
            assertTrue(json.getString("error").contains("Missing parameter: playerUUID"))
        }
    }
}
