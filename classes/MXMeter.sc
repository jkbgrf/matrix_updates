/*
to do:

- MXFFT and MXRTA should only be active if at least one device uses them (Button "Analyser" on)
- for both fft and rta:  max peak per band / component , button: manual reset  , toggle: in/visible



*/



MXSimpleMeter {
/*  
simple LED metering for devices
- indicates RMS ( RMS > PeakFollower )
- defines synthdef, osc-receiver and GUI view (?) 
  
*/  
  classvar <defname = "MXSimpleMeter";
  classvar <idbase; 
  classvar <>idcount = 0;

  var <device;      // Parent MXDevice
  var <target;      // group of parent MXDevice
  var <synth;
  var <busArray;    // Array of busnums
  var <inputs;      // Array of private MXBusses
  var <inputNums;   // Array of bus numbers
  var <id;
  var <oscpath;   // Symbol
  var <responder;
  var active;       // MXCV: [0, 1]   
  var <rms;     // MXCV [dB]

  *initClass {  
    Class.initClassTree(MXGlobals);
    idbase = MXGlobals.simplemeterIDbase;
  } 
  
  *new { arg device, busArray;
    ^super.new.init(device, busArray);
  }
  
  init { arg argdevice, argbusArray;
    device = argdevice;
    busArray = argbusArray;
    inputs = busArray;
    inputNums = inputs.collect(_.busNum);
    
    MXSimpleMeter.idcount = MXSimpleMeter.idcount + 1;
    id = MXSimpleMeter.idbase + MXSimpleMeter.idcount;
    rms = MXCV( ControlSpec(-90, -12, \lin, 0), -90);  // Display max: -12 dB 
    oscpath = ("/simplemeter" ++ id.asString).asSymbol;
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  
    active.action = { arg changer, what; 
      if ( changer.value == 1 ) { 
        this.makeresponder;
        this.makesynth;
       } { 
        this.removeresponder;
        this.removesynth; 
      };
    };
  } 
  
  remove {
    this.active_(0);
  }   

  makeresponder {
    // set rms.value
    responder = OSCFunc({ arg msg, time, addr, recvPort;
      rms.value = msg[3];
    }, oscpath, MXGlobals.server.addr );
    // update GUI-View ???
  } 
  
  removeresponder {
    responder.free; // responder.disable ??
  } 
  
  makesynth {
    var decay;
    decay = MXGlobals.simplemeterdecay.neg.dbamp ** MXGlobals.server.sampleRate.reciprocal;
    synth = SynthDef(defname ++ id.asString, { 
      var sig, trig, rms;
      sig = In.ar(inputNums);
      sig = sig.sum;
      rms = RunningSum.rms(sig, MXGlobals.simplemeterrms);
      trig = Impulse.kr(MXGlobals.simplemeterrate);
      rms = PeakFollower.ar(rms, decay).clip(-90.dbamp, 1.0).ampdb;
      SendReply.kr(trig, oscpath, rms, id);
    }).play(device.group, [ ], \addToTail );
  } 
  
  removesynth {
    synth.free;
  } 

  active_ { arg value;
    if (value != active.value) { active.value = value };
  //  active.value = value;   
  }
  
  active {
    ^active.value;    
  }
    
} 




/*
DIN IEC 60268-18    Digitaler Aussteuerungsmesser

Bezugspegel: 0 dBfs
Integrationszeit: <= 1ms
Rcklaufzeit: 1.7 s von 0 bis -20 dB  (0 .. -60 db -> 5.1 s  == 12 db/s ??
*/
MXMeterBar {  // View

  var <level, <clipstate, <peakvalue, <>maxpeak = -90.0;
  var <>barcolor, <>background, <>peakcolor, <>clipcolor, <>clipsize = 5, <clipgap=2;
  var <>dbrange = 60, <>color6db, <>color20db, <>colorfloor;
  var <>backcolor6db, <>backcolor20db, <>backcolorfloor;
  var <y6db, <y14db, <y20db, <yfloor;
  var <h6db, <h14db, <h20db, <hfloor;
  var <bar, <clip, <>clipbounds, <>barbounds, <bview, barwidth, barleft, barheight, barbottom;
  var <>keyDownAction, <>clipFunc, <>action;
  var pen, pad;

  *new { arg parent, bounds; 
    ^super.new.initBar(parent, bounds);
  }

  initBar { arg parent, bounds;

    barcolor = Color.green;
    peakcolor = Color.yellow;
    background = Color.black;
    color6db = Color(0.85, 0.15, 0, 1.0); // 0 ... -6db  red-orange
    color20db = Color.yellow(0.75, 0.6);  // Color(1.0, 1.0, 0);// -6 ... -20db  yellow
    colorfloor = Color.green(0.75, 0.4);  // Color(0.0, 1.0, 0);// -20 ... -inf db  green
    backcolor6db = color6db.copy.alpha_(0.1).scaleByAlpha;    backcolor20db = color20db.copy.alpha_(0.1).scaleByAlpha;    backcolorfloor = colorfloor.copy.alpha_(0.1).scaleByAlpha;   
    clipcolor = Color.red;

    pen = SCPen;
    pad = if( GUI.id === \cocoa, 0, 0.5 );

    clipstate = false;
  //  clipbounds = Rect(bounds.left, bounds.top, bounds.width, clipsize);
  //  barbounds = Rect(bounds.left, bounds.top + clipsize + 2, bounds.width, bounds.height - clipsize);
    level = 0.0;

    bview = SCCompositeView(parent, bounds);
  //  bview.decorator = FlowLayout(bview.bounds, margin: Point(0,0), gap: Point(0, clipgap));

    clip = SCUserView(bview, Rect(0,0, bview.bounds.width, clipsize)) //(parent, clipbounds)
      .resize_(1)
      .clearOnRefresh_(false)
      .canFocus_(false)
      .mouseDownAction_({arg v, x, y, modifiers;
        // clipFunc.value(v);
      })
      .drawFunc_({ arg view;
        pen.width = 1;
        pen.color = clipstate.if(clipcolor, Color.red(0.2)); 
        pen.fillRect(view.bounds);
      //  pen.stroke;
      }); 

  //  bview.decorator.nextLine; 
    
    barwidth = bview.bounds.width;
    barleft = bview.bounds.left;
    barheight = bview.bounds.height - clipsize - clipgap;
    barbottom = bview.bounds.bottom;
    
    // y in Pixel vom oberen Rand
    y6db = 6/dbrange * barheight;
    y14db = 14/dbrange * barheight;
    y20db = 20/dbrange * barheight;
    yfloor = (1 - (20/dbrange)) * barheight;
    
    // height in in Pixel fuer Balkenabschnitt
    h6db = 6/dbrange * barheight;
    h14db = 14/dbrange * barheight;
    h20db = 20/dbrange * barheight;
    hfloor = (1 - (20/dbrange)) * barheight;
      
  //  bar = SCUserView(bview, Rect(0,0, barwidth, barheight )) //(parent, barbounds)
  //  bar = SCUserView(bview, Rect(0, clipsize + clipgap, barwidth, barheight )) //(parent, barbounds)
    bar = SCUserView(bview, Rect(4, clipsize + clipgap, barwidth - 8, barheight )) //(parent, barbounds)
      .resize_(1)
      .clearOnRefresh_(false)
      .canFocus_(false)
      .keyDownAction_({arg me, key, modifiers, unicode;
        keyDownAction.value(key, modifiers, unicode);
      })
      .mouseDownAction_({arg v, x, y, modifiers;
        // clipFunc.value(v);
      })
      .drawFunc_({ arg view;
        var height, width, smallwidth, left=4;
        width = view.bounds.width;
        smallwidth = width - (2*left);
        pen.width = 1;
      //  pen.fillColor = background;
      //  pen.fillRect(view.bounds.moveTo(0,0) ); 
        
      // dB color scale:  
        pen.fillColor = backcolor6db;
  //      pen.fillRect(Rect(2, view.bounds.top, barwidth - 4, h6db));
        pen.fillRect(Rect(0, 0, width, h6db));
        pen.fillColor = backcolor20db;
        pen.fillRect(Rect(0, y6db, width, h14db));
        pen.fillColor = backcolorfloor;
        pen.fillRect(Rect(0, y20db, width, hfloor));

      // RMS bar:       
        pen.fillColor = barcolor;
        height = barheight * level;
  //      pen.fillRect(Rect.new(barbounds.left, barbounds.bottom - height, barbounds.width, height));
        if(height > (barheight - h6db)) { 
          pen.fillColor = color6db;
  //        pen.fillRect(Rect(4, view.bounds.bottom - height, barwidth - 8, height - hfloor - h14db));
          pen.fillRect(Rect(left, barheight - height, smallwidth, height - hfloor - h14db));
        };  
        if(height > (barheight - h20db)) { 
          pen.fillColor = color20db;
          pen.fillRect(Rect(left, barheight - height.clip(0, hfloor + h14db), smallwidth, (height- hfloor).clip(0, h14db)));
        };
        if(height > 0) { 
          pen.fillColor = colorfloor;
          pen.fillRect(Rect(left, barheight - height.clip(0, hfloor), smallwidth, height.clip(0, hfloor)));
        };  
        
      // Peak Indicator:  
        pen.fillColor = peakcolor;
        height = barheight * peakvalue;
        pen.fillRect(Rect(0, barheight - height, width, 3));
      //  pen.stroke;
      }); 
  } 

  setLevels {arg val, peak;
    val = val.clip(0, 1);
    peak = peak.clip(0, 1);
    if ( (val != level) || (peak != peakvalue) ) {
      level = val;
      peakvalue = peak;
      bar.refresh;
    };    
  } 
    
  level_ { arg val;
    this.value_(val);
  //  level = val.clip(0.0, 1.0);
  //  bar.refresh;
    //^level;
  }

  value_ { arg val;
    val = val.clip(0.0, 1.0);
    if (val != level ) { 
      level = val;
      bar.refresh;
    };
  }

  peakLevel_ { arg val;
    val = val.clip(0.0, 1.0);
    if ( val != peakvalue ) {Ê
      peakvalue = val;
      bar.refresh;
    };
  }


/*  
  valueAction_ {arg val;
    level = val.clip(0.0, 1.0);
    action.value(this);
    bar.refresh;
    //clip.refresh;
    ^level
  }
*/
  clipstate_ {arg state;
    clipstate = state;
    clip.refresh;
  } 

  canFocus_ { arg state = false;
    clip.canFocus_(state);
    ^this
  }

  canFocus {
    ^clip.canFocus;
  }

  visible_ { arg bool;
    bar.visible_(bool);
    clip.visible_(bool);
  }

  visible {
    //^bar.visible;
    //^clip.visible;
  }
  
  enabled_{ arg bool;
    bar.enabled_(bool);
    clip.enabled_(bool);
  }
  
  enabled {
    //^bar.enabled;
    //^clip.enabled;
  }
  
  refresh {
    bar.refresh;
    clip.refresh;
    ^this
  }

}  // --------------- MXMeterBar


MXAudioMeterView {
  // 16 meter bars incl. clip LEDS
  // 16 maxPeak stringviews
  // dB scale markers
  
  classvar <>idCount = 0, <id; 
  
  var <size;
  var <>onClose;
  var <view, <parent, <bounds, <meter, <peakval, <label, <peakheight;
  var <peakString, <channelString;
  var <dbmax = 0.0, <dbmin = -60.0, <dbrange, <decay=60, <rate=20;
  var <barwidth, <gap=10, <width, <numberheight =20;
  var <barsbounds;
 
  *new { arg parent, bounds, size=16;
    ^super.new.initAudioMeter(parent, bounds, size);
  } 

  initAudioMeter { arg argparent, argbounds, argsize;
    parent = argparent;
    bounds = argbounds ? parent.bounds;
    bounds = bounds.moveTo(0,0);
    size = argsize;
    width = bounds.width;
  //  barwidth = ((width - (gap*(size-1))) / size).max(1);
    barwidth = ((width - (gap*(size+2-1))) / (size+2)).max(1);
    dbrange = dbmax - dbmin;

    view = SCCompositeView(parent, bounds);
    view.background = Color.grey(0.1);
    
  //  barsbounds = bounds.top_(bounds.top + (2 * numberheight)).height_(bound.height - (2 * numberheight));

    channelString = Array.fill( size, { arg i; 
      SCStaticText(view, Rect( (barwidth+gap) * (i+1), bounds.height - numberheight + 3, barwidth, numberheight) )
        .background_(Color.clear)
        .font_(Font("Arial", 11))
        .stringColor_(MXGUI.rulerColor)
        .align_(\center)
        .string_((i+1).asString)
      });

    // db scale underlays and reset "button"    
    // left ruler:
    SCUserView(view, Rect(0, 0, barwidth+gap, bounds.height))  
      .canFocus_(false)
      .clearOnRefresh_(false)
      .mouseDownAction_({arg v, x, y, modifiers;
        this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var db3, db10, dbh, sh = (1*numberheight) + meter[0].clipsize + meter[0].clipgap;
        var h = 16;
        dbh = (bounds.height - sh - numberheight) / dbrange;
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        [0, 6, 12, 20, 30, 40, 50, 60].do({ arg db, i;
          var y = db*dbh+sh;
          SCPen.line(barwidth @ y, (barwidth + gap - 2) @ y); 
          SCPen.stringRightJustIn(db.neg.asString, Rect(0, y-(0.5*h), (barwidth-2), h));
          });
        SCPen.stringRightJustIn("d B", Rect(0, 0, barwidth, h));
        SCPen.stroke;
      }).refresh;
        
    // right ruler
    SCUserView(view, Rect(bounds.width - barwidth - gap, 0, barwidth+gap, bounds.height))  
      .canFocus_(false)
      .clearOnRefresh_(false)
      .mouseDownAction_({arg v, x, y, modifiers;
        this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var db3, db10, dbh, sh = (1*numberheight) + meter[0].clipsize + meter[0].clipgap;
        var h = 16;
        dbh = (bounds.height - sh - numberheight) / dbrange;
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        [0, 6, 12, 20, 30, 40, 50, 60].do({ arg db, i;
          var y = db*dbh+sh;
          SCPen.line(0 @ y, (gap - 2) @ y); 
          SCPen.stringLeftJustIn(db.neg.asString, Rect(gap, y-(0.5*h), barwidth, h));
          });
        SCPen.stringLeftJustIn("d B", Rect(gap, 0, barwidth, h));
        SCPen.stroke;
      }).refresh; 

    peakString = Array.fill( size, { arg i; 
      SCStaticText(view, Rect( (barwidth+gap) * (i+1), 0, barwidth, numberheight) )
      //  .background_(Color.grey(0.15))
  //      .background_(MXGUI.levelBackColor)
        .background_(Color.grey(0.1))
        .font_(Font("Arial Narrow", 11))
        .stringColor_(MXGUI.levelColor)
        .align_(\center)
        .string_("-inf")
        .mouseDownAction_({   
          this.resetPeaks;
        //  this.resetfunc.value;
        });
      });

    meter = size.collect( { arg i;
      var m;
      m = MXMeterBar(view, Rect( ((i+1)*(barwidth + gap)), 1 * numberheight, barwidth, bounds.height - (2*numberheight)));
    //  m.barcolor = Color.cyan(1.0).alpha_(0.5);
    //  m.peakcolor = Color.white.alpha_(0.65); // Color.cyan(1.0).alpha_(1.0);
      m.barcolor = Color.cyan(0.5);
      m.peakcolor = Color.grey(0.65); // Color.cyan(1.0).alpha_(1.0);
      m.background = Color.black;
      m.dbrange = dbrange;
      m.canFocus = false;
      m.level = 0.0;  
      m.peakLevel = 0.0;  
      m
    });
    this.resetPeaks;
  } 

  updateVals { arg i, level, peakLevel;
  //  meter[i].peakLevel = (peakLevel - dbmin) / dbrange;
  //  meter[i].level = (level - dbmin) / dbrange;
    meter[i].setLevels( (level - dbmin) / dbrange, (peakLevel - dbmin) / dbrange);
    if (peakLevel > meter[i].maxpeak) {
      this.setPeak(i, peakLevel);
      if(peakLevel > 0) { 
        this.updateClip(i, true);
      }
    };  
  } 

  updateLevel { arg i, dbvalue;
    meter[i].level = (dbvalue - dbmin) / dbrange;
  } 
  
  updateClip { arg i, value;
    meter[i].clipstate = value;
  } 

  setPeak { arg i, peakdb;
    meter[i].maxpeak = peakdb;
    peakString[i].string = peakdb.round(0.1).asString;
    peakString[i].refresh;
  }

  resetPeaks {
    size.do {arg i;  
      this.setPeak(i, -90);
      this.updateClip(i, false);
    };
  } 
    
  clear {
    meter.do { arg m; m.level = 0.0; m.clipstate = false };
    this.resetPeaks;
  }
  
  decay_ { arg value;
    decay = value;
    //if(synth.isPlaying) { synth.set(\decay, this.decayrate(decay);); };
  }
  
  decayrate { arg value;    // value is dB per second
    ^value.neg.dbamp ** MXGlobals.server.sampleRate.reciprocal;
    }
  
  rate_ { arg value;
    rate = value;
    //if(synth.isPlaying) { synth.set(\rate, rate); };  
  }

  dbmin_ { arg value;
    dbmin = value;
    dbrange = dbmax - dbmin;  
  }

  dbmax_ { arg value;
    dbmax = value;
    dbrange = dbmax - dbmin;  
  }
  
  remove {
    onClose.value;
    view.close;
  }
  
} // ---------------- MXAudioMeterView



MXMultiChannelMeter { // singleton ?
  // 16 channels mit eigenen Inputbussen!, verhaelt sich wie ein MXMonitorDevice ?
    
//  var meterrate = 30;  // Levelmeter refresh-rate
//  var metersmps, meterdt = 0.01; // 10ms Integrationszeit

  var <name = "LevelMeter"; // String or Symbol
  var <type = \levelMeter;
  var <numChannels = 16;  // Integer, number of input channels
  var <busArray;      // Array of (private) MXBusses
  var <inputs;        // Array of private MXBusses
  var <inputNums;     // Array of bus numbers
  var <active;        // MXCV: [0, 1]  
  var <group;       // Group (Node) for Synths
  var <synth;
  var <oscName = "/LevelMeter";   // used as cmdname for SendReply and path for OSCFunc
  var <responder;

  *new { arg name, type, numChannels;
    ^super.new.init(name, type, numChannels);
  }

  init { arg argname, argtype, argnumChannels;
    name = argname ? name;
    type = argtype ? type;
    numChannels = argnumChannels ? numChannels;
    
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI ??
    active.action = { arg changer, what;  
      if (changer.value == 1) {
        this.startDSP(MXMain.meterManager.group);
      } {
        this.stopDSP;
        { if (MXGUI.levelMeterView.view.notClosed) {
          MXGUI.levelMeterView.clear; 
        } }.defer(MXGlobals.switchrate + 0.02);
      };  
    };
  //  this.makeResponder;
  } 
  
  active_ { arg value;
    if (value != active.value) { active.value = value };  //  active.value = value; 
  } 
    
  makeSynth {
  
    synth = SynthDef("MXLevelMeter", {  
      var in, imp, numRMSSamps;
    //  numRMSSamps = MXGlobals.meterrms; // s.sampleRate / rate;
      numRMSSamps = MXGlobals.meterrmstime * SampleRate.ir ; 
      in = In.ar( inputNums );
      imp = Impulse.ar(MXGlobals.meterrate);
      SendReply.ar(imp, oscName,
        // do the mean and sqrt clientside to save CPU
        [
          RunningSum.ar(in.squared, numRMSSamps).lag(0,1), // Normwert fuer Abklingzeit ??
          Peak.ar(in, Delay1.ar(imp)).lag(0, MXGlobals.meterpeaklag ) // .lag(0,3);
        ].flop.flat
      );
    }).play(group, nil, \addToHead) ; 
  }
  
  removeSynth {
    synth.free;
  }

  makeResponder {
    responder = OSCFunc({ arg msg, time, addr, recvPort;
      var numRMSSampsRecip;
  //    numRMSSampsRecip = meterdt; // meterrate / s.sampleRate;
  //    numRMSSampsRecip = MXGlobals.meterinterval;
      numRMSSampsRecip = (MXGlobals.meterrmstime * MXGlobals.server.sampleRate).reciprocal;
      msg.copyToEnd(3).pairsDo({|val, peak, i|
        var peakdb;
        i = i * 0.5;
        peakdb = peak.ampdb;
        { if (MXGUI.levelMeterView.view.notClosed) {
          // RMS muss +3db angehoben werden fuer QPPM:
          MXGUI.levelMeterView.updateVals(i, (val.max(0.0) * numRMSSampsRecip).sqrt.ampdb + 3, peakdb); 
        } }.defer;

      }) 
    }, oscName, MXGlobals.server.addr );

    responder.disable;
  }
  
  removeResponder {
    // ??
  }     

  setSR {
    busArray = numChannels.collect {arg i;  MXDeviceManager.getNewBus };
    inputs = busArray;
    inputNums = inputs.collect(_.busNum);
    this.makeResponder;
  }
  
  unsetSR {
    busArray.do(_.free);
    busArray.clear;
  }     

  startDSP { arg target; 
    group = Group(target);
    this.makeSynth;
  //  this.makeResponder;

    // + responder ?
    responder.enable;
  } 

  stopDSP {  // ??
    // remove group, synth, responder 
    this.removeSynth;
    responder.disable;
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
    
}


MXSonagram {
/*  
var fftbus = 0, fftask;
var fftbuf, fftsize =  2048, sonapointer = 0, sonadata = 0.dup(256);


  gui.sonawin = Window("FFT", Rect(1, 0, 890, 300), false, false ).front;
//  gui.sonawin.onClose = { t.stop; nodes.masterfft.free;  };

  gui.sonaview = UserView(gui.sonawin, gui.sonawin.view.bounds.extent).clearOnRefresh_(false);

  gui.sonaview.drawFunc = { arg view;
    Pen.fillColor = Color.grey(0.0); 
    Pen.fillRect(Rect((sonapointer + 1).wrap(0,view.bounds.width-1), 0, 1, 300));
    Pen.fillColor = Color.grey(0.25, 0.4); 
    Pen.fillRect(Rect((sonapointer + 2).wrap(0,view.bounds.width-1), 0, 5, 300));
    Pen.fillColor = Color.grey(0.25, 0.15); 
    Pen.fillRect(Rect((sonapointer + 7).wrap(0,view.bounds.width-1), 0, 15, 300));
    Pen.fillColor = Color.grey(0.25, 0.05); 
    Pen.fillRect(Rect((sonapointer + 22).wrap(0,view.bounds.width-1), 0, 30, 300));
    300.do {arg y;
//      Pen.fillColor = Color.grey(sonadata[y] ** 1.0 * 1.1);
      Pen.fillColor = Color.grey(1 - (sonadata[y] ** 1.0 * 1.1));
      Pen.fillRect(Rect(sonapointer, 300-y, 1, 1));
      };
    sonapointer = (sonapointer + 1).wrap(0, gui.sonawin.view.bounds.width - 1); 
    
    };
  
*/

// MXSonagram doesn't have it's own DSP, it uses MXFFT's DSP instead !

  var <name = "Sonagramm"; // String or Symbol
  var <type = \spectralDisplay;
  var <numChannels = 1; // Integer, number of input channels
  var <busArray;      // Array of (private) MXBusses
  var <inputs;      // Array of private MXBusses
  var <inputNums;   // Array of bus numbers
  var <active;        // MXCV: [0, 1]  
  var <group;       // Group (Node) for Synths
  var <synth;
//  var <responder;
  var <dbrange = 90;
  var <fmin, <fmax;     // MXCVs, freqmin and freqmax for display scaling
  var <dbmin, dbmax;    // MXCVs, dbmin and dbmax for display scaling
  var <colormode;     // MXCV: [0, 1]  0: white on black, 1: black on white
  var <rmsmode;     // MXCV: [0, 1]  1: overlay showing peak and rms
  var <>guiUpdateFunc;  // to be set and removed by MXGUI 
  var <view;        // parent view (Container)
  var <sonaView, <freqgrid, <frRangeSlider;
  var <sonapointer = 0, <sonadata;
  var <oldrms = 0, <oldpeak = 0;


  *new { arg name, type, numChannels;
    ^super.new.init(name, type, numChannels);
  }

  init { arg argname, argtype, argnumChannels;
    name = argname ? name;
    type = argtype ? type;
    numChannels = argnumChannels ? numChannels;
    
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI ??
    active.action = { arg changer, what;  
      if (changer.value == 1) {
        this.startDSP(MXMain.meterManager.group);
      } {
        this.stopDSP;
      };  
    };
    fmin = MXCV( ControlSpec(0, 1, \lin, 0), 0);   
    fmin.action = { arg changer; freqgrid.refresh; };
    fmax = MXCV( ControlSpec(0, 1, \lin, 0), 0.25);   
    fmax.action = { arg changer; freqgrid.refresh; };

  //  dbmin = MXCV( ControlSpec(0, 1, \lin, 0), 0);   
    dbmin = MXCV( ControlSpec(dbrange.neg, 0, \lin, 1), -50 );   
  //  dbmin.action = { arg changer; sonaView.refresh; dbgrid.refresh; };
  //  dbmax = MXCV( ControlSpec(0, 1, \lin, 0), 1);   
    dbmax = MXCV( ControlSpec(dbrange.neg, 0, \lin, 1), -6);   
  //  dbmax.action = { arg changer; sonaView.refresh; dbgrid.refresh; };

    colormode = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // 0: white on black, 1: black on white
    rmsmode = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // 0: no overlay, 1: overlay showing peak and rms
    
  } 

  active_ { arg value;
    if (value != active.value) { active.value = value };  //  active.value = value; 
  } 
  
  setSR {
  //  busArray = numChannels.collect {arg i;  MXDeviceManager.getNewBus };
  }
  
  unsetSR {
  //  busArray.do(_.free);
  //  busArray.clear;
  }     

  startDSP { arg target; 
  //  group = Group(target);
    // start synth
    // + responder ?
  } 

  stopDSP {  // ??
    // remove group, synth, responder 
  //  { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }

  makeView { arg parent; // , bounds;
    var bounds;
    var scalemargin, rangemargin;
    var resetbutton;
    var sonabounds;
    var colormodebutton, dbminnumber, dbmaxnumber, rmsbutton, clearbutton;
    
    view = parent; // SCCompositeView(parent, bounds);
  //  view.background = Color.grey(0.1);
    bounds = view.bounds.moveTo(0,0);
    scalemargin = 30 @ 30;
    rangemargin = 20 @ 20;
//    sonabounds = Rect(scalemargin.x, scalemargin.y, bounds.width - scalemargin.x - rangemargin.x, bounds.height - scalemargin.y - rangemargin.y);
    sonabounds = Rect(scalemargin.x, 5, bounds.width - scalemargin.x - rangemargin.x, bounds.height - rangemargin.y - 10);

    // right of fft 
    frRangeSlider = MXRangeSlider(view, Rect(sonabounds.right, sonabounds.top, rangemargin.x, sonabounds.height), 5)
      .inset_(4)
      .knobColor_(MXGUI.sliderKnobColor.alpha_(0.5))
      .background_(MXGUI.sliderBackColor.alpha_(0.25))
    //  .background_(Color.clear)
      .lo_(fmin.value)
      .hi_(fmax.value)
      .action_({ arg view;   fmin.input = view.lo; fmax.input = view.hi; })
      ;
    
    // below fft
    
    colormodebutton = MXButton(view, Rect(70, view.bounds.height - rangemargin.y, 50, rangemargin.y), 5)
      .states_([  
        ["b < w", Color.white, Color.black],  
        ["w < b", Color.black, Color.white]  ])
      .font_(MXGUI.setupButtonFont)
      ;

    colormodebutton.connect(colormode); 

    SCStaticText(view, Rect(130, view.bounds.height - rangemargin.y, 60, rangemargin.y))
        .string_("min:")
        .stringColor_(MXGUI.rulerColor)
        .align_(\right)
        .font_(Font("Arial", 11))
      ;
    dbminnumber = MXNumber(view, Rect(200, view.bounds.height - rangemargin.y, 50, rangemargin.y), 5)
      .shifty_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .font_(Font("Arial", 11))
      .unit_(" dB")
      .align_(\center)
      ;
      
    dbminnumber.connect(dbmin); 

    SCStaticText(view, Rect(260, view.bounds.height - rangemargin.y, 60, rangemargin.y))
        .string_("max:")
        .stringColor_(MXGUI.rulerColor)
        .align_(\right)
        .font_(Font("Arial", 11))
      ;
    dbmaxnumber = MXNumber(view, Rect(330, view.bounds.height - rangemargin.y, 50, rangemargin.y), 5)
      .shifty_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .font_(Font("Arial", 11))
      .unit_(" dB")
      .align_(\center)
      ;
      
    dbmaxnumber.connect(dbmax); 

    rmsbutton = MXButton(view, Rect(550, view.bounds.height - rangemargin.y, 50, rangemargin.y), 5)
      .states_([  
        ["RMS", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor],  
        ["RMS", MXGUI.setupButtonTextColor, MXGUI.plugColor]  ])
      .font_(MXGUI.setupButtonFont)
      ;

    rmsbutton.connect(rmsmode); 


    clearbutton = MXButton(view, Rect(700, view.bounds.height - rangemargin.y, 50, rangemargin.y), 5)
      .states_([  ["clear", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor] ])
      .font_(MXGUI.setupButtonFont)
      .action_({ arg view;  
        sonaView.background_( (colormode.value==0).if(Color.black, Color.white) );
        sonaView.clearDrawing;
        sonaView.refresh;
        sonapointer = 0;
        sonaView.clearOnRefresh = false;
      });

    // lower right corner
    resetbutton = MXButton(view, Rect(sonabounds.right, bounds.height - rangemargin.y , rangemargin.x, rangemargin.y))
      .states_([  ["¥", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor] ])
      .font_(MXGUI.setupButtonFont)
      .action_({ arg view;  
        frRangeSlider.setSpanActive(fmin.spec.default, fmax.spec.default); 
      //  dbRangeSlider.setSpanActive(0,1); 
        dbmin.reset;
        dbmax.reset;        
      });

  
    SCStaticText(view, Rect( 0, bounds.height - rangemargin.y, scalemargin.x - 2, rangemargin.y ))
        .string_("kHz")
        .stringColor_(MXGUI.rulerColor)
        .align_(\right)
        .font_(Font("Arial", 11))
      ;
  
    // left of sonaview
    freqgrid = SCUserView(view, Rect(0, sonabounds.top, scalemargin.x, sonabounds.height))  
      .canFocus_(false)
      .clearOnRefresh_(true)
      .mouseDownAction_({arg v, x, y, modifiers;
      //  this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var fs;
      //  var width = view.bounds.width - rangemargin.x;
        var height = view.bounds.height;
      //  var bottom = view.bounds.bottom;
        var width = view.bounds.width;
    //    var nkhz = MXGlobals.sampleRate * 0.5 * 0.001;
        var nkhz = MXGlobals.sampleRate * 0.5 * 0.001;
        var freqs = ( 0 .. (nkhz.floor - 1) );
    //    var freqstep = width / nkhz;
        var freqstep = height / (nkhz * (fmax.value - fmin.value));
        var freqsh = (nkhz * fmin.value).neg;
        var h = 14;
        var indent = width * 0.3;
      //  var color = Color.grey(0.2); 
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        SCPen.strokeColor = MXGUI.rulerColor; 
        //  h = 18;
        freqs.do({ arg f, i;
        //  var x = f * freqstep;
          var y = (f + freqsh) * freqstep;
          var y2 = (f + 0.5 + freqsh) * freqstep;
          y = height - y;
          y2 = height - y2;
        //  var h = 18;
          fs = f.asString;
          if ( (y >= 0) && (y <= height)) {
          //  SCPen.line(x @ (h+2), x @ bottom ); 
            SCPen.line(0 @ y, width @ y ); 
          //  if (f > 0) { SCPen.stringCenteredIn(f.asString, Rect(0, y - h, 40, h)) };
            if (f >= 0) { SCPen.stringRightJustIn(f.asString, Rect(0, y - h, width, h)) };
          };  
          if ( (y2 > 0) && (y2 < height )) {
        //    SCPen.line(x2 @ (h+6), x2 @ bottom ); 
            if (freqstep > 50) {
              SCPen.line(indent  @ y2, width @ y2 ); 
          //    SCPen.stringCenteredIn( fs ++ ".5", Rect(0, y2 - h, 40, h));
              SCPen.stringRightJustIn( fs ++ ".5", Rect(0, y2 - h, width, h));
            };
          };
          if ( freqstep > 200) {
            4.do {arg i;  
              y2 = (f + (0.1 * (i+1)) + freqsh) * freqstep;             y2 = height - y2;
              if ( (y2 > 0) && (y2 < height )) { 
                SCPen.line(indent @ y2, width @ y2 ); 
          //      SCPen.stringCenteredIn( fs ++ "." ++ (i+1), Rect(0, y2 - h, 40, h));
                SCPen.stringRightJustIn( fs ++ "." ++ (i+1), Rect(0, y2 - h, width, h));
              };
              y2 = (f + (0.1 * (i + 6)) + freqsh) * freqstep;             y2 = height - y2;
              if ( (y2 > 0) && (y2 < height )) { 
                SCPen.line(indent @ y2, width @ y2 ); 
              //  SCPen.stringCenteredIn( fs ++ "." ++ (i+6), Rect(0, y2 - h, 40, h));
                SCPen.stringRightJustIn( fs ++ "." ++ (i+1), Rect(0, y2 - h, width, h));
              };
            };
          }
        });
  //      SCPen.stringCenteredIn("kHz", Rect(view.bounds.width - 20, 0, 20, h));
  //      SCPen.stringCenteredIn("kHz", Rect(0, view.bounds.height, scalemargin.x, h));
      SCPen.stroke;
      }).refresh; 

    sonaView = SCUserView(view, sonabounds)
      .clearOnRefresh_(false)
      .background_( (colormode.value==0).if({Color.black}, {Color.white}) )
      ;
    
    sonaView.drawFunc = { arg view;
      var xs, ys, yb, px;
      var rms, peak;
      yb = view.bounds.height;
      ys = yb / sonadata.size;
      xs = 1;
      px = (sonapointer + 1).wrap(0, view.bounds.width - 1);
  
      // time cursor line:
      SCPen.strokeColor = Color(0.9, 0.5, 0, 1);
    //  SCPen.color = Color(0.9, 0.5, 0, 1);
      SCPen.width = 1;
      SCPen.line( Point(px, 0), Point(px, yb));
      SCPen.stroke;
    
      if (colormode.value == 0) { 
        sonadata.reverseDo {arg mag, y;
        //  Pen.fillColor = Color.grey(1 - (mag ** 1.0 * 1.1));
        //  SCPen.fillColor = Color.grey(1 - mag);
          SCPen.fillColor = Color.grey(mag);
          SCPen.fillRect(Rect(sonapointer, y*ys, xs, ys + 0.5));
          };
      } {
        sonadata.reverseDo {arg mag, y;
        //  Pen.fillColor = Color.grey(1 - (mag ** 1.0 * 1.1));
          SCPen.fillColor = Color.grey(1 - mag);
        //  SCPen.fillColor = Color.grey(mag);
          SCPen.fillRect(Rect(sonapointer, y*ys, xs, ys + 0.5));
          };
      };
      
      if (rmsmode.value > 0) { 
        SCPen.width = 2;
        
      //  rms = MXMeterManager.fft.rmsvalue.clip(0, 1).ampdb.linlin(-90, 0, 0, 0.99);
        rms = MXMeterManager.fft.rmsvalue.clip(0, 0.99);
        SCPen.strokeColor = Color.blue(1, 0.6);
      //  SCPen.line( Point(px,  yb * (1 - rms)), Point(px, yb * (1 - rms) - 2) );
        SCPen.line( Point(px - 1,  yb * (1 - oldrms)), Point(px, yb * (1 - rms)) );
        SCPen.stroke;
  
      //  peak = MXMeterManager.fft.peakvalue.clip(0, 1).ampdb.linlin(-90, 0, 0, 0.99);
        peak = MXMeterManager.fft.peakvalue.clip(0, 0.999);
        SCPen.strokeColor = Color.red(1, 0.6);
      //  SCPen.line( Point(px, yb * (1 - peak)), Point(px, yb * (1 - peak) - 2) );
        SCPen.line( Point(px - 1, yb * (1 - oldpeak)), Point(px, yb * (1 - peak)) );
        SCPen.stroke;
          
        oldrms = rms;
        oldpeak = peak; 
      };
      sonapointer = px; 
    };

    guiUpdateFunc = { arg array; 
      var size, min, max;
      { 
      if (sonaView.notClosed) { 
        size = array.size;
        min = dbmin.value;
        max = dbmax.value;
        sonadata = array[ (fmin.value * size).asInteger .. (fmax.value * size).asInteger ].linlin(min, max, 0, 1);
        sonaView.refresh;
      } }.defer;
    };
  }
  
  removeView {
    guiUpdateFunc = { };
    
  }

}

MXFFT {
  var <name = "FFT";    // String or Symbol
  var <type = \spectralDisplay;
  var <numChannels = 1; // Integer, number of input channels
  var <busArray;      // Array of (private) MXBusses
  var <inputs;        // Array of either MXIOs or private MXBusses
  var <inputNums;     // Array of bus numbers
  var <active;        // MXCV: [0, 1]  
  var <group;       // Group (Node) for Synths
  var <synth;
  var <fftbuf;  
  var <rmschannels = 2; // number of rms signals: 2 [rms, peak]
  var <rmsbus;        // control Bus for sending rms signals
  var <rmssmps;     // Integer, number of samples for RMS mean
  var <task;
  var <oscName = "/fft";  // used as cmdname for SendReply and path for OSCFunc
  var <dbrange = 90;
  var <fmin, <fmax;     // MXCVs, freqmin and freqmax for display scaling
  var <dbmin, dbmax;    // MXCVs, dbmin and dbmax for display scaling
  var <>guiUpdateFunc;  // to be set and removed by MXGUI 
  var <view;
  var <fftView, <dbgrid, <freqgrid, <frRangeSlider, <dbRangeSlider;
  var <rmsvalue=0, <peakvalue=0;
  

  *new { arg name, type, numChannels;
    ^super.new.init(name, type, numChannels);
  }

  init { arg argname, argtype, argnumChannels;
    name = argname ? name;
    type = argtype ? type;
    numChannels = argnumChannels ? numChannels;
    
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI ??
    active.action = { arg changer, what;  
      if (changer.value == 1) {
        this.startDSP(MXMain.meterManager.group);
      } {
        this.stopDSP;
      };  
    };
    
    fmin = MXCV( ControlSpec(0, 1, \lin, 0), 0);   
    fmin.action = { arg changer; fftView.refresh; freqgrid.refresh; };
    fmax = MXCV( ControlSpec(0, 1, \lin, 0), 1);   
    fmax.action = { arg changer; fftView.refresh; freqgrid.refresh; };

    dbmin = MXCV( ControlSpec(0, 1, \lin, 0), 0);   
    dbmin.action = { arg changer; fftView.refresh; dbgrid.refresh; };
    dbmax = MXCV( ControlSpec(0, 1, \lin, 0), 1);   
    dbmax.action = { arg changer; fftView.refresh; dbgrid.refresh; };

  } 
  
  active_ { arg value;
    if (value != active.value) { active.value = value };  //  active.value = value; 
  } 

  makeSynth {
    synth = SynthDef("MXFFTMeter", { arg buf;
      var in;
      var numRMSSamps, rms, peak;
      in = In.ar( inputNums );
      in = in.sum;
      FFT(buf, in, hop: 0.5, wintype: 1);
      numRMSSamps = MXGlobals.fftdisplayinterval * SampleRate.ir; 
      in = DelayN.ar(in, 0.1, (MXGlobals.fftsize - numRMSSamps) * SampleDur.ir);
      rms = RunningSum.rms(in, numRMSSamps);
      peak =  Peak.ar(in, Impulse.ar( MXGlobals.fftdisplayinterval.reciprocal) );
      Out.kr(rmsbus, [ rms, peak ])
    }).play(group, [\buf, fftbuf.bufnum], \addToHead) ; }
  
  removeSynth {
    synth.free;
  }

  makeTask {
    // je eine Funktion fuer FFTView und SonagrammView einrichten und mit skalierten fftbuf Werten aufrufen
    // diese Funktion von den Views aus setzen bzw. bei onClose() wieder entfernen
    
    task = Task( { 
    //  var magscale = (MXGlobals.fftsize.sqrt * 2).reciprocal.ampdb; // 520.reciprocal.ampdb
      var magscale = (MXGlobals.fftsize.sqrt * 4).reciprocal.ampdb; // 520.reciprocal.ampdb
    //  var dbmin = dbrange.neg, dbmax = 0;
      loop { 
        rmsbus.getn(rmschannels, { arg vals;
          rmsvalue = vals[0];
          peakvalue = vals[1];              });
        if ( MXGlobals.fftsize <= 1024) {
          fftbuf.getn(0, MXGlobals.fftsize, { arg buf;
            var z, x, vals;
            z = buf.clump(2).flop;
            x = hypot(z[0], z[1]);
  //          vals = (x * 520.reciprocal).ampdb.clip(-90, 0).linlin(-70,0,0,1);
  //          vals = (x.ampdb + magscale).linlin(dbmin,dbmax,0,1);
            vals = (x.ampdb + magscale);
            guiUpdateFunc.value( vals );
            MXMeterManager.rta.guiUpdateFunc.value( vals[2 .. 303] );
          //  { gui.fftview.value = (x * 520.reciprocal).ampdb.clip(-90, 0).linlin(-70,0,0,1) }.defer;
          })
        } {   var vals;
          fftbuf.getn(0, 1024 , { arg buf;
            var z, x;
            z = buf.clump(2).flop;
            x = hypot(z[0], z[1]);
  //          vals = (x * 520.reciprocal).ampdb.clip(-90, 0).linlin(-70,0,0,1);
  //          vals = (x.ampdb + magscale).linlin(dbmin,dbmax,0,1);
            vals = (x.ampdb + magscale);
      //       MXMeterManager.sona.guiUpdateFunc.value( vals[2 .. 303] );
      //      { sonadata = vals[2 .. 223]; gui.sonaview.refresh; }.defer;
      //      { sonadata = vals[2 .. 303]; gui.sonaview.refresh; }.defer;
  
          fftbuf.getn(1024, 1024 , { arg buf;
            var z, x, vals2;
            var allvals;
            z = buf.clump(2).flop;
            x = hypot(z[0], z[1]);
        //    vals2 = (x * 520.reciprocal).ampdb.clip(-90, 0).linlin(-70,0,0,1);
        //    vals2 = (x.ampdb + magscale).linlin(dbmin,dbmax,0,1);
            vals2 = (x.ampdb + magscale);
            allvals = vals ++ vals2;
            guiUpdateFunc.value( allvals );
            MXMeterManager.sona.guiUpdateFunc.value( allvals );
      //    { gui.fftview.value = vals ++ vals2 }.defer;
            });
          })
        };  
        MXGlobals.fftdisplayinterval.wait;
      } 
    });
  }
  
  removeTask {
    task.stop;
  }     

  setSR {
    busArray = numChannels.collect {arg i;  MXDeviceManager.getNewBus };
    inputs = busArray;
    inputNums = inputs.collect(_.busNum);
    this.makeTask;
  }
  
  unsetSR {
    this.removeTask;
    busArray.do(_.free);
    busArray.clear;
  }     

  startDSP { arg target; 
    group = Group(target);
    fftbuf = Buffer.alloc(MXGlobals.server, MXGlobals.fftsize);
    rmsbus = Bus.control(MXGlobals.server, rmschannels);
    { this.makeSynth; task.start; }.defer(0.1);
    
    // start synth
    // + responder ?
  } 

  stopDSP {  // ??
    // remove group, synth, responder 
    this.removeSynth;
    task.stop;
    { group.free; fftbuf.free; rmsbus.free; }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  makeView { arg parent; // , bounds;
    var bounds;
    var scalemargin, rangemargin;
    var resetbutton;
    var fftbounds;
    
    view = parent; // SCCompositeView(parent, bounds);
  //  view.background = Color.grey(0.1);
    bounds = view.bounds.moveTo(0,0);
    scalemargin = 30 @ 30;
    rangemargin = 20 @ 20;
    fftbounds = Rect(scalemargin.x, scalemargin.y, bounds.width - scalemargin.x - rangemargin.x, bounds.height - scalemargin.y - rangemargin.y);

    // below fft 
    frRangeSlider = MXRangeSlider(view, Rect(scalemargin.x, fftbounds.bottom, fftbounds.width, rangemargin.y), 5)
      .inset_(4)
      .knobColor_(MXGUI.sliderKnobColor.alpha_(0.5))
      .background_(MXGUI.sliderBackColor.alpha_(0.25))
    //  .background_(Color.clear)
      .lo_(0)
      .hi_(1)
      .action_({ arg view;   fmin.input = view.lo; fmax.input = view.hi; })
      ;
    
    // right of fft
    dbRangeSlider = MXRangeSlider(view, Rect(fftbounds.right, scalemargin.y, rangemargin.x, fftbounds.height), 5)
      .inset_(4)
      .knobColor_(MXGUI.sliderKnobColor.alpha_(0.5))
      .background_(MXGUI.sliderBackColor.alpha_(0.25))
    //  .background_(Color.clear)
      .lo_(0)
      .hi_(1)
      .action_({ arg view;   dbmin.input = view.lo; dbmax.input = view.hi; })
      ;

    // lower right corner
    resetbutton = MXButton(view, Rect(fftbounds.right, fftbounds.bottom, rangemargin.x, rangemargin.y))
      .states_([  ["¥", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor] ])
      .font_(MXGUI.setupButtonFont)
      .action_({ arg view;  frRangeSlider.setSpanActive(0,1); dbRangeSlider.setSpanActive(0,1); });


    // left of fft
    dbgrid = SCUserView(view, Rect(0, scalemargin.y, scalemargin.x, fftbounds.height))  
      .canFocus_(false)
      .clearOnRefresh_(true)
      .mouseDownAction_({arg v, x, y, modifiers;
      //  this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var dbh, sh = 0;
        var h = 14;
    //    dbh = view.bounds.height / dbrange;
        dbh = view.bounds.height / (dbrange * (dbmax.value - dbmin.value));
        sh = (dbrange * (1 - dbmax.value)).neg;
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        (0, 10 .. 90).do({ arg db, i;
        //  var y = db*dbh+sh;
          var y = (db+sh)*dbh;
          SCPen.line(0 @ y, view.bounds.width @ y); 
          if (db > 0) { SCPen.stringRightJustIn("-" ++ db.asString, Rect(0, y-h, view.bounds.width, h)) };
          });
      //  SCPen.stringCenteredIn("dB", Rect(0, view.bounds.height, scalemargin.x, rangemargin.y));
        SCPen.stroke;
      }).refresh; 
  
    SCStaticText(view, Rect( 0, bounds.height - rangemargin.y, scalemargin.x - 2, rangemargin.y ))
        .string_("dB")
        .stringColor_(MXGUI.rulerColor)
        .align_(\right)
        .font_(Font("Arial", 11))
      ;
  
    // above fft
    freqgrid = SCUserView(view, Rect(scalemargin.x, 0, fftbounds.width + rangemargin.x, scalemargin.y))  
      .canFocus_(false)
      .clearOnRefresh_(true)
      .mouseDownAction_({arg v, x, y, modifiers;
      //  this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var fs;
        var width = view.bounds.width - rangemargin.x;
        var bottom = view.bounds.bottom;
    //    var nkhz = MXGlobals.sampleRate * 0.5 * 0.001;
        var nkhz = MXGlobals.sampleRate * 0.5 * 0.001;
        var freqs = ( 0 .. (nkhz.floor - 1) );
    //    var freqstep = width / nkhz;
        var freqstep = width / (nkhz * (fmax.value - fmin.value));
        var freqsh = (nkhz * fmin.value).neg;
        var h = 16;
      //  var color = Color.grey(0.2); 
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        //  h = 18;
        freqs.do({ arg f, i;
        //  var x = f * freqstep;
          var x = (f + freqsh) * freqstep;
          var x2 = (f + 0.5 + freqsh) * freqstep;
        //  var h = 18;
          fs = f.asString;
          if ( (x > 0) && (x < width)) {
            SCPen.line(x @ (h+2), x @ bottom ); 
            if (f > 0) { SCPen.stringCenteredIn(f.asString, Rect(x-20, 0, 40, h)) };
          };  
          if ( (x2 > 0) && (x2 < width )) {
            SCPen.line(x2 @ (h+6), x2 @ bottom ); 
            if (freqstep > 50) {
              SCPen.stringCenteredIn( fs ++ ".5", Rect(x2-20, 0, 40, h));
            };
          };
          if ( freqstep > 250) {
            4.do {arg i;  
              x2 = (f + (0.1 * (i+1)) + freqsh) * freqstep;             if ( (x2 > 0) && (x2 < width )) { 
                SCPen.line(x2 @ (h+6), x2 @ bottom ); 
                SCPen.stringCenteredIn( fs ++ "." ++ (i+1), Rect(x2-20, 0, 40, h));
              };
              x2 = (f + (0.1 * (i + 6)) + freqsh) * freqstep;             if ( (x2 > 0) && (x2 < width )) { 
                SCPen.line(x2 @ (h+6), x2 @ bottom ); 
                SCPen.stringCenteredIn( fs ++ "." ++ (i+6), Rect(x2-20, 0, 40, h));
              };
            };
          }
        });
  //      SCPen.stringCenteredIn("kHz", Rect(view.bounds.width - 20, 0, 20, h));
        SCPen.stringCenteredIn("kHz", Rect(view.bounds.width - rangemargin.x, 0, rangemargin.x, h));
        SCPen.stroke;
      }).refresh; 

    fftView = SCMultiSliderView(view, fftbounds ) 
      .readOnly_(true)
    //  .drawLines_(true)
      .drawRects_(true)
      .isFilled_(true)
      .indexThumbSize_(1) 
      .valueThumbSize_(1)
      .elasticMode_(1)
      .gap_(0)
      .background_(Color.grey(0.0))
      .colors_(Color(0.9, 0.5, 0, 0.7), Color(9, 0.5, 0, 0.7))
    //  .value_( {Ê0.01 } ! 12 )
      ;
      
    guiUpdateFunc = { arg array; 
      var size, min, max, lines;
      { 
      if (fftView.notClosed) { 
      //  if (array.sum > 0) {
          size = array.size;
          min = dbmin.value.linlin(0, 1, dbrange.neg, 0);
          max = dbmax.value.linlin(0, 1, dbrange.neg, 0);
          lines = array[ (fmin.value * size).asInteger .. (fmax.value * size).asInteger ].linlin(min, max, 0, 1);
          fftView.value = lines;
      //  }
      } }.defer;
    };
  }
  
  removeView {
    guiUpdateFunc = { };
    
  } 
  
}



MXRTA {
/*  
- 30 bandpass filters (1/3 octave)
- reference freq = 1000 Hz 
  (see DIN/IEC 61260:1995 + A1:2001, PDF page 33 "Tabelle A.1 Bandmittenfrequenzen fuer Oktav- und Terzfilter im Hoerbereich")
- bandpass filters are composed by one 10th-order L-R-lowpass and one 10th-order L-R-hipass (10th order = 60 db/octave)-
- per band: RMS (integration time = reply period ??), peak (5 seconds release time), max peak (until reset) 

*/  
  var <name = "RTA";    // String or Symbol
  var <type = \spectralDisplay;
  var <numChannels = 1; // Integer, number of input channels
//  var <busArray;      // Array of (private) MXBusses
//  var <inputs;        // Array of either MXIOs or private MXBusses
  var <inputNums;     // Array of bus numbers
  var <active;        // MXCV: [0, 1]  
  var <group;       // Group (Node) for Synths
  var <synth;
  var <bands = 30;      // number of rta bands, 30 = 1/3 octave (10 octaves)
  var <order = 5;     // (half) order of cascaded butterworth Lo- and Hipass-filters (Linkwitz-Riley-Filter)
  var <lofreqs, <hifreqs;   // Arrays of floats, for low-cut and hi-cut freqs (-3db freqs) for each band
  var <bandcenters;   // Array of Strings (rounded center freqs)
  var <rmssmps;     // Integer, number of samples for RMS mean
  var <responder;
  var <oscName = "/rta";  // used as cmdname for SendReply and path for OSCFunc
  var <dbrange = 90;
  var <dbmin, dbmax;    // MXCVs, dbmin and dbmax for display scaling
  var <>guiUpdateFunc;  // to be set and removed by MXGUI 
  var <view;
  var <rtaView, <dbgrid, <dbRangeSlider;
  var <rmsvalues, <peakvalues, <maxpeakvalues;
  

  *new { arg name, type, numChannels;
    ^super.new.init(name, type, numChannels);
  }

  init { arg argname, argtype, argnumChannels;
    name = argname ? name;
    type = argtype ? type;
    numChannels = argnumChannels ? numChannels;
    
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI ??
    active.action = { arg changer, what;  
      if (changer.value == 1) {
        this.startDSP(MXMain.meterManager.group);
      } {
        this.stopDSP;
      };  
    };
    
    // calculation of band freqs according to DIN 61260:1995:
    lofreqs = { arg x; 2.pow((x-16)/3) * 2.neg.midiratio *1000} ! bands;
    hifreqs = { arg x; 2.pow((x-16)/3) * 2.midiratio *1000} ! bands;
    hifreqs[bands-1] = 44100 / 2;  // limit highest lowcut to sr/2
    bandcenters = { arg x; 2.pow((x-16)/3) * 1000} ! bands;
    bandcenters = bandcenters.collect {arg fm; 
      var string;
      string =  fm.round(1).asString;
      if (fm >= 1000) { string = (fm * 0.001).round(0.1).asString ++ "k"; };
      string
    };
  
    rmsvalues = { 0 } ! bands;
    peakvalues = { 0 } ! bands;
    maxpeakvalues  = { 0 } ! bands;
  
    dbmin = MXCV( ControlSpec(0, 1, \lin, 0), (dbrange - 60)/dbrange);   
    dbmin.action = { arg changer; rtaView.refresh; dbgrid.refresh; };
    dbmax = MXCV( ControlSpec(0, 1, \lin, 0), 1);   
    dbmax.action = { arg changer; rtaView.refresh; dbgrid.refresh; };

  } 
  
  active_ { arg value;
    if (value != active.value) { active.value = value };  } 

  makeSynth {
    synth = SynthDef("MXRTAMeter", { 
      var in, sig;
      var numRMSSamps, rms, peak;
      in = In.ar( inputNums );
      in = in.sum;
//      sig = in ! bands;
      sig = bands.collect { arg i; LRHiCut.ar(LRLowCut.ar(in, lofreqs[i], order),  hifreqs[i], order) };
      SendPeakRMS.kr(sig, MXGlobals.meterrate, MXGlobals.meterpeaklag, oscName)   }).play(group, [ ], \addToHead) ; }
  
  removeSynth {
    synth.free;
  }


  makeResponder {
    responder = OSCFunc({ arg msg, time, addr, recvPort;
      var min, max, pk;
  //    var numRMSSampsRecip;
  //    numRMSSampsRecip = meterdt; // meterrate / s.sampleRate;
  //    numRMSSampsRecip = MXGlobals.meterinterval;
  //    numRMSSampsRecip = (MXGlobals.meterrmstime * MXGlobals.server.sampleRate).reciprocal;
  //    "Hi".postln;
  //    msg.postln;
      min = dbmin.value.linlin(0, 1, dbrange.neg, 0);
      max = dbmax.value.linlin(0, 1, dbrange.neg, 0);
      msg.copyToEnd(3).pairsDo({ arg peak, rms, i;
        i = i * 0.5;
        // RMS muss +3db angehoben werden fuer QPPM ??
        rmsvalues[i] = (rms.ampdb + 3).linlin(min, max, 0, 1);
        pk = peak.ampdb.linlin(min, max, 0, 1);
        peakvalues[i] = pk;
        if ( pk >  maxpeakvalues[i] ) { maxpeakvalues[i]  = pk };
        { if (rtaView.notClosed) { rtaView.refresh; } }.defer;
      }) 
    }, oscName, MXGlobals.server.addr );

    responder.disable;
  }
  
  removeResponder {
    // ??
  }     

  setSR {
  //  busArray = numChannels.collect {arg i;  MXDeviceManager.getNewBus };
  //  inputs = busArray;
  //  inputNums = inputs.collect(_.busNum);
    inputNums = MXMeterManager.fft.inputNums;
    this.makeResponder;
  }
  
  unsetSR {
  //  busArray.do(_.free);
  //  busArray.clear;
  }     

  startDSP { arg target; 
    group = Group(target);
  //  { this.makeSynth; task.start; }.defer(0.1);
    
    // start synth
    this.makeSynth;   
    // + responder ?
    responder.enable;
  } 

  stopDSP {  // ??
    // remove group, synth, responder 
    this.removeSynth;
    responder.disable;
    { group.free; }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  makeView { arg parent; // , bounds;
    var bounds;
    var scalemargin, rangemargin;
    var resetbutton, peakbutton;
    var rtabounds;
    
    view = parent; // SCCompositeView(parent, bounds);
  //  view.background = Color.grey(0.1);
    bounds = view.bounds.moveTo(0,0);
    scalemargin = 30 @ 30;
    rangemargin = 20 @ 20;
    rtabounds = Rect(scalemargin.x, scalemargin.y, bounds.width - scalemargin.x - rangemargin.x, bounds.height - scalemargin.y - rangemargin.y);

    // above rta
    peakbutton = MXButton(view, Rect(700, 5, 80, rangemargin.y), 5)
      .states_([  ["peak reset", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor] ])
      .font_(MXGUI.setupButtonFont)
      .action_({ arg view;  
        maxpeakvalues  = { 0 } ! bands;
        rtaView.refresh;
      });

    // below rta 
    bandcenters.do { arg freqstring, i;
      SCStaticText(view, Rect( scalemargin.x + (rtabounds.width / bands * i), bounds.height - rangemargin.y, rtabounds.width / bands, rangemargin.y ))
        .string_(freqstring)
        .stringColor_(MXGUI.rulerColor)
        .align_(\center)
        .font_(Font("Arial", 11))
      ;
    };
    
    // right of rta
    dbRangeSlider = MXRangeSlider(view, Rect(rtabounds.right, scalemargin.y, rangemargin.x, rtabounds.height), 5)
      .inset_(4)
      .knobColor_(MXGUI.sliderKnobColor.alpha_(0.5))
      .background_(MXGUI.sliderBackColor.alpha_(0.25))
    //  .background_(Color.clear)
      .lo_(0)
      .hi_(1)
      .action_({ arg view;   dbmin.input = view.lo; dbmax.input = view.hi; })
      ;

    // lower right corner
    resetbutton = MXButton(view, Rect(rtabounds.right, rtabounds.bottom, rangemargin.x, rangemargin.y))
      .states_([  ["¥", MXGUI.setupButtonTextColor, MXGUI.setupButtonBackColor] ])
      .font_(MXGUI.setupButtonFont)
      .action_({ arg view;  dbRangeSlider.setSpanActive(0,1); });


    // left of rta
    dbgrid = SCUserView(view, Rect(0, scalemargin.y, scalemargin.x, rtabounds.height))  
      .canFocus_(false)
      .clearOnRefresh_(true)
      .mouseDownAction_({arg v, x, y, modifiers;
      //  this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var dbh, sh = 0;
        var h = 14;
    //    dbh = view.bounds.height / dbrange;
        dbh = view.bounds.height / (dbrange * (dbmax.value - dbmin.value));
        sh = (dbrange * (1 - dbmax.value)).neg;
        SCPen.font = Font("Arial", 11);
        SCPen.width = 1;
        SCPen.color = MXGUI.rulerColor; 
        (0, 10 .. 90).do({ arg db, i;
        //  var y = db*dbh+sh;
          var y = (db+sh)*dbh;
          SCPen.line(0 @ y, view.bounds.width @ y); 
          if (db > 0) { SCPen.stringRightJustIn("-" ++ db.asString, Rect(0, y-h, view.bounds.width, h)) };
          });
      //  SCPen.stringCenteredIn("dB", Rect(0, view.bounds.height, scalemargin.x, rangemargin.y));
        SCPen.stroke;
      }).refresh; 
  
    SCStaticText(view, Rect( 0, bounds.height - rangemargin.y, scalemargin.x - 2, rangemargin.y ))
        .string_("dB")
        .stringColor_(MXGUI.rulerColor)
        .align_(\right)
        .font_(Font("Arial", 11))
      ;
    
    rtaView = SCUserView(view, rtabounds)  
      .background_(Color.grey(0.0))
      .canFocus_(false)
      .clearOnRefresh_(true)
      .mouseDownAction_({arg v, x, y, modifiers;
      //  this.resetPeaks; 
      })
      .drawFunc_({ arg view;
        var fs;
        var width = view.bounds.width; //  - rangemargin.x;
        var height = view.bounds.height ;
        var bottom = view.bounds.bottom;
        var fbandgap = 6;
        var fbandwidth = width - (bands + 1 * fbandgap) / bands;
        var barheight;
      //  SCPen.font = Font("Arial", 11);
        bands.do({ arg i;
          // RMS rect
          SCPen.color = Color(0.9, 0.5, 0, 0.7);
          barheight = rmsvalues[i] * height;
          SCPen.fillRect(Rect( i*fbandwidth + (i + 1 * fbandgap), height - barheight, fbandwidth, barheight));
          // peak rect
          SCPen.color = Color.grey(0.7);
          barheight = peakvalues[i] * height;
          SCPen.fillRect(Rect( i*fbandwidth + (i + 1 * fbandgap), height - barheight - 2, fbandwidth, 4));
          // maxpeak line
          SCPen.color = Color(0.9, 0.5, 0, 0.7);
          barheight = maxpeakvalues[i] * height;
          SCPen.fillRect(Rect( i*fbandwidth + (i + 1 * fbandgap), height - barheight - 1, fbandwidth, 2));
          });
      }).refresh; 

  }
  
  removeView {
    guiUpdateFunc = { };
    
  }
  
}



MXMeterManager { // singleton !
/*
- vergibt Inputbusse fr "meter devices"
- de/aktiviert Multichannelmeter und spectral displays
*/

  classvar <devices;    // List of displays
  classvar <group;
  classvar <multiMeter;   // MXMultiChannelMeter 
  classvar <rta;      // MXRTA
  classvar <fft;        // MXFFT
  classvar <sona;     // MXSonagram
  classvar <analyserRequests; // MXCV, number of devices requesting the analysers
  classvar <multiMeterRequests; // MXCV, number of devices requesting the multimeter

  *init {
    devices = List.new;
    multiMeter = MXMultiChannelMeter(numChannels: 16);
    rta = MXRTA.new;
    fft = MXFFT.new;
    sona = MXSonagram.new;

    devices.add(multiMeter);
    devices.add(fft);
    devices.add(sona);
    devices.add(rta);
    
    analyserRequests = MXCV( ControlSpec(0, 1000, \lin, 1), 0);   
    analyserRequests.action = { arg changer; 
      if (changer.value > 0) { 
        rta.active_(1);
        fft.active_(1);
        sona.active_(1);
      } {
        rta.active_(0);
        fft.active_(0);
        sona.active_(0);
      };
    };

    multiMeterRequests = MXCV( ControlSpec(0, 1000, \lin, 1), 0);   
    multiMeterRequests.action = { arg changer; 
      if (changer.value > 0) { 
        multiMeter.active_(1);
      } {
        multiMeter.active_(0);
      };
    };

    
  }

/*    
  *addDevice {arg device;
    devices.add(device);
  }
*/  

  *setSR {
    "MeterManager-setSR".postln;    
    devices.do {arg d; d.setSR };  
  }
  
  *unsetSR {
    devices.do {arg d; d.unsetSR };  
  }
    
  *startDSP { arg target; 
    "MonitorManager-startDSP".postln;
    group = target;
  //  devices.do {arg d;   d.active_(1) };
  } 

  *stopDSP {  // ??
    // remove group, devices
    devices.do {arg d;   d.active_(0) };   
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  *reset {
    
  } 
  
  *registerAnalyserRequests { arg bool;
    if (bool) { analyserRequests.inc } {ÊanalyserRequests.dec };
  }
  
  *registerMultiMeterRequests { arg bool;
    if (bool) { multiMeterRequests.inc } {ÊmultiMeterRequests.dec };
  }
  

}




