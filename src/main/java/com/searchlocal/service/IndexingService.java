package com.searchlocal.service;

import com.searchlocal.model.*;
import com.searchlocal.repository.*;
import com.searchlocal.util.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@Service
public class IndexingService {
    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);
    
    @Autowired
    private SiteRepository siteRepository;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private LemmaRepository lemmaRepository;
    
    @Autowired
    private IndexRepository indexRepository;
    
    private final Lemmatizer lemmatizer = new Lemmatizer();
    
    /**
     * Индексирует отдельную страницу
     */
    @Transactional
    public void indexPage(String url, String siteUrl) {
        try {
            // Проверяем, что страница принадлежит указанному сайту
            if (!url.startsWith(siteUrl)) {
                throw new IllegalArgumentException("Страница не принадлежит указанному сайту");
            }
            
            // Получаем или создаем сайт
            Site site = siteRepository.findByUrl(siteUrl)
                    .orElseGet(() -> {
                        Site newSite = new Site(siteUrl, extractSiteName(siteUrl));
                        return siteRepository.save(newSite);
                    });
            
            // Извлекаем путь страницы
            String path = extractPath(url, siteUrl);
            
            // Проверяем, существует ли страница
            Optional<Page> existingPage = pageRepository.findBySiteAndPath(site, path);
            if (existingPage.isPresent()) {
                // Удаляем старую информацию
                deletePageData(existingPage.get());
            }
            
            // Загружаем страницу
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .get();
            
            int statusCode = doc.connection().response().statusCode();
            
            // Сохраняем только страницы с успешными кодами
            if (statusCode >= 400) {
                logger.warn("Пропущена страница с кодом ошибки {}: {}", statusCode, url);
                return;
            }
            
            String html = doc.html();
            String cleanText = lemmatizer.cleanHtml(html);
            
            // Создаем запись страницы
            Page page = new Page(site, path, statusCode, html);
            page = pageRepository.save(page);
            
            // Получаем леммы
            Map<String, Integer> lemmas = lemmatizer.getLemmas(cleanText);
            
            // Сохраняем леммы и индексы
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                String lemmaText = entry.getKey();
                Integer count = entry.getValue();
                
                // Получаем или создаем лемму
                Lemma lemma = lemmaRepository.findByLemma(lemmaText)
                        .orElseGet(() -> {
                            Lemma newLemma = new Lemma(lemmaText, 0);
                            return lemmaRepository.save(newLemma);
                        });
                
                // Увеличиваем frequency только если это новая страница для этой леммы
                if (!indexRepository.findByPage(page).stream()
                        .anyMatch(idx -> idx.getLemma().equals(lemma))) {
                    lemma.setFrequency(lemma.getFrequency() + 1);
                    lemmaRepository.save(lemma);
                }
                
                // Создаем запись в индексе
                Index index = new Index(page, lemma, count.floatValue());
                indexRepository.save(index);
            }
            
            logger.info("Страница проиндексирована: {}", url);
            
        } catch (IOException e) {
            logger.error("Ошибка при загрузке страницы {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить страницу: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы {}: {}", url, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Удаляет данные страницы из всех таблиц
     */
    @Transactional
    private void deletePageData(Page page) {
        // Удаляем индексы
        indexRepository.findByPage(page).forEach(index -> {
            Lemma lemma = index.getLemma();
            indexRepository.delete(index);
            
            // Уменьшаем frequency леммы
            lemma.setFrequency(Math.max(0, lemma.getFrequency() - 1));
            if (lemma.getFrequency() == 0) {
                lemmaRepository.delete(lemma);
            } else {
                lemmaRepository.save(lemma);
            }
        });
        
        // Удаляем страницу
        pageRepository.delete(page);
    }
    
    /**
     * Извлекает имя сайта из URL
     */
    private String extractSiteName(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host != null ? host : url;
        } catch (URISyntaxException e) {
            return url;
        }
    }
    
    /**
     * Извлекает путь страницы относительно сайта
     */
    private String extractPath(String pageUrl, String siteUrl) {
        try {
            URI siteUri = new URI(siteUrl);
            URI pageUri = new URI(pageUrl);
            
            String path = pageUri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            
            String query = pageUri.getQuery();
            if (query != null && !query.isEmpty()) {
                path += "?" + query;
            }
            
            return path;
        } catch (URISyntaxException e) {
            return pageUrl.replace(siteUrl, "");
        }
    }
}

