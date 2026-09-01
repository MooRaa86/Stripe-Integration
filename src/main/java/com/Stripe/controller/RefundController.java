package com.Stripe.controller;

import com.Stripe.service.RefundService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/payments/{paymentId}")
    public ResponseEntity<String> refundPayment(
            @PathVariable UUID paymentId,
            @RequestParam BigDecimal amount
    ) throws StripeException {

        String refundId =
                refundService.refundPayment(
                        paymentId,
                        amount
                );

        return ResponseEntity.ok(
                "Refund created: " + refundId
        );
    }
}