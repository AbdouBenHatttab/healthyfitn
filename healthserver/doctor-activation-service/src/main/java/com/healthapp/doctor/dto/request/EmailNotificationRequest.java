package com.healthapp.doctor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {
    
    private String to;
    private String subject;
    private String templateType;
    private Map<String, Object> templateVariables;
}
