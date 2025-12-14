-- Скрипт для создания базы данных поискового движка

-- Создание базы данных (выполняется от имени суперпользователя)
-- CREATE DATABASE search_engine;

-- Подключение к базе данных
-- \c search_engine;

-- Таблица для страниц
CREATE TABLE IF NOT EXISTS pages (
    id SERIAL PRIMARY KEY,
    url VARCHAR(2048) UNIQUE NOT NULL,
    title VARCHAR(512),
    content TEXT,
    depth INTEGER,
    crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска по URL
CREATE INDEX IF NOT EXISTS idx_url ON pages(url);

-- Таблица для индекса слов
CREATE TABLE IF NOT EXISTS word_index (
    id SERIAL PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    page_url VARCHAR(2048) NOT NULL,
    occurrences INTEGER NOT NULL,
    UNIQUE(word, page_url)
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_word ON word_index(word);
CREATE INDEX IF NOT EXISTS idx_page_url ON word_index(page_url);

-- Внешний ключ
ALTER TABLE word_index 
ADD CONSTRAINT fk_word_index_page 
FOREIGN KEY (page_url) REFERENCES pages(url) ON DELETE CASCADE;

