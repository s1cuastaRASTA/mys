# #injuraturi — arena de roast

Recreare modernă a spiritului canalului `#injuraturi` de pe mIRC: intri cu o
poreclă, camere multiple, dueluri 1v1 votate de comunitate, badge-uri și
clasament. Fără cont, fără parolă — exact ca acum 20 de ani.

## Ce conține

- `index.html` — structura paginii
- `style.css` — tema vizuală (terminal / cărbuni aprinși)
- `app.js` — toată logica: camere, mesaje, voturi, dueluri, clasament, filtru
- `firebase-config.js` — aici pui cheile tale de Firebase (vezi pasul 1)

## Pasul 1 — configurează Firebase (baza de date, gratuită)

GitHub Pages servește doar fișiere statice, așa că avem nevoie de un loc unde
mesajele/voturile să fie salvate și văzute de toată lumea în timp real.
Firebase Realtime Database face asta gratuit, fără server propriu.

1. Mergi pe [console.firebase.google.com](https://console.firebase.google.com)
   și creează un proiect nou (poți folosi orice cont Google).
2. In pagina proiectului, apasă iconița web `</>` ca să adaugi o "Web app".
   Nu ai nevoie de Firebase Hosting — doar copiază obiectul `firebaseConfig`
   pe care ți-l arată.
3. Deschide `firebase-config.js` din acest proiect și înlocuiește valorile
   placeholder cu cele copiate.
4. In meniul din stânga: **Build → Realtime Database → Create Database**.
   Alege orice regiune, pornește în **test mode** (permite citire/scriere
   fără autentificare — e suficient pentru un proiect mic/prieteni).
5. După ce ai testat că totul merge, e recomandat să restrângi puțin regulile
   din tab-ul **Rules**, ca să nu poată oricine șterge tot din greșeală:

```json
{
  "rules": {
    ".read": true,
    ".write": true,
    "messages": {
      "$room": {
        "$msg": {
          ".validate": "newData.hasChildren(['nick','text','ts'])"
        }
      }
    }
  }
}
```

Astea sunt reguli minimale (oricine poate scrie) — potrivite pentru un
proiect mic cu prieteni. Dacă vrei protecție reală împotriva spam-ului sau
a atacurilor, ai nevoie de Firebase Authentication (autentificare anonimă),
ceea ce e un pas în plus pe care ți-l pot adăuga dacă vrei.

## Pasul 2 — pune-l live pe GitHub Pages

1. Creează un repo nou pe GitHub (poate fi public).
2. Urcă toate fișierele din acest folder (`index.html`, `style.css`,
   `app.js`, `firebase-config.js` — cu cheile tale deja completate).
3. In repo, mergi la **Settings → Pages**.
4. La "Source", alege branch-ul `main` și folderul `/ (root)`.
5. Salvează. In 1-2 minute, site-ul e live la
   `https://<username-ul-tau>.github.io/<numele-repo-ului>/`.

Nu e nevoie de build step, npm, sau server — sunt fișiere statice simple.

## Notă despre `firebase-config.js` fiind public

Cheile din `firebase-config.js` nu sunt secrete — sunt identificatori publici
de proiect (Google le construiește special pentru a fi expuse în cod client).
Protecția reală vine din **Database Rules** (pasul 4-5 de mai sus), nu din
ascunderea acestui fișier. E normal și sigur să-l urci pe GitHub.

## Cum funcționează filtrul de moderare

In `app.js`, funcția `containsHateSpeech()` verifică mesajele înainte să fie
trimise. Din start lista `HATE_SPEECH_PATTERNS` e goală — adaugă-ți propriile
cuvinte/expresii interzise (atacuri pe bază de rasă, etnie, orientare
sexuală, dizabilitate), nu injurăturile "clasice", care rămân libere.

Pentru o detecție mai bună decât o listă fixă de cuvinte (care oricum poate
fi ocolită ușor cu greșeli de scriere intenționate), poți integra gratuit
[Perspective API](https://perspectiveapi.com/) de la Google (Jigsaw) —
analizează un text și-ți dă un scor de "toxicitate/ură", nu doar potrivire
exactă de cuvinte. E un upgrade bun de făcut după ce ai prototipul de bază
funcțional.

## Idei de extins mai departe

- Autentificare anonimă Firebase, ca să nu poată cineva să fure porecla
  altcuiva și să posteze in numele lui.
- Notificări/sunete când ești provocat la duel.
- Reset automat săptămânal al clasamentului (acum e cumulativ pe toate
  mesajele din istoric).
- Buton de raportare pe fiecare mesaj, vizibil doar ție ca admin.
