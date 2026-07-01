import { firebaseConfig } from './firebase-config.js';
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getDatabase, ref, push, set, update, remove, get, onValue,
  query, orderByChild, serverTimestamp, onDisconnect
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js";
import {
  getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword,
  GoogleAuthProvider, signInWithPopup, signInWithRedirect, getRedirectResult,
  setPersistence, browserLocalPersistence, signInAnonymously,
  sendPasswordResetEmail, onAuthStateChanged, signOut
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);
const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();

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
let HATE_SPEECH_PATTERNS = [];

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
  return HATE_SPEECH_PATTERNS.some((pattern) => pattern.test(text) || pattern.test(normalized));
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
let roomsCache = { general: { name: '#general' } };
let currentUid = null;
let isAdmin = false;
let pendingQuickNick = null;
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

const quickEntryBox = document.getElementById('quickEntryBox');
const accountBox = document.getElementById('accountBox');
const claimNicknameBox = document.getElementById('claimNicknameBox');
const localWarning = document.getElementById('localWarning');
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
const isLocalFileOpen = window.location.protocol === 'file:';

const roomList = document.getElementById('roomList');
const presenceList = document.getElementById('presenceList');
const newRoomName = document.getElementById('newRoomName');
const createRoomBtn = document.getElementById('createRoomBtn');
const roomTitle = document.getElementById('roomTitle');
const roomNotification = document.getElementById('roomNotification');
const feed = document.getElementById('feed');
const msgInput = document.getElementById('msgInput');
const sendBtn = document.getElementById('sendBtn');
const filterWarning = document.getElementById('filterWarning');

const duelBtn = document.getElementById('duelBtn');
const shareRoomBtn = document.getElementById('shareRoomBtn');
const profileBtn = document.getElementById('profileBtn');
const profileModal = document.getElementById('profileModal');
const profileContent = document.getElementById('profileContent');
const closeProfileModal = document.getElementById('closeProfileModal');
const duelModal = document.getElementById('duelModal');
const duelOpponent = document.getElementById('duelOpponent');
const duelOpening = document.getElementById('duelOpening');
const duelCancelBtn = document.getElementById('duelCancelBtn');
const duelSendBtn = document.getElementById('duelSendBtn');

const newFilterInput = document.getElementById('newFilterInput');
const addFilterBtn = document.getElementById('addFilterBtn');
const filterList = document.getElementById('filterList');
const filterAdminMsg = document.getElementById('filterAdminMsg');

const leaderboardBtn = document.getElementById('leaderboardBtn');
const backToRoomBtn = document.getElementById('backToRoomBtn');
const roomView = document.getElementById('roomView');
const leaderboardView = document.getElementById('leaderboardView');
const leaderboardContent = document.getElementById('leaderboardContent');

const adminBtn = document.getElementById('adminBtn');
const adminView = document.getElementById('adminView');
const adminContent = document.getElementById('adminContent');
const backToRoomFromAdminBtn = document.getElementById('backToRoomFromAdminBtn');

function resetGateView() {
  quickEntryBox.classList.remove('hidden');
  accountBox.classList.remove('hidden');
  claimNicknameBox.classList.add('hidden');
  bannedNotice.classList.add('hidden');
  authError.classList.add('hidden');
  claimError.classList.add('hidden');
  nickTakenWarning.classList.add('hidden');
  if (localWarning) localWarning.classList.add('hidden');
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

let currentProfile = { nickname: '', email: 'anonim', score: 0, badges: [] };
let duelNotificationUnsubscribe = null;
let notifiedDuelIds = new Set();

async function loadCurrentUserProfile() {
  currentProfile = { nickname: currentNick, email: 'anonim', score: 0, badges: [] };
  if (!currentUid) return;
  const userSnap = await get(ref(db, `users/${currentUid}`));
  const userData = userSnap.val() || {};
  currentProfile = {
    nickname: currentNick,
    email: userData.email || 'anonim',
    score: userData.score || 0,
    badges: userData.badges || [],
  };
}

function renderUserProfile() {
  profileContent.innerHTML = `
    <h3 style="margin-top:0;">profil ${escapeHtml(currentProfile.nickname)}</h3>
    <p style="color:var(--text-faint); margin:0 0 12px;">email: ${escapeHtml(currentProfile.email)}</p>
    <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:12px;">${currentProfile.badges.map((b) => `<span class="lb-tier ${b}">${escapeHtml(b)}</span>`).join('')}</div>
    <p style="margin:0 0 10px;">scor total: <strong>${currentProfile.score}</strong></p>
    <button id="saveProfileBadgeBtn" class="btn-ghost">adauga badge demo</button>
  `;
  const saveBadgeBtn = document.getElementById('saveProfileBadgeBtn');
  saveBadgeBtn.addEventListener('click', async () => {
    const newBadge = 'bronze';
    currentProfile.badges = Array.from(new Set([...currentProfile.badges, newBadge]));
    if (currentUid) {
      await update(ref(db, `users/${currentUid}`), { badges: currentProfile.badges });
    }
    renderUserProfile();
  });
}

let presenceListenerUnsubscribe = null;
let currentPresenceRef = null;

async function leavePresence() {
  if (!currentPresenceRef) return;
  try {
    await remove(currentPresenceRef);
  } catch (e) {
    // ignorăm erorile de cleanup
  }
  currentPresenceRef = null;
}

async function listenPresence() {
  if (!currentUid) return;
  await leavePresence();
  const presenceRef = ref(db, `presence/${currentRoom}/${currentUid}`);
  currentPresenceRef = presenceRef;
  await set(presenceRef, { nick: currentNick, online: true, ts: Date.now() });
  onDisconnect(presenceRef).remove();
}

function renderPresence(roomId) {
  if (presenceListenerUnsubscribe) {
    presenceListenerUnsubscribe();
    presenceListenerUnsubscribe = null;
  }
  const presenceRef = ref(db, `presence/${roomId}`);
  presenceListenerUnsubscribe = onValue(presenceRef, (snap) => {
    const users = snap.val() || {};
    presenceList.innerHTML = '';
    Object.entries(users).forEach(([uid, user]) => {
      const item = document.createElement('div');
      item.className = 'room-item';
      item.innerHTML = `<span>${escapeHtml(user.nick || 'anonim')}</span><span class="${user.online ? 'presence-online' : 'presence-offline'}"></span>`;
      presenceList.appendChild(item);
    });
  });
}

function showRoomNotification(message) {
  roomNotification.textContent = message;
  roomNotification.classList.remove('hidden');
  setTimeout(() => roomNotification.classList.add('hidden'), 5000);
}

function openProfileModal() {
  profileModal.classList.remove('hidden');
  renderUserProfile();
}

function closeProfileModalHandler() {
  profileModal.classList.add('hidden');
}

function watchDuelNotifications(roomId) {
  if (duelNotificationUnsubscribe) {
    duelNotificationUnsubscribe();
    duelNotificationUnsubscribe = null;
  }
  const duelsRef = ref(db, `duels/${roomId}`);
  duelNotificationUnsubscribe = onValue(duelsRef, (snap) => {
    const duels = snap.val() || {};
    Object.entries(duels).forEach(([id, duel]) => {
      if (duel.opponent === currentNick && !duel.opponentText && !notifiedDuelIds.has(id)) {
        showRoomNotification(`Ai fost provocat la duel de ${escapeHtml(duel.challenger)}!`);
        notifiedDuelIds.add(id);
      }
    });
  });
}

async function updateAdminFilterUI() {
  const patternsSnap = await get(ref(db, 'adminFilters'));
  const patterns = patternsSnap.val() || [];
  HATE_SPEECH_PATTERNS = patterns.map((pattern) => {
    try { return new RegExp(pattern, 'i'); } catch (e) { return null; }
  }).filter(Boolean);
  filterList.innerHTML = '';
  patterns.forEach((pattern) => {
    const div = document.createElement('div');
    div.className = 'filter-tag';
    div.innerHTML = `${escapeHtml(pattern)} <button type="button">✕</button>`;
    div.querySelector('button').addEventListener('click', async () => {
      const remaining = patterns.filter((p) => p !== pattern);
      await set(ref(db, 'adminFilters'), remaining);
      filterAdminMsg.textContent = `filtrul "${pattern}" a fost șters.`;
      updateAdminFilterUI();
    });
    filterList.appendChild(div);
  });
  filterAdminMsg.textContent = `filtre active: ${patterns.length}`;
}

addFilterBtn.addEventListener('click', async () => {
  const pattern = newFilterInput.value.trim();
  if (!pattern) return;
  const patternsSnap = await get(ref(db, 'adminFilters'));
  const patterns = patternsSnap.val() || [];
  if (patterns.includes(pattern)) {
    filterAdminMsg.textContent = 'Acest filtru există deja.';
    return;
  }
  await set(ref(db, 'adminFilters'), [...patterns, pattern]);
  newFilterInput.value = '';
  updateAdminFilterUI();
  filterAdminMsg.textContent = `filtrul "${pattern}" a fost adăugat.`;
});

async function refreshPresenceAndProfile(roomId) {
  await listenPresence();
  await renderPresence(roomId);
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
  await loadCurrentUserProfile();
  const hashedRoom = getRoomFromHash();
  switchRoom(hashedRoom || 'general');
}

async function tryQuickEntry(nick) {
  if (isLocalFileOpen) {
    authError.textContent = 'Nu poți folosi autentificarea locală din fișierele file://. Rulează proiectul pe un server local sau public.';
    authError.classList.remove('hidden');
    return;
  }
  nick = nick.trim().slice(0, 24);
  if (!nick) return;
  const claimSnap = await get(ref(db, `nicknames/${nick.toLowerCase()}`));
  if (claimSnap.exists()) {
    nickTakenWarning.classList.remove('hidden');
    return;
  }
  nickTakenWarning.classList.add('hidden');
  pendingQuickNick = nick;
  try {
    await signInAnonymously(auth);
    // onAuthStateChanged preia de aici și intră în arenă
  } catch (e) {
    pendingQuickNick = null;
    authError.textContent = friendlyAuthError(e.code || e.message || 'unknown');
    authError.classList.remove('hidden');
  }
}

const isLocalFileOpen = window.location.protocol === 'file:';
if (currentNick) {
  nickInput.value = currentNick;
}
if (isLocalFileOpen) {
  if (localWarning) localWarning.classList.remove('hidden');
  quickEntryBox.classList.add('hidden');
  accountBox.classList.add('hidden');
  claimNicknameBox.classList.add('hidden');
  enterBtn.disabled = true;
  emailAuthBtn.disabled = true;
  googleBtn.disabled = true;
}
enterBtn.addEventListener('click', () => tryQuickEntry(nickInput.value));
nickInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') tryQuickEntry(nickInput.value); });

changeNickBtn.addEventListener('click', async () => {
  if (!currentUid) {
    // sesiune anonimă — o închidem ca să poată alege altă poreclă curat
    try { await signOut(auth); } catch (e) {}
  }
  localStorage.removeItem('roastarena_nick');
  appEl.classList.add('hidden');
  gate.classList.remove('hidden');
  resetGateView();
});

signOutBtn.addEventListener('click', async () => {
  await signOut(auth);
  localStorage.removeItem('roastarena_nick');
  appEl.classList.add('hidden');
  gate.classList.remove('hidden');
  resetGateView();
});

/* ---------------- ACCOUNT: EMAIL/PASSWORD + GOOGLE ---------------- */

let authMode = 'login';
tabLogin.addEventListener('click', () => {
  authMode = 'login';
  tabLogin.classList.add('active');
  tabSignup.classList.remove('active');
  emailAuthBtn.textContent = 'autentificare';
});
tabSignup.addEventListener('click', () => {
  authMode = 'signup';
  tabSignup.classList.add('active');
  tabLogin.classList.remove('active');
  emailAuthBtn.textContent = 'creează cont';
});

function friendlyAuthError(codeOrMessage) {
  const map = {
    'auth/invalid-email': 'email invalid.',
    'auth/email-already-in-use': 'există deja un cont cu acest email — încearcă autentificare.',
    'auth/weak-password': 'parola trebuie să aibă cel puțin 6 caractere.',
    'auth/invalid-credential': 'email sau parolă greșite.',
    'auth/wrong-password': 'email sau parolă greșite.',
    'auth/user-not-found': 'nu există cont cu acest email — încearcă "cont nou".',
    'auth/popup-closed-by-user': 'fereastra Google a fost închisă înainte de autentificare.',
    'auth/popup-blocked': 'popup-ul Google a fost blocat de browser. încearcă din nou sau folosește redirect.',
    'auth/operation-not-supported-in-this-environment': 'browserul tău nu suportă autentificarea prin popup. încearcă din nou sau folosește redirect.',
    'auth/redirect-cancelled-by-user': 'autentificarea Google a fost anulată. încearcă din nou.',
    'auth/cancelled-popup-request': 'cererea Google a fost anulată. încearcă din nou.',
    'auth/operation-not-allowed': 'autentificarea nu este activată în Firebase. activează Email/Password și/sau Google în consola Firebase.',
    'auth/unauthorized-domain': `domeniul ${window.location.host} nu este autorizat în Firebase Auth. adaugă domeniul în consola Firebase.`,
    'auth/network-request-failed': 'eroare de rețea. verifică conexiunea la internet.',
    'auth/web-storage-unsupported': 'browserul tău nu suportă stocare necesară Firebase Auth.',
    'auth/missing-email': 'scrie mai întâi emailul, apoi apasă din nou.',
  };

  let code = codeOrMessage;
  if (typeof codeOrMessage === 'string') {
    const match = codeOrMessage.match(/auth\/[a-zA-Z-]+/);
    if (match) code = match[0];
  }

  return map[code] || `ceva n-a mers. cod eroare: ${code || 'unknown'}`;
}

emailAuthBtn.addEventListener('click', async () => {
  if (isLocalFileOpen) {
    authError.textContent = 'Nu poți folosi autentificarea locală din fișierele file://. Rulează proiectul pe un server local sau public.';
    authError.classList.remove('hidden');
    return;
  }
  const email = emailInput.value.trim();
  const password = passwordInput.value;
  authError.classList.add('hidden');
  if (!email || !password) return;
  try {
    await setPersistence(auth, browserLocalPersistence);
    if (authMode === 'signup') {
      await createUserWithEmailAndPassword(auth, email, password);
    } else {
      await signInWithEmailAndPassword(auth, email, password);
    }
    // onAuthStateChanged preia de aici
  } catch (err) {
    authError.textContent = friendlyAuthError(err.code || err.message);
    authError.classList.remove('hidden');
  }
});

forgotPasswordLink.addEventListener('click', async (e) => {
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

googleBtn.addEventListener('click', async () => {
  if (isLocalFileOpen) {
    authError.textContent = 'Nu poți folosi autentificarea locală din fișierele file://. Rulează proiectul pe un server local sau public.';
    authError.classList.remove('hidden');
    return;
  }
  authError.classList.add('hidden');
  try {
    await setPersistence(auth, browserLocalPersistence);
    await signInWithPopup(auth, googleProvider);
  } catch (err) {
    console.warn('Google auth failed, falling back if possible:', err);
    const fallbackCodes = new Set([
      'auth/popup-blocked',
      'auth/popup-closed-by-user',
      'auth/cancelled-popup-request',
      'auth/operation-not-supported-in-this-environment',
    ]);

    if (fallbackCodes.has(err.code)) {
      try {
        await signInWithRedirect(auth, googleProvider);
      } catch (redirectErr) {
        console.error('Google redirect auth failed:', redirectErr);
        authError.textContent = friendlyAuthError(redirectErr.code || redirectErr.message);
        authError.classList.remove('hidden');
      }
    } else {
      authError.textContent = friendlyAuthError(err.code || err.message);
      authError.classList.remove('hidden');
    }
  }
});

getRedirectResult(auth).then((result) => {
  if (result && result.user) {
    // utilizator logat prin redirect; onAuthStateChanged îl va prelua
  }
}).catch((err) => {
  authError.textContent = friendlyAuthError(err.code || err.message);
  authError.classList.remove('hidden');
});

onAuthStateChanged(auth, async (user) => {
  if (!user) return;

  if (user.isAnonymous) {
    const nick = pendingQuickNick || currentNick;
    if (nick) {
      pendingQuickNick = null;
      await enterArena(nick, user.uid, false);
    }
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

claimNickBtn.addEventListener('click', async () => {
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
    roomsCache = { general: { name: '#general' }, ...data };
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

createRoomBtn.addEventListener('click', async () => {
  let name = newRoomName.value.trim();
  if (!name) return;
  if (!name.startsWith('#')) name = '#' + name;
  const roomsRef = ref(db, 'rooms');
  const newRef = push(roomsRef);
  await set(newRef, { name, createdBy: currentNick, createdAt: Date.now() });
  newRoomName.value = '';
  switchRoom(newRef.key);
});

let unsubscribeFeed = null;

function switchRoom(roomId) {
  currentRoom = roomId;
  roomTitle.textContent = (roomsCache[roomId] && roomsCache[roomId].name) || ('#' + roomId);
  renderRoomList();
  updateRoomHash(roomId);
  refreshPresenceAndProfile(roomId);
  watchDuelNotifications(roomId);
  listenFeed(roomId);
}

function getRoomFromHash() {
  const hash = window.location.hash.slice(1);
  if (!hash) return null;
  if (hash.startsWith('room=')) {
    return decodeURIComponent(hash.slice(5));
  }
  return null;
}

function updateRoomHash(roomId) {
  if (!roomId) return;
  const newHash = `room=${encodeURIComponent(roomId)}`;
  const current = window.location.hash.slice(1);
  if (current !== newHash) {
    window.history.replaceState(null, '', `#${newHash}`);
  }
}

/* ---------------- MESSAGES + DUELS FEED ---------------- */

function listenFeed(roomId) {
  if (unsubscribeFeed) {
    unsubscribeFeed();
    unsubscribeFeed = null;
  }

  const msgsRef = query(ref(db, `messages/${roomId}`), orderByChild('ts'));
  const duelsRef = query(ref(db, `duels/${roomId}`), orderByChild('ts'));

  let messages = {};
  let duels = {};

  function render() {
    const items = [
      ...Object.entries(messages).map(([id, m]) => ({ type: 'msg', id, ts: m.ts || 0, data: m })),
      ...Object.entries(duels).map(([id, d]) => ({ type: 'duel', id, ts: d.ts || 0, data: d })),
    ].sort((a, b) => a.ts - b.ts);

    feed.innerHTML = '';
    if (items.length === 0) {
      feed.innerHTML = '<p style="color:var(--text-faint)">nimic aici încă. fii primul care aruncă o replică.</p>';
    }
    items.forEach((item) => {
      feed.appendChild(item.type === 'msg' ? renderMessage(roomId, item.id, item.data) : renderDuel(roomId, item.id, item.data));
    });
    feed.scrollTop = feed.scrollHeight;
  }

  const unsubscribeMsgs = onValue(msgsRef, (snap) => { messages = snap.val() || {}; render(); });
  const unsubscribeDuels = onValue(duelsRef, (snap) => { duels = snap.val() || {}; render(); });
  unsubscribeFeed = () => {
    unsubscribeMsgs();
    unsubscribeDuels();
  };
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
  if (!confirm('Raportezi acest mesaj moderatorilor?')) return;
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

function renderDuel(roomId, id, d) {
  const el = document.createElement('div');
  el.className = 'duel';
  const cVotes = d.challengerVotes || 0;
  const oVotes = d.opponentVotes || 0;
  const myVote = (d.voters || {})[currentUid];
  const hasOpponentReply = !!d.opponentText;

  el.innerHTML = `
    <div class="duel-title">⚔️ duel: ${escapeHtml(d.challenger)} vs ${escapeHtml(d.opponent)}</div>
    <div class="duel-sides">
      <div class="duel-side">
        <span class="msg-nick">${escapeHtml(d.challenger)}</span>
        <div class="msg-text">${escapeHtml(d.challengerText || '')}</div>
        <div class="duel-vote-count">🔥 ${cVotes}</div>
      </div>
      <div class="duel-side">
        <span class="msg-nick">${escapeHtml(d.opponent)}</span>
        <div class="msg-text">${hasOpponentReply ? escapeHtml(d.opponentText) : '<span style="color:var(--text-faint)">încă n-a răspuns...</span>'}</div>
        <div class="duel-vote-count">🔥 ${oVotes}</div>
      </div>
    </div>
  `;

  const sides = el.querySelectorAll('.duel-side');

  if (!hasOpponentReply && d.opponent === currentNick) {
    const replyBox = document.createElement('div');
    replyBox.style.marginTop = '10px';
    replyBox.style.display = 'flex';
    replyBox.style.gap = '8px';
    const input = document.createElement('input');
    input.placeholder = 'răspunde la duel...';
    input.style.flex = '1';
    input.style.padding = '8px';
    input.style.borderRadius = '6px';
    input.style.border = '1px solid var(--line)';
    input.style.background = 'var(--bg-input)';
    input.style.color = 'var(--text)';
    const btn = document.createElement('button');
    btn.className = 'btn-flame small';
    btn.textContent = 'răspunde';
    btn.addEventListener('click', async () => {
      const text = input.value.trim();
      if (!text) return;
      btn.disabled = true;
      if (await isMessageBlocked(text)) {
        alert('Replica ta conține limbaj interzis (ură reală, nu roast).');
        btn.disabled = false;
        return;
      }
      await update(ref(db, `duels/${roomId}/${id}`), { opponentText: text, opponentUid: currentUid });
    });
    replyBox.appendChild(input);
    replyBox.appendChild(btn);
    el.appendChild(replyBox);
  } else if (hasOpponentReply) {
    sides.forEach((sideEl, i) => {
      const side = i === 0 ? 'challenger' : 'opponent';
      sideEl.style.cursor = 'pointer';
      if (myVote === side) sideEl.style.borderColor = 'var(--flame-1)';
      sideEl.addEventListener('click', () => voteDuel(roomId, id, side, myVote));
    });
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

sendBtn.addEventListener('click', sendMessage);
msgInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendMessage(); });
profileBtn.addEventListener('click', openProfileModal);
closeProfileModal.addEventListener('click', closeProfileModalHandler);
window.addEventListener('hashchange', () => {
  const newRoom = getRoomFromHash();
  if (newRoom && newRoom !== currentRoom) {
    switchRoom(newRoom);
  }
});

shareRoomBtn.addEventListener('click', async () => {
  const roomUrl = `${window.location.origin}${window.location.pathname}#room=${encodeURIComponent(currentRoom)}`;
  try {
    await navigator.clipboard.writeText(roomUrl);
    const prevText = shareRoomBtn.textContent;
    shareRoomBtn.textContent = 'copiat!';
    setTimeout(() => { shareRoomBtn.textContent = prevText; }, 1200);
  } catch (err) {
    prompt('Copiază linkul camerei:', roomUrl);
  }
});

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

duelBtn.addEventListener('click', () => duelModal.classList.remove('hidden'));
duelCancelBtn.addEventListener('click', () => duelModal.classList.add('hidden'));

duelSendBtn.addEventListener('click', async () => {
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
  const duelsRef = ref(db, `duels/${currentRoom}`);
  await push(duelsRef, {
    challenger: currentNick,
    challengerUid: currentUid,
    opponent,
    challengerText: opening,
    opponentText: '',
    challengerVotes: 0,
    opponentVotes: 0,
    ts: Date.now(),
  });
  duelOpponent.value = '';
  duelOpening.value = '';
  duelModal.classList.add('hidden');
  duelSendBtn.disabled = false;
});

/* ---------------- LEADERBOARD ---------------- */

leaderboardBtn.addEventListener('click', async () => {
  roomView.classList.add('hidden');
  leaderboardView.classList.remove('hidden');
  await renderLeaderboard();
});
backToRoomBtn.addEventListener('click', () => {
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

adminBtn.addEventListener('click', async () => {
  roomView.classList.add('hidden');
  leaderboardView.classList.add('hidden');
  adminView.classList.remove('hidden');
  await updateAdminFilterUI();
  await renderAdmin();
});
backToRoomFromAdminBtn.addEventListener('click', () => {
  adminView.classList.add('hidden');
  roomView.classList.remove('hidden');
});

async function renderAdmin() {
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
      if (!confirm(`Blochezi definitiv utilizatorul "${r.nick}"?`)) return;
      await set(ref(db, `banned/${r.reportedUid}`), { reason: 'raport admin', ts: Date.now() });
      await remove(ref(db, `messages/${r.roomId}/${r.msgId}`));
      await remove(ref(db, `reports/${r.roomId}/${r.reportId}`));
      renderAdmin();
    });
    adminContent.appendChild(card);
  });
}

/* ---------------- UTIL ---------------- */

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
