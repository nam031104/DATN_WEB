import { api, state, escapeHtml, notify } from "./core.js";
import { loadDashboard } from "./dashboard.js";

export function renderFarmSelect() {
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

export async function loadFarms() {
  state.farms = await api("/api/farms");
  if (!state.farms.some((farm) => farm.farmId === state.farmId))
    state.farmId = state.farms[0]?.farmId || null;
  renderFarmSelect();
  await loadDashboard();
}

export function renderFarms() {
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

export function openFarmForm(farm = null) {
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

export async function saveFarm(event, setPage) {
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

export async function deleteFarm(id) {
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

export function bindFarmEvents(setPage) {
  document
    .querySelector("#add-farm-button")
    .addEventListener("click", () => openFarmForm());
  document.querySelector("#cancel-farm-button").addEventListener("click", () => {
    document.querySelector("#farm-form-panel").hidden = true;
  });
  document.querySelector("#farm-form").addEventListener("submit", (event) => saveFarm(event, setPage));
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
}
