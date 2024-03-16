/*
 * Copyright (C) 2019 CW Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.videopal.util.video;

import android.app.Activity;

import com.cw.videopal.util.Util;

import java.io.File;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class UtilVideo {

	public static AppCompatActivity mAct;
	public final static int VIDEO_AT_STOP = 0;
	public final static int VIDEO_AT_PLAY = 1;
	public final static int VIDEO_AT_PAUSE = 2;

	public static VideoViewCustom mVideoView;
	public static int mVideoState;
	public static int mPlayVideoPosition;

	UtilVideo()	{}
	
    // check if file has video extension
    // refer to http://developer.android.com/intl/zh-tw/guide/appendix/media-formats.html
    public static boolean hasVideoExtension(File file){
    	boolean isVideo = false;
    	String fn = file.getName().toLowerCase(Locale.getDefault());
    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") || fn.endsWith("avi") ||
    		fn.endsWith("ts") || fn.endsWith("webm") || fn.endsWith("mkv")  ) 
	    	isVideo = true;
	    
    	return isVideo;
    } 
    
    // check if string has video extension
    public static boolean hasVideoExtension(String string, Activity act){
    	boolean hasVideo = false;
    	if(!Util.isEmptyString(string))
    	{
	    	String fn = string.toLowerCase(Locale.getDefault());
//	    	System.out.println("UtilVideo / _hasVideoExtension / fn 1 = " + fn);
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") || fn.endsWith("avi") ||
	    		fn.endsWith("ts") || fn.endsWith("webm") || fn.endsWith("mkv")  ) 
		    	hasVideo = true;
    	}
		else
			return false;
    	
    	if(!hasVideo)
    	{
    		String fn = Util.getDisplayNameByUriString(string, act);
	    	fn = fn.toLowerCase(Locale.getDefault());
//	    	System.out.println("UtilVideo / _hasVideoExtension / fn 2 = " + fn);
	    	if(	fn.endsWith("3gp") || fn.endsWith("mp4") || fn.endsWith("avi") ||
	    		fn.endsWith("ts") || fn.endsWith("webm") || fn.endsWith("mkv")  ) 
		    	hasVideo = true;    		
    	}
    	
    	return hasVideo;
    } 
    
	public static int getVideoState() {
		return mVideoState;
	}

	public static void setVideoState(int videoState) {
		System.out.print("UtilVideo / _setVideoState / set state to be = ");

		if(videoState == VIDEO_AT_STOP)
			System.out.println("VIDEO_AT_STOP");
		else if(videoState == VIDEO_AT_PLAY)
			System.out.println("VIDEO_AT_PLAY");
		else if(videoState == VIDEO_AT_PAUSE)
			System.out.println("VIDEO_AT_PAUSE");

		mVideoState = videoState;
	}

	// stop ExoPlayer
//	public static void stopExoPlayer(){
//		if( exoPlayer!=null && exoPlayer.isPlaying())
//			exoPlayer.stop();
//	}
}

