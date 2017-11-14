/*

real SC InputBus > MXInterface input > [ MXInput > MXInDevice > MXBus ]   

>>   [MXBus > MXConnection (MXNode(s)) > MXBus ] >>

[ MXBus > MXInDevice > MXOutput] > MXInterface output > real SC OutputBus


*/



MXBus {
/*
- represents and manages a single audio bus 
    
*/  
  var <num;     // Integer: SC private audio bus number
  var <index;     // Integer: SC private audio bus number (same as num !!)
  var <bus;     // Bus (SC Bus object (audio) )
  
  *new { // arg num;    // num provided by a bus allocator or MXIOManager ??
    ^super.new.init( /*num*/ )
  }
  
  init { /*arg argnum;*/
  //  num = argnum;
  //  bus = Bus(\audio, num, 1, MXGlobals.server);  
    bus = Bus.audio(MXGlobals.server, 1);
    num = bus.index;
    index = num;
  } 
  
  busNum {
    ^bus.index  
  }
  
  free {
    bus.free; 
  } 
  
}

/*

MXDeviceChannel {
/*  
- single channel for MXDevice
  - active, gain, phase, delay
  - 1 synth
  - 1 meter
*/  
  classvar <defName = "MXDeviceChannel";
  classvar <defNameDelay = "MXDeviceChannelDelay";

  var <device;      // Parent MXDevice
  var <num;     // Integer: channelnumber
  var <in;        // Integer: busindex
  var <out;     // Integer: busindex
//  var <group;     // target for synthnode
  var active;       // MXCV: [0, 1]   
  var <gain;      // MXCV: gain in dB  
  var <phase;       // MXCV: [0, 1]  (1 = 180¡)  
  var <delay;     // MXCV: delay in ms  (!!!)
  var <synth;     // Synthnode
//  var <meters;      // MXSimpleMeter 

  *initClass {    
  /*     // single channel synthdefs now replaced by multichannel synthdefs (one per MXDevice) !    MXGlobals.synthdefs.add (
      SynthDef(defName, { arg in=0, out=1, gain=0.0, on=1, gate=1;      var sig;
        sig = In.ar(in, 1);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [\ir, \ir, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add (
      SynthDef(defNameDelay, { arg in=0, out=1, gain=0.0, delay=0.0, on=1, gate=1;      var sig;
        sig = In.ar(in, 1);
        sig = DelayC.ar(sig, MXGlobals.delaymax, delay);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [\ir, \ir, MXGlobals.levelrate, MXGlobals.delayrate, MXGlobals.switchrate, 0] );
    );
  */
  } 

  *new { arg device, num, in, out, active=0;
    ^super.new.init(device, num, in, out, active);
  }

  init { arg argdevice, argnum, argin, argout, argactive;
    device = argdevice;
    num = argnum;
    in = argin;
    out = argout;
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, 0), 0.0);   
    phase = MXCV( ControlSpec(-1, 1, \lin, 1), 1);   
    delay = MXCV( ControlSpec(0, MXGlobals.delaymax * 1000, \lin, 0), 0.0);   
    active = MXCV( ControlSpec(0, 1, \lin, 1), argactive);  
    active.action = { arg changer, what; 
      if (changer.value == 1) {
        this.startDSP;
      } {
        this.stopDSP;
      };  
    //  if ( changer.value == 1 ) { this.makeSynth } { this.removeSynth };
    };
  //  ("Device channel added:" + device.name + device.type + num).postln;
  
  }

  active_ { arg value;
    active.value = value;   
  }
  
  active {
    ^active.value;    
  }

  makeSynth {
    if (delay.value > 0.0) {
      synth = Synth.controls(defNameDelay, 
        [   in: in, 
          out: out, 
        //  meterout: MXMeterManager.multiMeter.busArray[num].index,
        //  spectralout: MXMeterManager.fft.busArray[0].index,
          //  on: [device.mute, 1 - device.mute],
          on: [device.mute, { 1 - device.mute.value } ],
          gain: [ [gain, device.gain, phase, device.phase], 
            (gain + device.gain).dbamp * (phase * device.phase) ], 
          delay: [ [delay, device.delay], (delay + device.delay) * 0.001],
        ],
        device.group)
    } {
      synth = Synth.controls(defName, 
        [   in: in, 
          out: out, 
        //  meterout: MXMeterManager.multiMeter.busArray[num].index,
        //  spectralout: MXMeterManager.fft.busArray[0].index,
          on: [device.mute, { 1 - device.mute.value } ],
          gain: [ [gain, device.gain, phase, device.phase], 
            (gain + device.gain).dbamp * (phase * device.phase) ],
        ],
        device.group)
    };
  }

  removeSynth {
    synth.set(\gate, 0);
  }

  remove {
    // this.removeSynth;  
    // this.stopDSP;
    this.active_(0);
  }
    
  gain_ { arg value;
    gain.value = value; 
  }

  phase_ { arg value;
    phase.value = value;  
  }

  delay_ { arg value;   
    if ( ((value > 0) && (delay.value == 0))  ||Ê ((value == 0) && (delay.value > 0)) ) {
      this.removeSynth;
      delay.value = value;  
      this.makeSynth; 
    } {   
      delay.value = value;  
    };
  }

  startDSP { 
    this.makeSynth;
  }
  
  stopDSP {
    this. removeSynth;
  } 

}

*/

MXDevice {
/*
- class for virtual IODevices using a number of MXIOs:
  - real audio devices connected to the system, like CD-Player, microphones, laptops, loudspeakers, fx etc.
  - audio-interfaces and connection panels
  - groups and subgroups of these
  - any MXIO(s)
- separate devices for each samplingrate (single, double, quad) of a real audio device !!
- provides settings (global as well as for each channel ????):
  - in/active
  - gain, phase, delay (in ms)
- simple level meter for each channel

editable by MXDeviceManager
  
*/
  var <name;      // String or Symbol
  var <type;      // Symbol: [\in, \out]
  var <>sampleRates;  // Array of Floats, indicating the valid sampleRates of this virtual device 
  var <ioDict;      // Dictionary:   samplingrate -> [ <Array of MXIOs (\in or \out)> ] 
//  var <ioBusArray;    // Dictionary:   samplingrate -> [ <Array of (IO) busNums > ]   
  var <busArray;    // Array of (private) MXBusses
//  var <channels;    // Array of MXDeviceChannels
  var <numChannels; // Integer, number of channels / busses of this device
  var <inputs;      // Array of either MXIOs or private MXBusses
  var <outputs;     // Array of either MXIOs or private MXBusses
  var <inputNums;   // Array of bus numbers
  var <outputNums;  // Array of bus numbers
  // global:
//  var <target;      // Parent Group Node
  var <group;     // Group (Node) for Synths
  var <synth;     
  var <active;      // MXCV: [0, 1]  
  var <mute;      // MXCV: [0, 1]   
  var <>gainoffset = 0; // Float: device gain in dB, from devices.txt
  var <gain;      // MXCV: gain in dB, for controllers  
  var <>phaseoffset = 1; // Float: device phase factor [-1, 1], from devices.txt
  var <phase;       // MXCV: [-1, 1]  (1 = 180¡)  
  var <>delayoffset = 0;  // Float: device delay in ms, from devices.txt
  var <delay;     // MXCV: delay in ms   
  var <meter;     // MXSimpleMeter 
  var <monitorConnections;  // Dictionary of MXConnections (for monitoring !)
  var <routDict;    // Dictionary / Order for usable (according to the available device channels!) monitorpresets:   
            // presetname -> [input, output, gain in dB]
  var <routSV;      // MXSV: [presetnames], symbols for routings menue, item / value = selected routing
  var <controlSV;   // MXSV: [fader nums], associated MIDI fader
  var <phones;    // MXCV: [0, 1]  activates phones monitoring
  var <near;      // MXCV: [0, 1]  activates nearfield monitoring
  var <multimeter;    // MXCV: [0, 1]  activates sends to multichannelmeter
  var <spectral;    // MXCV: [0, 1]  activates sends to spectral displays
  var <>guifunc;    // Function to change Views according to active
  var <>ctlgainfunc;  // Function for connecting this gain MXCV to the MXMIDI control model
  var <>ctlmutefunc;  // Function for connecting this mute MXCV to the MXMIDI control model
  
  
  *new { arg name, type, ioDict;
    ^super.new.init(name, type, ioDict);
  }

  init { arg argname, argtype, argioDict;
    ioDict = argioDict;
    name = argname ? "noname";
    type = argtype;
    routDict = Dictionary.new;
    
    monitorConnections = Dictionary.new;
    monitorConnections.add( \main -> nil );
    monitorConnections.add( \sub -> nil );
    monitorConnections.add( \near -> nil );
    monitorConnections.add( \phones -> nil );
    monitorConnections.add( \multimeter -> nil );
    monitorConnections.add( \spectral -> nil );

    // init global CVs
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, 0.5), 0.0);   
/*    gain.action = {Êarg changer, what;
      if (ctlgainfunc.notNil) { ctlgainfunc.value(changer.value) };
    };
*/
    phase = MXCV( ControlSpec(-1, 1, \lin, 1), 1);   
    delay = MXCV( ControlSpec(0, MXGlobals.delaymax * 1000, \lin, 0), 0.0);   
    mute = MXCV( ControlSpec(0, 1, \lin, 1), 0);   // called by GUI mute button
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // + action ??
    active.action = { arg changer, what;  
      guifunc.value(active.value);
      if (changer.value == 1) {
        switch (type)
          { \in }   {Êthis.startDSP(MXMain.deviceManager.inGroup) }           { \out }  {Êthis.startDSP(MXMain.deviceManager.outGroup) }  
          ; 
      } {
        this.stopDSP;
      //  + disable buttons: M, P, N, X, and menu ? 
      };  
    };
    
  //  routSV = MXSV( [ \off, \matrix ], 0);     // has always at least these two items!
  //  routSV = MXSV( [ \off ], 0);    // has always at least this item!
    routSV = MXSV( [ nil  ], 0);     
    routSV.action = { arg changer, what;  
      if ( active.value == 1 ) {
        // remove existing connections:
        if ( monitorConnections[ \main ].notNil ) {
          monitorConnections[ \main ].disconnect;
          monitorConnections[ \main ] = nil;
        };
        if ( monitorConnections[ \sub ].notNil ) {
          monitorConnections[ \sub ].disconnect;
          monitorConnections[ \sub ] = nil;
        };
        if ( routSV.value > 0 ) {
          // make a new connection: 
          if (routSV.item == \matrix) {
            // open local matrix editor window ...
          } {
            monitorConnections[ \main ] = 
              MXMatrixManager.makeConnectionFromMonitorPreset(this, routDict[routSV.item]);
            monitorConnections[ \sub ] = 
              MXMatrixManager.makeSubConnectionFromMonitorPreset(this, routDict[routSV.item]);
          }
        };  
      };
    };

    controlSV = MXSV( ['MIDI off'] ++ (1 .. 16).collect({ arg i; ("fader" + i).asSymbol }) , 0);   
    controlSV.action = { arg changer, what;
      // disconnect the old connections to MIDI (remove all SimpleControllers to and from gain and mute)
      MXMIDI.disconnectControlFromMIDI(gain);
      MXMIDI.disconnectControlFromMIDI(mute);
      if (changer.value > 0) {
        // connect gain to devicevolumeN (make a SimpleController from and to gain, register both)
        MXMIDI.connectControlToMIDI(gain, ("devicevolume" ++ changer.value.asString).asSymbol, name);
        // connect mute to devicemuteN (make a SimpleController from and to mute, register both)
        MXMIDI.connectControlToMIDI(mute, ("devicemute" ++ changer.value.asString).asSymbol, name);
      } 
      
    };   
    phones = MXCV( ControlSpec(0, 1, \lin, 1), 0);      phones.action = { arg changer, what;  
      if ( (phones.value == 0) &&  monitorConnections[ \phones ].notNil ) {
        monitorConnections[ \phones ].disconnect;
        monitorConnections[ \phones ] = nil;
      };
      if ( (active.value == 1) &&  (phones.value == 1) && monitorConnections[ \phones ].isNil ) {
        monitorConnections[ \phones ] = 
          MXConnection(MXMatrixManager.monitorGroup, this, MXMonitorManager.phonesMonitor, \simple);      };
    };

    near = MXCV( ControlSpec(0, 1, \lin, 1), 0);      near.action = { arg changer, what;  
      if ( (near.value == 0) &&  monitorConnections[ \near ].notNil ) {
        monitorConnections[ \near ].disconnect;
        monitorConnections[ \near ] = nil;
      };
      if ( (active.value == 1) &&  (near.value == 1) && monitorConnections[ \near ].isNil ) {
        monitorConnections[ \near ] = 
          MXConnection(MXMatrixManager.monitorGroup, this, MXMonitorManager.nearMonitor, \simple);      };
    };

    multimeter = MXCV( ControlSpec(0, 1, \lin, 1), 0);    multimeter.action = { arg changer, what;  
      if ( (multimeter.value == 0) &&  monitorConnections[ \multimeter ].notNil ) {
        monitorConnections[ \multimeter ].disconnect;
        monitorConnections[ \multimeter ] = nil;
        MXMeterManager.registerMultiMeterRequests(false);
      };
      if ( (active.value == 1) &&  (multimeter.value == 1) && monitorConnections[ \multimeter ].isNil ) {
        monitorConnections[ \multimeter ] = 
          MXConnection(MXMatrixManager.monitorGroup, this, MXMeterManager.multiMeter, \simple);       MXMeterManager.registerMultiMeterRequests(true);
      };
    };

    spectral = MXCV( ControlSpec(0, 1, \lin, 1), 0);    spectral.action = { arg changer, what;  
      if ( (spectral.value == 0) &&  monitorConnections[ \spectral ].notNil ) {
        monitorConnections[ \spectral ].disconnect;
        monitorConnections[ \spectral ] = nil;
        MXMeterManager.registerAnalyserRequests(false);
      };
      if ( (active.value == 1) &&  (spectral.value == 1) && monitorConnections[ \spectral ].isNil ) {
        monitorConnections[ \spectral ] = 
          MXConnection(MXMatrixManager.monitorGroup, this, MXMeterManager.fft, \auto);
        MXMeterManager.registerAnalyserRequests(true);
      };
    
    };
    
//    this.makeIOs; // immer auch beim Wechsel der sampleRate !
//    this.makeChannels;
    ("Device added:" + type + name).postln; 
  }

  gain_ { arg value;
    if (value != gain.value) {
      gain.value = value; 
    };
  }

  phase_ { arg value;
    if (value != phase.value) {
      phase.value = value;  
    };
  }

  delay_ { arg value;   
    if (value != delay.value) {
      if ( ((value > 0) && (delay.value == 0))  ||Ê ((value == 0) && (delay.value > 0)) ) {
        this.removeSynth;
        delay.value = value;  
        this.makeSynth; 
      } {   
        delay.value = value;  
      };
    }
  }
  
  active_ { arg value;  // called by GUI-activation (device title button)
    if (value != active.value) {
      active.value = value; 
    };
  }

  mute_ { arg value;  // called by GUI mute button
    if (value != mute.value) {
      mute.value = value; 
    };
  }

  routSV_ { arg value;    
    if (value != routSV.value) {
      routSV.value = value; 
    }
  }

  routSVItem_ { arg item;   
    if (item != routSV.item) {
      routSV.item = item; 
    }
  }

  controlSV_ { arg value;   
    if (value != controlSV.value) {
      controlSV.value = value;  
    }
  }

  phones_ { arg value;    
    if (value != phones.value) {
      phones.value = value; 
    }
  }

  near_ { arg value;    
    if (value != near.value) {
      near.value = value; 
    }
  }

  multimeter_ { arg value;    
    if (value != multimeter.value) {
      multimeter.value = value; 
    }
  }

  spectral_ { arg value;    
    if (value != spectral.value) {
      spectral.value = value; 
    }
  }




  getMonitorPresets { // collect all monitorpresets that doesn't have more inputs than the number of channels in this device:
    var name, connectionarray;
    routSV.items = [ (MXMonitorManager.mainMonitor.name + "off").asSymbol ]; // has always at least this item!

    MXMatrixManager.monitorpresets.do({ arg list, numInputs, i;
      if (numInputs <= numChannels) {
        list.do({ arg assoc, r;
          name = assoc.key.asSymbol;
          connectionarray = assoc.value.asArray;
          routDict.add( name -> connectionarray );
          routSV.items = routSV.items ++ name;
        });
      };
    });
  } 

  removeMonitorPresets {
    routDict.clear;
    routSV.items = [ \off ];
    routSV.value = 0;  // resets routing menu
  } 
  
  removeMonitorConnections {
    monitorConnections.keysValuesDo { arg key, conn, i;
      if (conn.notNil) {
        conn.remove;
        monitorConnections[key] = nil;
      };  
    };
    // monitorConnections.clear;
  }

  setSR { // to be called by the device manager when sampleRate is known and server has been booted
    var srspeed, ioArray;
    srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];

    // make busArray for current samplerate
    ioArray = ioDict[ srspeed ];
    busArray = ioArray.collect {Êarg io, i;  MXDeviceManager.getNewBus };
    numChannels = ioArray.size;
    
    switch (type)
      { \in }   {Ê  inputs = ioArray;     // Array of MXInputs
            outputs = busArray;   // Array of MXBusses 
          } 
      { \out }  {Ê  inputs = busArray;  // Array of MXBusses 
            outputs = ioArray;    // Array of MXOutputs
          } 
      ;
    inputNums = inputs.collect(_.busNum);
    outputNums = outputs.collect(_.busNum);
    
    // remove existing channels and meter before??
    this.makeSynthDef;
  //  this.makeChannels;
  //  if (type ==  \in) {Êmeter = MXSimpleMeter(this, busArray.collect(_.busNum)) };
    meter = MXSimpleMeter(this, outputs);
    this.getMonitorPresets;
  }
  
  unsetSR { // to be called before sample rate changes
    this.removeMonitorPresets;
    meter.remove;
    busArray.do(_.free);
    busArray.clear;
    inputs.clear;
    outputs.clear;

  //  this.removeChannels;
  }
  
/*
  makeChannels {
    var srspeed;
    srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];

    // make busArray for current samplerate
    busArray = ioDict[ srspeed ].collect {Êarg io, i;  MXDeviceManager.getNewBus };
    
    // make an inactive MXDeviceChannel for each MXIO !
    switch (type)
      { \in }   {Êchannels = ioDict[ srspeed ].collect {Êarg io, i; MXDeviceChannel(this, i, io.busNum, busArray[i].busNum, active.value) } }
      { \out }  {Êchannels = busArray.collect {Êarg mxbus, i; MXDeviceChannel(this, i, mxbus.busNum, ioDict[ srspeed ][i].busNum, active.value) } }
      ;
  }

  removeChannels {
    channels.do(_.remove);
    channels.clear;
    busArray.do(_.free);
    busArray.clear;
  } 
*/
  makeSynthDef {

    SynthDef(name.asString + type.asString, { arg gain=0.0, on=1, gate=1;     var sig;
      sig = In.ar(inputNums);
//      sig = ins.collect { arg in; In.ar(in, 1) };
      sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
      sig = sig * on;
  //    outs.do { arg out, i; Out.ar(out, sig[i]) };
      outputNums.collect({ arg bus, i; Out.ar(bus, sig[i]) });
    //  Out.ar(outs, sig);
    }, [MXGlobals.levelrate, MXGlobals.switchrate, 0] ).add(\global);
      
    SynthDef(name.asString + type.asString + "delay", { arg gain=0.0, delay=0.0, on=1, gate=1;      var sig;
      sig = In.ar(inputNums);
//      sig = ins.collect { arg in; In.ar(in, 1) };
      sig = DelayC.ar(sig, MXGlobals.delaymax, delay);
      sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
      sig = sig * on;
      outputNums.collect({ arg bus, i; Out.ar(bus, sig[i]) });
    //  Out.ar(outs, sig);
    }, [MXGlobals.levelrate, MXGlobals.delayrate, MXGlobals.switchrate, 0] ).add(\global);
  } 

  makeSynth {
    if (delay.value > 0.0) {
      synth = Synth.controls(name.asString + type.asString + "delay", 
        [   on: [mute, { 1 - mute.value } ],
          gain: [ [gain, phase], (gain + gainoffset).dbamp * (phase * phaseoffset) ], 
          delay: [ delay, (delay + delayoffset) * 0.001],
        ], group)
    } {
      synth = Synth.controls(name.asString + type.asString, 
        [   
          on: [mute, { 1 - mute.value } ],
        //  on: [mute, mute.neg + 1 ],
          gain: [ [gain, phase], (gain + gainoffset).dbamp * (phase * phaseoffset) ],
        //  gain: [ [gain, phase], { (gain.value + gainoffset).dbamp * (phase.value * phaseoffset) } ],
        //  gain: [ [gain, phase], gain.dbamp * phase ],
        ], group)
    };
  }

  removeSynth {
    synth.set(\gate, 0);
  }

  remove {
    // this.removeSynth;  
    // this.stopDSP;
    this.active_(0);
  }

  startDSP { arg target; 
  //  this.target = target;
  //  target = target;
    group = Group(target);
    {   
    //  channels.do {Êarg chan;  chan.active_(1) };
      this.makeSynth;

  //    if (type ==  \in) { 
        meter.active_(1);
  //       };
    }.defer( MXGlobals.switchrate + 0.01 );
  } 
  
  stopDSP {    
    // remove group and channel synths  
  //  if (type ==  \in) { 
      meter.active_(0); 
      meter.rms.input = 0; 
      routSV.value = 0;
      phones.value = 0;
      near.value = 0;
      multimeter.value = 0;
      spectral.value = 0;
  //    };
    this.removeMonitorConnections;
  //  channels.do {Êarg chan;  chan.active_(0) };
    this.removeSynth;
    controlSV.value = 0; // disconnect MIDI
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  reset {
    this.active_(1);  
    this.mute_(mute.spec.default);  
    this.gain_(gain.spec.default);  
    this.phase_(phase.spec.default);  
    this.delay_(delay.spec.default);  
    this.routSV_(routSV.spec.default);  
    this.controlSV_(controlSV.spec.default);  
    this.phones_(phones.spec.default);  
    this.near_(near.spec.default);  
    this.multimeter_(multimeter.spec.default);  
    this.spectral_(spectral.spec.default);  
  }

  getValues {
    var dict = IdentityDictionary.new;
    dict.add( \name     -> name );
    dict.add( \channels -> numChannels );
    dict.add( \mute     -> mute.value );
    dict.add( \gain     -> gain.value );
    dict.add( \phase    -> phase.value );
    dict.add( \delay    -> delay.value );
    dict.add( \phones   -> phones.value );
    dict.add( \near     -> near.value );
    dict.add( \multimeter   -> multimeter.value );
    dict.add( \spectral   -> spectral.value );
    dict.add( \routing    -> routSV.item );  // name des monitorpresets !
    dict.add( \control    -> controlSV.value );
    ^dict;
  }

  setValues { arg dict;
    if (dict.includesKey( \mute ))    { this.mute_(dict[\mute]); };
    if (dict.includesKey( \gain ))    { this.gain_(dict[\gain]); };
    if (dict.includesKey( \phase ))     { this.phase_(dict[\phase]) };
    if (dict.includesKey( \delay ))     { this.delay_(dict[\delay])  };
    if (dict.includesKey( \phones ))  { this.phones_(dict[\phones]); };
    if (dict.includesKey( \near ))    { this.near_(dict[\near]); };
    if (dict.includesKey( \multimeter ))  { this.multimeter_(dict[\multimeter]); };
    if (dict.includesKey( \spectral ))  { this.spectral_(dict[\spectral]); };
    if (dict.includesKey( \routing ))     { if (routSV.items.includesEqual(dict[\routing])) { 
                      this.routSVItem_(dict[\routing]);
                      //  routSV.item = dict[\routing] 
                      } {
                        ("monitor routing" + dict[\routing] + "doesn't exist !").warn;
                      };
                    };
    if (dict.includesKey( \control ))     { this.controlSV_(dict[\control]); };
  }
  
}

/*
MXInDevice : MXDevice {
  // represents a device which sends signals into the system / matrix
  // MXInputs -> MXDevice synths -> MXBusses
  var <type = \in; 
/*  
  type {
    ^\in  
  }
*/  
  inputs {
    ^ioDict   // Array of MXInputs
  }
  
  outputs {
    ^busArray // Array of MXBusses
  }
    
}

MXOutDevice : MXDevice {
  // represents a device which gets signals from the system / matrix
  // MXBusses  -> MXDevice synths -> MXOutputs
  var <type = \out; 
/*
  type {
    ^\out 
  }
*/
  inputs {
    ^busArray   // Array of MXBusses
  }
  
  outputs {
    ^ioDict     // Array of MXOutputs
  }

}
*/

MXDeviceView {
/*  
  titleButton (device on/off)
  meterView (simplemeter)
  muteButton (on/off)
  gainNumber (numberbox)
  phonesButton (on/off)
  nearButton (on/off)
  matrixButton (on/off)
  routingMenu
    alt: showMatrixButton (trigger)
*/
  var <parent, <bounds;  // parent: sourcesTab of routingTabs
  var <panel, <titleButton, <meterView, <muteButton, <gainNumber, <phonesButton, <nearButton, <matrixButton;
  var <multimeterButton, <spectralButton;
  var <routingMenu, <showMatrixButton;
  var <controlMenu;
  var <font, <smallFont, <titleFont;
  var <backgroundColorOff, <backgroundColorOn;
  var <buttonBackColorOff, <buttonBackColorOn;
  var <buttonTextColorOff, <buttonTextColorOn;
  var <buttonFrameColor;
  var <titleBackColorOff, <titleBackColorOn;
  var <titleTextColorOff, <titleTextColorOn;
  var <titleFrameColor; 
  var <device;  // associated inDevice
  
  *new { arg parent, bounds;
    ^super.new.initView(parent, bounds)
  }

  initView { arg argparent, argbounds;
    var panelrect, button1size, button2size, button3size, hgap=4, vgap=4, radius=5, shifty= 0, border=0;
    parent = argparent;
    bounds = argbounds.asRect;

    font = SCFont("Arial Rounded MT Bold", 10.0); // small buttons
    smallFont = SCFont("Arial Rounded MT Bold", 10.0); // routingmenu and showMatrixButton
    titleFont = SCFont("Arial Rounded MT Bold", 12.0); // titleButton
    backgroundColorOff = Color.grey(0, 0.5);
    backgroundColorOn = Color.grey(0.2, 0);
    buttonBackColorOff = Color.grey(0.5);
    buttonBackColorOn = Color.grey(0.5);
    buttonTextColorOff = Color.grey(0.0);
    buttonTextColorOn = Color.grey(0.0);
    buttonFrameColor = Color.grey(0.5);
    titleBackColorOff = Color.grey(0.5);
    titleBackColorOn = Color.grey(0.7);
    titleTextColorOff = Color.grey(0.0);
    titleTextColorOn = Color.grey(0.0);
    titleFrameColor = Color.grey(0.5);  

    panel = SCCompositeView(parent, bounds);
    panel.background = Color.clear; // backgroundColorOn;
    panel.decorator = FlowLayout(panel.bounds, margin: 0@0, gap: hgap@vgap);

    panelrect = panel.bounds.moveTo(0,0);
    button1size = Point(panelrect.width - (0*hgap), (panelrect.height - (4*vgap) - (0*hgap)) / 5);
    button2size = Point((panelrect.width - (1*hgap) - 1) / 2, (panelrect.height - (4*vgap) - (0*hgap)) / 5);
    button3size = Point((panelrect.width - (2*hgap) - 1) / 3, (panelrect.height - (4*vgap) - (0*hgap)) / 5);
    
  /*  titleButton = MXButton(panel, Rect(0, 0, panelrect.width, buttonsize.y))
      .shifty_(shifty)
      .states_([  ["device", titleTextColorOff, titleBackColorOff],
             ["device", titleTextColorOn, titleBackColorOn] ])
      .font_(titleFont);
  */
  
    titleButton = MXStringView(panel, button1size, radius)
      .shifty_(-2)
      .font_(titleFont)
      .stringColor_(titleTextColorOn)
      .align_(\center)
      .orientation_(\right)
      .background_(titleBackColorOn)
    //  .border_(1)
    //  .borderColor_(Color.grey(0.5))
      .inset_(0)
      .string_("device");

    muteButton = MXButton(panel, button3size, radius)
      .shifty_(shifty)
      .states_([["mute", buttonTextColorOff, buttonBackColorOff ], ["mute", buttonTextColorOn, Color.red(1)]])
      .font_(font);
    
    gainNumber = MXNumber(panel, button3size, radius)
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(font)
      .unit_(" dB")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ; 

    controlMenu = SCPopUpMenu(panel, button3size)
      .font_(font)
      .stringColor_(buttonTextColorOn)
      .background_(buttonBackColorOff)
      .items_( [" "] )
      ;


    multimeterButton = MXButton(panel, button2size, radius)
      .shifty_(shifty)
      .states_([  ["Levelmeter", buttonTextColorOff, buttonBackColorOff],
             ["Levelmeter", buttonTextColorOn, MXGUI.plugColor] ])
      .font_(font);

    spectralButton = MXButton(panel, button2size, radius)
      .shifty_(shifty)
      .states_([  ["Analyzer", buttonTextColorOff, buttonBackColorOff],
             ["Analyzer", buttonTextColorOn, MXGUI.plugColor] ])
      .font_(font);

    nearButton = MXButton(panel, button2size, radius)
      .shifty_(shifty)
      .states_([  ["Nearfield", buttonTextColorOff, buttonBackColorOff],
             ["Nearfield", buttonTextColorOn, Color.green] ])
      .font_(font);

    phonesButton = MXButton(panel, button2size, radius)
      .shifty_(shifty)
      .states_([  ["Phones", buttonTextColorOff, buttonBackColorOff],
             ["Phones", buttonTextColorOn, Color.green] ])
      .font_(font);

    routingMenu = SCPopUpMenu(panel, button1size)
      .font_(font)
      .stringColor_(buttonTextColorOn)
      .background_(buttonBackColorOff)
      .items_( (0 .. 5).collect(_.asString) )
      ;
  
    panel.decorator.reset;
    panel.decorator.shift(4, 4);
      
    meterView = MXLEDView(panel, 17@17)
      .background_(Color.green(0.0, 0.8))
      .color_(Color.green(1, 1))
      .border_(2)
      .borderColor_(Color.black)
      .value_(0)
      ;
  } 
  
  enable {Êarg bool;
    // titleButton and panelbackground  > device.active

    if (bool) {
      panel.background = backgroundColorOn;
    } {
      panel.background = backgroundColorOff;
    };      
    
    phonesButton.visible_(bool);
    nearButton.visible_(bool);
  //  nearButton.enabled_(bool);
  //  matrixButton.visible_(bool);
    routingMenu.visible_(bool);
    controlMenu.visible_(bool);
    muteButton.visible_(bool);
    gainNumber.visible_(bool);
    meterView.visible_(bool);
  }
  
  connect { arg dev;
    device = dev;
    { 
    titleButton.string = device.name;
  //  titleButton.connect(device.active);
  //  this.enable(true);
    dev.guifunc = {Êarg value;
  //    this.enable(value.booleanValue);    
    };
    meterView.connect(device.meter.rms);
    gainNumber.connect(device.gain);
    muteButton.connect(device.mute);
    multimeterButton.connect(device.multimeter);
    spectralButton.connect(device.spectral);
    phonesButton.connect(device.phones);
    nearButton.connect(device.near);
    // matrixButton > ??
    routingMenu.connect(device.routSV);
    device.routSV.action = { arg changer, what;  
      if ( changer.value > 0 ) {
        {ÊroutingMenu.background = Color.green; }.defer;
      } { 
        {ÊroutingMenu.background = buttonBackColorOff;}.defer;
      }
    };
    controlMenu.connect(device.controlSV);
    device.controlSV.action = { arg changer, what;  
      if ( changer.value > 0 ) {
        {ÊcontrolMenu.background = Color.yellow;  }.defer;
      } { 
        {ÊcontrolMenu.background = buttonBackColorOff; }.defer;
      }
    };

    // routingMenu.valueAction = 0;
    // showMatrixButton > ??
    }.defer;
  }
  
  disconnect {
    
  }     


} 


MXDeviceManager {  // singleton !
/*
- scannt beim Start folder mit XMLs fuer MXDevices fuer die aktuelle Samplingrate und legt entsprechende Objekte an
- verwaltet und erzeugt MXBus-Objekte fuer inputs und outputs
- stellt Editor fuer bestehende und neue MXDevices bereit:
  erzeugen, kopieren und loeschen von MXDevices
  laden und speichern von einzelnen MXDevices
  editieren von MXDevices (channels/MXIOs, name)
  GUI (active, gain, phase, delay) und MXSimpleMeter anzeigen
*/
  classvar <inDevices;    // Array of MXInDevices
  classvar <outDevices;   // Array of MXOutDevices

  classvar <inGroup;  // Node: target for server group for MXInDevices input synth nodes
  classvar <outGroup; // dito
  
  classvar <win, <view;   // GUI

  *init {
    inDevices = List.new;
    outDevices = List.new;
    this.readConfig;
  }

  *readConfig {
    var path, arrayfromfile, dict;
    path = MXGlobals.configDir ++ "devices.txt";
    if (File.exists(path)) {
      ("\n------------------\nreading" + path + "\n------------------").postln;
      arrayfromfile = File.open(path, "r").readAllString.interpret;
    //  arrayfromfile.postln;
      dict = Dictionary.new;
      arrayfromfile.do { arg assoc, i;
        // assoc.key = assoc.key.asSymbol;
        dict.add( assoc.key.asSymbol -> assoc.value )
      };
      if (dict.includesKey( \inputs )) { 
        dict[\inputs].do {Êarg assoc, i;
          this.addDeviceFromArray(\in, [assoc.key] ++ assoc.value );
        }
      } { 
        "WARNING: no input devices found in file".postln;
      };
      if (dict.includesKey( \monitors )) { 
        dict[\monitors].do {Êarg assoc, i;
          this.addDeviceFromArray(\monitorout, [assoc.key] ++ assoc.value );
          
        }
      } { 
        "WARNING: no monitor devices found in file".postln;
      };
      if (dict.includesKey( \outputs )) { 
        dict[\outputs].do {Êarg assoc, i;
          this.addDeviceFromArray(\out, [assoc.key] ++ assoc.value );
        }
      } { 
        "WARNING: no output devices found in file".postln;
      };
    } {
      "FILE ERROR: devices.txt not found!".postln; 
    };    
  } 
  
  *addDeviceFromArray { arg type, array;
    var device, cons, ioDict;
    var name, gain, phase, latency, sampleRates, busNums;
    var function;
    name = array[0].asString;
    gain = array[3].asFloat;
    phase = array[4].asFloat;
    latency = array[5].asInteger;
    sampleRates = [44100.0, 48000.0, 88200.0, 96000.0];
    busNums = Dictionary.new;
    busNums.add( 48000.0 -> (( array[1][0] .. array[1][1] ) ).collect(_.asInteger - 1) );
    busNums.add( 96000.0 -> (( array[2][0] .. array[2][1] ) ).collect(_.asInteger - 1) );
    if (type == \in) {
      busNums[48000.0] = busNums[48000.0] + MXGlobals.numOutputs;
      busNums[96000.0] = busNums[96000.0] + MXGlobals.numOutputs;
    };

  //  busNums[48000.0].postln;
    ioDict = Dictionary.new;
    switch (type)
      {\in}       { cons =  MXMain.ioManager.inputs }
      {\out}      { cons =  MXMain.ioManager.outputs }
      {\monitorout}   { cons =  MXMain.ioManager.outputs }
      ;
    
    ioDict.add( 48000.0 ->  cons[48000.0].select({arg io, i;  busNums[48000.0].includes(io.busNum) }) );
    ioDict.add( 96000.0 ->  cons[96000.0].select({arg io, i;  busNums[96000.0].includes(io.busNum) }) );
  //  ioDict.postln;

    if (type == \monitorout) {
      function = array[6] ? \aux;
      device = MXMonitorDevice(name, type, ioDict);
      device.function = function.asSymbol;
      device.gain = (function == \sub).if(-20, -20);
    } {   
      device = MXDevice(name, type, ioDict);
      device.gain = 0;
    };
      
    device.gainoffset = gain;
    device.phaseoffset = phase;
    device.phase = 1;
    device.delayoffset = latency;
    device.delay = 0;
    device.sampleRates = sampleRates;

    switch (type)
      {\in}       { inDevices.add(device) }
      {\out}      { outDevices.add(device) }
      {\monitorout}   { 
       // outDevices.add(device);
        MXMonitorManager.addDevice(device);
         }
    ;   
  }
  
  *addDevice {
    
  }
  
  *removeDevice {
    
  }
    
  *getNewBus { 
    // provides a new MXBus for MXDevices on demand
  //  ^MXBus( Bus.audio(MXGlobals.server, 1).index );
    ^MXBus.new;
  }
  
  *releaseBus { arg mxbus;
    mxbus.free;
  }

  *setSR { // to be called once sampleRate is known and server has been booted
    "DeviceManager-setSR".postln;   
    inDevices.do {Êarg d; d.setSR };
    outDevices.do {Êarg d; d.setSR };
  }
  
  *unsetSR { // to be called when sampleRate will be changed and before server reboots
    inDevices.do {Êarg d; d.unsetSR };
    outDevices.do {Êarg d; d.unsetSR };
  }
  
  *startDSP { arg inTarget, outTarget; 
    "DeviceManager-startDSP".postln;
    inGroup = inTarget;
    outGroup = outTarget;
    inDevices.do {Êarg d;   d.active = 1 };
    outDevices.do {Êarg d;   d.active = 1 };
  //  outDevices.do {Êarg d;   d.startDSP(outGroup) };
  } 
  
  *stopDSP {  // ??
    inDevices.do {Êarg d; d.stopDSP };
    outDevices.do {Êarg d; d.stopDSP };
    { inGroup.free; outGroup.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  *reset {
    inDevices.do {Êarg d;   d.reset };
    outDevices.do {Êarg d;   d.reset };
  } 
    
  *getValues {
    var dict = IdentityDictionary.new;
    dict.add( \inDevices  -> inDevices.collect({ arg dev, i; (dev.name -> dev.getValues) }) );
    dict.add( \outDevices -> outDevices.collect({ arg dev, i; (dev.name -> dev.getValues) }) );
    ^dict;
  }
  
  *setValues { arg dict;
    if (dict.includesKey( \inDevices )) { 
      dict[\inDevices].do({ arg assoc;
        var devname, devdict;
        var index;
        devname = assoc.key;
        devdict = assoc.value;              index = inDevices.detectIndex({ arg dev;  dev.name == devname });
        if (index.notNil) { 
          inDevices[index].setValues(devdict); 
        } {    
          ("input device" + devname + "doesn't exist !").warn;
        };
      }); 
    };
    if (dict.includesKey( \outDevices )) { 
      dict[\outDevices].do({ arg assoc;
        var devname, devdict;
        var index;
        devname = assoc.key;
        devdict = assoc.value;
        index = outDevices.detectIndex({ arg dev;  dev.name == devname });
        if (index.notNil) { 
          outDevices[index].setValues(devdict); 
        } {    
          ("output device" + devname + "doesn't exist !").warn;
        };
      }); 
    };
  }
}


