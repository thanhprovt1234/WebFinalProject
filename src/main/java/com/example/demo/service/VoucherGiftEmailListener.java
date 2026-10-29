package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VoucherGiftEmailListener {
    private static final Logger log = LoggerFactory.getLogger(VoucherGiftEmailListener.class);
    private final EmailService emails;

    public VoucherGiftEmailListener(EmailService emails) { this.emails = emails; }

    @TransactionalEventListener
    public void onVoucherGifted(VoucherGifted event) {
        try {
            emails.sendVoucherGifted(event);
            log.info("SMTP accepted gift notification for voucher {}", event.voucherId());
        } catch (MailException exception) {
            // The gift is committed. A failed notification must not undo it or stop other recipients.
            log.error("Could not send gift notification for voucher {}", event.voucherId(), exception);
        }
    }
}
