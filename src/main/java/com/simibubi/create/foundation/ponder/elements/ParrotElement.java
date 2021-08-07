package com.simibubi.create.foundation.ponder.elements;

import java.util.function.Supplier;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderUI;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.MatrixStacker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class ParrotElement extends AnimatedSceneElement {

	private Vec3 location;
	private Parrot entity;
	private ParrotPose pose;
	private Supplier<? extends ParrotPose> initialPose;

	public static ParrotElement create(Vec3 location, Supplier<? extends ParrotPose> pose) {
		return new ParrotElement(location, pose);
	}

	protected ParrotElement(Vec3 location, Supplier<? extends ParrotPose> pose) {
		this.location = location;
		initialPose = pose;
		setPose(initialPose.get());
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		setPose(initialPose.get());
		entity.setPosRaw(0, 0, 0);
		entity.xo = 0;
		entity.yo = 0;
		entity.zo = 0;
		entity.xOld = 0;
		entity.yOld = 0;
		entity.zOld = 0;
		entity.xRotO = entity.xRot = 0;
		entity.yRotO = entity.yRot = 180;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		if (entity == null) {
			entity = pose.create(scene.getWorld());
			entity.yRotO = entity.yRot = 180;
		}

		entity.tickCount++;
		entity.yHeadRotO = entity.yHeadRot;
		entity.oFlapSpeed = entity.flapSpeed;
		entity.oFlap = entity.flap;
		entity.setOnGround(true);

		entity.xo = entity.getX();
		entity.yo = entity.getY();
		entity.zo = entity.getZ();
		entity.yRotO = entity.yRot;
		entity.xRotO = entity.xRot;

		pose.tick(scene, entity, location);

		entity.xOld = entity.getX();
		entity.yOld = entity.getY();
		entity.zOld = entity.getZ();
	}

	public void setPositionOffset(Vec3 position, boolean immediate) {
		if (entity == null)
			return;
		entity.setPos(position.x, position.y, position.z);
		if (!immediate)
			return;
		entity.xo = position.x;
		entity.yo = position.y;
		entity.zo = position.z;
	}

	public void setRotation(Vec3 eulers, boolean immediate) {
		if (entity == null)
			return;
		entity.xRot = (float) eulers.x;
		entity.yRot = (float) eulers.y;
		if (!immediate)
			return;
		entity.xRotO = entity.xRot;
		entity.yRotO = entity.yRot;
	}

	public Vec3 getPositionOffset() {
		return entity != null ? entity.position() : Vec3.ZERO;
	}

	public Vec3 getRotation() {
		return entity != null ? new Vec3(entity.xRot, entity.yRot, 0) : Vec3.ZERO;
	}

	@Override
	protected void renderLast(PonderWorld world, MultiBufferSource buffer, PoseStack ms, float fade, float pt) {
		EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance()
			.getEntityRenderDispatcher();

		if (entity == null) {
			entity = pose.create(world);
			entity.yRotO = entity.yRot = 180;
		}

		ms.pushPose();
		ms.translate(location.x, location.y, location.z);
		ms.translate(Mth.lerp(pt, entity.xo, entity.getX()),
			Mth.lerp(pt, entity.yo, entity.getY()), Mth.lerp(pt, entity.zo, entity.getZ()));

		MatrixTransformStack.of(ms)
			.rotateY(AngleHelper.angleLerp(pt, entity.yRotO, entity.yRot));

		entityrenderermanager.render(entity, 0, 0, 0, 0, pt, ms, buffer, lightCoordsFromFade(fade));
		ms.popPose();
	}

	public void setPose(ParrotPose pose) {
		this.pose = pose;
	}

	public static abstract class ParrotPose {

		abstract void tick(PonderScene scene, Parrot entity, Vec3 location);

		Parrot create(PonderWorld world) {
			Parrot entity = new Parrot(EntityType.PARROT, world);
			int nextInt = Create.RANDOM.nextInt(5);
			entity.setVariant(nextInt == 1 ? 0 : nextInt); // blue parrots are kinda hard to see
			return entity;
		}

	}

	public static class DancePose extends ParrotPose {

		@Override
		Parrot create(PonderWorld world) {
			Parrot entity = super.create(world);
			entity.setRecordPlayingNearby(BlockPos.ZERO, true);
			return entity;
		}

		@Override
		void tick(PonderScene scene, Parrot entity, Vec3 location) {
			entity.yRotO = entity.yRot;
			entity.yRot -= 2;
		}

	}

	public static class FlappyPose extends ParrotPose {

		@Override
		void tick(PonderScene scene, Parrot entity, Vec3 location) {
			double length = entity.position()
				.subtract(entity.xOld, entity.yOld, entity.zOld)
				.length();
			entity.setOnGround(false);
			double phase = Math.min(length * 15, 8);
			float f = (float) ((PonderUI.ponderTicks % 100) * phase);
			entity.flapSpeed = Mth.sin(f) + 1;
			if (length == 0)
				entity.flapSpeed = 0;
		}

	}

	public static class SpinOnComponentPose extends ParrotPose {

		private BlockPos componentPos;

		public SpinOnComponentPose(BlockPos componentPos) {
			this.componentPos = componentPos;
		}

		@Override
		void tick(PonderScene scene, Parrot entity, Vec3 location) {
			BlockEntity tileEntity = scene.getWorld()
				.getBlockEntity(componentPos);
			if (!(tileEntity instanceof KineticTileEntity))
				return;
			float rpm = ((KineticTileEntity) tileEntity).getSpeed();
			entity.yRotO = entity.yRot;
			entity.yRot += (rpm * .3f);
		}

	}

	public static abstract class FaceVecPose extends ParrotPose {

		@Override
		void tick(PonderScene scene, Parrot entity, Vec3 location) {
			Vec3 p_200602_2_ = getFacedVec(scene);
			Vec3 Vector3d = location.add(entity.getEyePosition(0));
			double d0 = p_200602_2_.x - Vector3d.x;
			double d1 = p_200602_2_.y - Vector3d.y;
			double d2 = p_200602_2_.z - Vector3d.z;
			double d3 = (double) Mth.sqrt(d0 * d0 + d2 * d2);
			float targetPitch =
				Mth.wrapDegrees((float) -(Mth.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
			float targetYaw =
				Mth.wrapDegrees((float) -(Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) + 90);

			entity.xRot = AngleHelper.angleLerp(.4f, entity.xRot, targetPitch);
			entity.yRot = AngleHelper.angleLerp(.4f, entity.yRot, targetYaw);
		}

		protected abstract Vec3 getFacedVec(PonderScene scene);

	}

	public static class FacePointOfInterestPose extends FaceVecPose {

		@Override
		protected Vec3 getFacedVec(PonderScene scene) {
			return scene.getPointOfInterest();
		}

	}

	public static class FaceCursorPose extends FaceVecPose {

		@Override
		protected Vec3 getFacedVec(PonderScene scene) {
			Minecraft minecraft = Minecraft.getInstance();
			Window w = minecraft.getWindow();
			double mouseX = minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / w.getScreenWidth();
			double mouseY = minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / w.getScreenHeight();
			return scene.getTransform()
				.screenToScene(mouseX, mouseY, 300, 0);
		}

	}

}
