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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CrawlingService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlingService.class);
    
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final IndexingService indexingService;
    
    public CrawlingService(
            SiteRepository siteRepository,
            PageRepository pageRepository,
            LemmaRepository lemmaRepository,
            IndexRepository indexRepository,
            IndexingService indexingService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.indexingService = indexingService;
    }
    
    private final Lemmatizer lemmatizer = new Lemmatizer();
    private final Map<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    
    /**
     * Запускает индексацию сайта
     */
    public void startIndexing(String siteUrl) {
        stopFlags.put(siteUrl, new AtomicBoolean(false));
        
        CompletableFuture.runAsync(() -> {
            try {
                Site site = siteRepository.findByUrl(siteUrl)
                        .orElseGet(() -> {
                            Site newSite = new Site(siteUrl, extractSiteName(siteUrl));
                            return siteRepository.save(newSite);
                        });
                
                // Удаляем старые данные
                deleteSiteData(site);
                
                // Обновляем статус
                site.setStatus(SiteStatus.INDEXING);
                site.setLastError(null);
                siteRepository.save(site);
                
                // Обходим сайт
                crawlSite(site, siteUrl);
                
                // Обновляем статус
                if (!stopFlags.get(siteUrl).get()) {
                    site.setStatus(SiteStatus.INDEXED);
                    siteRepository.save(site);
                    logger.info("Индексация сайта {} завершена", siteUrl);
                }
                
            } catch (Exception e) {
                logger.error("Ошибка при индексации сайта {}: {}", siteUrl, e.getMessage(), e);
                Site site = siteRepository.findByUrl(siteUrl).orElse(null);
                if (site != null) {
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError(e.getMessage());
                    siteRepository.save(site);
                }
            } finally {
                stopFlags.remove(siteUrl);
            }
        });
    }
    
    /**
     * Останавливает индексацию
     */
    public void stopIndexing() {
        stopFlags.values().forEach(flag -> flag.set(true));
        
        // Обновляем статусы всех индексируемых сайтов
        List<Site> indexingSites = siteRepository.findByStatus(SiteStatus.INDEXING);
        for (Site site : indexingSites) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            siteRepository.save(site);
        }
    }
    
    /**
     * Обходит сайт
     */
    private void crawlSite(Site site, String baseUrl) {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.offer(baseUrl);
        visited.add(baseUrl);
        
        ForkJoinPool pool = new ForkJoinPool();
        
        while (!queue.isEmpty() && !stopFlags.get(site.getUrl()).get()) {
            String url = queue.poll();
            if (url == null) continue;
            
            try {
                // Задержка между запросами
                Thread.sleep(500 + (long)(Math.random() * 4500));
                
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .referrer("http://www.google.com")
                        .timeout(10000)
                        .get();
                
                int statusCode = doc.connection().response().statusCode();
                
                if (statusCode >= 400) {
                    continue;
                }
                
                String path = extractPath(url, baseUrl);
                
                // Проверяем, не посещали ли мы эту страницу
                if (pageRepository.existsBySiteAndPath(site, path)) {
                    continue;
                }
                
                String html = doc.html();
                String cleanText = lemmatizer.cleanHtml(html);
                
                // Сохраняем страницу
                Page page = new Page(site, path, statusCode, html);
                page = pageRepository.save(page);
                
                // Обновляем время статуса
                site.setStatusTime(java.time.LocalDateTime.now());
                siteRepository.save(site);
                
                // Индексируем страницу
                indexPage(page, cleanText);
                
                // Извлекаем ссылки
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("abs:href");
                    if (href != null && !href.isEmpty() && href.startsWith(baseUrl)) {
                        if (!visited.contains(href)) {
                            visited.add(href);
                            queue.offer(href);
                        }
                    }
                }
                
            } catch (Exception e) {
                logger.warn("Ошибка при обходе страницы {}: {}", url, e.getMessage());
            }
        }
        
        pool.shutdown();
    }
    
    /**
     * Индексирует страницу (лемматизация и сохранение)
     */
    @Transactional
    private void indexPage(Page page, String cleanText) {
        Map<String, Integer> lemmas = lemmatizer.getLemmas(cleanText);
        
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaText = entry.getKey();
            Integer count = entry.getValue();
            
            // Получаем или создаем лемму
            Lemma lemma = lemmaRepository.findByLemma(lemmaText)
                    .orElseGet(() -> {
                        Lemma newLemma = new Lemma(lemmaText, 0);
                        return lemmaRepository.save(newLemma);
                    });
            
            // Увеличиваем frequency
            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);
            
            // Создаем запись в индексе
            Index index = new Index(page, lemma, count.floatValue());
            indexRepository.save(index);
        }
    }
    
    /**
     * Удаляет данные сайта
     */
    @Transactional
    private void deleteSiteData(Site site) {
        List<Page> pages = pageRepository.findAll().stream()
                .filter(p -> p.getSite().equals(site))
                .toList();
        
        for (Page page : pages) {
            indexRepository.findByPage(page).forEach(index -> {
                Lemma lemma = index.getLemma();
                indexRepository.delete(index);
                
                lemma.setFrequency(Math.max(0, lemma.getFrequency() - 1));
                if (lemma.getFrequency() == 0) {
                    lemmaRepository.delete(lemma);
                } else {
                    lemmaRepository.save(lemma);
                }
            });
            
            pageRepository.delete(page);
        }
    }
    
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
    
    private String extractPath(String pageUrl, String siteUrl) {
        try {
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

