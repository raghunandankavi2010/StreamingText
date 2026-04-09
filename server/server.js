/**
 * Simple Node.js SSE server for the StreamingText Android demo.
 *
 * Endpoint: GET /sse?query=<text>
 *   - Streams the response word-by-word as SSE events
 *   - Sends "data: [DONE]\n\n" when the stream is finished
 *
 * Run:
 *   node server.js
 *
 * Test from terminal:
 *   curl -N "http://localhost:3000/sse?query=hello"
 *
 * Android emulator connects via:
 *   http://10.0.2.2:3000/sse
 *
 * Physical device: replace 10.0.2.2 with your machine's local IP
 *   (e.g. http://192.168.1.42:3000/sse)
 */

const http = require('http');
const url  = require('url');

// ---------------------------------------------------------------------------
// Canned responses (keyword → reply text)
// ---------------------------------------------------------------------------
const RESPONSES = {
  hello:      'Hello from your Node.js SSE server! Each word you are reading arrived as a separate Server-Sent Event streamed over a real HTTP connection.',
  kotlin:     'Kotlin is a concise, null-safe language from JetBrains. It is the preferred language for Android development and supports coroutines for async work out of the box.',
  compose:    'Jetpack Compose is Android\'s declarative UI toolkit. You describe what the UI should look like and Compose figures out the minimal updates needed when state changes.',
  sse:        'Server-Sent Events use a plain HTTP connection where the server keeps sending data: lines separated by blank lines. The client reads them as a stream. No WebSocket needed!',
  android:    'Android is the world\'s most popular mobile OS. With Kotlin, Coroutines, and Jetpack Compose, building modern Android apps has never been more enjoyable.',
  node:       'Node.js is an event-driven JavaScript runtime built on Chrome\'s V8 engine. It is perfect for lightweight streaming servers like this one.',
  streaming:  'Streaming responses are sent token by token so the user sees output immediately instead of waiting for the full response. SSE makes this trivial on both server and client.',
};

function pickResponse(query) {
  const lower = query.toLowerCase();
  for (const [key, text] of Object.entries(RESPONSES)) {
    if (lower.includes(key)) return text;
  }
  return `You asked: "${query}". This is a real SSE response from your local Node.js server! ` +
    'Each word arrives as a separate data event streamed over a persistent HTTP connection. ' +
    'Try asking about Kotlin, Compose, SSE, Android, Node, or streaming!';
}

// ---------------------------------------------------------------------------
// HTTP server
// ---------------------------------------------------------------------------
const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);

  // Health-check endpoint
  if (parsed.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('SSE server is running. Use GET /sse?query=<text>\n');
    return;
  }

  if (parsed.pathname !== '/sse') {
    res.writeHead(404);
    res.end();
    return;
  }

  // SSE headers
  res.writeHead(200, {
    'Content-Type':                'text/event-stream',
    'Cache-Control':               'no-cache',
    'Connection':                  'keep-alive',
    'Access-Control-Allow-Origin': '*',
  });

  const query    = parsed.query.query || '';
  const response = pickResponse(query);

  // Split into words, preserving spaces as separate tokens so the Android
  // side gets the same word-by-word streaming effect as a real LLM backend.
  const rawTokens = response.split(' ');
  const tokens    = rawTokens.flatMap((word, i) =>
    i < rawTokens.length - 1 ? [word, ' '] : [word]
  );

  let index = 0;

  const interval = setInterval(() => {
    if (index < tokens.length) {
      // SSE event: "data: <token>\n\n"
      res.write(`data: ${tokens[index]}\n\n`);
      index++;
    } else {
      // Signal end-of-stream
      res.write('data: [DONE]\n\n');
      clearInterval(interval);
      res.end();
    }
  }, 80); // 80 ms between tokens → ~12 tokens/sec

  // Clean up if the client disconnects early
  req.on('close', () => clearInterval(interval));
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`SSE server listening on http://localhost:${PORT}`);
  console.log(`Test: curl -N "http://localhost:${PORT}/sse?query=hello"`);
});
