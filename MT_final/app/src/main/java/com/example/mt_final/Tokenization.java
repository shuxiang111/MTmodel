package com.example.mt_final;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Tokenization {
    private static final String TAG = Tokenization.class.getName();

    public static int getTextLength(String text) {
        return text.split(" ").length;
    }

    public static ArrayList<Long> Tokenize(String text, JSONObject word2idx) {
        String inputStr = text.toLowerCase();
        String[] inputWords = inputStr.split(" ");
        ArrayList<Long> inputs = new ArrayList<>();
        try {
            for (int i = 0; i < inputWords.length; i++) {
                while (!inputWords[i].isEmpty()) {
                    int len = config.MAX_WORD_LENGTH;
                    if (inputWords[i].length() < len) {
                        len = inputWords[i].length();
                    }
                    String TryWord = inputWords[i].substring(0, len);
                    while (!word2idx.has(TryWord)) {
                        if (inputWords[i].length() == 1) {
                            break;
                        }
                        if (TryWord.length() == 1) {
                            break;
                        }
                        TryWord = TryWord.substring(0, TryWord.length() - 1);
                    }
                    if (TryWord.length() > inputWords[i].length()) {
                        break;  // 防止 inputWords[i].substring(TryWord.length()) 抛出异常
                    }
                    inputWords[i] = inputWords[i].substring(TryWord.length());
                    if (word2idx.has(TryWord)) {
                        inputs.add(word2idx.getLong(TryWord));
                    } else {
                        inputs.add(word2idx.getLong("<unk>"));
                    }
                }
            }
            inputs.add(word2idx.getLong("</s>"));
        } catch (JSONException e) {
            android.util.Log.e(TAG, "JSONException ", e);
            return null;
        }
        return inputs;
    }

}
