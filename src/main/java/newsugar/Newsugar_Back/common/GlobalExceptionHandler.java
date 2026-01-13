package newsugar.Newsugar_Back.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 유효성 검사 실패 시 400 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), "유효성 오류"));
    }

    // 잘못된 인자 전달 시 400 반환
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    // 서버 내부 오류 발생 시 500 반환
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        ex.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
    }

    // 인증 헤더 누락 시 401 반환
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error(ErrorCode.UNAUTHORIZED.name(), "Authorization 헤더가 존재하지 않습니다."));
    }


    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResult<Void>> handleCustomException(CustomException ex) {
        ex.printStackTrace(); // 디버깅용 스택 트레이스 출력
        HttpStatus status;

        // ErrorCode 기반 HTTP 상태 코드 매핑
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

