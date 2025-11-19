package net.saadbr.saadbrbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;

/**
 * @author saade
 **/
@Component
public class AIAgent {
    private ChatClient chatClient;
    public AIAgent(ChatClient.Builder builder, ChatMemory memory, ToolCallbackProvider tools) {
        Arrays.stream(tools.getToolCallbacks()).forEach(tool -> {
            System.out.println("-------------------");
            System.out.println(tool.getToolDefinition());
            System.out.println("-------------------");
        });
        this.chatClient = builder
                .defaultSystem("""
                        You are an assistant responsible for answering the user's questions based on the provided context.
                        If no context is provided, respond with: I DON'T KNOW.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .defaultToolCallbacks(tools)
                .build();
    }
    public String sendMessage(String message) {
        return chatClient.prompt()
                .user(message)
                .call().content();
    }
}
