# ViewPerf
[Xposed模块]检测View测量、布局和绘制性能

- 可以显示View树
- 显示每次performTraversal耗时，以及performMeasure、performLayout、performDraw耗时
- 显示每个节点的measure、layout、draw耗时
- 显示每个节点的measure次数

## 使用

- 结果会在logcat以树形图打印出来，日志TAG为ViewPerfMonitor
- 第一行表示ViewRootImpl各个阶段的耗时，格式为`应用名->[traversal耗时](m:performMeasure耗时,l:performLayout耗时,d:performDraw耗时`
- 每个节点的格式为`[measure次数](measure耗时，layout耗时，draw耗时)View类名(view id)`
- traversal性能不同打印的日志类型也会不同
  - 耗时1毫秒以内日志类型是debug
  - 耗时16毫秒以内日志类型是info
  - 耗时33毫秒以内日志类型是warn
  - 大于33毫秒日志类型是error

```
2021-11-01 20:19:37.665 27071-27071/? W/ViewPerfMonitor: [拼多多]->[24ms](m:13ms,l:6ms,d:0ms)DecorView@5db6f0a[HomeActivity]
    * [2](13ms,6ms,0ms)DecorView()
      * [2](13ms,6ms,0ms)LinearLayout()
        * [2](13ms,6ms,0ms)FrameLayout(@id/content)
          * [2](13ms,6ms,0ms)MainFrameContainerView(@id/pdd)
            * [4](10ms,5ms,0ms)FrameLayout(@id/pdd)
              * [4](10ms,5ms,0ms)FrameLayout()
                * [4](10ms,5ms,0ms)LinearLayout(@id/pdd)
                  * [4](0ms,0ms,0ms)SearchBarPlaceholderLayout(@id/pdd)
                    * [4](0ms,0ms,0ms)View()
                  * [4](2ms,0ms,0ms)TabPlaceHolderLayout(@id/pdd)
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](1ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                    * [4](0ms,0ms,0ms)TextView()
                  * [4](0ms,0ms,0ms)View(@id/pdd)
                  * [4](8ms,5ms,0ms)CustomViewPager(@id/pdd)
                    * [5](0ms,4ms,0ms)FrameLayout()
                      * [5](0ms,4ms,0ms)ProductListView(@id/pdd)
                        * [1](0ms,0ms,0ms)LoadingHeader(@id/pdd)
                          * [1](0ms,0ms,0ms)ImageView(@id/pdd)
                        * [1](0ms,0ms,0ms)View()
            * [4](0ms,0ms,0ms)View(@id/pdd)
            * [4](3ms,1ms,0ms)PddTabView(@id/pdd)
              * [8](1ms,0ms,0ms)TabRelativeLayout()
                * [16](0ms,0ms,0ms)TextView(@id/pdd)
                * [16](0ms,0ms,0ms)ImageView(@id/pdd)
              * [8](1ms,1ms,0ms)TabRelativeLayout()
                * [16](1ms,0ms,0ms)TextView(@id/pdd)
                * [16](0ms,0ms,0ms)ImageView(@id/pdd)
              * [8](0ms,0ms,0ms)TabRelativeLayout()
                * [16](0ms,0ms,0ms)TextView(@id/pdd)
                * [16](0ms,0ms,0ms)ImageView(@id/pdd)
              * [8](0ms,0ms,0ms)TabRelativeLayout()
                * [16](0ms,0ms,0ms)TextView(@id/pdd)
                * [16](0ms,0ms,0ms)ImageView(@id/pdd)
              * [8](0ms,0ms,0ms)TabRelativeLayout()
                * [16](0ms,0ms,0ms)TextView(@id/pdd)
                * [16](0ms,0ms,0ms)ImageView(@id/pdd)
          * [2](0ms,0ms,0ms)StatusBarHolderView(@id/pdd)
      * [2](0ms,0ms,0ms)View()
      * [2](0ms,0ms,0ms)View(@id/navigationBarBackground)
```
**注意**
- measure次数为0的节点将不会被打印
- 日志过长会被截断，使用 -continue- 标识
- 节点的measure耗时是总耗时，即measure多次的耗时会被累加
- 由于部分View的特性，比如RecyclerView会在layout过程中会measure子View，或measure过程layout子View，会出现子View的某一阶段耗时会大于父View的同一阶段耗时
- 这里不只是统计onMeasure、onLayout、onDraw，而是完整的measure、layout、draw，因此比如drawBackground、dispatchDraw等也会计入draw的耗时
- 因为没有触发手动traversal，所以有时候会有缓存导致measure次数和耗时不一样，如果需要去掉缓存可以手动调用一次ViewPerfMonitor#performTraversal

## 下载
可以在LSPosed模块仓库搜索ViewPerf

也可直接下载安装
https://github.com/Xposed-Modules-Repo/com.janking.viewperf/releases
