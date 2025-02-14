// File: RestServerTest.kt
package ar.caes.velocitypartymanager

import io.mockk.*
import kong.unirest.Unirest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class RestServerTest {

    // Use a test port (ensure it is free)
    private val testPort = 7001

    // Create a mock manager; we use a relaxed mock so that functions returning Unit do nothing unless specified.
    private val manager = mockk<VelocityPartyManager>(relaxed = true)

    // Hold a reference to our RestServer instance
    private var restServer: RestServer? = null

    /**
     * Helper to start the RestServer. The Javalin app is created in the RestServer constructor,
     * but endpoints are only added when start() is called.
     */
    private fun startServer() {
        restServer = RestServer(manager, testPort)
        restServer!!.start()
    }

    @AfterEach
    fun tearDown() {
        // Stop the Javalin server after each test.
        restServer?.stop()
        // Shut down Unirest so that connections are closed.
        Unirest.shutDown()
        clearAllMocks()
    }

    @Test
    fun `POST party register - success`() {
        startServer()
        val leaderUuid = UUID.randomUUID()
        val partyUuid = UUID.randomUUID()

        // When the manager is called to register the party, return our fake party UUID.
        every { manager.registerParty(leaderUuid) } returns partyUuid

        val response = Unirest.post("http://localhost:$testPort/party/register")
            .queryString("leaderUuid", leaderUuid.toString())
            .asJson()

        // Expect 201 Created and a JSON object with "partyUUID"
        assertEquals(201, response.status)
        val body = response.body.`object`
        assertEquals(partyUuid.toString(), body.getString("partyUUID"))
        verify(exactly = 1) { manager.registerParty(leaderUuid) }
    }

    @Test
    fun `POST party register - missing parameter`() {
        startServer()

        // No leaderUuid provided should result in an error response.
        val response = Unirest.post("http://localhost:$testPort/party/register")
            .asJson()

        assertEquals(400, response.status)
        val body = response.body.`object`
        assertTrue(body.getString("error").contains("Missing parameter"))
    }

    @Test
    fun `POST party join - success`() {
        startServer()
        val partyUuid = UUID.randomUUID()
        val playerUuid = UUID.randomUUID()

        // Assume joinParty returns Unit; no further configuration is needed.
        every { manager.joinParty(partyUuid, playerUuid) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/join")
            .queryString("partyUUID", partyUuid.toString())
            .queryString("playerUUID", playerUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Joined party successfully", body.getString("message"))
        verify(exactly = 1) { manager.joinParty(partyUuid, playerUuid) }
    }

    @Test
    fun `POST party join - missing parameters`() {
        startServer()
        // Missing both partyUUID and playerUUID
        val response = Unirest.post("http://localhost:$testPort/party/join")
            .asJson()

        assertEquals(400, response.status)
        val body = response.body.`object`
        assertTrue(body.getString("error").contains("Missing parameter"))
    }

    @Test
    fun `POST party leave - success`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        every { manager.leaveParty(playerUuid) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/leave")
            .queryString("playerUUID", playerUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Left party successfully", body.getString("message"))
        verify(exactly = 1) { manager.leaveParty(playerUuid) }
    }

    @Test
    fun `POST party unregister - success`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        every { manager.unregisterParty(playerUuid) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/unregister")
            .queryString("playerUUID", playerUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Party unregistered successfully", body.getString("message"))
        verify(exactly = 1) { manager.unregisterParty(playerUuid) }
    }

    @Test
    fun `POST party transfer - success with serverAlias`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        val serverAlias = "lobby"
        every { manager.transferParty(playerUuid, serverAlias) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/transfer")
            .queryString("playerUUID", playerUuid.toString())
            .queryString("serverAlias", serverAlias)
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Party transfer initiated", body.getString("message"))
        verify(exactly = 1) { manager.transferParty(playerUuid, serverAlias) }
    }

    @Test
    fun `POST party transfer - success without serverAlias`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        every { manager.transferParty(playerUuid, null) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/transfer")
            .queryString("playerUUID", playerUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Party transfer initiated", body.getString("message"))
        verify(exactly = 1) { manager.transferParty(playerUuid, null) }
    }

    @Test
    fun `POST party transferLeader - success`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        val newLeaderUuid = UUID.randomUUID()
        every { manager.transferLeader(playerUuid, newLeaderUuid) } just runs

        val response = Unirest.post("http://localhost:$testPort/party/transferLeader")
            .queryString("playerUUID", playerUuid.toString())
            .queryString("newLeaderUUID", newLeaderUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        assertEquals("Party leader transferred successfully", body.getString("message"))
        verify(exactly = 1) { manager.transferLeader(playerUuid, newLeaderUuid) }
    }

    @Test
    fun `GET party info - success`() {
        startServer()
        val playerUuid = UUID.randomUUID()
        // Create a dummy Party object to return. We use the real Party class.
        val party = Party(playerUuid).apply { name = "Test Party" }
        every { manager.partyInfo(playerUuid) } returns party

        val response = Unirest.get("http://localhost:$testPort/party/info")
            .queryString("playerUUID", playerUuid.toString())
            .asJson()

        assertEquals(200, response.status)
        val body = response.body.`object`
        // Check that the JSON contains key fields from the Party.
        assertEquals(party.uuid.toString(), body.getString("uuid"))
        assertEquals(party.leader.toString(), body.getString("leader"))
        assertEquals("Test Party", body.getString("name"))
    }

    @Test
    fun `GET party info - missing parameter`() {
        startServer()

        val response = Unirest.get("http://localhost:$testPort/party/info")
            .asJson()

        assertEquals(400, response.status)
        val body = response.body.`object`
        assertTrue(body.getString("error").contains("Missing parameter"))
    }
}
