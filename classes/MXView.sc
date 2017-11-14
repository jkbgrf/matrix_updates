
MXSlider {
  var <view, <>action, <>mouseOverAction, <>keyDownAction, value;
  var <background, <knobColor, <step, bounds, <thumbSize=0, <inset=4;
  var <border=0, <borderColor, <radius=0;
  var pen, pad;
  var bottom, height, vertical=true, slrect, viewrect;

  *new { arg parent, bounds, radius = 0;  // radius for rounded corners;
    ^super.new.initView(parent, bounds, radius)
  }

  initView { arg parent, argbounds, argradius;
    var viewFunc;
    
    bounds = argbounds.asRect;  
    radius = argradius;
    viewrect = bounds.moveTo(0,0);
    slrect = bounds.insetBy(inset); 
    value = 0;
    knobColor = Color.grey(0.4);
    background = Color.grey(0.7);
    border = 0;
    borderColor = Color.black;
    pen = SCPen;
    pad = if( GUI.id === \cocoa, 0, 0.5 );
    
    
    view = SCUserView(parent, bounds);
    view.focusColor = Color.grey(0, 0);
  //  view.relativeOrigin = true;
    view
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers;
        //hit = Point(x,y);
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseDownAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseUpAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        var width, height, knobrect, pos;
        //width = view.bounds.width;
        //height = view.bounds.height;
        //"draw".postln;    
        pen.width = 1;
        pen.color = background;
        if (radius > 0) { pen.roundedRect(viewrect, radius).fill } { pen.fillRect(viewrect) };
        
        if(vertical) {
          height = slrect.height * value;
          if(thumbSize > 0) {
            knobrect = Rect(0, max(0,slrect.height - height - thumbSize), slrect.width, thumbSize*2);
          } {
            knobrect = Rect(0, slrect.height - height, slrect.width, height);
          } 
        } {  width = slrect.width * value;
          if(thumbSize > 0) {
            knobrect = Rect(max(0, width - thumbSize), 0, thumbSize*2, slrect.height);
          } {
            knobrect = Rect(0, 0, width, slrect.height);
          }
        };
        knobrect = knobrect.moveBy(inset, inset);
        if(knobrect.width < 2) { knobrect.width = 2 };
        if(knobrect.height < 2) { knobrect.height = 2 };
        
        pen.color = knobColor;
        // pen.fillRect(knobrect);
        if (radius > 0) { pen.roundedRect(knobrect, radius).fill } { pen.fillRect(knobrect) };

        if(border > 0) { 
          pen.width = border;
          pen.color = borderColor;
          if (radius > 0) { pen.roundedRect(viewrect, radius).stroke } { pen.strokeRect(viewrect) };
        };  
        
        pen.stroke;
      })
      .canReceiveDragHandler_({
        GUI.view.currentDrag.isFloat;
      })
      .receiveDragHandler_({
        this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });
    
    vertical = view.bounds.height > view.bounds.width; // true: vertical, false: horizontal

    viewFunc = { arg v, x, y, modifiers;
      //[x,y, v.bounds].postln;
      bottom = slrect.bottom;
      height = slrect.height;
    //  [x,y].postln;
    //  if(vertical) { value = (bottom - y) / height } { value = (x-slrect.left) / slrect.width };
    //  if(vertical) { value = (bottom - y) / height } { value = x / slrect.width }; // <--- letzte Fassung!
      if(vertical) { value = (height - y) / height } { value = x / slrect.width };
      value = value.clip(0,1);
    //  value.postln;
      this.action.value(this, x, y, modifiers);
      view.refresh;
    };

    keyDownAction = { arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl;
    var link;
    this.value = ctl.input;
    this.action_( {arg view;
      ctl.input_( view.value ); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.value = ctl.input; } }
      });
    view.onClose = {  link.remove };
  }

  value_ {arg val;
    value = val.clip(0.0, 1.0);
    view.refresh;
    ^value;
  }

  value {
    ^value;
  } 

  valueAction_ {arg val;
    //value = val.clip(0.0, 1.0);
    value = val.clip(0,1);
    action.value(this);
    view.refresh;
    ^value
  }
  
  knobColor_ { arg color;
    knobColor = color;
    view.refresh;
  }

  background_ { arg color;
    background = color;
    view.refresh;
  }

  bounds_ { arg rect;
    view.bounds = rect.asRect;
    slrect = view.bounds.insetBy(inset);  
    view.refresh;
  }

  bounds {
    ^view.bounds
  }

  inset_ { arg size;
    inset = size.clip(0, 10);
    slrect = view.bounds.insetBy(inset);
    view.refresh;
  }

  thumbSize_ { arg size;
    thumbSize = max(size * 0.5, 1);
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }

  step_ { arg stepSize;
    step = stepSize;
  }
  
  increment { ^this.valueAction = value + this.bounds.width.reciprocal }
  
  decrement { ^this.valueAction = value - this.bounds.width.reciprocal }

  isClosed {
    ^view.isClosed;
  }

  notClosed {
    ^view.notClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
    //if (char == $r, { this.valueAction = 1.0.rand; });
    if (char == $n, { this.valueAction = 0.0; });
    if (char == $x, { this.valueAction = 1.0; });
    //if (char == $c, { this.valueAction = 0.5; });
    //if (char == $], { this.increment; ^this });
    //if (char == $[, { this.decrement; ^this });
    if (unicode == 16rF700, { this.valueAction = 1.0; });
    if (unicode == 16rF703, { this.valueAction = 1.0; });
    if (unicode == 16rF701, { this.valueAction = 0.0; });
    if (unicode == 16rF702, { this.valueAction = 0.0; });
  }

}

MXRangeSlider {
  var <view, <>action, <>mouseOverAction, <>keyDownAction;
  var mousepos, mousedist=0, rangerect, which, relx, rely, tolerance, temptolerance;
  var <lo, <hi, <range;
  var <orientation = \v;
  var <background, <knobColor, <step, bounds, <thumbSize=0, <inset=2;
  var <border=0, <borderColor, <radius=0;
  var pen, pad;
  var bottom, height, vertical=true, slrect, viewrect;

  *new { arg parent, bounds, radius = 0;  // radius for rounded corners;
    ^super.new.initView(parent, bounds, radius)
  }

  initView { arg parent, argbounds, argradius;
    var viewFunc;
    var shiftclick=false, altclick=false;
    
    bounds = argbounds.asRect;  
    if( bounds.width > bounds.height )
      { orientation = \h;  };

    radius = argradius;
    viewrect = bounds.moveTo(0,0);
    slrect = bounds.insetBy(inset); 
    step = 0.01;
    lo = 0.4;
    hi = 0.6;
    range = hi-lo;
    if( orientation == \v) { tolerance = 5 / bounds.height } { tolerance = 5 / bounds.width };
    mousedist=0;

    knobColor = Color.grey(0.4);
    background = Color.grey(0.7);
    border = 0;
    borderColor = Color.black;
    pen = SCPen;
    pad = if( GUI.id === \cocoa, 0, 0.5 );
    
    
    view = SCUserView(parent, bounds);
  //  view.clearOnRefresh_(false);
    view.focusColor = Color.grey(0, 0);
  //  view.relativeOrigin = true;
    view
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers, buttonNumber, clickCount;
        //hit = Point(x,y);
        temptolerance = (range > (2*tolerance)).if(tolerance, 0);
        shiftclick = modifiers.bitTest(1) or: modifiers.bitTest(2); // ((modifiers & 2) + (modifiers & 4)) > 0;
        altclick =  modifiers.bitTest(5); //(modifiers & 32) > 0;
        which = nil;
        //buttonNumber.postln;
        if( orientation == \v ) { 
        //  mousepos = 1 - ((y - v.bounds.top) / v.bounds.height);
          mousepos = 1 - (y / v.bounds.height);
        //  mousedist = y - v.bounds.top - (( 1-hi ) * v.bounds.height );
          mousedist = y - (( 1-hi ) * v.bounds.height );
        } { 
      //    mousepos = (x - v.bounds.left) / v.bounds.width;
      //    mousedist = x - v.bounds.left - (hi * v.bounds.width );
          mousepos = x / v.bounds.width;
          mousedist = x - (hi * v.bounds.width );
        };  
        mousepos = mousepos.clip(0,1);
        if (shiftclick) {
          which = \shift; 
          hi = lo = mousepos; 
          range = 0; 
          mousedist = 0;
        } {
          if ( altclick) {
            which = \alt;
            lo = (mousepos - (range * 0.5)).clip(0,1);
            hi = (lo + range).clip(0,1);
            range = hi - lo;
            mousedist = 0;
          } {
            if (buttonNumber == 1) { 
              which = \minrange; 
              hi = lo = mousepos; 
              range = 0; 
              mousedist = 0;
            } {
              if ( mousepos >= (hi - temptolerance) ) { 
                which = \hi; 
                if ( mousepos > hi ) {
                  hi = mousepos; 
                  range = (hi - lo).clip(0,1); 
                };  
                mousedist = 0;
              } {
                if ( mousepos <= (lo + temptolerance) ) {
                  which = \lo; 
                  if( mousepos < lo) {
                    lo = mousepos; 
                    range = (hi - lo).clip(0,1); 
                  };  
                  mousedist = 0;
              } { 
                  which = \range;    
                }
              }
            }
          } 
        };  
        //[x,y,relx, rely, which].postln;
        //if(which.notNil) { mousedist = mousepoint - (circles[which].origin) };
        //this.view.action.value(this, x, y, modifiers);
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        if( orientation == \v ) {
        //  mousepos = 1 - ((y - mousedist - v.bounds.top) / v.bounds.height);
          mousepos = 1 - ((y - mousedist ) / v.bounds.height);
        } { 
      //    mousepos = (x - mousedist - v.bounds.left) / v.bounds.width;
          mousepos = (x - mousedist) / v.bounds.width;
        };
        mousepos = mousepos.clip(0,1);
        switch (which)
          { \lo }     { lo = min(mousepos, hi);  range = (hi - lo).clip(0,1) }            { \hi }     { hi = max(mousepos, lo);  range = (hi - lo).clip(0,1) }            { \range }  { hi = max(range, mousepos);  lo = (hi - range).clip(0,1); }            { \minrange } { hi = lo = mousepos;  range = 0;}            { \shift }    { if (mousepos >= lo) 
                  { hi = mousepos; }
                  { lo = mousepos; };                range = (hi - lo).clip(0,1);
                }
          { \alt }    { lo = min(1-range, (mousepos - (range * 0.5)).clip(0,1));
                  hi = (lo + range).clip(0,1);
                 //range = hi - lo;
                }
          ;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseUpAction_({arg v, x, y, modifiers;
        which = nil;
        v.refresh;  
        // viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        var width, height, knobrect, pos;
        width = slrect.width;
        height = slrect.height;
        //"draw".postln;    
        pen.width = 1;
        pen.color = background;
      //  if (radius > 0) { pen.roundedRect(viewrect, radius).fill } { pen.fillRect(viewrect) };
        if (radius > 0) { pen.roundedRect(viewrect, radius).fill } { pen.fillRect(viewrect) };
        
        if(vertical) {
        //  height = slrect.height * value;
          knobrect = Rect(1, height - 2  * (1-(lo+range)) + 1 , width - 2, max(height - 2 * range, 1) );
        //  knobrect = Rect(0, max(0,slrect.height - height - thumbSize), slrect.width, thumbSize*2);

        } {  
        //  width = slrect.width * value;
          knobrect = Rect(width - 2 * lo + 1, 1, max(width - 2 * range, 1), height - 2);
        //  knobrect = Rect(max(0, width - thumbSize), 0, thumbSize*2, slrect.height);
        };
        knobrect = knobrect.moveBy(inset, inset);
        if(knobrect.width < 2) { knobrect.width = 2 };
        if(knobrect.height < 2) { knobrect.height = 2 };
        
        pen.color = knobColor;
        // pen.fillRect(knobrect);
        if (radius > 0) { pen.roundedRect(knobrect, radius).fill } { pen.fillRect(knobrect) };

        if(border > 0) { 
          pen.width = border;
          pen.color = borderColor;
      //    if (radius > 0) { pen.roundedRect(viewrect, radius).stroke } { pen.strokeRect(viewrect) };
          if (radius > 0) { pen.roundedRect(viewrect, radius).stroke } { pen.strokeRect(viewrect) };
        };  
        
        pen.stroke;
      })
      .canReceiveDragHandler_({
        GUI.view.currentDrag.isFloat;
      })
      .receiveDragHandler_({
        this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });
    
    vertical = orientation == \v; // true: vertical, false: horizontal

    viewFunc = { arg v, x, y, modifiers;
      //[x,y, v.bounds].postln;
     // bottom = slrect.bottom;
     // height = slrect.height;
    //  if(vertical) { value = (height - y) / height } { value = x / slrect.width };
    //  value = value.clip(0,1);
      this.action.value(this, x, y, modifiers);
      view.refresh;
    };

    keyDownAction = { arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connectLo { arg ctl;
    var link;
    this.lo = ctl.input;
    this.action_( {arg view;
      ctl.input_( view.lo ); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.lo = ctl.input; } }
      });
    view.onClose = {  link.remove };
  }

  connectHi { arg ctl;
    var link;
    this.hi = ctl.input;
    this.action_( {arg view;
      ctl.input_( view.hi ); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.hi = ctl.input; } }
      });
    view.onClose = {  link.remove };
  }

  connect { arg ctl;
    var link;
    this.value = ctl.input;
    this.action_( {arg view;
      ctl.input_( this.value ); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.value = ctl.input; } }
      });
    view.onClose = {  link.remove };
  }

  orientation_ { arg val;
    if( val.asSymbol == \v) { orientation = \v; tolerance = 5 / bounds.height; view.refresh; };
    if( val.asSymbol == \h) { orientation = \h; tolerance = 5 / bounds.width; view.refresh; };
  }

  value_ { arg val;  
    val = val.clip(0,1);
    this.setSpan(val, val);
    //value = val.clip(0.0, 1.0);
    //view.refresh;
  }
  
  value { // "center"
    ^lo + (range*0.5);
  }

  valueAction_ { arg val;
    this.value_(val);
    action.value(this);
    //value = val.clip(0.0, 1.0);
    //action.value(this);
    //view.refresh;
  }

  lo_ { arg val;
    //this.setProperty(\lo, val);
    lo = val.clip(0,1);
    range = hi - lo;
    view.refresh;
  } 
  
  activeLo_ { arg val;
    //this.setPropertyWithAction(\lo, val);
    this.lo_(val);
    //this.doAction;
    action.value(this);
  } 

  hi_ { arg val;
    hi = val.clip(0,1);
    range = hi - lo;
    view.refresh;
  }
    
  activeHi_ { arg val;
    this.hi_(val);
    //this.doAction;
    action.value(this);
  } 

  range_ { arg val;
    range = val.clip(0,1);
    hi = (lo + range).clip(0,1);
    view.refresh;
  } 

  activeRange_ { arg val;
    this.range_(val);
    //this.doAction;
    action.value(this);
  } 

  center_ { arg val;
    var l;
    l = (val.clip(0,1) - (range * 0.5)).clip(0,1);
    this.setSpan(l, l + range);
  } 

  activeCenter_ { arg val;
    this.center_(val);
    action.value(this);
  } 

  center { 
    ^lo + (range*0.5);
  }

  setSpan { arg lo, hi;
    this.lo = lo;
    this.hi = hi;
  }
  
  setSpanActive { arg lo, hi;
    this.setSpan( lo, hi );
    //this.doAction;
    action.value(this);
  }
  
  knobColor_ { arg color;
    knobColor = color;
    view.refresh;
  }

  background_ { arg color;
    background = color;
    view.refresh;
  }

  bounds_ { arg rect;
    view.bounds = rect.asRect;
    slrect = view.bounds.insetBy(inset);  
    view.refresh;
  }

  bounds {
    ^view.bounds
  }

  inset_ { arg size;
    inset = size.clip(0, 10);
    slrect = view.bounds.insetBy(inset);
    view.refresh;
  }

  thumbSize_ { arg size;
    thumbSize = max(size * 0.5, 1);
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }

  step_ { arg stepSize;
    step = stepSize;
  }
  
  increment { 
    var inc, val; 
    if( orientation == \v ) 
      { inc = this.bounds.height.reciprocal }
      {Êinc = this.bounds.width.reciprocal };
      
    val = this.hi + inc;
    if (val > 1, {
      inc = 1 - this.hi;
      val = 1;
    });
    //this.activeLo_(this.lo + inc);
    //this.activeHi_(val);
    this.setSpanActive( this.lo + inc, val);
  }
  decrement { 
    var inc, val;
    if( orientation == \v ) 
      { inc = this.bounds.height.reciprocal }
      {Êinc = this.bounds.width.reciprocal };
    val = this.lo - inc;
    if (val < 0, {
      inc = this.lo;
      val = 0;
    });
    //this.activeLo_(val);
    //this.activeHi_(this.hi - inc);
    this.setSpanActive( val, this.hi - inc );
  }

  isClosed {
    ^view.isClosed;
  }

  notClosed {
    ^view.notClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    var a, b;
    switch ( char )
      { $r }  { 
          a = 1.0.rand; b = 1.0.rand; 
          this.setSpanActive(min(a, b), max(a, b));
          }
      { $n } {  this.setSpanActive(0,0); }
      { $0 } {  this.setSpanActive(0,0); }
      { $l } {  this.setSpanActive(0,0); }
      { $x } { this.setSpanActive(1,1); }
      { $1 } { this.setSpanActive(1,1); }
      { $h } { this.setSpanActive(1,1); }
      { $c } { this.setSpanActive(0.5,0.5); }
      { $5 } { this.setSpanActive(0.5,0.5); }
      { $a } { this.setSpanActive(0,1); }
      { $+ } { this.increment; }
      { $- } { this.decrement; }
      ;
    switch ( unicode )
      { 16rF700 } { this.increment; }
      { 16rF703 } { this.increment; }
      { 16rF701 } { this.decrement; }
      { 16rF702 } { this.decrement; }
      ;       
  /*  
    if (char == $n, { this.valueAction = 0.0; });
    if (char == $x, { this.valueAction = 1.0; });
    if (unicode == 16rF700, { this.valueAction = 1.0; });
    if (unicode == 16rF703, { this.valueAction = 1.0; });
    if (unicode == 16rF701, { this.valueAction = 0.0; });
    if (unicode == 16rF702, { this.valueAction = 0.0; });
  */
  }

}


MXButton {
  var <view, <>action, <>mouseOverAction, <>keyDownFunc, value, <>states, <font;
  var <>mouseRightDownFunc, <>mouseLeftDownFunc;
  var <shape=\fillRect, bounds, <radius=0, <shiftx=0, <shifty=0;
  var <border=0, <borderColor;

  *new { arg parent, bounds, shape;  // if shape.isNumber > radius for rounded corners
    ^super.new.initView(parent, bounds, shape)
  }

  initView { arg parent, argbounds, argshape;
    var viewFunc;
    var pressed = false;

    bounds = argbounds.asRect;  
    if (argshape.isNumber) { shape = \fillRect; radius = argshape };
    if (argshape.isSymbol) {Êshape = argshape; radius = 0 };
    if (argshape.isNil) { shape = \fillRect; radius = 0 };
//    shape = argshape ? \fillRect;
//    radius = argradius ? 0;
    value = 0;
    font = MXGUI.buttonFont;
    border = 0;
    borderColor = Color.black;
    
    view = SCUserView(parent, bounds);
    view.focusColor = Color.grey(0, 0);

  //  view.relativeOrigin = true;
    view
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownFunc.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers, buttonNumber;
        //hit = Point(x,y);
        switch (buttonNumber)
          { 0 } {
            if (states.size == 1) { pressed = true };
            viewFunc.value(v, x, y, modifiers);
            mouseLeftDownFunc.value(this, x, y, modifiers, buttonNumber);
            }
          {1 }  {
            mouseRightDownFunc.value(this, x, y, modifiers, buttonNumber);
            }
          ;
      })
      .mouseUpAction_({arg v, x, y, modifiers;
        //hit = Point(x,y);
        pressed = false;
        // viewFunc.value(v, x, y, modifiers);
        v.refresh;
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        //viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg view; 
        var width, height, boxcolor, stringcolor, string, rect;
        rect = view.bounds.moveTo(0,0);
        width = rect.width;
        height = rect.height;
        boxcolor = pressed.if({ Color.grey(0.9) }, { states[value][2] });
        stringcolor = states[value][1];
        string = states[value][0];
            
        if (radius > 0) { 
          if (border > 0) {
            SCPen.fillColor = borderColor;
            SCPen.roundedRect(rect, radius);
            SCPen.fill; 
          };
          SCPen.fillColor = boxcolor;
          SCPen.roundedRect(rect.insetBy(border, border), radius);
          SCPen.fill
        } { 
          if (border > 0) {
            SCPen.fillColor = borderColor;
            if (shape == \fillRect) { SCPen.fillRect(rect) } { SCPen.fillOval(rect) };
          };
          SCPen.fillColor = boxcolor;
          SCPen.perform(shape, rect.insetBy(border, border) );
        };  
      /*  
        if (radius > 0) { 
        //  Pen.strokeColor = borderColor;
          Pen.roundedRect(rect, radius);
          Pen.fill;
        } { 
          SCPen.color = boxcolor;
          SCPen.perform(shape, rect );
        };  
      */  
        SCPen.fillColor = stringcolor;
        // font.setDefault;
        SCPen.font = font;
        SCPen.stringCenteredIn(string, rect.moveBy(shiftx, shifty));
        //Pen.stringInRect(string, view.bounds);
      //  SCPen.stroke;


      })
      .canReceiveDragHandler_({
        View.currentDrag.isFloat
      })
      .receiveDragHandler_({
        this.valueAction = View.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });

    viewFunc = {arg v, x, y, modifiers;
      value = (value.asInteger + 1).wrap(0, states.size - 1);
      this.action.value(this, x, y, modifiers);
      v.refresh;
    };

    keyDownFunc = {arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl;
    var link;
    this.value = ctl.input;
    this.action_( {arg view;
      ctl.input_( view.value ); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.value = ctl.input } };
      });
    view.onClose = {  link.remove };
  }

  value_ { arg val;
    value = val.clip(0.0, 1.0);
    view.refresh;
    ^value;
  }

  value {
    ^value;
  }

  valueAction_ { arg val;
    //value = val.clip(0.0, 1.0);
    value = val.clip(0, states.size - 1);
    action.value(this);
    view.refresh;
    ^value
  }

  string_ { arg string;
    states.do { arg st, i;   st[0] = string.asString; };
    view.refresh;
  } 
    
  isClosed {
    ^view.isClosed;
  }

  notClosed {
    ^view.notClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  font_ { arg f;  font = f; view.refresh;  }

  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }

  bounds_ { arg rect;
    view.bounds = rect.asRect;
    view.refresh;
  //  ^this
  }

  bounds {
    ^view.bounds
  }

  shiftx_ { arg value;
    shiftx = value;
    view.refresh;
  }

  shifty_ { arg value;
    shifty = value;
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }

  focusColor_ { arg color;
    view.focusColor = color;
    view.refresh;
  }

  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
    //if (char == $r, { this.valueAction = 1.0.rand; });
    if (char == $n, { this.valueAction = 0.0; });
    if (char == $x, { this.valueAction = 1.0; });
    //if (char == $c, { this.valueAction = 0.5; });
    //if (char == $], { this.increment; ^this });
    //if (char == $[, { this.decrement; ^this });
    if (unicode == 16rF700, { this.valueAction = 1.0; });
    if (unicode == 16rF703, { this.valueAction = 1.0; });
    if (unicode == 16rF701, { this.valueAction = 0.0; });
    if (unicode == 16rF702, { this.valueAction = 0.0; });
  }

}


MXStatusButton : MXButton {

  connect { arg ctl;
    var link;
    this.value = ctl.input;
    this.action_( {ctl.input_(this.value); } );
    link = SimpleController(ctl).put(\synch, {
        defer { if(this.isClosed.not) { this.value = ctl.input; } }
      });
    view.onClose = {  link.remove };
  }
}


MXNumber {
  var <view, <>action, <>mouseOverAction, <>keyDownAction, value;
  var <unit = "";
  var <skin;
  var <step, <background, <stringColor, <font, <align = \left, <shiftx=0, <shifty=0;
  var <shape=\fillRect, <radius=0;
  var bounds, <inset=0, <viewrect;
  var <border=0, <borderColor;
  var hit;

  *new { arg parent, bounds, shape;  // if shape.isNumber > radius for rounded corners
    ^super.new.initView(parent, bounds, shape)
  }

  initView { arg parent, argbounds, argshape;
    var viewFunc;

    value = 0;
    step = 0.01;
    
    bounds = argbounds.asRect;  
    viewrect = bounds.moveTo(0,0).insetBy(inset);   if (argshape.isNumber) { shape = \fillRect; radius = argshape };
    if (argshape.isSymbol) {Êshape = argshape; radius = 0 };
    if (argshape.isNil) { shape = \fillRect; radius = 0 };
  
    stringColor = Color.black;
    background = Color.white;
    border = 0;
    borderColor = Color.black;
    font = MXGUI.numberFont;

  //  pad = if( GUI.id === \cocoa, 0, 0.5 );
  
    view = SCUserView(parent, bounds)
      .focusColor_(Color.grey(0, 0))
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers;
        hit = Point(x,y);
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        // var direction = 1.0;
        // horizontal or vertical scrolling:
        if (  (y - hit.y) > 0 ) { 
          case
            { hit.x >= (bounds.width * 0.5) } { this.decrement }
            { hit.x < (bounds.width * 0.1) } { this.decrement1000 }
            { hit.x < (bounds.width * 0.25) } { this.decrement100 }
            { hit.x < (bounds.width * 0.5) } { this.decrement10 }
          ;
        };
        if (  (y - hit.y) < 0 ) { 
          case
            { hit.x >= (bounds.width * 0.5) } { this.increment }
            { hit.x < (bounds.width * 0.1) } { this.increment1000 }
            { hit.x < (bounds.width * 0.25) } { this.increment100 }
            { hit.x < (bounds.width * 0.5) } { this.increment10 }
          ;
        };  
    /*    if ( (x - hit.x) < 0 or: { (y - hit.y) > 0 }) { 
          // direction = -1.0; 
          this.decrement;
        } {
          this.increment;
        };  
    */    
      hit = Point(x, y);
        viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        var width, height, relbounds, string;
        width = viewrect.width;
        height = viewrect.height;
      //  relbounds = v.bounds.moveTo(0,0);
        
        if (radius > 0) { 
          SCPen.width = border;
          SCPen.fillColor = background;
          SCPen.strokeColor = borderColor;
          SCPen.roundedRect(viewrect, radius);
          if (border > 0) { SCPen.fillStroke } { SCPen.fill };
          
        } { 
          SCPen.width = border;
          SCPen.fillColor = background;
          SCPen.strokeColor = borderColor;
          SCPen.perform(shape, viewrect );
          if (border > 0) {Ê
            if (shape == \fillRect) { SCPen.strokeRect(viewrect) } { SCPen.strokeOval(viewrect) };
          };
        };  
        
    //    SCPen.color = background;
    //    SCPen.fillRect(viewrect);
        
        SCPen.color = stringColor;
        SCPen.font = font;
        string = value.asString ++ unit;
        switch (align)
          { \left } { SCPen.stringLeftJustIn(string, viewrect.moveBy(shiftx, shifty)); }
          { \right } { SCPen.stringRightJustIn(string, viewrect.moveBy(shiftx, shifty)); }
          { \center } { SCPen.stringCenteredIn(string, viewrect.moveBy(shiftx, shifty)); }
          ;
      //  Pen.stringCenteredIn(string, viewrect);
      //  Pen.stringLeftJustIn(value.asString, viewrect);
      //  Pen.stringInRect(value.asString, view.bounds);
        
      /*  if(border > 0) { 
          SCPen.width = border;
          SCPen.color = borderColor;
          SCPen.strokeRect(viewrect);
        };  
      */
        SCPen.stroke;
      })
      .canReceiveDragHandler_({
        GUI.view.currentDrag.isFloat
      })
      .receiveDragHandler_({
        this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });

    viewFunc = {arg v, x, y, modifiers;
      //value = (value + 1).wrap(0, states.size - 1);
      this.action.value(this, x, y, modifiers);
      v.refresh;
    };

    keyDownAction = {arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl;
    var link;
    this.value_(ctl.value);
    this.step = ctl.spec.step;
    this.action_({ctl.value_(this.value); });
    link = SimpleController(ctl)
      .put(\synch, 
       { arg changer, what;
        defer({ 
        if(this.isClosed.not) {
          this.value = ctl.value                  .round(pow(10, floor(max(min(0, ctl.value.abs.log10 - 3).asInteger,-12))));
        } 
        });
      }
    );
    view.onClose = {  link.remove };
  }


  value_ {arg val;
    value = val.round(step);
  //  value = val.clip(0.0, 1.0);
    view.refresh;
  //  ^value;
  }

  value {
    ^value;
  }
  
  valueAction_ {arg val;
    //value = val.clip(0.0, 1.0);
    //value = val.clip(0, states.size - 1);
    value = val.round(step);
    action.value(this);
    view.refresh;
  //  ^value
  }
  
  step_ { arg stepSize;
    step = stepSize;
  }
  
  increment { ^this.valueAction = value + step }
  
  decrement { ^this.valueAction = value - step }
  
  increment10 { ^this.valueAction = value + (step * 10) }

  decrement10 { ^this.valueAction = value - (step * 10)}

  increment100 { ^this.valueAction = value + (step * 100) }

  decrement100 { ^this.valueAction = value - (step * 100) }

  increment1000 { ^this.valueAction = value + (step * 1000) }

  decrement1000 { ^this.valueAction = value - (step * 1000) }


  font_ { arg f;  font = f; view.refresh;  }

  unit_ { arg string;
    unit = string.asString;
    view.refresh;
  }

  bounds_ { arg rect;
    view.bounds = rect.asRect;
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);  
    view.refresh;
  } 

  bounds {
    ^view.bounds
  }
  
  inset_ { arg size;
    inset = size.clip(0, 10);
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);    view.refresh;
  }

  align_ { arg side;
    align = [\left, \center, \right].includes(side).if(side, \left);
    view.refresh;
  } 
    
  shiftx_ { arg value;
    shiftx = value;
    view.refresh;
  }

  shifty_ { arg value;
    shifty = value;
    view.refresh;
  }

  stringColor_ { arg color;
    stringColor = color;
    view.refresh;
  }

  normalColor_ { arg color;
    stringColor = color;
    view.refresh;
  }

  background_ { arg color;
    background = color;
    view.refresh;
  }

  focusColor_ { arg color;
    view.focusColor = color;
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }
/*
  skin_ { arg dict:
    font = dict.font;
    borderColor
    background  
    stringColor
    shiftx
    shifty
    align
    inset
  }
*/
  isClosed {
    ^view.isClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
    if (char == $r, { this.valueAction = 1.0.rand; });
    if (char == $n, { this.valueAction = 0.0; });
    if (char == $x, { this.valueAction = 1.0; });
    if (char == $c, { this.valueAction = 0.5; });
    if (char == $], { this.increment; ^this });
    if (char == $[, { this.decrement; ^this });
    if (unicode == 16rF700, { this.increment; ^this });
    if (unicode == 16rF703, { this.increment; ^this });
    if (unicode == 16rF701, { this.decrement; ^this });
    if (unicode == 16rF702, { this.decrement; ^this });
  }

}


MXLEDView {
  var <view, <>action, <>mouseOverAction, <>keyDownAction, value;
  var <background, <color;
  var bounds, <inset=0, <viewrect;
  var <border=0, <borderColor;
  var hit;

  *new { arg parent, bounds;
    ^super.new.initView(parent, bounds)
  }

  initView { arg parent, argbounds;
    var viewFunc;

    value = 0;
    
    bounds = argbounds.asRect;  
    viewrect = bounds.moveTo(0,0).insetBy(inset); 
    background = Color.black; // Color when value == 0.0:
    color = Color.green;    // Color when value == 1.0:
    border = 0;
    borderColor = Color.black;

  //  pad = if( GUI.id === \cocoa, 0, 0.5 );
  
    view = SCUserView(parent, bounds)
      .focusColor_(Color.grey(0, 0))
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        if (border > 0) {
          SCPen.fillColor = borderColor;
          SCPen.fillOval(viewrect);
        };
        SCPen.fillColor = background.blend(color, value);
        SCPen.fillOval(viewrect.insetBy(border, border));
      })
      .canReceiveDragHandler_({
        GUI.view.currentDrag.isFloat
      })
      .receiveDragHandler_({
        this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });

    viewFunc = {arg v, x, y, modifiers;
      //value = (value + 1).wrap(0, states.size - 1);
      this.action.value(this, x, y, modifiers);
      v.refresh;
    };

    keyDownAction = {arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl;
    var link;
    this.value_(ctl.input);
    this.action_({ctl.input_(this.value); });
    link = SimpleController(ctl)
      .put(\synch, 
       { arg changer, what;
        defer({ 
          if(this.isClosed.not) { this.value = ctl.input };       });
      }
    );
    view.onClose = {  link.remove };
  }


  value_ {arg val;
    value = val.clip(0.0, 1.0);
    view.refresh;
  }

  value {
    ^value;
  }
  
  valueAction_ {arg val;
    value = val.clip(0.0, 1.0);
    action.value(this);
    view.refresh;
  }
  
  bounds_ { arg rect;
    view.bounds = rect.asRect;
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);    view.refresh;
  } 

  bounds {
    ^view.bounds
  }
  
  inset_ { arg size;
    inset = size.clip(0, 10);
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);    view.refresh;
  }

  color_ { arg col;
    color = col;
    view.refresh;
  }

  background_ { arg color;
    background = color;
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }

  isClosed {
    ^view.isClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
  //  if (char == $r, { this.valueAction = 1.0.rand; });
  //  if (unicode == 16rF700, { this.increment; ^this });
  }

}

MXLEDMeterView {
  var <view, <>action, <>mouseOverAction, <>keyDownAction;
  var <scaleView, <ledView;
  var value, <ledValues, <peakValue = -100.0, <clipValue = 0.0;
  var <font, <align = \left, <orientation = \vertical; // \horizontal
  var <shape=\fillRect, bounds, <radius=0, <inset=0; // shape and radius only for the LEDs !
  var <viewrect, <scaleViewRect, <shiftx=0, <shifty=0;
  var width, height;
  var <background, <stringColor, <ledColors;
  var <border=0, <borderColor;
  var hit;

  *new { arg parent, bounds, shape;  // if shape.isNumber > radius for rounded corners
    ^super.new.initView(parent, bounds, shape)
  }

  initView { arg parent, argbounds, argshape;
    var viewFunc;

    value = -90;
    ledValues = [-70, -60, -50, -40, -30, -20, -12, -6, -1, clipValue];
    ledValues = ledValues.reverse;
    ledColors = { Color.green }.dup(ledValues.size - 2) ++ [ Color.yellow, Color(1, 0.2, 0) ];
    ledColors = ledColors.reverse;

    background = Color.black;  
  //  color = Color.green;     
    border = 0;
    borderColor = Color.black;
    
    bounds = argbounds.asRect;  
    
    view = SCCompositeView(parent, bounds);
    view.background = background;
    
    this.calcBounds;
    if (argshape.isNumber) { shape = \fillRect; radius = argshape };
    if (argshape.isSymbol) {Êshape = argshape; radius = 0 };
    if (argshape.isNil) { shape = \fillRect; radius = 0 };
  

  //  pad = if( GUI.id === \cocoa, 0, 0.5 );
  
    scaleView = SCUserView(view, scaleViewRect)
      .focusColor_(Color.grey(0, 0))
      .drawFunc_({ arg v; 
        var ledrect;
        // background
      //  SCPen.fillColor = background;
      //  SCPen.fillRect(v.bounds.moveTo(0,0));
  
        SCPen.fillColor = Color.white;            SCPen.font = Font("Arial", 9);
  
        // dB scale
        ledValues.do { arg led, i;
          var str;
          ledrect = Rect(0, i * height + 1, width, height - 2);
            str = (i == 0).if( "OL", {Êled.asString });
          SCPen.stringRightJustIn( str, Rect(0, i * height + 1 , width , height - 2 ));
        };
      });

    ledView = SCUserView(view, viewrect)
      .focusColor_(Color.grey(0, 0))
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        var ledrect, peakled;
        // background
      //  SCPen.fillColor = background;
      //  SCPen.fillRect(v.bounds.moveTo(0,0));
        
        // dB scale --> scaleView !!
        
        // leds
        ledValues.do { arg led, i;
          ledrect = Rect(0, i * height + 1, width-2, height - 2);
          SCPen.color = ledColors[i].alpha_( (value >= led).if(1, 0.4) );
          if (radius > 0) { 
            SCPen.roundedRect(ledrect, radius);
            SCPen.fill ;
          } { 
            SCPen.perform(shape, ledrect );
          };
        };
        
        // peakvalue:
        peakled = case 
          { peakValue >= clipValue} { 0 }
          { peakValue < ledValues.last } { nil }
          { peakled = ledValues.detectIndex({ arg item, i; ( peakValue < item ) && ( peakValue >= ledValues[i+1])}) + 1};
          
        if (peakled.notNil ) {Ê
          SCPen.color = ledColors[peakled].alpha_(1);         ledrect = Rect(0, peakled * height + 1, width-2, height - 2);
          if (radius > 0) { 
            SCPen.roundedRect(ledrect, radius);
            SCPen.fill ;
          } { 
            SCPen.perform(shape, ledrect );
          };
        };
          
  /*      
        // border
        if (border > 0) {
          SCPen.width = border;
          SCPen.strokeColor = borderColor;
          SCPen.strokeRect(viewrect);
        };
  */
      })
      .canReceiveDragHandler_({
        // GUI.view.currentDrag.isFloat
      })
      .receiveDragHandler_({
        // this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });

    viewFunc = {arg v, x, y, modifiers;
      //value = (value + 1).wrap(0, states.size - 1);
      this.action.value(this, x, y, modifiers);
      v.refresh;
    };

    keyDownAction = {arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  calcBounds {
    width = (bounds.width - inset) * 0.5 - 2;
    viewrect = bounds.moveBy(width,2).insetBy(inset).width_(width);
    height = (viewrect.height / ledValues.size).asInteger;
    scaleViewRect = bounds.moveBy(0,2).insetBy(inset).width_(width);
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl;
    var link;
    this.value_(ctl.input);
    this.action_({ctl.input_(this.value); });
    link = SimpleController(ctl)
      .put(\synch, 
       { arg changer, what;
        defer({ 
          if(this.isClosed.not) { this.value = ctl.input };       });
      }
    );
    view.onClose = {  link.remove };
  }

  ledValues_ {arg array;
    clipValue = array.last;
    ledValues = array.reverse;  
    if (ledColors.size < ledValues.size) {
      ledColors = ledColors ++ { ledColors.last }.dup(ledValues.size - ledColors.size);
    };
    this.calcBounds;
    scaleView.refresh;
    ledView.refresh;
  }

  value_ { arg val;
    if (value != val) {
      value = val;
      ledView.refresh;
    };  
  }

  value {
    ^value;
  }
  
  valueAction_ { arg val;
    if (value != val) {
      value = val;
      action.value(this);
      ledView.refresh;
    };  
  }
  
  peakValue_ { arg val;
    if (peakValue != val) {
      peakValue = val;
      ledView.refresh;
    };  
  }

  clipValue_ { arg val;
    if (clipValue != val) {
      clipValue = val;
      ledValues[0] = clipValue;
      ledView.refresh;
    };  
  }
  
  bounds_ { arg rect;
    bounds = rect.asRect;
    this.calcBounds;
    ledView.refresh;
    scaleView.refresh;
  } 

  bounds {
    ^view.bounds
  }
  
  inset_ { arg size;
    inset = size.clip(0, 10);
    this.calcBounds;
    ledView.refresh;
    scaleView.refresh;
  }

/*  color_ { arg col;
    color = col;
    view.refresh;
  }
*/
  ledColors_ {arg array;
    ledColors = array.reverse;  
    ledView.refresh;
  }

  stringColor_ { arg col;
    stringColor = col;
    ledView.refresh;
    scaleView.refresh;
  }

  background_ { arg color;
    background = color;
    ledView.refresh;
    scaleView.refresh;
  }

  border_ { arg size;
    border = size;
    ledView.refresh;
    scaleView.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    scaleView.refresh;
    ledView.refresh;
  }
  
  font_ { arg f;  
    font = f; 
    scaleView.refresh;  
  }

  isClosed {
    ^view.isClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    ledView.refresh;
    scaleView.refresh;    
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
  //  if (char == $r, { this.valueAction = 1.0.rand; });
  //  if (unicode == 16rF700, { this.increment; ^this });
  }

}


MXStringView {
  var <view, <>action, <>mouseOverAction, <>keyDownAction, string;
  var <font, <align = \left, <orientation = \right; // \up, \down, \right
  var <background, <stringColor;
  var <shape=\fillRect, bounds, <radius=0, <inset=0, <viewrect, <shiftx=0, <shifty=0;
  var <border=0, <borderColor;
  var hit;

  *new { arg parent, bounds, shape;  // if shape.isNumber > radius for rounded corners
    ^super.new.initView(parent, bounds, shape)
  }

  initView { arg parent, argbounds, argshape;
    var viewFunc;

    string = "";
    
    bounds = argbounds.asRect;  
    viewrect = bounds.moveTo(0,0).insetBy(inset);   if (argshape.isNumber) { shape = \fillRect; radius = argshape };
    if (argshape.isSymbol) {Êshape = argshape; radius = 0 };
    if (argshape.isNil) { shape = \fillRect; radius = 0 };
  
    background = Color.black; // Color when value == 0.0:
    stringColor = Color.green;    // Color when value == 1.0:
    border = 0;
    borderColor = Color.black;

  //  pad = if( GUI.id === \cocoa, 0, 0.5 );
  
    view = SCUserView(parent, bounds)
      .focusColor_(Color.grey(0, 0))
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseOverAction_({arg v, x, y; this.mouseOverAction.value(this, x, y); })
      .mouseDownAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .mouseMoveAction_({arg v, x, y, modifiers;
        viewFunc.value(v, x, y, modifiers);
      })
      .drawFunc_({ arg v; 
        var xc, yc;
      //  SCPen.fillColor = background;
      //  SCPen.fillRect(viewrect);
  
        if (radius > 0) { 
          SCPen.width = border;
          SCPen.fillColor = background;
          SCPen.strokeColor = borderColor;
          SCPen.roundedRect(viewrect, radius);
          if (border > 0) { SCPen.fillStroke } { SCPen.fill };
          
        } { 
          SCPen.width = border;
          SCPen.fillColor = background;
          SCPen.strokeColor = borderColor;
          SCPen.perform(shape, viewrect );
          if (border > 0) {Ê
            if (shape == \fillRect) { SCPen.strokeRect(viewrect) } { SCPen.strokeOval(viewrect) };
          };
        };  
        
        SCPen.color = stringColor;
        SCPen.font = font;
        
        xc = viewrect.center.x;
        yc = viewrect.center.y;
        
        switch (orientation) 
          { \up }     { SCPen.rotate( -pi/2, xc, yc) }
          { \down }   {ÊSCPen.rotate( pi/2, xc, yc) }
          { \right }    {  }
          ;
        
        switch (align)
          { \left } { SCPen.stringLeftJustIn(string, viewrect.moveBy(shiftx, shifty)); }
          { \right } { SCPen.stringRightJustIn(string, viewrect.moveBy(shiftx, shifty)); }
          { \center } { SCPen.stringCenteredIn(string, viewrect.moveBy(shiftx, shifty)); }
          ;
      /*
        if(border > 0) { 
          SCPen.width = border;
          SCPen.strokeColor = borderColor;
        //  SCPen.strokeOval(viewrect);
          SCPen.strokeRect(viewrect);
        };
      */  
      })
      .canReceiveDragHandler_({
        GUI.view.currentDrag.isFloat
      })
      .receiveDragHandler_({
        this.valueAction = GUI.view.currentDrag.clip(0.0, 1.0);
      })
      .beginDragAction_({ arg v;  this.value.asFloat;  });

    viewFunc = {arg v, x, y, modifiers;
      //value = (value + 1).wrap(0, states.size - 1);
      this.action.value(this, x, y, modifiers);
      v.refresh;
    };

    keyDownAction = {arg key, modifiers, unicode;
      this.defaultKeyDownAction(key, modifiers, unicode);
    };
  }

  remove {
    if(view.notNil) { view.remove };
  }

  connect { arg ctl, method= \item;
    var link;
    
    this.string_( ctl.perform(method) );
    link = SimpleController(ctl)
      .put(\synch, 
       { arg changer, what;
        defer({ 
          if(this.isClosed.not) { this.string_( ctl.perform(method) ) };        });
      }
    );
    view.onClose = {  link.remove };
  }

  font_ { arg f;  font = f; view.refresh;  }

  string_ {arg str;
    string = str.asString;
    view.refresh;
  }

  string {
    ^string;
  }
  
  align_ { arg side;
    align = [\left, \center, \right].includes(side).if(side, \left);
    view.refresh;
  } 

  orientation_ { arg side;
    orientation = [\left, \up, \down, \right].includes(side).if(side, \right);
    view.refresh;
  } 

  bounds_ { arg rect;
    view.bounds = rect.asRect;
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);    view.refresh;
  } 

  bounds {
    ^view.bounds
  }
  
  inset_ { arg size;
    inset = size.clip(0, 10);
    viewrect = view.bounds.moveTo(0,0).insetBy(inset);    view.refresh;
  }

  shiftx_ { arg value;
    shiftx = value;
    view.refresh;
  }

  shifty_ { arg value;
    shifty = value;
    view.refresh;
  }

  color_ { arg col;
    stringColor = col;
    view.refresh;
  }

  stringColor_ { arg col;
    stringColor = col;
    view.refresh;
  }

  background_ { arg color;
    background = color;
    view.refresh;
  }

  border_ { arg size;
    border = size;
    view.refresh;
  }

  borderColor_ { arg color;
    borderColor = color;
    view.refresh;
  }

  focusColor_ { arg color;
    view.focusColor = color;
    view.refresh;
  }

  isClosed {
    ^view.isClosed;
  }

  canFocus_ { arg state = false;
    view.canFocus_(state);
    ^this
  }

  canFocus {
    ^view.canFocus;
  }

  visible_ { arg bool;
    view.visible_(bool)
  }

  visible {
    ^view.visible
  }
  
  enabled_{ arg bool;
    view.enabled_(bool)
  }
  
  enabled {
    ^view.enabled
  }
  
  refresh {
    view.refresh;
    ^this
  }
    
  properties {
    ^view.properties;
  }

  canReceiveDragHandler_ { arg f;
    view.canReceiveDragHandler_(f);
  }
  
  canReceiveDragHandler {
    ^view.canReceiveDragHandler;
  }
  
  receiveDragHandler_ { arg f;
    view.receiveDragHandler_(f);
  }
  
  receiveDragHandler {
    ^view.receiveDragHandler;
  }
  
  beginDragAction_ { arg f;  view.beginDragAction_(f);  }

  beginDragAction {  ^view.beginDragAction;  }


  defaultKeyDownAction { arg char, modifiers, unicode,keycode;
    // standard keydown
  //  if (char == $r, { this.valueAction = 1.0.rand; });
  //  if (unicode == 16rF700, { this.increment; ^this });
  }

}


