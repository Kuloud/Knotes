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

package com.noctis.android.notes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.StrictMode;

import androidx.multidex.MultiDexApplication;

import com.noctis.android.notes.helpers.LanguageHelper;
import com.noctis.android.notes.utils.Constants;
import com.noctis.android.notes.utils.ConstantsBase;
import com.noctis.android.notes.utils.notifications.NotificationsHelper;


public class NotesApp extends Application {

  static SharedPreferences prefs;
  private static Context mContext;

  public static boolean isDebugBuild () {
    return BuildConfig.BUILD_TYPE.equals("debug");
  }

  public static Context getAppContext () {
    return NotesApp.mContext;
  }

  /**
   * Statically returns app's default SharedPreferences instance
   *
   * @return SharedPreferences object instance
   */
  public static SharedPreferences getSharedPreferences () {
    return getAppContext().getSharedPreferences(Constants.PREFS_NAME, MODE_MULTI_PROCESS);
  }

  @Override
  protected void attachBaseContext (Context base) {
    super.attachBaseContext(base);
  }

  @Override
  public void onCreate () {
    super.onCreate();

    mContext = getApplicationContext();
    prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_MULTI_PROCESS);

    enableStrictMode();

    new NotificationsHelper(this).initNotificationChannels();
  }

  private void enableStrictMode () {
    if (isDebugBuild()) {
      StrictMode.enableDefaults();
    }
  }

  @Override
  public void onConfigurationChanged (Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    String language = prefs.getString(ConstantsBase.PREF_LANG, "");
    LanguageHelper.updateLanguage(this, language);
  }

}
