package com.example.mynoesapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Data.*;
import com.example.mynoesapplication.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> items;

    public ChatAdapter(List<ChatMessage> items) {
        this.items = items;
    }

    @Override public int getItemCount() { return items == null ? 0 : items.size(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatMessage m = items.get(position);

        if (m.isUser) {
            h.rowUser.setVisibility(View.VISIBLE);
            h.rowBot.setVisibility(View.GONE);
            h.txtUser.setText(m.text);
        } else {
            h.rowUser.setVisibility(View.GONE);
            h.rowBot.setVisibility(View.VISIBLE);
            h.txtBot.setText(m.text);
            // Optionally change bot icon: h.imgBot.setImageResource(...)
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        View rowBot, rowUser;
        ImageView imgBot;
        TextView txtBot, txtUser;

        VH(@NonNull View v) {
            super(v);
            rowBot = v.findViewById(R.id.rowBot);
            rowUser = v.findViewById(R.id.rowUser);
            imgBot = v.findViewById(R.id.imgBot);
            txtBot = v.findViewById(R.id.txtBot);
            txtUser = v.findViewById(R.id.txtUser);
        }
    }
}
