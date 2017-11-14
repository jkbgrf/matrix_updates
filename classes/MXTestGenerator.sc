MXTestGenerator {
  
  var <busArray;    // Array of just one MXBus (output of the test synth!)
  var <outputs;
  var <outputNums;  // Array of bus number
  var <numChannels;
  var <group;     // Group (Node) for Synths
  var <mute;      // MXCV: [0, 1]
  var <level;     // MXCV: test level in dB  
  var <freq;      // MXCV: [1, sr/2] for sine osc
  var <cycletime;   // MXCV: [0.1, 10]
  var <cycle;     // MXCV: [0, 1]
  var <cycletask;   // Task, cycles over active test nodes
  var <modeSV;    // MXSV: [\mono, \cycle];
  var <signalSV;    // MXSV: [\sine, 'pink noise', 'white noise', \soundfile]
  var <path;      // path to soundfile
  var <buf;     // Buffer of soundfile  
  var <synth;
  var <>guifunc;    // Function to change Views according to active
  var <inMatrix, <outMatrix;   // access to matrices for cycle

  *initClass {    
    MXGlobals.synthdefs.add(
      SynthDef("test_sine", { arg out=1, freq=1000, amp=1, on=1, gate=1;        var sig;
        sig = SinOsc.ar(freq, 0, amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0.01, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_square", { arg out=1, freq=1000, amp=1, on=1, gate=1;        var sig;
        sig = Pulse.ar(freq, 0.5, amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0.01, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_saw", { arg out=1, freq=1000, amp=1, on=1, gate=1;       var sig;
        sig = Saw.ar(freq, amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0.01, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_impulse", { arg out=1, freq=1000, amp=1, on=1, gate=1;       var sig;
        sig = Impulse.ar(freq, 0, amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0.01, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_greynoise", { arg out=1, amp=1, on=1, gate=1;        var sig;
        sig = GrayNoise.ar(amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_clipnoise", { arg out=1, amp=1, on=1, gate=1;        var sig;
        sig = ClipNoise.ar(amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_brownnoise", { arg out=1, amp=1, on=1, gate=1;       var sig;
        sig = BrownNoise.ar(amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_pinknoise", { arg out=1, amp=1, on=1, gate=1;        var sig;
        sig = PinkNoise.ar(amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_whitenoise", { arg out=1, amp=1, on=1, gate=1;       var sig;
        sig = WhiteNoise.ar(amp);
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add(
      SynthDef("test_soundfile", { arg out=1, buf=0, amp=1, on=1, gate=1;       var sig;
        sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), loop: 1);
        sig = sig * amp;
        sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
  }

  *new { 
    ^super.new.init;
  }

  init { 

    // init global CVs
    mute = MXCV( ControlSpec(0, 1, \lin, 1), 0);   
    mute.action = {arg changer, what;
      this.setmute(changer.value);
    };
    
    level = MXCV( ControlSpec(-90, 12, \lin, 0.5), -12);   
  //  level.spec.step_(0.5);
    level.action = { arg changer, what; 
      var value; 
    //  value = changer.value.dbamp * (1 - mute.value);
    };

    freq = MXCV( ControlSpec(1, 20000, \lin, 1), 1000.0);   
    cycletime = MXCV( ControlSpec(0.1, 4, \lin, 0.1), 1.0);   
    cycle = MXCV( ControlSpec(0, 1, \lin, 1), 0);   
    cycle.action = { arg changer, what;
      if (changer.value > 0) {
        this.startcycle;
      } {
        this.stopcycle;
      };    
    };
/*    
    modeSV = MXSV([\mono, \cycle], \mono );
    modeSV.action = { arg changer, what;
      this.setmode;
      };    
*/
    signalSV = MXSV([\off, \sine, \square, \saw, \impulse, \greynoise, \clipnoise, \brownnoise, 'pinknoise', 'whitenoise', \soundfile], \off);
    signalSV.action = { arg changer, what;
      this.setmode;
    };    

    path = Platform.resourceDir +/+ "sounds/a11wlk01.wav";
    
  }

  setmute {arg value;
    if (synth.notNil) {
      if (value > 0) {
        synth.set(\on, 0)
      } {
        synth.set(\on, 1)
      }
    };
  }

  level_ { arg value;
    level.value = value;  
  }

  loadSoundfile {arg pathname;
    path = pathname ? path;
    if (buf.notNil) { buf.free };
    buf = Buffer.readChannel(MXGlobals.server, path, channels: [0] , action: { arg buf; synth.set(\buf, buf.bufnum) });
  }

  openSoundfile { arg okfunc;
    File.openDialog(
      "Select a soundfile...",
      { arg pathname;  
        this.loadSoundfile(pathname); 
        okfunc.value(PathName(pathname).fileName); 
      }); 
  }
    
  setmode {
    // vorher alle synths releasen!
    this.remove;
    
    switch(signalSV.item)
      {\off}    {   synth = nil;
           }
      {\sine}   { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], freq: freq, amp: [level, level.dbamp]], group);
          }
      {\square} { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], freq: freq, amp: [level, level.dbamp]], group);
          }
      {\saw}    { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], freq: freq, amp: [level, level.dbamp]], group);
          }
      {\impulse}  { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], freq: freq, amp: [level, level.dbamp]], group);
          }
      {\greynoise}  { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], amp: [level, level.dbamp]], group);
          }
      {\clipnoise}  { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], amp: [level, level.dbamp]], group);
          }
      {\brownnoise} { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], amp: [level, level.dbamp]], group);
          }
      {\pinknoise}  { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], amp: [level, level.dbamp]], group);
          }
      {\whitenoise} { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], amp: [level, level.dbamp]], group);
          }
      {\soundfile}  { synth = Synth.controls("test_" ++ signalSV.item,
               [out: outputNums[0], buf: buf.bufnum, amp: [level, level.dbamp]], group);
          }
      ;
  }

  remove {
  //  this.stopcycle;
    if( synth.notNil ) { synth.release; synth = nil; };
  }
  
  clear {  // gute Idee ???
    cycle.value = 0;
    signalSV.item = \off;
    mute.value = 0;
  //  this.remove;
  } 
      
  startcycle {
    var sortfunc;
    
    sortfunc = { arg a, b;  a[1].num <= b[1].num };
    // mute all active nodes of both testgen matrices:
    inMatrix.nodes.do(_.mute);
    outMatrix.nodes.do(_.mute);

    cycletask = Task({
      cycletime.value.wait;
      inf.do {
        if ( (inMatrix.nodes.size +  outMatrix.nodes.size) == 0) { cycletime.value.wait };
        inMatrix.nodes.sortedKeysValuesDo( {arg array, node;
          if(node.notNil) { node.unmute };
          cycletime.value.wait;
          if(node.notNil) { node.mute };
        }, sortfunc );
        outMatrix.nodes.sortedKeysValuesDo( {arg array, node;
          if(node.notNil) { node.unmute };
          cycletime.value.wait;
          if(node.notNil) { node.mute };
        }, sortfunc );
      };
    });
    cycletask.start;
  } 
  
  stopcycle {
    cycletask.stop;
    inMatrix.nodes.do(_.unmute);
    outMatrix.nodes.do(_.unmute);
  }

  setSR { 
    var inarray, outarray, inlabels, outlabels;
    // busArray, outputs and outputNums contain only one bus which gets the signal from the test synth:
    busArray = [ MXDeviceManager.getNewBus ];
    outputs = busArray;   
    outputNums = outputs.collect(_.busNum);   

  //  this.makeChannels;
    inarray = MXDeviceManager.inDevices.collect({ arg d, i; d.inputs}).flat;
    outarray = MXMonitorManager.devices.collect({ arg d, i; d.inputs}).flat
       ++ MXDeviceManager.outDevices.collect({ arg d, i; d.inputs}).flat;
/*
    inlabels = MXDeviceManager.inDevices.collect({ arg d, i; [ d.name, d.numChannels ] });
    outlabels = MXMonitorManager.devices.collect({ arg d, i; [ d.name, d.numChannels ] }) 
        ++ MXDeviceManager.outDevices.collect({ arg d, i; [ d.name, d.numChannels ] });
*/    
    MXMatrixManager.makeMatrix(\intestgenerator, outputs , inarray, [ ['', 1] ], MXMatrixManager.globalMatrix.inLabels);
    MXMatrixManager.makeMatrix(\outtestgenerator, outputs , outarray, [ ['', 1] ], MXMatrixManager.globalMatrix.outLabels);
    inMatrix = MXMatrixManager.matrices[\intestgenerator];
    outMatrix = MXMatrixManager.matrices[\outtestgenerator];
  }
  
  unsetSR { // to be called before sample rate changes
    busArray.do(_.free);
  }
  
  startDSP { arg target; 
  //  this.target = target;
    this.loadSoundfile;
    group = target;
    MXMatrixManager.matrices[\intestgenerator].startDSP(group);
    MXMatrixManager.matrices[\outtestgenerator].startDSP(group);
  //  group = Group(target);
    {   
    //  channels.do {arg chan;  chan.active_(1) };
    }.defer( MXGlobals.switchrate + 0.01 );
  } 
  
  stopDSP { 
    this.remove;   
    MXMatrixManager.matrices[\intestgenerator].stopDSP;
    MXMatrixManager.matrices[\outtestgenerator].stopDSP;
    // remove group and channel synths  
  //  channels.do {arg chan;  chan.active_(0) };
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  reset {
     { MXGUI.testgenView.clear }.defer;  // und schon wieder unsauber ...
  //   signalSV.item = \off;
  //  this.clear;
  } 

}


  
MXTestGeneratorGUI {
/*

clear + mute buttons

signal type (list)  + freq num + level num + load button

distribution type (list) + cycletime num

matrixGrid with inDevices (1 row)
matrixGrid with outDevices (1 row)

*/  
  var <parent, <bounds, <view, <testview, <ingridview, <ingrid, <outgridview, <outgrid;
  var inArray, outArray;
  var <inLabels, <outLabels;
  var inmatrix, outmatrix;
  var <muteButton, <clearButton;
  var <signalMenu, <levelNum, <freqNum;
  var <loadButton, <soundfilename;
  var <cycleButton, <cycleNum;
  var <inDeviceName, <outDeviceName, <inNum, <outNum;
  var <inlevel, <outlevel;
  var inselRow, inselCol, outselRow, outselCol;
  var buttonSize, font, <cellSize;
  var testgen;

  *new { arg parent, bounds, inmatrix, outmatrix;
    ^super.new.initMatrixGUI(parent, bounds, inmatrix, outmatrix);
  }
  
  initMatrixGUI { arg argparent, argbounds, arginmatrix, argoutmatrix;
    parent = argparent;
    bounds = argbounds ? parent.bounds;
    inmatrix = arginmatrix; //  MXMain.matrixManager.globalMatrix;
    outmatrix = argoutmatrix; //  MXMain.matrixManager.globalMatrix;
  //  inArray = arginArray;
  //  outArray = argoutArray;

    cellSize = MXGUI.matrixCellSize;
    font = MXGUI.editorFont;

    view = parent;
    
    testview = SCCompositeView(view, Rect(0, 0, view.bounds.width, view.bounds.height * 0.5));
    testview.background = MXGUI.tabPanelColor; // Color.grey(0.3);
    testview.decorator = FlowLayout(testview.bounds, margin: Point(20,20), gap: Point(10,20));

    ingridview = SCCompositeView(view, Rect(0, view.bounds.height * 0.5, view.bounds.width, view.bounds.height * 0.25));
    ingridview.background = MXGUI.tabPanelColor; // Color.grey(0.3);
  //  ingridview.decorator = FlowLayout(ingridview.bounds, margin: Point(0,0), gap: Point(0,0));

    outgridview = SCCompositeView(view, Rect(0, view.bounds.height * 0.75, view.bounds.width, view.bounds.height * 0.25));
    outgridview.background = MXGUI.tabPanelColor; // Color.grey(0.3);
  //  outgridview.decorator = FlowLayout(outgridview.bounds, margin: Point(0,0), gap: Point(0,0));
    
    this.prInitModels;
  //  this.prInitMappings;

    this.makeGrid;
    this.makeTestView;
  //  this.connect;
  //  selRow.value = 0;
  //  selCol.value = 0;
  }
  
  prInitModels {  
  
    inlevel =  MXCV(ControlSpec(-90.0, 0.0, -2, 0.5), -90.0);

    inlevel.action_( { arg changer, what; 
      // only on/off
      // ----> adjust testgen level, not nodes levels !!
      inmatrix.setNodeByIndex(0, inselCol.value, inlevel.value);
      {
        var amp;
        amp = (inlevel.value > -90).if( inlevel.value.dbamp, 0.0 );
        ingrid.setState_( inselCol.value, amp );
      }.defer 
    }, \synch);

    outlevel =  MXCV(ControlSpec(-90.0, 0.0, -2, 0.5), -90.0);

    outlevel.action_( { arg changer, what; 
      // only on/off
      // ----> adjust testgen level, not nodes levels !!
      outmatrix.setNodeByIndex(0, outselCol.value, outlevel.value);
      {
        var amp;
        amp = (outlevel.value > -90).if( outlevel.value.dbamp, 0.0 );
        outgrid.setState_( outselCol.value, amp );
      }.defer 
    }, \synch);


    inselCol = MXCV(ControlSpec(0, inmatrix.toBusArray.size - 1, \lin, 1), 0);  
    inselCol.action = { arg changer, what; ingrid.selectNode( inselCol.value) };
    inselCol.action = { arg changer, what; 
  //    inDeviceName.string = inLabels[changer.value][0];
  //    inNum.string = inLabels[changer.value][1];
    };
    inselCol.action = { arg changer, what; 
      var val;
      val = ingrid.getState(inselCol.value);
    //  level.value = (val == 0.0).if(-90.0, val.ampdb); 
    //  set.setTestOutbus( manager.outbuslist[selCol.value] );
    };

    outselCol = MXCV(ControlSpec(0, outmatrix.toBusArray.size - 1, \lin, 1), 0);  
    outselCol.action = { arg changer, what; outgrid.selectNode( outselCol.value ) };
    outselCol.action = { arg changer, what; 
  //    outDeviceName.string = outLabels[changer.value][0];
  //    outNum.string = outLabels[changer.value][1];
    };
    outselCol.action = { arg changer, what; 
      var val;
      val = outgrid.getState(outselCol.value);
    //  level.value = (val == 0.0).if(-90.0, val.ampdb); 
    //  set.setTestOutbus( manager.outbuslist[selCol.value] );
    };

  } 
  
  close {
  //  win.close;
  }

  makeTestView {
    var width, radius=5, border=0, shifty= -2;
    var nodeFont;

    width = testview.bounds.width - 16;

    nodeFont = MXGUI.infoFont;
    buttonSize = Point(100, 26);  

    muteButton = MXButton(testview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)      
    //  .states_([["mute", Color.white(1, 0.5), Color.red(1, 0.5) ], ["mute", Color.white, Color.red(1)]])
      .states_([["mute", Color.grey(0), Color.grey(0.7) ], ["mute", Color.grey(0), Color.red(1)]])
      ;
      
    clearButton = MXButton(testview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)      
    //  .states_([["clear", Color.white, Color.red(0.5)]])
      .states_([["clear", Color.grey(0), Color.grey(0.7)]])
      .action_({ arg view;
        this.clear;
      });
      
    testview.decorator.nextLine;
    testview.decorator.shift(0, 40);

    signalMenu = SCPopUpMenu(testview, buttonSize)
      .font_(nodeFont)
      .stringColor_(Color.grey(0))
      .background_(Color.grey(0.5))
      .items_( [""] )
      ;

    MXStringView(testview,  buttonSize)
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .align_(\right)
      .orientation_(\right)
      .background_(Color.grey(0.1, 0))
  //    .border_(1)
  //    .borderColor_(Color.grey(0.5))
      .string_("level:");

    levelNum = MXNumber(testview, buttonSize, radius  )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .unit_(" dB")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ;
      
    MXStringView(testview,  buttonSize )
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .align_(\right)
      .orientation_(\right)
      .background_(Color.grey(0.1, 0))
  //    .border_(1)
  //    .borderColor_(Color.grey(0.5))
      .string_("freq:");

    freqNum = MXNumber(testview, buttonSize, radius  )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .unit_(" Hz")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ;
      
    testview.decorator.nextLine;

  //  MXStringView(testview,  buttonSize).background_(Color.clear); // dummy

    loadButton = MXButton(testview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .states_([[ "load soundfile", Color.grey(0), Color.grey(0.7)]])
      ;

    soundfilename = MXStringView(testview,  Point(buttonSize.x * 4, buttonSize.y) )
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      .orientation_(\right)
      .background_(MXGUI.levelBackColor)
    //  .border_(1)
    //  .borderColor_(Color.grey(0.5))
      .string_("test.aif");

    testview.decorator.nextLine;
    testview.decorator.shift(0, 40);
    
//    MXStringView(testview,  buttonSize).background_(Color.clear); // dummy

    cycleButton = MXButton(testview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .states_([[ "cycle off", Color.grey(0), Color.grey(0.5)], [ "cycle on", Color.grey(0), MXGUI.plugColor ]])
    //  .action_({ arg changer, what; })
      ;
  
    // SCListView (mono, multi, cycle)

    MXStringView(testview,  buttonSize )
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .align_(\right)
      .orientation_(\right)
      .background_(Color.grey(0.1, 0))
  //    .border_(1)
  //    .borderColor_(Color.grey(0.5))
      .string_("cycle time:");

    cycleNum = MXNumber(testview, buttonSize, radius  )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .unit_(" s")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ;
      
//    MXStringView(testview,  buttonSize).background_(Color.clear); // dummy


  }

  makeGrid { 
    var nodeFont;
    nodeFont = MXGUI.infoFont;

/*
    inLabels = inmatrix.outLabels.collect({ arg array, i;  // label und anzahl busse / channels !
      var name, size;
      name = array[0];
      size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);

    outLabels = outmatrix.outLabels.collect({ arg array, i;  // label und anzahl busse / channels !
      var name, size;
      name = array[0];
      size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);
*/
    inLabels = inmatrix.outLabels2;
    outLabels = outmatrix.outLabels2;
    
    
    MXStringView(ingridview, Rect(0, 0, ingridview.bounds.width, 20 ) )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .background_(Color.clear)
      .align_(\center)
      .orientation_(\right)
      .border_(0)
      .inset_(0)
      .string_("Testsignal to Input Devices");

    ingrid = MXMatrixRow(ingridview, Rect(0, 22, ingridview.bounds.width, ingridview.bounds.height - 22) , inmatrix.outLabels)
      .setBackgrColor_(Color.grey(0.1))
      .setGridColor_(Color.grey(0.3))
      .setFillMode_(true)
      .setFillColor_(Color.cyan)
      .setCursorColor_(Color.red)
      .setTrailDrag_(false)
      .setNodeBorder_(2)
      .nodeDownAction_({ arg node; 
        inselCol.value = node.nodeloc[0];
        inlevel.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
        })
      .nodeTrackAction_({ arg node; 
        inselCol.value = node.nodeloc[0];
        inlevel.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
        })
      .nodeUpAction_({ arg node; 
        })
      .keyDownAction_({ arg char, modifiers, unicode, keycode;
        //[char.ascii, modifiers, unicode].postln;
        if (char == Char.space, { inlevel.input = (inlevel.input > 0.0).if(0,1)});
        if (unicode == 16rF703, { inselCol.inc; });
        if (unicode == 16rF702, { inselCol.dec; });
        //^nil  // bubble if it's an invalid key
        })
      ;
    ingrid.refresh;
    ingrid.focus(true);
    
    MXStringView(outgridview, Rect(0, 0, outgridview.bounds.width, 20 ) )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .background_(Color.clear)
      .align_(\center)
      .orientation_(\right)
      .border_(0)
      .inset_(0)
      .string_("Testsignal to Monitor and Output Devices");

    outgrid = MXMatrixRow(outgridview, Rect(0, 22, outgridview.bounds.width, outgridview.bounds.height - 22) , outmatrix.outLabels)
      .setBackgrColor_(Color.grey(0.1))
      .setGridColor_(Color.grey(0.3))
      .setFillMode_(true)
      .setFillColor_(Color.cyan)
      .setCursorColor_(Color.red)
      .setTrailDrag_(false)
      .setNodeBorder_(2)
      .nodeDownAction_({ arg node; 
        outselCol.value = node.nodeloc[0];
        outlevel.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
        })
      .nodeTrackAction_({ arg node; 
        outselCol.value = node.nodeloc[0];
        outlevel.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
        })
      .nodeUpAction_({ arg node; 
        })
      .keyDownAction_({ arg char, modifiers, unicode, keycode;
        //[char.ascii, modifiers, unicode].postln;
        if (char == Char.space, { outlevel.input = (outlevel.input > 0.0).if(0,1)});
        if (unicode == 16rF703, { outselCol.inc; });
        if (unicode == 16rF702, { outselCol.dec; });
        //^nil  // bubble if it's an invalid key
        })
      ;
    outgrid.refresh;
    outgrid.focus(true);
  }
  
  clear {
    testgen.clear;
    inmatrix.removeNodes;
    outmatrix.removeNodes;
    ingrid.clearGrid;
    outgrid.clearGrid;
  }

/*  
  mute { arg value;
    if ( value == 1 ) {Ê
      inmatrix.mute; 
      outmatrix.mute;
    }Ê{ 
      inmatrix.unmute;
      outmatrix.unmute;
    };
  } 
*/
  connect { arg gen;
    { 
    testgen = gen;
    levelNum.connect(gen.level);
    freqNum.connect(gen.freq);
  //  cycleButton.connect(gen.modeSV);
    cycleButton.connect(gen.cycle);
    cycleNum.connect(gen.cycletime);
    muteButton.connect(gen.mute);
    // clear ?

    signalMenu.connect(gen.signalSV);
    gen.signalSV.action = { arg changer, what;  
      if ( changer.value > 0 ) {
        signalMenu.background = MXGUI.plugColor;
      } { 
        signalMenu.background = Color.grey(0.5);
      }
    };
    
    soundfilename.string = PathName(gen.path).fileName;
    loadButton.action_({ arg changer, what;
      gen.openSoundfile( { arg string;
        soundfilename.string = string;
      });
    });

    
    }.defer;
  }
  
  disconnect {
    
  } 


}

