'use strict';

// =====================================================================
// 테마 (라이트 / 다크 모드) — app.js 와 동일한 로직, localStorage 공유
// =====================================================================

(function initTheme() {
  const saved = localStorage.getItem('theme');
  const isDark = saved === 'dark';
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

    applyTheme(isDark);

    toggle.addEventListener('click', () => {
      const nowDark = document.documentElement.getAttribute('data-theme') === 'dark';
      applyTheme(!nowDark);
    });
  });
})();

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

const STAT_LABEL = { NORMAL: '정상', LOCKED: '잠금', DORMANT: '휴면', WITHDRAWN: '탈퇴' };
const STAT_CLS   = { NORMAL: 'on',  LOCKED: 'off',  DORMANT: 'warn', WITHDRAWN: '' };
const won = (v) => v == null ? '-' : new Intl.NumberFormat('ko-KR').format(v) + '원';
const signed = (v) => v == null ? '-' : (Number(v) >= 0 ? '+' : '') + new Intl.NumberFormat('ko-KR').format(v) + '원';

async function loadOverview() {
  const o = await api('/overview');
  $('#overview').innerHTML = `
    <div class="card"><div class="label">회원</div><div class="value">${num(o.memberCount)}명</div></div>
    <div class="card"><div class="label">모의투자 계좌</div><div class="value">${num(o.investAccountCount)}개</div></div>
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

async function loadMembers() {
  const stat = $('#member-stat-filter').value;
  const tbody = $('#member-rows');
  tbody.innerHTML = `<tr><td class="empty" colspan="8">불러오는 중…</td></tr>`;
  const list = await api('/members' + (stat ? `?stat=${stat}` : ''));
  if (!list.length) return emptyRow(tbody, 8, '회원이 없습니다.');
  tbody.innerHTML = list.map((m) => `
    <tr>
      <td class="muted">${m.memberNo}</td>
      <td><b>${esc(m.loginId)}</b></td>
      <td>${esc(m.memberNm || '-')}</td>
      <td><span class="key ${STAT_CLS[m.memberStatCd] || ''}">${STAT_LABEL[m.memberStatCd] || esc(m.memberStatCd)}</span></td>
      <td class="num ${m.loginFailCnt >= 3 ? 'down' : ''}">${m.loginFailCnt}</td>
      <td class="muted">${dt(m.lastLoginDt)}</td>
      <td class="muted">${dt(m.regDt)}</td>
      <td>
        <div class="row-actions">
          ${m.memberStatCd === 'LOCKED'
            ? `<button class="btn ghost icon" data-member-unlock="${m.memberNo}">잠금 해제</button>`
            : m.memberStatCd === 'NORMAL'
              ? `<button class="btn ghost icon" data-member-lock="${m.memberNo}">잠금</button>`
              : ''}
        </div>
      </td>
    </tr>`).join('');
}

async function loadInvestAccounts() {
  const tbody = $('#invest-account-rows');
  tbody.innerHTML = `<tr><td class="empty" colspan="8">불러오는 중…</td></tr>`;
  const list = await api('/invest-accounts');
  if (!list.length) return emptyRow(tbody, 8, '계좌가 없습니다.');
  tbody.innerHTML = list.map((a) => {
    const pnl = Number(a.depositAmt) - Number(a.initAmt);
    return `
    <tr>
      <td class="muted">${a.accountNo}</td>
      <td><b>${esc(a.loginId)}</b><span class="muted small"> #${a.memberNo}</span></td>
      <td>${esc(a.accountNm)}</td>
      <td class="num">${won(a.depositAmt)}</td>
      <td class="num muted">${won(a.initAmt)}</td>
      <td class="num ${pnl >= 0 ? 'up' : 'down'}">${signed(pnl)}</td>
      <td><span class="key ${a.accountStatCd === 'ACTIVE' ? 'on' : 'off'}">${esc(a.accountStatCd)}</span></td>
      <td class="muted">${dt(a.regDt)}</td>
    </tr>`;
  }).join('');
}

async function loadAll() {
  showError(null);
  const results = await Promise.allSettled([
    loadOverview(), loadMembers(), loadInvestAccounts(), loadHoldings(), loadWatchlist()
  ]);
  const failed = results.find((r) => r.status === 'rejected');
  if (failed) showError(failed.reason);
}

document.addEventListener('click', async (e) => {
  const holding    = e.target.closest('[data-del-holding]');
  const watch      = e.target.closest('[data-del-watch]');
  const lockBtn    = e.target.closest('[data-member-lock]');
  const unlockBtn  = e.target.closest('[data-member-unlock]');
  try {
    if (holding) {
      if (!confirm(`"${holding.dataset.name}" 자산을 삭제할까요?`)) return;
      await api(`/holdings/${holding.dataset.delHolding}`, { method: 'DELETE' });
      await loadAll();
    } else if (watch) {
      if (!confirm(`관심종목 ${watch.dataset.delWatch} 를 삭제할까요?`)) return;
      await api(`/watchlist/${encodeURIComponent(watch.dataset.delWatch)}`, { method: 'DELETE' });
      await loadAll();
    } else if (lockBtn) {
      if (!confirm('이 회원을 잠금 처리할까요?')) return;
      await api(`/members/${lockBtn.dataset.memberLock}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ statCd: 'LOCKED' }),
      });
      await loadMembers();
    } else if (unlockBtn) {
      if (!confirm('이 회원의 잠금을 해제할까요?')) return;
      await api(`/members/${unlockBtn.dataset.memberUnlock}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ statCd: 'NORMAL' }),
      });
      await loadMembers();
    }
  } catch (err) {
    showError(err);
  }
});

$('#reload').addEventListener('click', loadAll);
$('#member-reload').addEventListener('click', loadMembers);
$('#member-stat-filter').addEventListener('change', loadMembers);
$('#account-reload').addEventListener('click', loadInvestAccounts);
loadAll();
