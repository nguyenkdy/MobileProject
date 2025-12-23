// java
package com.example.mynoesapplication.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
    private ScrollView scrollSummary;

    private boolean isClosing = false;

    // Handler cho loading dots và animation
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable dotsRunnable;
    private int dotCount = 0;
    private boolean isLoading = false;

    // Token to prevent stale runnables from updating UI after a new load or after showSummary()
    private int loadingToken = -1;

    public static NoteSummaryFragment findOrCreate(FragmentManager fm) {
        NoteSummaryFragment ex = (NoteSummaryFragment) fm.findFragmentByTag(TAG);
        if (ex != null) return ex;

        NoteSummaryFragment frag = new NoteSummaryFragment();
        fm.beginTransaction()
                .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                )
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
        btnClose = v.findViewById(R.id.btnClose);
        scrollSummary = v.findViewById(R.id.scrollSummary);

        // Khởi tạo trạng thái view trước khi animate
        if (scrim != null) scrim.setAlpha(0f);
        if (panel != null) {
            panel.setAlpha(0f);
            panel.setScaleX(0.98f);
            panel.setScaleY(0.98f);
        }
        if (progress != null) progress.setVisibility(View.GONE);

        // Close actions
        btnClose.setOnClickListener(view -> closeWithAnimation());
        scrim.setOnClickListener(view -> closeWithAnimation());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        panel.post(this::startEnterAnimation);
    }

    private void startEnterAnimation() {
        if (panel == null || scrim == null) return;

        scrim.animate()
                .alpha(1f)
                .setDuration(220)
                .start();

        panel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(AnimationUtils.loadInterpolator(requireContext(),
                        android.R.interpolator.decelerate_cubic))
                .start();
    }

    private void closeWithAnimation() {
        if (isClosing) return;
        isClosing = true;

        if (btnClose != null) btnClose.setEnabled(false);
        if (panel != null) panel.setEnabled(false);
        if (scrim != null) scrim.setEnabled(false);

        if (scrim != null) {
            scrim.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .start();
        }

        if (panel != null) {
            panel.animate()
                    .alpha(0f)
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(160)
                    .withEndAction(() -> {
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                requireActivity().getSupportFragmentManager()
                                        .popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                            });
                        } else {
                            isClosing = false;
                        }
                    })
                    .start();
        } else {
            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    requireActivity().getSupportFragmentManager()
                            .popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                });
            } else isClosing = false;
        }
    }

    public void showLoading() {
        if (getActivity() == null) return;

        requireActivity().runOnUiThread(() -> {
            // increment token for a new loading session and remove any previous callbacks
            loadingToken++;
            final int token = loadingToken;

            if (dotsRunnable != null) mainHandler.removeCallbacks(dotsRunnable);
            isLoading = true;
            dotCount = 0;

            // 1. Spinner animation (visible and on top)
            if (progress != null) {
                progress.clearAnimation();
                progress.setVisibility(View.VISIBLE);
                progress.bringToFront();
                progress.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.loading_rotate));
            }

            // 2. Text loading with dots — the runnable checks the token to avoid stale updates
            if (txtSummary != null) {
                txtSummary.setAlpha(1f);
                final String base = "Đang tạo tóm tắt";
                txtSummary.setText(base);

                dotsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // stop if loading ended or token mismatched (newer session started or showSummary called)
                        if (!isLoading || txtSummary == null || token != loadingToken) return;

                        dotCount = (dotCount + 1) % 4;
                        StringBuilder dots = new StringBuilder();
                        for (int i = 0; i < dotCount; i++) dots.append('.');
                        txtSummary.setText(base + dots);

                        if (scrollSummary != null) scrollSummary.post(() -> scrollSummary.fullScroll(View.FOCUS_UP));

                        mainHandler.postDelayed(this, 100);
                    }
                };

                // start after 100ms to avoid immediate overwrite UI thrash
                mainHandler.postDelayed(dotsRunnable, 100);
            }
        });
    }

    public void showSummary(String summary) {
        if (getActivity() == null) return;

        requireActivity().runOnUiThread(() -> {
            // stop spinner animation and hide
            if (progress != null) {
                progress.clearAnimation();
                progress.setVisibility(View.GONE);
            }

            // stop dot animation and invalidate token so runnables stop
            isLoading = false;
            loadingToken = -1;
            if (dotsRunnable != null) {
                mainHandler.removeCallbacks(dotsRunnable);
                dotsRunnable = null;
            }

            // show final summary with fade
            if (txtSummary != null) {
                txtSummary.setAlpha(0f);
                txtSummary.setText(summary);
                txtSummary.animate().alpha(1f).setDuration(180).start();
            }
        });
    }

    @Override
    public void onDestroyView() {
        // cleanup to avoid handler leaks and stop animations
        isLoading = false;
        loadingToken = -1;
        if (dotsRunnable != null) mainHandler.removeCallbacks(dotsRunnable);
        dotsRunnable = null;
        if (progress != null) {
            progress.clearAnimation();
            progress.setVisibility(View.GONE);
        }
        super.onDestroyView();
    }
}
