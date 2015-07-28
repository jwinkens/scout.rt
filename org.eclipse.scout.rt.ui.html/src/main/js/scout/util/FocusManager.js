/**
 * The focus manager exists once per session, and ensures proper focus positioning based on so called focus contexts.
 *
 * A focus context is associated with a $container, and validates its child controls focus position and their tab cycle.
 * Once a context is installed, the currently active context is suspended and activated anew once a 'focusIn' event
 * on one of its child controls occurs, or the active focus context is uninstalled.
 */
scout.FocusManager = function() {
  this._sessionFocusContexts = {};
};

/**
 * Rule for initial focus gain once a focus context is installed.
 */
scout.FocusRule = {
  /**
   * Focus the first focusable field.
   */
  AUTO: 1,
  /**
   * Do not gain focus.
   */
  NONE: 2
};

/**
 * Installs focus handling for the given session.
 */
scout.FocusManager.prototype.installManagerForSession = function(session, options) {
  this._sessionFocusContexts[session.uiSessionId] = {
    active:  scout.helpers.nvl(options.focusManagerActive, true),
    session: session,
    focusContexts: [],
    glassPaneTargets: []
  };

  var $entryPoint = session.$entryPoint;
  var portletPartId = $entryPoint.data('partid') || '0';
  // Make container focusable and install focus context
  $entryPoint.attr('tabindex', portletPartId);

  var setInitialFocus = scout.helpers.nvl(options.setInitialFocus, true);
  if (setInitialFocus) {
    // FIXME: AWE/NBU: mit IMO diskutieren, was die anforderungen an fokus für die office integration sind
    // a.) wir brauchen _immer_ einen focus context, weil sonst z.B. message-box handling kaputt geht
    // b.) -vermutlich- darf in office die Html app keinen initialen fokus haben (Ivan fragen)
    // Sobald diese frage geklärt ist, kann im DetachHelper#storeFocus der check für den focusContext entfernt
    // werden, weil wir denken, dass wir immer einen focusContext haben sollten.
    this.installFocusContext(session.uiSessionId, $entryPoint, scout.FocusRule.AUTO);
  }

  // Install global 'mousedown' listener to accept or prevent focus gain.
  session.$entryPoint.on('mousedown', function(event) {
    if (!this._acceptFocusChangeOnMouseDown($(event.target), session.$entryPoint, session.uiSessionId)) {
      event.preventDefault();
    }
    return true;
  }.bind(this));
};

/**
 * Activates focus management for the given session.
 */
scout.FocusManager.prototype.activate = function(uiSessionId) {
  this._sessionFocusContexts[uiSessionId].active = true;
  this.validateFocus(uiSessionId);
};

/**
 * Deactivates focus management for the given session.
 */
scout.FocusManager.prototype.deactivate = function(uiSessionId) {
  this._sessionFocusContexts[uiSessionId].active = false;
};

/**
 * Returns whether focus management is active for the given session.
 */
scout.FocusManager.prototype.active = function(uiSessionId) {
  return this._sessionFocusContexts[uiSessionId].active;
};

/**
 * Installs a new focus context for the given $container, and sets the $container's initial focus, either by
 * the given rule, or tries to gain focus for the given element.
 *
 * @param initialFocusRuleOrElement: rule how to set the initial focus, or the element to gain focus.
 *
 *        rule: scout.FocusRule.AUTO: to focus the first child control (if applicable);
 *              scout.FocusRule.NONE: to not focus any element;
 *        element: tries to focus the given element, but only if being a child control of the $container, and if being accessible,
 *                 e.g. not covert by a glasspane;
 */
scout.FocusManager.prototype.installFocusContext = function(uiSessionId, $container, initialFocusRuleOrElement) {
  var elementToFocus;
  if (!initialFocusRuleOrElement || initialFocusRuleOrElement === scout.FocusRule.AUTO) {
    elementToFocus = this.findFirstFocusableElement(uiSessionId, $container);
  } else if (initialFocusRuleOrElement === scout.FocusRule.NONE) {
    elementToFocus = null;
  } else {
    elementToFocus = initialFocusRuleOrElement;
  }

  // Create and register the focus context.
  var focusContext = new scout.FocusContext($container, uiSessionId);
  this._registerContextIfAbsentElseMoveTop(focusContext);

  // Focuses the given element, but only if accessible (not covert by glasspane), and if being a child control of the $container.
  if (elementToFocus) {
    focusContext._validateAndSetFocus(elementToFocus);
  }
};

/**
 * Uninstalls the focus context for the given $container, and activates the last active context.
 */
scout.FocusManager.prototype.uninstallFocusContext = function(uiSessionId, $container) {
  var focusContext = this._contextByContainer(uiSessionId, $container);
  if (!focusContext) {
    return;
  }

  scout.arrays.remove(this._contextsBySession(uiSessionId), focusContext);
  focusContext._dispose();

  // Activate last active focus context.
  var activeFocusContext = this._activeContext(uiSessionId);
  if (activeFocusContext) {
    activeFocusContext._validateAndSetFocus(activeFocusContext._lastFocusedElement);
  }
};

/**
 * Registers the given glasspane target, so that the focus cannot be gained on the given target or one of its child controls.
 *
 * Typically, this method is invoked by 'GlassPaneRenderer' once a glasspane is rendered over the given target element.
 */
scout.FocusManager.prototype.registerGlassPaneTarget = function(uiSessionId, glassPaneTarget) {
  this._glassPaneTargetsBySession(uiSessionId).push(glassPaneTarget);
  this.validateFocus(uiSessionId);
};

/**
 * Unregisters the given glasspane target, so that the focus can be gained again for the target or one of its child controls.
 */
scout.FocusManager.prototype.unregisterGlassPaneTarget = function(uiSessionId, glassPaneTarget) {
  scout.arrays.remove(this._glassPaneTargetsBySession(uiSessionId), glassPaneTarget);
  this.validateFocus(uiSessionId);
};

/**
 * Finds the first focusable element of the given $container, or null if not found.
 */
scout.FocusManager.prototype.findFirstFocusableElement = function(uiSessionId, $container) {
  var firstElement, firstDefaultButton, firstButton, i, candidate, $menuParents, $tabParents;

  // Find all focusable child elements.
  // Note: If the $container itself is focusable (with a tabindex), also add it to the list of candidates.
  //       E.g. menu-items in a context-menu are not focusable, but the popup container instead.
  //       That allows the popup to gain focus even if having no focusable children.
  var $candidates = $container.find(':focusable').addBack('[tabindex]');

  for (i = 0; i < $candidates.length; i++) {
    candidate = $candidates[i];

    // Check whether the candidate is accessible and not covert by a glass pane.
    if (!this._isElementAccessible(candidate, uiSessionId)) {
      continue;
    }

    if (!firstElement) {
      firstElement = candidate;
    }

    if (!firstDefaultButton && $(candidate).is('.default-menu')) {
      firstDefaultButton = candidate;
    }

    $menuParents = $(candidate).parents('div.menubar');
    $tabParents = $(candidate).parents('div.tab-area');
    if (!firstButton && ($(candidate).hasClass('button') || $(candidate).hasClass('menu-item'))) { // TODO [nbu] menu-items are not focusable, why this check?
      firstButton = candidate;
    } else if (!$menuParents.length && !$tabParents.length && typeof candidate.focus === 'function') {
      return candidate; // TODO [nbu] why is this necessary? is immediate return correct?
    }
  }

  return firstDefaultButton || firstButton || firstElement;
};

/**
 * Ensures proper focus on the currently active focus context.
 */
scout.FocusManager.prototype.validateFocus = function(uiSessionId) {
  var activeContext = this._activeContext(uiSessionId);
  if (activeContext) {
    activeContext._validateAndSetFocus(activeContext._lastFocusedElement);
  }
};

/**
 * Requests the focus for the given element, but only if being a valid focus location.
 */
scout.FocusManager.prototype.requestFocus = function(uiSessionId, element) {
  var activeContext = this._activeContext(uiSessionId);
  if (activeContext) {
    activeContext._validateAndSetFocus(element);
  }
};

/**
 * Registers the given focus context, or moves it on top if already registered.
 */
scout.FocusManager.prototype._registerContextIfAbsentElseMoveTop = function(focusContext) {
  var focusContexts = this._contextsBySession(focusContext._uiSessionId);

  scout.arrays.remove(focusContexts, focusContext);
  focusContexts.push(focusContext);
};

/**
 * Returns all focus contexts for the given session.
 */
scout.FocusManager.prototype._contextsBySession = function(uiSessionId) {
  return this._sessionFocusContexts[uiSessionId].focusContexts;
};

/**
 * Returns all registered glasspane targets for the given session.
 */
scout.FocusManager.prototype._glassPaneTargetsBySession = function(uiSessionId) {
  return this._sessionFocusContexts[uiSessionId].glassPaneTargets;
};

/**
 * Returns the currently active focus context for the given session, or null if not applicable.
 */
scout.FocusManager.prototype._activeContext = function(uiSessionId) {
  return scout.arrays.last(this._contextsBySession(uiSessionId));
};

/**
 * Returns the focus context with is associated with the given $container, or null if not applicable.
 */
scout.FocusManager.prototype._contextByContainer = function(uiSessionId, $container) {
  return scout.arrays.find(this._contextsBySession(uiSessionId), function(context) {
    return context._$container === $container;
  });
};

/**
 * Returns whether to accept a 'mouse-down-event' for focus change,
 * and is allowed if one of the following criteria match:
 *
 *  - on natively focusable elements, or elements with a tab index;
 *  - if the source is not the glasspane;
 *  - if the source is not a menu or button;
 *  - if the source contains some selectable text;
 *  - if the source is contained within a focusable DOM parent;
 */
scout.FocusManager.prototype._acceptFocusChangeOnMouseDown = function($element, $entryPoint, uiSessionId) {
  // 1. Prevent focus gain when glasspane is clicked.
  //    Even if the glasspane is not focusable, this check is required because the glasspane might be contained in a focusable container
  //    like table. Use case: outline modality with table-page as 'outlineContent'.
  if ($element.hasClass('glasspane')) {
    return false;
  }

  // 2. Prevent focus gain if covert by glasspane.
  if (!this._isElementAccessible($element, uiSessionId)) {
    return false;
  }

  // 3. Prevent focus gain on button click.
  //    That is because buttons only can gain focus by keyboard (tabbing).
  if (this._isMenuOrButton($element)) {
    return false;
  }

  // 4. Allow focus gain on focusable elements.
  if ($element.is(':focusable')) {
    return true;
  }

  // 5. Allow focus gain on elements with selectable content, e.g. the value of a label field.
  if (this._isSelectableText($element)) {
    return true;
  }

  // 6. Allow focus gain on elements with a focusable parent, e.g. when clicking on a row in a table.
  if (this._containsFocusableParent($element, $entryPoint)) {
    return true;
  }

  return false;
};

/**
 * Checks if the given element is a menu or button.
 */
scout.FocusManager.prototype._isMenuOrButton = function($element) {
  return $element.data('menu') || $element.data('button') || $element.data('buttonOption');
};

/**
 * Checks if the element contains a parent which is focusable.
 */
scout.FocusManager.prototype._containsFocusableParent = function($element, $entryPoint) {
  return $element.parents(":focusable").not($entryPoint).length; // exclude entry point; is only focusable to provide Portlet support;
};

/**
 * Checks if the given element represents content which is selectable to the user, e.g. to be copied into clipboard.
 */
scout.FocusManager.prototype._isSelectableText = function($element) {
  return $element
    .clone()
    .children()
    .remove()
    .end()
    .text().trim().length && $element.css('user-select') !== 'none';
};

/**
 * Checks if the given element is accessible, meaning not covert by a glasspane.
 */
scout.FocusManager.prototype._isElementAccessible = function(element, uiSessionId) {
  var glassPaneTargets = this._glassPaneTargetsBySession(uiSessionId);
  if (!glassPaneTargets.length) {
    return true;
  }

  var i, $glassPaneTarget;
  for (i = 0; i < glassPaneTargets.length; i++) {
    if ($(element).closest($(glassPaneTargets[i])).length) {
      return false;
    }
  }

  return true;
};

// Singleton
scout.focusManager = new scout.FocusManager();
