package com.example.asm;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.asm.databinding.FragmentQuizBinding;
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
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class QuizFragment extends Fragment {
    private FragmentQuizBinding binding;
    private GenerativeModelFutures model;
    private DatabaseHelper dbHelper;
    private String correctAnswerKey = "";
    private String currentQuestionText = "";
    private String selectedLevel = "Cấp 1 (Tiểu học)";
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    private static final String MODEL_NAME = "gemini-3.1-flash-lite";
    private static final String API_KEY = "AQ.Ab8RN6IkxYmvJ6RqEYu3OnufbXGuIs1D5Enq1T74pfclkf2pEg";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper(requireContext());
        
        setupLevelSpinner();
        initAI();
        generateNewQuestion();
        
        binding.buttonSubmitQuiz.setOnClickListener(v -> checkAnswer());
    }

    private void setupLevelSpinner() {
        String[] levels = {"Cấp 1 (Tiểu học)", "Cấp 2 (THCS)", "Cấp 3 (THPT)", "Sinh viên (Đại học)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLevel.setAdapter(adapter);

        binding.spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newLevel = levels[position];
                if (!newLevel.equals(selectedLevel)) {
                    selectedLevel = newLevel;
                    retryCount = 0;
                    generateNewQuestion();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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

    private void generateNewQuestion() {
        if (model == null) return;
        setUIState(true);

        // Lấy danh sách câu hỏi gần đây để yêu cầu AI tránh lặp lại
        List<String> recentQuestions = dbHelper.getRecentQuestions(20);
        StringBuilder avoidInstruction = new StringBuilder();
        if (!recentQuestions.isEmpty()) {
            avoidInstruction.append("\nKHÔNG ĐƯỢC lặp lại các nội dung câu hỏi sau:\n");
            for (String q : recentQuestions) {
                avoidInstruction.append("- ").append(q).append("\n");
            }
        }

        Content content = new Content.Builder()
                .addText("Tạo 1 câu hỏi trắc nghiệm tiếng Việt mới lạ, độc đáo dành cho đối tượng: " + selectedLevel + ". " +
                         "Nội dung cần phù hợp với trình độ này. Tránh các câu hỏi quá đơn giản hoặc đã phổ biến. " + 
                         avoidInstruction.toString() + 
                         "\nTrả về JSON: {\"question\": \"...\", \"a\": \"...\", \"b\": \"...\", \"c\": \"...\", \"d\": \"...\", \"correct\": \"a/b/c/d\"}")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String raw = result.getText();
                    if (raw == null) { retry(); return; }
                    String jsonPart = raw.substring(raw.indexOf("{"), raw.lastIndexOf("}") + 1);
                    JSONObject json = new JSONObject(jsonPart);
                    String question = json.getString("question");

                    // Kiểm tra trùng lặp trong Database
                    if (dbHelper.isQuestionDuplicate(question) && retryCount < MAX_RETRIES) {
                        retryCount++;
                        retry();
                        return;
                    }

                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        try {
                            currentQuestionText = question;
                            retryCount = 0;
                            binding.textViewQuestion.setText(currentQuestionText);
                            binding.textViewQuestion.setTextColor(Color.BLACK);
                            binding.radioOptionA.setText("A. " + json.getString("a"));
                            binding.radioOptionB.setText("B. " + json.getString("b"));
                            binding.radioOptionC.setText("C. " + json.getString("c"));
                            binding.radioOptionD.setText("D. " + json.getString("d"));
                            binding.radioGroupOptions.clearCheck();
                            correctAnswerKey = json.getString("correct").toLowerCase().trim();
                            setUIState(false);
                        } catch (Exception e) { retry(); }
                    });
                } catch (Exception e) { retry(); }
            }

            private void retry() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> generateNewQuestion());
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    setUIState(false);
                    binding.textViewQuestion.setText("Lỗi kết nối AI. Vui lòng thử lại.");
                    binding.buttonSubmitQuiz.setText("Thử lại");
                });
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void setUIState(boolean isLoading) {
        if (binding == null) return;
        binding.progressBarQuiz.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textViewQuestion.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.radioGroupOptions.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.buttonSubmitQuiz.setEnabled(!isLoading);
        binding.buttonSubmitQuiz.setText(isLoading ? "AI đang soạn câu hỏi mới..." : "Kiểm tra đáp án");
    }

    private void checkAnswer() {
        String btnText = binding.buttonSubmitQuiz.getText().toString();
        if (btnText.contains("Tiếp tục") || btnText.contains("Thử lại") || btnText.contains("học")) {
            generateNewQuestion();
            return;
        }

        int selectedId = binding.radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Vui lòng chọn đáp án!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userChoice = "a";
        if (selectedId == R.id.radioOptionB) userChoice = "b";
        else if (selectedId == R.id.radioOptionC) userChoice = "c";
        else if (selectedId == R.id.radioOptionD) userChoice = "d";

        boolean isCorrect = userChoice.equals(correctAnswerKey);
        
        if (dbHelper != null) {
            dbHelper.saveQuizResult(currentQuestionText, selectedLevel, isCorrect);
        }

        binding.textViewQuestion.setText(isCorrect ? "CHÍNH XÁC! ✨" : "SAI RỒI! ❌\nĐáp án đúng là: " + correctAnswerKey.toUpperCase());
        binding.textViewQuestion.setTextColor(ContextCompat.getColor(requireContext(), isCorrect ? R.color.correct_green : R.color.wrong_red));
        binding.buttonSubmitQuiz.setText("Tiếp tục học");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}