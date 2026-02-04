package com.example.unbox_user.notification.application.service;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_user.user.domain.entity.User;
import com.example.unbox_user.user.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${mail.from:noreply@unbox.com}")
    private String fromEmail;

    public NotificationService(
            UserRepository userRepository,
            @Autowired(required = false) JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public void sendNotification(
            Long userId,
            String title,
            String message,
            String type,
            String referenceId) {

        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("📢 Sending notification to userId={}: title={}, message={}",
                userId, title, message);

        // 2. 메일 발송 (mailSender가 없으면 스킵)
        if (mailSender == null) {
            log.warn("⚠️ JavaMailSender is not configured. Skipping email notification.");
            return;
        }

        // 3. 이메일 발송
        try {
            sendEmail(user.getEmail(), title, message, type, referenceId);
            log.info("✅ Email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
            // 이메일 발송 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    private void sendEmail(String to, String title, String message, String type, String referenceId)
            throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(title);

        // HTML 이메일 템플릿
        String htmlContent = buildEmailTemplate(title, message, type, referenceId);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    private String buildEmailTemplate(String title, String message, String type, String referenceId) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                        .button {
                            display: inline-block;
                            padding: 10px 20px;
                            background-color: #4CAF50;
                            color: white;
                            text-decoration: none;
                            border-radius: 5px;
                            margin-top: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>UNBOX 알림</h1>
                        </div>
                        <div class="content">
                            <h2>%s</h2>
                            <p>%s</p>
                            <p><strong>참조 ID:</strong> %s</p>
                            <p><strong>알림 유형:</strong> %s</p>
                        </div>
                        <div class="footer">
                            <p>본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
                            <p>&copy; 2026 UNBOX. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(title, message, referenceId, type);
    }
}
