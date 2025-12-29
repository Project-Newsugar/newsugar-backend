CREATE DATABASE IF NOT EXISTS news_db;
USE news_db;

-- 1. 카테고리 테이블
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 2. 요약 테이블 (LONGTEXT로 변경)
CREATE TABLE IF NOT EXISTS `summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `summary_text` LONGTEXT DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3. 퀴즈 테이블
CREATE TABLE IF NOT EXISTS `quiz` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `question` varchar(255) DEFAULT NULL,
  `correct_answer` varchar(255) DEFAULT NULL,
  `is_revealed` bit(1) DEFAULT NULL,
  `start_at` datetime(6) DEFAULT NULL,
  `end_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `summary_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_quiz_summary` (`summary_id`),
  CONSTRAINT `FK_quiz_summary` FOREIGN KEY (`summary_id`) REFERENCES `summary` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 4. 문제(Question) 테이블 (TEXT로 변경)
CREATE TABLE IF NOT EXISTS `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `text` TEXT DEFAULT NULL,
  `explanation` TEXT DEFAULT NULL,
  `correct_index` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `quiz_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_question_quiz` (`quiz_id`),
  CONSTRAINT `FK_question_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. 문제 보기(Option) 테이블 (TEXT로 변경)
CREATE TABLE IF NOT EXISTS `question_option` (
  `question_id` bigint NOT NULL,
  `option_text` TEXT DEFAULT NULL,
  `option_order` int DEFAULT NULL, -- 순서 보장 컬럼 추가
  KEY `FK_option_question` (`question_id`),
  CONSTRAINT `FK_option_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 6. 사용자(User) 테이블
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `nickname` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 7. 사용자-카테고리 연결 테이블
CREATE TABLE IF NOT EXISTS `user_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_uc_user` (`user_id`),
  KEY `FK_uc_category` (`category_id`),
  CONSTRAINT `FK_uc_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `FK_uc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 8. 사용자 점수(Total Score) 테이블
CREATE TABLE IF NOT EXISTS `user_score` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `score` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_us_user` (`user_id`),
  CONSTRAINT `FK_us_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 9. 퀴즈 제출 결과(User Quiz) 테이블
CREATE TABLE IF NOT EXISTS `user_quiz` (
  `score_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `quiz_id` bigint DEFAULT NULL,
  `quiz_score` int DEFAULT NULL,
  `total` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`score_id`),
  KEY `FK_uq_quiz` (`quiz_id`),
  CONSTRAINT `FK_uq_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 10. 상세 정답 제출 내역(Quiz Answers) 테이블
-- 주의: Java Entity (SubmissionAnswer)가 @Column(name = "questionIndex")를 사용하므로 컬럼명 수정
CREATE TABLE IF NOT EXISTS `quiz_answers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint DEFAULT NULL,
  `quiz_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `questionIndex` int DEFAULT NULL,
  `user_answer` int DEFAULT NULL,
  `is_correct` bit(1) DEFAULT NULL,
  `answered_at` datetime(6) DEFAULT NULL,
  `answer_order` int DEFAULT NULL, -- 순서 보장 컬럼 추가
  PRIMARY KEY (`id`),
  KEY `FK_qa_submission` (`submission_id`),
  CONSTRAINT `FK_qa_submission` FOREIGN KEY (`submission_id`) REFERENCES `user_quiz` (`score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
