package com.netfieldpro.placeholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.netfieldpro.tokens.TokensTest;
import com.netfieldpro.ui.placeholder.PlaceholderSpec;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlaceholderSpecTest {

    @Test
    public void testExactly21Specs() {
        List<PlaceholderSpec> all = PlaceholderSpec.all();
        assertEquals(21, all.size());

        for (int i = 1; i <= 21; i++) {
            String id = String.format("S%02d", i);
            assertNotNull("Missing spec " + id, PlaceholderSpec.get(id));
            assertEquals(id, PlaceholderSpec.get(id).getId());
        }
    }

    @Test
    public void testEveryOutgoingIdExists() {
        for (PlaceholderSpec spec : PlaceholderSpec.all()) {
            for (String outId : spec.getOutgoingScreenIds()) {
                assertNotNull("Spec " + spec.getId() + " has nonexistent outgoing " + outId,
                        PlaceholderSpec.get(outId));
            }
        }
    }

    @Test
    public void testEveryEdgeExistsInNavGraph() throws Exception {
        File navFile = TokensTest.findFile("src/main/res/navigation/nav_main.xml");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(navFile);

        NodeList fragments = doc.getElementsByTagName("fragment");
        NodeList dialogs = doc.getElementsByTagName("dialog");

        Map<String, Element> destMap = new HashMap<>();
        for (int i = 0; i < fragments.getLength(); i++) {
            Element el = (Element) fragments.item(i);
            destMap.put(el.getAttribute("android:id"), el);
        }
        for (int i = 0; i < dialogs.getLength(); i++) {
            Element el = (Element) dialogs.item(i);
            destMap.put(el.getAttribute("android:id"), el);
        }

        for (PlaceholderSpec spec : PlaceholderSpec.all()) {
            String destId = "@+id/nav_" + spec.getId().toLowerCase();
            Element destEl = destMap.get(destId);
            assertNotNull("Destination missing for " + spec.getId(), destEl);

            NodeList actions = destEl.getElementsByTagName("action");
            Map<String, String> actionDestMap = new HashMap<>();
            for (int a = 0; a < actions.getLength(); a++) {
                Element act = (Element) actions.item(a);
                actionDestMap.put(act.getAttribute("android:id"), act.getAttribute("app:destination"));
            }

            for (String outId : spec.getOutgoingScreenIds()) {
                String expectedActionId = "@+id/action_" + spec.getId().toLowerCase() + "_to_" + outId.toLowerCase();
                String targetDestId = "@id/nav_" + outId.toLowerCase();

                assertTrue("Edge from " + spec.getId() + " to " + outId + " missing action in nav_main.xml",
                        actionDestMap.containsKey(expectedActionId));
                assertEquals(targetDestId, actionDestMap.get(expectedActionId));
            }
        }
    }
}

