package newsugar.Newsugar_Back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record  GoogleUserInfoDTO(
       String googleUserId,
        String email,
       String name
) {
}