/* LuxeStay frontend – talks to the Spring Boot REST API (same origin, session cookie login) */

const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];
const money = n => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 });
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/* Images / names are presentation only, so they stay in the frontend, keyed by room type from the DB */
const TYPE_META = {
  STANDARD: { name: 'Classic Standard', tag: 'Easy comfort',
    image: 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=900&q=80' },
  DELUXE: { name: 'Deluxe Garden', tag: 'Guest favourite',
    image: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=900&q=80' },
  SUITE: { name: 'Signature Suite', tag: 'More room to unwind',
    image: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=900&q=80' }
};
const meta = type => TYPE_META[type] || { name: type, tag: '', image: '' };
// each room has its own image; if the file is missing, fall back to the room-type image
const roomImg = r => r.imageUrl || meta(r.roomType).image;
const imgFallback = r => `onerror="this.onerror=null;this.src='${meta(r.roomType).image}'"`;

let user = null;      // { name, email, role } when logged in, otherwise null
let allRooms = [];    // rooms loaded from GET /api/rooms

/* ───────────── API helper ───────────── */
async function api(path, { method = 'GET', json, form } = {}) {
  const opts = { method, credentials: 'same-origin', headers: {} };
  if (json) { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(json); }
  if (form) { opts.headers['Content-Type'] = 'application/x-www-form-urlencoded'; opts.body = new URLSearchParams(form).toString(); }

  let res;
  try { res = await fetch(path, opts); }
  catch { throw new Error('Cannot reach the server. Is the Spring Boot app running?'); }

  let data = null;
  try { data = await res.json(); } catch { /* empty body */ }

  if (!res.ok) {
    const err = new Error((data && data.message) || 'Something went wrong (' + res.status + ').');
    err.status = res.status;
    throw err;
  }
  return data;
}

function message(txt, error = false) {
  const el = $('#form-message');
  if (el) el.innerHTML = `<div class="notice ${error ? 'error' : ''}">${esc(txt)}</div>`;
}

const isAdmin = () => user && user.role === 'ROLE_ADMIN';

/* ───────────── session / navbar ───────────── */
async function loadUser() {
  try {
    const me = await api('/api/auth/me');
    user = me && me.authenticated ? me : null;
  } catch { user = null; }
}

function renderNav() {
  const el = $('#nav-actions');
  if (el) {
    el.innerHTML = user
      ? `<span class="small muted">Hi, ${esc(user.name.split(' ')[0])}</span><button class="btn btn-outline" id="logout-btn">Sign out</button>`
      : `<a class="btn btn-outline" href="login.html">Sign in</a><a class="btn btn-primary" href="register.html">Join us</a>`;
  }
  // Admin link only for admins
  $$('#nav-links a[href="admin-bookings.html"]').forEach(a => { if (!isAdmin()) a.remove(); });

  $('#logout-btn')?.addEventListener('click', async () => {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* ignore */ }
    location.href = 'login.html?logout=true';
  });
}

function goToLogin() {
  const next = location.pathname.split('/').pop() + location.search;
  location.href = 'login.html?next=' + encodeURIComponent(next);
}

// only allow redirects to our own pages, e.g. "payment.html?booking=5"
function safeNext() {
  const next = new URLSearchParams(location.search).get('next');
  return next && /^[a-z0-9-]+\.html(\?[\w=&%.-]*)?$/i.test(next) ? next : null;
}

/* ───────────── register + login ───────────── */
function auth() {
  const params = new URLSearchParams(location.search);

  if ($('#login-form')) {
    if (params.get('registered')) message('Account created. Please sign in.');
    if (params.get('logout')) message('You have been signed out.');
    if (user) { location.href = safeNext() || (isAdmin() ? 'admin-bookings.html' : 'rooms.html'); return; }
  }

  $('#register-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = e.target.querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      await api('/api/auth/register', {
        method: 'POST',
        json: { name: $('#name').value.trim(), email: $('#email').value.trim(), password: $('#password').value }
      });
      location.href = 'login.html?registered=true';
    } catch (err) {
      message(err.message, true);
      btn.disabled = false;
    }
  });

  $('#login-form')?.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = e.target.querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      await api('/api/auth/login', {
        method: 'POST',
        form: { email: $('#email').value.trim(), password: $('#password').value }
      });
      await loadUser();
      location.href = safeNext() || (isAdmin() ? 'admin-bookings.html' : 'rooms.html');
    } catch (err) {
      message(err.message, true);
      btn.disabled = false;
    }
  });
}

/* ───────────── rooms ───────────── */
function roomCard(r) {
  const m = meta(r.roomType);
  return `<article class="room-card"><div class="room-image"><img src="${esc(roomImg(r))}" alt="${esc(m.name)}" loading="lazy" ${imgFallback(r)}><span class="room-badge">${esc(m.tag)}</span></div>
  <div class="room-body"><div class="eyebrow">${esc(r.roomType.toLowerCase())}</div><h3>${esc(m.name)}</h3><p>${esc(r.description)}</p>
  <div class="room-meta"><span>♙ Up to ${r.maxGuests} guests</span><span>✦ Free Wi-Fi</span><span>⌂ Room ${esc(r.roomNumber)}</span></div>
  <div class="room-bottom"><div class="price">${money(r.pricePerNight)} <small>/ night</small></div>
  <a class="btn btn-primary" href="booking-form.html?room=${r.id}">Book now →</a></div></div></article>`;
}

function renderRooms(target, list) {
  const el = $(target);
  if (!el) return;
  el.innerHTML = list.map(roomCard).join('');
  $('#rooms-empty')?.classList.toggle('hidden', list.length > 0);
}

async function roomsPages() {
  const needsRooms = $('#home-rooms') || $('#rooms-grid') || $('#booking-form');
  if (!needsRooms) return;
  try { allRooms = await api('/api/rooms'); }
  catch (err) {
    allRooms = [];
    const box = $('#home-rooms') || $('#rooms-grid');
    if (box) box.innerHTML = `<div class="notice error">${esc(err.message)}</div>`;
    return;
  }

  // Home page: one room per type
  if ($('#home-rooms')) {
    const seen = new Set();
    renderRooms('#home-rooms', allRooms.filter(r => !seen.has(r.roomType) && seen.add(r.roomType)));
  }

  // Rooms page with filter buttons
  if ($('#rooms-grid')) {
    let filter = (new URLSearchParams(location.search).get('type') || 'ALL').toUpperCase();
    const draw = () => renderRooms('#rooms-grid', allRooms.filter(r => filter === 'ALL' || r.roomType === filter));
    $$('#room-filters button').forEach(b => {
      b.classList.toggle('active', b.dataset.type === filter);
      b.addEventListener('click', () => {
        filter = b.dataset.type;
        $$('#room-filters button').forEach(x => x.classList.toggle('active', x === b));
        draw();
      });
    });
    draw();
  }
}

/* ───────────── booking form ───────────── */
function bookingForm() {
  const form = $('#booking-form');
  if (!form) return;
  if (!allRooms.length) { message('No rooms are available right now.', true); return; }

  const select = $('#room-select');
  const wanted = Number(new URLSearchParams(location.search).get('room'));
  const byId = id => allRooms.find(r => r.id === Number(id)) || allRooms[0];

  select.innerHTML = allRooms.map(r =>
    `<option value="${r.id}">${esc(meta(r.roomType).name)} · Room ${esc(r.roomNumber)} — ${money(r.pricePerNight)}/night</option>`).join('');
  select.value = byId(wanted).id;

  // dates (local time, not UTC)
  const fmt = d => { const x = new Date(d); x.setMinutes(x.getMinutes() - x.getTimezoneOffset()); return x.toISOString().slice(0, 10); };
  const today = new Date();
  const min = fmt(today);
  const tomorrow = new Date(today); tomorrow.setDate(tomorrow.getDate() + 1);
  $('#checkin').min = min;  $('#checkin').value = min;
  $('#checkout').min = fmt(tomorrow);  $('#checkout').value = fmt(tomorrow);

  $('#guestName').value = user.name;
  $('#guestEmail').value = user.email;

  function update() {
    const r = byId(select.value);
    const ci = $('#checkin').value, co = $('#checkout').value;

    // keep check-out after check-in
    if (ci) {
      const next = new Date(ci + 'T00:00:00'); next.setDate(next.getDate() + 1);
      $('#checkout').min = fmt(next);
      if (!co || co <= ci) $('#checkout').value = fmt(next);
    }
    const co2 = $('#checkout').value;
    const nights = ci && co2 ? Math.max(0, Math.round((new Date(co2 + 'T00:00:00') - new Date(ci + 'T00:00:00')) / 86400000)) : 0;

    // guest dropdown follows the room's capacity
    const g = $('#guests'), prev = Number(g.value) || 2;
    g.innerHTML = Array.from({ length: r.maxGuests }, (_, i) => `<option>${i + 1}</option>`).join('');
    g.value = Math.min(prev, r.maxGuests);

    const img = $('#summary-image');
    img.onerror = () => { img.onerror = null; img.src = meta(r.roomType).image; };
    img.src = roomImg(r);
    $('#summary-name').textContent = meta(r.roomType).name + ' · Room ' + r.roomNumber;
    $('#summary-type').textContent = r.roomType;
    $('#summary-price').textContent = money(r.pricePerNight) + ' / night';
    $('#summary-checkin').textContent = ci || 'Choose date';
    $('#summary-checkout').textContent = co2 || 'Choose date';
    $('#summary-nights').textContent = nights;
    $('#summary-guests').textContent = g.value;
    $('#summary-total').textContent = money(nights * r.pricePerNight);
  }
  [select, $('#checkin'), $('#checkout'), $('#guests')].forEach(el => el.addEventListener('change', update));
  update();

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = form.querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      const b = await api('/api/bookings', {
        method: 'POST',
        json: {
          roomId: Number(select.value),
          guestName: $('#guestName').value.trim(),
          guestEmail: $('#guestEmail').value.trim(),
          guestPhone: $('#guestPhone')?.value.trim() || '',
          checkInDate: $('#checkin').value,
          checkOutDate: $('#checkout').value,
          numGuests: Number($('#guests').value)
        }
      });
      location.href = 'payment.html?booking=' + b.id;
    } catch (err) {
      message(err.message, true);
      btn.disabled = false;
    }
  });
}

/* ───────────── my bookings ───────────── */
function bookingRow(b, admin = false) {
  const room = `${esc(meta(b.roomType).name)}<br><span class="small muted">Room ${esc(b.roomNumber)}</span>`;
  const actions = admin
    ? `<button class="btn btn-danger" data-delete="${b.id}">Delete</button>`
    : (b.status === 'PENDING' ? `<a class="btn btn-outline" href="payment.html?booking=${b.id}">Pay</a> ` : '')
      + (b.status !== 'CANCELLED' ? `<button class="btn btn-danger" data-cancel="${b.id}">Cancel</button>` : '');

  return `<tr><td>#${b.id}</td>
  ${admin ? `<td>${esc(b.guestName)}<br><span class="small muted">${esc(b.guestEmail)}</span></td>` : ''}
  <td>${room}</td><td>${esc(b.checkInDate)}</td><td>${esc(b.checkOutDate)}</td><td>${b.numNights} night(s)</td>
  <td>${money(b.totalAmount)}</td><td><span class="status ${b.status.toLowerCase()}">${esc(b.status)}</span></td><td>${actions}</td></tr>`;
}

async function myBookings() {
  const el = $('#mybookings-content');
  if (!el) return;
  $('#booking-greeting').textContent = `Reservations for ${user.name}`;

  let list;
  try { list = await api('/api/bookings/my'); }
  catch (err) { el.innerHTML = `<div class="notice error">${esc(err.message)}</div>`; return; }

  if (!list.length) {
    el.innerHTML = `<div class="empty-state"><div class="empty-icon">⌂</div><h3>No bookings yet</h3><p>Once you book a room, your reservations will appear here.</p><a class="btn btn-primary" href="rooms.html">Explore rooms →</a></div>`;
    return;
  }
  el.innerHTML = `<div class="table-wrap"><table><thead><tr><th>Booking</th><th>Room</th><th>Check-in</th><th>Check-out</th><th>Stay</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead><tbody>${list.map(b => bookingRow(b)).join('')}</tbody></table></div>`;

  $$('[data-cancel]').forEach(btn => btn.onclick = async () => {
    if (!confirm('Cancel this booking?')) return;
    try { await api(`/api/bookings/${btn.dataset.cancel}/cancel`, { method: 'POST' }); myBookings(); }
    catch (err) { alert(err.message); }
  });
}

/* ───────────── payment + success ───────────── */
const bookingIdFromUrl = () => new URLSearchParams(location.search).get('booking');

async function payment() {
  const form = $('#payment-form');
  if (!form) return;
  const el = $('#payment-summary');

  let b;
  try { b = await api('/api/bookings/' + bookingIdFromUrl()); }
  catch (err) {
    el.innerHTML = `<p>${esc(err.message)} <a href="rooms.html">Choose a room first.</a></p>`;
    form.classList.add('hidden');
    return;
  }

  el.innerHTML = `<div class="room-summary"><img src="${meta(b.roomType).image}" alt="Room"><div><div class="eyebrow">${esc(b.roomType)}</div><h3>${esc(meta(b.roomType).name)}</h3><p class="small muted">Reservation #${b.id}</p></div></div>
  <div class="summary-line"><span>Guest</span><strong>${esc(b.guestName)}</strong></div>
  <div class="summary-line"><span>Check-in</span><strong>${esc(b.checkInDate)}</strong></div>
  <div class="summary-line"><span>Check-out</span><strong>${esc(b.checkOutDate)}</strong></div>
  <div class="summary-line"><span>Guests / nights</span><strong>${b.numGuests} / ${b.numNights}</strong></div>
  <div class="summary-line"><span>Status</span><strong>${esc(b.status)}</strong></div>
  <div class="summary-line total"><span>Total</span><strong>${money(b.totalAmount)}</strong></div>`;

  if (b.status !== 'PENDING') {
    form.classList.add('hidden');
    el.insertAdjacentHTML('beforeend',
      `<div class="notice">${b.status === 'CONFIRMED' ? 'This booking is already paid.' : 'This booking was cancelled.'} <a href="mybookings.html">Go to my bookings</a></div>`);
    return;
  }

  $('#payer-name').value = b.guestName;
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = form.querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      const p = await api('/api/payments', {
        method: 'POST',
        json: { bookingId: b.id, paymentMethod: $('#payment-method').value }
      });
      location.href = `payment-success.html?booking=${b.id}&txn=${encodeURIComponent(p.transactionId)}`;
    } catch (err) {
      message(err.message, true);
      btn.disabled = false;
    }
  });
}

async function success() {
  const el = $('#success-summary');
  if (!el) return;
  let b;
  try { b = await api('/api/bookings/' + bookingIdFromUrl()); }
  catch (err) { el.innerHTML = `<p>${esc(err.message)}</p>`; return; }

  const txn = new URLSearchParams(location.search).get('txn');
  el.innerHTML = `<div class="summary-line"><span>Booking reference</span><strong>#${b.id}</strong></div>
  <div class="summary-line"><span>Guest</span><strong>${esc(b.guestName)}</strong></div>
  <div class="summary-line"><span>Room</span><strong>${esc(meta(b.roomType).name)} · ${esc(b.roomNumber)}</strong></div>
  <div class="summary-line"><span>Stay</span><strong>${esc(b.checkInDate)} → ${esc(b.checkOutDate)}</strong></div>
  <div class="summary-line"><span>Payment</span><strong>${b.status === 'CONFIRMED' ? 'Confirmed' : esc(b.status)}${txn ? ' · ' + esc(txn) : ''}</strong></div>
  <div class="summary-line total"><span>Total</span><strong>${money(b.totalAmount)}</strong></div>`;
}

/* ───────────── admin ───────────── */
async function admin() {
  if (!$('#admin-table')) return;
  let list;
  try { list = await api('/api/admin/bookings'); }
  catch (err) { $('#admin-table').innerHTML = `<div class="notice error">${esc(err.message)}</div>`; return; }

  $('#stat-bookings').textContent = list.length;
  $('#stat-confirmed').textContent = list.filter(b => b.status === 'CONFIRMED').length;
  $('#stat-revenue').textContent = money(list.filter(b => b.status === 'CONFIRMED').reduce((s, b) => s + b.totalAmount, 0));

  $('#admin-table').innerHTML = list.length
    ? `<div class="table-wrap"><table><thead><tr><th>Booking</th><th>Guest</th><th>Room</th><th>Check-in</th><th>Check-out</th><th>Nights</th><th>Total</th><th>Status</th><th>Action</th></tr></thead><tbody>${list.map(b => bookingRow(b, true)).join('')}</tbody></table></div>`
    : `<div class="empty-state"><div class="empty-icon">▤</div><h3>No reservations yet</h3><p>Bookings will appear here.</p></div>`;

  $$('[data-delete]').forEach(btn => btn.onclick = async () => {
    if (!confirm('Delete this booking permanently?')) return;
    try { await api('/api/admin/bookings/' + btn.dataset.delete, { method: 'DELETE' }); admin(); }
    catch (err) { alert(err.message); }
  });

  const exp = $('#export-bookings');
  if (exp) exp.onclick = () => {
    const rows = [['Booking ID', 'Guest', 'Email', 'Room', 'Check-in', 'Check-out', 'Nights', 'Guests', 'Amount', 'Status'],
      ...list.map(b => [b.id, b.guestName, b.guestEmail, b.roomNumber, b.checkInDate, b.checkOutDate, b.numNights, b.numGuests, b.totalAmount, b.status])];
    const csv = rows.map(r => r.map(v => '"' + String(v ?? '').replaceAll('"', '""') + '"').join(',')).join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    a.download = 'luxestay-bookings.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  };
}

/* ───────────── start-up ───────────── */
document.addEventListener('DOMContentLoaded', async () => {
  $('#year') && ($('#year').textContent = new Date().getFullYear());
  $('#menu-toggle')?.addEventListener('click', () => $('#nav-links').classList.toggle('open'));

  await loadUser();
  renderNav();

  const page = document.body.dataset.page;

  // pages that need a logged-in user
  if (['booking', 'mybookings', 'payment', 'success'].includes(page) && !user) { goToLogin(); return; }
  // admin page needs an admin
  if (page === 'admin' && !isAdmin()) { user ? (location.href = 'index.html') : goToLogin(); return; }

  auth();
  await roomsPages();
  bookingForm();
  myBookings();
  payment();
  success();
  admin();
});
