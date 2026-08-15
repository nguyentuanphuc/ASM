package com.example.asm;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.textViewContent.setText(message.getContent());

        // Hiển thị ảnh nếu có
        if (message.getImageUri() != null) {
            holder.imageViewMessage.setVisibility(View.VISIBLE);
            holder.imageViewMessage.setImageURI(message.getImageUri());
        } else {
            holder.imageViewMessage.setVisibility(View.GONE);
        }

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.cardBubble.getLayoutParams();
        
        if (message.getSender().equals("Bạn")) {
            // Căn phải cho tin nhắn người dùng
            params.startToStart = ConstraintLayout.LayoutParams.UNSET;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            holder.cardBubble.setCardBackgroundColor(Color.parseColor("#007AFF")); // Màu xanh Apple
            holder.textViewContent.setTextColor(Color.WHITE);
        } else {
            // Căn trái cho AI
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            holder.cardBubble.setCardBackgroundColor(Color.WHITE);
            holder.textViewContent.setTextColor(Color.BLACK);
        }
        holder.cardBubble.setLayoutParams(params);
        
        // Hiệu ứng mờ dần khi hiện tin nhắn mới
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(300).start();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardBubble;
        TextView textViewContent;
        ImageView imageViewMessage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBubble = itemView.findViewById(R.id.cardBubble);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
        }
    }
}