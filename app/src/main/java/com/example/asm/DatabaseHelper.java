package com.example.asm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "study_mentor.db";
    private static final int DATABASE_VERSION = 4;

    // Table Chat
    private static final String TABLE_CHAT = "chat";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_IMAGE_URI = "image_uri";

    // Table Quiz Scores
    private static final String TABLE_QUIZ = "quiz_scores";
    private static final String COLUMN_SCORE_ID = "quiz_id";
    private static final String COLUMN_QUESTION_TEXT = "question_text";
    private static final String COLUMN_LEVEL = "level";
    private static final String COLUMN_IS_CORRECT = "is_correct"; // 1 for true, 0 for false
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createChatTable = "CREATE TABLE " + TABLE_CHAT + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_SENDER + " TEXT, " +
                COLUMN_IMAGE_URI + " TEXT)";
        db.execSQL(createChatTable);

        String createQuizTable = "CREATE TABLE " + TABLE_QUIZ + " (" +
                COLUMN_SCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_QUESTION_TEXT + " TEXT, " +
                COLUMN_LEVEL + " TEXT, " +
                COLUMN_IS_CORRECT + " INTEGER, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createQuizTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_QUIZ + " (" +
                    COLUMN_SCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_IS_CORRECT + " INTEGER, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_QUIZ + " ADD COLUMN " + COLUMN_QUESTION_TEXT + " TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_QUIZ + " ADD COLUMN " + COLUMN_LEVEL + " TEXT");
        }
    }

    // Chat methods
    public void addMessage(ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.getContent());
        values.put(COLUMN_SENDER, message.getSender());
        values.put(COLUMN_IMAGE_URI, message.getImageUri() != null ? message.getImageUri().toString() : null);
        db.insert(TABLE_CHAT, null, values);
        db.close();
    }

    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CHAT, null);

        if (cursor.moveToFirst()) {
            do {
                int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);
                int senderIndex = cursor.getColumnIndex(COLUMN_SENDER);
                int imageUriIndex = cursor.getColumnIndex(COLUMN_IMAGE_URI);

                String content = contentIndex != -1 ? cursor.getString(contentIndex) : "";
                String sender = senderIndex != -1 ? cursor.getString(senderIndex) : "";
                String imageUriStr = imageUriIndex != -1 ? cursor.getString(imageUriIndex) : null;
                
                Uri imageUri = imageUriStr != null ? Uri.parse(imageUriStr) : null;
                messages.add(new ChatMessage(content, sender, imageUri));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }

    public void clearAllMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHAT, null, null);
        db.close();
    }

    // Quiz methods
    public void saveQuizResult(String question, String level, boolean isCorrect) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUESTION_TEXT, question);
        values.put(COLUMN_LEVEL, level);
        values.put(COLUMN_IS_CORRECT, isCorrect ? 1 : 0);
        db.insert(TABLE_QUIZ, null, values);
        db.close();
    }

    public boolean isQuestionDuplicate(String question) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_QUIZ + " WHERE " + COLUMN_QUESTION_TEXT + " = ? LIMIT 1", new String[]{question});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public List<String> getRecentQuestions(int limit) {
        List<String> questions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_QUESTION_TEXT + " FROM " + TABLE_QUIZ + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT ?", new String[]{String.valueOf(limit)});
        if (cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex(COLUMN_QUESTION_TEXT);
                if (index != -1) {
                    String q = cursor.getString(index);
                    if (q != null) questions.add(q);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return questions;
    }

    // Lấy số câu đúng theo cấp độ
    public int getCorrectQuizCountByLevel(String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUIZ + " WHERE " + COLUMN_LEVEL + " = ? AND " + COLUMN_IS_CORRECT + " = 1", new String[]{level});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    // Lấy tổng số câu theo cấp độ
    public int getTotalQuizCountByLevel(String level) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUIZ + " WHERE " + COLUMN_LEVEL + " = ?", new String[]{level});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    public int getCorrectQuizCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUIZ + " WHERE " + COLUMN_IS_CORRECT + " = 1", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalQuizCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUIZ, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }
}