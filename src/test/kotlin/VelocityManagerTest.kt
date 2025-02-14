// File: VelocityPartyManagerTest.kt
package ar.caes.velocitypartymanager

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.lang.reflect.Field
import java.net.ServerSocket
import java.util.*

/**
 * Unit tests for VelocityPartyManager.
 */
class VelocityPartyManagerTest {
    private var proxyServer: ProxyServer? = null
    private var logger: Logger? = null


    /**
     * Helper function to get a free port from the OS.
     */
    private fun getFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    /**
     * Creates a new instance of VelocityPartyManager with a mocked ProxyServer and Logger.
     */
    private fun createManager(): VelocityPartyManager {
        proxyServer = mockk<ProxyServer>(relaxed = true)
        logger = mockk<Logger>(relaxed = true)

        System.setProperty("PARTY_API_PORT", getFreePort().toString())
        return VelocityPartyManager(proxyServer!!, logger!!)
    }

    /**
     * Helper function that registers a party for the given leader and then
     * has the leader join the party so that they are “in” the party.
     */
    private fun registerAndJoinParty(manager: VelocityPartyManager, leader: UUID): UUID {
        val partyUUID = manager.registerParty(leader)
        return partyUUID
    }

    @Test
    fun `registerParty should successfully register a new party and registers the leader as a member`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()

        // when
        val partyUUID = manager.registerParty(leader)

        // then
        val party = manager.partyInfo(leader)
        assertEquals(leader, party.getLeaderUUID(), "Leader should match")
        assertEquals(partyUUID, party.getPartyUUID(), "Party UUID should match")
    }

    @Test
    fun `registerParty should fail if leader already in a party`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        manager.registerParty(leader)

        // when/then
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.registerParty(leader)
        }
        assertTrue(exception.message?.contains("already") == true)
    }

    @Test
    fun `joinParty should fail if party does not exist`() {
        // given
        val manager = createManager()
        val nonExistentPartyUUID = UUID.randomUUID()
        val player = UUID.randomUUID()

        // when/then
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.joinParty(nonExistentPartyUUID, player)
        }
        assertTrue(exception.message?.contains("does not exist") == true)
    }

    @Test
    fun `joinParty should fail if player is already in a party`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        val partyUUID = manager.registerParty(leader)

        // when/then: attempting to join the same party twice for the same player
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.joinParty(partyUUID, leader)
        }
        assertTrue(exception.message?.contains("already in a party") == true)
    }

    @Test
    fun `leaveParty should allow a member to leave without unregistering the party`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        val partyUUID = registerAndJoinParty(manager, leader)
        val member = UUID.randomUUID()
        manager.joinParty(partyUUID, member)

        // when: member leaves the party
        manager.leaveParty(member)

        // then: the party should still exist for the leader,
        // and the party's member list should no longer include the member.
        val party = manager.partyInfo(leader)
        assertFalse(party.getPartyMembers().contains(member), "Member should have been removed")
    }

    @Test
    fun `leaveParty should unregister the party if the leader leaves and no members remain`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        registerAndJoinParty(manager, leader)

        // when: leader leaves (the only member), so the party is unregistered
        manager.leaveParty(leader)

        // then: querying partyInfo for the leader should fail
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.partyInfo(leader)
        }
        assertTrue(exception.message?.contains("not in any Party") == true)
    }

    @Test
    fun `leaveParty should fail if player is not in any party`() {
        // given
        val manager = createManager()
        val randomPlayer = UUID.randomUUID()

        // when/then
        val exception = assertThrows(IllegalStateException::class.java) {
            manager.leaveParty(randomPlayer)
        }
        assertTrue(exception.message?.contains("not in any Party") == true)
    }

    @Test
    fun `transferLeader should successfully transfer leadership to new leader`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        val partyUUID = registerAndJoinParty(manager, leader)
        val newLeader = UUID.randomUUID()
        // new leader must join the party before the transfer
        manager.joinParty(partyUUID, newLeader)

        // when
        manager.transferLeader(leader, newLeader)

        // then: party's leader should now be newLeader.
        // (Since both leader and newLeader are in the same party, partyInfo(newLeader) works.)
        val party = manager.partyInfo(newLeader)
        assertEquals(newLeader, party.getLeaderUUID(), "Leadership should have been transferred")
    }

    @Test
    fun `transferParty should send connection requests to all party members`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        val partyUUID = manager.registerParty(leader)
        val member1 = UUID.randomUUID()
        val member2 = UUID.randomUUID()
        manager.joinParty(partyUUID, member1)
        manager.joinParty(partyUUID, member2)

        val serverAlias = "lobby"

        // Prepare a mock RegisteredServer to be returned by getServer.
        val registeredServer: RegisteredServer = mockk(relaxed = true)
        every { proxyServer!!.getServer(serverAlias) } returns Optional.of(registeredServer)

        // Prepare mocks for players.
        val playerLeader: Player = mockk(relaxed = true)
        val playerMember1: Player = mockk(relaxed = true)
        val playerMember2: Player = mockk(relaxed = true)
        every { proxyServer!!.getPlayer(leader) } returns Optional.of(playerLeader)
        every { proxyServer!!.getPlayer(member1) } returns Optional.of(playerMember1)
        every { proxyServer!!.getPlayer(member2) } returns Optional.of(playerMember2)

        // when
        manager.transferParty(leader, serverAlias)

        // then: verify that createConnectionRequest was called on all party members
        verify(exactly = 1) { playerLeader.createConnectionRequest(registeredServer) }
        verify(exactly = 1) { playerMember1.createConnectionRequest(registeredServer) }
        verify(exactly = 1) { playerMember2.createConnectionRequest(registeredServer) }
    }

    @Test
    fun `partyInfo should return correct party information for a player in a party`() {
        // given
        val manager = createManager()
        val leader = UUID.randomUUID()
        val partyUUID = registerAndJoinParty(manager, leader)

        // when
        val party = manager.partyInfo(leader)

        // then
        assertEquals(partyUUID, party.getPartyUUID(), "Party UUID should match")
        assertEquals(leader, party.getLeaderUUID(), "Leader should match")
    }

    @Test
    fun `onProxyInitialization should start the rest server`() {
        // given
        val manager = createManager()

        // Use reflection to replace the private 'restServer' field with a mock
        val restServerField: Field = manager.javaClass.getDeclaredField("restServer")
        restServerField.isAccessible = true
        val restServerMock = mockk<RestServer>(relaxed = true)
        restServerField.set(manager, restServerMock)

        // when: simulate the proxy initialization event
        manager.onProxyInitialization(mockk(relaxed = true))

        // then: verify that restServer.start() was called.
        verify { restServerMock.start() }
    }
}
