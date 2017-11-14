MXMIDIRemote {
// abstract class for MIDI remote controlled devices (RME Micstasy, M16 DA etc.)

/*  

n values 
per value:
  CV
  GUI-View
  MIDI Func
  MIDI sysex sequence or controllers

m groups for values (similar elements 

1 gui 

1 midiinport
1 midioutport

1 device id


methods:

  init - make cvs
     MIDI sysex seqs

  connect (MIDI)
  
  disconnect (MIDI)
  
  updater (sync to MIDI to device)
  
  makeGUI
  
  

*/  
  
}

MXMIDIRemoteValue {
// class for single remote controlled values
  var <device;      // the instance of the "parent" remote device
  var <name;
  var <cv;
  var <>midimax = 127;    // Integer, scales the normalized value (cv.input) to the midi data range
  var <>uid;        // receives from 
  var <>midiout;      // sends to
  var <>midiinfunc;     // MIDIFunc, responder for receiving MIDI messages from the device
  var <>midioutfunc;    // Function for sending MIDI messages to the device
  var <>parambyte;    // single byte containing the parameter number (only for RME Micstasy ??)
  
  *new { arg name, device;
    ^super.new.init( name, device )
  }
  
  init { arg argname, argdevice;
    name = argname ? \value;
    device = argdevice;
    
  }
  
  send {
    if (midioutfunc.notNil) {
      midioutfunc.value((cv.input * midimax).asInteger);
    //  (name.asString + cv.value + "sent!").postln;
    };
  }
  
  sync {
    
  }
  
  request {
  // same as sync ? 
  } 
  
  cv_ { arg mxcv;
  //  mxcv.postln;
    cv = mxcv;
    cv.action_({  this.send }); 
  //  cv.postln;
  //  (name.asString + "cv set!").postln;
  }
  
  value_ { arg val;
    cv.value = val; 
  }

/*    
  update {arg changer, what;
    if (what == \synch) { 
      this.value = changer.input;
    };
  }
*/  
}

MXMIDIRemoteByte : MXMIDIRemoteValue { 
// class for up to 8 bits of a MIDI sysex data byte
  var <bits;      // Order of max. 8 MXMIDIRemoteBit objects

  // whenever this cv.value changes all bits must be updated
  // on update sends all bits to the device

  init { arg argname, argdevice;
    name = argname ? \value;
    device = argdevice;
    cv = MXCV(ControlSpec(0, 127, \lin, 1), 0); 
    cv.action = { this.send };
    bits = Order.new;
  }
  
  setBits { arg value, lsb, numbits;
    var bitfield, newvalue;
    newvalue = cv.value.asInteger;
    bitfield = value.asInteger.asBinaryString.reverse.collectAs({arg char;  char.digit.booleanValue }, Array);
    (value.asInteger).max(1).numBits.do {arg i;  newvalue  = newvalue.setBit(lsb + i, bitfield[i]) };
    cv.value = newvalue;
  }

  send {
    if (midioutfunc.notNil) {
      midioutfunc.value(cv.value.asInteger);
    //  (name.asString + cv.value + "sent!").postln;
    };
  }

  
}

MXMIDIRemoteBits {
// class representing a value smaller than a full data byte (nibbles or even single bits)
  var <name;
  var <cv;
  var <lsb;       // Integer, least significant bit (0 .. 7)
  var <numBits;   // Integer, number of bits represented by this object ( 1 .. 8)
  var <parentByte;    // MXMIDIRemoteByte
        
  // whenever this cv.value changes the parentByte must be updated

  *new { arg name, lsb, numBits, parentByte;
    ^super.new.init( name, lsb, numBits, parentByte )
  }
  
  init { arg argname, arglsb, argnumBits, argparentByte;
    name = argname ? \value;
    lsb = arglsb ? 0;
    numBits = argnumBits ? 1;
    parentByte = argparentByte;
    parentByte.bits[lsb] = this;
    cv = MXCV(ControlSpec(0, 2 ** (numBits - 1), \lin, 1), 0);    cv.action = {
      parentByte.setBits(cv.value, lsb, numBits);   };  
  
  }

  value_ {arg val;
    cv.value = val; 
    
  }     
}


MXRemoteMicstasy {
  var <deviceName = "MCU Pro USB v3.1" ; // "USB Midi        "; //n "IAC Driver"; // 
  var <portInName =  "Anschluss 4"; // "USB Midi        ";  // "Bus 1"; // 
  var <portOutName = "Anschluss 4"; //  "USB Midi        "; // "Bus 1"; // 
  var <uid;       // receives from 
  var <midiout;     // sends to
  var <deviceID;    // Integer
  var <gui;       // SCCompositeView holding all specific views
  var <valuesDict;    // Dictionary [ name -> MXMIDIRemoteValue or MXMIDIRemoteByte]
  var <groupsDict;    // Dictionary [ groupname -> Array of valuesDict-keys ], for n channels, setup groups etc.
  var <gains;     // Array of MXCV
  var <meters;      // Array of MXLEDMeterView
  var <testButtons;   // Array of MXButtons for the Micstasy test generator
  var <testCV;      // MXCV for the Micstasy test generator

// to sync with the device: send "request value" (10h) 1x per second 
// and respond to message type 30h (receives 27 parameters in pairs: 1 byte param number, 1 byte data )

// to show level meter: send a "request level meter data" (11h) 10x per second 
// and respond to message type 31h (receives 1 byte ( only 4 bits) per channel )

  *new { arg deviceID;
    ^super.new.init( deviceID )
  } 
  
  init { arg argdeviceID;
    var eox = 0xF7;
    var header;
    var endpoint;
    
    endpoint = MIDIIn.findPort(deviceName, portInName);
    midiout = MIDIOut.findPort(deviceName, portOutName);
    if (endpoint.isNil) { (deviceName + "MIDI in port" +  portInName + "not found!").warn; } { uid = endpoint.uid; } ;
    if (midiout.isNil) { (deviceName + "MIDI out port" +  portOutName + "not found!").warn; };

    if ( endpoint.notNil && midiout.notNil  ) {
      midiout = MIDIOut.newByName(deviceName, portOutName);     "Initializing RME Micstasy MIDI remote ...".postln;
      deviceID = argdeviceID ? 0x7F;
      header = Int8Array[0xf0, 0x00, 0x20, 0x0d, 0x68, 0x7f];
      valuesDict = IdentityDictionary.new;
      gains = { MXCV(ControlSpec(-9, 76.5, \lin, 0.5), 0) } ! 8;
      meters = Array.newClear(8);
      testButtons = Array.newClear(8);
      testCV = MXCV(ControlSpec(0, 8, \lin, 1), 0); // 0=off or on channel 1 to 8
      
      8.do { arg i;  // Channels Gain coarse
        var remotevalue, name, parambyte;
        name = ("ch" + (i+1) + "gain coarse").asSymbol;
        parambyte = i*3;
        remotevalue = MXMIDIRemoteValue(name, this);
        remotevalue.cv_(MXCV(ControlSpec(-9, 76, \lin, 1), 0));
        remotevalue.midimax = 85;
        remotevalue.parambyte = parambyte;
        remotevalue.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( name -> remotevalue );
        
      };
  
      8.do { arg i; // Channels Gain Fine Settings
        var remotebyte, remotevalue, bytename, paramname, parambyte, parambit;
        bytename = ("ch" + (i+1) + "fine").asSymbol;
        parambyte = i*3 + 1;
        remotebyte = MXMIDIRemoteByte(bytename, this);
    //    remotebyte.cv = MXCV(ControlSpec(0, 127, \lin, 1), 0);
        remotebyte.parambyte = parambyte;
        remotebyte.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( bytename -> remotebyte );
      
        paramname = ("ch" + (i+1) + "fine 0.5").asSymbol;
        parambit = 0;
        remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
        valuesDict.add( paramname -> remotevalue );
        
        gains[i].action = { arg changer, what;
          var coarse, fine;
          fine = changer.value - changer.value.floor;
          coarse = changer.value - fine;
          valuesDict[ ("ch" + (i+1) + "gain coarse").asSymbol; ].cv.value = coarse.asInteger;
          valuesDict[ ("ch" + (i+1) + "fine 0.5").asSymbol; ].cv.value = (fine * 2).asInteger;
        };
  
        if ( i > 0) {
          paramname = ("ch" + (i+1) + "d-link").asSymbol;
          parambit = 1;
          remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          valuesDict.add( paramname -> remotevalue );
        } {
          paramname = ("ch 1" + "digitalout").asSymbol;
          parambit = 1;
          remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          valuesDict.add( paramname -> remotevalue );
        };
  
        if ( i == 0) {
          paramname = ("ch 1" + "display").asSymbol;
          parambit = 6;
          remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          valuesDict.add( paramname -> remotevalue );
        };
      };
  
      8.do { arg i; // Channels Settings
        var remotebyte, remotevalue, bytename, paramname, parambyte, parambit;
        bytename = ("ch" + (i+1) + "settings").asSymbol;
        parambyte = i*3 + 2;
        remotebyte = MXMIDIRemoteByte(bytename, this);
    //    remotebyte.cv = MXCV(ControlSpec(0, 127, \lin, 1), 0);
        remotebyte.parambyte = parambyte;
        remotebyte.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( bytename -> remotebyte );
      
        ["input", "Hi Z", "Autoset", "Lo Cut", "M/S", "Phase", "48V"].do {arg name, b;
          paramname = ("ch" + (i+1) + name).asSymbol;
          parambit = b;
          remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          valuesDict.add( paramname -> remotevalue );
        };
      };
        
      1.do { arg i; // Setup 1
        var remotebyte, remotevalue, bytename, paramname, parambyte, parambit;
        bytename = "setup1".asSymbol;
        parambyte = 0x18;
        remotebyte = MXMIDIRemoteByte(bytename, this);
        remotebyte.parambyte = parambyte;
        remotebyte.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( bytename -> remotebyte );
      
        ["int freq", "clockspeed", "clocksource", "outdBref"].do {arg name, b;
          paramname = name.asSymbol;
          if (b == 0) {
            parambit = b;
            remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          } {
            parambit = b * 2 - 1;
            remotevalue = MXMIDIRemoteBits(paramname, parambit, 2, remotebyte);
          };
          valuesDict.add( paramname -> remotevalue );
        };
      };
  
      1.do { arg i; // Setup 2
        var remotebyte, remotevalue, bytename, paramname, parambyte, parambit;
        bytename = "setup2".asSymbol;
        parambyte = 0x19;
        remotebyte = MXMIDIRemoteByte(bytename, this);
        remotebyte.parambyte = parambyte;
        remotebyte.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( bytename -> remotebyte );
      
        ["lock", "peakhold", "followclock", "autosetlimit", "delaycomp", "autodevice"].do {arg name, b;
          paramname = name.asSymbol;
          if (b == 3) {
            parambit = 3;
            remotevalue = MXMIDIRemoteBits(paramname, parambit, 2, remotebyte);
          } { 
            parambit = (b==5).if(6, b);
            remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          };
          valuesDict.add( paramname -> remotevalue );
        };
      };
  
      1.do { arg i; // Lock / Sync
        var remotebyte, remotevalue, bytename, paramname, parambyte, parambit;
        bytename = "sync".asSymbol;
        parambyte = 0x1A;
        remotebyte = MXMIDIRemoteByte(bytename, this);
        remotebyte.parambyte = parambyte;
        remotebyte.midioutfunc = {arg cvinput;
          midiout.sysex(header ++ Int8Array[0x20, parambyte, cvinput, eox] );
        };
        valuesDict.add( bytename -> remotebyte );
      
        ["optionlock", "optionsync", "aeslock", "aessync", "wcklock", "wcksync", "wcout"].do {arg name, b;
          paramname = name.asSymbol;
          parambit = b;
          remotevalue = MXMIDIRemoteBits(paramname, parambit, 1, remotebyte);
          valuesDict.add( paramname -> remotevalue );
        };
      };
    } {
      ^nil  
    };
    
  }
  
  makeGUI { arg parent, bounds;
    var panelWidth, panelHeight, panelBounds;
    var channelPanels, settingsPanel;
    var buttonSize;
    var smallFont, bigFont, titleFont, buttonFont;
    var stringColor, shifty = 0, radius = 5, border=0;
    var buttonBackColorOff, buttonBackColorOn;
    var buttonTextColorOff, buttonTextColorOn;
    
//    bounds = parent.bounds;
    gui = SCCompositeView(parent, bounds);
    gui.decorator = FlowLayout(gui.bounds, margin: 5 @ 5, gap: 6 @ 5);
    
    panelWidth = (bounds.width - 6) / 9 - 6;
    panelHeight = bounds.height - 6;
    
    buttonSize = (panelWidth - 10) @ 22;
    smallFont = SCFont("Arial", 9); // SCFont("Arial Rounded MT Bold", 10.0); 
    buttonFont = SCFont("Arial Rounded MT Bold", 10.0); // small buttons
    bigFont = MXGUI.levelFontBig;
    titleFont = MXGUI.infoTitleFont;

    stringColor = Color.white;  
    
    buttonBackColorOff = Color.grey(0.5);
    buttonBackColorOn = Color.grey(0.5);
    buttonTextColorOff = Color.grey(0.0);
    buttonTextColorOn = Color.grey(0.0);
      
    channelPanels = 8.collect {arg i;
      var p, view;
      p = SCCompositeView(gui, panelWidth @ panelHeight);
      p.background = MXGUI.panelColor.copy.alpha_(0.25); 
      p.decorator = FlowLayout(p.bounds, margin: 5@5, gap: 5@5);
      
    //  p.decorator.shift(0, 5);

      MXStringView(p, buttonSize.x @ 30)
        .shifty_(shifty)
        .font_(bigFont)
        .stringColor_(MXGUI.infoTitleTextColor)
        .background_(Color.clear)
        .align_(\center)
        .string_( "Ch" + (i+1));

      p.decorator.nextLine;
      p.decorator.shift(0, 5);

      view = MXNumber(p, buttonSize.x @ 30, radius)
        .shifty_(shifty)
        .border_(border)
        .borderColor_(Color.grey(0.5))
        .font_(bigFont)
        .unit_(" dB")
        .inset_(0)
        .background_(MXGUI.levelBackColor)
        .stringColor_(MXGUI.levelColor)
        .align_(\center)
      ; 
      view.connect(gains[i]);

      p.decorator.nextLine;
      p.decorator.shift(0, 10);
      
      meters[i] = MXLEDMeterView(p, buttonSize.x @ 200, 8)
        .ledValues_([-70, -60, -50, -42, -36, -30, -24, -18, -12, -6, -3, -1, -0.1])
        .font_(smallFont)
        .stringColor_(stringColor)
        ;
        
      p.decorator.nextLine;
      p.decorator.shift(0, 10);
      
        
      // [ "input", "Hi Z", "Autoset", "Lo Cut", "M/S", "Phase", "48V" ]
              
      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "48V", buttonTextColorOff, buttonBackColorOff],
                    [ "48V", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "48V").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "Phase", buttonTextColorOff, buttonBackColorOff],
                    [ "Phase", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "Phase").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "M/S", buttonTextColorOff, buttonBackColorOff],
                    [ "M/S", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "M/S").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "Lo Cut", buttonTextColorOff, buttonBackColorOff],
                    [ "Lo Cut", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "Lo Cut").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "Autoset", buttonTextColorOff, buttonBackColorOff],
                    [ "Autoset", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "Autoset").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "D-Link", buttonTextColorOff, buttonBackColorOff],
                    [ "D-Link", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      if (i > 0) {Êview.connect( valuesDict[  ("ch" + (i+1) + "d-link").asSymbol; ].cv ); };

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "Hi Z", buttonTextColorOff, buttonBackColorOff],
                    [ "Hi Z", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "Hi Z").asSymbol; ].cv );

      view = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "XLR In", buttonTextColorOff, buttonBackColorOff],
                    [ "LINE In", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      view.connect( valuesDict[ ("ch" + (i+1) + "input").asSymbol; ].cv );
    
      p.decorator.nextLine;
      p.decorator.shift(0, 10);
      
      testButtons[i] = MXButton(p, buttonSize, radius)
        .shifty_(shifty)
        .states_([  [ "Test Gen.", buttonTextColorOff, buttonBackColorOff],
                    [ "Test Gen.", buttonTextColorOn, MXGUI.plugColor] ])
        .font_(buttonFont);
      // only one of the testbuttons can be active or none
    };
    
  /* general:
    off/online - Button
    lock keys - Button
    dark - Button
  */  
    
  /* per channel:
    channel number - StringView ("Channel" + Nr)
  
  
    gain - Number (+ "dB")
    sel   - Button (sync gain changes over channels)
    Test - Button (aktiviert Test Osc , nur jeweils 1 Kanal moeglich!)
    
    8 Buttons:  48V, Phase, M/S, LoCut, Auto, D-Link, Hi Z, Front
    
  */  
  
  /* Settings:
    Analog Out - Menu
    AutoSet Lim - Menu
    Peakhold - Button
    DelayComp - Button 
    ( AutoID - Button )
    AES/ADAT out - Button
  */
  
  /* Clock:
    Source - Menu
    Clock Freq - Menu (44/48 incl. SS, DS, QS)
    State - StringView
    DS/QS - StringView
    follow Clock - Menu
    WCK single - Button
    
    WCK / AES / OPT - Lock/Sync state 3x StringView
    
  */    
    
      
  } 
  
  connect {
    // start syncing Task 
  }
  
  disconnect {
    // stop sync Task
  }
  
}
