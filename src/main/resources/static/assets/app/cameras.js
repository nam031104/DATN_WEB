import { api, escapeHtml } from "./core.js";

export function bindCameraEvents() {
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
}
