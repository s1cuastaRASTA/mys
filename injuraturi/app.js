import { firebaseConfig } from './firebase-config.js';
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getDatabase, ref, push, set, update, get, onValue,
  query, orderByChild, serverTimestamp
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js";

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

/* -------------------------------------------------------------
   FILTRU DE MODERARE
   Nu blocăm cuvintele grele / injurăturile "clasice" — sunt punctul
   central al aplicației. Blocăm doar limbajul de ură reală (atacuri
   pe bază de rasă, etnie, orientare sexuală, dizabilitate).
   Adaugă propriile tale cuvinte/expresii (regex, case-insensitive)
   in lista de mai jos. Pentru o detecție mai bună decât o listă
   fixă, vezi sugestia din README.md despre integrarea Perspective API.
------------------------------------------------------------- */
const HATE_SPEECH_PATTERNS = [
  // exemplu: /cuvant-interzis/i,
];

function containsHateSpeech(text) {
  return HATE_SPEECH_PATTERNS.some((pattern) => pattern.test(text));
}

/* ------------------------------------------------------------- */

let currentNick = localStorage.getItem('roastarena_nick') || '';
let currentRoom = 'general';
let roomsCache = { general: { name: '#general' } };

const gate = document.getElementById('gate');
const appEl = document.getElementById('app');
const nickInput = document.getElementById('nickInput');
const enterBtn = document.getElementById('enterBtn');
const whoNick = document.getElementById('whoNick');
const changeNickBtn = document.getElementById('changeNick');

const roomList = document.getElementById('roomList');
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

function enterArena(nick) {
  currentNick = nick.trim().slice(0, 24);
  if (!currentNick) return;
  localStorage.setItem('roastarena_nick', currentNick);
  whoNick.textContent = currentNick;
  gate.classList.add('hidden');
  appEl.classList.remove('hidden');
  listenRooms();
  switchRoom('general');
}

if (currentNick) {
  nickInput.value = currentNick;
}
enterBtn.addEventListener('click', () => enterArena(nickInput.value));
nickInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') enterArena(nickInput.value); });
changeNickBtn.addEventListener('click', () => {
  appEl.classList.add('hidden');
  gate.classList.remove('hidden');
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
  listenFeed(roomId);
}

/* ---------------- MESSAGES + DUELS FEED ---------------- */

function listenFeed(roomId) {
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

  onValue(msgsRef, (snap) => { messages = snap.val() || {}; render(); });
  onValue(duelsRef, (snap) => { duels = snap.val() || {}; render(); });
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
  const voted = (m.voters || {})[currentNick];
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
    </div>
  `;
  el.querySelector('.vote-btn').addEventListener('click', () => toggleVote(roomId, id, !!voted));
  return el;
}

async function toggleVote(roomId, id, alreadyVoted) {
  const votersRef = ref(db, `messages/${roomId}/${id}/voters/${currentNick}`);
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
  const myVote = (d.voters || {})[currentNick];
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
      if (containsHateSpeech(text)) { alert('Replica ta conține limbaj interzis (ură reală, nu roast).'); return; }
      await update(ref(db, `duels/${roomId}/${id}`), { opponentText: text });
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
  updates[`voters/${currentNick}`] = side;
  await update(duelRef, updates);
}

/* ---------------- SEND MESSAGE ---------------- */

sendBtn.addEventListener('click', sendMessage);
msgInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendMessage(); });

async function sendMessage() {
  const text = msgInput.value.trim();
  if (!text) return;
  if (containsHateSpeech(text)) {
    filterWarning.classList.remove('hidden');
    return;
  }
  filterWarning.classList.add('hidden');
  const msgsRef = ref(db, `messages/${currentRoom}`);
  await push(msgsRef, { nick: currentNick, text, votes: 0, ts: Date.now() });
  msgInput.value = '';
}

/* ---------------- DUELS ---------------- */

duelBtn.addEventListener('click', () => duelModal.classList.remove('hidden'));
duelCancelBtn.addEventListener('click', () => duelModal.classList.add('hidden'));

duelSendBtn.addEventListener('click', async () => {
  const opponent = duelOpponent.value.trim();
  const opening = duelOpening.value.trim();
  if (!opponent || !opening) return;
  if (containsHateSpeech(opening)) { alert('Replica ta conține limbaj interzis (ură reală, nu roast).'); return; }
  const duelsRef = ref(db, `duels/${currentRoom}`);
  await push(duelsRef, {
    challenger: currentNick,
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

  const scores = {};
  Object.values(allMessages).forEach((roomMsgs) => {
    Object.values(roomMsgs).forEach((m) => {
      scores[m.nick] = (scores[m.nick] || 0) + (m.votes || 0);
    });
  });

  const ranked = Object.entries(scores).sort((a, b) => b[1] - a[1]).slice(0, 20);

  if (ranked.length === 0) {
    leaderboardContent.innerHTML = '<p style="color:var(--text-faint)">niciun roast votat încă. fii primul.</p>';
    return;
  }

  leaderboardContent.innerHTML = '';
  ranked.forEach(([nick, score], i) => {
    const tier = score >= 50 ? 'gold' : score >= 20 ? 'silver' : score >= 5 ? 'bronze' : null;
    const tierLabel = tier === 'gold' ? '🥇 legendă' : tier === 'silver' ? '🥈 veteran' : tier === 'bronze' ? '🥉 promițător' : '';
    const card = document.createElement('div');
    card.className = 'lb-card';
    card.innerHTML = `
      <span class="lb-rank">#${i + 1}</span>
      <span class="lb-nick">${escapeHtml(nick)}</span>
      <span class="lb-score">${score} 🔥 total</span>
      ${tier ? `<span class="lb-tier ${tier}">${tierLabel}</span>` : ''}
    `;
    leaderboardContent.appendChild(card);
  });
}

/* ---------------- UTIL ---------------- */

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
