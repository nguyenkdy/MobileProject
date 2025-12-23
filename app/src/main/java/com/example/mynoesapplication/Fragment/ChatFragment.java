package com.example.mynoesapplication.Fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.mynoesapplication.Data.ChatMessage;
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

    // ================= UI =================
    private RecyclerView rv;
    private EditText edt;
    private ImageButton btnModeChat, btnModeNote, btnSend;
    private ImageButton btnExpandChat, btnCloseChat;

    // ================= STATE =================
    private boolean isExpanded = false;
    private View rootView;

    // ================= DATA =================
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private enum Mode { CHAT, NOTE }
    private Mode mode = Mode.CHAT;

    // ================= FIREBASE =================
    private FirebaseFirestore db;
    private String uid;

    // =========================================================
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        rootView = v;

        // ===== BIND UI =====
        rv = v.findViewById(R.id.rvChat);
        edt = v.findViewById(R.id.edtMessage);
        btnModeChat = v.findViewById(R.id.btnModeChat);
        btnModeNote = v.findViewById(R.id.btnModeNote);
        btnSend = v.findViewById(R.id.btnSend);
        btnExpandChat = v.findViewById(R.id.btnExpandChat);
        btnCloseChat = v.findViewById(R.id.btnCloseChat);

        // ===== FIREBASE =====
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // ===== RECYCLER =====
        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);

        // ===== DEFAULT MODE =====
        setMode(Mode.CHAT);

        // ===== EVENTS =====
        btnModeChat.setOnClickListener(v1 -> setMode(Mode.CHAT));
        btnModeNote.setOnClickListener(v1 -> setMode(Mode.NOTE));

        btnSend.setOnClickListener(v1 -> {
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

        // ===== PHÓNG TO / THU NHỎ =====
        btnExpandChat.setOnClickListener(v1 -> toggleExpand());

        // ===== ĐÓNG CHAT =====
        btnCloseChat.setOnClickListener(v1 -> closeChat());

        return v;
    }

    // =========================================================
    // MODE
    // =========================================================
    private void setMode(Mode m) {
        mode = m;
        btnModeChat.setAlpha(m == Mode.CHAT ? 1f : 0.5f);
        btnModeNote.setAlpha(m == Mode.NOTE ? 1f : 0.5f);
        edt.setHint(m == Mode.NOTE
                ? "Nhập nội dung để tạo ghi chú…"
                : "Nhập tin nhắn…");
    }

    // =========================================================
    // MESSAGE
    // =========================================================
    private void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(
                String.valueOf(System.currentTimeMillis()),
                text,
                isUser,
                System.currentTimeMillis()
        ));
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);
    }

    // =========================================================
    // CREATE NOTE
    // =========================================================
    private void createNoteFromText(String content) {
        if (uid == null) {
            addMessage("Bạn cần đăng nhập để tạo ghi chú.", false);
            return;
        }

        String title = content.length() > 40
                ? content.substring(0, 40)
                : content;

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
                .addOnSuccessListener(ref ->
                        addMessage("✅ Đã tạo ghi chú.", false))
                .addOnFailureListener(e ->
                        addMessage("❌ Tạo ghi chú thất bại: " + e.getMessage(), false));
    }

    // =========================================================
    // AI CALL
    // =========================================================
    private void callAiForReply(String prompt) {
        AiRequest req = new AiRequest(
                prompt + ", answer in Vietnamese, nicely and shortly."
        );

        AiApiService.getApi().summarize(req)
                .enqueue(new Callback<AiResponse>() {
                    @Override
                    public void onResponse(
                            Call<AiResponse> call,
                            Response<AiResponse> response
                    ) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getSummary() != null) {

                            addMessage(response.body().getSummary(), false);

                        } else {
                            addMessage("❌ AI trả về lỗi.", false);
                        }
                    }

                    @Override
                    public void onFailure(Call<AiResponse> call, Throwable t) {
                        addMessage("❌ AI lỗi: " + t.getMessage(), false);
                    }
                });
    }

    // =========================================================
    // EXPAND / COLLAPSE CHAT
    // =========================================================
    private void toggleExpand() {
        if (getActivity() == null) return;

        View container = getActivity().findViewById(R.id.chat_container);
        if (container == null) return;

        ViewGroup.LayoutParams lp = container.getLayoutParams();

        if (!isExpanded) {
            // 🔥 PHÓNG TO (KHÔNG CHE APPBAR)
            int[] location = new int[2];
            container.getLocationOnScreen(location);

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int topY = location[1];

            lp.height = screenHeight - topY;
            btnExpandChat.setImageResource(R.drawable.ic_collapse);
            isExpanded = true;
        } else {
            // 🔽 THU NHỎ
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            btnExpandChat.setImageResource(R.drawable.ic_expand);
            isExpanded = false;
        }

        container.setLayoutParams(lp);
    }


    // =========================================================
    // CLOSE CHAT
    // =========================================================
    private void closeChat() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager()
                    .beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss();
        }

        if (getActivity() != null) {
            View host = getActivity().findViewById(R.id.chat_container);
            if (host != null) host.setVisibility(View.GONE);
        }
    }
}
