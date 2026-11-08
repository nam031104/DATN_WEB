import { state } from "./core.js";
import { loadDashboard } from "./dashboard.js";
import { renderFarmDevices } from "./devices.js";
import { renderFarms } from "./farms.js";
import { renderHistorySensors } from "./history.js";
import { renderSchedules } from "./schedules.js";
import { renderNotifications } from "./notifications.js";

export function setPage(name) {
  document.querySelectorAll("[data-view]").forEach((view) => {
    view.hidden = view.dataset.view !== name;
  });
  document
    .querySelectorAll(".nav-link[data-page]")
    .forEach((link) =>
      link.classList.toggle("is-active", link.dataset.page === name),
    );
  const titles = {
    overview: "Tổng quan",
    farms: "Khu vực chuồng",
    devices: "Thiết bị",
    schedules: "Lịch điều khiển",
    history: "Lịch sử dữ liệu",
    notifications: "Thông báo",
  };
  document.title = `Chicken Monitor | ${titles[name] || "Tổng quan"}`;
  if (name === "farms") renderFarms();
  if (name === "devices") renderFarmDevices();
  if (name === "history") renderHistorySensors();
  if (name === "schedules") renderSchedules();
  if (name === "notifications") renderNotifications();
}

export function bindNavigationEvents() {
  document
    .querySelectorAll(".nav-link[data-page]")
    .forEach((button) =>
      button.addEventListener("click", () => setPage(button.dataset.page)),
    );
  document
    .querySelectorAll("[data-go]")
    .forEach((button) =>
      button.addEventListener("click", () => setPage(button.dataset.go)),
    );
  document
    .querySelector("#farm-select")
    .addEventListener("change", async (event) => {
      state.farmId = Number(event.target.value) || null;
      await loadDashboard();
      if (!document.querySelector('[data-view="devices"]').hidden) {
        await renderFarmDevices();
      }
    });
}
