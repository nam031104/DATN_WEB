import { api, state, escapeHtml, notify } from "./core.js";
import { loadDashboard } from "./dashboard.js";
import { loadFarms } from "./farms.js";

export async function renderFarmDevices() {
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

export function openDeviceRecordForm(device = null) {
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

export function bindDeviceEvents() {
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
}
