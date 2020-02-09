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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.noctis.android.notes.async.DataBackupIntentService;
import com.noctis.android.notes.helpers.AppVersionHelper;
import com.noctis.android.notes.helpers.BackupHelper;
import com.noctis.android.notes.helpers.LanguageHelper;
import com.noctis.android.notes.helpers.LogDelegate;
import com.noctis.android.notes.helpers.PermissionsHelper;
import com.noctis.android.notes.helpers.SpringImportHelper;
import com.noctis.android.notes.models.ONStyle;
import com.noctis.android.notes.models.PasswordValidator;
import com.noctis.android.notes.models.listeners.RecyclerViewItemClickSupport;
import com.noctis.android.notes.utils.Constants;
import com.noctis.android.notes.utils.ConstantsBase;
import com.noctis.android.notes.utils.FileHelper;
import com.noctis.android.notes.utils.IntentChecker;
import com.noctis.android.notes.utils.PasswordHelper;
import com.noctis.android.notes.utils.ResourcesUtils;
import com.noctis.android.notes.utils.StorageHelper;
import com.noctis.android.notes.utils.SystemHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import com.noctis.android.notes.R;

import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;


public class SettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences prefs;

    private static final int SPRINGPAD_IMPORT = 0;
    private static final int RINGTONE_REQUEST_CODE = 100;
    public static final String XML_NAME = "xmlName";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int xmlId = R.xml.settings;
        if (getArguments() != null && getArguments().containsKey(XML_NAME)) {
            xmlId = ResourcesUtils.getXmlId(NotesApp.getAppContext(), ResourcesUtils.ResourceIdentifiers.XML, String
                    .valueOf(getArguments().get(XML_NAME)));
        }
        addPreferencesFromResource(xmlId);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        prefs = getContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS);
        setTitle();
    }

    private void setTitle() {
        String title = getString(R.string.settings);
        if (getArguments() != null && getArguments().containsKey(XML_NAME)) {
            String xmlName = getArguments().getString(XML_NAME);
            if (!TextUtils.isEmpty(xmlName)) {
                int stringResourceId = getActivity().getResources().getIdentifier(xmlName.replace("settings_",
                        "settings_screen_"), "string", getActivity().getPackageName());
                title = stringResourceId != 0 ? getString(stringResourceId) : title;
            }
        }
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
        } else {
            LogDelegate.e("Wrong element choosen: " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Export notes
        Preference export = findPreference("settings_export_data");
        if (export != null) {
            export.setOnPreferenceClickListener(arg0 -> {

                // Inflate layout
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View v = inflater.inflate(R.layout.dialog_backup_layout, null);

                // Finds actually saved backups names
                PermissionsHelper.requestPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, R
                        .string.permission_external_storage, getActivity().findViewById(R.id.crouton_handle), () -> export
                        (v));

                return false;
            });
        }

        // Import notes
        Preference importData = findPreference("settings_import_data");
        if (importData != null) {
            importData.setOnPreferenceClickListener(arg0 -> {
                PermissionsHelper.requestPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, R
                        .string.permission_external_storage, getActivity().findViewById(R.id.crouton_handle), this::importNotes);
                return false;
            });
        }

        // Import legacy notes
        Preference importLegacyData = findPreference("settings_import_data_legacy");
        if (importLegacyData != null) {
            importLegacyData.setOnPreferenceClickListener(arg0 -> {

                // Finds actually saved backups names
                PermissionsHelper.requestPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE, R
                        .string.permission_external_storage, getActivity().findViewById(R.id.crouton_handle), () -> new
                        FolderChooserDialog.Builder(getActivity())
                        .chooseButton(R.string.md_choose_label)
                        .show(getActivity()));
                return false;
            });
        }

        Preference importFromSpringpad = findPreference("settings_import_from_springpad");
        if (importFromSpringpad != null) {
            importFromSpringpad.setOnPreferenceClickListener(arg0 -> {
                Intent intent;
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                if (!IntentChecker.isAvailable(getActivity(), intent, null)) {
                    Toast.makeText(getActivity(), R.string.feature_not_available_on_this_device,
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                startActivityForResult(intent, SPRINGPAD_IMPORT);
                return false;
            });
        }

        // Swiping action
        final SwitchPreference swipeToTrash = findPreference("settings_swipe_to_trash");
        if (swipeToTrash != null) {
            if (prefs.getBoolean("settings_swipe_to_trash", false)) {
                swipeToTrash.setChecked(true);
                swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
            } else {
                swipeToTrash.setChecked(false);
                swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
            }
            swipeToTrash.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
                } else {
                    swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
                }
                return true;
            });
        }

        // Show uncategorized notes in menu
        final SwitchPreference showUncategorized = findPreference(ConstantsBase.PREF_SHOW_UNCATEGORIZED);
        if (showUncategorized != null) {
            showUncategorized.setOnPreferenceChangeListener((preference, newValue) -> true);
        }

        // Show Automatically adds location to new notes
        final SwitchPreference autoLocation = findPreference(ConstantsBase.PREF_AUTO_LOCATION);
        if (autoLocation != null) {
            autoLocation.setOnPreferenceChangeListener((preference, newValue) -> true);
        }

        // Maximum video attachment size
        final EditTextPreference maxVideoSize = findPreference("settings_max_video_size");
        if (maxVideoSize != null) {
            maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": "
                    + prefs.getString("settings_max_video_size", getString(R.string.not_set)));
            maxVideoSize.setOnPreferenceChangeListener((preference, newValue) -> {
                maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + newValue);
                prefs.edit().putString("settings_max_video_size", newValue.toString()).apply();
                return false;
            });
        }

        // Set notes' protection password
        Preference password = findPreference("settings_password");
        if (password != null) {
            password.setOnPreferenceClickListener(preference -> {
                Intent passwordIntent = new Intent(getActivity(), PasswordActivity.class);
                startActivity(passwordIntent);
                return false;
            });
        }

        // Use password to grant application access
        final SwitchPreference passwordAccess = findPreference("settings_password_access");
        if (passwordAccess != null) {
            if (prefs.getString(ConstantsBase.PREF_PASSWORD, null) == null) {
                passwordAccess.setEnabled(false);
                passwordAccess.setChecked(false);
            } else {
                passwordAccess.setEnabled(true);
            }
            passwordAccess.setOnPreferenceChangeListener((preference, newValue) -> {
                PasswordHelper.requestPassword(getActivity(), passwordConfirmed -> {
                    if (passwordConfirmed.equals(PasswordValidator.Result.SUCCEED)) {
                        passwordAccess.setChecked((Boolean) newValue);
                    }
                });
                return true;
            });
        }

        // Languages
        ListPreference lang = findPreference("settings_language");
        if (lang != null) {
            String languageName = getResources().getConfiguration().locale.getDisplayName();
            lang.setSummary(languageName.substring(0, 1).toUpperCase(getResources().getConfiguration().locale)
                    + languageName.substring(1));
            lang.setOnPreferenceChangeListener((preference, value) -> {
                LanguageHelper.updateLanguage(getActivity(), value.toString());
                SystemHelper.restartApp(getActivity().getApplicationContext(), MainActivity.class);
                return false;
            });
        }

        // Checklists
        final ListPreference checklist = findPreference("settings_checked_items_behavior");
        if (checklist != null) {
            int checklistIndex = checklist.findIndexOfValue(prefs.getString("settings_checked_items_behavior", "0"));
            String checklistString = getResources().getStringArray(R.array.checked_items_behavior)[checklistIndex];
            checklist.setSummary(checklistString);
            checklist.setOnPreferenceChangeListener((preference, newValue) -> {
                int checklistIndex1 = checklist.findIndexOfValue(newValue.toString());
                String checklistString1 = getResources().getStringArray(R.array.checked_items_behavior)
                        [checklistIndex1];
                checklist.setSummary(checklistString1);
                prefs.edit().putString("settings_checked_items_behavior", newValue.toString()).apply();
                checklist.setValueIndex(checklistIndex1);
                return false;
            });
        }

        // Widget's colors
        final ListPreference colorsWidget = findPreference("settings_colors_widget");
        if (colorsWidget != null) {
            int colorsWidgetIndex = colorsWidget.findIndexOfValue(prefs.getString("settings_colors_widget",
                    ConstantsBase.PREF_COLORS_APP_DEFAULT));
            String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
            colorsWidget.setSummary(colorsWidgetString);
            colorsWidget.setOnPreferenceChangeListener((preference, newValue) -> {
                int colorsWidgetIndex1 = colorsWidget.findIndexOfValue(newValue.toString());
                String colorsWidgetString1 = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex1];
                colorsWidget.setSummary(colorsWidgetString1);
                prefs.edit().putString("settings_colors_widget", newValue.toString()).apply();
                colorsWidget.setValueIndex(colorsWidgetIndex1);
                return false;
            });
        }

        // Ringtone selection
        final Preference ringtone = findPreference("settings_notification_ringtone");
        if (ringtone != null) {
            ringtone.setOnPreferenceClickListener(arg0 -> {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, DEFAULT_NOTIFICATION_URI);

                String existingValue = prefs.getString("settings_notification_ringtone", null);
                if (existingValue != null) {
                    if (existingValue.length() == 0) {
                        // Select "Silent"
                        intent.putExtra(EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                    } else {
                        intent.putExtra(EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                    }
                } else {
                    // No ringtone has been selected, set to the default
                    intent.putExtra(EXTRA_RINGTONE_EXISTING_URI, DEFAULT_NOTIFICATION_URI);
                }

                startActivityForResult(intent, RINGTONE_REQUEST_CODE);

                return false;
            });
        }

        // Notification snooze delay
        final EditTextPreference snoozeDelay = findPreference("settings_notification_snooze_delay");
        if (snoozeDelay != null) {
            String snooze = prefs.getString("settings_notification_snooze_delay", ConstantsBase.PREF_SNOOZE_DEFAULT);
            snooze = TextUtils.isEmpty(snooze) ? ConstantsBase.PREF_SNOOZE_DEFAULT : snooze;
            snoozeDelay.setSummary(snooze + " " + getString(R.string.minutes));
            snoozeDelay.setOnPreferenceChangeListener((preference, newValue) -> {
                String snoozeUpdated = TextUtils.isEmpty(String.valueOf(newValue)) ? ConstantsBase.PREF_SNOOZE_DEFAULT : String.valueOf(newValue);
                snoozeDelay.setSummary(snoozeUpdated + " " + getString(R.string.minutes));
                prefs.edit().putString("settings_notification_snooze_delay", snoozeUpdated).apply();
                return false;
            });
        }

        // NotificationServiceListener shortcut
        final Preference norificationServiceListenerPreference = findPreference("settings_notification_service_listener");
        if (norificationServiceListenerPreference != null) {
            getPreferenceScreen().removePreference(norificationServiceListenerPreference);
        }

        // Settings reset
        Preference resetData = findPreference("reset_all_data");
        if (resetData != null) {
            resetData.setOnPreferenceClickListener(arg0 -> {

                new MaterialDialog.Builder(getContext())
                        .content(R.string.reset_all_data_confirmation)
                        .positiveText(R.string.confirm)
                        .onPositive((dialog, which) -> {
                            prefs.edit().clear().apply();
                            File db = getActivity().getDatabasePath(ConstantsBase.DATABASE_NAME);
                            StorageHelper.delete(getActivity(), db.getAbsolutePath());
                            File attachmentsDir = StorageHelper.getAttachmentDir();
                            StorageHelper.delete(getActivity(), attachmentsDir.getAbsolutePath());
                            File cacheDir = StorageHelper.getCacheDir(getActivity());
                            StorageHelper.delete(getActivity(), cacheDir.getAbsolutePath());
                            SystemHelper.restartApp(getActivity().getApplicationContext(), MainActivity.class);
                        }).build().show();

                return false;
            });
        }

        // Logs on files activation
        final SwitchPreference enableFileLogging = findPreference(ConstantsBase.PREF_ENABLE_FILE_LOGGING);
        if (enableFileLogging != null) {
            enableFileLogging.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    PermissionsHelper.requestPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, R
                                    .string.permission_external_storage, getActivity().findViewById(R.id.crouton_handle),
                            () -> enableFileLogging.setChecked(true));
                } else {
                    enableFileLogging.setChecked(false);
                }
                return false;
            });
        }

    }


    private void importNotes() {
        final List<String> backups = asList(StorageHelper.getExternalStoragePublicDir().list());
        reverse(backups);

        if (backups.isEmpty()) {
            ((SettingsActivity) getActivity()).showMessage(R.string.no_backups_available, ONStyle.WARN);
        } else {

            MaterialDialog importDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.data_import_message)
                    .items(backups)
                    .positiveText(R.string.confirm)
                    .onPositive((dialog, which) -> {

                    }).build();

            // OnShow is overridden to allow long-click on item so user can remove them
            importDialog.setOnShowListener(dialog -> {

                RecyclerViewItemClickSupport.addTo(importDialog.getRecyclerView()).setOnItemClickListener((recyclerView, position, v) -> {
                    // Retrieves backup size
                    File backupDir = StorageHelper.getBackupDir(backups.get(position));
                    long size = StorageHelper.getSize(backupDir) / 1024;
                    String sizeString = size > 1024 ? size / 1024 + "Mb" : size + "Kb";

                    // Check preference presence
                    String prefName = StorageHelper.getSharedPreferencesFile(getActivity()).getName();
                    boolean hasPreferences = (new File(backupDir, prefName)).exists();

                    String message = backups.get(position)
                            + " (" + sizeString
                            + (hasPreferences ? " " + getString(R.string.settings_included) : "")
                            + ")";

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.confirm_restoring_backup)
                            .content(message)
                            .positiveText(R.string.confirm)
                            .onPositive((dialog1, which) -> {

                                importDialog.dismiss();

                                // An IntentService will be launched to accomplish the import task
                                Intent service = new Intent(getActivity(),
                                        DataBackupIntentService.class);
                                service.setAction(DataBackupIntentService.ACTION_DATA_IMPORT);
                                service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME,
                                        backups.get(position));
                                getActivity().startService(service);
                            }).build().show();
                });

                // Creation of backup removal dialog
                RecyclerViewItemClickSupport.addTo(importDialog.getRecyclerView()).setOnItemLongClickListener((recyclerView, position, v) -> {
                    // Retrieves backup size
                    File backupDir = StorageHelper.getBackupDir(backups.get(position));
                    long size = StorageHelper.getSize(backupDir) / 1024;
                    String sizeString = size > 1024 ? size / 1024 + "Mb" : size + "Kb";

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.confirm_removing_backup)
                            .content(backups.get(position) + "" + " (" + sizeString + ")")
                            .positiveText(R.string.confirm)
                            .onPositive((dialog12, which) -> {
                                importDialog.dismiss();
                                // An IntentService will be launched to accomplish the deletion task
                                Intent service = new Intent(getActivity(),
                                        DataBackupIntentService.class);
                                service.setAction(DataBackupIntentService.ACTION_DATA_DELETE);
                                service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME,
                                        backups.get(position));
                                getActivity().startService(service);
                            }).build().show();

                    return true;
                });
            });

            importDialog.show();
        }
    }


    private void export(View v) {
        final List<String> backups = asList(StorageHelper.getExternalStoragePublicDir().list());

        // Sets default export file name
        SimpleDateFormat sdf = new SimpleDateFormat(ConstantsBase.DATE_FORMAT_EXPORT);
        String fileName = sdf.format(Calendar.getInstance().getTime());
        final EditText fileNameEditText = v.findViewById(R.id.export_file_name);
        final TextView backupExistingTextView = v.findViewById(R.id.backup_existing);
        fileNameEditText.setHint(fileName);
        fileNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // Nothing to do
            }


            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // Nothing to do
            }


            @Override
            public void afterTextChanged(Editable arg0) {

                if (backups.contains(arg0.toString())) {
                    backupExistingTextView.setText(R.string.backup_existing);
                } else {
                    backupExistingTextView.setText("");
                }
            }
        });

        new MaterialDialog.Builder(getActivity())
                .title(R.string.data_export_message)
                .customView(v, false)
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {
                    String backupName = TextUtils.isEmpty(fileNameEditText.getText().toString()) ?
                            fileNameEditText.getHint().toString() : fileNameEditText.getText().toString();
                    BackupHelper.startBackupService(backupName);
                }).build().show();
    }


    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SPRINGPAD_IMPORT:
                    Uri filesUri = intent.getData();
                    String path = FileHelper.getPath(getActivity(), filesUri);
                    // An IntentService will be launched to accomplish the import task
                    Intent service = new Intent(getActivity(), DataBackupIntentService.class);
                    service.setAction(SpringImportHelper.ACTION_DATA_IMPORT_SPRINGPAD);
                    service.putExtra(SpringImportHelper.EXTRA_SPRINGPAD_BACKUP, path);
                    getActivity().startService(service);
                    break;

                case RINGTONE_REQUEST_CODE:
                    Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String notificationSound = uri == null ? null : uri.toString();
                    prefs.edit().putString("settings_notification_ringtone", notificationSound).apply();
                    break;

                default:
                    LogDelegate.e("Wrong element choosen: " + requestCode);
            }
        }
    }
}
