

MXStatusSpec : Spec {
  var <>default;
  var <clipLo, <clipHi;
  
  *new { arg default=false;
    ^super.newCopyArgs(default).init
  }
  storeArgs { ^[default] }
  init { 
    clipLo = 0.0;
    clipHi = 1.0;
  }
  constrain { arg value;
    if (value.isKindOf(Boolean)) 
      { ^value }
      { if ( value.isNumber ) 
        { if (value != 0) { ^true } }
      } 
    ^false
  }
  //range { ^maxval - minval }
  //ratio { ^maxval / minval }
  map { arg value;
    // maps a value from [0..1] to spec range
    ^this.constrain(value);
  }
  unmap { arg value;
    // maps a value from [false, true] to [0..1]
    ^value.binaryValue;
    }
}


MXFaderSpec : Spec {
  var <minval, <maxval, <>step, <>default, <>units;
  var <clipLo, <clipHi;
  
  *new { arg minval= -90.0, maxval=10.0, step=0.0, default=0.0, units="dB";
    ^super.newCopyArgs(minval, maxval, step, 
        default ? minval, units ? ""
      ).init
  }
  storeArgs { ^[minval,maxval,step,default,units] }
  init { 
    if(minval < maxval,{
      clipLo = minval;
      clipHi = maxval;
    }, {
      clipLo = maxval;
      clipHi = minval;
    }); 
  }
/*  
  minval_ { arg v;
    minval = v;
    this.init;  // rechoose the constrainfunc
  }
  maxval_ { arg v;
    maxval = v;
    this.init
  }
*/  
  constrain { arg value;
    ^value.asFloat.clip(clipLo, clipHi).round(step)
  }
  range { ^maxval - minval }
  ratio { ^maxval / minval }
  map { arg value;
    // maps a value from [0..1] to spec range
    //^warp.map(value.clip(0.0, 1.0)).round(step);    // 0.0 .. 0.2 -> -90 .. -30 , 0.2-1.0  - > +0.1 = +5dB  -> -30 .. +10 dB
    value = value.clip(0.0, 1.0);
    if( value >= 0.2 )      {Ê^(value*50 - 40) }
      { if( value >= 0.1 )    { ^(value*100 - 50) }
        { if( value >= 0.04 ) { ^(value*333.333 - 73.3) }
                  { ^(value*750 - 90) };
        }
      }
  }               

  unmap { arg value;
    // maps a value from spec range to [0..1]
    //^warp.unmap(value.round(step).clip(clipLo, clipHi));
    value = this.constrain(value);
    if ( value >= -30 )       { ^( value + 40 * 0.02) }
      { if( value >= -40 )    { ^( value + 50 * 0.01) }
        { if( value >= -60 )  { ^( value + 73.3 * 0.00300) }
                  { ^( value + 90 * 0.001333333) }
        }   
      }       
  }
  
} 
  
