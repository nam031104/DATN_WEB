import { api, escapeHtml, notify } from "./core.js";

export async function renderNotifications() {
  const target = document.querySelector("#notification-list");
  try {
    const notifications = await api("/api/notifications");
    const unread = notifications.filter((item) => item.status !== 1).length;
    const count = document.querySelector("#notification-count");
    count.textContent = unread;
    count.hidden = unread === 0;
    target.innerHTML = notifications.length
      ? notifications
          .map(
            (item) =>
              `<article class="notification-item"><span class="notification-mark" aria-hidden="true">!</span><div><strong>${item.status === 1 ? "Đã xem" : "Chưa xem"}</strong><p>${escapeHtml(item.message)}</p><time>${item.createdAt ? new Date(item.createdAt).toLocaleString("vi-VN") : ""}</time></div><button class="table-action" type="button" data-notification-read="${item.notificationId}" ${item.status === 1 ? "disabled" : ""}>${item.status === 1 ? "Đã đọc" : "Đánh dấu đã đọc"}</button></article>`,
          )
          .join("")
      : '<div class="empty-state">Chưa có thông báo.</div>';
  } catch (error) {
    target.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
  }
}

export function bindNotificationEvents() {
  document
    .querySelector("#notification-list")
    .addEventListener("click", async (event) => {
      const button = event.target.closest("[data-notification-read]");
      if (!button) return;
      try {
        await api(`/api/notifications/${button.dataset.notificationRead}/read`, {
          method: "PATCH",
          body: "{}",
        });
        await renderNotifications();
      } catch (error) {
        notify(error.message);
      }
    });
}
