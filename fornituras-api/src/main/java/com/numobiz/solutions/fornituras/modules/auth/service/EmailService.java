package com.numobiz.solutions.fornituras.modules.auth.service;

import com.numobiz.solutions.fornituras.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final MailProperties mailProperties;

	@Value("${spring.mail.username:}")
	private String mailUsername;

	public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, MailProperties mailProperties) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
		this.mailProperties = mailProperties;
	}

	@Async
	public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
		try {
			String fromAddress = resolveFromAddress();
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(to);
			helper.setSubject(subject);

			Context context = new Context();
			if (variables != null) {
				variables.forEach(context::setVariable);
			}
			String html = templateEngine.process(templateName, context);
			helper.setText(html, true);

			mailSender.send(message);
			log.info("Email sent to {} subject: {}", to, subject);
		} catch (MessagingException ex) {
			log.error("Failed to send email to {}: {}", to, ex.getMessage());
		}
	}

	private String resolveFromAddress() {
		if (mailProperties.from() != null && !mailProperties.from().isBlank()) {
			return mailProperties.from();
		}
		if (mailUsername != null && !mailUsername.isBlank()) {
			return mailUsername;
		}
		return "noreply@fornituras.local";
	}
}
