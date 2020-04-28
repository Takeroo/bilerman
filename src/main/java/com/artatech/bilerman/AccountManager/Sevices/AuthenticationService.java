package com.artatech.bilerman.AccountManager.Sevices;

import com.artatech.bilerman.AccountManager.Entities.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {

    static final Long EXPIRATION_TIME = 864_000_00L;
    static final String SIGNING_KEY = "bilerman_singing_key";
    static final String BEARER_PREFIX = "Bearer";
    static final String APPLICATION_URL = "http://localhost:3000";

    @Autowired
    private UserService service;

    @Autowired
    private MessageSource messages;

    @Autowired
    private JavaMailSender mailSender;

    static public void addJWTToken(HttpServletResponse response, String username){
        String jwtToken = Jwts.builder().setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SIGNING_KEY)
                .compact();

        response.addHeader("Authorization", BEARER_PREFIX + " " + jwtToken);
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
    }

    static public Authentication getAuthentication(HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if(token != null) {
            String user = Jwts.parser()
                    .setSigningKey(SIGNING_KEY)
                    .parseClaimsJws(token.replace(BEARER_PREFIX, ""))
                    .getBody()
                    .getSubject();

            if(user == null) throw new RuntimeException("Authentication failed");

            return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        }

        return null;
    }

    public void sendEmailVerificationEmail(User user, Locale locale){
        try {
            String token = UUID.randomUUID().toString();
            service.createVerificationToken(user, token);

            String confirmationUrl = APPLICATION_URL + "/user/confirm?token=" + token;
            File input = new File(getClass().getClassLoader().getResource("templates/email_template.html").getFile());
            Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
            String html = doc.html();
            html = html.replace("{{name}}", user.getName());
            html = html.replace("{{top}}", "We're excited to have you get started. First, you need to confirm your account. Just press the button below.");
            html = html.replace("{{bottom}}", "If you did not register to our site, you can safely ignore this email.");
            html = html.replace("{{button_name}}", "Activate");
            html = html.replace("{{action_url}}", confirmationUrl);
            String message = html;

            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
                messageHelper.setTo(user.getEmail());
                messageHelper.setSubject("Bilerman account confirmation");
                messageHelper.setText(message, true);
            };
            mailSender.send(messagePreparator);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void sendPasswordResetEmail(User user, Locale locale){
        try {
            String token = UUID.randomUUID().toString();
            service.createPasswordResetToken(user, token);

            String resetURL = APPLICATION_URL + "/user/password/reset?token=" + token;
            File input = new File(getClass().getClassLoader().getResource("templates/email_template.html").getFile());
            Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
            String html = doc.html();
            html = html.replace("{{name}}", user.getName());
            html = html.replace("{{top}}", "Seems like you forgot your password for Bilerman. If this is true, click below to reset your password.");
            html = html.replace("{{bottom}}", "If you did not forgot your password you can safely ignore this email.");
            html = html.replace("{{button_name}}", "Reset password");
            html = html.replace("{{action_url}}", resetURL);
            String message = html;

            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
                messageHelper.setTo(user.getEmail());
                messageHelper.setSubject("Bilerman password reset");
                messageHelper.setText(message, true);
            };
            mailSender.send(messagePreparator);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
    }
}