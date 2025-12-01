package com.add.venture.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationServiceImpl implements IEmailVerificationService {

    @Autowired
    private IEmailService emailService;

    // Almacenamos los códigos temporales en memoria (email -> código)
    private Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    private Random random = new Random();

    @Override
    public void sendVerificationCode(String email) {
        // Generar código de 6 dígitos
        String code = String.format("%06d", random.nextInt(999999));

        // Guardar el código para el email
        verificationCodes.put(email, code);

        // Enviar correo con el código usando plantilla HTML
        String subject = "🔐 Código de verificación - AddVenture";
        String htmlContent = buildVerificationEmailTemplate(code, email);

        emailService.sendHtmlEmail(new String[]{email}, subject, htmlContent);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String storedCode = verificationCodes.get(email);

        if (storedCode != null && storedCode.equals(code)) {
            // Código correcto: eliminamos para no reutilizar
            verificationCodes.remove(email);
            return true;
        }
        return false;
    }

    private String buildVerificationEmailTemplate(String code, String email) {
        return "<!DOCTYPE html>" +
            "<html lang=\"es\">" +
            "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "</head>" +
            "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;\">" +
                "<table role=\"presentation\" style=\"width: 100%; border-collapse: collapse;\">" +
                    "<tr>" +
                        "<td align=\"center\" style=\"padding: 40px 0;\">" +
                            "<table role=\"presentation\" style=\"width: 100%; max-width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\">" +
                                
                                "<!-- Header -->" +
                                "<tr>" +
                                    "<td style=\"padding: 40px 40px 20px; text-align: center; background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); border-radius: 16px 16px 0 0;\">" +
                                        "<h1 style=\"margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;\">" +
                                            "✈️ AddVenture" +
                                        "</h1>" +
                                        "<p style=\"margin: 10px 0 0; color: rgba(255, 255, 255, 0.9); font-size: 14px;\">" +
                                            "Viaja, Comparte, Aventura" +
                                        "</p>" +
                                    "</td>" +
                                "</tr>" +
                                
                                "<!-- Content -->" +
                                "<tr>" +
                                    "<td style=\"padding: 40px;\">" +
                                        "<h2 style=\"margin: 0 0 10px; color: #1e293b; font-size: 24px; font-weight: 600; text-align: center;\">" +
                                            "Verifica tu correo electrónico" +
                                        "</h2>" +
                                        "<p style=\"margin: 0 0 30px; color: #64748b; font-size: 16px; line-height: 1.6; text-align: center;\">" +
                                            "¡Estás a un paso de unirte a la comunidad de viajeros! Usa el siguiente código para completar tu registro:" +
                                        "</p>" +
                                        
                                        "<!-- Code Box -->" +
                                        "<div style=\"background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); border-radius: 12px; padding: 30px; text-align: center; margin-bottom: 30px; border: 2px dashed #3b82f6;\">" +
                                            "<p style=\"margin: 0 0 10px; color: #64748b; font-size: 14px; text-transform: uppercase; letter-spacing: 1px;\">" +
                                                "Tu código de verificación" +
                                            "</p>" +
                                            "<div style=\"font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #1d4ed8; font-family: 'Courier New', monospace;\">" +
                                                code +
                                            "</div>" +
                                            "<p style=\"margin: 15px 0 0; color: #94a3b8; font-size: 12px;\">" +
                                                "⏱️ Este código expira en 10 minutos" +
                                            "</p>" +
                                        "</div>" +
                                        
                                        "<!-- Info -->" +
                                        "<div style=\"background-color: #fef3c7; border-radius: 8px; padding: 15px; margin-bottom: 20px;\">" +
                                            "<p style=\"margin: 0; color: #92400e; font-size: 14px; line-height: 1.5;\">" +
                                                "⚠️ <strong>Importante:</strong> Si no solicitaste este código, ignora este correo. Tu cuenta está segura." +
                                            "</p>" +
                                        "</div>" +
                                        
                                        "<!-- Benefits -->" +
                                        "<div style=\"border-top: 1px solid #e2e8f0; padding-top: 25px;\">" +
                                            "<p style=\"margin: 0 0 15px; color: #1e293b; font-size: 16px; font-weight: 600;\">" +
                                                "🎉 Al unirte podrás:" +
                                            "</p>" +
                                            "<ul style=\"margin: 0; padding: 0 0 0 20px; color: #64748b; font-size: 14px; line-height: 1.8;\">" +
                                                "<li>Crear y unirte a grupos de viaje</li>" +
                                                "<li>Conectar con otros viajeros</li>" +
                                                "<li>Compartir tus experiencias</li>" +
                                                "<li>Descubrir destinos increíbles</li>" +
                                            "</ul>" +
                                        "</div>" +
                                    "</td>" +
                                "</tr>" +
                                
                                "<!-- Footer -->" +
                                "<tr>" +
                                    "<td style=\"padding: 30px 40px; background-color: #f8fafc; border-radius: 0 0 16px 16px; text-align: center;\">" +
                                        "<p style=\"margin: 0 0 10px; color: #64748b; font-size: 14px;\">" +
                                            "¿Tienes problemas? Contáctanos en " +
                                            "<a href=\"mailto:somosaddventure@gmail.com\" style=\"color: #3b82f6; text-decoration: none;\">somosaddventure@gmail.com</a>" +
                                        "</p>" +
                                        "<p style=\"margin: 0; color: #94a3b8; font-size: 12px;\">" +
                                            "© 2024 AddVenture. Todos los derechos reservados." +
                                        "</p>" +
                                        "<div style=\"margin-top: 15px;\">" +
                                            "<span style=\"color: #94a3b8; font-size: 20px;\">🌍 🏔️ 🏖️ ✨</span>" +
                                        "</div>" +
                                    "</td>" +
                                "</tr>" +
                                
                            "</table>" +
                        "</td>" +
                    "</tr>" +
                "</table>" +
            "</body>" +
            "</html>";
    }
}
