CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255),
                       level VARCHAR(50),
                       role VARCHAR(50) NOT NULL
);

-- 🔹 TOPICS
CREATE TABLE topics (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        level VARCHAR(50)
);

-- 🔹 QUESTIONS
CREATE TABLE questions (
                           id SERIAL PRIMARY KEY,
                           text TEXT NOT NULL,
                           topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE
);

-- 🔹 ANSWERS
CREATE TABLE answers (
                         id SERIAL PRIMARY KEY,
                         text TEXT NOT NULL,
                         is_correct BOOLEAN NOT NULL,
                         question_id BIGINT REFERENCES questions(id) ON DELETE CASCADE
);

-- 🔹 ASSESSMENT
CREATE TABLE assessment_sessions (
                                     id SERIAL PRIMARY KEY,
                                     user_id BIGINT,
                                     status VARCHAR(50) NOT NULL,
                                     generated_json TEXT,
                                     result_json TEXT,
                                     created_at TIMESTAMP,
                                     completed_at TIMESTAMP
);


INSERT INTO users (username, first_name, last_name, password, email,role)
VALUES
    ('zhubanysh', 'Admin', 'Admin', '123', 'admin@test.com',  'ROLE_ADMIN'),
    ('aigerim', 'Admin', 'Admin', '123', 'admin@test.com',  'ROLE_ADMIN'),
    ('danagul', 'Admin', 'Admin', '123', 'admin@test.com',  'ROLE_ADMIN'),
    ('uldana', 'Admin', 'Admin', '123', 'admin@test.com',  'ROLE_ADMIN'),
    ('admin', 'Admin', 'Admin', '123', 'admin@test.com',  'ROLE_ADMIN'),
    ('test', 'Test', 'User', '123', 'test@test.com',  'ROLE_STUDENT');


INSERT INTO topics (name) VALUES
                              ('English Grammar'),
                              ('Vocabulary');

INSERT INTO questions (text, topic_id) VALUES
                                           ('Choose the correct form: She ___ to school every day.', 1),
                                           ('What is the synonym of "big"?', 2);

INSERT INTO answers (text, is_correct, question_id) VALUES
                                                        ('go', false, 1),
                                                        ('goes', true, 1),
                                                        ('gone', false, 1),
                                                        ('huge', true, 2),
                                                        ('small', false, 2),
                                                        ('tiny', false, 2);