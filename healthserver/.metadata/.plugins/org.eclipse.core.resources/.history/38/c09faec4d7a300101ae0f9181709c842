package com.healthapp.notification.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    // Type de template à utiliser
    private EmailTemplateType templateType;
    
    // Variables pour remplir le template
    private Map<String, Object> templateVariables;
    
    // Ou contenu HTML direct
    private String htmlContent;
    
    // Ou contenu texte simple
    private String textContent;
    
    // Email de réponse (optionnel)
    private String replyTo;
    
    // Copies (optionnel)
    private String[] cc;
    
    // Copie cachée (optionnel)
    private String[] bcc;
}
