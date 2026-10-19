-- H2-Variante des Schemas (lokale Entwicklung & Tests). Fachlich identisch zu db/migration/postgresql.
CREATE TABLE users (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profiles (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    zip_code VARCHAR(20),
    city VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    linkedin VARCHAR(255),
    github VARCHAR(255),
    letter_style VARCHAR(20) DEFAULT 'CLASSIC' NOT NULL,
    custom_fields JSON,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    category VARCHAR(30) NOT NULL,
    size_bytes BIGINT NOT NULL,
    page_count INTEGER,
    storage_path VARCHAR(512) NOT NULL,
    status VARCHAR(20) NOT NULL,
    extracted_text VARCHAR(1000000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_documents_user ON documents(user_id);

CREATE TABLE applications (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_url VARCHAR(2048) NOT NULL,
    job_title VARCHAR(255),
    company_name VARCHAR(255),
    contact_person VARCHAR(255),
    recipient_address VARCHAR(500),
    raw_job_text VARCHAR(1000000),
    requirements JSON,
    tasks JSON,
    context_document_ids JSON,
    tone VARCHAR(20) DEFAULT 'FORMAL' NOT NULL,
    letter_length VARCHAR(20) DEFAULT 'STANDARD' NOT NULL,
    status VARCHAR(30) DEFAULT 'ANALYZING' NOT NULL,
    analysis_step INTEGER DEFAULT 0 NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_applications_user ON applications(user_id);

CREATE TABLE cover_letters (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    paragraphs JSON NOT NULL,
    changed_paragraphs JSON,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cover_letter_version UNIQUE (application_id, version)
);

CREATE TABLE chat_messages (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    message VARCHAR(1000000) NOT NULL,
    meta VARCHAR(255),
    cover_letter_version INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_messages_application ON chat_messages(application_id);
