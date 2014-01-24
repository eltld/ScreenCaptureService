package me.wtao.service;

import me.wtao.utils.Logcat;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;

public class ScreenCaptureService extends Service {

	private static final Logcat sLogcat = new Logcat();
	static {
		sLogcat.setOn(); // debug mode enable
	}

	@Override
	public void onCreate() {
		sLogcat.d("entry");
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
			return ScreenCaptureService.this.nativeTakeScreenCapture();
		}

	}

	private native Bitmap nativeTakeScreenCapture();

	static {
		System.loadLibrary("coords");
	}

}
