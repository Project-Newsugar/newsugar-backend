package newsugar.Newsugar_Back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizRepository;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizSubmissionRepository;
import newsugar.Newsugar_Back.domain.summary.repository.SummaryRepository;
import newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient;
import newsugar.Newsugar_Back.domain.score.repository.ScoreRepository;
import newsugar.Newsugar_Back.domain.user.repository.UserRepository;
import newsugar.Newsugar_Back.domain.category.repository.CategoryRepository;
import newsugar.Newsugar_Back.domain.user.repository.UserCategoryRepository;
import newsugar.Newsugar_Back.domain.user.utils.JwtUtil;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;


@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class NewsugarBackEndApplicationTests {

    @MockBean
    private QuizRepository quizRepository;

    @MockBean
    private QuizSubmissionRepository quizSubmissionRepository;

    @MockBean
    private SummaryRepository summaryRepository;

    @MockBean
    private AiQuizClient aiQuizClient;

    @MockBean
    private ScoreRepository scoreRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private UserCategoryRepository userCategoryRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void contextLoads() {
    }
}
