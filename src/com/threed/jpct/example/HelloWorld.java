package com.threed.jpct.example;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Logger;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

/**
 * A simple demo. This shows more how to use jPCT-AE than it shows how to write
 * a proper application for Android. It includes basic activity management to
 * handle pause and resume...
 * 
 * @author EgonOlsen
 * 
 */
public class HelloWorld extends Activity {

	// Used to handle pause and resume...
	private static HelloWorld master = null;

	private GLSurfaceView mGLView;
	private MyRenderer renderer = null;
	private FrameBuffer fb = null;
	private World world = null;
	private RGBColor back = new RGBColor(50, 50, 100);

	private float touchTurn = 0;
	private float touchTurnUp = 0;

	private float xpos = -1;
	private float ypos = -1;

	private Object3D cube = null;
	private Object3D[] eagle = null;
	private Object3D kitty = null;
	private int fps = 0;
	private boolean gl2 = !true;

	private Light sun = null;
	Texture texBack, texFront;


	private boolean truizm = true;

	protected void onCreate(Bundle savedInstanceState) {

		Logger.log("onCreate");

		if (master != null) {
			copy(master);
		}

		super.onCreate(savedInstanceState);
		mGLView = new GLSurfaceView(getApplication());

		if (gl2) {
			mGLView.setEGLContextClientVersion(2);
		} else {
			mGLView.setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
				public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
					// Ensure that we get a 16bit framebuffer. Otherwise, we'll
					// fall back to Pixelflinger on some device (read: Samsung
					// I7500). Current devices usually don't need this, but it
					// doesn't hurt either.
					int[] attributes = new int[] { EGL10.EGL_DEPTH_SIZE, 16,
							EGL10.EGL_NONE };
					EGLConfig[] configs = new EGLConfig[1];
					int[] result = new int[1];
					egl.eglChooseConfig(display, attributes, configs, 1, result);
					return configs[0];
				}
			});

		}

		renderer = new MyRenderer();
		mGLView.setRenderer(renderer);
		setContentView(mGLView);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mGLView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mGLView.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void copy(Object src) {
		try {
			Logger.log("Copying data from master Activity!");
			Field[] fs = src.getClass().getDeclaredFields();
			for (Field f : fs) {
				f.setAccessible(true);
				f.set(this, f.get(src));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean onTouchEvent(MotionEvent me) {

		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			xpos = me.getX();
			ypos = me.getY();
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_UP) {
			xpos = -1;
			ypos = -1;
			touchTurn = 0;
			touchTurnUp = 0;
			return true;
		}

		if (me.getAction() == MotionEvent.ACTION_MOVE) {
			float xd = me.getX() - xpos;
			float yd = me.getY() - ypos;

			xpos = me.getX();
			ypos = me.getY();

			touchTurn = xd / -100f;
			touchTurnUp = yd / -100f;
			return true;
		}

		try {
			Thread.sleep(15);
		} catch (Exception e) {
			// No need for this...
		}

		return super.onTouchEvent(me);
	}

	protected boolean isFullscreenOpaque() {
		return true;
	}

	class MyRenderer implements GLSurfaceView.Renderer {

		private long time = System.currentTimeMillis();

		public MyRenderer() {
		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			if (fb != null) {
				fb.dispose();
			}

			if (gl2) {
				fb = new FrameBuffer(w, h); // OpenGL ES 2.0 constructor
			} else {
				fb = new FrameBuffer(gl, w, h); // OpenGL ES 1.x constructor
			}

			if (master == null) {

				world = new World();
				world.setAmbientLight(20, 20, 20);

				sun = new Light(world);
				sun.setIntensity(250, 250, 250);

				texFront = new Texture(BitmapHelper.rescale(
						BitmapHelper.convert(getResources().getDrawable(
								R.drawable.__auto_)), 256, 256));
				TextureManager.getInstance().addTexture("tex_front", texFront);

				texBack = new Texture(BitmapHelper.rescale(
						BitmapHelper.convert(getResources().getDrawable(
								R.drawable.__auto_1)), 256, 256));
				TextureManager.getInstance().addTexture("tex_back", texBack);

				kitty = loadCharacter("shark", 10);
				kitty.rotateX((float) Math.PI);
				// kitty.setTexture("tex_back");

				world.addObject(kitty);
				// world.removeAll();

				Camera cam = world.getCamera();
				cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
				cam.lookAt(kitty.getTransformedCenter());

				SimpleVector sv = new SimpleVector();
				sv.set(kitty.getTransformedCenter());
				sv.y -= 100;
				sv.z -= 100;
				sun.setPosition(sv);
				MemoryHelper.compact();

				if (master == null) {
					Logger.log("Saving master Activity!");
					master = HelloWorld.this;
				}
			}
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		}

		public void onDrawFrame(GL10 gl) {
			if (touchTurn != 0) {
				kitty.rotateY(touchTurn);
				// cube[1].rotateY(touchTurn);

				touchTurn = 0;
			}

			if (touchTurnUp != 0) {
				kitty.rotateX(touchTurnUp);
				// cube[1].rotateX(touchTurnUp);
				touchTurnUp = 0;
			}

			fb.clear(back);
			world.renderScene(fb);
			world.draw(fb);
			fb.display();

			if (System.currentTimeMillis() - time >= 1000) {
				Logger.log(fps + "fps");
				fps = 0;
				time = System.currentTimeMillis();
			}
			fps++;
		}
	}

	private Object3D loadCharacter(String model_name, float scale) {
		Object3D out = null;
		Object3D[] outs = null;
		try {
			outs = Loader
					.loadOBJ(
							getResources().getAssets()
									.open(model_name + ".obj"), getResources()
									.getAssets().open(model_name + ".mtl"),
							scale);
			outs[0].setTexture("tex_back");
			outs[1].setTexture("tex_front");
			outs[0].build();
			outs[1].build();
			out = Object3D.mergeAll(outs);
			out.build();
			out.strip();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return out;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.spider:
			applyRest("spider_man", 20);
			return true;
		case R.id.cat_in_boots:
			applyRest("puss_in_boots", 20);
			return true;
		case R.id.shark:
			applyRest("shark", 10);
			return true;
		case R.id.superman:
			applyRest("frizza", 10);
			return true;
		case R.id.cat:
			applyRest("joker",1);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void applyRest(String model_name, float scale) {
		world.removeObject(kitty);;
		kitty = loadCharacter(model_name, scale);
		
		
		kitty.rotateX((float) Math.PI);
//		world.setAmbientLight(20, 20, 20);
//		sun = new Light(world);
//		sun.setIntensity(250, 250, 250);

		Camera cam = world.getCamera();
		if(truizm ){
		cam.moveCamera(Camera.CAMERA_MOVEOUT, 50);
		truizm = false;}
		cam.lookAt(kitty.getTransformedCenter());

		SimpleVector sv = new SimpleVector();
		sv.set(kitty.getTransformedCenter());
		sv.y -= 200;
		sv.z -= 200;
		sun.setPosition(sv);
		MemoryHelper.compact();
		world.addObject(kitty);

	}
}
