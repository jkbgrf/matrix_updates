/*

real SC InputBus > MXInterface input > [ MXInput > MXInDevice > MXBus ]   

>>   [MXBus > MXConnection (MXNode(s)) > MXBus ] >>

[ MXBus > MXInDevice > MXOutput] > MXInterface output > real SC OutputBus


*/


MXInterface {
/*  
class for audio Interfaces and Formatconverters connected to the general audio interface (MADI)
  AD + DA converters, AES/EBU, SPDIF, TDIF, ADAT, DANTE (audio over IP) etc.
  MADI-Ports connected to the studio without using a formatconverter are also regarded as an MXInterface, i.e. MADI ports connected to MADI on another computer

compensates per synth nodes:
  different latencies (in samples), 
  in/output levels releated to dBfs (as gain), 
  phase (1, -1) (as gain sign)
  
also holds info about:
  available samplingrates
  I/O busNumbers per samperate
    
non-editable, reads all settings as xml file from disk 

*/  
  classvar <defName = "MXInterface";  
  classvar <defNameDelay = "MXInterfaceDelay";

// parameters provided by XML file:
  var <type;          // Symbol: [\in, \out]
  var <name;      // String or Symbol, name of the real device (i.e. "SSL MADI AX analog", "MADI 1A" etc.)
  var <via;       // String or Symbol, name of the real device between this device and the computer (i.e. "MADI 1A"), typically for converters connected via an interface  
  var <>format = "MADI";   // String or Symbol, describing the converted format (i.e. "AD", "DA", "ADAT", "MADI", "AES" etc.)
  var <>gain = 0.0;   // Float: analog level related to 0 dBfs (typically +15, +18, +22, +24 etc. dBu)
  var <>phase = 1;    // Integer: [1, -1]
  var <>latency = 0;  // Integer:  Audiosamples
  var <>sampleRates;  // Array of Floats [44100.0, 48000.0, 96000.0 ... ]
  var <>busNums;    // Dictionary:   samplingrate -> [ <array of busNumbers> ]  (number according to the OS audio driver, zero-based)
//  var <inputs;      // Dictionary:   samplingrate -> [ <array of inputbusNumbers> ]  (number according to the OS audio driver, zero-based)
//  var <outputs;     // Dictionary:   samplingrate -> [ <array of outputbusNumbers> ]

// objects provided by the program: 
  var <group;     // Node: Server Group for synth nodes (gain, latency, phase compensation)
//  var <inGroup;   // Node: Server Group for input synth nodes
//  var <outGroup;    // dito
  var <synths;      // Array of Synth nodes (one for each bus, for gain/phase/latency adjustment) for inputs, to be put into inGroup
//  var <insynths;    // Array of Synth nodes (one for each bus, for gain/phase/latency adjustment) for inputs, to be put into inGroup
//  var <outsynths;   // dito

  *initClass {  
  /*     // single channel synthdefs now replaced by multichannel synthdefs (one per MXInterface) ! 
    MXGlobals.synthdefs.add (
      SynthDef(defName, { arg bus=0, gain=1.0, gate=1;        var sig;
        sig = In.ar(bus, 1);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate);
        ReplaceOut.ar(bus, sig);
      }, [\ir, \ir, 0] );
    );
    MXGlobals.synthdefs.add (
      SynthDef(defNameDelay, { arg bus=0, gain=1.0, latency=0, gate=1;        var sig;
        sig = In.ar(bus, 1);
        sig = DelayN.ar(sig, 0.01, latency * SampleDur.ir);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate);
        ReplaceOut.ar(bus, sig);
      }, [\ir, \ir, \ir, 0] );
    );
  */
  } 
  
  *new {arg type, name;
    ^super.new.init(type, name)
  }
  
  init {arg argtype, argname;
    type = argtype;
    name = argname;
    sampleRates = [ ];  
    busNums = Dictionary.new; 
    // hier noch MXCVs fuer gain etc. !!!
    ("Interface added:" + type + name).postln;  
  }

/*  // old multiple singel channel synths:
  startDSP { arg target;
    var delay;
    group = Group(target, \addToTail);
    // start only synths for interfaces with gain != 0 or phase != 1 or delay != 0 !!
    if ( (gain != 0.0) || (phase != 1) || (latency > 0) ) {
      if (latency > 0) { 
        synths = busNums[ MXGlobals.srspeeds[ MXGlobals.sampleRate ] ].collect({ arg bus;
          Synth(defNameDelay, [bus: bus, gain: gain.dbamp * phase, latency: latency], group);
        })
      }  {  
        synths = busNums[ MXGlobals.srspeeds[ MXGlobals.sampleRate ] ].collect({ arg bus;
          Synth(defName, [bus: bus, gain: gain.dbamp * phase], group);
        })
      }
    }  
  }
*/
  // new multichannel synth:
  startDSP { arg target;
    var delay;
    group = Group(target, \addToTail);
    // start only synths for interfaces with gain != 0 or phase != 1 or delay != 0 !!
    if ( (gain != 0.0) || (phase != 1) || (latency > 0) ) {
      if (latency > 0) { 
        synths = SynthDef(name.asString + type.asString, { arg gain=1.0, gate=1;          var sig, bus;
          bus = busNums[ MXGlobals.srspeeds[ MXGlobals.sampleRate ] ];
          sig = In.ar(bus);
          sig = DelayN.ar(sig, 0.01, latency * SampleDur.ir);
          sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate);
          bus.do { arg out, i; ReplaceOut.ar(out, sig[i]) };
          // ReplaceOut.ar(bus, sig);
        }, [\ir, 0] ).play(group, [ \gain, gain.dbamp * phase ]);
      } {
        synths = SynthDef(name.asString + type.asString, { arg gain=1.0, gate=1;          var sig, bus;
          bus = busNums[ MXGlobals.srspeeds[ MXGlobals.sampleRate ] ];
          sig = In.ar(bus);
          sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate);
          bus.do { arg out, i; ReplaceOut.ar(out, sig[i]) };
          // ReplaceOut.ar(bus, sig);
        }, [\ir, 0] ).play(group, [ \gain, gain.dbamp * phase ]);
      };
      synths = synths.asArray;  // backward compat.
    } { synths = [ ] };    
  }
  
  stopDSP {
    synths.do(_.set(\gate, 0));
    { group.free }.defer( MXGlobals.switchrate + 0.01 );  } 
  
}

/*
MXInInterface : MXInterface {
  // represents an input interface
  type {
    ^\in  
  }
}

MXOutInterface : MXInterface {
  // represents an output interface
  type {
    ^\out 
  }
}
*/



MXIO  {
/*
- represents and manages a single I/O bus connected to an interface
- maps a physical input (real core audio device number, opt. via a MXInterface) to a logical input (only bus numbers of available devices are counted)
- provides the name of the interface/converter and its physical input or output number, i.e. "SSL MADI AX analog in 3"
- names and logical numbers are used for display in matrices, MXInput- and MXOutputDevices, meters etc.
- 
- SC busNumbers are used for nodes ??

non-editable, will be generated by MXIOManager according to the available MXInterfaces at (re-)start time

*/
  var <busNum ; // Integer: SC audio input- or outputbus number >> MXCV (Integer) ??
  var <num;     // Integer: logical bus number provided by the MXIOManager (inputs start at 0, all MXInterfaces are continously numbered without gaps)
  var <type;      // Symbol: [\in, \out]
  var <interfaceNum;  // Integer: I/O number of the device connected to this bus (1 based)
  var <interfaceName; // String or Symbol, name of the associated MXInterface (product + format (analog, ADAT, MADI, AES etc.) )
  var <fullName;    // String or Symbol, interfaceName + type + (logical) num
  
  *new { arg type, busNum, num, interface, interfaceNum;
    ^super.new.init(type, busNum, num, interface, interfaceNum)
  }
  
  init { arg argtype, argbusNum, argnum, arginterface, arginterfaceNum;
    type = argtype;
    busNum = argbusNum;
    num = argnum;
    interfaceName = arginterface;
    interfaceNum = arginterfaceNum;
    fullName = interfaceName + type.asString + interfaceNum;
  }
  
}


/*
MXInput : MXIO {
  type {
    ^\in  
  }
}

MXOutput : MXIO {
  type {
    ^\out 
  }
}
*/


MXIOManager { // singleton !
/*
- scannt beim Start folder mit XMLs fuer MXInterfaces und legt entsprechende Objekte an
- erzeugt fuer jedes MXInterface und aktuelle Samplingrate n MXInputs und -Outputs
- 
non-editable,  reads all settings as xml files from disk

*/
  classvar <inInterfaces;   // List of MXInInterfaces
  classvar <outInterfaces;  // List of MXOutInterfaces
//  classvar <inputs;         // List of MXIOs 
  classvar <inputs;     // Dictionary:   samplingrate -> [ <List of MXIOs> ]  classvar <outputs;    // List of MXIOs
  classvar <outputs;    // Dictionary:   samplingrate -> [ <List of MXIOs> ] 
  classvar <inputsOrder;  // Dictionary:   samplingrate -> Order:  busNum -> MXIO
  classvar <outputsOrder; // Dictionary:   samplingrate -> Order:  busNum -> MXIO
  classvar <inputsCounter = 0;
  classvar <outputsCounter = 0;
  
  classvar <connectors; // Dictionary:  interface -> [ <List of MXIOs (\in) >, <List of MXIOs (\out)> ] 

  classvar <inGroup;  // Node: target for server group for MXInInterfaces input synth nodes
  classvar <outGroup; //
  classvar <swapCV;   // MXCV [0, 1] for swapping MADI cards
  classvar <swapInGroup, swapOutGroup;

  *init {
    MXGlobals.synthdefs.add (
      SynthDef("out_swap_node", { arg gate=1;
        var sig0, sig128, env;
        env = Linen.kr(gate, 0.1, 1, 0.1, doneAction: 2);
        sig0 = In.ar(0, 128) * env;
        sig128 = In.ar(128, 128) * env;
        ReplaceOut.ar(0, sig128 ++ sig0);
      //  ReplaceOut.ar(128, sig0);
      }));  
    MXGlobals.synthdefs.add (
      SynthDef("in_swap_node", { arg gate=1;
        var sig0, sig128, env;
        env = Linen.kr(gate, 0.1, 1, 0.1, doneAction: 2);
        sig0 = In.ar(MXGlobals.numOutputs, 128) * env;
        sig128 = In.ar(MXGlobals.numOutputs + 128, 128) * env;
        ReplaceOut.ar(MXGlobals.numOutputs, sig128 ++ sig0);
      //  ReplaceOut.ar(128, sig0);
      }));
    inInterfaces = List.new;
    outInterfaces = List.new;
    swapCV = MXCV( [0,1].asSpec, 0);
    swapCV.action = { arg changer;
      swapInGroup.set(\gate, 0);
      swapOutGroup.set(\gate, 0);
    //  gui.swap.value = changer.value;
      if (changer.value == 1) { 
        Synth("in_swap_node", nil, swapInGroup); 
        Synth("out_swap_node", nil, swapOutGroup); 
      };  
    };

    this.readConfig;
    this.makeIOs; 
  //  this.setSR;  // immer beim Start sowie beim Wechsel der sampleRate !!
  }

  *readConfig {
    var path, arrayfromfile, dict;
    path = MXGlobals.configDir ++ "converters.txt";
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
        dict[\inputs].do {arg assoc, i;
          this.addInterfaceFromArray(\in, [assoc.key] ++ assoc.value );
        }
      } { 
        "WARNING: no input converters found in file".postln;
      };
      if (dict.includesKey( \outputs )) { 
        dict[\outputs].do {arg assoc, i;
          this.addInterfaceFromArray(\out, [assoc.key] ++ assoc.value );
        }
      } { 
        "WARNING: no output converters found in file".postln;
      };
    } {
      "FILE ERROR: converters.txt not found!".postln; 
    };    
  } 
  
  *addInterfaceFromArray { arg type, array;
    var interface;
    var name, format, gain, phase, latency, sampleRates, busNums;
    name = array[0].asString;
    format = array[3].asString;
    gain = array[4].asFloat;
    latency = array[5].asInteger;
    phase = 1;
    sampleRates = [44100.0, 48000.0, 88200.0, 96000.0];
    busNums = Dictionary.new;
    busNums.add( 48000.0 -> (( array[1][0] .. array[1][1] ) -1) );
    busNums.add( 96000.0 -> (( array[2][0] .. array[2][1] ) -1) );
    if (type == \in) {
      busNums[48000.0] = busNums[48000.0] + MXGlobals.numOutputs;
      busNums[96000.0] = busNums[96000.0] + MXGlobals.numOutputs;
    };
    interface = MXInterface(type, name);
    interface.format = format.asSymbol;
    interface.gain = gain;
    interface.latency = latency;
    interface.phase = phase;
    interface.sampleRates = sampleRates;
    interface.busNums = busNums;
    
    switch (type)
      {\in}   { inInterfaces.add(interface) }
      {\out}  { outInterfaces.add(interface) }
    ;
  }
  
  *setSR {
    "IOManager-setSR".postln;
    // this.makeIOs; 
  }

  *unsetSR {
    "IOManager-unsetSR".postln;
    // this.removeIOs; 
  }
    
  *makeIOs {
    var mxio;
    var srspeed;
    inputs = Dictionary.new;
    outputs = Dictionary.new;
    inputsOrder = Dictionary.new;
    outputsOrder = Dictionary.new;
    connectors = Dictionary.new;  // ?? 
  //  srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];
    [48000.0, 96000.0].do({ arg sr, i;
      inputsCounter = 0;
      outputsCounter = 0;
      inputs.add( sr -> List.new );
      outputs.add( sr -> List.new );
      inputsOrder.add( sr -> Order.new );
      outputsOrder.add( sr -> Order.new );
      inInterfaces.do {arg d, i; 
        d.busNums[ sr ].do { arg busNum, b;
          mxio = MXIO(\in, busNum, inputsCounter, d.name, b+1);
          inputsCounter = inputsCounter + 1;
          inputs[sr].add(mxio);
          inputsOrder[sr][busNum] = mxio;
          // "Input added".postln;
        }
      };
      outInterfaces.do {arg d, i; 
        d.busNums[ sr ].do { arg busNum, b;
          mxio = MXIO(\out, busNum, outputsCounter, d.name, b+1);
          outputsCounter = outputsCounter + 1;
          outputs[sr].add(mxio);
          outputsOrder[sr][busNum] = mxio;
        }
      };
    });   
  }
  
  *removeIOs {
    connectors.clear; // ??
    inputs.clear;
    outputs.clear;
    inputsCounter = 0;
    outputsCounter = 0;
  } 
  
  *startDSP { arg inTarget, outTarget; 
    "IOManager-startDSP".postln;
    inGroup = inTarget;
    outGroup = outTarget;
    swapInGroup = Group(inGroup, \addBefore);
    swapOutGroup = Group(outGroup, \addAfter);
    { swapCV.touch }.defer(0.1);
    inInterfaces.do {arg d;  d.startDSP(inGroup) };
    outInterfaces.do {arg d;  d.startDSP(outGroup) };
  }
  
  *stopDSP {  
    "IOManager-stopDSP".postln;
    inInterfaces.do {arg d;   d.stopDSP };
    outInterfaces.do {arg d;   d.stopDSP };
    { [inGroup, outGroup, swapInGroup, swapOutGroup].do(_.free) }.defer( MXGlobals.switchrate + 0.01 ); }
    
  *reset {
    
  } 

}

