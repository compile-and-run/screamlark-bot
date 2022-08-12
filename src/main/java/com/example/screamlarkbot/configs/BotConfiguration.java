package com.example.screamlarkbot.configs;

import com.example.screamlarkbot.utils.Emotes;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class BotConfiguration {

    @Value("${screamlark-bot.channel-name}")
    private String channelName;
    @Value("${screamlark-bot.access-token}")
    private String accessToken;
    @Value("${screamlark-bot.client-id}")
    private String clientId;
    @Value("${screamlark-bot.client-secret}")
    private String clientSecret;

    @Bean
    public TwitchClient twitchClient() {
        OAuth2Credential credential = new OAuth2Credential("twitch", accessToken);
        var client = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnableChat(true)
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withChatAccount(credential)
                .withDefaultAuthToken(credential)
                .build();
        client.getChat().joinChannel(channelName);
        client.getChat().sendMessage(channelName, Emotes.FROG_WAVE.getName());
        client.getClientHelper().enableFollowEventListener(channelName);
        client.getClientHelper().enableStreamEventListener(channelName);
        return client;
    }
}
