package com.example.asm;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Bật lại chế độ chuẩn để hệ thống tự động đẩy nội dung lên khi hiện bàn phím
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        
        setContentView(R.layout.activity_main);
    }
}
