+ SCPen {  // partly taken from wslib 2006
  
  *roundedRect { arg rect, radius;  
    var points, lastPoint;
    
    radius = radius ?? {  rect.width.min( rect.height ) / 2; };
      
    if( radius != 0 ) { 
      // auto scale radius if too large
      radius = min( radius, min( rect.width, rect.height ) / 2 );
      
      points = [rect.rightTop, rect.rightBottom,rect.leftBottom, rect.leftTop];
      lastPoint = points.last;
          
      SCPen.moveTo( points[2] - (0@radius) );
      points.do({ arg point, i;
        SCPen.arcTo( lastPoint, point, radius );
        lastPoint = point;
      });
            
      ^SCPen; // allow follow-up methods
    } { 
      ^SCPen.addRect( rect ); 
    }
          
  }
  
  
  
}

