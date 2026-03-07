package net.saadbr.saadbrbot.agents;

import net.saadbr.saadbrbot.tools.EcomTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author saade
 **/
@Component
public class AIAgent {

    private static final String DONT_KNOW = "I DON'T KNOW";

    private static final Pattern CUSTOMER_ID = Pattern.compile("(?i)\\bcustomer\\s+(\\d+)\\b");
    private static final Pattern PRODUCT_ID  = Pattern.compile("(?i)\\bproduct\\s+(\\d+)\\b");
    private static final Pattern BILL_ID     = Pattern.compile("(?i)\\bbill\\s+(\\d+)\\b");

    private static boolean isListCustomers(String s) {
        String t = s.trim().toLowerCase();
        return t.equals("customers") || t.equals("list customers") || t.equals("show customers")
                || t.contains("list customers") || t.contains("show customers");
    }

    private static boolean isListProducts(String s) {
        String t = s.trim().toLowerCase();
        return t.equals("products") || t.equals("list products") || t.equals("show products")
                || t.contains("list products") || t.contains("show products");
    }

    private static boolean isListBills(String s) {
        String t = s.trim().toLowerCase();
        return t.equals("bills") || t.equals("list bills") || t.equals("show bills")
                || t.contains("list bills") || t.contains("show bills");
    }

    private static final String SYS_ECOM_ASSISTANT = """
            You are an assistant inside an e-commerce microservices system.

            Rules:
            1) If the question needs LIVE business data (customers, products, bills), use the available tools or live data provided by the application.
               When you decide to use a tool, call it immediately (do not ask for permission).
            2) Otherwise, use the provided CONTEXT (RAG) when relevant.
            3) If you still cannot answer after using tools and/or context, say: I DON'T KNOW.
            """;

    private static final String SYS_FORMAT_LIVE = """
            You are an assistant for an e-commerce system.
            The following is LIVE data returned by microservices.

            Rules:
            - Summarize clearly and concisely.
            - Do not invent fields that are not present.
            - Ignore HAL links like "_links" unless the user asks for them.
            - If it's a list (HAL), show up to 10 items with the most useful fields you can find.
            """;

    private static final int RAG_TOP_K = 5;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final EcomTools ecomTools;

    public AIAgent(ChatClient.Builder builder,
                   ChatMemory memory,
                   ToolCallbackProvider tools,
                   VectorStore vectorStore,
                   EcomTools ecomTools) {

        this.vectorStore = vectorStore;
        this.ecomTools = ecomTools;

        ToolCallback[] ecomOnly = Arrays.stream(tools.getToolCallbacks())
                .filter(tc -> {
                    String name = tc.getToolDefinition().name();
                    return name.equals("getCustomerById")
                            || name.equals("listCustomers")
                            || name.equals("getProductById")
                            || name.equals("listProducts")
                            || name.equals("getBillById")
                            || name.equals("listBills");
                })
                .toArray(ToolCallback[]::new);

        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .defaultToolCallbacks(ecomOnly)
                .build();
    }

    public String sendMessage(Prompt prompt) {

        var first = prompt.getInstructions().isEmpty() ? null : prompt.getInstructions().get(0);
        String userText = (first == null) ? null : first.getText();

        boolean hasMedia = (first instanceof UserMessage um)
                && um.getMedia() != null
                && !um.getMedia().isEmpty();

        if (hasMedia) {
            return ask(prompt);
        }

        if (userText == null || userText.isBlank()) {
            return DONT_KNOW;
        }

        Optional<String> routed = routeLiveData(userText);
        if (routed.isPresent()) {
            return routed.get();
        }

        String context = buildRagContext(userText);

        if (context.isBlank()) {
            return ask(SYS_ECOM_ASSISTANT, userText);
        }

        String answer = ask(
                SYS_ECOM_ASSISTANT,
                """
                CONTEXT (may be empty or partial):
                %s

                USER QUESTION:
                %s
                """.formatted(context, userText)
        );

        return (answer == null || answer.isBlank()) ? DONT_KNOW : answer;
    }

    private Optional<String> routeLiveData(String userText) {

        if (isListCustomers(userText)) {
            return Optional.of(formatLive("CUSTOMERS", ecomTools.listCustomers()));
        }
        if (isListProducts(userText)) {
            return Optional.of(formatLive("PRODUCTS", ecomTools.listProducts()));
        }

        Matcher cm = CUSTOMER_ID.matcher(userText);
        if (cm.find()) {
            long id = Long.parseLong(cm.group(1));
            return Optional.of(formatLive("CUSTOMER", ecomTools.getCustomerById(id)));
        }

        Matcher pm = PRODUCT_ID.matcher(userText);
        if (pm.find()) {
            long id = Long.parseLong(pm.group(1));
            return Optional.of(formatLive("PRODUCT", ecomTools.getProductById(id)));
        }

        Matcher bm = BILL_ID.matcher(userText);
        if (bm.find()) {
            long id = Long.parseLong(bm.group(1));
            return Optional.of(formatLive("BILL", ecomTools.getBillById(id)));
        }

        return Optional.empty();
    }

    private String formatLive(String type, Map<String, Object> payload) {
        String formatted = ask(SYS_FORMAT_LIVE, type + " JSON: " + payload);
        if (formatted == null || formatted.isBlank()) {
            return payload.toString();
        }
        return formatted;
    }

    private String buildRagContext(String userText) {
        List<Document> top = vectorStore.similaritySearch(userText).stream()
                .limit(RAG_TOP_K)
                .toList();

        return top.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n---\n" + b)
                .trim();
    }

    private String ask(Prompt prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    private String ask(String system, String user) {
        return chatClient.prompt()
                .system(system)
                .user(user)
                .call()
                .content();
    }
}