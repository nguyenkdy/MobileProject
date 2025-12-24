// language: java
package com.example.mynoesapplication.Fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ColorPickerFragment extends BottomSheetDialogFragment {

    private static final String ARG_IS_MARKER = "arg_is_marker";
    private static final String ARG_SIZE = "arg_size";

    public interface OnColorSizeSelectedListener {
        void onColorSizeSelected(int color, int size);
    }

    private OnColorSizeSelectedListener listener;
    private boolean isMarker = false;
    private int selectedSize = 6;

    private final int[] colors = {
            Color.BLACK, Color.BLUE, Color.RED, Color.GREEN,
            Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.DKGRAY
    };
    private final String[] names = {
            "Đen", "Xanh dương", "Đỏ", "Xanh lá",
            "Tím", "Cyan", "Vàng", "Xám đậm"
    };

    public static ColorPickerFragment newInstance(boolean isMarker, int currentSize) {
        ColorPickerFragment f = new ColorPickerFragment();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_MARKER, isMarker);
        b.putInt(ARG_SIZE, currentSize);
        f.setArguments(b);
        return f;
    }

    public void setOnColorSizeSelectedListener(OnColorSizeSelectedListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        Bundle args = getArguments();
        if (args != null) {
            isMarker = args.getBoolean(ARG_IS_MARKER, false);
            selectedSize = args.getInt(ARG_SIZE, isMarker ? 20 : 6);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog d = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        d.setCanceledOnTouchOutside(true);

        d.setOnShowListener(dialog -> {
            BottomSheetDialog bd = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = bd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(null);
                bottomSheet.setPadding(0, 0, 0, 0);
            }
        });

        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_color_picker, container, false);

        TextView txtSizeLabel = v.findViewById(R.id.txtSizeLabel);
        SeekBar seekSize = v.findViewById(R.id.seekSize);
        RecyclerView rv = v.findViewById(R.id.recyclerColors);

        final int min = isMarker ? 10 : 2;
        final int max = isMarker ? 40 : 12;

        if (txtSizeLabel != null) {
            txtSizeLabel.setText((isMarker ? "Độ to Marker: " : "Độ to Pen: ") + selectedSize);
        }

        if (seekSize != null) {
            seekSize.setMax(max - min);
            seekSize.setProgress(Math.max(0, selectedSize - min));
            seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    selectedSize = min + p;
                    if (txtSizeLabel != null) {
                        txtSizeLabel.setText((isMarker ? "Độ to Marker: " : "Độ to Pen: ") + selectedSize);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        rv.setLayoutManager(new GridLayoutManager(getContext(), 1));
        rv.setAdapter(new ColorAdapter(colors, names, pos -> {
            if (listener != null) listener.onColorSizeSelected(colors[pos], selectedSize);
            dismiss();
        }));

        return v;
    }

    // Adapter classes
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
