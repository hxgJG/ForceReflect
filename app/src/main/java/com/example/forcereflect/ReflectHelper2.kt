package com.example.forcereflect

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

import java.lang.reflect.Method

import android.os.Build.VERSION.PREVIEW_SDK_INT
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P

object ReflectHelper2 {
    private val TAG = "ReflectHelper"
    private val UNKNOWN = -9999
    private val ERROR = -20
    private var sVmRuntime: Any? = null
    private var setHiddenApiExemptions: Method? = null
    private var unsealed = UNKNOWN

    init {
        try {
            val forName = Class::class.java.getDeclaredMethod("forName", java.lang.String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod",
                java.lang.String::class.java,
                arrayOf<Class<*>>()::class.java
            )

            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
            setHiddenApiExemptions = getDeclaredMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf<Class<*>>(Array<String>::class.java)
            ) as Method
            sVmRuntime = getRuntime.invoke(null)
        } catch (e: Throwable) {
            Log.e(TAG, "reflect bootstrap failed:", e)
        }

    }

    fun unseal(context: Context?) {
        if (SDK_INT < 28) {
            // Below Android P, ignore
            return
        }

        // try exempt API first.
        if (exemptAll()) {
            return
        }

        if (context == null) {
            return
        }

        val applicationInfo = context.applicationInfo

        synchronized(ReflectHelper2::class.java) {
            if (unsealed != UNKNOWN) {
                return
            }

            unsealed = 0

            if (SDK_INT == P && PREVIEW_SDK_INT > 0 || SDK_INT > P) {
                return
            }

            // Android P, we need to sync the flags with ApplicationInfo
            // We needn't to this on Android Q.
            try {
                @SuppressLint("PrivateApi")
                val setHiddenApiEnforcementPolicy = ApplicationInfo::class.java
                    .getDeclaredMethod("setHiddenApiEnforcementPolicy", Int::class.javaPrimitiveType!!)
                setHiddenApiEnforcementPolicy.invoke(applicationInfo, 0)
            } catch (e: Throwable) {
                e.printStackTrace()
                unsealed = ERROR
            }
        }
    }

    /**
     * make the method exempted from hidden API check.
     *
     * @param method the method signature prefix.
     * @return true if success.
     */
    fun exempt(method: String): Boolean {
        return doExempt(method)
    }

    /**
     * make specific methods exempted from hidden API check.
     *
     * @param methods the method signature prefix, such as "Ldalvik/system", "Landroid" or even "L"
     * @return true if success
     */
    private fun doExempt(vararg methods: String): Boolean {
        if (sVmRuntime == null || setHiddenApiExemptions == null) {
            return false
        }

        return try {
            setHiddenApiExemptions!!.invoke(sVmRuntime, *arrayOf<Any>(methods))
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Make all hidden API exempted.
     *
     * @return true if success.
     */
    private fun exemptAll(): Boolean {
        return doExempt("L")
    }
}
