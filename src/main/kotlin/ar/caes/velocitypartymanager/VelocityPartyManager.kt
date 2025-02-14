package ar.caes.velocitypartymanager

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import org.slf4j.Logger
import java.util.*


@Plugin(
    id = "velocitypartymanager",
    name = "VelocityPartyManager",
    version = BuildConstants.VERSION,
    url = "https://github.com/project-hans/VelocityPartyManager",
    description =
        "Party Manager for Velocity, Use a REST client to use the functionality",
    authors = ["Caesar"]
)
class VelocityPartyManager @Inject constructor(private val server: ProxyServer, private val logger: Logger) {

    // Map of PartyUUID to Party
    private val parties: MutableMap<UUID, Party> = mutableMapOf()

    private val restServer: RestServer

    // Map of PlayerUUID to PartyUUID
    private val playerToParty: MutableMap<UUID, UUID> = mutableMapOf()

    init {
        val port: Int = System.getProperty("PARTY_API_PORT").toInt()
        restServer = RestServer(this, port)
    }

    private fun partyElevatedPrivileges(playerUUID: UUID): Party {
        val partyUUID = getPartyUUIDForPlayer(playerUUID)
        if (!parties.containsKey(partyUUID)) {
            throw IllegalStateException("Party $partyUUID does not exist")
        }
        val party = parties[partyUUID]!!
        if (party.leader != playerUUID) {
            throw IllegalAccessException("Player $playerUUID is not the Leader")
        }

        return party
    }

    fun transferParty(playerUUID: UUID, serverAlias: String) {
        val party = partyElevatedPrivileges(playerUUID)
        val uuids = party.getPartyMembers()
        val instanceFound =
            server.getServer(serverAlias) ?: throw IllegalStateException("Server $serverAlias doesn't exist")

        instanceFound.ifPresent { targetServer: RegisteredServer? ->
            for (u in uuids) {
                server.getPlayer(u).ifPresent { player: Player -> player.createConnectionRequest(targetServer) }
            }
        }
    }

    fun joinParty(partyUUID: UUID, playerUUID: UUID) {
        if (!parties.containsKey(partyUUID)) {
            throw IllegalStateException("Party $partyUUID does not exist")
        }
        if (playerToParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID already in a party")
        }
        parties[partyUUID]!!.addMember(playerUUID)
        playerToParty[playerUUID] = partyUUID
    }

    fun leaveParty(playerUUID: UUID) {
        if (!playerToParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID not in any Party")
        }
        val party = parties[playerToParty[playerUUID]]!!
        party.removeMember(playerUUID)

        if (party.getPartyMembers().size == 0) {
            // Works because leader field is still set even with no members
            unregisterParty(playerUUID)
        }
        playerToParty.remove(playerUUID)
    }

    fun registerParty(leaderUuid: UUID): UUID {
        if (leaderUuid in playerToParty.keys) {
            throw IllegalStateException("Leader $leaderUuid already part of a party")
        }
        for (party in parties.values) {
            if (party.getLeaderUUID() == leaderUuid) {
                throw IllegalStateException("Leader $leaderUuid already registered a party")
            }
        }
        val party = Party(leaderUuid)
        parties[party.getPartyUUID()] = party
        playerToParty[leaderUuid] = party.getPartyUUID()
        return party.getPartyUUID()
    }

    fun unregisterParty(playerUUID: UUID) {
        val party = partyElevatedPrivileges(playerUUID)
        parties.remove(party.getPartyUUID())
    }

    fun transferLeader(playerUUID: UUID, newLeaderUUID: UUID) {
        val party = partyElevatedPrivileges(playerUUID)
        party.setPartyLeader(newLeaderUUID)
    }

    fun partyInfo(playerUUID: UUID): Party {
        val partyUUID = getPartyUUIDForPlayer(playerUUID)
        return parties[partyUUID]!!
    }

    private fun getPartyUUIDForPlayer(playerUUID: UUID): UUID {
        if (!playerToParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID not in any Party")
        }
        return playerToParty[playerUUID]!!
    }

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent?) {
        restServer.start()
    }
}
