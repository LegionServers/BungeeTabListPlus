/*
 *
 *  * BungeeTabListPlus - a bungeecord plugin to customize the tablist
 *  *
 *  * Copyright (C) 2014 Florian Stober
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package codecrafter47.bungeetablistplus.bridge;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.common.Constants;
import codecrafter47.bungeetablistplus.player.IPlayer;
import codecrafter47.data.DataCache;
import codecrafter47.data.Value;
import codecrafter47.data.Values;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Florian Stober
 */
public class BukkitBridge implements Listener {
    private final BungeeTabListPlus plugin;

    private final Map<String, DataCache> serverInformation = new ConcurrentHashMap<>();
    private final Cache<UUID, DataCache> playerInformation = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

    public BukkitBridge(BungeeTabListPlus plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin.getPlugin(), this);
    }

    private DataCache getServerDataCache(String serverName) {
        if (!serverInformation.containsKey(serverName)) {
            serverInformation.putIfAbsent(serverName, new DataCache());
        }
        return serverInformation.get(serverName);
    }

    @SneakyThrows
    private DataCache getPlayerDataCache(UUID uuid) {
        return playerInformation.get(uuid, DataCache::new);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals(Constants.channel)) {
            event.setCancelled(true);
            if (event.getReceiver() instanceof ProxiedPlayer && event.getSender() instanceof Server) {
                try {
                    ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
                    Server server = (Server) event.getSender();

                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(event.getData()));

                    String subchannel = in.readUTF();

                    switch (subchannel) {
                        case Constants.subchannelUpdateServer:
                            updateData(in, getServerDataCache(server.getInfo().getName()));
                            break;
                        case Constants.subchannelUpdatePlayer:
                            updateData(in, getPlayerDataCache(player.getUniqueId()));
                            break;
                        default:
                            plugin.getLogger().log(Level.SEVERE,
                                    "BukkitBridge on server " + server.getInfo().
                                            getName() + " send an unknown packet! Is everything up-to-date?");
                            break;
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Exception while parsing data from Bukkit", ex);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateData(ObjectInputStream in, DataCache dataCache) throws IOException, ClassNotFoundException {
        for (Entry<String, Object> entry : ((Map<String, Object>) in.readObject()).entrySet()) {
            Value<Object> value = Values.getValue(entry.getKey());
            if (value == null) {
                plugin.getLogger().warning("Received unknown data \"" + entry.getKey() + "\" from bukkit.");
            } else {
                dataCache.updateValue(value, entry.getValue());
            }
        }
    }

    @EventHandler
    public void onServerChange(ServerConnectedEvent event) {
        final ProxiedPlayer player = event.getPlayer();

        playerInformation.invalidate(player.getUniqueId());
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        final ProxiedPlayer player = event.getPlayer();

        playerInformation.invalidate(player.getUniqueId());
    }

    public <T> Optional<T> getServerInformation(ServerInfo server, Value<T> key) {
        Optional<T> value = getServerDataCache(server.getName()).getValue(key);
        if (!value.isPresent()) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(os);
                out.writeUTF(Constants.subchannelRequestServerVariable);
                out.writeUTF(key.getId());
                out.close();
                server.sendData(Constants.channel, os.toByteArray());
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error while requesting data from bukkit", ex);
            }
        }
        return value;
    }

    public <T> Optional<T> getPlayerInformation(IPlayer player, Value<T> key) {
        Optional<T> value = getPlayerDataCache(player.getUniqueID()).getValue(key);
        if (!value.isPresent()) {
            ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(player.getUniqueID());
            if (proxiedPlayer != null) {
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(os);
                    out.writeUTF(Constants.subchannelRequestPlayerVariable);
                    out.writeUTF(key.getId());
                    out.close();
                    Optional.ofNullable(proxiedPlayer.getServer()).ifPresent(server -> server.sendData(Constants.channel, os.toByteArray()));
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Error while requesting data from bukkit", ex);
                }
            }
        }
        return value;
    }
}
