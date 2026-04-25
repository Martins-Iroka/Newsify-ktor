ALTER TABLE followers
    ADD CONSTRAINT check_user_not_following_self
        CHECK (creator_id <> reader_id);