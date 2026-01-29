/**
 * 保险箱认证管理器
 * 支持生物识别和自定义密码
 */
package com.example.myapplication.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class VaultAuthManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "vault_auth_prefs"
        private const val KEY_VAULT_PASSWORD = "vault_password"
        private const val KEY_USE_BIOMETRIC = "use_biometric"
        private const val KEY_VAULT_INITIALIZED = "vault_initialized"
    }
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * 检查保险箱是否已初始化（设置过密码）
     */
    fun isVaultInitialized(): Boolean {
        return encryptedPrefs.getBoolean(KEY_VAULT_INITIALIZED, false)
    }
    
    /**
     * 设置保险箱密码
     */
    fun setVaultPassword(password: String) {
        encryptedPrefs.edit()
            .putString(KEY_VAULT_PASSWORD, password)
            .putBoolean(KEY_VAULT_INITIALIZED, true)
            .apply()
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(password: String): Boolean {
        val savedPassword = encryptedPrefs.getString(KEY_VAULT_PASSWORD, null)
        return savedPassword == password
    }
    
    /**
     * 修改密码
     */
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (verifyPassword(oldPassword)) {
            setVaultPassword(newPassword)
            return true
        }
        return false
    }
    
    /**
     * 设置是否使用生物识别
     */
    fun setUseBiometric(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_USE_BIOMETRIC, enabled)
            .apply()
    }
    
    /**
     * 获取是否使用生物识别
     */
    fun useBiometric(): Boolean {
        return encryptedPrefs.getBoolean(KEY_USE_BIOMETRIC, true)
    }
    
    /**
     * 检查设备是否支持生物识别
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * 检查设备凭据是否可用（PIN/密码/图案）
     */
    fun isDeviceCredentialAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * 执行生物识别认证
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallbackToPassword: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        // 用户取消或点击了使用密码按钮
                        onFallbackToPassword()
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        // 没有生物识别，使用密码
                        onFallbackToPassword()
                    }
                    else -> {
                        onError(errString.toString())
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // 识别失败，但不退出，让用户重试
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("保险箱认证")
            .setSubtitle("使用生物识别解锁保险箱")
            .setNegativeButtonText("使用密码")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * 重置保险箱（清除密码）
     */
    fun resetVault() {
        encryptedPrefs.edit()
            .remove(KEY_VAULT_PASSWORD)
            .remove(KEY_VAULT_INITIALIZED)
            .remove(KEY_USE_BIOMETRIC)
            .apply()
    }
}
