package newsugar.Newsugar_Back.domain.quiz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;

@Entity
@Table(name = "quiz_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "questionIndex")
    private Integer questionIndex;
    @Column(name = "user_answer")
    private Integer chosenIndex;
    @Column(name = "is_correct")
    private Boolean correct;
    @Column(name = "answered_at")
    private java.time.Instant answeredAt;
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "quiz_id")
    private Long quizId;
}
