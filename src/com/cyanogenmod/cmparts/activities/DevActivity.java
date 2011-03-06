package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.Xml;
import android.view.IWindowManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

public class DevActivity extends PreferenceActivity implements OnPreferenceChangeListener {

     public ProgressDialog patience = null;
     final Handler mHandler = new Handler();


    //Adb wifi 
    private static final String ADB_WIFI_PREF = "adb_wifi";
    private CheckBoxPreference mAdbWifiPref;
    private static final String ADB_PORT = "5555";

    //remount 
    private static final String REMOUNT_RW_PREF = "remount_rw";
    private Preference mRemountRWPref;
    private static final String REMOUNT_RO_PREF = "remount_ro";
    private Preference mRemountROPref;
    private static final String REMOUNT_RO = "mount -o ro,remount -t yaffs2 /dev/block/mtdblock3 /system";
    private static final String REMOUNT_RW = "mount -o rw,remount -t yaffs2 /dev/block/mtdblock3 /system";


    //reboot 
    private static final String SHUTDOWN_PREF = "shutdown_shutdown";
    private Preference mShutdownPref;
    private static final String REBOOT_PREF = "reboot_reboot";
    private Preference mRebootPref;
    private static final String RECOVERY_PREF = "reboot_recovery";
    private Preference mRecoveryPref;
    private static final String BOOTLOADER_PREF = "reboot_bootloader";
    private Preference mBootloaderPref;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.dev_settings);
        PreferenceScreen prefSet = getPreferenceScreen();


        mAdbWifiPref = (CheckBoxPreference) prefSet.findPreference(ADB_WIFI_PREF);
        mAdbWifiPref.setOnPreferenceChangeListener(this);



        mRemountRWPref = (Preference) prefSet.findPreference(REMOUNT_RW_PREF);
        findPreference(REMOUNT_RO_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String[] commands = { REMOUNT_RO };
                sendshell(commands, false, getResources().getString(R.string.remounting));
                return true;
            }
        });
        mRemountROPref = (Preference) prefSet.findPreference(REMOUNT_RO_PREF);
        findPreference(REMOUNT_RW_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String[] commands = { REMOUNT_RW };
                sendshell(commands, false, getResources().getString(R.string.remounting));
                return true;
            }
        });


	mShutdownPref = (Preference) prefSet.findPreference(SHUTDOWN_PREF);
	findPreference(SHUTDOWN_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "toolbox reboot -p" };
		    sendshell(commands, false, getResources().getString(R.string.shuting_down));
		    return true;
		}
	    });
	mRebootPref = (Preference) prefSet.findPreference(REBOOT_PREF);
	findPreference(REBOOT_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });
	mRecoveryPref = (Preference) prefSet.findPreference(BOOTLOADER_PREF);
	findPreference(RECOVERY_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot recovery" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });
	mBootloaderPref = (Preference) prefSet.findPreference(RECOVERY_PREF);
	findPreference(BOOTLOADER_PREF).setOnPreferenceClickListener(new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
		    String[] commands = { "reboot bootloader" };
		    sendshell(commands, false, getResources().getString(R.string.rebooting));
		    return true;
		}
	    });


    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mAdbWifiPref) {
            boolean have = mAdbWifiPref.isChecked();
            if (!have) {
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = null;
                String promote = null;
                if (wifiInfo != null) {
                    long addr = wifiInfo.getIpAddress();
                    if (addr != 0) {
                        // handle negative values whe first octet > 127
                        if (addr < 0) addr += 0x100000000L;
                        ipAddress = String.format("%d.%d.%d.%d", addr & 0xFF, (addr >> 8) & 0xFF, (addr >> 16) & 0xFF, (addr >> 24) & 0xFF);

                        promote = getResources().getString(R.string.adb_instructions_on_with_address)
                            .replaceFirst("%ip%", ipAddress)
                            .replaceFirst("%P%", ADB_PORT);
                    }
                }

                if(promote == null){
                    promote = getResources().getString(R.string.adb_instructions_on_without_address)
                        .replaceFirst("%P%", ADB_PORT);
                }
                String[] commands = {
                    "setprop service.adb.tcp.port " + ADB_PORT,
                    "stop adbd",
                    "start adbd"
                };

                sendshell(commands, false,promote);

                mAdbWifiPref.setSummary("$ adb connect " + ipAddress + ":" + ADB_PORT);
            } else {
                String[] commands = {
                    "setprop service.adb.tcp.port -1",
                    "stop adbd",
                    "start adbd"
                };
                sendshell(commands, false, getResources().getString(R.string.adb_instructions_off));
                mAdbWifiPref.setSummary("$ adb usb");
            }
        }


        return true;
    }



    /**
     *  Shell interaction
     */

    final Runnable mCommandFinished = new Runnable() {
        public void run() { 
            patience.cancel(); 
        }
    };

    public boolean sendshell(final String[] commands, final boolean reboot, final String message) {
        if (message != null)
            patience = ProgressDialog.show(this, "", message, true);

        Thread t = new Thread() {
            public void run() {
                ShellInterface shell = new ShellInterface(commands);
                shell.start();
                while (shell.isAlive())
                {
                    if (message != null)
                        patience.setProgress(shell.getStatus());
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (message != null)
                    mHandler.post(mCommandFinished);
                /*
                   if (shell.interrupted())
                   popup(getResources().getString(R.string.error), getResources().getString(R.string.download_install_error));
                   if (reboot == true)
                   mHandler.post(mNeedReboot);
                   */
            }
        };

        t.start();
        return true;
    }
}
