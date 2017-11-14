MXOSC { // singleton !
/*  
- manager for OSC receivers / sender, NetAddr etc.
- connects to touchosc etc.
- registers device / matrix etc. parameters for OSC remote control and display
  
  
*/  
  classvar <serveraddr;
  classvar <langaddr;
  
  
  *init {
    // initialize OSC-"Devices" (list of NetAddr + receivers)
    serveraddr = NetAddr("127.0.0.1", 57110);
    langaddr = NetAddr("127.0.0.1", NetAddr.langPort);
  } 
  
  
}
