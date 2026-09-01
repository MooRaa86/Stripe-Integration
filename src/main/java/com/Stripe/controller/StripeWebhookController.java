package com.Stripe.controller;

import com.Stripe.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) throws StripeException {

        /*
         * The raw request body must be verified against the Stripe-Signature
         * header before the event payload is trusted.
         */
        final Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook signature verification failed");
            return ResponseEntity
                    .badRequest()
                    .body("Invalid signature");
        }

        stripeWebhookService.handleEvent(event);

        return ResponseEntity.ok("Webhook received");
    }
}
