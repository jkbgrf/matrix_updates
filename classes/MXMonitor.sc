
MXMonitorChannel {
/* class for individual monitor channels and synths
  - on/off, gain, phase
*/  
  classvar <defName = "MXMonitorChannel";
  classvar <defNameDelay = "MXMonitorChannelDelay";

  var <device;      // Parent MXMonitorDevice
  var <num;     // Integer: channelnumber
  var <in;        // Integer: real busindex
  var <out;     // Integer: real busindex
//  var <group;     // target for synthnode >> device.group !
  var active;       // MXCV: [0, 1]   
  var <gain;      // MXCV: gain in dB, no general volume, only for balancing between channels
  var <on;      // MXCV: [0, 1]   
  var <phase;       // MXCV: [-1, 1]  (-1 = 180¡)  
  var <delay;     // MXCV: delay in ms (!!!)
  var <synth;     // Synthnode
  var <modified;    // MXCV: [0, 1]  , for displaying any difference to default values
//  var <meters;      // MXSimpleMeter 

  *initClass {    
    MXGlobals.synthdefs.add (
      SynthDef(defName, { arg in=0, out=1, gain=0.0, on=1, gate=1;      var sig;
        sig = In.ar(in, 1);
        sig = LeakDC.ar(sig);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0, MXGlobals.levelrate, MXGlobals.switchrate, 0] );
    );
    MXGlobals.synthdefs.add (
      SynthDef(defNameDelay, { arg in=0, out=1, gain=0.0, delay=0.0, on=1, gate=1;      var sig;
        sig = In.ar(in, 1);
        sig = LeakDC.ar(sig);
        sig = DelayC.ar(sig, MXGlobals.delaymax, delay);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        sig = sig * on;
        Out.ar(out, sig);
      }, [0, 0, MXGlobals.levelrate, MXGlobals.delayrate, MXGlobals.switchrate, 0] );
    );
  } 

  *new { arg device, num, in, out, active=0;
    ^super.new.init(device, num, in, out, active);
  }

  init { arg argdevice, argnum, argin, argout, argactive;
    device = argdevice;
    num = argnum;
    in = argin;
    out = argout;
    gain = MXCV( ControlSpec(-20, 20, \lin, 0.5), 0.0);   
    gain.action = { arg changer, what; 
      if ((gain.value != gain.spec.default) || (phase.value != phase.spec.default) || (delay.value != delay.spec.default)) 
        { modified.value = 1 }
        { modified.value = 0 }
    };
    phase = MXCV( ControlSpec(-1, 1, \lin, 1), 1);   
    phase.action = { arg changer, what; 
      if ((gain.value != gain.spec.default) || (phase.value != phase.spec.default) || (delay.value != delay.spec.default)) 
        { modified.value = 1 }
        { modified.value = 0 }
    };
    delay = MXCV( ControlSpec(0, MXGlobals.delaymax * 1000, \lin, 0.1), 0.0);   
    delay.action = { arg changer, what;
      if ((gain.value != gain.spec.default) || (phase.value != phase.spec.default) || (delay.value != delay.spec.default)) 
        { modified.value = 1 }
        { modified.value = 0 }
    };  
    on = MXCV( ControlSpec(0, 1, \lin, 1), 1);   
    active = MXCV( ControlSpec(0, 1, \lin, 1), argactive);  
    active.action = { arg changer, what; 
    //  ("channel activation" + num + changer.value).postln;
      if (changer.value == 1) {
        this.startDSP;
      } {
        this.stopDSP;
      };  
    //  if ( changer.value == 1 ) { this.makeSynth } { this.removeSynth };
    };

    modified = MXCV( ControlSpec(0, 1, \lin, 1), 0);  

  //  ("Device channel added:" + device.name + device.type + num).postln;
  
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
  
  active_ { arg value;   
    if (value != active.value) {
      active.value = value; 
    };
  }
  
  active {
    ^active.value;    
  }

  on_ { arg value;   
    if (value != on.value) {
      on.value = value; 
    };
  }

  makeSynth {
    if (device.function == \sub) { // sub gain is controlled also by main gain !
      if (delay.value > 0.0) {
        synth = Synth.controls(defNameDelay, 
          [   in: in, 
            out: out, 
            on: on,
            gain: [ [gain, device.gain, MXMonitorManager.mainMonitor.gain, phase, device.phase], 
              (gain + device.gain + MXMonitorManager.mainMonitor.gain + device.gainoffset).dbamp * (phase * device.phaseoffset * device.phase) ], 
            delay: [ [delay, device.delay], (delay + device.delay + device.delayoffset) * 0.001],
          ],
          device.group, \addToHead)
      } {
        synth = Synth.controls(defName, 
          [   in: in, 
            out: out, 
            on: on,
            gain: [ [gain, device.gain, MXMonitorManager.mainMonitor.gain, phase, device.phase], 
              (gain + device.gain + MXMonitorManager.mainMonitor.gain + device.gainoffset).dbamp * (phase * device.phaseoffset * device.phase) ],
          ],
          device.group, \addToHead)
      };
    } {
      if (delay.value > 0.0) {
        synth = Synth.controls(defNameDelay, 
          [   in: in, 
            out: out, 
            on: on,
            gain: [ [gain, device.gain, phase, device.phase], 
              (gain + device.gain + device.gainoffset).dbamp * (phase * device.phaseoffset * device.phase) ], 
            delay: [ [delay, device.delay], (delay + device.delay + device.delayoffset) * 0.001],
          ],
          device.group, \addToHead)
      } {
        synth = Synth.controls(defName, 
          [   in: in, 
            out: out, 
            on: on,
            gain: [ [gain, device.gain, phase, device.phase], 
              (gain + device.gain + device.gainoffset).dbamp * (phase * device.phaseoffset * device.phase) ],
          ],
          device.group, \addToHead)
      };
    };
    NodeWatcher.register(synth);
  }

  removeSynth {
    synth.set(\gate, 0);
  }

  remove {
    // this.removeSynth;  
    // this.stopDSP;
    this.active_(0);
  }
/*    
  in_ { arg bus;    // for L R swapping
    in = bus; 
    if (active.value == 1) { 
      fork {
        synth.set(\on, 0); 
        MXGlobals.switchrate.wait;
        synth.set(\in, in);
        MXGlobals.switchrate.wait;
        synth.set(\on, 1); 
      }
    } 
  }
*/
  tempOut_ { arg bus;   // for L R swapping
    if (active.value == 1) { 
      fork {
      //  synth.set(\on, 0); 
      //  MXGlobals.switchrate.wait;
        synth.set(\out, bus);
      //  MXGlobals.switchrate.wait;
      //  synth.set(\on, 1); 
      }
    } 
  }
  

  startDSP { 
    this.makeSynth;
  }
  
  stopDSP {
    this. removeSynth;
  } 

  reset {
    this.gain_(gain.spec.default);  
    this.phase_(phase.spec.default);  
    this.delay_(delay.spec.default);  
    this.on_(1);  
  } 

  getValues {
    var dict = IdentityDictionary.new;
    dict.add( \num    -> num );
    dict.add( \on       -> on.value );
    dict.add( \gain     -> gain.value );
    dict.add( \phase    -> phase.value );
    dict.add( \delay    -> delay.value );
    ^dict;
  }
  
  setValues { arg dict;
    if (dict.includesKey( \on ))      { this.on_(dict[\on]); };
    if (dict.includesKey( \gain ))    { this.gain_(dict[\gain]); };
    if (dict.includesKey( \phase ))     { this.phase_(dict[\phase]) };
    if (dict.includesKey( \delay ))     { this.delay_(dict[\delay])  };
  }


}


MXMonitorProcessor : MXProcessor {
/*  
- class for monitor signal processors 
  - binaural
  - Ambisonics
  - MS Matrix
  - GraphicEQ
  - ParamEQ

*/  
  
}


MXMonitorDevice {
/*
- class for (processing and) output of signals for monitoring
  - stereo nearfield
  - headphones
  - multichannel (incl. sub)
  - WFS
  - etc.
- mixer features: solo + mute, VCA groups
- simple level meter for each channel ?

editable by MXMonitorManager ?
  
*/
  classvar <defNameMono = "MXMonitorMono";
  classvar <defNameLimiter = "MXMonitorLimiter";

  var <name;      // String or Symbol
  var <type = \monitorout;
  var <function = \aux; // Symbol indicating the role of this monitor

  var <>sampleRates;  // Array of Floats, indicating the valid sampleRates of this virtual device 
  var <ioDict;      // Dictionary:   samplingrate -> [ <Array of MXIOs  \out > ]  
  var <busArray;    // Array of (private) MXBusses
  var <channels;    // Array of MXMonitorDeviceChannels
  var <numChannels; // Integer, number of channels / busses of this device
  var <inputs;      // Array of private MXBusses
  var <outputs;     // Array of  MXIOs
  var <inputNums;   // Array of bus numbers
  var <outputNums;  // Array of bus number
  // global:
//  var <target;      // Parent Group Node
  var <group;     // Group (Node) for Channel Synths
  var <monogroup;   // Group (Node) for Synths
  var <crossovergroup;  // Group (Node) for Synths
  var <limitergroup;    // Group (Node) for Synths
  var <active;      // MXCV: [0, 1]  (overwrites channels)
//  var <mute;      // MXCV: [0, 1]  (overwrites channels)
  var <>gainoffset = 0; // Float: device gain in dB, from devices.txt
  var <gain;      // MXCV: gain in dB (offset to channels)
  var <>phaseoffset = 1; // Float: device phase factor [-1, 1], from devices.txt
  var <phase;       // MXCV: [1, 1]  (1 = 180¡)  
  var <>delayoffset = 0;  // Float: device delay in ms, from devices.txt
  var <delay;     // MXCV: delay in ms  (offset to channels)
  var <meter;     // MXSimpleMeter 

//  var <processors;    // Array of MXProcessors or MXMonitorProcessors
//  var <volume;      // MXCV in dBfs
  var <mono;      // MXCV [0, 1]
  var <limiter;     // MXCV [0, 1]
  var <crossover;   // MXCV [0, 1]
  
//  var <soloStates;    // Array, not used
//  var <muteStates;    // Array, not used
  var <swap;    // MXCV [0, 1]
  var <monoSynth;
  var <limiterSynth;
  var <crossoverSynth;
//  var <masterSynth; // Synth (Node) for Monitor volume, limiter, DCkiller, denormals handling etc  

  function_ { arg symbol;
    function = symbol;
    switch (function)
      { \main }   { MXMonitorManager.mainMonitor = this }
      { \sub }    { MXMonitorManager.subMonitor = this }
      { \near }   { MXMonitorManager.nearMonitor = this }
      { \phones } { MXMonitorManager.phonesMonitor = this }
      { \wfs }    { MXMonitorManager.wfsMonitor = this }
    ;
  }

  *new { arg name, type, ioDict;
    ^super.new.init(name, type, ioDict);
  }

  init { arg argname, argtype, argioDict;
    ioDict = argioDict;
    name = argname ? "noname";
    type = argtype;
    // init global CVs
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, MXGlobals.dbstep), -20);   
    phase = MXCV( ControlSpec(-1, 1, \lin, 1), 1);   
    delay = MXCV( ControlSpec(0, MXGlobals.delaymax * 1000, \lin, 0), 0.0);   
  //  mute = MXCV( ControlSpec(0, 1, \lin, 1), 0);   // not used !
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI on/off button
    active.action = { arg changer, what;  
      if (changer.value == 1) {
        this.startDSP(MXMain.monitorManager.group);
      } {
        this.stopDSP;
      };  
    };

    swap = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI L-R Swap button
    swap.action = { arg changer, what;  
      var chL, chR;
      if (numChannels == 2) {
        if (changer.value == 1) {
          chL = channels[0].out;
          chR = channels[1].out;
          channels[0].tempOut_(chR);
          channels[1].tempOut_(chL);
        } {
          chL = channels[0].out;
          chR = channels[1].out;
          channels[0].tempOut_(chL);
          channels[1].tempOut_(chR);
        };  
      };
    };

    mono = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI on/off button
    mono.action = { arg changer, what;  
      if (changer.value == 1) {
        this.makeMonoSynth;
      } {
        if ( monoSynth.isPlaying ) { this.removeMonoSynth };
      };  
    };

    limiter = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI on/off button
    limiter.action = { arg changer, what;  
      if (changer.value == 1) {
        this.makeLimiterSynth;
      } {
        if ( limiterSynth.isPlaying ) { this.removeLimiterSynth };
      };  
      // synchronize sub limiter: 
      if (function == \main) { MXMonitorManager.subMonitor.limiter.value = limiter.value };
    };

    crossover = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // called by GUI on/off button
    crossover.action = { arg changer, what;  
      if (changer.value == 1) {
        this.makeCrossoverSynth;
      } {
        if ( crossoverSynth.isPlaying ) { this.removeCrossoverSynth };
      };  
      // synchronize sub crossover: 
      if (function == \main) { MXMonitorManager.subMonitor.crossover.value = crossover.value };
    };
    
//    this.makeIOs; // immer auch beim Wechsel der sampleRate !
//    this.makeChannels;
    ("Monitor device added:" + type + name).postln; 
  }

  setSR { // to be called by the device manager when sampleRate is known and server has been booted
    var srspeed, ioArray;
    srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];

    ioArray = ioDict[ srspeed ];
    // make busArray for current samplerate
    busArray = ioArray.collect {arg io, i;  MXDeviceManager.getNewBus };
    numChannels = ioArray.size;
    inputs = busArray;  // Array of MXBusses 
    outputs = ioArray;    // Array of MXOutputs

    inputNums = inputs.collect(_.busNum);
    outputNums = outputs.collect(_.busNum);
    
    // remove existing channels and meter before??
    this.makeChannels;
    // meter = MXSimpleMeter(busArray.collect(_.busNum), group);
  }
  
  unsetSR { // to be called before sample rate changes
    // meter.remove;
    this.removeChannels;
    busArray.do(_.free);
    busArray.clear;
    inputs.clear;
    outputs.clear;
  }
  
  makeChannels {
    // make an inactive MXMonitorDeviceChannel for each MXIO !
    channels = inputNums.collect {arg bus, i; MXMonitorChannel(this, i, bus, outputNums[i], active.value) };
  }

  removeChannels {
    channels.do(_.remove);
    channels.clear;
  } 

  makeMonoSynth {
    monoSynth = SynthDef(name + "mono", { arg gate=1;     var sig, mono;
      sig = In.ar(outputNums);
      mono = sig.asArray.sum;
      mono = mono * sig.size.reciprocal.sqrt;
      mono = mono * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
      outputNums.collect({ arg bus, i; ReplaceOut.ar(bus, mono) });
    }, [0] ).play(monogroup);
    NodeWatcher.register(monoSynth);
  } 

  removeMonoSynth {
    monoSynth.set(\gate, 0);  
  //  monoSynth = nil;
  }

  makeLimiterSynth {
    limiterSynth = SynthDef(name + "limiter", { arg gate=1;     var sig;
  //    sig = In.ar(channels.collect(_.out));
      sig = In.ar(outputNums);
      sig = Limiter.ar(sig, MXGlobals.limiterLevel, MXGlobals.limiterDur);
      sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
      outputNums.collect({ arg bus, i; ReplaceOut.ar(bus, sig[i]) });
    }, [0] ).play(limitergroup);
    NodeWatcher.register(limiterSynth);
  } 

  removeLimiterSynth {
    limiterSynth.set(\gate, 0); 
  //  limiterSynth = nil;
  }

  makeCrossoverSynth {
    // 4th order Butterworth low- or hi-pass filter  (just 2 cascaded second order LPF or HPF) 
    // 2 cascaded LPF in parallel to 2 cascaded HPF seem to reconstruct the original signal when mixed in the same channel 
    // but what about group delay?
    // FIR filters are better ? (-> FFT ?)
    // or Linkwitz-Riley-Filters from wslib ???
    crossoverSynth = SynthDef(name + "crossover", { arg gate=1;     var sig;
      sig = In.ar(outputNums);
      if (function == \sub) {
        2.do { sig = LPF.ar(sig, MXGlobals.crossoverFrequency) }
      } {
        2.do { sig = HPF.ar(sig, MXGlobals.crossoverFrequency) }
      };
      sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
      outputNums.collect({ arg bus, i; ReplaceOut.ar(bus, sig[i]) });
    }, [0] ).play(crossovergroup);
    NodeWatcher.register(crossoverSynth);
  } 

  removeCrossoverSynth {
    crossoverSynth.set(\gate, 0); 
  //  crossoverSynth = nil;
  }

  remove {
    // free synths
    // free group   
  }

/*
  addchannel { arg mxio;
    // gets a new mxbus
  //  ioDict = ioDict.add(mxio);
    busArray = busArray.add( MXDeviceManager.getNewBus );
    // new synth
    MXDeviceChannel(this, this.inputnumbers.size - 1, this.inputnumbers.last, this.outputnumbers.last, active.value)
    // new meter  
  }
  
  removechannel { arg channel;
    // remove mxio and mxbus
    // remove mxdevicechannel
  }
*/  
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
      delay.value = value;  
    }
  }
  
  active_ { arg value;   
    if (value != active.value) {
      active.value = value; 
    };
  }

  swap_ { arg value;   
    if (value != swap.value) {
      swap.value = value; 
    }
  }
  
  mono_ { arg value;    
    if (value != mono.value) {
      mono.value = value; 
    }
  }

  limiter_ { arg value;   
    if (value != limiter.value) {
      limiter.value = value;  
    }
  }

  crossover_ { arg value;   
    if (value != crossover.value) {
      crossover.value = value;  
    }
  }


/*  mute_ { arg value;  // called by GUI mute button
    mute.value = value; 
  }
*/
  startDSP { arg target;  // called by GUI-activation via this.active MCV 
  //  this.target = target;
  //  target = target;
    group = Group(target);
    monogroup = Group.tail(group);
    crossovergroup = Group.tail(group);
    limitergroup = Group.tail(group);
    {   
      channels.do {arg chan;  chan.active_(1) };
    //  meter.active_(1);
    }.defer( MXGlobals.switchrate + 0.01 );
  } 
  
  stopDSP { // called by GUI-activation via this.active MCV 
    // mono and limiter ?? >> auch deaktvieren !
    if (channels.size > 1) {
      swap.value = 0;
      mono.value = 0;
    };
    limiter.value = 0;
    crossover.value = 0;
    // meter.active_(0);
    // remove group and channel synths  
    channels.do {arg chan;  chan.active_(0) };
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  reset {
    channels.do(_.reset); 
    this.active_(1);  
    this.gain_((function == \sub).if(-20, -20));  
    this.phase_(phase.spec.default);  
    this.delay_(delay.spec.default);  
    this.swap_(swap.spec.default);  
    this.mono_(mono.spec.default);  
    this.limiter_(limiter.spec.default);  
    this.crossover_(crossover.spec.default);  
    MXGUI.selectedSpeaker.value = 0;  // schnelle Notloesung ...
  }
  
  getValues {
    var dict = IdentityDictionary.new;
    dict.add( \name     -> name );
    dict.add( \function   -> function );
    dict.add( \channels -> channels.collect({ arg ch, i; (i -> ch.getValues) }) );
    dict.add( \active     -> active.value );
    dict.add( \gain     -> gain.value );
    dict.add( \phase    -> phase.value );
    dict.add( \delay    -> delay.value );
    dict.add( \swap     -> swap.value );
    dict.add( \mono     -> mono.value );
    dict.add( \limiter    -> limiter.value );
    dict.add( \crossover  -> crossover.value );
    ^dict;
  }
  
  setValues { arg dict;
    if (dict.includesKey( \active ))    { this.active_(dict[\active]); };
    if (dict.includesKey( \gain ))    { this.gain_(dict[\gain]); };
    if (dict.includesKey( \phase ))     { this.phase_(dict[\phase]) };
    if (dict.includesKey( \delay ))     { this.delay_(dict[\delay])  };
    if (dict.includesKey( \swap ))    { this.swap_(dict[\swap])  };
    if (dict.includesKey( \mono ))    { this.mono_(dict[\mono])  };
    if (dict.includesKey( \limiter ))     { this.limiter_(dict[\limiter])  };
    if (dict.includesKey( \crossover ))   { this.crossover_(dict[\crossover])  };
    if (dict.includesKey( \channels ))  { dict[\channels].do({ arg assoc;
                      var num, chdict;
                      num = assoc.key;
                      chdict = assoc.value;
                      if( num < numChannels ) {
                        channels[num].setValues(chdict);
                      } {
                        ("monitor channel" + num + "doesn't exist !").warn;
                      };
                    })
                  };
  }

}




MXMonitorManager { // singleton !
/*
- scannt beim Start folder mit XMLs fuer MXDevices fuer die aktuelle Samplingrate und legt entsprechende Objekte an -> macht MXDeviceManager !
- stellt Editor fuer bestehende und neue MXDevices bereit:
  erzeugen, kopieren und loeschen von MXDevices
  laden und speichern von einzelnen MXDevices
  editieren von MXDevices (channels/MXIOs, name)
  GUI (active, gain, phase, delay) und MXSimpleMeter anzeigen
*/

  classvar <devices;    // Array of MXMonitorDevices
  classvar <group;
  classvar <>mainMonitor; // MXMonitorDevice, main monitor device (i.e. "Meyer 1-12")
  classvar <>subMonitor;    // MXMonitorDevice, sub monitor device
  classvar <>nearMonitor; // MXMonitorDevice, nearfield monitor device
  classvar <>phonesMonitor; // MXMonitorDevice, headphones monitor device
  classvar <>wfsMonitor;  // MXMonitorDevice, WFS monitor device

  *init {
    devices = List.new;
  }
    
  *addDevice {arg device;
    devices.add(device);
  }
  
  *setSR {
    "MonitorManager-setSR".postln;    
    devices.do {arg d; d.setSR };  
  }
  
  *unsetSR {
    devices.do {arg d; d.unsetSR };  
  }
    
  *startDSP { arg target; 
    "MonitorManager-startDSP".postln;
    group = target;
    devices.do {arg d;   d.active_(1) };
    this.connectMainMonitorToMIDI;
    this.connectMonitorsToMIDI;
  } 

  *stopDSP {  // ??
    // remove group, devices
    devices.do {arg d;   d.active_(0) };  // schon getestet ?

    { group.free }.defer( MXGlobals.switchrate + 0.01 );
    this.disconnectFromMIDI;
  }

  *reset {
    devices.do {arg d;   d.reset };
  } 

  *connectMainMonitorToMIDI {
//  MXMIDI.connectControlToMIDI(gain, ("devicevolume" ++ changer.value.asString).asSymbol);
    MXMIDI.connectControlToMIDI(mainMonitor.gain, \monitorvolume1, mainMonitor.name );
    MXMIDI.connectControlToMIDI(mainMonitor.active, \monitormute1, mainMonitor.name, inverted: true);
    mainMonitor.channels.do { arg ch, i;
      i = i + 1; 
      MXMIDI.connectControlToMIDI(ch.gain, ("mainchannelgain" ++ i.asString).asSymbol, "Spkr" ++ i);
      MXMIDI.connectControlToMIDI(ch.on, ("mainchannelmute" ++ i.asString).asSymbol, "Spkr" ++ i, inverted: true);
      MXMIDI.connectControlToMIDI(ch.phase, ("mainchannelphase" ++ i.asString).asSymbol, "Spkr" ++ i, inverted: true);
    };
  }

  *connectMonitorsToMIDI {
    MXMIDI.connectControlToMIDI(nearMonitor.gain, \monitorvolume3, \Nearfd);
    MXMIDI.connectControlToMIDI(nearMonitor.active, \monitormute3, nearMonitor.name, inverted: true);
    MXMIDI.connectControlToMIDI(phonesMonitor.gain, \monitorvolume4, \Phones);
    MXMIDI.connectControlToMIDI(phonesMonitor.active, \monitormute4, phonesMonitor.name, inverted: true);
    MXMIDI.connectControlToMIDI(subMonitor.gain, \monitorvolume5, \Sub);
    MXMIDI.connectControlToMIDI(subMonitor.active, \monitormute5, subMonitor.name, inverted: true);
    
      
  }
  
  *disconnectFromMIDI {
    MXMIDI.disconnectControlFromMIDI(mainMonitor.gain);
    MXMIDI.disconnectControlFromMIDI(mainMonitor.active);
    mainMonitor.channels.do { arg ch, i;
      MXMIDI.disconnectControlFromMIDI(ch.gain);
      MXMIDI.disconnectControlFromMIDI(ch.on);
    };
    MXMIDI.disconnectControlFromMIDI(nearMonitor.gain);
    MXMIDI.disconnectControlFromMIDI(nearMonitor.active);
    MXMIDI.disconnectControlFromMIDI(phonesMonitor.gain);
    MXMIDI.disconnectControlFromMIDI(phonesMonitor.active);
    MXMIDI.disconnectControlFromMIDI(subMonitor.gain);
    MXMIDI.disconnectControlFromMIDI(subMonitor.active);
  }

  *getValues {
    var dict = IdentityDictionary.new;
    dict.add( \monitorDevices -> devices.collect({ arg dev, i; (dev.name -> dev.getValues) }) );
    ^dict;
  }
  
  *setValues { arg dict;
    if (dict.includesKey( \monitorDevices )) { 
      dict[\monitorDevices].do({ arg assoc;
        var devname, devdict;
        var index;
        devname = assoc.key;
        devdict = assoc.value;
        index = devices.detectIndex({ arg dev;  dev.name == devname });
        if (index.notNil) { 
          devices[index].setValues(devdict); 
        } {    
          ("monitor device" + devname + "doesn't exist !").warn;
        };
      }); 
    };
  }
  
}

