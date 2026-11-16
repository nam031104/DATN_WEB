
import { api, state, escapeHtml, notify } from "./core.js";
import { renderHistorySensors } from "./history.js";

/**
 * Tải dữ liệu Dashboard từ Backend.
 */
export async function loadDashboard() {
  const sensorGrid = document.querySelector("#sensor-grid");
  const actorList = document.querySelector("#actor-list");

  if (!sensorGrid || !actorList) {
    console.error("[Dashboard] Không tìm thấy vùng hiển thị.");
    return;
  }

  // Chưa chọn Farm
  if (!state.farmId) {
    state.dashboard = null;

    setText("#device-total", "0");
    setText("#sensor-total", "0");
    setText("#actor-total", "0");
    setText("#actor-status", "0 đang bật");

    sensorGrid.innerHTML =
      '<div class="empty-state">Chưa có khu vực chuồng. Tạo khu vực đầu tiên để bắt đầu.</div>';

    actorList.innerHTML =
      '<div class="empty-state">Chưa có thiết bị chấp hành.</div>';

    const cameraGrid = document.querySelector("#camera-grid");
    if (cameraGrid) {
      cameraGrid.innerHTML =
        '<div class="empty-state camera-empty">Chưa có camera được ghép vào khu vực này.</div>';
    }

    renderHistorySensors();
    return;
  }

  try {
    console.log("[Dashboard] Đang tải dữ liệu...");

    const data = await api(
      `/api/dashboard?farmId=${encodeURIComponent(state.farmId)}`
    );

    // Tránh hiển thị dữ liệu của Farm cũ nếu người dùng đổi Farm
    if (!data) {
      throw new Error("Backend không trả về dữ liệu Dashboard.");
    }

    state.dashboard = data;

    renderDashboard(data);

    setText("#connection-label", "Đã kết nối máy chủ");

    setText(
      "#last-updated",
      `Cập nhật ${new Date().toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
      })}`
    );

    // Cập nhật lịch sử cảm biến
    renderHistorySensors();

    console.log("[Dashboard] Đã cập nhật thành công.");

  } catch (error) {
    console.error("[Dashboard] Lỗi tải dữ liệu:", error);

    notify(error.message || "Không tải được dữ liệu Dashboard.");

    sensorGrid.innerHTML =
      '<div class="empty-state">Không tải được dữ liệu khu vực.</div>';
  }
}

/**
 * Render toàn bộ Dashboard.
 */
export function renderDashboard(data) {
  const sensors = data.sensors || [];
  const actors = data.actors || [];
  const devices = data.devices || [];

  // Lọc Camera
  const cameras = devices.filter(
    (device) =>
      device.deviceCode &&
      `${device.name || ""} ${device.typeDevice || ""}`
        .toLowerCase()
        .includes("camera")
  );

  // Thống kê
  setText("#device-total", devices.length);

  setText(
    "#sensor-total",
    sensors.filter((sensor) => sensor.value !== null && sensor.value !== undefined).length
  );

  setText("#actor-total", actors.length);

  setText(
    "#actor-status",
    `${actors.filter((actor) => Number(actor.status) === 1).length} đang bật`
  );

  // Render Sensor
  renderSensors(sensors);

  // Render Actor
  renderActors(actors);

  // Render Camera
  renderCameras(cameras);
}

/**
 * Render danh sách cảm biến.
 */
function renderSensors(sensors) {
  const sensorGrid = document.querySelector("#sensor-grid");

  if (!sensorGrid) return;

  const colors = [
    "#75a05f",
    "#c3973d",
    "#4f8790",
    "#b6654f"
  ];

  if (!sensors.length) {
    sensorGrid.innerHTML =
      '<div class="empty-state">Chưa có cảm biến trong khu vực này.</div>';
    return;
  }

  sensorGrid.innerHTML = sensors
    .map((sensor, index) => {
      const value =
        sensor.value === null || sensor.value === undefined
          ? "Chưa có"
          : escapeHtml(sensor.value);

      const emptyClass =
        sensor.value === null || sensor.value === undefined
          ? "sensor-empty-value"
          : "";

      const sensorName = escapeHtml(sensor.typeSensor || "Cảm biến");
      const deviceName = escapeHtml(sensor.deviceName || "Thiết bị");
      const unit = escapeHtml(sensor.unit || "");

      const sensorTime = sensor.createdAt
        ? new Date(sensor.createdAt).toLocaleString("vi-VN")
        : "Đang chờ dữ liệu";

      return `
        <article
          class="sensor-card"
          style="--sensor-color:${colors[index % colors.length]}"
        >
          <span class="sensor-name">
            ${sensorName} · ${deviceName}
          </span>

          <div class="sensor-reading">
            <strong class="${emptyClass}">
              ${value}
            </strong>

            <span>${unit}</span>
          </div>

          <small class="sensor-time">
            ${escapeHtml(sensorTime)}
          </small>
        </article>
      `;
    })
    .join("");
}

/**
 * Render danh sách Actor.
 */
function renderActors(actors) {
  const actorList = document.querySelector("#actor-list");

  if (!actorList) return;

  if (!actors.length) {
    actorList.innerHTML =
      '<div class="empty-state">Chưa có actor nào được ghép nối.</div>';
    return;
  }

  actorList.innerHTML = actors
    .map((actor) => {
      const mode = Number(actor.mode);
      const status = Number(actor.status);

      const isOn = status === 1;

      // Hiển thị tên chế độ
      const modeName =
        mode === 0
          ? "Tự động"
          : mode === 1
            ? "Thủ công"
            : mode === 2
              ? "Cố định"
              : "Chưa xác định";

      // Mode 2 không được phép thay đổi cấu hình
      const configButton =
        mode !== 2
          ? `
            <button
              type="button"
              data-actor-config="${actor.actorId}"
            >
              ${mode === 1 ? "Chuyển tự động" : "Cấu hình"}
            </button>
          `
          : "";

      return `
        <article class="actor-card">

          <span class="actor-glyph" aria-hidden="true">
            ${actorGlyph(actor.typeActor)}
          </span>

          <div class="actor-info">
            <strong>
              ${escapeHtml(actor.typeActor || "Actor")}
            </strong>

            <small>
              ${escapeHtml(actor.deviceName || "Thiết bị")}
              · ${modeName}
            </small>
          </div>

          <span class="actor-state ${isOn ? "is-on" : ""}">
            ${isOn ? "ĐANG BẬT" : "ĐANG TẮT"}
          </span>

          <div class="actor-actions">

            <button
              type="button"
              data-actor-toggle="${actor.actorId}"
              data-next-status="${isOn ? 0 : 1}"
            >
              ${isOn ? "Tắt" : "Bật"}
            </button>

            ${configButton}

          </div>

        </article>
      `;
    })
    .join("");
}

/**
 * Render danh sách Camera.
 */
function renderCameras(cameras) {
  const cameraGrid = document.querySelector("#camera-grid");

  if (!cameraGrid) return;

  if (!cameras.length) {
    cameraGrid.innerHTML =
      '<div class="empty-state camera-empty">Chưa có camera được ghép vào khu vực này.</div>';
    return;
  }

  cameraGrid.innerHTML = cameras
    .map(
      (camera, index) => `
        <button
          class="camera-tile"
          type="button"
          data-camera-device="${camera.deviceId}"
        >

          <span class="camera-feed ${
            index % 2 ? "camera-feed-alt" : ""
          }">

            <span class="camera-crosshair"></span>

            <span class="camera-state">
              THEO YÊU CẦU
            </span>

          </span>

          <span class="camera-caption">

            <strong>
              ${escapeHtml(camera.name || "Camera")}
            </strong>

            <small>
              Nhấn để yêu cầu luồng video
            </small>

          </span>

        </button>
      `
    )
    .join("");
}

/**
 * Icon cho Actor.
 */
function actorGlyph(type = "") {
  const normalized = type.toLowerCase();

  if (normalized.includes("pump")) return "◉";
  if (normalized.includes("fan")) return "✣";
  if (normalized.includes("light")) return "☼";

  return "⌁";
}

/**
 * Cập nhật text an toàn.
 */
function setText(selector, value) {
  const element = document.querySelector(selector);

  if (element) {
    element.textContent = value;
  }
}
