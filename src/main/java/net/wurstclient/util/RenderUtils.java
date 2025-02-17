/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.WurstClient;

public enum RenderUtils
{
	;
	
	private static final Box DEFAULT_BOX = new Box(0, 0, 0, 1, 1, 1);
	
	/**
	 * Enables a new scissor box with the given coordinates, while avoiding the
	 * strange side-effects of Minecraft's own enableScissor() method.
	 */
	public static void enableScissor(DrawContext context, int x1, int y1,
		int x2, int y2)
	{
		RenderSystem.setShaderColor(1, 1, 1, 1);
		context.enableScissor(x1, y1, x2, y2);
		RenderSystem.setShader(ShaderProgramKeys.POSITION);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}
	
	/**
	 * Disables the current scissor box, while avoiding most of the strange
	 * side-effects of Minecraft's own disableScissor() method.
	 *
	 * <p>
	 * <b>Note:</b> You have to draw some text after calling this method,
	 * otherwise there will be some weird colors in the sky. It's unclear why
	 * this happens.
	 */
	public static void disableScissor(DrawContext context)
	{
		context.disableScissor();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack)
	{
		applyRegionalRenderOffset(matrixStack, getCameraRegion());
	}
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack,
		Chunk chunk)
	{
		applyRegionalRenderOffset(matrixStack, RegionPos.of(chunk.getPos()));
	}
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack,
		RegionPos region)
	{
		Vec3d offset = region.toVec3d().subtract(getCameraPos());
		matrixStack.translate(offset.x, offset.y, offset.z);
	}
	
	public static void applyRenderOffset(MatrixStack matrixStack)
	{
		Vec3d camPos = getCameraPos();
		matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
	}
	
	public static Vec3d getCameraPos()
	{
		Camera camera = WurstClient.MC.getBlockEntityRenderDispatcher().camera;
		if(camera == null)
			return Vec3d.ZERO;
		
		return camera.getPos();
	}
	
	public static BlockPos getCameraBlockPos()
	{
		Camera camera = WurstClient.MC.getBlockEntityRenderDispatcher().camera;
		if(camera == null)
			return BlockPos.ORIGIN;
		
		return camera.getBlockPos();
	}
	
	public static RegionPos getCameraRegion()
	{
		return RegionPos.of(getCameraBlockPos());
	}
	
	public static float[] getRainbowColor()
	{
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float pi = (float)Math.PI;
		
		float[] rainbow = new float[3];
		rainbow[0] = 0.5F + 0.5F * MathHelper.sin(x * pi);
		rainbow[1] = 0.5F + 0.5F * MathHelper.sin((x + 4F / 3F) * pi);
		rainbow[2] = 0.5F + 0.5F * MathHelper.sin((x + 8F / 3F) * pi);
		return rainbow;
	}
	
	public static void setShaderColor(float[] rgb, float opacity)
	{
		RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], opacity);
	}
	
	public static void drawSolidBox(MatrixStack matrixStack)
	{
		drawSolidBox(DEFAULT_BOX, matrixStack);
	}
	
	public static void drawSolidBox(Box bb, MatrixStack matrixStack)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		RenderSystem.setShader(ShaderProgramKeys.POSITION);
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawSolidBox(Box bb, VertexBuffer vertexBuffer)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		drawSolidBox(bb, bufferBuilder);
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public static void drawSolidBox(Box bb, BufferBuilder bufferBuilder)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(minX, minY, maxZ);
		
		bufferBuilder.vertex(minX, maxY, minZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(minX, maxY, minZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		bufferBuilder.vertex(maxX, minY, minZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		bufferBuilder.vertex(maxX, minY, maxZ);
		
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		bufferBuilder.vertex(minX, maxY, minZ);
	}
	
	public static void drawOutlinedBox(MatrixStack matrixStack)
	{
		drawOutlinedBox(DEFAULT_BOX, matrixStack);
	}
	
	public static void drawOutlinedBox(Box bb, MatrixStack matrixStack)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		RenderSystem.setShader(ShaderProgramKeys.POSITION);
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawOutlinedBox(Box bb, VertexBuffer vertexBuffer)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		drawOutlinedBox(bb, bufferBuilder);
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public static void drawOutlinedBox(Box bb, BufferBuilder bufferBuilder)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(maxX, minY, minZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(maxX, minY, maxZ);
		
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(minX, minY, maxZ);
		
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(minX, minY, minZ);
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(minX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, maxY, minZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, maxY, minZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		
		bufferBuilder.vertex(maxX, maxY, maxZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, maxY, maxZ);
		bufferBuilder.vertex(minX, maxY, minZ);
	}
	
	public static void drawCrossBox(Box bb, MatrixStack matrixStack)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, minZ);
		bufferBuilder.vertex(matrix, maxX, maxY, minZ);
		
		bufferBuilder.vertex(matrix, minX, maxY, maxZ);
		bufferBuilder.vertex(matrix, maxX, maxY, maxZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		bufferBuilder.vertex(matrix, minX, minY, maxZ);
		
		bufferBuilder.vertex(matrix, maxX, minY, maxZ);
		bufferBuilder.vertex(matrix, maxX, minY, minZ);
		
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawCrossBox(Box bb, VertexBuffer vertexBuffer)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		drawCrossBox(bb, bufferBuilder);
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public static void drawCrossBox(Box bb, BufferBuilder bufferBuilder)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(minX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(minX, maxY, minZ);
		
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(maxX, maxY, minZ);
		
		bufferBuilder.vertex(minX, minY, maxZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, minY, minZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(minX, maxY, minZ);
		bufferBuilder.vertex(maxX, maxY, maxZ);
		
		bufferBuilder.vertex(maxX, maxY, minZ);
		bufferBuilder.vertex(minX, maxY, maxZ);
		
		bufferBuilder.vertex(maxX, minY, minZ);
		bufferBuilder.vertex(minX, minY, maxZ);
		
		bufferBuilder.vertex(maxX, minY, maxZ);
		bufferBuilder.vertex(minX, minY, minZ);
	}
	
	public static void drawNode(Box bb, MatrixStack matrixStack)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		RenderSystem.setShader(ShaderProgramKeys.POSITION);
		
		double midX = (bb.minX + bb.maxX) / 2;
		double midY = (bb.minY + bb.maxY) / 2;
		double midZ = (bb.minZ + bb.maxZ) / 2;
		
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.maxZ);
		bufferBuilder.vertex(matrix, (float)bb.minX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)bb.minX, (float)midY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.minZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.minZ);
		bufferBuilder.vertex(matrix, (float)bb.maxX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)bb.maxX, (float)midY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.maxZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.maxY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)bb.maxX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.maxY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)bb.minX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.maxY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.minZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.maxY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.maxZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.minY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)bb.maxX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.minY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)bb.minX, (float)midY, (float)midZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.minY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.minZ);
		
		bufferBuilder.vertex(matrix, (float)midX, (float)bb.minY, (float)midZ);
		bufferBuilder.vertex(matrix, (float)midX, (float)midY, (float)bb.maxZ);
		
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawNode(Box bb, VertexBuffer vertexBuffer)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		drawNode(bb, bufferBuilder);
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public static void drawNode(Box bb, BufferBuilder bufferBuilder)
	{
		float minX = (float)bb.minX;
		float minY = (float)bb.minY;
		float minZ = (float)bb.minZ;
		float maxX = (float)bb.maxX;
		float maxY = (float)bb.maxY;
		float maxZ = (float)bb.maxZ;
		float midX = (minX + maxX) / 2F;
		float midY = (minY + maxY) / 2F;
		float midZ = (minZ + maxZ) / 2F;
		
		bufferBuilder.vertex(midX, midY, maxZ);
		bufferBuilder.vertex(minX, midY, midZ);
		
		bufferBuilder.vertex(minX, midY, midZ);
		bufferBuilder.vertex(midX, midY, minZ);
		
		bufferBuilder.vertex(midX, midY, minZ);
		bufferBuilder.vertex(maxX, midY, midZ);
		
		bufferBuilder.vertex(maxX, midY, midZ);
		bufferBuilder.vertex(midX, midY, maxZ);
		
		bufferBuilder.vertex(midX, maxY, midZ);
		bufferBuilder.vertex(maxX, midY, midZ);
		
		bufferBuilder.vertex(midX, maxY, midZ);
		bufferBuilder.vertex(minX, midY, midZ);
		
		bufferBuilder.vertex(midX, maxY, midZ);
		bufferBuilder.vertex(midX, midY, minZ);
		
		bufferBuilder.vertex(midX, maxY, midZ);
		bufferBuilder.vertex(midX, midY, maxZ);
		
		bufferBuilder.vertex(midX, minY, midZ);
		bufferBuilder.vertex(maxX, midY, midZ);
		
		bufferBuilder.vertex(midX, minY, midZ);
		bufferBuilder.vertex(minX, midY, midZ);
		
		bufferBuilder.vertex(midX, minY, midZ);
		bufferBuilder.vertex(midX, midY, minZ);
		
		bufferBuilder.vertex(midX, minY, midZ);
		bufferBuilder.vertex(midX, midY, maxZ);
	}
	
	public static void drawArrow(Vec3d from, Vec3d to, MatrixStack matrixStack)
	{
		RenderSystem.setShader(ShaderProgramKeys.POSITION);
		
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		
		double startX = from.x;
		double startY = from.y;
		double startZ = from.z;
		
		double endX = to.x;
		double endY = to.y;
		double endZ = to.z;
		
		matrixStack.push();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		
		bufferBuilder.vertex(matrix, (float)startX, (float)startY,
			(float)startZ);
		bufferBuilder.vertex(matrix, (float)endX, (float)endY, (float)endZ);
		
		matrixStack.translate(endX, endY, endZ);
		matrixStack.scale(0.1F, 0.1F, 0.1F);
		
		double xDiff = endX - startX;
		double yDiff = endY - startY;
		double zDiff = endZ - startZ;
		
		float xAngle = (float)(Math.atan2(yDiff, -zDiff) + Math.toRadians(90));
		matrix.rotate(xAngle, new Vector3f(1, 0, 0));
		
		double yzDiff = Math.sqrt(yDiff * yDiff + zDiff * zDiff);
		float zAngle = (float)Math.atan2(xDiff, yzDiff);
		matrix.rotate(zAngle, new Vector3f(0, 0, 1));
		
		bufferBuilder.vertex(matrix, 0, 2, 1);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, -1, 2, 0);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 2, -1);
		bufferBuilder.vertex(matrix, 1, 2, 0);
		
		bufferBuilder.vertex(matrix, 1, 2, 0);
		bufferBuilder.vertex(matrix, 0, 2, 1);
		
		bufferBuilder.vertex(matrix, 1, 2, 0);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 2, 1);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 0, 2, 1);
		
		matrixStack.pop();
		
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawArrow(Vec3d from, Vec3d to,
		VertexBuffer vertexBuffer)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
		drawArrow(from, to, bufferBuilder);
		BuiltBuffer buffer = bufferBuilder.end();
		
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public static void drawArrow(Vec3d from, Vec3d to,
		BufferBuilder bufferBuilder)
	{
		double startX = from.x;
		double startY = from.y;
		double startZ = from.z;
		
		double endX = to.x;
		double endY = to.y;
		double endZ = to.z;
		
		Matrix4f matrix = new Matrix4f();
		matrix.identity();
		
		bufferBuilder.vertex(matrix, (float)startX, (float)startY,
			(float)startZ);
		bufferBuilder.vertex(matrix, (float)endX, (float)endY, (float)endZ);
		
		matrix.translate((float)endX, (float)endY, (float)endZ);
		matrix.scale(0.1F, 0.1F, 0.1F);
		
		double xDiff = endX - startX;
		double yDiff = endY - startY;
		double zDiff = endZ - startZ;
		
		float xAngle = (float)(Math.atan2(yDiff, -zDiff) + Math.toRadians(90));
		matrix.rotate(xAngle, new Vector3f(1, 0, 0));
		
		double yzDiff = Math.sqrt(yDiff * yDiff + zDiff * zDiff);
		float zAngle = (float)Math.atan2(xDiff, yzDiff);
		matrix.rotate(zAngle, new Vector3f(0, 0, 1));
		
		bufferBuilder.vertex(matrix, 0, 2, 1);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, -1, 2, 0);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 2, -1);
		bufferBuilder.vertex(matrix, 1, 2, 0);
		
		bufferBuilder.vertex(matrix, 1, 2, 0);
		bufferBuilder.vertex(matrix, 0, 2, 1);
		
		bufferBuilder.vertex(matrix, 1, 2, 0);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 2, 1);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, -1, 2, 0);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 0, 2, -1);
		
		bufferBuilder.vertex(matrix, 0, 0, 0);
		bufferBuilder.vertex(matrix, 0, 2, 1);
	}
	
	public static void drawItem(DrawContext context, ItemStack stack, int x,
		int y, boolean large)
	{
		MatrixStack matrixStack = context.getMatrices();
		
		matrixStack.push();
		matrixStack.translate(x, y, 0);
		if(large)
			matrixStack.scale(1.5F, 1.5F, 1.5F);
		else
			matrixStack.scale(0.75F, 0.75F, 0.75F);
		
		ItemStack renderStack = stack.isEmpty() || stack.getItem() == null
			? new ItemStack(Blocks.GRASS_BLOCK) : stack;
		
		DiffuseLighting.enableGuiDepthLighting();
		context.drawItem(renderStack, 0, 0);
		DiffuseLighting.disableGuiDepthLighting();
		
		matrixStack.pop();
		
		if(stack.isEmpty())
		{
			matrixStack.push();
			matrixStack.translate(x, y, 250);
			if(large)
				matrixStack.scale(2, 2, 2);
			
			TextRenderer tr = WurstClient.MC.textRenderer;
			context.drawText(tr, "?", 3, 2, 0xf0f0f0, true);
			
			matrixStack.pop();
		}
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
