package com.brightloop.subscriptionmanagement.service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Password reset email: HTML values are escaped, and SMTP mode sends from the configured address.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(emailService, "fromAddress", "billing@looppay.com");
    }

    @Test
    void htmlEmail_escapesTheCustomerName() {
        String html = ReflectionTestUtils.invokeMethod(emailService, "buildResetEmailHtml",
                "<a href=\"https://evil.example\">Click</a>", "http://localhost:5173/reset-password?token=abc");

        assertThat(html).doesNotContain("<a href=\"https://evil.example\">");
        assertThat(html).contains("&lt;a href=&quot;https://evil.example&quot;&gt;Click&lt;/a&gt;");
        assertThat(html).contains("http://localhost:5173/reset-password?token=abc");
    }

    @Test
    void smtpMode_sendsFromConfiguredAddress() throws Exception {
        ReflectionTestUtils.setField(emailService, "useConsole", false);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        emailService.sendPasswordResetEmail("asha@example.com", "Asha", "token-123");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(((InternetAddress) sent.getValue().getFrom()[0]).getAddress()).isEqualTo("billing@looppay.com");
        assertThat(sent.getValue().getAllRecipients()[0].toString()).isEqualTo("asha@example.com");
    }

    @Test
    void consoleMode_doesNotSendEmail() {
        ReflectionTestUtils.setField(emailService, "useConsole", true);

        emailService.sendPasswordResetEmail("asha@example.com", "Asha", "token-123");

        verifyNoInteractions(mailSender);
    }
}
