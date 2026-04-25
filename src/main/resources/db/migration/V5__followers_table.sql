CREATE TABLE IF NOT EXISTS followers(
                                        creator_id bigint NOT NULL,
                                        reader_id bigint NOT NULL,
                                        created_at timestamp(0) with time zone NOT NULL DEFAULT NOW(),

                                        PRIMARY KEY (creator_id, reader_id), --Composite key which prevents duplicate entry into the table in case of users following each other
                                        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE,
                                        FOREIGN KEY (reader_id) REFERENCES users (id) ON DELETE CASCADE
);