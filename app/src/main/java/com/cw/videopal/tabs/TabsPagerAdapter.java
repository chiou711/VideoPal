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

package com.cw.videopal.tabs;

import com.cw.videopal.db.DB_folder;
import com.cw.videopal.page.Page_recycler;
import com.cw.videopal.util.preferences.Pref;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Created by cw on 2018/3/20.
 *
 *  View Pager Adapter Class
 *
 */
    public class TabsPagerAdapter extends FragmentStateAdapter {
    public ArrayList<Page_recycler> fragmentList = new ArrayList<>();
    DB_folder dbFolder;

    public TabsPagerAdapter(@NonNull AppCompatActivity act) {
        super(act);
        int folderTableId = Pref.getPref_focusView_folder_tableId(act);
        dbFolder = new DB_folder(act, folderTableId);
    }

    public Page_recycler getItem(int position){
        return fragmentList.get(position);
    }

    // add fragment
    public void addFragment(Page_recycler fragment) {
        fragmentList.add(fragment);
    }

    public CharSequence getPageTitle(int position){
//        System.out.println("TabsPagerAdapter / _getPageTitle / position = " + position);
        return dbFolder.getPageTitle(position,true);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
}

