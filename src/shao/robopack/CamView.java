package shao.robopack;

import java.io.IOException;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

public class CamView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
	public static final int NEW_FRAME_READY = 30001;
	
    private Handler uiHandler;
    
	private Camera camera;
	private SurfaceHolder previewHolder;	
	byte[] callbackBuffer;
	
	public CamView(Context ctx, AttributeSet as)
	{
		
		super(ctx, as);
		//We can't start drawing immediately - create and add a callback to be notified when the surface is ready
		previewHolder = this.getHolder();
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		previewHolder.addCallback(this);
	
	}
	
	public void setHandler(Handler uiHandler) {
		this.uiHandler = uiHandler;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		
		camera=Camera.open();
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height)
	{
		try {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.setDisplayOrientation(90);			
			Parameters params = camera.getParameters();			
			params.setPreviewSize(176, 144);
			callbackBuffer = new byte[176*144*3];  //3 bytes per pixel yuv - setpreviewformat doesn't seem to work
			//params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			camera.setParameters(params);
			camera.startPreview();
			camera.setPreviewCallbackWithBuffer(this);  //Re-uses the buffer, seems to knock ~400 ms off camera lag
			camera.addCallbackBuffer(callbackBuffer);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void surfaceDestroyed(SurfaceHolder arg0)
	{
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();   
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		
		//Log.w("MyActivity", Integer.toString(data.length));
		//Log.w("roboeyes", "Preview callback");
		act.finalbox.imageProcThread.processImage(callbackBuffer);
		act.callBackFrameCount ++;
		
		//sendState(NEW_FRAME_READY);		
	}
	
	public void addBuffer(byte[] callbackBuffer) {
		camera.addCallbackBuffer(callbackBuffer);
	}
	
	public byte[] getCallbackBuffer() {
		return callbackBuffer;		
	}
	
    private void sendState(int message) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        sendBundle(myBundle);
    }

    private void sendBundle(Bundle myBundle) {
        Message myMessage = uiHandler.obtainMessage();
        myMessage.setData(myBundle);
        uiHandler.sendMessage(myMessage);
    }
	
}


