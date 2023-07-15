package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.core.BuildConfig
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

/* SY --> */
open /* SY <-- */ class NetworkHelper(
    context: Context,
    private val preferences: NetworkPreferences,
) {

    private val cacheDir = File(context.cacheDir, "network_cache")
    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    /* SY --> */
    open /* SY <-- */val cookieJar = AndroidCookieJar()

    private val userAgentInterceptor by lazy {
        UserAgentInterceptor(::defaultUserAgentProvider)
    }
    private val cloudflareInterceptor by lazy {
        CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider)
    }

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(2, TimeUnit.MINUTES)
                .addInterceptor(UncaughtExceptionInterceptor())
                .addInterceptor(userAgentInterceptor)

            if (BuildConfig.DEBUG) {
                val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                builder.addNetworkInterceptor(httpLoggingInterceptor)
            }

            builder.addInterceptor(cloudflareInterceptor)

            when (preferences.dohProvider().get()) {
                PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
                PREF_DOH_GOOGLE -> builder.dohGoogle()
                PREF_DOH_ADGUARD -> builder.dohAdGuard()
                PREF_DOH_QUAD9 -> builder.dohQuad9()
                PREF_DOH_ALIDNS -> builder.dohAliDNS()
                PREF_DOH_DNSPOD -> builder.dohDNSPod()
                PREF_DOH_360 -> builder.doh360()
                PREF_DOH_QUAD101 -> builder.dohQuad101()
                PREF_DOH_MULLVAD -> builder.dohMullvad()
                PREF_DOH_CONTROLD -> builder.dohControlD()
                PREF_DOH_NJALLA -> builder.dohNajalla()
                PREF_DOH_SHECAN -> builder.dohShecan()
            }

            return builder
        }

    /* SY --> */
    open /* SY <-- */val client by lazy { baseClientBuilder.cache(Cache(cacheDir, cacheSize)).build() }

    /**
     * @deprecated Since extension-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    /* SY --> */
    open /* SY <-- */val cloudflareClient by lazy {
        client
    }

    fun defaultUserAgentProvider() = preferences.defaultUserAgent().get().trim()
}
