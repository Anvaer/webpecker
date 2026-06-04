import { describe, expect, it, vi } from "vitest";
import { useWebSocket } from "../websocket.api";

const createWebSocketMock = () => {
  let instance;
  const WebSocketMock = vi.fn((url) => {
    instance = {
      url,
      readyState: WebSocketMock.OPEN,
      onopen: null,
      onerror: null,
      onmessage: null,
      send: vi.fn(),
    };
    return instance;
  });
  WebSocketMock.OPEN = 1;
  WebSocketMock.CLOSED = 3;
  return { WebSocketMock, getInstance: () => instance };
};

describe("useWebSocket", () => {
  it("opens socket and calls onOpen once connected", async () => {
    const { WebSocketMock, getInstance } = createWebSocketMock();
    global.WebSocket = WebSocketMock;

    const onOpen = vi.fn();
    useWebSocket("ws://localhost/req", { onOpen });

    const ws = getInstance();
    expect(ws.url).toBe("ws://localhost/req");

    ws.onopen();
    await Promise.resolve();

    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it("reuses open socket for sending messages", async () => {
    const { WebSocketMock, getInstance } = createWebSocketMock();
    global.WebSocket = WebSocketMock;

    const { sendMessage } = useWebSocket("ws://localhost/req", {});
    const ws = getInstance();

    ws.onopen();
    await Promise.resolve();

    sendMessage("ping");

    expect(ws.send).toHaveBeenCalledWith("ping");
  });

  it("reopens socket when closed before sending", async () => {
    const { WebSocketMock, getInstance } = createWebSocketMock();
    global.WebSocket = WebSocketMock;

    const { sendMessage } = useWebSocket("ws://localhost/req", {});
    const first = getInstance();

    first.onopen();
    await Promise.resolve();

    first.readyState = WebSocketMock.CLOSED;
    sendMessage("ping");

    const second = getInstance();
    expect(second).not.toBe(first);

    second.onopen();
    await Promise.resolve();

    expect(second.send).toHaveBeenCalledWith("ping");
  });
});
