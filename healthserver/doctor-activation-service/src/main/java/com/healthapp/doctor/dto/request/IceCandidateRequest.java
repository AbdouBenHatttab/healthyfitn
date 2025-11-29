package com.healthapp.doctor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceCandidateRequest {
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
}

