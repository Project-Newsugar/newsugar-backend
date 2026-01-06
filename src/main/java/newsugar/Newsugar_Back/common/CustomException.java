package newsugar.Newsugar_Back.common;

import org.springframework.http.HttpStatus;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode, String message) {
        super(message); // RuntimeException에 메시지 전달
        this.errorCode = errorCode;
    }
}
