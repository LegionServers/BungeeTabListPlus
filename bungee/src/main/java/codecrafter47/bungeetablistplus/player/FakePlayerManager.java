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

package codecrafter47.bungeetablistplus.player;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class FakePlayerManager implements IPlayerProvider {
    private List<IPlayer> online = new ArrayList<>();
    private List<String> offline;
    private final Plugin plugin;

    public FakePlayerManager(final Plugin plugin) {
        this.plugin = plugin;
        if (BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().fakePlayers.size() > 0) {
            offline = new ArrayList<>(BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().fakePlayers);
            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    triggerRandomEvent();
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    private void triggerRandomEvent() {
        if (Math.random() < 0.3 && online.size() > 0) {
            // do a server switch
            FakePlayer player = (FakePlayer) online.get((int) (Math.random() * online.size()));
            player.server = new ArrayList<>(plugin.getProxy().getServers().values()).get((int) (Math.random() * plugin.getProxy().getServers().values().size()));
        } else if (Math.random() < 0.9 && offline.size() > 0) {
            // add player
            String name = offline.get((int) (Math.random() * offline.size()));
            FakePlayer player = new FakePlayer();
            player.name = name;
            player.server = new ArrayList<>(plugin.getProxy().getServers().values()).get((int) (Math.random() * plugin.getProxy().getServers().values().size()));
            offline.remove(name);
            online.add(player);
        } else if (online.size() > 0) {
            // remove player
            offline.add(online.remove((int) (online.size() * Math.random())).getName());
        }
    }

    public void reload() {
        offline = new ArrayList<>(BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().fakePlayers);
        online = new ArrayList<>();
        for (int i = offline.size(); i > 0; i--) {
            triggerRandomEvent();
        }
    }

    @Override
    public Collection<IPlayer> getPlayers() {
        return Collections.unmodifiableCollection(online);
    }

}
