package ar.caes.velocitypartymanager

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class PartyTest {

    @Test
    fun `initialization should set leader and add leader to members`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)

        // The leader should be set correctly.
        assertEquals(leader, party.getLeaderUUID())
        // The members list should contain the leader.
        val members = party.getPartyMembers()
        assertEquals(1, members.size)
        assertTrue(members.contains(leader))
    }

    @Test
    fun `addMember should add new member successfully`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val newMember = UUID.randomUUID()

        party.addMember(newMember)
        val members = party.getPartyMembers()
        assertEquals(2, members.size)
        assertTrue(members.contains(newMember))
    }

    @Test
    fun `addMember should throw exception if member already exists`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)

        // Adding the leader again should trigger an exception.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            party.addMember(leader)
        }
        assertTrue(exception.message?.contains("does already exist") ?: false)
    }

    @Test
    fun `removeMember should remove a non-leader member successfully`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val member = UUID.randomUUID()

        party.addMember(member)
        assertEquals(2, party.getPartyMembers().size)

        party.removeMember(member)
        val members = party.getPartyMembers()
        assertFalse(members.contains(member))
        // Since the removed member is not the leader, the leader remains unchanged.
        assertEquals(leader, party.getLeaderUUID())
    }

    @Test
    fun `removeMember should throw exception when member does not exist`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val nonExistentMember = UUID.randomUUID()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            party.removeMember(nonExistentMember)
        }
        assertTrue(exception.message?.contains("does not exist") ?: false)
    }

    @Test
    fun `setPartyLeader should update leader if member exists`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val newLeader = UUID.randomUUID()

        party.addMember(newLeader)
        party.setPartyLeader(newLeader)
        assertEquals(newLeader, party.getLeaderUUID())
    }

    @Test
    fun `setPartyLeader should throw exception if member does not exist`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val notAMember = UUID.randomUUID()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            party.setPartyLeader(notAMember)
        }
        assertTrue(exception.message?.contains("does not exist") ?: false)
    }

    @Test
    fun `removeMember should update leader to first remaining member when leader is removed and multiple members remain`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val member1 = UUID.randomUUID()
        val member2 = UUID.randomUUID()

        // Build a members list: [leader, member1, member2]
        party.addMember(member1)
        party.addMember(member2)

        // Remove the leader. After removal, the members list is [member1, member2],
        // so the leader should be updated to member1.
        party.removeMember(leader)
        assertEquals(member1, party.getLeaderUUID())
        val members = party.getPartyMembers()
        assertFalse(members.contains(leader))
    }

    @Test
    fun `removeMember should update leader to first remaining member when leader is removed and only one member remains`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        val member = UUID.randomUUID()

        // Build a members list: [leader, member]
        party.addMember(member)

        // Remove the leader. Now only one member remains, so the leader should be updated to that member.
        party.removeMember(leader)
        assertEquals(member, party.getLeaderUUID())
        val members = party.getPartyMembers()
        assertFalse(members.contains(leader))
    }

    @Test
    fun `getPartyUUID should return a non-null UUID`() {
        val leader = UUID.randomUUID()
        val party = Party(leader)
        assertNotNull(party.getPartyUUID())
    }
}
