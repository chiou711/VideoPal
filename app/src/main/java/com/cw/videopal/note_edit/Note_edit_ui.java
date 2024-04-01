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

package com.cw.videopal.note_edit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cw.videopal.R;
import com.cw.videopal.db.DB_folder;
import com.cw.videopal.db.DB_page;
import com.cw.videopal.main.MainAct;
import com.cw.videopal.tabs.TabsHost;
import com.cw.videopal.util.ColorSet;
import com.cw.videopal.util.Util;
import com.cw.videopal.util.image.TouchImageView;
import com.cw.videopal.util.image.UtilImage_bitmapLoader;
import com.cw.videopal.util.preferences.Pref;
import com.cw.videopal.util.uil.UilCommon;

public class Note_edit_ui {
	private ImageView picImageView;
	private String pictureUriInDB;
	String oriPictureUri;
	String currPictureUri;

	private EditText titleEditText;
	private Button newTitleButton;
	private EditText pictureUriEditText;
	private String oriTitle;

	private Long noteId;
	private Long oriCreatedTime;
	private Long oriMarking;

	boolean bRollBackData;
	boolean bRemovePictureUri = false;
	private boolean bEditPicture = false;
    private DB_page dB_page;
	private Activity act;
	private int style;
	private ProgressBar progressBar;
	private ProgressBar progressBarExpand;
	private TouchImageView enlargedImage;

	Note_edit_ui(Activity act, DB_page _db, Long noteId, String strTitle, String pictureUri, Long createdTime)
    {
    	this.act = act;
    	this.noteId = noteId;
    			
    	oriTitle = strTitle;
	    oriPictureUri = pictureUri;

	    oriCreatedTime = createdTime;
	    currPictureUri = pictureUri;

	    dB_page = _db;//Page.mDb_page;
	    
	    oriMarking = dB_page.getNoteMarking_byId(noteId);
		
	    bRollBackData = false;
		bEditPicture = true;
		bShowEnlargedImage = false;
    }

	void UI_init()
    {

		UI_init_text();

	    newTitleButton = act.findViewById(R.id.edit_new_title);
	    newTitleButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);

        picImageView = (ImageView) act.findViewById(R.id.edit_picture);

        progressBar = (ProgressBar) act.findViewById(R.id.edit_progress_bar);
        progressBarExpand = (ProgressBar) act.findViewById(R.id.edit_progress_bar_expand);

		DB_folder dbFolder = new DB_folder(act, Pref.getPref_focusView_folder_tableId(act));
		style = dbFolder.getPageStyle(TabsHost.getFocus_tabPos(), true);

		enlargedImage = (TouchImageView)act.findViewById(R.id.expanded_image);

		picImageView.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

	    final InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);

		// new title button listener
	    newTitleButton.setOnClickListener(new OnClickListener(){
		    @Override
		    public void onClick(View view) {
				// show IME
			    InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
			    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

				// clear original title
			    titleEditText.setText("");
			    titleEditText.setSelection(0);
				titleEditText.requestFocus();
		    }
	    } );

		// set thumb nail listener
        picImageView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view) {
            	if(bShowEnlargedImage == true)
            	{
            		closeEnlargedImage();
            		// show soft input
//            		if (act.getCurrentFocus() != null)
//            		    imm.showSoftInput(act.getCurrentFocus(), 0);
            	}
            	else
                {
            		// hide soft input
            		if (act.getCurrentFocus() != null)
            			imm.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);

                	System.out.println("Note_edit_ui / pictureUriInDB = " + pictureUriInDB);
                	if( (!Util.isEmptyString(pictureUriInDB)) )
                	{
                		bRemovePictureUri = false;
                		System.out.println("picImageView.setOnClickListener / pictureUriInDB = " + pictureUriInDB);

                		// check if pictureUri has scheme
		                //todo need the following?
//                		if(Util.isUriExisted(pictureUriInDB, act) ||
//                         Util.isUriExisted(drawingUriInDB, act)	)
//                		{
	                		if(Uri.parse(pictureUriInDB).isAbsolute())//||
	                		{
//	                			int style =  Util.getCurrentPageStyle(TabsHost.getFocus_tabPos());
	                			new UtilImage_bitmapLoader(enlargedImage,
                                                           pictureUriInDB,
                                                           progressBarExpand,
//	                					                   (style % 2 == 1 ?
//                                                            UilCommon.optionsForRounded_light:
//                                                            UilCommon.optionsForRounded_dark),
                                                           UilCommon.optionsForFadeIn,
                                                           act);
				                enlargedImage.setVisibility(View.VISIBLE);
	                			bShowEnlargedImage = true;
	                		}
	                		else
	                		{
	                			System.out.println("pictureUriInDB is not Uri format");
	                		}
//                		}
//                		else
//                			Toast.makeText(act,R.string.file_not_found,Toast.LENGTH_SHORT).show();
                	}
                	else
            			Toast.makeText(act,R.string.file_is_not_created,Toast.LENGTH_SHORT).show();

				}
            }
        });

		// set thumb nail long click listener
        picImageView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view) {
            	if(bEditPicture) {
					if(!Util.isEmptyString(pictureUriInDB) )
						openSetPictureDialog();
//					else if(!Util.isEmptyString(drawingUriInDB))
//					{
//						Intent i = new Intent(act, Note_drawingAct.class);
//						i.putExtra("drawing_id",noteId);
//						i.putExtra("drawing_mode",Util.DRAWING_EDIT);
//						act.startActivityForResult(i,Util.DRAWING_EDIT);
//					}
				}
                return false;
            }
        });
    }

	private void UI_init_text()
	{
        int focusFolder_tableId = Pref.getPref_focusView_folder_tableId(act);
        DB_folder db = new DB_folder(MainAct.mAct, focusFolder_tableId);
		style = db.getPageStyle(TabsHost.getFocus_tabPos(), true);

		LinearLayout block = (LinearLayout) act.findViewById(R.id.edit_title_block);
		if(block != null)
			block.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		titleEditText = (EditText) act.findViewById(R.id.edit_title);
		pictureUriEditText = (EditText) act.findViewById(R.id.edit_picture_uri);

		//set title color
		titleEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		titleEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);

		//set body color
		pictureUriEditText.setTextColor(ColorSet.mText_ColorArray[style]);
		pictureUriEditText.setBackgroundColor(ColorSet.mBG_ColorArray[style]);
	}

    // set image close listener
	private void setCloseImageListeners(EditText editText)
    {
    	editText.setOnClickListener(new OnClickListener()
    	{   @Override
			public void onClick(View v) 
			{
				if(bShowEnlargedImage == true)
					closeEnlargedImage();
			}
		});
    	
    	editText.setOnFocusChangeListener(new OnFocusChangeListener() 
    	{   @Override
            public void onFocusChange(View v, boolean hasFocus) 
    		{
    				if(bShowEnlargedImage == true)
    					closeEnlargedImage();
            } 
    	});   
    }


	boolean bShowEnlargedImage;
	void closeEnlargedImage()
    {
    	System.out.println("closeExpandImage");
		enlargedImage.setVisibility(View.GONE);
		bShowEnlargedImage = false;
    }

	private void openSetPictureDialog()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setTitle(R.string.edit_note_set_picture_dlg_title)
			   .setMessage(currPictureUri)
			   .setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						bRemovePictureUri = false; // reset
						// For selecting local gallery
//						Intent intent = new Intent(act, PictureGridAct.class);
//						intent.putExtra("gallery", false);
//						act.startActivityForResult(intent, Util.ACTIVITY_SELECT_PICTURE);
						
						// select global
						final String[] items = new String[]{act.getResources().getText(R.string.note_ready_image).toString(),
															act.getResources().getText(R.string.note_ready_video).toString()};
					    AlertDialog.Builder builder = new AlertDialog.Builder(act);
					   
					    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener()
					    {
							@Override
							public void onClick(DialogInterface dialog, int which) 
							{
								String mediaType = null;
								if(which ==0)
									mediaType = "image/*";
								else if(which ==1)
									mediaType = "video/*";
								
								System.out.println("Note_edit_ui / _openSetPictureDialog / mediaType = " + mediaType);
								act.startActivityForResult(Util.chooseMediaIntentByType(act, mediaType),
				   						Util.CHOOSER_SET_PICTURE);	
								//end
								dialog.dismiss();
							}
					    };
					    builder.setTitle(R.string.edit_note_set_picture_dlg_title)
							   .setSingleChoiceItems(items, -1, listener)
							   .setNegativeButton(R.string.btn_Cancel, null)
							   .show();
					}
				})					
			   .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
			   {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{// cancel
					}
				});

				if(!Util.isEmptyString(pictureUriInDB))
				{
					builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) 
						{
							//just delete picture file name
							currPictureUri = "";
							oriPictureUri = "";
					    	removePictureStringFromCurrentEditNote(noteId);
					    	populateFields_all(noteId);
					    	bRemovePictureUri = true;
						}
					});
				}
		
		Dialog dialog = builder.create();
		dialog.show();
    }

	void deleteNote(Long rowId)
    {
    	System.out.println("Note_edit_ui / _deleteNote");
        // for Add new note (noteId is null first), but decide to cancel
        if(rowId != null)
        	dB_page.deleteNote(rowId,true);
    }
    
    // populate text fields
	void populateFields_text(Long rowId)
	{
		if (rowId != null) {
			// title
			String strTitleEdit = dB_page.getNoteTitle_byId(rowId);
			titleEditText.setText(strTitleEdit);
			titleEditText.setSelection(strTitleEdit.length());

			// picture Uri
			String strPictureUriEdit = dB_page.getNotePictureUri_byId(rowId);

			System.out.println("strPictureUriEdit 1 = " + strPictureUriEdit);
			// get real path at local device storage
			if(strPictureUriEdit.startsWith("content") ){
				strPictureUriEdit = Util.getLocalRealPathByUri(act,Uri.parse(strPictureUriEdit));

				// can not get real path, still shows DB path starts with content
				if(strPictureUriEdit==null)
					strPictureUriEdit = dB_page.getNotePictureUri_byId(rowId);
			}

			System.out.println("strPictureUriEdit 2 = " + strPictureUriEdit);

			pictureUriEditText.setText(strPictureUriEdit);
			pictureUriEditText.setSelection(strPictureUriEdit.length());
		}
        else
        {
            // renew title
            String strBlank = "";
            titleEditText.setText(strBlank);
            titleEditText.setSelection(strBlank.length());
            titleEditText.requestFocus();

            // renew body
            pictureUriEditText.setText(strBlank);
            pictureUriEditText.setSelection(strBlank.length());
        }
	}

    // populate all fields
	void populateFields_all(Long rowId)
    {
    	if (rowId != null) 
    	{
			populateFields_text(rowId);

    		// for picture block
			pictureUriInDB = dB_page.getNotePictureUri_byId(rowId);
			System.out.println("populateFields_all / mPictureFileNameInDB = " + pictureUriInDB);
    		
			// load bitmap to image view
			if( (!Util.isEmptyString(pictureUriInDB)) ){
				String thumbUri = "";
				if(!Util.isEmptyString(pictureUriInDB) )
					thumbUri = pictureUriInDB;

				new UtilImage_bitmapLoader(picImageView,
						                   thumbUri,
										   progressBar,
//    					                   (style % 2 == 1 ?
//                                            UilCommon.optionsForRounded_light:
//                                            UilCommon.optionsForRounded_dark),
                                           UilCommon.optionsForFadeIn,
                                           act);
			}
			else
			{
	    		picImageView.setImageResource(style %2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);
			}
			
			// set listeners for closing image view 
	    	if(!Util.isEmptyString(pictureUriInDB)){
	    		setCloseImageListeners(titleEditText);
	    		setCloseImageListeners(pictureUriEditText);
	    	}			
        }
    }

	private boolean isTitleModified()
    {
    	return !oriTitle.equals(titleEditText.getText().toString());
    }

	private boolean isPictureModified()
    {
    	return !oriPictureUri.equals(pictureUriInDB);
    }

	boolean isNoteModified()
    {
    	boolean bModified = false;
//		System.out.println("Note_edit_ui / _isNoteModified / isTitleModified() = " + isTitleModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isPictureModified() = " + isPictureModified());
//		System.out.println("Note_edit_ui / _isNoteModified / isBodyModified() = " + isBodyModified());
//		System.out.println("Note_edit_ui / _isNoteModified / bRemovePictureUri = " + bRemovePictureUri);
    	if( isTitleModified() ||
    		isPictureModified() ||
    		bRemovePictureUri )
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }

	Long saveStateInDB(Long rowId,boolean enSaveDb)
	{
    	String title = titleEditText.getText().toString();
        String pictureUri = pictureUriEditText.getText().toString();

        if(enSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!Util.isEmptyString(title)) ||
	        		(!Util.isEmptyString(pictureUri))       )
	        	{
	        		// insert
	        		System.out.println("Note_edit_ui / _saveStateInDB / insert");
	        		rowId = dB_page.insertNote(title, pictureUri, 0, (long) 0);// add new note, get return row Id
	        	}
        		currPictureUri = pictureUri; // update file name
	        }
	        else // for Edit
	        {
	        	if( !Util.isEmptyString(title) ||
	        		!Util.isEmptyString(pictureUri)  )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_edit_ui / _saveStateInDB / update: roll back");
	        			title = oriTitle;
	        			Long time = oriCreatedTime;
	        			dB_page.updateNote(rowId, title, pictureUri, oriMarking, time,true);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_edit_ui / _saveStateInDB / update new");
                        long marking;
                        if(null == oriMarking)
                            marking = 0;
                        else
                            marking = oriMarking;

	        			dB_page.updateNote(rowId, title, pictureUri, marking, oriCreatedTime,true); // update note
	        		}
	        		currPictureUri = pictureUri;
	        	}
	        	else if( Util.isEmptyString(title) &&
 						 Util.isEmptyString(pictureUri)    )
	        	{
	        		// delete
	        		System.out.println("Note_edit_ui / _saveStateInDB / delete");
	        		deleteNote(rowId);
			        rowId = null;
	        	}
	        }
        }

		return rowId;
	}

	// for confirmation condition
	void removePictureStringFromOriginalNote(Long rowId) {
    	dB_page.updateNote(rowId,
				oriTitle,
    				   "",
				oriMarking,
				oriCreatedTime, true );
	}

	private void removePictureStringFromCurrentEditNote(Long rowId) {
        String title = titleEditText.getText().toString();

    	dB_page.updateNote(rowId,
    				   title,
    				   "",
				oriMarking,
				oriCreatedTime, true );
	}

	public int getCount()
	{
		int noteCount = dB_page.getNotesCount(true);
		return noteCount;
	}
	
}