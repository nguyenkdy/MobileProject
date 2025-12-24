package com.example.mynoesapplication.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mynoesapplication.R;

public class NoteSummaryFragment extends Fragment {
    public static final String TAG = "NoteSummaryFragment";
    private TextView txtSummary;
    private ProgressBar progress;
    private View panel;
    private View scrim;
    private ImageButton btnClose;

    public static NoteSummaryFragment findOrCreate(FragmentManager fm) {
        NoteSummaryFragment ex = (NoteSummaryFragment) fm.findFragmentByTag(TAG);
        if (ex != null) return ex;
        NoteSummaryFragment frag = new NoteSummaryFragment();
        fm.beginTransaction()
                .add(android.R.id.content, frag, TAG)
                .addToBackStack(TAG)
                .commitAllowingStateLoss();
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.note_summary, container, false);
        txtSummary = v.findViewById(R.id.txtSummary);
        progress = v.findViewById(R.id.progressSummary);
        panel = v.findViewById(R.id.panel);
        scrim = v.findViewById(R.id.scrim);
        // start panel off-screen to the right
        panel.setTranslationX(1000f);

        // Close actions (button and scrim tap)
        scrim.setOnClickListener(view -> closeWithAnimation());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // animate slide-in
        panel.post(() -> {
            panel.setTranslationX(panel.getWidth());
            panel.animate()
                    .translationX(0)
                    .setDuration(220)
                    .start();
        });
    }

    private void closeWithAnimation() {
        panel.animate()
                .translationX(panel.getWidth())
                .setDuration(180)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Pop backstack entry to cleanly remove fragment
                        if (getActivity() != null) {
                            FragmentManager fm = requireActivity().getSupportFragmentManager();
                            fm.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }
                })
                .start();

        // fade out scrim
        scrim.animate().alpha(0f).setDuration(160).start();
    }

    public void showLoading() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (txtSummary != null) txtSummary.setText("");
        });
    }

    public void showSummary(String summary) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            if (progress != null) progress.setVisibility(View.GONE);
            if (txtSummary != null) txtSummary.setText(summary);
        });
    }
}
