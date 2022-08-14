package com.example.screamlarkbot.handlers;

import com.example.screamlarkbot.models.fight.DuelRequest;
import com.example.screamlarkbot.models.fight.Fighter;
import com.example.screamlarkbot.services.FightService;
import com.example.screamlarkbot.utils.Emotes;
import com.example.screamlarkbot.utils.Messages;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class FightMessageHandler {

    private static final String DUEL_COMMAND = "!duel";

    private final TwitchClient twitchClient;

    private final FightService fightService;

    @Value("${screamlark-bot.channel-name}")
    private String channelName;

    @PostConstruct
    public void init() {
        EventManager eventManager = twitchClient.getEventManager();
        eventManager.onEvent(ChannelMessageEvent.class, this::processDuelCommand);
        eventManager.onEvent(ChannelMessageEvent.class, this::processPunchCommand);
        fightService.setOnTimeUp(() -> twitchClient.getChat().sendMessage(channelName, "Время дуэли вышло " + Emotes.FEELS_WEAK_MAN));
    }

    private void processDuelCommand(ChannelMessageEvent event) {
        String username = event.getUser().getName();
        String message = event.getMessage();

        if (message.startsWith(DUEL_COMMAND)) {
            String[] words = message.split(" ");
            if (words.length != 2) return;
            String opponent = words[1].trim().toLowerCase();
            if (opponent.startsWith("@")) {
                opponent = opponent.substring(1);
            }

            if (username.equals(opponent)) {
                twitchClient.getChat().sendMessage(channelName, Messages.reply(username, "MMMM"));
                return;
            }

            fightService.acceptRequestOrAdd(new DuelRequest(username, opponent),
                    request -> onAccept(channelName, request),
                    request -> onAdd(channelName, request));
        }
    }

    private void onAccept(String channelName, DuelRequest duelRequest) {
        String response = "%s принял вызов от %s! Да начнется битва! " + Emotes.FIGHT + " " + Emotes.FIGHT2;
        response = String.format(response, duelRequest.getOpponent(), duelRequest.getRequester());
        twitchClient.getChat().sendMessage(channelName, response);

        response = "Используйте эти смайлы, чтобы ударить: %s %s %s";
        response = String.format(response, Emotes.FIGHT, Emotes.FIGHT2, Emotes.STREAML_SMASH);
        twitchClient.getChat().sendMessage(channelName, response);
    }

    private void onAdd(String channelName, DuelRequest duelRequest) {
        String response = "%s вызывает тебя на дуэль! Отправь %s %s в чат, чтобы принять вызов! " + Emotes.FIGHT;
        response = String.format(response, duelRequest.getRequester(), DUEL_COMMAND, duelRequest.getRequester());
        twitchClient.getChat().sendMessage(channelName, Messages.reply(duelRequest.getOpponent(), response));
    }

    private void processPunchCommand(ChannelMessageEvent event) {
        String username = event.getUser().getName();
        String message = event.getMessage();

        if (!fightService.isFighter(username)) {
            return;
        }

        if (message.contains(Emotes.FIGHT.toString()) || message.contains(Emotes.FIGHT2.toString())
                || message.contains(Emotes.STREAML_SMASH.toString())) {
            fightService.punch(username,
                    (f1, f2, d) -> onDamage(channelName, f1, f2, d),
                    (f1, f2) -> onKnockout(channelName, f1, f2));
        }
    }

    private void onDamage(String channelName, Fighter puncher, Fighter fighter, int damage) {
        String response = "%s(%s) ударил %s(%s) и отнял %s hp!";
        response = String.format(response, puncher.getUsername(), puncher.getHp(), fighter.getUsername(), fighter.getHp(), damage);
        twitchClient.getChat().sendMessage(channelName, response);
    }

    private void onKnockout(String channelName, Fighter winner, Fighter looser) {
        String response = "%s нокаутировал %s! " + Emotes.OOOO;
        response = String.format(response, winner.getUsername(), looser.getUsername());
        twitchClient.getChat().sendMessage(channelName, response);

        int time = ThreadLocalRandom.current().nextInt(1, 6);
        twitchClient.getChat().timeout(channelName, looser.getUsername(), Duration.ofMinutes(time), "");
    }
}