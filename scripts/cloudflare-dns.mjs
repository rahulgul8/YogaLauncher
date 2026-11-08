#!/usr/bin/env node
// Points a subdomain at a Firebase Hosting site through Cloudflare DNS (no dependencies).
//
// It asks Firebase which records the custom domain needs (creating the custom domain if
// it is missing), then adds the missing ones in Cloudflare: the CNAME (proxy off, which
// Firebase needs to issue its certificate) and the _acme-challenge TXT record.
// Existing matching records are left alone; conflicting ones are reported, not changed,
// unless --replace is given.
//
// Usage:
//   node scripts/cloudflare-dns.mjs --domain yogaphone.tuwi.in --project yogaphone-c3901 [--site <siteId>] [--dry-run] [--replace]
//
// Credentials, never printed:
//   Cloudflare: api_token from ~/.secrets/cloudflare.yaml (or --secrets <path>, or CLOUDFLARE_API_TOKEN).
//               The token needs Zone > DNS > Edit on the zone.
//   Google:     `gcloud auth print-access-token` (an account with Firebase Hosting admin on the project).

import { readFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";
import { execSync } from "node:child_process";

const args = parseArgs(process.argv.slice(2));
const domain = required(args.domain, "--domain").toLowerCase().replace(/\.$/, "");
const project = required(args.project, "--project");
const site = args.site || project;
const dryRun = Boolean(args["dry-run"]);
const replace = Boolean(args.replace);

const cloudflareToken = readCloudflareToken(args.secrets || join(homedir(), ".secrets", "cloudflare.yaml"));
const googleToken = readGoogleToken();

const desired = await firebaseRecords();
if (!desired.length) {
  console.log(`Firebase lists no DNS changes for ${domain}. Nothing to do.`);
  process.exit(0);
}

const zone = await findZone(domain);
console.log(`Zone ${zone.name}${dryRun ? " (dry run: nothing will change)" : ""}`);

let failed = false;
for (const record of desired) {
  try {
    await apply(zone, record);
  } catch (error) {
    failed = true;
    console.error(`  ! ${record.type} ${record.name}: ${error.message}`);
  }
}

if (!dryRun && !failed) {
  console.log("\nDone. Firebase checks DNS on its own and issues the certificate, usually within an hour.");
  console.log(`Status: node scripts/cloudflare-dns.mjs --domain ${domain} --project ${project} --dry-run`);
}
process.exit(failed ? 1 : 0);

// ---------- Firebase ----------

async function firebaseRecords() {
  const base = `https://firebasehosting.googleapis.com/v1beta1/projects/${project}/sites/${site}/customDomains`;
  let custom = await google(`${base}/${encodeURIComponent(domain)}`, { allow404: true });
  if (!custom) {
    if (dryRun) {
      console.log(`${domain} is not yet a custom domain of ${site}; a real run adds it first.`);
      return [{ type: "CNAME", name: domain, content: `${site}.web.app`, action: "ADD" }];
    }
    console.log(`Adding ${domain} as a custom domain of ${site}…`);
    await google(`${base}?customDomainId=${encodeURIComponent(domain)}`, { method: "POST", body: {} });
    for (let i = 0; i < 10 && !(custom && custom.requiredDnsUpdates); i++) {
      await sleep(3000);
      custom = await google(`${base}/${encodeURIComponent(domain)}`, { allow404: true });
    }
    if (!custom) throw new Error("Firebase did not return the new custom domain.");
  }

  console.log(`Firebase: host ${custom.hostState || "?"}, ownership ${custom.ownershipState || "?"}, certificate ${custom.cert?.state || "?"}`);

  const sets = [...(custom.requiredDnsUpdates?.desired || []), ...(custom.cert?.verification?.dns?.desired || [])];
  const records = [];
  for (const set of sets) {
    for (const r of set.records || []) {
      records.push({
        type: r.type,
        name: (r.domainName || set.domainName).replace(/\.$/, "").toLowerCase(),
        content: String(r.rdata).replace(/\.$/, "").replace(/^"|"$/g, ""),
        action: r.requiredAction || "ADD",
      });
    }
  }
  return records;
}

// ---------- Cloudflare ----------

async function apply(zone, record) {
  const label = `${record.type} ${record.name} → ${record.content}`;
  const existing = await cloudflare(`/zones/${zone.id}/dns_records?type=${record.type}&name=${encodeURIComponent(record.name)}`);
  const same = existing.find((r) => normalise(r.content) === normalise(record.content));

  if (record.action === "REMOVE") {
    if (!same) return console.log(`  = ${label} already absent`);
    if (!replace) return console.log(`  ? ${label} should be removed; rerun with --replace to delete it`);
    if (!dryRun) await cloudflare(`/zones/${zone.id}/dns_records/${same.id}`, { method: "DELETE" });
    return console.log(`  - ${label}${dryRun ? " (would delete)" : " deleted"}`);
  }

  if (same) {
    if (record.type === "CNAME" && same.proxied) {
      if (!dryRun) await cloudflare(`/zones/${zone.id}/dns_records/${same.id}`, { method: "PATCH", body: { proxied: false } });
      return console.log(`  ~ ${label}: proxy turned off${dryRun ? " (would)" : ""}`);
    }
    return console.log(`  = ${label} already present`);
  }

  // A name can hold only one CNAME, and a CNAME can't share its name with other records.
  const conflicts = record.type === "CNAME"
    ? await cloudflare(`/zones/${zone.id}/dns_records?name=${encodeURIComponent(record.name)}`)
    : [];
  if (conflicts.length) {
    const list = conflicts.map((r) => `${r.type} ${r.content}`).join(", ");
    if (!replace) throw new Error(`conflicts with existing ${list}; rerun with --replace to replace them`);
    for (const r of conflicts) {
      if (!dryRun) await cloudflare(`/zones/${zone.id}/dns_records/${r.id}`, { method: "DELETE" });
      console.log(`  - ${r.type} ${record.name} → ${r.content}${dryRun ? " (would delete)" : " deleted"}`);
    }
  }

  const body = { type: record.type, name: record.name, content: record.content, ttl: 1 };
  if (["A", "AAAA", "CNAME"].includes(record.type)) body.proxied = false;
  if (record.type === "TXT") body.content = `"${record.content}"`;
  if (!dryRun) await cloudflare(`/zones/${zone.id}/dns_records`, { method: "POST", body });
  console.log(`  + ${label}${dryRun ? " (would add)" : " added"}${body.proxied === false ? ", DNS only" : ""}`);
}

async function findZone(name) {
  const labels = name.split(".");
  for (let i = 0; i < labels.length - 1; i++) {
    const candidate = labels.slice(i).join(".");
    const zones = await cloudflare(`/zones?name=${encodeURIComponent(candidate)}`);
    if (zones.length) return zones[0];
  }
  throw new Error(`No Cloudflare zone this token can see contains ${name}.`);
}

async function cloudflare(path, { method = "GET", body } = {}) {
  const res = await fetch(`https://api.cloudflare.com/client/v4${path}`, {
    method,
    headers: { Authorization: `Bearer ${cloudflareToken}`, "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  if (!res.ok || json.success === false) {
    const why = (json.errors || []).map((e) => `${e.code}: ${e.message}`).join("; ") || res.statusText;
    const hint = res.status === 403 && path.includes("/dns_records")
      ? ". The token can see the zone but not its DNS: give it Zone > DNS > Edit for this zone."
      : "";
    throw new Error(`Cloudflare ${method} ${path.split("?")[0]} failed (${res.status}) ${why}${hint}`);
  }
  return json.result;
}

// ---------- Helpers ----------

async function google(url, { method = "GET", body, allow404 = false } = {}) {
  const res = await fetch(url, {
    method,
    headers: { Authorization: `Bearer ${googleToken}`, "Content-Type": "application/json", "x-goog-user-project": project },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (allow404 && res.status === 404) return null;
  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(`Firebase ${method} failed (${res.status}) ${json.error?.message || res.statusText}`);
  return json;
}

function readCloudflareToken(path) {
  if (process.env.CLOUDFLARE_API_TOKEN) return process.env.CLOUDFLARE_API_TOKEN;
  let text;
  try {
    text = readFileSync(path, "utf8");
  } catch {
    throw new Error(`Can't read ${path}. Put api_token there or set CLOUDFLARE_API_TOKEN.`);
  }
  const match = text.match(/^\s*api_token\s*:\s*["']?([^"'\s#]+)["']?/m);
  if (!match) throw new Error(`No api_token field in ${path}.`);
  return match[1];
}

function readGoogleToken() {
  try {
    return execSync("gcloud auth print-access-token", { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }).trim();
  } catch {
    throw new Error("Couldn't get a Google token. Run `gcloud auth login` with an account that can manage the Firebase project.");
  }
}

function normalise(value) {
  return String(value).replace(/^"|"$/g, "").replace(/\.$/, "").toLowerCase();
}

function parseArgs(list) {
  const out = {};
  for (let i = 0; i < list.length; i++) {
    if (!list[i].startsWith("--")) continue;
    const key = list[i].slice(2);
    const next = list[i + 1];
    if (next === undefined || next.startsWith("--")) out[key] = true;
    else out[key] = list[++i];
  }
  return out;
}

function required(value, flag) {
  if (!value || value === true) {
    console.error(`Missing ${flag}.\nUsage: node scripts/cloudflare-dns.mjs --domain sub.example.com --project <firebase-project> [--site <siteId>] [--dry-run] [--replace]`);
    process.exit(2);
  }
  return value;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
