package newsugar.Newsugar_Back.common;

public enum ErrorCode {
    // 정상 처리
    OK,

    // 공통 에러
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_ERROR,

    // 인증/가입 관련
    TERMS_NOT_ACCEPTED,
    AUTH_INVALID_CREDENTIALS,
    AUTH_ACCOUNT_NOT_FOUND,

    // 퀴즈 관련
    QUIZ_NOT_FOUND,
    QUIZ_EXPIRED,
    // 콘텐츠 관련
    CONTENT_NOT_AVAILABLE,

    //구글 소셜 로그인
    GOOGLE_LOGIN_FAILED
}

