package com.newritage.app.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    // 16바이트 하드코딩 키 (시연용)
    private val BLE_SECRET_KEY = "NewRitageSecure1".toByteArray() // 16 bytes for AES-128
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16

    fun decryptBleData(encryptedData: ByteArray): String? {
        return try {
            if (encryptedData.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) return null

            val iv = encryptedData.sliceArray(0 until GCM_IV_LENGTH)
            val ciphertext = encryptedData.sliceArray(GCM_IV_LENGTH until encryptedData.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            val keySpec = SecretKeySpec(BLE_SECRET_KEY, "AES")

            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            decryptedBytes.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val hashedBytes = md.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    fun verifyPassword(password: String, salt: String, hash: String): Boolean {
        return hashPassword(password, salt) == hash
    }
}
