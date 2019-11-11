-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-word! :! :n
-- :doc creates a new word record
INSERT INTO words (spelling) VALUES (:spelling)

-- :name get-word :? :1
-- :doc retrieves a word record given the spelling
SELECT * FROM words
WHERE spelling = :spelling

-- :name create-pronunciation! :! :n
-- :doc creates a new pronunciation record
INSERT INTO pronunciations
(word_id, ipa, sounds_like, audio_uri) VALUES
(:word_id, :ipa, :sounds_like, :audio_uri)
