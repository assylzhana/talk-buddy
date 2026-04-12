                                        ('test', '$2a$10$R7G6bDqJzi10BGSAdoo6gOKX1sC3yRyvE4s46HoPazTA/gkGEXLMa', 'ROLE_STUDENT');

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