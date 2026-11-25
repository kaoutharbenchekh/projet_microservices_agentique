package net.kb.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.messages.UserMessage;

@Service
public class EbankAIAgent {

    private final ChatClient chatClient;

    public EbankAIAgent(ChatClient.Builder chatClientBuilder,
                        ChatMemory chatMemory,
                        ToolCallbackProvider tools) {

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Vous êtes un assistant bancaire chargé de répondre
                        aux questions de l'utilisateur concernant les clients
                        et les comptes bancaires.

                        Vous devez utiliser exclusivement les outils disponibles
                        et le contexte fourni pour répondre.

                        Si les informations nécessaires ne sont pas disponibles,
                        répondez exactement :
                        JE NE SAIS PAS
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .build();
    }

    public String chat(String query, String conversationId) {

        return chatClient.prompt()
                .user(query)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .call()
                .content();
    }

    public String chat(UserMessage userMessage, String conversationId) {

        return chatClient.prompt()
                .messages(userMessage)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId
                ))
                .call()
                .content();
    }

}
