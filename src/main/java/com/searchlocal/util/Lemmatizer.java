package com.searchlocal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Класс для лемматизации текста
 * Упрощенная версия без внешних морфологических библиотек
 */
public class Lemmatizer {
    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}]+");
    
    // Простой список служебных слов для исключения
    private static final Set<String> STOP_WORDS = Set.of(
            "и", "в", "на", "с", "по", "для", "от", "до", "из", "к", "о", "у", "за", "со",
            "а", "но", "или", "что", "как", "так", "это", "то", "же", "бы", "ли", "не",
            "он", "она", "они", "мы", "вы", "я", "ты", "его", "её", "их", "мой", "твой",
            "был", "была", "было", "были", "есть", "будет", "быть"
    );
    
    public Lemmatizer() {
        // Упрощенная версия не требует инициализации
    }
    
    /**
     * Извлекает леммы из текста и возвращает их количество
     * В упрощенной версии просто нормализует слова (приводит к нижнему регистру)
     */
    public Map<String, Integer> getLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        
        if (text == null || text.isEmpty()) {
            return lemmas;
        }
        
        // Разбиваем текст на слова
        String[] words = text.toLowerCase()
                .replaceAll("[^\\p{L}\\s]", " ")
                .split("\\s+");
        
        for (String word : words) {
            // Пропускаем короткие слова и служебные части речи
            if (word.length() < 2 || !WORD_PATTERN.matcher(word).matches()) {
                continue;
            }
            
            // Пропускаем стоп-слова
            if (STOP_WORDS.contains(word)) {
                continue;
            }
            
            // В упрощенной версии используем слово как есть (уже в нижнем регистре)
            String lemma = word;
            lemmas.put(lemma, lemmas.getOrDefault(lemma, 0) + 1);
        }
        
        return lemmas;
    }
    
    /**
     * Очищает HTML от тегов
     */
    public String cleanHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        return html.replaceAll("<script[^>]*>.*?</script>", " ")
                .replaceAll("<style[^>]*>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&[a-z]+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
