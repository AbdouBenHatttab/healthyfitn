package com.healthapp.user.dto.request;

import com.healthapp.user.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
   private Integer page = 0;
    private Integer size = 10;
}

