package io.github.nicolasfara.mktt.client

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal object NoTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
