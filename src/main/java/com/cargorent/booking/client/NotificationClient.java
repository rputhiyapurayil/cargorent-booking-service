package com.cargorent.booking.client;

import com.cargorent.booking.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Best effort: a notification failure must never fail the booking action itself. */
@Slf4j
@Component
public class NotificationClient {

    private final RestClient rest;

    public NotificationClient(RestClient.Builder builder, @Value("${services.notification.url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    public void send(Long userId, String type, String title, String message, Long bookingId) {
        try {
            rest.post().uri("/notifications")
                    .body(new NotificationRequest(userId, type, title, message, bookingId))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Could not send {} notification for booking {}: {}", type, bookingId, e.getMessage());
        }
    }
}
