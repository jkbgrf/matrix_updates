

MXGlobals { // singleton !
  classvar <>audioDeviceName = "MADI256"; 
//  classvar <midiControllerName = \NanoKontrol; 
  classvar <midiControllerName = \MackieControl16; // \MackieControl8, \QCon, \Tascam2400, \BCF2000
  classvar <lfeSymbol = \LFE, <crossoverFrequency;

  classvar <>levelrate, <>switchrate, <>delaymax, <>delayrate, <>eqrate;
  classvar <>limiterLevel, <>limiterDur;
  classvar <>meterIDbase, <>meterrate, <>meterrms, <>meterrmstime, <>meterinterval, <>meterdecay, <>meterrange, <>meterpeaklag;
  classvar <>simplemeterIDbase, <>simplemeterrate, <>simplemeterrms, <>simplemeterdecay, <>simplemeterrange;
  classvar <>fftsize, <>fftdisplayinterval;
  classvar <>dbmin, <>dbmax, <>dbstep, <>dbstepsmall;
  classvar <>numAudioBusses, <>numInputs, <>numOutputs, <>numAux, <>numListen, <inputShift;
  classvar <srspeeds;
  classvar <defaultsampleRate=nil, <defaultblockSize=64, <defaulthardwareBufferSize=128;  classvar <>sampleRate, <>blockSize, <>hardwareBufferSize, <maxNodes;
//  classvar <>sampleRateOnBoot; // compare regularly with current sr, for warning about sr clock change ...
  classvar <>server, <>serveroptions, <device;
  classvar <synthdefs;
  
  classvar <>docDir, <>configDir, <>helpDir, <>prefsDir, <>sessionsDir;
  classvar <>sessionName, <>sessionFolder;
  classvar <>meterPoints, <>masterMeterPoints, <>filterTypes, <>filterDefs;

  *initClass {

    numAudioBusses = 2048;
    numInputs = 256; //56;
    numOutputs = 256; //56;
    //numAux = 8;
    //numListen = 2;
    sampleRate = defaultsampleRate;
    blockSize = defaultblockSize;
    hardwareBufferSize  = defaulthardwareBufferSize;
    
    maxNodes = 4096;

    synthdefs = List.new;

    srspeeds = IdentityDictionary[
      32000.0 -> 48000.0,
      44100.0 -> 48000.0,
      48000.0 -> 48000.0,
      88200.0 -> 96000.0,
      96000.0 -> 96000.0,
      176400.0 -> 192000.0,
      192000.0 -> 192000.0
    ];
    
    levelrate = 0.06;   // time fuer Lag fuer Fader, Matrixgains etc.
    switchrate = 0.02;  // time fuer Lag fuer Matrixknoten, on/offs etc.
    dbmin = -80.0;    // min db value fuer Matrixknoten, Fader etc.
    dbmax = 12.0;   // max db value fuer Matrixknoten, Fader etc.
    dbstep = 0.5;     // db step value fuer Fader etc.
    dbstepsmall = 0.1;  // db step value fuer Matrixknoten etc.

    delaymax = 1.0;
    delayrate = 0.002;
    eqrate = 0.05;

    limiterLevel = 0.95;
    limiterDur = 0.02;

    crossoverFrequency = 80;  // for sub LPF and main HPF !
      
    meterIDbase = 4000; // fuer SendTrig und Responder
    meterrate = 20; // Hz
    meterrmstime = 0.01;    // RMS integration time in seconds
    meterrms = 44100 * meterrmstime;  // number of audio samples for integration
//    meterrms = 100; // number of audio samples for integration
    meterinterval = meterrms.reciprocal;
    meterdecay = 60;  // dB/second
    meterrange = 60;  // dB
    meterpeaklag = 5; // lag time (decay) for peak values (signal meter, RTA, ...)

    simplemeterIDbase = 3000; // fuer SendTrig und Responder
    simplemeterrate = 10;   // Hz
    simplemeterrms = 100; // integration time (number of audio samples)
    simplemeterdecay = 120; // dB/second
    simplemeterrange = 60;  // dB
    
    fftsize = 2048;     // 512 or 1024 or 2048
    fftdisplayinterval = 25.reciprocal;
    
  /*
    StartUp.add({
      this.setServer;
      this.storeDefs;
      //this.setGUI(\cocoa);
      //this.setGUI(\swing);
      
    }); 
  */  
    docDir = "/Users/akusmix/_Akusmix/";
    //docDir = "/Users/abart/_Acousmix/";
    helpDir = docDir ++ "Help/";
    prefsDir = docDir ++ "Preferences/";
    sessionsDir = docDir ++ "Sessions/";

    sessionName = nil;    
    sessionFolder = sessionsDir;
    
  //  this.setServer;
    
  }
  

  *setServer {  
    server = Server.local;
    server.options.sampleRate = defaultsampleRate;
    server.options.hardwareBufferSize = hardwareBufferSize;
    server.options.blockSize = blockSize;
    server.options.memSize = 8192 * 64;
    server.options.numWireBufs = 256 * 4;
    server.latency = 0.03;
    server.options.device = audioDeviceName;  
    server.options.numPrivateAudioBusChannels = numAudioBusses;
    server.options.numInputBusChannels = numInputs;
    server.options.numOutputBusChannels = numOutputs; // + numListen; //  + numAux
    inputShift = server.options.numOutputBusChannels;
    server.options.maxNodes = maxNodes;
  }
  
  *sendSynthDefs {
    SynthDescLib.global.addServer(server);
    ("\n------------------\nsending synthdefs to server \n------------------").postln;
    synthdefs.do {Êarg d; d.add(\global) };
  } 
  
/*    
  
  *storeDefs {
    var lrate, delmax, drate, eqrate;
    
    lrate = this.levelrate;
    delmax = this.delaymax;
    drate = this.delayrate;
    eqrate = this.eqrate;
    
    SynthDef(\MXChannelGain ,{arg i_inbus=0, i_outbus=0, gate=1, gain=1.0;    // in gain auch Phase codieren!!
      var sig;
      sig = In.ar(i_inbus, 1);
      ReplaceOut.ar(i_outbus, sig * gain);
    }, [\ir, \ir, 0, lrate] ).store;
      
    SynthDef(\MXChannelDelay ,{arg i_inbus=0, gate=1, delay=0.01;  
      var sig;
      sig = In.ar(i_inbus, 1);
      sig = DelayL.ar(sig, delmax, delay);
      ReplaceOut.ar(i_inbus, sig);
    }, [\ir, 0, drate] ).store;

    SynthDef(\MXChannelEQ ,{arg i_inbus=0, gate=1, f1=100.0, f2=1000.0, f3=10000.0, 
        r1=0.2, r2=0.2, r3=0.2, db1=0, db2=0, db3=0;  
      var sig;
      sig = In.ar(i_inbus, 1);
  //    sig = BLowShelf.ar(sig, f1, r1, db1);
      sig = BLowShelf.ar(sig, f1, 1.0, db1);
      sig = BPeakEQ.ar(sig, f2, r2, db2);
  //    sig = BHiShelf.ar(sig, f3, r3, db3);
      sig = BHiShelf.ar(sig, f3, 1.0, db3);
      ReplaceOut.ar(i_inbus, sig);
    }, [\ir, 0] ++ (eqrate ! 9)).store;


    SynthDef(\MXChannelEQX ,{arg i_inbus=0, gate=1;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0] ).store;

    SynthDef(\MXChannelEQLS ,{arg i_inbus=0, gate=1, f=100.0, r=1, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      //sig = BLowShelf.ar(sig, f, r, db); // r = rs
      sig = BLowShelf.ar(sig, f, 1, db); // rs = 1
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQLP ,{arg i_inbus=0, gate=1, f=100.0, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      sig = LPF.ar(sig, f, db.dbamp);
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQHS ,{arg i_inbus=0, gate=1, f=10000.0, r=1, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      //sig = BHiShelf.ar(sig, f, r, db); // r = rs
      sig = BHiShelf.ar(sig, f, 1, db); // rs = 1
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQHP ,{arg i_inbus=0, gate=1, f=10000.0, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      sig = HPF.ar(sig, f, db.dbamp);
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQPEQ ,{arg i_inbus=0, gate=1, f=100.0, r=1, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      sig = BPeakEQ.ar(sig, f, r, db);
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQBP ,{arg i_inbus=0, gate=1, f=100.0, r=1, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      sig = BBandPass.ar(sig, f, r, db.dbamp);  // r = bw
      env = Linen.kr(gate, 0.1, 1, 0.1, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate, eqrate] ).store;

    SynthDef(\MXChannelEQBR ,{arg i_inbus=0, gate=1, f=100.0, r=1, db=0;  
      var sig, env;
      sig = In.ar(i_inbus, 1);
      sig = BBandStop.ar(sig, f, r, db.dbamp); // r = bw
      env = Linen.kr(gate, 0.05, 1, 0.05, doneAction: 2);
      ReplaceOut.ar(i_inbus, sig * env);
    }, [\ir, 0, eqrate, eqrate, eqrate] ).store;

    // fuer Inputchannel, Masterchannel:
    SynthDef(\MXChannelFader ,{arg i_inbus=0, gate=1, level=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      ReplaceOut.ar(i_inbus, sig * Lag2.ar(K2A.ar(level), lrate));
    }, [\ir, 0, 0] ).store;

    // nur fuer Outputchannel:
    SynthDef(\MXOutChannelFader ,{arg i_inbus=0, i_outbus=0, gate=1, level=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1) * Lag2.ar(K2A.ar(level), lrate);
      ReplaceOut.ar(i_inbus, sig);  // auf diesen Bus weiterschreiben fuer Meter und Listen
      Out.ar(i_outbus, sig);      // dieser Bus ist richtiger Ausgang
    }, [\ir, \ir, 0, 0] ).store;

    // fuer Masterchannel, wie Input nur mit masterlevel fuer Group:set
    SynthDef(\MXMasterChannelFader ,{arg i_inbus=0, gate=1, level=1.0, masterlevel=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      ReplaceOut.ar(i_inbus, sig * Lag2.ar(K2A.ar(masterlevel * level), lrate));
    }, [\ir, 0, 0, 0] ).store;
    
    // nur fuer Listen Masterchannel! (ohne masterlevel)
    SynthDef(\MXMasterChannelFader ,{arg i_inbus=0, gate=1, level=1.0, masterlevel=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      ReplaceOut.ar(i_inbus, sig * Lag2.ar(K2A.ar(masterlevel * level), lrate));
    }, [\ir, 0, 0, 0] ).store;

    SynthDef(\MXChannelLimiter ,{arg i_inbus=0, gate=1, limit=1.0, time=0.01;    
      var sig;
      sig = In.ar(i_inbus, 1);
      sig = Limiter.ar(sig, limit, time);
      ReplaceOut.ar(i_inbus, sig);
    }, [\ir, 0, lrate, lrate] ).store;

    SynthDef(\MXChannelListen ,{arg i_inbus=0, i_outbus=0, gate=1, level=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      sig = sig * Linen.kr(gate, lrate, 1.0, lrate, doneAction: 1);
      Out.ar(i_outbus, (sig * level) ! 2);
    }, [\ir, \ir, 0, lrate] ).store;

    SynthDef(\MXChannelAuxSend ,{arg i_inbus=0, i_outbus=0, gate=1, level=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      sig = sig * Linen.kr(gate, lrate, 1.0, lrate, doneAction: 2);
      Out.ar(i_outbus, sig * level);
    }, [\ir, \ir, 0, lrate] ).store;

    SynthDef(\MXChannelMeter, {arg i_inbus=0, decay=0.99994, rate=15;
      var p, t;
      p = PeakFollower.ar( In.ar(i_inbus, 1), decay);
      t = Impulse.ar(rate);
      SendTrig.ar(t, MXGlobals.meterIDbase, p);   
      0
    }, [\ir, 0, 0] ).store;
      
    SynthDef(\MXMatrixNode ,{arg i_inbus=0, i_outbus=0, gate=1, level=1.0, offset=1.0;    
      var sig;
      sig = In.ar(i_inbus, 1);
      sig = sig * Linen.kr(gate, lrate, 1.0, lrate, doneAction: 2);
      Out.ar(i_outbus, sig * Lag.ar(K2A.ar(level * offset), lrate)); 
    }, [\ir, \ir, 0, 0, 0] ).store;

  }

  *startSwingOSC {
    SwingOSC.default.boot; 
  } 
  

  *getValues {
    var dict = IdentityDictionary.new;
    dict.add( \sampleRate       -> sampleRate );
    dict.add( \blockSize      -> blockSize );
    dict.add( \hardwareBufferSize   -> hardwareBufferSize );
    dict.add( \sessionName      -> sessionName );
    dict.add( \sessionFolder    -> sessionFolder );
    
    ^dict;
  }
  
  *setValues { arg dict;
    "----------- adding session Input Bundles ...".postln;
    if (dict.includesKey( \sampleRate ))      { sampleRate = dict[\sampleRate]; };
    if (dict.includesKey( \blockSize ))       { blockSize = dict[\blockSize]; };
    if (dict.includesKey( \hardwareBufferSize ))  { hardwareBufferSize = dict[\hardwareBufferSize]; };
    if (dict.includesKey( \sessionName ))     { sessionName = dict[\sessionName]; };
    if (dict.includesKey( \sessionFolder ))     { sessionFolder = dict[\sessionFolder]; };
    ("----------- Session SampleRate: " ++ sampleRate).postln;
    
  }
    
*/  
  
}






