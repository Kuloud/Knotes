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

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.noctis.android.notes.NotesApp;
import com.noctis.android.notes.BaseAndroidTestCase;

import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class CountFactoryTest extends BaseAndroidTestCase {

  @Test
  public void getWordCounterForEnglish () {
    NotesApp.getAppContext().getResources().getConfiguration().setLocale(Locale.US);
    assertEquals(DefaultWordCounter.class, CountFactory.getWordCounter().getClass());
  }

  @Test
  public void getWordCounterForItalian () {
    NotesApp.getAppContext().getResources().getConfiguration().setLocale(Locale.ITALY);
    assertEquals(DefaultWordCounter.class, CountFactory.getWordCounter().getClass());
  }

  @Test
  public void getWordCounterForChineseSimplified () {
    NotesApp.getAppContext().getResources().getConfiguration().setLocale(Locale.SIMPLIFIED_CHINESE);
    assertEquals(IdeogramsWordCounter.class, CountFactory.getWordCounter().getClass());
  }

  @Test
  public void getWordCounterForChineseTraditional () {
    NotesApp.getAppContext().getResources().getConfiguration().setLocale(Locale.TRADITIONAL_CHINESE);
    assertEquals(IdeogramsWordCounter.class, CountFactory.getWordCounter().getClass());
  }

}