import { api, state, escapeHtml, notify } from "./core.js";
import { renderHistorySensors } from "./history.js";

export async function loadDashboard() {
  const sensorGrid = document.querySelector("#sensor-grid");
  if (!state.farmId) {
    state.dashboard = null;
    document.querySelector("#device-total").textContent = "0";
    document.querySelector("#sensor-total").textContent = "0";
    document.querySelector("#actor-total").textContent = "0";
    sensorGrid.innerHTML =
      '<div class="empty-state">Chưa có khu vực chuồng. Tạo khu vực đầu tiên để bắt đầu.</div>';
    document.querySelector("#actor-list").innerHTML =
      '<div class="empty-state">Chưa có thiết bị chấp hành.</div>';
    renderHistorySensors();
    return;
  }
  try {
    state.dashboard = await api(
      `/api/dashboard?farmId=${encodeURIComponent(state.farmId)}`,
    );
    renderDashboard(state.dashboard);
    document.querySelector("#connection-label").textContent =
      "Đã kết nối máy chủ";
    document.querySelector("#last-updated").textContent =
      `Cập nhật ${new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}`;
  } catch (error) {
    notify(error.message);
    sensorGrid.innerHTML =
      '<div class="empty-state">Không tải được dữ liệu khu vực.</div>';
  }
}

export function renderDashboard(data) {
  const sensors = data.sensors || [];
  const actors = data.actors || [];
  const cameras = (data.devices || []).filter(
    (device) =>
      device.deviceCode &&
      `${device.name} ${device.typeDevice}`.toLowerCase().includes("camera"),
  );
  document.querySelector("#device-total").textContent =
    data.devices?.length || 0;
  document.querySelector("#sensor-total").textContent = sensors.filter(
    (sensor) => sensor.value !== null,
  ).length;
  document.querySelector("#actor-total").textContent = actors.length;
  document.querySelector("#actor-status").textContent =
    `${actors.filter((actor) => actor.status === 1).length} đang bật`;
  const colors = ["#75a05f", "#c3973d", "#4f8790", "#b6654f"];
  document.querySelector("#sensor-grid").innerHTML = sensors.length
    ? sensors
        .map(
          (sensor, index) =>
            `<article class="sensor-card" style="--sensor-color:${colors[index % colors.length]}"><span class="sensor-name">${escapeHtml(sensor.typeSensor)} · ${escapeHtml(sensor.deviceName)}</span><div class="sensor-reading"><strong class="${sensor.value === null ? "sensor-empty-value" : ""}">${sensor.value === null ? "Chưa có" : escapeHtml(sensor.value)}</strong><span>${escapeHtml(sensor.unit || "")}</span></div><small class="sensor-time">${sensor.createdAt ? new Date(sensor.createdAt).toLocaleString("vi-VN") : "Đang chờ dữ liệu"}</small></article>`,
        )
        .join("")
    : '<div class="empty-state">Chưa có cảm biến trong khu vực này.</div>';
  document.querySelector("#actor-list").innerHTML = actors.length
    ? actors
        .map(
          (actor) =>
            `<article class="actor-card"><span class="actor-glyph" aria-hidden="true">${actorGlyph(actor.typeActor)}</span><div class="actor-info"><strong>${escapeHtml(actor.typeActor)}</strong><small>${escapeHtml(actor.deviceName)} · ${actor.mode === 1 ? "Thủ công" : "Tự động"}</small></div><span class="actor-state ${actor.status === 1 ? "is-on" : ""}">${actor.status === 1 ? "ĐANG BẬT" : "ĐANG TẮT"}</span><div class="actor-actions">${actor.mode === 1 ? `<button type="button" data-actor-toggle="${actor.actorId}" data-next-status="${actor.status === 1 ? 0 : 1}">${actor.status === 1 ? "Tắt" : "Bật"}</button>` : ""}<button type="button" data-actor-config="${actor.actorId}">${actor.mode === 1 ? "Chuyển tự động" : "Cấu hình"}</button></div></article>`,
        )
        .join("")
    : '<div class="empty-state">Chưa có actor nào được ghép nối.</div>';
  document.querySelector("#camera-grid").innerHTML = cameras.length
    ? cameras
        .map(
          (camera, index) =>
            `<button class="camera-tile" type="button" data-camera-device="${camera.deviceId}"><span class="camera-feed ${index % 2 ? "camera-feed-alt" : ""}"><span class="camera-crosshair"></span><span class="camera-state">THEO YÊU CẦU</span></span><span class="camera-caption"><strong>${escapeHtml(camera.name)}</strong><small>Nhấn để yêu cầu luồng video</small></span></button>`,
        )
        .join("")
    : '<div class="empty-state camera-empty">Chưa có camera được ghép vào khu vực này.</div>';
}

function actorGlyph(type = "") {
  const normalized = type.toLowerCase();
  if (normalized.includes("pump")) return "◉";
  if (normalized.includes("fan")) return "✣";
  if (normalized.includes("light")) return "☼";
  return "⌁";
}
