package com.serilum.netherportalspread.util;

import com.natamus.collective.functions.*;
import com.natamus.collective.objects.RandomCollection;
import com.serilum.netherportalspread.config.ConfigHandler;
import com.serilum.netherportalspread.data.Constants;
import com.serilum.netherportalspread.data.Variables;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Util {
	public static HashMap<Level, CopyOnWriteArrayList<BlockPos>> portals = new HashMap<Level, CopyOnWriteArrayList<BlockPos>>();
	public static HashMap<Level, HashMap<BlockPos, Boolean>> preventedPortals = new HashMap<Level, HashMap<BlockPos, Boolean>>();

	private static final HashMap<Block, HashMap<Block, Double>> convertInto = new HashMap<Block, HashMap<Block, Double>>();
	private static final List<Block> convertBlocks = new ArrayList<Block>();
	private static final List<Block> convertedBlocks = new ArrayList<Block>();
	public static Block preventSpreadBlock = null;

	public static void attemptSpreadBlockProcess(Level level) {
		if (!Variables.processedSpreadBlockLoad) {
			try {
				loadSpreadBlocks(level);
				Variables.processedSpreadBlockLoad = true;
			} catch (IOException ex) {
				Constants.logger.warn("[" + Reference.NAME + "] Something went wrong loading the nether spread block config.");
			}
		}
	}

	public static void loadSpreadBlocks(Level level) throws IOException {
		Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);

		String dirPath = DataFunctions.getConfigDirectory() + File.separator + "netherportalspread";
		File dir = new File(dirPath);
		File file = new File(dirPath + File.separator + "spreadsettings.txt");

		if (dir.isDirectory() && file.isFile()) {
			String spreadSettings = new String(Files.readAllBytes(Paths.get(dirPath + File.separator + "spreadsettings.txt")));
			spreadSettings = spreadSettings.replace("\n", "").replace("\r", "").replace(" ", ""); // remove newlines, tabs and spaces

			for (String line : spreadSettings.split(",")) {
				if (line.length() < 4) {
					Constants.logger.warn("[" + Reference.NAME + "] The spread settings contain an empty line. Ignoring it.");
					continue;
				}

				String[] lineSplit = line.split(";");
				if (lineSplit.length != 2) {
					Constants.logger.warn("[" + Reference.NAME + "] The spread settings line '" + line + "' contains errors. Ignoring it.");
					continue;
				}

				String fromBlockString = lineSplit[0];
				if (!fromBlockString.contains(":")) {
					fromBlockString = "minecraft:" + fromBlockString;
				}
				ResourceLocation fromBlockIdentifier = new ResourceLocation(fromBlockString);
				if (!blockRegistry.keySet().contains(fromBlockIdentifier)) {
					Constants.logger.warn("[" + Reference.NAME + "] Unable to find from-block '" + fromBlockString + "' in the block registry. Ignoring it.");
					continue;
				}
				Block fromBlock = blockRegistry.get(fromBlockIdentifier);

				String toBlocks = lineSplit[1].replace("[", "").replace("]", "");

				double totalWeight = 0;
				HashMap<Block, Double> toBlockWeights = new HashMap<Block, Double>();
				for (String toBlockEntry : toBlocks.split("\\+")) {
					String[] toBlockEntrySplit = toBlockEntry.split(">");
					if (toBlockEntrySplit.length < 2) {
						continue;
					}

					String toBlockString = toBlockEntrySplit[0];
					if (!toBlockString.contains(":")) {
						toBlockString = "minecraft:" + toBlockString;
					}

					double weight = 1.0;
					try {
						weight = Double.parseDouble(toBlockEntrySplit[1]);
					}
					catch (NumberFormatException ignored) { }
					totalWeight += weight;

					ResourceLocation toBlockIdentifier = new ResourceLocation(toBlockString);
					if (blockRegistry.keySet().contains(toBlockIdentifier)) {
						toBlockWeights.put(blockRegistry.get(toBlockIdentifier), weight);
					}
					else {
						Constants.logger.warn("[" + Reference.NAME + "] Unable to find to-block '" + toBlockString + "' in the block registry. Ignoring it.");
					}
				}

				if (toBlockWeights.size() == 0 || totalWeight == 0) {
					Constants.logger.warn("[" + Reference.NAME + "] The spread settings line '" + line + "' contains errors, no convert blocks were found. Ignoring it.");
				}
				else {
					for (Block key : toBlockWeights.keySet()) {
						Double weightValue = toBlockWeights.get(key);
						toBlockWeights.put(key, (1/totalWeight)*weightValue);
					}
					convertInto.put(fromBlock, toBlockWeights);
				}
			}

			for (Block fromBlock : convertInto.keySet()) {
				convertBlocks.add(fromBlock);
				convertedBlocks.addAll(convertInto.get(fromBlock).keySet());
			}
		}
		else {
			dir.mkdirs();

			List<String> defaultConfig = DefaultConfigs.getDefaultConfigForVersion(Reference.ACCEPTED_VERSIONS);

			PrintWriter writer = new PrintWriter(dirPath + File.separator + "spreadsettings.txt", StandardCharsets.UTF_8);
			for (String line : defaultConfig) {
				writer.println(line);
			}
			writer.close();

			loadSpreadBlocks(level);
		}
	}

	private static Block getPreventSpreadBlock(Level level) {
		Block cachedBlock = preventSpreadBlock;
		if (cachedBlock != null) {
			return cachedBlock;
		}

		Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);
		String preventBlockString = ConfigHandler.preventSpreadBlockString.strip().replaceAll("[^a-z0-9_.-:]", "");
		try {
			ResourceLocation preventBlockIdentifier = new ResourceLocation(preventBlockString);
			if (blockRegistry.keySet().contains(preventBlockIdentifier)) {
				Block registryBlock = blockRegistry.get(preventBlockIdentifier);
				preventSpreadBlock = registryBlock;
				return registryBlock;
			}
		}
		catch (ResourceLocationException ignored) { }

		Constants.logger.warn("[" + Reference.NAME + "] Unable to get a prevent-spread-block from the string '" + preventBlockString + "'. Using the default coal block instead.");
		preventSpreadBlock = Blocks.COAL_BLOCK;
		return Blocks.COAL_BLOCK;
	}

	public static void loadPortalsFromWorld(Level level) {
		String portalFolder = WorldFunctions.getWorldPath((ServerLevel)level) + File.separator + "data" + File.separator + "netherportalspread" + File.separator + "portals";
		String specificPortalFolder = portalFolder + File.separator + DimensionFunctions.getSimpleDimensionString(level);

		if (specificPortalFolder.endsWith("overworld")) {
			final File sourceDir = new File(portalFolder);
			final File destinationDir = new File(specificPortalFolder);
			destinationDir.mkdirs();

			File[] files = sourceDir.listFiles((File pathname) -> pathname.getName().endsWith(".portal"));

			for (File f : files ) {
				Path sourcePath = Paths.get(sourceDir.getAbsolutePath() + File.separator + f.getName());
				Path destinationPath = Paths.get(destinationDir.getAbsolutePath() + File.separator + f.getName());

				try {
					Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ignored) { }
			}
		}

		File dir = new File(specificPortalFolder);
		dir.mkdirs();

		File[] listOfFiles = dir.listFiles();
		if (listOfFiles == null) {
			return;
		}

		for (File listOfFile : listOfFiles) {
			String fileName = listOfFile.getName();
			if (fileName.endsWith(".portal")) {
				String[] cs = fileName.replace(".portal", "").split("_");
				if (cs.length == 3) {
					if (NumberFunctions.isNumeric(cs[0]) && NumberFunctions.isNumeric(cs[1]) && NumberFunctions.isNumeric(cs[2])) {
						int x = Integer.parseInt(cs[0]);
						int y = Integer.parseInt(cs[1]);
						int z = Integer.parseInt(cs[2]);

						BlockPos portal = new BlockPos(x, y, z);
						HashMapFunctions.computeIfAbsent(portals, level, k -> new CopyOnWriteArrayList<BlockPos>()).add(portal);
					}
				}
			}
		}
	}

	public static void savePortalToWorld(Level level, BlockPos portal) {
		String portalFolder = WorldFunctions.getWorldPath((ServerLevel)level) + File.separator + "data" + File.separator + "netherportalspread" + File.separator + "portals" + File.separator + DimensionFunctions.getSimpleDimensionString(level);
		File dir = new File(portalFolder);
		dir.mkdirs();

		String fileName = portal.getX() + "_" + portal.getY() + "_" + portal.getZ() + ".portal";
		try {
			PrintWriter writer = new PrintWriter(portalFolder + File.separator + fileName, StandardCharsets.UTF_8);
			writer.close();
		} catch (Exception e) {
			Constants.logger.error("[" + Reference.NAME + "] Something went wrong while saving a portal location.");
		}
	}

	private static Boolean portalExists(Level level, BlockPos pos) {
		for (BlockPos portalPos : HashMapFunctions.computeIfAbsent(portals, level, k -> new CopyOnWriteArrayList<BlockPos>())) {
			double distance = pos.distSqr(new Vec3i(portalPos.getX(), portalPos.getY(), portalPos.getZ()));
			if (distance < 10) {
				return true;
			}
		}
		return false;
	}

	public static void validatePortalAndAdd(Level level, BlockPos p) {
		BlockPos rawPortal = null;

		int r = 3;
		for (BlockPos nextPos : BlockPos.betweenClosed(p.getX()-r, p.getY()-r, p.getZ()-r, p.getX()+r, p.getY()+r, p.getZ()+r)) {
			BlockState blockState = level.getBlockState(nextPos);
			if (isPortalBlock(blockState)) {
				rawPortal = nextPos.immutable();
				break;
			}
		}

		if (rawPortal == null) {
			return;
		}

		while (isPortalBlock(level.getBlockState(rawPortal.below()))) {
			rawPortal = rawPortal.below().immutable();
		}
		while (isPortalBlock(level.getBlockState(rawPortal.west()))) {
			rawPortal = rawPortal.west().immutable();
		}
		while (isPortalBlock(level.getBlockState(rawPortal.north()))) {
			rawPortal = rawPortal.north().immutable();
		}

		if (HashMapFunctions.computeIfAbsent(portals, level, k -> new CopyOnWriteArrayList<BlockPos>()).contains(rawPortal) || HashMapFunctions.computeIfAbsent(preventedPortals, level, k -> new HashMap<BlockPos, Boolean>()).containsKey(rawPortal)) {
			return;
		}

		if (portalExists(level, p)) {
			return;
		}

		sendSpreadingMessage(level, rawPortal);
		preventedPortals.get(level).put(rawPortal, false);
		portals.get(level).add(rawPortal);
		savePortalToWorld(level, rawPortal);

		int netherBlockCount = countNetherBlocks(level, rawPortal);
		if (netherBlockCount < ConfigHandler.instantConvertAmount) {
			while (netherBlockCount < ConfigHandler.instantConvertAmount) {
				if (!spreadNextBlock(level, rawPortal)) {
					break;
				}

				netherBlockCount+=1;
			}
		}
	}

	private static boolean isPortalBlock(BlockState blockState) {
		return blockState.getBlock().equals(Blocks.NETHER_PORTAL);
	}

	public static void removePortal(Level level, BlockPos portal) {
		String portalFolder = WorldFunctions.getWorldPath((ServerLevel)level) + File.separator + "data" + File.separator + "netherportalspread" + File.separator + "portals" + File.separator + DimensionFunctions.getSimpleDimensionString(level);
		String fileName = portal.getX() + "_" + portal.getY() + "_" + portal.getZ() + ".portal";
		File filePath = new File(portalFolder + File.separator + fileName);
		try {
			Files.deleteIfExists(filePath.toPath());
		} catch (Exception e) {
			Constants.logger.error("[" + Reference.NAME + "] Something went wrong while removing an old portal location.");
		}

		HashMapFunctions.computeIfAbsent(portals, level, k -> new CopyOnWriteArrayList<BlockPos>()).remove(portal);
		HashMapFunctions.computeIfAbsent(preventedPortals, level, k -> new HashMap<BlockPos, Boolean>()).remove(portal);
		sendBrokenPortalMessage(level, portal);
	}

	public static Boolean spreadNextBlock(Level level, BlockPos portal) {
		if (!level.hasChunk(portal.getX() >> 4, portal.getZ() >> 4)) {
			return true;
		}

		BlockState portalBlockState = level.getBlockState(portal);
		if (portalBlockState == null) {
			return false;
		}

		if (!CompareBlockFunctions.isPortalBlock(portalBlockState.getBlock())) {
			removePortal(level, portal);
			return false;
		}

		HashMap<BlockPos, Boolean> levelPreventedPortals = HashMapFunctions.computeIfAbsent(preventedPortals, level, k -> new HashMap<BlockPos, Boolean>());
		boolean spreadStateKnown = levelPreventedPortals.containsKey(portal);
		boolean prevented = spreadStateKnown && levelPreventedPortals.get(portal);

		HashMap<SectionPos, LevelChunkSection> sections = getLoadedSectionsAround(level, portal);
		if (ConfigHandler.preventSpreadWithBlock) {
			if (countPreventSpreadBlocks(level, portal, sections) >= ConfigHandler.preventSpreadBlockAmountNeeded) {
				if (spreadStateKnown && !prevented) {
					sendPreventedMessage(level, portal);
				}

				levelPreventedPortals.put(portal, true);
				return true;
			}
		}

		BlockPos closest = findClosestConvertBlock(portal, sections);
		if (closest != null) {
			if (prevented) {
				sendSpreadingMessage(level, portal);
			}

			levelPreventedPortals.put(portal, false);
			spreadNetherToBlock(level, closest);
			return true;
		}
		return false;
	}

	private static HashMap<SectionPos, LevelChunkSection> getLoadedSectionsAround(Level level, BlockPos portal) {
		HashMap<SectionPos, LevelChunkSection> sections = new HashMap<>();
		int r = ConfigHandler.portalSpreadRadius;
		int minSectionY = SectionPos.blockToSectionCoord(portal.getY() - r);
		int maxSectionY = SectionPos.blockToSectionCoord(portal.getY() + r);

		for (int sectionX = SectionPos.blockToSectionCoord(portal.getX() - r); sectionX <= SectionPos.blockToSectionCoord(portal.getX() + r); sectionX++) {
			for (int sectionZ = SectionPos.blockToSectionCoord(portal.getZ() - r); sectionZ <= SectionPos.blockToSectionCoord(portal.getZ() + r); sectionZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
				if (chunk == null) {
					continue;
				}

				for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
					int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
					if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
						continue;
					}

					LevelChunkSection section = chunk.getSection(sectionIndex);
					if (!section.hasOnlyAir()) {
						sections.put(SectionPos.of(sectionX, sectionY, sectionZ), section);
					}
				}
			}
		}

		return sections;
	}

	private static Iterable<BlockPos> getPositionsInRadius(SectionPos sectionPos, BlockPos portal) {
		int r = ConfigHandler.portalSpreadRadius;
		int minX = Math.max(sectionPos.minBlockX(), portal.getX() - r);
		int minY = Math.max(sectionPos.minBlockY(), portal.getY() - r);
		int minZ = Math.max(sectionPos.minBlockZ(), portal.getZ() - r);
		int maxX = Math.min(sectionPos.maxBlockX(), portal.getX() + r);
		int maxY = Math.min(sectionPos.maxBlockY(), portal.getY() + r);
		int maxZ = Math.min(sectionPos.maxBlockZ(), portal.getZ() + r);
		return BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static double getSectionDistanceSqr(SectionPos sectionPos, BlockPos portal) {
		int dx = Math.max(0, Math.max(sectionPos.minBlockX() - portal.getX(), portal.getX() - sectionPos.maxBlockX()));
		int dy = Math.max(0, Math.max(sectionPos.minBlockY() - portal.getY(), portal.getY() - sectionPos.maxBlockY()));
		int dz = Math.max(0, Math.max(sectionPos.minBlockZ() - portal.getZ(), portal.getZ() - sectionPos.maxBlockZ()));
		return (double)dx*dx + (double)dy*dy + (double)dz*dz;
	}

	private static BlockState getSectionBlockState(LevelChunkSection section, BlockPos pos) {
		return section.getBlockState(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
	}

	private static int countPreventSpreadBlocks(Level level, BlockPos portal, HashMap<SectionPos, LevelChunkSection> sections) {
		Block preventBlock = getPreventSpreadBlock(level);
		int amountNeeded = ConfigHandler.preventSpreadBlockAmountNeeded;
		int preventBlockCount = 0;

		for (Map.Entry<SectionPos, LevelChunkSection> entry : sections.entrySet()) {
			LevelChunkSection section = entry.getValue();
			if (!section.maybeHas(state -> state.is(preventBlock))) {
				continue;
			}

			for (BlockPos pos : getPositionsInRadius(entry.getKey(), portal)) {
				if (getSectionBlockState(section, pos).is(preventBlock)) {
					preventBlockCount++;
					if (preventBlockCount >= amountNeeded) {
						return preventBlockCount;
					}
				}
			}
		}

		return preventBlockCount;
	}

	private static BlockPos findClosestConvertBlock(BlockPos portal, HashMap<SectionPos, LevelChunkSection> sections) {
		List<SectionPos> candidateSections = new ArrayList<>();
		for (Map.Entry<SectionPos, LevelChunkSection> entry : sections.entrySet()) {
			if (entry.getValue().maybeHas(state -> convertBlocks.contains(state.getBlock()))) {
				candidateSections.add(entry.getKey());
			}
		}
		candidateSections.sort(Comparator.comparingDouble(sectionPos -> getSectionDistanceSqr(sectionPos, portal)));

		BlockPos closest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (SectionPos sectionPos : candidateSections) {
			if (getSectionDistanceSqr(sectionPos, portal) >= nearestDistance) {
				break;
			}

			LevelChunkSection section = sections.get(sectionPos);
			for (BlockPos pos : getPositionsInRadius(sectionPos, portal)) {
				double distance = portal.distSqr(pos);
				if (distance < nearestDistance && convertBlocks.contains(getSectionBlockState(section, pos).getBlock())) {
					nearestDistance = distance;
					closest = pos.immutable();
				}
			}
		}

		return closest;
	}

	public static int countNetherBlocks(Level level, BlockPos portal) {
		int netherBlockCount = 0;

		for (Map.Entry<SectionPos, LevelChunkSection> entry : getLoadedSectionsAround(level, portal).entrySet()) {
			LevelChunkSection section = entry.getValue();
			if (!section.maybeHas(state -> convertedBlocks.contains(state.getBlock()))) {
				continue;
			}

			for (BlockPos pos : getPositionsInRadius(entry.getKey(), portal)) {
				if (convertedBlocks.contains(getSectionBlockState(section, pos).getBlock())) {
					netherBlockCount++;
				}
			}
		}

		return netherBlockCount;
	}

	public static void spreadNetherToBlock(Level level, BlockPos pos) {
		if (level == null) {
			return;
		}

		BlockState currentBlockState = level.getBlockState(pos);
		BlockState newBlockState = null;

		Block curBlock = currentBlockState.getBlock();

		if (convertBlocks.contains(curBlock)) {
			// key: from-block, value: HashMap of to-block and weight. so if two had weight 10 and 5, (1/15)*10 == 66.65% and 33.35% chance.
			RandomCollection<Block> randomCollection = new RandomCollection<>(); //.add(40, "a").add(35, "b").add(25, "c");
			HashMap<Block, Double> toBlockWeights = convertInto.get(curBlock);
			for (Block toBlock : toBlockWeights.keySet()) {
				Double weight = toBlockWeights.get(toBlock);
				randomCollection.add(weight*100, toBlock);
			}

			newBlockState = randomCollection.next().withPropertiesOf(currentBlockState);
		}

		if (newBlockState != null) {
			level.setBlockAndUpdate(pos, newBlockState);
		}
	}

	private static void sendSpreadingMessage(Level level, BlockPos p) {
		if (!ConfigHandler.sendMessageOnPortalCreation) {
			return;
		}

		String message = ConfigHandler.messageOnPortalCreation;
		MessageFunctions.sendMessageToPlayersAround(level, p, ConfigHandler.portalSpreadRadius, formatAroundString(level, message, ConfigHandler.preventSpreadBlockAmountNeeded, p), ChatFormatting.RED);
	}

	private static void sendPreventedMessage(Level level, BlockPos p) {
		if (!ConfigHandler.sendMessageOnPreventSpreadBlocksFound) {
			return;
		}

		String message = ConfigHandler.messageOnPreventSpreadBlocksFound;
		MessageFunctions.sendMessageToPlayersAround(level, p, ConfigHandler.portalSpreadRadius, formatAroundString(level, message, ConfigHandler.preventSpreadBlockAmountNeeded, p), ChatFormatting.DARK_GREEN);
	}

	private static void sendBrokenPortalMessage(Level level, BlockPos p) {
		if (!ConfigHandler.sendMessageOnPortalBroken) {
			return;
		}

		String message = ConfigHandler.messageOnPortalBroken;
		MessageFunctions.sendMessageToPlayersAround(level, p, ConfigHandler.portalSpreadRadius, formatAroundString(level, message, ConfigHandler.preventSpreadBlockAmountNeeded, p), ChatFormatting.DARK_GREEN);
	}
	
	private static String formatAroundString(Level level, String message, int amountNeeded, BlockPos portal) {
		String blockString = BlockFunctions.blockToReadableString(getPreventSpreadBlock(level), amountNeeded);

		message = message.replace("%preventSpreadBlockString%", blockString);
		message = message.replace("%preventSpreadBlockAmountNeeded%", amountNeeded + "");
		message = message.replace("%portalSpreadRadius%", ConfigHandler.portalSpreadRadius + "");
		if (ConfigHandler.prefixPortalCoordsInMessage) {
			message = "Portal {" + portal.getX() + ", " + portal.getY() + ", " + portal.getZ() + "}: " + message;
		}
		return message;
	}
}
