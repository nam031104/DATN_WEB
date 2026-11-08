import { api, state, escapeHtml } from "./core.js";

export function renderHistorySensors() {
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

export function drawSensorChart(canvas, samples) {
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

export function bindHistoryEvents() {
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
}
