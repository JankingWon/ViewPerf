package com.janking.viewperf

import android.util.Log

/**
 * 防止log被截断
 * @author jankingwon@foxmail.com
 * @since 2021/10/27
 */
object ContinuePrintLog {
    private const val SEGMENT_SIZE = 3 * 1024
    private const val PREFIX_CONTINUE = "-continue-"

    fun v(tag: String, msg: String) {
        continuePrint(tag, msg) { _tag, _msg ->
            Log.v(_tag, _msg)
        }
    }

    fun d(tag: String, msg: String) {
        continuePrint(tag, msg) { _tag, _msg ->
            Log.d(_tag, _msg)
        }
    }

    fun i(tag: String, msg: String) {
        continuePrint(tag, msg) { _tag, _msg ->
            Log.i(_tag, _msg)
        }
    }

    fun w(tag: String, msg: String) {
        continuePrint(tag, msg) { _tag, _msg ->
            Log.w(_tag, _msg)
        }
    }

    fun e(tag: String, msg: String) {
        continuePrint(tag, msg) { _tag, _msg ->
            Log.e(_tag, _msg)
        }
    }

    private fun continuePrint(tag: String, msg: String, action: (String, String) -> Unit) {
        if (msg.length <= SEGMENT_SIZE) {
            action.invoke(tag, msg)
        } else {
            // 分段打印日志
            var restMsg = msg
            var firstLine = true
            while (restMsg.length > SEGMENT_SIZE) {
                // 最好是在换行的地方截断
                var endIndex = SEGMENT_SIZE
                for (i in SEGMENT_SIZE downTo 1) {
                    if (restMsg[i - 1] == '\n') {
                        endIndex = i
                        break
                    }
                }
                val logContent = restMsg.substring(0, endIndex)
                restMsg = restMsg.replace(logContent, "")
                if (firstLine) {
                    action.invoke(tag, logContent)
                    firstLine = false
                } else {
                    action.invoke(tag, "$PREFIX_CONTINUE\n$logContent")
                }
            }
            action.invoke(tag, "$PREFIX_CONTINUE\n$restMsg")
        }
    }
}