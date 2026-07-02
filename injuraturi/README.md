# #injuraturi — arena de roast

Recreare modernă a spiritului canalului `#injuraturi` de pe mIRC: intri cu o
poreclă (sau cu cont), camere multiple, dueluri 1v1 votate de comunitate,
badge-uri, clasament săptămânal, raportare și panou de admin.

## Ce conține

- `index.html` — structura paginii
- `style.css` — tema vizuală (terminal / cărbuni aprinși)
- `app.js` — toată logica: camere, mesaje, voturi, dueluri, clasament,
  conturi, filtru, raportare, admin
- `firebase-config.js` — aici pui cheile tale de Firebase

## Pasul 1 — configurează Firebase (baza de date, gratuită)

GitHub Pages servește doar fișiere statice, așa că avem nevoie de un loc unde
mesajele/voturile să fie salvate și văzute de toată lumea în timp real.

1. Mergi pe [console.firebase.google.com](https://console.firebase.google.com)
   și creează un proiect nou.
2. Apasă iconița web `</>` ca să adaugi o "Web app", copiază obiectul
   `firebaseConfig` pe care ți-l arată.
3. Deschide `firebase-config.js` și înlocuiește valorile cu cele copiate.
4. In meniul din stânga (poate apărea ca "Build" sau "Databases & Storage"):
   **Realtime Database → Create Database**. Alege orice regiune, pornește în
   **test mode** (o să înlocuim regulile la Pasul 4 de mai jos).

## Pasul 2 — activează autentificarea (poreclă + cont)

Aplicația foloseşte Firebase Authentication pentru **toată lumea**, inclusiv
pentru cei care intră doar cu poreclă (fac o autentificare anonimă invizibilă
pentru ei) — asta ne dă un identificator unic per-utilizator, necesar ca să
poți bloca pe cineva sau proteja o poreclă.

1. In consola Firebase → **Authentication** → tab-ul **Sign-in method**.
2. Activează providerul **Anonymous** → Save. *(fără el, intrarea rapidă cu
   poreclă nu mai funcționează deloc)*
3. Activează providerul **Email/Password** → Save.
4. Activează providerul **Google** → alege un email de suport → Save.
5. Mergi la **Settings → Authorized domains** și adaugă domeniul unde va fi
   live site-ul, de tipul `<username-ul-tau>.github.io` (fără `https://` și
   fără calea repo-ului). Fără acest pas, "continuă cu Google" nu merge pe
   site-ul live (pe `localhost` e deja autorizat automat).

## Pasul 3 — devino admin

Ca să vezi rapoartele și să poți șterge mesaje/bloca oameni:

1. Intră o dată în aplicație cu contul cu care vrei să fii admin (email sau
   Google).
2. In consola Firebase → **Realtime Database → Data**, găsește nodul `users`
   → deschide-ți contul → copiază-ți `uid`-ul (e cheia părinte, un șir gen
   `aBcD1234...`). Alternativ, în **Authentication → Users**, coloana `User UID`.
3. Tot în tab-ul **Data**, adaugă manual un nod nou la rădăcină:
   `admins/<uid-ul-tau>` cu valoarea `true`.
4. Reintră în aplicație (sau dă refresh + reautentificare) — ar trebui să
   apară butonul roșu "🛡️ panou admin" în meniul din stânga.

Nodul `admins` e scris intenționat doar din consolă, nu din aplicație, ca
nimeni să nu se poată auto-numi admin din cod.

## Pasul 4 — reguli de securitate reale (IMPORTANT pentru lansare publică)

Cu "test mode", oricine poate scrie/șterge orice din baza de date. Inainte
să dai linkul public, înlocuiește regulile din **Realtime Database → Rules**
cu ceva de genul:

```json
{
  "rules": {
    "rooms": {
      ".read": true,
      "$room": { ".write": "auth != null" }
    },
    "nicknames": {
      ".read": true,
      "$nick": { ".write": "auth != null" }
    },
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "admins": {
      ".read": "auth != null",
      ".write": false
    },
    "banned": {
      ".read": "auth != null",
      "$uid": {
        ".write": "auth != null && root.child('admins').child(auth.uid).exists()"
      }
    },
    "messages": {
      "$room": {
        "$msg": {
          ".read": true,
          ".write": "auth != null && !root.child('banned').child(auth.uid).exists() && (!data.exists() || root.child('admins').child(auth.uid).exists())",
          ".validate": "newData.hasChildren(['nick','uid','text','ts']) && newData.child('text').val().length <= 280 && newData.child('nick').val().length <= 24",
          "votes": { ".write": "auth != null && !root.child('banned').child(auth.uid).exists()" },
          "voters": {
            "$uid": { ".write": "auth != null && auth.uid == $uid" }
          }
        }
      }
    },
    "duels": {
      "$room": {
        "$duel": {
          ".read": true,
          ".write": "auth != null && !root.child('banned').child(auth.uid).exists()"
        }
      }
    },
    "reports": {
      "$room": {
        ".read": "auth != null && root.child('admins').child(auth.uid).exists()",
        "$report": { ".write": "auth != null" }
      }
    },
    "moderation": {
      "blockedWords": {
        ".read": true,
        ".write": "auth != null && root.child('admins').child(auth.uid).exists()"
      }
    }
  }
}
```

Ce fac aceste reguli, pe scurt: doar utilizatori autentificați (inclusiv
anonim) pot scrie; un utilizator blocat (`banned/{uid}`) nu mai poate scrie
nimic nicăieri; mesajele odată create nu mai pot fi editate/șterse decât de
un admin; oricine poate vota, dar votul fiecăruia e legat de propriul `uid`;
doar adminii pot citi rapoartele.

**Notă onestă**: regulile de mai sus sunt un nivel bun de protecție de bază
pentru un proiect mic-mediu, dar nu sunt "audit de securitate profesionist".
Dacă aplicația crește mult, merită revizuite de cineva cu experiență în
Firebase Rules — sunt ușor de scris greșit în moduri subtile.

## Pasul 5 — (opțional, dar recomandat) filtru de ură mai bun

In `app.js`, `HATE_SPEECH_PATTERNS` e gol by default — trebuie completat cu
propriile cuvinte/expresii interzise, sau (mai bine) integrezi
[Perspective API](https://perspectiveapi.com/) de la Google (gratuit):

1. Ia o cheie API de pe [console.cloud.google.com](https://console.cloud.google.com)
   → activezi "Perspective Comment Analyzer API" → creezi o cheie.
2. Restrânge cheia la domeniul tău GitHub Pages (ca la Google Maps API), ca
   să nu poată fi folosită abuziv de altcineva.
3. Pune cheia în `PERSPECTIVE_API_KEY` din `app.js`.

Fără cheie, aplicația funcționează doar cu filtrul de cuvinte local.

## Pasul 6 — pune-l live pe GitHub Pages

1. Creează un repo nou pe GitHub (poate fi public).
2. Urcă toate fișierele din acest folder.
3. **Settings → Pages** → branch `main`, folder `/ (root)` → Save.
4. In 1-2 minute e live la `https://<username-ul-tau>.github.io/<repo>/`.

## Notă despre `firebase-config.js` fiind public

Cheile din `firebase-config.js` nu sunt secrete — sunt identificatori publici
de proiect. Protecția reală vine din **Database Rules** (Pasul 4), nu din
ascunderea acestui fișier.

## Limitări reale, dacă devine cu adevărat public (nu doar prieteni)

Sunt sincer aici pentru că e important: cu arhitectura asta (site static +
Firebase gratuit, fără server propriu), există limite pe care niciun cod nu
le rezolvă complet:

- **Rate limiting-ul e "soft"** — cooldown-ul de 3 secunde e verificat în
  browser-ul utilizatorului, nu pe un server. Cineva cu cunoștințe tehnice
  poate ocoli asta. Rate limiting real, la nivel de server, ar cere Firebase
  Cloud Functions (plan Blaze, cu cost la scară mare) sau alt backend.
- **Un utilizator blocat poate reveni cu o sesiune anonimă nouă** — ștergerea
  datelor din browser (sau alt device) îi dă un `uid` anonim nou. Contul cu
  email/Google e mult mai greu de ocolit, deci dacă vrei protecție serioasă,
  ia în calcul să ceri cont obligatoriu, nu doar poreclă rapidă.
- **Filtrul automat, oricât de bun, nu prinde tot** — nicio combinație de
  cuvinte-cheie sau AI de moderare nu e perfectă. Platforme comparabile
  (ex. r/RoastMe) funcționează cu moderatori umani activi, nu doar filtre.
  Dacă publicul crește, ia în calcul un mod de raportare + un om care
  verifică rapoartele des (panoul de admin ajută la asta, dar tot cere timp
  omenesc).

Nimic din astea nu înseamnă că nu poți lansa — înseamnă doar că, pe măsură
ce crește comunitatea, merită revizitat periodic ce funcționează și ce nu.

## Idei de extins mai departe

- Notificare/sunet când ești provocat la duel, chiar dacă nu ești în cameră.
- Autentificare obligatorie cu cont (elimină intrarea rapidă cu poreclă),
  dacă vrei protecție mai serioasă împotriva abuzului.
- Panou de admin cu istoric de acțiuni (cine a blocat pe cine și de ce).
