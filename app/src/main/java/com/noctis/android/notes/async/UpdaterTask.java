/*
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.noctis.android.notes.async;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.noctis.android.notes.helpers.AppVersionHelper;
import com.noctis.android.notes.helpers.LogDelegate;
import com.noctis.android.notes.models.misc.PlayStoreMetadataFetcherResult;
import com.noctis.android.notes.utils.ConnectionManager;
import com.noctis.android.notes.utils.Constants;
import com.noctis.android.notes.utils.ConstantsBase;
import com.noctis.android.notes.NotesApp;
import com.noctis.android.notes.R;

import java.lang.ref.WeakReference;


public class UpdaterTask extends AsyncTask<String, Void, Void> {

  private static final String BETA = " Beta ";
  private final WeakReference<Activity> mActivityReference;
  private final Activity mActivity;
  private final SharedPreferences prefs;
  private boolean promptUpdate = false;
  private long now;


  public UpdaterTask (Activity mActivity) {
    this.mActivityReference = new WeakReference<>(mActivity);
    this.mActivity = mActivity;
    this.prefs = mActivity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS);
  }


  @Override
  protected void onPreExecute () {
    now = System.currentTimeMillis();
    if (NotesApp.isDebugBuild() || !ConnectionManager.internetAvailable(NotesApp.getAppContext())
        || now < prefs.getLong(ConstantsBase.PREF_LAST_UPDATE_CHECK, 0) + ConstantsBase.UPDATE_MIN_FREQUENCY) {
      cancel(true);
    }
    super.onPreExecute();
  }


  @Override
  protected Void doInBackground (String... params) {
    if (!isCancelled()) {
      try {
        // Temporary disabled untill MetadataFetcher will work again
        // promptUpdate = isVersionUpdated(getAppData());
        promptUpdate = false;
        if (promptUpdate) {
          prefs.edit().putLong(ConstantsBase.PREF_LAST_UPDATE_CHECK, now).apply();
        }
      } catch (Exception e) {
        LogDelegate.w("Error fetching app metadata", e);
      }
    }
    return null;
  }


  private void promptUpdate () {
    new MaterialDialog.Builder(mActivityReference.get())
        .title(R.string.app_name)
        .content(R.string.new_update_available)
        .positiveText(R.string.update)
        .negativeText(R.string.not_now)
        .negativeColorRes(R.color.colorPrimary)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick (@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

          }
        }).build().show();
  }


  @Override
  protected void onPostExecute (Void result) {
    if (isAlive(mActivityReference)) {
      if (promptUpdate) {
        promptUpdate();
      } else {
        try {
          boolean appVersionUpdated = AppVersionHelper.isAppUpdated(mActivity);
          if (appVersionUpdated) {
            restoreReminders();
            AppVersionHelper.updateAppVersionInPreferences(mActivity);
          }
        } catch (NameNotFoundException e) {
          LogDelegate.e("Error retrieving app version", e);
        }
      }
    }
  }

  private void restoreReminders () {
    Intent service = new Intent(mActivity, AlarmRestoreOnRebootService.class);
    mActivity.startService(service);
  }


  private boolean isAlive (WeakReference<Activity> weakActivityReference) {
    return !(weakActivityReference.get() == null || weakActivityReference.get().isFinishing());
  }


  /**
   * Checks parsing "android:versionName" if app has been updated
   */
  private boolean isVersionUpdated (PlayStoreMetadataFetcherResult playStoreMetadataFetcherResult)
      throws NameNotFoundException {

    String playStoreVersion = playStoreMetadataFetcherResult.getSoftwareVersion();

    // Retrieval of installed app version
    PackageInfo pInfo = mActivity.getPackageManager().getPackageInfo(
        mActivity.getPackageName(), 0);
    String installedVersion = pInfo.versionName;

    // Parsing version string to obtain major.minor.point (excluding eventually beta)
    String[] playStoreVersionArray = playStoreVersion.split(BETA)[0].split("\\.");
    String[] installedVersionArray = installedVersion.split(BETA)[0].split("\\.");

    // Versions strings are converted into integer
    String playStoreVersionString = playStoreVersionArray[0];
    String installedVersionString = installedVersionArray[0];
    for (int i = 1; i < playStoreVersionArray.length; i++) {
      playStoreVersionString += String.format("%02d", Integer.parseInt(playStoreVersionArray[i]));
      installedVersionString += String.format("%02d", Integer.parseInt(installedVersionArray[i]));
    }

    boolean playStoreHasMoreRecentVersion =
        Integer.parseInt(playStoreVersionString) > Integer.parseInt(installedVersionString);
    boolean outOfBeta = Integer.parseInt(playStoreVersionString) == Integer.parseInt(installedVersionString)
        && playStoreVersion.split("b").length == 1 && installedVersion.split("b").length == 2;

    return playStoreHasMoreRecentVersion || outOfBeta;
  }
}
