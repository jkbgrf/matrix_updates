/*

--- Based on CV and ControlValue ---

 A CV models a value constrained by a ControlSpec. The value can be a single Float or an array of Floats.
 
 Whenever the CV's value changes, it sends a changed message labeled 'synch'.  This way dependants 
 (such as GUI objects or server value) can be updated with SimpleControllers.  The method 
    aCV-action_(function)
 creates such a connection.
 
 A CV's value can be read with the 'value' message. 
 CV can also be used as a Pattern (in Pbind) or in combination with other Streams.  
*/

MXSimpleSpec {  // for numbers without min/maxvals etc.
  var <>step;
  var <>default;
  
  *new { arg step, default;
    ^super.new.init(step, default);
  }
  
  init { arg st, def;
    step = st ? 1;
    default = def ? 0;
  }
  
  constrain { arg val;
    ^val
  }
  
  map { arg val;
    ^val
  }
  
  unmap { arg val;
    ^val
  }
  
  asSpec { 
    ^this
  }   
} 


MXCV : Stream {
  var <value, <internvalue, <spec;
  var <actions;
  
  *new { | spec = \unipolar, default |      
    ^super.new.spec_(spec,default);
  }

  action_ { | function, what=\synch | ^SimpleController(this) .put(what, function) }

// reading and writing the CV
  value_ { | val |  
    value = spec.constrain(val);
    internvalue = value;
    this.changed(\synch);
  } 
  
  input_  { | in, type=\abs | 
  
    switch(type)
      { \abs } { this.value_(spec.map( in) ) }
      { \rel } { this.value_(spec.map( this.input + in )) }
      { \relswitch } { if ( in > 0) { this.value_(spec.map(1)) } { this.value_(spec.map(0)) } }
      { \incdec } { if ( in > 0) { this.inc } { this.dec } }
//      { \trigger } { if( ( in.isNumber && (in!= 0)) || (in.booleanValue == true) ) { this.value_(spec.map(1.0)) } }
//      { \toggle }  { if( ( in.isNumber && (in!= 0)) || (in.booleanValue == true) ) { this.value_(spec.map( 1 - this.input )) } }
      { \trigger } { if( ( in.isNumber && (in!= 0)) || (in.booleanValue == true) ) { this.value_(spec.map(1.0)) } }
      { \toggle }  { if( ( in.isNumber && (in!= 0)) || (in.booleanValue == true) ) { this.value_(spec.map( 1 - this.input )) } }
      { \switch } { this.value_(spec.map(in)) }
      { \value } { this.value_(in) }
      ;
  }

  reset {
    value = spec.default;
    this.changed(\synch);
  }
  
  inc {
    value = spec.constrain(value + spec.step);
    internvalue = value;
    this.changed(\synch);
  }

  dec {
    value = spec.constrain(value - spec.step);
    internvalue = value;
    this.changed(\synch);
  }

  
  groupOffset { arg val;
  //  if (val != 0) {
      internvalue = internvalue + val;
      value = spec.constrain(internvalue);
      this.changed(\synch, \groupOffset);
  //  }
  
  }

  groupSet { arg val;
  //  if (val != 0) {
      value = spec.constrain(val);
      this.changed(\synch, \groupSet);
  //  }
  
  }

  groupInput { arg in;
    value = spec.constrain(spec.map( in ));
    this.changed(\synch, \groupSet);
  }


  
  touch {
  //  this.changed(\touch);
    this.changed(\synch);
  }
  
  input   { ^spec.unmap(value) }
  asInput   { | val | ^spec.unmap(val) }
  
// setting the ControlSpec
  spec_   { | s, v | 
        spec = s.asSpec; 
        spec.default = v ? spec.default;
        this.value_(v ? spec.default); 
  } 
  sp  { | default= 0, lo = 0, hi=0, step = 0, warp = 'lin' |
    this.spec = ControlSpec(lo,hi, warp, step, default);
  }

  db  { | default= 0, lo = -100, hi = 20, step = 1, warp = 'lin' |
    this.spec = ControlSpec(lo,hi, warp, step, default);
  }

// split turns a multi-valued CV into an array of single-valued CV's
  split {
    ^value.collect { |v| CV(spec, v) }
  }

// Stream and Pattern support
  next { ^value }
//  reset {}
  embedInStream { ^value.yield }
  
}


// Variante fuer Arrays, besonders fuer Menues:
MXSV : MXCV {
  var <items;
  
  *new {arg items, default;       
    ^super.new.items_(items,default);
  }

  items_ { | argItems, default = 0|
    //var index = 0;
    items = argItems ? [\nil];
    if (default.isNumber.not) { default = this.getIndex(default) };
    //super.sp(default, 0, items.size, 1, 'lin');
    spec = ControlSpec(0,items.size, 'lin', 1, default);

    this.changed(\items);
  }
  
  item { ^items[value] }

  item_ { | symbol |
    this.value = this.getIndex(symbol);
  }

  addItem { arg x;
    this.items_( items ++ x );
  }

  removeItem { arg x;
    //this.items_( items.reject {arg i;  i == x} );
    this.items_( items.removing(x) );
  }

  inc {
    value = (value + 1).clip(0, items.size - 1);
    //value = array[index];
    this.changed(\synch);
  }

  dec {
    value = (value - 1).clip(0, items.size - 1);
    //value = array[index];
    this.changed(\synch);
  }


  
  getIndex { | symbol |
    items.do { | it, i| if (symbol == it) { ^i } };
    ^0
  }
  
  sp { | default = 0, symbols| this.items_(symbols, default) }
  
  next { ^items[value] }

}


/*

// Variante fuer Symbols:
MXCS : Stream {
  var <value, <index, <>array;
  
  *new { arg array, index;      
    ^super.new.initAMXCS(array, index);
  }
  
  initAMXCS { arg argarray, argindex;
    array = argarray ? Array[];
    index = argindex ? 0;
    value = array[index];
  }

  setArray { arg argarray;
    array = argarray
    index = 0;
    this.value_(array[index]);
  }

  action_ { | function, what=\synch | ^SimpleController(this) .put(what, function) }

// reading and writing the CV
  value_ { | val |  
    if(array.includes(val.asSymbol)) {
      index = array.indexOf(val.asSymbol);
      value = array[index];
      this.changed(\synch);
    };  
  } 
  
  input_  { | in, type=\abs | 
    if(in.isInteger) {
      index = in.clip(0, array.size - 1);
      value = array[index];
      this.changed(\synch);
    };
  /*
    switch(type)
      { \abs } { this.value_(spec.map( in) ) }
      { \rel } { this.value_(spec.map( this.input + in )) }
      { \trigger } { if( in || ( in.isNumber && (in!= 0)) ) { this.value_(spec.map(1.0)) } }
      { \toggle }  { if( in || ( in.isNumber && (in!= 0)) ) { this.value_(spec.map( 1 - this.input )) } }
      { \switch } { this.value_(spec.map(in)) }
      ;
  */    
  }

  
  inc {
    index = (index + 1).clip(0, array.size - 1);
    value = array[index];
    this.changed(\synch);
  }

  dec {
    index = (index - 1).clip(0, array.size - 1);
    value = array[index];
    this.changed(\synch);
  }

  input   { ^index }
//  asInput   { | val | ^spec.unmap(val) }
  

// Stream and Pattern support
  next { ^value }
  reset {}
  embedInStream { ^value.yield }

  
}

*/

