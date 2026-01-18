export const useWebSocket = (
  url,
  { onMessage = () => {}, onOpen = () => {} }
) => {
  let websocket;

  const openSocket = () => {
    return new Promise(function (resolve, reject) {
      var server = new WebSocket(url);
      server.onopen = function () {
        resolve(server);
      };
      server.onerror = function (err) {
        reject(err);
      };
    });
  };

  const sendMessage = (m) => {
    if (!websocket || websocket.readyState === WebSocket.CLOSED) {
      openSocket().then((ws) => {
        websocket = ws;
        websocket.onmessage = onMessage;
        websocket.send(m);
      });
    } else websocket.send(m);
  };

  openSocket().then((ws) => {
    websocket = ws;
    websocket.onmessage = onMessage;
    onOpen();
  });

  return { sendMessage };
};
