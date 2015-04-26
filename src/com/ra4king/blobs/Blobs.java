package com.ra4king.blobs;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;

import com.ra4king.opengl.util.GLProgram;
import com.ra4king.opengl.util.ShaderProgram;
import com.ra4king.opengl.util.Utils;
import com.ra4king.opengl.util.buffers.BufferSubData;
import com.ra4king.opengl.util.buffers.GLBuffer;
import com.ra4king.opengl.util.math.Vector2;

import net.indiespot.struct.cp.Struct;

/**
 * @author Roi Atalla
 */
public class Blobs extends GLProgram {
	public static void main(String[] args) {
		new Blobs().run(true);
	}
	
	private ShaderProgram blobsProgram;
	private int showCirclesUniform;
	private boolean showCircles = false;
	
	private int blobsVAO;
	private GLBuffer blobsBuffer;
	
	private Circle[] circles;
	private final int MAX_CIRCLES = 100;
	
	public Blobs() {
		super("Marching Squares", 800, 600, false);
	}
	
	@Override
	public void init() {
		setFPS(0);
		setPrintDebug(true);
		
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		blobsProgram = new ShaderProgram(Utils.readFully(getClass().getResourceAsStream("blobs.vert")), Utils.readFully(getClass().getResourceAsStream("blobs.frag")));
		showCirclesUniform = blobsProgram.getUniformLocation("showCircles");
		
		blobsBuffer = new BufferSubData(GL_UNIFORM_BUFFER, MAX_CIRCLES * Circle.SIZE + 4, true, false);
		
		glUniformBlockBinding(blobsProgram.getProgram(), blobsProgram.getUniformBlockIndex("Circles"), 1);
		glBindBufferBase(GL_UNIFORM_BUFFER, 1, blobsBuffer.getName());
		
		float[] fullscreenQuad = {
		                           1.0f, 1.0f,
		                           -1.0f, 1.0f,
		                           -1.0f, -1.0f,
		                           1.0f, -1.0f
		};
		short[] quadIndices = {
		                        0, 1, 2, 2, 3, 0
		};
		
		blobsVAO = glGenVertexArrays();
		glBindVertexArray(blobsVAO);
		
		int quadVBO = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)BufferUtils.createFloatBuffer(fullscreenQuad.length).put(fullscreenQuad).flip(), GL_STATIC_DRAW);
		
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
		
		int indicesVBO = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indicesVBO);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer)BufferUtils.createShortBuffer(quadIndices.length).put(quadIndices).flip(), GL_STATIC_DRAW);
		
		glBindVertexArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		final float minSize = 0.01f;
		final float maxSize = 0.05f;
		final float maxSpeed = 0.2f;
		
		circles = new Circle[MAX_CIRCLES];
		for(int i = 0; i < MAX_CIRCLES; i++) {
			circles[i] = new Circle(new Vector2((float)(Math.random() * 2f - 1f), (float)(Math.random() * 2f - 1f)), 
			                         (float)Math.random() * maxSize + minSize,
									 new Vector2((float)Math.random() * maxSpeed * 2f - maxSpeed, (float)Math.random() * maxSpeed * 2f - maxSpeed));
		}
	}
	
	@Override
	public void update(long deltaTime) {
		super.update(deltaTime);
		
		for(Circle c : circles) {
			if(c != null)
				c.update(deltaTime);
		}
	}
	
	@Override
	public void keyPressed(int key, char c) {
		if(key == Keyboard.KEY_SPACE) {
			showCircles = !showCircles;
			
			blobsProgram.begin();
			glUniform1i(showCirclesUniform, showCircles ? 1 : 0);
			blobsProgram.end();
		}
	}
	
	@Override
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT);
		
		ByteBuffer buffer = blobsBuffer.bind(0, blobsBuffer.getSize());
		int circleCount = 0;
		for(Circle c : circles) {
			if(c != null) {
				c.toBuffer(buffer);
				circleCount++;
			}
			else {
				for(int i = 0; i < Circle.SIZE; i++)
					buffer.put((byte)0);
			}
		}
		buffer.putInt(circleCount);
		blobsBuffer.unbind();
		
		blobsProgram.begin();
		glBindVertexArray(blobsVAO);
		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
		glBindVertexArray(0);
		blobsProgram.end();
	}
	
	private static class Circle {
		private Vector2 center;
		private float radius;
		
		private Vector2 velocity;
		
		public static final int SIZE = 16;
		
		public Circle(Vector2 center, float radius, Vector2 velocity) {
			this.center = Struct.malloc(Vector2.class).set(center);
			this.radius = radius;
			this.velocity = Struct.malloc(Vector2.class).set(velocity);
		}
		
		static int count = 0;
		
		public void update(long deltaTime) {
			center.add(new Vector2(velocity).mult((float)(deltaTime / 1e9)));
			
			if(center.x() + radius >= 1.0f) {
				velocity.x(-velocity.x());
				center.x(1.0f - radius);
			}
			else if(center.x() - radius <= -1.0f) {
				velocity.x(-velocity.x());
				center.x(-1.0f + radius);
			}
			
			if(center.y() + radius >= 1.0f) {
				velocity.y(-velocity.y());
				center.y(1.0f - radius);
			}
			else if(center.y() - radius <= -1.0f) {
				velocity.y(-velocity.y());
				center.y(-1.0f + radius);
			}
		}
		
		public void toBuffer(ByteBuffer buffer) {
			buffer.putFloat(center.x());
			buffer.putFloat(center.y());
			buffer.putFloat(radius);
			buffer.putFloat(0);
		}
	}
}
