public class BXLPad extends Pad {

  public BXLPad(long w,
                long h,
                long x,
                long y,
                long rot,
                Char shape,
                String attr,
                String plated,
                String layer,
                String holeDiam,
                String pinNum,
                String pinName) {

    kicadShapeOrientation = rot * 10;

    kicadShapePadName = "1";
    kicadShapeNetName = "GND";
    
    kicadShapeXsizeNm = 800*2540;
    kicadShapeYsizeNm = 800*2540;
    kicadShapeXdeltaNm = 0;
    kicadShapeYdeltaNm = 0;
    kicadShapeOrientation = 0;
    
    kicadDrillShape = 'C';
    kicadDrillOneSizeNm = 600;
    kicadDrillOneXoffsetNm = 0;
    kicadDrillOneYoffsetNm = 0;
    kicadDrillShapeTwo = 'C';
    kicadDrillSlotWidthNm = 0;
    kicadDrillSlotHeightNm = 0;
    
    kicadPadPositionXNm = 1000*2540;
    kicadPadPositionYNm = 1000*2540;
    
    kicadPadAttributeType = "STD";


  }

}
