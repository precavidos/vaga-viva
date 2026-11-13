// Vaga Viva — motion components (DESIGN.md, section 3.3). Plain script, no build: exposes window.VVMotion.
// Each component registers itself with a selector and a bind function; init(root) binds every match once.
(() => {
  const REDUCED = '(prefers-reduced-motion: reduce)';
  const reduced = () => window.matchMedia(REDUCED).matches;
  const easeOutCubic = (p) => 1 - (1 - p) ** 3;

  // ---------- Registry ----------
  const components = [];
  const bound = new WeakMap(); // element -> Set of component indexes already bound to it
  const register = (selector, bind) => components.push({ selector, bind });

  function init(root = document) {
    components.forEach(({ selector, bind }, index) => {
      const matches = [...root.querySelectorAll(selector)];
      if (root instanceof Element && root.matches(selector)) matches.unshift(root);
      for (const el of matches) {
        let done = bound.get(el);
        if (!done) bound.set(el, (done = new Set()));
        if (done.has(index)) continue;
        done.add(index);
        bind(el);
      }
    });
  }

  // ---------- Dot matrix ----------
  // The element is empty in the HTML; 16 dots are injected with their row and column as custom properties.
  // scan and wave are CSS loops (.is-running); flip is stepped here because each dot stays green until the reset.
  const FLIP_ORDER = [5, 0, 10, 3, 12, 7, 1, 14, 9, 4, 15, 2, 11, 6, 13, 8];
  const FLIP_STEP = 90; // 16 dots × 90 ms = 1.44 s, then all green until the 1.6 s cycle resets.
  const CYCLE = 1600;
  const matrices = new WeakMap();

  function bindMatrix(el) {
    el.setAttribute('aria-hidden', 'true');
    el.innerHTML = Array.from({ length: 16 }, (_, i) => `<i style="--r:${Math.floor(i / 4)};--c:${i % 4}"></i>`).join('');
    const dots = [...el.children];
    let timer = 0;

    const clear = () => { clearInterval(timer); timer = 0; };
    const ctrl = {
      start() {
        clear();
        el.classList.remove('is-done');
        dots.forEach((d) => d.classList.remove('is-on'));
        if (reduced()) { el.classList.add('is-still'); el.classList.remove('is-running'); return; }
        el.classList.remove('is-still');
        el.classList.add('is-running');
        if (el.dataset.dotMatrix !== 'flip') return;
        const t0 = performance.now();
        let lit = 0;
        let cycle = 0;
        timer = setInterval(() => {
          const elapsed = performance.now() - t0;
          const t = elapsed % CYCLE;
          const want = t >= CYCLE - FLIP_STEP ? 0 : Math.min(16, Math.floor(t / FLIP_STEP) + 1);
          const turn = Math.floor(elapsed / CYCLE);
          if (want === lit && turn === cycle) return;
          if (want < lit || turn !== cycle) { // a new cycle: reset without the fade, so the grid snaps back to coral
            cycle = turn;
            el.classList.add('is-reset');
            dots.forEach((d) => d.classList.remove('is-on'));
            void el.offsetWidth;
            el.classList.remove('is-reset');
            lit = 0;
          }
          for (; lit < want; lit++) dots[FLIP_ORDER[lit]].classList.add('is-on');
        }, 30);
      },
      // Ends the loop. done: the final state of the pattern (flip all green); otherwise still as it is drawn at rest.
      stop(done) {
        clear();
        el.classList.remove('is-running');
        el.classList.toggle('is-still', reduced());
        el.classList.toggle('is-done', !!done);
        if (!done) dots.forEach((d) => d.classList.remove('is-on'));
      },
    };
    matrices.set(el, ctrl);
    // Inside a status line the loop waits for statusLine(); a matrix on its own runs from the start.
    if (el.closest('.status-line')) ctrl.stop(false);
    else ctrl.start();
  }
  register('[data-dot-matrix]', bindMatrix);

  // ---------- Status line ----------
  // statusLine(el, label) shows the line, writes the label, counts "0,0 s" and runs the matrix.
  // done() / stop() freeze the timer and end the matrix loop.
  const lines = new WeakMap();
  const seconds = (ms) => `${(ms / 1000).toFixed(1).replace('.', ',')} s`;

  function statusLine(el, label) {
    lines.get(el)?.stop();
    init(el);
    const labelEl = el.querySelector('.status-label');
    const timerEl = el.querySelector('.status-timer');
    const matrixEl = el.querySelector('[data-dot-matrix]');
    const matrix = matrixEl && matrices.get(matrixEl);
    if (!el.hasAttribute('role')) el.setAttribute('role', 'status');
    timerEl?.setAttribute('aria-hidden', 'true');
    el.hidden = false;
    el.classList.remove('is-done', 'is-stopped');
    if (labelEl) labelEl.textContent = label;
    const t0 = performance.now();
    const tick = () => { if (timerEl) timerEl.textContent = seconds(performance.now() - t0); };
    tick();
    const timer = setInterval(tick, 100);
    matrix?.start();

    let open = true;
    const end = (done) => {
      if (!open) return;
      open = false;
      clearInterval(timer);
      tick();
      matrix?.stop(done);
      el.classList.add(done ? 'is-done' : 'is-stopped');
      if (lines.get(el) === handle) lines.delete(el);
    };
    const handle = { done: () => end(true), stop: () => end(false) };
    lines.set(el, handle);
    return handle;
  }

  // ---------- Step mark ----------
  // The state is the class (is-todo, is-current, is-done), switched by the page; the SVG is injected once.
  const STEP_SVG = '<svg viewBox="0 0 18 18" aria-hidden="true" focusable="false">'
    + '<circle class="sm-ring" cx="9" cy="9" r="7"/>'
    + '<g class="sm-spin"><circle class="sm-arc" cx="9" cy="9" r="7" pathLength="100"/></g>'
    + '<circle class="sm-disc" cx="9" cy="9" r="9"/>'
    + '<path class="sm-check" d="M5.3 9.4l2.4 2.4 5-5.1" pathLength="1"/></svg>';

  register('.step-mark', (el) => {
    if (!el.querySelector('svg')) el.innerHTML = STEP_SVG;
    el.setAttribute('aria-hidden', 'true');
    if (reduced()) el.classList.add('is-still');
  });

  // ---------- Count-up ----------
  // The element's text already holds the final number (no JS, reduced motion); the animation runs once per call.
  const counting = new WeakMap();
  const format = (n) => n.toLocaleString('pt-BR');

  function countUp(el, to) {
    cancelAnimationFrame(counting.get(el));
    const target = Number(to);
    if (!Number.isFinite(target)) return;
    if (reduced()) { el.textContent = format(target); return; }
    const t0 = performance.now();
    const frame = (now) => {
      const p = Math.min(1, (now - t0) / 600);
      el.textContent = format(Math.round(target * easeOutCubic(p)));
      if (p < 1) counting.set(el, requestAnimationFrame(frame));
      else counting.delete(el);
    };
    el.textContent = format(0);
    counting.set(el, requestAnimationFrame(frame));
  }
  register('[data-count-up]', (el) => countUp(el, el.dataset.countUp));

  window.VVMotion = { init, statusLine, countUp };
})();
