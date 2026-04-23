CREATE TABLE loans (
    id                   UUID        PRIMARY KEY,
    member_id            UUID        NOT NULL,
    title_id             UUID        NOT NULL,
    copy_id              UUID,
    status               VARCHAR(20) NOT NULL
                             CHECK (status IN (
                                 'pending', 'awaiting_copy', 'approved',
                                 'rejected', 'started', 'ended', 'cancelled'
                             )),
    desired_pickup_from  DATE        NOT NULL,
    desired_pickup_to    DATE        NOT NULL,
    rejection_reason     TEXT,
    due_date             DATE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);