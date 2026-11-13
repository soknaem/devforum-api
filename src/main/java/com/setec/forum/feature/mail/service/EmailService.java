package com.setec.forum.feature.mail.service;

public interface EmailService {
    void send(String to, String subject, String body);
}
