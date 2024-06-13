package com.example.mt_final;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class Translator {

    private OrtEnvironment environment;

    private OrtSession ortsession;

    private OrtSession.SessionOptions options;
    private static final String TAG = Translator.class.getName();

    Translator(Context context, String moduleAssetName) throws IOException
    {
        environment = OrtEnvironment.getEnvironment();
        options = new OrtSession.SessionOptions();


        try {
            ortsession = environment.createSession(utils.assetFilePath(context.getApplicationContext(), moduleAssetName), options);

            Log.d(TAG, "Loading module succeed");

        } catch (OrtException e) {
            Log.e(TAG, "ortSession create Error");
        } catch (IOException e)
        {
            Log.e(TAG,"IOexception");
        }
    }

    public FloatBuffer runModule(ArrayList<Long> inputs, int length, long target_idx)
    {
        long[] inputTensorShape = new long[]{1, length};
        long[] attn_maskTensorShape = new long[]{1, length};
        long[] dec_input_idsTensorShape = new long[]{1, length};
        long[] dec_attn_maskTensorShape = new long[]{1, length};

        long[] input_idxs = new long[length];
        long[] attn_mask = new long[length];
        long[] dec_input_idxs = new long[length];
        long[] dec_attn_mask = new long[length];

        for(int i = 0;i < length;i++)
        {
            input_idxs[i] = inputs.get(i);
        }
        Arrays.fill(attn_mask,1);

        dec_input_idxs[0] = target_idx;

        LongBuffer inputTensorBuffer = LongBuffer.wrap(input_idxs);
        LongBuffer attn_maskTensorBuffer = LongBuffer.wrap(attn_mask);
        LongBuffer dec_input_idsTensorBuffer = LongBuffer.wrap(dec_input_idxs);
        LongBuffer dec_attn_maskTensorBuffer = LongBuffer.wrap(dec_attn_mask);


        OnnxTensor inputTensor;
        OnnxTensor attn_maskTensor;
        OnnxTensor dec_input_idsTensor;
        OnnxTensor dec_attn_maskTensor;
        try {

            inputTensor = OnnxTensor.createTensor(
                    environment,inputTensorBuffer,inputTensorShape);
            attn_maskTensor = OnnxTensor.createTensor(
                    environment, attn_maskTensorBuffer, attn_maskTensorShape);
            dec_input_idsTensor = OnnxTensor.createTensor(
                    environment, dec_input_idsTensorBuffer, dec_input_idsTensorShape);
            dec_attn_maskTensor = OnnxTensor.createTensor(
                    environment, dec_attn_maskTensorBuffer, dec_attn_maskTensorShape);

            long[] shape = inputTensor.getInfo().getShape();
            System.out.println("Input Tensor Shape: " + Arrays.toString(shape));
            Log.d(TAG, "Input Tensor Shape: " + Arrays.toString(shape));

        }
        catch (OrtException e)
        {
            Log.e(TAG,"Tensor create failed");
            return null;
        }

        Map<String,OnnxTensor>inputMap = new HashMap<>();
        inputMap.put("input_ids",inputTensor);
        inputMap.put("attention_mask",attn_maskTensor);
        inputMap.put("decoder_input_ids",dec_input_idsTensor);
        inputMap.put("decoder_attention_mask",dec_attn_maskTensor);
        OrtSession.Result ort_result;

        try {
            ort_result = ortsession.run(inputMap);
        } catch (OrtException e) {
            Log.e(TAG, "Model run failed: " + e.getMessage());
            return null;
        }


        float[] result;
        try {
            result = ((float[][][])ort_result.get(0).getValue())[0][0];
        } catch (OrtException e) {
            Log.e(TAG,"get value of result failed");
            return null;
        }

        return FloatBuffer.wrap(result);
    }

    public int generate(FloatBuffer resultBuffer) {

        if (resultBuffer == null) {
            Log.e(TAG, "Result buffer is null");
            return -1;
        }

        float[] probs = resultBuffer.array();
        float max_prob = probs[0];
        int idx = 0;

        for (int i = 1; i < probs.length; i++)
        {
            if(probs[i] > max_prob)
            {
                max_prob = probs[i];
                idx = i;
            }
        }
        return idx;
    }

}




