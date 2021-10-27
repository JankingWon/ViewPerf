# ViewPerf
[Xposed模块]检测View测量、布局和绘制性能

- 可以显示View树
- 显示每次performTraversal耗时，以及performMeasure、performLayout、performDraw耗时
- 显示每个节点的measure、layout、draw耗时
- 显示每个节点的measure次数

## Example

```
2021-10-26 14:56:47.389 25460-25460/? E/ViewPerfMonitor: TraversalResult->[109ms](m:69ms,l:2ms,d:29ms)DecorView@aa324a[RootExplorer]
    * [2](69ms,2ms,2ms)DecorView()
      * [2](69ms,2ms,0ms)LinearLayout()
        * [2](69ms,2ms,0ms)FrameLayout()
          * [2](69ms,2ms,0ms)FitWindowsFrameLayout(@id/action_bar_root)
            * [2](69ms,2ms,0ms)ContentFrameLayout(@id/content)
              * [2](69ms,2ms,2ms)DrawerLayout(@id/drawer_layout)
                * [2](65ms,1ms,0ms)LinearLayout(@id/main_layout)
                  * [2](3ms,0ms,1ms)LinearLayout(@id/headerbar)
                    * [2](0ms,0ms,0ms)Toolbar(@id/toolbar_actionbar)
                      * [2](0ms,0ms,0ms)FrameLayout()
                        * [2](0ms,0ms,0ms)HorizontalScrollView(@id/breadcrumb_scroller)
                          * [2](0ms,0ms,0ms)LinearLayout(@id/breadcrumb_layout)
                      * [2](0ms,0ms,0ms)AppCompatImageButton()
                    * [2](2ms,0ms,0ms)SlidingTabLayout(@id/sliding_tabs)
                      * [4](1ms,0ms,0ms)pr()
                        * [6](0ms,0ms,0ms)AppCompatTextView(@id/text1)
                        * [6](0ms,0ms,0ms)AppCompatTextView(@id/text1)
                  * [2](62ms,1ms,0ms)FrameLayout()
                    * [2](62ms,1ms,1ms)ViewPager(@id/view_pager)
                    * [2](0ms,0ms,0ms)RelativeLayout()
                      * [4](0ms,0ms,0ms)FloatingActionButton(@id/fab_add)
                    * [2](0ms,0ms,0ms)RelativeLayout()
                * [2](4ms,1ms,0ms)ScrimInsetsScrollView(@id/navdrawer)
                  * [2](4ms,1ms,0ms)LinearLayout()
                    * [2](1ms,0ms,0ms)FrameLayout(@id/chosen_account_view)
                      * [2](0ms,0ms,0ms)AppCompatImageView(@id/profile_cover_image)
                      * [2](0ms,0ms,0ms)RelativeLayout(@id/chosen_account_content_view)
                        * [4](0ms,0ms,0ms)AppCompatTextView(@id/profile_email_text)
                        * [4](0ms,0ms,0ms)AppCompatTextView(@id/profile_name_text)
                    * [2](3ms,1ms,0ms)FrameLayout()
                      * [2](3ms,1ms,0ms)LinearLayout(@id/navdrawer_items_list)
                        * [2](0ms,1ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](0ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](0ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](0ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](1ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](1ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](0ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](0ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](0ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](0ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](0ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](0ms,0ms,0ms)AppCompatTextView(@id/title)
                        * [2](1ms,0ms,0ms)LinearLayout()
                          * [2](0ms,0ms,0ms)AppCompatImageView(@id/icon)
                          * [2](1ms,0ms,0ms)AppCompatTextView(@id/title)
```
