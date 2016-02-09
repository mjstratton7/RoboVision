/*==============================================================================
            Copyright (c) 2012 Qualcomm Technologies Incorporated.
            All Rights Reserved.
            Qualcomm Technologies Confidential and Proprietary
==============================================================================*/

package com.qualcomm.fastcvdemo.apis.featureDetection;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.mstratton.robovision2.R;
import com.qualcomm.fastcvdemo.base.GraphicalActivity;
import com.qualcomm.fastcvdemo.utils.GraphicalObject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class Corner extends GraphicalActivity {
	/** Shared preference value to enable/disable viewfinder */
	private static int mPrefViewFinderEnable = 1;

	/** Preference options for enabling/disabling viewfinder */
	private static final int[] mPrefOptsViewFinder = { 0, 1 };

	/** Shared preference value for FAST corner threshold */
	private static int mPrefThreshold = 5;

	/**
	 * Shared preference value for scaling prior to corner detection
	 */
	private static int mPrefScale = 1;

	/**
	 * Shared preference value to enable/disable gaussian blurring
	 */
	private static int mPrefEnableGauss = 0;

	/** Shared preference value to change detection function */
	private static int mPrefDetectionSwitch = 1;

	/** Preference options for threshold for Fast Corner Detector */
	private static final int[] mPrefOptsFastThreshold = { 5, 10, 20, 40, 80 };

	/** Preference options for scaling for Fast Corner Detector */
	private static final int[] mPrefOptsDownScale = { 1, 2, 4, 8 };

	// TODO: Why aren't these checks for booleans?
	/** Preference options for enabling/disabling corner overlay */
	private static final int[] mPrefOptsEnableCornerOverlay = { 0, 1 };

	/** Preference options for enabling/disabling Gaussian Filter */
	private static final int[] mPrefOptsEnableGaussianFilter = { 0, 1 };

	/** Shared preference value to enable/disable pixel overlay */
	private static int mPrefEnablePixelOverlay = 0;

	/** Preference options for detection function for Fast Corner */
	private static final int[] mPrefOptsDetectionSwitch = { 1, 2, 3, 4, 5, 6, 7, 8 };

	/** Shared preference value to display Detection Function */
	private static String detectionFunction = "Corner Detection Function";

	private static String distanceString = "H:0 V:0";

	private static AtomicInteger numObjects = new AtomicInteger(0);

	private static AtomicInteger numTargets = new AtomicInteger(0);
	
	int shotNumber = 1;

	static {
		Log.v(TAG, "Corner: load fastcvFeatDetect library");
		System.loadLibrary("fastcvFeatDetect");
	}

	private int confidence = 0;

	private int screwUps = 0;

	/** Function which retrieves title based on module used. */
	protected void initTitle() {
		title = "RoboVision2";
	}

	/**
	 * Resumes camera preview
	 */
	@Override
	protected void onResume() {
		Log.v(TAG, "Corner: onResume()");
		updatePreferences();
		super.onResume();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.settings:
			Intent settingsActivity = new Intent(getBaseContext(), com.qualcomm.fastcvdemo.apis.featureDetection.CornerPrefs.class);
			startActivity(settingsActivity);

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected Runnable mUpdateTimeTask = new Runnable() {
		/**
		 * Overridden run function to handle callback This updates the fps and
		 * number of corners detected for the user
		 */

		@SuppressLint("DefaultLocale")
		public void run() {
			float camFPS = util.getCameraFPS();
			int numCorners = getNumCorners();
			String message;

			// Setup next profiling run.
			mProfileHandler.postDelayed(this, mProfilePeriod);
			mText[0].setText("Corners Found: " + numCorners + ", Targets Found: " + numTargets + "/" + numObjects);

			mText[1].setText(String.format("Process Delay: %.2fms", mProcessTime));
		}
	};

	protected void updatePreferences() {
		// Retrieve Preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// set the default camera resolution to WVGA

		// retrieves user preference to set the threshold for Fast
		// Corner Detector algorithm
		int resIndex = Integer.decode(prefs.getString("camThreshold", "3"));
		if (resIndex < mPrefOptsFastThreshold.length) {
			Log.e(TAG, "Configured FastThreshold: " + resIndex);
			mPrefThreshold = mPrefOptsFastThreshold[resIndex];
			Log.e(TAG, "Configured Threshold: " + mPrefThreshold);
		}

		// retrieves user preference for scaling factor to be
		// applied to image before applying corner detection on it
		resIndex = Integer.decode(prefs.getString("camScaler", "0"));
		if (resIndex < mPrefOptsDownScale.length) {
			Log.e(TAG, "Configured Scaling: " + resIndex);
			mPrefScale = mPrefOptsDownScale[resIndex];
			Log.e(TAG, "Configured Scale: " + mPrefScale);
		}

		// retrieves user's preference to enable or disable the
		// green pixel overlay to be applied where corners are
		// detected
		resIndex = Integer.decode(prefs.getString("cornerOverlay", "1"));
		if (resIndex < mPrefOptsEnableCornerOverlay.length) {
			Log.e(TAG, "Configured Pixel Overlay: " + resIndex);
			mPrefEnablePixelOverlay = mPrefOptsEnableCornerOverlay[resIndex];
			Log.e(TAG, "Configured Scale: " + mPrefScale);
		}

		// retrieves user's preference to enable or disable the
		// camera preview, or display a blank canvas
		resIndex = Integer.decode(prefs.getString("viewfinder", "0"));
		if (resIndex < mPrefOptsViewFinder.length) {
			Log.e(TAG, "Configured Viewfinder: " + resIndex);
			mPrefViewFinderEnable = mPrefOptsViewFinder[resIndex];
			Log.e(TAG, "Configured viewfinder: " + mPrefViewFinderEnable);
		}

		// retrieves user preference for gaussian filter to be
		// applied to image before applying corner detection on it
		resIndex = Integer.decode(prefs.getString("gaussianEnable", "1"));
		if (resIndex < mPrefOptsEnableGaussianFilter.length) {
			Log.e(TAG, "Configured Scaling: " + resIndex);
			mPrefEnableGauss = mPrefOptsEnableGaussianFilter[resIndex];
			Log.e(TAG, "Configured Scale: " + mPrefScale);
		}

		// retrieves user preferences for which fastcv
		// detection to use and applies the appropriate detection
		resIndex = Integer.decode(prefs.getString("detectionSwitch", "1"));
		if (resIndex < mPrefOptsDetectionSwitch.length) {
			Log.e(TAG, "Configured Corner Detection: " + resIndex);
			mPrefDetectionSwitch = mPrefOptsDetectionSwitch[resIndex - 1];
			Log.e(TAG, "Configured Corner Detection Function: " + mPrefDetectionSwitch);
		}
	}

	protected void startPreview() {
		Log.v(TAG, "GraphicalActivity: startPreview()");
		if (mCamera != null) {
			setupCamera();
			setCallback();
			mCamera.startPreview();
			mPreviewRunning = true;
		} else {
			mPreviewRunning = false;
		}

		mProfileHandler.removeCallbacks(mUpdateTimeTask);
		mProfileHandler.postDelayed(mUpdateTimeTask, mProfilePeriod);
	}

	/**
	 * Stops camera preview
	 */
	protected void stopPreview() {
		Log.v(TAG, "GraphicalActivity: stopPreview()");
		if ((mCamera != null) && (mPreviewRunning == true)) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
		}

		mProfileHandler.removeCallbacks(mUpdateTimeTask);

		mPreviewRunning = false;
		System.gc();
	}

	protected void setupCamera() {
		super.setupCamera();

		//
		// Perform FastCV example configurations as retrieved from application's
		// preferences
		//
		setScaling(4);
		setFastCornerThreshold(80);
//		setScaling( mPrefScale );
//		setFastCornerThreshold( mPrefThreshold );
		setDetection(mPrefDetectionSwitch);
		detectionFunction = detectionMethod();

		if (mPrefViewFinderEnable == 1)
			setViewFinderEnable(true);
		else
			setViewFinderEnable(false);

		if (mPrefEnablePixelOverlay == 1)
			setOverlayPixelsEnable(true);
		else
			setOverlayPixelsEnable(false);

		if (mPrefEnableGauss == 1)
			setGaussianEnable(true);
		else
			setGaussianEnable(false);
	}

	/**
	 * Sets the native FAST corner threshold.
	 * 
	 * @param threshold
	 *            Barrier threshold to FAST Corner algorithm.
	 */
	protected native void setFastCornerThreshold(int threshold);

	/**
	 * Sets the desired scaling factor.
	 * 
	 * 
	 * @param scaleFactor
	 *            Desired scaling factor, 1 is no scaling.
	 */
	protected native void setScaling(int scaleFactor);

	/**
	 * Enables caller to disable camera backdrop.
	 * 
	 * @param enableViewFinder
	 *            Flag to enable/disable camera backdrop.
	 */
	protected native void setViewFinderEnable(boolean enableViewFinder);

	/**
	 * Enables/disables overlaying of pixels.
	 * 
	 * @param enableOverlayPixels
	 *            Flag to enable/disable pixel overlay.
	 */
	protected native void setOverlayPixelsEnable(boolean enableOverlayPixels);

	/**
	 * Enables/disables blurring camera image before processing corners.
	 * 
	 * @param enableGaussian
	 *            Flag enables/disables Gaussian blurring.
	 */
	protected native void setGaussianEnable(boolean enableGaussian);

	/** Sets the detection function to be used. */
	protected native void setDetection(int detectionSwitch);

	/**
	 * Retrieves the latest number of corners for debug purposes.
	 * 
	 * @return int Number of corners.
	 */
	protected native int getNumCorners();

	/**
	 * Function to pass camera frame for native, FastCV processing.
	 * 
	 * @param data
	 *            Byte buffer for data.
	 * @param w
	 *            Width of data
	 * @param h
	 *            Height of data
	 */
	public void update(byte[] data, int w, int h) {
		updateFastCv(data, w, h);

		long start = System.nanoTime();
		mRenderer.drawPixel(w / 2, h / 2, 97, 97);

		int[] corners = getCorners();
		int targets = 0;
		LinkedList<GraphicalObject> objects = GraphicalObject.getObjectsFromCorners(corners);

		GraphicalObject target = null;
		boolean sizeTestPass = false;
		boolean squareTestPass = false;
		boolean cutoutTestPass = false;
		int testPasses = 0;
		for (GraphicalObject obj : objects) {
			int[][] range = obj.getRange();
		    testPasses  = 0;

			// Check if big enough
			if (obj.isBigEnough()) { 				
				// Big enough target; draw box.
				sizeTestPass = true;
				testPasses++;
			}
			
			// Check if square enough
			 startTime = System.currentTimeMillis();
			if (obj.isSquare()) { 
				// Found a squareish thing. Might be target. 
				squareTestPass = true;
				testPasses++;
				if (sizeTestPass) {
					// If big enough and square, draw box.
					mRenderer.drawSquare(range[0][0] - 10, range[0][1] + 10, range[1][0] - 10, range[1][1] + 10, 50, 50);
				} else {
					// Not a square. Draw red box.
					mRenderer.drawSquare(range[0][0] - 10, range[0][1] + 10, range[1][0] - 10, range[1][1] + 10, 97, 97);
				}
			}

			// Check if has cutout
			if (obj.hasCutout()) {
				// Found proper target.
				cutoutTestPass = true;
				testPasses++;
				if (sizeTestPass && squareTestPass) {
					mRenderer.drawSquare(range[0][0] - 10, range[0][1] + 10, range[1][0] - 10, range[1][1] + 10, 50, 50);
					mRenderer.drawPixel(obj.getCenter().x, obj.getCenter().y, 50, 50);
				}
			}
			
			if (testPasses >= 3) {
				target = obj;
				targets++;
			}
		}
		
		numObjects.set(objects.size());
		numTargets.set(targets);

		// Prevent crash when no corners present.
		// Only computer COG with 2+ points.
		if (targets == 1) {
			// Center of gravity
			int x = target.getCenter().x;
			int y = target.getCenter().y;

			// Distance from center
			int centerx = w / 2;
			int centery = h / 2;
			// Calculate how far to move. Slight bias towards smaller movements.
			int distX = (int)Math.round((centerx - x) / 6.0 - 0.1); // WAS 10 TRIED: (15 - 1H 3L) 
			int distY = (int)Math.round((centery - y) / 19.0 - 0.1); // Was 	16 TRIED: (21 - 1H 3L) 
			distanceString = "H:" + distX + " V:" + distY + "  (T=" + ((System.nanoTime() - start) / 100000 / 10.0 + "ms)" + " Shot#: " 
			                           + shotNumber + " Tests Passed: " + testPasses);

			// Shot Specific Settings
			int thresholdX = 0;
			int thresholdY = 0;
			int confthreshold = 0;
			if (shotNumber == 1) {
				thresholdX = 9;
				thresholdY = 7;
				confthreshold = 3;
			} else if (shotNumber == 2) {
				thresholdX = 9;
				thresholdY = 7;
				confthreshold = 4;
			} else {
				thresholdX = 9; // WAS 7
				thresholdY = 7; // WAS 9
				confthreshold = 4; // WAS 5
			}
			
			// Aim Check 1 - Confirm / Close
			if (((x < (centerx + thresholdX)) && (x > (centerx - thresholdX))) && ((y < (centery + thresholdY)) && (y > (centery - thresholdY)))) {

				// Check multiple frames
				if (confidence >= confthreshold) {
					// Send fire command to arduino.
					mText[2].setText(distanceString + " - FIRE");
					WriteAdk("F" + distX + " " + distY + '\n');
					shotNumber++;
					SystemClock.sleep(3200);
				} else if (screwUps == 0) {
					mText[2].setText(distanceString + " - CONFIRM(" + confidence + ")");
					confidence++;
				} else {
					mText[2].setText(distanceString + " - WAITING(" + confidence + ")");
					screwUps = 0;
				}
			}

			// Aim Check 2 - Half Speed
			else {
				// Send distance data to arduino.
				WriteAdk(" " + distX + " " + distY + '\n');
				SystemClock.sleep(150);

				// Reset Confidence
				if (screwUps > 0) {
					mText[2].setText(distanceString + " - AIM");
					confidence = 0;
				} else {
					mText[2].setText(distanceString + " - WAITING(" + confidence + ")");
					screwUps += 1;
				}
			}
		} else if (screwUps == 0) {
			mText[2].setText("H:??? V:??? - WAITING(" + confidence + ")");
			screwUps++;
		} else if (targets > 1) {
			mText[2].setText("H:??? V:??? - MULTIPLE TARGETS(" + targets + ")");
			confidence = 0;
		} else {			
			mText[2].setText("H:??? V:??? - NO TARGET");
			confidence = 0;  
		}
	}

	public native void updateFastCv(byte[] data, int w, int h);

	/** Performs native cleanup routines for application exit. */
	public native void cleanup();

	/** Retrieves the name of the detection method being used. */
	public native String detectionMethod();

	@Override
	protected void doOnCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doAdkRead(String stringIn) {
		// TODO Auto-generated method stub

	}

	protected native int[] getCorners();

}
