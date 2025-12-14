package com.searchlocal.service;

import com.searchlocal.model.*;
import com.searchlocal.repository.*;
import com.searchlocal.util.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    // Процент страниц, при превышении которого лемма исключается (80%)
    private static final double MAX_LEMMA_PERCENTAGE = 0.8;
    
    @Autowired
    private LemmaRepository lemmaRepository;
    
    @Autowired
    private IndexRepository indexRepository;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private SiteRepository siteRepository;
    
    private final Lemmatizer lemmatizer = new Lemmatizer();
    
    /**
     * Выполняет поиск по запросу согласно алгоритму
     */
    public List<SearchResult> search(String query, String siteUrl) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        logger.info("Поиск по запросу: '{}', сайт: {}", query, siteUrl);
        
        // 1. Разбиваем запрос на слова и формируем список уникальных лемм
        Map<String, Integer> queryLemmasMap = lemmatizer.getLemmas(query);
        Set<String> queryLemmaStrings = queryLemmasMap.keySet();
        
        if (queryLemmaStrings.isEmpty()) {
            logger.info("Не найдено лемм в запросе");
            return Collections.emptyList();
        }
        
        // Получаем общее количество страниц для расчета процента
        long totalPages = siteUrl != null 
            ? pageRepository.findAll().stream()
                .filter(p -> p.getSite().getUrl().equals(siteUrl))
                .count()
            : pageRepository.count();
        
        if (totalPages == 0) {
            return Collections.emptyList();
        }
        
        // 2. Находим леммы в БД и исключаем те, что встречаются на слишком большом количестве страниц
        // Если страниц мало (меньше 10), не применяем фильтр по проценту
        boolean applyPercentageFilter = totalPages >= 10;
        
        List<Lemma> foundLemmas = new ArrayList<>();
        for (String lemmaText : queryLemmaStrings) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findByLemma(lemmaText);
            if (lemmaOpt.isPresent()) {
                Lemma lemma = lemmaOpt.get();
                
                if (applyPercentageFilter) {
                    // Исключаем леммы, которые встречаются на более чем MAX_LEMMA_PERCENTAGE страниц
                    double lemmaPercentage = (double) lemma.getFrequency() / totalPages;
                    if (lemmaPercentage <= MAX_LEMMA_PERCENTAGE) {
                        foundLemmas.add(lemma);
                    } else {
                        logger.debug("Лемма '{}' исключена (встречается на {:.2f}% страниц)", 
                            lemmaText, lemmaPercentage * 100);
                    }
                } else {
                    // Если страниц мало, не фильтруем по проценту
                    foundLemmas.add(lemma);
                }
            }
        }
        
        if (foundLemmas.isEmpty()) {
            logger.info("Не найдено подходящих лемм после фильтрации");
            return Collections.emptyList();
        }
        
        // 3. Сортируем леммы по возрастанию частоты (от самых редких к самым частым)
        foundLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        
        logger.debug("Найдено {} лемм после фильтрации, отсортировано по частоте", foundLemmas.size());
        
        // 4. Находим страницы для самой редкой леммы
        Lemma firstLemma = foundLemmas.get(0);
        Set<Page> candidatePages = indexRepository.findByLemma(firstLemma).stream()
            .map(Index::getPage)
            .filter(page -> siteUrl == null || page.getSite().getUrl().equals(siteUrl))
            .collect(Collectors.toSet());
        
        if (candidatePages.isEmpty()) {
            logger.info("Не найдено страниц для первой леммы");
            return Collections.emptyList();
        }
        
        // 5. Для каждой следующей леммы находим страницы, которые есть и в предыдущем списке
        for (int i = 1; i < foundLemmas.size(); i++) {
            Lemma lemma = foundLemmas.get(i);
            Set<Page> pagesForLemma = indexRepository.findByLemma(lemma).stream()
                .map(Index::getPage)
                .filter(page -> siteUrl == null || page.getSite().getUrl().equals(siteUrl))
                .collect(Collectors.toSet());
            
            // Пересечение: оставляем только страницы, которые есть в обоих множествах
            candidatePages.retainAll(pagesForLemma);
            
            // 6. Если страниц не осталось - возвращаем пустой список
            if (candidatePages.isEmpty()) {
                logger.info("Не осталось страниц после обработки леммы {}", lemma.getLemma());
                return Collections.emptyList();
            }
        }
        
        logger.debug("Найдено {} страниц-кандидатов", candidatePages.size());
        
        // 7. Рассчитываем абсолютную релевантность для каждой страницы
        Map<Page, Double> absoluteRelevance = new HashMap<>();
        
        for (Page page : candidatePages) {
            double absRelevance = 0.0;
            for (Lemma lemma : foundLemmas) {
                Optional<Index> indexOpt = indexRepository.findByLemma(lemma).stream()
                    .filter(idx -> idx.getPage().equals(page))
                    .findFirst();
                if (indexOpt.isPresent()) {
                    absRelevance += indexOpt.get().getRank();
                }
            }
            absoluteRelevance.put(page, absRelevance);
        }
        
        // Находим максимальную абсолютную релевантность
        double maxAbsoluteRelevance = absoluteRelevance.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(1.0);
        
        // 8. Рассчитываем относительную релевантность и создаем результаты
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Page, Double> entry : absoluteRelevance.entrySet()) {
            Page page = entry.getKey();
            double absRel = entry.getValue();
            // Относительная релевантность = абсолютная / максимальная абсолютная
            double relativeRelevance = maxAbsoluteRelevance > 0 
                ? absRel / maxAbsoluteRelevance 
                : 0.0;
            
            String uri = page.getSite().getUrl() + page.getPath();
            String title = extractTitle(page.getContent());
            String snippet = createSnippet(page.getContent(), query, queryLemmaStrings);
            
            results.add(new SearchResult(uri, title, snippet, relativeRelevance));
        }
        
        // 9. Сортируем по убыванию относительной релевантности
        results.sort((a, b) -> Double.compare(b.getRelevance(), a.getRelevance()));
        
        logger.info("Найдено {} результатов поиска", results.size());
        
        return results;
    }
    
    /**
     * Извлекает заголовок из HTML
     */
    private String extractTitle(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        try {
            Document doc = Jsoup.parse(html);
            Element titleElement = doc.selectFirst("title");
            if (titleElement != null) {
                return titleElement.text().trim();
            }
            
            // Если нет title, пробуем h1
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) {
                return h1.text().trim();
            }
        } catch (Exception e) {
            logger.warn("Ошибка при извлечении заголовка: {}", e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Создает сниппет текста с выделением найденных слов
     */
    private String createSnippet(String html, String query, Set<String> queryLemmas) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        try {
            Document doc = Jsoup.parse(html);
            
            // Удаляем script и style
            doc.select("script, style").remove();
            
            // Получаем текст из body
            String text = doc.body() != null ? doc.body().text() : doc.text();
            
            if (text == null || text.isEmpty()) {
                return "";
            }
            
            // Ищем первое вхождение любого из слов запроса (в нижнем регистре)
            String lowerText = text.toLowerCase();
            int snippetStart = -1;
            int snippetLength = 200;
            
            for (String lemma : queryLemmas) {
                int pos = lowerText.indexOf(lemma.toLowerCase());
                if (pos != -1) {
                    snippetStart = Math.max(0, pos - 50);
                    break;
                }
            }
            
            if (snippetStart == -1) {
                snippetStart = 0;
            }
            
            // Берем фрагмент текста
            int end = Math.min(text.length(), snippetStart + snippetLength);
            String snippet = text.substring(snippetStart, end);
            
            // Выделяем найденные слова тегами <b>
            for (String lemma : queryLemmas) {
                // Используем регулярное выражение для поиска слова целиком (с учетом регистра)
                String pattern = "(?i)\\b(" + java.util.regex.Pattern.quote(lemma) + ")\\b";
                snippet = snippet.replaceAll(pattern, "<b>$1</b>");
            }
            
            // Добавляем многоточие, если текст обрезан
            if (snippetStart > 0) {
                snippet = "..." + snippet;
            }
            if (end < text.length()) {
                snippet = snippet + "...";
            }
            
            return snippet.trim();
            
        } catch (Exception e) {
            logger.warn("Ошибка при создании сниппета: {}", e.getMessage());
            // Fallback: просто очищаем HTML
            String text = lemmatizer.cleanHtml(html);
            if (text.length() > 200) {
                text = text.substring(0, 200) + "...";
            }
            return text;
        }
    }
}
