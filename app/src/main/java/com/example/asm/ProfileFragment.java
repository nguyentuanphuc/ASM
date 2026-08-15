package com.example.asm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.asm.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private final String[] levels = {"Cấp 1 (Tiểu học)", "Cấp 2 (THCS)", "Cấp 3 (THPT)", "Sinh viên (Đại học)"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        dbHelper = new DatabaseHelper(requireContext());
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // 1. Hiển thị tên người dùng
        String savedName = sharedPreferences.getString("userName", "Người dùng AI 3.1");
        binding.editTextUserName.setText(savedName);

        // 2. Lưu tên khi người dùng sửa xong
        binding.editTextUserName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String newName = binding.editTextUserName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    sharedPreferences.edit().putString("userName", newName).apply();
                    Toast.makeText(getContext(), "Đã cập nhật tên!", Toast.LENGTH_SHORT).show();
                    binding.editTextUserName.clearFocus();
                }
                return true;
            }
            return false;
        });

        // 3. Cấu hình phần chọn cấp độ người dùng
        setupUserLevelSpinner();

        // 4. Cập nhật số liệu thống kê
        updateStats();

        // 5. Xử lý nút Xóa lịch sử trò chuyện
        binding.buttonClearHistory.setOnClickListener(v -> {
            dbHelper.clearAllMessages();
            Toast.makeText(getContext(), "Đã xóa lịch sử trò chuyện", Toast.LENGTH_SHORT).show();
            updateStats();
        });

        // 6. Xử lý nút Đăng xuất
        binding.buttonLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Hẹn gặp lại bạn!", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_login);
        });
    }

    private void setupUserLevelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerUserLevel.setAdapter(adapter);

        // Load cấp độ đã lưu
        String savedLevel = sharedPreferences.getString("userLevel", levels[0]);
        int selectedIndex = 0;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equals(savedLevel)) {
                selectedIndex = i;
                break;
            }
        }
        binding.spinnerUserLevel.setSelection(selectedIndex);

        // Lưu cấp độ khi thay đổi
        binding.spinnerUserLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLevel = levels[position];
                sharedPreferences.edit().putString("userLevel", selectedLevel).apply();
                // Cập nhật thống kê chi tiết khi trình độ thay đổi
                updateStats();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateStats() {
        if (binding == null) return;
        
        // Cập nhật tổng số tin nhắn
        int msgCount = dbHelper.getAllMessages().size();
        binding.textViewMsgCount.setText("Tổng số tin nhắn: " + msgCount);

        // Tạo chuỗi thống kê chi tiết theo từng cấp độ
        StringBuilder statsBuilder = new StringBuilder();
        
        // Thống kê tổng quát
        int totalQuiz = dbHelper.getTotalQuizCount();
        int correctQuiz = dbHelper.getCorrectQuizCount();
        if (totalQuiz > 0) {
            int percent = (correctQuiz * 100) / totalQuiz;
            statsBuilder.append("Tổng quát: ").append(percent).append("% (").append(correctQuiz).append("/").append(totalQuiz).append(")\n\n");
        } else {
            statsBuilder.append("Tổng quát: Chưa làm bài\n\n");
        }

        // Thống kê chi tiết từng trình độ
        for (String level : levels) {
            int total = dbHelper.getTotalQuizCountByLevel(level);
            int correct = dbHelper.getCorrectQuizCountByLevel(level);
            
            statsBuilder.append("• ").append(level).append(": ");
            if (total > 0) {
                int percent = (correct * 100) / total;
                statsBuilder.append(percent).append("% (").append(correct).append("/").append(total).append(")\n");
            } else {
                statsBuilder.append("Chưa có dữ liệu\n");
            }
        }
        
        // Hiển thị vào View StatsDetail (Thay thế cho View cũ textViewQuizScore)
        binding.textViewStatsDetail.setText(statsBuilder.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
