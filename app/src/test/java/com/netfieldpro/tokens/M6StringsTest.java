package com.netfieldpro.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.netfieldpro.R;

@RunWith(RobolectricTestRunner.class)
public class M6StringsTest {

    @Test
    public void testAllM6CanonicalStrings() {
        Context context = ApplicationProvider.getApplicationContext();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(R.string.projects_title, "Projects");
        expected.put(R.string.projects_empty_title, "No projects yet");
        expected.put(R.string.projects_empty_hint, "Create a project, then import a floor plan.");
        expected.put(R.string.projects_new, "New project");
        expected.put(R.string.project_floors_count, "%1$d floors");
        expected.put(R.string.project_floors_count_one, "1 floor");
        expected.put(R.string.project_menu_rename, "Rename");
        expected.put(R.string.project_menu_delete, "Delete");
        expected.put(R.string.project_delete_title, "Delete project?");
        expected.put(R.string.project_delete_body, "This deletes the project, its floors and all surveys. This cannot be undone.");
        expected.put(R.string.dialog_delete, "Delete");
        expected.put(R.string.dialog_cancel, "Cancel");
        expected.put(R.string.dialog_save, "Save");
        expected.put(R.string.project_name_label, "Project name");
        expected.put(R.string.project_site_label, "Site (optional)");
        expected.put(R.string.err_name_empty, "Enter a name.");
        expected.put(R.string.err_name_long, "Use 60 characters or fewer.");
        expected.put(R.string.err_name_chars, "Remove line breaks and control characters.");
        expected.put(R.string.floor_level_label, "Level %1$d");
        expected.put(R.string.floor_scale_set, "Scale set · ±%1$s%");
        expected.put(R.string.floor_scale_missing, "Scale not set");
        expected.put(R.string.floor_set_scale, "Set scale");
        expected.put(R.string.floor_start_survey, "Start survey");
        expected.put(R.string.floor_add, "Add floor");
        expected.put(R.string.import_pick, "Choose floor plan");
        expected.put(R.string.import_hint, "PNG, JPG or PDF, up to 50 MB.");
        expected.put(R.string.import_floor_name, "Floor name");
        expected.put(R.string.import_level, "Level number");
        expected.put(R.string.import_save, "Save floor plan");
        expected.put(R.string.import_err_type, "This file type isn't supported.");
        expected.put(R.string.import_err_size, "The file is larger than 50 MB.");
        expected.put(R.string.import_err_small, "The image is too small to use as a floor plan.");
        expected.put(R.string.import_err_decode, "Couldn't read this file.");
        expected.put(R.string.import_err_pdf_empty, "This PDF has no pages.");
        expected.put(R.string.import_err_level, "Level must be between -5 and 99.");
        expected.put(R.string.scale_title, "Set scale");
        expected.put(R.string.scale_mode_line, "Measure a known distance");
        expected.put(R.string.scale_mode_width, "Plan width is known");
        expected.put(R.string.scale_distance_label, "Real distance (meters)");
        expected.put(R.string.scale_readout, "1 px = %1$s m · ±%2$s%");
        expected.put(R.string.scale_err_short, "Place the points farther apart.");
        expected.put(R.string.scale_err_distance, "Enter a distance between 0.5 and 500 meters.");
        expected.put(R.string.scale_save, "Save scale");
        expected.put(R.string.scale_locked, "Scale can't be changed after a survey was finalized.");
        expected.put(R.string.paywall_quota_project, "Free plan includes 1 project.");
        expected.put(R.string.paywall_quota_floor, "Free plan includes 1 floor.");
        expected.put(R.string.paywall_quota_session, "Free plan includes 2 surveys.");

        for (Map.Entry<Integer, String> entry : expected.entrySet()) {
            String actual = context.getString(entry.getKey());
            assertNotNull("String resource missing for " + entry.getKey(), actual);
            assertEquals(entry.getValue(), actual);
        }
    }
}
