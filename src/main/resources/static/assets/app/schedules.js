import { api, state, escapeHtml, notify } from "./core.js";

export async function renderSchedules() {
  const target = document.querySelector("#schedule-list");
  try {
    const schedules = await api("/api/schedules");
    target.innerHTML = schedules.length
      ? `<table class="data-table"><thead><tr><th>THIẾT BỊ</th><th>GIỜ</th><th>HÀNH ĐỘNG</th><th>GHI CHÚ</th><th></th></tr></thead><tbody>${schedules.map((item) => `<tr><td>${escapeHtml(item.actorType)} · ${escapeHtml(item.deviceName)}</td><td>${escapeHtml(item.executionTime)}</td><td>${item.status === 1 ? "Bật" : "Tắt"}</td><td>${escapeHtml(item.note || "")}</td><td><button class="table-action" type="button" data-schedule-delete="${item.scheduleId}">Xóa</button></td></tr>`).join("")}</tbody></table>`
      : '<div class="empty-state">Chưa có lịch điều khiển nào.</div>';
    const actors = state.dashboard?.actors || [];
    document.querySelector("#schedule-form [name=actorId]").innerHTML = actors
      .map(
        (actor) =>
          `<option value="${actor.actorId}">${escapeHtml(actor.typeActor)} · ${escapeHtml(actor.deviceName)}</option>`,
      )
      .join("");
  } catch (error) {
    target.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
  }
}

export function bindScheduleEvents() {
  document.querySelector("#add-schedule-button").addEventListener("click", () => {
    const form = document.querySelector("#schedule-form");
    const actors = state.dashboard?.actors || [];
    form.elements.actorId.innerHTML = actors
      .map(
        (actor) =>
          `<option value="${actor.actorId}">${escapeHtml(actor.typeActor)} · ${escapeHtml(actor.deviceName)}</option>`,
      )
      .join("");
    document.querySelector("#schedule-form-panel").hidden = false;
    document
      .querySelector("#schedule-form-panel")
      .scrollIntoView({ behavior: "smooth", block: "nearest" });
  });
  document
    .querySelector("#cancel-schedule-button")
    .addEventListener("click", () => {
      document.querySelector("#schedule-form-panel").hidden = true;
    });
  document
    .querySelector("#schedule-form")
    .addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const payload = Object.fromEntries(new FormData(form));
      payload.actorId = Number(payload.actorId);
      payload.status = Number(payload.status);
      try {
        await api("/api/schedules", {
          method: "POST",
          body: JSON.stringify(payload),
        });
        form.reset();
        document.querySelector("#schedule-form-panel").hidden = true;
        await renderSchedules();
        notify("Đã tạo lịch điều khiển hằng ngày.", true);
      } catch (error) {
        notify(error.message);
      }
    });
  document
    .querySelector("#schedule-list")
    .addEventListener("click", async (event) => {
      const button = event.target.closest("[data-schedule-delete]");
      if (!button || !window.confirm("Xóa lịch điều khiển này?")) return;
      try {
        await api(`/api/schedules/${button.dataset.scheduleDelete}`, {
          method: "DELETE",
        });
        await renderSchedules();
        notify("Đã xóa lịch.", true);
      } catch (error) {
        notify(error.message);
      }
    });
}
