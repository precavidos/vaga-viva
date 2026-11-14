// Vaga Viva — helpers shared by the real pages. Nothing here holds data of its own.
// Built from docs/design/prototipos/vv.js (icons, tiles, status line, step marks, test grid) plus what the real
// pages need to talk to the server (CSRF token, error bodies, GitHub sign-in, the signed-in user).
const VV = (() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const esc = (text) => String(text ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const reducedMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, reducedMotion() ? Math.min(ms, 150) : ms));

  // One icon family, drawn for this product: 24 px grid, 2.2 px ink stroke, round joins, no fills.
  // Icons are ink; only an icon that stands for a situation takes that situation's colour (see `ic`).
  const svg = (d) => `<svg class="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${d}</svg>`;
  const ICON = {
    arrow: svg('<path d="M5 12h14M13 6l6 6-6 6"/>'),
    check: svg('<path d="M5 12.5l4.5 4.5 9.5-10"/>'),
    close: svg('<path d="M6 6l12 12M18 6L6 18"/>'),
    lock: svg('<rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>'),
    eye: svg('<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>'),
    eyeOff: svg('<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><path d="M4 4l16 16"/>'),
    info: svg('<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.1"/>'),
    warning: svg('<path d="M12 4l9 16H3z"/><path d="M12 10v4M12 17v.1"/>'),
    copy: svg('<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V6a2 2 0 0 1 2-2h8"/>'),
    link: svg('<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>'),
    posting: svg('<path d="M6 3h9l4 4v14H6z"/><path d="M15 3v4h4M9 12h7M9 16h4"/>'),
    scissors: svg('<circle cx="6" cy="6" r="2.5"/><circle cx="6" cy="18" r="2.5"/><path d="M8 7.5L20 18M8 16.5L20 6"/>'),
    level: svg('<path d="M3 20h5v-5h5v-5h5V5h3"/>'),
    stack: svg('<path d="M12 3l9 5-9 5-9-5z"/><path d="M3 13l9 5 9-5"/>'),
    skill: svg('<path d="M12 3l7.8 4.5v9L12 21l-7.8-4.5v-9z"/><circle cx="12" cy="12" r="2.5"/>'),
    mission: svg('<path d="M6 21V4M6 5h12l-3 4 3 4H6"/>'),
    plan: svg('<path d="M10 6h10M10 12h10M10 18h10M4 6h1.5M4 12h1.5M4 18h1.5"/>'),
    repo: svg('<path d="M4 19V5a2 2 0 0 1 2-2h13v14H6a2 2 0 0 0 0 4h13"/>'),
    tests: svg('<path d="M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.8 3h10.4a2 2 0 0 0 1.8-3l-5-9V3M7.5 15h9"/>'),
    question: svg('<path d="M4 5h16v11H10l-5 4v-4H4z"/><path d="M10.2 9a2 2 0 1 1 2.8 1.8c-.7.4-1 .7-1 1.2M12 13.9v.1"/>'),
    hint: svg('<path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.7.7 1 1.5 1 2.5h6c0-1 .3-1.8 1-2.5A6 6 0 0 0 12 3z"/>'),
    users: svg('<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.800a3.5 3.5 0 0 1 0 6.400M18 14.500a6.5 6.5 0 0 1 3.5 5.5"/>'),
    mail: svg('<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3.5 7.500l8.5 6 8.5-6"/>'),
    clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
    board: svg('<rect x="3" y="4" width="5" height="16" rx="1.5"/><rect x="10" y="4" width="5" height="10" rx="1.5"/><rect x="17" y="4" width="4" height="13" rx="1.5"/>'),
    domain: svg('<rect x="4" y="4" width="16" height="16" rx="3"/><path d="M12 8v8M8 12h8"/>'),
    out: svg('<circle cx="12" cy="12" r="9"/><path d="M8 12h8"/>'),
    github: '<svg class="ico" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/></svg>',
    STUCK: svg('<path d="M20 12a8 8 0 1 1-2.6-5.900M20 4v5h-5"/>'),
    IDLE: svg('<path d="M20 14.500A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.500z"/>'),
    ADVANCING: svg('<path d="M7 17L17 7M9 7h8v8"/>'),
    COMPLETED: svg('<path d="M5 12.5l4.5 4.5 9.5-10"/>'),
  };
  const STATUS = {
    STUCK: { label: 'travado', plural: 'Travados' },
    IDLE: { label: 'parado', plural: 'Parados' },
    ADVANCING: { label: 'avançando', plural: 'Avançando' },
    COMPLETED: { label: 'concluído', plural: 'Concluídos' },
  };
  // An icon tile: a 28 px outlined square with an ink glyph (DESIGN.md 2.5). `status` names a situation (STUCK, IDLE,
  // ADVANCING, COMPLETED, or the older colour names coral, idle, green, purple) and is the only way to colour it;
  // any other value (an older tile colour such as yellow) leaves the glyph ink.
  const TONES = { coral: 'STUCK', idle: 'IDLE', green: 'ADVANCING', purple: 'COMPLETED' };
  const ic = (name, status = '', size = '') => {
    const situation = STATUS[status] ? status : TONES[status];
    return `<span class="ic ${esc(size)}"${situation ? ` data-status="${situation}"` : ''} aria-hidden="true">${ICON[name] ?? ''}</span>`;
  };
  const LEVELS = { INTERN: 'estágio', JUNIOR: 'júnior', MID: 'pleno', SENIOR: 'sênior' };
  const REASONS = { CORE: ['central', 'core'], SKILL_MATCH: ['pela vaga', 'match'], DEFAULT: ['padrão', ''] };

  const initials = (name) => String(name ?? '').split(/\s+/).filter(Boolean).slice(0, 2).map((word) => [...word][0]).join('').toUpperCase();
  // A neutral circle with the initials in mono, with no colour per person (DESIGN.md 4.11).
  const avatar = (name, size = '') => `<span class="avatar ${esc(size)}" aria-hidden="true">${esc(initials(name))}</span>`;
  const statusBadge = (status, label = STATUS[status]?.label ?? '') => `<span class="status" data-status="${esc(status)}">${ICON[status] ?? ''}${esc(label)}</span>`;

  // The test grid: one row per plan mission, one square dot per test (DESIGN.md 4.16). Missions up to the current one
  // show passing (green) and failing (coral) dots with "passed/total"; missions after it show empty dots and "—", even
  // when the report has them failing, because the whole suite runs on every push. A mission without a tally shows only
  // its number and "—". A completed student is all green. The dots are decorative; each row carries its sentence.
  // The data comes from test reports: a total that is not a positive integer counts as no tally, and so does a
  // current or past mission whose `passed` is missing; `passed` is kept within 0..total.
  function testGrid(tests, missionOrder, completed = false) {
    const current = typeof missionOrder === 'number' && Number.isFinite(missionOrder) ? missionOrder : 0;
    const row = (t) => {
      const order = esc(t.order ?? '');
      const total = Number.isInteger(t.total) && t.total > 0 ? t.total : 0;
      const future = !completed && Number(t.order) > current;
      const hasPassed = typeof t.passed === 'number' && Number.isFinite(t.passed);
      const hasTotal = total > 0 && (completed || future || hasPassed);
      const passed = completed ? total : Math.max(0, Math.min(total, Math.floor(hasPassed ? t.passed : 0)));
      let dots = '';
      let count = '—';
      let text;
      if (!hasTotal) text = future ? 'ainda não começou' : 'ainda sem contagem de testes';
      else if (future) {
        dots = '<i class="td is-empty"></i>'.repeat(total);
        text = 'ainda não começou';
      } else {
        dots = '<i class="td is-pass"></i>'.repeat(passed) + '<i class="td is-fail"></i>'.repeat(total - passed);
        count = `${passed}/${total}`;
        text = `${passed} de ${total} ${total === 1 ? 'teste passando' : 'testes passando'}`;
      }
      const state = !hasTotal ? 'none' : future ? 'future' : 'counted';
      return `<span class="test-row" data-order="${order}" data-state="${state}"><span class="sr">Missão ${order}: ${text}.</span>`
        + `<span class="test-mission" aria-hidden="true">${esc(String(t.order ?? '').padStart(2, '0'))}</span>`
        + `<span class="test-dots" aria-hidden="true">${dots}</span>`
        + `<span class="test-count" aria-hidden="true">${count}</span></span>`;
    };
    return `<span class="test-grid">${(Array.isArray(tests) ? tests : []).map(row).join('')}</span>`;
  }

  // One mission of a plan: the server's strings are untrusted, so every one is escaped. `skillLabels` maps a skill
  // id from `matchedSkills` to the name the extraction gave it; without it the id is shown.
  const missionItem = (m, skillLabels = {}) => {
    const [label, cls] = REASONS[m.reason] ?? REASONS.DEFAULT;
    const skills = Array.isArray(m.matchedSkills) ? m.matchedSkills : [];
    const chips = skills.length ? `<div class="chips">${skills.map((s) => `<span class="chip">${esc(skillLabels[s] ?? s)}</span>`).join('')}</div>` : '';
    const level = Math.max(0, Math.min(3, Number(m.difficulty) || 0));
    const bars = [1, 2, 3].map((n) => `<i class="${n <= level ? 'on' : ''}"></i>`).join('');
    return `<li class="mission"><span class="num">${String(Number(m.order) || 0).padStart(2, '0')}</span>
      <div><h3>${esc(m.title)}</h3><p>${esc(m.summary)}</p>${chips}</div>
      <div class="side"><span class="tag ${cls}">${label}</span><span class="diff" role="img" aria-label="Dificuldade ${level} de 3">${bars}</span></div></li>`;
  };

  // The repository card of the last step. The name, the link and the missions come from the server.
  const repoPanel = (fullName, htmlUrl, missions) => `<div class="night-panel rise">
      <div class="panel-tag"><span>Seu repositório</span><b>${missions.length} ${missions.length === 1 ? 'missão aberta' : 'missões abertas'}</b></div>
      <div class="repo-name">${ICON.github}<a href="${esc(htmlUrl)}" rel="noopener">${esc(fullName)}</a></div>
      <ul class="issues stagger">${missions.map((m) => `<li><span class="dot" aria-hidden="true"></span>Missão ${Number(m.order) || ''}${m.title ? `: ${esc(m.title)}` : ''}</li>`).join('')}</ul>
      <p class="muted small" style="margin-top:14px">Os testes ficam vermelhos até você implementar cada missão.</p>
    </div>`;

  // The stages advance on a timer and are illustrative; the last one only turns done when `done` (the server's
  // answer) resolves, so a slow answer keeps it waiting (DESIGN.md 3.2). If `done` rejects, the sequence stops at once
  // on the stage it reached and the rejection reaches the caller. Each <li> holds a .step-mark and its name; the state
  // is also written in text for screen readers.
  const STAGE_TEXT = { 'is-done': 'feito', 'is-current': 'em andamento', 'is-todo': 'a fazer' };
  function paintStages(items, current) {
    items.forEach((li, i) => {
      const state = i < current ? 'is-done' : i === current ? 'is-current' : 'is-todo';
      const mark = $('.step-mark', li);
      if (mark && !mark.classList.contains(state)) { mark.classList.remove('is-done', 'is-current', 'is-todo'); mark.classList.add(state); }
      li.classList.remove('is-done', 'is-current', 'is-todo'); li.classList.add(state);
      let sr = $('.sr', li);
      if (!sr) li.append(sr = Object.assign(document.createElement('span'), { className: 'sr' }));
      sr.textContent = `, ${STAGE_TEXT[state]}`;
    });
  }
  async function runProgress(list, done) {
    const items = $$('li', list);
    const failed = Promise.resolve(done).then(() => new Promise(() => {})); // settles only by rejecting
    window.VVMotion?.init(list);
    for (let i = 0; i < items.length; i++) {
      paintStages(items, i);
      await Promise.race([failed, i === items.length - 1 ? Promise.all([done, wait(600)]) : wait(850)]);
    }
    paintStages(items, items.length);
    await wait(450);
  }

  // The count-up lives in vv-motion.js; without it the number is written directly.
  function countUp(el, to) {
    if (window.VVMotion) { window.VVMotion.countUp(el, to); return; }
    el.textContent = to;
  }

  let toastTimer;
  function toast(message) {
    let el = $('.toast');
    if (!el) {
      el = Object.assign(document.createElement('div'), { className: 'toast' });
      el.setAttribute('role', 'status');
      document.body.append(el);
    }
    el.innerHTML = `${ICON.check}<span>${esc(message)}</span>`;
    requestAnimationFrame(() => el.classList.add('show'));
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 3000);
  }

  function copyField(text) {
    return `<div class="copy"><code>${esc(text)}</code><button type="button" class="btn ghost sm" data-copy="${esc(text)}">${ICON.copy}<span>Copiar</span></button></div>`;
  }
  document.addEventListener('click', async (event) => {
    const button = event.target.closest('[data-copy]');
    if (!button) return;
    try {
      await navigator.clipboard.writeText(button.dataset.copy);
    } catch {
      // No clipboard access: select the link so it can be copied by hand.
      const code = button.parentElement.querySelector('code');
      if (code) getSelection().selectAllChildren(code);
      toast('Selecione o link e copie.');
      return;
    }
    const label = $('span', button);
    label.textContent = 'Copiado';
    toast('Link copiado.');
    setTimeout(() => { label.textContent = 'Copiar'; }, 2000);
  });

  // Loading: the button's content becomes the status line, "Lendo a vaga · 1,2 s" (DESIGN.md 4.1 and 4.14), with
  // the matrix pattern of the stage (scan, wave or flip). The width is kept so the layout does not jump. The line
  // measures the real wait: call busy() when the request is sent and idle() when it settles; idle(button, false)
  // ends it as stopped (an error or a redirect) rather than done.
  const lines = new WeakMap();
  function busy(button, label, pattern = 'scan') {
    if (lines.has(button)) idle(button, false);
    button.dataset.label = button.innerHTML;
    button.style.minWidth = `${button.offsetWidth}px`;
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    button.innerHTML = `<span class="status-line"><span data-dot-matrix="${esc(pattern)}"></span><span class="status-label"></span><span class="status-timer"></span></span>`;
    const line = $('.status-line', button);
    if (window.VVMotion) lines.set(button, window.VVMotion.statusLine(line, label));
    else { $('.status-label', line).textContent = label; lines.set(button, null); }
  }
  function idle(button, ok = true) {
    const line = lines.get(button);
    if (ok) line?.done(); else line?.stop();
    lines.delete(button);
    button.disabled = false;
    button.removeAttribute('aria-busy');
    button.style.minWidth = '';
    if (button.dataset.label !== undefined) button.innerHTML = button.dataset.label;
  }

  function setStep(n) {
    $$('.steps li').forEach((li, i) => {
      li.classList.toggle('done', i + 1 < n);
      if (i + 1 === n) li.setAttribute('aria-current', 'step'); else li.removeAttribute('aria-current');
      $('.n', li).innerHTML = i + 1 < n ? ICON.check : String(i + 1);
    });
  }

  // Marks a field as invalid or not. aria-invalid must read "true": an empty value means false to assistive tech.
  function invalid(field, on) {
    if (on) field.setAttribute('aria-invalid', 'true'); else field.removeAttribute('aria-invalid');
  }

  // The CSRF token Spring sets in a cookie; the API wants it back in a header on every write.
  function csrfToken() {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  }

  // The body of an error answer (a ProblemDetail), or {} when it is not JSON.
  async function problemDetail(response) {
    try { return await response.json(); } catch { return {}; }
  }

  // A link from the server is followed only when it points at GitHub.
  const gitHubUrl = (url) => (typeof url === 'string' && url.startsWith('https://github.com/') ? url : '#');

  // After GitHub signs the person in (or after installing the app), the start page reads this key and sends them back.
  const RETURN_KEY = 'vagaviva.voltar';
  function rememberReturn() {
    try { sessionStorage.setItem(RETURN_KEY, location.pathname); } catch { /* private mode: they land on the start page */ }
  }
  function signIn() {
    rememberReturn();
    location.assign('/oauth2/authorization/github');
  }

  // The top bar; `setUser` fills in who is signed in once the page knows.
  function chrome(area = '') {
    const logo = '<svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="6" fill="#141414"/><path d="M8 17l5 5 11-12" stroke="#33c47a" stroke-width="3.6" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    document.body.insertAdjacentHTML('afterbegin', `<header class="top"><div class="wrap">
      <a class="brand" href="/" aria-label="Vaga Viva, início">${logo}Vaga Viva${area ? `<small>${esc(area)}</small>` : ''}</a><span id="who"></span></div></header>`);
  }
  function setUser(login) {
    $('#who').innerHTML = login ? `<span class="user">${avatar(login, 'sm')}@${esc(login)}</span>` : '';
  }

  return { $, $$, esc, wait, ICON, ic, STATUS, LEVELS, avatar, statusBadge, testGrid, missionItem, repoPanel, runProgress, paintStages, countUp, toast, copyField, busy, idle, setStep, invalid, csrfToken, problemDetail, gitHubUrl, rememberReturn, signIn, chrome, setUser };
})();
