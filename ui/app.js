/* ThanOS – Frontend UI logic (talks to Java via window.javaBridge) */

const $ = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

let bridge = null;
let apps = [];
let openWindows = {}; // id -> { model, el }
let zCounter = 100;

// ---------- Bootstrap ----------
window.onJavaReady = function (appsJson, wsJson, activeId, theme, wallpaperUrl) {
  bridge = window.javaBridge;
  apps = typeof appsJson === 'string' ? JSON.parse(appsJson) : appsJson;
  applyTheme(theme || 'DARK', wallpaperUrl || '');
  renderShortcuts();
  renderStartMenu();
  startClocks();
};

document.addEventListener('DOMContentLoaded', () => {
  if (window.javaBridge && !bridge) {
    bridge = window.javaBridge;
    try {
      apps = JSON.parse(bridge.getApps());
      applyTheme(bridge.getTheme(), bridge.getWallpaperUrl(bridge.getTheme()));
      renderShortcuts();
      renderStartMenu();
      startClocks();
    } catch (e) { console.warn(e); }
  }
  bindLockScreen();
  bindDesktop();
});

// ---------- Theme ----------
function applyTheme(theme, wallpaperUrl) {
  const t = (theme || 'DARK').toUpperCase();
  document.documentElement.setAttribute('data-theme', t === 'LIGHT' ? 'light' : 'dark');
  const wall = document.querySelector('.desktop-wallpaper');
  if (wall) {
    let url = wallpaperUrl;
    if (!url && bridge) {
      try { url = bridge.getWallpaperUrl(t); } catch (e) {}
    }
    if (url) {
      wall.style.backgroundImage = "url('" + url + "')";
      wall.style.backgroundSize = 'cover';
      wall.style.backgroundPosition = 'center';
    } else {
      wall.style.backgroundImage = '';
    }
  }
}

// ---------- Lock screen ----------
function bindLockScreen() {
  const pw = $('#lock-password');
  const btn = $('#unlock-btn');
  const feedback = $('#lock-feedback');

  function attempt() {
    const val = pw.value;
    if (!val || !val.trim()) {
      feedback.textContent = 'Please enter a password';
      feedback.classList.remove('hidden');
      return;
    }
    let ok = true;
    if (bridge) ok = bridge.unlock(val);
    if (ok) {
      feedback.classList.add('hidden');
      $('#lock-screen').classList.remove('active');
      $('#desktop').classList.add('active');
    } else {
      feedback.textContent = 'Invalid password';
      feedback.classList.remove('hidden');
    }
  }

  btn.addEventListener('click', attempt);
  pw.addEventListener('keydown', e => { if (e.key === 'Enter') attempt(); });
}

function startClocks() {
  function tick() {
    if (bridge) {
      $('#lock-clock').textContent = bridge.getTime();
      $('#lock-date').textContent = bridge.getDate();
      $('#tb-clock').textContent = bridge.getTaskbarClock();
    } else {
      const now = new Date();
      $('#lock-clock').textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
      $('#lock-date').textContent = now.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
      $('#tb-clock').textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
    }
  }
  tick();
  setInterval(tick, 1000);
}

// ---------- Desktop chrome ----------
function bindDesktop() {
  $('#start-btn').addEventListener('click', () => {
    $('#start-menu').classList.toggle('hidden');
  });
  $('#lock-btn').addEventListener('click', () => {
    if (bridge) bridge.saveAll();
    $('#desktop').classList.remove('active');
    $('#lock-screen').classList.add('active');
    $('#lock-password').value = '';
    $('#start-menu').classList.add('hidden');
  });
  $('.desktop-wallpaper').addEventListener('click', () => {
    $('#start-menu').classList.add('hidden');
  });
}

function renderShortcuts() {
  const box = $('#shortcuts');
  box.innerHTML = '';
  apps.forEach(a => {
    const el = document.createElement('div');
    el.className = 'shortcut';
    el.innerHTML = `<div class="shortcut-icon">${a.icon}</div><div class="shortcut-name">${a.name}</div>`;
    el.addEventListener('click', () => launchApp(a.id));
    box.appendChild(el);
  });
}

function renderStartMenu() {
  const box = $('#start-apps');
  box.innerHTML = '';
  apps.forEach(a => {
    const el = document.createElement('div');
    el.className = 'start-item';
    el.innerHTML = `<div class="icon">${a.icon}</div><span>${a.name}</span>`;
    el.addEventListener('click', () => {
      launchApp(a.id);
      $('#start-menu').classList.add('hidden');
    });
    box.appendChild(el);
  });
}

// ---------- Window management ----------
function launchApp(appId) {
  if (!bridge) {
    createWindowUI({ id: 'local-' + Date.now(), appId, title: appId, workspaceId: 'ws-main', state: 'FOCUSED', x: 100, y: 80, width: 560, height: 420 });
    return;
  }
  const json = bridge.launchApp(appId);
  const model = JSON.parse(json);
  if (model.error) return;
  createWindowUI(model);
  refreshTaskbar();
}

function createWindowUI(model) {
  const layer = $('#windows-layer');
  const win = document.createElement('div');
  win.className = 'window focused';
  win.style.left = model.x + 'px';
  win.style.top = model.y + 'px';
  win.style.width = model.width + 'px';
  win.style.height = model.height + 'px';
  win.style.zIndex = ++zCounter;
  win.dataset.id = model.id;

  win.innerHTML = `
    <div class="win-titlebar">
      <span class="win-title">${esc(model.title)}</span>
      <button class="win-btn min" title="Minimize">—</button>
      <button class="win-btn close" title="Close">✕</button>
    </div>
    <div class="win-body"></div>
  `;

  const body = $('.win-body', win);
  body.appendChild(buildAppContent(model.appId, model.id));

  // drag
  const bar = $('.win-titlebar', win);
  let dragging = false, ox = 0, oy = 0;
  bar.addEventListener('mousedown', e => {
    if (e.target.closest('.win-btn')) return;
    dragging = true;
    ox = e.clientX - win.offsetLeft;
    oy = e.clientY - win.offsetTop;
    focusWin(model.id);
  });
  document.addEventListener('mousemove', e => {
    if (!dragging) return;
    win.style.left = Math.max(0, e.clientX - ox) + 'px';
    win.style.top = Math.max(0, e.clientY - oy) + 'px';
  });
  document.addEventListener('mouseup', () => { dragging = false; });

  $('.min', win).addEventListener('click', () => {
    win.classList.add('minimized');
    model.state = 'MINIMIZED';
    if (bridge) bridge.minimizeWindow(model.id);
    refreshTaskbar();
  });
  $('.close', win).addEventListener('click', () => {
    if (bridge) bridge.closeWindow(model.id);
    win.remove();
    delete openWindows[model.id];
    refreshTaskbar();
  });

  win.addEventListener('mousedown', () => focusWin(model.id));

  layer.appendChild(win);
  openWindows[model.id] = { model, el: win };
}

function focusWin(id) {
  Object.values(openWindows).forEach(w => {
    w.el.classList.remove('focused');
    w.model.state = w.model.state === 'MINIMIZED' ? 'MINIMIZED' : 'NORMAL';
  });
  const w = openWindows[id];
  if (w) {
    w.el.classList.add('focused');
    w.el.style.zIndex = ++zCounter;
    w.model.state = 'FOCUSED';
    if (bridge) bridge.focusWindow(id);
  }
}

function refreshTaskbar() {
  const box = $('#taskbar-apps');
  box.innerHTML = '';
  Object.values(openWindows).forEach(w => {
    const btn = document.createElement('button');
    btn.className = 'taskbar-app' + (w.model.state === 'MINIMIZED' ? ' minimized' : '');
    btn.textContent = w.model.title;
    btn.addEventListener('click', () => {
      if (w.model.state === 'MINIMIZED') {
        w.el.classList.remove('minimized');
        w.model.state = 'FOCUSED';
        if (bridge) bridge.restoreWindow(w.model.id);
      }
      focusWin(w.model.id);
      refreshTaskbar();
    });
    box.appendChild(btn);
  });
}

function syncWindowsFromJava() {
  if (!bridge) return;
  try {
    const list = JSON.parse(bridge.getWindows());
    // simple: if new windows appeared from queue, create them
    list.forEach(m => {
      if (!openWindows[m.id]) createWindowUI(m);
    });
  } catch (e) {}
}

// ---------- App content builders ----------
function buildAppContent(appId, winId) {
  switch (appId) {
    case 'notes': return buildNotes();
    case 'calendar': return buildCalendar();
    case 'calculator': return buildCalculator();
    case 'files': return buildFiles();
    case 'settings': return buildSettings();
    default: {
      const d = document.createElement('div');
      d.textContent = 'Unknown app';
      return d;
    }
  }
}

function buildNotes() {
  const root = document.createElement('div');
  root.className = 'app-layout';
  root.innerHTML = `
    <div class="app-sidebar">
      <h3>Notes</h3>
      <div class="notes-list"></div>
      <button class="btn small" style="margin-top:8px">+ New</button>
    </div>
    <div class="app-main">
      <input type="text" class="note-title" placeholder="Title" />
      <textarea class="note-body" placeholder="Write your note..."></textarea>
      <div style="display:flex;gap:8px">
        <button class="btn primary save-note">Save</button>
        <button class="btn small del-note">Delete</button>
      </div>
    </div>
  `;
  const list = $('.notes-list', root);
  const titleIn = $('.note-title', root);
  const bodyIn = $('.note-body', root);
  let currentId = null;

  function refresh() {
    list.innerHTML = '';
    if (!bridge) return;
    const notes = JSON.parse(bridge.getNotes());
    notes.forEach(n => {
      const item = document.createElement('div');
      item.className = 'list-item' + (n.id === currentId ? ' active' : '');
      item.textContent = n.title;
      item.addEventListener('click', () => {
        currentId = n.id;
        titleIn.value = n.title;
        bodyIn.value = n.content;
        refresh();
      });
      list.appendChild(item);
    });
  }

  $('.btn.small', root).addEventListener('click', () => {
    if (!bridge) return;
    const n = JSON.parse(bridge.createNote('Untitled', ''));
    currentId = n.id;
    titleIn.value = n.title;
    bodyIn.value = '';
    refresh();
  });
  $('.save-note', root).addEventListener('click', () => {
    if (!bridge) return;
    if (currentId) {
      bridge.updateNote(currentId, titleIn.value, bodyIn.value);
    } else {
      const n = JSON.parse(bridge.createNote(titleIn.value, bodyIn.value));
      currentId = n.id;
    }
    refresh();
  });
  $('.del-note', root).addEventListener('click', () => {
    if (!bridge || !currentId) return;
    bridge.deleteNote(currentId);
    currentId = null;
    titleIn.value = '';
    bodyIn.value = '';
    refresh();
  });
  refresh();
  return root;
}

function buildCalendar() {
  const root = document.createElement('div');
  root.className = 'app-layout';
  let year = new Date().getFullYear();
  let month = new Date().getMonth(); // 0-based
  let selected = toDateStr(new Date());

  root.innerHTML = `
    <div class="app-main" style="flex:1">
      <div class="cal-nav">
        <button class="prev">&lt;</button>
        <span class="month-label"></span>
        <button class="next">&gt;</button>
      </div>
      <div class="cal-grid"></div>
    </div>
    <div class="app-sidebar" style="width:200px">
      <h3>Events</h3>
      <div class="evt-list"></div>
      <input type="text" class="evt-title" placeholder="Event title" style="margin-top:8px" />
      <textarea class="evt-desc" placeholder="Description" style="min-height:60px;margin-top:6px"></textarea>
      <button class="btn small add-evt" style="margin-top:6px">Add Event</button>
      <button class="btn small del-evt" style="margin-top:4px">Delete Selected</button>
    </div>
  `;

  const grid = $('.cal-grid', root);
  const label = $('.month-label', root);
  const evtList = $('.evt-list', root);
  let selectedEvtId = null;
  const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];

  function render() {
    label.textContent = months[month] + ' ' + year;
    grid.innerHTML = '';
    ['Mo','Tu','We','Th','Fr','Sa','Su'].forEach(d => {
      const h = document.createElement('div');
      h.className = 'cal-day-h';
      h.textContent = d;
      grid.appendChild(h);
    });
    const first = new Date(year, month, 1);
    let startCol = first.getDay() - 1; // Mon=0
    if (startCol < 0) startCol = 6;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = toDateStr(new Date());

    for (let i = 0; i < startCol; i++) {
      const e = document.createElement('div');
      grid.appendChild(e);
    }
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
      const cell = document.createElement('div');
      cell.className = 'cal-day';
      if (dateStr === today) cell.classList.add('today');
      if (dateStr === selected) cell.classList.add('selected');
      cell.textContent = d;
      // events indicator
      if (bridge) {
        try {
          const evts = JSON.parse(bridge.getEventsForDate(dateStr));
          if (evts.length) cell.classList.add('has-event');
        } catch (e) {}
      }
      cell.addEventListener('click', () => {
        selected = dateStr;
        selectedEvtId = null;
        render();
        loadEvents();
      });
      grid.appendChild(cell);
    }
  }

  function loadEvents() {
    evtList.innerHTML = '';
    if (!bridge) return;
    const evts = JSON.parse(bridge.getEventsForDate(selected));
    evts.forEach(e => {
      const item = document.createElement('div');
      item.className = 'list-item' + (e.id === selectedEvtId ? ' active' : '');
      item.textContent = e.title;
      item.addEventListener('click', () => { selectedEvtId = e.id; loadEvents(); });
      evtList.appendChild(item);
    });
  }

  $('.prev', root).addEventListener('click', () => {
    month--; if (month < 0) { month = 11; year--; }
    render();
  });
  $('.next', root).addEventListener('click', () => {
    month++; if (month > 11) { month = 0; year++; }
    render();
  });
  $('.add-evt', root).addEventListener('click', () => {
    if (!bridge) return;
    const t = $('.evt-title', root).value;
    if (!t.trim()) return;
    bridge.createEvent(selected, t, $('.evt-desc', root).value);
    $('.evt-title', root).value = '';
    $('.evt-desc', root).value = '';
    render();
    loadEvents();
  });
  $('.del-evt', root).addEventListener('click', () => {
    if (!bridge || !selectedEvtId) return;
    bridge.deleteEvent(selectedEvtId);
    selectedEvtId = null;
    render();
    loadEvents();
  });

  render();
  loadEvents();
  return root;
}

function buildCalculator() {
  const root = document.createElement('div');
  let display = '0';
  let acc = 0;
  let pending = null;
  let fresh = true;

  root.innerHTML = `
    <div class="calc-display">0</div>
    <div class="calc-grid">
      <button class="calc-btn" data-k="C">C</button>
      <button class="calc-btn" data-k="±">±</button>
      <button class="calc-btn" data-k="⌫">⌫</button>
      <button class="calc-btn op" data-k="/">÷</button>
      <button class="calc-btn" data-k="7">7</button>
      <button class="calc-btn" data-k="8">8</button>
      <button class="calc-btn" data-k="9">9</button>
      <button class="calc-btn op" data-k="*">×</button>
      <button class="calc-btn" data-k="4">4</button>
      <button class="calc-btn" data-k="5">5</button>
      <button class="calc-btn" data-k="6">6</button>
      <button class="calc-btn op" data-k="-">−</button>
      <button class="calc-btn" data-k="1">1</button>
      <button class="calc-btn" data-k="2">2</button>
      <button class="calc-btn" data-k="3">3</button>
      <button class="calc-btn op" data-k="+">+</button>
      <button class="calc-btn span2" data-k="0">0</button>
      <button class="calc-btn" data-k=".">.</button>
      <button class="calc-btn op" data-k="=">=</button>
    </div>
  `;
  const disp = $('.calc-display', root);

  function setDisp(v) {
    display = String(v);
    disp.textContent = display;
  }

  function applyPending() {
    const cur = parseFloat(display) || 0;
    if (pending === null) { acc = cur; return; }
    if (bridge && ['+','-','*','/'].includes(pending)) {
      const r = bridge.calc(pending, String(acc), String(cur));
      if (r === 'Error') { setDisp('Error'); pending = null; fresh = true; return; }
      acc = parseFloat(r);
      setDisp(r);
    } else {
      switch (pending) {
        case '+': acc += cur; break;
        case '-': acc -= cur; break;
        case '*': acc *= cur; break;
        case '/':
          if (cur === 0) { setDisp('Error'); pending = null; fresh = true; return; }
          acc /= cur; break;
      }
      setDisp(acc === (acc|0) ? String(acc|0) : String(acc));
    }
  }

  root.addEventListener('click', e => {
    const k = e.target.dataset && e.target.dataset.k;
    if (!k) return;
    if (k === 'C') { setDisp('0'); acc = 0; pending = null; fresh = true; }
    else if (k === '⌫') {
      if (display.length > 1 && display !== 'Error') setDisp(display.slice(0, -1));
      else setDisp('0');
    }
    else if (k === '±') {
      if (display !== '0' && display !== 'Error')
        setDisp(display.startsWith('-') ? display.slice(1) : '-' + display);
    }
    else if (['+','-','*','/'].includes(k)) {
      applyPending(); pending = k; fresh = true;
    }
    else if (k === '=') { applyPending(); pending = null; fresh = true; }
    else if (k === '.') {
      if (fresh) { setDisp('0.'); fresh = false; }
      else if (!display.includes('.')) setDisp(display + '.');
    }
    else { // digit
      if (fresh || display === '0' || display === 'Error') { setDisp(k); fresh = false; }
      else setDisp(display + k);
    }
  });
  return root;
}

function buildFiles() {
  const root = document.createElement('div');
  root.className = 'app-main files-app';
  let current = '/';
  let selected = null;

  root.innerHTML = `
    <div class="files-toolbar">
      <button class="btn small up-btn" title="Up">↑ Up</button>
      <button class="btn small refresh-btn">Refresh</button>
      <button class="btn small new-folder">New Folder</button>
      <button class="btn small new-file">New File</button>
      <button class="btn small rename-btn">Rename</button>
      <button class="btn small copy-btn">Copy</button>
      <button class="btn small del-file">Delete</button>
      <span class="path-label"></span>
    </div>
    <div class="files-body">
      <div class="files-main">
        <div class="file-list-header">
          <span class="col-name">Name</span>
          <span class="col-type">Type</span>
          <span class="col-size">Size</span>
          <span class="col-date">Modified</span>
        </div>
        <div class="file-list"></div>
      </div>
      <div class="files-side">
        <h4>Recent</h4>
        <div class="recent-list"></div>
        <div class="preview-pane hidden">
          <h4 class="preview-title">Preview</h4>
          <div class="preview-content"></div>
        </div>
      </div>
    </div>
    <p class="sandbox-note"></p>
  `;

  const list = $('.file-list', root);
  const pathLabel = $('.path-label', root);
  const recentList = $('.recent-list', root);
  const previewPane = $('.preview-pane', root);
  const previewContent = $('.preview-content', root);
  const previewTitle = $('.preview-title', root);

  function formatSize(n) {
    if (n == null || n === 0) return '—';
    if (n < 1024) return n + ' B';
    if (n < 1048576) return (n / 1024).toFixed(1) + ' KB';
    return (n / 1048576).toFixed(1) + ' MB';
  }
  function formatDate(ms) {
    if (!ms) return '—';
    const d = new Date(ms);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
  }
  function iconFor(f) {
    if (f.dir) return '📁';
    if (f.isApp) return '⚡';
    if (f.type === 'image') return '🖼';
    if (f.type === 'pdf') return '📕';
    if (f.type === 'code') return '💻';
    if (f.type === 'data') return '{ }';
    if (f.type === 'text') return '📄';
    return '📎';
  }

  function refreshRecent() {
    recentList.innerHTML = '';
    if (!bridge) return;
    try {
      const items = JSON.parse(bridge.getRecentFiles());
      if (!items.length) {
        recentList.innerHTML = '<div class="recent-empty">No recent items</div>';
        return;
      }
      items.forEach(it => {
        const row = document.createElement('div');
        row.className = 'recent-row';
        const label = it.isApp ? '⚡ ' + it.displayName : '📄 ' + it.displayName;
        row.innerHTML = '<span class="recent-name">' + esc(label) + '</span>';
        row.title = it.pathOrId;
        row.addEventListener('click', () => {
          if (!bridge) return;
          const res = JSON.parse(bridge.openRecent(it.pathOrId));
          handleOpenResult(res, it.displayName);
          refreshRecent();
        });
        recentList.appendChild(row);
      });
    } catch (e) {
      recentList.innerHTML = '<div class="recent-empty">Unable to load</div>';
    }
  }

  function handleOpenResult(res, fallbackName) {
    if (!res || res.error) {
      if (res && res.error === 'missing') {
        alert('File is missing and was removed from Recent.');
        refreshRecent();
      } else {
        alert('Could not open: ' + (res && res.error ? res.error : 'unknown'));
      }
      return;
    }
    // App launch returns window model with id/appId
    if (res.appId && res.id) {
      createWindowUI(res);
      refreshTaskbar();
      return;
    }
    if (res.type === 'folder') return;
    if (res.type === 'image') {
      previewPane.classList.remove('hidden');
      previewTitle.textContent = res.name || fallbackName || 'Image';
      previewContent.innerHTML = '<img class="preview-img" src="' + res.dataUrl + '" alt=""/>';
      return;
    }
    if (res.type === 'text') {
      previewPane.classList.remove('hidden');
      previewTitle.textContent = res.name || fallbackName || 'Text';
      const ta = document.createElement('textarea');
      ta.className = 'preview-text';
      ta.value = res.content || '';
      previewContent.innerHTML = '';
      previewContent.appendChild(ta);
      const saveBtn = document.createElement('button');
      saveBtn.className = 'btn small';
      saveBtn.textContent = 'Save';
      saveBtn.style.marginTop = '6px';
      saveBtn.addEventListener('click', () => {
        // Write using absolute path is not exposed; use relative from current if name matches
        const name = res.relativeName || res.name;
        if (bridge.writeTextFile(current === '/' ? '' : current, name, ta.value)) {
          alert('Saved');
          refresh();
        } else {
          alert('Save failed');
        }
      });
      previewContent.appendChild(saveBtn);
      return;
    }
    if (res.type === 'pdf') {
      previewPane.classList.remove('hidden');
      previewTitle.textContent = res.name || 'PDF';
      previewContent.innerHTML = '<p>PDF document (' + formatSize(res.size) + ').</p><p style="font-size:12px;color:var(--text3)">Path: ' + esc(res.path) + '</p><p style="font-size:12px">Open with an external PDF viewer from the sandbox folder.</p>';
      return;
    }
    if (res.type === 'binary') {
      previewPane.classList.remove('hidden');
      previewTitle.textContent = res.name || 'File';
      previewContent.innerHTML = '<p>Binary file (' + formatSize(res.size) + ').</p>';
      return;
    }
  }

  function refresh() {
    list.innerHTML = '';
    selected = null;
    pathLabel.textContent = current === '/' ? '/' : '/' + current;
    if (!bridge) return;
    let files = [];
    try {
      files = JSON.parse(bridge.listFiles(current === '/' ? '' : current));
    } catch (e) { files = []; }
    files.forEach(f => {
      const row = document.createElement('div');
      row.className = 'file-row';
      row.innerHTML =
        '<span class="col-name">' + iconFor(f) + ' ' + esc(f.name) + '</span>' +
        '<span class="col-type">' + esc(f.type || (f.dir ? 'folder' : 'file')) + '</span>' +
        '<span class="col-size">' + (f.dir ? '—' : formatSize(f.size)) + '</span>' +
        '<span class="col-date">' + formatDate(f.modified) + '</span>';
      row.addEventListener('click', () => {
        $$('.file-row', list).forEach(r => r.classList.remove('selected'));
        row.classList.add('selected');
        selected = f;
      });
      row.addEventListener('dblclick', () => {
        if (f.dir) {
          current = (current === '/' || !current ? '' : current + '/') + f.name;
          if (current.startsWith('/')) current = current.slice(1);
          refresh();
        } else {
          openSelected(f);
        }
      });
      list.appendChild(row);
    });
    try {
      $('.sandbox-note', root).textContent = 'Local files: ' + bridge.getSandboxPath();
    } catch (e) {}
    refreshRecent();
  }

  function openSelected(f) {
    if (!bridge || !f) return;
    try {
      const res = JSON.parse(bridge.openFile(current === '/' ? '' : current, f.name));
      handleOpenResult(res, f.name);
      refreshRecent();
    } catch (e) {
      alert('Open failed');
    }
  }

  $('.up-btn', root).addEventListener('click', () => {
    if (current === '/' || !current) return;
    const parts = current.split('/').filter(Boolean);
    parts.pop();
    current = parts.length ? parts.join('/') : '/';
    refresh();
  });
  $('.refresh-btn', root).addEventListener('click', refresh);
  $('.new-folder', root).addEventListener('click', () => {
    const name = prompt('Folder name:');
    if (!name || !bridge) return;
    if (!bridge.createFolder(current === '/' ? '' : current, name)) alert('Could not create folder (name may already exist)');
    refresh();
  });
  $('.new-file', root).addEventListener('click', () => {
    const name = prompt('File name (e.g. notes.txt):');
    if (!name || !bridge) return;
    if (!bridge.createFile(current === '/' ? '' : current, name)) alert('Could not create file (name may already exist)');
    refresh();
  });
  $('.rename-btn', root).addEventListener('click', () => {
    if (!selected || !bridge) { alert('Select an item first'); return; }
    const name = prompt('New name:', selected.name);
    if (!name || name === selected.name) return;
    if (!bridge.renamePath(current === '/' ? '' : current, selected.name, name)) alert('Rename failed');
    refresh();
  });
  $('.copy-btn', root).addEventListener('click', () => {
    if (!selected || !bridge) { alert('Select an item first'); return; }
    if (!bridge.copyPath(current === '/' ? '' : current, selected.name, current === '/' ? '' : current)) alert('Copy failed');
    else alert('Copied');
    refresh();
  });
  $('.del-file', root).addEventListener('click', () => {
    if (!selected || !bridge) { alert('Select an item first'); return; }
    if (!confirm('Delete "' + selected.name + '"? This cannot be undone.')) return;
    if (!bridge.deletePath(current === '/' ? '' : current, selected.name)) alert('Delete failed');
    previewPane.classList.add('hidden');
    refresh();
  });

  // Open button via Enter on selected
  list.addEventListener('keydown', e => {
    if (e.key === 'Enter' && selected) {
      if (selected.dir) {
        current = (current === '/' || !current ? '' : current + '/') + selected.name;
        if (current.startsWith('/')) current = current.slice(1);
        refresh();
      } else openSelected(selected);
    }
  });

  refresh();
  return root;
}

function buildSettings() {
  const root = document.createElement('div');
  root.className = 'app-main';
  let theme = 'DARK';
  let stats = { apps: 5, windows: 0, history: 0, recent: 0 };
  if (bridge) {
    try {
      theme = bridge.getTheme();
      stats = JSON.parse(bridge.getStats());
    } catch (e) {}
  }

  root.innerHTML = `
    <div class="settings-section">
      <h3>Appearance</h3>
      <div class="theme-toggle">
        <label><input type="radio" name="theme" value="DARK" ${theme === 'DARK' ? 'checked' : ''}/> Dark</label>
        <label><input type="radio" name="theme" value="LIGHT" ${theme === 'LIGHT' ? 'checked' : ''}/> Light</label>
      </div>
      <p style="font-size:12px;color:var(--text3);margin-top:6px">Theme also switches the desktop wallpaper.</p>
    </div>
    <div class="settings-section">
      <h3>Navigation</h3>
      <p style="font-size:13px;color:var(--text2)">App history depth: <span class="hist-size">${stats.history}</span></p>
      <button class="btn small hist-back">Go Back</button>
    </div>
    <div class="settings-section">
      <h3>Status</h3>
      <div class="stats-grid">
        <div class="stat-card"><div class="num">${stats.apps}</div><div class="lbl">Apps</div></div>
        <div class="stat-card"><div class="num">${stats.windows}</div><div class="lbl">Open windows</div></div>
        <div class="stat-card"><div class="num hist-size2">${stats.history}</div><div class="lbl">History</div></div>
        <div class="stat-card"><div class="num recent-count">${stats.recent}</div><div class="lbl">Recent items</div></div>
      </div>
    </div>
    <div class="settings-section">
      <h3>About ThanOS</h3>
      <p style="font-size:13px;color:var(--text2);line-height:1.5">
        ThanOS – Java + HTML/CSS/JS desktop environment<br/>
        Offline · Local persistence under ~/.thanos/<br/>
        Unlock with any non-empty password
      </p>
    </div>
  `;

  $$('input[name="theme"]', root).forEach(r => {
    r.addEventListener('change', () => {
      if (r.checked) {
        let wall = '';
        try { if (bridge) wall = bridge.getWallpaperUrl(r.value); } catch (e) {}
        applyTheme(r.value, wall);
        if (bridge) bridge.setTheme(r.value);
      }
    });
  });

  $('.hist-back', root).addEventListener('click', () => {
    if (!bridge) return;
    const prev = bridge.historyBack();
    const sz = bridge.historySize();
    $$('.hist-size', root).forEach(el => el.textContent = sz);
    $$('.hist-size2', root).forEach(el => el.textContent = sz);
    if (prev) {
      // quietly navigate back by launching previous app if known
      launchApp(prev);
    } else {
      alert('Nothing further in history');
    }
  });

  return root;
}

// ---------- Helpers ----------
function esc(s) {
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}

function toDateStr(d) {
  return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
}
