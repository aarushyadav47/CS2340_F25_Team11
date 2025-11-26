package com.example.spendwise.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.List;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ChatbotBinding;
import com.example.spendwise.model.ChatMessage;
import com.example.spendwise.model.ChatSession;
import com.example.spendwise.viewModel.ChatbotViewModel;

public class Chatbot extends AppCompatActivity {

    private ChatbotBinding binding;
    private ChatbotViewModel viewModel;

    private Calendar calendar = Calendar.getInstance();
    private Calendar currentSimulatedDate = Calendar.getInstance();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ChatbotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ChatbotViewModel.class);

        // Receive Dashboard-selected date
        Intent intent = getIntent();
        String dashboardDate = intent.getStringExtra("selected_date");
        if (dashboardDate != null && !dashboardDate.isEmpty()) {
            try {
                Date date = dateFormat.parse(dashboardDate);
                calendar.setTime(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        EditText inputBox = binding.inputBox;
        Button sendBtn = binding.sendBtn;

        // Button to show previous sessions
        Button previousChatsBtn = new Button(this);
        previousChatsBtn.setText("📋 Previous Chats");
        previousChatsBtn.setOnClickListener(v -> showPreviousSessionsDialog());

        // Add button above chat container
        ((LinearLayout) binding.chatScrollView.getParent()).addView(previousChatsBtn, 0);

        // Quick command buttons
        setupQuickCommands();

        // Observe messages from ViewModel
        viewModel.getMessages().observe(this, messages -> {
            binding.chatContainer.removeAllViews();
            for (ChatMessage msg : messages) {
                addChatMessage(msg.getContent(), msg.getRole().equals("user"));
            }
        });

        // Observe status messages (errors)
        viewModel.getStatusMessage().observe(this, status -> {
            if (!TextUtils.isEmpty(status)) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            }
        });

        // Send button click - NOW shows previous chats dialog
        sendBtn.setOnClickListener(v -> {
            String message = inputBox.getText().toString().trim();
            if (message.isEmpty()) return;

            inputBox.setText("");

            // Show dialog to choose: new chat or continue previous
            showPreviousChatSelectionDialog(message);
        });

        setupNavBar(dashboardDate);
    }

    /** Setup quick command buttons */
    private void setupQuickCommands() {
        LinearLayout quickCommandsLayout = new LinearLayout(this);
        quickCommandsLayout.setOrientation(LinearLayout.HORIZONTAL);
        quickCommandsLayout.setPadding(16, 8, 16, 8);

        String[] commands = {
                "📊 Weekly spending",
                "💰 Cut costs",
                "📈 vs Last month"
        };

        String[] fullCommands = {
                "Summarize my spending this week",
                "Suggest where I can cut costs",
                "How did I perform compared to last month?"
        };

        for (int i = 0; i < commands.length; i++) {
            final String fullCommand = fullCommands[i];
            Button btn = new Button(this);
            btn.setText(commands[i]);
            btn.setTextSize(11f);
            btn.setPadding(8, 6, 8, 6);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                // Quick commands always start new session
                viewModel.startNewSession();
                viewModel.sendMessage(fullCommand, null);
            });

            quickCommandsLayout.addView(btn);
        }

        ((LinearLayout) binding.chatScrollView.getParent()).addView(quickCommandsLayout, 0);
    }

    /** Adds a chat bubble to the container */
    private void addChatMessage(String message, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(16f);
        tv.setTextColor(isUser ? 0xFFFFFFFF : 0xFF000000);
        tv.setBackgroundResource(isUser ? R.drawable.bubble_user : R.drawable.bubble_ai);
        tv.setPadding(24, 16, 24, 16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 8, 8, 8);
        params.gravity = isUser ? Gravity.END : Gravity.START;

        tv.setLayoutParams(params);
        binding.chatContainer.addView(tv);

        // Scroll to bottom
        binding.chatScrollView.post(() -> binding.chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    /** Show dialog to choose: new chat or continue previous (TRIGGERED ON SEND) */
    private void showPreviousChatSelectionDialog(String newMessage) {
        viewModel.getPreviousChats(sessions -> {
            if (sessions.isEmpty()) {
                // No previous chats, start new session
                Log.d("Chatbot", "No previous sessions, starting new chat");
                viewModel.startNewSession();
                viewModel.sendMessage(newMessage, null);
                return;
            }

            Log.d("Chatbot", "Found " + sessions.size() + " previous sessions");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Continue a conversation?");

            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_previous_chat, null);
            ListView listView = dialogView.findViewById(R.id.sessionsListView);
            Button newChatButton = dialogView.findViewById(R.id.newChatButton);

            // Create adapter with titles
            List<String> sessionTitles = new ArrayList<>();
            for (ChatSession session : sessions) {
                sessionTitles.add(session.getTitle());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, sessionTitles);
            listView.setAdapter(adapter);

            AlertDialog dialog = builder.setView(dialogView).create();

            // When a session is clicked, continue that conversation
            listView.setOnItemClickListener((parent, view, position, id) -> {
                ChatSession selectedSession = sessions.get(position);
                Log.d("Chatbot", "Loading session: " + selectedSession.getId());
                viewModel.loadSession(selectedSession);
                viewModel.sendMessage(newMessage, selectedSession.getId());
                dialog.dismiss();
            });

            // New chat button - start fresh
            newChatButton.setOnClickListener(v -> {
                Log.d("Chatbot", "Starting new session");
                viewModel.startNewSession();
                viewModel.sendMessage(newMessage, null);
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    /** Show dialog with previous chat sessions (TRIGGERED BY BUTTON) */
    private void showPreviousSessionsDialog() {
        viewModel.getPreviousChats(sessions -> {
            if (sessions.isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("No Previous Chats")
                        .setMessage("You don't have any previous conversations yet.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            Log.d("Chatbot", "Showing " + sessions.size() + " previous sessions");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_previous_chat, null);
            ListView listView = dialogView.findViewById(R.id.sessionsListView);
            Button newChatButton = dialogView.findViewById(R.id.newChatButton);

            // Create adapter with titles
            List<String> sessionTitles = new ArrayList<>();
            for (ChatSession session : sessions) {
                sessionTitles.add(session.getTitle());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, sessionTitles);
            listView.setAdapter(adapter);

            AlertDialog dialog = builder.setView(dialogView).create();

            // When a session is clicked, show details
            listView.setOnItemClickListener((parent, view, position, id) -> {
                ChatSession selectedSession = sessions.get(position);
                showSessionDetailsDialog(selectedSession);
                dialog.dismiss();
            });

            // New chat button
            newChatButton.setOnClickListener(v -> {
                viewModel.startNewSession();
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    /** Show details of a specific session with summary */
    private void showSessionDetailsDialog(ChatSession session) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(session.getTitle());

        String message = "Summary: " + session.getSummary() +
                "\n\n" + session.getMessages().size() + " messages" +
                "\n\nWould you like to continue this conversation?";

        builder.setMessage(message)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Log.d("Chatbot", "Loading session from details: " + session.getId());
                    viewModel.loadSession(session);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Bottom navigation setup */
    private void setupNavBar(String dashboardDate) {
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);

        dashboardNavigate.setOnClickListener(v -> startActivity(new Intent(this, Dashboard.class)));
        findViewById(R.id.expenseLog_navigate).setOnClickListener(v -> {
            Intent expenseIntent = new Intent(this, ExpenseLog.class);
            expenseIntent.putExtra("selected_date", dashboardDate);
            startActivity(expenseIntent);
        });
        findViewById(R.id.budget_navigate).setOnClickListener(v -> {
            Intent budgetIntent = new Intent(this, Budgetlog.class);
            budgetIntent.putExtra("selected_date", dashboardDate);
            startActivity(budgetIntent);
        });
        findViewById(R.id.savingCircle_navigate).setOnClickListener(v -> {
            Intent savingIntent = new Intent(this, SavingCircleLog.class);
            savingIntent.putExtra("selected_date", dashboardDate);
            startActivity(savingIntent);
        });

        findViewById(R.id.chatbot_navigate).setOnClickListener(v -> {
            Intent chatbotIntent = new Intent(this, Chatbot.class);
            chatbotIntent.putExtra("selected_date", dashboardDate);
            startActivity(chatbotIntent);
        });
    }

    /**
     * Generates a random integer between min (inclusive) and max (inclusive).
     *
     * @param min The minimum value of the range (inclusive).
     * @param max The maximum value of the range (inclusive).
     * @return A random integer between min and max.
     */
    public static int generateRandomNumber(int min, int max) {
        // Ensure that min is not greater than max
        if (min > max) {
            throw new IllegalArgumentException("Max must be greater than or equal to Min.");
        }

        // 1. Create a new Random object
        Random random = new Random();

        // 2. Calculate the range (max - min + 1)
        // The nextInt(n) method returns a random number from 0 (inclusive) to n (exclusive).
        // To get a number up to 'max' (inclusive), we need a range of (max - min + 1).
        int range = max - min + 1;

        // 3. Generate the random number
        // random.nextInt(range) gives a number from 0 to (range - 1).
        // Adding 'min' shifts this range to be from 'min' to 'max'.
        return random.nextInt(range) + min;
    }
}