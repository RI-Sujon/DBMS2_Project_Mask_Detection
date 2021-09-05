import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class KMeansCl {
    
    int R[], G[], B[], r[], g[], b[];
    BufferedImage image;
    int height , width;
    int totCount[] ;
    int K ;
    double arr[] ;
    int xmin, ymin, xmax, ymax ;
        
    public KMeansCl(int K, BufferedImage image){
        this.K = K ;

        this.image = image ;
    }

    public int setFaceArea(int xmin, int ymin, int xmax, int ymax) throws IOException {
        this.xmin = xmin - 0 ;
        this.xmax = xmax + 0 ;
        this.ymin = ymin - 0 ;
        this.ymax = ymax + 0;

        init() ;
        middle() ;

        return printImage() ;
    }
    
    
    public void init() throws IOException{
        R = new int[K];
        G = new int[K];
        B = new int[K];

        r = new int[K];
        g = new int[K];
        b = new int[K];
        
        for(int i = 0 ; i<K ; i++){
            r[i] = 0 ;
            g[i] = 0 ;
            b[i] = 0 ;
        }
        
        arr = new double[K] ;
        totCount = new int[K] ;
        
        int y[] = new int[K];
        int x[] = new int[K];

        width=xmax-xmin;
        height=ymax-ymin;

        for(int i=0;i<K;i++){
            Random rand=new Random();

            y[i]=rand.nextInt(width) + xmin;
            x[i]=rand.nextInt(height) + ymin;

            int pixel = image.getRGB(y[i], x[i]);

            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;

            R[i]=red;
            G[i]=green;
            B[i]=blue;
        }
    } 
    
    public double getDistance(double x,double y,double x1,double y1,double x2,double y2){
        double distance=Math.pow((x-y),2)+Math.pow((x1-y1),2)+Math.pow((x1-y1),2);
        distance = Math.sqrt(distance);
        
        return distance;
    }
    
    public void middle(){
        while(true){
            for(int i = 0 ; i<K ; i++){
                totCount[i] = 0 ;
            }
            
            for (int i = ymin; i < ymax ; i++){
                for (int j = xmin; j < xmax ; j++){
                    for(int k=0;k<K;k++){
                        arr[k]=0;
                    }

                    int pixel = image.getRGB(j, i);

                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = (pixel) & 0xff;

                    for(int k=0;k<K;k++){
                        arr[k]=getDistance(red,R[k],green,G[k],blue,B[k]);
                    }
                    
                    double min= arr[0];
                    int index=0;

                    for(int k=0;k < K;k++){
                        if(min>arr[k]){
                            min=(int) arr[k];
                            index=k;
                        }
                    }
                   
                    r[index] = r[index] + red ;
                    g[index] = g[index] + green ;
                    b[index] = b[index] + blue ;
                    
                    totCount[index]++ ;                 
                }
            }
            
            for(int i=0;i < K;i++){
                if(totCount[i]!=0){
                    r[i] = r[i]/totCount[i] ;
                    g[i] = g[i]/totCount[i] ;
                    b[i] = b[i]/totCount[i] ;
                }
                else {
                    r[i] = 0 ;
                    g[i] = 0 ;
                    b[i] = 0 ;
                }
                
                //System.out.println("i:"+i+"\tr:"+r[i]+"\tg:"+g[i] +"\tb:"+b[i] + "\n");
            }
            int count = 0 ;
            for(int i = 0 ; i<K ; i++){
                if(getDistance(r[i],R[i],g[i],G[i],b[i],B[i])<0.000000001){
                    count++ ;
                }
                
                R[i] = r[i] ;
                G[i] = g[i] ;
                B[i] = b[i] ;
            }
            
            if(count==K) break ;
       }
    }
    
    public int printImage() throws IOException{
        int a[] = new int[3];
        a[0] = 0 ;
        a[1] = 0 ;
        a[2] = 0 ;

        double slice = width/12 ;
        double slice2 = height/28 ;

        for (int i = ymin; i < ymax; i++){
            for (int j = xmin; j < xmax; j++){
                int pixel = image.getRGB(j, i);

                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;

                for(int k=0 ; k<K ; k++){
                    arr[k]=getDistance(red,R[k],green,G[k],blue,B[k]);
                }

                int min=(int) arr[0];

                int index = 0;
                for(int k=0 ; k<K ;k++){
                    if(min>arr[k]){
                        min = (int) arr[k];
                        index = k;
                    }
                }

                if (i>ymin+4*slice2 && i<ymax-slice2)
                    a[index]++ ;

                image.setRGB(j,i,(new Color(R[index],G[index],B[index])).getRGB());

            }
        }


        int nose ;
        if(a[0]<a[1] && a[0]<a[2]){
            nose = 0 ;
        }
        else if(a[1]<a[0] && a[1]<a[2]){
            nose = 1 ;
        }
        else nose = 2 ;

        a[0] = 0 ;
        a[1] = 0 ;
        a[2] = 0 ;

        for (int i = (int) (ymin+12*slice2); i < ymax-8*slice2; i++){
            for (int j = (int) (xmin+3*slice); j < xmax-3*slice; j++){
                int pixel = image.getRGB(j, i);

                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;

                int index = -1 ;
                for(int k=0; k<3 ; k++){
                    if(red==R[k] && green==G[k] && blue==B[k]){
                        index = k ;
                    }
                }

                a[index]++ ;
            }
        }

        double nosePercent = a[nose]*1.0/(a[0]+a[1]+a[2]) ;

        if(nosePercent>0.08){
            return -1;
        }
        else return 1;

    }
}

