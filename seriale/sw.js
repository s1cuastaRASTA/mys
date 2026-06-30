// Service worker simplu: cacheuieste shell-ul aplicatiei (HTML, manifest, icoane)
// ca aplicatia sa se deschida instant si offline. Episoadele/video-urile tot au
// nevoie de net (sunt embed-uri externe), dar lista de seriale/sezoane manuale
// (Las Fierbinti are zeci de sezoane definite direct in cod) merge si fara semnal.

const CACHE_NAME = 'seriale-shell-v1';
const SHELL_FILES = [
    './',
    './index.html',
    './seriale-manifest.json',
    './icon-192.png',
    './icon-512.png'
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

    // Doar fisierele proprii (shell-ul) trec prin cache.
    // API-ul WordPress, proxy-urile CORS si embed-urile video merg mereu direct
    // la retea (sunt date dinamice / cross-origin si nu trebuie cache-uite aici).
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
            // si actualizeaza cache-ul in fundal cand exista semnal.
            return cached || networkFetch;
        })
    );
});
