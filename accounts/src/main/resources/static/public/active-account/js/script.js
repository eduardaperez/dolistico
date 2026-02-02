window.addEventListener("load", () => {
    const loading = document.getElementById("loading");
    const responseEl = document.getElementById("response");
    const body = document.body;

    // Read email and token
    const params = new URLSearchParams(window.location.search);
    const email = params.get("email");
    const token = params.get("token");

    // Call the API endpoint
    const url = `${window.location.origin}/api/v1/accounts/activate-account`;

    fetch(url, {
        method: "POST",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            email: email,
            token: token
        })
    })

    .then(async response => {

        const data = await response.json();

        console.log(data);

        setTimeout(() => {

            loading.style.display = "none";
            responseEl.style.display = "block";

            // Text response
            responseEl.textContent = data.message;
            responseEl.style.color = "#ffffff";
            responseEl.style.fontSize = "2.8vh";
            responseEl.style.lineHeight = "1.5";
            responseEl.style.width = "90%";
            responseEl.style.maxWidth = "650px";
            responseEl.style.margin = "0 auto";
            responseEl.style.textAlign = "center";
            responseEl.style.fontFamily = "Arial, sans-serif";
            responseEl.style.textTransform = "uppercase";
            responseEl.style.padding = "50px 50px";

            if (response.ok) {
                body.style.backgroundColor = "#9DE872";
            } else {
                body.style.backgroundColor = "#F44336";
            }

        }, 1000);

    });

});