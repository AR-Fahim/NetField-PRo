package com.netfieldpro;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.netfieldpro.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets statusInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            binding.toolbarContainer.setPadding(0, statusInsets.top, 0, 0);
            binding.navHostFragment.setPadding(0, 0, 0, navInsets.bottom);
            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            appBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_s02).build();
            NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);

            binding.toolbar.inflateMenu(R.menu.menu_main);
            android.view.MenuItem badgeItem = binding.toolbar.getMenu().findItem(R.id.action_tier_badge);
            View actionView = badgeItem != null ? badgeItem.getActionView() : null;
            com.netfieldpro.ui.TierBadgeView badgeView = null;
            if (actionView instanceof com.netfieldpro.ui.TierBadgeView) {
                badgeView = (com.netfieldpro.ui.TierBadgeView) actionView;
            } else if (actionView != null) {
                badgeView = actionView.findViewById(R.id.tier_badge_view);
            }

            if (badgeView != null) {
                badgeView.setOnClickListener(v -> {
                    if (navController != null) {
                        navController.navigate(R.id.action_global_paywall);
                    }
                });
            }
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_tier_badge) {
                    if (navController != null) {
                        navController.navigate(R.id.action_global_paywall);
                    }
                    return true;
                }
                return false;
            });

            com.netfieldpro.ui.TierBadgeView finalBadge = badgeView;
            NetFieldApp.entitlements().tier().observe(this, tier -> {
                if (finalBadge != null && tier != null) {
                    finalBadge.setTier(tier);
                }
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                boolean isS02 = destination.getId() == R.id.nav_s02;
                if (badgeItem != null) {
                    badgeItem.setVisible(isS02);
                }
                if (finalBadge != null) {
                    finalBadge.setVisibility(isS02 ? View.VISIBLE : View.GONE);
                }
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams) binding.chipDev.getLayoutParams();
                if (lp != null) {
                    lp.setMarginEnd(isS02
                            ? (int) (72 * getResources().getDisplayMetrics().density)
                            : getResources().getDimensionPixelSize(R.dimen.space_2));
                    binding.chipDev.setLayoutParams(lp);
                }
            });

            if (savedInstanceState == null) {
                android.content.Intent intent = getIntent();
                com.netfieldpro.core.nav.StartRouter.StartIntent startIntent =
                        com.netfieldpro.core.nav.StartRouter.StartIntent.MAIN;
                if (intent != null) {
                    String action = intent.getAction();
                    if (android.content.Intent.ACTION_VIEW.equals(action) || android.content.Intent.ACTION_SEND.equals(action)) {
                        startIntent = com.netfieldpro.core.nav.StartRouter.StartIntent.VIEW_OR_SEND_DOCUMENT;
                    }
                }
                com.netfieldpro.auth.AuthSessionGateway gateway = com.netfieldpro.auth.AuthGatewayFactory.create();
                boolean hasSession = gateway.hasActiveSession();
                boolean firstLaunchDone = NetFieldApp.onboardingStore().isDone();
                com.netfieldpro.core.nav.StartRouter.StartDestination dest =
                        com.netfieldpro.core.nav.StartRouter.route(startIntent, hasSession, firstLaunchDone);

                int targetDestId = R.id.nav_s02;
                if (dest == com.netfieldpro.core.nav.StartRouter.StartDestination.S00) {
                    targetDestId = R.id.nav_auth_slot;
                } else if (dest == com.netfieldpro.core.nav.StartRouter.StartDestination.S01) {
                    targetDestId = R.id.nav_s01;
                } else if (dest == com.netfieldpro.core.nav.StartRouter.StartDestination.S17) {
                    targetDestId = R.id.nav_s17;
                }

                if (targetDestId != R.id.nav_s02) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_s02, true)
                            .build();
                    navController.navigate(targetDestId, null, navOptions);
                }
            }
        }

        binding.chipDev.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        binding.chipDev.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.nav_debug_menu);
            }
        });

        com.netfieldpro.session.SurveyRuntime.get().keepScreenOn().observe(this, keepOn -> {
            if (Boolean.TRUE.equals(keepOn)) {
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    public ActivityMainBinding getBinding() {
        return binding;
    }

    public NavController getNavController() {
        return navController;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && appBarConfiguration != null) {
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
