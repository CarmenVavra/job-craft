-- Weitere Angaben zur Person (z. B. Privatprojekte), die nicht im Lebenslauf stehen.
CREATE TABLE profile_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_profile_notes_user ON profile_notes(user_id);
