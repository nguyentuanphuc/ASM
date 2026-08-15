package com.example.asm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.asm.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private final String[] levels = {"Cấp 1 (Tiểu học)", "Cấp 2 (THCS)", "Cấp 3 (THPT)", "Sinh viên (Đại học)"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Cấu hình Spinner trình độ
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerRegisterLevel.setAdapter(adapter);

        binding.buttonRegister.setOnClickListener(v -> {
            if (binding.editTextName.getText() == null || binding.editTextEmail.getText() == null || binding.editTextPassword.getText() == null) return;
            
            String name = binding.editTextName.getText().toString();
            String email = binding.editTextEmail.getText().toString();
            String password = binding.editTextPassword.getText().toString();
            String selectedLevel = binding.spinnerRegisterLevel.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                // Lưu thông tin người dùng vào SharedPreferences
                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("userName", name)
                        .putString("userEmail", email)
                        .putString("userLevel", selectedLevel)
                        .apply();

                Toast.makeText(getContext(), "Đăng ký thành công cho " + selectedLevel + "!", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login);
            }
        });

        binding.textViewGoToLogin.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
