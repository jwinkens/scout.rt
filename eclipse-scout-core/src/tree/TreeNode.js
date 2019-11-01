/*
 * Copyright (c) 2014-2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {defaultValues} from '../index';
import {icons} from '../index';
import {Tree} from '../index';
import {texts} from '../index';
import {objects} from '../index';
import {styles} from '../index';
import * as $ from 'jquery';
import {scout} from '../index';

/**
 * @class
 */
export default class TreeNode {

  constructor(tree) {
    this.$node = null;
    this.$text = null;
    this.attached = false;
    this.checked = false;
    this.childNodes = [];
    this.childrenLoaded = false;
    this.destroyed = false;
    this.enabled = true;
    this.expanded = false;
    this.expandedLazy = false;
    this.filterAccepted = true;
    this.filterDirty = false;
    this.id = null;
    this.initialized = false;
    this.initialExpanded = false;
    this.lazyExpandingEnabled = false;
    this.leaf = false;
    this.level = 0;
    this.parent = null;
    this.parentNode = undefined;
    this.prevSelectionAnimationDone = false;
    this.rendered = false;
    this.session = null;
    this.text = null;

    /**
     * This internal variable stores the promise which is used when a loadChildren() operation is in progress.
     */
    this._loadChildrenPromise = false;
  }

  init(model) {
    var staticModel = this._jsonModel();
    if (staticModel) {
      model = $.extend({}, staticModel, model);
    }
    this._init(model);
    if (model.initialExpanded === undefined) {
      this.initialExpanded = this.expanded;
    }
  }

  destroy() {
    if (this.destroyed) {
      // Already destroyed, do nothing
      return;
    }
    this._destroy();
    this.destroyed = true;
  }

  /**
   * Override this method to do something when TreeNode gets destroyed. The default impl. does nothing.
   */
  _destroy() {
    // NOP
  }

  getTree() {
    return this.parent;
  }

  _init(model) {
    scout.assertParameter('parent', model.parent, Tree);
    this.session = model.session || model.parent.session;

    $.extend(this, model);
    defaultValues.applyTo(this);

    texts.resolveTextProperty(this, 'text');
    icons.resolveIconProperty(this, 'iconId');

    // make sure all child nodes are TreeNodes too
    if (this.hasChildNodes()) {
      this.getTree()._ensureTreeNodes(this.childNodes);
    }
  }

  _jsonModel() {
  };

  hasChildNodes() {
    return this.childNodes.length > 0;
  }

  reset() {
    if (this.$node) {
      this.$node.remove();
      this.$node = null;
    }
    this.rendered = false;
    this.attached = false;
  }

  /**
   * Check if node is in hierarchy of a parent. is used on removal from flat list.
   */
  isChildOf(parentNode) {
    if (parentNode === this.parentNode) {
      return true;
    } else if (!this.parentNode) {
      return false;
    }
    return this.parentNode.isChildOf(parentNode);
  }

  isFilterAccepted(forceFilter) {
    if (this.filterDirty || forceFilter) {
      this.getTree()._applyFiltersForNode(this);
    }
    return this.filterAccepted;
  }

  /**
   * This method loads the child nodes of this node and returns a jQuery.Deferred to register callbacks
   * when loading is done or has failed. This method should only be called when childrenLoaded is false.
   *
   * @return {$.Deferred} or null when TreeNode cannot load children (which is the case for all
   *     TreeNodes in the remote case). The default impl. return null.
   */
  loadChildren() {
    return $.resolvedDeferred();
  }

  /**
   * This method calls loadChildren() but does nothing when children are already loaded or when loadChildren()
   * is already in progress.
   * @returns {Promise}
   */
  ensureLoadChildren() {
    // when children are already loaded we return an already resolved promise so the caller can continue immediately
    if (this.childrenLoaded) {
      return $.resolvedPromise();
    }
    // when load children is already in progress, we return the same promise
    if (this._loadChildrenPromise) {
      return this._loadChildrenPromise;
    }
    var deferred = this.loadChildren();
    var promise = deferred.promise();
    // check if we can get rid of this state-check in a future release
    if (deferred.state() === 'resolved') {
      this._loadChildrenPromise = null;
      return promise;
    }

    this._loadChildrenPromise = promise;
    promise.done(this._onLoadChildrenDone.bind(this));
    return promise; // we must always return a promise, never null - otherwise caller would throw an error
  }

  _onLoadChildrenDone() {
    this._loadChildrenPromise = null;
  }

  setText(text) {
    this.text = text;
  }

  /**
   * This functions renders sets the $node and $text properties.
   *
   * @param {jQuery} $parent the tree DOM
   * @param {number} paddingLeft calculated by tree
   */
  render($parent, paddingLeft) {
    this.$node = $parent.makeDiv('tree-node')
      .data('node', this)
      .attr('data-nodeid', this.id)
      .attr('data-level', this.level);
    if (!objects.isNullOrUndefined(paddingLeft)) {
      this.$node.css('padding-left', paddingLeft);
    }
    this.$text = this.$node.appendSpan('text');

    this._renderControl();
    if (this.getTree().checkable) {
      this._renderCheckbox();
    }
    this._renderText();
    this._renderIcon();
  }

  _renderText() {
    if (this.htmlEnabled) {
      this.$text.html(this.text);
    } else {
      this.$text.textOrNbsp(this.text);
    }
  }

  _renderChecked() {
    // if node is not rendered, do nothing
    if (!this.rendered) {
      return;
    }

    this.$node
      .children('.tree-node-checkbox')
      .children('.check-box')
      .toggleClass('checked', this.checked);
  }

  _renderIcon() {
    this.$node.icon(this.iconId, function($icon) {
      $icon.insertBefore(this.$text);
    }.bind(this));
  }

  $icon() {
    return this.$node.children('.icon');
  }

  _renderControl() {
    var $control = this.$node.prependDiv('tree-node-control');
    this._updateControl($control, this.getTree());
  }

  _updateControl($control, tree) {
    $control.toggleClass('checkable', tree.checkable);
    $control.cssPaddingLeft(tree.nodeControlPaddingLeft + this.level * tree.nodePaddingLevel);
    $control.setVisible(!this.leaf);
  }

  _renderCheckbox() {
    var $checkboxContainer = this.$node.prependDiv('tree-node-checkbox');
    var $checkbox = $checkboxContainer
      .appendDiv('check-box')
      .toggleClass('checked', this.checked)
      .toggleClass('disabled', !(this.getTree().enabled && this.enabled));
    $checkbox.toggleClass('children-checked', !!this.childrenChecked);
  }

  _decorate() {
    // This node is not yet rendered, nothing to do
    if (!this.$node) {
      return;
    }

    var $node = this.$node,
      tree = this.getTree();

    $node.attr('class', this._preserveCssClasses($node));
    $node.addClass(this.cssClass);
    $node.toggleClass('leaf', !!this.leaf);
    $node.toggleClass('expanded', (!!this.expanded && this.childNodes.length > 0));
    $node.toggleClass('lazy', $node.hasClass('expanded') && this.expandedLazy);
    $node.toggleClass('group', !!tree.groupedNodes[this.id]);
    $node.setEnabled(!!this.enabled);
    $node.children('.tree-node-control').setVisible(!this.leaf);
    $node
      .children('.tree-node-checkbox')
      .children('.check-box')
      .toggleClass('disabled', !(tree.enabled && this.enabled));

    if (!this.parentNode && tree.selectedNodes.length === 0 || // root nodes have class child-of-selected if no node is selected
      tree._isChildOfSelectedNodes(this)) {
      $node.addClass('child-of-selected');
    }

    this._renderText();
    this._renderIcon();
    styles.legacyStyle(this._getStyles(), $node);

    // If parent node is marked as 'lazy', check if any visible child nodes remain.
    if (this.parentNode && this.parentNode.expandedLazy) {
      var hasVisibleNodes = this.parentNode.childNodes.some(function(childNode) {
        if (tree.visibleNodesMap[childNode.id]) {
          return true;
        }
      }.bind(this));
      if (!hasVisibleNodes && this.parentNode.$node) {
        // Remove 'lazy' from parent
        this.parentNode.$node.removeClass('lazy');
      }
    }
  }

  /**
   * @return The object that has the properties used for styles (colors, fonts, etc.)
   *     The default impl. returns "this". Override this function to return another object.
   */
  _getStyles() {
    return this;
  }

  /**
   * This function extracts all CSS classes that are set externally by the tree.
   * The classes depend on the tree hierarchy or the selection and thus cannot determined
   * by the node itself.
   */
  _preserveCssClasses($node) {
    var cssClass = 'tree-node';
    if ($node.isSelected()) {
      cssClass += ' selected';
    }
    if ($node.hasClass('ancestor-of-selected')) {
      cssClass += ' ancestor-of-selected';
    }
    if ($node.hasClass('parent-of-selected')) {
      cssClass += ' parent-of-selected';
    }
    return cssClass;
  }

  _updateIconWidth() {
    var cssWidth = '';
    if (this.iconId) {
      // always add 1 pixel to the result of outer-width to prevent rendering errors in IE, where
      // the complete text is replaced by an ellipsis, when the .text element is a bit too large
      cssWidth = 'calc(100% - ' + (this.$icon().outerWidth() + 1) + 'px)';
    }
    this.$text.css('max-width', cssWidth);
  }
}