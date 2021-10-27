package com.janking.viewperf

import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import java.lang.StringBuilder

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
}

enum class Step {
    Measure,
    Layout,
    Draw
}

open class SimpleNode {
    val measureData = StepData()
    val layoutData = StepData()
    val drawData = StepData()

    open fun clear() {
        measureData.clear()
        layoutData.clear()
        drawData.clear()
    }
}

class ViewRootImplNode : SimpleNode() {
    val globalData = StepData()

    override fun clear() {
        super.clear()
        globalData.clear()
    }
}

class ViewNode : SimpleNode() {
    var view: View? = null
    var children: List<ViewNode>? = null

    fun setView(v: View, forceLayout: Boolean) {
        view = v
        children = if (v is ViewGroup) {
            // 添加孩子节点
            v.children.toList().map {
                // 转换成node
                ViewNode().apply {
                    if (forceLayout) {
                        it.forceLayout()
                    }
                    setView(it, forceLayout)
                }
            }
        } else {
            null
        }
    }

    override fun clear() {
        super.clear()
        view = null
        children = null
    }
}

object ViewPerfMonitor {
    private val rootNode = ViewNode()
    private val viewRootImplNode = ViewRootImplNode()

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
        rootNode.setView(root, false)
        viewRootImplNode.globalData.begin()
    }

    @JvmStatic
    fun stopTraversal(): String {
        viewRootImplNode.globalData.end()
        return getResult().also {
            viewRootImplNode.clear()
            rootNode.clear()
        }
    }

    @JvmStatic
    fun beginViewRootImplStep(step: Step) {
        beginStep(viewRootImplNode, step)
    }

    @JvmStatic
    fun endViewRootImplStep(step: Step) {
        endStep(viewRootImplNode, step)
    }

    @JvmStatic
    fun beginViewStep(v: View, step: Step) {
        recursionFindNode(rootNode, v)?.let {
            beginStep(it, step)
        }
    }

    @JvmStatic
    fun endViewStep(v: View, step: Step) {
        recursionFindNode(rootNode, v)?.let {
            endStep(it, step)
        }
    }

    private fun beginStep(node: SimpleNode, step: Step) {
        when (step) {
            Step.Measure -> node.measureData.begin()
            Step.Layout -> node.layoutData.begin()
            Step.Draw -> node.drawData.begin()
        }
    }

    private fun endStep(node: SimpleNode, step: Step) {
        when (step) {
            Step.Measure -> node.measureData.end()
            Step.Layout -> node.layoutData.end()
            Step.Draw -> node.drawData.end()
        }
    }


    private fun recursionFindNode(node: ViewNode, view: View): ViewNode? {
        if (node.view == view) {
            return node
        }
        node.children?.forEach { it ->
            recursionFindNode(it, view)?.let {
                return it
            }
        }
        return null
    }

    @JvmStatic
    private fun getResult(): String {
        return recursionBuildOutput(rootNode, "*", buildViewRootImplOutput(StringBuilder())).also { result ->
            if (result.isNotEmpty()) {
                when {
                    viewRootImplNode.globalData.cost > 33 -> {
                        Log.e("ViewPerfMonitor", "TraversalResult->$result")
                    }
                    viewRootImplNode.globalData.cost > 16 -> {
                        Log.w("ViewPerfMonitor", "TraversalResult->$result")
                    }
                    viewRootImplNode.globalData.cost > 1 -> {
                        Log.i("ViewPerfMonitor", "TraversalResult->$result")
                    }
                    else -> {
                        Log.d("ViewPerfMonitor", "TraversalResult->$result")
                    }
                }
            }
        }
    }

    private fun buildViewRootImplOutput(stringBuilder: StringBuilder) : StringBuilder {
        return stringBuilder.append(
            "[${viewRootImplNode.globalData.cost}ms](m:${viewRootImplNode.measureData.cost}ms,l:${viewRootImplNode.layoutData.cost}ms,d:${viewRootImplNode.drawData.cost}ms)${rootNode.view}\n"
        )
    }

    private fun recursionBuildOutput(
        node: ViewNode,
        prefix: String,
        stringBuilder: StringBuilder
    ): String {
        if (node.measureData.count == 0 && node.layoutData.count == 0 && node.drawData.count == 0) {
            return ""
        }
        stringBuilder.append(
            "$prefix [${node.measureData.count}](${node.measureData.cost}ms,${node.layoutData.cost}ms,${node.drawData.cost}ms)${node.view?.javaClass?.simpleName.toString()}(${
                getNameFromId(
                    node.view
                )
            })\n"
        )
        node.children?.forEach {
            recursionBuildOutput(it, "  $prefix", stringBuilder)
        }
        return stringBuilder.toString()
    }

    private fun getNameFromId(view: View?): String {
        return try {
            "@id/${view?.resources?.getResourceEntryName(view.id)}"
        } catch (ignore: Exception) {
            ""
        }
    }
}