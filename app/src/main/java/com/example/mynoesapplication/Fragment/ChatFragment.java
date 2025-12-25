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
import androidx.constraintlayout.widget.ConstraintLayout;
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
    private ImageButton btnModeChat, btnModeNote, btnModeInfo, btnSend;
    private ImageButton btnExpandChat, btnCloseChat;

    // ================= STATE =================
    private boolean isExpanded = false;
    private boolean infoMenuShown = false;

    // ================= SIZE CONFIG =================
    private static final int COLLAPSED_HEIGHT_DP = 300;

    // ================= DATA =================
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private enum Mode { CHAT, NOTE, INFO }
    private Mode mode = Mode.CHAT;

    // ================= FIREBASE =================
    private FirebaseFirestore db;
    private String uid;

    // ================= INFO MENU =================
    private static final String INFO_MENU =
            "ℹ️ HƯỚNG DẪN SỬ DỤNG ỨNG DỤNG\n\n" +
                    "Vui lòng chọn một số:\n" +
                    "1️⃣ Giới thiệu ứng dụng\n" +
                    "2️⃣ Các tính năng chính\n" +
                    "3️⃣ Cách tạo & quản lý ghi chú\n" +
                    "4️⃣ Vẽ và ghi chú tự do\n" +
                    "5️⃣ Chia sẻ ghi chú / thư mục\n" +
                    "6️⃣ AI tóm tắt nội dung\n\n" +
                    "👉 Nhập số tương ứng để xem chi tiết.";

    // =========================================================
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        // ===== BIND UI =====
        rv = v.findViewById(R.id.rvChat);
        edt = v.findViewById(R.id.edtMessage);

        btnModeChat = v.findViewById(R.id.btnModeChat);
        btnModeNote = v.findViewById(R.id.btnModeNote);
        btnModeInfo = v.findViewById(R.id.btnModeMicro);

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

        // ===== RESET SIZE =====
        resetToCollapsed();

        // ===== DEFAULT MODE =====
        setMode(Mode.CHAT);

        // ===== EVENTS =====
        btnModeChat.setOnClickListener(v1 -> setMode(Mode.CHAT));
        btnModeNote.setOnClickListener(v1 -> setMode(Mode.NOTE));
        btnModeInfo.setOnClickListener(v1 -> setMode(Mode.INFO));

        btnSend.setOnClickListener(v1 -> {
            String txt = edt.getText().toString().trim();
            if (TextUtils.isEmpty(txt)) return;

            addMessage(txt, true);
            edt.setText("");

            if (mode == Mode.NOTE) {
                createNoteFromText(txt);
            } else if (mode == Mode.INFO) {
                handleInfoInput(txt);
            } else {
                callAiForReply(txt);
            }
        });

        btnExpandChat.setOnClickListener(v1 -> toggleExpand());
        btnCloseChat.setOnClickListener(v1 -> closeChat());

        return v;
    }

    // =========================================================
    // MODE
    // =========================================================
    private void setMode(Mode m) {
        mode = m;

        btnModeChat.setAlpha(m == Mode.CHAT ? 1f : 0.4f);
        btnModeNote.setAlpha(m == Mode.NOTE ? 1f : 0.4f);
        btnModeInfo.setAlpha(m == Mode.INFO ? 1f : 0.4f);

        if (m == Mode.NOTE) {
            edt.setHint("Nhập nội dung để tạo ghi chú…");
        } else if (m == Mode.INFO) {
            edt.setHint("Nhập số (1–6) để xem hướng dẫn…");

            if (!infoMenuShown) {
                addMessage(INFO_MENU, false);
                infoMenuShown = true;
            }
        } else {
            edt.setHint("Nhập tin nhắn…");
        }
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
    // INFO MODE (MENU NUMBER)
    // =========================================================
    private void handleInfoInput(String input) {
        switch (input) {
            case "1":
                addMessage(getOverview(), false);
                break;
            case "2":
                addMessage(getFeatures(), false);
                break;
            case "3":
                addMessage(getCreateNoteGuide(), false);
                break;
            case "4":
                addMessage(getDrawingGuide(), false);
                break;
            case "5":
                addMessage(getShareGuide(), false);
                break;
            case "6":
                addMessage(getAiGuide(), false);
                break;
            default:
                addMessage(
                        "❌ Lựa chọn không hợp lệ.\n\n" +
                                "👉 Vui lòng nhập số từ 1 đến 6.\n\n" +
                                INFO_MENU,
                        false
                );
                break;
        }
    }

    // =========================================================
    // INFO CONTENT
    // =========================================================
    private String getOverview() {
        return "📒 GIỚI THIỆU ỨNG DỤNG\n\n" +
                "Notes Application là một ứng dụng ghi chú thông minh, được xây dựng nhằm hỗ trợ " +
                "người dùng quản lý, tổ chức và khai thác thông tin một cách hiệu quả trong môi trường " +
                "học tập và làm việc hiện đại. Ứng dụng không chỉ cho phép tạo và quản lý nội dung, " +
                "mà còn mở rộng khả năng xử lý tài liệu và cộng tác giữa nhiều người dùng.\n\n" +
                "Ứng dụng hỗ trợ làm việc trực tiếp với tài liệu PDF, tích hợp trí tuệ nhân tạo để " +
                "tự động tóm tắt nội dung, giúp người dùng nhanh chóng nắm bắt thông tin quan trọng. " +
                "Bên cạnh đó, tính năng chia sẻ ghi chú và thư mục cho phép nhiều người cùng truy cập " +
                "và làm việc trên dữ liệu theo thời gian thực, mang lại trải nghiệm linh hoạt và hiện đại.";
    }

    private String getFeatures() {
        return "✅ CÁC TÍNH NĂNG CHÍNH\n\n" +
                "• Tạo, chỉnh sửa và quản lý ghi chú\n" +
                "• Quản lý ghi chú theo thư mục\n" +
                "• Làm việc với tài liệu PDF\n" +
                "• Vẽ và ghi chú tự do\n" +
                "• Chia sẻ ghi chú và thư mục\n" +
                "• AI tóm tắt nội dung\n" +
                "• Cập nhật dữ liệu theo thời gian thực";
    }

    private String getCreateNoteGuide() {
        return "📝 CÁCH TẠO & QUẢN LÝ GHI CHÚ\n\n" +
                "1. Tạo ghi chú mới từ màn hình chính\n" +
                "2. Nhập nội dung \n" +
                "3. Ấn quay về thì dữ liệu tự động cập nhật\n\n" +
                "📌 Có thể tạo nhanh bằng chế độ NOTE trong khung chat của tôi.";
    }

    private String getDrawingGuide() {
        return "🎨 VẼ & GHI CHÚ TỰ DO\n\n" +
                "• Pen: viết nội dung tự do\n" +
                "• Marker: đánh dấu thông tin\n" +
                "• Eraser: xoá nét vẽ\n" +
                "• Laser: chỉ dẫn tạm thời\n" +
                "• Undo / Redo: hoàn tác thao tác";
    }

    private String getShareGuide() {
        return "🤝 CHIA SẺ THƯ MỤC\n\n" +
                "• Trên mỗi thư mục đều có nút Tùy chọn\n" +
                "• Người dùng chọn Tùy chọn → Chia sẻ thư mục hoặc ghi chú\n" +
                "• Hệ thống sẽ tạo một mã chia sẻ (code) để cấp quyền truy cập\n" +
                "• Người dùng gửi mã này cho người khác để họ có thể truy cập nội dung\n\n" +
                "📌 Sau khi chia sẻ, các thay đổi sẽ được cập nhật theo thời gian thực.";
    }


    private String getAiGuide() {
        return "🤖 AI TÓM TẮT NỘI DUNG\n\n" +
                "• Trên mỗi ghi chú hoặc tài liệu PDF đều có nút tùy chọn (Options)\n" +
                "• Người dùng có thể sử dụng chức năng tìm kiếm trong menu tùy chọn " +
                "để nhanh chóng tìm thấy nút \"Tóm tắt\"\n" +
                "• Khi chọn \"Tóm tắt\", hệ thống sẽ sử dụng trí tuệ nhân tạo để " +
                "phân tích và rút gọn nội dung ghi chú hoặc PDF\n\n" +
                "📌 Kết quả tóm tắt giúp người dùng nắm bắt nhanh các ý chính " +
                "mà không cần đọc toàn bộ nội dung.";
    }


    // =========================================================
    // CREATE NOTE
    // =========================================================
    private void createNoteFromText(String content) {
        if (uid == null) {
            addMessage("❌ Bạn cần đăng nhập để tạo ghi chú.", false);
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("title", content.length() > 40 ? content.substring(0, 40) : content);
        doc.put("content", content);
        doc.put("createdAt", Timestamp.now());
        doc.put("updatedAt", Timestamp.now());
        doc.put("deleted", false);

        db.collection("users")
                .document(uid)
                .collection("notes")
                .add(doc)
                .addOnSuccessListener(r -> addMessage("✅ Đã tạo ghi chú.", false))
                .addOnFailureListener(e -> addMessage("❌ Lỗi: " + e.getMessage(), false));
    }

    // =========================================================
    // AI CALL
    // =========================================================
    private void callAiForReply(String prompt) {
        AiRequest req = new AiRequest(prompt + ", answer in Vietnamese, shortly.");

        AiApiService.getApi().summarize(req)
                .enqueue(new Callback<AiResponse>() {
                    @Override
                    public void onResponse(Call<AiResponse> call, Response<AiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
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
    // EXPAND / COLLAPSE
    // =========================================================
    private void toggleExpand() {
        if (getActivity() == null) return;

        View container = getActivity().findViewById(R.id.chat_container);
        if (container == null) return;

        ConstraintLayout.LayoutParams lp =
                (ConstraintLayout.LayoutParams) container.getLayoutParams();

        if (!isExpanded) {
            lp.width = 0;
            lp.height = 0;
            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.topToBottom = R.id.topBar;
            lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.bottomToTop = ConstraintLayout.LayoutParams.UNSET;
            lp.setMargins(dp(12), dp(8), dp(12), dp(12));
            btnExpandChat.setImageResource(R.drawable.ic_collapse);
            isExpanded = true;
        } else {
            resetToCollapsed();
        }

        container.setLayoutParams(lp);
    }

    private void resetToCollapsed() {
        if (getActivity() == null) return;

        View container = getActivity().findViewById(R.id.chat_container);
        if (container == null) return;

        ConstraintLayout.LayoutParams lp =
                (ConstraintLayout.LayoutParams) container.getLayoutParams();

        lp.width = dp(280);
        lp.height = dp(COLLAPSED_HEIGHT_DP);
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.bottomToTop = R.id.btnOpenChat;
        lp.setMargins(0, 0, dp(12), dp(6));

        btnExpandChat.setImageResource(R.drawable.ic_expand);
        isExpanded = false;
        container.setLayoutParams(lp);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // =========================================================
    // CLOSE CHAT
    // =========================================================
    private void closeChat() {
        getParentFragmentManager()
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();

        if (getActivity() != null) {
            View host = getActivity().findViewById(R.id.chat_container);
            if (host != null) host.setVisibility(View.GONE);
        }
    }
}
