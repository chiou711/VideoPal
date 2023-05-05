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

package com.cw.videopal.note;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.cw.videopal.R;
import com.cw.videopal.db.DB_page;
import com.cw.videopal.page.PageAdapter_recycler;
import com.cw.videopal.tabs.TabsHost;
import com.cw.videopal.util.ColorSet;
import com.cw.videopal.util.Util;
import com.cw.videopal.util.image.TouchImageView;
import com.cw.videopal.util.server.WebService;
import com.cw.videopal.util.video.UtilVideo;
import com.cw.videopal.util.video.VideoViewCustom;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.gms.cast.framework.CastContext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class Note_adapter extends FragmentStatePagerAdapter
{
	static int mLastPosition;
	private static LayoutInflater inflater;
	private AppCompatActivity act;
	private ViewPager pager;
	DB_page db_page;
	CastContext castContext;
	PlayerManager playerManager;
	PlayerManager.Listener listener;


    public Note_adapter(ViewPager viewPager, AppCompatActivity activity,CastContext _castContext,
		    PlayerManager _playerManager,PlayerManager.Listener _listener)
    {
    	super(activity.getSupportFragmentManager());
		pager = viewPager;
    	act = activity;
        inflater = act.getLayoutInflater();
        mLastPosition = -1;
	    db_page = new DB_page(act, TabsHost.getCurrentPageTableId());
        System.out.println("Note_adapter / constructor / mLastPosition = " + mLastPosition);

	    castContext = _castContext;
	    playerManager = _playerManager;
		listener = _listener;
    }
    
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView((View) object);
	}

    @SuppressLint("SetJavaScriptEnabled")
	@Override
	public Object instantiateItem(ViewGroup container, final int position) 
    {
    	System.out.println("Note_adapter / instantiateItem / position = " + position);
    	// Inflate the layout containing 
    	// 1. picture group: image,video, thumb nail, control buttons
    	// 2. text group: title, body, time 
    	View pagerView = inflater.inflate(R.layout.note_view_adapter, container, false);
    	int style = Note.getStyle();
        pagerView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

    	// Picture group
        ViewGroup pictureGroup = (ViewGroup) pagerView.findViewById(R.id.pictureContent);
        String tagPictureStr = "current"+ position +"pictureView";
        pictureGroup.setTag(tagPictureStr);
    	
        // image view
    	TouchImageView imageView = ((TouchImageView) pagerView.findViewById(R.id.image_view));
        String tagImageStr = "current"+ position +"imageView";
        imageView.setTag(tagImageStr);

		// video view
    	VideoViewCustom videoView = ((VideoViewCustom) pagerView.findViewById(R.id.video_view));
        String tagVideoStr = "current"+ position +"videoView";
        videoView.setTag(tagVideoStr);

		// exoplayer view
	    StyledPlayerView exoplayer_view = ((StyledPlayerView) pagerView.findViewById(R.id.exoplayer_view));

		ProgressBar spinner = (ProgressBar) pagerView.findViewById(R.id.loading);

        // line view
        View line_view = pagerView.findViewById(R.id.line_view);

    	// text group
        ViewGroup textGroup = (ViewGroup) pagerView.findViewById(R.id.textGroup);

        // Set tag for text view
    	TextView textView = textGroup.findViewById(R.id.textBody);

    	// set accessibility
        textGroup.setContentDescription(act.getResources().getString(R.string.note_text));
		textView.getRootView().setContentDescription(act.getResources().getString(R.string.note_text));

        String strTitle = db_page.getNoteTitle(position,true);

        // View mode
    	// picture only
	  	if(Note.isPictureMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isPictureMode ");
	  		pictureGroup.setVisibility(View.VISIBLE);

			showExoPlayerView(imageView,videoView,exoplayer_view);

	  	    line_view.setVisibility(View.GONE);
	  	    textGroup.setVisibility(View.GONE);
	  	}
	    // text only
	  	else if(Note.isTextMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isTextMode ");
	  		pictureGroup.setVisibility(View.GONE);

	  		line_view.setVisibility(View.VISIBLE);
	  		textGroup.setVisibility(View.VISIBLE);

	  	    if(!Util.isEmptyString(strTitle) )
	  	    {
	  	    	showTextView(position,textView);
	  	    }
	  	}
  		// picture and text
	  	else if(Note.isViewAllMode())
	  	{
			System.out.println("Note_adapter / _instantiateItem / isViewAllMode ");

			// picture
			pictureGroup.setVisibility(View.VISIBLE);

			showExoPlayerView(imageView,videoView,exoplayer_view);

	  	    line_view.setVisibility(View.VISIBLE);
	  	    textGroup.setVisibility(View.VISIBLE);

			// text
	  	    if( !Util.isEmptyString(strTitle)  )
	  	    {
	  	    	showTextView(position,textView);
	  	    }
	  	    else
			{
				textGroup.setVisibility(View.GONE);
			}
	  	}

		// footer of note view
		TextView footerText = (TextView) pagerView.findViewById(R.id.note_view_footer);
		if(!Note.isPictureMode())
		{
			footerText.setVisibility(View.VISIBLE);
			footerText.setText(String.valueOf(position+1)+"/"+ pager.getAdapter().getCount());
            footerText.setTextColor(ColorSet.mText_ColorArray[Note.mStyle]);
            footerText.setBackgroundColor(ColorSet.mBG_ColorArray[Note.mStyle]);
		}
		else
			footerText.setVisibility(View.GONE);

    	container.addView(pagerView, 0);

		return pagerView;
    } //instantiateItem
	
    // show text view
    private void showTextView(int position, TextView textView){
    	System.out.println("Note_adapter/ _showTextView / position = " + position);
	    String textStr = db_page.getNotePictureUri(position,true);
	    textView.setText(textStr);
    }

	// show ExoPlayer view
	private void showExoPlayerView(TouchImageView imageView,
	                             VideoView videoView,
	                             StyledPlayerView exoPlayerVideoView){
		imageView.setVisibility(View.GONE);
		videoView.setVisibility(View.GONE);
		exoPlayerVideoView.setVisibility(View.VISIBLE);
	}

	@Override
	public Fragment getItem(int position) {
		return null;
	}

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
    
	@Override
    public int getCount() 
    {
		if(db_page != null)
			return db_page.getNotesCount(true);
		else
			return 0;
    }

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}
	
	static Intent mIntentView;
	static NoteUi picUI_primary;

	@Override
	public void setPrimaryItem(final ViewGroup container, int position, Object object) 
	{
		// set primary item only
	    if(mLastPosition != position)
		{
			System.out.println("Note_adapter / _setPrimaryItem / mLastPosition = " + mLastPosition);
            System.out.println("Note_adapter / _setPrimaryItem / position = " + position);

			String pictureStr = db_page.getNotePictureUri(position,true);
			String titleStr = db_page.getNoteTitle(position,true);
			System.out.println("Note_adapter / _setPrimaryItem / pictureStr = " + pictureStr);

			// for video view
			if (!Note.isTextMode() ){
				// stop last video view running
				if (mLastPosition != -1){
					String tagVideoStr = "current" + mLastPosition + "videoView";
					VideoViewCustom lastVideoView = (VideoViewCustom) pager.findViewWithTag(tagVideoStr);
					lastVideoView.stopPlayback();
				}

				StyledPlayerView exoplayer_view = (StyledPlayerView) ((View)object).findViewById(R.id.exoplayer_view);

				// ref: https://github.com/google/ExoPlayer
				///cw: set ExoPlayer
				if(pictureStr.contains("drive.google") )
					exoPlayer_gDrive(exoplayer_view,pictureStr); //@@@ cast failed
				else
					exoPlayer_cast(exoplayer_view,pictureStr,titleStr);
			}

		}
	    mLastPosition = position;
	    
	} //setPrimaryItem

	// Google drive path ExoPlayer
	void exoPlayer_gDrive(StyledPlayerView object, String pictureStr){
		System.out.println("---------- gDriveExoPlayer / pictureStr = " + pictureStr);
		UtilVideo.mCurrentPagerView = object;
		UtilVideo.exoPlayer = new ExoPlayer.Builder(act).build();
		StyledPlayerView exoplayer_view = ((StyledPlayerView) UtilVideo.mCurrentPagerView.findViewById(R.id.exoplayer_view));
		try {
			Uri videoUri= Uri.parse(Util.getTransformedGDrivePath(pictureStr));

            MediaItem mediaItem = new MediaItem.Builder()
							            .setUri(videoUri)
							            .setMimeType(MimeTypes.VIDEO_MP4V)
							            .build();
			UtilVideo.exoPlayer.setMediaItem(mediaItem);

			exoplayer_view.setPlayer(UtilVideo.exoPlayer);

			UtilVideo.exoPlayer.prepare();
			System.out.println("------------ gDriveExoPlayer / videoUri = " + videoUri);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	// cast ExoPlayer (player manager)
	// support
	// - https path with MP4
	// - device storage
	// - SdCard
	void exoPlayer_cast(StyledPlayerView object, String pictureStr, String titleStr){
		// at local device storage
		if(pictureStr.contains("file://"))
			pictureStr = pictureStr.replace("file://","");
		else if(pictureStr.startsWith("content") )
			pictureStr = Util.getLocalRealPathByUri(act,Uri.parse(pictureStr));

		// set root path for Web service
		// root of external storage:
		//  - Environment.getExternalStorageDirectory().getAbsolutePath()
		//  - /storage/emulated/0/
		String deviceStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();

		// root of sdcard:
		//  - /storage/0403-0201/
		String[] sdCardPath= Util.getStorageDirectories(act);

		WebService.root_path = null;
		if(pictureStr.contains(deviceStoragePath))
			WebService.root_path = deviceStoragePath;
		else {
			for(int i=0;i<sdCardPath.length;i++){
				if(pictureStr.contains(sdCardPath[i]))
					WebService.root_path = sdCardPath[i];
			}
		}

		if( WebService.root_path!= null) {
			// add http://device_IP:8080 prefix
			if (pictureStr.contains(WebService.root_path)) {
				pictureStr = pictureStr.replace(WebService.root_path, "");
				pictureStr = "http://" + PageAdapter_recycler.deviceIpAddress + ":8080" + pictureStr;
			}

			// start web service
			act.startService(new Intent(act, WebService.class));
		}
		System.out.println("Note_adapter / exoPlayer_cast / pictureStr = " + pictureStr);

		UtilVideo.mCurrentPagerView = (View) object;
		StyledPlayerView exoplayer_view = ((StyledPlayerView) UtilVideo.mCurrentPagerView.findViewById(R.id.exoplayer_view));

		// player inside player manager
		playerManager = new PlayerManager(act,listener,exoplayer_view,castContext,pictureStr,titleStr);
	}

}