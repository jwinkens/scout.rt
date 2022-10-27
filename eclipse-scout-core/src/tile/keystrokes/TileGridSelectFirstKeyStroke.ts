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
import {keys, ScoutKeyboardEvent, TileGrid, TileGridSelectKeyStroke} from '../../index';
import {TileGridSelectionInstruction} from '../TileGridSelectionHandler';

export default class TileGridSelectFirstKeyStroke extends TileGridSelectKeyStroke {

  constructor(tileGrid: TileGrid) {
    super(tileGrid);
    this.stopPropagation = true;
    this.which = [keys.HOME];
  }

  protected override _accept(event: ScoutKeyboardEvent): boolean {
    let accepted = super._accept(event);
    if (!accepted) {
      return false;
    }
    if (!(this.getSelectionHandler().isHorizontalGridActive())) {
      return false;
    }
    return true;
  }

  protected override _computeNewSelection(extend: boolean): TileGridSelectionInstruction {
    return this.getSelectionHandler().computeSelectionToFirst(extend);
  }
}