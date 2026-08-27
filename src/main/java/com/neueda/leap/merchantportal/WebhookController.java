package com.neueda.leap.merchantportal;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@RestController
public class WebhookController {

    @Autowired
    private PayoutStatusUpdater payoutStatusUpdater;

    @Value("${webhook.secret.key:default-webhook-secret}")
    private String secret;

    @PostMapping("/api/webhooks/payment-status")
    public ResponseEntity<Void> handlePaymentStatusWebhook(
            @RequestBody PaymentStatusEvent event,
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestHeader("X-Webhook-Timestamp") String timestamp) {
        
        try {
            // Validate timestamp (prevent replay attacks)
            if (!isValidTimestamp(timestamp)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Verify signature
            String message = event.getPayoutId() + ":" + event.getStatus() + ":" + timestamp;
            if (!verifySignature(message, signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Process webhook
            payoutStatusUpdater.markSettled(event.getPayoutId(), event.getStatus());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean verifySignature(String message, String signature) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] computedSignature = hmac.doFinal(message.getBytes());
        String computedHex = HexFormat.of().formatHex(computedSignature);
        return computedHex.equals(signature);
    }

    private boolean isValidTimestamp(String timestamp) {
        try {
            long webhookTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            return Math.abs(currentTime - webhookTime) < 300; // 5-minute window
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
