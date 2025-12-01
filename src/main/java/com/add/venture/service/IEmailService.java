package com.add.venture.service;

import java.io.File;

public interface IEmailService {
    
    void sendEmail(String[] toUser, String subject, String message);

    void sendEmailWithFile(String[] toUser, String subject, String message, File File);
    
    void sendHtmlEmail(String[] toUser, String subject, String htmlContent);

}
 