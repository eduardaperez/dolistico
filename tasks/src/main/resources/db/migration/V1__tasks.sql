-- TASKS
CREATE TABLE IF NOT EXISTS tasks.tasks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(255),
    color VARCHAR(20),
    priority INTEGER NOT NULL,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    location VARCHAR(255),
    is_all_day BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_time TIMESTAMPTZ,
    notify BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(255) NOT NULL,
    due_date DATE
);

-- CATEGORY
CREATE TABLE IF NOT EXISTS tasks.category (
    id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    category_name VARCHAR(100) NOT NULL
);
