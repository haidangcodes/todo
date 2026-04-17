package com.todo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminderEmail(String toEmail, String taskTitle, String priority, String deadline) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Email sender not configured (spring.mail.username is empty)");
            return;
        }

        String subject = "🔔 Nhắc nhở: " + taskTitle;
        String body = String.format(
            "Nhắc nhở task\n\nTask: %s\nĐộ ưu tiên: %s\nDeadline: %s\n\nXem chi tiết tại: http://localhost:8080/app",
            taskTitle, priority, deadline != null ? deadline : "Không có"
        );

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(toEmail);
        msg.setSubject(subject);
        msg.setText(body);

        try {
            mailSender.send(msg);
            log.info("Reminder email sent to {} for task: {}", toEmail, taskTitle);
        } catch (Exception e) {
            log.error("Failed to send reminder email to {}: {}", toEmail, e.getMessage());
        }
    }
}
