package com.simibubi.create.foundation.block.render;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.DynamicItemRenderer;

import com.simibubi.create.foundation.item.PartialItemModelRenderer;

public class CustomRenderedItemModelRenderer<M extends CustomRenderedItemModel> implements DynamicItemRenderer {

	@Override
	@SuppressWarnings("unchecked")
	public void render(ItemStack stack, ItemCameraTransforms.TransformType p_239207_2_, MatrixStack ms, IRenderTypeBuffer buffer, int light, int overlay) {
		M mainModel = ((M) Minecraft.getInstance()
			.getItemRenderer()
			.getItemModelWithOverrides(stack, null, null));
		PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, p_239207_2_, ms, buffer, overlay);

		ms.push();
		ms.translate(0.5F, 0.5F, 0.5F);
		render(stack, mainModel, renderer, ms, buffer, light, overlay);
		ms.pop();
	}

	protected void render(ItemStack stack, M model, PartialItemModelRenderer renderer, MatrixStack ms,
		IRenderTypeBuffer buffer, int light, int overlay) {

	}

}
