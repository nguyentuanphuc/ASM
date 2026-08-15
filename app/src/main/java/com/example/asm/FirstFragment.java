package com.example.asm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.asm.databinding.FragmentFirstBinding;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private GenerativeModelFutures model;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private static final String MODEL_NAME = "gemini-3.1-flash-lite";
    private static final String API_KEY = "AQ.Ab8RN6IkxYmvJ6RqEYu3OnufbXGuIs1D5Enq1T74pfclkf2pEg";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        dbHelper = new DatabaseHelper(requireContext());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(0, 0, 0, imeHeight);
            return insets;
        });

        chatMessages = dbHelper.getAllMessages();
        if (chatMessages == null) chatMessages = new ArrayList<>();
        
        chatAdapter = new ChatAdapter(chatMessages);
        binding.recyclerViewChat.setAdapter(chatAdapter);
        binding.recyclerViewChat.setLayoutManager(new LinearLayoutManager(getContext()));
        
        initAI();

        binding.buttonSend.setOnClickListener(v -> {
            String message = binding.editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) sendMessage(message);
        });
    }

    private void initAI() {
        try {
            List<SafetySetting> safetySettings = Arrays.asList(
                new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            );
            
            GenerativeModel gm = new GenerativeModel(MODEL_NAME, API_KEY, null, safetySettings);
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khởi tạo AI", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String userText) {
        if (model == null) return;

        ChatMessage userMsg = new ChatMessage(userText, "Bạn");
        chatMessages.add(userMsg);
        dbHelper.addMessage(userMsg);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
        binding.editTextMessage.setText("");

        int aiPos = chatMessages.size();
        chatMessages.add(new ChatMessage("AI đang suy nghĩ...", "AI"));
        chatAdapter.notifyItemInserted(aiPos);

        // Logic cá nhân hóa theo trình độ
        String userLevel = sharedPreferences.getString("userLevel", "Học sinh");
        String systemInstruction = "";
        
        if (userLevel.contains("Cấp 1")) {
            systemInstruction = "Bạn là giáo viên Tiểu học. Hãy giải thích cực kỳ đơn giản, dùng ví dụ gần gũi và động viên học sinh.";
        } else if (userLevel.contains("Cấp 2")) {
            systemInstruction = "Bạn là giáo viên THCS. Hãy giải thích rõ ràng, súc tích, phù hợp với kiến thức lớp 6-9.";
        } else if (userLevel.contains("Cấp 3")) {
            systemInstruction = "Bạn là giáo viên THPT. Hãy giải thích sâu, cung cấp công thức hoặc mẹo làm bài thi Đại học.";
        } else {
            systemInstruction = "Bạn là giảng viên Đại học. Hãy giải thích chuyên sâu, có tính học thuật và cung cấp tài liệu tham khảo.";
        }

        String fullPrompt = systemInstruction + "\nCâu hỏi của người dùng: " + userText;

        ListenableFuture<GenerateContentResponse> response = model.generateContent(new Content.Builder().addText(fullPrompt).build());
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    String text = result.getText();
                    ChatMessage aiMsg = new ChatMessage(text != null ? text : "AI không có phản hồi.", "AI");
                    chatMessages.set(aiPos, aiMsg);
                    dbHelper.addMessage(aiMsg);
                    chatAdapter.notifyItemChanged(aiPos);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    ChatMessage errorMsg = new ChatMessage("Lỗi kết nối. Vui lòng kiểm tra lại mạng.", "AI");
                    chatMessages.set(aiPos, errorMsg);
                    chatAdapter.notifyItemChanged(aiPos);
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
