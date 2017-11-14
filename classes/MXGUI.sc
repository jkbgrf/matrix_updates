// screen: 1920 x 1080 ??

MXGUI { // singleton !
  classvar <screen, <screenRect, <screenCenter;
  classvar <win, winPanel;
  classvar <statusPanel; 
  classvar <routingPanel, <setupPanel, <infoPanel, <meterPanel;
  classvar <volumePanel, <speakerPanel;
  classvar <rtaPanel, <fftPanel, <sonaPanel;
  classvar <mainPanel, <nearPanel, <phonesPanel, <wfsPanel;
  classvar <speakerEditorPanel;

  classvar <>mixerFont, <>mixerLabelFont, <>numberFont, <>buttonFont, <>listFont, <>editorFont;
  classvar <>groupGridHeight, <matrixCellSize = 16;
//  classvar <>textColor;
  classvar <rulerColor;
  classvar <>soloColor, <>muteColor, <>selectColor, <>listenColor, <>plugColor, <>labelColor;
  classvar <>buttonTextColor0, <>buttonTextColor1, <>buttonBackBolor0; 
  classvar <>levelBackColor, <>levelColor , <>sliderBackColor, <>sliderKnobColor;
  classvar <panelColor, <tabPanelColor;
  
  classvar <setupButtonSize, <setupButtonFont, <setupButtonTextColor, <setupButtonBackColor;
  classvar <setupName = "default", setupNameView;
  classvar <swapButton;
    classvar <infoTitleSize, <infoTitleFont, <infoTitleTextColor, <infoTitleBackColor;
    classvar <infoSize, <infoFont, <infoTextColor, <infoTextOffColor, <infoBackColor;
  classvar <infoSRView, <infoCPUView, <infoInsView, <infoOutsView;
  classvar <infoMIDIView, <infoOSCView;

  classvar <levelMeterView, <fftView, <sonaView, <rtaView;
  
  classvar <monitorTitleSize, <monitorButtonSize, <monitorSmallButtonSize, <monitorSliderSize;
  classvar <levelFontBig, <levelFontSmall;
  classvar <mainOn, <mainVolSlider, <mainVolNumber, <mainLimit, <mainMono;
  classvar <subOn, <subVolNumber, <subFilter;
  classvar <nearOn, <nearVolSlider, <nearVolNumber, <nearLimit, <nearMono, <nearLeft, <nearRight, <nearLPhase, <nearRPhase, <nearSwap;
  classvar <phonesOn, <phonesVolSlider, <phonesVolNumber, <phonesLimit, <phonesMono, 
      <phonesLeft, <phonesRight, <phonesLPhase, <phonesRPhase, <phonesSwap;
    
  classvar <speakerButtons; // Array of round buttons
  classvar <speakerMenu, <speakerGain, <speakerPhase, <speakerDelay;
  classvar <selectedSpeaker, <speakerAllOn, <speakerAllOff, <speakerReset, <speakerInvert, <modifiedSpeakers;

  classvar <routingTabs, <matrixTab, <micpreTab, <testgenTab, <inDevicesTab, <outDevicesTab;
  classvar <inDevicesPanel, <inDevicesViews;
  classvar <outDevicesPanel, <outDevicesViews;
  classvar <matrixPanel, <matrixView;
  classvar <testgenPanel, <testgenView;
  classvar <micprePanel, <micpreView;
  
  *init {
//    var screen, screenRect, screenCenter;
//    var win, winPanel;
//    var statusPanel; 
//    var routingPanel, meterPanel, volumePanel, speakerPanel;
//    var spectralPanel;

    var leftshift, topshift;
    var gap;
    var wb, bg = 3;
    var panelgap;
    var xp, yp;   // Arrays for layout guide lines / points
    var xg, yg; // grid size (= gap size)
        
    GUI.cocoa; // though we are using SC GUI classes directly instead of redirections !
  //  screen = 1920 @ (1080 - 20); // 1920 @ 1200;
  //  screen = 1920 @ 1200; // 1920 @ 1200;
  //  screen = 1920 @ 1080; // @ home
    screen = 1920 @ 1200;  // gr. Studio
    
    
    leftshift = 0; topshift = -20;  // gr. Studio
//  leftshift =  -1920;  topshift = -20; // -30;  // @ home
  
  //  leftshift =  0;  topshift = 0; // -30;  // @ mobil

  //  screen.translate(0 @ topshift.neg);   
  //  screenRect = Rect(0, 20, screen.x, screen.y - 20);
  //  screenRect = Rect(0, 0, screen.x, screen.y);
    screenRect = Rect(0, 0, screen.x, screen.y - 10);
    screenCenter = screenRect.center;

  //  panelgap = Point(screen.x * 0.01, screen.y * 0.018);
    xg = screen.x / 200;
    yg = screen.y / 120;
    xp = [0] ++ ([1, 72, 1, 34, 1, 90, 1].integrate * xg);
    yp = [0] ++ ([1, 5, 1, 36.5, 1, 36.5, 1, 37, 1].integrate * yg);

    //Font("Arial Rounded MT Bold", 10).setDefault;
    //Font("GB18030 Bitmap", 9).setDefault;
    SCFont("Arial Rounded MT Bold", 10).setDefault;
    mixerFont = SCFont("Arial", 10);
    mixerLabelFont = SCFont("Arial Narrow", 10);
  //  editorFont = SCFont("Arial Narrow", 10);
    editorFont = SCFont("Arial Narrow", 10);
    numberFont = SCFont("Arial Narrow", 10);
    buttonFont = SCFont("Arial Rounded MT Bold", 9.0);
    levelFontBig = SCFont("Arial Rounded MT Bold", 16.0);
    levelFontSmall = SCFont("Arial Rounded MT Bold", 9.0);
    listFont = SCFont("Arial", 12);

    setupButtonFont = SCFont("Arial Rounded MT Bold", 12.0);
  //  infoTitleFont = SCFont("Arial Rounded MT Bold", 9.0);
    infoTitleFont = SCFont("Arial Rounded MT Bold", 12.0);
      infoFont = SCFont("Arial Rounded MT Bold", 12.0);

    panelColor = Color.grey(0.1);
    tabPanelColor = Color.grey(0.3);

    plugColor =  Color.new(0.0, 1.0, 1.0); //    cyan
    listenColor =  Color.green;
    soloColor =  Color.red;
    muteColor =  Color(1.0, 0.6, 0.0);    // orange
    selectColor =  Color.green;
    labelColor =  Color.white;  

    setupButtonTextColor =  Color.grey(0.7);        setupButtonBackColor =  Color.grey(0.3);
    infoTitleTextColor =  Color.grey(0.7);        infoTitleBackColor =  Color.grey(0.0, 0.0);
    infoTextColor =  plugColor;
    infoTextOffColor = Color.grey(0.2);
    infoBackColor =  Color.grey(0.1);
    rulerColor = Color.grey(0.25);
  //  textColor = Color.grey(0.7);

    buttonTextColor0 =  Color.grey(0.7);  
    buttonTextColor1 =  Color.black;  
    buttonBackBolor0 =  Color.black;  
    
  //  levelColor = Color.grey(0.5); // Color.yellow(0.8);
    levelColor = plugColor; // Color.yellow(0.8);
    levelBackColor = Color.grey(0.2); // Color.grey(0.1);
    sliderBackColor = Color.grey(0.2);
    sliderKnobColor = Color.grey(0.5);
    

  //  groupGridHeight = 14;
  //  matrixCellSize = 16;

    
    win = SCWindow("MATRIX", screenRect, resizable: false, border: false);
  //  win.view.background = Color.black;
  //  win.bounds = win.bounds.left_(leftshift).top_(topshift);
    win.front;
  //  win.fullScreen;
    
    win.view.keyDownAction = { arg view, char, modifiers, unicode,keycode;
      if (char == $w, { win.endFullScreen; win.minimize; });
    };
    
    wb = win.view.bounds;   
    winPanel = SCCompositeView(win, Rect(0, 0, wb.width, wb.height));
    winPanel.background = Color.black;
  //  winPanel.decorator = FlowLayout(winPanel.bounds, panelgap, panelgap );
      
  //  setupPanel = SCCompositeView(winPanel, Rect(panelgap.x, (2*panelgap.y)+(screen.y*0.638), screen.x*0.16, screen.y*0.31));
    setupPanel = SCCompositeView(winPanel, Rect(xp[1], yp[1], xp[4]-xp[1], yp[2]-yp[1] ));
    setupPanel.background = Color.grey(0, 0); // panelColor;
    setupPanel.decorator = FlowLayout(setupPanel.bounds, margin: 0@0, gap: 5@5); // margin: 6@6, gap: 12@12;

    infoPanel = SCCompositeView(winPanel, Rect(xp[5], yp[1], xp[6]-xp[5], yp[2]-yp[1] ));
    infoPanel.background = Color.grey(0, 0); // panelColor;
    infoPanel.decorator = FlowLayout(infoPanel.bounds, margin: 0@0, gap: 5@5); // margin: 6@6, gap: 12@12;

//    routingPanel = SCCompositeView(winPanel, Rect(panelgap.x, panelgap.y, screen.x*0.345, screen.y*0.638));
    routingPanel = SCCompositeView(winPanel, Rect(xp[1], yp[3], xp[2]-xp[1], yp[6]-yp[3] ));
    routingPanel.background = panelColor;
  //  routingPanel.decorator = FlowLayout(routingPanel.bounds);

//    meterPanel = SCCompositeView(winPanel, Rect((2*panelgap.x)+(screen.x*0.16), (2*panelgap.y)+(screen.y*0.638), screen.x*0.175, screen.y*0.31));
    meterPanel = SCCompositeView(winPanel, Rect(xp[1], yp[7], xp[2]-xp[1], yp[8]-yp[7] ));
    meterPanel.background = panelColor;
    meterPanel.decorator = FlowLayout(meterPanel.bounds, margin: 5@5);

//    volumePanel = SCCompositeView(winPanel, Rect((2*panelgap.x)+(screen.x*0.345), panelgap.y, screen.x*0.175, screen.y*0.638));
    volumePanel = SCCompositeView(winPanel, Rect(xp[3], yp[3], xp[4]-xp[3], yp[6]-yp[3] ));
    volumePanel.background = panelColor;
  //  volumePanel.decorator = FlowLayout(volumePanel.bounds);

//    speakerPanel = SCCompositeView(winPanel, Rect((2*panelgap.x)+(screen.x*0.345), (2*panelgap.y)+(screen.y*0.638), screen.x*0.175, screen.y*0.31));
    speakerPanel = SCCompositeView(winPanel, Rect(xp[3], yp[7], xp[4]-xp[3], yp[8]-yp[7] ));
    speakerPanel.background = panelColor;
  //  speakerPanel.decorator = FlowLayout(speakerPanel.bounds);
    

//    sonaPanel = SCCompositeView(winPanel, Rect((3*panelgap.x)+(screen.x*0.52), (3*panelgap.y)+(screen.y*0.62), screen.x*0.44, screen.y*0.31));
    sonaPanel = SCCompositeView(winPanel, Rect(xp[5], yp[3], xp[6]-xp[5], yp[4]-yp[3] ));
    sonaPanel.background = panelColor;

//    fftPanel = SCCompositeView(winPanel, Rect((3*panelgap.x)+(screen.x*0.52), (2*panelgap.y)+(screen.y*0.31), screen.x*0.44, screen.y*0.31));
//    fftPanel = SCCompositeView(winPanel, Rect(xp[5], yp[5], xp[6]-xp[5], yp[6]-yp[5] ));
    fftPanel = SCCompositeView(winPanel, Rect(xp[5], yp[5], xp[6]-xp[5], yp[6]-yp[5] ));
    fftPanel.background = panelColor;

//    rtaPanel = SCCompositeView(winPanel, Rect((3*panelgap.x)+(screen.x*0.52), panelgap.y, screen.x*0.44, screen.y*0.31));
//    rtaPanel = SCCompositeView(winPanel, Rect(xp[5], yp[3], xp[6]-xp[5], yp[4]-yp[3] ));
    rtaPanel = SCCompositeView(winPanel, Rect(xp[5], yp[7], xp[6]-xp[5], yp[8]-yp[7] ));
    rtaPanel.background = panelColor;
  //  rtaPanel.decorator = FlowLayout(spectralPanel.bounds);

    routingTabs = TabbedView2(routingPanel, routingPanel.bounds.moveTo(0,0))
      .backgrounds_([tabPanelColor])   // Color.grey(0.5)
      .labelColors_([Color.grey(0.8)])
      .unfocusedColors_([Color.grey(0.4)])
      .stringColors_([Color.grey(0)])
      .stringFocusedColors_([Color.grey(0)])
      .followEdges_(true)
      .dragTabs_(false)
      .lockEdges_(true)
      .tabPosition_(\top)
      .lockPosition_(true)
      .tabWidth_( floor(routingPanel.bounds.width / 5 - 0.25) )
    //  .tabWidth_(\auto)
      .tabHeight_(25) 
      .labelPadding_(6)
      .tabCurve_(8)
      .font_(setupButtonFont)
      ;
      
    inDevicesTab = routingTabs.add("Input Devices", scroll: true);
    matrixTab = routingTabs.add("Matrix", scroll: false);
    outDevicesTab = routingTabs.add("Output Devices", scroll: true);
    micpreTab = routingTabs.add("MicPreAmp", scroll: false);
    testgenTab = routingTabs.add("Testgenerator", scroll: false);
    

  //  this.makeRoutingViews;
    this.makeSetupViews;
    this.makeInfoViews;
  //  this.makeMeterViews;
  //  this.makeVolumeViews; 
  //  this.makeSpeakerViews;
  //  this.makeSpectralViews;

  } 

  *makeSetupViews {
    var radius=5, shifty= -2;
    
    setupButtonSize = Point( (setupPanel.bounds.width - (setupPanel.decorator.gap.x * 8)) / 9 - 0.1, 24);
    
    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Reset", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)
      .action_({ arg view;
        MXMain.reset;
      });

    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Reload Setup", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)
      .action_({ arg view;
        if (MXMain.setupPathName.notNil) {
          MXMain.loadSetup(
            MXMain.setupPathName, 
            { arg result, pathname; setupNameView.string = PathName(pathname).fileNameWithoutExtension }
          );
        };
      });

    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Load Setup", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)     
      .action_({ arg view;
        MXMain.openSetup({ arg result, pathname;
          setupNameView.string = PathName(pathname).fileNameWithoutExtension;
        });
      });

    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Save Setup", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)
      .action_({ arg view;
        MXMain.saveAs({ arg result, pathname;
          setupNameView.string = PathName(pathname).fileNameWithoutExtension;
        });
      });

    // setup name static text
    setupNameView = SCStaticText(setupPanel, Point(setupButtonSize.x * 2 + setupPanel.decorator.gap.x, setupButtonSize.y))
      .font_(setupButtonFont)
      .stringColor_(plugColor)
      .background_(Color.grey(0.1))
      .align_(\center)
      .string_(setupName)
      ;
    
    swapButton = MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Swap Cards", setupButtonTextColor, setupButtonBackColor], 
            ["Swap Cards", setupButtonTextColor, plugColor]])
      .font_(setupButtonFont);
    swapButton.connect(MXIOManager.swapCV);
    
    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Shut Down", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)
      .action_({ 
        var win, t;
        win = SCWindow("Do you really want to shut off the Matrix computer?", Rect(100, 400, 450, 150));
        win.front;
        t = Task({ inf.do { arg i;  
          win.view.background = Color.red( (i / 30).fold(0.1, 0.8) );
          0.01.wait;
        }}, AppClock).start;
        win.onClose = { t.stop };
        SCButton(win, Rect(50,50,100,40))
          .font_(Font("Helvetica-Bold", 20))
          .states_([ ["Cancel", Color.black, Color.white] ])
          .action_({ win.close })
          ;
        SCButton(win, Rect(300,50,100,40))
          .font_(Font("Helvetica-Bold", 20))
          .states_([ ["OK", Color.black, Color.red] ])
          .action_({ win.close; MXMain.shutDown; })
          ;
         })
      ;

    MXButton(setupPanel, setupButtonSize, radius)
      .shifty_(shifty)
      .states_([ ["Restart Matrix", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont)
      .action_({ 
        var win, t;
      //  win = SCWindow("Do you really want to restart the Matrix?", Rect(100, 400, 450, 150));
        win = SCWindow("----- Press CMD-K ! ----- ", Rect(100, 400, 450, 150));
        win.front;
        t = Task({ inf.do { arg i;  
          win.view.background = Color.red( (i / 30).fold(0.1, 0.8) );
          0.01.wait;
        }}, AppClock).start;
        win.onClose = { t.stop };
        SCButton(win, Rect(50,50,100,40))
          .font_(Font("Helvetica-Bold", 20))
          .states_([ ["Cancel", Color.black, Color.white] ])
          .action_({ win.close })
          ;
        SCButton(win, Rect(300,50,100,40))
          .font_(Font("Helvetica-Bold", 20))
          .states_([ ["OK", Color.black, Color.red] ])
        //  .action_({ win.close; MXMain.restartMatrix })
          .action_({ win.close })
          ;
         })
      ;
  }

  *makeInfoViews {
    var ninfos = 12;
    var radius=5, shifty= -2;

    infoTitleSize = Point( (infoPanel.bounds.width - (infoPanel.decorator.gap.x * (ninfos-1))) / ninfos - 0.1, 24);
      infoSize = Point( (infoPanel.bounds.width - (infoPanel.decorator.gap.x * (ninfos-1))) / ninfos - 0.1, 24);

      // status display audio (SR, CPU, IO channels total, IO channels used)
    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("SR")
        ;
    infoSRView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_(" ")
        ;
    infoSRView.connect(MXStatus.srCV, \value);
    
    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("CPU")
        ;

    infoCPUView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_(" ")
        ;
    infoCPUView.connect(MXStatus.cpuCV, \value);  
    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("Inputs")
        ;
  
    infoInsView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_("256")
        ;

    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("Outputs")
        ;
        
    infoOutsView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_("256")
        ;

    // status display MIDI (incoming MIDI-LED, incoming OSC-LED)

    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("MIDI in")
        ;
  
    infoMIDIView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_("M")
        ;

    MXStringView(infoPanel, infoTitleSize, radius)
        .shifty_(shifty)
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\right)
        .string_("OSC in")
        ;
        
    infoOSCView = MXStringView(infoPanel, infoSize, radius )
        .shifty_(shifty)
        .font_(infoFont)
        .stringColor_(infoTextOffColor)
        .background_(infoBackColor)
        .align_(\center)
        .string_("O")
        ;
        
  }
  
  *makeRoutingViews {
    var cols, rows, hgap = 8, vgap = 10;
    cols = 3;
    rows = (MXDeviceManager.inDevices.size / cols).ceil;
  //  inDevicesPanel = SCCompositeView(inDevicesTab, Rect(0, 0, inDevicesTab.bounds.width - 15, (MXDeviceManager.inDevices.size / 3).ceil * 120)  );
    inDevicesPanel = SCCompositeView(inDevicesTab, Rect(0, 0, inDevicesTab.bounds.width - 12, rows * (140 + vgap) + 20) );
    inDevicesPanel.background = tabPanelColor; // Color.grey(0.3);
    inDevicesPanel.decorator = FlowLayout(inDevicesPanel.bounds, margin: hgap@vgap, gap: hgap@vgap);
    
    inDevicesViews = MXDeviceManager.inDevices.collect({ arg dev, i;
      var dview;
      dview = MXDeviceView(inDevicesPanel, (inDevicesPanel.bounds.width - (inDevicesPanel.decorator.gap.x * 4) / cols - 0.1) @ 140);
      dview.connect(dev);
    });
    rows = (MXDeviceManager.outDevices.size / cols).ceil;
    outDevicesPanel = SCCompositeView(outDevicesTab, Rect(0, 0, outDevicesTab.bounds.width - 12, rows * (140 + vgap) + 20 ) );
    outDevicesPanel.background = Color.grey(0.3);
    outDevicesPanel.decorator = FlowLayout(outDevicesPanel.bounds, margin: hgap@vgap, gap: hgap@vgap);
    
    outDevicesViews = MXDeviceManager.outDevices.collect({ arg dev, i;
      var dview;
      dview = MXDeviceView(outDevicesPanel, (outDevicesPanel.bounds.width - (outDevicesPanel.decorator.gap.x * 4) / cols - 0.1) @ 140);
      dview.connect(dev);
    });   
    
    if ( MXMain.micpre.notNil ) {
      micprePanel = SCCompositeView(micpreTab, micpreTab.bounds.moveTo(0,0) );
    //  micprePanel.background = Color.grey(0.3);
      micpreView = MXMain.micpre.makeGUI(micprePanel, micprePanel.bounds);
    };
  }
  
  *removeRoutingViews { 
    inDevicesViews.do({ arg view;  view.disconnect; });
    inDevicesPanel.removeAll; 
    outDevicesViews.do({ arg view;  view.disconnect; });
    outDevicesPanel.removeAll; 
    
    micpreView.close;
    micprePanel.removeAll;
  }

  *makeMatrixViews {
  //  matrixPanel = SCCompositeView(matrixTab, Rect(0, 0, 2000, 2000) );
  //  matrixPanel = SCCompositeView(matrixTab, matrixTab.bounds.moveTo(0,0).resizeBy(-15, -15) );
    matrixPanel = SCCompositeView(matrixTab, matrixTab.bounds.moveTo(0,0) );
  //  matrixPanel.decorator = FlowLayout(matrixPanel.bounds, margin: 10@10, gap: 10@10);
    
  //  matrixView = MXMatrixGUI(MXMain.matrixManager);
    matrixView = MXMatrixGUI(matrixPanel, matrixPanel.bounds, MXMatrixManager.globalMatrix);

    testgenPanel = SCCompositeView(testgenTab, testgenTab.bounds.moveTo(0,0) );
    testgenView = MXTestGeneratorGUI(testgenPanel, testgenPanel.bounds, 
      MXMatrixManager.matrices[\intestgenerator], MXMatrixManager.matrices[\outtestgenerator]);
    testgenView.connect(MXMain.testGenerator);  }
  
  *removeMatrixViews { 
    matrixView.close;  // disconnect ?
    matrixPanel.removeAll; 

    testgenView.close;  // disconnect ?
    testgenPanel.removeAll; 
  }


  *makeMeterViews {
    // 16 meters!  (eigentlich nur 12+1, aber fuer WFS besser mehr ?)

    levelMeterView = MXAudioMeterView(meterPanel, meterPanel.bounds.insetBy(5, 5), MXMeterManager.multiMeter.numChannels);   
    levelMeterView.onClose = { MXMeterManager.multiMeter.responder.disable };
    MXMeterManager.multiMeter.responder.enable;
  } 

  *removeMeterViews { 
    MXMeterManager.multiMeter.responder.disable;
    
    meterPanel.removeAll;
    meterPanel.refresh; 
  }
  
  *makeVolumeViews {
    var mwidth, cwidth, theight, bheight, sheight, pgap=45, sgap = 5, hgap = 10;
    var corner = 5, shifty = -2;
    
    setupButtonTextColor =  Color.grey(0.0);      setupButtonBackColor =  Color.grey(0.5);

    mwidth = volumePanel.bounds.width - (hgap*2) - (pgap * 2) / 3 - 1; // button width
  //  cwidth = pgap - 20; // unit / comment width
    theight = 40;
    bheight = 25;
    sheight = volumePanel.bounds.height - theight - (7 * hgap) - (6 * bheight) - 20;
    
    monitorTitleSize = Point(mwidth, theight);
    monitorButtonSize = Point(mwidth, bheight);
    monitorSmallButtonSize = Point(mwidth - sgap / 2, bheight);
    monitorSliderSize = Point(mwidth, sheight);
    
    // sgap = monitorButtonSize.x - (2 * monitorSmallButtonSize.x);
    
    mainPanel  = SCCompositeView(volumePanel, Rect(hgap, hgap, mwidth/*+cwidth+sgap*/, volumePanel.bounds.height  - hgap));
    mainPanel.background = panelColor;
    mainPanel.decorator = FlowLayout(mainPanel.bounds, margin: 0@0, gap: sgap@hgap);

    nearPanel  = SCCompositeView(volumePanel, Rect(mwidth + pgap + hgap, hgap, mwidth, volumePanel.bounds.height  - hgap));
    nearPanel.background = panelColor;
    nearPanel.decorator = FlowLayout(nearPanel.bounds, margin: 0@0, gap: sgap@hgap);

    phonesPanel  = SCCompositeView(volumePanel, Rect( (mwidth + pgap) * 2 + hgap, hgap, mwidth, volumePanel.bounds.height  - hgap));
    phonesPanel.background = panelColor;
    phonesPanel.decorator = FlowLayout(phonesPanel.bounds, margin: 0@0, gap: sgap@hgap);

    // main (+ sub)
    mainOn = MXButton(mainPanel, monitorTitleSize, corner)
      .shifty_(shifty)
      .states_([  [MXMonitorManager.mainMonitor.name, setupButtonTextColor, setupButtonBackColor],
             [MXMonitorManager.mainMonitor.name, setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    mainOn.connect(MXMonitorManager.mainMonitor.active);
    
    subOn = MXButton(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  [MXMonitorManager.subMonitor.name, setupButtonTextColor, setupButtonBackColor],
             [MXMonitorManager.subMonitor.name, setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    subOn.connect(MXMonitorManager.subMonitor.active);
    
    subVolNumber = MXNumber(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(levelColor)
      .align_(\center)
      .font_(levelFontBig)
      .unit_(" dB")
      ;
    subVolNumber.connect(MXMonitorManager.subMonitor.gain);

/*    SCStaticText(mainPanel, cwidth @ bheight )
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\left)
        .string_("dB")
        ;
*/
  //  MXNumber(mainPanel, monitorButtonSize).visible_(false); // dummy for decorator layout shifting!

    subFilter = MXButton(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["crossover", setupButtonTextColor, setupButtonBackColor],
             ["crossover", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    subFilter.connect(MXMonitorManager.mainMonitor.crossover);
    
    mainMono = MXButton(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["mono mix", setupButtonTextColor, setupButtonBackColor],
             ["mono mix", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    mainMono.connect(MXMonitorManager.mainMonitor.mono);
  
    mainLimit = MXButton(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["limiter", setupButtonTextColor, setupButtonBackColor],
             ["limiter", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    mainLimit.connect(MXMonitorManager.mainMonitor.limiter);

    mainVolNumber = MXNumber(mainPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(levelColor)
      .align_(\center)
      .font_(levelFontBig)
      .unit_(" dB")
      ;
    mainVolNumber.connect(MXMonitorManager.mainMonitor.gain);

/*    SCStaticText(mainPanel, cwidth @ bheight )
        .font_(infoTitleFont)
        .stringColor_(infoTitleTextColor)
        .background_(infoTitleBackColor)
        .align_(\left)
        .string_("dB")
        ;
*/
    mainVolSlider = MXSlider(mainPanel, monitorSliderSize, corner)
      .background_(sliderBackColor)
      .knobColor_(sliderKnobColor)
      .thumbSize_(20)
      ;
    mainVolSlider.connect(MXMonitorManager.mainMonitor.gain);
  
    // near / WFS
    nearOn = MXButton(nearPanel, monitorTitleSize, corner)
      .shifty_(shifty)
      .states_([  [MXMonitorManager.nearMonitor.name, setupButtonTextColor, setupButtonBackColor],
             [MXMonitorManager.nearMonitor.name, setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    nearOn.connect(MXMonitorManager.nearMonitor.active);
    
    nearLeft = MXButton(nearPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["L", setupButtonTextColor, setupButtonBackColor],
             ["L", setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    nearLeft.connect(MXMonitorManager.nearMonitor.channels[0].on);

    nearRight = MXButton(nearPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["R", setupButtonTextColor, setupButtonBackColor],
             ["R", setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    nearRight.connect(MXMonitorManager.nearMonitor.channels[1].on);

    nearLPhase = MXButton(nearPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["- L", setupButtonTextColor, plugColor],
             ["- L", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont);
    nearLPhase.connect(MXMonitorManager.nearMonitor.channels[0].phase);

    nearRPhase = MXButton(nearPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["- R", setupButtonTextColor, plugColor],
             ["- R", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont);
    nearRPhase.connect(MXMonitorManager.nearMonitor.channels[1].phase);

    nearSwap = MXButton(nearPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["R < > L", setupButtonTextColor, setupButtonBackColor],
             ["R < > L", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    nearSwap.connect(MXMonitorManager.nearMonitor.swap);

    nearMono = MXButton(nearPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["mono mix", setupButtonTextColor, setupButtonBackColor],
             ["mono mix", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    nearMono.connect(MXMonitorManager.nearMonitor.mono);
  
    nearLimit = MXButton(nearPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["limiter", setupButtonTextColor, setupButtonBackColor],
             ["limiter", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    nearLimit.connect(MXMonitorManager.nearMonitor.limiter);

    nearVolNumber = MXNumber(nearPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(levelColor)
      .align_(\center)
      .font_(levelFontBig)
      .unit_(" dB")
      ;
    nearVolNumber.connect(MXMonitorManager.nearMonitor.gain);

    nearVolSlider = MXSlider(nearPanel, monitorSliderSize, corner)
      .background_(sliderBackColor)
      .knobColor_(sliderKnobColor)
      .thumbSize_(20)
      ;
    nearVolSlider.connect(MXMonitorManager.nearMonitor.gain);
  
    
    // phones
    phonesOn = MXButton(phonesPanel, monitorTitleSize, corner)
      .shifty_(shifty)
      .states_([  [MXMonitorManager.phonesMonitor.name, setupButtonTextColor, setupButtonBackColor],
             [MXMonitorManager.phonesMonitor.name, setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    phonesOn.connect(MXMonitorManager.phonesMonitor.active);
    
    phonesLeft = MXButton(phonesPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["L", setupButtonTextColor, setupButtonBackColor],
             ["L", setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    phonesLeft.connect(MXMonitorManager.phonesMonitor.channels[0].on);

    phonesRight = MXButton(phonesPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["R", setupButtonTextColor, setupButtonBackColor],
             ["R", setupButtonTextColor, Color.green] ])
      .font_(setupButtonFont);
    phonesRight.connect(MXMonitorManager.phonesMonitor.channels[1].on);

    phonesLPhase = MXButton(phonesPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["- L", setupButtonTextColor, plugColor],
             ["- L", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont);
    phonesLPhase.connect(MXMonitorManager.phonesMonitor.channels[0].phase);

    phonesRPhase = MXButton(phonesPanel, monitorSmallButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["- R", setupButtonTextColor, plugColor],
             ["- R", setupButtonTextColor, setupButtonBackColor] ])
      .font_(setupButtonFont);
    phonesRPhase.connect(MXMonitorManager.phonesMonitor.channels[1].phase);

    phonesSwap = MXButton(phonesPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["R < > L", setupButtonTextColor, setupButtonBackColor],
             ["R < > L", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    phonesSwap.connect(MXMonitorManager.phonesMonitor.swap);

    phonesMono = MXButton(phonesPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["mono mix", setupButtonTextColor, setupButtonBackColor],
             ["mono mix", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    phonesMono.connect(MXMonitorManager.phonesMonitor.mono);
  
    phonesLimit = MXButton(phonesPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .states_([  ["limiter", setupButtonTextColor, setupButtonBackColor],
             ["limiter", setupButtonTextColor, plugColor] ])
      .font_(setupButtonFont);
    phonesLimit.connect(MXMonitorManager.phonesMonitor.limiter);

    phonesVolNumber = MXNumber(phonesPanel, monitorButtonSize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(levelColor)
      .align_(\center)
      .font_(levelFontBig)
      .unit_(" dB")
      ;
    phonesVolNumber.connect(MXMonitorManager.phonesMonitor.gain);

    phonesVolSlider = MXSlider(phonesPanel, monitorSliderSize, corner)
      .background_(sliderBackColor)
      .knobColor_(sliderKnobColor)
      .thumbSize_(20)
      ;
    phonesVolSlider.connect(MXMonitorManager.phonesMonitor.gain);
    
  }
  
  *removeVolumeViews { 
    mainPanel.removeAll; 
    nearPanel.removeAll; 
    phonesPanel.removeAll; 
    volumePanel.removeAll; 
    volumePanel.refresh;
  }
  
  *makeSpeakerViews {

    // reading individual angles from file ?
    var bsize, cwidth, bhgap, bvgap, sprad, angle, radius, rect, centerPoint, n;
    var corner=5, shifty= -2;

    setupButtonTextColor =  Color.grey(0.0);        setupButtonBackColor =  Color.grey(0.5);
    
    bsize = 60 @ 25;
  //  cwidth = 30;  // unit/comment width
    bhgap = 5;
    bvgap = 5;
    sprad = 30 / 2 + 5;  // circular button radius
    n = MXMonitorManager.mainMonitor.channels.size;
    angle = 2pi / n;  // orientation: channels[1] (speaker nr. 2) = top = pi/2
    rect = speakerPanel.bounds.moveTo(0,0);
    radius = min(rect.width, rect.height) * 0.5 - sprad - 5;
    centerPoint = rect.center;

                
    speakerEditorPanel  = SCCompositeView(speakerPanel, Rect.aboutPoint(centerPoint, bsize.x/2, (4*bsize.y+(3*bvgap))/2) );
    speakerEditorPanel.background = panelColor;
    speakerEditorPanel.decorator = FlowLayout(speakerEditorPanel.bounds, margin: 0@0, gap: bhgap@bvgap);

    speakerButtons = n.collect({arg ch; 
      var button, ang, pt;
      
      ang = ((ch-1) * angle) + (pi/2) + pi;
      pt = centerPoint.translate(Polar(radius, ang).asPoint);
      
      button = MXButton(speakerPanel, Rect.aboutPoint(pt, sprad, sprad), shape: \fillOval)
        .border_(5)
        .borderColor_(Color.grey(0, 0))
        .shifty_(shifty)
        .states_([  [ (ch+1).asString, setupButtonTextColor, Color.grey(0.3)],
               [ (ch+1).asString, setupButtonTextColor, Color.green] ])
      //  .focusColor_(Color.red)
        .font_(setupButtonFont)
        .mouseRightDownFunc_({ arg view; selectedSpeaker.value = ch })
        .mouseLeftDownFunc_({ arg view; selectedSpeaker.value = ch })
        ;
      button.connect(MXMonitorManager.mainMonitor.channels[ch].on);
    });

  //  selectedSpeaker = 0;    
    selectedSpeaker = MXSV( (1 .. (n)).collect(_.asSymbol), 0);   
    selectedSpeaker.action = ({ arg changer, what;
      var chan;
      chan = MXMonitorManager.mainMonitor.channels[selectedSpeaker.value];
      speakerGain.step = chan.gain.spec.step;
      speakerGain.action = { arg changer, what; 
        chan.gain = changer.value;
        changer.value = chan.gain.value;
      };
      speakerGain.valueAction = chan.gain.value;
        
      speakerPhase.action = { arg changer, what; 
        chan.phase = [-1,1][changer.value]; 
      };
      
      speakerPhase.valueAction = chan.phase.input;
      
      speakerDelay.step = chan.delay.spec.step;
      speakerDelay.action = { arg changer, what; 
        chan.delay = changer.value;
        changer.value = chan.delay.value;
      };
      speakerDelay.valueAction = chan.delay.value;
    });
      
    modifiedSpeakers = n.collect { arg i;
      SimpleController(MXMonitorManager.mainMonitor.channels[i].modified)
        .put(\synch, {Êarg changer, what;
          if (changer.value > 0) {
            { speakerButtons[i].borderColor_(Color.yellow(1, 1)) }.defer;         } { 
            {ÊspeakerButtons[i].borderColor_(Color.yellow(1, 0)) }.defer;
          };
        }
      )
    };

    speakerMenu = SCPopUpMenu(speakerEditorPanel, bsize ) // >> besser als SCMenu !!
        .font_(infoFont)
        .stringColor_(setupButtonTextColor)
        .background_(setupButtonBackColor)
//        .align_(\center)
//        .string_("speaker:" + (selectedSpeaker+1) )
        ;
    speakerMenu.connect(selectedSpeaker);

    speakerGain = MXNumber(speakerEditorPanel, bsize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(levelColor)
      .align_(\center)
      .font_(infoFont)
      .unit_(" dB")
      ;

    speakerPhase = MXButton(speakerEditorPanel, bsize, corner)
      .shifty_(shifty)
      .states_([  [ "-180¡", setupButtonTextColor, plugColor],
             [ "-180¡", setupButtonTextColor, setupButtonBackColor] ])
      .font_(infoFont)  
      ;

    speakerDelay = MXNumber(speakerEditorPanel, bsize, corner)
      .shifty_(shifty)
      .background_(levelBackColor)
      .stringColor_(plugColor)
      .align_(\center)
      .font_(infoFont)
      .unit_(" ms")
      ;

    speakerAllOff = MXButton(speakerPanel, Rect(0, 0, bsize.x, bsize.y), corner)
      .shifty_(shifty)
      .states_([  [ "all off", setupButtonTextColor, setupButtonBackColor] ])
      .font_(infoFont)  
      .action_({ arg changer, what;  speakerButtons.do(_.valueAction_(0)) })
      ;

    speakerAllOn = MXButton(speakerPanel, Rect(speakerPanel.bounds.width-bsize.x, 0, bsize.x, bsize.y), corner)
      .shifty_(shifty)
      .states_([  [ "all on", setupButtonTextColor, setupButtonBackColor] ])
      .font_(infoFont)  
      .action_({ arg changer, what;  speakerButtons.do(_.valueAction_(1)) })
      ;

    speakerReset= MXButton(speakerPanel, Rect(0, speakerPanel.bounds.height-bsize.y, bsize.x, bsize.y), corner)
      .shifty_(shifty)
      .states_([  [ "reset", setupButtonTextColor, setupButtonBackColor] ])
      .font_(infoFont)  
      .action_({ arg changer, what;  
        MXMonitorManager.mainMonitor.channels.do(_.reset);
        selectedSpeaker.value = selectedSpeaker.value;
         })
      ;

    speakerInvert= MXButton(speakerPanel, Rect(speakerPanel.bounds.width-bsize.x, speakerPanel.bounds.height-bsize.y, bsize.x, bsize.y), corner)
      .shifty_(shifty)
      .states_([  [ "invert", setupButtonTextColor, setupButtonBackColor] ])
      .font_(infoFont)  
      .action_({ arg changer, what;  
        speakerButtons.do {Êarg but, i; but.valueAction_(1 - but.value) }
      })
      ;

    speakerMenu.valueAction_(1);
    speakerMenu.valueAction_(0);
    
  }

  *removeSpeakerViews { 
    modifiedSpeakers.do(_.remove);
    speakerEditorPanel.removeAll;
    speakerEditorPanel.refresh;
    speakerPanel.removeAll;  
    speakerPanel.refresh;  
    
  }
  
  *makeSpectralViews {
    MXMeterManager.fft.makeView(fftPanel);
    MXMeterManager.sona.makeView(sonaPanel);
    MXMeterManager.rta.makeView(rtaPanel);
  //  MXMeterManager.rta.responder.enable;

    // rtaView = ?

  //  MXMeterManager.rta.guiUpdateFunc = { arg array;  { if (sonaView.notClosed) { sonaView.value = array } }.defer };
  }

  *removeSpectralViews { 
  //  MXMeterManager.fft.guiUpdateFunc = { };
    MXMeterManager.fft.removeView;
    MXMeterManager.sona.removeView;
    MXMeterManager.rta.responder.disable;     MXMeterManager.rta.removeView;
  
    rtaPanel.removeAll; 
    fftPanel.removeAll;
    sonaPanel.removeAll; 
    rtaPanel.refresh; 
    fftPanel.refresh;
    sonaPanel.refresh; 
    }
    
  
  *unsetSR {
    // remove SR dependent views:  contents of routing tabs (deviceviews, matrix, micpre ...)
    this.removeRoutingViews;
    this.removeVolumeViews;
    this.removeMeterViews;
    this.removeSpeakerViews;
    this.removeSpectralViews;
    this.removeMatrixViews;
    MXMIDI.disconnectControlFromMIDI(MXStatus.srCV);
  }
  

  *setSR {
    // build SR dependent views:  contents of routing tabs (deviceviews, matrix, micpre ...)
    this.makeVolumeViews;
    this.makeMeterViews;
    this.makeSpeakerViews;
    this.makeSpectralViews;
    this.makeRoutingViews;
    this.makeMatrixViews;
    MXMIDI.connectControlToMIDI(MXStatus.srCV, \srdisplay, "SR");

  }
  
  *startDSP {
    // activate meters, spectral views ..
    infoSRView.stringColor_(infoTextColor);
    infoCPUView.stringColor_(infoTextColor);
    infoInsView.stringColor_(infoTextColor);
    infoOutsView.stringColor_(infoTextColor);
  }   

  *stopDSP {  // ??
    // deactivate (clear) meters, spectral views ..
    infoSRView.stringColor_(infoTextOffColor);
    infoCPUView.stringColor_(infoTextOffColor);
    infoInsView.stringColor_(infoTextOffColor);
    infoOutsView.stringColor_(infoTextOffColor);
  }
  
  
}



/*

  classvar <>gui, <>skin, <screen, <screenRect, <screenCenter;
  classvar <>mixerFont, <>mixerLabelFont, <>numberFont, <>buttonFont, <>editorfont, <>listFont;
  classvar <>groupGridHeight, <matrixCellSize;
  classvar <>soloColor, <>muteColor, <>selectColor, <>listenColor, <>plugColor, <>labelColor;
  classvar <>buttonTextColor0, <>buttonTextColor1, <>buttonBackBolor0; 
  classvar <>levelBackColor, <>levelColor;

  *setGUI { arg id;
    id = id ? \cocoa;
    //if(id == \swing) { SwingOSC.default.boot };
    GUI.fromID(id);
    gui = GUI.current.id;

    screen = Point( GUI.window.screenBounds.width, GUI.window.screenBounds.height);     // iMac 20" = 1680 x 1050 pixels
    screenRect = Rect(0, 20, screen.x, screen.y - 20);
    screenCenter = screenRect.center;
      
    //Font("Arial Rounded MT Bold", 10).setDefault;
    //Font("GB18030 Bitmap", 9).setDefault;
    Font("Arial Rounded MT Bold", 10).setDefault;
    mixerFont = Font("Arial", 10);
    mixerLabelFont = Font("Arial Narrow", 10);
    editorfont = Font("Arial", 10);
    numberFont = Font("Arial Narrow", 10);
    buttonFont = Font("Arial Rounded MT Bold", 9.0);
    listFont = Font("Arial", 12);
    
    plugColor =  Color.new(0.0, 1.0, 1.0); //    cyan
    listenColor =  Color.green;
    soloColor =  Color.red;
    muteColor =  Color(1.0, 0.6, 0.0);    // orange
    selectColor =  Color.green;
    labelColor =  Color.white;  

    buttonTextColor0 =  Color.grey(0.7);  
    buttonTextColor1 =  Color.black;  
    buttonBackBolor0 =  Color.black;  
    
    levelColor = Color.yellow(0.8);
    levelBackColor = Color.grey(0.1);
    
    
    groupGridHeight = 14;
    matrixCellSize = 16;
    meterPoints = [\gain, \EQ, \delay, \fader];
    masterMeterPoints = [\pre, \fader, \limit];
    
    filterTypes = [\off, \HP, \LS, \PEQ, \BP, \BR, \HS, \LP];
    filterDefs = IdentityDictionary[ 
      \off -> \MXChannelEQX,
      \LP  -> \MXChannelEQLP,  \LS -> \MXChannelEQLS, 
      \PEQ -> \MXChannelEQPEQ, \BP  -> \MXChannelEQBP, \BR -> \MXChannelEQBR, 
      \HS -> \MXChannelEQHS,  \HP  -> \MXChannelEQHP 
      ];
  
    Document.listener
      .background_(Color.grey(0.95))
    //  .background_(Color(0.2, 0.2, 0.3))
    //  .stringColor_(Color(1.0, 1.0, 0.2))
      .stringColor_(Color.black)
      //.bounds_(Rect(0, 50, 400, 600))
      .name_("akus.mix Post Window")
      .font_(Font("Helvetica", 11))
    //  .font_(Font("Monaco", 9))
      .bounds_(Rect(MXGlobals.screenRect.right-400-10, MXGlobals.screenRect.height-600-32,  400, 600))
      ;
  }

*/
