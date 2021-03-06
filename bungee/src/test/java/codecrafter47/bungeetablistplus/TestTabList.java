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

package codecrafter47.bungeetablistplus;

import codecrafter47.bungeetablistplus.api.ITabList;
import codecrafter47.bungeetablistplus.api.Slot;
import codecrafter47.bungeetablistplus.tablist.TabList;
import org.junit.Assert;
import org.junit.Test;

public class TestTabList {

    @Test
    public void testTabList() {
        ITabList tabList = new TabList(20, 3);
        Slot slot1 = new Slot("Test");
        tabList.setSlot(0, 2, slot1);
        Assert.assertEquals(tabList.getSlot(0, 2), slot1);
        Assert.assertEquals(tabList.flip().getSlot(2, 0), slot1);
        Assert.assertEquals(tabList.getSize(), 60);
        Assert.assertEquals(tabList.getUsedSlots(), 3);
        Assert.assertEquals(tabList.flip().getUsedSlots(), 41);
    }
}
