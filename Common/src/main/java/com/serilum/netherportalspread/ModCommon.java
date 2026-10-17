package com.serilum.netherportalspread;

import com.natamus.collective.globalcallbacks.ConfigCallback;
import com.serilum.netherportalspread.config.ConfigHandler;
import com.serilum.netherportalspread.util.Reference;
import com.serilum.netherportalspread.util.Util;

public class ModCommon {

	public static void init() {
		ConfigHandler.initConfig();
		load();
	}

	private static void load() {
		ConfigCallback.CONFIG_CHANGED.register((String modid) -> {
			if (!modid.equals(Reference.MOD_ID)) {
				return;
			}

			Util.preventSpreadBlock = null;
		});
	}
}