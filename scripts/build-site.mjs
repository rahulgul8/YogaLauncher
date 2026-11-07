// Builds the Yoga Phone website into build/site (no dependencies).
// - Expands <!-- include:name --> from web/site/_partials and {{version}} cache keys.
// - Renders docs/launch/privacy-policy.md (the single source) to /privacy.
// Run: node scripts/build-site.mjs
import { readFileSync, writeFileSync, mkdirSync, rmSync, readdirSync, statSync, copyFileSync } from "node:fs";
import { join, dirname, extname } from "node:path";
import { fileURLToPath } from "node:url";

const repo = join(dirname(fileURLToPath(import.meta.url)), "..");
const src = join(repo, "web", "site");
const out = join(repo, "build", "site");
const version = Date.now().toString(36);

const partial = (name) => readFileSync(join(src, "_partials", `${name}.html`), "utf8");
const expand = (html) =>
  html.replace(/<!-- include:(\w+) -->/g, (_, name) => partial(name)).replaceAll("{{version}}", version);

const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const inline = (s) =>
  esc(s)
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
    .replace(/([\w.+-]+@[\w-]+\.[\w.]+)/g, '<a href="mailto:$1">$1</a>');
const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

function renderMarkdown(md) {
  const lines = md.replace(/\r/g, "").split("\n");
  let title = "", updated = "", body = "";
  const toc = [];
  for (let i = 0; i < lines.length; ) {
    const line = lines[i];
    if (!line.trim()) { i++; continue; }
    if (line.startsWith("# ")) { title = line.slice(2).trim(); i++; continue; }
    if (/^Last updated:/i.test(line)) { updated = line.trim(); i++; continue; }
    if (line.startsWith("## ")) {
      const text = line.slice(3).trim(), id = slug(text);
      toc.push({ id, text });
      body += `<h2 id="${id}">${inline(text)}</h2>\n`; i++; continue;
    }
    if (line.startsWith("|")) {
      const rows = [];
      while (i < lines.length && lines[i].startsWith("|")) rows.push(lines[i++]);
      const cells = (r) => r.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
      const head = cells(rows[0]);
      body += `<div class="table-scroll"><table><thead><tr>${head.map((c) => `<th>${inline(c)}</th>`).join("")}</tr></thead><tbody>`;
      for (const r of rows.slice(2)) body += `<tr>${cells(r).map((c) => `<td>${inline(c)}</td>`).join("")}</tr>`;
      body += "</tbody></table></div>\n"; continue;
    }
    if (/^- /.test(line)) {
      body += "<ul>";
      while (i < lines.length && /^- /.test(lines[i])) body += `<li>${inline(lines[i++].slice(2))}</li>`;
      body += "</ul>\n"; continue;
    }
    const para = [];
    while (i < lines.length && lines[i].trim() && !/^(#|\||- )/.test(lines[i])) para.push(lines[i++].trim());
    body += `<p>${inline(para.join(" "))}</p>\n`;
  }
  return { title, updated, body, toc };
}

function copyTree(from, to) {
  mkdirSync(to, { recursive: true });
  for (const name of readdirSync(from)) {
    if (name.startsWith("_") || name.startsWith(".") || name === "README.md") continue;
    const a = join(from, name), b = join(to, name);
    if (statSync(a).isDirectory()) copyTree(a, b);
    else if (extname(name) === ".html") writeFileSync(b, expand(readFileSync(a, "utf8")));
    else copyFileSync(a, b);
  }
}

rmSync(out, { recursive: true, force: true });
copyTree(src, out);

const policy = renderMarkdown(readFileSync(join(repo, "docs", "launch", "privacy-policy.md"), "utf8"));
const template = readFileSync(join(src, "_partials", "privacy.html"), "utf8");
const page = expand(template)
  .replace("{{updated}}", inline(policy.updated))
  .replace("{{toc}}", policy.toc.map((t) => `<li><a href="#${t.id}">${esc(t.text)}</a></li>`).join(""))
  .replace("{{body}}", policy.body);
mkdirSync(join(out, "privacy"), { recursive: true });
writeFileSync(join(out, "privacy", "index.html"), page);
console.log(`Built ${out} (version ${version}, ${policy.toc.length} policy sections)`);
