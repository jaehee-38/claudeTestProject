const levelSelectEl = document.getElementById('level-select');
const studyScreenEl = document.getElementById('study-screen');
const doneScreenEl = document.getElementById('done-screen');
const levelListEl = document.getElementById('level-list');
const progressTextEl = document.getElementById('progress-text');
const progressFillEl = document.getElementById('progress-fill');
const wordEnEl = document.getElementById('word-en');
const wordKoEl = document.getElementById('word-ko');
const btnPlay = document.getElementById('btn-play');
const btnPrev = document.getElementById('btn-prev');
const btnNext = document.getElementById('btn-next');
const btnExit = document.getElementById('btn-exit');
const btnRepeat = document.getElementById('btn-repeat');
const btnBack = document.getElementById('btn-back');
const intervalInput = document.getElementById('interval');
const intervalValueEl = document.getElementById('interval-value');

let words = [];
let index = 0;
let isPlaying = false;
let sessionToken = 0;
let intervalSeconds = Number(intervalInput.value);

function showScreen(el) {
  [levelSelectEl, studyScreenEl, doneScreenEl].forEach(s => s.hidden = (s !== el));
}

async function loadLevels() {
  const res = await fetch('/api/levels');
  const levels = await res.json();
  levelListEl.innerHTML = '';
  levels.forEach(lv => {
    const btn = document.createElement('button');
    btn.className = 'level-btn';
    btn.innerHTML = `<span>${lv.label}</span><span class="count">${lv.count}개</span>`;
    btn.addEventListener('click', () => startLevel(lv.level));
    levelListEl.appendChild(btn);
  });
}

async function startLevel(level) {
  const res = await fetch(`/api/words?level=${level}`);
  words = await res.json();
  if (words.length === 0) return;
  index = 0;
  isPlaying = true;
  btnPlay.textContent = '⏸';
  showScreen(studyScreenEl);
  renderWord(false);
  updateProgress();
  playLoop(sessionToken);
}

function updateProgress() {
  progressTextEl.textContent = `${index + 1} / ${words.length}`;
  progressFillEl.style.width = `${((index + 1) / words.length) * 100}%`;
}

function renderWord(showMeaning) {
  const word = words[index];
  wordEnEl.textContent = word.english;
  wordKoEl.textContent = word.korean;
  wordKoEl.classList.toggle('hidden-meaning', !showMeaning);
}

function speak(text, lang, token) {
  return new Promise(resolve => {
    if (token !== sessionToken) return resolve(false);
    speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = lang;
    utter.onend = () => resolve(token === sessionToken);
    utter.onerror = () => resolve(false);
    speechSynthesis.speak(utter);
  });
}

function wait(ms, token) {
  return new Promise(resolve => {
    setTimeout(() => resolve(token === sessionToken), ms);
  });
}

async function playLoop(token) {
  while (token === sessionToken && isPlaying) {
    renderWord(false);
    updateProgress();

    if (!(await speak(words[index].english, 'en-US', token))) return;
    renderWord(true);

    if (!(await wait(300, token))) return;
    if (!(await speak(words[index].korean, 'ko-KR', token))) return;

    if (!(await wait(intervalSeconds * 1000, token))) return;

    if (index + 1 >= words.length) {
      showScreen(doneScreenEl);
      isPlaying = false;
      return;
    }
    index++;
  }
}

function stopCurrentAudio() {
  speechSynthesis.cancel();
  sessionToken++;
}

btnPlay.addEventListener('click', () => {
  if (isPlaying) {
    stopCurrentAudio();
    isPlaying = false;
    btnPlay.textContent = '▶';
  } else {
    isPlaying = true;
    btnPlay.textContent = '⏸';
    playLoop(sessionToken);
  }
});

btnNext.addEventListener('click', () => {
  stopCurrentAudio();
  index = (index + 1) % words.length;
  updateProgress();
  if (isPlaying) {
    playLoop(sessionToken);
  } else {
    renderWord(false);
  }
});

btnPrev.addEventListener('click', () => {
  stopCurrentAudio();
  index = (index - 1 + words.length) % words.length;
  updateProgress();
  if (isPlaying) {
    playLoop(sessionToken);
  } else {
    renderWord(false);
  }
});

btnExit.addEventListener('click', () => {
  stopCurrentAudio();
  isPlaying = false;
  showScreen(levelSelectEl);
});

btnBack.addEventListener('click', () => {
  showScreen(levelSelectEl);
});

btnRepeat.addEventListener('click', () => {
  index = 0;
  isPlaying = true;
  btnPlay.textContent = '⏸';
  showScreen(studyScreenEl);
  playLoop(sessionToken);
});

intervalInput.addEventListener('input', () => {
  intervalSeconds = Number(intervalInput.value);
  intervalValueEl.textContent = intervalSeconds;
});

loadLevels();
