package com.example.screamlarkbot.configs;

import com.example.screamlarkbot.utils.Emote;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
@EnableRetry
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
                .withEnableTMI(true)
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withChatAccount(credential)
                .withDefaultAuthToken(credential)
                .build();

        client.getClientHelper().enableFollowEventListener(channelName);
        client.getClientHelper().enableStreamEventListener(channelName);

        client.getChat().joinChannel(channelName);
        client.getChat().sendMessage(channelName, Emote.FROG_WAVE.toString());
        return client;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(getClientHttpRequestFactory());
    }

    private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        // Connect timeout
        clientHttpRequestFactory.setConnectTimeout(10_000);
        // Read timeout
        clientHttpRequestFactory.setReadTimeout(10_000);
        return clientHttpRequestFactory;
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("Bot-");
        executor.initialize();
        return executor;
    }
}
