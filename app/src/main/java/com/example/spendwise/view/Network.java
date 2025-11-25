package com.example.spendwise.view;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Network {

    public interface Callback {
        void onSuccess(String reply);
        void onError(String error);
    }

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    // Emulator → Laptop mapping
    private final String baseUrl = "http://10.0.2.2:11434/api/chat";

    private final OkHttpClient client = new OkHttpClient();

    public void chat(String userMessage, Callback cb) {
        new Thread(() -> {
            try {
                // Build JSON payload
                JSONObject json = new JSONObject();
                json.put("model", "llama3.2");

                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", userMessage);
                messages.put(msg);
                json.put("messages", messages);
                json.put("stream", false);

                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url(baseUrl)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    // Graceful error
                    cb.onError("😴 Llama is napping. Please try again later! (HTTP " + response.code() + ")");
                    return;
                }

                String responseText = response.body().string();
                Log.d("Network", responseText);

                JSONObject root = new JSONObject(responseText);
                JSONObject messageObj = root.getJSONObject("message");

                String reply = messageObj.getString("content");

                cb.onSuccess(reply);

            } catch (Exception e) {
                // Graceful error on any exception
                cb.onError("😴 Llama is napping. Please try again later!");
            }
        }).start();
    }
}

