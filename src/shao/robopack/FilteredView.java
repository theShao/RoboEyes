package shao.robopack;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class FilteredView extends SurfaceView implements SurfaceHolder.Callback {
	SurfaceHolder finalHolder;
	imageProc imageProcThread;
	act myAct;

	//RGB Filter config - min = 0, max = 262143
	//Over-ridden by last used settings if availale
	public int minRed = 180000;
	public int maxRed = 262143;
	public int minGreen = 0;
	public int maxGreen = 110000;
	public int minBlue = 0;
	public int maxBlue = 140000;
	public int minIntensity = 0;
	public int maxIntensity = 255;
	public int pixelCountThreshold = 70; //If less than this number of pixels match the filter CoG will be set to null
	public int minBlobSize = 70;
	public int cogX = 0;
	public int cogY = 0;

	public boolean ready = false;

	int foreColour = 0xFFFF0000;  //Pixel colour for objects that pass the filter
	int backColour = 0xFFFFFFFF; //Pixel colour for objects that fail the filter
	int cogColour = 0xFF0000FF; //Colour to use for box drawn round COG
	int boxX = 3; //Size of box to draw round cog
	int boxY = 3; //Size of box to draw round cog	

	//Image constants
	public static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;	
	public int inwidth = 176;
	public int inheight = 144;
	public int outwidth = 144;
	public int outheight = 144;

	static int Ytranslate = 15; //by default the rotated image is taken from the center of the source image -
	int offset = (inwidth - outwidth)/2 + Ytranslate;

	public FilteredView(Context ctx, AttributeSet as)
	{
		super(ctx, as);
		//We can't start drawing immediately - create and add a callback to be notified when the surface is ready
		finalHolder = this.getHolder();
		finalHolder.addCallback(this);

	}

	public void setAct(act thisAct) {
		myAct = thisAct;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		act.filterconfigbox.toggleView();
		return true;
	}

	public void surfaceCreated(SurfaceHolder holder) {
		//Start the image processing thread
		imageProcThread = new imageProc();
		imageProcThread.start();
		//imageProcThread.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	// nested image processing class
	class imageProc extends Thread {
		int[] fullRGB = new int[inwidth * inheight];
		int[] smallRGB = new int[outwidth * outheight];
		byte[] smallMono = new byte[outwidth * outheight];
		Paint myPaint = new Paint();
		int sumX, sumY, count;
		Bitmap bmp = Bitmap.createBitmap(outwidth, outheight, DEFAULT_BITMAP_CONFIG);

		public void processImage(byte[] inImage) {

			//Log.w("roboeyes", "Convert image");
			//Get the canvas from the surfaceholder
			Canvas canvas = finalHolder.lockCanvas();

			//Convert and filter
			toARGB8888(inImage, fullRGB, inwidth, inheight);

			//Fix and do calculations				
			analyzeImage(fullRGB, smallRGB);



			// Blob filter - takes about .15s on wildfile and not useful enough yet...
			//Create mono version for blob filter
			createMonoImage(smallMono, smallRGB);				

			// Create Blob Finder
			BlobFinder finder = new BlobFinder(outwidth, outheight);

			ArrayList<BlobFinder.Blob> blobList = new ArrayList<BlobFinder.Blob>();
			finder.detectBlobs(smallMono, null, minBlobSize, -1, (byte)0, blobList);
			Collections.sort(blobList);

			// List Blobs
			//Log.w("roboeyes", "Found " +  blobList.size() + "Blobs");
			if (blobList.size() > 0) {				//for (BlobFinder.Blob blob : blobList) System.out.println(blob);
				act.addDataString("COG_X", Integer.toString(blobList.get(0).cogX));
				act.addDataString("COG_Y", Integer.toString(blobList.get(0).cogY));
				act.addDataString("MAX_X", Integer.toString(blobList.get(0).xMax));
				act.addDataString("MAX_Y", Integer.toString(blobList.get(0).yMax));
				act.addDataString("MIN_X", Integer.toString(blobList.get(0).xMin));
				act.addDataString("MIN_Y", Integer.toString(blobList.get(0).yMin));
			} else {
				act.addDataString("COG_X", null);
				act.addDataString("COG_Y", null);
				act.addDataString("MAX_X", null);
				act.addDataString("MAX_Y", null);
				act.addDataString("MIN_X", null);
				act.addDataString("MIN_Y", null);
			}
			
			if (myAct.isConnected()) {
				myAct.pulse();
				//Log.w("roboeyes", "pulse");
			}
			//Setpixels is much faster than creating a new bitmap from the rgb array
			//Bitmap bmp = Bitmap.createBitmap(smallRGB, outwidth, outheight, DEFAULT_BITMAP_CONFIG);
			bmp.setPixels(smallRGB, 0, outwidth, 0, 0, outwidth, outheight);

			//Log.w("roboeyes", "Draw bitmap");				

			canvas.drawBitmap(bmp, 0, 0, myPaint);				

			//Release the canvas to be updated
			finalHolder.unlockCanvasAndPost(canvas);
			//Log.w("roboeyes", "Done...");

			//Give the camera callback buffer back
			act.cambox.addBuffer(inImage);

		}

		/* rotate 90deg and take image of output dimensions from the middle of the source image
		 * Then re-write pixels in the right place.
		 * Also calcualtes image CoG
		 */
		public void analyzeImage(int[] imageIn, int[] imageOut) {
			//Log.w("roboeyes", "Make frame");
			sumX = 0; sumY = 0;	count = 0;			
			for(int outRow = 0; outRow < outheight; outRow++){
				for(int outCol=0; outCol < outwidth; outCol++) {
					//Source pixels start on the bottom row, %offset% columns in and colums map to rows					
					int srcPxl = fullRGB[((outwidth - outCol - 1) * inwidth) + (offset + outRow)];
					imageOut[((outRow * outwidth) + outCol)] = srcPxl;

					/*
					if (srcPxl == foreColour) {
						//sum Xs and Ys for pixels that qualify
						sumX += outCol;
						sumY += outRow;
						count++;
					}
					 */
				}

			}
			/*
			//Calculate image cog
			//Log.w("roboeyes", "Calculate CoG");
			if (count > pixelThreshold) {
				cogX = sumX / count;
				cogY = outheight - (sumY / count);
				//act.writeLine("COG:" + cogX + "," + cogY);
				act.addDataString("COG_X", Integer.toString(cogX));
				act.addDataString("COG_Y", Integer.toString(cogY));

				//Draw a square centred on the COG
				for (int i = 0; i == boxX; i++) {
					for (int j = 0; j == boxY; j++) {
						try {
							imageOut[outwidth * ((outheight - cogY) + i - 1) + cogX + j - 1] = cogColour;
						} catch (Exception e) {
							//probably trying to draw a box out of the edge of the bitmap...
						}
					}
				}				
				//Log.w("roboeyes", "COG:" + cogX + "," + cogY);
			} else {
				act.removeDataString("COG_X");
				act.removeDataString("COG_Y");
			}
			 */
		}

		/* 
		 * Create B/W (1 byte per pixel (why not 1 bit per pixel?) version of the analyzed image
		 */
		public void createMonoImage(byte[] mono, int[] rgb) {

			int srcPtr = 0;

			while (srcPtr < rgb.length)
			{
				int val = rgb[srcPtr];
				mono[srcPtr] = (val > foreColour) ? (byte) 0xFF : 0;
				srcPtr++;
			}
		}

		/*
		 * Relatively quick conversion from yuv420 to argb888 hacked to filter results using simple RGB
		 */
		public void toARGB8888(byte[] yuv420sp, int[] rgb, int width, int height) {

			final int frameSize = width * height;

			for (int j = 0, yp = 0; j < height; j++) {
				int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
				for (int i = 0; i < width; i++, yp++) {
					int y = (0xff & ((int) yuv420sp[yp])) - 16;
					// intensity filter doesn't work very well...
					//if ((y > minIntensity) && (y < maxIntensity)) {
					if (y < 0) y = 0;
					if ((i & 1) == 0) {
						v = (0xff & yuv420sp[uvp++]) - 128;
						u = (0xff & yuv420sp[uvp++]) - 128;
					}
					int y1192 = 1192 * y;
					int r = (y1192 + 1634 * v);
					int g = (y1192 - 833 * v - 400 * u);
					int b = (y1192 + 2066 * u);
					if (r < 0) r = 0; else if (r > 262143) r = 262143;
					if (g < 0) g = 0; else if (g > 262143) g = 262143;
					if (b < 0) b = 0; else if (b > 262143) b = 262143;		            

					// Crude minx/max filter
					if (((r > minRed) && (r < maxRed)) &&
							((g > minGreen) && (g < maxGreen)) &&
							((b > minBlue) && (b < maxBlue))) {

						rgb[yp] = foreColour;// 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
					}
					else {
						rgb[yp] = backColour;
					}
					//} else {
					//	rgb[yp] = backColour;
					//}
				}
			}
		}

	}

}


