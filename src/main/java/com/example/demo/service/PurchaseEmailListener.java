package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PurchaseEmailListener {
    private static final Logger log = LoggerFactory.getLogger(PurchaseEmailListener.class);
    private final EmailService emails;

    public PurchaseEmailListener(EmailService emails) {
        this.emails = emails;
    }

    @TransactionalEventListener
    public void onPurchaseCompleted(PurchaseCompleted event) {
        log.info("Sending purchase confirmation for order {}", event.order().id());
        try {
            emails.sendPurchaseCompleted(event);
            log.info("SMTP accepted purchase confirmation for order {}", event.order().id());
        } catch (MailException exception) {
            // The order is already committed; an SMTP failure must not undo it.
            log.error("Could not send purchase confirmation for order {}", event.order().id(), exception);
        }
    }
}
