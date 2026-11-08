import { api, state, escapeHtml, notify } from "./core.js";
import { loadDashboard } from "./dashboard.js";

export function openActorConfig(actor) {
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

export function syncActorModeFields() {
  const automatic =
    document.querySelector("#actor-config-form [name=mode]").value === "0";
  document.querySelectorAll("[data-auto-field]").forEach((field) => {
    field.hidden = !automatic;
    const input = field.querySelector("input, select");
    if (input) input.required = automatic;
  });
}

export function bindActorEvents() {
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
}
