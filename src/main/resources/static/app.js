'use strict';

// =====================================================================
// 테마 (라이트 / 다크 모드)
// =====================================================================

(function initTheme() {
  const saved = localStorage.getItem('theme'); // 'dark' | 'light' | null
  const isDark = saved === 'dark';              // 기본값 = 라이트
  if (isDark) document.documentElement.setAttribute('data-theme', 'dark');

  document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('theme-toggle');
    const icon   = document.getElementById('theme-icon');
    const label  = document.getElementById('theme-label');
    if (!toggle) return;

    function applyTheme(dark) {
      if (dark) {
        document.documentElement.setAttribute('data-theme', 'dark');
        icon.textContent  = '🌙';
        label.textContent = '다크';
        localStorage.setItem('theme', 'dark');
      } else {
        document.documentElement.removeAttribute('data-theme');
        icon.textContent  = '☀️';
        label.textContent = '라이트';
        localStorage.setItem('theme', 'light');
      }
    }

    // 초기 UI 동기화
    applyTheme(isDark);

    toggle.addEventListener('click', () => {
      const nowDark = document.documentElement.getAttribute('data-theme') === 'dark';
      applyTheme(!nowDark);
    });
  });
})();

// =====================================================================
// 공통 유틸
// =====================================================================

const $ = (sel) => document.querySelector(sel);

const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const nf0 = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 });
const nf2 = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 });
const nf4 = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 4 });

const won = (v) => (v == null ? '-' : nf0.format(v) + '원');
const num = (v, f = nf2) => (v == null ? '-' : f.format(v));
const signCls = (v) => (v == null || Number(v) === 0 ? '' : Number(v) > 0 ? 'up' : 'down');
const signed = (v, f = nf0) => (v == null ? '-' : (Number(v) > 0 ? '+' : '') + f.format(v));
const pct = (v) => (v == null ? '-' : (Number(v) > 0 ? '+' : '') + nf2.format(v) + '%');
const fmtDate = (ymd) => (ymd && ymd.length === 8 ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6)}` : esc(ymd));

/** 만원 단위 금액을 "12억 3,500만" 형태로 */
function manwon(v) {
  if (v == null) return '-';
  const eok = Math.floor(v / 10000);
  const rest = v % 10000;
  if (eok === 0) return nf0.format(rest) + '만';
  return eok + '억' + (rest ? ' ' + nf0.format(rest) + '만' : '');
}

async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: options.body ? { 'Content-Type': 'application/json' } : {},
    ...options,
  });
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch { /* 본문 없음 */ }
  if (!res.ok) {
    const err = new Error(data?.message || `요청 실패 (${res.status})`);
    err.status = res.status;
    throw err;
  }
  return data;
}

/**
 * 화면 쪽 짧은 캐시. 대시보드 → 상세 화면으로 갈 때 같은 데이터를 다시 받지 않도록.
 * (서버도 따로 캐시하고 있어서 여기선 짧게만 둡니다)
 */
const memo = new Map();
function cached(key, loader, ttlMs = 60_000) {
  const hit = memo.get(key);
  if (hit && Date.now() < hit.until) return hit.promise;
  const promise = loader();
  memo.set(key, { promise, until: Date.now() + ttlMs });
  promise.catch(() => memo.delete(key)); // 실패는 캐시하지 않음
  return promise;
}
function invalidate(...prefixes) {
  for (const k of [...memo.keys()]) if (prefixes.some((p) => k.startsWith(p))) memo.delete(k);
}

const data = {
  portfolio: () => cached('portfolio', () => api('/api/portfolio'), 30_000),
  gold: () => cached('gold', () => api('/api/gold?days=30'), 5 * 60_000),
  exchange: () => cached('exchange', () => api('/api/exchange'), 5 * 60_000),
  watchlist: () => cached('watchlist', () => api('/api/watchlist'), 5 * 60_000),
  quote: (code) => cached(`quote:${code}`, () => api(`/api/stocks/quote?code=${encodeURIComponent(code)}`), 30_000),
  subscriptions: () => cached('subscriptions', () => api('/api/subscriptions'), 10 * 60_000),
  apt: (lawdCd, ym) => cached(`apt:${lawdCd}:${ym}`,
    () => api(`/api/real-estate/apt-trades?lawdCd=${encodeURIComponent(lawdCd)}&dealYm=${encodeURIComponent(ym)}`), 10 * 60_000),
};

function showMsg(el, text, kind = 'error') {
  el.textContent = text;
  el.classList.toggle('info', kind === 'info');
  el.hidden = !text;
}

function emptyRow(tbody, cols, text) {
  tbody.innerHTML = `<tr><td class="empty" colspan="${cols}">${esc(text)}</td></tr>`;
}

const store = {
  get(key, fallback) {
    try { return JSON.parse(localStorage.getItem(key)) ?? fallback; } catch { return fallback; }
  },
  set(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* 무시 */ }
  },
};

const skeleton = (...sizes) => sizes.map((s) => `<span class="skel ${s}"></span>`).join('');

/** 대시보드 카드용 짧은 오류 표시 */
function cardError(e) {
  const title = e.status === 503 ? 'API 키 미설정' : '불러오지 못했습니다';
  return `<div class="derr"><b>${title}</b>${esc(e.message)}</div>`;
}

// =====================================================================
// 라우터  (#/  #/portfolio  #/stock  #/stock/005930  #/gold  #/exchange  #/realestate)
// =====================================================================

const views = {
  home: renderHome,
  portfolio: renderPortfolioView,
  stock: renderStockView,
  gold: renderGoldView,
  exchange: renderExchangeView,
  realestate: renderRealEstateView,
  subscription: renderSubscriptionView,
  invest: renderInvestView,
};

function route() {
  const [, name = '', param] = location.hash.replace(/^#/, '').split('/');
  const view = views[name] ? name : 'home';
  document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.route === view));
  document.querySelectorAll('.view').forEach((v) => { v.hidden = v.id !== `view-${view}`; });
  window.scrollTo(0, 0);
  views[view](param);
}

window.addEventListener('hashchange', route);

// =====================================================================
// API 키 상태
// =====================================================================

let status = {};

async function loadStatus() {
  try { status = await api('/api/status'); } catch { status = {}; }
  const items = [
    ['공공데이터포털', status.dataGoKr],
    ['수출입은행', status.koreaexim],
    [`한국투자증권${status.kisMock ? '(모의)' : ''}`, status.kis],
  ];
  $('#key-status').innerHTML = items
    .map(([n, on]) => `<span class="key ${on ? 'on' : 'off'}" title="${on ? '키 설정됨' : '.env 에 키 없음'}">${esc(n)}</span>`)
    .join('');
  $('#kis-mode').textContent = status.kisMock ? '모의투자' : '실전';
}

// =====================================================================
// 홈 (대시보드) — 카드마다 따로 불러와서 먼저 온 것부터 보여줍니다
// =====================================================================

const TYPE_ORDER = ['STOCK', 'GOLD', 'CURRENCY', 'REAL_ESTATE', 'ETC'];
const ALLOC_COLORS = { STOCK: 'var(--alloc-1)', GOLD: 'var(--alloc-2)', CURRENCY: 'var(--alloc-3)', REAL_ESTATE: 'var(--alloc-4)', ETC: 'var(--alloc-5)' };
const FX_MAIN = ['USD', 'JPY', 'EUR', 'CNH', 'GBP'];
const DASH_STOCKS = 5;

function renderHome() {
  dashPortfolio();
  dashGold();
  dashExchange();
  dashStocks();
  dashRealEstate();
  dashSubscription();
}

function allocBar(allocation) {
  if (!allocation.length) return '';
  const sorted = [...allocation].sort((a, b) => TYPE_ORDER.indexOf(a.assetType) - TYPE_ORDER.indexOf(b.assetType));
  return `
    <div class="alloc">
      <div class="alloc-bar">${sorted.map((a) => `<span style="flex:${Number(a.ratio)};background:${ALLOC_COLORS[a.assetType]}" title="${esc(a.label)} ${nf2.format(a.ratio)}%"></span>`).join('')}</div>
      <div class="alloc-legend">${sorted.map((a) => `<span><i style="background:${ALLOC_COLORS[a.assetType]}"></i>${esc(a.label)} ${nf2.format(a.ratio)}%</span>`).join('')}</div>
    </div>`;
}

async function dashPortfolio() {
  const body = $('#d-portfolio .dcard-body');
  body.innerHTML = skeleton('lg', 'md', 'sm');
  try {
    const p = await data.portfolio();
    if (!p.items.length) {
      body.innerHTML = '<div class="derr">아직 등록한 자산이 없습니다. 눌러서 보유 자산을 추가하세요.</div>';
      return;
    }
    const top = p.items.filter((h) => h.value != null).sort((a, b) => b.value - a.value).slice(0, 4);
    body.innerHTML = `
      <div class="kpis">
        <div><div class="label">총 평가금액</div><div class="big">${won(p.totalValue)}</div></div>
        <div><div class="label">평가손익</div><div class="v ${signCls(p.totalProfit)}">${signed(p.totalProfit)}원</div>
          <div class="sub ${signCls(p.totalProfitRate)}">${pct(p.totalProfitRate)}</div></div>
        <div><div class="label">총 매입금액</div><div class="v">${won(p.totalCost)}</div></div>
      </div>
      ${allocBar(p.allocation)}
      <div class="dlist">${top.map((h) => `
        <div class="r"><span class="nm">${esc(h.name)}<small>${esc(h.assetTypeLabel)}</small></span>
          <span class="vals">${won(h.value)}<small class="${signCls(h.profitRate)}">${pct(h.profitRate)}</small></span></div>`).join('')}
      </div>
      ${p.unpricedCount ? `<div class="dnote">현재가를 못 구한 자산 ${p.unpricedCount}개는 합계에서 빠졌습니다.</div>` : ''}`;
  } catch (e) {
    body.innerHTML = cardError(e);
  }
}

async function dashGold() {
  const body = $('#d-gold .dcard-body');
  body.innerHTML = skeleton('lg', 'md', 'sm');
  try {
    const list = await data.gold();
    if (!list.length) { body.innerHTML = '<div class="derr">최근 시세가 없습니다.</div>'; return; }
    const latestDate = list[0].baseDate;
    const latest = list.filter((g) => g.baseDate === latestDate);
    const main = latest.find((g) => /1kg/i.test(g.itemName || '')) || latest[0];
    body.innerHTML = `
      <div class="label muted small">${esc(main.itemName)}</div>
      <div class="big">${num(main.close, nf0)}<span class="muted small"> 원/g</span></div>
      <div class="${signCls(main.change)}">${signed(main.change)} (${pct(main.changeRate)})</div>
      <div class="dnote">한 돈(3.75g) ≈ ${main.close == null ? '-' : won(main.close * 3.75)} · ${fmtDate(main.baseDate)} 기준</div>`;
  } catch (e) {
    body.innerHTML = cardError(e);
  }
}

async function dashExchange() {
  const body = $('#d-exchange .dcard-body');
  body.innerHTML = skeleton('md', 'md', 'md', 'sm');
  try {
    const fx = await data.exchange();
    const main = FX_MAIN.slice(0, 4).map((c) => fx.rates.find((r) => r.code === c)).filter(Boolean);
    body.innerHTML = `
      <div class="dlist">${main.map((r) => `
        <div class="r"><span class="nm">${esc(r.code)}${r.unit !== 1 ? ` ${r.unit}` : ''}<small>${esc(r.name)}</small></span>
          <span class="vals"><b>${num(r.baseRate)}</b>원</span></div>`).join('')}
      </div>
      <div class="dnote">매매기준율 · ${esc(fx.date)} 기준</div>`;
  } catch (e) {
    body.innerHTML = cardError(e);
  }
}

/** 관심종목이 없으면 내 투자에 등록한 주식을 대신 보여줍니다 */
async function dashboardStockList() {
  const watch = await data.watchlist();
  if (watch.length) return { items: watch, source: 'watch' };
  try {
    const p = await data.portfolio();
    const held = p.items.filter((h) => h.assetType === 'STOCK' && h.code)
      .map((h) => ({ code: h.code, name: h.name }));
    return { items: held, source: 'held' };
  } catch {
    return { items: [], source: 'held' };
  }
}

async function dashStocks() {
  const body = $('#d-stock .dcard-body');
  body.innerHTML = skeleton('md', 'md', 'md');
  let list;
  try {
    list = await dashboardStockList();
  } catch (e) {
    body.innerHTML = cardError(e);
    return;
  }
  if (!list.items.length) {
    body.innerHTML = '<div class="derr">관심종목이 없습니다. <a href="#/stock">주식 화면</a>에서 종목을 조회하고 ☆ 를 눌러 추가하세요.</div>';
    return;
  }
  const shown = list.items.slice(0, DASH_STOCKS);
  body.innerHTML = `
    <div class="dlist">${shown.map((s) => `
      <a class="r" href="#/stock/${esc(s.code)}" data-code="${esc(s.code)}">
        <span class="nm"><span class="nm-text">${esc(s.name || s.code)}</span><small>${esc(s.code)}</small></span>
        <span class="vals"><span class="skel sm" style="width:90px"></span></span>
      </a>`).join('')}
    </div>
    <div class="dnote">${list.source === 'held' ? '관심종목이 없어 보유 주식을 보여줍니다.' : ''}${list.items.length > DASH_STOCKS ? ` 외 ${list.items.length - DASH_STOCKS}개` : ''}</div>`;

  // 이름이 없는 종목은 내 투자에 등록한 이름으로 나중에 채웁니다 (내 투자 로딩을 기다리지 않음)
  fillNames(body, shown.filter((s) => !s.name).map((s) => s.code));

  // 종목마다 따로 불러와서 온 순서대로 채웁니다 (모의투자는 서버가 호출 간격을 둠)
  shown.forEach(async (s) => {
    const cell = body.querySelector(`[data-code="${CSS.escape(s.code)}"] .vals`);
    try {
      const q = await data.quote(s.code);
      cell.innerHTML = `<b>${num(q.price, nf0)}</b><small class="${signCls(q.change)}">${pct(q.changeRate)}</small>`;
    } catch (e) {
      cell.innerHTML = `<small class="muted" title="${esc(e.message)}">${e.status === 503 ? '키 미설정' : '조회 실패'}</small>`;
    }
  });
}

async function dashRealEstate() {
  const body = $('#d-realestate .dcard-body');
  const region = store.get('reRegion', '11680');
  const code = region === 'custom' ? store.get('reCode', '') : region;
  const ym = store.get('reMonth', null) || defaultMonth();
  const regionName = (REGIONS.find(([c]) => c === code) || [code, `지역코드 ${code}`])[1];
  if (!/^\d{5}$/.test(code)) {
    body.innerHTML = '<div class="derr">부동산 화면에서 지역을 한 번 조회하면 여기에 요약됩니다.</div>';
    return;
  }
  body.innerHTML = skeleton('sm', 'lg', 'md');
  try {
    const trades = await data.apt(code, ym.replace('-', ''));
    const s = aptStats(trades);
    body.innerHTML = s ? `
      <div class="label muted small">${esc(regionName)} · ${esc(ym.replace('-', '.'))} 아파트 매매</div>
      <div class="big">${manwon(s.median)}</div>
      <div class="muted small">중간 거래가</div>
      <div class="dnote">거래 ${nf0.format(s.count)}건 · 최고 ${manwon(s.max)}${s.avgPy ? ` · 평당 ${manwon(Math.round(s.avgPy))}` : ''}</div>`
      : `<div class="derr">${esc(regionName)} ${esc(ym)} 거래가 없습니다.</div>`;
  } catch (e) {
    body.innerHTML = cardError(e);
  }
}

// =====================================================================
// 내 투자 (상세)
// =====================================================================

const TYPE_META = {
  STOCK: { code: '종목코드', codePh: '005930', qty: '수량(주)', avg: '평균매입가(원/주)', hint: '현재가는 한국투자증권 시세로 자동 평가됩니다.' },
  GOLD: { code: null, qty: '보유량(g)', avg: '평균매입가(원/g)', hint: '현재가는 KRX 금 99.99_1Kg 종가(원/g)로 자동 평가됩니다. 1돈 = 3.75g' },
  CURRENCY: { code: '통화코드', codePh: 'USD', qty: '보유 외화 금액', avg: '평균 환전 환율(원/1단위)', hint: '현재가는 수출입은행 매매기준율로 자동 평가됩니다. JPY도 1엔 기준으로 입력하세요.' },
  REAL_ESTATE: { code: null, qty: '수량', avg: '매입가(원)', hint: '부동산은 현재가(시세)를 직접 입력하세요. 수량은 보통 1입니다.' },
  ETC: { code: null, qty: '수량', avg: '평균매입가(원)', hint: '예금·코인 등. 현재가를 직접 입력하세요.' },
};

let portfolio = null;

async function renderPortfolioView() {
  const tbody = $('#pf-rows');
  if (!portfolio) emptyRow(tbody, 9, '불러오는 중…');
  showMsg($('#pf-msg'), '');
  try {
    portfolio = await data.portfolio();
    drawPortfolio();
  } catch (e) {
    $('#pf-summary').innerHTML = '';
    $('#pf-alloc').innerHTML = '';
    emptyRow(tbody, 9, '불러오지 못했습니다.');
    showMsg($('#pf-msg'), e.message);
  }
}

function drawPortfolio() {
  const p = portfolio;
  $('#pf-summary').innerHTML = `
    <div class="card"><div class="label">총 평가금액</div><div class="value">${won(p.totalValue)}</div></div>
    <div class="card"><div class="label">총 매입금액</div><div class="value">${won(p.totalCost)}</div></div>
    <div class="card"><div class="label">평가손익</div><div class="value ${signCls(p.totalProfit)}">${signed(p.totalProfit)}원</div></div>
    <div class="card"><div class="label">수익률</div><div class="value ${signCls(p.totalProfitRate)}">${pct(p.totalProfitRate)}</div></div>`;
  $('#pf-alloc').innerHTML = allocBar(p.allocation);

  showMsg($('#pf-msg'), p.unpricedCount
    ? `현재가를 구하지 못한 자산 ${p.unpricedCount}개는 합계에서 빠졌습니다. 표에서 이유를 확인하세요.` : '', 'info');

  const tbody = $('#pf-rows');
  if (!p.items.length) {
    emptyRow(tbody, 9, '아직 등록한 자산이 없습니다. "+ 자산 추가"로 보유 자산을 넣어보세요.');
    return;
  }
  const items = [...p.items].sort((a, b) => TYPE_ORDER.indexOf(a.assetType) - TYPE_ORDER.indexOf(b.assetType) || a.id - b.id);
  tbody.innerHTML = items.map((h) => `
    <tr>
      <td><span class="type-tag">${esc(h.assetTypeLabel)}</span></td>
      <td>${h.assetType === 'STOCK' && h.code
        ? `<a class="lnk" href="#/stock/${esc(h.code)}">${esc(h.name)}</a>`
        : esc(h.name)}${h.code ? `<span class="code">${esc(h.code)}</span>` : ''}${h.memo ? `<span class="src">${esc(h.memo)}</span>` : ''}</td>
      <td class="num">${num(h.quantity, nf4)}</td>
      <td class="num">${num(h.averagePrice)}</td>
      <td class="num">${h.currentPrice != null
        ? `${num(h.currentPrice)}<span class="src">${esc(h.priceSource)}</span>`
        : `-<span class="warn">${esc(h.priceError)}</span>`}</td>
      <td class="num">${won(h.value)}</td>
      <td class="num ${signCls(h.profit)}">${h.profit == null ? '-' : signed(h.profit)}</td>
      <td class="num ${signCls(h.profitRate)}">${pct(h.profitRate)}</td>
      <td><div class="row-actions">
        <button class="btn ghost icon" data-edit="${h.id}">수정</button>
        <button class="btn ghost icon" data-del="${h.id}">삭제</button>
      </div></td>
    </tr>`).join('');
}

$('#pf-rows').addEventListener('click', (e) => {
  const edit = e.target.closest('[data-edit]');
  const del = e.target.closest('[data-del]');
  if (edit) openHolding(portfolio.items.find((h) => h.id === Number(edit.dataset.edit)));
  if (del) confirmDelete(portfolio.items.find((h) => h.id === Number(del.dataset.del)));
});
$('#pf-refresh').addEventListener('click', () => { invalidate('portfolio', 'quote:'); renderPortfolioView(); });
$('#pf-add').addEventListener('click', () => openHolding(null));

// 자산 추가/수정 다이얼로그
const hDialog = $('#holding-dialog');
const hForm = $('#holding-form');
let editingId = null;

function applyTypeMeta() {
  const m = TYPE_META[$('#h-type').value];
  $('#h-code-wrap').hidden = !m.code;
  if (m.code) {
    $('#h-code-label').textContent = m.code;
    hForm.code.placeholder = m.codePh;
  }
  $('#h-qty-label').textContent = m.qty;
  $('#h-avg-label').textContent = m.avg;
  $('#h-hint').textContent = m.hint;
}
$('#h-type').addEventListener('change', applyTypeMeta);

function openHolding(h, preset = {}) {
  editingId = h ? h.id : null;
  hForm.reset();
  $('#holding-title').textContent = h ? '자산 수정' : '자산 추가';
  $('#h-error').hidden = true;
  const v = h || preset;
  if (v.assetType) hForm.assetType.value = v.assetType;
  hForm.name.value = v.name ?? '';
  hForm.code.value = v.code ?? '';
  hForm.quantity.value = v.quantity ?? '';
  hForm.averagePrice.value = v.averagePrice ?? '';
  hForm.manualPrice.value = v.manualPrice ?? '';
  hForm.memo.value = v.memo ?? '';
  applyTypeMeta();
  hDialog.showModal();
}

$('#h-cancel').addEventListener('click', () => hDialog.close());

hForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = hForm;
  const body = {
    assetType: f.assetType.value,
    name: f.name.value,
    code: TYPE_META[f.assetType.value].code ? f.code.value : null,
    quantity: f.quantity.value === '' ? null : Number(f.quantity.value),
    averagePrice: f.averagePrice.value === '' ? null : Number(f.averagePrice.value),
    manualPrice: f.manualPrice.value === '' ? null : Number(f.manualPrice.value),
    memo: f.memo.value,
  };
  const submit = f.querySelector('[type=submit]');
  submit.disabled = true;
  try {
    if (editingId) {
      await api(`/api/portfolio/holdings/${editingId}`, { method: 'PUT', body: JSON.stringify(body) });
    } else {
      await api('/api/portfolio/holdings', { method: 'POST', body: JSON.stringify(body) });
    }
    hDialog.close();
    invalidate('portfolio');
    route();
  } catch (err) {
    $('#h-error').textContent = err.message;
    $('#h-error').hidden = false;
  } finally {
    submit.disabled = false;
  }
});

// 삭제 확인 (브라우저 기본 confirm 대신 다이얼로그)
const cDialog = $('#confirm-dialog');
let deletingId = null;

function confirmDelete(h) {
  deletingId = h.id;
  $('#confirm-text').textContent = `"${h.name}" 자산을 삭제할까요?`;
  cDialog.returnValue = '';
  cDialog.showModal();
}

cDialog.addEventListener('close', async () => {
  if (cDialog.returnValue !== 'ok' || deletingId == null) return;
  try {
    await api(`/api/portfolio/holdings/${deletingId}`, { method: 'DELETE' });
    invalidate('portfolio');
    renderPortfolioView();
  } catch (e) {
    showMsg($('#pf-msg'), e.message);
  }
});

// 한국투자증권 계좌 잔고
$('#kis-load').addEventListener('click', async () => {
  const box = $('#kis-box');
  const btn = $('#kis-load');
  btn.disabled = true;
  box.className = 'muted small';
  box.textContent = '불러오는 중…';
  try {
    const b = await api('/api/stocks/balance');
    box.className = '';
    box.innerHTML = `
      <div class="cards">
        <div class="card"><div class="label">예수금</div><div class="value">${won(b.deposit)}</div></div>
        <div class="card"><div class="label">주식 평가금액</div><div class="value">${won(b.totalEvaluation)}</div></div>
        <div class="card"><div class="label">평가손익</div><div class="value ${signCls(b.totalProfit)}">${signed(b.totalProfit)}원</div></div>
        <div class="card"><div class="label">순자산</div><div class="value">${won(b.netAsset)}</div></div>
      </div>
      <div class="table-wrap"><table class="data">
        <thead><tr><th>종목</th><th class="num">수량</th><th class="num">평균단가</th><th class="num">현재가</th><th class="num">평가금액</th><th class="num">손익</th><th class="num">수익률</th></tr></thead>
        <tbody>${b.items.length ? b.items.map((i) => `
          <tr>
            <td><a class="lnk" href="#/stock/${esc(i.code)}">${esc(i.name)}</a><span class="code">${esc(i.code)}</span></td>
            <td class="num">${num(i.quantity, nf0)}</td>
            <td class="num">${num(i.averagePrice)}</td>
            <td class="num">${num(i.currentPrice, nf0)}</td>
            <td class="num">${won(i.evaluationAmount)}</td>
            <td class="num ${signCls(i.profit)}">${signed(i.profit)}</td>
            <td class="num ${signCls(i.profitRate)}">${pct(i.profitRate)}</td>
          </tr>`).join('') : '<tr><td class="empty" colspan="7">보유 종목이 없습니다.</td></tr>'}
        </tbody>
      </table></div>`;
  } catch (e) {
    box.className = 'msg';
    box.textContent = e.message;
  } finally {
    btn.disabled = false;
  }
});

// =====================================================================
// 주식 (검색 + 종목 상세 + 관심종목)
// =====================================================================

/** 종목명 찾기: 관심종목에 붙인 이름 → 내 투자에 등록한 이름 순 (KIS 시세 API 는 종목명을 주지 않음) */
async function nameLookup() {
  const names = {};
  try { (await data.portfolio()).items.forEach((h) => { if (h.code && h.assetType === 'STOCK') names[h.code] = h.name; }); } catch { /* 무시 */ }
  try { (await data.watchlist()).forEach((w) => { if (w.name) names[w.code] = w.name; }); } catch { /* 무시 */ }
  return names;
}

/** 이름 없이 코드로 먼저 그려둔 행에, 이름을 찾는 대로 채워 넣습니다 */
async function fillNames(container, codes) {
  if (!codes.length) return;
  const names = await nameLookup();
  codes.forEach((code) => {
    if (!names[code]) return;
    const el = container.querySelector(`[data-code="${CSS.escape(code)}"] .nm-text`);
    if (el) el.textContent = names[code];
  });
}

$('#stock-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const code = $('#stock-code').value.trim().toUpperCase();
  location.hash = `#/stock/${encodeURIComponent(code)}`;
});

function renderStockView(param) {
  const code = param ? decodeURIComponent(param).toUpperCase() : null;
  $('#stock-code').value = code || '';
  const detail = $('#stock-detail');
  detail.hidden = !code;
  if (code) renderStockDetail(code);
  renderWatchTable();
}

async function renderStockDetail(code) {
  const out = $('#stock-detail');
  out.innerHTML = `<div class="quote">${skeleton('sm', 'lg', 'md')}</div>`;
  const [q, watch, names, held] = await Promise.all([
    data.quote(code).catch((e) => e),
    data.watchlist().catch(() => []),
    nameLookup(),
    data.portfolio().then((p) => p.items.filter((h) => h.assetType === 'STOCK' && h.code === code)).catch(() => []),
  ]);
  if (q instanceof Error) {
    out.innerHTML = `<div class="msg">${esc(q.message)}</div>`;
    return;
  }
  const inWatch = watch.find((w) => w.code === code);
  const name = names[code];
  out.innerHTML = `
    <div class="quote">
      <div class="quote-head">
        <strong>${esc(name || code)}</strong>
        ${name ? `<span class="muted small">${esc(code)}</span>` : ''}
        ${q.sector ? `<span class="muted small">${esc(q.sector)}</span>` : ''}
        <div class="quote-actions">
          ${inWatch ? '' : `<input id="watch-name" placeholder="표시 이름 (선택)" value="${esc(name || '')}" maxlength="100">`}
          <button id="watch-toggle" class="star ${inWatch ? 'on' : ''}">${inWatch ? '★ 관심종목' : '☆ 관심종목 추가'}</button>
        </div>
      </div>
      <div class="quote-head">
        <span class="quote-price ${signCls(q.change)}">${num(q.price, nf0)}원</span>
        <span class="${signCls(q.change)}">${signed(q.change)} (${pct(q.changeRate)})</span>
      </div>
      <div class="quote-grid">
        <div><span>시가</span><b>${num(q.open, nf0)}</b></div>
        <div><span>고가</span><b>${num(q.high, nf0)}</b></div>
        <div><span>저가</span><b>${num(q.low, nf0)}</b></div>
        <div><span>거래량</span><b>${num(q.volume, nf0)}</b></div>
        <div><span>52주 최고</span><b>${num(q.high52w, nf0)}</b></div>
        <div><span>52주 최저</span><b>${num(q.low52w, nf0)}</b></div>
        <div><span>PER</span><b>${num(q.per)}</b></div>
        <div><span>PBR</span><b>${num(q.pbr)}</b></div>
      </div>
      ${held.length ? held.map((h) => `
        <div class="held">내 보유: <b>${num(h.quantity, nf4)}주</b> · 평균 ${num(h.averagePrice)}원 ·
          평가 ${won(h.value)} <span class="${signCls(h.profitRate)}">(${pct(h.profitRate)})</span></div>`).join('')
        : `<div class="held muted">보유하고 있지 않은 종목입니다. <button id="hold-add" class="btn ghost icon">내 투자에 추가</button></div>`}
    </div>`;

  $('#watch-toggle').addEventListener('click', async (ev) => {
    ev.target.disabled = true;
    try {
      if (inWatch) {
        await api(`/api/watchlist/${encodeURIComponent(code)}`, { method: 'DELETE' });
      } else {
        await api('/api/watchlist', { method: 'POST', body: JSON.stringify({ code, name: $('#watch-name').value }) });
      }
      invalidate('watchlist');
      renderStockDetail(code);
      renderWatchTable();
    } catch (e) {
      ev.target.disabled = false;
      out.insertAdjacentHTML('beforeend', `<div class="msg">${esc(e.message)}</div>`);
    }
  });
  $('#hold-add')?.addEventListener('click', () =>
    openHolding(null, { assetType: 'STOCK', code, name: name || '', averagePrice: q.price }));
}

async function renderWatchTable() {
  const tbody = $('#watch-rows');
  let watch;
  try {
    watch = await data.watchlist();
  } catch (e) {
    emptyRow(tbody, 5, e.message);
    return;
  }
  if (!watch.length) {
    emptyRow(tbody, 5, '관심종목이 없습니다. 종목을 조회한 뒤 ☆ 를 눌러 추가하세요.');
    return;
  }
  tbody.innerHTML = watch.map((w) => `
    <tr data-code="${esc(w.code)}">
      <td><a class="lnk nm-text" href="#/stock/${esc(w.code)}">${esc(w.name || w.code)}</a><span class="code">${esc(w.code)}</span></td>
      <td class="num q-price"><span class="skel sm" style="width:70px;margin-left:auto"></span></td>
      <td class="num q-change"></td>
      <td class="num q-rate"></td>
      <td><div class="row-actions"><button class="btn ghost icon" data-unwatch="${esc(w.code)}">삭제</button></div></td>
    </tr>`).join('');
  fillNames(tbody, watch.filter((w) => !w.name).map((w) => w.code));
  watch.forEach(async (w) => {
    const row = tbody.querySelector(`tr[data-code="${CSS.escape(w.code)}"]`);
    try {
      const q = await data.quote(w.code);
      row.querySelector('.q-price').textContent = num(q.price, nf0);
      row.querySelector('.q-change').className = `num q-change ${signCls(q.change)}`;
      row.querySelector('.q-change').textContent = signed(q.change);
      row.querySelector('.q-rate').className = `num q-rate ${signCls(q.changeRate)}`;
      row.querySelector('.q-rate').textContent = pct(q.changeRate);
    } catch (e) {
      row.querySelector('.q-price').innerHTML = `<span class="muted small" title="${esc(e.message)}">${e.status === 503 ? '키 미설정' : '조회 실패'}</span>`;
    }
  });
}

$('#watch-rows').addEventListener('click', async (e) => {
  const btn = e.target.closest('[data-unwatch]');
  if (!btn) return;
  btn.disabled = true;
  try {
    await api(`/api/watchlist/${encodeURIComponent(btn.dataset.unwatch)}`, { method: 'DELETE' });
    invalidate('watchlist');
    route();
  } catch {
    btn.disabled = false;
  }
});
$('#watch-refresh').addEventListener('click', () => { invalidate('quote:', 'watchlist'); route(); });

// =====================================================================
// 금 (상세)
// =====================================================================

async function renderGoldView() {
  const tbody = $('#gold-rows');
  emptyRow(tbody, 7, '불러오는 중…');
  showMsg($('#gold-msg'), '');
  try {
    const list = await data.gold();
    if (!list.length) {
      $('#gold-cards').innerHTML = '';
      emptyRow(tbody, 7, '최근 30일 시세가 없습니다.');
      return;
    }
    const latestDate = list[0].baseDate;
    const latest = list.filter((g) => g.baseDate === latestDate);
    $('#gold-cards').innerHTML = latest.map((g) => `
      <div class="card">
        <div class="label">${esc(g.itemName)} · ${fmtDate(g.baseDate)}</div>
        <div class="value">${num(g.close, nf0)}<span class="muted small"> 원/g</span></div>
        <div class="sub ${signCls(g.change)}">${signed(g.change)} (${pct(g.changeRate)})</div>
        <div class="sub muted">한 돈(3.75g) ≈ ${g.close == null ? '-' : won(g.close * 3.75)}</div>
      </div>`).join('');
    tbody.innerHTML = list.map((g) => `
      <tr>
        <td>${fmtDate(g.baseDate)}</td>
        <td>${esc(g.itemName)}</td>
        <td class="num">${num(g.close, nf0)}</td>
        <td class="num ${signCls(g.change)}">${signed(g.change)}</td>
        <td class="num ${signCls(g.changeRate)}">${pct(g.changeRate)}</td>
        <td class="num">${num(g.high, nf0)}</td>
        <td class="num">${num(g.low, nf0)}</td>
      </tr>`).join('');
  } catch (e) {
    $('#gold-cards').innerHTML = '';
    emptyRow(tbody, 7, '불러오지 못했습니다.');
    showMsg($('#gold-msg'), e.message);
  }
}

$('#gold-refresh').addEventListener('click', () => { invalidate('gold'); renderGoldView(); });

// =====================================================================
// 환율 (상세)
// =====================================================================

let fx = null;

async function renderExchangeView() {
  loadFxChart();
  const tbody = $('#fx-rows');
  emptyRow(tbody, 5, '불러오는 중…');
  showMsg($('#fx-msg'), '');
  try {
    fx = await data.exchange();
    fx.rates.sort((a, b) => {
      const ia = FX_MAIN.indexOf(a.code), ib = FX_MAIN.indexOf(b.code);
      return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib) || a.code.localeCompare(b.code);
    });
    $('#fx-date').textContent = `한국수출입은행 · ${fx.date} 기준`;
    const prev = $('#fx-cur').value;
    $('#fx-cur').innerHTML = fx.rates.map((r) => `<option value="${esc(r.code)}">${esc(r.code)} ${esc(r.name)}</option>`).join('');
    if (prev && fx.rates.some((r) => r.code === prev)) $('#fx-cur').value = prev;
    drawFx();
    convert();
  } catch (e) {
    emptyRow(tbody, 5, '불러오지 못했습니다.');
    showMsg($('#fx-msg'), e.message);
  }
}

function drawFx() {
  if (!fx) return;
  const q = $('#fx-search').value.trim().toLowerCase();
  const rows = fx.rates.filter((r) => !q || r.code.toLowerCase().includes(q) || r.name.toLowerCase().includes(q));
  const tbody = $('#fx-rows');
  if (!rows.length) { emptyRow(tbody, 5, '검색 결과가 없습니다.'); return; }
  tbody.innerHTML = rows.map((r) => `
    <tr>
      <td><b>${esc(r.code)}</b>${r.unit !== 1 ? `<span class="code">${r.unit}단위</span>` : ''}</td>
      <td>${esc(r.name)}</td>
      <td class="num">${num(r.baseRate)}</td>
      <td class="num">${num(r.ttb)}</td>
      <td class="num">${num(r.tts)}</td>
    </tr>`).join('');
}

function convert() {
  if (!fx) return;
  const r = fx.rates.find((x) => x.code === $('#fx-cur').value);
  const amount = Number($('#fx-amount').value);
  $('#fx-result').textContent = r && r.baseRate != null && !Number.isNaN(amount)
    ? won(amount * r.baseRate / r.unit) : '-';
}

$('#fx-search').addEventListener('input', drawFx);
$('#fx-amount').addEventListener('input', convert);
$('#fx-cur').addEventListener('change', convert);
$('#fx-refresh').addEventListener('click', () => { invalidate('exchange', 'fxh:'); renderExchangeView(); });

// ---------------------------------------------------------------------
// 환율 그래프 (미국·중국·일본·유럽, 기간 시작 대비 변동률)
//  - 통화마다 원화 환율 크기가 달라서(달러 약 1,400원, 위안 약 190원) 변동률(%)로 맞춰 한 축에 그립니다.
//  - 색만으로 구별하지 않도록 선 끝에 나라 이름도 표시합니다.
// ---------------------------------------------------------------------

const FXC_RANGES = {
  '7d': (d) => d.setDate(d.getDate() - 7),
  '1m': (d) => d.setMonth(d.getMonth() - 1),
  '3m': (d) => d.setMonth(d.getMonth() - 3),
  '6m': (d) => d.setMonth(d.getMonth() - 6),
  '1y': (d) => d.setFullYear(d.getFullYear() - 1),
  '3y': (d) => d.setFullYear(d.getFullYear() - 3),
  '5y': (d) => d.setFullYear(d.getFullYear() - 5),
  '10y': (d) => d.setFullYear(d.getFullYear() - 10),
};

const isoDate = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

const fxc = {
  range: store.get('fxcRange', '3m'),
  data: null,
  hidden: new Set(store.get('fxcHidden', [])),
  hover: null,      // 강조 중인 통화코드
  geom: null,       // 마지막으로 그린 좌표 정보 (마우스 처리용)
};

data.fxHistory = (from, to) => cached(`fxh:${from}:${to}`,
  () => api(`/api/exchange/history?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`), 10 * 60_000);

function fxcPeriod() {
  if (fxc.range === 'custom') return { from: $('#fxc-from').value, to: $('#fxc-to').value };
  const d = new Date();
  (FXC_RANGES[fxc.range] || FXC_RANGES['3m'])(d);
  return { from: isoDate(d), to: isoDate(new Date()) };
}

function fxcSyncControls() {
  document.querySelectorAll('#fxc-ranges button').forEach((b) => {
    b.classList.toggle('active', b.dataset.range === fxc.range);
    b.setAttribute('aria-pressed', b.dataset.range === fxc.range);
  });
  $('#fxc-custom').hidden = fxc.range !== 'custom';
}

async function loadFxChart() {
  fxcSyncControls();
  const { from, to } = fxcPeriod();
  if (!from || !to) return;
  showMsg($('#fxc-msg'), '');
  $('#fxc-wrap').classList.add('loading');
  try {
    fxc.data = await data.fxHistory(from, to);
    if (fxc.data.dates.length < 2) {
      fxc.data = null;
      $('#fxc-svg').innerHTML = '';
      showMsg($('#fxc-msg'), '이 기간에는 영업일 데이터가 2일 미만이라 그래프를 그릴 수 없습니다. 기간을 넓혀 주세요.', 'info');
      return;
    }
    // 출처·기간은 화면에 늘 띄우지 않고 제목에 마우스를 올렸을 때만
    $('#fxc-title').title = `${fxc.data.start} ~ ${fxc.data.end} · ${fxc.data.source} · 영업일 하루 1번 갱신`;
    drawFxLegend();
    drawFxChart();
  } catch (e) {
    fxc.data = null;
    $('#fxc-svg').innerHTML = '';
    showMsg($('#fxc-msg'), e.message);
  } finally {
    $('#fxc-wrap').classList.remove('loading');
  }
}

/** 통화 i 의 색/선 모양 (색은 통화에 고정 — 숨겨도 다른 통화 색이 바뀌지 않음) */
function fxcStyle(i) {
  return { color: `var(--s${(i % 8) + 1})`, dash: i >= 8 ? '6 4' : '' };
}

/** 각 통화의 기간 시작 대비 변동률(%) */
function fxcSeries() {
  const { currencies, series } = fxc.data;
  return currencies.map((c, i) => {
    const raw = series[c.code] || [];
    const base = raw.find((v) => v != null);
    const pct = raw.map((v) => (v == null || !base ? null : (v / base - 1) * 100));
    let last = raw.length - 1;
    while (last >= 0 && raw[last] == null) last--;
    return { ...c, i, raw, base, pct, last, ...fxcStyle(i) };
  });
}

function drawFxLegend() {
  const list = fxcSeries();
  $('#fxc-legend').innerHTML = list.map((s) => `
    <button type="button" class="fxc-chip ${fxc.hidden.has(s.code) ? 'off' : ''}" data-code="${esc(s.code)}"
            aria-pressed="${!fxc.hidden.has(s.code)}" title="눌러서 숨기기/보이기">
      <svg width="18" height="8" aria-hidden="true"><line x1="1" y1="4" x2="17" y2="4" stroke="${s.color}" stroke-width="2.5"
        ${s.dash ? 'stroke-dasharray="4 3"' : ''} stroke-linecap="round"/></svg>
      ${esc(s.country)} <span class="muted">${esc(s.code)}</span>
    </button>`).join('');
}

/** 보기 좋은 눈금 (1, 2, 5 × 10^n 간격) */
function niceTicks(min, max, count = 5) {
  const span = max - min || 1;
  const raw = span / count;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 5, 10].map((m) => m * mag).find((s) => s >= raw) || 10 * mag;
  const ticks = [];
  for (let v = Math.ceil(min / step) * step; v <= max + step * 1e-9; v += step) ticks.push(+v.toFixed(10));
  return { ticks, step };
}

function drawFxChart() {
  if (!fxc.data) return;
  const svg = $('#fxc-svg');
  const W = $('#fxc-wrap').clientWidth || 800;
  const narrow = W < 560;
  const H = narrow ? 300 : 380;
  const M = { l: 52, r: narrow ? 76 : 104, t: 14, b: 30 };
  const pw = W - M.l - M.r, ph = H - M.t - M.b;
  const { dates } = fxc.data;
  const n = dates.length;

  const list = fxcSeries();
  const shown = list.filter((s) => !fxc.hidden.has(s.code));

  let min = 0, max = 0;
  shown.forEach((s) => s.pct.forEach((v) => { if (v != null) { min = Math.min(min, v); max = Math.max(max, v); } }));
  const pad = (max - min) * 0.08 || 1;
  const { ticks } = niceTicks(min - pad, max + pad);
  const y0 = Math.min(ticks[0], min - pad), y1 = Math.max(ticks[ticks.length - 1], max + pad);

  const x = (i) => M.l + (n === 1 ? pw / 2 : (i / (n - 1)) * pw);
  const y = (v) => M.t + (1 - (v - y0) / (y1 - y0)) * ph;
  const fmtPct = (v) => (v > 0 ? '+' : '') + nf2.format(v) + '%';

  // 세로 눈금 + 격자
  const grid = ticks.map((t) => `
    <line class="fxc-grid ${t === 0 ? 'zero' : ''}" x1="${M.l}" x2="${M.l + pw}" y1="${y(t)}" y2="${y(t)}"/>
    <text class="fxc-axis" x="${M.l - 8}" y="${y(t)}" text-anchor="end" dominant-baseline="middle">${t > 0 ? '+' : ''}${nf2.format(t)}%</text>`).join('');

  // 가로 날짜 눈금 (5~6개)
  const long = (new Date(dates[n - 1]) - new Date(dates[0])) / 864e5 > 400;
  const fmtX = (d) => (long ? `${d.slice(2, 4)}.${d.slice(5, 7)}` : `${d.slice(5, 7)}.${d.slice(8, 10)}`);
  const xCount = narrow ? 4 : 6;
  const xIdx = [...new Set(Array.from({ length: xCount }, (_, k) => Math.round((k / (xCount - 1)) * (n - 1))))];
  const xAxis = xIdx.map((i, k) => `
    <text class="fxc-axis" x="${x(i)}" y="${H - 8}" text-anchor="${k === 0 ? 'start' : k === xIdx.length - 1 ? 'end' : 'middle'}">${fmtX(dates[i])}</text>`).join('');

  // 선
  const lines = shown.map((s) => {
    let d = '', pen = false;
    s.pct.forEach((v, i) => {
      if (v == null) { pen = false; return; }
      d += `${pen ? 'L' : 'M'}${x(i).toFixed(1)},${y(v).toFixed(1)}`;
      pen = true;
    });
    return `<path class="fxc-line" data-code="${esc(s.code)}" d="${d}" stroke="${s.color}" ${s.dash ? `stroke-dasharray="${s.dash}"` : ''}/>`;
  }).join('');

  // 선 끝 이름표 (겹치지 않게 위아래로 밀어냄)
  const GAP = 13;
  const labels = shown.filter((s) => s.last >= 0)
    .map((s) => ({ s, want: y(s.pct[s.last]) }))
    .sort((a, b) => a.want - b.want);
  labels.forEach((l, k) => { l.at = Math.max(l.want, k ? labels[k - 1].at + GAP : M.t); });
  for (let k = labels.length - 1; k >= 0; k--) {
    const limit = k === labels.length - 1 ? M.t + ph : labels[k + 1].at - GAP;
    labels[k].at = Math.min(labels[k].at, limit);
  }
  const ends = labels.map(({ s, want, at }) => `
    <g class="fxc-end" data-code="${esc(s.code)}">
      <line x1="${x(s.last) + 3}" y1="${want}" x2="${M.l + pw + 8}" y2="${at}" stroke="${s.color}" stroke-width="1" opacity=".5"/>
      <circle cx="${x(s.last)}" cy="${want}" r="3" fill="${s.color}"/>
      <text class="fxc-endtxt" x="${M.l + pw + 11}" y="${at}" dominant-baseline="middle">${esc(s.country)} <tspan class="fxc-endval">${fmtPct(s.pct[s.last])}</tspan></text>
    </g>`).join('');

  svg.setAttribute('viewBox', `0 0 ${W} ${H}`);
  svg.setAttribute('width', W);
  svg.setAttribute('height', H);
  svg.innerHTML = `
    <g>${grid}</g>${xAxis}
    <g class="fxc-lines">${lines}</g>
    <g>${ends}</g>
    <line id="fxc-cross" class="fxc-cross" y1="${M.t}" y2="${M.t + ph}" visibility="hidden"/>
    <circle id="fxc-dot" r="5" visibility="hidden"/>
    <rect id="fxc-hit" x="${M.l}" y="${M.t}" width="${pw}" height="${ph}" fill="transparent"/>`;

  fxc.geom = { M, pw, ph, n, x, y, shown, fmtPct };
  fxcApplyHover();
}

/** 강조 중인 선은 두껍게, 나머지는 흐리게 */
function fxcApplyHover() {
  const code = fxc.hover;
  document.querySelectorAll('#fxc-svg .fxc-line, #fxc-svg .fxc-end').forEach((el) => {
    el.classList.toggle('on', el.dataset.code === code);
    el.classList.toggle('dim', code != null && el.dataset.code !== code);
  });
  document.querySelectorAll('#fxc-legend .fxc-chip').forEach((el) => el.classList.toggle('hl', el.dataset.code === code));
  // 강조한 선을 맨 위로
  if (code) {
    const el = document.querySelector(`#fxc-svg .fxc-line[data-code="${CSS.escape(code)}"]`);
    if (el) el.parentNode.appendChild(el);
  }
}

function fxcHideTip() {
  fxc.hover = null;
  $('#fxc-tip').hidden = true;
  $('#fxc-cross')?.setAttribute('visibility', 'hidden');
  $('#fxc-dot')?.setAttribute('visibility', 'hidden');
  fxcApplyHover();
}

function fxcOnMove(ev) {
  const g = fxc.geom;
  if (!g || !g.shown.length) return;
  const svg = $('#fxc-svg');
  const r = svg.getBoundingClientRect();
  const mx = (ev.clientX - r.left) * (svg.viewBox.baseVal.width / r.width);
  const my = (ev.clientY - r.top) * (svg.viewBox.baseVal.height / r.height);
  const i = Math.max(0, Math.min(g.n - 1, Math.round(((mx - g.M.l) / g.pw) * (g.n - 1))));

  // 마우스와 세로로 가장 가까운 선
  let best = null;
  g.shown.forEach((s) => {
    const v = s.pct[i];
    if (v == null) return;
    const dist = Math.abs(g.y(v) - my);
    if (!best || dist < best.dist) best = { s, v, dist };
  });
  if (!best) return;
  const { s, v } = best;

  fxc.hover = s.code;
  fxcApplyHover();
  const cx = g.x(i), cy = g.y(v);
  const cross = $('#fxc-cross');
  cross.setAttribute('x1', cx); cross.setAttribute('x2', cx); cross.setAttribute('visibility', 'visible');
  const dot = $('#fxc-dot');
  dot.setAttribute('cx', cx); dot.setAttribute('cy', cy); dot.setAttribute('fill', s.color); dot.setAttribute('visibility', 'visible');

  const valid = s.raw.filter((x) => x != null);
  const hi = Math.max(...valid), lo = Math.min(...valid);
  const rate = s.raw[i];
  const diff = rate - s.base;
  const unit = s.unit === 1 ? `1${s.name}` : `${s.unit}${s.name}`;
  const tip = $('#fxc-tip');
  tip.innerHTML = `
    <div class="fxc-tip-h"><svg width="18" height="8" aria-hidden="true"><line x1="1" y1="4" x2="17" y2="4" stroke="${s.color}" stroke-width="3" ${s.dash ? 'stroke-dasharray="4 3"' : ''}/></svg>
      <b>${esc(s.country)} ${esc(s.name)}</b> <span class="muted">${esc(s.code)}</span></div>
    <div class="muted small">${esc(fxc.data.dates[i])}</div>
    <div class="fxc-tip-big">${nf2.format(rate)}원 <span class="muted small">/ ${esc(unit)}</span></div>
    <table>
      <tr><td>기간 시작 대비</td><td class="${signCls(diff)}">${signed(diff, nf2)}원 (${g.fmtPct(v)})</td></tr>
      <tr><td>기간 최고</td><td>${nf2.format(hi)}원</td></tr>
      <tr><td>기간 최저</td><td>${nf2.format(lo)}원</td></tr>
    </table>`;
  tip.hidden = false;

  // 말풍선은 커서 오른쪽, 넘치면 왼쪽
  const wrap = $('#fxc-wrap').getBoundingClientRect();
  const px = cx * (r.width / svg.viewBox.baseVal.width) + (r.left - wrap.left);
  const py = cy * (r.height / svg.viewBox.baseVal.height) + (r.top - wrap.top);
  const tw = tip.offsetWidth, th = tip.offsetHeight;
  let left = px + 14;
  if (left + tw > wrap.width) left = px - tw - 14;
  tip.style.left = `${Math.max(0, left)}px`;
  tip.style.top = `${Math.max(0, Math.min(wrap.height - th, py - th / 2))}px`;
}

$('#fxc-wrap').addEventListener('pointermove', (e) => { if (e.target.closest('#fxc-svg')) fxcOnMove(e); });
$('#fxc-wrap').addEventListener('pointerleave', fxcHideTip);

$('#fxc-legend').addEventListener('click', (e) => {
  const chip = e.target.closest('.fxc-chip');
  if (!chip) return;
  const code = chip.dataset.code;
  if (fxc.hidden.has(code)) fxc.hidden.delete(code); else fxc.hidden.add(code);
  if (fxc.hidden.size === fxc.data?.currencies.length) fxc.hidden.delete(code); // 하나는 남겨둠
  store.set('fxcHidden', [...fxc.hidden]);
  drawFxLegend();
  drawFxChart();
});
$('#fxc-legend').addEventListener('pointerover', (e) => {
  const chip = e.target.closest('.fxc-chip');
  if (chip && !fxc.hidden.has(chip.dataset.code)) { fxc.hover = chip.dataset.code; fxcApplyHover(); }
});
$('#fxc-legend').addEventListener('pointerleave', () => { fxc.hover = null; fxcApplyHover(); });

$('#fxc-ranges').addEventListener('click', (e) => {
  const b = e.target.closest('button[data-range]');
  if (!b) return;
  fxc.range = b.dataset.range;
  store.set('fxcRange', fxc.range);
  if (fxc.range === 'custom') {
    // 직접 설정을 처음 열면 지금 보고 있는 기간으로 채워둠
    if (!$('#fxc-from').value && fxc.data) { $('#fxc-from').value = fxc.data.start; $('#fxc-to').value = fxc.data.end; }
    fxcSyncControls();
    return;
  }
  loadFxChart();
});

$('#fxc-custom').addEventListener('submit', (e) => {
  e.preventDefault();
  store.set('fxcFrom', $('#fxc-from').value);
  store.set('fxcTo', $('#fxc-to').value);
  loadFxChart();
});

(function initFxChart() {
  const today = isoDate(new Date());
  const min = new Date(); min.setFullYear(min.getFullYear() - 10);
  ['#fxc-from', '#fxc-to'].forEach((s) => { $(s).max = today; $(s).min = isoDate(min); });
  $('#fxc-from').value = store.get('fxcFrom', '');
  $('#fxc-to').value = store.get('fxcTo', '') || today;
})();

let fxcResizeTimer = null;
window.addEventListener('resize', () => {
  clearTimeout(fxcResizeTimer);
  fxcResizeTimer = setTimeout(() => { if (!$('#view-exchange').hidden) drawFxChart(); }, 150);
});


// =====================================================================
// 부동산 (상세)
// =====================================================================

const REGIONS = [
  ['11110', '서울 종로구'], ['11140', '서울 중구'], ['11170', '서울 용산구'], ['11200', '서울 성동구'],
  ['11215', '서울 광진구'], ['11230', '서울 동대문구'], ['11260', '서울 중랑구'], ['11290', '서울 성북구'],
  ['11305', '서울 강북구'], ['11320', '서울 도봉구'], ['11350', '서울 노원구'], ['11380', '서울 은평구'],
  ['11410', '서울 서대문구'], ['11440', '서울 마포구'], ['11470', '서울 양천구'], ['11500', '서울 강서구'],
  ['11530', '서울 구로구'], ['11545', '서울 금천구'], ['11560', '서울 영등포구'], ['11590', '서울 동작구'],
  ['11620', '서울 관악구'], ['11650', '서울 서초구'], ['11680', '서울 강남구'], ['11710', '서울 송파구'],
  ['11740', '서울 강동구'],
  ['41135', '성남 분당구'], ['41465', '용인 수지구'], ['41117', '수원 영통구'], ['41210', '광명시'],
  ['41450', '하남시'], ['41290', '과천시'],
];

/** 신고가 어느 정도 채워진 지난달 (yyyy-MM) */
function defaultMonth() {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function aptStats(trades) {
  const valid = trades.filter((t) => !t.canceled && t.dealAmount != null);
  if (!valid.length) return null;
  const amounts = valid.map((t) => t.dealAmount).sort((a, b) => a - b);
  const perPyeong = valid.filter((t) => t.area).map((t) => t.dealAmount / (t.area / 3.3058));
  return {
    count: valid.length,
    canceled: trades.length - valid.length,
    median: amounts[Math.floor(amounts.length / 2)],
    max: amounts[amounts.length - 1],
    avgPy: perPyeong.length ? perPyeong.reduce((s, v) => s + v, 0) / perPyeong.length : null,
  };
}

function initRealEstate() {
  $('#re-region').innerHTML = REGIONS.map(([c, n]) => `<option value="${c}">${esc(n)}</option>`).join('')
    + '<option value="custom">직접 입력…</option>';
  $('#re-region').value = store.get('reRegion', '11680');
  if (!$('#re-region').value) $('#re-region').value = '11680';
  $('#re-code').value = store.get('reCode', '');
  $('#re-month').value = store.get('reMonth', null) || defaultMonth();
  $('#re-code').hidden = $('#re-region').value !== 'custom';
}

$('#re-region').addEventListener('change', () => {
  $('#re-code').hidden = $('#re-region').value !== 'custom';
});

let reData = null;

function renderRealEstateView() {
  // 대시보드에서 눌러 들어오면 저장된 지역으로 바로 조회
  if (reData == null) searchRealEstate();
}

async function searchRealEstate() {
  const region = $('#re-region').value;
  const code = region === 'custom' ? $('#re-code').value.trim() : region;
  const month = $('#re-month').value;
  store.set('reRegion', region);
  store.set('reCode', $('#re-code').value.trim());
  store.set('reMonth', month);
  const tbody = $('#re-rows');
  emptyRow(tbody, 7, '불러오는 중…');
  showMsg($('#re-msg'), '');
  $('#re-summary').innerHTML = '';
  try {
    reData = await data.apt(code, month.replace('-', ''));
    drawRealEstate();
  } catch (err) {
    reData = [];
    emptyRow(tbody, 7, '불러오지 못했습니다.');
    showMsg($('#re-msg'), err.message);
  }
}

$('#re-form').addEventListener('submit', (e) => { e.preventDefault(); searchRealEstate(); });
$('#re-filter').addEventListener('input', drawRealEstate);

function drawRealEstate() {
  if (!reData) return;
  const q = $('#re-filter').value.trim();
  const rows = reData.filter((t) => !q || (t.aptName || '').includes(q));
  const tbody = $('#re-rows');
  const s = aptStats(rows);

  $('#re-summary').innerHTML = s ? `
    <div class="card"><div class="label">거래 건수</div><div class="value">${nf0.format(s.count)}건</div>
      ${s.canceled ? `<div class="sub muted">취소 ${s.canceled}건 제외</div>` : ''}</div>
    <div class="card"><div class="label">중간 거래가</div><div class="value">${manwon(s.median)}</div></div>
    <div class="card"><div class="label">최고 거래가</div><div class="value">${manwon(s.max)}</div></div>
    <div class="card"><div class="label">평균 평당가</div><div class="value">${s.avgPy == null ? '-' : manwon(Math.round(s.avgPy))}</div></div>` : '';

  if (!rows.length) {
    emptyRow(tbody, 7, reData.length ? '필터에 맞는 거래가 없습니다.' : '해당 월의 거래가 없습니다.');
    return;
  }
  tbody.innerHTML = rows.map((t) => `
    <tr class="${t.canceled ? 'canceled' : ''}">
      <td>${esc(t.dealDate)}</td>
      <td>${esc(t.aptName)}</td>
      <td>${esc(t.dong)}</td>
      <td class="num">${num(t.area)}</td>
      <td class="num">${num(t.floor, nf0)}</td>
      <td class="num"><b>${manwon(t.dealAmount)}</b></td>
      <td class="num">${t.buildYear ?? '-'}</td>
    </tr>`).join('');
}

// =====================================================================
// 청약 (청약홈 + LH 공고)
// =====================================================================

const SUB_SOURCE = { APPLYHOME: '청약홈', LH: 'LH' };

/** 상태 문자열을 open / soon / closed 세 묶음으로 (LH 는 상태 이름이 여러 가지라서) */
function subGroup(status) {
  const s = status || '';
  if (s.includes('접수중') || s === '상담요청') return 'open';
  if (s.includes('예정') || s.includes('공고중')) return 'soon';
  if (s.includes('마감') || s.includes('완료') || s.includes('발표')) return 'closed';
  return 'soon';
}

function subBadge(status) {
  return `<span class="st st-${subGroup(status)}">${esc(status || '-')}</span>`;
}

const fmtMd = (d) => (d ? esc(d.slice(5).replace('-', '.')) : '');

function subPeriod(n) {
  if (n.receiptStart && n.receiptEnd) return `${fmtMd(n.receiptStart)} ~ ${fmtMd(n.receiptEnd)}`;
  if (n.receiptEnd) return `~ ${fmtMd(n.receiptEnd)}`;
  return '-';
}

/** 출처 하나만 실패했을 때 보여줄 안내 */
function subErrorText(errors) {
  return Object.entries(errors || {})
    .map(([src, msg]) => `${SUB_SOURCE[src] || src}: ${msg}`)
    .join('\n');
}

let subData = null;

function renderSubscriptionView() {
  loadSubscriptions();
}

async function loadSubscriptions() {
  const tbody = $('#sub-rows');
  if (!subData) emptyRow(tbody, 9, '불러오는 중…');
  showMsg($('#sub-msg'), '');
  try {
    subData = await data.subscriptions();
    const regions = [...new Set(subData.items.map((n) => n.region).filter(Boolean))].sort();
    const sel = $('#sub-region');
    const keep = sel.value;
    sel.innerHTML = '<option value="">지역 전체</option>'
      + regions.map((r) => `<option value="${esc(r)}">${esc(r)}</option>`).join('');
    sel.value = regions.includes(keep) ? keep : '';
    showMsg($('#sub-msg'), subErrorText(subData.errors));
    drawSubscriptions();
  } catch (err) {
    subData = null;
    $('#sub-summary').innerHTML = '';
    emptyRow(tbody, 9, '불러오지 못했습니다.');
    showMsg($('#sub-msg'), err.message);
  }
}

function drawSubscriptions() {
  if (!subData) return;
  const source = $('#sub-source').value;
  const group = $('#sub-status').value;
  const region = $('#sub-region').value;
  const q = $('#sub-filter').value.trim().replace(/\s/g, '');
  const rows = subData.items.filter((n) => (!source || n.source === source)
    && (!group || subGroup(n.status) === group)
    && (!region || n.region === region)
    && (!q || (n.name || '').replace(/\s/g, '').includes(q)));

  const count = (g) => rows.filter((n) => subGroup(n.status) === g).length;
  $('#sub-summary').innerHTML = `
    <div class="card"><div class="label">공고</div><div class="value">${nf0.format(rows.length)}건</div></div>
    <div class="card"><div class="label">접수중</div><div class="value">${nf0.format(count('open'))}건</div></div>
    <div class="card"><div class="label">접수예정 · 공고중</div><div class="value">${nf0.format(count('soon'))}건</div></div>
    <div class="card"><div class="label">마감</div><div class="value">${nf0.format(count('closed'))}건</div></div>`;

  const tbody = $('#sub-rows');
  if (!rows.length) {
    emptyRow(tbody, 9, subData.items.length ? '조건에 맞는 공고가 없습니다.' : '최근 60일 공고가 없습니다.');
    return;
  }
  tbody.innerHTML = rows.map((n) => {
    const safeUrl = /^https?:\/\//.test(n.url || '') ? n.url : null;
    const title = safeUrl
      ? `<a class="lnk sub-open" href="${esc(safeUrl)}" target="_blank" rel="noopener noreferrer">${esc(n.name)}</a>`
      : esc(n.name);
    return `
    <tr class="${subGroup(n.status) === 'closed' ? 'dim' : ''}">
      <td>${subBadge(n.status)}</td>
      <td>${esc(SUB_SOURCE[n.source] || n.source)}</td>
      <td>${esc(n.category || '-')}</td>
      <td>${title}${n.address ? `<div class="muted small">${esc(n.address)}</div>` : ''}</td>
      <td>${esc(n.region || '-')}</td>
      <td class="num">${n.households == null ? '-' : nf0.format(n.households)}</td>
      <td>${esc(n.noticeDate || '-')}</td>
      <td>${subPeriod(n)}</td>
      <td>${n.winnerDate ? fmtMd(n.winnerDate) : '-'}</td>
    </tr>`;
  }).join('');
}

// 공고명 클릭 → 새 탭 대신 내 페이지 안에서 원문 보기
// (Ctrl/Cmd/가운데 클릭은 원래대로 새 탭)
$('#sub-rows').addEventListener('click', (e) => {
  const a = e.target.closest('a.sub-open');
  if (!a || e.ctrlKey || e.metaKey || e.shiftKey || e.button !== 0) return;
  e.preventDefault();
  openSubViewer(a.href, a.textContent);
});

function openSubViewer(url, title) {
  $('#sub-viewer-title').textContent = title;
  $('#sub-viewer-open').href = url;
  $('#sub-viewer-frame').src = url;
  $('#sub-viewer').showModal();
}

function closeSubViewer() {
  $('#sub-viewer').close();
}

$('#sub-viewer-close').addEventListener('click', closeSubViewer);
$('#sub-viewer-open').addEventListener('click', closeSubViewer);
// 바깥(어두운 배경) 클릭으로 닫기
$('#sub-viewer').addEventListener('click', (e) => { if (e.target === e.currentTarget) closeSubViewer(); });
// 닫을 때 페이지를 비워서 뒤에서 계속 돌지 않게
$('#sub-viewer').addEventListener('close', () => { $('#sub-viewer-frame').src = 'about:blank'; });

$('#sub-form').addEventListener('submit', (e) => e.preventDefault());
['#sub-source', '#sub-status', '#sub-region'].forEach((s) => $(s).addEventListener('change', drawSubscriptions));
$('#sub-filter').addEventListener('input', drawSubscriptions);
$('#sub-refresh').addEventListener('click', () => { invalidate('subscriptions'); loadSubscriptions(); });

async function dashSubscription() {
  const body = $('#d-subscription .dcard-body');
  body.innerHTML = skeleton('sm', 'lg', 'md');
  try {
    const res = await data.subscriptions();
    const open = res.items.filter((n) => subGroup(n.status) === 'open');
    const soon = res.items.filter((n) => subGroup(n.status) === 'soon');
    if (!res.items.length && Object.keys(res.errors || {}).length) {
      body.innerHTML = `<div class="derr"><b>불러오지 못했습니다</b>${esc(subErrorText(res.errors))}</div>`;
      return;
    }
    const next = open.concat(soon).slice(0, 3);
    body.innerHTML = `
      <div class="label muted small">청약홈 · LH 최근 60일</div>
      <div class="big">접수중 ${nf0.format(open.length)}건</div>
      <div class="muted small">접수예정 · 공고중 ${nf0.format(soon.length)}건</div>
      ${next.length ? `<div class="dnote">${next.map((n) => esc(n.name)).join(' · ')}</div>` : ''}`;
  } catch (e) {
    body.innerHTML = cardError(e);
  }
}

// =====================================================================
// 모의투자
// =====================================================================

let invAssets = [];
let invAccountNo = null;

function renderInvestView() {
  if (!invAssets.length) loadInvAssets();
}

async function loadInvAssets() {
  try {
    invAssets = await api('/api/invest/assets');
    const sel = $('#inv-asset');
    sel.innerHTML = '<option value="">자산 선택</option>' +
      invAssets.map((a) => `<option value="${a.assetNo}" data-price="${a.currentPrice ?? ''}">${esc(a.assetNm)} (${esc(a.assetCd || a.assetTypeCd)})</option>`).join('');
  } catch (e) { /* 자산 로드 실패 무시 */ }
}

$('#inv-load-btn').addEventListener('click', loadInvAccounts);

async function loadInvAccounts() {
  const memberNo = $('#inv-member-no').value;
  try {
    const list = await api(`/api/invest/accounts?memberNo=${memberNo}`);
    const sel = $('#inv-account-sel');
    sel.innerHTML = '<option value="">계좌 선택</option>' +
      list.map((a) => `<option value="${a.accountNo}">${esc(a.accountNm)} (${won(a.depositAmt)})</option>`).join('');
    if (list.length) {
      sel.value = list[0].accountNo;
      invAccountNo = list[0].accountNo;
      refreshInvAccount();
      $('#inv-tabs-wrap').hidden = false;
    }
  } catch (e) { alert('계좌 불러오기 실패: ' + e.message); }
}

$('#inv-account-sel').addEventListener('change', () => {
  invAccountNo = $('#inv-account-sel').value || null;
  if (invAccountNo) { refreshInvAccount(); $('#inv-tabs-wrap').hidden = false; }
  else { $('#inv-account-info').hidden = true; $('#inv-tabs-wrap').hidden = true; }
});

async function refreshInvAccount() {
  if (!invAccountNo) return;
  try {
    const a = await api(`/api/invest/accounts/${invAccountNo}`);
    const diff = Number(a.depositAmt) - Number(a.initAmt);
    $('#inv-account-info').innerHTML = `
      <div class="card"><div class="label">예수금</div><div class="value">${won(a.depositAmt)}</div></div>
      <div class="card"><div class="label">초기금액</div><div class="value muted">${won(a.initAmt)}</div></div>
      <div class="card"><div class="label">손익</div><div class="value ${diff >= 0 ? 'up' : 'down'}">${signed(diff)}원</div></div>
      <div class="card"><div class="label">상태</div><div class="value">${esc(a.accountStatCd)}</div></div>`;
    $('#inv-account-info').hidden = false;
  } catch (e) { /* 무시 */ }
}

// 서브 탭
document.querySelectorAll('.inv-tab').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.inv-tab').forEach((b) => b.classList.remove('active'));
    document.querySelectorAll('.inv-panel').forEach((p) => { p.hidden = true; });
    btn.classList.add('active');
    const panel = btn.dataset.panel;
    $(`#inv-panel-${panel}`).hidden = false;
    if (panel === 'holdings') loadInvHoldings();
    else if (panel === 'orders') loadInvOrders();
    else if (panel === 'ledger') loadInvLedger();
  });
});

// 자산 선택 → 현재가/예상금액 갱신
function updateInvEstimate() {
  const opt = $('#inv-asset').options[$('#inv-asset').selectedIndex];
  const price = opt ? Number(opt.dataset.price) || 0 : 0;
  const qty = Number($('#inv-qty').value) || 0;
  const side = document.querySelector('[name="inv-side"]:checked')?.value;
  $('#inv-cur-price').textContent = price ? won(price) : '-';
  if (price && qty) {
    const amt = Math.floor(price * qty);
    const fee = Math.ceil(amt * 0.0005);
    const tax = side === 'SELL' ? Math.ceil(amt * 0.002) : 0;
    const net = side === 'BUY' ? amt + fee : amt - fee - tax;
    $('#inv-est-amt').textContent = `${won(net)} (수수료 ${won(fee)}${tax ? ' · 세금 ' + won(tax) : ''})`;
  } else {
    $('#inv-est-amt').textContent = '-';
  }
}
$('#inv-asset').addEventListener('change', updateInvEstimate);
$('#inv-qty').addEventListener('input', updateInvEstimate);
document.querySelectorAll('[name="inv-side"]').forEach((r) => r.addEventListener('change', updateInvEstimate));

// 주문 실행
$('#inv-order-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!invAccountNo) return;
  const assetNo = Number($('#inv-asset').value);
  const side = document.querySelector('[name="inv-side"]:checked')?.value;
  const qty = Number($('#inv-qty').value);
  if (!assetNo || !side || !qty) return;
  showMsg($('#inv-order-msg'), '');
  try {
    const o = await api(`/api/invest/accounts/${invAccountNo}/orders`, {
      method: 'POST',
      body: JSON.stringify({ assetNo, side, qty }),
    });
    showMsg($('#inv-order-msg'),
      `✓ ${o.orderSideCd === 'BUY' ? '매수' : '매도'} 주문 완료 (주문번호: ${o.orderNo}, ${o.orderStatCd})`, 'info');
    $('#inv-qty').value = '';
    updateInvEstimate();
    refreshInvAccount();
  } catch (e) { showMsg($('#inv-order-msg'), e.message); }
});

// 보유잔고
async function loadInvHoldings() {
  const tbody = $('#inv-holding-rows');
  emptyRow(tbody, 7, '불러오는 중…');
  $('#inv-pnl-summary').innerHTML = '';
  try {
    const list = await api(`/api/invest/accounts/${invAccountNo}/holdings`);
    if (!list.length) { emptyRow(tbody, 7, '보유 종목이 없습니다.'); return; }

    // 합계 계산
    let totalEval = null, totalPnl = null, totalBuy = BigDecimal_ZERO;
    list.forEach((h) => {
      if (h.evalAmt != null) { totalEval = (totalEval ?? 0) + Number(h.evalAmt); }
      if (h.evalPnl != null) { totalPnl  = (totalPnl  ?? 0) + Number(h.evalPnl); }
      totalBuy += Number(h.totBuyAmt ?? 0);
    });
    const totalRate = (totalPnl != null && totalBuy) ? (totalPnl / totalBuy * 100) : null;

    $('#inv-pnl-summary').innerHTML = `
      <div class="card"><div class="label">총 매입금액</div><div class="value">${won(totalBuy)}</div></div>
      ${totalEval != null ? `<div class="card"><div class="label">총 평가금액</div><div class="value">${won(totalEval)}</div></div>` : ''}
      ${totalPnl  != null ? `<div class="card"><div class="label">총 평가손익</div><div class="value ${totalPnl >= 0 ? 'up' : 'down'}">${signed(totalPnl)}원</div></div>` : ''}
      ${totalRate != null ? `<div class="card"><div class="label">총 수익률</div><div class="value ${totalRate >= 0 ? 'up' : 'down'}">${pct(totalRate)}</div></div>` : ''}`;

    tbody.innerHTML = list.map((h) => `
      <tr>
        <td>${esc(h.assetNm ?? invAssetName(h.assetNo))}</td>
        <td class="num">${num(h.holdQty, nf4)}</td>
        <td class="num">${won(h.avgBuyPrc)}</td>
        <td class="num">${h.currentPrice != null ? won(h.currentPrice) : '-'}</td>
        <td class="num">${h.evalAmt != null ? won(h.evalAmt) : '-'}</td>
        <td class="num ${h.evalPnl != null ? (Number(h.evalPnl) >= 0 ? 'up' : 'down') : ''}">${h.evalPnl != null ? signed(h.evalPnl) + '원' : '-'}</td>
        <td class="num ${h.returnRate != null ? (Number(h.returnRate) >= 0 ? 'up' : 'down') : ''}">${h.returnRate != null ? pct(h.returnRate) : '-'}</td>
      </tr>`).join('');
  } catch (e) { emptyRow(tbody, 7, '불러오지 못했습니다.'); }
}

const BigDecimal_ZERO = 0;

// 주문내역
async function loadInvOrders() {
  const tbody = $('#inv-order-rows');
  emptyRow(tbody, 7, '불러오는 중…');
  try {
    const list = await api(`/api/invest/accounts/${invAccountNo}/orders`);
    if (!list.length) { emptyRow(tbody, 7, '주문 내역이 없습니다.'); return; }
    tbody.innerHTML = list.map((o) => `
      <tr>
        <td class="num muted">${o.orderNo}</td>
        <td>${invAssetName(o.assetNo)}</td>
        <td class="${o.orderSideCd === 'BUY' ? 'up' : 'down'}">${o.orderSideCd === 'BUY' ? '매수' : '매도'}</td>
        <td class="num">${num(o.orderQty, nf4)}</td>
        <td class="num">${won(o.orderPrc)}</td>
        <td>${esc(o.orderStatCd)}</td>
        <td class="muted small">${invFmtDt(o.orderDt)}</td>
      </tr>`).join('');
  } catch (e) { emptyRow(tbody, 7, '불러오지 못했습니다.'); }
}

// 거래원장
async function loadInvLedger() {
  const tbody = $('#inv-ledger-rows');
  emptyRow(tbody, 5, '불러오는 중…');
  try {
    const list = await api(`/api/invest/accounts/${invAccountNo}/ledger`);
    if (!list.length) { emptyRow(tbody, 5, '거래 내역이 없습니다.'); return; }
    tbody.innerHTML = list.map((l) => `
      <tr>
        <td>${esc(l.txnTypeCd)}</td>
        <td class="num ${Number(l.txnAmt) >= 0 ? 'up' : 'down'}">${signed(l.txnAmt)}원</td>
        <td class="num">${won(l.balanceAfter)}</td>
        <td class="muted small">${esc(l.remark || '')}</td>
        <td class="muted small">${invFmtDt(l.regDt)}</td>
      </tr>`).join('');
  } catch (e) { emptyRow(tbody, 5, '불러오지 못했습니다.'); }
}

function invAssetName(assetNo) {
  const a = invAssets.find((x) => x.assetNo === assetNo);
  return a ? esc(a.assetNm) : `#${assetNo}`;
}

function invFmtDt(dt) {
  return dt ? String(dt).replace('T', ' ').substring(0, 16) : '-';
}

// 계좌 개설 다이얼로그
$('#inv-new-account-btn').addEventListener('click', () => {
  $('#inv-acct-member').value = $('#inv-member-no').value || 1;
  $('#inv-acct-err').hidden = true;
  $('#inv-acct-dialog').showModal();
});
$('#inv-acct-cancel').addEventListener('click', () => $('#inv-acct-dialog').close());
$('#inv-acct-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const memberNo = Number($('#inv-acct-member').value);
  const accountNm = $('#inv-acct-name').value.trim();
  const initAmt = Number($('#inv-acct-amt').value);
  try {
    await api('/api/invest/accounts', {
      method: 'POST',
      body: JSON.stringify({ memberNo, accountNm, initAmt }),
    });
    $('#inv-acct-dialog').close();
    $('#inv-member-no').value = memberNo;
    loadInvAccounts();
  } catch (e) {
    const err = $('#inv-acct-err');
    err.textContent = e.message;
    err.hidden = false;
    e.preventDefault(); // dialog가 닫히지 않게
  }
});

// =====================================================================
// 시작
// =====================================================================

initRealEstate();
loadStatus();
route();
