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

form.addEventListener("submit", (e) => {
  e.preventDefault();
  console.log(getPin());
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
