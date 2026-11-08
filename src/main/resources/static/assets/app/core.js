// Shared API client, application state and UI helpers.

export const api = async (url, options = {}) => {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new Error(body.message || body.error || "Yêu cầu thất bại.");
  return body;
};

export const state = {
  user: null,
  farms: [],
  farmId: null,
  dashboard: null,
  editingFarmId: null,
  editingDeviceId: null,
  period: "day",
};
const pageAlert = document.querySelector("#page-alert");
export const escapeHtml = (value = "") =>
  String(value).replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );

export function notify(message, success = false) {
  pageAlert.textContent = message;
  pageAlert.classList.toggle("success", success);
  pageAlert.hidden = false;
  window.setTimeout(() => {
    pageAlert.hidden = true;
  }, 4500);
}
