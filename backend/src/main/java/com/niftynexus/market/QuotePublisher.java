package com.niftynexus.market;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fans quotes out over STOMP to every connected browser (one feed, many clients). */
@Component
public class QuotePublisher {
    private final SimpMessagingTemplate template;
    public QuotePublisher(SimpMessagingTemplate template) { this.template = template; }

    public void publish(List<Quote> quotes) {
        if (quotes.isEmpty()) return;
        template.convertAndSend("/topic/quotes", quotes);
        for (Quote q : quotes) template.convertAndSend("/topic/quotes." + q.symbol().replace(' ', '_'), q);
    }
    public void publishStatus(FeedStatus status) { template.convertAndSend("/topic/feed-status", status); }
    public void notifyUser(Long userId, Object payload) { template.convertAndSend("/topic/user." + userId, payload); }
}
