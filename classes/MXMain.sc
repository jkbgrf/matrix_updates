/*
Presetverwaltung fuer Setups

was gehoert dazu?
  - in/outDevice routings (monitors, levelmeter, analyse), gain, mute-state, MIDI controller,
  - matrix nodes
  - micPre settings

  - monitor (main, nearfield, phones etc.) settings

was nicht?
  - testgenerator einstellungen


Reset
  reload des zuletzt geladenen Setups ohne Filedialog!

Load

  filedialog
  dict einlesen
  warnung wenn:
    SamplingRate anders
    bestimmte Devices nicht existieren
    bestimmte Matrix-Verbindungen nicht moeglich sind
  settings aktivieren (nicht existierende Devices ignorieren)
    je object: setValues
  setup name anzeigen

Save
  filedialog (immer wie Save As ?)
  je object: getValues
  in dict kopieren
  setup name ggfs. aktualisieren

Setup name
  - bei irgendeiner Aenderung: * hinzufuegen

Updates need to be made

*/

MXMain { // singleton !
  classvar <ioManager;
  classvar <deviceManager;
  classvar <monitorManager;
  classvar <meterManager;
  classvar <matrixManager;
  classvar <testGenerator;
  classvar <micpre;
  classvar <gui;
  classvar <>startcondition;
  classvar <setupPathName;
//  classvar <clockChecker;

  *init { arg configpath;
    if (configpath.isNil) { "ERROR:\nCould not start MXMain! \nPlease specify a path for the config files!".postln; ^nil };

    MXGlobals.configDir = configpath;
    startcondition = Condition.new;
    Routine.run({
      MXMIDI.init;
      startcondition.hang(2); // wait for MIDIClient.init ...
      MXGlobals.setServer;  // ServerOptions etc.
      this.testServer; // gets sampleRate of the audio driver, sets bus allocator

      MXStatus.init;
      // call MXIOManager:
      ioManager = MXIOManager.init;
      // reads converters.txt
      // makes MXInterfaces und MXIOs

      // call MXDeviceManager:
      monitorManager  = MXMonitorManager.init;
      deviceManager = MXDeviceManager.init;
      meterManager  = MXMeterManager.init;
      // reads devices.txt
      // makes MXInDevices, MXOutDevices

      // call MXMatrixManager:
      matrixManager = MXMatrixManager.init;
      // reads monitorpresets.txt
      // makes an Order with monitorpresets, indexed by number of inputs (Mono, Stereo, Quadro etc.)
      testGenerator = MXTestGenerator.new;
      micpre = MXRemoteMicstasy.new;
      // make GUI
      { gui = MXGUI.init; }.defer;
      // container for areas (status, routing tabs, meter, volumes, speaker, spectral displays)
      // build routing tab panes:
        // monitor sources (according devices.txt and monitorpresets.txt)
        // matrix (according converters.txt (>> matrix size!) and devices.txt (labelling)
        // testgenerator and micpre dito

      startcondition.hang(2);
      this.restartMatrix;

    });

  }

  *restartMatrix {
    "\n------------------\nrestarting matrix ... \n------------------".postln;
    Routine.run({
      this.setSR;
      startcondition.hang(2);
      this.startDSP;
    //  if(MIDIClient.initialized.not,{ MIDIClient.init });
    });
  }

  *restart {
    "\n------------------\nrestarting computer ... \n------------------".postln;
    (MXGlobals.configDir ++ "apps/restart.app/Contents/MacOS/applet").unixCmd
  }

  *shutDown {
    "\n------------------\nshutting down ... \n------------------".postln;
    (MXGlobals.configDir ++ "apps/aus.app/Contents/MacOS/applet").unixCmd
  }

  *testServer {
    "\n------------------\ntesting clock and server ... \n------------------".postln;
    MXGlobals.setServer;
    MXGlobals.server.bootSync(startcondition);
    MXGlobals.sampleRate = MXGlobals.server.sampleRate;
    MXGlobals.server.quit;
  //  MXGlobals.server.sync(startcondition);

  /*
    MXGlobals.server.waitForBoot({
    //  MXGlobals.sampleRate = MXGlobals.server.sampleRate;
      MXGlobals.server.quit;
    //  MXGlobals.server.audioBusAllocator.dump;
    } );
  */
  }

  *setSR {
    if (MXGlobals.server.serverRunning) {
      this.stopDSP;
      startcondition.hang(1);
      // remove sr-depending GUI views
      {Êgui.unsetSR }.defer;
      // remove all IOs, devices, etc.
      startcondition.hang(1);

      testGenerator.unsetSR;
      matrixManager.unsetSR;
      deviceManager.unsetSR;
      meterManager.unsetSR;
      monitorManager.unsetSR;
      ioManager.unsetSR;

      startcondition.hang(1);
      this.testServer; // get new sample rate from WC generator !
      startcondition.hang(1);
    };

    // sr fuer angeschlossene Wandler und Geraete per remote setzen (MIDI, d.o.tec net etc. ?)

    // rebuild IOs, devices, matrix etc.
    ioManager.setSR;
    deviceManager.setSR;
    monitorManager.setSR;
    meterManager.setSR;
    matrixManager.setSR;
    testGenerator.setSR;

    // rebuild GUI
    {Êgui.setSR }.defer;
    // restart server -> this.startDSP
  }

  *startDSP {
    var ioInGroup, ioOutGroup, inDeviceGroup, outDeviceGroup, monitorGroup;
    var testGroup, meterGroup, matrixGroup, monitorMatrixGroup;
    // start server
    MXGlobals.server.bootSync(startcondition);
    "\n------------------\nServer online!\n------------------".postln;
  //  MXGlobals.sampleRateOnBoot = MXGlobals.server.sampleRate;
    MXGlobals.sendSynthDefs;
    MXGlobals.server.sync(startcondition);
    ioInGroup =     Group(MXGlobals.server, \addToHead);
    testGroup =     Group(ioInGroup, \addAfter);
    inDeviceGroup =   Group(testGroup, \addAfter);
    matrixGroup =   Group(inDeviceGroup, \addAfter);
    outDeviceGroup = Group(matrixGroup, \addAfter);
    monitorMatrixGroup = Group(outDeviceGroup, \addAfter);
    monitorGroup =  Group(monitorMatrixGroup, \addAfter);
    meterGroup =    Group(monitorGroup, \addAfter);
    ioOutGroup =  Group(meterGroup, \addAfter);
    MXGlobals.server.sync(startcondition);
    ioManager.startDSP(ioInGroup, ioOutGroup);
    MXGlobals.server.sync(startcondition);
    deviceManager.startDSP(inDeviceGroup, outDeviceGroup);      // starts only 2 groups
    MXGlobals.server.sync(startcondition);
    meterManager.startDSP(meterGroup); // ??
    monitorManager.startDSP(monitorGroup); // ??
    MXGlobals.server.sync(startcondition);
    matrixManager.startDSP(matrixGroup, monitorMatrixGroup); // ??
    MXGlobals.server.sync(startcondition);
    testGenerator.startDSP( testGroup ); // ??
    {Êgui.startDSP; MXMIDI.bankChange; }.defer;

    // activate meters and displays:
      // OSCFuncs
      // Synths

    MXGlobals.server.sync(startcondition);
    "\n------------------\nMATRIX ready!\n------------------".postln;

  }

  *stopDSP {
    // reset server related GUI features
    {Êgui.stopDSP }.defer;
    // remove all nodes from server
    testGenerator.stopDSP;
    matrixManager.stopDSP;
    monitorManager.stopDSP;
    meterManager.stopDSP;
    deviceManager.stopDSP;
    ioManager.stopDSP;
    MXGlobals.server.quit;
    MXMIDI.disconnectAll;
  }

  *reset {
    // switch off all connections, reset to default values
    testGenerator.reset;
    matrixManager.reset;
    monitorManager.reset;
    meterManager.reset;
    deviceManager.reset;
    ioManager.reset;
    {Êgui.reset }.defer;
  }


  *saveAs { arg resultfunc;  // { arg result, name; }
  // FileDialog
    File.saveDialog(
      "save setup as ...",
      "setup.txt",
      { arg path;
        // AMXGlobals.sessionFolder = PathName(path).pathOnly;
        // AMXGlobals.sessionName = PathName(path).fileNameWithoutExtension;
        path = path ++ ".matrix";
        this.saveSetup(path, resultfunc);
      },
    //  { "cancelled".postln; resultfunc.value(false); }
    )
  }

  *saveSetup { arg pathname, resultfunc; // { arg result, name; }
    var path, file, text, doc;
    var dict;

    // path = AMXGlobals.sessionFolder ++ AMXGlobals.sessionName;
    // if (path.contains("_amx.xml")) { pathname = path } { pathname = path ++ "_amx.xml" };

    dict = IdentityDictionary.new;
    dict.add( \SamplingRate -> MXGlobals.sampleRate );    dict.add( \Devices ->     deviceManager.getValues );
    dict.add( \Monitors ->    monitorManager.getValues );
    dict.add( \Matrix ->    matrixManager.getValues );

  //  doc = DOMDocument.new; // create empty XML document
  //  dict = this.getValues;
  //  dict.toXML(doc, doc, "AMXsession");
    file = File(pathname, "w");
    file.write(dict.asCompileString);
    file.flush;
    file.close;
    setupPathName = pathname;
    resultfunc.value(true, setupPathName)
  }

  *openSetup { arg resultfunc;
    File.openDialog(
      "select a setup...",
      { arg path;
        if ( PathName(path).extension != ".matrix" ) {
          this.loadSetup(path, resultfunc);
        } { "Invalid File!".warn;
        };
      //  resultfunc.value(PathName(path).fileName);
      },
    //  { "cancelled".postln; }
    );
  }

  *loadSetup { arg pathname, resultfunc;
    var dict, file;
    file = File(pathname, "r");
    dict = file.readAllString.interpret;
    file.close;
    this.reset; // ????
    if (dict.includesKey( \Monitors ))  { monitorManager.setValues( dict[\Monitors] )  };
    if (dict.includesKey( \Devices ))   { deviceManager.setValues( dict[\Devices] )  };
    if (dict.includesKey( \Matrix ))    { matrixManager.setValues( dict[\Matrix] )  };
    setupPathName = pathname;
    resultfunc.value(true, setupPathName)
  }

}
