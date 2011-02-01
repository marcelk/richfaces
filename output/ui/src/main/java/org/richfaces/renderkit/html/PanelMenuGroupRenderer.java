/*
 * JBoss, Home of Professional Open Source
 * Copyright ${year}, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.richfaces.renderkit.html;

import static org.richfaces.renderkit.HtmlConstants.CLASS_ATTRIBUTE;
import static org.richfaces.renderkit.HtmlConstants.DIV_ELEM;
import static org.richfaces.renderkit.HtmlConstants.ID_ATTRIBUTE;
import static org.richfaces.renderkit.HtmlConstants.INPUT_ELEM;
import static org.richfaces.renderkit.HtmlConstants.INPUT_TYPE_HIDDEN;
import static org.richfaces.renderkit.HtmlConstants.NAME_ATTRIBUTE;
import static org.richfaces.renderkit.HtmlConstants.TYPE_ATTR;
import static org.richfaces.renderkit.HtmlConstants.VALUE_ATTRIBUTE;
import static org.richfaces.renderkit.html.TogglePanelRenderer.addEventOption;
import static org.richfaces.renderkit.html.TogglePanelRenderer.getAjaxOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.ajax4jsf.javascript.JSFunction;
import org.ajax4jsf.javascript.JSObject;
import org.ajax4jsf.javascript.ScriptUtils;
import org.richfaces.PanelMenuMode;
import org.richfaces.cdk.annotations.JsfRenderer;
import org.richfaces.component.AbstractPanelMenuGroup;
import org.richfaces.component.AbstractPanelMenuItem;
import org.richfaces.renderkit.HtmlConstants;

import com.google.common.base.Strings;

/**
 * @author akolonitsky
 * @since 2010-10-25
 */
@JsfRenderer(type = "org.richfaces.PanelMenuGroupRenderer", family = AbstractPanelMenuGroup.COMPONENT_FAMILY)
public class PanelMenuGroupRenderer extends DivPanelRenderer {

    public static final String COLLAPSE = "collapse";
    public static final String EXPAND = "expand";
    public static final String SWITCH = "switch";
    public static final String BEFORE_COLLAPSE = "beforecollapse";
    public static final String BEFORE_EXPAND = "beforeexpand";
    public static final String BEFORE_SWITCH = "beforeswitch";
    private static final String CSS_CLASS_PREFIX = "rf-pm-gr";

    //TODO nick - shouldn't this be rf-pm-gr-top?
    private static final String TOP_CSS_CLASS_PREFIX = "rf-pm-top-gr";
    private static final String DEFAULT_EXPAND_EVENT = "click";
    private static final String DEFAULT_COLLAPSE_EVENT = "click";

    private final TableIconsRendererHelper<AbstractPanelMenuGroup> headerRenderer = new PanelMenuGroupHeaderRenderer(CSS_CLASS_PREFIX);
    private final TableIconsRendererHelper<AbstractPanelMenuGroup> topHeaderRenderer = new PanelMenuGroupHeaderRenderer(TOP_CSS_CLASS_PREFIX);

    @Override
    protected void doDecode(FacesContext context, UIComponent component) {
        AbstractPanelMenuGroup menuGroup = (AbstractPanelMenuGroup) component;

        Map<String, String> requestMap =
              context.getExternalContext().getRequestParameterMap();

        // Don't overwrite the value unless you have to!
        //TODO! nick - ":expanded" suffix is not necessary
        String newValue = requestMap.get(component.getClientId(context) + ":expanded");
        if (newValue != null) {
            menuGroup.setSubmittedExpanded(newValue);
        }

        String clientId = component.getClientId(context);
        if (requestMap.get(clientId) != null) {
            context.getPartialViewContext().getRenderIds().add(clientId);

            //TODO nick - this should be done on encode, not on decode
            PanelMenuRenderer.addOnCompleteParam(context, clientId);
        }
    }

    @Override
    protected void doEncodeBegin(ResponseWriter writer, FacesContext context, UIComponent component) throws IOException {
        super.doEncodeBegin(writer, context, component);

        AbstractPanelMenuGroup menuGroup = (AbstractPanelMenuGroup) component;

        writer.startElement(INPUT_ELEM, component);

        //TODO nick - there is no need to encode this input - group state can be extracted from class
        final String expanded = component.getClientId(context) + ":expanded";
        writer.writeAttribute(ID_ATTRIBUTE, expanded, null);
        writer.writeAttribute(NAME_ATTRIBUTE, expanded, null);
        writer.writeAttribute(TYPE_ATTR, INPUT_TYPE_HIDDEN, null);
        writer.writeAttribute(VALUE_ATTRIBUTE, String.valueOf(menuGroup.isExpanded()), null);
        writer.endElement(INPUT_ELEM);

        encodeHeader(writer, context, menuGroup);
        encodeContentBegin(writer, context, menuGroup);
    }

    private void encodeHeader(ResponseWriter writer, FacesContext context, AbstractPanelMenuGroup menuGroup) throws IOException {
        writer.startElement(DIV_ELEM, null);
        writer.writeAttribute(ID_ATTRIBUTE, menuGroup.getClientId(context) + ":hdr", null);
        writer.writeAttribute(CLASS_ATTRIBUTE, concatClasses(getCssClass(menuGroup, "-hdr"),
                "rf-pm-hdr-" + (menuGroup.isExpanded() ? "exp" : "colps"), 
                menuGroup.isDisabled() ? getCssClass(menuGroup, "-hdr-dis") : null), null);

        (menuGroup.isTopItem() ? topHeaderRenderer : headerRenderer).encodeHeader(writer, context, menuGroup);

        writer.endElement(DIV_ELEM);
    }

    public String getCssClass(AbstractPanelMenuItem item, String postfix) {
        return (item.isTopItem() ? TOP_CSS_CLASS_PREFIX : CSS_CLASS_PREFIX) + postfix;
    }

    private void encodeContentBegin(ResponseWriter writer, FacesContext context, AbstractPanelMenuGroup menuGroup) throws IOException {
        writer.startElement(DIV_ELEM, null);
        writer.writeAttribute(ID_ATTRIBUTE, menuGroup.getClientId(context) + ":cnt", null);
        writer.writeAttribute(CLASS_ATTRIBUTE, concatClasses(getCssClass(menuGroup, "-cnt"), menuGroup.isExpanded() ? "rf-pm-exp" : "rf-pm-colps"), null);

        writeJavaScript(writer, context, menuGroup);
    }

    private void encodeContentEnd(ResponseWriter writer, FacesContext context, UIComponent component) throws IOException {
        writer.endElement(DIV_ELEM);
    }

    @Override
    protected String getStyleClass(UIComponent component) {
        AbstractPanelMenuItem menuItem = (AbstractPanelMenuItem) component;

        return concatClasses(getCssClass(menuItem, ""),
            attributeAsString(component, "styleClass"),
            menuItem.isDisabled() ? getCssClass(menuItem, "-dis") : "",
            menuItem.isDisabled() ? attributeAsString(component, "disabledClass") : "");
    }

    @Override
    protected JSObject getScriptObject(FacesContext context, UIComponent component) {
        return new JSObject("RichFaces.ui.PanelMenuGroup",
            component.getClientId(context), getScriptObjectOptions(context, component));
    }

    private String getExpandEvent(AbstractPanelMenuGroup group) {
        String expandEvent = group.getExpandEvent();
        if (Strings.isNullOrEmpty(expandEvent)) {
            expandEvent = group.getPanelMenu().getExpandEvent();
        }
        if (Strings.isNullOrEmpty(expandEvent)) {
            expandEvent = DEFAULT_EXPAND_EVENT;
        }
        return expandEvent;
    }

    private String getCollapseEvent(AbstractPanelMenuGroup group) {
        String collapseEvent = group.getCollapseEvent();
        if (Strings.isNullOrEmpty(collapseEvent)) {
            collapseEvent = group.getPanelMenu().getCollapseEvent();
        }
        if (Strings.isNullOrEmpty(collapseEvent)) {
            collapseEvent = DEFAULT_COLLAPSE_EVENT;
        }
        return collapseEvent;
    }
    
    @Override
    protected Map<String, Object> getScriptObjectOptions(FacesContext context, UIComponent component) {
        AbstractPanelMenuGroup panelMenuGroup = (AbstractPanelMenuGroup) component;

        Map<String, Object> options = new HashMap<String, Object>();
        //TODO nick - non-default values should not be rendered
        options.put("ajax", getAjaxOptions(context, panelMenuGroup));
        options.put("name", panelMenuGroup.getName());
        options.put("mode", panelMenuGroup.getMode());
        options.put("disabled", panelMenuGroup.isDisabled());
        options.put("expandEvent", getExpandEvent(panelMenuGroup));
        options.put("collapseEvent", getCollapseEvent(panelMenuGroup));
        options.put("expandSingle", panelMenuGroup.isExpandSingle());
        options.put("bubbleSelection", panelMenuGroup.isBubbleSelection());
        options.put("expanded", panelMenuGroup.isExpanded());
        options.put("selectable", panelMenuGroup.isSelectable());
        options.put("unselectable", panelMenuGroup.isUnselectable());

        addEventOption(context, panelMenuGroup, options, COLLAPSE);
        addEventOption(context, panelMenuGroup, options, EXPAND);
        addEventOption(context, panelMenuGroup, options, SWITCH);
        addEventOption(context, panelMenuGroup, options, BEFORE_COLLAPSE);
        addEventOption(context, panelMenuGroup, options, BEFORE_EXPAND);
        addEventOption(context, panelMenuGroup, options, BEFORE_SWITCH);

        return options;
    }

    @Override
    protected void doEncodeEnd(ResponseWriter writer, FacesContext context, UIComponent component) throws IOException {
        encodeContentEnd(writer, context, component);

        writer.endElement(DIV_ELEM);
    }

    @Override
    protected Class<? extends UIComponent> getComponentClass() {
        return AbstractPanelMenuGroup.class;
    }
    
    @Override
    public boolean getRendersChildren() {
        return true;
    }
    
    private boolean containsActiveItem(UIComponent component, String activeItem) {
        if (component instanceof AbstractPanelMenuItem) {
            AbstractPanelMenuItem item = (AbstractPanelMenuItem) component;
            if (activeItem.equals(item.getName())) {
                return true;
            }
        }
        
        if (component instanceof AbstractPanelMenuGroup) {
            AbstractPanelMenuGroup group = (AbstractPanelMenuGroup) component;
            if (!group.isBubbleSelection()) {
                return false;
            }
        }
        
        if (component.getChildCount() > 0) {
            for (UIComponent child : component.getChildren()) {
                if (!child.isRendered()) {
                    continue;
                }
                
                if (!(child instanceof AbstractPanelMenuItem)) {
                    continue;
                }
                
                if (containsActiveItem(child, activeItem)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    protected void doEncodeChildren(ResponseWriter writer, FacesContext context, UIComponent component)
        throws IOException {

        AbstractPanelMenuGroup group = (AbstractPanelMenuGroup) component;
        
        boolean isClientMode = group.getMode() == PanelMenuMode.client;
        
        if (isClientMode || group.isExpanded()) {
            renderChildren(context, component);
        } else {
            String activeItem = group.getPanelMenu().getActiveItem();
            if (!Strings.isNullOrEmpty(activeItem) && containsActiveItem(component, activeItem)) {
                writer.startElement(HtmlConstants.SCRIPT_ELEM, component);
                writer.writeAttribute(HtmlConstants.TYPE_ATTR, HtmlConstants.TEXT_JAVASCRIPT_TYPE, null);
                writer.writeText(ScriptUtils.toScript(new JSFunction("RichFaces.$", component.getClientId(context))) + ".__restoreSelection();", null);
                writer.endElement(HtmlConstants.SCRIPT_ELEM);
            }
        }
    }
}
