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
  const generations = new WeakMap();
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
    // The live region is shown empty and the label is written after a rendered frame: a region that appears
    // and fills in the same tick is often not announced. A newer call on the same line cancels the write.
    const gen = (generations.get(el) ?? 0) + 1;
    generations.set(el, gen);
    if (labelEl) labelEl.textContent = '';
    el.hidden = false;
    el.classList.remove('is-done', 'is-stopped');
    const write = () => { if (labelEl && generations.get(el) === gen && labelEl.textContent !== label) labelEl.textContent = label; };
    requestAnimationFrame(() => setTimeout(write, 0));
    setTimeout(write, 100); // rAF does not run in a background tab; the timeout still does
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

  // ---------- Dot field ----------
  // The only <canvas> of a page, inserted behind the wrapper's content. Near the cursor the dots turn green and
  // move a few px away, then drift back slowly. The rAF loop runs only while something moves and the field is
  // on screen; with touch (pointer: coarse) or reduced motion the grid is drawn once and stays still.
  const FIELD = { gap: 22, radius: 1.6, reach: 96, push: 6, baseAlpha: 0.5, tauIn: 70, tauOut: 650 };
  const COARSE = '(pointer: coarse)';
  const DARK = '(prefers-color-scheme: dark)';

  function bindDotField(el) {
    if (document.querySelector('canvas.dot-field-canvas')) return; // one canvas per page (DESIGN.md 3.2)
    const canvas = document.createElement('canvas');
    canvas.className = 'dot-field-canvas';
    canvas.setAttribute('aria-hidden', 'true');
    el.prepend(canvas);
    const ctx = canvas.getContext('2d');
    let width = 0, height = 0, dots = [], base = [0, 0, 0], hot = [0, 0, 0];
    let pointer = null, raf = 0, last = 0, visible = false;
    const still = () => reduced() || window.matchMedia(COARSE).matches;

    // Reads a CSS colour through the canvas, which normalises any notation to #rrggbb.
    const rgb = (value) => {
      ctx.fillStyle = '#000';
      ctx.fillStyle = value.trim() || '#000';
      const hex = ctx.fillStyle.startsWith('#') ? ctx.fillStyle : '#000000';
      return [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
    };
    const readColors = () => {
      const style = getComputedStyle(el);
      base = rgb(style.getPropertyValue('--df-dot'));
      hot = rgb(style.getPropertyValue('--df-hot'));
    };

    const layout = () => {
      const box = el.getBoundingClientRect();
      width = box.width; height = box.height;
      const dpr = Math.min(2, window.devicePixelRatio || 1);
      canvas.width = Math.round(width * dpr); canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const cols = Math.floor(width / FIELD.gap), rows = Math.floor(height / FIELD.gap);
      const x0 = (width - (cols - 1) * FIELD.gap) / 2, y0 = (height - (rows - 1) * FIELD.gap) / 2;
      dots = [];
      for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) dots.push({ x: x0 + c * FIELD.gap, y: y0 + r * FIELD.gap, dx: 0, dy: 0, heat: 0 });
    };

    const draw = () => {
      ctx.clearRect(0, 0, width, height);
      const [br, bg, bb] = base;
      ctx.fillStyle = `rgba(${br},${bg},${bb},${FIELD.baseAlpha})`; // the resting dots in one path
      ctx.beginPath();
      for (const d of dots) if (d.heat < 0.01) { ctx.moveTo(d.x + FIELD.radius, d.y); ctx.arc(d.x, d.y, FIELD.radius, 0, Math.PI * 2); }
      ctx.fill();
      for (const d of dots) {
        if (d.heat < 0.01) continue;
        const h = d.heat, mix = (a, b) => Math.round(a + (b - a) * h);
        ctx.fillStyle = `rgba(${mix(br, hot[0])},${mix(bg, hot[1])},${mix(bb, hot[2])},${FIELD.baseAlpha + (1 - FIELD.baseAlpha) * h})`;
        ctx.beginPath();
        ctx.arc(d.x + d.dx, d.y + d.dy, FIELD.radius + h * 0.8, 0, Math.PI * 2);
        ctx.fill();
      }
    };

    const stopLoop = () => { cancelAnimationFrame(raf); raf = 0; };
    const frame = (now) => {
      raf = 0;
      if (!el.isConnected) return teardown();
      // A frame's timestamp can precede the performance.now() read in wake(): never step with a negative dt.
      const dt = now > last ? Math.min(64, now - last) : 16; last = now;
      let moving = 0;
      for (const d of dots) {
        let tx = 0, ty = 0, th = 0;
        if (pointer) {
          const vx = d.x - pointer.x, vy = d.y - pointer.y, dist = Math.hypot(vx, vy);
          if (dist < FIELD.reach) {
            const f = 1 - dist / FIELD.reach, s = f * f * (3 - 2 * f);
            th = s;
            if (dist > 0.01) { tx = (vx / dist) * FIELD.push * s; ty = (vy / dist) * FIELD.push * s; }
          }
        }
        const k = 1 - Math.exp(-dt / (th > d.heat ? FIELD.tauIn : FIELD.tauOut)); // fast towards the cursor, slow back
        const ndx = d.dx + (tx - d.dx) * k, ndy = d.dy + (ty - d.dy) * k, nh = d.heat + (th - d.heat) * k;
        moving = Math.max(moving, Math.abs(ndx - d.dx), Math.abs(ndy - d.dy), Math.abs(nh - d.heat) * 10);
        d.dx = ndx; d.dy = ndy; d.heat = nh < 0.002 && th === 0 ? 0 : nh;
      }
      draw();
      if (moving > 0.01) raf = requestAnimationFrame(frame); // settled: the loop ends until the next pointer move
    };
    const wake = () => {
      if (raf || !visible || still()) return;
      last = performance.now();
      raf = requestAnimationFrame(frame);
    };
    const rest = () => { stopLoop(); for (const d of dots) { d.dx = 0; d.dy = 0; d.heat = 0; } draw(); };

    const onMove = (event) => {
      if (event.pointerType === 'touch' || still()) return;
      const box = el.getBoundingClientRect();
      pointer = { x: event.clientX - box.left, y: event.clientY - box.top };
      wake();
    };
    const onLeave = () => { pointer = null; wake(); };
    const onScheme = () => { readColors(); draw(); };
    const onMotion = () => { if (still()) { pointer = null; rest(); } };
    const media = [[DARK, onScheme], [REDUCED, onMotion], [COARSE, onMotion]].map(([q, fn]) => {
      const mq = window.matchMedia(q);
      mq.addEventListener?.('change', fn);
      return () => mq.removeEventListener?.('change', fn);
    });
    const sizer = new ResizeObserver(() => { layout(); draw(); wake(); });
    const watcher = new IntersectionObserver(([entry]) => {
      if (!el.isConnected) return teardown();
      visible = entry.isIntersecting;
      if (visible) wake(); else { stopLoop(); pointer = null; } // off screen: no frames at all, and no stale cursor on return
    });
    function teardown() {
      stopLoop(); sizer.disconnect(); watcher.disconnect(); media.forEach((off) => off());
      el.removeEventListener('pointermove', onMove); el.removeEventListener('pointerleave', onLeave);
    }
    el.addEventListener('pointermove', onMove);
    el.addEventListener('pointerleave', onLeave);
    readColors(); layout(); draw();
    sizer.observe(el);
    watcher.observe(el);
  }
  register('[data-dot-field]', bindDotField);

  // ---------- Decrypt ----------
  // The name resolves left to right from symbols in up to 600 ms. The final text in the DOM is the source of truth:
  // during the run the noise is aria-hidden and a visually hidden copy holds the final text; at the end the element
  // holds the plain final text. No aria-label, which screen readers ignore or double-read on a generic span.
  const GLYPHS = '·:•+×#%&/<>=*';
  const decrypting = new WeakMap(); // element -> { raf, resolve }
  const noise = () => GLYPHS[Math.floor(Math.random() * GLYPHS.length)];

  function decrypt(el, text) {
    const final = String(text ?? '');
    const prev = decrypting.get(el);
    if (prev) { cancelAnimationFrame(prev.raf); prev.resolve(); decrypting.delete(el); }
    el.classList.remove('is-decrypting');
    if (reduced() || !final.trim()) { el.textContent = final; return Promise.resolve(); }

    el.classList.add('is-decrypting');
    el.innerHTML = '<span class="dc-ghost" aria-hidden="true"></span><span class="dc-run" aria-hidden="true"><span class="dc-done"></span><span class="dc-noise"></span></span><span class="dc-sr"></span>';
    const [ghost, , sr] = el.children;
    const done = el.querySelector('.dc-done'), rest = el.querySelector('.dc-noise');
    ghost.textContent = final; sr.textContent = final; // the ghost alone sizes the box; the run layer sits over it
    const chars = [...final];
    rest.textContent = chars.map((c) => (c.trim() ? noise() : c)).join(''); // all noise from the first paint, never a blank
    const duration = Math.min(580, 180 + chars.length * 22); // 580 leaves a frame of slack inside the 600 ms
    const t0 = performance.now();
    let shown = -1, tick = -1;
    return new Promise((resolve) => {
      const job = { raf: 0, resolve };
      decrypting.set(el, job);
      const frame = (now) => {
        const p = Math.min(1, (now - t0) / duration);
        const resolvedCount = Math.floor(p * chars.length);
        const step = Math.floor((now - t0) / 45); // the noise changes about 22 times a second, not every frame
        if (resolvedCount !== shown || step !== tick) {
          shown = resolvedCount; tick = step;
          done.textContent = chars.slice(0, resolvedCount).join('');
          rest.textContent = chars.slice(resolvedCount).map((c) => (c.trim() ? noise() : c)).join('');
        }
        if (p < 1) { job.raf = requestAnimationFrame(frame); return; }
        el.classList.remove('is-decrypting');
        el.textContent = final;
        decrypting.delete(el);
        resolve();
      };
      job.raf = requestAnimationFrame(frame);
    });
  }
  register('[data-decrypt]', (el) => { decrypt(el, el.dataset.decrypt || el.textContent.trim()); });

  // ---------- Dot swap ----------
  // A mesh of purple dots covers the block in random order, swap() runs under it, and the mesh clears in a new
  // random order: about 500 ms. Each call swaps exactly once; a call made during a swap waits for it to end.
  const SWAP = { cover: 150, uncover: 150, pop: 90, hold: 20 }; // 150 + 90 + 20 + 150 + 90 = 500 ms
  const swapping = new WeakMap(); // element -> promise of the swap in progress
  const wait = (ms) => new Promise((r) => setTimeout(r, ms));
  const shuffledDelays = (n, span) => {
    const order = Array.from({ length: n }, (_, i) => i);
    for (let i = n - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [order[i], order[j]] = [order[j], order[i]]; }
    return order.map((rank) => Math.round((rank / Math.max(1, n - 1)) * span));
  };

  async function runSwap(el, swap) {
    if (reduced()) { swap(); return; }
    const box = el.getBoundingClientRect();
    const cell = Math.max(12, Math.ceil(Math.sqrt((box.width * box.height) / 480))); // about 480 dots, whatever the size
    const cols = Math.max(1, Math.ceil(box.width / cell)), rows = Math.max(1, Math.ceil(box.height / cell));
    const mesh = document.createElement('span');
    mesh.className = 'ds-mesh';
    mesh.setAttribute('aria-hidden', 'true');
    mesh.style.setProperty('--ds-cell', `${cell}px`);
    mesh.style.setProperty('--ds-cols', cols);
    const delays = shuffledDelays(cols * rows, SWAP.cover);
    mesh.innerHTML = delays.map((d) => `<i style="--d:${d}ms"></i>`).join('');
    el.setAttribute('aria-busy', 'true');
    el.append(mesh);
    void mesh.offsetWidth; // commit scale(0) so the cover transitions
    mesh.classList.add('is-on');
    try {
      await wait(SWAP.cover + SWAP.pop + SWAP.hold);
      swap();
      if (mesh.parentNode !== el) { el.append(mesh); void mesh.offsetWidth; } // swap() may have replaced the children
      const again = shuffledDelays(delays.length, SWAP.uncover);
      [...mesh.children].forEach((dot, i) => dot.style.setProperty('--d', `${again[i]}ms`));
      mesh.classList.remove('is-on');
      await wait(SWAP.uncover + SWAP.pop);
    } finally {
      mesh.remove();
      el.removeAttribute('aria-busy');
    }
  }

  function dotSwap(el, swap) {
    const next = (swapping.get(el) ?? Promise.resolve()).catch(() => {}).then(() => runSwap(el, swap));
    const tracked = next.finally(() => { if (swapping.get(el) === tracked) swapping.delete(el); });
    swapping.set(el, tracked);
    return tracked;
  }
  register('[data-dot-swap]', () => {}); // nothing to set up: the block only changes when dotSwap() is called

  window.VVMotion = { init, statusLine, countUp, decrypt, dotSwap };
})();
