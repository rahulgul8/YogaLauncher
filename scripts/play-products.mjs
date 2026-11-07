// Creates or lists Yoga Pro's Play products with the TUWI service account:
//   node scripts/play-products.mjs list
//   node scripts/play-products.mjs create [--dry-run]
// Prices: India in INR, every other region converted by Play from the USD price.
// The key is read from PLAY_SERVICE_ACCOUNT_JSON, or ~/.secrets/play-service-account.json.
import {readFileSync} from 'node:fs';
import {homedir} from 'node:os';
import {join} from 'node:path';
import {createSign} from 'node:crypto';

const packageName = 'com.tuwi.yogaphone';
const api = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}`;

const BENEFITS = ['All looks and both app icons', 'Value of time and the live pill', 'Unlimited allowances and focus', 'Insights, reports and widgets'];
const PLANS = [
  {productId: 'yoga_pro_yearly', basePlanId: 'yearly', period: 'P1Y', inr: 999, usd: 24.99,
    title: 'Yoga Pro (yearly)', description: 'Every Yoga Pro feature, billed once a year.'},
  {productId: 'yoga_pro_monthly', basePlanId: 'monthly', period: 'P1M', inr: 149, usd: 3.99,
    title: 'Yoga Pro (monthly)', description: 'Every Yoga Pro feature, billed monthly.'},
];
const LIFETIME = {sku: 'yoga_pro_lifetime', inr: 2499, usd: 59.99,
  title: 'Yoga Pro (lifetime)', description: 'Every Yoga Pro feature, paid once and yours to keep.'};

const [command, ...rest] = process.argv.slice(2);
const dryRun = rest.includes('--dry-run');
if (!['list', 'create'].includes(command)) {
  console.error('usage: node scripts/play-products.mjs list | create [--dry-run]');
  process.exit(2);
}
const keyPath = process.env.PLAY_SERVICE_ACCOUNT_JSON ?? join(homedir(), '.secrets', 'play-service-account.json');
const key = JSON.parse(readFileSync(keyPath, 'utf8'));
const headers = {Authorization: `Bearer ${await accessToken(key)}`, 'Content-Type': 'application/json'};

if (command === 'list') {
  const subs = await call('GET', `${api}/subscriptions`);
  for (const s of subs.subscriptions ?? []) {
    console.log(`subscription ${s.productId}: ${(s.basePlans ?? []).map((b) => `${b.basePlanId} ${b.state}`).join(', ')}`);
  }
  const once = await call('GET', `${api}/oneTimeProducts`);
  for (const p of once.oneTimeProducts ?? []) {
    console.log(`one-time ${p.productId}: ${(p.purchaseOptions ?? []).map((o) => `${o.purchaseOptionId} ${o.state}`).join(', ')}`);
  }
} else {
  const language = await defaultLanguage();
  console.log(`Default listing language: ${language}`);
  for (const plan of PLANS) await createSubscription(plan, language);
  await createLifetime(language);
}

async function createSubscription(plan, language) {
  const existing = await call('GET', `${api}/subscriptions/${plan.productId}`, undefined, true);
  if (existing) { console.log(`${plan.productId} already exists; skipped`); return; }
  const converted = await call('POST', `${api}/pricing:convertRegionPrices`,
    JSON.stringify({price: money('USD', plan.usd)}));
  const regionalConfigs = Object.values(converted.convertedRegionPrices ?? {}).map((r) => ({
    regionCode: r.regionCode, newSubscriberAvailability: true,
    price: r.regionCode === 'IN' ? money('INR', plan.inr) : r.price,
  }));
  const body = {
    packageName, productId: plan.productId,
    listings: [{languageCode: language, title: plan.title, benefits: BENEFITS, description: plan.description}],
    basePlans: [{
      basePlanId: plan.basePlanId,
      autoRenewingBasePlanType: {billingPeriodDuration: plan.period, gracePeriodDuration: 'P7D',
        resubscribeState: 'RESUBSCRIBE_STATE_ACTIVE', legacyCompatible: true},
      regionalConfigs,
      otherRegionsConfig: {usdPrice: money('USD', plan.usd), eurPrice: converted.convertedOtherRegionsPrice?.eurPrice,
        newSubscriberAvailability: true},
    }],
  };
  console.log(`${plan.productId}: ${regionalConfigs.length} regions, IN ₹${plan.inr}, US $${plan.usd}`);
  if (dryRun) return;
  const regions = converted.regionVersion?.version ?? '2026/01';
  await call('POST', `${api}/subscriptions?productId=${plan.productId}&regionsVersion.version=${regions}`, JSON.stringify(body));
  await call('POST', `${api}/subscriptions/${plan.productId}/basePlans/${plan.basePlanId}:activate`,
    JSON.stringify({packageName, productId: plan.productId, basePlanId: plan.basePlanId}));
  console.log(`  created and activated`);
}

async function createLifetime(language) {
  const existing = await call('GET', `${api}/oneTimeProducts/${LIFETIME.sku}`, undefined, true);
  if (existing) { console.log(`${LIFETIME.sku} already exists; skipped`); return; }
  const converted = await call('POST', `${api}/pricing:convertRegionPrices`, JSON.stringify({price: money('USD', LIFETIME.usd)}));
  const regional = Object.values(converted.convertedRegionPrices ?? {}).map((r) => ({
    regionCode: r.regionCode, availability: 'AVAILABLE',
    price: r.regionCode === 'IN' ? money('INR', LIFETIME.inr) : r.price,
  }));
  const body = {
    packageName, productId: LIFETIME.sku,
    listings: [{languageCode: language, title: LIFETIME.title, description: LIFETIME.description}],
    purchaseOptions: [{
      purchaseOptionId: 'lifetime',
      buyOption: {legacyCompatible: true, multiQuantityEnabled: false},
      regionalPricingAndAvailabilityConfigs: regional,
      newRegionsConfig: {usdPrice: money('USD', LIFETIME.usd), eurPrice: converted.convertedOtherRegionsPrice?.eurPrice,
        availability: 'AVAILABLE'},
    }],
  };
  console.log(`${LIFETIME.sku}: ${regional.length} regions, IN ₹${LIFETIME.inr}, US $${LIFETIME.usd}`);
  if (dryRun) return;
  await call('PATCH', `${api}/onetimeproducts/${LIFETIME.sku}?allowMissing=true&updateMask=listings,purchaseOptions`
    + `&regionsVersion.version=${converted.regionVersion?.version ?? '2026/01'}`, JSON.stringify(body));
  await call('POST', `${api}/oneTimeProducts/${LIFETIME.sku}/purchaseOptions:batchUpdateStates`, JSON.stringify({requests: [
    {activatePurchaseOptionRequest: {packageName, productId: LIFETIME.sku, purchaseOptionId: 'lifetime'}}]}));
  console.log('  created and activated');
}

async function defaultLanguage() {
  const edit = await call('POST', `${api}/edits`, '{}');
  try {
    return (await call('GET', `${api}/edits/${edit.id}/details`)).defaultLanguage ?? 'en-US';
  } finally {
    await call('DELETE', `${api}/edits/${edit.id}`);
  }
}

function money(currencyCode, amount) {
  const units = Math.floor(amount);
  return {currencyCode, units: String(units), nanos: Math.round((amount - units) * 1e9)};
}

async function call(method, url, body, allow404 = false) {
  let res;
  for (let attempt = 1; ; attempt++) {
    res = await fetch(url, {method, headers, body});
    if (res.status < 500 || attempt === 3) break;
    await new Promise((r) => setTimeout(r, 2000 * attempt));
  }
  if (allow404 && res.status === 404) return null;
  const json = res.status === 204 ? {} : await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(`${method} ${url.split('?')[0]}: ${res.status} ${json.error?.message ?? ''}`);
  return json;
}

async function accessToken(key) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const unsigned = `${b64({alg: 'RS256', typ: 'JWT'})}.${b64({
    iss: key.client_email, scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: key.token_uri, iat: now, exp: now + 3600,
  })}`;
  const signature = createSign('RSA-SHA256').update(unsigned).sign(key.private_key, 'base64url');
  const res = await fetch(key.token_uri, {
    method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    body: new URLSearchParams({grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: `${unsigned}.${signature}`}),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`Token request failed: ${res.status} ${json.error_description ?? json.error}`);
  return json.access_token;
}
