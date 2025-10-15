package com.celcoin.disbursement.controller;

import com.celcoin.disbursement.model.dto.ExternalRequestResponse;
import com.celcoin.disbursement.service.DisbursementNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private DisbursementNotificationService notificationService;

    @PostMapping("/pix")
    public ResponseEntity<?> handlePixNotification(@RequestBody ExternalRequestResponse request,
                                                   // here we would implement a HMAC signature that would be checked to validate
                                                   // the request owner
                                                   @RequestHeader(name = "X-Signature", required = false) String signature) {
        notificationService.processPixResponse(request);

        return ResponseEntity.ok().build();
    }
}
