// Yoga Phone website behaviour: live clocks, the look picker, brightness, and quiet reveals.
(function () {
  var root = document.documentElement;
  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function store(key, value) {
    try {
      if (value == null) localStorage.removeItem(key);
      else localStorage.setItem(key, value);
    } catch (e) {}
  }

  function pad(n) {
    return n < 10 ? "0" + n : String(n);
  }

  // ---------- Clocks: the visitor's own time, in their 12/24-hour habit, like BrandClock. ----------

  var uses24 = (function () {
    try {
      var cycle = new Intl.DateTimeFormat(undefined, { hour: "numeric" }).resolvedOptions().hourCycle;
      if (cycle) return cycle === "h23" || cycle === "h24";
      return !/AM|PM/i.test(new Date(2020, 0, 1, 15).toLocaleTimeString());
    } catch (e) {
      return true;
    }
  })();

  var dateFormat = new Intl.DateTimeFormat(undefined, { weekday: "long", day: "numeric", month: "long" });

  function tick() {
    var now = new Date();
    var h = now.getHours();
    var hours = uses24 ? pad(h) : String(h % 12 === 0 ? 12 : h % 12);
    var minutes = pad(now.getMinutes());
    var spoken = now.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });

    document.querySelectorAll("[data-clock]").forEach(function (clock) {
      clock.querySelector("[data-h]").textContent = hours;
      clock.querySelector("[data-m]").textContent = minutes;
      var ampm = clock.querySelector("[data-ampm]");
      if (ampm) {
        ampm.hidden = uses24;
        ampm.textContent = h < 12 ? "AM" : "PM";
      }
      clock.setAttribute("aria-label", spoken);
    });
    document.querySelectorAll("[data-status-time]").forEach(function (el) {
      el.textContent = hours + ":" + minutes;
    });
    document.querySelectorAll("[data-date]").forEach(function (el) {
      el.textContent = dateFormat.format(now);
    });
    document.querySelectorAll("[data-year]").forEach(function (el) {
      el.textContent = now.getFullYear();
    });
    setTimeout(tick, (60 - now.getSeconds()) * 1000 - now.getMilliseconds() + 30);
  }

  tick();

  // Progress lines and week bars read their values from data attributes (keeps markup free of inline styles).
  document.querySelectorAll("[data-progress]").forEach(function (el) {
    el.style.setProperty("--p", el.getAttribute("data-progress") + "%");
  });
  document.querySelectorAll(".bars [data-h]").forEach(function (el) {
    el.style.setProperty("--h", el.getAttribute("data-h") + "%");
  });

  // ---------- Radio groups (look picker, brightness) ----------

  function radioGroup(group, onChange) {
    var items = Array.prototype.slice.call(group.querySelectorAll("[role=radio]"));
    function select(item, focus) {
      items.forEach(function (other) {
        var on = other === item;
        other.setAttribute("aria-checked", on ? "true" : "false");
        other.tabIndex = on ? 0 : -1;
      });
      if (focus) item.focus();
      onChange(item.getAttribute("data-value"));
    }
    items.forEach(function (item, i) {
      item.addEventListener("click", function () {
        select(item, false);
      });
      item.addEventListener("keydown", function (event) {
        var step = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.key];
        if (!step) return;
        event.preventDefault();
        select(items[(i + step + items.length) % items.length], true);
      });
    });
    return {
      set: function (value) {
        items.forEach(function (item) {
          var on = item.getAttribute("data-value") === value;
          item.setAttribute("aria-checked", on ? "true" : "false");
          item.tabIndex = on ? 0 : -1;
        });
      },
    };
  }

  var looks = document.querySelector("[data-looks]");
  if (looks) {
    var lookGroup = radioGroup(looks, function (value) {
      root.setAttribute("data-look", value);
      store("yoga.look", value === "obsidian" ? null : value);
    });
    lookGroup.set(root.getAttribute("data-look") || "obsidian");
  }

  var modes = document.querySelector("[data-modes]");
  if (modes) {
    var current = root.getAttribute("data-grey") === "on" ? "grey" : root.getAttribute("data-theme") || "system";
    var modeGroup = radioGroup(modes, function (value) {
      root.removeAttribute("data-theme");
      root.removeAttribute("data-grey");
      if (value === "light" || value === "dark") root.setAttribute("data-theme", value);
      if (value === "grey") root.setAttribute("data-grey", "on");
      store("yoga.mode", value === "system" ? null : value);
    });
    modeGroup.set(current);
  }

  // ---------- Header hairline once the page scrolls ----------

  var header = document.querySelector("[data-header]");
  if (header) {
    var onScroll = function () {
      header.classList.toggle("scrolled", window.scrollY > 8);
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();
  }

  // ---------- Reveals and the mindful pause ----------

  var reveals = document.querySelectorAll(".reveal");
  var pause = document.querySelector("[data-pause]");

  if (!("IntersectionObserver" in window) || reduceMotion) {
    reveals.forEach(function (el) {
      el.classList.add("in");
    });
  } else {
    var revealer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("in");
          revealer.unobserve(entry.target);
        });
      },
      { rootMargin: "0px 0px -8% 0px", threshold: 0.08 }
    );
    reveals.forEach(function (el) {
      revealer.observe(el);
    });
  }

  if (pause) {
    var count = pause.querySelector("[data-count]");
    var open = pause.querySelector("[data-open]");
    var timer = null;
    var visible = false;

    var run = function () {
      var remaining = 7;
      clearTimeout(timer);
      var step = function () {
        if (remaining > 0) {
          count.textContent = remaining;
          count.classList.remove("ready");
          open.classList.remove("on");
          remaining -= 1;
          timer = setTimeout(step, 1000);
        } else {
          count.textContent = "Breathe";
          count.classList.add("ready");
          open.classList.add("on");
          timer = setTimeout(function () {
            if (visible) run();
          }, 6000);
        }
      };
      step();
    };

    if (reduceMotion || !("IntersectionObserver" in window)) {
      count.textContent = "7";
    } else {
      new IntersectionObserver(
        function (entries) {
          entries.forEach(function (entry) {
            visible = entry.isIntersecting;
            if (visible) run();
            else clearTimeout(timer);
          });
        },
        { threshold: 0.4 }
      ).observe(pause);
    }
  }

  // ---------- Privacy policy contents: mark the section in view ----------

  var toc = document.querySelectorAll(".toc a");
  if (toc.length && "IntersectionObserver" in window) {
    var links = {};
    toc.forEach(function (a) {
      links[a.getAttribute("href").slice(1)] = a;
    });
    var spy = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          toc.forEach(function (a) {
            a.classList.remove("active");
          });
          var link = links[entry.target.id];
          if (link) link.classList.add("active");
        });
      },
      { rootMargin: "-15% 0px -75% 0px" }
    );
    document.querySelectorAll(".doc h2[id]").forEach(function (h) {
      spy.observe(h);
    });
  }
})();
