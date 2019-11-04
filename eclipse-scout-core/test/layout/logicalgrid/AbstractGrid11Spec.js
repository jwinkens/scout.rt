/*
 * Copyright (c) 2010-2019 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {Button, FormField, GridData, GroupBoxGridConfig, HorizontalGrid, scout, VerticalSmartGrid} from '../../../src/index';
import {GroupBoxSpecHelper} from '@eclipse-scout/testing';

/**
 * Reference implementation javadoc:
 *
 * <h4>Vertical</h4>
 *
 * <pre>
 * ---------------------------
 *    Group01   |   Group01
 * ---------------------------
 *    Group02   |   Group03
 * ---------------------------
 *    Group04   |   Group04
 * ---------------------------
 * </pre>
 *
 * <h4>Horizontal</h4>
 *
 * <pre>
 * ---------------------------
 *    Group01   |   Group01
 * ---------------------------
 *    Group02   |   Group03
 * ---------------------------
 *    Group04   |   Group04
 * ---------------------------
 * </pre>
 *
 * @author Andreas Hoegger
 * @since 4.0.0 M6 13.03.2014
 */
// see reference implementation org.eclipse.scout.rt.client.ui.form.fields.groupbox.internal.GroupBoxLayout11Test
describe('AbstractGrid11', function() {
  var session;

  beforeEach(function() {
    setFixtures(sandbox());
    session = sandboxSession();

    this.fields = [];
    this.groupBox = scout.create('GroupBox', {
      parent: session.desktop,
      gridColumnCount: 2
    });
    this.fields.push(scout.create('GroupBox', {
      parent: this.groupBox,
      label: 'Field 01',
      gridDataHints: new GridData({
        w: FormField.FULL_WIDTH
      })
    }));
    this.fields.push(scout.create('GroupBox', {
      parent: this.groupBox,
      label: 'Field 02',
      gridColumnCount: 1,
      gridDataHints: new GridData({
        w: 1
      })
    }));
    this.fields.push(scout.create('GroupBox', {
      parent: this.groupBox,
      label: 'Field 03',
      gridColumnCount: 1,
      gridDataHints: new GridData({
        w: 1
      })
    }));
    this.fields.push(scout.create('GroupBox', {
      parent: this.groupBox,
      label: 'Field 04',
      gridDataHints: new GridData({
        w: 2
      })
    }));
    this.fields.push(scout.create('Button', {
      parent: this.groupBox,
      label: 'Ok',
      systemType: Button.SystemType.OK
    }));
    this.fields.push(scout.create('Button', {
      parent: this.groupBox,
      label: 'Cancel',
      systemType: Button.SystemType.CANCEL
    }));
    this.groupBox.setProperty('fields', this.fields);
    this.groupBox.render();
  });

  describe('group box layout 11', function() {
    it('test horizontal layout', function() {
      var grid = new HorizontalGrid();
      grid.setGridConfig(new GroupBoxGridConfig());
      grid.validate(this.groupBox);

      // group box
      expect(grid.getGridRowCount()).toEqual(3);
      expect(grid.getGridColumnCount()).toEqual(2);

      // field01
      GroupBoxSpecHelper.assertGridData(0, 0, 2, 1, this.fields[0].gridData);

      // field02
      GroupBoxSpecHelper.assertGridData(0, 1, 1, 1, this.fields[1].gridData);

      // field03
      GroupBoxSpecHelper.assertGridData(1, 1, 1, 1, this.fields[2].gridData);

      // field04
      GroupBoxSpecHelper.assertGridData(0, 2, 2, 1, this.fields[3].gridData);
    });

    it('test vertical smart layout', function() {
      var grid = new VerticalSmartGrid();
      grid.setGridConfig(new GroupBoxGridConfig());
      grid.validate(this.groupBox);

      // group box
      expect(grid.getGridRowCount()).toEqual(3);
      expect(grid.getGridColumnCount()).toEqual(2);

      // field01
      GroupBoxSpecHelper.assertGridData(0, 0, 2, 1, this.fields[0].gridData);

      // field02
      GroupBoxSpecHelper.assertGridData(0, 1, 1, 1, this.fields[1].gridData);

      // field03
      GroupBoxSpecHelper.assertGridData(1, 1, 1, 1, this.fields[2].gridData);

      // field04
      GroupBoxSpecHelper.assertGridData(0, 2, 2, 1, this.fields[3].gridData);
    });
  });

});
