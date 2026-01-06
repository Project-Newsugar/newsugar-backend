package newsugar.Newsugar_Back.domain.user.repository;

import newsugar.Newsugar_Back.domain.user.model.User;
import newsugar.Newsugar_Back.domain.user.model.UserCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {
    Optional<UserCategory> findById(Long id);
    Optional<UserCategory> findByUserIdAndCategoryId(Long userId, Long categoryId);
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);
    List<UserCategory> findByUserId(Long userId);

}
