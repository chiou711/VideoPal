/*
 * Copyright (C) 2023 CW Chiu
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.videopal.R;
import com.cw.videopal.db.DB_folder;
import com.cw.videopal.db.DB_page;
import com.cw.videopal.operation.mail.MailNotes;
import com.cw.videopal.page.PageAdapter_recycler;
import com.cw.videopal.tabs.TabsHost;
import com.cw.videopal.util.DeleteFileAlarmReceiver;
import com.cw.videopal.util.Util;
import com.cw.videopal.util.image.TouchImageView;
import com.cw.videopal.util.image.UtilImage;
import com.cw.videopal.util.preferences.Pref;
import com.cw.videopal.util.server.WebService;
import com.cw.videopal.util.uil.UilCommon;
import com.cw.videopal.util.video.UtilVideo;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.dynamite.DynamiteModule;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class Note_cast extends AppCompatActivity implements  PlayerManager.Listener{

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;

    static SharedPreferences mPref_show_note_attribute;

    public AppCompatActivity act;
    public static int mPlayVideoPositionOfInstance;

	public CastContext castContext;
	public PlayerManager playerManager;
	PlayerManager.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note_cast / _onCreate");

	    // Getting the cast context later than onStart can cause device discovery not to take place.
	    try {
		    castContext = CastContext.getSharedInstance(this);
	    } catch (RuntimeException e) {
		    Throwable cause = e.getCause();
		    while (cause != null) {
			    if (cause instanceof DynamiteModule.LoadingException) {
				    setContentView(R.layout.cast_context_error);
				    return;
			    }
			    cause = cause.getCause();
		    }
		    // Unknown error. We propagate it.
		    throw e;
	    }

		// set current selection
		mEntryPosition = getIntent().getExtras().getInt("POSITION");
		NoteUi.setFocus_notePos(mEntryPosition);

		// init video
		UtilVideo.mPlayVideoPosition = 0;   // not played yet
		mPlayVideoPositionOfInstance = 0;

		act = this;

	} //onCreate end

	// Add to prevent resizing full screen picture,
	// when popup menu shows up at picture mode
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note_cast / _onWindowFocusChanged");
	}

	void setLayoutView()
	{
        System.out.println("Note_cast / _setLayoutView");

		if( UtilVideo.mVideoView != null)
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape2);
		else
			setContentView(R.layout.note_view_portrait2);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);

		UilCommon.init();

		// DB
		DB_folder dbFolder = new DB_folder(act,Pref.getPref_focusView_folder_tableId(act));
		mStyle = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
		}
	}

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode,resultCode,data);
		System.out.println("Note_cast / _onActivityResult ");
		if(requestCode == MailNotes.EMAIL){
			Toast.makeText(act,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(act,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}
		setOutline(act);
	}

	public void setOutline(AppCompatActivity act){
        // renew pager
        showSelectedView();

        // renew options menu
        act.invalidateOptionsMenu();

		DB_page mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());
		String pictureStr = mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
		String titleStr = mDb_page.getNoteTitle(NoteUi.getFocus_notePos(),true);

		if(UtilImage.hasImageExtension(pictureStr,act)) {
			Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
			setSupportActionBar(mToolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
			showPictureView(pictureStr);
		}
		else if(UtilVideo.hasVideoExtension(pictureStr,act)) {
			setContentView(R.layout.note_view_portrait_cast);
			Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
			setSupportActionBar(mToolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
			exoPlayer_cast2(pictureStr, titleStr);
		}
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note_cast / _onConfigurationChanged");

		setLayoutView();

		// always set picture mode for this App
		setPictureMode();

		setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note_cast / _onStart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note_cast / _onResume");

		setLayoutView();

		// always set picture mode for this App
		setPictureMode();

		setOutline(act);
	}

	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note_cast / _onPause");

		// set pause when key guard is ON
		if( UtilVideo.mVideoView != null)
		{
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

			// keep play video position
			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
			System.out.println("Note_cast / _onPause / mPlayVideoPositionOfInstance = " + mPlayVideoPositionOfInstance);
		}

		// stop exoPlayer
		UtilVideo.stopExoPlayer();

		if (castContext == null) {
			// Nothing to release.
			return;
		}

		if(playerManager!=null){
			playerManager.release();
			playerManager = null;
		}

	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note_cast / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note_cast / _onDestroy");
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note_cast / _finish");
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note_cast / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
		System.out.println("Note_cast / _onCreateOptionsMenu");

	    getMenuInflater().inflate(R.menu.menu, menu);
	    CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);

        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
	            	finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note_cast / _onBackPressed");
        finish();
    }
    
    // get current picture string
    public String getCurrentPictureString()
    {
		return mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
    }

    // Show selected view
    static void showSelectedView()
    {
		System.out.println("Note_cast / _showSelectedView");
   		mIsViewModeChanged = false;

        if(UtilVideo.mVideoView != null){
            // keep current video position for NOT text mode
			mPositionOfChangeView = UtilVideo.mPlayVideoPosition;
            mIsViewModeChanged = true;
        }
    }
    
    public static int mPositionOfChangeView;
    public static boolean mIsViewModeChanged;
    
    static void setPictureMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","PICTURE_ONLY")
		   						  .apply();
    }

	@Override
	public void onQueuePositionChanged(int previousIndex, int newIndex) {
		if (previousIndex != C.INDEX_UNSET) {
//			mediaQueueListAdapter.notifyItemChanged(previousIndex);
		}
		if (newIndex != C.INDEX_UNSET) {
//			mediaQueueListAdapter.notifyItemChanged(newIndex);
		}
	}

	@Override
	public void onUnsupportedTrack(int trackType) {
		if (trackType == C.TRACK_TYPE_AUDIO) {
//			showToast(R.string.error_unsupported_audio);
		} else if (trackType == C.TRACK_TYPE_VIDEO) {
//			showToast(R.string.error_unsupported_video);
		}
	}

	///cw
	// local ExoPlayer
//	void localExoPlayer2(String pictureStr){
//
//		if(pictureStr.contains("file://"))
//			pictureStr = pictureStr.replace("file://","");
//		else if(pictureStr.startsWith("content"))
//			pictureStr = Util.getLocalRealPathByUri(act, Uri.parse(pictureStr));
//
////		UtilVideo.mCurrentPagerView = (View) object;
//
//		UtilVideo.exoPlayer = new ExoPlayer.Builder(act).build();
//		StyledPlayerView exoplayer_view = ((StyledPlayerView) act.findViewById(R.id.exoplayer_view2));
//		exoplayer_view.setControllerShowTimeoutMs(0);
//		exoplayer_view.showController();
//		exoplayer_view.setDefaultArtwork(
//				ResourcesCompat.getDrawable(
//						act.getResources(),
//						R.drawable.ic_baseline_cast_connected_400,
//						/* theme= */ null));
//
//		try {
//			MediaItem mediaItem = new MediaItem.Builder()
//					.setUri(Uri.parse(pictureStr))
//					.setMimeType(MimeTypes.VIDEO_MP4V)
//					.build();
//			UtilVideo.exoPlayer.setMediaItem(mediaItem);
//
//			exoplayer_view.setPlayer(UtilVideo.exoPlayer);
//			UtilVideo.exoPlayer.prepare();
//			System.out.println("------------ Uri.parse(pictureStr) = " + Uri.parse(pictureStr));
//			//UtilVideo.exoPlayer.setPlayWhenReady(true);
//		}catch (Exception e){
//			e.printStackTrace();
//		}
//	}

	// cast ExoPlayer (player manager)
	// support
	// - https path with MP4
	// - device storage
	// - SdCard
	void exoPlayer_cast2(String pictureStr, String titleStr){
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
		System.out.println("Note_cast / exoPlayer_cast2 / pictureStr = " + pictureStr);

		StyledPlayerView exoplayer_view = ((StyledPlayerView) findViewById(R.id.exoplayer_view2));

		// player inside player manager
		playerManager = new PlayerManager(act,listener,exoplayer_view,castContext,pictureStr,titleStr);
	}

	void showPictureView(String pictureUri){
		TouchImageView imageView = ((TouchImageView) findViewById(R.id.image_view));
		// show image with Glide
		RequestOptions req_options = new RequestOptions()
				.centerCrop()
				.placeholder(R.drawable.ic_color_a)
				.error(R.drawable.ic_error)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.priority(Priority.HIGH);

		Glide.with(act)
				.asBitmap()
				.load(pictureUri)
				.apply(req_options)
				.into(new CustomTarget<Bitmap>() {
					@Override
					public void onResourceReady(
							Bitmap resource,
							Transition<? super Bitmap> transition) {
						imageView.setImageBitmap(resource);
					}

					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
					}

					@Override
					public void onLoadFailed(@Nullable Drawable errorDrawable) {
						super.onLoadFailed(errorDrawable);
						System.out.println("UtilImage_bitmapLoader / _onLoadFailed");
						imageView.setImageDrawable(act.getResources().getDrawable(R.drawable.ic_empty));
					}
				});

	}
}