package com.netfieldpro.tokens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TokensTest {

    public static File findFile(String relativePath) {
        File f1 = new File(relativePath);
        if (f1.exists()) return f1;
        File f2 = new File("app", relativePath);
        if (f2.exists()) return f2;
        File f3 = new File("../app", relativePath);
        if (f3.exists()) return f3;
        throw new IllegalArgumentException("Cannot find " + relativePath + " from " + new File(".").getAbsolutePath());
    }

    public static Map<String, String> parseXmlMap(File file, String tagName, String nameAttr) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        NodeList list = doc.getElementsByTagName(tagName);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            String name = el.getAttribute(nameAttr);
            String val = el.getTextContent().trim();
            map.put(name, val);
        }
        return map;
    }

    @Test
    public void testLightColors() throws Exception {
        File f = findFile("src/main/res/values/colors.xml");
        Map<String, String> colors = parseXmlMap(f, "color", "name");

        assertEquals("#036434", colors.get("nf_primary"));
        assertEquals("#FFFFFF", colors.get("nf_on_primary"));
        assertEquals("#D6EFE0", colors.get("nf_primary_container"));
        assertEquals("#F6F8F7", colors.get("nf_background"));
        assertEquals("#FFFFFF", colors.get("nf_surface"));
        assertEquals("#0F1412", colors.get("nf_on_surface"));
        assertEquals("#4A5651", colors.get("nf_on_surface_variant"));
        assertEquals("#C9D1CD", colors.get("nf_outline"));
        assertEquals("#B3261E", colors.get("nf_error"));
        assertEquals("#8A5A00", colors.get("nf_warning"));
        assertEquals("#1F5FA8", colors.get("nf_info"));
    }

    @Test
    public void testDarkColors() throws Exception {
        File f = findFile("src/main/res/values-night/colors.xml");
        Map<String, String> colors = parseXmlMap(f, "color", "name");

        assertEquals("#6FD39A", colors.get("nf_primary"));
        assertEquals("#00391C", colors.get("nf_on_primary"));
        assertEquals("#03432A", colors.get("nf_primary_container"));
        assertEquals("#0F1412", colors.get("nf_background"));
        assertEquals("#171D1A", colors.get("nf_surface"));
        assertEquals("#E6ECE9", colors.get("nf_on_surface"));
        assertEquals("#A9B5AF", colors.get("nf_on_surface_variant"));
        assertEquals("#3C4742", colors.get("nf_outline"));
        assertEquals("#F2B8B5", colors.get("nf_error"));
        assertEquals("#F0C36D", colors.get("nf_warning"));
        assertEquals("#9CC4F0", colors.get("nf_info"));
    }

    @Test
    public void testDimens() throws Exception {
        File f = findFile("src/main/res/values/dimens.xml");
        Map<String, String> dimens = parseXmlMap(f, "dimen", "name");
        Map<String, String> integers = parseXmlMap(f, "integer", "name");

        assertEquals("4dp", dimens.get("space_050"));
        assertEquals("8dp", dimens.get("space_1"));
        assertEquals("16dp", dimens.get("space_2"));
        assertEquals("24dp", dimens.get("space_3"));
        assertEquals("32dp", dimens.get("space_4"));
        assertEquals("48dp", dimens.get("touch_min"));
        assertEquals("56dp", dimens.get("touch_primary"));
        assertEquals("72dp", dimens.get("bottom_bar"));
        assertEquals("12dp", dimens.get("radius_card"));
        assertEquals("8dp", dimens.get("radius_chip"));
        assertEquals("24dp", dimens.get("radius_sheet"));
        assertEquals("1dp", dimens.get("stroke"));

        assertEquals("150", integers.get("motion_short_ms"));
        assertEquals("200", integers.get("motion_medium_ms"));
    }

    @Test
    public void testTextStyles() throws Exception {
        File f = findFile("src/main/res/values/styles.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(f);
        NodeList styles = doc.getElementsByTagName("style");

        Map<String, Map<String, String>> styleItems = new HashMap<>();
        for (int i = 0; i < styles.getLength(); i++) {
            Element styleEl = (Element) styles.item(i);
            String styleName = styleEl.getAttribute("name");
            Map<String, String> items = new HashMap<>();
            NodeList itemList = styleEl.getElementsByTagName("item");
            for (int j = 0; j < itemList.getLength(); j++) {
                Element itemEl = (Element) itemList.item(j);
                items.put(itemEl.getAttribute("name"), itemEl.getTextContent().trim());
            }
            styleItems.put(styleName, items);
        }

        assertStyle(styleItems, "TextAppearance.NF.Display", "28sp", "36sp", null);
        assertStyle(styleItems, "TextAppearance.NF.Title", "20sp", "28sp", null);
        assertStyle(styleItems, "TextAppearance.NF.Body", "16sp", "24sp", null);
        assertStyle(styleItems, "TextAppearance.NF.Label", "14sp", "20sp", null);
        assertStyle(styleItems, "TextAppearance.NF.Caption", "12sp", "16sp", null);
        assertStyle(styleItems, "TextAppearance.NF.Mono", "14sp", "20sp", "monospace");
    }

    private void assertStyle(Map<String, Map<String, String>> styleItems, String name, String size, String lineHeight, String fontFamily) {
        Map<String, String> items = styleItems.get(name);
        assertNotNull("Style not found: " + name, items);
        assertEquals(size, items.get("android:textSize"));
        assertEquals(lineHeight, items.get("lineHeight"));
        if (fontFamily != null) {
            assertEquals(fontFamily, items.get("fontFamily"));
        }
    }
}

