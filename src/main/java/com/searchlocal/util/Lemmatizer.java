package com.searchlocal.util;

import com.github.demidko.aot.WordformMeaning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

/**
 * Класс для лемматизации текста
 * Использует библиотеку AOT для морфологического анализа русского языка
 */
public class Lemmatizer {
    private static final Logger logger = LoggerFactory.getLogger(Lemmatizer.class);
    
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}]+");
    
    // Служебные части речи для исключения
    private static final Set<String> EXCLUDED_POS = Set.of(
            "МЕЖД", "СОЮЗ", "ПРЕДЛ", "ЧАСТ"
    );
    
    public Lemmatizer() {
        // AOT библиотека не требует явной инициализации
    }
    
    /**
     * Извлекает леммы из текста и возвращает их количество
     * Использует библиотеку AOT для получения нормальных форм слов
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
            // Пропускаем короткие слова
            if (word.length() < 2 || !WORD_PATTERN.matcher(word).matches()) {
                continue;
            }
            
            try {
                // Получаем морфологическую информацию о слове
                List<WordformMeaning> meanings = lookupForMeanings(word);
                
                if (meanings.isEmpty()) {
                    // Если слово не найдено в словаре, используем его как есть
                    lemmas.put(word, lemmas.getOrDefault(word, 0) + 1);
                    continue;
                }
                
                // Берем первое значение (основное)
                WordformMeaning meaning = meanings.get(0);
                
                // Проверяем часть речи через морфологию
                String morphologyStr = meaning.getMorphology().toString();
                boolean isExcluded = EXCLUDED_POS.stream()
                        .anyMatch(morphologyStr::contains);
                
                if (isExcluded) {
                    continue;
                }
                
                // Получаем лемму (нормальную форму) - метод возвращает WordformMeaning, нужно преобразовать в String
                WordformMeaning lemmaMeaning = meaning.getLemma();
                if (lemmaMeaning != null) {
                    String lemma = lemmaMeaning.toString();
                    if (!lemma.isEmpty()) {
                        lemmas.put(lemma.toLowerCase(), lemmas.getOrDefault(lemma.toLowerCase(), 0) + 1);
                    }
                }
            } catch (Exception e) {
                // Если произошла ошибка при обработке слова, используем его как есть
                logger.debug("Ошибка при обработке слова {}: {}", word, e.getMessage());
                lemmas.put(word, lemmas.getOrDefault(word, 0) + 1);
            }
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
