import { api, escapeHtml, notify } from "./core.js";

let notificationItems = [];
let currentFilter = "all";
let isUpdating = false;
let pollingStarted = false;

const ICONS = {
    login: '<path d="M10 17l5-5-5-5"/><path d="M15 12H3"/><path d="M12 3h6a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3h-6"/>',
    temperature: '<path d="M14 14.76V5a3 3 0 0 0-6 0v9.76a5 5 0 1 0 6 0Z"/><path d="M11 10v7"/>',
    disconnect: '<path d="M2 8.82a15 15 0 0 1 4.18-2.55"/><path d="M9.7 5.08A15 15 0 0 1 22 8.82"/><path d="M5 12.86a10 10 0 0 1 5.17-2.62"/><path d="M14.67 10.57A10 10 0 0 1 19 12.86"/><path d="M8.5 16.43a5 5 0 0 1 7 0"/><path d="M12 20h.01"/><path d="m2 2 20 20"/>',
    reconnect: '<path d="M2 8.82a15 15 0 0 1 20 0"/><path d="M5 12.86a10 10 0 0 1 14 0"/><path d="M8.5 16.43a5 5 0 0 1 7 0"/><path d="M12 20h.01"/>',
    claim: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
    default: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/>'
};

function classifyNotification(message) {
    const text = String(message || "").toLocaleLowerCase("vi-VN");
    if (text.includes("đăng nhập thành công"))
        return { title: "Đăng nhập thành công", type: "system", icon: "login", badge: "Hệ thống" };
    if (text.includes("mất kết nối") || text.includes("mất tín hiệu"))
        return { title: "Thiết bị mất kết nối", type: "warning", icon: "disconnect", badge: "Cảnh báo" };
    if (text.includes("nhiệt độ") && (text.includes("vượt") || text.includes("cảnh báo") || text.includes("cao")))
        return { title: "Nhiệt độ vượt ngưỡng", type: "warning", icon: "temperature", badge: "Cảnh báo" };
    if (text.includes("độ ẩm") && (text.includes("vượt") || text.includes("cảnh báo") || text.includes("thấp")))
        return { title: "Độ ẩm bất thường", type: "warning", icon: "temperature", badge: "Cảnh báo" };
    if (text.includes("kết nối trở lại") || text.includes("đã kết nối") || text.includes("hoạt động trở lại"))
        return { title: "Thiết bị đã kết nối", type: "system", icon: "reconnect", badge: "Hệ thống" };
    if (text.includes("ghép mã") || text.includes("ghép thiết bị"))
        return { title: "Ghép mã thiết bị thành công", type: "system", icon: "claim", badge: "Hệ thống" };
    if (text.includes("cảnh báo") || text.includes("vượt ngưỡng"))
        return { title: "Cảnh báo trang trại", type: "warning", icon: "temperature", badge: "Cảnh báo" };
    return { title: "Cập nhật hệ thống", type: "system", icon: "default", badge: "Hệ thống" };
}

function isUnread(item) {
    return Number(item?.status) !== 1;
}

function formatDate(value) {
    if (!value) return "";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? ""
        : date.toLocaleString("vi-VN", {
            day: "2-digit", month: "2-digit", year: "numeric",
            hour: "2-digit", minute: "2-digit"
        });
}

function updateCounters() {
    const unreadItems = notificationItems.filter(isUnread);
    const warningItems = notificationItems.filter(
        item => classifyNotification(item.message).type === "warning"
    );
    const unread = unreadItems.length;
    const warning = warningItems.length;

    const navCount = document.querySelector("#notification-count");
    if (navCount) {
        navCount.textContent = unread > 99 ? "99+" : String(unread);
        navCount.hidden = unread === 0;
    }

    const alertTotal = document.querySelector("#alert-total");
    if (alertTotal) alertTotal.textContent = String(unreadItems.filter(
        item => classifyNotification(item.message).type === "warning"
    ).length);

    const totalCount = document.querySelector("#notification-total-count");
    const unreadCount = document.querySelector("#notification-unread-count");
    const warningCount = document.querySelector("#notification-warning-count");
    if (totalCount) totalCount.textContent = String(notificationItems.length);
    if (unreadCount) unreadCount.textContent = String(unread);
    if (warningCount) warningCount.textContent = String(warning);

    const readAllButton = document.querySelector("#notification-read-all");
    if (readAllButton) readAllButton.disabled = unread === 0;
}

function renderList() {
    const target = document.querySelector("#notification-list");
    if (!target) return;

    const filtered = notificationItems.filter(item => {
        const kind = classifyNotification(item.message);
        if (currentFilter === "unread") return isUnread(item);
        if (currentFilter === "warning") return kind.type === "warning";
        if (currentFilter === "system") return kind.type === "system";
        return true;
    });

    target.innerHTML = filtered.length
        ? filtered.map(item => {
            const kind = classifyNotification(item.message);
            const unread = isUnread(item);
            return `<article class="notice-card ${kind.type === "warning" ? "is-warning" : ""} ${unread ? "is-unread" : "is-read"}">
          <span class="notice-icon" aria-hidden="true"><svg viewBox="0 0 24 24">${ICONS[kind.icon] || ICONS.default}</svg></span>
          <div class="notice-body">
            <div class="notice-topline">
              <strong>${escapeHtml(kind.title)}</strong>
              <span class="notice-badge ${kind.type === "warning" ? "is-alert" : ""}">${escapeHtml(kind.badge)}</span>
            </div>
            <p>${escapeHtml(item.message || "")}</p>
            <time>${escapeHtml(formatDate(item.createdAt))}</time>
          </div>
          ${unread ? `<div class="notice-actions"><span class="notice-unread-dot" title="Chưa đọc"></span><button class="notice-read-button" type="button" data-notification-read="${Number(item.notificationId)}" title="Đánh dấu đã đọc" aria-label="Đánh dấu thông báo đã đọc">✓</button></div>` : ""}
        </article>`;
        }).join("")
        : `<div class="empty-state">${notificationItems.length ? "Không có thông báo phù hợp." : "Chưa có thông báo."}</div>`;

    updateCounters();
}

async function fetchNotifications(showError = false) {
    if (isUpdating) return;
    isUpdating = true;
    try {
        const result = await api("/api/notifications");
        notificationItems = Array.isArray(result) ? result : [];
        updateCounters();
        const page = document.querySelector('[data-view="notifications"]');
        if (page && !page.hidden) renderList();
    } catch (error) {
        if (showError) {
            const target = document.querySelector("#notification-list");
            if (target) target.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
        }
    } finally {
        isUpdating = false;
    }
}

export async function renderNotifications() {
    await fetchNotifications(true);
}

export function startNotificationPolling() {
    if (pollingStarted) return;
    pollingStarted = true;
    fetchNotifications();
    window.setInterval(() => {
        if (!document.hidden) fetchNotifications();
    }, 20000);
}

export function bindNotificationEvents() {
    const filters = document.querySelector(".notification-filters");
    if (filters) {
        filters.addEventListener("click", event => {
            const button = event.target.closest("[data-notification-filter]");
            if (!button) return;
            currentFilter = button.dataset.notificationFilter;
            filters.querySelectorAll("[data-notification-filter]").forEach(filter => {
                const active = filter === button;
                filter.classList.toggle("is-active", active);
                filter.setAttribute("aria-pressed", String(active));
            });
            renderList();
        });
    }

    const list = document.querySelector("#notification-list");
    if (list) {
        list.addEventListener("click", async event => {
            const button = event.target.closest("[data-notification-read]");
            if (!button) return;
            button.disabled = true;
            try {
                await api(`/api/notifications/${button.dataset.notificationRead}/read`, {
                    method: "PATCH", body: "{}"
                });
                await fetchNotifications(true);
            } catch (error) {
                notify(error.message);
                button.disabled = false;
            }
        });
    }

    const readAllButton = document.querySelector("#notification-read-all");
    if (readAllButton) {
        readAllButton.addEventListener("click", async () => {
            readAllButton.disabled = true;
            try {
                await api("/api/notifications/read-all", { method: "PATCH", body: "{}" });
                await fetchNotifications(true);
                notify("Đã đánh dấu tất cả thông báo là đã đọc.", true);
            } catch (error) {
                notify(error.message);
                readAllButton.disabled = false;
            }
        });
    }
}
