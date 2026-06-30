// Service worker care cacheuieste shell-ul aplicatiei (HTML, manifest, icoane)
// ca pagina sa se deschida instant si offline. Lista de canale e statica, deja
// inclusa in index.html, asa ca odata cache-uit shell-ul, intreaga lista de
// canale e disponibila fara semnal. Doar redarea live (iframe-urile catre
// tvonline123.eu / canale-tv.net) tot are nevoie de internet.

const CACHE_NAME = 'tv-online-shell-v1';
const SHELL_FILES = [
    './',
    './index.html',
    './tv-manifest.json',
    './tv-icon-192.png',
    './tv-icon-512.png'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(SHELL_FILES))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
        ).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    const req = event.request;
    if (req.method !== 'GET') return;
    const url = new URL(req.url);

    // Doar fisierele proprii (shell-ul) trec prin cache. Embed-urile video
    // (tvonline123.eu, canale-tv.net etc.) merg mereu direct la retea.
    if (url.origin !== self.location.origin) return;

    event.respondWith(
        caches.match(req).then(cached => {
            const networkFetch = fetch(req).then(res => {
                if (res && res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE_NAME).then(cache => cache.put(req, copy));
                }
                return res;
            }).catch(() => cached);
            // Stale-while-revalidate: raspunde imediat din cache daca exista,
            // si actualizeaza in fundal cand exista semnal.
            return cached || networkFetch;
        })
    );
});
