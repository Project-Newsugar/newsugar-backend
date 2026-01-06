package newsugar.Newsugar_Back.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 요청 유효성 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), "유효성 오류"));
    }

    // 잘못된 인자 전달 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    // 기타 예상하지 못한 오류 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        ex.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
    }

    // 필수 헤더 없을 경우 (Token 헤더)
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error(ErrorCode.UNAUTHORIZED.name(), "Authorization 헤더가 존재하지 않습니다."));
    }


    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResult<Void>> handleCustomException(CustomException ex) {
        ex.printStackTrace(); // 콘솔에서 오류 확인 가능
        HttpStatus status;

        // ErrorCode에 따라 HTTP 상태 코드 지정
        switch (ex.getErrorCode()) {
            case CONFLICT -> status = HttpStatus.CONFLICT;
            case BAD_REQUEST -> status = HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> status = HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> status = HttpStatus.FORBIDDEN;
            case NOT_FOUND , AUTH_ACCOUNT_NOT_FOUND -> status = HttpStatus.NOT_FOUND;
            default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(status)
                .body(ApiResult.error(ex.getErrorCode().name(), ex.getMessage()));
    }
}

