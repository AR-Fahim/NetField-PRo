package com.netfieldpro.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

public class SettingsExtensionRegistryTest {

    private SettingsExtensionRegistry registry;

    @Before
    public void setUp() {
        registry = SettingsExtensionRegistry.getInstance();
        registry.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateIdRejected() {
        registry.register(new SettingsRow("row1", "Title 1", "Sub 1", null, true));
        registry.register(new SettingsRow("row1", "Duplicate Title", "Sub 2", null, true));
    }

    @Test
    public void testRegistrationOrderPreserved() {
        registry.register(new SettingsRow("r1", "Row 1", null, null, true));
        registry.register(new SettingsRow("r2", "Row 2", null, null, false));
        registry.register(new SettingsRow("r3", "Row 3", null, null, true));

        List<SettingsRow> rows = registry.rows();
        assertEquals(3, rows.size());
        assertEquals("r1", rows.get(0).getId());
        assertEquals("r2", rows.get(1).getId());
        assertEquals("r3", rows.get(2).getId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRowsUnmodifiable() {
        registry.register(new SettingsRow("r1", "Row 1", null, null, true));
        registry.rows().add(new SettingsRow("r2", "Row 2", null, null, true));
    }
}

