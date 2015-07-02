scout.ViewMenuPopup = function($tab, viewMenus, naviBounds, session) {
  scout.ViewMenuPopup.parent.call(this, session);
  this.$tab = $tab;
  this.$headBlueprint = this.$tab;
  this.viewMenus = viewMenus;
  this._naviBounds = naviBounds;
};
scout.inherits(scout.ViewMenuPopup, scout.PopupWithHead);

scout.ViewMenuPopup.MAX_MENU_WIDTH = 300;

scout.ViewMenuPopup.prototype._render = function($parent) {
  scout.ViewMenuPopup.parent.prototype._render.call(this, $parent);

  this.viewMenus.forEach(function(viewMenu) {
    viewMenu.render(this.$body);
    viewMenu.afterSendDoAction = this.close.bind(this);
    this.addChild(viewMenu);
  }, this);
  this.alignTo();
};

scout.ViewMenuPopup.prototype._renderHead = function() {
  scout.ViewMenuPopup.parent.prototype._renderHead.call(this);
  this._copyCssClassToHead('navigation-tab-outline-button');
  this.$head.addClass('navigation-header');
};

scout.ViewMenuPopup.prototype.alignTo = function() {
  var pos = this.$tab.offset(),
    headSize = scout.graphics.getSize(this.$head, true);
  // horiz. alignment
  var left = pos.left,
    top = pos.top,
    bodyTop = headSize.height;
  $.log.debug(' pos=[left' + pos.left + ' top=' + pos.top + '] headSize=' + headSize + ' left=' + left + ' top=' + top);
  this.$body.cssTop(bodyTop);
  var offsetBounds;
  if (this.$tab.parents('.navigation-breadcrumb').length) {
    offsetBounds = scout.graphics.offsetBounds(this.$tab.parent());
    this.$head.cssWidth(offsetBounds.width / 2);
    this.$deco.cssWidth(offsetBounds.width / 2 - 2);
  }
  else {
    offsetBounds = scout.graphics.offsetBounds(this.$tab);
    this.$deco.cssWidth(headSize.width - 2);
  }

  this.$body.cssWidth(Math.min(scout.ViewMenuPopup.MAX_MENU_WIDTH, this._naviBounds.width));
  this.$deco.cssTop(bodyTop);
  this.$head.cssLeft(0);
  this.$deco.cssLeft(1);
  this.setLocation(new scout.Point(left, top));
};

scout.ViewMenuPopup.prototype._createKeyStrokeAdapter = function() {
  return new scout.ViewMenuPopupKeyStrokeAdapter(this);
};
