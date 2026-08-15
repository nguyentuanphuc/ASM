package com.example.asm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.asm.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hiệu ứng mượt mà khi vào Dashboard
        binding.getRoot().setAlpha(0f);
        binding.getRoot().animate().alpha(1f).setDuration(500).start();

        // Hiển thị thông tin người dùng và trình độ
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String name = prefs.getString("userName", "Người dùng");
        String level = prefs.getString("userLevel", "Chưa chọn trình độ");
        binding.textViewUserStatus.setText(name + " • " + level);

        // Chuyển sang màn hình Chat
        binding.cardChat.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_dashboard_to_chat)
        );

        // Chuyển sang màn hình Trắc nghiệm
        binding.cardQuiz.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_dashboard_to_quiz)
        );

        // Chuyển sang màn hình Hồ sơ cá nhân
        binding.cardProfile.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_dashboard_to_profile)
        );

        // Mở chat với môn học cụ thể
        binding.cardMath.setOnClickListener(v -> openChatWithSubject("Toán Học"));
        binding.cardPhysics.setOnClickListener(v -> openChatWithSubject("Vật Lý"));
    }

    private void openChatWithSubject(String subject) {
        Bundle bundle = new Bundle();
        bundle.putString("subject", subject);
        NavHostFragment.findNavController(this).navigate(R.id.action_dashboard_to_chat, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
