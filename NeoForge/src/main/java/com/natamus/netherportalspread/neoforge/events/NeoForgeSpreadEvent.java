package com.natamus.netherportalspread.neoforge.events;

import com.natamus.collective.functions.WorldFunctions;
import com.natamus.netherportalspread.events.SpreadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.TickEvent.LevelTickEvent;
import net.neoforged.neoforge.event.TickEvent.Phase;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent.PortalSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class NeoForgeSpreadEvent {
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent e) {
		Level level = e.level;
		if (level.isClientSide || !e.phase.equals(Phase.END)) {
			return;
		}

		SpreadEvent.onWorldTick((ServerLevel)level);
	}
	
	@SubscribeEvent
	public static void onWorldLoad(LevelEvent.Load e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		SpreadEvent.onWorldLoad((ServerLevel)level);
	}

	@SubscribeEvent
	public static void onPortalSpawn(PortalSpawnEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		SpreadEvent.onPortalSpawn(level, e.getPos(), e.getPortalSize());
	}
	
	@SubscribeEvent
	public static void onDimensionChange(PlayerChangedDimensionEvent e) {
		Player player = e.getEntity();
		Level level = player.level();
		if (level.isClientSide) {
			return;
		}

		SpreadEvent.onDimensionChange((ServerLevel)level, (ServerPlayer)player);
	}
}
