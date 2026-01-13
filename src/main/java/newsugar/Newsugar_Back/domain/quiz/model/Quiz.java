package newsugar.Newsugar_Back.domain.quiz.model;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;
import lombok.Builder.Default;

@Entity
@Table(name = "quiz")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Column(name = "question")
    private String question;
    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "start_at")
    private Instant startAt;
    @Column(name = "end_at")
    private Instant endAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "is_revealed")
    private Boolean isRevealed;

    // Question 연관 관계 매핑 (1:N)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Default
    private List<Question> questions = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "summary_id")
    private newsugar.Newsugar_Back.domain.summary.model.Summary summary;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
