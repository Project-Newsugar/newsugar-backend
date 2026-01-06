-- 1. Fix Question Options Ordering
-- Add option_order column if it doesn't exist (handled by ddl-auto usually, but safe to ensure)
-- ALTER TABLE question_option ADD COLUMN option_order INTEGER;

-- Initialize option_order for existing data (This is tricky without a stored procedure or manual logic, 
-- but we can at least set it to 0 for single options or sequential if possible. 
-- For simplicity in a fix script, we might just have to clear old data or warn user.)

-- 2. Fix Quiz Submission Answers Ordering
-- Add answer_order column
-- ALTER TABLE quiz_answers ADD COLUMN answer_order INTEGER;

-- 3. Ensure User ID and Quiz ID consistency
-- (No specific SQL needed unless data is known to be bad)

-- IMPORTANT: If you have existing "twisted" data, it is best to truncate the quiz tables and start fresh 
-- because recreating the correct order from scrambled lists is impossible without timestamps or other hints.

-- TRUNCATE COMMANDS (Use with CAUTION - deletes all quiz history):
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE question_option;
-- TRUNCATE TABLE question;
-- TRUNCATE TABLE quiz_answers;
-- TRUNCATE TABLE user_quiz;
-- TRUNCATE TABLE quiz;
-- SET FOREIGN_KEY_CHECKS = 1;
