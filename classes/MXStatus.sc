MXStatus { // singleton !
  classvar server;      
  classvar runningCtl;    // SimpleController for /running
  classvar countsCtl;   // SimpleController for /counts
  classvar <runningCV;    // MXCV [0, 1];
  classvar <cpuCV;      // MXCV [0.0 ... 1.0] % (peakCPU)
  classvar <synthsCV;   // MXCV Integer (numSynths)
  classvar <srCV;     // MXCV Float
  classvar <actualsrCV;   // MXCV Float
  classvar <clockDiff;    // MXCV [0, 1];
  classvar <warnwin;    // SCWindow, floating, warns about clock problems
    
  *init {arg argserver;
    server = argserver ? MXGlobals.server;
    runningCV = MXCV( ControlSpec(0, 1, \lin, 1), 0);
    runningCV.action_({ arg changer, what;
    }); 
    

    clockDiff = MXCV( ControlSpec(0, 1, \lin, 1), 0); // for warning about sr problems ...
    clockDiff.action_({ arg changer, what;
    //  var win;
      if ((changer.value > 0) && warnwin.isNil ) {
        warnwin = SCWindow("warning", Rect(200, 500, 1520, 200), border: false)
          .front
          .alwaysOnTop_(true)
          .alpha_(0.8);
        warnwin.view.background_(Color.yellow(1.0));
  
        SCStaticText(warnwin, 1520@200)
          .font_(Font("Helvetica-Bold", 44))
          .align_(\center)
          .stringColor_(Color.grey(0))
          .string_("check wordclock generator and restart Matrix");     };
      if (warnwin.notNil) {
        if ((changer.value == 0) && (warnwin.isClosed.not) ) {
        warnwin.close;
        warnwin = nil;
        };        
      };
    });   

    cpuCV = MXCV( ControlSpec(0, 100, \lin, 0.1), 0.0);   
    synthsCV = MXCV( ControlSpec(0, MXGlobals.maxNodes, \lin, 1), 0);   
    srCV = MXCV( MXSimpleSpec(0, 0), 0.0);   
    actualsrCV = MXCV( MXSimpleSpec(0, 0), 0.0);  
    
    
    runningCtl = SimpleController(server).put(\serverRunning, {     runningCV.value = server.serverRunning.binaryValue;
    });
    
    countsCtl = SimpleController(server).put(\counts, {
      cpuCV.value = server.peakCPU.round(0.1);
      synthsCV.value = server.numSynths;
      srCV.value = server.sampleRate;
      actualsrCV.value = server.actualSampleRate;
    });
  
    actualsrCV.action_({ arg changer, what;
      if (runningCV.value > 0) {
        if ( (abs( actualsrCV.value - srCV.value ) > 100) && (clockDiff.value == 0) ) {
          clockDiff.value = 1;
        //  clockWarning on;
        } {
          if ( (abs( actualsrCV.value - srCV.value ) < 10) && (clockDiff.value == 1) ) {
            clockDiff.value = 0;
          //  clockWarning off
          }   
        }     
      }   
    });
    
    server.startAliveThread;
  
  }
  
  
  
  
}
