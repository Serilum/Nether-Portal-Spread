package com.natamus.netherportalspread.config;

import com.natamus.collective.config.DuskConfig;
import com.natamus.netherportalspread.util.Reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler extends DuskConfig {
	public static HashMap<String, List<String>> configMetaData = new HashMap<String, List<String>>();

	@Entry public static boolean sendMessageOnPortalCreation = true;
	@Entry public static String messageOnPortalCreation = "You feel a corrupted energy coming from the portal. The nether will slowly spread into the overworld unless %preventSpreadBlockAmountNeeded% %preventSpreadBlockString% are placed within a %portalSpreadRadius% block radius around the portal.";
	@Entry public static boolean sendMessageOnPreventSpreadBlocksFound = true;
	@Entry public static String messageOnPreventSpreadBlocksFound = "With enough %preventSpreadBlockString% placed, you feel the corrupted energy fade.";
	@Entry public static boolean sendMessageOnPortalBroken = true;
	@Entry public static String messageOnPortalBroken = "With the nether portal broken, the corrupted energy is no longer able to enter the overworld.";
	@Entry public static boolean prefixPortalCoordsInMessage = true;
	@Entry(min = 1, max = 100) public static int portalSpreadRadius = 30;
	@Entry(min = 1, max = 72000) public static int spreadDelayTicks = 40;
	@Entry(min = 0, max = 1000) public static int instantConvertAmount = 50;
	@Entry public static boolean preventSpreadWithBlock = true;
	@Entry(min = 1, max = 100) public static int preventSpreadBlockAmountNeeded = 4;
	@Entry public static String preventSpreadBlockString = "minecraft:coal_block";

	public static void initConfig() {
		configMetaData.put("sendMessageOnPortalCreation", Arrays.asList(
			"When enabled, sends a message to players around the portal that the nether is spreading and that you can stop the spread with 'preventSpreadBlockAmountNeeded' of the 'preventSpreadBlockString' block."
		));
		configMetaData.put("messageOnPortalCreation", Arrays.asList(
			"The message sent on portal creation."
		));
		configMetaData.put("sendMessageOnPreventSpreadBlocksFound", Arrays.asList(
			"When enabled, sends a message to players around the portal that the nether spread has stopped when the portal detects new 'preventSpreadBlockString' blocks."
		));
		configMetaData.put("messageOnPreventSpreadBlocksFound", Arrays.asList(
			"The message sent on preventspread blocks found."
		));
		configMetaData.put("sendMessageOnPortalBroken", Arrays.asList(
			"When enabled, sends a message to players around the portal when it is broken."
		));
		configMetaData.put("messageOnPortalBroken", Arrays.asList(
			"The message sent when a portal is broken."
		));
		configMetaData.put("prefixPortalCoordsInMessage", Arrays.asList(
			"When enabled, shows the portal coordinates in portal messages."
		));
		configMetaData.put("portalSpreadRadius", Arrays.asList(
			"The radius around the portal to which the nether blocks can spread."
		));
		configMetaData.put("spreadDelayTicks", Arrays.asList(
			"The delay in ticks in between the spread around the nether portal. 20 ticks = 1 second."
		));
		configMetaData.put("instantConvertAmount", Arrays.asList(
			"The amount of blocks that are instantly converted to a nether block around a portal when it is detected. If there are existing nether blocks within the radius, their count is substracted from this number."
		));
		configMetaData.put("preventSpreadWithBlock", Arrays.asList(
			"When enabled, blocks the spread effect when there are n (defined) prevent-spread-blocks (defined) within the radius."
		));
		configMetaData.put("preventSpreadBlockAmountNeeded", Arrays.asList(
			"The amount of prevent-spread-blocks (defined) needed within the radius of the nether portal to prevent spread."
		));
		configMetaData.put("preventSpreadBlockString", Arrays.asList(
			"The block which prevents the nether portal from spreading. By default a coal block (minecraft:coal_block is the namespace ID)."
		));

		DuskConfig.init(Reference.NAME, Reference.MOD_ID, ConfigHandler.class);
	}
}