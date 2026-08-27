package com.neueda.leap.merchantportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class MerchantController {

    @Autowired
    private PayoutRepository payoutRepository;

    @GetMapping("/api/payouts/{payoutId}")
    public ResponseEntity<PayoutRequest> getPayout(@PathVariable Long payoutId,
                                                    @AuthenticationPrincipal UserDetails user) {
            PayoutRequest payout = payoutRepository.findById(payoutId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            Long merchantID = Long.valueOf(user.getUsername());
            if (!payout.getMerchantId().equals(merchantID)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            return ResponseEntity.ok(payout);
    }
}
