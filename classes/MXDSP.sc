
MXNode { // was AMXMatrixNode
/*  
- class represents a single signal connection between two private busses (MXBus) ( = a "matrix node" )

- gain, de/activation

*/  
  classvar <defname = "MXNode";
  
  var <target;        // Parent Group Node
  var <frombus;     // MXBus
  var <tobus;       // MXBus
  var <gain;        // MXCV: gain in dB  
  var <active;        // MXCV: [0, 1] 
  var <synth;       // Synth Node
  
  *initClass {
    MXGlobals.synthdefs.add (
      SynthDef(defname, { arg in=0, out=1, gain=0.0, gate=1;      var sig;
        sig = In.ar(in, 1);
        sig = sig * gain * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
        Out.ar(out, sig);
      }, [\ir, \ir, MXGlobals.levelrate, 0] )
    )
        
  } 
  
  *new { arg target, frombus, tobus, gain=0.0;
    ^super.new.init(target, frombus, tobus, gain);
  }
  
  init { arg argtarget, argfrombus, argtobus, arggain;
    target = argtarget;
    frombus = argfrombus;
    tobus = argtobus;
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, 0), arggain ? 0.0);
    active = MXCV( ControlSpec(0, 1, \lin, 1), 1);
  //  active.action = { arg changer, what; { ["active", frombus.num, tobus.num , changer.value, what].postln  }.defer };
    synth = Synth.controls(defname, [
      in: frombus.busNum, out: tobus.busNum, gain: [[gain, active], gain.dbamp * active]
      ], target);
  }
  
  remove {
    synth.set(\gate, 0);
    gain.releaseDependants;
    active.releaseDependants;
  }
  
  gain_ { arg value;  // Float in dB
    gain.value = value;
  }
  
  active_ { arg value;   // Boolean or Integer {0, 1}
    active.value = value.asInteger;
  }
  
  mute { 
    active.value = 0; 
  }

  unmute { 
    active.value = 1; 
  }

  
}


MXMatrix {  
/*
- represents the nodes of the global Matrix (and others like MicPre-Matrix, TestGen-Matrix ?)


*/    
  var <target;        // Parent Group Node
  var <addAction = \addToHead;
  var <group;       // Group Node
  var <fromBusArray;    // Array of MXBusses
  var <toBusArray;      // Array of MXBusses
  var <>inLabels;     // Array of Devicenames and -sizes for labeling
  var <>outLabels;      // Array of Devicenames and -sizes for labeling
  var <>inLabels2;      // Array of [Devicename, channel] for every single input channel
  var <>outLabels2;   // Array of [Devicename, channel] for every single output channel
  var <nodes;       // Dictionary, [frombus, tobus| -> MXNode
  var <nodeLabels;      // Dictionary [fromindex, toindex] -> [ fromlabel, fromchannel, tolabel, tochannel, gain ], 
              // used for restoring nodes from setup files
  var <gain;        // MXCV: gain in dB (global to all MXNodes)
  var <active;        // MXCV: [0, 1]   (global to all MXNodes)

  *new { arg fromBusArray, toBusArray;
    ^super.new.init(fromBusArray, toBusArray);
  }

  init { arg argfromBusArray, argtoBusArray;
    fromBusArray = argfromBusArray;
    toBusArray = argtoBusArray;
    
    nodes = Dictionary.new;
    nodeLabels = Dictionary.new;
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, 0), 0.0);  // + action ??
  //  gain.action = { arg changer, what;  nodes.do {Êarg node;  node.gain_(changer.value) } };
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // + action ??
    active.action = { arg changer, what;  nodes.do {Êarg node;  node.active_(changer.value) } };

    this.activate;
  
  }

  arrayconnect { arg array;
    var from, to, gain = 0.0;
    array.do { arg conn, i;
      from = fromBusArray[conn[0] - 1];
      to = toBusArray[conn[1] - 1];
      if ( conn.size > 2 ) { gain = conn[2] };
      this.addNode( from, to, gain );
    }
  }
      
  removeNodes {
  // disconnects all nodes and removes all resources
    nodes.do(_.remove);
    nodes.clear;
  //  group.free;
    nodeLabels.clear;
  }
  
  addNode { arg frombus, tobus, gain=0.0;
    var node;
    node = MXNode(group, frombus, tobus, gain);
    nodes.add( [frombus, tobus] -> node);  // key: Array with frombus and tobus objects !
  }
  
  removeNode { arg frombus, tobus;
    var key = [frombus, tobus];
    nodes.at(key).remove; // kill node contents
    nodes.removeAt(key); // remove node from dict
  }
  
  markNode { arg frombus, tobus, bool;
    var from, to;
    from = fromBusArray.indexOf(frombus);
    to = toBusArray.indexOf(tobus);
    if (from.notNil && to.notNil) {
      { MXGUI.matrixView.grid.markNode( from, to, bool ); }.defer // schnelle unsaubere Notloesung ....
    };
  }
    
  
  setNodeByIndex {Êarg from, to, gain;
    var frombus, tobus;
    frombus = fromBusArray[from];
    tobus = toBusArray[to];
    case 
      { (gain > -90) && nodes[ [frombus, tobus] ].isNil } { 
        this.addNode(frombus, tobus, gain);
        nodeLabels.add( [from, to] -> [inLabels2[from], outLabels2[to], gain] );
         }
      { (gain > -90) && nodes[ [frombus, tobus] ].notNil } { 
        nodes[ [frombus, tobus] ].gain_(gain);
        nodeLabels[ [from, to] ][2] = gain;
        }
      { (gain <= -90) && nodes[ [frombus, tobus] ].notNil } {
        this.removeNode(frombus, tobus);
        nodeLabels.removeAt( [from, to] );
        }
      ;
  }
  
  mute {
    // new nodes don't get muted while mute button is on !
    // needs a MXCV or at least a flag for mute state !
    nodes.do(_.mute); 
  }
  
  unmute {
    nodes.do(_.unmute); 
  }
  
  activate {
    if (nodes.size > 0) { active.value = 1 };
  }
  
  deactivate {
    if (nodes.size > 0) { active.value = 0 };
  }
  
  gain_ { arg value;  // Float in dB
    gain.value = value;
  }

  setSR {

  }

  unsetSR {
    
  }
  
  startDSP { arg target; 
    group = Group(target, addAction);

  }   

  stopDSP {  // ??
    this.removeNodes;
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  reset {
    this.removeNodes;
  } 
  
  getValues {
    var dict = IdentityDictionary.new;
    dict.add( \active     -> active.value );
    dict.add( \gain     -> gain.value );
    dict.add( \nodes    -> nodeLabels )  // statt busnums je in und out: device-name und channel, sowie gain und active
    ^dict;
  }
  
  setValues { arg dict;
    this.reset;
    if (dict.includesKey( \nodes )) { dict[\nodes].keysValuesDo({ arg key, array, i;
      var from, to, gain;
      from = inLabels2.indexOfEqual( array[0] ); // -1 ??
      to = outLabels2.indexOfEqual( array[1] ); // -1 ??
      if ( from.notNil && to.notNil ) { 
        gain = array[2];
        this.setNodeByIndex(from, to, gain);
        MXGUI.matrixView.grid.setState_( from, to, (gain > -90).if( { gain.dbamp }, 0.0 ) );  // schnelle unsaubere Notloesung ....
      }
    })};
  }

  
/*
  getValues {
    var dict = IdentityDictionary.new;
    dict.add( \name     -> name );
    dict.add( \level    -> level.value );
    dict.add( \usedNodes  -> usedNodes );
    dict.add( \isActive   -> isActive );
    
    ^dict;
  }
  
  setValues { arg dict;
    var inbus, outbus, label, num;
  
    if (dict.includesKey( \level ))       { level.value = dict[\level]; };
    if (dict.includesKey( \usedNodes ))     { 
      //  usedNodes.add( address -> [level, [inlabel, insubnum], [outlabel, outsubnum]] );
      //  inChannels.add( label -> busArray.collect {arg bus, i; [bus, inBusArray[i], inputLabels[i] ] } );
      usedNodes = IdentityDictionary.new;
      dict[\usedNodes].do {Êarg array;
        label = array[1][0];
        num = array[1][1];
        inbus = manager.inChannels[label][num][0];
        //[name, label, num, inbus].post;
        label = array[2][0];
        num = array[2][1];
        outbus = manager.outChannels[label][num][0];
        //[name, label, num, outbus, array[0]].postln;
        this.addNode( inbus, outbus, array[0]);
      }
    };  
    if (dict.includesKey( \isActive )) { 
      if (dict[\isActive]) {Êthis.activate };
    };  
  
  }
*/


}


MXConnection { // was AMXMatrixGroup
/*
- represents a connection (a number of MXNodes = a local matrix) between two MXDevices


*/    
  var <target;        // Parent Group Node
  var <addAction = \addToHead;
  var <autoconnected = false; // Booloean
  var <group;       // Group Node
  var <fromdevice;      // MXDevice or MXInDevice or MXProcessor
  var <todevice;      // MXDevice, MXOutDevice, MXMonitorDevice or MXProcessor
  var <nodes;       // Array of MXNodes
  var <gain;        // MXCV: gain in dB (global to all MXNodes)
  var <active;        // MXCV: [0, 1]   (global to all MXNodes)

  *new { arg target, fromdevice, todevice, routing; // auto = true;
    ^super.new.init(target, fromdevice, todevice, routing);
  }

  init { arg argtarget, argfromdevice, argtodevice, argrouting; // argauto;
    var routing;
    target = argtarget;
    fromdevice = argfromdevice;
    todevice = argtodevice;
    
    group = Group(target, addAction);
    nodes = Dictionary.new;
    gain = MXCV( ControlSpec(MXGlobals.dbmin, MXGlobals.dbmax, \lin, 0), 0.0);  // + action ??
    gain.action = { arg changer, what;  nodes.do {Êarg node;  node.gain_(changer.value) } };
    active = MXCV( ControlSpec(0, 1, \lin, 1), 0);  // + action ??
    active.action = { arg changer, what;  nodes.do {Êarg node;  node.active_(changer.value) } };
  
    case 
      { argrouting == \auto }  { this.autoconnect }
      { argrouting == \simple }  { this.simpleconnect }
      { argrouting.isArray }    { this.arrayconnect(argrouting) }
      { this.simpleconnect };
      
    this.activate;
  
  }

  // handles automatically LFE management !!
  arrayconnect { arg array;
    var from, to, gain = 0.0;
    array.do { arg conn, i;
    //  from = fromdevice.busArray[conn[0]];
      from = fromdevice.outputs[conn[0]];
      if ( conn[1] == MXGlobals.lfeSymbol ) {
      //  to = MXMonitorManager.subMonitor.busArray[0];
        to = MXMonitorManager.subMonitor.inputs[0];
        if ( conn.size > 2 ) { gain = conn[2] } { gain = 0 }; // { gain = -10 };
      } {
      //  to = todevice.busArray[conn[1]];
        to = todevice.inputs[conn[1]];
        if ( conn.size > 2 ) { gain = conn[2] };
      };
      this.addNode( from, to, gain );
    //  show this node in the global matrix grid !!
    
    }
  }
/*
  arrayconnect { arg array;
    var from, to, gain = 0.0;
    array.do { arg conn, i;
    //  from = fromdevice.busArray[conn[0] - 1];
      from = fromdevice.outputs[conn[0] - 1];
      if ( conn[1] == MXGlobals.lfeSymbol ) {
      //  to = MXMonitorManager.subMonitor.busArray[0];
        to = MXMonitorManager.subMonitor.inputs[0];
        if ( conn.size > 2 ) { gain = conn[2] } { gain = -10 };
      } {
      //  to = todevice.busArray[conn[1] - 1];
        to = todevice.inputs[conn[1] - 1];
        if ( conn.size > 2 ) { gain = conn[2] };
      };
      this.addNode( from, to, gain );
    }
  }
*/    
  autoconnect {
  // connects all outs of fromdevice per best guess to all ins of todevice
    var numouts, numins;
    var fromarray, toarray;

  //  numouts = fromdevice.outputs.size;
  //  numins = todevice.inputs.size;
    fromarray = fromdevice.outputs; // fromdevice.busArray;
    toarray = todevice.inputs; // busArray;
    
    numouts = fromarray.size;
    numins = toarray.size;
    
    gain.value = 0.0; 
    
    case 
      { (numouts < 1) || (numins < 1) } { "can't connect devices without inputs or outputs".warn }
      { numouts == numins }       { this.simpleconnect;  }
      // if num outs = 1  > distribute one to all
      { numouts == 1 }          { toarray.do { arg in, i;  this.addNode( fromarray[0], in, gain.value ); } }
      // if num ins = 1   > mix all to one
      { numins == 1 }         { // gain.value = -3 * (numouts-1);
                        fromarray.do { arg out, i;  this.addNode(  out, toarray[0], gain.value); } 
                      }
      // if num outs = 2  > distribute left to odds, right to evens
      { numouts == 2 }          { toarray.pairsDo { arg in1, in2, i;  
                        this.addNode( fromarray[0], in1 );
                        this.addNode( fromarray[1], in2 );
                        };
                        if (numins.odd) { this.addNode( fromarray[0], toarray.last, gain.value ) };
                      }
      // if num ins = 2    > mix odds to left, evens to right
      { numins == 2 }         { // gain.value = -3 * (numouts-1);
                        fromarray.pairsDo { arg out1, out2, i;  
                        this.addNode( out1, toarray[0], gain.value );
                        this.addNode( out2, toarray[1], gain.value );
                        };                        if (numouts.odd) { this.addNode( fromarray.last, toarray[0], gain.value ) };
                      }
      // more ?
  
      // else > simpleconnect
      {true }              { this.simpleconnect };
      
      // + activate ??
  }
/*
  autoconnect {
  // connects all outs of fromdevice per best guess to all ins of todevice
    var numouts, numins;
    var fromarray, toarray;

  //  numouts = fromdevice.outputs.size;
  //  numins = todevice.inputs.size;
    fromarray = fromdevice.outputs; // fromdevice.busArray;
    toarray = todevice.inputs; // busArray;
    
    numouts = fromarray.size;
    numins = toarray.size;
    
    gain.value = 0.0; 
    
    case 
      { (numouts < 1) || (numins < 1) } { "can't connect devices without inputs or outputs".warn }
      { numouts == numins }       { this.simpleconnect;  }
      // if num outs = 1  > distribute one to all
      { numouts == 1 }          { toarray.do { arg in, i;  this.addNode( fromarray[0], in, gain.value ); } }
      // if num ins = 1   > mix all to one, -3db per out
      { numins == 1 }         { gain.value = -3 * (numouts-1);
                        fromarray.do { arg out, i;  this.addNode(  out, toarray[0], gain.value); } 
                      }
      // if num outs = 2  > distribute left to odds, right to evens
      { numouts == 2 }          { toarray.pairsDo { arg in1, in2, i;  
                        this.addNode( fromarray[0], in1 );
                        this.addNode( fromarray[1], in2 );
                        };
                        if (numins.odd) { this.addNode( fromarray[0], toarray.last, gain.value ) };
                      }
      // if num ins = 2    > mix odds to left, evens to right
      { numins == 2 }         { gain.value = -3 * (numouts-1);
                        fromarray.pairsDo { arg out1, out2, i;  
                        this.addNode( out1, toarray[0], gain.value );
                        this.addNode( out2, toarray[1], gain.value );
                        };                        if (numouts.odd) { this.addNode( fromarray.last, toarray[0], gain.value ) };
                      }
      // more ?
  
      // else > simpleconnect
      {true }              { this.simpleconnect };
      
      // + activate ??
  }
*/  
  simpleconnect {
  // connects every out to the corresponding in (if available)
    fromdevice.outputs.do { arg out, i;  
      if ( i < todevice.inputs.size ) {
        this.addNode( out, todevice.inputs[i], gain.value ) }
    }
  }
  
  disconnect {
  // "destructor": remove all MXNodes of this MXConnection, remove group, CVs etc.
  //  nodes.do(_.remove);
    nodes.copy.keysValuesDo { arg key, node;
      this.removeNode(key[0], key[1]);  // keine gute Idee ...
    };
    nodes.clear;
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  remove { // ???
  // disconnects all nodes and removes all resources
    this.disconnect;  
    group.free; // ???
  }
  
  addNode { arg frombus, tobus, gain=0.0;
    var node;
    node = MXNode(group, frombus, tobus, gain);
    nodes.add( [frombus, tobus] -> node);  // key: Array with frombus and tobus objects !
    MXMatrixManager.globalMatrix.markNode(frombus, tobus, true);
  }
  
  removeNode { arg frombus, tobus;
    var key = [frombus, tobus];
    nodes.at(key).remove; // kill node contents
    nodes.removeAt(key); // remove node from dict
    MXMatrixManager.globalMatrix.markNode(frombus, tobus, false);
  }
  
  mute {
    nodes.do(_.mute); 
  }
  
  unmute {
    nodes.do(_.unmute); 
  }
  
  activate {
    if (nodes.size > 0) { active.value = 1 };
  }
  
  deactivate {
    if (nodes.size > 0) { active.value = 0 };
  }
  
  gain_ { arg value;  // Float in dB
    gain.value = value;
  }

}



MXMatrixManager {  // singleton !, was AMXMatrixManager
/*
- scannt beim Start folder mit XMLs fuer monitorpresets fuer die aktuelle Samplingrate und legt entsprechende Objekte (??) an

*/
  classvar <globalMatrix;   // THE global MATRIX !
  classvar <globalArraySources; // dict  (busnum -> [indevice, outputnum])
  classvar <globalArrayTargets; // dict  (busnum -> [outdevice, inputnum])
  classvar <matrices;     // others (testgenerator matrix, MicPre matrix, WFS ... ?)
  classvar <numIns, <numOuts;   // Integer counting all interface IOs 
  classvar <monitorpresets;   // Order, index > List of Associations [ ( <presetname> -> [ numberofinputs>, <nodes-array> ]) * ]
  classvar <group;
  classvar <monitorGroup;
  classvar <win, <view;   // GUI

  *init {
  //  nodes = List.new;
    monitorpresets = Order.new(64); 
    matrices = Dictionary.new;
    this.readConfig;
      
  }
  
  *readConfig {
    var path, arrayfromfile, dict;
    path = MXGlobals.configDir ++ "monitorpresets.txt";
    if (File.exists(path)) {
      ("\n------------------\nreading" + path + "\n------------------").postln;
      arrayfromfile = File.open(path, "r").readAllString.interpret;
    //  arrayfromfile.postln;
      if (arrayfromfile.isNil) { "WARNING: no monitorings presets in file".postln };
      arrayfromfile.do { arg assoc, i;
        this.addMonitorPresetFromArray([ assoc.key] ++ assoc.value );
      } ;
    } {
      "FILE ERROR: monitorpresets.txt not found!".postln; 
    };    
  } 
  
  *addMonitorPresetFromArray { arg array;
    var preset, name, inputs, nodesarray;
    var from, to, gain;
    var lastinput = 0;
    var lfegain = 0; // -10 ??
    name = array[0].asString;
    inputs = array[1].asInteger;
    nodesarray = array[2];
    if (monitorpresets[inputs].isNil) {Êmonitorpresets.put(inputs, List.new) };
    nodesarray = nodesarray.collect {Êarg node; 
      from = node[0] - 1;
      to = node[1];
      if ( to.isNumber ) { to = to - 1 };
      if ( node.size > 2 ) { gain = node[2] } { gain = nil };
      [ from, to, gain ]
    };
    monitorpresets[inputs].add(name -> nodesarray);
    ("Monitoring preset added:" + inputs + name).postln;
    // if there is no LFE in nodesarray, make also version with additional LFE channel for the added preset:
    if ( nodesarray.select({ arg node; node[1] == MXGlobals.lfeSymbol }).size == 0 ) {
      if (monitorpresets[inputs + 1].isNil) {Êmonitorpresets.put(inputs + 1, List.new) };
      name = name + "+LFE";
      // find the last input of this preset:
      nodesarray.do { arg node, i;  if (node[0] > lastinput ) { lastinput = node[0] } }; 
      monitorpresets[inputs + 1].add(name -> (nodesarray ++ [ [lastinput + 1, MXGlobals.lfeSymbol, lfegain] ]) );
    }
  }
  
  *makeConnectionFromMonitorPreset { arg device, monitorpresetarray;
    // handles automatically LFE routing !
    ^MXConnection(monitorGroup, device, MXMonitorManager.mainMonitor, monitorpresetarray);
  }
  
  *makeSubConnectionFromMonitorPreset { arg device, monitorpresetarray;
    //  sub management for the given preset array
    var subarray, inputchannels;
    inputchannels = { false }.dup(device.numChannels);
    subarray = List.new;
    monitorpresetarray.do { arg node;
      // make a node from each input channel (only once!) to the sub, 
      if ( inputchannels[node[0]].not &&  (node[1] != MXGlobals.lfeSymbol) ) {
        inputchannels[node[0]] = true;  
        subarray.add( [ node[0], 0, 0 ] );
      }
    };  
    // reduce gain by 3 db per input if more than one input 
    if (subarray.size > 1) {
      subarray = subarray.collect({ arg node, i;  [ node[0], node[1], -3 * (subarray.size - 1) ] });    };
    if (subarray.size > 0) {    
      ^MXConnection(monitorGroup, device, MXMonitorManager.subMonitor, subarray);
    } {Ê^nil };
  }

  *addDevice {
    
  }
  
  *removeDevice {
    
  }
    
  *makeGlobalMatrix {
    var srspeed;
    var frombusarray, tobusarray;
    
    srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];
    
  //  frombusarray = MXDeviceManager.inDevices.collect({ arg d, i; d.busArray}).flat;
  //  tobusarray = MXMonitorManager.devices.collect({ arg d, i; d.busArray}).flat
  //     ++ MXDeviceManager.outDevices.collect({ arg d, i; d.busArray}).flat;
    frombusarray = MXDeviceManager.inDevices.collect({ arg d, i; d.outputs}).flat;
    tobusarray = MXMonitorManager.devices.collect({ arg d, i; d.inputs}).flat
       ++ MXDeviceManager.outDevices.collect({ arg d, i; d.inputs}).flat;
    
    globalMatrix = MXMatrix(frombusarray, tobusarray);

  //  globalMatrix.inLabels = MXDeviceManager.inDevices.collect({ arg d, i; [ d.name, d.ioDict[srspeed].size ] });
    globalMatrix.inLabels = MXDeviceManager.inDevices.collect({ arg d, i;  [d.name.asString, d.numChannels] });

    globalMatrix.outLabels = MXMonitorManager.devices.collect({ arg d, i;  [d.name.asString, d.numChannels] })
              ++ MXDeviceManager.outDevices.collect({ arg d, i;  [d.name.asString, d.numChannels] });

    globalMatrix.inLabels2 = MXDeviceManager.inDevices.collect({ arg d, i; 
      d.numChannels.collect({ arg k; [d.name.asString, (k+1).asString] })
    }).flatten(1);
    globalMatrix.outLabels2 = MXMonitorManager.devices.collect({ arg d, i; 
      d.numChannels.collect({ arg k; [d.name.asString, (k+1).asString] })
    }).flatten(1) ++ MXDeviceManager.outDevices.collect({ arg d, i; 
      d.numChannels.collect({ arg k; [d.name.asString, (k+1).asString] })
    }).flatten(1);
  } 

  *makeMatrix { arg name, frombusarray, tobusarray, inlabels, outlabels;
    var newmatrix;

    newmatrix = MXMatrix(frombusarray, tobusarray);
    newmatrix.inLabels = inlabels;
    newmatrix.outLabels = outlabels;
    newmatrix.inLabels2 = inlabels.collect({ arg array, i;
      var name = array[0], size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);
    newmatrix.outLabels2 = outlabels.collect({ arg array, i;
      var name = array[0], size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);
    
    matrices.add(name -> newmatrix);      
  } 
  
  *setSR {
    var srspeed;
    "MatrixManager-setSR".postln;
    // count numins and numouts
    srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];
    numIns = MXIOManager.inputs[srspeed].size;
    numOuts = MXIOManager.outputs[srspeed].size;
    this.makeGlobalMatrix;
    // other matrices ??
  }

  *unsetSR {
  //  globalMatrix ??
  }
  
  *startDSP { arg target, target2; 
    "MatrixManager-startDSP".postln;
    group = target;
    monitorGroup = target2;
    globalMatrix.startDSP(group);
  //  matrices.do({ arg mat, i; mat.startDSP });
  }   

  *stopDSP {  // ??
  //  matrices.do({ arg mat, i; mat.stopDSP });
    globalMatrix.stopDSP;
    { group.free }.defer( MXGlobals.switchrate + 0.01 );
  }
  
  *reset {
    MXGUI.matrixView.grid.clearGrid;  // schnelle unsaubere Notloesung ....
    globalMatrix.reset;
  } 
  
  *getValues {
    var dict = IdentityDictionary.new;
    dict.add( \globalMatrix -> globalMatrix.getValues);
    ^dict;
  }
  
  *setValues { arg dict;
    this.reset;
    if (dict.includesKey( \globalMatrix )) { globalMatrix.setValues(dict[\globalMatrix] ) };
  }

  
}




MXProcessor {
/*  
- abstract class for virtual internal devices which do something on signals
  - filter
  - mixer
  - en/decoder (MS, Ambisonics etc.)

- inputs and outputs are private audio busses

- de/activation either bypasses ins to outs or mutes outs

- simple level meter for each channel ?

*/  
  
  
}
