package com.serilum.netherportalspread.events;

import com.natamus.collective.functions.HashMapFunctions;
import com.natamus.collective.functions.WorldFunctions;
import com.serilum.netherportalspread.config.ConfigHandler;
import com.serilum.netherportalspread.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpreadEvent {
	private static final HashMap<Level, CopyOnWriteArrayList<BlockPos>> portalsToProcess = new HashMap<Level, CopyOnWriteArrayList<BlockPos>>();
	private static final HashMap<Level, Integer> levelTicks = new HashMap<Level, Integer>();

	public static void onWorldLoad(ServerLevel level) {
		Util.attemptSpreadBlockProcess(level);

		if (!WorldFunctions.isNether(level)) {
			Util.loadPortalsFromWorld(level);
		}
	}

	public static void onWorldTick(ServerLevel level) {
		if (WorldFunctions.isNether(level)) {
			return;
		}

		if (HashMapFunctions.computeIfAbsent(portalsToProcess, level, k -> new CopyOnWriteArrayList<BlockPos>()).size() > 0) {
			BlockPos portal = portalsToProcess.get(level).get(0);

			if (!HashMapFunctions.computeIfAbsent(Util.portals, level, k -> new CopyOnWriteArrayList<BlockPos>()).contains(portal) && !HashMapFunctions.computeIfAbsent(Util.preventedPortals, level, k -> new HashMap<BlockPos, Boolean>()).containsKey(portal)) {
				Util.validatePortalAndAdd(level, portal);
			}

			portalsToProcess.get(level).remove(0);
		}

		int levelTick = HashMapFunctions.computeIfAbsent(levelTicks, level, k -> 1);
		if (levelTick % ConfigHandler.spreadDelayTicks != 0) {
			levelTicks.put(level, levelTick+1);
			return;
		}
		levelTicks.put(level, 1);

		for (BlockPos portal : HashMapFunctions.computeIfAbsent(Util.portals, level, k -> new CopyOnWriteArrayList<BlockPos>())) {
			Util.spreadNextBlock(level, portal);
		}
	}

	public static void onPortalSpawn(Level level, BlockPos pos) {
		if (level == null) {
			return;
		}

		if (level.isClientSide) {
			return;
		}

		if (WorldFunctions.isNether(level)) {
			return;
		}

		HashMapFunctions.computeIfAbsent(portalsToProcess, level, k -> new CopyOnWriteArrayList<BlockPos>()).add(pos);
	}

	public static void onDimensionChange(ServerLevel level, ServerPlayer player) {
		if (level.isClientSide) {
			return;
		}

		if (WorldFunctions.isNether(level)) {
			return;
		}

		BlockPos playerPos = player.blockPosition();
		HashMapFunctions.computeIfAbsent(portalsToProcess, level, k -> new CopyOnWriteArrayList<BlockPos>()).add(playerPos);
	}
}
