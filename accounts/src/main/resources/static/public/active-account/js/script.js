window.addEventListener("load", () => {
    const loading = document.getElementById("loading");
    const sucessIcon = document.getElementById("successicon");
    const errorIcon = document.getElementById("erroricon");
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

        setTimeout(() => {

            loading.style.display = "none";
            responseEl.style.display = "block";
            responseEl.textContent = (data.message ? data.message : data.detail);

            if (response.ok) {
                
                errorIcon.style.display = "none";
                sucessIcon.style.display = "block";
                body.classList.add("success-background");

            } else {
                errorIcon.style.display = "block";
                sucessIcon.style.display = "none";
                body.classList.add("error-background");

            }

        }, 1000 );

    })

    .catch(error => {

        errorIcon.style.display = "block";
        sucessIcon.style.display = "none";
        loading.style.display = "none";
        responseEl.style.display = "block";
        responseEl.textContent = "Something went wrong while activating your account. Please try again later.";
        body.classList.add("error-background");

    });

});