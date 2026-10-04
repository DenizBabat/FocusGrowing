package com.focusgrowing.app.core.billing

import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Checks that a purchase really comes from Google Play, using the app's licensing public key.
 * This is the recommended check for apps without their own server. (With a backend, verify the
 * purchase token with the Google Play Developer API instead.)
 */
object PurchaseVerifier {

    /** Returns true when no key is configured (testing), or when the signature is valid. */
    fun isValid(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (base64PublicKey.isBlank()) return true
        if (signedData.isBlank() || signature.isBlank()) return false
        return try {
            val keyBytes = Base64.decode(base64PublicKey, Base64.DEFAULT)
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            Signature.getInstance("SHA1withRSA").run {
                initVerify(publicKey)
                update(signedData.toByteArray())
                verify(Base64.decode(signature, Base64.DEFAULT))
            }
        } catch (_: Exception) {
            false
        }
    }
}
