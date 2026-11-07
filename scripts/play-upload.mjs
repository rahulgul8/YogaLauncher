// Publishes to Google Play with the Tuwi service account:
//   node scripts/play-upload.mjs check
//   node scripts/play-upload.mjs upload [--track internal] [--aab <path>] [--status completed|draft]
//   node scripts/play-upload.mjs promote --version <code> --track beta [--status completed|draft]
//   node scripts/play-upload.mjs listing
// Each change command also takes --dry-run, --no-review and --no-notes.
// `check` lists every track's releases. `upload` uploads the bundle, points the
// track at it (replacing the track's current releases), adds the release notes
// for its version code from docs/play/release-notes.md, and commits. `promote`
// does the same for a version code that is already uploaded. `listing` pushes
// docs/play/listing-text.json to every listed language and replaces the en-IN
// phone screenshots with docs/play/screenshots/*.png in name order. With
// --dry-run the edit is validated and then discarded, so nothing changes.
// The key is read from PLAY_SERVICE_ACCOUNT_JSON, or
// ~/.secrets/play-service-account.json. The production track also needs
// --production.
import {readFileSync, readdirSync} from 'node:fs';
import {homedir} from 'node:os';
import {join} from 'node:path';
import {createSign} from 'node:crypto';

const root = new URL('..', import.meta.url);
const packageName = 'com.tuwi.yogaphone';
const api = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications';
const uploadApi = 'https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications';

const [command, ...rest] = process.argv.slice(2);
const flags = parseFlags(rest);
if (!['check', 'upload', 'promote', 'listing'].includes(command)) {
  console.error('usage: node scripts/play-upload.mjs check | upload [--track internal] [--aab <path>] '
    + '| promote --version <code> --track <track> | listing  [--status completed|draft] '
    + '[--dry-run] [--no-review] [--no-notes]');
  process.exit(2);
}

const keyPath = process.env.PLAY_SERVICE_ACCOUNT_JSON
  ?? join(homedir(), '.secrets', 'play-service-account.json');
const key = JSON.parse(readFileSync(keyPath, 'utf8'));
const headers = {Authorization: `Bearer ${await accessToken(key)}`};

const edit = await call('POST', `${api}/${packageName}/edits`);
const editUrl = `${api}/${packageName}/edits/${edit.id}`;
let committed = false;
try {
  if (command === 'check') {
    await printTracks();
  } else {
    if (command === 'upload') await upload();
    if (command === 'promote') await setTrack(Number(flags.version));
    if (command === 'listing') await pushListing();
    await finish();
    committed = !flags['dry-run'];
  }
} finally {
  if (!committed) await fetch(editUrl, {method: 'DELETE', headers});
}

async function printTracks() {
  const {tracks = []} = await call('GET', `${editUrl}/tracks`);
  console.log(`${packageName} (key ${key.client_email})`);
  for (const t of tracks) {
    const releases = t.releases ?? [];
    if (releases.length === 0) console.log(`  ${t.track}: (empty)`);
    for (const r of releases) {
      console.log(`  ${t.track}: ${r.status} ${r.name ?? ''} versionCodes=${(r.versionCodes ?? []).join(',')}`);
    }
  }
}

async function upload() {
  const aab = flags.aab ?? new URL('app/build/app/outputs/bundle/release/app-release.aab', root);
  console.log(`Uploading ${aab} ...`);
  const bundle = await call(
    'POST',
    `${uploadApi}/${packageName}/edits/${edit.id}/bundles?uploadType=media`,
    readFileSync(aab),
    'application/octet-stream',
  );
  console.log(`Uploaded version code ${bundle.versionCode}`);
  await setTrack(bundle.versionCode);
}

// Replaces the track's releases with one release of [versionCode].
async function setTrack(versionCode) {
  const track = flags.track ?? 'internal';
  const status = flags.status ?? 'completed';
  if (!Number.isInteger(versionCode) || versionCode <= 0) throw new Error('Pass --version <code>.');
  if (track === 'production' && !flags.production) {
    throw new Error('Releasing to production needs --production.');
  }
  const releaseNotes = flags['no-notes'] ? [] : notesFor(versionCode);
  await call('PUT', `${editUrl}/tracks/${track}`, JSON.stringify({
    track,
    releases: [{versionCodes: [String(versionCode)], status, releaseNotes}],
  }), 'application/json');
  console.log(`Track ${track}: version code ${versionCode}, ${status}, `
    + `notes in ${releaseNotes.map((n) => n.language).join(', ') || 'no language'}`);
}

async function pushListing() {
  const text = JSON.parse(readFileSync(new URL('docs/play/listing-text.json', root), 'utf8'));
  if (text.shortDescription.length > 80 || text.fullDescription.length > 4000 || text.title.length > 30) {
    throw new Error('listing-text.json is over the Play limits (title 30, short 80, full 4000).');
  }
  const {listings = []} = await call('GET', `${editUrl}/listings`);
  for (const {language} of listings) {
    await call('PUT', `${editUrl}/listings/${language}`, JSON.stringify({language, ...text}), 'application/json');
    console.log(`Listing ${language}: title, short and full description updated`);
  }
  const dir = new URL('docs/play/screenshots/', root);
  const shots = readdirSync(dir).filter((f) => f.endsWith('.png')).sort();
  if (shots.length < 2 || shots.length > 8) throw new Error(`Play takes 2-8 phone screenshots, found ${shots.length}.`);
  await call('DELETE', `${editUrl}/listings/en-IN/phoneScreenshots`);
  for (const shot of shots) {
    await call(
      'POST',
      `${uploadApi}/${packageName}/edits/${edit.id}/listings/en-IN/phoneScreenshots?uploadType=media`,
      readFileSync(new URL(shot, dir)),
      'image/png',
    );
  }
  console.log(`Screenshots en-IN: ${shots.join(', ')}`);
}

// Play refuses to send for review while the console holds other unsent
// changes; then the edit must skip review and be sent from Publishing overview.
// validate doesn't take the flag, so a dry run reports that case instead.
async function finish() {
  const query = flags['no-review'] ? '?changesNotSentForReview=true' : '';
  if (flags['dry-run']) {
    try {
      await call('POST', `${editUrl}:validate`);
      console.log('Dry run: the edit is valid and has been discarded. Nothing changed on Play.');
    } catch (e) {
      if (!e.message.includes('changesNotSentForReview')) throw e;
      console.log('Dry run: the edit is valid and has been discarded. Nothing changed on Play. '
        + 'Play Console has unsent changes, so the real run needs --no-review.');
    }
    return;
  }
  try {
    await call('POST', `${editUrl}:commit${query}`);
  } catch (e) {
    if (e.message.includes('changesNotSentForReview')) {
      throw new Error('Play Console has unsent changes, so this edit cannot be sent for review. '
        + 'Rerun with --no-review, then press "Send for review" in Publishing overview.');
    }
    throw e;
  }
  console.log(flags['no-review']
    ? 'Committed. Send the changes for review in Publishing overview.'
    : 'Committed.');
}

// The fenced block under the "## <version> (<code>)" heading, one <lang>…</lang> per language.
function notesFor(versionCode) {
  const doc = readFileSync(new URL('docs/play/release-notes.md', root), 'utf8');
  const section = doc.split(/^## /m).find((s) => s.split('\n')[0].includes(`(${versionCode})`));
  const block = section?.match(/```\n([\s\S]*?)```/)?.[1];
  if (!block) {
    throw new Error(`No release notes for version code ${versionCode} in docs/play/release-notes.md `
      + '(add a section, or pass --no-notes).');
  }
  const notes = [...block.matchAll(/<([A-Za-z-]+)>\n?([\s\S]*?)\n?<\/\1>/g)]
    .map(([, language, text]) => ({language, text: text.trim()}));
  for (const n of notes) {
    if (n.text.length > 500) throw new Error(`${n.language} release notes are over 500 characters.`);
  }
  return notes;
}

// Retries the occasional 5xx from the API, up to three attempts.
async function call(method, url, body, contentType) {
  let res;
  for (let attempt = 1; ; attempt++) {
    res = await fetch(url, {
      method,
      headers: contentType ? {...headers, 'Content-Type': contentType} : headers,
      body,
    });
    if (res.status < 500 || attempt === 3) break;
    await new Promise((r) => setTimeout(r, 2000 * attempt));
  }
  const json = res.status === 204 ? {} : await res.json();
  if (!res.ok) throw new Error(`${method} ${url.split('?')[0]}: ${res.status} ${json.error?.message ?? ''}`);
  return json;
}

// Service-account JWT exchanged for an OAuth token scoped to the Play publishing API.
async function accessToken(key) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const unsigned = `${b64({alg: 'RS256', typ: 'JWT'})}.${b64({
    iss: key.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: key.token_uri,
    iat: now,
    exp: now + 3600,
  })}`;
  const signature = createSign('RSA-SHA256').update(unsigned).sign(key.private_key, 'base64url');
  const res = await fetch(key.token_uri, {
    method: 'POST',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion: `${unsigned}.${signature}`,
    }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`Token request failed: ${res.status} ${json.error_description ?? json.error}`);
  return json.access_token;
}

function parseFlags(args) {
  const out = {};
  for (let i = 0; i < args.length; i++) {
    const name = args[i].replace(/^--/, '');
    const next = args[i + 1];
    if (next !== undefined && !next.startsWith('--')) {
      out[name] = next;
      i++;
    } else {
      out[name] = true;
    }
  }
  return out;
}
