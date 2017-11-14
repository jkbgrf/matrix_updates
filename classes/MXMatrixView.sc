
/*

MXMatrixGUI - 
  compositeview fuer Set-Verwaltung (new, clear, select, merge ...)
  compositeview fuer selected Node 
  compositeview fuer Grid
  
MXMatrixGrid - View/Editor fuer Matrizen, zeigt jeweils nur eine Matrix

MXMatrixBox - View fuer einen Matrixknoten

*/



// ----------------- Matrix GUI -------------------------


MXMatrixGUI {

  var <parent, <win, <bounds, <view, <gridview, <grid, <nodeview;
  var inArray, outArray;
  var <inLabels, <outLabels;
  var matrix;
  var <muteButton, <clearButton;
  var <testButton, <testLevelBox, <testTypeMenu, <testsynth;
  var <inDeviceName, <outDeviceName, <inNum, <outNum, <inBus, <outBus, <valNum, <valSlider, <level;
//  var rowup, rowdown, colleft, colright;
  var selRow, selCol;
  var buttonSize, font, <cellSize;
  var <m, <mapseteditor, <mapsetModel;
//  var <up, <down, <left, <right, <plus, <minus, <levelon, <leveloff;
  var <set;
  var <testtoggle, <testlevel, <testtype;
/*
  *new { arg manager;
    ^super.new.initMatrixGUI(manager);
  }
*/
  *new { arg parent, bounds, matrix;
    ^super.new.initMatrixGUI(parent, bounds, matrix);
  }
  
  initMatrixGUI { arg argparent, argbounds, argmatrix;
    parent = argparent;
    bounds = argbounds ? parent.bounds;
    matrix = argmatrix ? MXMain.matrixManager.globalMatrix;
  //  inArray = arginArray;
  //  outArray = argoutArray;

    cellSize = MXGUI.matrixCellSize;
    font = MXGUI.editorFont;

    view = parent;
    
    nodeview = SCCompositeView(view, Rect(0, 0, view.bounds.width, 80));
    nodeview.background = Color.grey(0.3);
    nodeview.decorator = FlowLayout(nodeview.bounds, margin: Point(10,10), gap: Point(4,10));

    gridview = SCCompositeView(view, Rect(0, 80, view.bounds.width, view.bounds.height - 80));
    gridview.background = Color.grey(0.3);
  //  gridview.decorator = FlowLayout(gridview.bounds, margin: Point(0,0), gap: Point(0,0));
    
    this.prInitModels;
  //  this.prInitMappings;

    this.makeGrid;
    this.makeNodeView;
    
  //  selRow.value = 0;
  //  selCol.value = 0;
  }
  
  prInitModels {  
  
    level =  MXCV(ControlSpec(-90.0, 0.0, -2, 0.5), -90.0);

    testtoggle =  MXCV( ControlSpec(0, 1, \lin, 1), 0);
    testtoggle.action = { arg changer, what;
    //  set.setTestStatus( manager.outbuslist[selCol.value], changer.value );
      if( testtoggle.value == 1 ) { grid.setCursorColor_(Color.cyan) } { grid.setCursorColor_(Color.red) };
      testsynth.set(\gate, 0);
      if( testtoggle.value == 1 ) { 
        testsynth = SynthDef("quicktest", {arg out=0, on=0, gain= -20, gate=1;
          var sig;
          sig = WhiteNoise.ar(gain.dbamp);
          sig = sig * on;
          sig = sig * Linen.kr(gate, MXGlobals.switchrate, 1, MXGlobals.switchrate, doneAction: 2);
          Out.ar(out, sig);
        }, [0, MXGlobals.switchrate, MXGlobals.levelrate, 0] ).play(matrix.group, 
          [ \out, matrix.toBusArray[selRow.value].busNum, 
            \on, (level.value > -90).if( level.value.dbamp, 0.0 ),
            \gain, testlevel.value
          ], \addBefore);

      }      
    };

    testlevel =  MXCV(ControlSpec(-90.0, 0.0, -2, 0.5), -20.0);
    testlevel.action = { arg changer, what;
    //  set.setTestLevel( changer.value.dbamp );
      testsynth.set(\gain, testlevel.value);
    };

    selRow = MXCV(ControlSpec(0, matrix.fromBusArray.size - 1, \lin, 1), 0);  
    selRow.action = { arg changer, what; grid.selectNode( selCol.value, selRow.value ) };
    selRow.action = { arg changer, what; 
      outDeviceName.string = outLabels[changer.value][0];
      outNum.string = outLabels[changer.value][1];
    };
    selRow.action = { arg changer, what; 
      var val;
      val = grid.getState(selCol.value, selRow.value);
      level.value = (val == 0.0).if(-90.0, val.ampdb); 
      if(testtoggle.value > 0) { this.settestsynth };
    };

    selCol = MXCV(ControlSpec(0, matrix.toBusArray.size - 1, \lin, 1), 0);  
    selCol.action = { arg changer, what; grid.selectNode( selCol.value, selRow.value ) };
    selCol.action = { arg changer, what; 
      inDeviceName.string = inLabels[changer.value][0];
      inNum.string = inLabels[changer.value][1];
    };
    selCol.action = { arg changer, what; 
      var val;
      val = grid.getState(selCol.value, selRow.value);
      level.value = (val == 0.0).if(-90.0, val.ampdb); 
    };

    level.action_( { arg changer, what; 
    //  matrix.setNodeByIndex(selRow.value, selCol.value, level.value);
      matrix.setNodeByIndex(selCol.value, selRow.value, level.value);
      if(testtoggle.value > 0) { this.settestsynth };
        
      {
        var amp;
        amp = (level.value > -90).if( level.value.dbamp, 0.0 );
        grid.setState_( selCol.value, selRow.value, amp );
      }.defer
    }, \synch);

/*
    up = MXCV(MXStatusSpec.new, false);
    up.action_({arg changer, what;  { selRow.dec }.defer});

    down = MXCV(MXStatusSpec.new, false);
    down.action_({arg changer, what;  { selRow.inc }.defer});

    left = MXCV(MXStatusSpec.new, false);
    left.action_({arg changer, what;  { selCol.dec }.defer});

    right = MXCV(MXStatusSpec.new, false);
    right.action_({arg changer, what;  { selCol.inc }.defer});

    plus = MXCV(MXStatusSpec.new, false);
    plus.action_({arg changer, what;  { level.inc }.defer});

    minus = MXCV(MXStatusSpec.new, false);
    minus.action_({arg changer, what;  { level.dec }.defer});

    levelon = MXCV(MXStatusSpec.new, false);
    levelon.action_({arg changer, what;  { level.value = 0.0 }.defer});

    leveloff = MXCV(MXStatusSpec.new, false);
    leveloff.action_({arg changer, what;  { level.value = -90.0 }.defer});
*/
  } 
  
  close {
  //  win.close;
  }

  settestsynth {
    testsynth.set(\out, matrix.toBusArray[selRow.value].busNum);
    if (level.value > -90) { 
      testsynth.set(\on, level.value.dbamp);
    } {
      testsynth.set(\on, 0);
    };
  }

  makeNodeView {
    var width, radius=5, border=0, shifty= -2;
    var nodeFont;

    width = nodeview.bounds.width - 16;

    nodeFont = MXGUI.infoFont;
    buttonSize = Point(60, 26); 

    nodeview.decorator.shift(30, 0);
    
    muteButton = MXButton(nodeview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)      
    //  .states_([["mute", Color.white(1, 0.5), Color.red(1, 0.5) ], ["mute", Color.white, Color.red(1)]])
      .states_([["mute", Color.grey(0), Color.grey(0.7) ], ["mute", Color.grey(0), Color.red(1)]])
      .action_({ arg view;
        if ( view.value == 1 ) {Êmatrix.mute }Ê{ matrix.unmute };
      });

    clearButton = MXButton(nodeview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)      
    //  .states_([["clear", Color.white, Color.red(0.5)]])
      .states_([["clear", Color.grey(0), Color.grey(0.7)]])
      .action_({ arg view;
        matrix.removeNodes;
        grid.clearGrid;
        testtoggle.value = 0;
        level.value = -90.0;
        
      });
    
    nodeview.decorator.shift(352, 0);

    testButton = MXButton(nodeview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
    //  .states_([[ "Test", MXGUI.plugColor, Color.black], [ "Test",  Color.black, MXGUI.plugColor]])
      .states_([[ "Test", Color.grey(0), Color.grey(0.7)], [ "Test",  Color.grey(0), MXGUI.plugColor]])
      .connect(testtoggle)
      ;
  
    testLevelBox = MXNumber(nodeview, buttonSize, radius )
      .shifty_(shifty)
      .border_(border)
      .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .unit_(" dB")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ; 
            
    testLevelBox.connect(testlevel);

    nodeview.decorator.nextLine;
    nodeview.decorator.shift(30, 0);

    inDeviceName = MXStringView(nodeview, 150 @ buttonSize.y, radius  )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(MXGUI.infoBackColor)
    //  .border_(1)
    //  .borderColor_(infoBackColor)
      .inset_(0)
      .string_("");

    inNum = MXStringView(nodeview, buttonSize, radius  )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(MXGUI.infoBackColor)
    //  .border_(1)
    //  .borderColor_(infoBackColor)
      .inset_(0)
      .string_("");
  
  //  nodeview.decorator.shift(20, 0);
  
    MXStringView(nodeview,  30 @ buttonSize.y )
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(Color.clear)
  //    .border_(1)
  //    .borderColor_(Color.grey(0.5))
      .string_(">>");

    outDeviceName = MXStringView(nodeview, 150 @ buttonSize.y, radius  )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(MXGUI.infoBackColor)
    //  .border_(1)
    //  .borderColor_(Color.grey(0.5))
      .inset_(0)
      .string_("");

    outNum = MXStringView(nodeview, buttonSize, radius  )
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(MXGUI.infoBackColor)
    //  .border_(1)
    //  .borderColor_(Color.grey(0.5))
      .inset_(0)
      .string_("");

    nodeview.decorator.shift(10, 0);
      
    MXStringView(nodeview,  buttonSize )
      .shifty_(shifty)
      .font_(nodeFont)
      .stringColor_(MXGUI.infoTitleTextColor)
      .align_(\center)
      .orientation_(\right)
      .background_(Color.grey(0.1, 0))
  //    .border_(1)
  //    .borderColor_(Color.grey(0.5))
      .string_("gain:");

    valNum = MXNumber(nodeview, buttonSize, radius  )
      .shifty_(shifty)
  //    .border_(border)
  //    .borderColor_(Color.grey(0.5))
      .font_(nodeFont)
      .unit_(" dB")
      .inset_(0)
      .background_(MXGUI.levelBackColor)
      .stringColor_(MXGUI.levelColor)
      .align_(\center)
      ;
      
    valNum.connect(level);

  }

  makeGrid { 
    var cols, rows;
  //  var srspeed;

  //  srspeed = MXGlobals.srspeeds[ MXGlobals.sampleRate ];

/*    inLabels = matrix.inLabels.collect({ arg array, i;  // label und anzahl busse / channels !
      var name, size;
      name = array[0];
      size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);

    outLabels = matrix.outLabels.collect({ arg array, i;  // label und anzahl busse / channels !
      var name, size;
      name = array[0];
      size = array[1];
      size.collect({ arg k; [name.asString, (k+1).asString] })
    }).flatten(1);
*/

    inLabels = matrix.inLabels2;
    outLabels = matrix.outLabels2;
      
  //  grid = MXMatrixGrid(gridview, gridview.bounds, manager.numIns, manager.numOuts, inArray, outArray);
    grid = MXMatrixGrid(gridview, gridview.bounds, matrix.inLabels, matrix.outLabels);
    grid.setBackgrColor_(Color.grey(0.1));
    //grid.setBackgrColor_(Color.clear);
    grid.setGridColor_(Color.grey(0.3));
    grid.setFillMode_(true);
    grid.setFillColor_(Color.yellow);
    grid.setCursorColor_(Color.red);
    grid.setTrailDrag_(false);
    grid.setNodeBorder_(2);
    grid.nodeDownAction_({ arg node; 
      selRow.value = node.nodeloc[1];
      selCol.value = node.nodeloc[0];
      level.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
    });
    grid.nodeTrackAction_({ arg node, delta=0; 
    //  selRow.value = node.nodeloc[1];
    //  selCol.value = node.nodeloc[0];
    //  level.value = (node.state == 0.0).if(-90.0, node.state.ampdb);
    //  if (delta > 0) { level.inc };
    //  if (delta < 0) { level.dec};
    });
    grid.nodeUpAction_({ arg node; 
    });
    grid.keyDownAction_({ arg char, modifiers, unicode, keycode;
      //[char.ascii, modifiers, unicode].postln;
      if (char == Char.space, { level.input = (level.input > 0.0).if(0,1)});
      if (char == $+, { level.inc; });
      if (char == $-, { level.dec; });
      if (char == $m, { /*mute*/    });
      if (char == $s, { /*solo*/    });
      if (char == $t, { testtoggle.input_(1, \toggle) });
      if (unicode == 16rF700, { this.decRow;  });
      if (unicode == 16rF703, { this.incCol;  });
      if (unicode == 16rF701, { this.incRow;   });
      if (unicode == 16rF702, { this.decCol;  });
      //^nil  // bubble if it's an invalid key
    }); 
    grid.refresh;
    grid.focus(true);
  }

  incRow {
    selRow.inc;
  }
  decRow {
    selRow.dec;
  }
  incCol {
    selCol.inc;
  }
  decCol {
    selCol.dec;
  }

}



MXMatrixGrid {

// based on BoxGrid
// (c) 2006, Thor Magnusson - www.ixi-software.net
// GNU licence - google it.
// modified by Andre Bartetzki for use within a matrix mixer

  var <>gridNodes; 
  var tracknode, chosennode, matrixView;
  var parent, bounds, gridbounds, gridscroll, inlabelscroll, outlabelscroll;
//  var <transposed=false, <showinlabels=true, <showoutlabels=true;
  var downAction, upAction, trackAction, keyDownAction, rightDownAction, backgrDrawFunc;
  var mouseOverAction;
  var displayAction;
  var background;
  var gridcolor, cursorcolor, selectcolor;
  var columns, rows, inArray, outArray;
  var fillcolor, fillmode;
  var traildrag, bool;
  var font, fontColor;
  var <cellSize;
  var gitter;
  
  var clearButton, saveButton, newButton, setMenu, inNum, outNum, valNum, valSlider;
  var buttonSize;

  *new { arg parent, bounds, inArray, outArray; 
    ^super.new.initBoxGrid(parent, bounds, inArray, outArray);
  }
  
  // Achtung: cols und rows sind hier im Vergleich zu Boxgrid vertauscht!
  // xxxx  inputs sind rows, outputs sind cols xxxx
  // inputs sind cols, outputs sind rows !
  
  initBoxGrid { arg argparent, bounds, arginArray, argoutArray;
    var p, rect, pen, pad, textx, texty, fatx, faty;
    var clickx, clicky;
    var colshift, rowshift;
    
    parent = argparent; 
    inArray = arginArray;
    outArray = argoutArray;

  //  columns = outArray.collect({ arg device; device[1] }).sum ;
  //  rows = inArray.collect({ arg device; device[1] }).sum ;
    rows = outArray.collect({ arg device; device[1] }).sum ;
    columns = inArray.collect({ arg device; device[1] }).sum ;
  //  rows = outArray.size ;
  //  columns = inArray.size ;
      
  //  [columns, rows].postln;
    cellSize = MXGUI.matrixCellSize;
    colshift = 36;
    rowshift = 36;
    
    pad = if( GUI.id === \cocoa, 0.5, 0 );
    //parent.front;
    tracknode = 0;
    background = Color.clear;
    gridcolor = Color.grey(0.2);
    cursorcolor = Color.red;
    selectcolor = Color.green;
    fillcolor = Color.new255(103, 148, 103);
    fillmode = true;
    traildrag = false;
    bool = true;
    font = Font("Arial", 9);
    fontColor = Color.black;
    
  //  gridNodes = Array.newClear(columns) ! rows;
    gridNodes = Array.newClear(rows) ! columns;

    //sbounds   = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);
  //  gridbounds = Rect(bounds.left + 40 + 0.5, bounds.top + 40 + 0.5, columns * cellSize + 0.5, rows * cellSize + 0.5);
    gridbounds = Rect(0, 0, columns * cellSize + 0.5, rows * cellSize + 0.5);
  //  gridbounds.postln;

    outlabelscroll = SCScrollView(parent, Rect(colshift+2, 8, bounds.width - colshift - 10 - 2, rowshift)  )
      //  .background_(Color.grey)
        .hasBorder_(false)
        .autoScrolls_(false)
        .hasHorizontalScroller_(false)
        .hasVerticalScroller_(false)
        ;
    inlabelscroll = SCScrollView(parent, Rect(4, rowshift+6, colshift-2, bounds.height - rowshift - 10 - 8)  )
      //  .background_(Color.grey)
        .hasBorder_(false)
        .autoScrolls_(false)
        .hasHorizontalScroller_(false)
        .hasVerticalScroller_(false)
        ;
    gridscroll = SCScrollView(parent, Rect(colshift+2, rowshift+6, bounds.width - colshift - 2, bounds.height - rowshift - 6))
    //  .background_(Color.grey(0.3))
      .hasBorder_(false)
      .autoScrolls_(true)
      .autohidesScrollers_(true)
      .hasHorizontalScroller_(true)
      .hasVerticalScroller_(true)
      ;


    gridscroll.action = { 
      var origin;
      //"gridscroll action".postln;
      origin = gridscroll.visibleOrigin;
      inlabelscroll.visibleOrigin = 0@(origin.y);
      outlabelscroll.visibleOrigin = (origin.x)@0;
    };

    pen = SCPen;
    
    columns.do({arg c;
      rows.do({arg r;
        rect = Rect((c*(gridbounds.width/columns)).round(1)+0.5, 
              (r*(gridbounds.height/rows)).round(1)+0.5, 
              (gridbounds.width/columns).round(1), 
              (gridbounds.height/rows).round(1)
            );
        gridNodes[c][r] = MXMatrixGridNode.new(rect, c, r, fillcolor);
      });
    });

    textx = 0;
    texty = 0;
    fatx = List.new.add(0);
    faty = List.new.add(0);
    //  [label, busArray, outBusArray, outputLabels]
    
    inArray.do { arg array, i;  // label und anzahl busse / channels !
      var size, string;
      size = array[1];
      string = array[0];
      if (string.size > (size * 2)) { string = string[.. (size*2 - 1)] };
      MXStringView(outlabelscroll, Rect(textx + 0, 0, size * cellSize, 20))
    //  MXStringView(outlabelscroll, Rect(textx + 0, 20, size * cellSize, 20))
        .font_(font).stringColor_(Color.grey(1.0)).align_(\center).orientation_(\right)
        .background_(Color.grey(0.2)).border_(0).borderColor_(Color.grey(0.5))
        .inset_(1).shifty_(-1)
        .string_(string);
      size.do { arg k;
        SCStaticText(outlabelscroll, Rect(textx + 1 + (k * cellSize), 19, cellSize, 12))
      //  SCStaticText(outlabelscroll, Rect(textx + 1 + (k * cellSize), 0, cellSize, 18))
          .font_(font).stringColor_(Color.grey(1.0)).align_(\center)
          .string_((k+1).asString);
      };
      textx = textx + (size * cellSize);
      fatx.add( size + fatx.last );
    };
    texty = 0;
    outArray.do { arg array, i;
      var size, string;
      size = array[1];
      string = array[0];
      if (string.size > (size * 2)) { string = string[.. (size*2 - 1)] };
      MXStringView(inlabelscroll, Rect(1, texty, 20, size * cellSize))
        .font_(font).stringColor_(Color.grey(1.0)).align_(\center).orientation_(\up)
        .background_(Color.grey(0.2)).border_(0).borderColor_(Color.grey(0.5))
        .inset_(1).shifty_(-1) // .shiftx_(2)
        .string_(string);
      size.do { arg k;
        SCStaticText(inlabelscroll, Rect(1+20, texty + (k * cellSize) - 1, 12, cellSize))
          .font_(font).stringColor_(Color.grey(1.0)).align_(\center)
          .string_((k+1).asString);
      };
      texty = texty + (size * cellSize);
      faty.add( size + faty.last );
    };

    gitter = List.new;
    (columns+1).do({arg i;  
      var array, bundle;
      array = Array.newClear(3);
      bundle = fatx.includes(i);
      array[0] = bundle.if( Color.grey(0.6), gridcolor) ;
      array[1] = Point((i*(gridbounds.width/columns)), 0).round(1) + 0.5;
      array[2] = Point((i*(gridbounds.width/columns)), gridbounds.height).round(1) + 0.5;
      gitter.add(array);    
    });
    (rows+1).do({arg i;
      var array, bundle;
      array = Array.newClear(3);
      bundle = faty.includes(i);
      array[0] = bundle.if( Color.grey(0.6), gridcolor) ;
      array[1] = Point(0, (i*(gridbounds.height/rows))).round(1) + 0.5; 
      array[2] = Point(gridbounds.width, (i*(gridbounds.height/rows))).round(1) + 0.5;
      gitter.add(array);    
    });
            
    matrixView = SCUserView(gridscroll, Rect(0, 0, gridbounds.width, gridbounds.height))
      .canFocus_(true)
      /*
      .mouseBeginTrackFunc_({|me, x, y, mod|
          chosennode = this.findNode(x, y);
          if(mod == 262401, { // right mouse down
            rightDownAction.value(chosennode.nodeloc);
          }, {
            if(chosennode !=nil, {  
              chosennode.state = (chosennode.state > 0).if(0.0, 1.0);
              tracknode = chosennode;
              //downAction.value(chosennode.nodeloc);
              downAction.value(chosennode);
              //this.displayNode(chosennode);
              this.refresh; 
            });
          });
      })
      .mouseTrackFunc_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        if(chosennode != nil, {  
          if(tracknode.rect != chosennode.rect, {
            if(traildrag == true, { // on dragging mouse
              if(bool == true, { // boolean switching
                chosennode.state = (chosennode.state > 0).if(0.0, 1.0);
              }, {
                chosennode.state = 1.0;
              });
            },{
              chosennode.state = 1.0;
              tracknode.state = false;
            });
            tracknode = chosennode;
            //trackAction.value(chosennode.nodeloc);
            trackAction.value(chosennode);
            //this.displayNode(chosennode);
            this.refresh;
          });
        });
      })
      .mouseEndTrackFunc_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        if(chosennode !=nil, {  
          tracknode = chosennode;
          //upAction.value(chosennode.nodeloc);
          upAction.value(chosennode);
          //this.displayNode(chosennode);
          this.refresh;
        });
      })
      */
      .mouseOverAction_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        [x,y].postln;
        if(chosennode !=nil, {  
          this.displayNode(chosennode);
          //this.refresh;
        });
        this.refresh;
      })
      .mouseDownAction_({|me,  x, y, mod, buttonNumber, clickCount|
        //[mod, buttonNumber, clickCount].postln;
        chosennode = this.findNode(x, y);
        clickx = x;
        clicky = y;
        if(clickCount > 1) { chosennode.state = (chosennode.state > 0).if(0.0, 1.0) };
        downAction.value(chosennode);
        this.refresh;
      })
      .mouseMoveAction_({|me, x, y, mod|
      //  chosennode = this.findNode(x, y);
        if(chosennode.notNil) {  
          if ( clicky < y ) {
            trackAction.value(chosennode, 1);
          };
          if ( clicky > y ) {
            trackAction.value(chosennode, -1);
          };
          this.refresh;
        }
      })
      .mouseUpAction_({|me,  x, y, mod, buttonNumber, clickCount|
        upAction.value(chosennode);
        //this.refresh;
      })
      .keyDownAction_({ |me, key, modifiers, unicode, keycode |       keyDownAction.value(key, modifiers, unicode, keycode);
        this.refresh;
      })
      
      .drawFunc_({ arg v;
        pen.width = 1;
        //background.set; // background color
        pen.color = background;
        //pen.color = Color.clear;
        pen.fillRect(v.bounds.moveTo(0,0)); // background fill
        // backgrDrawFunc.value; // background draw function  ??
        //Color.black.set;
        //gridcolor.set;
        pen.color = gridcolor;
        // Draw the boxes
        gridNodes.do({arg row;
          row.do({arg node; 
            if (node.state > 0) {
              if(fillmode, {
                //node.color.set;
                pen.color = node.color.alpha_((node.state < 1).if( (node.state*0.3) ** 0.2, 1.0));
                pen.fillRect(node.fillrect);
                //gridcolor.set;
                //Color.black.set;    
                //pen.color = gridcolor;
                pen.strokeRect(node.fillrect);
              },{
                //gridcolor.set;
                //Color.black.set;    
                pen.color = gridcolor;
                pen.strokeRect(node.fillrect);
              });
              /*
              node.string.drawInRect(Rect(node.fillrect.left+5,
                        node.fillrect.top+(node.fillrect.height/2)-(font.size/1.5), 
                        80, 16),   
                        font, fontColor);
              */
            };
            if (node.mark)  {
              pen.color = Color.grey(0.3);
              pen.fillOval(node.fillrect.insetBy(2, 2));
            };

          });
        });
        //gridcolor.set;
        //Color.black.set;    
        if(chosennode !=nil, {  
            pen.color = cursorcolor;
            pen.strokeRect(chosennode.fillrect);
          });
          
        gitter.do { arg array;
          pen.color = array[0];
          pen.line(array[1], array[2]);
          pen.stroke;
        };    
        
        pen.stroke;     
      });
  }

  // GRID
  setBackgrColor_ {arg color;
    background = color;
    matrixView.refresh;
  }

  setGridColor_ {arg color;
    gridcolor = color;
    matrixView.refresh;
  }

  setCursorColor_ {arg color;
    cursorcolor = color;
    matrixView.refresh;
  }
    
  setFillMode_ {arg mode;
    fillmode = mode;
    matrixView.refresh;
  }
  
  setFillColor_ {arg color;
    gridNodes.do({arg row;
      row.do({arg node; 
        node.setColor_(color);
      });
    });
    matrixView.refresh;
  }
  
  setTrailDrag_{arg mode, argbool=false;
    traildrag = mode;
    bool = argbool;
  }

  refresh {
    matrixView.refresh;
  }
    
  focus { arg bool=true;
    matrixView.focus(bool);
  } 
    
  // NODES  
  setNodeBorder_ {arg border;
    gridNodes.do({arg row;
      row.do({arg node; 
        node.setBorder_(border);
      });
    });
    matrixView.refresh;
  }
  
  // depricated
  setVisible_ {arg col, row,  state;
    gridNodes[col][row].setVisible_(state);
    matrixView.refresh;
  }

  setState_ {arg col, row, state;
    //if(state.isInteger, {state = state!=0});
    gridNodes[col][row].setState_(state);
    matrixView.refresh;
  }
  
  getState {arg col, row;
    var state;
    ^state = gridNodes[col][row].getState;
    //^state.binaryValue;
  } 
  
  setBoxColor_ {arg col, row, color;
    gridNodes[col][row].setColor_(color);
    matrixView.refresh;
  }
  
  getBoxColor {arg col, row;
    ^gridNodes[col][row].getColor;  
  }
  
  getNodeStates {
    var array;
    array = Array.newClear(columns) ! rows;
    gridNodes.do({arg rows, c;
      rows.do({arg node, r; 
        array[c][r] = node.state; //.binaryValue;
      });
    });
    ^array;
  }
  
  setNodeStates_ {arg array;
    gridNodes.do({arg rows, c;
      rows.do({arg node, r; 
        node.state = array[c][r];
      });
    });
    matrixView.refresh;
  }
  
  clearGrid {
    gridNodes.do({arg rows, c;
      rows.do({arg node, r; 
        node.state = 0.0;
      });
    });
    matrixView.refresh;
  } 

  setDisplayAction_ { arg func;
    displayAction = func;
  }
  
  selectNode { arg col, row;
    chosennode = gridNodes[col][row];
    //downAction.value(chosennode);
    this.refresh;
  }
  
  markNode {arg col, row, bool;
    gridNodes[col][row].mark = bool;
    this.refresh;
  }
  
  
  // PASSED FUNCTIONS OF MOUSE OR BACKGROUND
  mouseOverAction { arg view, x, y;
    [x,y].postln;
  }

  nodeDownAction_ { arg func;
    downAction = func;
  }
  
  nodeUpAction_ { arg func;
    upAction = func;
  }
  
  nodeTrackAction_ { arg func;
    trackAction = func;
  }
  
  keyDownAction_ {arg func;
    matrixView.canFocus_(true); // in order to detect keys the view has to be focusable
    keyDownAction = func;
  }
  
  rightDownAction_ {arg func;
    rightDownAction = func;
  }
  
  setBackgrDrawFunc_ { arg func;
    backgrDrawFunc = func;
  }
    
  setFont_ {arg f;
    font = f;
  }
  
  setFontColor_ {arg fc;
    fontColor = fc;
  }
  
  setNodeString_ {arg col, row, string;
    gridNodes[col][row].string = string;
    matrixView.refresh;   
  }
  
  getNodeString {arg col, row;
    ^gridNodes[col][row].string;
  }

  // local function
  findNode {arg x, y;
    gridNodes.do({arg row;
      row.do({arg node; 
        if(node.rect.containsPoint(Point.new(x,y)), {
          ^node;
        });
      });
    });
    ^nil;
  }
  
  displayNode { arg node;
  //  inNum.value = chosennode.nodeloc[1];
  //  outNum.value = chosennode.nodeloc[0];
  //  if(chosennode.getState) { valNum.value = 1.0 } { valNum.value = 0.0 };
    displayAction.value(node);
  
  }
  
}


MXMatrixRow {

// based on BoxGrid
// (c) 2006, Thor Magnusson - www.ixi-software.net
// GNU licence - google it.
// modified by Andre Bartetzki for use within a matrix mixer

  var <>gridNodes; 
  var tracknode, chosennode, view, matrixView;
  var parent, bounds, gridbounds, gridscroll, labelscroll;
  var downAction, upAction, trackAction, keyDownAction, rightDownAction, backgrDrawFunc;
  var mouseOverAction;
  var displayAction;
  var background;
  var gridcolor, cursorcolor, selectcolor;
  var columns, rows, array;
  var fillcolor, fillmode;
  var traildrag, bool;
  var font, fontColor;
  var <cellSize;
  var gitter;
  
  var clearButton, saveButton, newButton, setMenu, inNum, outNum, valNum, valSlider;
  var buttonSize;

  *new { arg parent, bounds, array; 
    ^super.new.initBoxGrid(parent, bounds, array);
  }
  
  // Achtung: cols und rows sind hier im Vergleich zu Boxgrid vertauscht!
  // xxxx  inputs sind rows, outputs sind cols xxxx
  // inputs sind cols, outputs sind rows !
  
  initBoxGrid { arg argparent, bounds, argArray;
    var p, rect, pen, pad, textx, texty, fatx, faty;
    var clickx, clicky;
    var colshift, rowshift;
    
    parent = argparent; 
    array = argArray;

    rows = 1 ;
    columns = array.collect({ arg device; device[1] }).sum ;
      
  //  [columns, rows].postln;
    cellSize = MXGUI.matrixCellSize;
    colshift = 0;
    rowshift = 36;
    
    pad = if( GUI.id === \cocoa, 0.5, 0 );
    //parent.front;
    tracknode = 0;
    background = Color.clear;
    gridcolor = Color.grey(0.2);
    cursorcolor = Color.red;
    selectcolor = Color.green;
    fillcolor = Color.new255(103, 148, 103);
    fillmode = true;
    traildrag = false;
    bool = true;
    font = Font("Arial", 9);
    fontColor = Color.black;

    view = SCCompositeView(parent, bounds);
    
  //  gridNodes = Array.newClear(columns) ! rows;
    gridNodes = Array.newClear(columns);

    //sbounds   = Rect(bounds.left + 0.5, bounds.top + 0.5, bounds.width, bounds.height);
  //  gridbounds = Rect(bounds.left + 40 + 0.5, bounds.top + 40 + 0.5, columns * cellSize + 0.5, rows * cellSize + 0.5);
    gridbounds = Rect(0, 0, columns * cellSize + 0.5, rows * cellSize + 0.5);
  //  gridbounds.postln;

    labelscroll = SCScrollView(view, Rect(colshift+2, 8, view.bounds.width - colshift - 4, rowshift)  )
      //  .background_(Color.grey)
        .hasBorder_(false)
        .autoScrolls_(false)
        .hasHorizontalScroller_(false)
        .hasVerticalScroller_(false)
        ;
    gridscroll = SCScrollView(view, Rect(colshift+2, rowshift+6, view.bounds.width - colshift - 4, cellSize * 2 + 5))
    //  .background_(Color.grey(0.3))
      .hasBorder_(false)
      .autoScrolls_(true)
      .autohidesScrollers_(true)
      .hasHorizontalScroller_(true)
      .hasVerticalScroller_(false)
      ;

    gridscroll.action = { 
      var origin;
      //"gridscroll action".postln;
      origin = gridscroll.visibleOrigin;
      labelscroll.visibleOrigin = (origin.x)@0;
    };

    pen = SCPen;
    
    columns.do({arg c;
      rect = Rect((c*(gridbounds.width/columns)).round(1)+0.5, 
            (0 * gridbounds.height/rows).round(1)+0.5, 
            (gridbounds.width/columns).round(1), 
            (gridbounds.height/rows).round(1)
      );
      gridNodes[c] = MXMatrixGridNode.new(rect, c, 0, fillcolor);
    });

    textx = 0;
    fatx = List.new.add(0);
    //  [label, busArray, outBusArray, outputLabels]
    
    array.do { arg arr, i;  // label und anzahl busse / channels !
      var size, string;
      size = arr[1];
      string = arr[0];
      if (string.size > (size * 2)) { string = string[.. (size*2 - 1)] };
      MXStringView(labelscroll, Rect(textx + 0, 0, size * cellSize, 20))
        .font_(font).stringColor_(Color.grey(1.0)).align_(\center).orientation_(\right)
        .background_(Color.grey(0.2)).border_(0).borderColor_(Color.grey(0.5))
        .inset_(1).shifty_(-1)
        .string_(string);
      size.do { arg k;
        SCStaticText(labelscroll, Rect(textx + 1 + (k * cellSize), 19, cellSize, 12))
          .font_(font).stringColor_(Color.grey(1.0)).align_(\center)
          .string_((k+1).asString);
      };
      textx = textx + (size * cellSize);
      fatx.add( size + fatx.last );
    };

    gitter = List.new;
    (columns+1).do({arg i;  
      var arr, bundle;
      arr = Array.newClear(3);
      bundle = fatx.includes(i);
      arr[0] = bundle.if( Color.grey(0.6), gridcolor) ;
      arr[1] = Point((i*(gridbounds.width/columns)), 0).round(1) + 0.5;
      arr[2] = Point((i*(gridbounds.width/columns)), gridbounds.height).round(1) + 0.5;
      gitter.add(arr);    
    });
    (rows+1).do({arg i;
      var arr, bundle;
      arr = Array.newClear(3);
    //  bundle = faty.includes(i);
      arr[0] = gridcolor;
      arr[1] = Point(0, (i*(gridbounds.height/rows))).round(1) + 0.5; 
      arr[2] = Point(gridbounds.width, (i*(gridbounds.height/rows))).round(1) + 0.5;
      gitter.add(arr);    
    });
            
    matrixView = SCUserView(gridscroll, Rect(0, 0, gridbounds.width, gridbounds.height))
      .canFocus_(true)
      /*
      .mouseBeginTrackFunc_({|me, x, y, mod|
          chosennode = this.findNode(x, y);
          if(mod == 262401, { // right mouse down
            rightDownAction.value(chosennode.nodeloc);
          }, {
            if(chosennode !=nil, {  
              chosennode.state = (chosennode.state > 0).if(0.0, 1.0);
              tracknode = chosennode;
              //downAction.value(chosennode.nodeloc);
              downAction.value(chosennode);
              //this.displayNode(chosennode);
              this.refresh; 
            });
          });
      })
      .mouseTrackFunc_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        if(chosennode != nil, {  
          if(tracknode.rect != chosennode.rect, {
            if(traildrag == true, { // on dragging mouse
              if(bool == true, { // boolean switching
                chosennode.state = (chosennode.state > 0).if(0.0, 1.0);
              }, {
                chosennode.state = 1.0;
              });
            },{
              chosennode.state = 1.0;
              tracknode.state = false;
            });
            tracknode = chosennode;
            //trackAction.value(chosennode.nodeloc);
            trackAction.value(chosennode);
            //this.displayNode(chosennode);
            this.refresh;
          });
        });
      })
      .mouseEndTrackFunc_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        if(chosennode !=nil, {  
          tracknode = chosennode;
          //upAction.value(chosennode.nodeloc);
          upAction.value(chosennode);
          //this.displayNode(chosennode);
          this.refresh;
        });
      })
      */
      .mouseOverAction_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        [x,y].postln;
        if(chosennode !=nil, {  
          this.displayNode(chosennode);
        });
        this.refresh;
      })
      .mouseDownAction_({|me,  x, y, mod, buttonNumber, clickCount|
        //[mod, buttonNumber, clickCount].postln;
        chosennode = this.findNode(x, y);
        clickx = x;
        clicky = y;
        if(clickCount > 0) { chosennode.state = (chosennode.state > 0).if(0.0, 1.0) };
        downAction.value(chosennode);
        this.refresh;
      })
      .mouseMoveAction_({|me, x, y, mod|
        chosennode = this.findNode(x, y);
        if(chosennode.notNil) {  
          if ( clicky < y ) {
            trackAction.value(chosennode, 1);
          };
          if ( clicky > y ) {
            trackAction.value(chosennode, -1);
          };
          this.refresh;
        }
      })
      .mouseUpAction_({|me,  x, y, mod, buttonNumber, clickCount|
        upAction.value(chosennode);
        //this.refresh;
      })
      .keyDownAction_({ |me, key, modifiers, unicode, keycode |       keyDownAction.value(key, modifiers, unicode, keycode);
        this.refresh;
      })
      .drawFunc_({ arg v;
        pen.width = 1;
        pen.color = background;
        pen.fillRect(v.bounds.moveTo(0,0)); // background fill
        pen.color = gridcolor;
        gridNodes.do( {arg node;
          if(node.state > 0, {
            if(fillmode, {
              pen.color = node.color.alpha_((node.state < 1).if( (node.state*0.3) ** 0.2, 1.0));
              pen.fillRect(node.fillrect);
              pen.strokeRect(node.fillrect);
            },{
              pen.color = gridcolor;
              pen.strokeRect(node.fillrect);
            });
            /*
            node.string.drawInRect(Rect(node.fillrect.left+5,
                      node.fillrect.top+(node.fillrect.height/2)-(font.size/1.5), 
                      80, 16),   
                      font, fontColor);
            */
          });
        });
        if(chosennode !=nil, {  
          pen.color = cursorcolor;
          pen.strokeRect(chosennode.fillrect);
        });
        gitter.do { arg array;
          pen.color = array[0];
          pen.line(array[1], array[2]);
          pen.stroke;
        };    
        pen.stroke;     
      });
  }

  // GRID
  setBackgrColor_ {arg color;
    background = color;
    matrixView.refresh;
  }

  setGridColor_ {arg color;
    gridcolor = color;
    matrixView.refresh;
  }

  setCursorColor_ {arg color;
    cursorcolor = color;
    matrixView.refresh;
  }
    
  setFillMode_ {arg mode;
    fillmode = mode;
    matrixView.refresh;
  }
  
  setFillColor_ {arg color;
    gridNodes.do({arg node;
      node.setColor_(color);
    });
    matrixView.refresh;
  }
  
  setTrailDrag_{arg mode, argbool=false;
    traildrag = mode;
    bool = argbool;
  }

  refresh {
    matrixView.refresh;
  }
    
  focus { arg bool=true;
    matrixView.focus(bool);
  } 
    
  // NODES  
  setNodeBorder_ {arg border;
    gridNodes.do({arg node;
      node.setBorder_(border);
    });
    matrixView.refresh;
  }
  
  // depricated
  setVisible_ {arg col, state;
    gridNodes[col].setVisible_(state);
    matrixView.refresh;
  }

  setState_ {arg col, state;
    //if(state.isInteger, {state = state!=0});
    gridNodes[col].setState_(state);
    matrixView.refresh;
  }
  
  getState {arg col;
    var state;
    ^state = gridNodes[col].getState;
    //^state.binaryValue;
  } 
  
  setBoxColor_ {arg col, color;
    gridNodes[col].setColor_(color);
    matrixView.refresh;
  }
  
  getBoxColor {arg col;
    ^gridNodes[col].getColor; 
  }
  
  getNodeStates {
    var array;
    array = Array.newClear(columns);
    gridNodes.do({arg node, c;
      array[c]= node.state; //.binaryValue;
    });
    ^array;
  }
  
  setNodeStates_ {arg array;
    gridNodes.do({arg node, c;
      node.state = array[c];
    });
    matrixView.refresh;
  }
  
  clearGrid {
    gridNodes.do({arg node;
      node.state = 0.0;
    });
    matrixView.refresh;
  } 

  setDisplayAction_ { arg func;
    displayAction = func;
  }
  
  selectNode { arg col;
    chosennode = gridNodes[col];
    //downAction.value(chosennode);
    matrixView.refresh;
  }
  
  
  
  // PASSED FUNCTIONS OF MOUSE OR BACKGROUND
  mouseOverAction { arg view, x, y;
    [x,y].postln;
  }

  nodeDownAction_ { arg func;
    downAction = func;
  }
  
  nodeUpAction_ { arg func;
    upAction = func;
  }
  
  nodeTrackAction_ { arg func;
    trackAction = func;
  }
  
  keyDownAction_ {arg func;
    matrixView.canFocus_(true); // in order to detect keys the view has to be focusable
    keyDownAction = func;
  }
  
  rightDownAction_ {arg func;
    rightDownAction = func;
  }
  
  setBackgrDrawFunc_ { arg func;
    backgrDrawFunc = func;
  }
    
  setFont_ {arg f;
    font = f;
  }
  
  setFontColor_ {arg fc;
    fontColor = fc;
  }
  
  setNodeString_ {arg col, row, string;
    gridNodes[col].string = string;
    matrixView.refresh;   
  }
  
  getNodeString {arg col, row;
    ^gridNodes[col].string;
  }

  // local function
  findNode {arg x, y;
    gridNodes.do({arg node;
      if(node.rect.containsPoint(Point.new(x,y)), {
          ^node;
        });
    });
    ^nil;
  }
  
  displayNode { arg node;
  //  inNum.value = chosennode.nodeloc[1];
  //  outNum.value = chosennode.nodeloc[0];
  //  if(chosennode.getState) { valNum.value = 1.0 } { valNum.value = 0.0 };
    displayAction.value(node);
  
  }
  
}


MXMatrixGridNode {
  var <>fillrect, <>state, <>border, <>rect, <>nodeloc, <>color;
  var <>string;
  var <>mark = false;
  
  *new { arg rect, column, row, color ; 
    ^super.new.initGridNode( rect, column, row, color);
  }
  
  initGridNode {arg argrect, argcolumn, argrow, argcolor;
    rect = argrect;
    nodeloc = [ argcolumn, argrow ];  
    color = argcolor; 
    border = 3;
    fillrect = Rect(rect.left+border, rect.top+border, 
          rect.width-(border*2), rect.height-(border*2));
    state = 0.0;
    string = "";
  }
  
  setBorder_ {arg argborder;
    border = argborder;
    fillrect = Rect(rect.left+border, rect.top+border, 
          rect.width-(border*2), rect.height-(border*2));
  }
  
  setVisible_ {arg argstate;
    state = argstate;
  }
  
  setState_ {arg argstate;
    state = argstate;
  }
  
  getState {
    ^state;
  }
  
  setColor_ {arg argcolor;
    color = argcolor;
  }
  
  getColor {
    ^color;
  }
}

