import static marvin.MarvinPluginCollection.*;
import marvin.color.MarvinColorModelConverter;
import marvin.image.MarvinBlob;
import marvin.image.MarvinBlobSegment;
import marvin.image.MarvinContour;
import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;
import marvin.math.MarvinMath;
import marvin.math.Point;

import java.util.*;


class Segment {
  public ArrayList<Point> pixels;

  public Segment() {
    pixels = new ArrayList();
  }
  public void addPixel(Point p) {
    pixels.add(p);
  }
  public double dis(Segment s) {
    double dis2 = 999999;
    for (Point p: pixels) {
      int x = p.x, y = p.y;
      for (Point sp: s.pixels) {
        double d2 = (x-sp.x)*(x-sp.x)+(y-sp.y)*(y-sp.y);
	if (dis2>d2) dis2 = d2;
	if (dis2<=2) break;
      }
    }
    return Math.sqrt(dis2);
  }
  public boolean isSame(Segment s) {
    for (Point p: pixels) {
      int x = p.x, y = p.y;
      for (Point sp: s.pixels) 
	if (x==sp.x && y==sp.y) return true;
    }
    return false;
  }
  public boolean isConnect(Segment s) {
    if (this.dis(s) < 16) return true;
    return false;
  }
  public void merge(Segment s) {
    for (Point p: s.pixels) this.addPixel(p);
  }
}

class SortbySize implements Comparator<Segment> {
  public int compare(Segment a, Segment b) {
    return b.pixels.size() - a.pixels.size();
  }
}

public class TestMarvin {
  public static void main(String[] args) {
    String imagePath = "/Users/lh/data/images/sos096_1_743.png";
    MarvinImage originalImage = MarvinImageIO.loadImage(imagePath);
    MarvinImage image = originalImage.clone();
    // 1. Thesholding
    brightnessAndContrast(image, 0, 30);
    MarvinImage imageOut = image.clone();
    //thresholdingNeighborhood(image, imageOut, 1, 10, 1);
    // 2. Separate cells that are grouped
    invertColors(imageOut);
    MarvinImageIO.saveImage(imageOut, "./imageOut.png");
    MarvinImage bin = MarvinColorModelConverter.rgbToBinary(imageOut, 5);
    MarvinImageIO.saveImage(bin, "./bin5.png");
    bin = MarvinColorModelConverter.rgbToBinary(imageOut, 230);
    MarvinImageIO.saveImage(bin, "./bin200.png");
    // 3. Segment each cell
    image = MarvinColorModelConverter.binaryToRgb(bin);
    image.setAlphaByColor(0, 0xFFFFFFFF);		
    MarvinBlobSegment[] segments = floodfillSegmentationBlob(image);
    ArrayList<Segment> segGroups = new ArrayList();
    int totalCells=0;
    for(MarvinBlobSegment s:segments) {
      MarvinBlob blob = s.getBlob();
      MarvinContour contour = blob.toContour();
      if(blob.getArea() > 50) {
        Segment seg = new Segment();
	totalCells++;
	for (int i2=0; i2<blob.getWidth(); i2++)
	  for (int i1=0; i1<blob.getHeight(); i1++) {
            int x = s.getX() + i2;
	    int y = s.getY() + i1;
	    boolean isCell = blob.getValue(i2, i1);
	    if (isCell) seg.addPixel(new Point(x, y));
	  }
	segGroups.add(seg);
	//for(Point p:contour.getPoints())
	//  originalImage.setIntColor(s.getX()+p.x, s.getY()+p.y, 0xFF00FF00);
      }
    }

    // merge groups
    int sizeBeforeMerge, sizeAfterMerge;
    do {
      ArrayList<Segment> removeList = new ArrayList();
      sizeBeforeMerge = segGroups.size();
      for (int i=0; i<sizeBeforeMerge-1; ++i) {
	Segment seg1 = segGroups.get(i); 
	if (removeList.contains(seg1)) continue;
        for (int j=i+1; j<sizeBeforeMerge; ++j) {
	  Segment seg2 = segGroups.get(j);
          if (seg1.isConnect(seg2)) {
            System.out.println("merge!");
	    seg1.merge(seg2);
	    removeList.add(seg2);
	  }
        }
      }
      for (Segment k:removeList) segGroups.remove(k);
      sizeAfterMerge = segGroups.size();
      System.out.println(sizeBeforeMerge + "  " + sizeAfterMerge);
    } while (sizeBeforeMerge > sizeAfterMerge);

    // sort groups according to sizes
    Collections.sort(segGroups, new SortbySize());

    // visualize groups
    int w = originalImage.getWidth(), h = originalImage.getHeight();
    for (int i2=0; i2<w; ++i2)
      for (int i1=0; i1<h; ++i1) {
	originalImage.setIntColor(i2, i1, 255, 0, 0, 255);
      }
    int i = 0;
    int r = 50, b = 50, g = 50;
    for (Segment seg:segGroups) { 
      r = (i % 3) * 70 + 100;
      g = (9 - i % 9) * 30;
      b = (5 - i % 5) * 60;
     // if (seg.pixels.size() < 200) continue; 
      for (Point p:seg.pixels)
        originalImage.setIntColor(p.x, p.y, 255, r, g, b);
      i++;
    }    

    MarvinImageIO.saveImage(originalImage, "./cells_output.png");
    System.out.println("total cells: "+i);
   }
}
