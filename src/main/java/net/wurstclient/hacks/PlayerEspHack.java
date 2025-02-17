/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EspBoxSizeSetting;
import net.wurstclient.settings.EspStyleSetting;
import net.wurstclient.settings.EspStyleSetting.EspStyle;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.FilterInvisibleSetting;
import net.wurstclient.settings.filters.FilterSleepingSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"player esp", "PlayerTracers", "player tracers"})
public final class PlayerEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EspStyleSetting style =
		new EspStyleSetting(EspStyle.LINES_AND_BOXES);
	
	private final EspBoxSizeSetting boxSize = new EspBoxSizeSetting(
		"\u00a7lAccurate\u00a7r mode shows the exact hitbox of each player.\n"
			+ "\u00a7lFancy\u00a7r mode shows slightly larger boxes that look better.");
	
	private final EnumSetting colorMode = new EnumSetting("Color Mode",
		"\u00a7lDistance\u00a7r mode is green the farther away and red the closer.\n"
			+ "\u00a7lName Tag\u00a7r mode is based on the color of the player's name (usually team colors.)",
		ColorMode.values(), ColorMode.DISTANCE);
	
	private final CheckboxSetting blueFriends =
		new CheckboxSetting("Blue Friends",
			"Show friends in blue (only work with distance color mode.)", true);
	
	private final CheckboxSetting showGray = new CheckboxSetting(
		"Show Grayscale", "Show players with no team colors.", false);
	
	private final EntityFilterList entityFilters = new EntityFilterList(
		new FilterSleepingSetting("Won't show sleeping players.", false),
		new FilterInvisibleSetting("Won't show invisible players.", false));
	
	private final ArrayList<PlayerEntity> players = new ArrayList<>();
	
	public PlayerEspHack()
	{
		super("PlayerESP");
		setCategory(Category.RENDER);
		
		addSetting(style);
		addSetting(boxSize);
		addSetting(colorMode);
		addSetting(blueFriends);
		addSetting(showGray);
		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		PlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		players.clear();
		Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
			.parallelStream().filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
		
		stream = entityFilters.applyTo(stream);
		
		players.addAll(stream.collect(Collectors.toList()));
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.hasLines())
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		// draw boxes
		if(style.hasBoxes())
			renderBoxes(matrixStack, partialTicks, region);
		
		if(style.hasLines())
			renderTracers(matrixStack, partialTicks, region);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void renderBoxes(MatrixStack matrixStack, float partialTicks,
		RegionPos region)
	{
		float extraSize = boxSize.getExtraSize();
		
		for(PlayerEntity e : players)
		{
			matrixStack.push();
			
			Vec3d lerpedPos = EntityUtils.getLerpedPos(e, partialTicks)
				.subtract(region.toVec3d());
			matrixStack.translate(lerpedPos.x, lerpedPos.y, lerpedPos.z);
			
			matrixStack.scale(e.getWidth() + extraSize,
				e.getHeight() + extraSize, e.getWidth() + extraSize);
			
			Rgb color = processColor(e);
			
			if(showGray.isChecked() || !color.isGray())
			{
				RenderSystem.setShaderColor(color.r, color.g, color.b, 0.5F);
				
				Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
				RenderUtils.drawOutlinedBox(bb, matrixStack);
			}
			
			matrixStack.pop();
		}
	}
	
	private void renderTracers(MatrixStack matrixStack, float partialTicks,
		RegionPos region)
	{
		if(players.isEmpty())
			return;
		
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.begin(
			VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		
		Vec3d regionVec = region.toVec3d();
		Vec3d start = RotationUtils.getClientLookVec(partialTicks)
			.add(RenderUtils.getCameraPos()).subtract(regionVec);
		
		boolean shouldRender = false;
		
		for(PlayerEntity e : players)
		{
			Vec3d end = EntityUtils.getLerpedBox(e, partialTicks).getCenter()
				.subtract(regionVec);
			
			Rgb color = processColor(e);
			
			if(showGray.isChecked() || !color.isGray())
			{
				shouldRender = true;
				
				bufferBuilder.vertex(matrix, (float)start.x, (float)start.y,
					(float)start.z).color(color.r, color.g, color.b, 0.5F);
				
				bufferBuilder
					.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
					.color(color.r, color.g, color.b, 0.5F);
			}
		}
		
		if(shouldRender)
			BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	private Rgb processColor(PlayerEntity e)
	{
		Rgb color = new Rgb(0.8F, 0.8F, 0.8F);
		
		if(colorMode.getSelected().equals(ColorMode.DISTANCE))
		{
			if(blueFriends.isChecked()
				&& WURST.getFriends().contains(e.getName().getString()))
			{
				color.r = 0;
				color.g = 0;
				color.b = 1;
			}else
			{
				float dist = MC.player.distanceTo(e) / 20F;
				color.r = MathHelper.clamp(2 - dist, 0, 1);
				color.g = MathHelper.clamp(dist, 0, 1);
				color.b = 0;
			}
		}else if(colorMode.getSelected().equals(ColorMode.NAMETAG))
		{
			Text displayName = e.getDisplayName();
			TextColor colorComponent = null;
			if(displayName != null)
				colorComponent = displayName.getStyle().getColor();
			
			if(colorComponent != null)
			{
				int teamColor = colorComponent.getRgb();
				
				color.b = (float)(teamColor % 256);
				color.g = (float)(teamColor % 65536 / 256);
				color.r = (float)(teamColor / 65536);
				
				color.b /= 256;
				color.g /= 256;
				color.r /= 256;
			}
		}
		
		return color;
	}
	
	private static class Rgb
	{
		private float r, g, b;
		
		private Rgb(float r, float g, float b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		boolean isGray()
		{
			return Math.abs(r - g) < 0.05 && Math.abs(g - b) < 0.05
				&& Math.abs(b - r) < 0.05;
		}
	}
	
	private enum ColorMode
	{
		DISTANCE("Distance"),
		NAMETAG("Name Tag");
		
		private final String name;
		
		private ColorMode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
