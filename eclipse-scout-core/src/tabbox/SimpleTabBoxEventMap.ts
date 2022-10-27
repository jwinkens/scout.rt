/*
 * Copyright (c) 2010-2022 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {Event, SimpleTabBox, WidgetEventMap} from '../index';
import {OutlineContent} from '../desktop/bench/DesktopBench';

export interface SimpleTabBoxViewActivateEvent<S extends SimpleTabBox = SimpleTabBox> extends Event<S> {
  view: OutlineContent;
}

export interface SimpleTabBoxViewAddEvent<S extends SimpleTabBox = SimpleTabBox> extends Event<S> {
  view: OutlineContent;
  siblingView: OutlineContent;
}

export interface SimpleTabBoxViewDeactivateEvent<S extends SimpleTabBox = SimpleTabBox> extends Event<S> {
  view: OutlineContent;
}

export interface SimpleTabBoxViewRemoveEvent<S extends SimpleTabBox = SimpleTabBox> extends Event<S> {
  view: OutlineContent;
}

export default interface SimpleTabBoxEventMap extends WidgetEventMap {
  'viewActivate': SimpleTabBoxViewActivateEvent;
  'viewAdd': SimpleTabBoxViewAddEvent;
  'viewDeactivate': SimpleTabBoxViewDeactivateEvent;
  'viewRemove': SimpleTabBoxViewRemoveEvent;
}