CREATE TABLE words (
  id INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  spelling TEXT UNIQUE
);

