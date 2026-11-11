package com.netfieldpro.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.netfieldpro.R;
import com.netfieldpro.core.entitlement.Tier;

public class TierBadgeView extends AppCompatTextView {

    private Tier currentTier = Tier.FREE;

    public TierBadgeView(@NonNull Context context) {
        super(context);
        init();
    }

    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setTextAppearance(R.style.TextAppearance_NF_Label);
        setGravity(Gravity.CENTER);
        int minH = getResources().getDimensionPixelSize(R.dimen.touch_min);
        setMinHeight(minH);
        setMinimumHeight(minH);
        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (12 * density);
        int padV = (int) (4 * density);
        setPadding(padH, padV, padH, padV);
        setTier(Tier.FREE);
    }

    public void setTier(Tier tier) {
        if (tier == null) {
            tier = Tier.FREE;
        }
        this.currentTier = tier;

        int nameResId;
        switch (tier) {
            case PRO:
                nameResId = R.string.tier_pro;
                break;
            case ENTERPRISE:
                nameResId = R.string.tier_enterprise;
                break;
            case FREE:
            default:
                nameResId = R.string.tier_free;
                break;
        }
        String tierName = getContext().getString(nameResId);
        setText(tierName);
        setContentDescription(getContext().getString(R.string.tier_badge_desc, tierName));

        int colorRes = (tier == Tier.FREE) ? R.color.nf_on_surface_variant : R.color.nf_primary;
        int color = ContextCompat.getColor(getContext(), colorRes);
        setTextColor(color);

        float density = getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(8 * density);
        background.setColor(Color.TRANSPARENT);
        background.setStroke((int) Math.max(1, 1 * density), color);
        setBackground(background);
    }

    public Tier getTier() {
        return currentTier;
    }
}

