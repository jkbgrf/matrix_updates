MXDelayX {
  
  *ar { arg in, samples=2;
    var sig;
    sig = in;
    samples.div(2).do({ sig = Delay2.ar(sig) });
    (samples % 2).do({ sig = Delay1.ar(sig) });
    ^sig  
  }
  
}
  
