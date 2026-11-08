import { api, state, notify } from "./core.js";
import { loadFarms } from "./farms.js";
import { connectLiveUpdates } from "./realtime.js";

export function bindSessionEvents() {
  document.querySelector("#profile-button").addEventListener("click", (event) => {
    const popover = document.querySelector("#profile-popover");
    popover.hidden = !popover.hidden;
    event.currentTarget.setAttribute("aria-expanded", String(!popover.hidden));
  });
  document.querySelector("#logout-button").addEventListener("click", async () => {
    try {
      await api("/api/auth/logout", { method: "POST", body: "{}" });
    } finally {
      window.location.assign("/");
    }
  });
}

export async function startApp() {
  try {
    state.user = await api("/api/current-user");
    if (!state.user.authenticated) {
      window.location.assign("/");
      return;
    }
    document.querySelector("#profile-name").textContent =
      state.user.fullname || state.user.username;
    document.querySelector("#profile-role").textContent = state.user.role;
    document.querySelector("#profile-email").textContent =
      state.user.email || "";
    document.querySelector("#profile-initial").textContent = (
      state.user.fullname ||
      state.user.username ||
      "U"
    )
      .trim()
      .charAt(0)
      .toUpperCase();
    document.querySelector("#welcome-copy").textContent =
      `Xin chào ${state.user.fullname || state.user.username}, đây là tình trạng mới nhất của trang trại.`;
    document.querySelector(".admin-link").hidden = state.user.role !== "ADMIN";
    await loadFarms();
    connectLiveUpdates();
  } catch (error) {
    if (error instanceof TypeError)
      notify("Không kết nối được máy chủ. Hãy khởi động lại backend.");
    else window.location.assign("/");
  }
}
