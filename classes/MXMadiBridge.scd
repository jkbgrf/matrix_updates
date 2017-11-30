MXMadiBridge{

	*init {

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


}