import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MaskDetection {
    double finalArray[][][] = new double[256][256][256] ;
    int supportArray[][][] = new int[256][256][256] ;

    Map<File,File>map, testMap ;

    File folder1 = new File("src\\training\\mainImage");
    File folder2 = new File("src\\training\\maskImage");

    File testFolderAnnotations = new File("Val\\Annotations");
    File testFolderImages = new File("Val\\Pictures");

    File [] file1 = folder1.listFiles();
    File [] file2 = folder2.listFiles();

    File [] testFileAnnotations = testFolderAnnotations.listFiles();
    File [] testFileImages = testFolderImages.listFiles();

    public MaskDetection(String fileName) throws IOException, ParserConfigurationException, SAXException {
        map = new HashMap<File,File>() ;

        for(int i=0 ; i<file1.length ; i++){
            map.put(file2[i], file1[i]) ;
        }

        testMap = new TreeMap<File,File>() ;

        for(int i=0 ; i<testFileImages.length ; i++){
            testMap.put(testFileAnnotations[i], testFileImages[i]) ;
        }

        constructTrainingSupportArray();
        training();
        testing();

        realImageTesting(fileName);
    }

    public void realImageTesting(String fileName) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String imgFile = fileName ;
        Mat src = Imgcodecs.imread(imgFile) ;
        String xmlFile = "xml/lbpcascade_frontalface.xml" ;
        CascadeClassifier cc = new CascadeClassifier(xmlFile) ;

        BufferedImage image = ImageIO.read(new File(imgFile)) ;
        KMeansCl kMeans = new KMeansCl(3, image) ;

        MatOfRect faceDetection = new MatOfRect();
        cc.detectMultiScale(src, faceDetection);

        int sl = 0 ;
        for(Rect rect: faceDetection.toArray()){
            int boo = testImage(imgFile, rect.x, rect.x+rect.width, rect.y, rect.y+rect.height, sl, kMeans) ;
            double slice = rect.width/6 ;
            double slice2 = rect.height/28 ;

            if(boo==1){
                Imgproc.rectangle(src, new org.opencv.core.Point(rect.x+30, rect.y), new Point(rect.x+rect.width-15, rect.y + rect.height), new Scalar(0,255,0), 2);
            }
            else if(boo==-1){
                Imgproc.rectangle(src, new org.opencv.core.Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y + rect.height), new Scalar(1,255,0), 2);
                Imgproc.rectangle(src, new org.opencv.core.Point(rect.x+2*slice, rect.y+14*slice2), new Point(rect.x+rect.width-2*slice, rect.y + rect.height-7*slice2), new Scalar(203, 192, 255), 4);
            }
            else{
                Imgproc.rectangle(src, new org.opencv.core.Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y + rect.height), new Scalar(0,0,255), 2);
            }
            sl++ ;
        }

        Imgcodecs.imwrite("images/output.jpg", src) ;
    }

    public int testImage(String imageFile, int xmin, int xmax, int ymin, int ymax, int serialNo, KMeansCl kMeans) throws IOException {
        BufferedImage image = ImageIO.read(new File(imageFile)) ;

        int s=0, n=0;
        double startI = ymin + (ymax-ymin)*1.0/2 ;
        for(int i= (int) startI; i<image.getHeight() && i<ymax ; i++){
            for(int j=xmin ; j<image.getWidth() && j<xmax ; j++){
                int iR=0, iG=0, iB=0 ;

                int imgRGB = image.getRGB(j,i) ;
                iB = imgRGB & 0xff ;
                iG = (imgRGB & 0xff00) >> 8 ;
                iR = (imgRGB & 0xff0000) >> 16 ;
                if(finalArray[iR][iG][iB]<0.8){
                    n++ ;
                }
                else{
                    s++ ;
                }
            }
        }

        double answer = s*1.0/(s+n) ;

        if(answer>=0.2){
            System.out.println("Result: Without mask.");
            return 0 ;
        }
        else {
            System.out.println("Result: With mask.");
            return noseTesting(xmin, xmax, ymin, ymax, kMeans) ;
        }
    }

    public int noseTesting(int xmin, int xmax, int ymin, int ymax, KMeansCl kMeans) throws IOException {
        return kMeans.setFaceArea(xmin, ymin, xmax, ymax) ;
    }

    public void training() throws IOException{

        double skinArray[][][] = new double[256][256][256] ;
        double nonSkinArray[][][] = new double[256][256][256] ;

        int totalOfSkin=0 , totalOfNonSkin=0 ,f = -1 ;

        for(Map.Entry m:map.entrySet()){

            BufferedImage img = ImageIO.read((File)m.getValue()) ;
            BufferedImage mask = ImageIO.read((File)m.getKey()) ;

            for(int i=0 ; i<img.getHeight() ; i++){
                for(int j=0 ; j<img.getWidth() ; j++){
                    int iR=0,iG=0,iB=0,mR=0,mG=0,mB=0 ;

                    int imgRGB = img.getRGB(j,i) ;
                    iB = imgRGB & 0xff ;
                    iG = (imgRGB & 0xff00) >> 8 ;
                    iR = (imgRGB & 0xff0000) >> 16 ;

                    int maskRGB = mask.getRGB(j,i) ;
                    mB = maskRGB & 0xff ;
                    mG = (maskRGB & 0xff00) >> 8 ;
                    mR = (maskRGB & 0xff0000) >> 16 ;

                    if(mB>240 && mG>240 && mR>240){
                        nonSkinArray[iR][iG][iB]++ ;
                        totalOfNonSkin++ ;
                    }

                    else{
                        skinArray[iR][iG][iB]++ ;
                        totalOfSkin++ ;
                    }
                }
            }
        }

        for(int i=0 ; i<256 ; i++){
            for(int j=0 ; j<256 ; j++){
                for(int k=0 ; k<256 ; k++){
                    skinArray[i][j][k] = skinArray[i][j][k]/totalOfSkin ;
                    nonSkinArray[i][j][k] = nonSkinArray[i][j][k]/totalOfNonSkin ;

                    if(nonSkinArray[i][j][k]==0 && skinArray[i][j][k]==0){
                        if(supportArray[i][j][k]==1){
                            finalArray[i][j][k] = 1 ;
                        }else{
                            finalArray[i][j][k] = 0 ;
                        }
                    }
                    else if(nonSkinArray[i][j][k]==0){
                        finalArray[i][j][k] = 1 ;
                    }
                    else
                        finalArray[i][j][k] = skinArray[i][j][k]/nonSkinArray[i][j][k] ;
                }
            }
        }
    }

    public void testing() throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document ;

        int tp=0, tn=0, fp=0, fn=0, tc=0, ti=0, fc=0, fi=0 ;

        for (Map.Entry m:testMap.entrySet()){
            BufferedImage image = ImageIO.read((File)m.getValue()) ;

            document = db.parse((File)m.getKey()) ;
            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("object") ;

            BufferedImage image1 = ImageIO.read((File)m.getValue()) ;
            KMeansCl kMeans = new KMeansCl(3, image1) ;

            for(int itr=0; itr < nodeList.getLength(); itr++) {
                int xmin=0, ymin=0, xmax=0, ymax=0 ;
                String name = "" ;
                Node node = nodeList.item(itr);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    name = eElement.getElementsByTagName("name").item(0).getTextContent();

                    NodeList bndChildren = eElement.getElementsByTagName("bndbox").item(0).getChildNodes() ;

                    for (int s=0; s<bndChildren.getLength(); s++){
                        Node axis = bndChildren.item(s);
                        if(axis.getNodeType()==Node.ELEMENT_NODE){
                            Element child = (Element) axis ;
                            String key = child.getNodeName();
                            String value = child.getTextContent();

                            switch (key){
                                case "xmin":
                                    xmin = Integer.parseInt(value) ;
                                    break;
                                case "ymin":
                                    ymin = Integer.parseInt(child.getTextContent()) ;
                                    break;
                                case "xmax":
                                    xmax = Integer.parseInt(child.getTextContent()) ;
                                    break;
                                case "ymax":
                                    ymax = Integer.parseInt(child.getTextContent()) ;
                                    break;
                            }

                        }
                    }
                }

                int s=0, n=0;
                double startI = ymin + (ymax-ymin)*1.0/2 ;
                for(int i= (int) startI; i<image.getHeight() && i<ymax ; i++){
                    for(int j=xmin ; j<image.getWidth() && j<xmax ; j++){
                        int iR=0, iG=0, iB=0 ;

                        int imgRGB = image.getRGB(j,i) ;
                        iB = imgRGB & 0xff ;
                        iG = (imgRGB & 0xff00) >> 8 ;
                        iR = (imgRGB & 0xff0000) >> 16 ;
                        if(finalArray[iR][iG][iB]<0.8){
                            n++ ;
                        }
                        else{
                            s++ ;
                        }
                    }
                }

                double answer = s*1.0/(s+n) ;

                if(answer>=0.2){
                    if(name.equals("without_mask")){
                        tn++ ;
                    }
                    else if(name.equals("with_mask")){
                        fp++ ;
                    }
                    else if(name.equals("incorrect_mask")){
                        fp++ ;
                    }
                }
                else{


                    if(name.equals("with_mask")){
                        int boo = kMeans.setFaceArea(xmin, ymin, xmax, ymax) ;
                        tp++ ;
                        if(boo==1) {
                            tc++ ;
                        }
                        else if(boo==-1){
                            fc++ ;
                        }
                    }
                    else if(name.equals("incorrect_mask")){
                        int boo = kMeans.setFaceArea(xmin, ymin, xmax, ymax) ;
                        tp++ ;
                        if(boo==1) {
                            fi++ ;
                        }
                        else if(boo==-1) {
                            ti++ ;
                        }
                    }

                    else if(name.equals("without_mask")){
                        fn++ ;
                    }
                }
            }
        }

        double accuracy = (tp+tn)*1.0/(tp+tn+fp+fn) ;
        double Precision = tp*1.0 / (tp + fp) ;
        double Recall = tp*1.0 / (tp + fn) ;
        double F_Score = 2.0*tp /(2.0*tp + fn + fp);
        System.out.println("\n\nMask Detection(With_Mask/Without_Mask):");
        System.out.println("\tAccuracy: " + accuracy);
        System.out.println("\tPrecision: " + Precision);
        System.out.println("\tRecall: " + Recall);
        System.out.println("\tF-Score: " + F_Score);

        accuracy = (tc+ti)*1.0/(tc+ti+fc+fi) ;
        Precision = tc*1.0 / (tc + fc) ;
        Recall = tc*1.0 / (tc + fi) ;
        F_Score = 2.0*tc /(2.0*tc + fi + fc);

        System.out.println("\n\nNose Detection(Mask_Wear_Correctly/Mask_Wear_Incorrectly):");
        System.out.println("\tAccuracy: " + accuracy);
        System.out.println("\tPrecision: " + Precision);
        System.out.println("\tRecall: " + Recall);
        System.out.println("\tF-Score: " + F_Score);


        tp = tc + ti ;
        fp = fc + fi ;

        accuracy = (tp+tn)*1.0/(tp+tn+fp+fn) ;
        Precision = tp*1.0 / (tp + fp) ;
        Recall = tp*1.0 / (tp + fn) ;
        F_Score = 2.0*tp /(2.0*tp + fn + fp);

        System.out.println("\n\nOverall(Without_Mask/Mask_Wear_Correctly/Mask_Wear_Incorrectly):");
        System.out.println("\tAccuracy: " + accuracy);
        System.out.println("\tPrecision: " + Precision);
        System.out.println("\tRecall: " + Recall);
        System.out.println("\tF-Score: " + F_Score);
    }

    public void constructTrainingSupportArray() throws FileNotFoundException {
        File file = new File("src//Skin_NonSkin.txt");
        Scanner fileScanner = new Scanner(file) ;

        while (fileScanner.hasNextInt()) {
            int R, G, B, skinOrNonSkin ;
            B = fileScanner.nextInt() ;
            G = fileScanner.nextInt() ;
            R = fileScanner.nextInt() ;
            skinOrNonSkin = fileScanner.nextInt() ;

            if (skinOrNonSkin==2) {
                supportArray[R][G][B] = 0;
            } else {
                supportArray[R][G][B] = 1;
            }
        }
    }
}