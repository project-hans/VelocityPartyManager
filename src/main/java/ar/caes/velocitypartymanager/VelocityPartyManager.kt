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
    authors = ["Caesar"]
)
class VelocityPartyManager @Inject constructor(private val server: ProxyServer, private val logger: Logger) {

    private val parties: MutableMap<UUID, Party> = mutableMapOf()

    // Map of PlayerUUID to PartyUUID
    private val playersInParty: MutableMap<UUID, UUID> = mutableMapOf()

    init {
        val restServer = RestServer(this, 7000)
        restServer.start()
    }

    fun transferParty(partyUUID: UUID, serverAlias: String?) {
        if (!parties.containsKey(partyUUID)) {
            throw IllegalStateException("Party $partyUUID does not exist")
        }
        var uuids = parties[partyUUID]!!.getMembers()
        server.getServer(serverAlias).ifPresent { targetServer: RegisteredServer? ->
            for (u in uuids) {
                server.getPlayer(u).ifPresent { player: Player -> player.createConnectionRequest(targetServer) }
            }
        }
    }

    fun joinParty(partyUUID: UUID, playerUUID: UUID) {
        if (!parties.containsKey(partyUUID)) {
            throw IllegalStateException("Party $partyUUID does not exist")
        }
        if(playersInParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID already in a party")
        }
        parties[partyUUID]!!.addMember(playerUUID)
        playersInParty[playerUUID] = partyUUID
    }

    fun leaveParty(playerUUID: UUID) {
        if(!playersInParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID not in any Party")
        }
        val party = parties[playersInParty[playerUUID]]!!
        party.removeMember(playerUUID)
    }

    fun registerParty(leaderUuid: UUID) {
        if(leaderUuid in playersInParty.keys) {
            throw IllegalStateException("Leader $leaderUuid already part of a party")
        }
        for (party in parties.values) {
            if(party.getLeaderUUID() == leaderUuid) {
                throw IllegalStateException("Leader $leaderUuid already registered a party")
            }
        }
        val party = Party(leaderUuid)
        parties[party.getPartyUUID()] = party
    }

    fun unregisterParty(uuid: UUID) {
        if (!parties.containsKey(uuid)) {
            throw IllegalStateException("Party $uuid does not exist")
        }
        parties.remove(uuid)
    }

    fun transferLeader(playerUUID: UUID) {
        if(!playersInParty.containsKey(playerUUID)) {
            throw IllegalStateException("Player $playerUUID not in any Party")
        }
        val party = parties[playersInParty[playerUUID]]!!
        party.setLeader(playerUUID)
    }


    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent?) {
    }
}
