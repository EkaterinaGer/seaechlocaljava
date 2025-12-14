// Navigation
document.addEventListener('DOMContentLoaded', function() {
    // Setup navigation
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');
    
    navItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            
            // Remove active class from all nav items and pages
            navItems.forEach(nav => nav.classList.remove('active'));
            pages.forEach(page => page.classList.remove('active'));
            
            // Add active class to clicked nav item
            this.classList.add('active');
            
            // Show corresponding page
            const pageName = this.getAttribute('data-page');
            const targetPage = document.getElementById(pageName + '-page');
            if (targetPage) {
                targetPage.classList.add('active');
            }
        });
    });
    
    // Load initial stats
    refreshStats();
    
    // Auto-refresh stats every 5 seconds
    setInterval(refreshStats, 5000);
});

async function startIndexing() {
    const url = document.getElementById('crawlUrl').value.trim();
    if (!url) {
        alert('Введите URL');
        return;
    }
    
    const statusDiv = document.getElementById('crawlStatus');
    statusDiv.className = 'status-message';
    statusDiv.innerHTML = '<p>Начинаю индексацию...</p>';
    
    try {
        const response = await fetch('/api/startIndexing?url=' + encodeURIComponent(url), {
            method: 'POST'
        });
        
        const data = await response.json();
        if (data.result) {
            statusDiv.className = 'status-message success';
            statusDiv.innerHTML = '<p>Индексация начата. Обновите страницу через несколько секунд.</p>';
            setTimeout(refreshStats, 2000);
        } else {
            statusDiv.className = 'status-message error';
            statusDiv.innerHTML = '<p>Ошибка: ' + (data.error || 'Неизвестная ошибка') + '</p>';
        }
    } catch (error) {
        statusDiv.className = 'status-message error';
        statusDiv.innerHTML = '<p>Ошибка: ' + error.message + '</p>';
    }
}

async function stopIndexing() {
    try {
        const response = await fetch('/api/stopIndexing', {
            method: 'POST'
        });
        
        const data = await response.json();
        if (data.result) {
            alert('Индексация остановлена');
            refreshStats();
        } else {
            alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

async function indexPage() {
    const url = document.getElementById('pageUrl').value.trim();
    if (!url) {
        alert('Введите URL страницы');
        return;
    }
    
    const statusDiv = document.getElementById('pageStatus');
    statusDiv.className = 'status-message';
    statusDiv.innerHTML = '<p>Индексирую страницу...</p>';
    
    try {
        const response = await fetch('/api/indexPage?url=' + encodeURIComponent(url), {
            method: 'POST'
        });
        
        const data = await response.json();
        if (data.result) {
            statusDiv.className = 'status-message success';
            statusDiv.innerHTML = '<p>Страница проиндексирована</p>';
            refreshStats();
        } else {
            statusDiv.className = 'status-message error';
            statusDiv.innerHTML = '<p>Ошибка: ' + (data.error || 'Неизвестная ошибка') + '</p>';
        }
    } catch (error) {
        statusDiv.className = 'status-message error';
        statusDiv.innerHTML = '<p>Ошибка: ' + error.message + '</p>';
    }
}

async function performSearch() {
    const query = document.getElementById('searchQuery').value.trim();
    if (!query) {
        alert('Введите поисковый запрос');
        return;
    }
    
    const resultsDiv = document.getElementById('searchResults');
    resultsDiv.innerHTML = '<p>Поиск...</p>';
    
    try {
        const response = await fetch('/api/search?query=' + encodeURIComponent(query));
        const data = await response.json();
        
        if (data.result && data.data && data.data.length > 0) {
            let html = '<h3 style="margin-bottom: 20px; color: #2c3e50;">Найдено результатов: ' + data.count + '</h3>';
            data.data.forEach((result, index) => {
                html += `
                    <div class="result-item">
                        <div class="result-title">${index + 1}. ${result.title || 'Без названия'}</div>
                        <div class="result-url">${result.url}</div>
                        <div class="result-snippet">${result.snippet || ''}</div>
                        <div class="result-relevance">Релевантность: ${result.relevance ? result.relevance.toFixed(4) : 'N/A'}</div>
                    </div>
                `;
            });
            resultsDiv.innerHTML = html;
        } else {
            resultsDiv.innerHTML = '<p style="color: #7f8c8d;">Ничего не найдено.</p>';
        }
    } catch (error) {
        resultsDiv.innerHTML = '<p style="color: #e74c3c;">Ошибка: ' + error.message + '</p>';
    }
}

async function refreshStats() {
    try {
        const response = await fetch('/api/statistics');
        const data = await response.json();
        
        // Обновляем статистику
        const statSitesEl = document.getElementById('statSites');
        const statPagesEl = document.getElementById('statPages');
        const statLemmasEl = document.getElementById('statLemmas');
        
        if (statSitesEl) statSitesEl.textContent = data.total.sites || 0;
        if (statPagesEl) statPagesEl.textContent = data.total.pages || 0;
        if (statLemmasEl) statLemmasEl.textContent = data.total.lemmas || 0;
        
        // Обновляем список сайтов
        const sitesList = document.getElementById('sitesList');
        if (sitesList) {
            let html = '';
            if (data.detailed && data.detailed.length > 0) {
                data.detailed.forEach(site => {
                    const statusClass = site.status === 'INDEXED' ? 'status-indexed' : 
                                       site.status === 'INDEXING' ? 'status-indexing' : 'status-failed';
                    const statusText = site.status === 'INDEXED' ? 'INDEXED' : 
                                      site.status === 'INDEXING' ? 'INDEXING' : 'FAILED';
                    
                    html += `
                        <div class="site-item">
                            <div class="site-info">
                                <div>
                                    <div class="site-name">${site.name || 'Без названия'}</div>
                                    <div class="site-url">${site.url}</div>
                                </div>
                            </div>
                            <span class="site-status ${statusClass}">${statusText}</span>
                            <span class="site-arrow">▼</span>
                        </div>
                    `;
                });
            } else {
                html = '<p style="color: #7f8c8d; padding: 20px; text-align: center;">Нет проиндексированных сайтов</p>';
            }
            sitesList.innerHTML = html;
        }
    } catch (error) {
        console.error('Ошибка обновления статистики:', error);
    }
}
