'use strict';

// 관리자 화면. API 는 모두 /admin/api/** (로그인 세션 쿠키가 자동으로 같이 감)

const $ = (sel) => document.querySelector(sel);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const nf = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 6 });
const num = (v) => (v == null ? '-' : nf.format(v));
const dt = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-');

async function api(path, options = {}) {
  const res = await fetch('/admin/api' + path, options);
  if (res.status === 401) { // 세션 만료 → 로그인 화면으로
    location.href = '/admin/login';
    return new Promise(() => {});
  }
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch { /* 본문 없음 */ }
  if (!res.ok) throw new Error(data?.message || `요청 실패 (${res.status})`);
  return data;
}

function showError(e) {
  $('#msg').innerHTML = e ? `<div class="msg">${esc(e.message)}</div>` : '';
}

function emptyRow(tbody, cols, text) {
  tbody.innerHTML = `<tr><td class="empty" colspan="${cols}">${esc(text)}</td></tr>`;
}

const KEY_LABELS = {
  dataGoKr: '공공데이터포털', koreaexim: '수출입은행', kis: '한국투자증권', kisAccount: 'KIS 계좌',
};

async function loadOverview() {
  const o = await api('/overview');
  $('#overview').innerHTML = `
    <div class="card"><div class="label">보유 자산</div><div class="value">${num(o.holdingCount)}건</div></div>
    <div class="card"><div class="label">관심종목</div><div class="value">${num(o.watchCount)}<span class="muted small"> / ${o.watchMax}</span></div></div>`;
  const k = o.apiKeys;
  $('#keys').innerHTML = Object.entries(KEY_LABELS)
    .map(([key, label]) => `<span class="key ${k[key] ? 'on' : 'off'}">${label}</span>`).join('')
    + (k.kis ? `<span class="key">${k.kisMock ? '모의투자' : '실전투자'}</span>` : '');
}

async function loadHoldings() {
  const tbody = $('#holdings');
  const list = await api('/holdings');
  if (!list.length) return emptyRow(tbody, 9, '등록된 자산이 없습니다.');
  tbody.innerHTML = list.map((h) => `
    <tr>
      <td>${h.id}</td>
      <td><span class="type-tag">${esc(h.assetTypeLabel)}</span></td>
      <td>${esc(h.name)}${h.code ? `<span class="code">${esc(h.code)}</span>` : ''}</td>
      <td class="num">${num(h.quantity)}</td>
      <td class="num">${num(h.averagePrice)}</td>
      <td class="num">${num(h.manualPrice)}</td>
      <td>${esc(h.memo || '')}</td>
      <td class="muted">${dt(h.updatedAt)}</td>
      <td><div class="row-actions"><button class="btn ghost icon" data-del-holding="${h.id}" data-name="${esc(h.name)}">삭제</button></div></td>
    </tr>`).join('');
}

async function loadWatchlist() {
  const tbody = $('#watchlist');
  const list = await api('/watchlist');
  if (!list.length) return emptyRow(tbody, 4, '관심종목이 없습니다.');
  tbody.innerHTML = list.map((w) => `
    <tr>
      <td>${esc(w.code)}</td>
      <td>${esc(w.name || '-')}</td>
      <td class="muted">${dt(w.createdAt)}</td>
      <td><div class="row-actions"><button class="btn ghost icon" data-del-watch="${esc(w.code)}">삭제</button></div></td>
    </tr>`).join('');
}

async function loadAll() {
  showError(null);
  const results = await Promise.allSettled([loadOverview(), loadHoldings(), loadWatchlist()]);
  const failed = results.find((r) => r.status === 'rejected');
  if (failed) showError(failed.reason);
}

document.addEventListener('click', async (e) => {
  const holding = e.target.closest('[data-del-holding]');
  const watch = e.target.closest('[data-del-watch]');
  try {
    if (holding) {
      if (!confirm(`"${holding.dataset.name}" 자산을 삭제할까요?`)) return;
      await api(`/holdings/${holding.dataset.delHolding}`, { method: 'DELETE' });
      await loadAll();
    } else if (watch) {
      if (!confirm(`관심종목 ${watch.dataset.delWatch} 를 삭제할까요?`)) return;
      await api(`/watchlist/${encodeURIComponent(watch.dataset.delWatch)}`, { method: 'DELETE' });
      await loadAll();
    }
  } catch (err) {
    showError(err);
  }
});

$('#reload').addEventListener('click', loadAll);
loadAll();
