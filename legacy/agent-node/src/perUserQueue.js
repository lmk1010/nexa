// Per-user fair concurrency gate.
//
// Enforces both a global cap and a per-user cap. Requests that cannot be
// granted immediately queue in FIFO order but a queued entry is skipped if
// its user is already saturated, so one boss's fan-out cannot starve another.

export function deriveUserKey(context) {
  const login = context?.loginUser;
  const id = login?.id ?? login?.userId;
  if (id !== undefined && id !== null && id !== '') {
    return `u:${id}`;
  }
  if (context?.tenantId) {
    return `t:${context.tenantId}`;
  }
  return 'anon';
}

export function createPerUserQueue({ name, maxGlobal, maxPerUser, queueTimeoutMs }) {
  let active = 0;
  const perUser = new Map();
  const waiting = [];

  function userActive(key) {
    return perUser.get(key) || 0;
  }

  function bump(key, delta) {
    const next = Math.max(0, userActive(key) + delta);
    if (next === 0) {
      perUser.delete(key);
    } else {
      perUser.set(key, next);
    }
  }

  function release(key) {
    active = Math.max(0, active - 1);
    bump(key, -1);
    schedule();
  }

  function cleanup(entry) {
    if (entry.timer) clearTimeout(entry.timer);
    if (entry.signal && entry.abortHandler) {
      entry.signal.removeEventListener('abort', entry.abortHandler);
    }
  }

  function reject(entry, error) {
    if (entry.done) return;
    entry.done = true;
    const index = waiting.indexOf(entry);
    if (index >= 0) waiting.splice(index, 1);
    cleanup(entry);
    entry.reject(error);
  }

  function grant(entry) {
    if (entry.done) return;
    entry.done = true;
    cleanup(entry);
    active += 1;
    bump(entry.userKey, +1);
    entry.resolve(() => release(entry.userKey));
  }

  function schedule() {
    if (active >= maxGlobal || waiting.length === 0) return;
    for (let i = 0; i < waiting.length && active < maxGlobal; ) {
      const entry = waiting[i];
      if (userActive(entry.userKey) < maxPerUser) {
        waiting.splice(i, 1);
        grant(entry);
      } else {
        i += 1;
      }
    }
  }

  function acquire(context, signal) {
    if (signal?.aborted) {
      return Promise.reject(new Error(`${name} slot aborted before queueing`));
    }
    const key = deriveUserKey(context);
    if (active < maxGlobal && userActive(key) < maxPerUser) {
      active += 1;
      bump(key, +1);
      return Promise.resolve(() => release(key));
    }
    return new Promise((resolve, reject) => {
      const entry = {
        userKey: key,
        resolve,
        reject,
        signal,
        done: false,
        timer: null,
        abortHandler: null,
      };
      entry.timer = setTimeout(() => {
        rejectEntry(
          entry,
          new Error(`${name} queue timeout after ${queueTimeoutMs}ms`),
        );
      }, queueTimeoutMs);
      if (signal) {
        entry.abortHandler = () =>
          rejectEntry(entry, new Error(`${name} slot aborted while queued`));
        signal.addEventListener('abort', entry.abortHandler, { once: true });
      }
      waiting.push(entry);
    });
  }

  function rejectEntry(entry, error) {
    reject(entry, error);
  }

  function snapshot() {
    return {
      name,
      active,
      waiting: waiting.length,
      perUser: Object.fromEntries(perUser),
      maxGlobal,
      maxPerUser,
    };
  }

  function reset() {
    active = 0;
    perUser.clear();
    for (const entry of waiting.splice(0)) {
      cleanup(entry);
      entry.done = true;
    }
  }

  return { acquire, snapshot, reset };
}
