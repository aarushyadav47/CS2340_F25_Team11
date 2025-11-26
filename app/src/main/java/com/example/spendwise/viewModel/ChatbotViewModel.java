package com.example.spendwise.viewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.command.ChatCommand;
import com.example.spendwise.command.WeeklySpendingCommand;
import com.example.spendwise.command.CutCostsCommand;
import com.example.spendwise.command.MonthlyComparisonCommand;
import com.example.spendwise.command.BudgetQueryCommand;
import com.example.spendwise.command.ExpenseQueryCommand;
import com.example.spendwise.command.SavingCircleQueryCommand;
import com.example.spendwise.model.ChatMessage;
import com.example.spendwise.model.ChatSession;
import com.example.spendwise.util.NotificationConstants;
import com.example.spendwise.view.Network;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel for managing AI chatbot interactions and chat session persistence.
 * Implements MVVM architecture with Command Pattern for extensible commands.
 * Handles AI message processing, session management, and context-aware financial insights.
 */
public class ChatbotViewModel extends ViewModel {

    /** LiveData for chat messages list - observed by UI for real-time updates */
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    
    /** LiveData for loading state - indicates when AI is processing a request */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    
    /** LiveData for status messages - used for error notifications */
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");

    /** Network instance for making API calls to Ollama */
    private final Network network = new Network();
    
    /** Firebase database reference for chat session persistence */
    private final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    /**
     * Retrieves the current authenticated user's ID from Firebase Auth.
     * 
     * @return The user's unique ID if authenticated, otherwise a fallback ID for testing
     */
    private String getCurrentUserId() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        // Fallback to hardcoded ID if no user is logged in
        return "hlGiIdCw3vWSJP0kI0nZdhY26Kl2";
    }

    /** Current active chat session ID - null when starting a new session */
    private String currentSessionId = null;
    
    /** Flag indicating if this is a new chat session */
    private boolean isNewSession = true;
    
    /** Command Pattern: List of available chatbot commands for extensible command handling */
    private final List<ChatCommand> commands;
    
    /** Flag to track if title generation is in progress for current session */
    private boolean isGeneratingTitle = false;

    /**
     * Constructor initializes the Command Pattern command list.
     * Sets up all available chatbot commands for handling user queries.
     */
    public ChatbotViewModel() {
        // Initialize Command Pattern commands
        commands = Arrays.asList(
            new WeeklySpendingCommand(),
            new CutCostsCommand(),
            new MonthlyComparisonCommand(),
            new BudgetQueryCommand(),
            new ExpenseQueryCommand(),
            new SavingCircleQueryCommand()
        );
    }

    /**
     * Callback interface for retrieving previous chat sessions asynchronously.
     */
    public interface PreviousChatsCallback {
        /**
         * Called when previous chat sessions are retrieved from Firebase.
         * 
         * @param previousSessions List of previous chat sessions, ordered by timestamp
         */
        void onResult(List<ChatSession> previousSessions);
    }

    /**
     * Gets the LiveData stream of chat messages for UI observation.
     * 
     * @return LiveData containing the list of chat messages
     */
    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    /**
     * Gets the LiveData stream for loading state.
     * 
     * @return LiveData indicating if AI is currently processing a request
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Gets the LiveData stream for status messages (errors, notifications).
     * 
     * @return LiveData containing status message strings
     */
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sends a user message to the chatbot and processes the response.
     * Handles session creation, Command Pattern matching, and AI API calls.
     * 
     * @param userMessage The user's message to send
     * @param previousSessionId Optional session ID to continue previous conversation
     */
    public void sendMessage(String userMessage, String previousSessionId) {
        if (userMessage.trim().isEmpty()) return;

        addMessage(new ChatMessage("user", userMessage));

        String currentUserId = getCurrentUserId();

        // If continuing previous session, use that ID
        if (previousSessionId != null) {
            currentSessionId = previousSessionId;
            isNewSession = false;
        } else if (isNewSession) {
            // Create new session only on first message
            currentSessionId = dbRef.child("users")
                    .child(currentUserId)
                    .child("chatSessions")
                    .push()
                    .getKey();
            isNewSession = false; // Mark that we've created a session
        }

        // Command Pattern: Check if message matches any command
        for (ChatCommand command : commands) {
            if (command.matches(userMessage)) {
                command.execute(this, userMessage);
                return;
            }
        }
        
        // Handle monthly spending separately (not covered by WeeklySpendingCommand)
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("how much") && lowerMessage.contains("spend") && lowerMessage.contains("month") 
                && !lowerMessage.contains("week")) {
            computeMonthlySpending(userMessage);
            return;
        }

        // Normal API call
        isLoading.setValue(true);
        network.chat(userMessage, new Network.Callback() {
            @Override
            public void onSuccess(String reply) {
                addMessage(new ChatMessage("ai", reply));
                updateOrSaveSession(userMessage, reply);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                addMessage(new ChatMessage("ai", NotificationConstants.LLAMA_SLEEPING_MESSAGE));
                statusMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Adds a chat message to the current message list and notifies observers.
     * 
     * @param msg The chat message to add (user or AI message)
     */
    private void addMessage(ChatMessage msg) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(msg);
        messages.postValue(current);
    }

    /**
     * Generates a chat title using AI API based on the first user message.
     * 
     * <p>This method calls the Ollama API to generate a concise, descriptive title
     * (max 5 words) for the chat session based on the user's first message.
     * Falls back to a truncated first message if AI generation fails.
     * 
     * @param firstUserMessage The first user message in the session
     * @param callback Callback to receive the generated title
     */
    private void generateChatTitle(String firstUserMessage, TitleGenerationCallback callback) {
        String prompt = "Generate a short, descriptive title (maximum 5 words) for this conversation: \"" 
                + firstUserMessage + "\". Return only the title, nothing else.";
        
        network.chat(prompt, new Network.Callback() {
            @Override
            public void onSuccess(String aiTitle) {
                // Clean up the title - remove quotes, extra whitespace
                String cleanTitle = aiTitle.trim()
                        .replaceAll("^[\"']|[\"']$", "") // Remove surrounding quotes
                        .replaceAll("\\s+", " ") // Normalize whitespace
                        .trim();
                
                // Ensure max 50 characters
                if (cleanTitle.length() > 50) {
                    cleanTitle = cleanTitle.substring(0, 47) + "...";
                }
                
                // Fallback if title is empty or too short
                if (cleanTitle.isEmpty() || cleanTitle.length() < 3) {
                    cleanTitle = firstUserMessage.length() > 50 
                            ? firstUserMessage.substring(0, 47) + "..." 
                            : firstUserMessage;
                }
                
                callback.onTitleGenerated(cleanTitle);
            }

            @Override
            public void onError(String error) {
                // Fallback to truncated first message if AI fails
                Log.w("ChatbotViewModel", "AI title generation failed, using fallback: " + error);
                String fallbackTitle = firstUserMessage.length() > 50 
                        ? firstUserMessage.substring(0, 47) + "..." 
                        : firstUserMessage;
                callback.onTitleGenerated(fallbackTitle);
            }
        });
    }

    /**
     * Callback interface for AI title generation.
     */
    private interface TitleGenerationCallback {
        /**
         * Called when title generation completes (successfully or with fallback).
         * 
         * @param title The generated or fallback title
         */
        void onTitleGenerated(String title);
    }

    /**
     * Updates an existing chat session or saves a new one to Firebase.
     * 
     * <p>This method:
     * <ul>
     *   <li>Generates an AI-powered title for new sessions</li>
     *   <li>Creates a summary from the AI's last reply</li>
     *   <li>Persists all messages and session metadata to Firebase</li>
     * </ul>
     * 
     * @param userMessage The user's message that triggered this save
     * @param aiReply The AI's response to save
     */
    private void updateOrSaveSession(String userMessage, String aiReply) {
        if (currentSessionId == null) return;

        String currentUserId = getCurrentUserId();
        List<ChatMessage> allMessages = messages.getValue();
        if (allMessages == null || allMessages.isEmpty()) return;

        // Get first user message for title generation
        String firstUserMessage = userMessage;
        for (ChatMessage msg : allMessages) {
            if ("user".equals(msg.getRole())) {
                firstUserMessage = msg.getContent();
                break;
            }
        }

        // Generate summary from LAST AI reply
        String summary = aiReply.length() > 80 ? aiReply.substring(0, 77) + "..." : aiReply;

        // Check if this is the first AI reply (new session) - generate AI title
        boolean isFirstAIResponse = allMessages.stream()
                .filter(m -> "ai".equals(m.getRole()))
                .count() == 1;

        if (isFirstAIResponse && !isGeneratingTitle) {
            // Generate AI title for new session
            isGeneratingTitle = true;
            generateChatTitle(firstUserMessage, title -> {
                isGeneratingTitle = false;
                saveSessionToFirebase(currentUserId, title, summary, allMessages);
            });
        } else {
            // For continuing sessions, use existing title or generate fallback
            String title = firstUserMessage.length() > 50 
                    ? firstUserMessage.substring(0, 47) + "..." 
                    : firstUserMessage;
            saveSessionToFirebase(currentUserId, title, summary, allMessages);
        }
    }

    /**
     * Saves chat session data to Firebase database.
     * 
     * @param userId The current user's ID
     * @param title The session title (AI-generated or fallback)
     * @param summary The session summary from AI's last reply
     * @param allMessages All messages in the session
     */
    private void saveSessionToFirebase(String userId, String title, String summary, List<ChatMessage> allMessages) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("title", title);
        sessionData.put("summary", summary);
        sessionData.put("timestamp", System.currentTimeMillis());

        // Save ALL messages
        List<Map<String, String>> messageList = new ArrayList<>();
        for (ChatMessage msg : allMessages) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", msg.getRole());
            msgMap.put("content", msg.getContent());
            messageList.add(msgMap);
        }
        sessionData.put("messages", messageList);

        Log.d("ChatbotViewModel", "Saving session: " + currentSessionId + " with " + allMessages.size() + " messages");

        dbRef.child("users")
                .child(currentUserId)
                .child("chatSessions")
                .child(currentSessionId)
                .setValue(sessionData)
                .addOnSuccessListener(aVoid -> Log.d("ChatbotViewModel", "Session saved successfully"))
                .addOnFailureListener(e -> {
                    Log.e("ChatbotViewModel", "Failed to save session", e);
                    statusMessage.postValue("Failed to save session: " + e.getMessage());
                });
    }

    /**
     * Retrieves all previous chat sessions for the current user from Firebase.
     * 
     * <p>Sessions are returned in reverse chronological order (newest first).
     * Each session includes its title, summary, timestamp, and all messages.
     * 
     * @param callback Callback to receive the list of previous chat sessions
     */
    public void getPreviousChats(PreviousChatsCallback callback) {
        String currentUserId = getCurrentUserId();
        Log.d("ChatbotViewModel", "Fetching previous chats for user: " + currentUserId);

        dbRef.child("users").child(currentUserId).child("chatSessions")
                .get()
                .addOnCompleteListener(task -> {
                    List<ChatSession> sessions = new ArrayList<>();

                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("ChatbotViewModel", "Found " + task.getResult().getChildrenCount() + " sessions");

                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            String id = snapshot.getKey();
                            String title = snapshot.child("title").getValue(String.class);
                            String summary = snapshot.child("summary").getValue(String.class);
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                            Log.d("ChatbotViewModel", "Session: " + id + " - " + title);

                            if (id != null && title != null) {
                                ChatSession session = new ChatSession(
                                        id,
                                        title,
                                        summary != null ? summary : "",
                                        timestamp != null ? timestamp : 0
                                );

                                // Load messages
                                DataSnapshot messagesSnapshot = snapshot.child("messages");
                                for (DataSnapshot msgSnap : messagesSnapshot.getChildren()) {
                                    String role = msgSnap.child("role").getValue(String.class);
                                    String content = msgSnap.child("content").getValue(String.class);
                                    if (role != null && content != null) {
                                        session.addMessage(new ChatMessage(role, content));
                                    }
                                }

                                sessions.add(0, session); // Add at beginning for reverse chronological
                            }
                        }
                    } else {
                        Log.e("ChatbotViewModel", "Failed to fetch sessions", task.getException());
                    }

                    callback.onResult(sessions);
                });
    }

    /**
     * Loads a previous chat session into the current chat view.
     * 
     * <p>This method restores all messages from the selected session and sets
     * the current session ID to continue the conversation.
     * 
     * @param session The chat session to load
     */
    public void loadSession(ChatSession session) {
        currentSessionId = session.getId();
        isNewSession = false; // We're continuing an existing session
        messages.setValue(new ArrayList<>(session.getMessages()));
        Log.d("ChatbotViewModel", "Loaded session: " + currentSessionId + " with " + session.getMessages().size() + " messages");
    }

    /**
     * Starts a new chat session, clearing current messages and resetting session state.
     * 
     * <p>This method should be called when the user wants to begin a fresh conversation.
     * The next message sent will create a new session in Firebase.
     */
    public void startNewSession() {
        currentSessionId = null;
        isNewSession = true;
        messages.setValue(new ArrayList<>());
        Log.d("ChatbotViewModel", "Started new session");
    }

    // ========== CUSTOM INSIGHT COMMANDS ==========

    /**
     * Computes and summarizes weekly spending (last 7 days) by category.
     * Sends formatted data to AI for human-like summarization.
     * 
     * @param originalMessage The original user message that triggered this command
     */
    public void computeWeeklySpending(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();

        // Get last 7 days instead of "week starting Sunday"
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long weekStart = cal.getTimeInMillis();

        Log.d("ChatbotViewModel", "Fetching expenses from last 7 days: " + new Date(weekStart));
        Log.d("ChatbotViewModel", "Querying path: users/" + currentUserId + "/expenses");

        dbRef.child("users").child(currentUserId).child("expenses")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch expenses."));
                        isLoading.setValue(false);
                        Log.e("ChatbotViewModel", "Failed to fetch expenses", task.getException());
                        return;
                    }

                    Map<String, Double> categoryTotals = new HashMap<>();
                    double totalSpent = 0;
                    int expenseCount = 0;
                    int totalExpensesInDB = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                    Log.d("ChatbotViewModel", "Total expense records in DB: " + task.getResult().getChildrenCount());

                    for (DataSnapshot expense : task.getResult().getChildren()) {
                        totalExpensesInDB++;
                        String expenseId = expense.getKey();
                        Log.d("ChatbotViewModel", "Processing expense ID: " + expenseId);
                        Long timestamp = expense.child("timestamp").getValue(Long.class);
                        String dateStr = expense.child("date").getValue(String.class);
                        String category = expense.child("category").getValue(String.class);
                        Double amount = expense.child("amount").getValue(Double.class);
                        String name = expense.child("name").getValue(String.class);

                        Log.d("ChatbotViewModel", "  Raw data - name: " + name + ", category: " + category + ", amount: " + amount + ", date: " + dateStr);

                        // Parse date string if timestamp is null
                        if (timestamp == null && dateStr != null) {
                            try {
                                Date parsedDate = sdf.parse(dateStr);
                                if (parsedDate != null) {
                                    timestamp = parsedDate.getTime();
                                }
                            } catch (Exception e) {
                                Log.e("ChatbotViewModel", "Failed to parse date: " + dateStr, e);
                            }
                        }

                        Log.d("ChatbotViewModel", "  Processed - " + category + " - $" + amount + " at " + (timestamp != null ? new Date(timestamp) : "null") + " (date string: " + dateStr + ")");

                        // Filter by week in-memory
                        if (timestamp != null && timestamp >= weekStart && amount != null && category != null) {
                            totalSpent += amount;
                            categoryTotals.put(category,
                                    categoryTotals.getOrDefault(category, 0.0) + amount);
                            expenseCount++;
                            Log.d("ChatbotViewModel", "  ✓ INCLUDED in weekly total");
                        } else {
                            Log.d("ChatbotViewModel", "  ✗ EXCLUDED - timestamp: " + (timestamp != null) + ", inRange: " + (timestamp != null && timestamp >= weekStart) + ", hasAmount: " + (amount != null) + ", hasCategory: " + (category != null));
                        }
                    }

                    Log.d("ChatbotViewModel", "Processed " + totalExpensesInDB + " total expenses");
                    Log.d("ChatbotViewModel", "Total expenses this week: " + expenseCount + " = $" + totalSpent);

                    // Build data context for AI
                    StringBuilder dataContext = new StringBuilder();
                    dataContext.append("Last 7 days spending data:\n");
                    dataContext.append("Total: $").append(String.format("%.2f", totalSpent)).append("\n");
                    dataContext.append("Number of expenses: ").append(expenseCount).append("\n");
                    dataContext.append("By category:\n");
                    for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                        dataContext.append("- ").append(entry.getKey())
                                .append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
                    }

                    // Send to AI for human-like phrasing
                    String prompt = dataContext.toString() +
                            "\nPlease summarize the last 7 days of spending in a friendly, conversational way.";

                    callAIWithData(originalMessage, prompt);
                });
    }

    /**
     * Analyzes spending (last 30 days) and suggests cost reduction areas.
     * Compares spending against budgets and sends analysis to AI for suggestions.
     * 
     * @param originalMessage The original user message that triggered this command
     */
    public void suggestCostCutting(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();

        dbRef.child("users").child(currentUserId).child("expenses")
                .get().addOnCompleteListener(expenseTask -> {
                    dbRef.child("users").child(currentUserId).child("budgets")
                            .get().addOnCompleteListener(budgetTask -> {

                                if (!expenseTask.isSuccessful() || !budgetTask.isSuccessful()) {
                                    addMessage(new ChatMessage("ai", "😴 Could not fetch data."));
                                    isLoading.setValue(false);
                                    return;
                                }

                                Map<String, Double> spending = new HashMap<>();
                                Map<String, Double> budgets = new HashMap<>();
                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                                // Calculate spending by category (last 30 days)
                                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                                for (DataSnapshot expense : expenseTask.getResult().getChildren()) {
                                    Long timestamp = expense.child("timestamp").getValue(Long.class);
                                    String dateStr = expense.child("date").getValue(String.class);

                                    // Parse date string if timestamp is null
                                    if (timestamp == null && dateStr != null) {
                                        try {
                                            Date parsedDate = sdf.parse(dateStr);
                                            if (parsedDate != null) {
                                                timestamp = parsedDate.getTime();
                                            }
                                        } catch (Exception e) {
                                            Log.e("ChatbotViewModel", "Failed to parse date: " + dateStr, e);
                                        }
                                    }

                                    if (timestamp != null && timestamp >= thirtyDaysAgo) {
                                        String cat = expense.child("category").getValue(String.class);
                                        Double amt = expense.child("amount").getValue(Double.class);
                                        if (cat != null && amt != null) {
                                            spending.put(cat, spending.getOrDefault(cat, 0.0) + amt);
                                        }
                                    }
                                }

                                // Get budget limits
                                for (DataSnapshot budget : budgetTask.getResult().getChildren()) {
                                    String cat = budget.child("category").getValue(String.class);
                                    Double amt = budget.child("amount").getValue(Double.class);
                                    if (cat != null && amt != null) {
                                        budgets.put(cat, amt);
                                    }
                                }

                                // Build analysis
                                StringBuilder analysis = new StringBuilder();
                                analysis.append("Spending analysis (last 30 days):\n");

                                for (Map.Entry<String, Double> entry : spending.entrySet()) {
                                    String category = entry.getKey();
                                    double spent = entry.getValue();
                                    Double budget = budgets.get(category);

                                    analysis.append("- ").append(category).append(": $")
                                            .append(String.format("%.2f", spent));

                                    if (budget != null) {
                                        double percentage = (spent / budget) * 100;
                                        analysis.append(" (").append(String.format("%.0f", percentage))
                                                .append("% of $").append(String.format("%.2f", budget))
                                                .append(" budget)");
                                    }
                                    analysis.append("\n");
                                }

                                String prompt = analysis.toString() +
                                        "\nBased on this data, suggest specific areas where I can reduce spending. " +
                                        "Be practical and encouraging.";

                                callAIWithData(originalMessage, prompt);
                            });
                });
    }

    /**
     * Compares current month spending to previous month by category.
     * Calculates percentage changes and sends to AI for actionable insights.
     * 
     * @param originalMessage The original user message that triggered this command
     */
    public void compareToLastMonth(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();

        dbRef.child("users").child(currentUserId).child("expenses")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch expenses."));
                        isLoading.setValue(false);
                        return;
                    }

                    Calendar cal = Calendar.getInstance();

                    // Current month range
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    long currentMonthStart = cal.getTimeInMillis();

                    // Last month range
                    cal.add(Calendar.MONTH, -1);
                    long lastMonthStart = cal.getTimeInMillis();

                    double currentMonthTotal = 0;
                    double lastMonthTotal = 0;
                    Map<String, Double> currentCategories = new HashMap<>();
                    Map<String, Double> lastCategories = new HashMap<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                    for (DataSnapshot expense : task.getResult().getChildren()) {
                        Long timestamp = expense.child("timestamp").getValue(Long.class);
                        String dateStr = expense.child("date").getValue(String.class);
                        String category = expense.child("category").getValue(String.class);
                        Double amount = expense.child("amount").getValue(Double.class);

                        // Parse date string if timestamp is null
                        if (timestamp == null && dateStr != null) {
                            try {
                                Date parsedDate = sdf.parse(dateStr);
                                if (parsedDate != null) {
                                    timestamp = parsedDate.getTime();
                                }
                            } catch (Exception e) {
                                Log.e("ChatbotViewModel", "Failed to parse date: " + dateStr, e);
                            }
                        }

                        if (timestamp != null && amount != null && category != null) {
                            if (timestamp >= currentMonthStart) {
                                currentMonthTotal += amount;
                                currentCategories.put(category,
                                        currentCategories.getOrDefault(category, 0.0) + amount);
                            } else if (timestamp >= lastMonthStart && timestamp < currentMonthStart) {
                                lastMonthTotal += amount;
                                lastCategories.put(category,
                                        lastCategories.getOrDefault(category, 0.0) + amount);
                            }
                        }
                    }

                    StringBuilder comparison = new StringBuilder();
                    comparison.append("Month-over-month comparison:\n");
                    comparison.append("Last month total: $").append(String.format("%.2f", lastMonthTotal)).append("\n");
                    comparison.append("This month total: $").append(String.format("%.2f", currentMonthTotal)).append("\n");

                    double difference = currentMonthTotal - lastMonthTotal;
                    double percentChange = lastMonthTotal > 0 ? (difference / lastMonthTotal) * 100 : 0;

                    comparison.append("Difference: $").append(String.format("%.2f", difference))
                            .append(" (").append(String.format("%.1f", percentChange)).append("%)\n\n");

                    comparison.append("Category breakdown:\n");
                    for (String category : currentCategories.keySet()) {
                        double current = currentCategories.get(category);
                        double last = lastCategories.getOrDefault(category, 0.0);
                        double catDiff = current - last;

                        comparison.append("- ").append(category)
                                .append(": $").append(String.format("%.2f", current))
                                .append(" vs $").append(String.format("%.2f", last))
                                .append(" (").append(catDiff >= 0 ? "+" : "")
                                .append(String.format("%.2f", catDiff)).append(")\n");
                    }

                    String prompt = comparison.toString() +
                            "\nAnalyze my spending performance. Highlight improvements and areas of concern. " +
                            "Be specific and actionable.";

                    callAIWithData(originalMessage, prompt);
                });
    }

    /**
     * Helper method to call AI API with pre-computed data context.
     * 
     * <p>This method wraps the Network.chat() call and handles the response,
     * adding the AI reply to messages and saving the session.
     * 
     * @param originalMessage The original user message for session tracking
     * @param prompt The formatted prompt with data context to send to AI
     */
    private void callAIWithData(String originalMessage, String prompt) {
        network.chat(prompt, new Network.Callback() {
            @Override
            public void onSuccess(String reply) {
                addMessage(new ChatMessage("ai", reply));
                updateOrSaveSession(originalMessage, reply);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                addMessage(new ChatMessage("ai", NotificationConstants.LLAMA_NAPPING_MESSAGE));
                isLoading.postValue(false);
            }
        });
    }

    // ========== ORIGINAL FETCH METHODS ==========

    /**
     * Fetches user budgets from Firebase and sends them to AI with context.
     * 
     * <p>The AI receives budget data formatted as a readable list and can answer
     * questions about budgets using this context.
     * 
     * @param originalMessage The user's question about budgets
     */
    public void fetchBudgetsWithContext(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();
        dbRef.child("users").child(currentUserId).child("budgets")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch budgets."));
                        isLoading.setValue(false);
                    } else {
                        DataSnapshot snapshot = task.getResult();
                        StringBuilder budgetData = new StringBuilder("User's budget data:\n");
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            String category = child.child("category").getValue(String.class);
                            Double amount = child.child("amount").getValue(Double.class);
                            budgetData.append("- ").append(name).append(" (").append(category).append("): $")
                                    .append(String.format("%.2f", amount)).append("\n");
                        }

                        String prompt = budgetData.toString() + "\nUser question: " + originalMessage +
                                "\n\nPlease answer their question using the budget data above in a friendly way.";
                        callAIWithData(originalMessage, prompt);
                    }
                });
    }

    /**
     * Fetches recent user expenses from Firebase and sends them to AI with context.
     * 
     * <p>Limits to the 20 most recent expenses to avoid overwhelming the AI context.
     * The AI can answer questions about expenses using this data.
     * 
     * @param originalMessage The user's question about expenses
     */
    public void fetchExpensesWithContext(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();
        dbRef.child("users").child(currentUserId).child("expenses")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch expenses."));
                        isLoading.setValue(false);
                    } else {
                        DataSnapshot snapshot = task.getResult();
                        StringBuilder expenseData = new StringBuilder("User's recent expenses:\n");
                        int count = 0;
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            String category = child.child("category").getValue(String.class);
                            Double amount = child.child("amount").getValue(Double.class);
                            String dateStr = child.child("date").getValue(String.class);

                            expenseData.append("- ").append(name).append(" (").append(category).append("): $")
                                    .append(String.format("%.2f", amount));
                            if (dateStr != null) {
                                expenseData.append(" on ").append(dateStr);
                            }
                            expenseData.append("\n");

                            count++;
                            if (count >= 20) break; // Limit to 20 most recent
                        }

                        String prompt = expenseData.toString() + "\nUser question: " + originalMessage +
                                "\n\nPlease answer their question using the expense data above in a friendly way.";
                        callAIWithData(originalMessage, prompt);
                    }
                });
    }

    /**
     * Computes and summarizes monthly spending using database data.
     * 
     * <p>This method:
     * <ul>
     *   <li>Fetches expenses from the current month (starting from day 1)</li>
     *   <li>Calculates totals by category</li>
     *   <li>Sends formatted data to AI for human-like summarization</li>
     * </ul>
     * 
     * @param originalMessage The original user message that triggered this command
     */
    public void computeMonthlySpending(String originalMessage) {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();

        dbRef.child("users").child(currentUserId).child("expenses")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch expenses."));
                        isLoading.setValue(false);
                        return;
                    }

                    Map<String, Double> categoryTotals = new HashMap<>();
                    double totalSpent = 0;
                    int expenseCount = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                    for (DataSnapshot expense : task.getResult().getChildren()) {
                        Long timestamp = expense.child("timestamp").getValue(Long.class);
                        String dateStr = expense.child("date").getValue(String.class);
                        String category = expense.child("category").getValue(String.class);
                        Double amount = expense.child("amount").getValue(Double.class);

                        if (timestamp == null && dateStr != null) {
                            try {
                                Date parsedDate = sdf.parse(dateStr);
                                if (parsedDate != null) {
                                    timestamp = parsedDate.getTime();
                                }
                            } catch (Exception e) {
                                Log.e("ChatbotViewModel", "Failed to parse date: " + dateStr, e);
                            }
                        }

                        if (timestamp != null && timestamp >= monthStart && amount != null && category != null) {
                            totalSpent += amount;
                            categoryTotals.put(category,
                                    categoryTotals.getOrDefault(category, 0.0) + amount);
                            expenseCount++;
                        }
                    }

                    StringBuilder dataContext = new StringBuilder();
                    dataContext.append("This month's spending data:\n");
                    dataContext.append("Total: $").append(String.format("%.2f", totalSpent)).append("\n");
                    dataContext.append("Number of expenses: ").append(expenseCount).append("\n");
                    dataContext.append("By category:\n");
                    for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                        dataContext.append("- ").append(entry.getKey())
                                .append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
                    }

                    String prompt = dataContext.toString() +
                            "\nUser asked: \"" + originalMessage + "\"\n" +
                            "Please answer their question about this month's spending in a friendly way.";

                    callAIWithData(originalMessage, prompt);
                });
    }

    /**
     * Fetches budgets from Firebase and displays them directly (without AI processing).
     * 
     * <p>This is a legacy method that formats budgets as a simple text list.
     * Consider using fetchBudgetsWithContext() for AI-enhanced responses.
     */
    private void fetchBudgets() {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();
        dbRef.child("users").child(currentUserId).child("budgets")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch budgets."));
                        isLoading.setValue(false);
                    } else {
                        DataSnapshot snapshot = task.getResult();
                        StringBuilder reply = new StringBuilder("Your budgets:\n");
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            String category = child.child("category").getValue(String.class);
                            Double amount = child.child("amount").getValue(Double.class);
                            reply.append(name).append(" (").append(category).append("): $")
                                    .append(String.format("%.2f", amount)).append("\n");
                        }
                        addMessage(new ChatMessage("ai", reply.toString()));
                        isLoading.setValue(false);
                    }
                });
    }

    /**
     * Fetches recent expenses from Firebase and displays them directly (without AI processing).
     * 
     * <p>This is a legacy method that formats expenses as a simple text list.
     * Limits to 10 most recent expenses. Consider using fetchExpensesWithContext() 
     * for AI-enhanced responses.
     */
    private void fetchExpenses() {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();
        dbRef.child("users").child(currentUserId).child("expenses")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch expenses."));
                        isLoading.setValue(false);
                    } else {
                        DataSnapshot snapshot = task.getResult();
                        StringBuilder reply = new StringBuilder("Your recent expenses:\n");
                        int count = 0;
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String category = child.child("category").getValue(String.class);
                            Double amount = child.child("amount").getValue(Double.class);
                            Long timestamp = child.child("timestamp").getValue(Long.class);
                            String dateStr = child.child("date").getValue(String.class);

                            // Parse date string if timestamp is null
                            if (timestamp == null && dateStr != null) {
                                try {
                                    Date parsedDate = sdf.parse(dateStr);
                                    if (parsedDate != null) {
                                        timestamp = parsedDate.getTime();
                                    }
                                } catch (Exception e) {
                                    Log.e("ChatbotViewModel", "Failed to parse date: " + dateStr, e);
                                }
                            }

                            reply.append(category).append(": $")
                                    .append(String.format("%.2f", amount));
                            if (timestamp != null) {
                                reply.append(" (").append(new SimpleDateFormat("MMM dd", Locale.US).format(new Date(timestamp))).append(")");
                            } else if (dateStr != null) {
                                reply.append(" (").append(dateStr).append(")");
                            }
                            reply.append("\n");
                            count++;
                            if (count >= 10) break; // Limit to 10 most recent
                        }
                        addMessage(new ChatMessage("ai", reply.toString()));
                        isLoading.setValue(false);
                    }
                });
    }

    /**
     * Fetches saving circles from Firebase and displays them directly.
     * 
     * <p>This method retrieves all saving circles for the current user and
     * formats them as a simple text list showing name and total amount.
     */
    public void fetchSavingCircles() {
        isLoading.setValue(true);
        String currentUserId = getCurrentUserId();
        dbRef.child("users").child(currentUserId).child("savingCircles")
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        addMessage(new ChatMessage("ai", "😴 Could not fetch saving circles."));
                        isLoading.setValue(false);
                    } else {
                        DataSnapshot snapshot = task.getResult();
                        StringBuilder reply = new StringBuilder("Your saving circles:\n");
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            Double total = child.child("total").getValue(Double.class);
                            reply.append(name).append(": $")
                                    .append(String.format("%.2f", total)).append("\n");
                        }
                        addMessage(new ChatMessage("ai", reply.toString()));
                        isLoading.setValue(false);
                    }
                });
    }
}