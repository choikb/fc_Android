package com.example.myapplication

import android.os.Build
import androidx.annotation.RequiresApi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.charset.StandardCharsets
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object Aria256Crypto {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private const val SECRET_KEY = "dese!@F_FIRE_KBICT_C_SYS!@#^dese#"  // 32-byte key
    private const val SALT = "SHAKE_WITH_SALT!@#^dese#$"  // 임의의 SALT 값
    private const val ALGORITHM = "ARIA/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(SECRET_KEY.toCharArray(), SALT.toByteArray(), 65536, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "ARIA")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM, "BC")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(ByteArray(16)))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decrypt(encryptedText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM, "BC")
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(ByteArray(16)))
        val decodedBytes = Base64.getDecoder().decode(encryptedText)
        return String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
    }
}
