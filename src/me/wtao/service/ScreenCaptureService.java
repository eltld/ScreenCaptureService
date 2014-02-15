package me.wtao.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.wtao.utils.Logcat;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;

public class ScreenCaptureService extends Service {
	
	private static final Logcat sLogcat = new Logcat();
	static {
		sLogcat.setOn(); // debug mode enable
	}
	
	private Context mContext;
	
	private DisplayMetrics mDisplayMetrics;
	private Display mDisplay;
	private Matrix mDisplayMatrix;
	private Bitmap mScreenBitmap;

	@Override
	public void onCreate() {
		sLogcat.d("entry");
		
		mContext = this; // TODO
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sLogcat.d("exit");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new Stub();
	}

	private class Stub extends IScreenCaptureService.Stub {

		@Override
		public Bitmap takeScreenCapture() throws RemoteException {
			// Prepare to orient the screenshot correctly
//			mDisplay.getRealMetrics(mDisplayMetrics); // requires API JELLY_BEAN_MR1 (level 17)
	        mDisplay.getMetrics(mDisplayMetrics);
	        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
	        float degrees = getDegreesForRotation(mDisplay.getRotation());
	        boolean requiresRotation = (degrees > 0);
	        if (requiresRotation) {
	            // Get the dimensions of the device in its native orientation
	            mDisplayMatrix.reset();
	            mDisplayMatrix.preRotate(-degrees);
	            mDisplayMatrix.mapPoints(dims);
	            dims[0] = Math.abs(dims[0]);
	            dims[1] = Math.abs(dims[1]);
	        }

	        // Take the screenshot
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	        	Exception exception = null;
				try {
					Class<?> CLASS_Surface = Class.forName("android.view.Surface");
					Class<?> paramTypes[] = {Integer.class, Integer.class};
		        	Method METHOD_screenshot = CLASS_Surface.getMethod("screenshot", paramTypes);
		        	mScreenBitmap = (Bitmap) METHOD_screenshot.invoke(null, (int) dims[0], (int) dims[1]);
				} catch (ClassNotFoundException e) {
					exception = e;
				} catch (NoSuchMethodException e) {
					exception = e;
				} catch (IllegalAccessException e) {
					exception = e;
				} catch (IllegalArgumentException e) {
					exception = e;
				} catch (InvocationTargetException e) {
					exception = e;
				}
				if(exception != null) {
					exception.printStackTrace();
					mScreenBitmap = null;
				}
			} else {
				mScreenBitmap = nativeTakeScreenCapture();
			}
	        
			if (mScreenBitmap != null) {
				if (requiresRotation) {
					// Rotate the screenshot to the current orientation
					Bitmap ss = Bitmap.createBitmap(
							mDisplayMetrics.widthPixels,
							mDisplayMetrics.heightPixels,
							Bitmap.Config.ARGB_8888);
					Canvas c = new Canvas(ss);
					c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
					c.rotate(degrees);
					c.translate(-dims[0] / 2, -dims[1] / 2);
					c.drawBitmap(mScreenBitmap, 0, 0, null);
					c.setBitmap(null);
					mScreenBitmap = ss;
				}
			}
	        
			return mScreenBitmap;
		}

	}

	private float getDegreesForRotation(int value) {
		switch (value) {
		case Surface.ROTATION_90:
			return 360f - 90f;
		case Surface.ROTATION_180:
			return 360f - 180f;
		case Surface.ROTATION_270:
			return 360f - 270f;
		}
		return 0f;
	}

	private native Bitmap nativeTakeScreenCapture();

	static {
		System.loadLibrary("screencap");
	}

}
