scout.CellEditorPopupKeyStrokeAdapter = function(popup) {
  scout.CellEditorPopupKeyStrokeAdapter.parent.call(this, popup);

  this.keyStrokes.push(new scout.CellEditorCompleteEditKeyStroke(popup));
  this.keyStrokes.push(new scout.CellEditorCancelEditKeyStroke(popup));
  this.keyStrokes.push(new scout.CellEditorTabKeyStroke(popup));
};
scout.inherits(scout.CellEditorPopupKeyStrokeAdapter, scout.AbstractKeyStrokeAdapter);

/* ---- KeyStrokes ---------------------------------------------------------- */

scout.CellEditorCompleteEditKeyStroke = function(popup) {
  scout.CellEditorCompleteEditKeyStroke.parent.call(this);
  this._popup = popup;
};
scout.inherits(scout.CellEditorCompleteEditKeyStroke, scout.KeyStroke);

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorCompleteEditKeyStroke.prototype.handle = function(event) {
  this._popup.completeEdit();
};

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorCompleteEditKeyStroke.prototype.accept = function(event) {
  return event && event.which === scout.keys.ENTER;
};

scout.CellEditorCancelEditKeyStroke = function(popup) {
  scout.CellEditorCancelEditKeyStroke.parent.call(this);
  this._popup = popup;
};
scout.inherits(scout.CellEditorCancelEditKeyStroke, scout.KeyStroke);

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorCancelEditKeyStroke.prototype.handle = function(event) {
  this._popup.cancelEdit();
};

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorCancelEditKeyStroke.prototype.accept = function(event) {
  return event && event.which === scout.keys.ESC;
};

scout.CellEditorTabKeyStroke = function(popup) {
  scout.CellEditorCompleteEditKeyStroke.parent.call(this);
  this._popup = popup;
};
scout.inherits(scout.CellEditorTabKeyStroke, scout.KeyStroke);

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorTabKeyStroke.prototype.handle = function(event) {
  var pos,
    backwards = event.shiftKey,
    table = this._popup.table,
    column = this._popup.column,
    row = this._popup.row;

  this._popup.completeEdit();

  // Await server events before opening the next popup, otherwise the cell may be removed in the meantime -> popup loses its anchor
  this._popup.session.listen().done(function() {
    pos = table.nextEditableCellPos(column, row, backwards);
    if (pos) {
      table.sendPrepareCellEdit(pos.row.id, pos.column.id);
    }
  }.bind(this));

  // Prevent default tabbing
  event.preventDefault();
};

/**
 * @Override scout.KeyStroke
 */
scout.CellEditorTabKeyStroke.prototype.accept = function(event) {
  return event.which === scout.keys.TAB;
};
