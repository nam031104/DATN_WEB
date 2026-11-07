const authForm = document.querySelector("#auth-form");
const authView = document.querySelector("#auth-view");
const sessionView = document.querySelector("#session-view");
const fullnameField = document.querySelector("#fullname-field");
const emailField = document.querySelector("#email-field");
const formAlert = document.querySelector("#form-alert");
const sessionAlert = document.querySelector("#session-alert");
const submitButton = document.querySelector("#submit-button");
const submitLabel = document.querySelector("#submit-label");
const modeButtons = [...document.querySelectorAll(".mode-button")];
let mode = "login";

function showAlert(element, message, success = false) {
  element.textContent = message;
  element.classList.toggle("success", success);
  element.hidden = false;
}

function clearAlert(element) {
  element.textContent = "";
  element.hidden = true;
  element.classList.remove("success");
}

function setMode(nextMode) {
  mode = nextMode;
  const registering = mode === "register";
  document.querySelector("#form-eyebrow").textContent = registering
    ? "BẮT ĐẦU TẠI ĐÂY"
    : "TÀI KHOẢN CỦA BẠN";
  document.querySelector("#form-title").textContent = registering
    ? "Tạo tài khoản"
    : "Đăng nhập";
  document.querySelector("#form-subtitle").textContent = registering
    ? "Tạo tài khoản để bắt đầu quản lý trang trại."
    : "Nhập thông tin để tiếp tục vào trang trại.";
  document.querySelector("#password").autocomplete = registering
    ? "new-password"
    : "current-password";
  document.querySelector("#password").minLength = registering ? 8 : 0;
  document.querySelector("#password-hint").hidden = !registering;
  fullnameField.hidden = !registering;
  emailField.hidden = !registering;
  document.querySelector("#fullname").required = registering;
  document.querySelector("#email").required = registering;
  document.querySelector("#submit-label").textContent = registering
    ? "Tạo tài khoản"
    : "Đăng nhập";
  modeButtons.forEach((button) => {
    const active = button.dataset.mode === mode;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-selected", String(active));
  });
  clearAlert(formAlert);
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(
      payload.error ||
        payload.message ||
        "Yêu cầu chưa thực hiện được. Vui lòng thử lại.",
    );
  }
  return payload;
}

function showSession(user) {
  window.location.assign("/dashboard.html");
}

async function restoreSession() {
  try {
    const user = await requestJson("/api/current-user", { headers: {} });
    if (user.authenticated) showSession(user);
  } catch (error) {
    if (error instanceof TypeError)
      showAlert(
        formAlert,
        "Không thể kết nối máy chủ. Hãy kiểm tra backend đang chạy.",
      );
  }
}

modeButtons.forEach((button) =>
  button.addEventListener("click", () => setMode(button.dataset.mode)),
);

authForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearAlert(formAlert);
  if (!authForm.reportValidity()) return;

  const values = new FormData(authForm);
  const body = {
    username: values.get("username").trim(),
    password: values.get("password"),
  };

  if (mode === "register") {
    body.fullname = values.get("fullname").trim();
    body.email = values.get("email").trim();
  }

  submitButton.disabled = true;
  submitLabel.textContent =
    mode === "register" ? "Đang tạo tài khoản..." : "Đang đăng nhập...";

  try {
    if (mode === "register") {
      const result = await requestJson("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(body),
      });
      setMode("login");
      document.querySelector("#username").value = body.username;
      showAlert(
        formAlert,
        result.message || "Đăng ký thành công. Bạn có thể đăng nhập.",
        true,
      );
      document.querySelector("#password").value = "";
    } else {
      const user = await requestJson("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(body),
      });
      showSession(user);
    }
  } catch (error) {
    showAlert(
      formAlert,
      error.message || "Không thể kết nối máy chủ. Hãy thử lại.",
    );
  } finally {
    submitButton.disabled = false;
    submitLabel.textContent =
      mode === "register" ? "Tạo tài khoản" : "Đăng nhập";
  }
});

document
  .querySelector("#logout-button")
  .addEventListener("click", async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
      await requestJson("/api/auth/logout", { method: "POST", body: "{}" });
      sessionView.hidden = true;
      authView.hidden = false;
      authForm.reset();
      setMode("login");
    } catch (error) {
      showAlert(
        sessionAlert,
        error.message || "Không thể đăng xuất. Hãy thử lại.",
      );
    } finally {
      button.disabled = false;
    }
  });

restoreSession();
