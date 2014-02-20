package me.wtao.service;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.wtao.io.ExternalStorage;
import me.wtao.lang.reflect.Reflect;
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
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class ScreenCaptureService extends Service {
	
	private static final Logcat sLogcat = new Logcat();
	static {
		sLogcat.setOff();
	}
	
	private Context mContext;
	
	private Display mDisplay;
	private DisplayMetrics mDisplayMetrics;
	private Matrix mDisplayMatrix;
	private Bitmap mScreenBitmap;

	@Override
	public void onCreate() {
		Log.d(sLogcat.getTag(), "entry, debug mode "
				+ (sLogcat.isDebuggable() ? "on" : "off")
				+ ", setOn() to see more info, good luck");
		
		mContext = this;
		
		WindowManager manager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		mDisplay = manager.getDefaultDisplay();
		mDisplayMetrics = new DisplayMetrics();
		mDisplayMatrix = new Matrix();
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sLogcat.d("exit");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new ScreenCaptureServiceImpl();
	}

	private class ScreenCaptureServiceImpl extends IScreenCaptureService.Stub {

		@Override
		public Bitmap takeScreenCapture() throws RemoteException {
			sLogcat.d("prepare...");
			
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
	        
	        sLogcat.d("prepare ok, screencap...");

	        // Take the screenshot
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				try {
					Class<?> CLASS_Surface = Class.forName("android.view.Surface");
					
					if (sLogcat.isDebuggable()) {
						Reflect.log(CLASS_Surface);
					}
					
					Class<?>[] paramTypes = {int.class, int.class};
		        	Method METHOD_screenshot = CLASS_Surface.getMethod("screenshot", paramTypes);
		        	// it's not null, but bad bitmap :( we need android.permission.READ_FRAME_BUFFER
		        	mScreenBitmap = (Bitmap) METHOD_screenshot.invoke(null, (int) dims[0], (int) dims[1]);
				} catch (Exception e) {
					sLogcat.e(e);
					mScreenBitmap = null;
				}
			} else {
				mScreenBitmap = nativeTakeScreenCapture();
			}
	        
	        sLogcat.d("screencap ", ((mScreenBitmap == null) ? "failed" : "ok"), ", rotate...");
	        
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
			
			sLogcat.d("everything ok, done.");
			if(sLogcat.isDebuggable()) {
				dumpToSDCard();
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
	
	/**
	 * if {@link Logcat#isDebuggable()}, dump bitmap to sdcard for testing
	 */
	private void dumpToSDCard() {
		if(!ExternalStorage.isExternalStorageWritable()) {
			return;
		}
		
		try {
			File dir = ExternalStorage.getExternalStorageDirectory("screencap");
			sLogcat.d(dir.getAbsolutePath());
			
			StringBuilder sb = new StringBuilder();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.CHINA);
			Date currentDate = new Date(System.currentTimeMillis());
			sb.append(sdf.format(currentDate));
			sb.append(".png");
			String filename = sb.toString();
			sLogcat.d(filename);
			
			FileOutputStream out = new FileOutputStream(new File(dir, filename));
			// PNG which is lossless, will ignore the quality setting 85
			mScreenBitmap.compress(Bitmap.CompressFormat.PNG, 85, out);
			out.close();
		} catch (Exception e) {
			sLogcat.w(e);
		}
	}

	private native Bitmap nativeTakeScreenCapture();

	static {
		System.loadLibrary("screencap");
	}

}
