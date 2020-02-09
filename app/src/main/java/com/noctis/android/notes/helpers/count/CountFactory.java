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

package com.noctis.android.notes.helpers.count;

import com.noctis.android.notes.NotesApp;
import com.noctis.android.notes.helpers.LanguageHelper;

public class CountFactory {

  private CountFactory () {
  }

  public static WordCounter getWordCounter () {
    String locale = LanguageHelper.getCurrentLocaleAsString(NotesApp.getAppContext());
    return getCounterInstanceByLocale(locale);
  }

  static WordCounter getCounterInstanceByLocale (String locale) {
    switch (locale) {
      case "ja_JP":
        return new IdeogramsWordCounter();
      case "zh_CN":
        return new IdeogramsWordCounter();
      case "zh_TW":
        return new IdeogramsWordCounter();
      default:
        return new DefaultWordCounter();
    }
  }
}