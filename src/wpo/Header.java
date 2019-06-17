/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wpo;

/**
 * klasa reprezentująca nagłówek pliku pcx
 * @author Jakub
 */
public class Header {
    byte manufacturer; /* PCX Id Number (Always 0x0A) */
    byte version; /* Version Number */
    byte encoding; /* Encoding Format */
    byte bitsPerPixel; /* Bits per Pixel */
    short positionLeft; /* Left of image */
    short positionTop; /* Top of Image */
    short positionRight; /* Right of Image*/
    short positionBottom; /* Bottom of image */
    short hDPI; /* Horizontal Resolution */
    short vDPI; /* Vertical Resolution */
    byte[] colorMap=new byte[48]; /* 16-Color EGA Palette */
    byte reservedZero; /* Reserved (Always 0) */
    byte nPlanes; /* Number of Bit Planes */
    short bytesPerLine; /* Bytes per Scan-line */
    short paletteInfo; /* Palette Type */
    short hScreenSize; /* Horizontal Screen Size */
    short vScreenSize; /* Vertical Screen Size */
    byte[] filler=new byte[54]; /* Reserved (Always 0) */
    
    /**
     * konstruktor klasy
     * @param values
     */
    public Header(byte[] values){
        manufacturer=values[0];
        version=values[1];
        encoding=values[2];
        bitsPerPixel=values[3];
        positionLeft= (short) (values[4] & 0xff | (values[5] & 0xff) <<8);
        positionTop= (short) (values[6] & 0xff | (values[7] & 0xff) <<8);
        positionRight= (short) (values[8] & 0xff | (values[9] & 0xff) <<8);
        positionBottom= (short) (values[10] & 0xff | (values[11] & 0xff) <<8);
        hDPI=(short)(values[12] & 0xff | (values[13] & 0xff) <<8);
        vDPI=(short)(values[14] & 0xff | (values[15] & 0xff) <<8);
        for(int i=0;i<48;i++)
            colorMap[i]=values[16+i];
        reservedZero=values[64];
        nPlanes=values[65];
        bytesPerLine=(short)(values[66] & 0xff | (values[67] & 0xff) <<8);
        paletteInfo=(short)(values[68] & 0xff | (values[69] & 0xff) <<8);
        hScreenSize=(short)(values[70] & 0xff | (values[71] & 0xff) <<8);
        vScreenSize=(short)(values[72] & 0xff | (values[73] & 0xff) <<8);
        for(int i=0;i<54;i++)
            filler[i]=values[74+i];
    }
    /**
     * metoda służąca sprawdzeniu poprawności wartości krytycznych nagłówka
     * @return
     */
    public Boolean checkHeader(){
        if(manufacturer!=10) return false;
        if(version<0 || version >5 || version==1) return false;
        if(encoding!=1) return false;
        if((reservedZero & 0xff)!=0)return false;
        return true;
    }
    
    public int getHeight(){
        return positionBottom-positionTop+1;
    }
    
    public int getWidth(){
        return positionRight-positionLeft+1;
    }
    
    public int getScanLineLength(){
        return nPlanes*bytesPerLine;
    }
    
    public byte[] toBytes(){ /*konwersja headera do byte[] */
        byte[] bytes=new byte[128];
        bytes[0]=manufacturer;
        bytes[1]=version;
        bytes[2]=encoding;
        bytes[3]=bitsPerPixel;
        bytes[4]=(byte) (positionLeft % 256);
        bytes[5]=(byte) (positionLeft / 256);
        bytes[6]=(byte) (positionTop % 256);
        bytes[7]=(byte) (positionTop / 256);
        bytes[8]=(byte) (positionRight % 256);
        bytes[9]=(byte) (positionRight / 256);
        bytes[10]=(byte) (positionBottom % 256);
        bytes[11]=(byte) (positionBottom / 256);
        bytes[12]=(byte) (hDPI % 256);
        bytes[13]=(byte) (hDPI / 256);
        bytes[14]=(byte) (vDPI % 256);
        bytes[15]=(byte) (vDPI / 256);
        System.arraycopy(colorMap, 0, bytes, 16, 48);
        bytes[64]=reservedZero;
        bytes[65]=nPlanes;
        bytes[66]=(byte) (bytesPerLine % 256);
        bytes[67]=(byte) (bytesPerLine / 256);
        bytes[68]=(byte) (paletteInfo % 256);
        bytes[69]=(byte) (paletteInfo / 256);
        bytes[70]=(byte) (hScreenSize % 256);
        bytes[71]=(byte) (hScreenSize / 256);
        bytes[72]=(byte) (vScreenSize % 256);
        bytes[73]=(byte) (vScreenSize / 256);
        System.arraycopy(filler, 0, bytes, 74, 54);
        return bytes;
    }
}
