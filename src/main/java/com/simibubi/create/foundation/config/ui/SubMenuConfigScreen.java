package com.simibubi.create.foundation.config.ui;

import java.awt.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.ui.entries.BooleanEntry;
import com.simibubi.create.foundation.config.ui.entries.EnumEntry;
import com.simibubi.create.foundation.config.ui.entries.NumberEntry;
import com.simibubi.create.foundation.config.ui.entries.ValueEntry;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ConfirmationScreen;
import com.simibubi.create.foundation.gui.DelegatedStencilElement;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widgets.BoxWidget;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.lib.config.Config;
import com.simibubi.create.lib.config.ConfigValue;
import com.simibubi.create.lib.mixin.accessor.AbstractListAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class SubMenuConfigScreen extends ConfigScreen {

//	public final ModConfig.Type type;
//	protected ForgeConfigSpec spec;
//	protected UnmodifiableConfig configGroup;
	protected ConfigScreenList list;

	protected BoxWidget resetAll;
	protected BoxWidget saveChanges;
	protected BoxWidget discardChanges;
	protected BoxWidget goBack;
	protected BoxWidget serverLocked;
	protected int listWidth;
	protected String title;

	public Config config;


	public SubMenuConfigScreen(Screen parent, String title, Config config) {
		super(parent);
//		this.type = type;
//		this.spec = configSpec;
		this.title = title;
//		this.configGroup = configGroup;

		this.config = config;
	}

	public SubMenuConfigScreen(Screen parent, Config config) {
		super(parent);
//		this.type = type;
//		this.spec = configSpec;
		this.title = "root";
//		this.configGroup = configSpec.getValues();

		this.config = config;
	}

	protected void clearChanges() {
		changes.clear();
		list.children()
				.stream()
				.filter(e -> e instanceof ValueEntry)
				.forEach(e -> ((ValueEntry<?>) e).onValueChange());
	}

	protected void saveChanges() {
//		UnmodifiableConfig values = spec.getValues();
		changes.forEach((path, value) -> {
			ConfigValue configValue = (ConfigValue) config.get(path);
			configValue.set(value);
			if (config != AllConfigs.CLIENT.getConfig()) {
				AllPackets.channel.sendToServer(new CConfigureConfigPacket<>(path, value));
			}
		});
		clearChanges();
		config.writeAll();
	}

	protected void resetConfig() {
		config.getAllValues().forEach((value) -> {
//			if (obj instanceof AbstractConfig) {
//				resetConfig((UnmodifiableConfig) obj);
//			} else if (obj instanceof ForgeConfigSpec.ConfigValue<?>) {
//				ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) obj;
//				ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
//
//				if (!configValue.get().equals(valueSpec.getDefault()))
//					changes.put(String.join(".", configValue.getPath()), valueSpec.getDefault());
//			}
			value.set(value.defaultValue);
		});

		list.children()
				.stream()
				.filter(e -> e instanceof ValueEntry)
				.forEach(e -> ((ValueEntry<?>) e).onValueChange());
	config.writeAll();
	}

	@Override
	public void tick() {
		super.tick();
		list.tick();
	}

	@Override
	protected void init() {
		widgets.clear();
		super.init();

		listWidth = Math.min(width - 80, 300);

		int yCenter = height / 2;
		int listL = this.width / 2 - listWidth / 2;
		int listR = this.width / 2 + listWidth / 2;

		resetAll = new BoxWidget(listR + 10, yCenter - 25, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) ->
						new ConfirmationScreen()
								.at(x, y)
								.withText(ITextProperties.plain("You are about to reset all settings for the " + config.name + " config. Are you sure?"))
								.withAction(success -> {
									if (success)
										resetConfig();
								})
								.open(this)
				);

		resetAll.showingElement(AllIcons.I_CONFIG_RESET.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(resetAll)));
		resetAll.getToolTip().add(new StringTextComponent("Reset All"));
		resetAll.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to reset all configs to their default value.", TextFormatting.GRAY, TextFormatting.GRAY));

		saveChanges = new BoxWidget(listL - 30, yCenter - 25, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) -> {
					if (changes.isEmpty())
						return;

					new ConfirmationScreen()
							.at(x, y)
							.withText(ITextProperties.plain("You are about to change " + changes.size() + " value" + (changes.size() != 1 ? "s" : "") + ". Are you sure?"))
							.withAction(success -> {
								if (success)
									saveChanges();
							})
							.open(this);
				});
		saveChanges.showingElement(AllIcons.I_CONFIG_SAVE.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(saveChanges)));
		saveChanges.getToolTip().add(new StringTextComponent("Save Changes"));
		saveChanges.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to save your current changes.", TextFormatting.GRAY, TextFormatting.GRAY));

		discardChanges = new BoxWidget(listL - 30, yCenter + 5, 20, 20)
				.withPadding(2, 2)
				.withCallback((x, y) -> {
					if (changes.isEmpty())
						return;

					new ConfirmationScreen()
							.at(x, y)
							.withText(ITextProperties.plain("You are about to discard " + changes.size() + " unsaved change" + (changes.size() != 1 ? "s" : "") + ". Are you sure?"))
							.withAction(success -> {
								if (success)
									clearChanges();
							})
							.open(this);
				});
		discardChanges.showingElement(AllIcons.I_CONFIG_DISCARD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(discardChanges)));
		discardChanges.getToolTip().add(new StringTextComponent("Discard Changes"));
		discardChanges.getToolTip().addAll(TooltipHelper.cutStringTextComponent("Click here to discard all the changes you made.", TextFormatting.GRAY, TextFormatting.GRAY));

		goBack = new BoxWidget(listL - 30, yCenter + 65, 20, 20)
				.withPadding(2, 2)
				.withCallback(this::attemptBackstep);
		goBack.showingElement(AllIcons.I_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(goBack)));
		goBack.getToolTip().add(new StringTextComponent("Go Back"));

		widgets.add(resetAll);
		widgets.add(saveChanges);
		widgets.add(discardChanges);
		widgets.add(goBack);

		list = new ConfigScreenList(client, listWidth, height - 60, 45, height - 15, 40);
		list.setLeftPos(this.width / 2 - ((AbstractListAccessor) list).getWidth() / 2);

		children.add(list);

		config.updateValuesList();
		config.getAllValues().forEach((configValue) -> {
			String humanKey = toHumanReadable(configValue.key);

//			if (obj instanceof AbstractConfig) {
//				SubMenuEntry entry = new SubMenuEntry(this, humanKey);
//				list.children().add(entry);
//
//			} else if (obj instanceof ForgeConfigSpec.ConfigValue<?>) {
//				ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) obj;
//				ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
				Object value = configValue.get();

				if (value instanceof Boolean) {
					BooleanEntry entry = new BooleanEntry(humanKey, (ConfigValue<Boolean>) configValue);
					list.children().add(entry);
				} else if (value instanceof Enum) {
					EnumEntry entry = new EnumEntry(humanKey, configValue);
					list.children().add(entry);
				} else if (value instanceof Number) {
					NumberEntry<? extends Number> entry = NumberEntry.create(value, humanKey, configValue);
					if (entry != null) {
						list.children().add(entry);
					} else {
						list.children().add(new ConfigScreenList.LabeledEntry("n-" + configValue.getClass().getSimpleName() + "  " + humanKey + " : " + value));
					}
				} else {
					list.children().add(new ConfigScreenList.LabeledEntry(humanKey + " : " + value));
				}
//			}
		});

		//extras for server configs
		if (config == AllConfigs.CLIENT.config)
			return;

		list.isForServer = true;
		boolean canEdit = client != null && client.player != null && client.player.hasPermissionLevel(2);

		Couple<Color> red = Theme.p(Theme.Key.BUTTON_FAIL);
		Couple<Color> green = Theme.p(Theme.Key.BUTTON_SUCCESS);

		DelegatedStencilElement stencil = new DelegatedStencilElement();

		serverLocked = new BoxWidget(listR + 10, yCenter + 5, 20, 20)
				.withPadding(2, 2)
				.showingElement(stencil);

		if (!canEdit) {
			list.children().forEach(e -> e.setEditable(false));
			resetAll.active = false;
			stencil.withStencilRenderer((ms, w, h, alpha) -> AllIcons.I_CONFIG_LOCKED.draw(ms, 0, 0));
			stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90, 8, 0, 16, 16, red));
			serverLocked.withBorderColors(red);
			serverLocked.getToolTip().add(new StringTextComponent("Locked").formatted(TextFormatting.BOLD));
			serverLocked.getToolTip().addAll(TooltipHelper.cutStringTextComponent("You don't have enough permissions to edit the server config. You can still look at the current values here though.", TextFormatting.GRAY, TextFormatting.GRAY));
		} else {
			stencil.withStencilRenderer((ms, w, h, alpha) -> AllIcons.I_CONFIG_UNLOCKED.draw(ms, 0, 0));
			stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90, 8, 0, 16, 16, green));
			serverLocked.withBorderColors(green);
			serverLocked.getToolTip().add(new StringTextComponent("Unlocked").formatted(TextFormatting.BOLD));
			serverLocked.getToolTip().addAll(TooltipHelper.cutStringTextComponent("You have enough permissions to edit the server config. Changes you make here will be synced with the server when you save them.", TextFormatting.GRAY, TextFormatting.GRAY));
		}

		widgets.add(serverLocked);
	}

	@Override
	protected void renderWindow(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		super.renderWindow(ms, mouseX, mouseY, partialTicks);

		int x = width/2;
		drawCenteredString(ms, client.fontRenderer, "Editing config: " + config.name + "@" + title, x, 15, Theme.i(Theme.Key.TEXT));

		list.render(ms, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderWindowForeground(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		super.renderWindowForeground(ms, mouseX, mouseY, partialTicks);
	}

	@Override
	public void resize(@Nonnull Minecraft client, int width, int height) {
		double scroll = list.getScrollAmount();
		init(client, width, height);
		list.setScrollAmount(scroll);
	}

	@Nullable
	@Override
	public IGuiEventListener getFocused() {
		if (ConfigScreenList.currentText != null)
			return ConfigScreenList.currentText;

		return super.getFocused();
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_))
			return true;

		if (code == GLFW.GLFW_KEY_BACKSPACE) {
			attemptBackstep();
		}

		return false;
	}

	private void attemptBackstep() {
		if (!changes.isEmpty() && parent instanceof BaseConfigScreen) {
			new ConfirmationScreen()
					.centered()
					.addText(ITextProperties.plain("You still have " + changes.size() + " unsaved change" + (changes.size() != 1 ? "s" : "") + " for this config."))
					.addText(ITextProperties.plain("Leaving this screen will discard them without saving. Are you sure?"))
					.withAction(success -> {
						if (!success)
							return;

						changes.clear();
						ScreenOpener.open(parent);
					})
					.open(this);
		} else {
			ScreenOpener.open(parent);
		}
	}

	@Override
	public void onClose() {
		if (changes.isEmpty()) {
			super.onClose();
			return;
		}

		new ConfirmationScreen()
				.centered()
				.addText(ITextProperties.plain("You still have " + changes.size() + " unsaved change" + (changes.size() != 1 ? "s" : "") + " for this config."))
				.addText(ITextProperties.plain("Leaving this screen will discard them without saving. Are you sure?"))
				.withAction(success -> {
					if (!success)
						return;

					changes.clear();
					super.onClose();
				})
				.open(this);
	}
}
