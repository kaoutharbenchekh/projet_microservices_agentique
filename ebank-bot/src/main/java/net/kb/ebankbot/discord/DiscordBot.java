package net.kb.ebankbot.discord;

import com.zgamelogic.discord.annotations.DiscordController;
import com.zgamelogic.discord.annotations.DiscordMapping;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kb.ebankbot.agents.EbankAIAgent;

@DiscordController
public class DiscordBot {
    private EbankAIAgent ebankAIAgent;

    public DiscordBot(EbankAIAgent ebankAIAgent) {
        this.ebankAIAgent = ebankAIAgent;
    }

    @DiscordMapping
    private void perform(MessageReceivedEvent event){
        System.out.println(event);
        if (event.getAuthor().isBot()) return;
        String query = event.getMessage().getContentRaw();
        // Un historique indépendant pour chaque utilisateur Discord
        String conversationId = "discord-user-" + event.getAuthor().getId();

        try {
            String response = ebankAIAgent.chat(query, conversationId);

            if (response != null && !response.isBlank()) {
                event.getChannel()
                        .sendMessage(response)
                        .queue();
            }

        } catch (Exception e) {
            event.getChannel()
                    .sendMessage("Une erreur est survenue lors du traitement de votre demande.")
                    .queue();

            e.printStackTrace();
        }
    }
}
