package `in`.tuwi.yogaphone.pro

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class ProProduct(val plan: ProPlan, val details: ProductDetails, val price: String, val offerToken: String?)

/** Uses only Billing's Java artifact. Purchase callbacks never revoke cached ownership. */
class BillingClientWrapper(
    context: Context,
    private val scope: CoroutineScope,
    private val onPurchases: suspend (List<Purchase>, Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private val connection = Mutex()
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> scope.launch { onPurchases(purchases.orEmpty(), false) }
                BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Purchase cancelled")
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore() }
                else -> onMessage("Yoga Pro isn't available yet")
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private suspend fun connect(): Boolean = connection.withLock {
        if (client.isReady) return@withLock true
        withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    client.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                        override fun onBillingServiceDisconnected() {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    })
                } catch (_: Exception) { if (continuation.isActive) continuation.resume(false) }
            }
        } ?: false
    }

    suspend fun products(): List<ProProduct> {
        if (!connect()) return emptyList()
        val yearly = queryProduct("yoga_pro_yearly", BillingClient.ProductType.SUBS)?.let { details ->
            // A base plan has no offerId. Do not select an introductory discount or trial offer.
            val offer = details.subscriptionOfferDetails?.firstOrNull { it.offerId == null && it.pricingPhases.pricingPhaseList.lastOrNull()?.billingPeriod == "P1Y" }
            val phase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
            if (offer == null || phase == null) null else ProProduct(ProPlan.YEARLY, details, phase.formattedPrice, offer.offerToken)
        }
        val lifetime = queryProduct("yoga_pro_lifetime", BillingClient.ProductType.INAPP)?.let { details ->
            details.oneTimePurchaseOfferDetails?.let { ProProduct(ProPlan.LIFETIME, details, it.formattedPrice, null) }
        }
        return listOfNotNull(yearly, lifetime)
    }

    private suspend fun queryProduct(id: String, type: String): ProductDetails? = withTimeoutOrNull(15_000) {
        suspendCancellableCoroutine { continuation ->
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build())).build()
            try {
                client.queryProductDetailsAsync(params) { result, details ->
                    if (continuation.isActive) continuation.resume(
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) details.productDetailsList.firstOrNull() else null)
                }
            } catch (_: Exception) { if (continuation.isActive) continuation.resume(null) }
        }
    }

    suspend fun restore(): Boolean {
        if (!connect()) return false
        val subscriptions = queryPurchases(BillingClient.ProductType.SUBS)
        val lifetime = queryPurchases(BillingClient.ProductType.INAPP)
        if (subscriptions == null || lifetime == null) return false
        onPurchases(subscriptions + lifetime, true)
        return true
    }

    private suspend fun queryPurchases(type: String): List<Purchase>? = withTimeoutOrNull(15_000) {
        suspendCancellableCoroutine { continuation ->
            try {
                client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                    if (continuation.isActive) continuation.resume(if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else null)
                }
            } catch (_: Exception) { if (continuation.isActive) continuation.resume(null) }
        }
    }

    fun buy(activity: Activity, product: ProProduct) {
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product.details)
        product.offerToken?.let(item::setOfferToken)
        val result = runCatching { client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item.build())).build()) }.getOrNull()
        when (result?.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Purchase cancelled")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { restore() }
            else -> onMessage("Yoga Pro isn't available yet")
        }
    }

    suspend fun acknowledge(purchase: Purchase): Boolean {
        if (purchase.isAcknowledged) return true
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result ->
                        if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }
                } catch (_: Exception) { if (continuation.isActive) continuation.resume(false) }
            }
        } ?: false
    }
}
