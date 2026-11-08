import { state } from "./core.js";
import { loadDashboard } from "./dashboard.js";

let liveSocket = null;

export function connectLiveUpdates() {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  liveSocket = new WebSocket(`${protocol}//${window.location.host}/ws/live`);
  liveSocket.addEventListener("open", () => {
    document.querySelector("#connection-label").textContent =
      "Realtime đã kết nối";
  });
  liveSocket.addEventListener("message", async (event) => {
    try {
      const message = JSON.parse(event.data);
      if (
        message.farmId === state.farmId &&
        ["telemetry", "device-registered"].includes(message.type)
      ) {
        await loadDashboard();
      }
    } catch (error) {
      console.warn("Không đọc được bản tin realtime", error);
    }
  });
  liveSocket.addEventListener("close", () => {
    document.querySelector("#connection-label").textContent =
      "Realtime ngắt kết nối";
    window.setTimeout(() => {
      if (state.user?.authenticated) connectLiveUpdates();
    }, 10000);
  });
}
