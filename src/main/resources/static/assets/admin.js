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

const alertBox = document.querySelector("#admin-alert");
const escapeHtml = (value = "") =>
  String(value).replace(
    /[&<>"']/g,
    (char) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[char],
  );

function notify(message, success = false) {
  alertBox.textContent = message;
  alertBox.classList.toggle("success", success);
  alertBox.hidden = false;
  window.setTimeout(() => {
    alertBox.hidden = true;
  }, 4500);
}


const SENSOR_TYPES = [
  { value: "TEMPERATURE", label: "TEMPERATURE" },
  { value: "HUMIDITY", label: "HUMIDITY" },
  { value: "MQ135", label: "MQ135" },
  { value: "LOADCELL", label: "LOADCELL" },
];

const ACTOR_TYPES = [
  { value: "LIGHT", label: "LIGHT" },
  { value: "FAN", label: "FAN" },
  { value: "SERVO", label: "SERVO" },
];

function createTypeOptions(types) {
  return types
    .map(
      (type) =>
        `<option value="${type.value}">${type.label}</option>`,
    )
    .join("") +
    '<option value="CUSTOM">Khác - Tự nhập</option>';
}

function addHardwareRow(containerId, type) {
  const container = document.querySelector(`#${containerId}`);
  const isSensor = type === "sensor";
  const types = isSensor ? SENSOR_TYPES : ACTOR_TYPES;

  const row = document.createElement("div");
  row.className = "hardware-row";

  row.innerHTML = `
    <label>
      Mã ${isSensor ? "Sensor" : "Actor"}
      <input
        type="text"
        data-hardware-code
        required
        maxlength="100"
        placeholder="${isSensor ? "temp_1" : "fan_1"}"
      />
    </label>

    <label>
      Loại ${isSensor ? "Sensor" : "Actor"}
      <select data-hardware-type>
        ${createTypeOptions(types)}
      </select>
    </label>

    <label data-custom-wrapper hidden>
      Loại tự nhập
      <input
        type="text"
        data-custom-type
        maxlength="100"
        placeholder="Nhập loại thiết bị"
      />
    </label>

    ${
      isSensor
        ? ""
        : `
          <label>
            Chế độ hoạt động
            <select data-actor-mode>
              <option value="0">Tự động</option>
              <option value="1">Thủ công</option>
              <option value="2">Không được cấu hình</option>
            </select>
          </label>
        `
    }

    <button
      type="button"
      class="table-action"
      data-remove-hardware
      aria-label="Xóa dòng"
    >
      Xóa
    </button>
  `;

  container.appendChild(row);
}

function getHardwareRows(containerId, type) {
  const container = document.querySelector(`#${containerId}`);
  const isSensor = type === "sensor";

  return [...container.querySelectorAll(".hardware-row")].map((row) => {
    const externalCode = row.querySelector("[data-hardware-code]").value.trim();
    const selectedType = row.querySelector("[data-hardware-type]").value;
    const customType = row.querySelector("[data-custom-type]")?.value.trim();

    if (!externalCode) {
      throw new Error(`Vui lòng nhập mã ${isSensor ? "Sensor" : "Actor"}.`);
    }

    if (selectedType === "CUSTOM" && !customType) {
      throw new Error(`Vui lòng nhập loại ${isSensor ? "Sensor" : "Actor"} tùy chỉnh.`);
    }

    const hardwareType =
      selectedType === "CUSTOM" ? customType : selectedType;

    if (isSensor) {
      return {
        externalCode,
        typeSensor: hardwareType,
      };
    }

    return {
      externalCode,
      typeActor: hardwareType,
      mode: Number(row.querySelector("[data-actor-mode]").value),
      status: 0,
    };
  });
}

function bindHardwareEvents() {
  document.querySelector("#add-sensor-row").addEventListener("click", () => {
    addHardwareRow("sensor-rows", "sensor");
  });

  document.querySelector("#add-actor-row").addEventListener("click", () => {
    addHardwareRow("actor-rows", "actor");
  });

  document.querySelector("#sensor-rows").addEventListener("change", (event) => {
    if (event.target.matches("[data-hardware-type]")) {
      const row = event.target.closest(".hardware-row");
      const custom = row.querySelector("[data-custom-wrapper]");
      const input = row.querySelector("[data-custom-type]");

      custom.hidden = event.target.value !== "CUSTOM";
      input.required = event.target.value === "CUSTOM";
    }
  });

  document.querySelector("#actor-rows").addEventListener("change", (event) => {
    if (event.target.matches("[data-hardware-type]")) {
      const row = event.target.closest(".hardware-row");
      const custom = row.querySelector("[data-custom-wrapper]");
      const input = row.querySelector("[data-custom-type]");

      custom.hidden = event.target.value !== "CUSTOM";
      input.required = event.target.value === "CUSTOM";
    }
  });

  ["sensor-rows", "actor-rows"].forEach((containerId) => {
    document.querySelector(`#${containerId}`).addEventListener("click", (event) => {
      const remove = event.target.closest("[data-remove-hardware]");
      if (remove) {
        remove.closest(".hardware-row").remove();
      }
    });
  });
}



async function loadFarmsForUser(userId, select) {
  if (!userId) {
    select.innerHTML = '<option value="">Chưa gán</option>';
    return;
  }
  try {
    const farms = await api(
      `/api/admin/users/${encodeURIComponent(userId)}/farms`,
    );
    select.innerHTML = farms.length
      ? farms
          .map(
            (farm) =>
              `<option value="${farm.farmId}">${escapeHtml(farm.name)}</option>`,
          )
          .join("")
      : '<option value="">User chưa có chuồng</option>';
  } catch (error) {
    select.innerHTML = `<option value="">${escapeHtml(error.message)}</option>`;
  }
}

function parseHardwareRows(text, sensor = false) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [code, type] = line.split(":").map((part) => part.trim());
      if (!code || !type)
        throw new Error("Mỗi dòng linh kiện cần có dạng mã:type.");
      return sensor
        ? { externalCode: code, typeSensor: type }
        : { externalCode: code, typeActor: type, mode: 0, status: 0 };
    });
}

async function renderAdmin() {
  const usersTarget = document.querySelector("#admin-users");
  try {
    const [users, devices] = await Promise.all([
      api("/api/users"),
      api("/api/admin/devices"),
    ]);
    usersTarget.innerHTML = users.length
      ? `<table class="data-table"><thead><tr><th>TÀI KHOẢN</th><th>EMAIL</th><th>VAI TRÒ</th><th></th></tr></thead><tbody>${users.map((user) => `<tr><td>${escapeHtml(user.fullname || user.username)}<br><small>${escapeHtml(user.username)}</small></td><td>${escapeHtml(user.email)}</td><td><select data-user-role="${escapeHtml(user.userId)}"><option value="USER" ${user.role === "USER" ? "selected" : ""}>USER</option><option value="ADMIN" ${user.role === "ADMIN" ? "selected" : ""}>ADMIN</option></select></td><td><button class="table-action" type="button" data-user-save="${escapeHtml(user.userId)}">Lưu</button><button class="table-action" type="button" data-user-delete="${escapeHtml(user.userId)}">Xóa</button></td></tr>`).join("")}</tbody></table>`
      : '<div class="empty-state">Chưa có người dùng.</div>';

    const userSelect = document.querySelector(
      "#admin-device-form [name=userId]",
    );
    userSelect.innerHTML =
      '<option value="">Chưa gán</option>' +
      users
        .map(
          (user) =>
            `<option value="${escapeHtml(user.userId)}">${escapeHtml(user.fullname || user.username)} · ${escapeHtml(user.username)}</option>`,
        )
        .join("");
    userSelect.dispatchEvent(new Event("change"));

    document.querySelector("#admin-devices").innerHTML = devices.length
      ? `<table class="data-table"><thead><tr><th>MÃ</th><th>THIẾT BỊ</th><th>CHUỒNG</th></tr></thead><tbody>${devices.map((device) => `<tr><td>${escapeHtml(device.deviceCode || "-")}</td><td>${escapeHtml(device.name)}</td><td>${device.farmId ?? "Chưa gán"}</td></tr>`).join("")}</tbody></table>`
      : '<div class="empty-state">Chưa cấp thiết bị nào.</div>';
  } catch (error) {
    usersTarget.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
  }
}

document.querySelector("#add-user-button").addEventListener("click", () => {
  document.querySelector("#admin-user-form-panel").hidden = false;
});
document.querySelector("#cancel-user-button").addEventListener("click", () => {
  document.querySelector("#admin-user-form-panel").hidden = true;
});
document
  .querySelector("#admin-user-form")
  .addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await api("/api/users", {
        method: "POST",
        body: JSON.stringify(Object.fromEntries(new FormData(form))),
      });
      form.reset();
      document.querySelector("#admin-user-form-panel").hidden = true;
      await renderAdmin();
      notify("Đã tạo người dùng.", true);
    } catch (error) {
      notify(error.message);
    }
  });
document
  .querySelector("#admin-users")
  .addEventListener("click", async (event) => {
    const save = event.target.closest("[data-user-save]");
    const remove = event.target.closest("[data-user-delete]");
    if (save) {
      try {
        const users = await api("/api/users");
        const user = users.find(
          (item) => String(item.userId) === save.dataset.userSave,
        );
        const role = document.querySelector(
          `[data-user-role="${CSS.escape(save.dataset.userSave)}"]`,
        ).value;
        await api(`/api/users/${encodeURIComponent(user.userId)}`, {
          method: "PUT",
          body: JSON.stringify({
            username: user.username,
            fullname: user.fullname,
            email: user.email,
            role,
          }),
        });
        await renderAdmin();
        notify("Đã cập nhật vai trò.", true);
      } catch (error) {
        notify(error.message);
      }
    }
    if (remove && window.confirm("Xóa tài khoản người dùng này?")) {
      try {
        await api(
          `/api/users/${encodeURIComponent(remove.dataset.userDelete)}`,
          { method: "DELETE" },
        );
        await renderAdmin();
        notify("Đã xóa tài khoản.", true);
      } catch (error) {
        notify(error.message);
      }
    }
  });

document.querySelector("#add-device-button").addEventListener("click", () => {
  document.querySelector("#admin-device-form-panel").hidden = false;
});
document
  .querySelector("#cancel-device-button")
  .addEventListener("click", () => {
    document.querySelector("#admin-device-form-panel").hidden = true;
  });
document
  .querySelector("#admin-device-form [name=userId]")
  .addEventListener("change", (event) => {
    loadFarmsForUser(
      event.target.value,
      document.querySelector("#admin-device-form [name=farmId]"),
    );
  });

document
  .querySelector("#admin-device-form")
  .addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));

    try {
      const payload = {
        ...values,
        userId: values.userId || null,
        farmId: values.farmId ? Number(values.farmId) : null,

        sensors: getHardwareRows("sensor-rows", "sensor"),
        actors: getHardwareRows("actor-rows", "actor"),
      };

      await api("/api/admin/devices/provision", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      form.reset();

      document.querySelector("#sensor-rows").innerHTML = "";
      document.querySelector("#actor-rows").innerHTML = "";

      document.querySelector("#admin-device-form-panel").hidden = true;

      await renderAdmin();

      notify("Đã cấp cấu hình thiết bị.", true);
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

async function startAdmin() {
  try {
    const user = await api("/api/current-user");
    if (!user.authenticated || user.role !== "ADMIN") {
      window.location.assign("/dashboard.html");
      return;
    }
    document.querySelector("#profile-name").textContent =
      user.fullname || user.username;
    document.querySelector("#profile-email").textContent = user.email || "";
    document.querySelector("#profile-initial").textContent = (
      user.fullname ||
      user.username ||
      "A"
    )
      .trim()
      .charAt(0)
      .toUpperCase();
    await bindHardwareEvents();
    await renderAdmin();
  } catch {
    window.location.assign("/");
  }
}

startAdmin();
