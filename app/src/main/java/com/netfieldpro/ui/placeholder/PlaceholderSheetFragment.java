package com.netfieldpro.ui.placeholder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.netfieldpro.R;
import com.netfieldpro.databinding.FragmentPlaceholderBinding;
import com.netfieldpro.ui.NavArgs;

public class PlaceholderSheetFragment extends BottomSheetDialogFragment {

    private FragmentPlaceholderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String screenId = "S08";
        if (getArguments() != null && getArguments().containsKey(NavArgs.SCREEN_ID)) {
            String argId = getArguments().getString(NavArgs.SCREEN_ID);
            if (argId != null && !argId.isEmpty()) {
                screenId = argId;
            }
        }

        PlaceholderSpec spec = PlaceholderSpec.get(screenId);
        if (spec != null) {
            binding.textScreenId.setText(spec.getId());
            binding.textScreenTitle.setText(spec.getTitleRes());
            binding.textPlaceholderArrives.setText(getString(R.string.placeholder_arrives_format, spec.getOwner()));
            binding.textOwner.setText(getString(R.string.placeholder_owner_format, spec.getOwner()));
        }

        binding.cardNextScreens.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

