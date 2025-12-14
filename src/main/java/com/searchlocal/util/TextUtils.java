package com.searchlocal.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Утилиты для работы с текстом
 */
public class TextUtils {
    private static final List<String> STOP_WORDS = Arrays.asList(
            "а", "в", "и", "к", "на", "о", "с", "у", "я", "он", "она", "оно", "они",
            "мы", "вы", "ты", "это", "то", "как", "так", "для", "что", "где", "когда",
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"
    );

    /**
     * Разбивает текст на слова, удаляя знаки препинания и приводя к нижнему регистру
     */
    public static List<String> extractWords(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                        .split("\\s+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Создает сниппет текста вокруг найденных слов
     */
    public static String createSnippet(String text, List<String> searchWords, int snippetLength) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = text.replaceAll("\\s+", " ").trim();
        
        if (text.length() <= snippetLength) {
            return text;
        }

        // Ищем первое вхождение любого из слов поиска
        int startPos = -1;
        String lowerText = text.toLowerCase();
        
        for (String word : searchWords) {
            int pos = lowerText.indexOf(word.toLowerCase());
            if (pos != -1 && (startPos == -1 || pos < startPos)) {
                startPos = pos;
            }
        }

        if (startPos == -1) {
            startPos = 0;
        }

        // Вычисляем начальную позицию сниппета
        int snippetStart = Math.max(0, startPos - snippetLength / 2);
        int snippetEnd = Math.min(text.length(), snippetStart + snippetLength);

        // Корректируем границы, чтобы не обрезать слова
        if (snippetStart > 0) {
            int spacePos = text.lastIndexOf(' ', snippetStart);
            if (spacePos > snippetStart - 50) {
                snippetStart = spacePos + 1;
            }
        }

        if (snippetEnd < text.length()) {
            int spacePos = text.indexOf(' ', snippetEnd);
            if (spacePos > 0 && spacePos < snippetEnd + 50) {
                snippetEnd = spacePos;
            }
        }

        String snippet = text.substring(snippetStart, snippetEnd);
        if (snippetStart > 0) {
            snippet = "..." + snippet;
        }
        if (snippetEnd < text.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }
}

