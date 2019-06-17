/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wpo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.log10;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import static java.util.Arrays.sort;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jakub
 */
enum PictureType{
    mono, rgb, grey;
}
enum ColorTableType{
    h48,
    VGA,
    RGB;
}
public class PCXPicture {
    private Header header; //header pliku 
    private ArrayList<double[]> data; //dane obrazu po dekodowaniu
    private byte[] colorPalette=null; //paleta kolorów (dla obrazów rgb)
    private String fileName; //nazwa obrazu
    private PictureType pictureType; //typ obrazu (rgb/greyscale)
    private ColorTableType colorTableType; //rozmiar tablicy kolorów (typ 48/typ 256*3 bajtowy)
    private FileInputStream pictureInput; //strumień danych wejściowych obrazu
    private int[][] histogram;
    
    PCXPicture(String filePath,String fileName){
        this.fileName=fileName;
        try {
            //tworzenie headera
            pictureInput=new FileInputStream(new File(filePath));
            byte[] tempBytes=new byte[128];
            pictureInput.read(tempBytes,0,128);
            header=new Header(tempBytes);
            //sprawdzanie headera
            if(!header.checkHeader())
                System.exit(1);
            //ustawianie typów obrazu
            if(header.nPlanes== 0x01 && (header.bitsPerPixel== 0x01 
                    || header.bitsPerPixel== 0x02 ||header.bitsPerPixel== 0x04)){
                pictureType=PictureType.mono;
                colorTableType=ColorTableType.h48;
                colorPalette=null;
            }
            else if(header.bitsPerPixel==0x08 && header.nPlanes== 0x01){
                pictureType=PictureType.grey;
                colorTableType=ColorTableType.VGA;
                colorPalette=new byte[768];
            }
            else if(header.bitsPerPixel==0x08 && header.nPlanes==0x03){
                pictureType=PictureType.rgb;
                colorTableType=ColorTableType.RGB;
                colorPalette=new byte[768];
            } 
            else System.exit(1);
            //pobieranie zawartości obrazu:
            decode();
            //ewentualne pobieranie palety barw
            readPalette();
            makeHistogram();
        }catch (IOException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public int getWidth(){
        return header.getWidth();
    }
    
    public int getHeight(){
        return header.getHeight();
    }
    
    public void readPalette(){
        try {
            colorPalette=null;
            if(colorTableType==ColorTableType.VGA){
                byte[] b=new byte[1];
                pictureInput.read(b,0,1);
                if(b[0] !=0x0C)
                    throw new IllegalArgumentException("brak flagi");
            }
            if(pictureInput.available()==768){
                colorPalette=new byte[768];
                pictureInput.read(colorPalette,0,768);
            }
        }catch (IllegalArgumentException e) {
            System.err.print(e.getMessage());
        }catch (IOException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void encode(int scanLineLength,ArrayList<byte[]> copiedData, FileOutputStream fos){
        for(byte[] buffer: copiedData){
            int index=0;
            do{
                int i=0;
                while(i<62 && index+i+1< scanLineLength && buffer[index+i]==buffer[index+i+1])
                    ++i;
                try {
                    if(i>0){
                        fos.write(i+1 | 0xC0);
                        fos.write(buffer[index]);
                        index+=i+1;
                    
                    }else{
                        if((buffer[index] & 0xC0)==0xC0)
                            fos.write(0xC1);
                        fos.write(buffer[index++]);
                    }
                }catch (IOException ex) {
                        Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }while(index<scanLineLength);
        }
    }
    
    public void decode(){
        int scanLineLength=header.nPlanes*header.bytesPerLine;
        ArrayList<byte[]>byteData=new ArrayList<>();
        for(int h=0;h<header.getHeight();h++){
            int index=0;
            byte[] buffer= new byte[scanLineLength];
            do{
                try {
                    byte[] value=new byte[1];
                    pictureInput.read(value,0,1);
                    if((value[0] & 0xC0)== 0xC0){
                        int count=value[0] & 0x3F;
                        if(count==0 || index+count-1>=scanLineLength)
                            System.err.print("za dużo powtórzeń");
                        pictureInput.read(value,0,1);
                        for(int i=1;i<=count;i++)
                            buffer[index++]=value[0];
                    }
                    else buffer[index++]=value[0];
                } catch (IOException ex) {
                    Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }while(index<scanLineLength);
            byteData.add(buffer);
        }
        data=dataToDouble(byteData);
    }
    
    public ArrayList<double[]> dataToDouble(ArrayList<byte[]> translatedData){       //zamiana informacji z byte na double
        ArrayList<double[]> doubleData=new ArrayList<>();
        for(int i=0;i<translatedData.size();i++){
            double[] row=new double[translatedData.get(0).length];
            for(int j=0;j<translatedData.get(0).length;j++)
                row[j]=Byte.toUnsignedInt(translatedData.get(i)[j]);
            doubleData.add(row);
        }
        return doubleData;
    }
    public ArrayList<byte[]> dataToByte(ArrayList<double[]> translatedData){       //zamiana informacji z double na byte
        if(notInRange(translatedData))
            translatedData=getFitInRange();
        ArrayList<byte[]> byteData=new ArrayList<>();
        for(int i=0;i<translatedData.size();i++){
            byte[] row=new byte[translatedData.get(0).length];
            for(int j=0;j<translatedData.get(0).length;j++){
                row[j]=(byte)(floor(translatedData.get(i)[j]));
            }
            byteData.add(row);
        }
        return byteData;
    }
    
    public PictureType getPictureType(){
        return pictureType;
    }
    public ArrayList<double[]> getData(){
        return data;
    }
    
    public void addValue(int value){
        if(value==0)return;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                data.get(i)[j]+=value;
            }
        }
    }
    
    public void addPictureValues(ArrayList<double[]> copiedData){
        if(data.size()!=copiedData.size() || copiedData.get(0).length!= data.get(0).length) return;
        if(pictureType==pictureType.rgb){
            for(int i=0;i<data.size();i++){
                for(int j=0;j<data.get(i).length/3;j++){
                    Double[] rgb1=new Double[3]; //pierwsza wartość w rgb
                    Double[] rgb2=new Double[3]; //druga wartość w rgb
                    Double[] suma=new Double[3]; //suma wrtości
                    for(int k=0;k<3;k++){
                        rgb1[k]=data.get(i)[j+k*data.get(i).length/3];
                        rgb2[k]=copiedData.get(i)[j+k*copiedData.get(i).length/3];
                        suma[k]=rgb1[k]+rgb2[k];
                    }
                    int kMax=(suma[0]>suma[1])?0:1;
                    kMax=(suma[kMax]>suma[2])?kMax:2;
                    Double D=0.0;
                    if(suma[kMax]>255.0){
                        D=suma[kMax]-255.0;
                        D=ceil((D/255.0)*100); //procentowa proporcja D do 255
                    }
                    for(int k=0;k<3;k++)
                        suma[k]=rgb1[k]-rgb1[k]*D/100+rgb2[k]-rgb2[k]*D/100;
                    data.get(i)[j]=suma[0];
                    data.get(i)[j+data.get(i).length/3]=suma[1];
                    data.get(i)[j+2*data.get(i).length/3]=suma[2];
                }
            }
        }
        else if(pictureType==pictureType.grey){
            for(int i=0;i<data.size();i++){
                for(int j=0;j<data.get(i).length;j++){
                    Double dataValue=data.get(i)[j] 
                            +copiedData.get(i)[j];
                    data.get(i)[j]=dataValue;
                }
            }
        }
    }
    
    public void multiplyByValue(int value){
        if(value==1 || value<0)return;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue=data.get(i)[j];
                dataValue*=value;
                data.get(i)[j]=dataValue;
            }
        }
    }
    
    public void multiplyByPictureValues(ArrayList<double[]> copiedData){
        if(data.size()!=copiedData.size() || copiedData.get(0).length!= data.get(0).length) return;
        if(pictureType==pictureType.rgb){
            for(int i = 0; i < data.size(); i++) {
		for(int j = 0; j < data.get(i).length;j++) {
			Double c1 =data.get(i)[j];
                        Double c2 =copiedData.get(i)[j];
			if(c1== 255.0)
                             c1 = c2;
                        else if(c1 == 0)
                             ;
                        else
                             c1 = Math.ceil((c1 * c2)/255);
                        data.get(i)[j] = c1;
               }
            }
        }
        else if(pictureType==pictureType.grey){
            for(int i=0;i<data.size();i++){
                for(int j=0;j<data.get(i).length;j++){
                    Double dataValue=ceil(data.get(i)[j]
                            *copiedData.get(i)[j]);
                    data.get(i)[j]=dataValue;
                }
            }
        }
    }
    
    public void divideByPictureValues(ArrayList<double[]> copiedData){
        if(data.size()!=copiedData.size() || copiedData.get(0).length!= data.get(0).length) return;
        if(pictureType==pictureType.rgb){
            for(int i=0;i<data.size();i++){
                for(int j=0;j<data.get(i).length/3;j++){
                    Double[] rgb1=new Double[3]; //pierwsza wartość w rgb
                    Double[] rgb2=new Double[3]; //druga wartość w rgb
                    Double[] suma=new Double[3]; //suma kolorów
                    for(int k=0;k<3;k++){
                        rgb1[k]=data.get(i)[j+k*data.get(i).length/3];
                        rgb2[k]=copiedData.get(i)[j+k*copiedData.get(i).length/3];
                        suma[k]=rgb1[k]+rgb2[k];
                    }
                    double valueMax=suma[0];
                    if(suma[1]>valueMax)valueMax=suma[1];
                    if(suma[2]>valueMax)valueMax=suma[2];
                    data.get(i)[j]= ceil((rgb2[0]*255)/valueMax);
                    data.get(i)[j+data.get(i).length/3]= ceil((rgb2[1]*255)/valueMax);
                    data.get(i)[j+2*data.get(i).length/3]= ceil((rgb2[2]*255)/valueMax);
                }
            }
        }
        else if(pictureType==pictureType.grey){
            for(int i=0;i<data.size();i++){
                for(int j=0;j<data.get(i).length;j++){
                    if(copiedData.get(i)[j]==0){
                        data.get(i)[j]=0;
                        continue;
                    }
                    Double dataValue=ceil((double)(data.get(i)[j])
                            /copiedData.get(i)[j]);
                    data.get(i)[j]=dataValue;
                }
            }
        }
    }
    
    public void divideByValue(double value){
        if(value<=0 || value==1) return;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue=data.get(i)[j];
                dataValue/=value;
                data.get(i)[j]=dataValue;
            }
        }
    }
    
    public void normalize(){
        double max=MIN_VALUE;
        double min=MAX_VALUE;
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(0).length;j++){
                max=max(max,data.get(i)[j]);
                min=min(min,data.get(i)[j]);
            }
        double value=255/(max-min);
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(0).length;j++){
                data.get(i)[j]=(value*(data.get(i)[j]-min));
            }
    }
    public void raiseToThePower(int power){
        if(power<0.0|| power==1.0)return;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue=data.get(i)[j];
                data.get(i)[j]=pow(dataValue,power);
            }
        }
    }
    
    public void rootValue(int rootNumber){
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue=data.get(i)[j];
                dataValue=Math.pow(dataValue,1/(double)(rootNumber));
                data.get(i)[j]=dataValue;
            }
        }
    }
    
    public void logarithm(){
        double max=MIN_VALUE;
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(i).length;j++)
                max=max(max,data.get(i)[j]);
        max+=1;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue=data.get(i)[j];
                dataValue=log10((double)(1+dataValue))*(255*log10(max));
                data.get(i)[j]=dataValue;
            }
        }
    }
    
    public void moveBy(int moveX,int moveY){
        ArrayList<double[]> movedData=new ArrayList<>();
        for(int i=0;i<data.size();i++){
            double[] movedValues=new double[data.get(i).length];
            if(pictureType == PictureType.rgb){
                for(int j=0;j<movedValues.length/3;j++){
                    for(int k=0;k<3;k++){
                        if((i-moveY>=0) && (i-moveY<data.size()) && 
                            (j-moveX>=0) && (j-moveX < data.get(i).length/3))
                                movedValues[j+k*movedValues.length/3]=data.get(i-moveY)[j+k*movedValues.length/3-moveX];
                        else movedValues[j+k*movedValues.length/3]=0;
                    }
                }
            }
            else if(pictureType==pictureType.grey){
                for(int j=0;j<movedValues.length;j++){
                    if( (i-moveY)>=0 && (i-moveY)<data.size() && (j-moveX)>=0 && j-moveX<data.get(i).length )
                        movedValues[j]=data.get(i-moveY)[j-moveX];
                    else movedValues[j]=0;
                }
            }
            movedData.add(movedValues);
        }
        data=movedData;
    }
    public void resize(int  sizeX,int  sizeY){
        double scaleX=(double)sizeX/(double)getWidth();
        double scaleY=(double)sizeY/(double)getHeight();
        scale(scaleX,scaleY);
    }
    public void scale(double scaleX, double scaleY){
        if(scaleX==0 || scaleY==0)return;
        header.positionRight=(short)(ceil(header.getWidth()*scaleX)-1);
        header.positionBottom=(short)(ceil(header.getHeight()*scaleY)-1);
        header.bytesPerLine=(short) ceil(header.getWidth()*(header.bitsPerPixel/8));
        int scanLineLength=header.nPlanes*header.bytesPerLine;
        ArrayList<double[]> scaledData=new ArrayList<>();
        for(int i=0;i<header.getHeight();i++){
            double[] row=new double[scanLineLength];
            for(int j=0;j<row.length;j++)
                row[j]=0;
            scaledData.add(row);
        }
        for(int i=0;i<data.size();i++){//po igrekach
            if(pictureType==pictureType.rgb){
                for(int j=0;j<data.get(i).length/3;j++){  //po iksach
                    for(int nny=0;nny<(i+1)*scaleY-i*scaleY; nny++) //ale wypełniamy ny pól
                        for(int nnx=0;nnx<(j+1)*scaleX-j*scaleX;nnx++) //ale wypełniamy nx pól
                            for(int k=0;k<3;k++){
                                double b=data.get(i)[j+k*data.get(i).length/3];
                                int y=(int)floor((i*scaleY));
                                int x=(int)floor((j*scaleX)+k*getWidth());
                                scaledData.get(y+nny)[x+nnx]=b;
                            }
                }
            }
            else if(pictureType==pictureType.grey){
                for(int j=0;j<data.get(i).length;j++){  //po iksach
                    for(int nny=0;nny<(i+1)*scaleY-i*scaleY; nny++) //ale wypełniamy ny pól
                        for(int nnx=0;nnx<(j+1)*scaleX-j*scaleX;nnx++) //ale wypełniamy nx pól
                            scaledData.get((int)(i*scaleY)+nny)[(int)(j*scaleX)+nnx]=data.get(i)[j];
                }
            }
        }
        data=scaledData;
    }
    
    public void flipAxis(char axis){
        if(axis!='x' && axis!='X' && axis!='Y' && axis!='y')
            return;
        ArrayList<double[]> flippedData=new ArrayList<>();
        if(axis=='Y'|| axis=='y'){
            for(int i=0;i<data.size();i++){
                double[] row=new double[data.get(i).length];
                if(pictureType==pictureType.rgb){
                    for(int j=0;j<row.length/3;j++)
                        for(int k=0;k<3;k++){//wartość=wartość od końca z przesunięciem
                            row[j+k*row.length/3]=data.get(i)[(row.length/3)+(k*row.length/3)-1-j]; 
                        }
                }
                else if(pictureType==pictureType.grey){
                    for(int j=0;j<row.length;j++)
                        row[j]=data.get(i)[(row.length-1)-j];
                }
                flippedData.add(row);
            }
        }
        else if(axis=='X'||axis=='x'){
            for(int i=data.size()-1;i>=0;i--)
                flippedData.add(data.get(i));
        }
        data=flippedData;
    }
    
    public void flipByShiftedAxis(char axis, int shiftBy){
        if(shiftBy<=0 && axis!='x' &&  axis!='X'
            &&  axis!='Y' &&  axis!='y') return;
        ArrayList<double[]> flippedData=new ArrayList<>();
        if(axis=='Y'|| axis=='y'){ //odbijamy względem prostej x=c)
            for(int i=0;i<data.size();i++){
                double[] row=new double[data.get(i).length];
                if(pictureType==pictureType.rgb){
                    if(shiftBy>=row.length/3-1) return;
                    if(shiftBy<row.length/6){//odbijamy prawą cześć obrazu
                        for(int j=0;j<shiftBy;j++)
                            for(int k=0;k<3;k++){
                                int xS=2*shiftBy-j; //współrzędna symetryczna do obecnie analizowanej
                                row[j+k*row.length/3]=data.get(i)[xS+k*row.length/3];
                            }
                        for(int j=shiftBy;j<row.length/3;j++)
                            for(int k=0;k<3;k++)
                                row[j+k*row.length/3]=data.get(i)[j+k*row.length/3];
                    }
                    else{ //odbijamy lewą część obrazu
                        for(int j=0;j<=shiftBy;j++)
                            for(int k=0;k<3;k++)
                                row[j+k*row.length/3]=data.get(i)[j+k*row.length/3];
                        for(int j=shiftBy+1;j<row.length/3;j++)
                            for(int k=0;k<3;k++){
                                int xS=2*shiftBy-j; //współrzędna symetryczna do obecnie analizowanej
                                row[j+k*row.length/3]=data.get(i)[xS+k*row.length/3];
                            }
                    }
                }
                else if(pictureType==pictureType.grey){ 
                    if(shiftBy>=row.length-1) return;
                    if(shiftBy<row.length/2){//odbijamy prawą cześć obrazu
                        for(int j=0;j<shiftBy;j++){
                                int xS=2*shiftBy-j; //współrzędna symetryczna do obecnie analizowanej
                                row[j]=data.get(i)[xS];
                            }
                        for(int j=shiftBy;j<row.length;j++)
                                row[j]=data.get(i)[j];
                    }
                    else{ //odbijamy lewą część obrazu
                        for(int j=0;j<shiftBy;j++)
                                row[j]=data.get(i)[j];
                        for(int j=shiftBy;j<row.length;j++){
                                int xS=2*shiftBy-j; //współrzędna symetryczna do obecnie analizowanej
                                row[j]=data.get(i)[xS];
                        }
                    }
                }
                flippedData.add(row);
            }
        }
        else if(axis=='X'||axis=='x'){ //odbijamy względem prostej y=c
            if(shiftBy>=data.size()-1)return;
            if(shiftBy>data.size()/2){ //odbijamy górną połowę
                for(int i=0;i<=shiftBy;i++)
                    flippedData.add(data.get(i));
                for(int i=1;shiftBy+i<data.size();i++)
                    flippedData.add(data.get(shiftBy-i));
            }
            else{ //odbijamy dolną połowę
                for(int i=2*shiftBy;i>shiftBy;i--)
                    flippedData.add(data.get(i));
                for(int i=shiftBy;i<data.size();i++)
                    flippedData.add(data.get(i));
            }
        }
        data=flippedData;
    }
    
    public void mix(ArrayList<double[]> copiedData, double value){
        if(data.size()!=copiedData.size() || copiedData.get(0).length!= data.get(0).length) 
            return;
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).length;j++){
                Double dataValue= value*data.get(i)[j]
                        + (1-value)*copiedData.get(i)[j];
                data.get(i)[j]=dataValue;
            }
        }
    }
    
    public void cutFromPicture(int x,int y,int h,int w,String path){
        if(x<0 || y<0 || y+h>data.size())return;
        ArrayList<double[]> copiedData=new ArrayList<>();
        if(pictureType==PictureType.rgb){
            if(x+w>data.get(0).length/3)return;
            for(int i=0;i<h;i++){
                double[] row=new double[w*3];
                for(int j=0;j<w;j++)
                    for(int k=0;k<3;k++){
                        row[j+k*w]=data.get(y+i)[x+j+k*data.get(i).length/3];
                        data.get(y+i)[x+j+k*data.get(i).length/3]=0;
                    }
                copiedData.add(row);
            }
        }
        else if(pictureType==pictureType.grey){
            if(x+w>data.get(0).length)return;
            for(int i=0;i<h;i++){
                double[] row=new double[w];
                for(int j=0;j<w;j++){
                    row[j]=data.get(y+i)[x+j];
                    data.get(y+i)[x+j]=0;
                }
                copiedData.add(row);
            }
        }
        Header fileHeader=new Header(header.toBytes());
        fileHeader.positionBottom=(short) (fileHeader.positionTop+h-1);
        fileHeader.positionRight=(short) (fileHeader.positionLeft+w-1);
        fileHeader.bytesPerLine=(short) (w*fileHeader.bitsPerPixel/8);
        try {
            FileOutputStream fos=new FileOutputStream(new File(path));
            fos.write(fileHeader.toBytes());
            encode(fileHeader.getScanLineLength(),dataToByte(copiedData),fos);
            if(pictureType==PictureType.grey) //paleta szarości jest poprzedzona flagą
                fos.write(0xC0);
            if(colorPalette!=null)
                fos.write(colorPalette);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void copyFromPicture(int x,int y,int h,int w,String path){
        if(x<0 || y<0 || y+h>data.size())return;
        ArrayList<double[]> copiedData=new ArrayList<>();
        if(pictureType==PictureType.rgb){
            if(x+w>data.get(0).length/3)return;
            for(int i=0;i<h;i++){
                double[] row=new double[w*3];
                for(int j=0;j<w;j++)
                    for(int k=0;k<3;k++)
                        row[j+k*w]=data.get(y+i)[x+j+k*data.get(i).length/3];
                copiedData.add(row);
            }
        }
        else if(pictureType==pictureType.grey){
            if(x+w>data.get(0).length)return;
            for(int i=0;i<h;i++){
                double[] row=new double[w];
                for(int j=0;j<w;j++)
                    row[j]=data.get(y+i)[x+j];
                copiedData.add(row);
            }
        }
        Header fileHeader=new Header(header.toBytes());
        fileHeader.positionBottom=(short) (fileHeader.positionTop+h-1);
        fileHeader.positionRight=(short) (fileHeader.positionLeft+w-1);
        fileHeader.bytesPerLine=(short) (w*fileHeader.bitsPerPixel/8);
        try {
            FileOutputStream fos=new FileOutputStream(new File(path));
            fos.write(fileHeader.toBytes());
            encode(fileHeader.getScanLineLength(),dataToByte(copiedData),fos);
            if(pictureType==PictureType.grey) //paleta szarości jest poprzedzona flagą
                fos.write(0xC0);
            if(colorPalette!=null)
                fos.write(colorPalette);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void rotate(double angle){
        angle=toRadians(angle); //zamiana kąta na radiany
        //środek obrazu
        double xCPoint=(getWidth()-1)/2.0;
        double yCPoint=(getHeight()-1)/2.0;
        //współrzędne środka
        double[][] corners=new double[4][2];
        corners[0][0]= 0;
        corners[0][1]= 0;
        corners[1][0]= getWidth()-1;
        corners[1][1]= 0;
        corners[2][0]= 0;
        corners[2][1]= getHeight()-1;
        corners[3][0]= getWidth()-1;
        corners[3][1]= getHeight()-1;
        //min i max obu współrzędnych obrazu
        double xMin=MAX_VALUE;
        double yMin=MAX_VALUE;
        double xMax=MIN_VALUE;
        double yMax=MIN_VALUE;
        for(int i=0;i<4;i++){
            double x=corners[i][0];
            double y=corners[i][1];
            corners[i][0]=round(xCPoint+(x-xCPoint)*cos(angle)-(y-yCPoint)*sin(angle));
            corners[i][1]=round(yCPoint+(x-xCPoint)*sin(angle)+(y-yCPoint)*cos(angle));
            xMin=min(xMin,corners[i][0]);
            xMax=max(xMax,corners[i][0]);
            yMin=min(yMin,corners[i][1]);
            yMax=max(yMax,corners[i][1]);
        }
        //nowe wymiary obrazu
        short nWidth=(short) round(xMax-xMin+1);
        short nHeight=(short) round(yMax-yMin+1);
        //edycja wartości w headerze
        header.positionRight=(short) (header.positionLeft+nWidth-1);
        header.positionBottom=(short) (header.positionTop+nHeight-1);
        header.bytesPerLine=(short) ceil(header.getWidth()*(header.bitsPerPixel/8));
        //odwracamy kąt obrotu i pobieramy wartości nowego obrazu zgodne z obrotem o ten kąt
        angle=-angle;
        //środek nowego obrazu
        double xRCPoint=(getWidth()-1)/2.0;
        double yRCPoint=(getHeight()-1)/2.0;
        //obliczanie różnicy między środkami obrazów
        double xDiff=xRCPoint-xCPoint;
        double yDiff=yRCPoint-yCPoint;
        ArrayList<double[]> rotatedData=new ArrayList<>();
        if(pictureType==pictureType.grey){
            //tworzenie listy wartości obrazu 
            for(int i=0;i<getHeight();i++)
                rotatedData.add(new double[getWidth()]);
            //wypełnianie listy
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    //nowe współrzędne obrazu po translacji współrzędne starego obrazu
                    int x=(int) round(xRCPoint+(j-xRCPoint)*cos(angle)-(i-yRCPoint)*sin(angle) -xDiff);
                    int y=(int) round(yRCPoint+(j-xRCPoint)*sin(angle)+(i-yRCPoint)*cos(angle) -yDiff);
                    if(x>=0 && x<data.get(0).length && y>=0 && y<data.size())
                        rotatedData.get(i)[j]=
                                data.get(y)[x];
                    else rotatedData.get(i)[j]= 0;
                }
            }
        }
        else if(pictureType==pictureType.rgb){
            //tworzenie listy wartości obrazu 
            for(int i=0;i<getHeight();i++)
                rotatedData.add(new double[getWidth()*3]);
            //wypełnianie listy
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    for(int k=0;k<3;k++){
                        //nowe współrzędne obrazu po translacji współrzędne starego obrazu
                        int x=(int) round(xRCPoint+(j-xRCPoint)*cos(angle)-(i-yRCPoint)*sin(angle) -xDiff);
                        int y=(int) round(yRCPoint+(j-xRCPoint)*sin(angle)+(i-yRCPoint)*cos(angle) -yDiff);
                        if(x>=0 && x<data.get(0).length/3 && y>=0 && y<data.size())
                            rotatedData.get(i)[j+k*getWidth()]=
                                    data.get(y)[x+k*data.get(0).length/3];
                        else rotatedData.get(i)[j+k*getWidth()]= 0;
                    }
                }
            }
        }
        data=rotatedData;
    }
    
    public void makeHistogram(){
        if(pictureType==pictureType.rgb)
            histogram=new int[3][256];
        else histogram=new int[1][256];
        for(int k=0;k<histogram.length;k++)
            for(int i=0;i<256;i++)
                histogram[k][i]=0;
            for(int i=0;i<data.size();i++)
                for(int j=0;j<data.get(0).length/histogram.length;j++)
                    for(int k=0;k<histogram.length;k++)
                        histogram[k][(int)(data.get(i)
                            [j+k*data.get(0).length/histogram.length])]++;
    }
    
    public void stretchHistogram(){
        double[] min=new double[histogram.length];
        double[] max=new double[histogram.length];
        for(int k=0;k<histogram.length;k++)
            for(int i=0;i<256;i++)
                if(histogram[k][i]!=0){
                    min[k]=i;
                    break;
                }
        for(int k=0;k<histogram.length;k++)
            for(int i=255;i>=0;i--)
                if(histogram[k][i]!=0){
                    max[k]=i;
                    break;
                }
        for(int k=0;k<histogram.length;k++){
            double value=255.0/(max[k]-min[k]);
            for(int i=0;i<data.size();i++)
                for(int j=0;j<data.get(0).length/histogram.length;j++){
                    double val=(data.get(i)[j+k*data.get(i).length/histogram.length])-min[k];
                    data.get(i)[j+k*data.get(i).length/histogram.length]=value*val;
                }
        } 
    }
    
    public void localThresholding(int neighbours,double param){ //metodą White'a i Rohrera
        ArrayList<double[]> thresholdedData=new ArrayList<>();
        for(int i=0;i<data.size();i++)
            thresholdedData.add(new double[data.get(0).length]);
        if(pictureType==pictureType.rgb){
            for(int k=0;k<3;k++){
                for(int i=0;i<data.size();i++)
                    for(int j=0;j<data.get(0).length/3;j++){
                        double avg=0;
                        int count=0;
                        for(int y=-neighbours;y<=neighbours;y++)
                            for(int x=-neighbours;x<=neighbours;x++){
                                if(i+y>=0 && i+y<data.size() && j+x>=0 && j+x<data.get(0).length/3){
                                    avg+=(data.get(i+y)[j+x+k*data.get(0).length/3]);
                                    count++;
                                }
                            }
                        avg/=count;
                        thresholdedData.get(i)[j+k*data.get(0).length/3]=
                            (data.get(i)[j+k*data.get(0).length/3]>=(avg/param))?255:0;
                 }
           }
        }
        else if(pictureType==pictureType.grey){
            for(int i=0;i<data.size();i++)
                for(int j=0;j<data.get(0).length;j++){
                    double avg=0;
                    int count=0;
                    for(int y=-neighbours;y<=neighbours;y++)
                        for(int x=-neighbours;x<=neighbours;x++){
                            if(i+y>=0 && i+y<data.size() && j+x>=0 && j+x<data.get(0).length){
                                avg+=data.get(i+y)[j+x];
                                count++;
                            }
                        }
                    avg/=count;
                    thresholdedData.get(i)[j]=
                        (data.get(i)[j]>=(avg/param))?255:0;
                }
        }
        data=thresholdedData;
    }
  
    public void globalThresholding(){
        if(pictureType==pictureType.rgb){
            int T=100;
            int nT=100;
            do {
                T=nT;
                for(int k=0;k<3;k++){
                    int lower = 0, countLower = 0, higher = 0, countHigher = 0;
                    for(int i = 0; i <= nT; i++) {
                        lower +=(histogram[k][i] * i);
                        countLower +=histogram[k][i];
                    }
                    lower=(countLower!=0)?lower/countLower:0;
                    for(int i = (nT + 1); i < 256; i++) {
                        higher = higher + (histogram[k][i] * i);
                        countHigher = countHigher + histogram[k][i];
                    }
                    higher=(countHigher!=0)?higher/countHigher:0;
                    nT=(lower + higher)/2;
                }
            } while(T != nT);
            // Progowanie
            for(int i = 0; i < data.size(); i++) {
                for(int j = 0; j < data.get(i).length; j++) {
                    for(int k=0;k<3;k++)
                        data.get(i)[j] = (data.get(i)[j]>=T)?255:0;
                }
            }
        }
        else if(pictureType==pictureType.grey){
            int T=100;
            int nT=T;
            do {
                T=nT;
                int black = 0, countBlack = 0, white = 0, countWhite = 0;
                for(int i = 0; i <= nT; i++) {
                    black +=(histogram[0][i] * i);
                    countBlack +=histogram[0][i];
                }
                black=(countBlack!=0)?black/countBlack:0;
                for(int i = (nT + 1); i < 256; i++) {
                    white = white + (histogram[0][i] * i);
                    countWhite = countWhite + histogram[0][i];
                }
                white=(countWhite!=0)?white/countWhite:0;
                nT =(black + white)/2;
            } while(T != nT);
            // Progowanie
            for(int i = 0; i < data.size(); i++) {
                for(int j = 0; j < data.get(i).length; j++) {
                    data.get(i)[j] = (data.get(i)[j]>=T)?255:0;
                }
            }
        }
    }
    
    public void thresholdWithParams(int[] params){
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(0).length;j++){
                double value=data.get(i)[j];
                int prog=0;
                for(prog=0;prog<params.length;prog++){
                    if(params[prog]>value)
                        break;
                }
                if(prog==0)
                    data.get(i)[j]=0;
                else if(prog>=params.length)
                    data.get(i)[j]=255;
                else data.get(i)[j]= params[prog];
            }
    }
    public boolean isBinaryFile(){
        for(int i=0;i<histogram.length;i++){
            for(int j=1;j<histogram[i].length-1;j++){
                if(histogram[i][j]>0)
                    return false;
            }
        }
        return true;
    }
    
    public void erode(int range){
        boolean binaryFile=isBinaryFile();
        ArrayList<double[]> erodedData=new ArrayList<>();
        for(int i=0;i<data.size();i++)
            erodedData.add(new double[data.get(0).length]);
        if(binaryFile){
            if(pictureType==pictureType.rgb){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length/3;j++){
                        double value=0;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length/3){
                                    for(int k=0;k<3;k++){
                                        if(data.get(i+y)[j+x+k*data.get(0).length/3]==255)
                                            value=255; 
                                    }
                                }
                            }
                        }
                        for(int k=0;k<3;k++)
                            erodedData.get(i)[j+k*data.get(0).length/3]= value;
                    }
                }
            }
            else if(pictureType==pictureType.grey){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length;j++){
                        int value=0;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length){
                                    if(data.get(i+y)[j+x]==255)
                                        value=255;
                                }
                            }
                        }
                        erodedData.get(i)[j]= value;
                    }
                }
            }  
        }
        else{ //obraz szary
            if(pictureType==pictureType.grey){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length;j++){
                        double value=0;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length){
                                    value=max(value,data.get(i+y)[j+x]);
                                }
                            }
                        }
                        erodedData.get(i)[j]= value;
                    }
                }
            }
            else return;
        }
        data=erodedData;
    }
    
    public void dilate(int range){
        boolean binaryFile=isBinaryFile();
        ArrayList<double[]> dilatedData=new ArrayList<>();
        for(int i=0;i<data.size();i++)
            dilatedData.add(new double[data.get(0).length]);
        if(binaryFile){ //dowolny binarny
            if(pictureType==pictureType.rgb){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length/3;j++){
                        double value=255;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length/3){
                                    for(int k=0;k<3;k++){
                                        if(data.get(i+y)[j+x+k*data.get(0).length/3]==0)
                                            value=0;
                                    }
                                }
                            }
                        }
                        for(int k=0;k<3;k++)
                            dilatedData.get(i)[j+k*data.get(0).length/3]= value;
                    }
                }
            }
            else if(pictureType==pictureType.grey){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length;j++){
                        double value=255;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length){
                                    if(data.get(i+y)[j+x]==0)
                                        value=0;
                                }
                            }
                        }
                        dilatedData.get(i)[j]=(byte) value;
                    }
                }
            } 
        }
        else{ //obraz szary
            if(pictureType==pictureType.grey){
                for(int i=0;i<data.size();i++){
                    for(int j=0;j<data.get(0).length;j++){
                        double value=255;
                        for(int y=-range;y<=range;y++){
                            for(int x=-range;x<=range;x++){
                                if(i+y>=0 && j+x>=0 && i+y<data.size() && j+x<data.get(0).length){
                                    value=min(value,data.get(i+y)[j+x]);
                                }
                            }
                        }
                        dilatedData.get(i)[j]= value;
                    }
                }
            }
            else return;
        }
        data=dilatedData;
    }
    
    public void binaryOpen(int n){
            erode(n);
            dilate(n);
    }
    
    public void binaryClose(int n){
            dilate(n);
            erode(n);
    }
    
    public void linearLowPassFilter(int[][] square){  //filtorwanie dolnoprzepustowe, dzielenie przez sumę wag
        int squareSize=square.length/2;
        ArrayList<double[]> filteredData=new ArrayList<>();
        for(int i=0;i<getHeight();i++)
            filteredData.add(new double[data.get(0).length]);
        if(pictureType==pictureType.grey){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    double value=0;
                    int weightCount=0;
                    for(int y=-squareSize;y<=squareSize;y++){
                        for(int x=-squareSize;x<=squareSize;x++){
                            if(i+y>=0 && i+y<getHeight() && j+x>=0 && j+x<getWidth()){
                                weightCount+=square[y+squareSize][x+squareSize];
                                value+=data.get(i+y)[j+x]*square[y+squareSize][x+squareSize];
                            }
                        }
                    }
                    if(weightCount!=0)  //jeśli dzi
                        value/=weightCount;
                    filteredData.get(i)[j]= value;
                }
            }
        }
        else if(pictureType==pictureType.rgb){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    for(int k=0;k<3;k++){
                        double value=0;
                        int weightCount=0;
                        for(int y=-squareSize;y<=squareSize;y++){
                            for(int x=-squareSize;x<=squareSize;x++){
                                if(i+y>=0 && i+y<getHeight() && j+x>=0 && j+x<getWidth()){
                                    weightCount+=square[y+squareSize][x+squareSize];
                                    value+=data.get(i+y)[k*getWidth()+j+x]
                                            *square[y+squareSize][x+squareSize];
                                }
                            }
                        }
                        if(weightCount!=0)  //jeśli dzi
                            value/=weightCount;
                        filteredData.get(i)[k*getWidth()+j]= value;
                    }
                }
            }
        }
        data=filteredData;
    }
    
    public void linearHighPassFilter(int[][] square){  //filtorwanie górnoprzepustowe, tylko normalizacja do przedziału
        int squareSize=square.length/2;
        ArrayList<double[]> filteredData=new ArrayList<>();
        for(int i=0;i<getHeight();i++)
            filteredData.add(new double[data.get(0).length]);
        if(pictureType==pictureType.grey){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    double value=0;
                    for(int y=-squareSize;y<=squareSize;y++){
                        for(int x=-squareSize;x<=squareSize;x++){
                            if(i+y>=0 && i+y<getHeight() && j+x>=0 && j+x<getWidth())
                                value+=data.get(i+y)[j+x]*square[y+squareSize][x+squareSize];
                            else if(square[y+squareSize][x+squareSize]<0) 
                                    value+=255*square[y+squareSize][x+squareSize];
                        }
                    }
                    filteredData.get(i)[j]=value;
                }
            }
        }
        else if(pictureType==pictureType.rgb){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    for(int k=0;k<3;k++){
                        double value=0;
                        for(int y=-squareSize;y<=squareSize;y++){
                            for(int x=-squareSize;x<=squareSize;x++){
                                if(i+y>=0 && i+y<getHeight() && j+x>=0 && j+x<getWidth())
                                    value+=data.get(i+y)[j+x+k*getWidth()]
                                            *square[y+squareSize][x+squareSize];
                                else if(square[y+squareSize][x+squareSize]<0) 
                                    value+=255*square[y+squareSize][x+squareSize];
                            }
                        }
                        filteredData.get(i)[j+k*getWidth()]=value;
                    }
                }
            }
        }
        data=filteredData;
    }     
    
    public void statisticalFilter(int maskLength,String filterType){   //filtr statystyczny, wybierana wartość z otoczenia
        ArrayList<double[]> filteredData=new ArrayList<>();
        for(int i=0;i<getHeight();i++)
            filteredData.add(new double[data.get(0).length]);
        if(pictureType==pictureType.grey){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    double[] values=new double[(maskLength*2+1)*(maskLength*2+1)];
                    for(int n=0;n<values.length;n++)
                        values[n]=MAX_VALUE;
                    int it=0;
                    for(int y=-maskLength;y<=maskLength;y++){
                        for(int x=-maskLength;x<=maskLength;x++){
                            if(i+y>=0 && i+y<getHeight() && j+x>=0 && j+x<getWidth())
                                values[it++]+=data.get(i+y)[j+x];
                        }
                    }
                    sort(values);
                    if(filterType.equals("exMin"))
                        filteredData.get(i)[j]=values[0];
                    else if(filterType.equals("exMax"))
                        filteredData.get(i)[j]=values[it-1];
                    else if(filterType.equals("mediana"))
                        filteredData.get(i)[j] =values[it/2];
                }
            }
        }
        else if(pictureType==pictureType.rgb){
            for(int i=0;i<getHeight();i++){
                for(int j=0;j<getWidth();j++){
                    for(int k=0;k<3;k++){
                        double[] values=new double[(maskLength*2+1)*(maskLength*2+1)];
                        for(int n=0;n<values.length;n++)
                            values[n]=MAX_VALUE;
                        int it=0;
                        // Średnia wartości pikseli w oknie
                        for(int y = -maskLength; y <= maskLength; y++)
                            for(int x = -maskLength; x <= maskLength; x++)
                                if(i+y >= 0 && i+y < getHeight() && j+x >= 0 && j+x < getWidth())
                                    values[it++] =data.get(i+y)[j+x+k*getWidth()];
                        sort(values);
                        if(filterType.equals("exMin"))
                            filteredData.get(i)[j+k*getWidth()]=values[0];
                        else if(filterType.equals("exMax"))
                            filteredData.get(i)[j+k*getWidth()]=values[it-1];
                        else if(filterType.equals("mediana"))
                            filteredData.get(i)[j+k*getWidth()] =values[it/2];
                    }
                }
            }
        }
        data=filteredData;
    }
    
    public void printHistogram(String filename){
        try{
            PrintWriter writer=new PrintWriter(filename);
            for(int i=0;i<256;i++){
                writer.print(i);
                for(int j=0;j<histogram.length;j++)
                    writer.print(" "+histogram[j][i]);
                writer.println();
            }
            writer.close();
        } catch(FileNotFoundException ex){
            Logger.getLogger(PCXPicture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean notInRange(ArrayList<double[]> toCheck){
        double max=MIN_VALUE;
        double min=MAX_VALUE;
        for(int i=0;i<toCheck.size();i++)
            for(int j=0;j<toCheck.get(0).length;j++)
                if(toCheck.get(i)[j]<0 || toCheck.get(i)[j]>255)
                    return true;
        return false;
    }
    
    public ArrayList<double[]> getFitInRange(){
        ArrayList<double[]> fitting=new ArrayList<>();
        for(int i=0;i<data.size();i++){
            double[] row=new double[data.get(i).length];
            fitting.add(row);
        }
        double max=MIN_VALUE;
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(i).length;j++)
                max=max(max,data.get(i)[j]);
        double cal=255/max;
        for(int i=0;i<data.size();i++)
            for(int j=0;j<data.get(i).length;j++)
                fitting.get(i)[j]=floor(cal*data.get(i)[j]);
        return fitting;
    }
    
    public void makeAFile(String outputFileName){
        if(notInRange(data)){
            data=getFitInRange();
        }
        try {//tworzenie obrazu po operacji
            FileOutputStream pictureOutput=new FileOutputStream(new File(outputFileName+fileName));
            pictureOutput.write(header.toBytes());
            encode(header.getScanLineLength(),dataToByte(data),pictureOutput);
            if(colorTableType==ColorTableType.VGA) 
                pictureOutput.write(0xC0);
            if(colorPalette!=null)
                pictureOutput.write(colorPalette);
        } catch (IOException e) {
            System.err.print("Wystąpił problem: "+e.getMessage());
            System.exit(1);
        }
        //wykonanie normalizacji
        normalize();
        //wypisanie obrazu po normalizacji
        try {
            FileOutputStream pictureOutput=new FileOutputStream(new File(outputFileName+"Normalized"+fileName));
            pictureOutput.write(header.toBytes());
            encode(header.getScanLineLength(),dataToByte(data),pictureOutput);
            if( colorTableType==ColorTableType.VGA) 
                pictureOutput.write(0xC0);
            if(colorPalette!=null)
                pictureOutput.write(colorPalette);
        } catch (IOException e) {
            System.err.print("Wystąpił problem: "+e.getMessage());
            System.exit(1);
        }
    }

}
