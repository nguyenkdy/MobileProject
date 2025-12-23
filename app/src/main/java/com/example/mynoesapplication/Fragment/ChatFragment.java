package com.example.mynoesapplication.Fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynoesapplication.Adapter.ChatAdapter;
import com.example.mynoesapplication.Data.AiRequest;
import com.example.mynoesapplication.Data.AiResponse;
import com.example.mynoesapplication.Data.*;
import com.example.mynoesapplication.R;
import com.example.mynoesapplication.RetrofitClient.AiApiService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {
    private RecyclerView rv;
    private EditText edt;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ImageButton btnModeChat, btnModeNote;
    private ImageButton btnSend;
    private enum Mode { CHAT, NOTE }
    private Mode mode = Mode.CHAT;

    private FirebaseFirestore db;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        rv = v.findViewById(R.id.rvChat);
        edt = v.findViewById(R.id.edtMessage);
        btnModeChat = v.findViewById(R.id.btnModeChat);
        btnModeNote = v.findViewById(R.id.btnModeNote);
        btnSend = v.findViewById(R.id.btnSend);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);

        setMode(Mode.CHAT);

        btnModeChat.setOnClickListener(x -> setMode(Mode.CHAT));
        btnModeNote.setOnClickListener(x -> setMode(Mode.NOTE));

        btnSend.setOnClickListener(x -> {
            String txt = edt.getText().toString().trim();
            if (TextUtils.isEmpty(txt)) return;
            addMessage(txt, true);
            edt.setText("");
            if (mode == Mode.NOTE) {
                createNoteFromText(txt);
            } else {
                callAiForReply(txt);
            }
        });

        v.findViewById(R.id.btnCloseChat).setOnClickListener(view -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction().remove(ChatFragment.this).commitAllowingStateLoss();
            }
            if (getActivity() != null) {
                View host = getActivity().findViewById(R.id.chat_container);
                if (host != null) host.setVisibility(View.GONE);
            }
        });

        return v;
    }

    private void setMode(Mode m) {
        mode = m;
        btnModeChat.setAlpha(m == Mode.CHAT ? 1f : 0.6f);
        btnModeNote.setAlpha(m == Mode.NOTE ? 1f : 0.6f);
        edt.setHint(m == Mode.NOTE ? "Enter note content..." : "Nhập tin nhắn...");
    }

    private void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(String.valueOf(System.currentTimeMillis()), text, isUser, System.currentTimeMillis()));
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);
    }

    private void createNoteFromText(String content) {
        if (uid == null) {
            addMessage("Bạn cần đăng nhập để tạo ghi chú.", false);
            return;
        }

        String title = content.length() > 40 ? content.substring(0, 40) : content;
        Map<String, Object> doc = new HashMap<>();
        doc.put("title", title);
        doc.put("content", content);
        doc.put("createdAt", Timestamp.now());
        doc.put("updatedAt", Timestamp.now());
        doc.put("deleted", false);

        db.collection("users")
                .document(uid)
                .collection("notes")
                .add(doc)
                .addOnSuccessListener(ref -> addMessage("Đã tạo ghi chú.", false))
                .addOnFailureListener(e -> addMessage("Tạo ghi chú thất bại: " + e.getMessage(), false));
    }

    private void callAiForReply(String prompt) {
        AiRequest req = new AiRequest(prompt +", answer in Vietnamese, nicely and shortly.");
        AiApiService.getApi().summarize(req)
                .enqueue(new Callback<AiResponse>() {
                    @Override
                    public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String aiText = response.body().getSummary();
                            if (aiText == null) aiText = "AI returned empty.";
                            final String out = aiText;
                            if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(out, false));
                        } else {
                            String err = "AI error";
                            try {
                                if (response.errorBody() != null) err = response.errorBody().string();
                            } catch (Exception ignored) {}
                            final String out = "Failed: " + err;
                            if (getActivity() != null) getActivity().runOnUiThread(() -> addMessage(out, false));
                        }
                    }

                    @Override
                    public void onFailure(Call<AiResponse> call, Throwable t) {
                        if (getActivity() != null) getActivity().runOnUiThread(() ->
                                addMessage("AI call failed: " + t.getMessage(), false));
                    }
                });
    }
}
