/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.content.pm.IPackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;


import java.util.HashSet;
import java.util.List;
import java.util.Set;


import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class ApplicationActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    private static final String INSTALL_LOCATION_PREF = "pref_install_location";
    
    private static final String MOVE_ALL_APPS_PREF = "pref_move_all_apps";
    
    private static final String LOG_TAG = "CMParts";
    
    private CheckBoxPreference mMoveAllAppsPref;
    
    private ListPreference mInstallLocationPref;
    
    private IPackageManager mPm2;
        

    private static final String TASKMGR_APP_LIST_PREF = "taskmgr_app_pref";
	
	private ListPreference mTaskMgrPref;
	private AppInfoData mAppInfo ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPm2 = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (mPm2 == null) {
            Log.wtf(LOG_TAG, "Unable to get PackageManager!");
        }
        
        setTitle(R.string.application_settings_title_subhead);
        addPreferencesFromResource(R.xml.application_settings);
        
        PreferenceScreen prefSet = getPreferenceScreen();
        
        mInstallLocationPref = (ListPreference) prefSet.findPreference(INSTALL_LOCATION_PREF);
        String installLocation = "0";
        try {
            installLocation = String.valueOf(mPm2.getInstallLocation());
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Unable to get default install location!", e);
        }
        mInstallLocationPref.setValue(installLocation);
        mInstallLocationPref.setOnPreferenceChangeListener(this);
        
        mMoveAllAppsPref = (CheckBoxPreference) prefSet.findPreference(MOVE_ALL_APPS_PREF);
        mMoveAllAppsPref.setChecked(Settings.Secure.getInt(getContentResolver(), 
            Settings.Secure.ALLOW_MOVE_ALL_APPS_EXTERNAL, 0) == 1);


        mTaskMgrPref = 
        	(ListPreference) prefSet.findPreference(TASKMGR_APP_LIST_PREF);
        mTaskMgrPref.setEnabled(false);
        mTaskMgrPref.setOnPreferenceChangeListener(this);

        loadApps.start();
    }
        
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mMoveAllAppsPref) {
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ALLOW_MOVE_ALL_APPS_EXTERNAL, mMoveAllAppsPref.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mInstallLocationPref) {
            if (newValue != null) {
                try {
                    mPm2.setInstallLocation(Integer.valueOf((String)newValue));
                    return true;
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Unable to get default install location!", e);
                }
            }
        }else if(mTaskMgrPref == preference){
            String name = (String) newValue;
              Settings.Secure.putString(getContentResolver(),
                Settings.Secure.TASK_MGR_APP, name);
        }

        return false;
    }



  private Handler mHandler = new Handler(){
		   public void handleMessage(Message msg) {
			   super.handleMessage(msg);
               if (msg.what==1){
                   mTaskMgrPref.setEntries(mAppInfo.appName);
                   mTaskMgrPref.setEntryValues(mAppInfo.packageName);
                   String name = Settings.Secure.getString(getContentResolver(), 
            Settings.Secure.TASK_MGR_APP );
                   if(name != null){
                        mTaskMgrPref.setValue(name);
                   }
                   
                   mTaskMgrPref.setEnabled(true);
               }
		   }
    };
    
    private Thread loadApps = new Thread(){
    	public void run(){    		
    		mAppInfo =  getAppInfoLst();
    		Message msg = mHandler.obtainMessage(1);
    		mHandler.sendMessage(msg);    		
    	}
    };
    
    class AppInfoData{
    	public CharSequence[] appName; 
    	public CharSequence[] packageName;
    	public AppInfoData(int size){
    		appName = new CharSequence[size];
    		packageName = new CharSequence[size];
    	}
    }
    
    PackageManager mPm;
    
    public boolean isSystemApp(String packageName){
        PackageInfo pkgInfo = null;
        Set<PermissionInfo> permSet = new HashSet<PermissionInfo>();
        
        try {
            pkgInfo = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            //Log.w(TAG, "Could'nt retrieve permissions for package:"+p.packageName);
            //return;
        }
        // Extract all user permissions
        if((pkgInfo.applicationInfo != null) && (pkgInfo.applicationInfo.uid != -1)) {
            getAllUsedPermissions(pkgInfo.applicationInfo.uid, permSet);
        }
        
        
        for(PermissionInfo tmpInfo : permSet) {
            if(
            		tmpInfo.name.equals("android.permission.KILL_BACKGROUND_PROCESSES")){
            	return true;
            }
        }
        
        return false;
    }
    
    public  String getActivityName(String packageName){
    	PackageInfo pkgInfo = null;
    	try {
            pkgInfo = mPm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {
            //Log.w(TAG, "Could'nt retrieve permissions for package:"+p.packageName);
            //return;
        }
        
        ActivityInfo[] activities = pkgInfo.activities;
        
        if(activities != null ){
        	return activities[0].name;
        }
        return null;       
    }
  
    public AppInfoData getAppInfoLst(){
    	mPm = getPackageManager();  
    	
        List<PackageInfo> packs = mPm.getInstalledPackages(0);  
       
        int j = 0;
        for (int i = 0; i < packs.size(); i++) {  
            PackageInfo p = packs.get(i);  
          
           /* if (p.applicationInfo.sourceDir.startsWith("/system/app")) {  
                // ignore system application  
                continue;  
            }*/
            
            if(!isSystemApp(p.packageName))
            	continue;
            j++;
        }
        
        AppInfoData appLists = new AppInfoData(j);
        j = 0;
        for (int i = 0; i < packs.size(); i++) {  
            PackageInfo p = packs.get(i);  
            /*
            if (p.applicationInfo.sourceDir.startsWith("/system/app")) {  
                // ignore system application  
                continue;  
            }
            */
            
            if(!isSystemApp(p.packageName))
            	continue;
            
            ApplicationInfo appInfo = p.applicationInfo; 
            
            appLists.appName[j] = mPm.getApplicationLabel(appInfo)  
                    .toString();  
            appLists.packageName[j] =  p.packageName;
            
            String actname = getActivityName(p.packageName);
            j++;

        }  
        
        return appLists;
    }
    
    private void getAllUsedPermissions(int sharedUid, Set<PermissionInfo> permSet) {
        String sharedPkgList[] = mPm.getPackagesForUid(sharedUid);
        if(sharedPkgList == null || (sharedPkgList.length == 0)) {
            return;
        }
        for(String sharedPkg : sharedPkgList) {
            getPermissionsForPackage(sharedPkg, permSet);
        }
    }
    
    private void getPermissionsForPackage(String packageName, 
            Set<PermissionInfo> permSet) {
        PackageInfo pkgInfo;
        try {
            pkgInfo = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            //Log.w(TAG, "Could'nt retrieve permissions for package:"+packageName);
            return;
        }
        if ((pkgInfo != null) && (pkgInfo.requestedPermissions != null)) {
            extractPerms(pkgInfo.requestedPermissions, permSet);
        }
    }
    
    private void extractPerms(String strList[], Set<PermissionInfo> permSet) {
        if((strList == null) || (strList.length == 0)) {
            return;
        }
        
        for(String permName:strList) {
            try {
                PermissionInfo tmpPermInfo = mPm.getPermissionInfo(permName, 0);
                if(tmpPermInfo != null) {
                    permSet.add(tmpPermInfo);
                }
            } catch (NameNotFoundException e) {
                //Log.i(TAG, "Ignoring unknown permission:"+permName);
            }
        }
    }
    

}
