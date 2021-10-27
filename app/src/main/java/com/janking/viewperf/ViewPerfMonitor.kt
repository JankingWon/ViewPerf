package com.janking.viewperf

import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.lang.StringBuilder
import java.lang.ref.WeakReference

/** Returns a [MutableIterator] over the views in this view group. */
operator fun ViewGroup.iterator() = object : MutableIterator<View> {
    private var index = 0
    override fun hasNext() = index < childCount
    override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() = removeViewAt(--index)
}

/** Returns a [Sequence] over the child views in this view group. */
val ViewGroup.children: Sequence<View>
    get() = object : Sequence<View> {
        override fun iterator() = this@children.iterator()
    }

/**
 * 把布局数据存入tag
 */
var View.stepSet: StepSet
    get() = (this.getTag(R.id.view_perf_dataset) as? StepSet) ?: StepSet().also {
        stepSet = it
    }
    set(value) {
        this.setTag(R.id.view_perf_dataset, value)
    }

/**
 * 递归清除数据
 */
fun View.clearStepSet() {
    stepSet.clear()
    if (this is ViewGroup) {
        children.forEach {
            it.clearStepSet()
        }
    }
}

class StepData {
    /**
     * 次数
     */
    var count = 0

    /**
     * 总共的花费的毫秒数
     */
    var cost = 0L

    /**
     * 上次开始的时间
     */
    var lastBeginTs = 0L

    /**
     * 触发进入的次数
     */
    var triggerBegin = 0

    fun begin() {
        // 如果多次进入begin，以第一次为准
        if (lastBeginTs == 0L) {
            lastBeginTs = SystemClock.elapsedRealtime()
        }
        triggerBegin++
    }

    fun end() {
        triggerBegin--
        if (triggerBegin == 0) {
            // 最后进入结束，视为一次有效的过程
            count++
            cost += SystemClock.elapsedRealtime() - lastBeginTs
            lastBeginTs = 0
        }
    }

    fun clear() {
        count = 0
        cost = 0L
        lastBeginTs = 0L
        triggerBegin = 0
    }

    override fun toString(): String {
        return "StepData(count=$count, cost=$cost, lastBeginTs=$lastBeginTs, triggerBegin=$triggerBegin)"
    }

}

enum class Step {
    Measure,
    Layout,
    Draw
}

open class StepSet {
    val measureData = StepData()
    val layoutData = StepData()
    val drawData = StepData()

    open fun clear() {
        measureData.clear()
        layoutData.clear()
        drawData.clear()
    }

    override fun toString(): String {
        return "StepSet(measureData=$measureData, layoutData=$layoutData, drawData=$drawData)"
    }
}

class ViewRootImplStepSet : StepSet() {
    /**
     * 多了一个全局traversal的时长
     */
    val globalData = StepData()

    override fun clear() {
        super.clear()
        globalData.clear()
    }
}

object ViewPerfMonitor {
    /**
     * 虽然不会出问题，还是用弱引用规范点
     */
    private var rootViewRef: WeakReference<View>? = null
    private val viewRootImplStepSet = ViewRootImplStepSet()

    fun performTraversal(root: View?) {
        root?.run {
            forceLayout(this)
            requestLayout()
        }
    }

    private fun forceLayout(view: View?) {
        view?.run {
            forceLayout()
            if (this is ViewGroup) {
                this.children.forEach {
                    forceLayout(it)
                }
            }
        }

    }

    /**
     * 开始记录
     */
    @JvmStatic
    fun startTraversal(root: View) {
        rootViewRef = WeakReference(root)
        viewRootImplStepSet.globalData.begin()
    }

    @JvmStatic
    fun stopTraversal(): String {
        viewRootImplStepSet.globalData.end()
        return getResult().also {
            viewRootImplStepSet.clear()
            rootViewRef?.get()?.clearStepSet()
        }
    }

    @JvmStatic
    fun beginViewRootImplStep(step: Step) {
        beginStep(viewRootImplStepSet, step)
    }

    @JvmStatic
    fun endViewRootImplStep(step: Step) {
        endStep(viewRootImplStepSet, step)
    }

    @JvmStatic
    fun beginViewStep(v: View, step: Step) {
        beginStep(v.stepSet, step)
    }

    @JvmStatic
    fun endViewStep(v: View, step: Step) {
        endStep(v.stepSet, step)
    }

    private fun beginStep(node: StepSet, step: Step) {
        when (step) {
            Step.Measure -> node.measureData.begin()
            Step.Layout -> node.layoutData.begin()
            Step.Draw -> node.drawData.begin()
        }
    }

    private fun endStep(node: StepSet, step: Step) {
        when (step) {
            Step.Measure -> node.measureData.end()
            Step.Layout -> node.layoutData.end()
            Step.Draw -> node.drawData.end()
        }
    }

    @JvmStatic
    private fun getResult(): String {
        return buildViewTreeOutput(
            rootViewRef?.get(),
            "*",
            buildViewRootImplOutput(StringBuilder())
        ).also { result ->
            if (result.isNotEmpty()) {
                val context = rootViewRef?.get()?.context
                val appNameId = context?.applicationInfo?.labelRes ?: 0
                val appName = if (appNameId == 0) context?.applicationInfo?.nonLocalizedLabel.toString() else context?.getString(appNameId)
                when {
                    viewRootImplStepSet.globalData.cost > 33 -> {
                        Log.e("ViewPerfMonitor", "[$appName]->$result")
                    }
                    viewRootImplStepSet.globalData.cost > 16 -> {
                        Log.w("ViewPerfMonitor", "[$appName]->$result")
                    }
                    viewRootImplStepSet.globalData.cost > 1 -> {
                        Log.i("ViewPerfMonitor", "[$appName]->$result")
                    }
                    else -> {
                        Log.d("ViewPerfMonitor", "[$appName]->$result")
                    }
                }
            }
        }
    }

    private fun buildViewRootImplOutput(stringBuilder: StringBuilder): StringBuilder {
        return stringBuilder.append(
            "[${viewRootImplStepSet.globalData.cost}ms](m:${viewRootImplStepSet.measureData.cost}ms,l:${viewRootImplStepSet.layoutData.cost}ms,d:${viewRootImplStepSet.drawData.cost}ms)${rootViewRef?.get()}\n"
        )
    }

    private fun buildViewTreeOutput(
        view: View?,
        prefix: String,
        stringBuilder: StringBuilder
    ): String {
        return view?.let {
            val stepSet = it.stepSet
            if (stepSet.measureData.count == 0 && stepSet.layoutData.count == 0 && stepSet.drawData.count == 0) {
                return ""
            }
            stringBuilder.append(
                "$prefix [${stepSet.measureData.count}](${stepSet.measureData.cost}ms,${stepSet.layoutData.cost}ms,${stepSet.drawData.cost}ms)${it.javaClass.simpleName.toString()}(${
                    getNameFromId(it)
                })\n"
            )
            (it as? ViewGroup)?.children?.forEach { child ->
                buildViewTreeOutput(child, "  $prefix", stringBuilder)
            }
            return stringBuilder.toString()
        } ?: ""
    }

    private fun getNameFromId(view: View?): String {
        return try {
            "@id/${view?.resources?.getResourceEntryName(view.id)}"
        } catch (ignore: Exception) {
            ""
        }
    }
}