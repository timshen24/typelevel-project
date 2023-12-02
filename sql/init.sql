-- ADDED in LESSON "Running JOBs 'Algebra'"

CREATE DATABASE board;
-- connect to board的意思; \dt describe tables
\c board; 

CREATE TABLE jobs(
  id uuid DEFAULT gen_random_uuid(),
  date bigint NOT NULL,
  ownerEmail text NOT NULL,
  company text NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  externalUrl text NOT NULL,
  remote boolean NOT NULL DEFAULT false,
  location text,
  salaryLo integer,
  salaryHi integer,
  currency text,
  country text,
  tags text,
  image text,
  seniority text,
  other text,
  active BOOLEAN NOT NULL DEFAULT false
);

ALTER TABLE jobs
ADD CONSTRAINT pk_jobs PRIMARY KEY (id)