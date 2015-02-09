scout.MenuBar = function($parent, position, orderFunc) {
  this.position = position;
  this.orderFunc = orderFunc;
  this.menuItems = [];
  this.$parent = $parent;

  // Create a menubar container and add it to the parent, but don't show it yet. It will
  // be shown automatically when items are added to the menubar, see _updateVisibility().
  this.$container = $.makeDiv('menubar').hide();
  if (this.position === 'top') {
    this.$parent.prepend(this.$container);
  } else {
    this.$container.addClass('bottom');
    this.$parent.append(this.$container);
  }
};

scout.MenuBar.prototype.remove = function() {
  this.menuItems.forEach(function(item) {
    item.remove();
  });
  if (this.$container) {
    this.$container.remove();
  }
};

scout.MenuBar.prototype.updateItems = function(menuItems) {
  // stop if menus are the same as before
  if (scout.arrays.equals(this.menuItems, menuItems)) {
    return;
  }

  // remove existing menu items
  this.menuItems.forEach(function(item) {
    item.remove();
  });

  // The orderFunc may add separators to the list of items, that's why we
  // store the return value of orderFunc in this.menuItems and not the
  // menuItems passed to the updateItems method. We must do this because
  // otherwise we could not remove the added separator later.
  var orderedMenuItems = this.orderFunc(menuItems);
  this.menuItems = orderedMenuItems.left.concat(orderedMenuItems.right);
  this._renderMenuItems(orderedMenuItems.left, false);
  this._renderMenuItems(orderedMenuItems.right, true);

  // Fix for Firefox issue with float:right. In Firefox elements with float:right must
  // come first in the HTML order of elements. Otherwise a strange layout bug occurs.
  this.$container.children('.menu-right').detach().prependTo(this.$container);

  // The _first_ menu-right must have the 'last' class (reverse order because of float:right)
  this.$container.children('.menu-right').first().addClass('last');

  // FIXME AWE: (menu) check if this code is still needed
  // if (this.lastItem && !this.lastItem.$container.hasClass('menu-right')) {
  //    this.lastItem.$container.addClass('last');
  //  }

  this._updateVisibility();
};

scout.MenuBar.prototype._updateVisibility = function() {
  if (this.menuItems.length === 0) {
    this.$container.hide();
  } else {
    this.$container.show();
  }
  var htmlComp = scout.HtmlComponent.optGet(this.$parent);
  if (htmlComp) {
    htmlComp.revalidate();
  }
};

scout.MenuBar.prototype._renderMenuItems = function(menuItems, right) {
  var tooltipPosition = (this.position === 'top' ? 'bottom' : 'top');

  menuItems.forEach(function(item) {
    item.tooltipPosition = tooltipPosition;
    item.render(this.$container);
    // TODO BSH Menu | Ask AW.E why this is needed:
    item.$container.removeClass('form-field');
    if (right) {
      item.$container.addClass('menu-right');
    }
  }.bind(this));
};
