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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cw.videopal.R;
import com.cw.videopal.db.DB_folder;
import com.cw.videopal.db.DB_page;
import com.cw.videopal.note_edit.Note_edit;
import com.cw.videopal.operation.mail.MailNotes;
import com.cw.videopal.page.PageAdapter_recycler;
import com.cw.videopal.tabs.TabsHost;
import com.cw.videopal.util.DeleteFileAlarmReceiver;
import com.cw.videopal.util.Util;
import com.cw.videopal.util.image.UtilImage;
import com.cw.videopal.util.preferences.Pref;
import com.cw.videopal.util.server.WebService;
import com.cw.videopal.util.uil.UilCommon;
import com.cw.videopal.util.video.AsyncTaskVideoBitmapPager;
import com.cw.videopal.util.video.UtilVideo;
import com.cw.videopal.util.video.VideoPlayer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.dynamite.DynamiteModule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class Note_cast extends AppCompatActivity
		implements  PlayerManager.Listener
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    public ViewPager viewPager;
    public static boolean isPagerActive;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    public static PagerAdapter mPagerAdapter;

    // DB
    public DB_page mDb_page;
    public static Long mNoteId;
    int mEntryPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    static int mStyle;

    static SharedPreferences mPref_show_note_attribute;

    Button editButton;
    Button optionButton;
    Button backButton;

    public AppCompatActivity act;
    public static int mPlayVideoPositionOfInstance;

	public CastContext castContext;
	public PlayerManager playerManager;
	PlayerManager.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note / _onCreate");

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
		AsyncTaskVideoBitmapPager.mRotationStr = null;

		act = this;

//        MainAct.mMediaBrowserCompat = null;
	} //onCreate end

	// Add to prevent resizing full screen picture,
	// when popup menu shows up at picture mode
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("Note / _onWindowFocusChanged");
		///cw
//		if (hasFocus && isPictureMode() )
//			Util.setFullScreen(act);
	}

	// key event: 1 from bluetooth device 2 when notification bar dose not shown
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int newPos;
		System.out.println("Note / _onKeyDown / keyCode = " + keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
				if(viewPager.getCurrentItem() == 0)
                    newPos = mPagerAdapter.getCount() - 1;//back to last one
				else
					newPos = NoteUi.getFocus_notePos()-1;

				NoteUi.setFocus_notePos(newPos);
				viewPager.setCurrentItem(newPos);

				return true;

			case KeyEvent.KEYCODE_MEDIA_NEXT: //87
				if(viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
					newPos = 0;
				else
					newPos = NoteUi.getFocus_notePos() + 1;

				NoteUi.setFocus_notePos(newPos);
				viewPager.setCurrentItem(newPos);

				return true;

			case KeyEvent.KEYCODE_MEDIA_PLAY: //126
				return true;

			case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
				return true;

			case KeyEvent.KEYCODE_BACK:
                onBackPressed();
				return true;

			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return true;

			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return true;

			case KeyEvent.KEYCODE_MEDIA_STOP:
				return true;
		}
		return false;
	}



	void setLayoutView()
	{
        System.out.println("Note / _setLayoutView");

		if( UtilVideo.mVideoView != null)
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

		// video view will be reset after _setContentView
		if(Util.isLandscapeOrientation(this))
			setContentView(R.layout.note_view_landscape);
		else
			setContentView(R.layout.note_view_portrait);

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

		// Instantiate a ViewPager and a PagerAdapter.
		viewPager = (ViewPager) findViewById(R.id.tabs_pager);
		mPagerAdapter = new Note_adapter(viewPager,this,castContext,playerManager,this.listener);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setCurrentItem(NoteUi.getFocus_notePos());

		// tab style
//		if(TabsHost.mDbFolder != null)
//			TabsHost.mDbFolder.close();

		if(mDb_page != null) {
			mNoteId = mDb_page.getNoteId(NoteUi.getFocus_notePos(), true);
		}

		// Note: if viewPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
		//       be called again after rotation
		viewPager.setOnPageChangeListener(onPageChangeListener);//todo deprecated

		// edit note button
		editButton = (Button) findViewById(R.id.view_edit);
		editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
		editButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				Intent intent = new Intent(Note_cast.this, Note_edit.class);
				intent.putExtra(DB_page.KEY_NOTE_ID, mNoteId);
				intent.putExtra(DB_page.KEY_NOTE_TITLE, mDb_page.getNoteTitle_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_PICTURE_URI , mDb_page.getNotePictureUri_byId(mNoteId));
				intent.putExtra(DB_page.KEY_NOTE_CREATED, mDb_page.getNoteCreatedTime_byId(mNoteId));
				startActivityForResult(intent, EDIT_CURRENT_VIEW);
			}
		});

		// send note button
		optionButton = (Button) findViewById(R.id.view_option);
		optionButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
		optionButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				View_note_option.note_option(act,mNoteId);
			}
		});

		// back button
		backButton = (Button) findViewById(R.id.view_back);
		backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
		backButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view) {
				if(isTextMode())
				{
					// back to view all mode
					setViewAllMode();
					setOutline(act);
				}
				else //view all mode
				{
					stopAV();
					finish();
				}
			}
		});
	}

	// on page change listener
	ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener()
	{
		@Override
		public void onPageSelected(int nextPosition)
		{

			NoteUi.setFocus_notePos(viewPager.getCurrentItem());
			System.out.println("Note / _onPageSelected");
//			System.out.println("    NoteUi.getFocus_notePos() = " + NoteUi.getFocus_notePos());
//			System.out.println("    nextPosition = " + nextPosition);

			mIsViewModeChanged = false;

			// stop video when changing note
			String pictureUriInDB = mDb_page.getNotePictureUri_byId(mNoteId);
			if(UtilVideo.hasVideoExtension(pictureUriInDB,act)) {
				VideoPlayer.stopVideo();
				NoteUi.cancel_UI_callbacks();
			}

			// stop ExoPlayer
			UtilVideo.stopExoPlayer();

            setOutline(act);
		}
	};

	public static int getStyle() {
		return mStyle;
	}

	public void setStyle(int style) {
		mStyle = style;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode,resultCode,data);
		System.out.println("Note / _onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
			stopAV();
        }
		else if(requestCode == MailNotes.EMAIL)
		{
			Toast.makeText(act,R.string.mail_exit,Toast.LENGTH_SHORT).show();
			// note: result code is always 0 (cancel), so it is not used
			new DeleteFileAlarmReceiver(act,
					                    System.currentTimeMillis() + 1000 * 60 * 5, // formal: 300 seconds
//						    		    System.currentTimeMillis() + 1000 * 10, // test: 10 seconds
					                    MailNotes.mAttachmentFileName);
		}

	    // check if there is one note at least in the pager
		if( viewPager.getAdapter().getCount() > 0 )
			setOutline(act);
		else
			finish();
	}

    /** Set outline for selected view mode
    *
    *   Determined by view mode: all, picture, text
    *
    *   Controlled factor:
    *   - action bar: hide, show
    *   - full screen: full, not full
    */
	public static void setOutline(AppCompatActivity act)
	{
        // Set full screen or not, and action bar
		if(isViewAllMode() || isTextMode())
		{
			Util.setFullScreen_noImmersive(act);
            if(act.getSupportActionBar() != null)
			    act.getSupportActionBar().show();
		}
		else if(isPictureMode())
		{
//			Util.setFullScreen(act);
//            if(act.getSupportActionBar() != null)
//    			act.getSupportActionBar().hide();
		}

        // renew pager
        showSelectedView();

		LinearLayout buttonGroup = (LinearLayout) act.findViewById(R.id.view_button_group);
        // button group
        if(Note_cast.isPictureMode() )
            buttonGroup.setVisibility(View.GONE);
        else
            buttonGroup.setVisibility(View.VISIBLE);

        // renew options menu
        act.invalidateOptionsMenu();
	}


    //Refer to http://stackoverflow.com/questions/4434027/android-videoview-orientation-change-with-buffered-video
	/***************************************************************
	video play spec of Pause and Rotate:
	1. Rotate: keep pause state
	 pause -> rotate -> pause -> play -> continue

	2. Rotate: keep play state
	 play -> rotate -> continue play

	3. Key guard: enable pause
	 play -> key guard on/off -> pause -> play -> continue

	4. Key guard and Rotate: keep pause
	 play -> key guard on/off -> pause -> rotate -> pause
	 ****************************************************************/
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    System.out.println("Note / _onConfigurationChanged");

		// dismiss popup menu
		if(NoteUi.popup != null)
		{
			NoteUi.popup.dismiss();
			NoteUi.popup = null;
		}

		NoteUi.cancel_UI_callbacks();

        setLayoutView();

        if(canShowFullScreenPicture())
            Note_cast.setPictureMode();
        else
            Note_cast.setViewAllMode();

        // Set outline of view mode
        setOutline(act);
	}

	@Override
	protected void onStart() {
		super.onStart();
		System.out.println("Note / _onStart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Note / _onResume");

		///cw
//		setLayoutView();

		isPagerActive = true;

//        if(canShowFullScreenPicture())
//            Note.setPictureMode();
//        else
//            Note.setViewAllMode();

		///cw
		// always set picture mode for this App
//		Note.setPictureMode();

		///cw
//		setOutline(act);

		///cw
		setContentView(R.layout.note_view_portrait_cast);
		mDb_page = new DB_page(act, TabsHost.getCurrentPageTableId());
		String pictureStr = mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
		String titleStr = mDb_page.getNoteTitle(NoteUi.getFocus_notePos(),true);

		Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		exoPlayer_cast2(pictureStr, titleStr);
	}

	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("Note / _onPause");

		isPagerActive = false;

		// set pause when key guard is ON
		if( UtilVideo.mVideoView != null)
		{
			UtilVideo.mPlayVideoPosition = UtilVideo.mVideoView.getCurrentPosition();

			// keep play video position
			mPlayVideoPositionOfInstance = UtilVideo.mPlayVideoPosition;
			System.out.println("Note / _onPause / mPlayVideoPositionOfInstance = " + mPlayVideoPositionOfInstance);
		}

		NoteUi.cancel_UI_callbacks();

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

//		setTurnScreenOn(false);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("Note / _onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note / _onDestroy");
	}

	// avoid exception: has leaked window android.widget.ZoomButtonsController
	@Override
	public void finish() {
		System.out.println("Note / _finish");
		if(mPagerHandler != null)
			mPagerHandler.removeCallbacks(mOnBackPressedRun);		
	    
		ViewGroup view = (ViewGroup) getWindow().getDecorView();
//	    view.setBackgroundColor(getResources().getColor(color.background_dark)); // avoid white flash
	    view.setBackgroundColor(getResources().getColor(R.color.bar_color)); // avoid white flash
	    view.removeAllViews();

		super.finish();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		System.out.println("Note / _onSaveInstanceState");
	}

	Menu mMenu;
	// On Create Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
		System.out.println("Note / _onCreateOptionsMenu");

	    getMenuInflater().inflate(R.menu.menu, menu);
	    CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);


//		// inflate menu
//		getMenuInflater().inflate(R.menu.pager_menu, menu);
//		mMenu = menu;
//
//		// menu item: checked status
//		// get checked or not
//		int isChecked = mDb_page.getNoteMarking(NoteUi.getFocus_notePos(),true);
//		if( isChecked == 0)
//			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);
//		else
//			menu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
//
//		// menu item: view mode
//   		markCurrentSelected(menu.findItem(R.id.VIEW_ALL),"ALL");
//		markCurrentSelected(menu.findItem(R.id.VIEW_PICTURE),"PICTURE_ONLY");
//		markCurrentSelected(menu.findItem(R.id.VIEW_TEXT),"TEXT_ONLY");
//
//	    // menu item: previous
//		MenuItem itemPrev = menu.findItem(R.id.ACTION_PREVIOUS);
//		itemPrev.setEnabled(viewPager.getCurrentItem() > 0);
//		itemPrev.getIcon().setAlpha(viewPager.getCurrentItem() > 0?255:30);
//
//		// menu item: Next or Finish
//		MenuItem itemNext = menu.findItem(R.id.ACTION_NEXT);
//		itemNext.setTitle((viewPager.getCurrentItem() == mPagerAdapter.getCount() - 1)	?
//									R.string.view_note_slide_action_finish :
//									R.string.view_note_slide_action_next                  );
//
//        // set Disable and Gray for Last item
//		boolean isLastOne = (viewPager.getCurrentItem() == (mPagerAdapter.getCount() - 1));
//        if(isLastOne)
//        	itemNext.setEnabled(false);
//
//        itemNext.getIcon().setAlpha(isLastOne?30:255);

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
//            	if(isTextMode())
//            	{
//        			// back to view all mode
//            		setViewAllMode();
//					setOutline(act);
//            	}
//            	else if(isViewAllMode()||isPictureMode())
//            	{
//					stopAV();
	            	finish();
//            	}
                return true;

            case R.id.VIEW_NOTE_MODE:
            	return true;

			case R.id.VIEW_NOTE_CHECK:
				int markingNow = PageAdapter_recycler.toggleNoteMarking(this,NoteUi.getFocus_notePos());

				// update marking
				if(markingNow == 1)
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_on_holo_dark);
				else
					mMenu.findItem(R.id.VIEW_NOTE_CHECK).setIcon(R.drawable.btn_check_off_holo_dark);

				return true;

            case R.id.VIEW_ALL:
        		setViewAllMode();
				setOutline(act);
            	return true;
            	
            case R.id.VIEW_PICTURE:
        		setPictureMode();
				setOutline(act);
            	return true;

            case R.id.VIEW_TEXT:
        		setTextMode();
				setOutline(act);
            	return true;
            	
            case R.id.ACTION_PREVIOUS:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()-1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                return true;

            case R.id.ACTION_NEXT:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
				NoteUi.setFocus_notePos(NoteUi.getFocus_notePos()+1);
            	viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // on back pressed
    @Override
    public void onBackPressed() {
		System.out.println("Note / _onBackPressed");
    	// web view can go back
	    ///cw
//        if(isPictureMode())
//    	{
////            // dispatch touch event to show buttons
////            long downTime = SystemClock.uptimeMillis();
////            long eventTime = SystemClock.uptimeMillis() + 100;
////            float x = 0.0f;
////            float y = 0.0f;
////            // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
////            int metaState = 0;
////            MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP,
////                                                    x, y,metaState);
////            dispatchTouchEvent(event);
////            event.recycle();
////
////            // in order to make sure ImageViewBackButton is effective to be clicked
////            mPagerHandler = new Handler();
////            mPagerHandler.postDelayed(mOnBackPressedRun, 500);
//
//		    // back to view all mode
//		    setViewAllMode();
//		    setOutline(act);
//        }
//        else if(isTextMode())
//    	{
//			// back to view all mode
//    		setViewAllMode();
//			setOutline(act);
//    	}
//    	else
//    	{
//    		System.out.println("Note / _onBackPressed / view all mode");
//			stopAV();
        	finish();
//    	}
    }
    
    static Handler mPagerHandler;
	Runnable mOnBackPressedRun = new Runnable()
	{   @Override
		public void run()
		{
            String tagStr = "current"+ NoteUi.getFocus_notePos() +"pictureView";
            ViewGroup pictureGroup = (ViewGroup) viewPager.findViewWithTag(tagStr);
            System.out.println("Note / _showPictureViewUI / tagStr = " + tagStr);

            Button picView_back_button;
            if(pictureGroup != null)
            {
                picView_back_button = (Button) (pictureGroup.findViewById(R.id.image_view_back));
                picView_back_button.performClick();
            }

			if(Note_adapter.mIntentView != null)
				Note_adapter.mIntentView = null;
		}
	};
    
    // get current picture string
    public String getCurrentPictureString()
    {
		return mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
    }

    // Mark current selected
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
                .equalsIgnoreCase(str))
            subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
        else
            subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }

    // Show selected view
    static void showSelectedView()
    {
   		mIsViewModeChanged = false;

		if(!Note_cast.isTextMode())
   		{
	   		if(UtilVideo.mVideoView != null)
	   		{
	   	   		// keep current video position for NOT text mode
				mPositionOfChangeView = UtilVideo.mPlayVideoPosition;
	   			mIsViewModeChanged = true;

	   			if(VideoPlayer.mVideoHandler != null)
	   			{
					System.out.println("Note / _showSelectedView / just remove callbacks");
	   				VideoPlayer.mVideoHandler.removeCallbacks(VideoPlayer.mRunPlayVideo);
	   				if(UtilVideo.hasMediaControlWidget)
	   					VideoPlayer.cancelMediaController();
	   			}
	   		}
   			Note_adapter.mLastPosition = -1;
   		}

    	if(mPagerAdapter != null)
    		mPagerAdapter.notifyDataSetChanged(); // will call Note_adapter / _setPrimaryItem
    }
    
    public static int mPositionOfChangeView;
    public static boolean mIsViewModeChanged;
    
    static void setViewAllMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","ALL")
		   						  .apply();
    }
    
    static void setPictureMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","PICTURE_ONLY")
		   						  .apply();
    }
    
    static void setTextMode()
    {
		 mPref_show_note_attribute.edit()
		   						  .putString("KEY_PAGER_VIEW_MODE","TEXT_ONLY")
		   						  .apply();
    }
    
    
    public static boolean isPictureMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("PICTURE_ONLY");
    }
    
    public static boolean isViewAllMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("ALL");
    }

    public static boolean isTextMode()
    {
	  	return mPref_show_note_attribute.getString("KEY_PAGER_VIEW_MODE", "ALL")
										.equalsIgnoreCase("TEXT_ONLY");
    }

	static NoteUi picUI_touch;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
				///cw
//    			 System.out.println("Note / _dispatchTouchEvent / MotionEvent.ACTION_UP / viewPager.getCurrentItem() =" + viewPager.getCurrentItem());
//				 //1st touch to turn on UI
//				 if(picUI_touch == null) {
//				 	picUI_touch = new NoteUi(act, viewPager, viewPager.getCurrentItem());
//				 	picUI_touch.tempShow_picViewUI(5000,getCurrentPictureString());
//				 }
//				 //2nd touch to turn off UI
//				 else
//					 setTransientPicViewUI();

				 //1st touch to turn off UI (primary)
//				 if(Note_adapter.picUI_primary != null)
//					 setTransientPicViewUI();
    	  	  	 break;

	        case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
	        case MotionEvent.ACTION_CANCEL:
	        	 break;
        }

        return super.dispatchTouchEvent(event);
    }

	/**
	 * Set delay for transient picture view UI
	 *
	 */
    void setTransientPicViewUI()
    {
        NoteUi.cancel_UI_callbacks();
        picUI_touch = new NoteUi(act, viewPager, viewPager.getCurrentItem());

        // for video
        String pictureUriInDB = mDb_page.getNotePictureUri_byId(mNoteId);
        if(UtilVideo.hasVideoExtension(pictureUriInDB,act) &&
                (UtilVideo.mVideoView != null) &&
                (UtilVideo.getVideoState() != UtilVideo.VIDEO_AT_STOP) )
        {
            if (!NoteUi.showSeekBarProgress)
                picUI_touch.tempShow_picViewUI(110, getCurrentPictureString());
            else
                picUI_touch.tempShow_picViewUI(1110, getCurrentPictureString());
        }
        // for image
        else
            picUI_touch.tempShow_picViewUI(111,getCurrentPictureString());
    }

	public static void stopAV()	{
		VideoPlayer.stopVideo();
	}

	public static void changeToNext(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

	public static void changeToPrevious(ViewPager mPager)
	{
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}

    // Show full screen picture when device orientation and image orientation are the same
    boolean canShowFullScreenPicture()
    {
        String pictureStr = mDb_page.getNotePictureUri(NoteUi.getFocus_notePos(),true);
		System.out.println(" Note / _canShowFullPicture / pictureStr = " +pictureStr);
//		System.out.println(" Note / _canShowFullPicture / Util.isLandscapeOrientation(act) = " +Util.isLandscapeOrientation(act));
//		System.out.println(" Note / _canShowFullPicture / UtilImage.isLandscapePicture(pictureStr) = " +UtilImage.isLandscapePicture(pictureStr));
        if( !Util.isEmptyString(pictureStr) &&
            ( (Util.isLandscapeOrientation(act) && UtilImage.isLandscapePicture(pictureStr))||
              (Util.isPortraitOrientation(act) && !UtilImage.isLandscapePicture(pictureStr))  ) )
            return true;
        else
            return false;
    }

	//The BroadcastReceiver that listens for bluetooth broadcasts
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			System.out.println("MainAct / _BroadcastReceiver / onReceive");
			String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				//Device is now connected
				Toast.makeText(getApplicationContext(), "ACTION_ACL_CONNECTED: device is " + device, Toast.LENGTH_LONG).show();
			}

			Intent intentReceive = intent;
			KeyEvent keyEvent = (KeyEvent) intentReceive.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if(keyEvent != null)
				onKeyDown( keyEvent.getKeyCode(),keyEvent);
		}
	};



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
		System.out.println("Note / exoPlayer_cast2 / pictureStr = " + pictureStr);

//		UtilVideo.mCurrentPagerView = (View) object;
		StyledPlayerView exoplayer_view = ((StyledPlayerView) findViewById(R.id.exoplayer_view2));

		// player inside player manager
		playerManager = new PlayerManager(act,listener,exoplayer_view,castContext,pictureStr,titleStr);
	}
}
