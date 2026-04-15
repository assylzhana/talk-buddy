alter table if exists assessment_sessions
alter column generated_json set data type TEXT;

alter table if exists assessment_sessions
alter column result_json set data type TEXT;

alter table if exists photos
alter column url set data type TEXT;

alter table if exists videos
alter column url set data type TEXT;


ALTER TABLE questions ADD COLUMN type VARCHAR(50);
ALTER TABLE questions ADD COLUMN correct_answer TEXT;

CREATE TABLE matching_pairs (
                                id BIGSERIAL PRIMARY KEY,
                                left_text TEXT,
                                right_text TEXT,
                                question_id BIGINT REFERENCES questions(id) ON DELETE CASCADE
);