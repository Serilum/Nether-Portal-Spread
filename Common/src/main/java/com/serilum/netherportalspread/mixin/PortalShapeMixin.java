package com.serilum.netherportalspread.mixin;

import com.natamus.collective.functions.WorldFunctions;
import com.serilum.netherportalspread.events.SpreadEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PortalShape.class, priority = 1001)
public class PortalShapeMixin {
	@Shadow private @Final LevelAccessor level;
	@Shadow private BlockPos bottomLeft;

	@Inject(method = "createPortalBlocks()V", at = @At(value = "TAIL"))
	public void createPortalBlocks(CallbackInfo ci) {
		SpreadEvent.onPortalSpawn(WorldFunctions.getWorldIfInstanceOfAndNotRemote(level), bottomLeft);
	}
}
