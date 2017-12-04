MXMain.init("D:/Dokumente/UNI/M A S T E R/SuperColliderProj/config/");


/* Notes:
Controle Value für Midi Changer: Simple Controler?


*/
// testcode lines ab hier

(
var path, dict, arrayfromfile;
path = "D:/Dokumente/UNI/M A S T E R/SuperColliderProj/config/" ++ "devices.txt";
path.postln;

if (File.exists(path)) {
      ("\n------------------\nreading" + path + "\n------------------").postln;
      arrayfromfile = File.open(path, "r").readAllString.interpret;
    //  arrayfromfile.postln;
      dict = Dictionary.new;}
    {"FILE ERROR: devices.txt not found!".postln;};
arrayfromfile.postln;

      arrayfromfile.do { arg assoc, i;
        // assoc.key = assoc.key.asSymbol;
        dict.add( assoc.key.asSymbol -> assoc.value )
      };
// ... anderer Stuff
// hier kommt die if schleife ob madibridge vorhanden
	   if (dict.includesKey( \madibridge)) {
		dict[\madibridge].do {Êarg assoc, i;
          this.addDeviceFromArray(\madibridge, [assoc.key] ++ assoc.value );
		  MXMadiBridge.init;
// Controle Value für MADIBRIDGE Connectet yes/no
		}
	  } {
        "WARNING: no madi bridge Routing found in file: Madi Bridge not connectet".postln;
      };

arrayfromfile.postln;



MXMadiBridge {
	classvar <madiChanels //number of Madi Chanels
	classvar <madiRouting //dict for Madi Routing: dropdown Menue Text
	classvar <midiChanel  //


	*init {
		madiChanels = List.new;
	}

}









// --------------------------------------Code Examples--------------------------------------------
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
)


MXMatrixManager {  // singleton !, was AMXMatrixManager
/*
- scannt beim Start folder mit XMLs fuer monitorpresets fuer die aktuelle Samplingrate und legt entsprechende Objekte (??) an

*/
	classvar <globalMatrix;		// THE global MATRIX !
	classvar <globalArraySources;	// dict  (busnum -> [indevice, outputnum])
	classvar <globalArrayTargets;	// dict  (busnum -> [outdevice, inputnum])
	classvar <matrices;			// others (testgenerator matrix, MicPre matrix, WFS ... ?)
	classvar <numIns, <numOuts; 	// Integer counting all interface IOs
	classvar <monitorpresets;		// Order, index > List of Associations [ ( <presetname> -> [ numberofinputs>, <nodes-array> ]) * ]
	classvar <group;
	classvar <monitorGroup;
	classvar <win, <view;		// GUI

	*init {
	//	nodes = List.new;
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
		//	arrayfromfile.postln;
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
		if (monitorpresets[inputs].isNil) {�monitorpresets.put(inputs, List.new) };
		nodesarray = nodesarray.collect {�arg node;
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
			if (monitorpresets[inputs + 1].isNil) {�monitorpresets.put(inputs + 1, List.new) };
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
			subarray = subarray.collect({ arg node, i;  [ node[0], node[1], -3 * (subarray.size - 1) ] });		};
		if (subarray.size > 0) {
			^MXConnection(monitorGroup, device, MXMonitorManager.subMonitor, subarray);
		} {�^nil };
	}

	*addDevice {

	}

	*removeDevice {

	}


