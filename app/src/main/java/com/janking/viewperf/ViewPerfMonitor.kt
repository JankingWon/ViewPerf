package com.janking.viewperf

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
     * onMeasure的次数
     */
    var measureCount = 0

    /**
     * 总共的onMeasure花费的毫秒数
     */
    var measureCost = 0L

    /**
     * 上次onMeasure开始的时间
     */
    var lastMeasureTs = 0L

    /**
     * 触发了measure进入的次数
     */
    var triggerBegin = 0

    fun begin() {
        // 如果多次进入begin，以第一次为准
        if (lastMeasureTs == 0L) {
            lastMeasureTs = System.currentTimeMillis()
        }
        triggerBegin++
    }

    fun end() {
        triggerBegin--
        if (triggerBegin == 0) {
            // 最后进入结束，视为一次有效的measure过程
            measureCount++
            measureCost += System.currentTimeMillis() - lastMeasureTs
            lastMeasureTs = 0
        }
    }

    fun clear() {
        measureCount = 0
        measureCost = 0L
        lastMeasureTs = 0L
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
            rootNode.setView(this, true)
            requestLayout()
            post {
                getResult()
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
                    viewRootImplNode.globalData.measureCost > 33 -> {
                        Log.e("ViewPerfMonitor", "TraversalResult->$result")
                    }
                    viewRootImplNode.globalData.measureCost > 16 -> {
                        Log.w("ViewPerfMonitor", "TraversalResult->$result")
                    }
                    viewRootImplNode.globalData.measureCost > 1 -> {
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
            "[${viewRootImplNode.globalData.measureCost}ms](m:${viewRootImplNode.measureData.measureCost}ms,l:${viewRootImplNode.layoutData.measureCost}ms,d:${viewRootImplNode.drawData.measureCost}ms)${rootNode.view}\n"
        )
    }

    private fun recursionBuildOutput(
        node: ViewNode,
        prefix: String,
        stringBuilder: StringBuilder
    ): String {
        if (node.measureData.measureCount == 0 && node.layoutData.measureCount == 0 && node.drawData.measureCount == 0) {
            return ""
        }
        stringBuilder.append(
            "$prefix [${node.measureData.measureCount}](${node.measureData.measureCost}ms,${node.layoutData.measureCost}ms,${node.drawData.measureCost}ms)${node.view?.javaClass?.simpleName.toString()}(${
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