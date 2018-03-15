import java.awt.Image;
import java.awt.image.*;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

class ImageFrame extends JFrame {
  public static final int DEFAULT_WIDTH = 652, DEFAULT_HEIGHT = 442;
  public ImageFrame(Image image){
    setTitle("ImageTest");
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    ImageComponent component = new ImageComponent(image);
    add(component);
  }
}

class ImageComponent extends JComponent{
  private static final long serialVersionUID = 1L;
  private Image image;
  public ImageComponent(Image image){
    this.image = image;
  }
  public void paintComponent(Graphics g){
    if(image == null) return;
    int imageWidth = image.getWidth(this);
    int imageHeight = image.getHeight(this);
    g.drawImage(image, 0, 0, this);

    for (int i = 0; i*imageWidth <= getWidth(); i++)
      for(int j = 0; j*imageHeight <= getHeight();j++)
        if(i+j>0) g.copyArea(0, 0, imageWidth, imageHeight, i*imageWidth, j*imageHeight);
  }
}

public class opencvTest {

  public static List<Mat> cluster(Mat cutout, int k) {
    Mat samples = cutout.reshape(1, cutout.cols() * cutout.rows());
    Mat samples32f = new Mat();
    samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);
    Mat labels = new Mat();
    TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
    Mat centers = new Mat();
    Core.kmeans(samples32f, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);		
    return showClusters(cutout, labels, centers);
  }

  private static List<Mat> showClusters (Mat cutout, Mat labels, Mat centers) {
     centers.convertTo(centers, CvType.CV_8UC1, 255.0);
		centers.reshape(3);
		
		List<Mat> clusters = new ArrayList<Mat>();
		for(int i = 0; i < centers.rows(); i++) {
			clusters.add(Mat.zeros(cutout.size(), cutout.type()));
		}
		
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		for(int i = 0; i < centers.rows(); i++) counts.put(i, 0);
		
		int rows = 0;
		for(int y = 0; y < cutout.rows(); y++) {
		  for(int x = 0; x < cutout.cols(); x++) {
		int label = (int)labels.get(rows, 0)[0];
				int r = (int)centers.get(label, 2)[0];
				int g = (int)centers.get(label, 1)[0];
				int b = (int)centers.get(label, 0)[0];
				counts.put(label, counts.get(label) + 1);
			if (label==4)
			  clusters.get(label).put(y, x, 155, 0, 0);
			else if (label==3)
			  clusters.get(label).put(y, x, 0, 155, 0);
			else if (label==2)
			  clusters.get(label).put(y, x, 0, 0, 155);
			else if (label==1)
			  clusters.get(label).put(y, x, 0, 155, 155);
			else if (label==0)
			  clusters.get(label).put(y, x, 155, 155, 0);
				rows++;
			}
		}
		System.out.println(counts);
		return clusters;
	}


  public Image toBufferedImage(Mat m){
    int type = BufferedImage.TYPE_BYTE_GRAY;
    if (m.channels() > 1) {
      type = BufferedImage.TYPE_3BYTE_BGR;
      System.out.println("ah!");
    }
    int bufferSize = m.channels()*m.cols()*m.rows();
    byte [] b = new byte[bufferSize];
    m.get(0,0,b); // get all the pixels
    BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    System.arraycopy(b, 0, targetPixels, 0, b.length);  
    return image;
  }

  public void showImage() {
    String fileName = "/Users/lh/data/images/sosSpectrum.png";
    Mat mat = Imgcodecs.imread(fileName);
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
    //Mat smallMat = new Mat();
    //Size sz = new Size(652, 442);
    //Imgproc.resize(mat, smallMat, sz);
    Mat gray = new Mat();
    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
    Imgproc.threshold(gray, gray, 100, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
    //Noise removal
    Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(1,1));  //19,19
    Mat ret = new Mat(gray.size(),CvType.CV_8U);
    Imgproc.morphologyEx(gray, ret, Imgproc.MORPH_OPEN, kernel);

    //BackgroundSubtractorMOG2 bg2 = Video.createBackgroundSubtractorMOG2();
    //Sure background area
    Mat sure_bg = new Mat(gray.size(),CvType.CV_8U);
    Imgproc.dilate(ret,sure_bg,new Mat(),new Point(-1,-1),3);
    Imgproc.threshold(sure_bg,sure_bg,0,200,Imgproc.THRESH_BINARY_INV);
      
    Mat sure_fg = new Mat(gray.size(),CvType.CV_8U);
    Imgproc.erode(gray,sure_fg,new Mat(),new Point(-1,-1),2);
    
    Mat markers = new Mat(gray.size(),CvType.CV_8U, new Scalar(0));
    Core.add(sure_fg, sure_bg, markers);     
    markers.convertTo(markers, CvType.CV_32SC1);
    
    Imgproc.watershed(mat, markers);
    Core.convertScaleAbs(markers, markers);
    
    //Mat output = new Mat();
    //bg2.apply(smallMat, output);
    //System.out.println(output.dump());
    Image image = toBufferedImage(mat);
    EventQueue.invokeLater(new Runnable() {
      public void run(){
           ImageFrame frame = new ImageFrame(image);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
    });
    //Image image1 = toBufferedImage(markers);
    Mat hsv = new Mat();
    Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV); 
    Mat clusters1 = cluster(hsv, 5).get(0);
    Mat clusters2 = cluster(hsv, 5).get(1);
    Mat clusters3 = cluster(hsv, 5).get(2);
    Mat clusters4 = cluster(hsv, 5).get(3);
    Mat clusters5 = cluster(hsv, 5).get(4);
    Mat dst = new Mat();
    Core.add(clusters1, clusters2, dst);
    Core.add(dst, clusters3, dst);
    Core.add(dst, clusters4, dst);
    Core.add(dst, clusters5, dst);
    Image image1 = toBufferedImage(dst);
    /*int k = 5;
        Mat hsv = new Mat();
        Mat cropped_hsv = new Mat();
    Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_RGB2HSV); 
        List<Mat> channels = new ArrayList<Mat>();
        Core.split(hsv,channels);
        cropped_hsv = channels.get(0);
        List<Mat> temp = new ArrayList<Mat>();
        temp = cluster(hsv, k);
        Mat clusters1 = temp.get(0);
        Mat clusters2 = temp.get(1);
        Mat clusters3 = temp.get(2);
        Mat clusters4 = temp.get(3);
        Mat clusters5 = temp.get(4);
	Image image1 = toBufferedImage(clusters1);*/

    EventQueue.invokeLater(new Runnable() {
      public void run(){
           ImageFrame frame1 = new ImageFrame(image1);
                frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame1.setVisible(true);
            }
    });
  }
  
  public static void main(String[] args) {
    System.load("/Users/lh/opencv/build/lib/libopencv_java320.dylib");
    opencvTest otest = new opencvTest();
    otest.showImage();
  }
}
