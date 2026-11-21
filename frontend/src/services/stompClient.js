/**
 * Lightweight STOMP 1.2 client over native browser WebSocket with JWT CONNECT headers
 * and exponential-backoff auto-reconnect. Compatible with Spring Boot's @EnableWebSocketMessageBroker.
 */
export function createStompClient(
  url,
  {
    connectHeaders = {},
    onConnect,
    onDisconnect,
    onReconnecting,
    onError,
    maxReconnectDelay = 15000,
  } = {}
) {
  let socket = null;
  let connected = false;
  let manuallyClosed = false;
  let reconnectAttempts = 0;
  let reconnectTimer = null;
  let subCounter = 0;
  const subscriptions = new Map();

  function buildFrame(command, headers = {}, body = '') {
    const headerLines = Object.entries(headers)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${k}:${v}`)
      .join('\n');
    return `${command}\n${headerLines}\n\n${body}\0`;
  }

  function parseFrames(raw) {
    const chunks = raw.split('\0');
    const frames = [];
    for (const chunk of chunks) {
      const trimmed = chunk.replace(/^\r?\n/, '');
      if (!trimmed) continue;
      const divider = trimmed.indexOf('\n\n');
      if (divider === -1) continue;
      const headerPart = trimmed.slice(0, divider);
      const body = trimmed.slice(divider + 2);
      const lines = headerPart.split(/\r?\n/);
      const command = lines[0];
      const headers = {};
      for (let i = 1; i < lines.length; i += 1) {
        const sep = lines[i].indexOf(':');
        if (sep !== -1) {
          headers[lines[i].slice(0, sep)] = lines[i].slice(sep + 1);
        }
      }
      frames.push({ command, headers, body });
    }
    return frames;
  }

  function scheduleReconnect() {
    if (manuallyClosed || reconnectTimer) return;
    reconnectAttempts += 1;
    const delay = Math.min(1000 * Math.pow(1.8, reconnectAttempts - 1), maxReconnectDelay);
    if (onReconnecting) onReconnecting(reconnectAttempts);
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      if (!manuallyClosed) {
        connect();
      }
    }, delay);
  }

  function connect() {
    manuallyClosed = false;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    try {
      socket = new WebSocket(url);
    } catch (err) {
      if (onError) onError(err);
      scheduleReconnect();
      return;
    }

    socket.onopen = () => {
      socket.send(
        buildFrame('CONNECT', {
          'accept-version': '1.2,1.1,1.0',
          'heart-beat': '0,0',
          ...connectHeaders,
        })
      );
    };

    socket.onmessage = (event) => {
      const frames = parseFrames(String(event.data || ''));
      for (const frame of frames) {
        if (frame.command === 'CONNECTED') {
          connected = true;
          reconnectAttempts = 0;
          if (onConnect) onConnect();
        } else if (frame.command === 'MESSAGE') {
          const subId = frame.headers.subscription;
          const entry = subscriptions.get(subId);
          if (entry?.callback) {
            try {
              entry.callback(JSON.parse(frame.body));
            } catch (err) {
              console.error('Failed to parse STOMP message payload', err);
            }
          }
        } else if (frame.command === 'ERROR') {
          if (onError) onError(new Error(frame.headers.message || frame.body || 'STOMP error'));
        }
      }
    };

    socket.onerror = (err) => {
      if (onError) onError(err);
    };

    socket.onclose = () => {
      connected = false;
      subscriptions.clear();
      if (onDisconnect) onDisconnect();
      if (!manuallyClosed) {
        scheduleReconnect();
      }
    };
  }

  function subscribe(destination, callback) {
    if (!socket || socket.readyState !== WebSocket.OPEN || !connected) {
      return null;
    }
    const id = `sub-${++subCounter}`;
    subscriptions.set(id, { destination, callback });
    socket.send(buildFrame('SUBSCRIBE', { id, destination }));
    return () => {
      subscriptions.delete(id);
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(buildFrame('UNSUBSCRIBE', { id }));
      }
    };
  }

  function publish(destination, payload) {
    if (!socket || socket.readyState !== WebSocket.OPEN || !connected) {
      return false;
    }
    const body = typeof payload === 'string' ? payload : JSON.stringify(payload);
    socket.send(
      buildFrame(
        'SEND',
        {
          destination,
          'content-type': 'application/json',
        },
        body
      )
    );
    return true;
  }

  function disconnect() {
    manuallyClosed = true;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    subscriptions.clear();
    if (socket) {
      if (socket.readyState === WebSocket.OPEN && connected) {
        try {
          socket.send(buildFrame('DISCONNECT'));
        } catch {
          // ignore close errors
        }
      }
      socket.close();
      socket = null;
    }
    connected = false;
  }

  return {
    connect,
    subscribe,
    publish,
    disconnect,
    isConnected: () => connected,
  };
}
