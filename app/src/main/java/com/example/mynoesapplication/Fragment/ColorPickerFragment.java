package com.example.mynoesapplication.Fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mynoesapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ColorPickerFragment extends BottomSheetDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Make dialog window fully transparent
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    private OnColorSelectedListener listener;
    private final int[] colors = {
            Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
            Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.DKGRAY
    };
    private final String[] names = {
            "Đen", "Xanh dương", "Đỏ", "Xanh lá",
            "Tím", "Cyan", "Vàng", "Xám đậm"
    };

    public void setOnColorSelectedListener(OnColorSelectedListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog d = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        d.setCanceledOnTouchOutside(true);

        // Remove the default bottom-sheet background/shadow when it is shown
        d.setOnShowListener(dialog -> {
            BottomSheetDialog bd = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = bd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // remove background and padding so only inner card content remains visible
                bottomSheet.setBackground(null);
                bottomSheet.setPadding(0, 0, 0, 0);
            }
        });

        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_color_picker, container, false);

        TextView title = v.findViewById(R.id.txtTitle);
        RecyclerView rv = v.findViewById(R.id.recyclerColors);

        rv.setLayoutManager(new GridLayoutManager(getContext(), 1)); // 1 column list so each item shows full width
        rv.setAdapter(new ColorAdapter(colors, names, pos -> {
            if (listener != null) listener.onColorSelected(colors[pos]);
            dismiss();
        }));

        return v;
    }

    // Adapter
    private static class ColorAdapter extends RecyclerView.Adapter<ColorViewHolder> {
        private final int[] colors;
        private final String[] names;
        private final java.util.function.IntConsumer onClick;

        ColorAdapter(int[] colors, String[] names, java.util.function.IntConsumer onClick) {
            this.colors = colors;
            this.names = names;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color, parent, false);
            return new ColorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            holder.bind(colors[position], names[position], () -> onClick.accept(position));
        }

        @Override
        public int getItemCount() { return colors.length; }
    }

    private static class ColorViewHolder extends RecyclerView.ViewHolder {
        private final View colorBar;
        private final TextView name;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorBar = itemView.findViewById(R.id.viewColorBar);
            name = itemView.findViewById(R.id.txtColorName);
        }

        void bind(int color, String n, Runnable click) {
            name.setText(n);
            colorBar.setBackgroundColor(color);
            itemView.setOnClickListener(v -> click.run());
        }
    }
}
