package newsugar.Newsugar_Back.domain.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import newsugar.Newsugar_Back.common.ApiResult;
import newsugar.Newsugar_Back.domain.user.dto.GoogleUserInfoDTO;
import newsugar.Newsugar_Back.domain.user.dto.JwtRefreshTokenDTO;
import newsugar.Newsugar_Back.domain.user.dto.request.UserCategoryRequestDTO;
import newsugar.Newsugar_Back.domain.user.dto.request.UserLoginRequestDTO;
import newsugar.Newsugar_Back.domain.user.dto.request.UserModifyRequestDTO;
import newsugar.Newsugar_Back.domain.user.dto.response.*;
import newsugar.Newsugar_Back.domain.user.dto.request.UserSignupRequestDTO;
import newsugar.Newsugar_Back.domain.user.model.User;
import newsugar.Newsugar_Back.domain.user.service.GoogleService;
import newsugar.Newsugar_Back.domain.user.service.JwtService;
import newsugar.Newsugar_Back.domain.score.service.ScoreService;
import newsugar.Newsugar_Back.domain.user.service.UserService;
import newsugar.Newsugar_Back.domain.user.utils.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final ScoreService scoreService;
    private final GoogleService googleService;
    private final JwtUtil jwtUtil;


    @PostMapping("/signup")
    public ResponseEntity<ApiResult<UserResponseDTO>> signup(@RequestBody UserSignupRequestDTO request) {
        User savedUser = userService.signup(
                request.name(),
                request.email(),
                request.password(),
                request.nickname(),
                request.phone()
        );

        UserResponseDTO response = new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getNickname(),
                savedUser.getPhone()
        );

        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<UserLoginResponseDTO>> login (@RequestBody UserLoginRequestDTO request){
        UserLoginResponseDTO user = userService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(ApiResult.ok(user));
    }

    @PatchMapping("/modify")
    public ResponseEntity<ApiResult<UserResponseDTO>> modify(
            @RequestHeader("Authorization") String token,
            @RequestBody UserModifyRequestDTO request
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;

        Long userId = jwtService.getUserIdFromToken(actualToken);

        User updatedUser = userService.modify(
                userId,
                request.name(),
                request.password(),
                request.nickname(),
                request.phone()
        );

        UserResponseDTO response = new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getNickname(),
                updatedUser.getPhone()
        );

        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @GetMapping("/getInfo")
    public ResponseEntity<ApiResult<UserInfoResponseDTO>> getInfo (
            @RequestHeader("Authorization") String token
    ){
        String actualToken = token != null ? token.replace("Bearer ", "") : null;

        Long userId = jwtService.getUserIdFromToken(actualToken);
        User user = userService.getInfo(userId);
        Integer score = scoreService.getScore(userId);

        UserInfoResponseDTO response = new UserInfoResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getNickname(),
                user.getPhone(),
                score
        );

        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @PostMapping("/category")
    public ResponseEntity<ApiResult<UserCategoryResponseDTO>> preferCategory (
            @RequestHeader("Authorization") String token,
            @RequestBody UserCategoryRequestDTO request
    ){
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);

        UserCategoryResponseDTO response = userService.preferCategory(userId, request.categoryId());

        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @DeleteMapping("/category/{categoryId}")
    public ResponseEntity<ApiResult<String>> notPreferCategory(
            @PathVariable Long categoryId,
            @RequestHeader("Authorization") String token
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);

        userService.deleteCategory(userId, categoryId);

        return ResponseEntity.ok(ApiResult.ok("즐겨찾기가 해제되었습니다." ));
    }

    @GetMapping("/my-category")
    public ResponseEntity<ApiResult<UserPreferCategoryResponseDTO>> getPreferCategory(
            @RequestHeader("Authorization") String token
    ){
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);

        UserPreferCategoryResponseDTO response = userService.getPreferCategory(userId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<UserLoginResponseDTO>> refreshToken(
            @RequestBody JwtRefreshTokenDTO request
            ){
        Long userId = jwtUtil.validateRefresh(request.refreshToken());
        String newAccessToken = jwtUtil.generateToken(userId);

        UserLoginResponseDTO response = new UserLoginResponseDTO(userId, newAccessToken, request.refreshToken());

        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @PostMapping("/google/login")
    public ResponseEntity<ApiResult<UserLoginResponseDTO>> googleLogin(
            @RequestBody Map<String, String> request
    ) {
        String accessToken = request.get("accessToken");

        UserLoginResponseDTO response =
                googleService.googleLogin(accessToken);

        return ResponseEntity.ok(ApiResult.ok(response));
    }
}