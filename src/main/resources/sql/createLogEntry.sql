CREATE TABLE LogEntry(LOGENTRY_ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), PATH VARCHAR(64) NOT NULL, TOOL VARCHAR(64) NOT NULL)
