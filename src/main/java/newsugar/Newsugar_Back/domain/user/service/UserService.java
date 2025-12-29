package newsugar.Newsugar_Back.domain.user.service;

import jakarta.transaction.Transactional;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.category.model.Category;
import newsugar.Newsugar_Back.domain.category.repository.CategoryRepository;
import newsugar.Newsugar_Back.domain.score.service.ScoreService;
import newsugar.Newsugar_Back.domain.user.dto.Response.UserCategoryResponseDTO;
import newsugar.Newsugar_Back.domain.user.dto.Response.UserLoginResponseDTO;
import newsugar.Newsugar_Back.domain.user.dto.Response.UserPreferCategoryResponseDTO;
import newsugar.Newsugar_Back.domain.user.model.User;
import newsugar.Newsugar_Back.domain.user.model.UserCategory;
import newsugar.Newsugar_Back.domain.user.repository.UserCategoryRepository;
import newsugar.Newsugar_Back.domain.user.repository.UserRepository;
import newsugar.Newsugar_Back.domain.user.utils.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final ScoreService scoreService;
    private final CategoryRepository categoryRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ScoreService scoreService, CategoryRepository categoryRepository, UserCategoryRepository userCategoryRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.scoreService = scoreService;
        this.categoryRepository = categoryRepository;
        this.userCategoryRepository = userCategoryRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public UserLoginResponseDTO login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS, "아이디와 비밀번호가 일치하지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new UserLoginResponseDTO(user.getId(), token, refreshToken);
    }

    public User signup (String name, String email, String rawPassword, String nickname, String phone){
        if(userRepository.existsByEmail(email)){
            throw new CustomException(ErrorCode.CONFLICT, "이미 사용중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .name(name)
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .phone(phone)
                .build();

        User savedUser = userRepository.save(user);

        // 점수 초기화
        scoreService.createScore(savedUser.getId());

        return savedUser;
    }

    public User modify(Long userId, String name, String rawPassword, String nickname, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (name != null && !name.isEmpty()) {
            user.setName(name);
        }

        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }

        if (phone != null && !phone.isEmpty()) {
            user.setPhone(phone);
        }

        return userRepository.save(user);
    }

    public User getInfo(Long userId){

        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public UserCategoryResponseDTO preferCategory(Long userId, Long categoryId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 카테고리입니다."));

        if (userCategoryRepository.existsByUserIdAndCategoryId(userId, categoryId)) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 선호 카테고리가 등록되어 있습니다.");
        }

        UserCategory userCategory = UserCategory.builder()
                .user(user)
                .category(category)
                .build();

        UserCategory savedUC = userCategoryRepository.save(userCategory);

        return new UserCategoryResponseDTO(
                savedUC.getId(),
                savedUC.getUser().getId(),
                savedUC.getCategory().getId(),
                savedUC.getCategory().getName()
        );

    }

    public void deleteCategory(Long userId, Long categoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 카테고리입니다."));

        UserCategory userCategory = userCategoryRepository
                .findByUserIdAndCategoryId(userId, categoryId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND, "이미 즐겨찾기 해제된 카테고리입니다.")
                );

        userCategoryRepository.delete(userCategory);
    }

    public UserPreferCategoryResponseDTO getPreferCategory(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<UserCategory> userCategories = userCategoryRepository.findByUserId(userId);

        List<Long> categoryIdList = userCategories.stream()
                .map(uc -> uc.getCategory().getId())
                .collect(Collectors.toList());

        return new UserPreferCategoryResponseDTO(user.getId(), userId, categoryIdList);
    }

}
