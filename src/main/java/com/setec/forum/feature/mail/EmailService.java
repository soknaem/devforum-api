package com.setec.forum.feature.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text e-mail through SMTP when it is configured (spring.mail.host in application.yml).
 *
 * When SMTP is NOT configured - the normal situation on a developer machine - nothing is sent and the
 * whole message, including the reset link, is written to the application log instead, so the
 * "forgot password" flow can be demonstrated without a mail server.
 */
@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.from:no-reply@devforum.local}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("""

                    ===== E-MAIL (SMTP not configured - printed instead of sent) =====
                    To:      {}
                    Subject: {}

                    {}
                    ==================================================================
                    """, to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (Exception ex) {
            // Never break the request because the mail server is down.
            log.warn("Could not send e-mail to {}: {}", to, ex.getMessage());
        }
    }
}
