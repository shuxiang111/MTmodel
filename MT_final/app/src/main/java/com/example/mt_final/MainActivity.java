package com.example.mt_final;

import com.example.mt_final.R;

import static com.example.mt_final.config.MAX_LENGTH;
import com.example.mt_final.JSONloader;
import com.example.mt_final.Tokenization;
import com.example.mt_final.config;
import com.example.mt_final.Translator;

import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.nio.FloatBuffer;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private EditText myEditText;
    private TextView myTextView;
    private Button myButton;

    private Translator myTranslator;

    private Handler myHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            String result = (String) msg.obj;
            showTranslationResult(result);
            myButton.setEnabled(true);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setTheme(com.example.mt_final.R.style.Theme_MT_final);
        setContentView(R.layout.activity_main);

        myButton = findViewById(R.id.TranslateBtn1);
        myEditText = findViewById(R.id.editForm1);
        myTextView = findViewById(R.id.viewForm1);

        myButton.setOnClickListener(v -> {

            myButton.setEnabled(false);
            String textToTranslate = myEditText.getText().toString();
            Thread thread = new Thread(() -> {
                Looper.prepare();
                final String result = translate(textToTranslate);
                Message msg = Message.obtain();
                msg.obj = result;
                myHandler.sendMessage(msg);
            });
            thread.start();
        });

    }

    private void showTranslationResult(String result) {
        myTextView.setText(result);
    }

    private String translate(final String text) {
        if (myTranslator == null) {
            try {
                myTranslator = new Translator(getApplicationContext(), "opus-en-zh.onnx");
            } catch (IOException e) {
                Log.e(TAG, "Error reading assets", e);
                finish();
            }
        }

        JSONloader jl = new JSONloader();
        JSONObject word2idx = jl.jsonloader(getApplicationContext(), "word2idx.json");
        JSONObject idx2word = jl.jsonloader(getApplicationContext(), "idx2word.json");

        ArrayList<Long> inputs = Tokenization.Tokenize(text, word2idx);

        assert inputs != null;
        FloatBuffer resultBuffer = myTranslator.runModule(inputs, inputs.size(), config.BOS_TOKEN);

        ArrayList<Integer> results = new ArrayList<>();
        for (int i = 0; i < config.MAX_LENGTH; i++) {
            int nextToken = myTranslator.generate(resultBuffer);
            if (nextToken == config.EOS_TOKEN) {
                break;
            }
            results.add(nextToken);
        }

        StringBuilder translation = new StringBuilder();
        try {
            for (int idx : results) {
                String word = idx2word.getString(String.valueOf(idx));
                translation.append(word).append(" ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting tokens to words", e);
        }

        return translation.toString().trim();
    }
}
