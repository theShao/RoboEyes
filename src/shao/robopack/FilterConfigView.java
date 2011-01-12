package shao.robopack;

import shao.robopack.HorizontalSlider.OnProgressChangeListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class FilterConfigView extends LinearLayout {
	
	FilteredView finalbox;
	HorizontalSlider sMaxR, sMinR, sMaxG, sMinG, sMaxB, sMinB, sThresh;
	int rMin, rMax, bMin, bMax, gMin, gMax, threshold;

	
	public FilterConfigView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setVisibility(GONE);
	}
	
	
	public void init() {
		
		for (int i = 0; i < this.getChildCount(); i++) {
			try {
				HorizontalSlider thisSlider = (HorizontalSlider)this.getChildAt(i);
				thisSlider.setOnProgressChangeListener(changeListener);
	        	if(thisSlider.getTag().equals("MinRed")) {        		
	        		thisSlider.setProgress(act.finalbox.minRed);
	        	} else if(thisSlider.getTag().equals("MaxRed")) {
	        		thisSlider.setProgress(act.finalbox.maxRed);
	        	} else if(thisSlider.getTag().equals("MinGreen")) {
	        		thisSlider.setProgress(act.finalbox.minGreen);
	        	} else if(thisSlider.getTag().equals("MaxGreen")) {
	        		thisSlider.setProgress(act.finalbox.maxGreen);
	        	} else if(thisSlider.getTag().equals("MinBlue")) {
	        		thisSlider.setProgress(act.finalbox.minBlue);
	        	} else if(thisSlider.getTag().equals("MaxBlue")) {
	        		thisSlider.setProgress(act.finalbox.maxBlue);
	        	} else if(thisSlider.getTag().equals("Threshold")) {
	        		//thisSlider.setProgress(act.finalbox.pixelThreshold);
	        	} else if(thisSlider.getTag().equals("MinIntensity")) {
	        		thisSlider.setProgress(act.finalbox.minIntensity);
	        	} else if(thisSlider.getTag().equals("MaxIntensity")) {
	        		thisSlider.setProgress(act.finalbox.maxIntensity);
	        		} 
	        	//changeListener.onProgressChanged(thisSlider, thisSlider.getProgress());
			} catch (Exception e) {
				//probably the view isn't a slider....
			}
		}
	}
	
	public void toggleView() {
		if(this.getVisibility() == View.VISIBLE) {
			this.setVisibility(GONE);
		} else {
			this.setVisibility(VISIBLE);
		}
	}
	
	
    private OnProgressChangeListener changeListener = new OnProgressChangeListener() {
    	 
        public void onProgressChanged(View v, int progress) {
        	
        	if(v.getTag().equals("MinRed")) {        		
        		act.finalbox.minRed = progress;
        	} else if(v.getTag().equals("MaxRed")) {
        		act.finalbox.maxRed = progress;
        	} else if(v.getTag().equals("MinGreen")) {
        		act.finalbox.minGreen = progress;
        	} else if(v.getTag().equals("MaxGreen")) {
        		act.finalbox.maxGreen = progress;
        	} else if(v.getTag().equals("MinBlue")) {
        		act.finalbox.minBlue = progress;
        	} else if(v.getTag().equals("MaxBlue")) {
        		act.finalbox.maxBlue = progress;
        	} else if(v.getTag().equals("Threshold")) {
        		//act.finalbox.pixelThreshold = progress;
        	} else if(v.getTag().equals("MinIntensity")) {
        		act.finalbox.minIntensity = progress;
        	} else if(v.getTag().equals("MaxIntensity")) {
        		act.finalbox.maxIntensity = progress;
        	} 
        }

};
	

}
