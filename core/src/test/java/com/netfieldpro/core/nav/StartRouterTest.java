package com.netfieldpro.core.nav;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StartRouterTest {

    @Test
    public void testViewOrSendDocument_allVariants() {
        assertEquals(StartRouter.StartDestination.S17,
                StartRouter.route(StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT, false, false));
        assertEquals(StartRouter.StartDestination.S17,
                StartRouter.route(StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT, false, true));
        assertEquals(StartRouter.StartDestination.S17,
                StartRouter.route(StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT, true, false));
        assertEquals(StartRouter.StartDestination.S17,
                StartRouter.route(StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT, true, true));
    }

    @Test
    public void testMain_allVariants() {
        assertEquals(StartRouter.StartDestination.S00,
                StartRouter.route(StartRouter.StartIntent.MAIN, false, false));
        assertEquals(StartRouter.StartDestination.S00,
                StartRouter.route(StartRouter.StartIntent.MAIN, false, true));
        assertEquals(StartRouter.StartDestination.S01,
                StartRouter.route(StartRouter.StartIntent.MAIN, true, false));
        assertEquals(StartRouter.StartDestination.S02,
                StartRouter.route(StartRouter.StartIntent.MAIN, true, true));
    }
}

