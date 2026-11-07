// Runs before paint: applies the visitor's chosen look and brightness, so the page never flashes.
(function () {
  var root = document.documentElement;
  root.classList.remove("no-js");
  try {
    var look = localStorage.getItem("yoga.look");
    if (look === "obsidian" || look === "stone" || look === "sage") root.setAttribute("data-look", look);
    var mode = localStorage.getItem("yoga.mode");
    if (mode === "light" || mode === "dark") root.setAttribute("data-theme", mode);
    if (mode === "grey") root.setAttribute("data-grey", "on");
  } catch (e) {
    // Storage can be blocked; the default look still renders.
  }
})();
