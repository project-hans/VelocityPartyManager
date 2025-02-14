package ar.caes.velocitypartymanager

import java.util.*

class Party(leaderUUID: UUID) {
    var uuid: UUID = UUID.randomUUID()
    var name: String? = null
    var members: MutableList<UUID> = ArrayList()
    var leader: UUID? = null

    init {
        addMember(leaderUUID)
        setPartyLeader(leaderUUID)
    }

    fun setPartyLeader(uuid: UUID) {
        if (!members.contains(uuid)) {
            throw IllegalArgumentException("UUID $uuid of leader does not exist in member list")
        }
        leader = uuid
    }

    fun getPartyUUID(): UUID {
        return uuid
    }

    fun getLeaderUUID(): UUID = leader!!

    fun getPartyMembers(): MutableList<UUID> {
        return members
    }

    fun addMember(uuid: UUID) {
        if (members.contains(uuid)) {
            throw IllegalArgumentException("UUID $uuid does already exist in member list")
        }
        members.add(uuid)
    }

    fun removeMember(uuid: UUID) {
        if (!members.contains(uuid)) {
            throw IllegalArgumentException("UUID $uuid does not exist in member list")
        }
        members.remove(uuid)
        if ((leader == uuid) && (members.size >= 1)) {
            leader = members[0]
        }
    }
}