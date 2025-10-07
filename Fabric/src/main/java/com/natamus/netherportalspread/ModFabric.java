package com.natamus.netherportalspread;

import com.natamus.collective.check.RegisterMod;
import com.natamus.collective.check.ShouldLoadCheck;
import com.natamus.collective.fabric.callbacks.CollectiveBlockEvents;
import com.natamus.collective.fabric.callbacks.CollectivePlayerEvents;
import com.natamus.netherportalspread.events.SpreadEvent;
import com.natamus.netherportalspread.util.Reference;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalShape;

public class ModFabric implements ModInitializer {
	
	@Override
	public void onInitialize() {
		if (!ShouldLoadCheck.shouldLoad(Reference.MOD_ID)) {
			return;
		}

		setGlobalConstants();
		ModCommon.init();

		loadEvents();

		RegisterMod.register(Reference.NAME, Reference.MOD_ID, Reference.VERSION, Reference.ACCEPTED_VERSIONS);
	}

	private void loadEvents() {
		ServerWorldEvents.LOAD.register((MinecraftServer server, ServerLevel world) -> {
			SpreadEvent.onWorldLoad(world);
		});

		ServerTickEvents.START_WORLD_TICK.register((ServerLevel world) -> {
			SpreadEvent.onWorldTick(world);
		});

		CollectiveBlockEvents.ON_NETHER_PORTAL_SPAWN.register((Level world, BlockPos pos, PortalShape shape) -> {
			SpreadEvent.onPortalSpawn(world, pos, shape);
		});

		CollectivePlayerEvents.PLAYER_CHANGE_DIMENSION.register((ServerLevel world, ServerPlayer player) -> {
			SpreadEvent.onDimensionChange(world, player);
		});
	}

	private static void setGlobalConstants() {

	}
}
