/*******************************************************************************
 * Copyright (c) 2014-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.ProposalTreeNode = function() {
  scout.ProposalTreeNode.parent.call(this);
};
scout.inherits(scout.ProposalTreeNode, scout.TreeNode);

scout.ProposalTreeNode.prototype._init = function(model) {
  scout.ProposalTreeNode.parent.prototype._init.call(this, model);
};

scout.ProposalTreeNode.prototype._renderText = function() {
  var text = this.text;
  if (this.lookupRow.active === false) {
    text += ' (' + this.session.text('InactiveState') + ')';
  }
  if (this.htmlEnabled) {
    this.$text.html(text);
  } else {
    this.$text.textOrNbsp(text);
  }
};

scout.ProposalTreeNode.prototype._getStyles = function() {
  return this.lookupRow;
};

scout.ProposalTreeNode.prototype._decorate = function() {
  // This node is not yet rendered, nothing to do
  if (!this.$node) {
    return;
  }

  scout.ProposalTreeNode.parent.prototype._decorate.call(this);
  this.$node.toggleClass('inactive', !this.lookupRow.active);
};

scout.ProposalTreeNode.prototype.isBrowseLoadIncremental = function() {
  return this.proposalChooser.isBrowseLoadIncremental();
};

scout.ProposalTreeNode.prototype.loadChildren = function() {
  if (this.isBrowseLoadIncremental()) {
    var parentKey = this.lookupRow.key;
    return this.proposalChooser.smartField.lookupByRec(parentKey);
  }
  // child nodes are already loaded -> same as parent.loadChildren
  return $.resolvedDeferred();
};

scout.ProposalTreeNode.prototype.hasChildNodes = function() {
  if (this.isBrowseLoadIncremental() && !this.childrenLoaded) {
    return true; // because we don't now yet
  }
  return scout.ProposalTreeNode.parent.prototype.hasChildNodes.call(this);
};