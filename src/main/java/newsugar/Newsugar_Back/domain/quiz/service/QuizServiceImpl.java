package newsugar.Newsugar_Back.domain.quiz.service;

import newsugar.Newsugar_Back.domain.quiz.dto.SubmitResult;
import newsugar.Newsugar_Back.domain.quiz.dto.UserQuizStats;
import newsugar.Newsugar_Back.domain.quiz.model.Question;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import newsugar.Newsugar_Back.domain.quiz.model.QuizSubmission;
import newsugar.Newsugar_Back.domain.quiz.model.SubmissionAnswer;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizRepository;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizSubmissionRepository;
import newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient;
import newsugar.Newsugar_Back.domain.ai.GeminiService;
import newsugar.Newsugar_Back.domain.score.service.ScoreService;
import newsugar.Newsugar_Back.domain.summary.repository.SummaryRepository;
import newsugar.Newsugar_Back.domain.summary.model.Summary;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class QuizServiceImpl implements QuizService {
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final SummaryRepository summaryRepository;
    private final AiQuizClient aiQuizClient;
    private final ScoreService scoreService;
    private final GeminiService geminiService;

    public QuizServiceImpl(QuizRepository quizRepository, QuizSubmissionRepository quizSubmissionRepository, SummaryRepository summaryRepository, AiQuizClient aiQuizClient, ScoreService scoreService, GeminiService geminiService) {
        this.quizRepository = quizRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.summaryRepository = summaryRepository;
        this.aiQuizClient = aiQuizClient;
        this.scoreService = scoreService;
        this.geminiService = geminiService;
    }

    @Override
    public Quiz create(Quiz quiz) {
        if (quiz.getStartAt() != null && quiz.getEndAt() != null) {
            if (quiz.getEndAt().isBefore(quiz.getStartAt())) {
                throw new CustomException(ErrorCode.BAD_REQUEST, "종료 시간이 시작 시간보다 빠릅니다");
            }
        }
        if (quiz.getIsRevealed() == null) {
            quiz.setIsRevealed(Boolean.FALSE);
        }
        if (quiz.getQuestions() != null) {
            for (Question q : quiz.getQuestions()) {
                q.setQuiz(quiz); // 이부분이 양방향 설정해주는거에요 명찰 달아주고 주인 설정해줬으니 연결해주는 거죠 (이게 없으면 quiz_id가 null로 들어가요)
                if (q.getText() == null || q.getText().isBlank()) {
                    throw new CustomException(ErrorCode.BAD_REQUEST, "문제 내용이 비어 있습니다");
                }
                if (q.getOptions() == null || q.getOptions().size() < 2) {
                    throw new CustomException(ErrorCode.BAD_REQUEST, "객관식 옵션은 최소 2개 이상이어야 합니다");
                }
                if (q.getCorrectIndex() == null || q.getCorrectIndex() < 0 || q.getCorrectIndex() >= q.getOptions().size()) {
                    throw new CustomException(ErrorCode.BAD_REQUEST, "정답 인덱스가 옵션 범위를 벗어났습니다");
                }
            }
        }
        return quizRepository.save(quiz);
    }

    @Override
    public Quiz get(Long id) {
        return quizRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public SubmitResult score(Long id, Long userId, List<Integer> answers) {
        Quiz quiz = get(id);
        if (quiz == null) {
            throw new CustomException(ErrorCode.QUIZ_NOT_FOUND, "퀴즈를 찾을 수 없습니다");
        }
        Instant now = Instant.now();
        if ((quiz.getStartAt() != null && now.isBefore(quiz.getStartAt())) ||
            (quiz.getEndAt() != null && now.isAfter(quiz.getEndAt()))) {
            throw new CustomException(ErrorCode.QUIZ_EXPIRED, "퀴즈 제출 기간이 아닙니다");
        }

        // 중복 제출 체크 (이미 제출한 기록이 있으면 새로 저장하지 않고 기존 결과 반환하거나 에러 처리)
        // 여기서는 중복 제출을 허용하되, 점수 집계 로직을 보완하거나 프론트엔드 요구사항에 맞춰야 함
        // 현재는 중복 제출 시 계속 저장되므로 데이터가 쌓이는 구조임

        List<Question> qs = quiz.getQuestions() != null ? quiz.getQuestions() : List.of();
        int total = qs.size();
        int correct = 0;
        List<Boolean> results = new ArrayList<>();
        List<SubmissionAnswer> storedAnswers = new ArrayList<>();
        
        System.out.println("DEBUG: Quiz ID=" + id + ", User ID=" + userId + ", Received Answers=" + answers);

        for (int i = 0; i < total; i++) {
            Integer rawAnswer = (answers != null && i < answers.size()) ? answers.get(i) : null;
            Integer answer = rawAnswer;
            
            // 프론트엔드에서 1-based index(1, 2, 3, 4)로 보내고, DB에도 1-based로 저장됨
            if (answer == null || answer <= 0) {
                // 0이나 음수가 들어오면 오답 처리
                answer = -1;
            }
            
            // 기존 0-based 변환 로직 제거됨 (answer = answer - 1 삭제)

            Integer expected = qs.get(i).getCorrectIndex();
            int optionSize = qs.get(i).getOptions() != null ? qs.get(i).getOptions().size() : 0;
            
            // 1부터 optionSize까지가 유효 범위
            boolean inRange = (answer != null && answer >= 1 && answer <= optionSize);
            boolean ok = (inRange && expected != null && answer.equals(expected));
            
            System.out.println("Quiz Scoring - Q[" + i + "] UserRaw: " + rawAnswer + ", UserAdj: " + answer + ", Expected: " + expected + ", Correct: " + ok);
            
            if (ok) correct++;
            results.add(ok);

            SubmissionAnswer sa = new SubmissionAnswer();
            sa.setQuestionIndex(i);
            sa.setChosenIndex(answer); // DB에는 0-based로 저장
            sa.setCorrect(ok);
            sa.setAnsweredAt(Instant.now());
            sa.setUserId(userId);
            sa.setQuizId(quiz.getId());
            storedAnswers.add(sa);
        }
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setAnswers(storedAnswers);
        submission.setTotal(total);
        submission.setCorrect(correct);
        submission.setUserId(userId);
        
        // 중요: 중복 제출 방지 로직이 없어서 데이터가 계속 쌓임. 
        // 사용자별 최신 제출만 유지하고 싶다면 여기서 기존 제출을 조회해서 처리해야 함.
        // 현재는 그대로 저장.
        quizSubmissionRepository.save(submission);

        if (userId != null) {
            int gainedScore = total > 0 ? (int)Math.round((correct * 100.0) / total) : 0;
            if (gainedScore > 0) {
                scoreService.addScore(userId, gainedScore);
            }
        }

        return new SubmitResult(total, correct, results, userId, submission.getCreatedAt());
    }

    @Override
    public SubmitResult score(Long id, List<Integer> answers) {
        return score(id, null, answers);
    }

    @Override
    public List<Quiz> listToday() {
        Instant now = Instant.now();
        return quizRepository.findAll().stream()
                .filter(q -> (q.getStartAt() == null || !now.isBefore(q.getStartAt()))
                        && (q.getEndAt() == null || !now.isAfter(q.getEndAt())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Quiz> listByPeriod(Instant from, Instant to) {
        List<Quiz> all = quizRepository.findAll();
        return all.stream().filter(q -> {
            Instant qs = q.getStartAt() != null ? q.getStartAt() : Instant.MIN;
            Instant qe = q.getEndAt() != null ? q.getEndAt() : Instant.MAX;
            return !(qe.isBefore(from) || qs.isAfter(to));
        }).collect(Collectors.toList());
    }

    @Override
    public SubmitResult lastResult(Long quizId, Long userId) {
        if (userId == null) {
            return null; // 사용자 정보가 없으면 결과도 없음
        }
        return quizSubmissionRepository.findTopByQuiz_IdAndUserIdOrderByCreatedAtDesc(quizId, userId)
                .map(sub -> {
                    int total = sub.getTotal();
                    int correct = sub.getCorrect();
                    List<Boolean> results = sub.getAnswers() != null ?
                            sub.getAnswers().stream().map(SubmissionAnswer::getCorrect).toList() : List.of();
                    return new SubmitResult(total, correct, results, sub.getUserId(), sub.getCreatedAt());
                })
                .orElse(null);
    }

    

    @Override
    public void ensurePlayable(Long id) {
        Quiz quiz = get(id);
        if (quiz == null) {
            throw new CustomException(ErrorCode.QUIZ_NOT_FOUND, "퀴즈를 찾을 수 없습니다");
        }
        Instant now = Instant.now();
        if ((quiz.getStartAt() != null && now.isBefore(quiz.getStartAt())) ||
            (quiz.getEndAt() != null && now.isAfter(quiz.getEndAt()))) {
            throw new CustomException(ErrorCode.QUIZ_EXPIRED, "퀴즈 시작 기간이 아닙니다");
        }
    }

    @Override
    public SubmitResult resultOrThrow(Long quizId) {
        return quizSubmissionRepository.findTopByQuiz_IdOrderByCreatedAtDesc(quizId)
                .map(sub -> {
                    int total = sub.getTotal();
                    int correct = sub.getCorrect();
                    List<Boolean> results = sub.getAnswers() != null ?
                            sub.getAnswers().stream().map(SubmissionAnswer::getCorrect).toList() : List.of();
                    return new SubmitResult(total, correct, results, sub.getUserId(), sub.getCreatedAt());
                })
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "결과가 없습니다"));
    }

    @Override
    public Quiz generateFromSummary(Long summaryId) {
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "요약을 찾을 수 없습니다"));

        List<AiQuizClient.QuestionData> gen = geminiService.generateQuiz(summary.getSummaryText());
        List<Question> questions = new ArrayList<>();
        if (gen != null && !gen.isEmpty()) {
            AiQuizClient.QuestionData d = gen.get(0);
            Question q = new Question();
            q.setText(d.text);
            q.setOptions(d.options != null ? d.options : List.of());
            q.setCorrectIndex(d.correctIndex);
            q.setExplanation(d.explanation);
            questions.add(q);
        }
        Quiz quiz = new Quiz();
        quiz.setSummary(summary);
        quiz.setQuestions(questions);
        return create(quiz);
    }

    @Override
    public UserQuizStats statsForUser(Long userId) {
        List<QuizSubmission> subs = quizSubmissionRepository.findByUserId(userId);
        if (subs == null || subs.isEmpty()) {
            return new UserQuizStats(0, 0, 0, 0);
        }
        int totalQuestions = subs.stream().mapToInt(QuizSubmission::getTotal).sum();
        int totalCorrect = subs.stream().mapToInt(QuizSubmission::getCorrect).sum();
        int submissionCount = subs.size();
        int accuracyPercent = totalQuestions > 0 ? (int)Math.round((totalCorrect * 100.0) / totalQuestions) : 0;
        return new UserQuizStats(totalQuestions, totalCorrect, submissionCount, accuracyPercent);
    }

    @Override
    public boolean hasSubmission(Long quizId, Long userId) {
        List<QuizSubmission> subs = quizSubmissionRepository.findByUserId(userId);
        if (subs == null || subs.isEmpty()) return false;
        for (QuizSubmission s : subs) {
            Quiz q = s.getQuiz();
            if (q != null && q.getId() != null && q.getId().equals(quizId)) return true;
        }
        return false;
    }
}
