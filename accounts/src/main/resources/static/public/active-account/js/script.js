window.addEventListener("load", () => {
    const loading = document.getElementById("loading");
    const responseEl = document.getElementById("response");
    const body = document.body;

    const url = `${window.location.origin}/api/v1/accounts/activate-account`;

    fetch(url)
        .then(async response => {
            const data = await response.json();

            setTimeout(() => {
                // Esconde loader e mostra mensagem
                loading.style.display = "none";
                responseEl.style.display = "block";

                // Mensagem da API
                responseEl.textContent = data.message;

                // Estilo do texto
                responseEl.style.color = "#ffffff";
                responseEl.style.fontSize = "4vh";
                responseEl.style.textAlign = "center";
                responseEl.style.fontFamily = "Arial, sans-serif";
                responseEl.style.textTransform = "uppercase";
                responseEl.style.padding = "50px 50px";

                // Cor do body
                if (response.ok) {
                    body.style.backgroundColor = "#9DE872"; // verde
                } else {
                    body.style.backgroundColor = "#F44336"; // vermelho
                }
            }, 1000); // delay de 1 segundo
        });
});
