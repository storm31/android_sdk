/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.adt.gscripts;

/**
 * An {@link IViewRule} for android.widget.AbsoluteLayout and all its derived classes.
 */
public class AndroidWidgetAbsoluteLayoutRule extends BaseLayout {

    // ==== Drag'n'drop support ====
    // The AbsoluteLayout accepts any drag'n'drop anywhere on its surface.

    DropFeedback onDropEnter(INode targetNode, IDragElement[] elements) {

        if (elements.length == 0) {
            return null;
        }

        return new DropFeedback(
            [ "p": null ],      // Point: last cursor position
            {
                gc, node, feedback ->
                // Paint closure for the AbsoluteLayout.
                // This is called by the canvas when a draw is needed.

                drawFeedback(gc, node, elements, feedback);
            } as IFeedbackPainter);
    }

    void drawFeedback(IGraphics gc,
                      INode targetNode,
                      IDragElement[] elements,
                      DropFeedback feedback) {
        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        // Highlight the receiver
        gc.useStyle(DrawingStyle.DROP_RECIPIENT);
        gc.drawRect(b);

        // Get the drop point
        Point p = feedback.userData.p;

        if (p == null) {
            return;
        }

        int x = p.x;
        int y = p.y;

        Rect be = elements[0].getBounds();

        if (be.isValid()) {
            // At least the first element has a bound. Draw rectangles
            // for all dropped elements with valid bounds, offset at
            // the drop point.
            int offsetX = x - be.x;
            int offsetY = y - be.y;
            gc.useStyle(DrawingStyle.DROP_PREVIEW);
            elements.each {
                drawElement(gc, it, offsetX, offsetY);
            }
        } else {
            // We don't have bounds for new elements. In this case
            // just draw cross hairs to the drop point.
            gc.useStyle(DrawingStyle.GUIDELINE);
            gc.drawLine(x, b.y, x, b.y + b.h);
            gc.drawLine(b.x, y, b.x + b.w, y);

            // Use preview lines to indicate the bottom quadrant as well (to indicate
            // that you are looking at the top left position of the drop, not the center
            // for example)
            gc.useStyle(DrawingStyle.DROP_PREVIEW);
            gc.drawLine(x, y, b.x + b.w, y);
            gc.drawLine(x, y, x, b.y + b.h);
        }
    }

    DropFeedback onDropMove(INode targetNode,
                            IDragElement[] elements,
                            DropFeedback feedback,
                            Point p) {
        // Update the data used by the DropFeedback.paintClosure above.
        feedback.userData.p = p;
        feedback.requestPaint = true;

        return feedback;
    }

    void onDropLeave(INode targetNode, IDragElement[] elements, DropFeedback feedback) {
        // Nothing to do.
    }

    void onDropped(INode targetNode,
                   IDragElement[] elements,
                   DropFeedback feedback,
                   Point p) {

        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        int x = p.x - b.x;
        int y = p.y - b.y;

        // Collect IDs from dropped elements and remap them to new IDs
        // if this is a copy or from a different canvas.
        def idMap = getDropIdMap(targetNode, elements, feedback.isCopy || !feedback.sameCanvas);

        targetNode.editXml("Add elements to AbsoluteLayout", {

            boolean first = true;
            Point offset = null;

            // Now write the new elements.
            elements.each { element ->
                String fqcn = element.getFqcn();
                Rect be = element.getBounds();

                INode newChild = targetNode.appendChild(fqcn);

                // Copy all the attributes, modifying them as needed.
                def attrFilter = getLayoutAttrFilter();
                addAttributes(newChild, element, idMap) {
                    uri, name, value ->
                    // TODO need a better way to exclude other layout attributes dynamically
                    if (uri == ANDROID_URI && name in attrFilter) {
                        return false; // don't set these attributes
                    } else {
                        return value;
                    }
                };

                if (first) {
                    first = false;
                    if (be.isValid()) {
                        offset = new Point(x - be.x, y - be.y);
                    }
                } else if (offset != null && be.isValid()) {
                    x = offset.x + be.x;
                    y = offset.y + be.y;
                } else {
                    x += 10;
                    y += be.isValid() ? be.h : 10;
                }

                newChild.setAttribute(ANDROID_URI, "layout_x", "${x}dip");
                newChild.setAttribute(ANDROID_URI, "layout_y", "${y}dip");

                addInnerElements(newChild, element, idMap);
            }
        } as INodeHandler)
    }

}
