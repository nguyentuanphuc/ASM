package com.example.asm;

import android.net.Uri;

public class ChatMessage {
    private String content;
    private String sender;
    private Uri imageUri;

    public ChatMessage(String content, String sender) {
        this.content = content;
        this.sender = sender;
    }

    public ChatMessage(String content, String sender, Uri imageUri) {
        this.content = content;
        this.sender = sender;
        this.imageUri = imageUri;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public Uri getImageUri() {
        return imageUri;
    }
}