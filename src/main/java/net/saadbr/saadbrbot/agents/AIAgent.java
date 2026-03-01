package net.saadbr.saadbrbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AIAgent {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public AIAgent(ChatClient.Builder builder,
                   ChatMemory memory,
                   ToolCallbackProvider tools,
                   VectorStore vectorStore) {

        this.vectorStore = vectorStore;

        Arrays.stream(tools.getToolCallbacks()).forEach(tool -> {
            System.out.println("-------------------");
            System.out.println(tool.getToolDefinition());
            System.out.println("-------------------");
        });

        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .defaultToolCallbacks(tools)
                .build();
    }

    public String sendMessage(Prompt prompt) {

        var first = prompt.getInstructions().isEmpty() ? null : prompt.getInstructions().get(0);
        String userText = (first == null) ? null : first.getText();

        boolean hasMedia = (first instanceof UserMessage um)
                && um.getMedia() != null
                && !um.getMedia().isEmpty();

        // 1) If user sent an image, DO NOT block on RAG
        if (hasMedia) {
            return chatClient.prompt(prompt).call().content();
        }

        // 2) No text -> nothing to do
        if (userText == null || userText.isBlank()) {
            return "I DON'T KNOW";
        }

        // 3) Try RAG retrieval
        var top = vectorStore.similaritySearch(userText).stream()
                .limit(5)
                .toList();

        String context = top.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n---\n" + b)
                .trim();

        // 4) If no context, fallback to normal LLM so "simple queries" still work
        if (context.isBlank()) {
            return chatClient.prompt(prompt).call().content();
        }

        // 5) Answer using ONLY retrieved context
        String answer = chatClient.prompt()
                .system("""
                        You must answer using ONLY the CONTEXT.
                        If the answer is not in the context, say: I DON'T KNOW.
                        """)
                .user("""
                        CONTEXT:
                        %s

                        QUESTION:
                        %s
                        """.formatted(context, userText))
                .call()
                .content();

        return (answer == null || answer.isBlank()) ? "I DON'T KNOW" : answer;
    }
}