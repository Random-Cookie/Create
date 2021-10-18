package com.simibubi.create.lib.helper;

import com.simibubi.create.lib.event.RenderTooltipBorderColorCallback;
import com.simibubi.create.lib.mixin.accessor.ScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Unique;

public final class ScreenHelper {
	public static final int DEFAULT_BORDER_COLOR_START = 1347420415;
	public static final int DEFAULT_BORDER_COLOR_END = 1344798847;
	public static RenderTooltipBorderColorCallback.BorderColorEntry CURRENT_COLOR;

	public static Minecraft getClient(Screen screen) {
		return ((ScreenAccessor) screen).create$client();
	}
}
