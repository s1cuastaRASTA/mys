import { firebaseConfig } from './firebase-config.js';
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getDatabase, ref, push, set, update, remove, get, onValue,
  query, orderByChild, limitToLast, serverTimestamp
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js";
import {
  getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword,
  GoogleAuthProvider, signInWithPopup, signInAnonymously,
  sendPasswordResetEmail, onAuthStateChanged, signOut
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);
const auth = getAuth(app);

/* -------------------------------------------------------------
   CONFIG DE MODERARE
   Pentru rate-limiting: câte secunde minim între două mesaje.
------------------------------------------------------------- */
const SEND_COOLDOWN_MS = 3000;

/* Cheie opțională Perspective API (Google/Jigsaw) pentru detecție de
   toxicitate/ură mai bună decât o listă fixă de cuvinte. Lasă gol ca
   să folosești doar filtrul de cuvinte de mai jos. Detalii în README. */
const PERSPECTIVE_API_KEY = '';

/* -------------------------------------------------------------
   FILTRU DE MODERARE
   Nu blocăm cuvintele grele / injurăturile "clasice" — sunt punctul
   central al aplicației. Blocăm doar limbajul de ură reală (atacuri
   pe bază de rasă, etnie, orientare sexuală, dizabilitate).
   Adaugă propriile tale cuvinte/expresii (regex, case-insensitive)
   in lista de mai jos.
------------------------------------------------------------- */
const HATE_SPEECH_PATTERNS = [
  // exemplu: /cuvant-interzis/i,
];

// cuvinte blocate adăugate de admin din panoul de administrare (live,
// din baza de date) — se adaugă la lista fixă de mai sus, nu o înlocuiesc.
let dynamicBlockedWords = [];
onValue(ref(db, 'moderation/blockedWords'), (snap) => {
  const val = snap.val() || {};
  dynamicBlockedWords = Object.entries(val).map(([id, word]) => ({ id, word }));
});

// normalizează textul ca să prindă și variații gen "c-u-v-a-n-t", "cuv4nt"
function normalizeForFilter(text) {
  return text
    .toLowerCase()
    .replace(/0/g, 'o').replace(/1/g, 'i').replace(/3/g, 'e')
    .replace(/4/g, 'a').replace(/5/g, 's').replace(/7/g, 't')
    .replace(/[\s\-_.*]+/g, '');
}

function containsHateSpeechLocal(text) {
  const normalized = normalizeForFilter(text);
  const patternHit = HATE_SPEECH_PATTERNS.some((pattern) => pattern.test(text) || pattern.test(normalized));
  if (patternHit) return true;
  return dynamicBlockedWords.some(({ word }) => {
    const w = normalizeForFilter(word || '');
    return w.length > 0 && normalized.includes(w);
  });
}

// Verificare opțională cu Perspective API (Google/Jigsaw) — detectează
// toxicitate/ură mult mai bine decât o listă fixă de cuvinte, dar are
// nevoie de o cheie API gratuită. Fără cheie, se sare peste acest pas.
async function checkPerspectiveApi(text) {
  if (!PERSPECTIVE_API_KEY) return { flagged: false };
  try {
    const res = await fetch(
      `https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=${PERSPECTIVE_API_KEY}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          comment: { text },
          languages: ['ro', 'en'],
          requestedAttributes: { IDENTITY_ATTACK: {}, SEVERE_TOXICITY: {} },
        }),
      }
    );
    if (!res.ok) return { flagged: false };
    const data = await res.json();
    const identityAttack = data.attributeScores?.IDENTITY_ATTACK?.summaryScore?.value || 0;
    const severeToxicity = data.attributeScores?.SEVERE_TOXICITY?.summaryScore?.value || 0;
    return { flagged: identityAttack > 0.75 || severeToxicity > 0.9 };
  } catch (e) {
    // dacă API-ul e jos sau dă eroare, nu blocăm mesajul din cauza asta
    return { flagged: false };
  }
}

async function isMessageBlocked(text) {
  if (containsHateSpeechLocal(text)) return true;
  const perspective = await checkPerspectiveApi(text);
  return perspective.flagged;
}

/* ------------------------------------------------------------- */

let currentNick = localStorage.getItem('roastarena_nick') || '';
let currentRoom = 'general';
let roomsCache = { general: { name: '#general' }, dueluri: { name: '#dueluri' } };
let currentUid = null;
let isAdmin = false;
let lastSendAt = 0;

const gate = document.getElementById('gate');
const appEl = document.getElementById('app');
const nickInput = document.getElementById('nickInput');
const enterBtn = document.getElementById('enterBtn');
const nickTakenWarning = document.getElementById('nickTakenWarning');
const whoNick = document.getElementById('whoNick');
const changeNickBtn = document.getElementById('changeNick');
const signOutBtn = document.getElementById('signOutBtn');
const bannedNotice = document.getElementById('bannedNotice');
const ageConsent = document.getElementById('ageConsent');
const ageWarning = document.getElementById('ageWarning');
const rulesMore = document.getElementById('rulesMore');
const rulesModal = document.getElementById('rulesModal');
const rulesCloseBtn = document.getElementById('rulesCloseBtn');
const duelNoticeBtn = document.getElementById('duelNotice');

const quickEntryBox = document.getElementById('quickEntryBox');
const accountBox = document.getElementById('accountBox');
const claimNicknameBox = document.getElementById('claimNicknameBox');
const tabLogin = document.getElementById('tabLogin');
const tabSignup = document.getElementById('tabSignup');
const emailInput = document.getElementById('emailInput');
const passwordInput = document.getElementById('passwordInput');
const emailAuthBtn = document.getElementById('emailAuthBtn');
const googleBtn = document.getElementById('googleBtn');
const forgotPasswordLink = document.getElementById('forgotPasswordLink');
const authError = document.getElementById('authError');
const claimNickInput = document.getElementById('claimNickInput');
const claimNickBtn = document.getElementById('claimNickBtn');
const claimError = document.getElementById('claimError');

const roomList = document.getElementById('roomList');
const composerWrap = document.getElementById('composerWrap');
const newRoomName = document.getElementById('newRoomName');
const createRoomBtn = document.getElementById('createRoomBtn');
const roomTitle = document.getElementById('roomTitle');
const feed = document.getElementById('feed');
const msgInput = document.getElementById('msgInput');
const sendBtn = document.getElementById('sendBtn');
const filterWarning = document.getElementById('filterWarning');

const duelBtn = document.getElementById('duelBtn');
const duelModal = document.getElementById('duelModal');
const duelOpponent = document.getElementById('duelOpponent');
const duelOpening = document.getElementById('duelOpening');
const duelCancelBtn = document.getElementById('duelCancelBtn');
const duelSendBtn = document.getElementById('duelSendBtn');

const leaderboardBtn = document.getElementById('leaderboardBtn');
const backToRoomBtn = document.getElementById('backToRoomBtn');
const roomView = document.getElementById('roomView');
const leaderboardView = document.getElementById('leaderboardView');
const leaderboardContent = document.getElementById('leaderboardContent');

const adminBtn = document.getElementById('adminBtn');
const adminView = document.getElementById('adminView');
const adminContent = document.getElementById('adminContent');
const backToRoomFromAdminBtn = document.getElementById('backToRoomFromAdminBtn');
const adminTabReports = document.getElementById('adminTabReports');
const adminTabBanned = document.getElementById('adminTabBanned');
const adminTabWords = document.getElementById('adminTabWords');
const adminTabRooms = document.getElementById('adminTabRooms');

/* ---------------- CONFIRM MODAL (înlocuiește confirm() nativ) ---------------- */

const confirmModal = document.getElementById('confirmModal');
const confirmTitle = document.getElementById('confirmTitle');
const confirmMessage = document.getElementById('confirmMessage');
const confirmOkBtn = document.getElementById('confirmOkBtn');
const confirmCancelBtn = document.getElementById('confirmCancelBtn');

function showConfirm(message, title = 'confirmă') {
  return new Promise((resolve) => {
    confirmTitle.textContent = title;
    confirmMessage.textContent = message;
    confirmModal.classList.remove('hidden');

    function cleanup(result) {
      confirmModal.classList.add('hidden');
      confirmOkBtn.removeEventListener('click', onOk);
      confirmCancelBtn.removeEventListener('click', onCancel);
      resolve(result);
    }
    function onOk() { cleanup(true); }
    function onCancel() { cleanup(false); }

    confirmOkBtn.addEventListener('click', onOk);
    confirmCancelBtn.addEventListener('click', onCancel);
  });
}

// helper defensiv: dacă elementul nu există în HTML (versiuni desincronizate
// între index.html și app.js), nu mai crapă tot scriptul — doar ignoră
// acel buton specific și restul aplicației continuă să funcționeze.
function on(el, event, handler) {
  if (!el) {
    console.warn(`[roast-arena] element lipsă din HTML pentru evenimentul "${event}" — index.html și app.js par desincronizate.`);
    return;
  }
  el.addEventListener(event, handler);
}

/* ---------------- CONSIMȚĂMÂNT VÂRSTĂ + REGULI ---------------- */

if (localStorage.getItem('roastarena_age_consent_18plus') === 'true' && ageConsent) {
  ageConsent.checked = true;
}

on(ageConsent, 'change', () => {
  if (ageConsent.checked) {
    localStorage.setItem('roastarena_age_consent_18plus', 'true');
    ageWarning.classList.add('hidden');
  } else {
    localStorage.removeItem('roastarena_age_consent_18plus');
  }
});

on(rulesMore, 'click', (e) => {
  e.preventDefault();
  e.stopPropagation(); // nu bifa checkbox-ul doar fiindcă ai apăsat pe cuvântul "regulile"
  rulesModal.classList.remove('hidden');
});
on(rulesCloseBtn, 'click', () => rulesModal.classList.add('hidden'));

function requireAgeConsent() {
  if (ageConsent && !ageConsent.checked) {
    ageWarning.classList.remove('hidden');
    return false;
  }
  ageWarning.classList.add('hidden');
  return true;
}

function resetGateView() {
  quickEntryBox.classList.remove('hidden');
  accountBox.classList.remove('hidden');
  claimNicknameBox.classList.add('hidden');
  bannedNotice.classList.add('hidden');
  authError.classList.add('hidden');
  claimError.classList.add('hidden');
  nickTakenWarning.classList.add('hidden');
}

async function isBanned(uid) {
  if (!uid) return false;
  const snap = await get(ref(db, `banned/${uid}`));
  return snap.exists();
}

function showBannedNotice() {
  quickEntryBox.classList.add('hidden');
  accountBox.classList.add('hidden');
  claimNicknameBox.classList.add('hidden');
  bannedNotice.classList.remove('hidden');
}

async function enterArena(nick, uid, isAccount) {
  if (uid && await isBanned(uid)) {
    showBannedNotice();
    return;
  }
  currentNick = nick.trim().slice(0, 24);
  if (!currentNick) return;
  currentUid = uid || null;
  if (!isAccount) localStorage.setItem('roastarena_nick', currentNick);
  const adminSnap = currentUid ? await get(ref(db, `admins/${currentUid}`)) : null;
  isAdmin = !!(adminSnap && adminSnap.exists());
  adminBtn.classList.toggle('hidden', !isAdmin);
  whoNick.textContent = currentNick + (isAccount ? ' 🔒' : '');
  signOutBtn.classList.toggle('hidden', !isAccount);
  gate.classList.add('hidden');
  appEl.classList.remove('hidden');
  listenRooms();
  switchRoom('general');
  startDuelWatcher();
}

/* ---------------- NOTIFICARE DUEL ---------------- */

let hasPendingDuel = false;

function startDuelWatcher() {
  onValue(ref(db, 'duels/dueluri'), (snap) => {
    const duels = snap.val() || {};
    hasPendingDuel = Object.values(duels).some((d) => {
      if ((d.status || 'active') !== 'active') return false;
      if (d.challenger !== currentNick && d.opponent !== currentNick) return false;
      const turnCount = Object.keys(d.turns || {}).length;
      const nextSide = turnCount % 2 === 1 ? 'opponent' : 'challenger';
      const nextNick = nextSide === 'challenger' ? d.challenger : d.opponent;
      return nextNick === currentNick && turnCount > 0;
    });
    duelNoticeBtn.classList.toggle('hidden', !hasPendingDuel);
  });
}

on(duelNoticeBtn, 'click', () => {
  leaderboardView.classList.add('hidden');
  adminView.classList.add('hidden');
  roomView.classList.remove('hidden');
  switchRoom('dueluri');
});

async function tryQuickEntry(nick) {
  nick = nick.trim().slice(0, 24);
  if (!nick) return;
  if (!requireAgeConsent()) return;
  const claimSnap = await get(ref(db, `nicknames/${nick.toLowerCase()}`));
  if (claimSnap.exists()) {
    nickTakenWarning.classList.remove('hidden');
    return;
  }
  nickTakenWarning.classList.add('hidden');
  try {
    const credential = await signInAnonymously(auth);
    // intrăm direct cu uid-ul primit, nu așteptăm onAuthStateChanged —
    // dacă exista deja o sesiune anonimă (de ieri, de ex.), evenimentul
    // ăla nu se mai declanșează a doua oară.
    await enterArena(nick, credential.user.uid, false);
  } catch (e) {
    alert('Nu s-a putut porni sesiunea. Încearcă din nou.');
  }
}

if (currentNick) {
  nickInput.value = currentNick;
}
on(enterBtn, 'click', () => tryQuickEntry(nickInput.value));
on(nickInput, 'keydown', (e) => { if (e.key === 'Enter') tryQuickEntry(nickInput.value); });

on(changeNickBtn, 'click', async () => {
  if (!currentUid) {
    // sesiune anonimă — o închidem ca să poată alege altă poreclă curat
    try { await signOut(auth); } catch (e) {}
  }
  appEl.classList.add('hidden');
  gate.classList.remove('hidden');
  resetGateView();
});

on(signOutBtn, 'click', async () => {
  await signOut(auth);
  appEl.classList.add('hidden');
  gate.classList.remove('hidden');
  resetGateView();
});

/* ---------------- ACCOUNT: EMAIL/PASSWORD + GOOGLE ---------------- */

let authMode = 'login';
on(tabLogin, 'click', () => {
  authMode = 'login';
  tabLogin.classList.add('active');
  tabSignup.classList.remove('active');
  emailAuthBtn.textContent = 'autentificare';
});
on(tabSignup, 'click', () => {
  authMode = 'signup';
  tabSignup.classList.add('active');
  tabLogin.classList.remove('active');
  emailAuthBtn.textContent = 'creează cont';
});

function friendlyAuthError(code) {
  const map = {
    'auth/invalid-email': 'email invalid.',
    'auth/email-already-in-use': 'există deja un cont cu acest email — încearcă autentificare.',
    'auth/weak-password': 'parola trebuie să aibă cel puțin 6 caractere.',
    'auth/invalid-credential': 'email sau parolă greșite.',
    'auth/wrong-password': 'email sau parolă greșite.',
    'auth/user-not-found': 'nu există cont cu acest email — încearcă "cont nou".',
    'auth/popup-closed-by-user': 'fereastra Google a fost închisă înainte de autentificare.',
    'auth/missing-email': 'scrie mai întâi emailul, apoi apasă din nou.',
  };
  return map[code] || 'ceva n-a mers. încearcă din nou.';
}

on(emailAuthBtn, 'click', async () => {
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  authError.classList.add('hidden');
  if (!email || !password) return;
  if (!requireAgeConsent()) return;
  try {
    if (authMode === 'signup') {
      await createUserWithEmailAndPassword(auth, email, password);
    } else {
      await signInWithEmailAndPassword(auth, email, password);
    }
    // onAuthStateChanged preia de aici
  } catch (err) {
    authError.textContent = friendlyAuthError(err.code);
    authError.classList.remove('hidden');
  }
});

on(forgotPasswordLink, 'click', async (e) => {
  e.preventDefault();
  const email = emailInput.value.trim();
  authError.classList.add('hidden');
  if (!email) {
    authError.textContent = friendlyAuthError('auth/missing-email');
    authError.classList.remove('hidden');
    return;
  }
  try {
    await sendPasswordResetEmail(auth, email);
    authError.textContent = 'ți-am trimis un email de resetare a parolei.';
    authError.classList.remove('hidden');
  } catch (err) {
    authError.textContent = friendlyAuthError(err.code);
    authError.classList.remove('hidden');
  }
});

on(googleBtn, 'click', async () => {
  authError.classList.add('hidden');
  if (!requireAgeConsent()) return;
  try {
    await signInWithPopup(auth, new GoogleAuthProvider());
  } catch (err) {
    authError.textContent = friendlyAuthError(err.code);
    authError.classList.remove('hidden');
  }
});

onAuthStateChanged(auth, async (user) => {
  if (!user) return;

  if (user.isAnonymous) {
    // sesiune anonimă existentă (posibil de la o vizită anterioară) —
    // nu intrăm automat, așteptăm ca omul să apese "intră rapid" cu o
    // poreclă; tryQuickEntry() gestionează direct intrarea în arenă.
    return;
  }

  // utilizator cu cont (email sau Google)
  if (await isBanned(user.uid)) {
    showBannedNotice();
    return;
  }
  const userSnap = await get(ref(db, `users/${user.uid}`));
  const userData = userSnap.val();
  if (userData && userData.nickname) {
    enterArena(userData.nickname, user.uid, true);
  } else {
    quickEntryBox.classList.add('hidden');
    accountBox.classList.add('hidden');
    claimNicknameBox.classList.remove('hidden');
  }
});

on(claimNickBtn, 'click', async () => {
  const nick = claimNickInput.value.trim().slice(0, 24);
  claimError.classList.add('hidden');
  if (!nick || !auth.currentUser) return;
  const key = nick.toLowerCase();
  const existing = await get(ref(db, `nicknames/${key}`));
  if (existing.exists() && existing.val() !== auth.currentUser.uid) {
    claimError.textContent = 'poreclă deja luată de altcineva. alege alta.';
    claimError.classList.remove('hidden');
    return;
  }
  await set(ref(db, `nicknames/${key}`), auth.currentUser.uid);
  await set(ref(db, `users/${auth.currentUser.uid}`), { nickname: nick, email: auth.currentUser.email || null });
  enterArena(nick, auth.currentUser.uid, true);
});

/* ---------------- ROOMS ---------------- */

function listenRooms() {
  const roomsRef = ref(db, 'rooms');
  onValue(roomsRef, (snap) => {
    const data = snap.val() || {};
    roomsCache = { general: { name: '#general' }, dueluri: { name: '#dueluri' }, ...data };
    renderRoomList();
  });
}

function renderRoomList() {
  roomList.innerHTML = '';
  Object.entries(roomsCache).forEach(([id, room]) => {
    const div = document.createElement('div');
    div.className = 'room-item' + (id === currentRoom ? ' active' : '');
    div.textContent = room.name || ('#' + id);
    div.addEventListener('click', () => switchRoom(id));
    roomList.appendChild(div);
  });
}

on(createRoomBtn, 'click', async () => {
  let name = newRoomName.value.trim();
  if (!name) return;
  if (!name.startsWith('#')) name = '#' + name;
  const roomsRef = ref(db, 'rooms');
  const newRef = push(roomsRef);
  await set(newRef, { name, createdBy: currentNick, createdAt: Date.now() });
  newRoomName.value = '';
  switchRoom(newRef.key);
});

let unsubscribeFeed = [];

function switchRoom(roomId) {
  currentRoom = roomId;
  roomTitle.textContent = (roomsCache[roomId] && roomsCache[roomId].name) || ('#' + roomId);
  renderRoomList();

  // oprim listener-ele camerei anterioare, altfel rămân active la infinit
  unsubscribeFeed.forEach((unsub) => unsub && unsub());
  unsubscribeFeed = [];

  if (roomId === 'dueluri') {
    composerWrap.classList.add('hidden');
    unsubscribeFeed.push(listenDuelsRoom());
  } else {
    composerWrap.classList.remove('hidden');
    unsubscribeFeed = listenFeed(roomId);
  }
}

/* ---------------- MESSAGES + DUELS FEED ---------------- */

function listenFeed(roomId) {
  const msgsRef = query(ref(db, `messages/${roomId}`), orderByChild('ts'), limitToLast(150));

  function render(messages) {
    const items = Object.entries(messages)
      .map(([id, m]) => ({ id, ts: m.ts || 0, data: m }))
      .sort((a, b) => a.ts - b.ts);

    feed.innerHTML = '';
    if (items.length === 0) {
      feed.innerHTML = '<p style="color:var(--text-faint)">nimic aici încă. fii primul care aruncă o replică.</p>';
    }
    items.forEach((item) => {
      feed.appendChild(renderMessage(roomId, item.id, item.data));
    });
    feed.scrollTop = feed.scrollHeight;
  }

  const unsub = onValue(msgsRef, (snap) => render(snap.val() || {}));
  return [unsub];
}

function timeAgo(ts) {
  if (!ts) return '';
  const diff = Math.floor((Date.now() - ts) / 1000);
  if (diff < 60) return 'acum';
  if (diff < 3600) return Math.floor(diff / 60) + 'm';
  if (diff < 86400) return Math.floor(diff / 3600) + 'h';
  return Math.floor(diff / 86400) + 'z';
}

function renderMessage(roomId, id, m) {
  const el = document.createElement('div');
  el.className = 'msg';
  const votes = m.votes || 0;
  const voted = (m.voters || {})[currentUid];
  const isTopBadge = m.isDailyTop ? '<span class="msg-badge">🔥 roast-ul zilei</span>' : '';
  el.innerHTML = `
    <div class="msg-head">
      <span class="msg-nick">${escapeHtml(m.nick)}</span>
      <span class="msg-time">${timeAgo(m.ts)}</span>
      ${isTopBadge}
    </div>
    <div class="msg-text">${escapeHtml(m.text)}</div>
    <div class="msg-foot">
      <button class="vote-btn ${voted ? 'voted' : ''}" data-id="${id}">🔥 ${votes}</button>
      <button class="report-btn" data-id="${id}" title="raportează">🚩</button>
    </div>
  `;
  el.querySelector('.vote-btn').addEventListener('click', () => toggleVote(roomId, id, !!voted));
  el.querySelector('.report-btn').addEventListener('click', (e) => reportMessage(roomId, id, m, e.target));
  return el;
}

async function reportMessage(roomId, id, m, btnEl) {
  const ok = await showConfirm('Raportezi acest mesaj moderatorilor?', 'raportează mesajul');
  if (!ok) return;
  await push(ref(db, `reports/${roomId}`), {
    msgId: id,
    nick: m.nick,
    text: m.text,
    reportedUid: m.uid || null,
    reporterUid: currentUid,
    ts: Date.now(),
  });
  btnEl.textContent = '✓';
  btnEl.disabled = true;
}

async function toggleVote(roomId, id, alreadyVoted) {
  const votersRef = ref(db, `messages/${roomId}/${id}/voters/${currentUid}`);
  const msgRef = ref(db, `messages/${roomId}/${id}`);
  const snap = await get(msgRef);
  const m = snap.val();
  if (!m) return;
  const currentVotes = m.votes || 0;
  if (alreadyVoted) {
    await update(msgRef, { votes: Math.max(0, currentVotes - 1) });
    await set(votersRef, null);
  } else {
    await update(msgRef, { votes: currentVotes + 1 });
    await set(votersRef, true);
  }
}

/* ---------------- CAMERA #dueluri ---------------- */

let expandedDuels = new Set();
let duelsCache = {};

function listenDuelsRoom() {
  const duelsRef = query(ref(db, 'duels/dueluri'), orderByChild('ts'), limitToLast(100));
  const unsub = onValue(duelsRef, (snap) => {
    duelsCache = snap.val() || {};
    renderDuelsList();
  });
  return unsub;
}

function renderDuelsList() {
  const entries = Object.entries(duelsCache).sort((a, b) => (b[1].ts || 0) - (a[1].ts || 0));
  feed.innerHTML = '';
  if (entries.length === 0) {
    feed.innerHTML = '<p style="color:var(--text-faint)">niciun duel încă. apasă "⚔️ provoacă la duel" ca să începi unul.</p>';
    return;
  }
  entries.forEach(([id, d]) => feed.appendChild(renderDuelCard(id, d)));
}

function renderDuelCard(id, d) {
  const turns = Object.entries(d.turns || {})
    .map(([tid, t]) => ({ tid, ...t }))
    .sort((a, b) => (a.ts || 0) - (b.ts || 0));
  const turnCount = turns.length;
  const status = d.status || 'active';
  const isParticipant = currentNick === d.challenger || currentNick === d.opponent;
  const nextSide = turnCount % 2 === 1 ? 'opponent' : 'challenger';
  const nextNick = nextSide === 'challenger' ? d.challenger : d.opponent;
  const yourTurn = status === 'active' && isParticipant && nextNick === currentNick;
  const isExpanded = expandedDuels.has(id);
  const lastTurn = turns[turns.length - 1];
  const preview = lastTurn ? `${lastTurn.side === 'challenger' ? d.challenger : d.opponent}: ${lastTurn.text}` : 'niciun mesaj încă';

  const el = document.createElement('div');
  el.className = 'duel';

  const statusTag = status === 'closed'
    ? ' · <span style="color:var(--text-faint); font-weight:400;">încheiat</span>'
    : yourTurn
      ? ' · <span style="color:var(--flame-1); font-weight:700;">e rândul tău</span>'
      : '';

  const header = document.createElement('div');
  header.style.cursor = 'pointer';
  header.style.display = 'flex';
  header.style.justifyContent = 'space-between';
  header.style.alignItems = 'flex-start';
  header.style.gap = '10px';
  header.innerHTML = `
    <div style="min-width:0;">
      <div class="duel-title" style="margin-bottom:4px;">⚔️ ${escapeHtml(d.challenger)} vs ${escapeHtml(d.opponent)}${statusTag}</div>
      <p style="color:var(--text-faint); font-size:12px; margin:0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(preview)}</p>
    </div>
    <span style="color:var(--text-faint); flex-shrink:0;">${isExpanded ? '▲' : '▼'}</span>
  `;
  header.addEventListener('click', () => {
    if (isExpanded) expandedDuels.delete(id); else expandedDuels.add(id);
    renderDuelsList();
  });
  el.appendChild(header);

  if (!isExpanded) return el;

  const transcriptHtml = turns.length
    ? turns.map((t) => {
        const nick = t.side === 'challenger' ? d.challenger : d.opponent;
        return `<div class="duel-turn duel-turn-${t.side}"><span class="msg-nick">${escapeHtml(nick)}</span><div class="msg-text">${escapeHtml(t.text)}</div></div>`;
      }).join('')
    : '<p style="color:var(--text-faint); font-size:12px;">niciun mesaj încă...</p>';

  const body = document.createElement('div');
  body.style.marginTop = '10px';
  body.innerHTML = `<div class="duel-transcript">${transcriptHtml}</div>`;
  el.appendChild(body);

  if (status === 'active') {
    const actionsRow = document.createElement('div');
    actionsRow.style.marginTop = '10px';
    actionsRow.style.display = 'flex';
    actionsRow.style.gap = '8px';
    actionsRow.style.flexWrap = 'wrap';

    if (currentNick === nextNick) {
      const input = document.createElement('input');
      input.placeholder = turnCount === 0 ? 'replica ta de deschidere...' : 'răspunde...';
      input.style.flex = '1';
      input.style.minWidth = '160px';
      input.style.padding = '8px';
      input.style.borderRadius = '6px';
      input.style.border = '1px solid var(--line)';
      input.style.background = 'var(--bg-input)';
      input.style.color = 'var(--text)';
      const sendTurnBtn = document.createElement('button');
      sendTurnBtn.className = 'btn-flame small';
      sendTurnBtn.textContent = 'trimite';
      sendTurnBtn.addEventListener('click', async () => {
        const text = input.value.trim();
        if (!text) return;
        sendTurnBtn.disabled = true;
        if (await isMessageBlocked(text)) {
          alert('Replica ta conține limbaj interzis (ură reală, nu roast).');
          sendTurnBtn.disabled = false;
          return;
        }
        await push(ref(db, `duels/dueluri/${id}/turns`), { side: nextSide, uid: currentUid, text, ts: Date.now() });
        input.value = '';
      });
      actionsRow.appendChild(input);
      actionsRow.appendChild(sendTurnBtn);
    } else if (isParticipant) {
      const waiting = document.createElement('p');
      waiting.style.color = 'var(--text-faint)';
      waiting.style.fontSize = '12px';
      waiting.textContent = `aștepți răspunsul lui ${nextNick}...`;
      actionsRow.appendChild(waiting);
    }

    if (turnCount >= 2 && isParticipant) {
      const closeBtn = document.createElement('button');
      closeBtn.className = 'btn-ghost';
      closeBtn.style.borderColor = 'var(--danger)';
      closeBtn.style.color = 'var(--danger)';
      closeBtn.textContent = 'închide duelul, la vot';
      closeBtn.addEventListener('click', async () => {
        const ok = await showConfirm('Închizi duelul aici și deschizi votul comunității?', 'închide duelul');
        if (!ok) return;
        await update(ref(db, `duels/dueluri/${id}`), { status: 'closed' });
      });
      actionsRow.appendChild(closeBtn);
    }

    if (actionsRow.children.length) el.appendChild(actionsRow);
  } else {
    const myVote = (d.voters || {})[currentUid];
    const cVotes = d.challengerVotes || 0;
    const oVotes = d.opponentVotes || 0;
    const voteRow = document.createElement('div');
    voteRow.className = 'duel-sides';
    voteRow.style.marginTop = '10px';
    voteRow.innerHTML = `
      <div class="duel-side ${myVote === 'challenger' ? 'duel-side-voted' : ''}" data-side="challenger" style="cursor:pointer;">
        <span class="msg-nick">${escapeHtml(d.challenger)}</span>
        <div class="duel-vote-count">🔥 ${cVotes}</div>
      </div>
      <div class="duel-side ${myVote === 'opponent' ? 'duel-side-voted' : ''}" data-side="opponent" style="cursor:pointer;">
        <span class="msg-nick">${escapeHtml(d.opponent)}</span>
        <div class="duel-vote-count">🔥 ${oVotes}</div>
      </div>
    `;
    voteRow.querySelectorAll('.duel-side').forEach((sideEl) => {
      sideEl.addEventListener('click', () => voteDuel('dueluri', id, sideEl.dataset.side, myVote));
    });
    el.appendChild(voteRow);
  }

  return el;
}

async function voteDuel(roomId, id, side, myVote) {
  if (myVote === side) return;
  const duelRef = ref(db, `duels/${roomId}/${id}`);
  const snap = await get(duelRef);
  const d = snap.val();
  if (!d) return;
  const updates = {};
  if (myVote) {
    updates[myVote === 'challenger' ? 'challengerVotes' : 'opponentVotes'] = Math.max(0, (d[myVote === 'challenger' ? 'challengerVotes' : 'opponentVotes'] || 0) - 1);
  }
  updates[side === 'challenger' ? 'challengerVotes' : 'opponentVotes'] = (d[side === 'challenger' ? 'challengerVotes' : 'opponentVotes'] || 0) + 1;
  updates[`voters/${currentUid}`] = side;
  await update(duelRef, updates);
}

/* ---------------- SEND MESSAGE ---------------- */

on(sendBtn, 'click', sendMessage);
on(msgInput, 'keydown', (e) => { if (e.key === 'Enter') sendMessage(); });

async function sendMessage() {
  const text = msgInput.value.trim();
  if (!text) return;

  const now = Date.now();
  if (now - lastSendAt < SEND_COOLDOWN_MS) {
    filterWarning.textContent = `încet, campionule — mai așteaptă ${Math.ceil((SEND_COOLDOWN_MS - (now - lastSendAt)) / 1000)}s.`;
    filterWarning.classList.remove('hidden');
    return;
  }

  sendBtn.disabled = true;
  if (await isMessageBlocked(text)) {
    filterWarning.textContent = 'mesajul a fost blocat — conține limbaj interzis (atac pe bază de rasă/etnie/orientare/dizabilitate), nu genul de injurătură creativă cu care ne mândrim aici.';
    filterWarning.classList.remove('hidden');
    sendBtn.disabled = false;
    return;
  }
  filterWarning.classList.add('hidden');
  lastSendAt = now;
  const msgsRef = ref(db, `messages/${currentRoom}`);
  await push(msgsRef, { nick: currentNick, uid: currentUid, text, votes: 0, ts: Date.now() });
  msgInput.value = '';
  sendBtn.disabled = false;
}

/* ---------------- DUELS ---------------- */

on(duelBtn, 'click', () => duelModal.classList.remove('hidden'));
on(duelCancelBtn, 'click', () => duelModal.classList.add('hidden'));

on(duelSendBtn, 'click', async () => {
  const opponent = duelOpponent.value.trim();
  const opening = duelOpening.value.trim();
  if (!opponent || !opening) return;

  const now = Date.now();
  if (now - lastSendAt < SEND_COOLDOWN_MS) return;

  duelSendBtn.disabled = true;
  if (await isMessageBlocked(opening)) {
    alert('Replica ta conține limbaj interzis (ură reală, nu roast).');
    duelSendBtn.disabled = false;
    return;
  }
  lastSendAt = now;
  const duelsRef = ref(db, 'duels/dueluri');
  const newDuelRef = push(duelsRef);
  await set(newDuelRef, {
    challenger: currentNick,
    challengerUid: currentUid,
    opponent,
    status: 'active',
    challengerVotes: 0,
    opponentVotes: 0,
    ts: Date.now(),
  });
  await push(ref(db, `duels/dueluri/${newDuelRef.key}/turns`), {
    side: 'challenger',
    uid: currentUid,
    text: opening,
    ts: Date.now(),
  });
  duelOpponent.value = '';
  duelOpening.value = '';
  duelModal.classList.add('hidden');
  duelSendBtn.disabled = false;
  expandedDuels.add(newDuelRef.key);
  switchRoom('dueluri');
});

/* ---------------- LEADERBOARD ---------------- */

on(leaderboardBtn, 'click', async () => {
  roomView.classList.add('hidden');
  leaderboardView.classList.remove('hidden');
  await renderLeaderboard();
});
on(backToRoomBtn, 'click', () => {
  leaderboardView.classList.add('hidden');
  roomView.classList.remove('hidden');
});

async function renderLeaderboard() {
  leaderboardContent.innerHTML = '<p style="color:var(--text-faint)">se calculează...</p>';
  const allMessagesSnap = await get(ref(db, 'messages'));
  const allMessages = allMessagesSnap.val() || {};
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;

  const scores = {};
  Object.values(allMessages).forEach((roomMsgs) => {
    Object.values(roomMsgs).forEach((m) => {
      if ((m.ts || 0) < weekAgo) return;
      scores[m.nick] = (scores[m.nick] || 0) + (m.votes || 0);
    });
  });

  const ranked = Object.entries(scores).sort((a, b) => b[1] - a[1]).slice(0, 20);

  if (ranked.length === 0) {
    leaderboardContent.innerHTML = '<p style="color:var(--text-faint)">niciun roast votat săptămâna asta. fii primul.</p>';
    return;
  }

  leaderboardContent.innerHTML = '<p style="color:var(--text-faint); font-size:12px; margin-bottom:12px;">scor din ultimele 7 zile</p>';
  ranked.forEach(([nick, score], i) => {
    const tier = score >= 50 ? 'gold' : score >= 20 ? 'silver' : score >= 5 ? 'bronze' : null;
    const tierLabel = tier === 'gold' ? '🥇 legendă' : tier === 'silver' ? '🥈 veteran' : tier === 'bronze' ? '🥉 promițător' : '';
    const card = document.createElement('div');
    card.className = 'lb-card';
    card.innerHTML = `
      <span class="lb-rank">#${i + 1}</span>
      <span class="lb-nick">${escapeHtml(nick)}</span>
      <span class="lb-score">${score} 🔥 săpt.</span>
      ${tier ? `<span class="lb-tier ${tier}">${tierLabel}</span>` : ''}
    `;
    leaderboardContent.appendChild(card);
  });
}

/* ---------------- ADMIN ---------------- */

let adminTab = 'reports';

on(adminBtn, 'click', async () => {
  roomView.classList.add('hidden');
  leaderboardView.classList.add('hidden');
  adminView.classList.remove('hidden');
  await renderAdmin();
});
on(backToRoomFromAdminBtn, 'click', () => {
  adminView.classList.add('hidden');
  roomView.classList.remove('hidden');
});

function setAdminTab(tab) {
  adminTab = tab;
  [adminTabReports, adminTabBanned, adminTabWords, adminTabRooms].forEach((btn) => btn && btn.classList.remove('active'));
  const map = { reports: adminTabReports, banned: adminTabBanned, words: adminTabWords, rooms: adminTabRooms };
  if (map[tab]) map[tab].classList.add('active');
  renderAdmin();
}
on(adminTabReports, 'click', () => setAdminTab('reports'));
on(adminTabBanned, 'click', () => setAdminTab('banned'));
on(adminTabWords, 'click', () => setAdminTab('words'));
on(adminTabRooms, 'click', () => setAdminTab('rooms'));

async function renderAdmin() {
  if (adminTab === 'reports') return renderAdminReports();
  if (adminTab === 'banned') return renderAdminBanned();
  if (adminTab === 'words') return renderAdminWords();
  if (adminTab === 'rooms') return renderAdminRooms();
}

/* --- tab: rapoarte --- */
async function renderAdminReports() {
  adminContent.innerHTML = '<p style="color:var(--text-faint)">se încarcă rapoartele...</p>';
  const reportsSnap = await get(ref(db, 'reports'));
  const allReports = reportsSnap.val() || {};

  const flatReports = [];
  Object.entries(allReports).forEach(([roomId, roomReports]) => {
    Object.entries(roomReports).forEach(([reportId, r]) => {
      flatReports.push({ roomId, reportId, ...r });
    });
  });
  flatReports.sort((a, b) => (b.ts || 0) - (a.ts || 0));

  if (flatReports.length === 0) {
    adminContent.innerHTML = '<p style="color:var(--text-faint)">niciun raport activ. liniște.</p>';
    return;
  }

  adminContent.innerHTML = '';
  flatReports.forEach((r) => {
    const card = document.createElement('div');
    card.className = 'lb-card';
    card.style.flexDirection = 'column';
    card.style.alignItems = 'flex-start';
    card.style.gap = '8px';
    card.innerHTML = `
      <div><span class="lb-nick">${escapeHtml(r.nick)}</span> <span class="msg-time">în #${escapeHtml(r.roomId)}</span></div>
      <div class="msg-text">${escapeHtml(r.text)}</div>
      <div style="display:flex; gap:8px;">
        <button class="btn-ghost dismiss-btn">ignoră</button>
        <button class="btn-ghost delete-btn">șterge mesajul</button>
        <button class="btn-ghost ban-btn" style="border-color:var(--danger); color:var(--danger);" ${r.reportedUid ? '' : 'disabled title="nu am uid-ul autorului"'}>blochează autorul</button>
      </div>
    `;
    card.querySelector('.dismiss-btn').addEventListener('click', async () => {
      await remove(ref(db, `reports/${r.roomId}/${r.reportId}`));
      renderAdmin();
    });
    card.querySelector('.delete-btn').addEventListener('click', async () => {
      await remove(ref(db, `messages/${r.roomId}/${r.msgId}`));
      await remove(ref(db, `reports/${r.roomId}/${r.reportId}`));
      renderAdmin();
    });
    card.querySelector('.ban-btn').addEventListener('click', async () => {
      if (!r.reportedUid) return;
      const ok = await showConfirm(`Blochezi definitiv utilizatorul "${r.nick}"?`, 'blochează utilizator');
      if (!ok) return;
      await set(ref(db, `banned/${r.reportedUid}`), { reason: 'raport admin', nick: r.nick, ts: Date.now() });
      await remove(ref(db, `messages/${r.roomId}/${r.msgId}`));
      await remove(ref(db, `reports/${r.roomId}/${r.reportId}`));
      renderAdmin();
    });
    adminContent.appendChild(card);
  });
}

/* --- tab: utilizatori blocați --- */
async function renderAdminBanned() {
  adminContent.innerHTML = '<p style="color:var(--text-faint)">se încarcă...</p>';
  const bannedSnap = await get(ref(db, 'banned'));
  const banned = bannedSnap.val() || {};
  const entries = Object.entries(banned);

  if (entries.length === 0) {
    adminContent.innerHTML = '<p style="color:var(--text-faint)">niciun utilizator blocat momentan.</p>';
    return;
  }

  adminContent.innerHTML = '';
  entries.forEach(([uid, info]) => {
    const card = document.createElement('div');
    card.className = 'lb-card';
    card.innerHTML = `
      <span class="lb-nick">${escapeHtml(info.nick || '(poreclă necunoscută)')}</span>
      <span class="lb-score" style="flex:1;">${info.reason || ''} · ${timeAgo(info.ts)}</span>
      <button class="btn-ghost unban-btn">deblochează</button>
    `;
    card.querySelector('.unban-btn').addEventListener('click', async () => {
      await remove(ref(db, `banned/${uid}`));
      renderAdmin();
    });
    adminContent.appendChild(card);
  });
}

/* --- tab: cuvinte blocate --- */
async function renderAdminWords() {
  adminContent.innerHTML = `
    <p class="modal-sub" style="margin-bottom:14px;">
      cuvinte/expresii care blochează automat un mesaj dacă apar în el
      (nu sensibile la majuscule, spații sau cifre gen "4" în loc de "a").
      folosește doar pentru limbaj de ură reală — nu pentru injurături obișnuite.
    </p>
    <div style="display:flex; gap:8px; margin-bottom:16px; max-width:420px;">
      <input id="newBlockedWord" type="text" placeholder="cuvânt sau expresie" style="flex:1; padding:10px 12px; border-radius:6px; border:1px solid var(--line); background:var(--bg-input); color:var(--text);">
      <button id="addBlockedWordBtn" class="btn-flame small">adaugă</button>
    </div>
    <div id="blockedWordsList"></div>
  `;

  const list = document.getElementById('blockedWordsList');
  if (dynamicBlockedWords.length === 0) {
    list.innerHTML = '<p style="color:var(--text-faint)">nicio expresie adăugată încă.</p>';
  } else {
    dynamicBlockedWords.forEach(({ id, word }) => {
      const card = document.createElement('div');
      card.className = 'lb-card';
      card.innerHTML = `
        <span class="lb-nick">${escapeHtml(word)}</span>
        <button class="btn-ghost remove-word-btn" style="border-color:var(--danger); color:var(--danger);">șterge</button>
      `;
      card.querySelector('.remove-word-btn').addEventListener('click', async () => {
        await remove(ref(db, `moderation/blockedWords/${id}`));
        renderAdmin();
      });
      list.appendChild(card);
    });
  }

  document.getElementById('addBlockedWordBtn').addEventListener('click', async () => {
    const input = document.getElementById('newBlockedWord');
    const word = input.value.trim();
    if (!word) return;
    await push(ref(db, 'moderation/blockedWords'), word);
    input.value = '';
    renderAdmin();
  });
}

/* --- tab: camere --- */
async function renderAdminRooms() {
  adminContent.innerHTML = '<p style="color:var(--text-faint)">se încarcă...</p>';
  const roomsSnap = await get(ref(db, 'rooms'));
  const rooms = { general: { name: '#general' }, dueluri: { name: '#dueluri' }, ...(roomsSnap.val() || {}) };

  adminContent.innerHTML = '';
  Object.entries(rooms).forEach(([id, room]) => {
    const card = document.createElement('div');
    card.className = 'lb-card';
    const isFixed = id === 'general' || id === 'dueluri';
    card.innerHTML = `
      <span class="lb-nick">${escapeHtml(room.name || ('#' + id))}</span>
      <span class="lb-score" style="flex:1;">${isFixed ? 'cameră fixă, nu poate fi ștearsă' : ''}</span>
      ${isFixed ? '' : '<button class="btn-ghost delete-room-btn" style="border-color:var(--danger); color:var(--danger);">șterge camera</button>'}
    `;
    if (!isFixed) {
      card.querySelector('.delete-room-btn').addEventListener('click', async () => {
        const ok = await showConfirm(`Ștergi definitiv camera "${room.name}" și tot ce conține?`, 'șterge camera');
        if (!ok) return;
        await remove(ref(db, `rooms/${id}`));
        await remove(ref(db, `messages/${id}`));
        if (currentRoom === id) switchRoom('general');
        renderAdmin();
      });
    }
    adminContent.appendChild(card);
  });
}

/* ---------------- UTIL ---------------- */

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
