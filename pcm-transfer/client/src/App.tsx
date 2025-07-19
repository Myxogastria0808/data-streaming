import { useEffect, useRef, useState } from "react";

const SAMPLE_RATE = 44100;
const CHANNELS = 2; // ← WAVがモノラルなら 1 にする
const SAMPLE_SIZE = 2; // 16bit PCM = 2 byte/sample
const MIN_BUFFER_COUNT = 4; // 再生を始める最低バッファ数
const MAX_QUEUE_SIZE = 12; // バッファキュー最大数

function App() {
  const audioCtxRef = useRef<AudioContext | null>(null);
  const bufferQueueRef = useRef<AudioBuffer[]>([]);
  const isPlayingRef = useRef(false);
  const playbackTimeRef = useRef(0);
  const wsRef = useRef<WebSocket | null>(null);
  const [connected, setConnected] = useState(false);
  const [bufferCount, setBufferCount] = useState(0);

  useEffect(() => {
    return () => {
      wsRef.current?.close();
    };
  }, []);

  const handleConnect = () => {
    if (connected) return;

    const ws = new WebSocket("ws://localhost:7000/ws");
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
      ws.send("start");
      setConnected(true);
    };

    ws.onmessage = async (event) => {
      if (!(event.data instanceof Blob)) return;
      const arrayBuffer = await event.data.arrayBuffer();

      const audioCtx =
        audioCtxRef.current ?? new AudioContext({ sampleRate: SAMPLE_RATE });
      audioCtxRef.current = audioCtx;

      const pcmData = new DataView(arrayBuffer);
      const numSamples = pcmData.byteLength / SAMPLE_SIZE;
      const floatData = new Float32Array(numSamples);

      // Convert Int16 PCM to Float32
      for (let i = 0; i < numSamples; i++) {
        const int16 = pcmData.getInt16(i * 2, true); // little-endian
        floatData[i] = int16 > 0 ? int16 / 32767 : int16 / 32768;
      }

      // Split channels
      const frames = numSamples / CHANNELS;
      const buffer = audioCtx.createBuffer(CHANNELS, frames, SAMPLE_RATE);
      for (let ch = 0; ch < CHANNELS; ch++) {
        const channelData = new Float32Array(frames);
        for (let i = 0; i < frames; i++) {
          channelData[i] = floatData[i * CHANNELS + ch];
        }
        buffer.copyToChannel(channelData, ch);
      }

      bufferQueueRef.current.push(buffer);
      if (bufferQueueRef.current.length > MAX_QUEUE_SIZE) {
        bufferQueueRef.current.shift(); // discard oldest
      }

      setBufferCount(bufferQueueRef.current.length);
      scheduleBuffers();
    };

    ws.onerror = (e) => {
      console.error("WebSocket error:", e);
    };

    ws.onclose = () => {
      setConnected(false);
      console.log("WebSocket disconnected");
    };
  };

  const scheduleBuffers = () => {
    const audioCtx = audioCtxRef.current;
    if (!audioCtx || isPlayingRef.current) return;
    if (bufferQueueRef.current.length < MIN_BUFFER_COUNT) return;

    isPlayingRef.current = true;
    playbackTimeRef.current = Math.max(
      audioCtx.currentTime,
      playbackTimeRef.current
    );

    const playNext = () => {
      const buffer = bufferQueueRef.current.shift();
      if (!buffer) {
        isPlayingRef.current = false;
        return;
      }

      const source = audioCtx.createBufferSource();
      source.buffer = buffer;
      source.connect(audioCtx.destination);

      const startTime = Math.max(audioCtx.currentTime, playbackTimeRef.current);
      source.start(startTime);
      playbackTimeRef.current = startTime + buffer.duration;

      setBufferCount(bufferQueueRef.current.length);

      source.onended = () => {
        requestAnimationFrame(playNext);
      };
    };

    requestAnimationFrame(playNext);
  };

  return (
    <div>
      <h1>PCM Player (Buffered)</h1>
      <button onClick={handleConnect} disabled={connected}>
        {connected ? "Connected" : "Start Streaming"}
      </button>
      <p>Buffered Chunks: {bufferCount}</p>
    </div>
  );
}

export default App;
