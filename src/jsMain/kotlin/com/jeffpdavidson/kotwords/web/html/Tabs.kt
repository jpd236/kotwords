package com.jeffpdavidson.kotwords.web.html

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.role
import kotlinx.html.ul

/** Templates for creating tabbed layouts. */
internal object Tabs {

    /**
     * A tab to render in a tab layout.
     *
     * @param id unique identifier for the tab
     * @param label the label for the tab
     * @param tabContentBlock block run to render the tab contents
     */
    data class Tab(val id: String, val label: String, val tabContentBlock: FlowContent.() -> Unit)

    /** Render a tab layout containing the given tabs. */
    fun <T> TagConsumer<T>.tabs(vararg tabs: Tab) {
        ul(classes = "nav nav-tabs mb-3") {
            id = "tab"
            role = "tablist"
            tabs.forEachIndexed { i, tab ->
                li(classes = "nav-item") {
                    a(classes = "nav-link") {
                        if (i == 0) {
                            classes = classes + "active"
                            attributes["aria-selected"] = "true"
                        }
                        attributes["aria-controls"] = "${tab.id}-tab"
                        attributes["data-toggle"] = "tab"
                        id = "${tab.id}-tab"
                        role = "tab"
                        href = "#${tab.id}-body"
                        +tab.label
                    }
                }
            }
        }

        div(classes = "tab-content") {
            tabs.forEachIndexed { i, tab ->
                div(classes = "tab-pane fade") {
                    if (i == 0) {
                        classes = classes + setOf("show", "active")
                    }
                    id = "${tab.id}-body"
                    role = "tabpanel"
                    attributes["aria-labelledby"] = "${tab.id}-tab"

                    with(tab) {
                        tabContentBlock()
                    }
                }
            }
        }
    }
}