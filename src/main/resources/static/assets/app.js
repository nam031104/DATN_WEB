const api = async (url, options = {}) => {
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

const state = {
  user: null,
  farms: [],
  farmId: null,
  dashboard: null,
  editingFarmId: null,
  editingDeviceId: null,
  period: "day",
};
const pageAlert = document.querySelector("#page-alert");
const escapeHtml = (value = "") =>
  String(value).replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );

function notify(message, success = false) {
  pageAlert.textContent = message;
  pageAlert.classList.toggle("success", success);
  pageAlert.hidden = false;
  window.setTimeout(() => {
    pageAlert.hidden = true;
  }, 4500);
}

function setPage(name) {
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

function renderFarmSelect() {
  const select = document.querySelector("#farm-select");
  select.innerHTML = state.farms.length
    ? state.farms
        .map(
          (farm) =>
            `<option value="${farm.farmId}">${escapeHtml(farm.name)}</option>`,
        )
        .join("")
    : '<option value="">Chưa có khu vực</option>';
  select.value = state.farmId || "";
}

async function loadFarms() {
  state.farms = await api("/api/farms");
  if (!state.farms.some((farm) => farm.farmId === state.farmId))
    state.farmId = state.farms[0]?.farmId || null;
  renderFarmSelect();
  await loadDashboard();
}

async function loadDashboard() {
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

function renderDashboard(data) {
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

function openActorConfig(actor) {
  const form = document.querySelector("#actor-config-form");
  form.elements.actorId.value = actor.actorId;
  form.elements.mode.value = String(actor.mode ?? 0);
  form.elements.sourceSensorCode.innerHTML = (state.dashboard?.sensors || [])
    .filter((sensor) => sensor.externalCode)
    .map(
      (sensor) =>
        `<option value="${escapeHtml(sensor.externalCode)}">${escapeHtml(sensor.typeSensor)} · ${escapeHtml(sensor.deviceName)}</option>`,
    )
    .join("");
  form.elements.sourceSensorCode.value =
    actor.sourceSensorCode ||
    form.elements.sourceSensorCode.options[0]?.value ||
    "";
  form.elements.thresholdMin.value = actor.thresholdMin ?? "";
  form.elements.thresholdMax.value = actor.thresholdMax ?? "";
  document.querySelector("#actor-config-title").textContent =
    `Cấu hình ${actor.typeActor} · ${actor.deviceName}`;
  form.hidden = false;
  syncActorModeFields();
  form.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function syncActorModeFields() {
  const automatic =
    document.querySelector("#actor-config-form [name=mode]").value === "0";
  document.querySelectorAll("[data-auto-field]").forEach((field) => {
    field.hidden = !automatic;
    const input = field.querySelector("input, select");
    if (input) input.required = automatic;
  });
}

function renderFarms() {
  const list = document.querySelector("#farm-list");
  list.innerHTML = state.farms.length
    ? state.farms
        .map(
          (farm) =>
            `<article class="farm-card"><div class="farm-card-head"><span class="farm-mark" aria-hidden="true">⌂</span><span class="small-status">${farm.farmId === state.farmId ? "ĐANG XEM" : "KHU VỰC"}</span></div><h3>${escapeHtml(farm.name)}</h3><p>${escapeHtml(farm.location || "Chưa thêm vị trí")}<br>${escapeHtml(farm.description || "")}</p><div class="farm-actions"><button type="button" data-farm-edit="${farm.farmId}">Chỉnh sửa</button><button type="button" data-farm-select="${farm.farmId}">Mở khu vực</button><button type="button" data-farm-delete="${farm.farmId}">Xóa</button></div></article>`,
        )
        .join("")
    : '<div class="empty-state">Bạn chưa tạo khu vực chuồng nào.</div>';
}

async function renderFarmDevices() {
  const target = document.querySelector("#farm-devices");
  const farm = state.farms.find((item) => item.farmId === state.farmId);
  document.querySelector("#device-farm-title").textContent = farm
    ? `Thiết bị · ${farm.name}`
    : "Thiết bị";
  if (!state.farmId) {
    target.innerHTML =
      '<div class="empty-state">Tạo hoặc chọn một khu vực trước.</div>';
    return;
  }
  try {
    const devices = await api(`/api/devices/farm/${state.farmId}`);
    target.innerHTML = devices.length
      ? `<table class="data-table"><thead><tr><th>TÊN</th><th>MÃ THIẾT BỊ</th><th>LOẠI</th><th>GHI CHÚ</th><th></th></tr></thead><tbody>${devices.map((device) => `<tr><td>${escapeHtml(device.name)}</td><td>${escapeHtml(device.deviceCode || "Chưa cấp mã")}</td><td>${escapeHtml(device.typeDevice)}</td><td>${escapeHtml(device.note || "")}</td><td><button class="table-action" type="button" data-device-edit="${device.deviceId}">Sửa</button><button class="table-action" type="button" data-device-delete="${device.deviceId}">Xóa</button></td></tr>`).join("")}</tbody></table>`
      : '<div class="empty-state">Khu vực này chưa có thiết bị.</div>';
  } catch (error) {
    target.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
  }
}

function openDeviceRecordForm(device = null) {
  state.editingDeviceId = device?.deviceId || null;
  const form = document.querySelector("#farm-device-form");
  form.reset();
  if (device) {
    form.elements.name.value = device.name || "";
    form.elements.typeDevice.value = device.typeDevice || "";
    form.elements.note.value = device.note || "";
  }
  document.querySelector("#farm-device-form-title").textContent = device
    ? "Cập nhật thiết bị"
    : "Thêm thiết bị";
  document.querySelector("#farm-device-form-panel").hidden = false;
  document
    .querySelector("#farm-device-form-panel")
    .scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function openFarmForm(farm = null) {
  state.editingFarmId = farm?.farmId || null;
  const panel = document.querySelector("#farm-form-panel");
  const form = document.querySelector("#farm-form");
  form.reset();
  if (farm) {
    form.elements.name.value = farm.name || "";
    form.elements.location.value = farm.location || "";
    form.elements.description.value = farm.description || "";
  }
  document.querySelector("#farm-form-title").textContent = farm
    ? "Cập nhật khu vực"
    : "Thêm khu vực chuồng";
  panel.hidden = false;
  panel.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function saveFarm(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const payload = Object.fromEntries(new FormData(form));
  const editingId = state.editingFarmId;
  try {
    const result = await api(
      editingId ? `/api/farms/${editingId}` : "/api/farms",
      {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(payload),
      },
    );
    if (!editingId) state.farmId = result.farmId;
    document.querySelector("#farm-form-panel").hidden = true;
    await loadFarms();
    setPage("farms");
    notify(editingId ? "Đã cập nhật khu vực." : "Đã tạo khu vực mới.", true);
  } catch (error) {
    notify(error.message);
  }
}

async function deleteFarm(id) {
  const farm = state.farms.find((item) => item.farmId === Number(id));
  if (
    !farm ||
    !window.confirm(`Xóa khu vực “${farm.name}” và dữ liệu thiết bị liên quan?`)
  )
    return;
  try {
    await api(`/api/farms/${id}`, { method: "DELETE" });
    if (state.farmId === Number(id)) state.farmId = null;
    await loadFarms();
    renderFarms();
    notify("Đã xóa khu vực.", true);
  } catch (error) {
    notify(error.message);
  }
}

function renderHistorySensors() {
  const sensors = state.dashboard?.sensors || [];
  document.querySelector("#history-sensor-list").innerHTML = sensors.length
    ? sensors
        .map(
          (sensor, index) =>
            `<button type="button" class="history-sensor-button" data-history-sensor="${sensor.sensorId}" style="--sensor-color:${["#75a05f", "#c3973d", "#4f8790", "#b6654f"][index % 4]}"><span></span>${escapeHtml(sensor.typeSensor)}</button>`,
        )
        .join("")
    : '<div class="empty-state">Chưa có dữ liệu cảm biến để thống kê.</div>';
}

async function renderSchedules() {
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

async function renderNotifications() {
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
document
  .querySelector("#add-farm-button")
  .addEventListener("click", () => openFarmForm());
document.querySelector("#cancel-farm-button").addEventListener("click", () => {
  document.querySelector("#farm-form-panel").hidden = true;
});
document.querySelector("#farm-form").addEventListener("submit", saveFarm);
document
  .querySelector("#farm-list")
  .addEventListener("click", async (event) => {
    const edit = event.target.closest("[data-farm-edit]");
    const select = event.target.closest("[data-farm-select]");
    const remove = event.target.closest("[data-farm-delete]");
    if (edit)
      openFarmForm(
        state.farms.find(
          (farm) => farm.farmId === Number(edit.dataset.farmEdit),
        ),
      );
    if (select) {
      state.farmId = Number(select.dataset.farmSelect);
      renderFarmSelect();
      await loadDashboard();
      setPage("overview");
    }
    if (remove) await deleteFarm(remove.dataset.farmDelete);
  });
document
  .querySelector("#add-device-record-button")
  .addEventListener("click", () => {
    if (!state.farmId) {
      notify("Chọn hoặc tạo khu vực chuồng trước khi thêm thiết bị.");
      return;
    }
    openDeviceRecordForm();
  });
document
  .querySelector("#cancel-device-record-button")
  .addEventListener("click", () => {
    document.querySelector("#farm-device-form-panel").hidden = true;
  });
document
  .querySelector("#farm-device-form")
  .addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const editingId = state.editingDeviceId;
    const payload = {
      ...Object.fromEntries(new FormData(form)),
      farmId: Number(state.farmId),
    };
    try {
      await api(editingId ? `/api/devices/${editingId}` : "/api/devices", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });
      document.querySelector("#farm-device-form-panel").hidden = true;
      await renderFarmDevices();
      notify(editingId ? "Đã cập nhật thiết bị." : "Đã thêm thiết bị.", true);
    } catch (error) {
      notify(error.message);
    }
  });
document
  .querySelector("#farm-devices")
  .addEventListener("click", async (event) => {
    const edit = event.target.closest("[data-device-edit]");
    const remove = event.target.closest("[data-device-delete]");
    if (edit) {
      try {
        const device = await api(`/api/devices/${edit.dataset.deviceEdit}`);
        openDeviceRecordForm(device);
      } catch (error) {
        notify(error.message);
      }
    }
    if (
      remove &&
      window.confirm("Xóa thiết bị cùng dữ liệu cảm biến và actor liên quan?")
    ) {
      try {
        await api(`/api/devices/${remove.dataset.deviceDelete}`, {
          method: "DELETE",
        });
        await loadDashboard();
        await renderFarmDevices();
        notify("Đã xóa thiết bị.", true);
      } catch (error) {
        notify(error.message);
      }
    }
  });
document
  .querySelector("#actor-list")
  .addEventListener("click", async (event) => {
    const config = event.target.closest("[data-actor-config]");
    const toggle = event.target.closest("[data-actor-toggle]");
    if (config) {
      const actor = state.dashboard?.actors.find(
        (item) => item.actorId === Number(config.dataset.actorConfig),
      );
      if (actor) openActorConfig(actor);
    }
    if (toggle) {
      toggle.disabled = true;
      try {
        await api(`/api/actors/${toggle.dataset.actorToggle}/command`, {
          method: "POST",
          body: JSON.stringify({
            mode: 1,
            status: Number(toggle.dataset.nextStatus),
          }),
        });
        notify("Đã gửi lệnh, chờ thiết bị xác nhận.", true);
      } catch (error) {
        notify(error.message);
      } finally {
        toggle.disabled = false;
      }
    }
  });
document.querySelector("#cancel-actor-config").addEventListener("click", () => {
  document.querySelector("#actor-config-form").hidden = true;
});
document
  .querySelector("#actor-config-form [name=mode]")
  .addEventListener("change", syncActorModeFields);
document
  .querySelector("#actor-config-form")
  .addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const mode = Number(form.elements.mode.value);
    const payload = { mode };
    if (mode === 0) {
      payload.sourceSensorCode = form.elements.sourceSensorCode.value;
      payload.thresholdMin = Number(form.elements.thresholdMin.value);
      payload.thresholdMax = Number(form.elements.thresholdMax.value);
    }
    try {
      await api(`/api/actors/${form.elements.actorId.value}/mode`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      form.hidden = true;
      await loadDashboard();
      notify("Đã lưu chế độ điều khiển.", true);
    } catch (error) {
      notify(error.message);
    }
  });
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
document.querySelector("#claim-device-button").addEventListener("click", () => {
  const select = document.querySelector("#claim-device-form [name=farmId]");
  select.innerHTML = state.farms
    .map(
      (farm) =>
        `<option value="${farm.farmId}">${escapeHtml(farm.name)}</option>`,
    )
    .join("");
  document.querySelector("#claim-device-panel").hidden = false;
});
document.querySelector("#cancel-claim-button").addEventListener("click", () => {
  document.querySelector("#claim-device-panel").hidden = true;
});
document
  .querySelector("#claim-device-form")
  .addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = Object.fromEntries(new FormData(form));
    payload.farmId = Number(payload.farmId);
    try {
      await api("/api/devices/claim", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      form.reset();
      document.querySelector("#claim-device-panel").hidden = true;
      await loadFarms();
      notify("Đã ghép thiết bị vào khu vực.", true);
    } catch (error) {
      notify(error.message);
    }
  });
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
document
  .querySelector("#camera-grid")
  .addEventListener("click", async (event) => {
    const tile = event.target.closest("[data-camera]");
    const camera = event.target.closest("[data-camera-device]");
    if (!tile && !camera) return;
    document.querySelector("#camera-dialog-title").textContent = camera
      ? camera.querySelector("strong").textContent
      : `Camera ${tile.dataset.camera.padStart(2, "0")}`;
    const dialog = document.querySelector("#camera-dialog");
    const stage = dialog.querySelector(".video-stage");
    stage.innerHTML = camera
      ? "<p>Đang yêu cầu luồng video...</p>"
      : "<p>Camera chưa được ghép thiết bị</p><small>Vào Quản lý khu vực để thêm và cấp mã camera.</small>";
    dialog.showModal();
    if (!camera) return;
    try {
      const stream = await api(
        `/api/cameras/${camera.dataset.cameraDevice}/stream`,
        { method: "POST", body: "{}" },
      );
      stage.innerHTML = "<video controls autoplay playsinline></video>";
      const video = stage.querySelector("video");
      if (
        stream.streamUrl.toLowerCase().includes(".m3u8") &&
        window.Hls?.isSupported()
      ) {
        const player = new window.Hls();
        player.loadSource(stream.streamUrl);
        player.attachMedia(video);
        dialog.addEventListener("close", () => player.destroy(), {
          once: true,
        });
      } else {
        video.src = stream.streamUrl;
      }
    } catch (error) {
      stage.innerHTML = `<p>Không phát được camera</p><small>${escapeHtml(error.message)}</small>`;
    }
  });
document.querySelector("#camera-dialog").addEventListener("click", (event) => {
  if (event.target === event.currentTarget) event.currentTarget.close();
});
document.querySelectorAll(".period-button").forEach((button) =>
  button.addEventListener("click", () => {
    state.period = button.dataset.period;
    document
      .querySelectorAll(".period-button")
      .forEach((item) => item.classList.toggle("is-active", item === button));
    document.querySelector(".history-sensor-button.is-active")?.click();
  }),
);
document
  .querySelector("#history-sensor-list")
  .addEventListener("click", async (event) => {
    const button = event.target.closest("[data-history-sensor]");
    if (!button) return;
    document
      .querySelectorAll(".history-sensor-button")
      .forEach((item) => item.classList.toggle("is-active", item === button));
    const sensor = state.dashboard?.sensors.find(
      (item) => item.sensorId === Number(button.dataset.historySensor),
    );
    document.querySelector("#chart-title").textContent =
      sensor?.typeSensor || "Dữ liệu cảm biến";
    document.querySelector("#chart-unit").textContent = sensor?.unit || "";
    const chartArea = document.querySelector("#chart-area");
    const historyTable = document.querySelector("#history-table");
    chartArea.innerHTML = '<div class="empty-state">Đang tải lịch sử...</div>';
    historyTable.innerHTML = "";
    try {
      const samples = await api(
        `/api/sensors/${sensor.sensorId}/data?period=${state.period}`,
      );
      if (!samples.length) {
        chartArea.innerHTML =
          '<div class="empty-state">Chưa có dữ liệu trong khoảng thời gian này.</div>';
        return;
      }
      chartArea.innerHTML =
        '<canvas aria-label="Biểu đồ dữ liệu cảm biến"></canvas>';
      drawSensorChart(chartArea.querySelector("canvas"), samples);
      historyTable.innerHTML = `<table class="data-table"><thead><tr><th>THỜI GIAN</th><th>GIÁ TRỊ</th></tr></thead><tbody>${samples
        .slice(-20)
        .reverse()
        .map(
          (sample) =>
            `<tr><td>${new Date(sample.createdAt).toLocaleString("vi-VN")}</td><td>${escapeHtml(sample.value)} ${escapeHtml(sample.unit)}</td></tr>`,
        )
        .join("")}</tbody></table>`;
    } catch (error) {
      chartArea.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
    }
  });

function drawSensorChart(canvas, samples) {
  const ratio = window.devicePixelRatio || 1;
  const width = canvas.clientWidth;
  const height = canvas.clientHeight;
  canvas.width = width * ratio;
  canvas.height = height * ratio;
  const context = canvas.getContext("2d");
  context.scale(ratio, ratio);
  const values = samples
    .map((sample) => Number(sample.value))
    .filter(Number.isFinite);
  if (!values.length) return;
  const low = Math.min(...values);
  const span = Math.max(...values) - low || 1;
  const pad = 22;
  const points = values.map((value, index) => ({
    x: pad + (index * (width - pad * 2)) / Math.max(values.length - 1, 1),
    y: height - pad - ((value - low) * (height - pad * 2)) / span,
  }));
  context.strokeStyle = "#dfe8dc";
  context.lineWidth = 1;
  for (let line = 0; line < 4; line += 1) {
    const y = pad + (line * (height - pad * 2)) / 3;
    context.beginPath();
    context.moveTo(pad, y);
    context.lineTo(width - pad, y);
    context.stroke();
  }
  context.beginPath();
  points.forEach((point, index) =>
    index ? context.lineTo(point.x, point.y) : context.moveTo(point.x, point.y),
  );
  context.strokeStyle = "#6e995a";
  context.lineWidth = 2;
  context.stroke();
  points.forEach((point) => {
    context.beginPath();
    context.arc(point.x, point.y, 3, 0, Math.PI * 2);
    context.fillStyle = "#527b49";
    context.fill();
  });
}

function connectLiveUpdates() {
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

async function startApp() {
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

startApp();
