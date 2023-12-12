package com.natamus.netherportalspread.forge.events;

import com.natamus.collective.functions.WorldFunctions;
import com.natamus.netherportalspread.events.SpreadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.world.BlockEvent.PortalSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ForgeSpreadEvent {
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent e) {
		Level level = e.world;
		if (level.isClientSide || !e.phase.equals(Phase.END)) {
			return;
		}

		SpreadEvent.onWorldTick((ServerLevel)level);
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getWorld());
		if (level == null) {
			return;
		}

		SpreadEvent.onWorldLoad((ServerLevel)level);
	}

	@SubscribeEvent
	public void onPortalSpawn(PortalSpawnEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getWorld());
		if (level == null) {
			return;
		}

		SpreadEvent.onPortalSpawn(level, e.getPos(), e.getPortalSize());
	}
	
	@SubscribeEvent
	public void onDimensionChange(PlayerChangedDimensionEvent e) {
		Player player = e.getPlayer();
    	Level level = player.getCommandSenderWorld();
    	if (level.isClientSide) {
    		return;
    	}

		SpreadEvent.onDimensionChange((ServerLevel)level, (ServerPlayer)player);
	}
}
