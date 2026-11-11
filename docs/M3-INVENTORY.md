# M3-0 Inventory

## 1. S01 Fragment Class and Layout File

- **Fragment Class:** `com.netfieldpro.ui.placeholder.PlaceholderFragment`
  - Defined in `app/src/main/res/navigation/nav_main.xml` under destination id `@+id/nav_s01`:
    ```xml
    <fragment
        android:id="@+id/nav_s01"
        android:name="com.netfieldpro.ui.placeholder.PlaceholderFragment"
        android:label="@string/screen_s01_title">
        <argument
            android:name="screenId"
            app:argType="string"
            android:defaultValue="S01" />
        <action
            android:id="@+id/action_s01_to_s02"
            app:destination="@id/nav_s02" />
        <action
            android:id="@+id/action_s01_to_s17"
            app:destination="@id/nav_s17" />
    </fragment>
    ```
- **Layout File:** `app/src/main/res/layout/fragment_placeholder.xml` (existing layout file used by `PlaceholderFragment`; M3-5 introduces `app/src/main/res/layout/fragment_onboarding.xml` for S01)

---

## 2. StartRouter Method Signatures and MainActivity Application

### StartRouter Signatures
From `core/src/main/java/com/netfieldpro/core/nav/StartRouter.java`:
```java
package com.netfieldpro.core.nav;

public final class StartRouter {

    private StartRouter() {}

    public enum StartIntent {
        MAIN,
        VIEW_OR_SEND_DOCUMENT
    }

    public enum StartDestination {
        S00,
        S01,
        S02,
        S17
    }

    public static StartDestination route(StartIntent intent, boolean hasActiveSession, boolean firstLaunchDone) {
        if (intent == StartIntent.VIEW_OR_SEND_DOCUMENT) {
            return StartDestination.S17;
        }
        if (!hasActiveSession) {
            return StartDestination.S00;
        }
        if (!firstLaunchDone) {
            return StartDestination.S01;
        }
        return StartDestination.S02;
    }
}
```

### MainActivity Application of StartRouter Result
From `app/src/main/java/com/netfieldpro/MainActivity.java` (lines 99–128):
```java
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
                boolean firstLaunchDone = true;
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
                    navController.navigate(targetDestId);
                }
            }
```

---

## 3. Nav Edges Touching S01 and S17

From `app/src/main/res/navigation/nav_main.xml`:

### Edges Touching S01
- **Outgoing Edge to S02:**
  - Action ID: `@+id/action_s01_to_s02`
  - Destination: `@id/nav_s02`
  - Existing `popUpTo`: None
- **Outgoing Edge to S17:**
  - Action ID: `@+id/action_s01_to_s17`
  - Destination: `@id/nav_s17`
  - Existing `popUpTo`: None
- **Incoming Edges to S01:**
  - None defined as explicit `<action>` elements (entered via start routing from `MainActivity`).

### Edges Touching S17
- **Incoming Edge from S01:**
  - Action ID: `@+id/action_s01_to_s17`
  - Destination: `@id/nav_s17`
  - Existing `popUpTo`: None
- **Incoming Edge from S02:**
  - Action ID: `@+id/action_s02_to_s17`
  - Destination: `@id/nav_s17`
  - Existing `popUpTo`: None
- **Outgoing Edges from S17:**
  - None (S17 is a terminal destination in `nav_main.xml`).

---

## 4. Manifest `uses-permission` Set

From `app/src/main/AndroidManifest.xml`:
- Current `uses-permission` set: `[]` (Empty, 0 permissions).

---

## 5. Chip Style Used by `StyleGuideFragment`

From `app/src/main/res/values/styles.xml` and `app/src/main/res/layout/fragment_style_guide.xml`:
- Parent style: `Widget.Material3.Chip.Assist`
- Styles defined:
  - `Widget.NF.Chip.Verdict.Pass`
  - `Widget.NF.Chip.Verdict.Fail`
  - `Widget.NF.Chip.Verdict.Incomplete`
  - `Widget.NF.Chip.Verdict.NotEligible`
- Style attributes:
  ```xml
  <style name="Widget.NF.Chip.Verdict.Pass" parent="Widget.Material3.Chip.Assist">
      <item name="chipBackgroundColor">@android:color/transparent</item>
      <item name="chipStrokeColor">@color/nf_primary</item>
      <item name="chipStrokeWidth">1dp</item>
      <item name="android:textColor">@color/nf_primary</item>
      <item name="chipCornerRadius">@dimen/radius_chip</item>
  </style>
  ```
  Outline-only, transparent background, stroke width `1dp`, corner radius `@dimen/radius_chip` (8dp), text and stroke colored per state tokens.

---

## 6. How `DebugMenuFragment` Adds Rows

From `app/src/main/java/com/netfieldpro/ui/debug/DebugMenuFragment.java` and `app/src/main/res/layout/fragment_debug_menu.xml`:
1. **Layout Rows (Static buttons in XML):**
   - Declared as `com.google.android.material.button.MaterialButton` inside the vertical `LinearLayout`.
   - Style: `@style/Widget.Material3.Button.OutlinedButton`.
   - Layout parameters: `layout_width="match_parent"`, `layout_height="@dimen/touch_primary"` (48dp), `layout_marginTop="@dimen/space_1"` (8dp) or `@dimen/space_2` (16dp).
   - In `onViewCreated()`, `setOnClickListener` triggers navigation or actions.
2. **Dynamic Rows (`setupScreensList()`):**
   - Programmatically instantiates `MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle)`.
   - Sets `LinearLayout.LayoutParams(MATCH_PARENT, touch_primary)` with `topMargin = space_1`.
   - Formats label using `R.string.edge_button_format` (`%1$s: %2$s`).
   - Sets click listener with `NavHostFragment.findNavController(this).navigate(destinationId)`.
   - Adds the button to `binding.layoutScreensList`.

