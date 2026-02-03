window.addEventListener("load", () => {
    const loading = document.getElementById("loading");
    const formUpdatepasswordFrame = document.getElementById("formUpdatepasswordFrame");
    const body = document.body;

    setTimeout(() => {

        loading.style.display = "none";
        formUpdatepasswordFrame.style.display = "flex";

    }, 1000 );

});