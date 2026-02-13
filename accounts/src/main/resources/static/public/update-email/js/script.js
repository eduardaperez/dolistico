// Loading
window.addEventListener("load", () => {
  const loading = document.getElementById("loading");
  const formUpdateEmailFrame = document.getElementById("formUpdateEmailFrame");

  setTimeout(() => {
    loading.style.display = "none";
    formUpdateEmailFrame.style.display = "flex";
  }, 1000);
});

// MODIFICAÇÕES EDUARDA
const pinBox = document.querySelector("#pinBox");
const inputs = document.querySelectorAll("#pinBox input");
const form = document.querySelector("#updateEmailForm");

// Pegando sempre o primeiro vazio
const focusFirst = () => {
  const firstEmpty = [...inputs].find((input) => input.value === "");

  if (firstEmpty) {
    firstEmpty.focus();
  } else {
    inputs[inputs.length - 1].focus();
  }
};

// Pegando o PIN
const getPin = () => {
  return [...document.querySelectorAll("#pinBox input")]
    .map((input) => input.value)
    .join("");
};

pinBox.addEventListener("paste", (e) => {
  const paste = e.clipboardData.getData("text").replace(/\D/g, "");

  paste.split("").forEach((num, i) => {
    if (inputs[i]) {
      inputs[i].value = num;
    }
  });
});

// percorrer o preenchimento
inputs.forEach((input) => {
  input.addEventListener("focus", focusFirst);

  input.addEventListener("input", () => {
    input.value = input.value.replace(/\D/g, "");

    if (input.value.length === 1) {
      focusFirst();
    }
  });

  input.addEventListener("keydown", (e) => {
    if (e.key === "Backspace") {
      inputs.forEach((i) => (i.value = ""));
      inputs[0].focus();
    }
  });
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const pin = getPin();
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");

  // elementos da tela
  const loading = document.getElementById("loading");
  const formFrame = document.getElementById("formUpdateEmailFrame");
  const errorFrame = document.getElementById("errorFrame");
  const textResponse = document.getElementById("textResponse");
  const successIcon = document.getElementById("success-icon");
  const body = document.body;

  // const url = `${window.location.origin}/api/v1/accounts/update-email`;
  const url = `https://request.juliokozarewicz.com/api/v1/accounts/update-email`;

  const options = {
    method: "PATCH",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      pin: pin,
      token: token,
    }),
  };

  try {
    loading.style.display = "flex";
    formFrame.style.display = "none";

    errorFrame.innerHTML = "";
    errorFrame.style.display = "none";

    const response = await fetch(url, options);
    const data = await response.json();

    if (response.ok) {
      loading.style.display = "none";
      successIcon.style.display = "block";
      body.classList.add("success-background");

      textResponse.textContent = data.message || data.detail;
      textResponse.style.display = "block";
      formFrame.style.display = "none";
    } else {
      loading.style.display = "none";
      formFrame.style.display = "flex";
      errorFrame.innerHTML = "";

      if (response.status === 404) {
        inputs.forEach((input) => {
          input.classList.add("input-error")
        })
      }

      const message = data?.message || data?.detail;

      if (message) {
        const p = document.createElement("p");
        p.className = "errorText";
        p.textContent = message;
        errorFrame.appendChild(p);
      }

      errorFrame.style.display = "flex";
    }
  } catch (error) {
    // fallback
    loading.style.display = "none";
    formFrame.style.display = "flex";

    errorFrame.innerHTML = "";

    const p = document.createElement("p");
    p.className = "errorText";
    p.textContent =
      "Unable to change your email address. Please try again later.";
    errorFrame.appendChild(p);
    errorFrame.style.display = "flex";
  }
});
