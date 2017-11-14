MIDIModel {
  var <value;     // Float 0 .. 1
  var <type = \abs;   // Symbol [\abs, \rel, \relswitch, \incdec, \trigger, \toggle, \switch, \value], how to change the connected MXCV
  var <bank;      // Integer: Bank Number
  var <slot;      // Integer: number of MIDI controller slot (0...7 or 0...15 etc.)
            // or Symbol: \Master, \Transport, \Global etc
  var <messageType;   // Symbol \noteOn, \control \bend etc.
  var <channel;   // midi channel
  var <number;    // pitch, controlnumber
  var <uid;       // receives from 
  var <midiout;     // sends to
//  var <responder;
  var <midiinfunc;    // responder func for MIDIIn
  var <>displayNameFunc;  // Function for displaying the device name once connected
  var <>displayValueFunc; // Function for displaying a string for the changer.value once connected

  
  *new { arg messageType, channel, number, uid, midiout, bank, slot, type, displayValueFunc;
    ^super.new.init( messageType, channel, number, uid, midiout, bank, slot, type, displayValueFunc )
  }
  
  init { arg argmessageType, argchannel, argnumber, arguid, argmidiout, argbank, argslot, argtype, argdisplayValueFunc;
    messageType = argmessageType;
    channel = argchannel;
    number = argnumber;
    uid = arguid;
    midiout = argmidiout;
    bank = argbank;
    slot = argslot;
    type = argtype ? \abs;
    displayValueFunc = argdisplayValueFunc;
    value = 0;
    if ( [\noteOn, \noteOff, \polytouch, \control].includes(messageType) ) {
      midiinfunc = {arg src, chan, num, val;
        // [messageType, src, chan, num, val, uid, channel].postln;
        if ( (bank == MXMIDI.midiDevice.currentBank) || (bank.isNil) ) {
          if ( (src == uid) && (chan == channel) && (num == number) ) {
          //  ["value before change:", value].postln;
            switch (type) 
              {Ê\abs }  { value = val * 127.reciprocal }
              {Ê\toggle } { if (val > 0) {Êvalue = 1 - value } }
        //      {Ê\toggle } { if (val > 0) {Êvalue = 1 } }
              {Ê\switch } { if (val > 0) {Êvalue = 1 } { value = 0 } }  // useless ??
              
              ;
          //  [messageType, src, chan, num, val, uid, channel, "new value:", value].postln; 
            this.changed(\synch);
            
          }
        }
      };
    };
    if ( messageType == \bend ) {
      midiinfunc = {arg src, chan, val;
        // [\bend, src, chan, val, uid, channel].postln;
        if ( (bank == MXMIDI.midiDevice.currentBank) || (bank.isNil) ) {
          if ( (src == uid) && (chan == channel) ) {
            value = val * 16383.reciprocal;
          //  [chan, value].postln;
            this.changed(\synch);
          }
        }
      };
    };    
    if ( messageType.notNil ) {  MIDIIn.addFuncTo(messageType, midiinfunc) };
  }
  
  displayName {
    if ( (bank == MXMIDI.midiDevice.currentBank) || (bank.isNil) ) {
    //  ("displayName " ++ slot ).postln;
      displayNameFunc.value(slot);  
    };
  }
  
  update { arg changer, what;
    if (what == \synch) { 
      this.value = changer.input;
    };
  }
        
  value_ { arg val, displayvalue;
    value = val.clip(0, 1);
    if ( (bank == MXMIDI.midiDevice.currentBank) || (bank.isNil) ) { 
      this.send;
      if (displayvalue.notNil) {
        displayValueFunc.value(slot, displayvalue); 
      }
    };
  }
  
  send {
    var list;
    if ( messageType == \bend ) {
      list = [channel, (value * (2**14)).asInteger];
    } { 
      if ( [\noteOn, \noteOff, \polytouch, \control].includes(messageType) ) {
        list = [channel, number, (value * 127).asInteger];
      } {
        if ( [\program, \touch].includes(messageType) ) {
          list = [channel, (value * 127).asInteger];
        }
      }
    };
    if (list.notNil && messageType.notNil) {
    //  [messageType, list].postln;
      midiout.performList(messageType, list);
    };
  }
  
  remove {
    MIDIIn.removeFuncFrom(messageType, midiinfunc);
    // remove dependants ??
  }
}

MXMIDINanoKontrol {
  var <deviceName = "nanoKONTROL1" ;
  var <portInName = "SLIDER/KNOB";
  var <portOutName = "CTRL";
  var <endpoint, <uid;
  var <midiout;
  var <modelMessageDict;    // dict
  var <cvMessageDict;
  var <currentBank = 0;
  
  *new {
    ^super.new.init;    
  }
    
  init {
    endpoint = MIDIIn.findPort(deviceName, portInName);
    midiout = MIDIOut.findPort(deviceName, portOutName);
    if (endpoint.isNil) { (deviceName + "MIDI in port not found!").warn; } { uid = endpoint.uid; } ;
    if (midiout.isNil) { (deviceName + "MIDI out port not found!").warn; };
  
    if ( endpoint.notNil && midiout.notNil ) {
      midiout = MIDIOut.newByName(deviceName, portOutName);
      cvMessageDict = IdentityDictionary.new;
      modelMessageDict = IdentityDictionary.new;
      8.do {arg i;
        i = i+1;
        modelMessageDict.add( ("devicevolume" ++ i.asString).asSymbol -> [\control, 8, i, uid, midiout] );
        modelMessageDict.add( ("devicemute" ++ i.asString).asSymbol -> [\control, 8, i + 20, uid, midiout] );
        modelMessageDict.add( ("deviceselect" ++ i.asString).asSymbol -> [\control, 8, i + 30, uid, midiout] );
      };
      8.do {arg i;
        i = i+1;
        modelMessageDict.add( ("mainchannelgain" ++ i.asString).asSymbol -> [\control, 9, i, uid, midiout] );
        modelMessageDict.add( ("mainchannelmute" ++ i.asString).asSymbol -> [\control, 9, i + 20, uid, midiout] );
        modelMessageDict.add( ("mainchannelselect" ++ i.asString).asSymbol -> [\control, 9, i + 30, uid, midiout] );
      };
      8.do {arg i;
        i = i+1;
        modelMessageDict.add( ("monitorvolume" ++ i.asString).asSymbol -> [\control, 10, i, uid, midiout] );
        modelMessageDict.add( ("monitormute" ++ i.asString).asSymbol -> [\control, 10, i + 20, uid, midiout] );
        modelMessageDict.add( ("monitorselect" ++ i.asString).asSymbol -> [\control, 10, i + 30, uid, midiout] );
      };
    } {
      ^nil  
    };
  }
  
  displayDeviceName { 
    
  }

  displayModelValue { 

  } 
  
  bankChange {
    // not needed here
  }
}


MXMIDIMackieControl16 {
  var <deviceName = "MCU Pro USB v3.1" ;
  var <port1InName = "Anschluss 1";
  var <port1OutName = "Anschluss 1";
  var <port2InName = "Anschluss 2";
  var <port2OutName = "Anschluss 2";
  var <endpoint1, <uid1;
  var <midiout1;
  var <endpoint2, <uid2;
  var <midiout2;
  var <modelMessageDict;    // dict
  var <cvMessageDict;
  var <currentBank = 0;
  
  *new {
    ^super.new.init;    
  }
    
  init {
    var bank = 0;
    endpoint1 = MIDIIn.findPort(deviceName, port1InName);
    midiout1 = MIDIOut.findPort(deviceName, port1OutName);
    endpoint2 = MIDIIn.findPort(deviceName, port2InName);
    midiout2 = MIDIOut.findPort(deviceName, port2OutName);
    if (endpoint1.isNil) { (deviceName + "MIDI in port" +  port1InName + "not found!").warn; } { uid1 = endpoint1.uid; } ;
    if (midiout1.isNil) { (deviceName + "MIDI out port" +  port1OutName + "not found!").warn; };
    if (endpoint2.isNil) { (deviceName + "MIDI in port" +  port2InName + "not found!").warn; } { uid2 = endpoint2.uid; } ;
    if (midiout2.isNil) { (deviceName + "MIDI out port" +  port2OutName + "not found!").warn; };
    if ( endpoint1.notNil && midiout1.notNil && endpoint2.notNil && midiout2.notNil ) {
      midiout1 = MIDIOut.newByName(deviceName, port1OutName);     midiout2 = MIDIOut.newByName(deviceName, port2OutName); 
      "Initializing Mackie Control ...".postln;
      midiout1.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 16r10, 16r62,  16rF7]); // all LEDs off
      midiout1.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 16r10, 16r0C, 1, 16rF7]); // touchless
      midiout1.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 16r10, 16r61, 1, 16rF7]); // Faders min
      midiout1.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 16r10, 16r0A, 0, 16rF7]); // no click
      midiout1.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 16r10, 16r0B, 1, 16rF7]); // back light off after 1 min
  //    midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0, 32.dup(112), 16rF7].flat ) ); // clear LCD
  //    midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0, {arg i; "  L " ++ (i+9).asPaddedString(2," ", \right) ++ " "}.dup(4).ascii, 16rF7].flat ) ); 
      this.clearLCD;
    
      midiout1.sysex( [ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0, 32.dup(112), 16rF7].flat.collectAs({ arg c; c}, Int8Array) ); // clear LCD
      midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0] 
        ++ "           Elektronisches Studio der TU Berlin                        Fachgebiet Audiokommunikation".ascii ++ [16rF7]) ); 
      
      (16r76).do { arg i; midiout1.noteOn(0, i , 0) };
      8.do({ arg n; midiout1.bend(n, 0)});
  
      "Initializing Mackie Control Extender ...".postln;
      midiout2.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 21, 16r62,  16rF7]); // all LEDs off
      midiout2.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 21, 16r0C, 1, 16rF7]); // touchless
      midiout2.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 21, 16r61, 1, 16rF7]); // Faders min
      midiout2.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 21, 16r0A, 0, 16rF7]); // no click
      midiout2.sysex(Int8Array[ 16rf0, 0, 0, 16r66, 21, 16r0B, 1, 16rF7]); // back light off after 1 min
    //  midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 0, 32.dup(112), 16rF7].flat ) ); // clear LCD
      midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 0, {arg i; "  L " ++ (i+1).asPaddedString(2," ", \right) ++ " "}.dup(8).ascii, 16rF7].flat ) ); 
  
      midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 0] 
        ++ "Willkommen Welcome Welkom Vaelkommen Bienvenue Bemvindo  Benvenuto Bienvenidos Yokoso Hosgeldiniz Khoshumadi !".ascii ++ [16rF7]) ); 
      (16r76).do { arg i; midiout2.noteOn(0, i , 0) };
      8.do({ arg n; midiout2.bend(n, 0)});

      
      modelMessageDict = IdentityDictionary.new;
      cvMessageDict = IdentityDictionary.new;
  
  
      // ------------------------
      // bank 0: monitor controls:
      // ------------------------
      bank = 0;
      // Mackie Extender Pro:
      8.do {arg i;
        modelMessageDict.add( ("mainchannelgain" ++ (i+1).asString).asSymbol -> [\bend, i, nil, uid2, midiout2, bank, i] );  // Faders
        modelMessageDict.add( ("mainchannelmute" ++ (i+1).asString).asSymbol -> [\noteOn, 0, i + 16r10, uid2, midiout2, bank, nil, \toggle] );  // MUTEs
        modelMessageDict.add( ("mainchannelphase" ++ (i+1).asString).asSymbol -> [\noteOn, 0, i + 16r0, uid2, midiout2, bank, nil, \toggle] );  // RECs
      //  modelMessageDict.add( ("mainchanneldelay" ++ (i+1).asString).asSymbol -> [\control, 0, 16 + i, uid2, midiout2, bank] );  // V-POTs
      //  modelMessageDict.add( ("mainchannelselect" ++ i.asString).asSymbol -> [\control, 9, i + 30] );
      };
      // Mackie Control Pro:
      4.do {arg i;
        modelMessageDict.add( ("mainchannelgain" ++ (i+9).asString).asSymbol -> [\bend, i, nil, uid1, midiout1, bank, i+8] );  // Faders
        modelMessageDict.add( ("mainchannelmute" ++ (i+9).asString).asSymbol -> [\noteOn, 0, i + 16r10, uid1, midiout1, bank, nil, \toggle] );  // MUTEs
        modelMessageDict.add( ("mainchannelphase" ++ (i+9).asString).asSymbol -> [\noteOn, 0, i + 16r0, uid1, midiout1, bank, nil, \toggle] );  // RECs
      //  modelMessageDict.add( ("mainchannelselect" ++ i.asString).asSymbol -> [\control, 9, i + 30] );
      };
      bank = nil;
      modelMessageDict.add( \monitorvolume1 -> [\bend, 8, nil, uid1, midiout1, bank, nil, nil, {arg slot, value; this.displayLED1(value) } ] );
      modelMessageDict.add( \monitormute1 -> [\noteOn, 0, 16r5F, uid1, midiout1, bank, nil, \toggle] );
      modelMessageDict.add( \srdisplay -> [nil, nil, nil, uid1, midiout1, bank, nil, nil, {arg slot, value; this.displayLED2("SR: " ++ value.asInteger.asString) } ] );
      bank = 0;
      4.do {arg i;
        modelMessageDict.add( ("monitorvolume" ++ (i+2).asString).asSymbol  -> [\bend, i + 4, nil, uid1, midiout1, bank, i+8+4] );
        modelMessageDict.add( ("monitormute" ++ (i+2).asString).asSymbol  -> [\noteOn, 0, i + 4 + 16r10, uid1, midiout1, bank,nil, \toggle] );
      };

      // ------------------------
      // bank 1: device controls:
      // ------------------------
      bank = 1;
      // Mackie Extender Pro:
      8.do {arg i;
        modelMessageDict.add( ("devicevolume" ++ (i+1).asString).asSymbol -> [\bend, i, nil, uid2, midiout2, bank, i] );
        modelMessageDict.add( ("devicemute" ++ (i+1).asString).asSymbol -> [\noteOn, 0, i + 16r10, uid2, midiout2, bank, nil, \toggle] );
      };
      // Mackie Control Pro:
      8.do {arg i;
        modelMessageDict.add( ("devicevolume" ++ (i+9).asString).asSymbol -> [\bend, i, nil, uid1, midiout1, bank, i+8] );
        modelMessageDict.add( ("devicemute" ++ (i+9).asString).asSymbol -> [\noteOn, 0, i + 16r10, uid1, midiout1, bank, nil, \toggle] );
      };
      
      // Bank Switch responders:
      // Bank left > down
      NoteOnResponder({ arg src, chan, note, vel; 
        if (currentBank > 0) { 
          currentBank = currentBank - 1; 
          ("Bank: " ++ currentBank).postln;
          MXMIDI.bankChange; 
        } 
      }, uid1, 0, 16r2e, nil);
      // Bank right > up
      NoteOnResponder({ arg src, chan, note, vel; 
        if (currentBank < 7) { 
          currentBank = currentBank + 1; 
          ("Bank: " ++ currentBank).postln;
          MXMIDI.bankChange; 
        } 
      }, uid1, 0, 16r2f, nil);
    } {
      ^nil  
    };
  }
  
  displayLED1 {arg value;  // ASSIGNMENT Display
    var string;
    case 
    {  value.isNumber } { if ( (value > -10) && (value < 0) ) {
                string = value.round.asInteger.asString.padLeft(2, " ") 
              } { 
                string = value.round.abs.asInteger.asString.padLeft(2, " ") 
              }
            }
    {  value.isString } { string = value.padRight(2, " ") }
    ;
  //  string.postln;    
  //  midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r11, this.ascii2LED(string[1]), this.ascii2LED(string[0]), 16rF7].flat ) );  // sysex command doesn't work here ??
    midiout1.control(0, 74, this.ascii2LED(string[1]));
    midiout1.control(0, 75, this.ascii2LED(string[0])); }
  
  displayLED2 {arg value;  // TimeCode Display
    var string, asciiarray;
    case 
    {  value.isNumber } { string = value.round.asInteger.asString.padLeft(10, " ") }
    {  value.isString } { string = value.padLeft(10, " ") }
    ;
  //  string.postln;
    asciiarray =   string[..9].reverse.collectAs({ arg char; this.ascii2LED(char)}, Int8Array); 
  //  midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r10] ++ asciiarray ++ [16rF7] ) ); // sysex command doesn't work here ??
    asciiarray.do {arg x, i; 
      midiout1.control(0, 64 + i, x);
    };
    
  }
  
  ascii2LED { arg value;
    case 
      { value.class == Char} { value = value.ascii }
      { value.isNumber } { value = value.asString.at(0).toUpper.ascii }
      ; 
    if (value >= 64) { value = value - 64 };
  //  value.postln;
    ^value; 
  }
    
  displayDeviceName { arg slot, name;
    // 7 characters per slot
    var shift;
    name = name.asString;
  //  ("displayDeviceName " ++ slot + name ).postln;
    if (slot.notNil) {
      case 
      { slot.isNumber } {
        if (name.size < 3) {Êname = "  " ++ name };
        if (name.size < 7) { name = name ++ "        " };
        if (slot < 8) {
          shift = slot * 7;
          midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, shift, name[0..6].ascii, 16rF7].flat ) ); 
        } {
          shift = (slot - 8) * 7;
          midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, shift, name[0..6].ascii, 16rF7].flat ) ); 
        }
      }
      ;
    }
  }

  displayModelValue { arg slot, value;
    // 7 characters per slot
    // format, asString
    
    var shift;
    if (slot.notNil) {
      case 
      { slot.isNumber } {
        if (slot < 8) {
          shift = slot * 7;
          midiout2.sysex( Int8Array.newFrom( [ 16rf0, 0, 0, 16r66, 21, 16r12, 56 + shift] ++ 32 
          //  ++ value.round.asInteger.asPaddedString(3, " ").ascii 
          //  ++ value.asString(7).padLeft(7, " ").ascii 
            ++ value.asString(7).padRight(7, " ").ascii 
            ++  [16rF7].flat ) );
        } {
          shift = (slot - 8) * 7;
          midiout1.sysex( Int8Array.newFrom( [ 16rf0, 0, 0, 16r66, 16r10, 16r12, 56 + shift] ++ 32 
            //  ++ value.round.asInteger.asPaddedString(3, " ").ascii 
          //  ++ value.asString(7).padLeft(7, " ").ascii 
            ++ value.asString(7).padRight(7, " ").ascii 
            ++  [16rF7].flat ) );
        }
      }
    }
  } 
  
  
  clearLCD {
    this.clearUpperLCD;
    this.clearLowerLCD;
  }
  
  clearUpperLCD {
    midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0, 32.dup(56), 16rF7].flat ) ); 
    midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 0, 32.dup(56), 16rF7].flat ) );  }

  clearLowerLCD {
    midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 56, 32.dup(56), 16rF7].flat ) ); 
    midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 56, 32.dup(56), 16rF7].flat ) );   }
  
  bankChange { // reset all elements !  
    // clear LCD display:
    // midiout1.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 16r10, 16r12, 0, 32.dup(112), 16rF7].flat ) ); // clear LCD
    // midiout2.sysex( Int8Array.newFrom([ 16rf0, 0, 0, 16r66, 21, 16r12, 0, 32.dup(112), 16rF7].flat ) ); // clear LCD
    this.clearLCD;
    
    // reset bank dependant elements:
    (0..7).do {arg i; 
      midiout1.bend(i, 0); 
      midiout2.bend(i, 0); 
      midiout1.noteOn(0, 16r10 + i, 0); 
      midiout2.noteOn(0, 16r10 + i, 0); 
      };
      
    MXMIDI.midiModels.do {arg model, i; model.displayName }; 
  } 
}



MXMIDI { // singleton !
/*  
- manager for external MIDI controllers
- connects to MackieControl etc.
- registers device / matrix etc. parameters for MIDI remote control and display


global steuerelemente festlegen:
  devices [n] (fader, mute)
  
  main monitor (fader, mute) 
  monitors [n] (fader, mute)
  
  monitor channels [n] (fader, mute, select, phase)
  
  bank up / down 

je midi controller (Mackie Control, QCon, HUI, Tascam, nanokontrol etc.) 
  zuordnung der steuerelemente zu MIDI-Messages in eigener Klasse regeln:
  
  device[n] fader > pitch bend ch1, device[n] mute > CC 7, ch1
  etc.



Mappings for Mackie Control, QCon, HUI etc.:

8 or 16 Fader incl. Select/Solo/Mute/REC and Encoder:
  ¥ 12 Main Monitor channels:
    - encoder > delay ?
    - rec > phase ?
    - solo > ??
    - mute > on/off
    - select > selectedspeaker
    - fader > gain
  + 4 channels (> sub, near, phones, wfs ...)
    - mute > on/off
    - fader > gain
    
  ¥ Device controller:
    - encoder > ??
    - rec > analyser
    - solo > ??
    - mute > on/off
    - select > Meter on/off
    - fader > gain  

Master:
  Main Monitor gain
  
Fader bank buttons: for switching between monitor and device mode
  
Record Button:
  main monitor mute
  
LCD:  info about channel assignment
small LED: Main Monitor gain display // on or _
LED:  sampling rate     

*/  
  classvar <midiModels;   // dictionary of control Symbol -> model CV
  classvar <midiDeviceDict;   // dictionary of available MIDI controller classes
  classvar <midiDevice;     // selected midiDevice according to MXGlobals.midiControllerName
  classvar <deviceConnections;  // dictionary of device cv -> 2 SimpleControllers

  *init {
    midiModels = IdentityDictionary.new;
    deviceConnections = IdentityDictionary.new;

    MIDIIn.connectAll;

    midiDeviceDict = IdentityDictionary[
      \NanoKontrol  -> MXMIDINanoKontrol,
    //  \MackieControl8   -> MXMIDIMackieControl8,    // MackieControl w/o Extender
      \MackieControl16 -> MXMIDIMackieControl16, // MackieControl + Extender
    //  \QCon       -> MXMIDIQCon,
    //  \Tascam2400 -> MXMIDITascam2400,
    //  \BCF2000    -> MXMIDIBCF2000,
    ];
    
    midiDevice = midiDeviceDict[MXGlobals.midiControllerName].new;
    if (midiDevice.notNil) {
      this.initMIDIModels
    } { "MIDI not available".warn };
  } 
  
  *initMIDIModels {
  // dictionary of available matrix controls > model (CV)
  // devices etc. connect on request to these models
  // models are connected to available MIDI control elements (Faders, Buttons etc.)
  /*  [ \bankup, \bankdown, \mainvolume, \mainmute ].do { arg symbol, i;
      controlModels.add( symbol -> MXCV(ControlSpec(0, 1, \lin, 0), 0) ) 
    };
  */
    midiDevice.modelMessageDict.keysValuesDo { arg key, array, i;
      var message, channel, number, uid, out, bank, slot, type, displayfunc;
      message = array[0];
      channel = array[1];
      number = array[2];
      uid = array[3];
      out = array[4];
      bank = array[5];
      slot = array[6];
      type = array[7];
      displayfunc = array[8];
      midiModels.add( key -> MIDIModel( message, channel, number, uid, out, bank, slot, type, displayfunc) );
    };
  } 
  
  *connectControlToMIDI { arg cv, modelName, deviceName, inverted = false;
    var scout, scin;
    var model;
    model = midiModels[modelName];
    if (model.notNil ) {
      // make a SimpleController (scout) for the cv that updates the controlModel 
      scout = SimpleController(cv).put(\synch, {arg changer, what; // \midi
        var value;
        value = changer.input;
        if (inverted) {Êvalue = 1 - value };
        model.value_(value, changer.value);
      });
      // make a SimpleController (scin) for the controlModel that updates the cv
      scin = SimpleController(model).put(\synch, {arg changer, what; 
        var value;
        value = changer.value;
//        value = changer.input;
        if (inverted) {Êvalue = 1 - value };
//        {Êcv.input_(value, changer.type) }.defer;
        {Êcv.input_(value) }.defer;
      });
      deviceConnections.add( cv -> [scout, scin] );
      deviceName = deviceName.asSymbol;
      model.displayNameFunc = {arg slot;
        midiDevice.displayDeviceName(slot, deviceName);
      };
      if (Êmodel.displayValueFunc.isNil ) {
        model.displayValueFunc = {arg slot, value;   
          midiDevice.displayModelValue(slot, value);
        };
      };
      midiDevice.cvMessageDict.add( cv -> [modelName.asSymbol, deviceName] );
      model.displayName;
      cv.changed(\synch);
    };
  }


  *disconnectControlFromMIDI { arg cv;
    var model;
    if ( deviceConnections[cv].notNil ) {
      model = midiModels[midiDevice.cvMessageDict[cv][0]];
      model.displayNameFunc = nil;
    //  model.displayValueFunc = nil;
      
    //  deviceConnections[cv].do(_.remove);
      midiDevice.cvMessageDict.removeAt(cv);
      deviceConnections[cv].do { arg sctl;  sctl.remove;};
      deviceConnections.removeAt(cv);
      model.value = 0;
      midiDevice.displayDeviceName(model.slot, " ");
      midiDevice.displayModelValue(model.slot, " ");  // quick hack !

    };
  }
  
  *bankChange {
    // resets bank dependant elements and display device names:
    midiDevice.bankChange;  
    
    // update elements and display according to current cv values:
    deviceConnections.keys.do { arg cv; cv.changed(\synch) }; // \midi
  }
  
  *disconnectAll {
    // disconnect all midimodels
    //  
    
  }
  
  *disconnectMIDI {
    // disconnect all MIDI sources, MIDI destinations
    // MIDIClient.sources.do{ arg source, i; MIDIIn.disconnectByUID(i, source.uid) };
    // MIDIOut ??
  }
}


