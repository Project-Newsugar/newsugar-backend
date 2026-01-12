package newsugar.Newsugar_Back.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 들어온 값이 이상하면 여기서 막습니다. 400 에러 던집니다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), "유효성 오류"));
    }

    // 인자 잘못 넣으면 여기서 걸립니다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ex.printStackTrace();

        return ResponseEntity.badRequest()
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    // 웬만하면 여기까지 안 와야 하는데, 진짜 모르는 에러 터지면 여기서 잡습니다. 500 에러.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        ex.printStackTrace();

        return ResponseEntity.internalServerError()
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.name(), "서버 오류"));
    }

    // 헤더에 토큰 없으면 문전박대합니다.
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error(ErrorCode.UNAUTHORIZED.name(), "Authorization 헤더가 존재하지 않습니다."));
    }


    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResult<Void>> handleCustomException(CustomException ex) {
        ex.printStackTrace(); // 로그 남겨야 나중에 범인 잡습니다.
        HttpStatus status;

        // ErrorCode 보고 적절한 HTTP 상태 코드 찍어줍니다.
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

