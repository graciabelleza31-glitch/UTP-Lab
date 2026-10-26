document.addEventListener("DOMContentLoaded", function () {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }
});

function alternarVisibilidadClave() {
    const campoClave = document.getElementById("password");
    const iconoOjo = document.getElementById("iconoOjo");

    if (campoClave.type === "password") {
        campoClave.type = "text";
        iconoOjo.setAttribute("data-lucide", "eye-off");
    } else {
        campoClave.type = "password";
        iconoOjo.setAttribute("data-lucide", "eye");
    }
    lucide.createIcons();
}