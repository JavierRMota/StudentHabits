CREATE TABLE IDENTIFIER (
  ID INT AUTO_INCREMENT,
  IDENTIFIER VARCHAR(255),
  CONSTRAINT IDENTIFIER_PK PRIMARY KEY (ID)
);
CREATE TABLE DATA (
  ID INT,
  NAME VARCHAR(255),
  PKG_NAME VARCHAR(500),
  START_TIME BIGINT,
  END_TIME BIGINT,
  DATE_EVENT TIMESTAMP,
  DURATION BIGINT,
  CONSTRAINT DATA_PK PRIMARY KEY(ID,NAME,START_TIME),
  CONSTRAINT DATA_FK FOREIGN KEY(ID) REFERENCES
  IDENTIFIER(ID)
);
