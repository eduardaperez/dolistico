// Loading
window.addEventListener("load", () => {
    const loading = document.getElementById("loading");
    const formUpdatepasswordFrame = document.getElementById("formUpdatepasswordFrame");

    setTimeout(() => {

        loading.style.display = "none";
        formUpdatepasswordFrame.style.display = "flex";

    }, 1000 );

});

// Send form
document.getElementById("updatePasswordForm").addEventListener("submit", function (event) {
    
    event.preventDefault(); // Reload disabled

    // Get elements
    const errorFrame = document.getElementById("errorFrame");
    const passwordField = document.getElementById("password");
    const textResponse = document.getElementById("textResponse");
    const body = document.body;


    // Read password, email and token
    const params = new URLSearchParams(window.location.search);
    const email = params.get("email");
    const token = params.get("token");
    const password = document.getElementById("password").value;

    // Call the API endpoint
    // const url = `${window.location.origin}/api/v1/accounts/update-password`;
    const url = `http://186.206.106.93:3000/api/v1/accounts/update-password`;

    fetch(url, {
        method: "PATCH",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            email: email,
            password: password,
            token: token
        })
    })

    .then(async response => {

        const data = await response.json();

        if (response.ok) {

            formUpdatepasswordFrame.style.display = "none";
            body.classList.add("success-background");
            textResponse.textContent = (data.message ? data.message : data.detail);
            textResponse.style.display = "block";

        } else {

            // Error clean
            errorFrame.innerHTML = "";

            // Standard (message or detail)
            const generalMessage =
                data?.message ||
                data?.detail ||
                null;

            if (generalMessage) {
                const p = document.createElement("p");
                p.className = "errorText";
                p.textContent = generalMessage;
                errorFrame.appendChild(p);
            }

            // Field errors (fieldErrors[])
            if (Array.isArray(data?.fieldErrors)) {
                data.fieldErrors.forEach(err => {
                    if (err.message) {
                        const p = document.createElement("p");
                        p.className = "errorText";
                        p.textContent = err.message;
                        errorFrame.appendChild(p);
                    }
                });
            }

            errorFrame.style.display = "flex";
            passwordField.classList.add("passworderror");
            
        }


    });








});