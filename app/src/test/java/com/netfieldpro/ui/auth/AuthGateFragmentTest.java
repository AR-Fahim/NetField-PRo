package com.netfieldpro.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.netfieldpro.MainActivity;
import com.netfieldpro.NetFieldApp;
import com.netfieldpro.R;
import com.netfieldpro.auth.AuthCallback;
import com.netfieldpro.auth.AuthGatewayFactory;
import com.netfieldpro.auth.AuthSessionGateway;
import com.netfieldpro.core.auth.AuthOutcome;
import com.netfieldpro.core.session.Sm1State;
import com.netfieldpro.session.SessionRuntimeStore;
import com.netfieldpro.ui.settings.SettingsExtensionRegistry;
import com.netfieldpro.ui.settings.SettingsRow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AuthGateFragmentTest {

    private FakeAuthGateway fakeGateway;

    @Before
    public void setUp() {
        fakeGateway = new FakeAuthGateway();
        AuthGatewayFactory.setForTesting(fakeGateway);
        SettingsExtensionRegistry.getInstance().clear();
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionRuntimeStore.create(ctx).remove("active_session_1");
    }

    @After
    public void tearDown() {
        AuthGatewayFactory.resetForTesting();
        SettingsExtensionRegistry.getInstance().clear();
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionRuntimeStore.create(ctx).remove("active_session_1");
    }

    @Test
    public void testS00_1_viewsExist() {
        fakeGateway.activeSession = false;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.edit_email));
                assertNotNull(activity.findViewById(R.id.edit_password));
                assertNotNull(activity.findViewById(R.id.btn_sign_in));
                assertNotNull(activity.findViewById(R.id.btn_create_account));
                assertNotNull(activity.findViewById(R.id.btn_forgot_password));
                assertNotNull(activity.findViewById(R.id.btn_verify_only));
                assertNotNull(activity.findViewById(R.id.text_auth_status));
            });
        }
    }

    @Test
    public void testS00_2_emptyEmailShowsErrorAndNoGatewayCall() {
        fakeGateway.activeSession = false;
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText editEmail = activity.findViewById(R.id.edit_email);
                TextInputEditText editPassword = activity.findViewById(R.id.edit_password);
                Button btnSignIn = activity.findViewById(R.id.btn_sign_in);
                TextView textStatus = activity.findViewById(R.id.text_auth_status);

                editEmail.setText("");
                editPassword.setText("secret1");
                btnSignIn.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(activity.getString(R.string.auth_in_email_empty), textStatus.getText().toString());
                assertEquals(0, fakeGateway.signInCalls);
            });
        }
    }

    @Test
    public void testS00_3_validInputNavigatesAndClearsAuthSlot() {
        // Subtest A: onboarding not done -> routes to S01
        NetFieldApp.onboardingStore().reset();
        fakeGateway.activeSession = false;
        fakeGateway.outcomeToReturn = AuthOutcome.OK;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText editEmail = activity.findViewById(R.id.edit_email);
                TextInputEditText editPassword = activity.findViewById(R.id.edit_password);
                Button btnSignIn = activity.findViewById(R.id.btn_sign_in);

                editEmail.setText("tech@example.com");
                editPassword.setText("secret1");
                btnSignIn.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(R.id.nav_s01, activity.getNavController().getCurrentDestination().getId());
                assertNull(activity.getNavController().getPreviousBackStackEntry());
            });
        }

        // Subtest B: onboarding done -> routes to S02
        NetFieldApp.onboardingStore().markDone();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText editEmail = activity.findViewById(R.id.edit_email);
                TextInputEditText editPassword = activity.findViewById(R.id.edit_password);
                Button btnSignIn = activity.findViewById(R.id.btn_sign_in);

                editEmail.setText("tech@example.com");
                editPassword.setText("secret1");
                btnSignIn.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(R.id.nav_s02, activity.getNavController().getCurrentDestination().getId());
                assertNull(activity.getNavController().getPreviousBackStackEntry());
            });
        }
    }

    @Test
    public void testS00_4_noNetworkShowsErrorAndReenablesButtons() {
        fakeGateway.activeSession = false;
        fakeGateway.outcomeToReturn = AuthOutcome.NO_NETWORK;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText editEmail = activity.findViewById(R.id.edit_email);
                TextInputEditText editPassword = activity.findViewById(R.id.edit_password);
                Button btnSignIn = activity.findViewById(R.id.btn_sign_in);
                Button btnCreate = activity.findViewById(R.id.btn_create_account);
                Button btnForgot = activity.findViewById(R.id.btn_forgot_password);
                Button btnVerify = activity.findViewById(R.id.btn_verify_only);
                TextView textStatus = activity.findViewById(R.id.text_auth_status);

                editEmail.setText("tech@example.com");
                editPassword.setText("secret1");
                btnSignIn.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(activity.getString(R.string.auth_err_no_network), textStatus.getText().toString());
                assertTrue(btnSignIn.isEnabled());
                assertTrue(btnCreate.isEnabled());
                assertTrue(btnForgot.isEnabled());
                assertTrue(btnVerify.isEnabled());
            });
        }
    }

    @Test
    public void testS00_5_twoRapidTapsDispatchesSingleCall() {
        fakeGateway.activeSession = false;
        fakeGateway.holdCallback = true;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                TextInputEditText editEmail = activity.findViewById(R.id.edit_email);
                TextInputEditText editPassword = activity.findViewById(R.id.edit_password);
                Button btnSignIn = activity.findViewById(R.id.btn_sign_in);

                editEmail.setText("tech@example.com");
                editPassword.setText("secret1");
                btnSignIn.performClick();
                btnSignIn.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(1, fakeGateway.signInCalls);
            });
        }
    }

    @Test
    public void testS00_6_verifyOnlyNavigatesToS17AndBackReturnsToS00() {
        fakeGateway.activeSession = false;

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Button btnVerify = activity.findViewById(R.id.btn_verify_only);
                btnVerify.performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(R.id.nav_s17, activity.getNavController().getCurrentDestination().getId());

                activity.getNavController().popBackStack();
                ShadowLooper.idleMainLooper();

                assertEquals(R.id.authGateFragment, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    @Test
    public void testS00_7_logoutRowAndGating() {
        Context ctx = ApplicationProvider.getApplicationContext();
        SessionRuntimeStore store = SessionRuntimeStore.create(ctx);

        // 1. supportsAccounts() == false -> row hidden
        fakeGateway.supportsAccounts = false;
        fakeGateway.currentEmail = null;
        SettingsExtensionRegistry.getInstance().clear();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s19);
                ShadowLooper.idleMainLooper();

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                Fragment fragment = navHost.getChildFragmentManager().getPrimaryNavigationFragment();

                AuthSettingsExtension.setup(fragment, fakeGateway, store);

                SettingsRow row = findRow(SettingsExtensionRegistry.getInstance().rows(), AuthSettingsExtension.ROW_ID);
                assertNotNull(row);
                assertFalse(row.isVisible());
            });
        }

        // 2. supportsAccounts() == true -> row visible
        fakeGateway.supportsAccounts = true;
        fakeGateway.currentEmail = "tech@example.com";
        SettingsExtensionRegistry.getInstance().clear();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getNavController().navigate(R.id.nav_s19);
                ShadowLooper.idleMainLooper();

                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                Fragment fragment = navHost.getChildFragmentManager().getPrimaryNavigationFragment();

                AuthSettingsExtension.setup(fragment, fakeGateway, store);

                SettingsRow row = findRow(SettingsExtensionRegistry.getInstance().rows(), AuthSettingsExtension.ROW_ID);
                assertNotNull(row);
                assertTrue(row.isVisible());
                assertEquals("Log Out (Alpha)", row.getTitle());
                assertEquals("tech@example.com", row.getSubtitle());

                // 3. Blocked case -> active survey -> snackbar & signOut not called
                store.putState("active_session_1", Sm1State.ACTIVE);
                row.getOnClick().run();
                ShadowLooper.idleMainLooper();
                assertEquals(0, fakeGateway.signOutCalls);
                assertNull(ShadowAlertDialog.getLatestDialog());

                // 4. Allowed case -> ended survey -> confirm dialog -> signOut called once & navigate to auth slot
                store.putState("active_session_1", Sm1State.ENDED);
                row.getOnClick().run();
                ShadowLooper.idleMainLooper();

                AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
                assertNotNull(dialog);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                ShadowLooper.idleMainLooper();

                assertEquals(1, fakeGateway.signOutCalls);
                assertEquals(R.id.authGateFragment, activity.getNavController().getCurrentDestination().getId());
            });
        }
    }

    private static SettingsRow findRow(List<SettingsRow> rows, String id) {
        if (rows == null) return null;
        for (SettingsRow r : rows) {
            if (id.equals(r.getId())) {
                return r;
            }
        }
        return null;
    }

    static class FakeAuthGateway implements AuthSessionGateway {
        boolean activeSession = false;
        boolean supportsAccounts = false;
        String currentEmail = null;
        AuthOutcome outcomeToReturn = AuthOutcome.OK;
        boolean holdCallback = false;
        Callback pendingCallback;

        int signInCalls = 0;
        int signUpCalls = 0;
        int resetCalls = 0;
        int signOutCalls = 0;

        @Override
        public boolean hasActiveSession() {
            return activeSession;
        }

        @Override
        public String getCurrentUserEmail() {
            return currentEmail;
        }

        @Override
        public boolean supportsAccounts() {
            return supportsAccounts;
        }

        @Override
        public String currentEmail() {
            return currentEmail;
        }

        @Override
        public void signIn(String email, String password, Callback cb) {
            signInCalls++;
            if (holdCallback) {
                pendingCallback = cb;
            } else if (cb != null) {
                cb.onResult(outcomeToReturn);
            }
        }

        @Override
        public void signUp(String email, String password, Callback cb) {
            signUpCalls++;
            if (holdCallback) {
                pendingCallback = cb;
            } else if (cb != null) {
                cb.onResult(outcomeToReturn);
            }
        }

        @Override
        public void sendPasswordReset(String email, Callback cb) {
            resetCalls++;
            if (holdCallback) {
                pendingCallback = cb;
            } else if (cb != null) {
                cb.onResult(outcomeToReturn);
            }
        }

        @Override
        public void signOut() {
            signOutCalls++;
        }

        @Override
        public void signIn(String email, String pw, AuthCallback cb) {}

        @Override
        public void register(String email, String pw, AuthCallback cb) {}

        @Override
        public void sendPasswordReset(String email, AuthCallback cb) {}
    }
}
