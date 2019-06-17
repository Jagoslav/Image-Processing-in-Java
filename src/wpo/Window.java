/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wpo;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import static java.lang.Integer.min;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

/**
 *
 * @author Jakub
 */
public class Window extends JFrame implements ActionListener{

    enum State{
        start,  //okno startowe
        actions,    //działy zadań
    }
    State state=State.start;    //stan aplikacji
    int kategoria=0;
    PictureType pictureType=null;   //typ wybranego obrazka
    String path="images";   //ścieżka do folderu
    String picturePath; //pełna ścieżka do pliku
    String pictureName; //nazwa pliku (by utworzyć nowy wynikowy o nazwie używającej oryginalnej
    PCXPicture picture;
    //przyciski
    JButton resetButton;    //przywrócenie domyślnej ścieżki
    JButton changePathButton;   //zmiana ścieżki 
    JButton acceptButton;   //przycisk "dalej"
    JButton backButton; //przycisk cofania
    JButton helpButton; //przycisk pomocy
    JButton[] choiceButtons;    //przyciski zadań
    JTextField pathName;    //pełna ścieżka do pliku
    JList choices;    //lista wyników do wyboru
    String chosenOperation=null;
    
    public Window(){
        setTitle("Wprowadzenie do Przetwarzania Obrazów - Projekt");
        setSize(480,240);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
        resetButton=new JButton("↺");
        resetButton.addActionListener(this);
        resetButton.setToolTipText("Przywróć domyślną");
        changePathButton=new JButton("Zmień ścieżkę");
        changePathButton.addActionListener(this);
        pathName=new JTextField(path);
        if(pathName!=null)
            changePathButton.setToolTipText("Zmień ścieżkę na aktualnie ustawioną");
        acceptButton=new JButton("Dalej");
        acceptButton.addActionListener(this);
        acceptButton.setToolTipText("Akceptuj i przejdź dalej");
        backButton=new JButton("Wróć");
        backButton.addActionListener(this);
        backButton.setToolTipText("Cofnij do poprzedniego okna");
        helpButton=new JButton(UIManager.getIcon("OptionPane.questionIcon"));
        helpButton.setText("Pomoc");
        helpButton.addActionListener(this);
        helpButton.setToolTipText("Wyświetl pomoc dla akktywnego okna");
        choiceButtons=new JButton[8];
        for(int i=0;i<choiceButtons.length;i++){
            choiceButtons[i]=new JButton();
            choiceButtons[i].addActionListener(this);
        }
        picturePath=null;
        initState();
    }
    public void initState(){
        getContentPane().removeAll();
        JPanel mainPanel=new JPanel();
        JPanel buttonPanel=new JPanel();
        switch(state){
            case start:
                picture=null;
                mainPanel.setLayout(new BorderLayout());
                JPanel listPanel=new JPanel();
                JPanel pathNamePanel=new JPanel(new BorderLayout());
                //opcje ścieżki
                pathNamePanel.add(resetButton,BorderLayout.LINE_START);
                pathNamePanel.add(pathName,BorderLayout.CENTER);
                pathNamePanel.add(changePathButton,BorderLayout.LINE_END);
                mainPanel.add(pathNamePanel,BorderLayout.PAGE_START);
                choices=getListOfPCXFiles(path);
                if(choices==null){
                    listPanel.setLayout(new GridBagLayout());
                    JLabel text=new JLabel("Folder nie istnieje");
                    listPanel.add(text);
                }
                else if(choices.getModel().getSize()==0){
                    listPanel.setLayout(new GridBagLayout());
                    JLabel text=new JLabel("Brak plików");
                    listPanel.add(text);
                }
                else{
                    listPanel.setLayout(new BorderLayout());
                    listPanel.add(new JScrollPane(choices));
                }
                mainPanel.add(listPanel,BorderLayout.CENTER);
                //przyciski
                buttonPanel.setLayout(new GridLayout(2,1));
                buttonPanel.add(acceptButton);
                buttonPanel.add(helpButton);
                mainPanel.add(buttonPanel,BorderLayout.LINE_END);
                break;
            case actions:
                mainPanel.setLayout(new BorderLayout());
                JPanel choicePanel=new JPanel(new BorderLayout());
                JPanel optionPanel=new  JPanel(new GridLayout(1,2));
                optionPanel.add(backButton);
                optionPanel.add(acceptButton);
                optionPanel.add(helpButton);
                JTextArea filePathName=new JTextArea(picturePath);
                filePathName.setEditable(false);
                choicePanel.add(filePathName,BorderLayout.PAGE_START);
                mainPanel.add(optionPanel,BorderLayout.PAGE_END);
                String[] choiceOptions;
                switch(kategoria){
                    case 1: //arytmetyczne
                        choiceOptions=new String[]{
                            "Sumowanie z liczbą",
                            "Sumowanie z obrazem",
                            "Mnożenie przez Stałą",
                            "Mnożenie przez obraz",
                            "Mieszanie ze współczynnikiem",
                            "Potęgowanie",
                            "Dzielenie przez stałą",
                            "Dzielenie obrazem",
                            "Pierwiastkowanie",
                            "Logarytmowanie"};
                        choices=new JList(choiceOptions);
                        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        choicePanel.add(new JScrollPane(choices));
                        break;
                    case 2: //geometryczne
                        choiceOptions=new String[]{
                            "Przemieszczanie o wektor",
                            "Skalowanie jednorodne",
                            "Skalowanie niejednorodne",
                            "Obracanie obrazu",
                            "Odbicie symetryczne względem krawędzi",
                            "Odbicie fragmentu obrazu",
                            "Wycinanie fragmentu",
                            "Kopiowanie fragmentu"};
                        choices=new JList(choiceOptions);
                        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        choicePanel.add(new JScrollPane(choices));
                        break;
                    case 3: //na histogramie
                        if(picture==null)
                            picture=new PCXPicture(picturePath,pictureName);
                        if(picture.getPictureType()==PictureType.grey)
                            choiceOptions=new String[]{
                                "Obliczanie histogramu",
                                "Rozciąganie histogramu",
                                "Progowanie lokalne",
                                "Progowanie globalne"};
                        else choiceOptions=new String[]{
                                "Obliczanie histogramu",
                                "Rozciąganie histogramu",
                                "Progowanie lokalne",
                                "Progowanie globalne",
                                "Progowanie 1 progowe",
                                "Progowanie n progowe"};
                        choices=new JList(choiceOptions);
                        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        choicePanel.add(new JScrollPane(choices));
                        break;
                    case 4: //morfologiczne
                        if(picture==null)
                            picture=new PCXPicture(picturePath,pictureName);
                        if(picture.getPictureType()==PictureType.rgb && !picture.isBinaryFile())
                            choiceOptions=new String[0];
                        else choiceOptions=new String[]{
                                "Okrawanie",
                                "Dylatacja",
                                "Zamykanie",
                                "Otwieranie"};
                        choices=new JList(choiceOptions);
                        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        choicePanel.add(new JScrollPane(choices));
                        break;
                    case 5:
                        choiceOptions=new String[]{
                            "Dolnoprzepustowe",
                            "Górnoprzepustowe",
                            "Gradientowe",
                            "Medianowe",
                            "Ekstremalne"};
                        
                        choices=new JList(choiceOptions);
                        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        choicePanel.add(new JScrollPane(choices));
                        break;
                    default:
                        JPanel tempPanel=new JPanel(new GridBagLayout());
                        tempPanel.add(new JLabel("Nie wybrano kategorii"));
                        choicePanel.add(tempPanel);
                        break;
                }
                mainPanel.add(choicePanel);
                buttonPanel.setLayout(new GridLayout(5,1));
                choiceButtons[0].setToolTipText("Wyświetl listę dostępnych operacji arytmetycznych");
                choiceButtons[0].setText("Operacje arytmetyczne");          
                choiceButtons[1].setToolTipText("Wyświetl listę dostępnych operacji geometrycznych");      
                choiceButtons[1].setText("Operacje geometryczne");             
                choiceButtons[2].setToolTipText("Wyświetl listę dostępnych operacji na histogramie pliku");   
                choiceButtons[2].setText("Operacje na histogramie");    
                choiceButtons[3].setToolTipText("Wyświetl listę dostępnych operacji mofrologicznych");            
                choiceButtons[3].setText("Operacje morfologiczne");     
                choiceButtons[4].setToolTipText("Wyświetl listę dostępnych filtrów");           
                choiceButtons[4].setText("Filtrowanie obrazów");
                for(int i=0;i<5;i++)
                    buttonPanel.add(choiceButtons[i]);
                mainPanel.add(buttonPanel,BorderLayout.LINE_END);
                break;
            
            default:
                System.exit(1);
                break;
        }
        add(mainPanel);
        getContentPane().validate();
    }
    public JList getListOfPCXFiles(String path){
        JList list=null;
        File folder = new File(path);
        if(folder.exists() && folder.isDirectory()){
            File[] listOfFiles = folder.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                             return name.endsWith(".pcx"); }
                    });
            ArrayList<String> fileNames=new ArrayList<>();
            if(listOfFiles==null)
                list= new JList();
            else{
                for(int i=0;i<listOfFiles.length;i++)
                    fileNames.add(listOfFiles[i].getName());
                String[] tab=new String[fileNames.size()];
                tab=fileNames.toArray(tab);
                list=new JList(tab);
            }
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            return list;
        }
        else return null;
    }
    
    public String getPicturePath(){
        String tempPath=null;
        Boolean error=false;
        String errorMessage="";
        do{
            tempPath=JOptionPane.showInputDialog("Podaj ścieżkę do pliku");
            if(tempPath==null)
                return null;
            File file=new File(tempPath);
            if(!file.exists()){
                errorMessage="Plik nie istnieje!\n";
                error=true;
            }
            else{
                error=false;
                errorMessage="";
            }
        }while(error);
        return tempPath;
    }
    public double[] getDouble(String question,double min,double max){
        double[] value=new double[1];
        Boolean error=false;
        String errorMessage="";
        do{
            String stringInput=JOptionPane.showInputDialog(errorMessage+question
                    +"\nWartość musi należeć do przedziału ["+min+";"+max+"]");
            if(stringInput==null)
                return null;
            try{
                value[0]=Double.parseDouble(stringInput);
                if(value[0]>max || value[0]<min){
                    errorMessage="Wartość po za przedziałem\n";
                    error=true;
                }
                else{
                    errorMessage="";
                    error=false;
                }
            }catch(NumberFormatException ex){
                    errorMessage="Zły format danych\n";
                error=true;
            }
        }while(error);
        return value;
    }
    public int[] getInt(String question,int min,int max){
        int[] value=new int[1];
        Boolean error=false;
        String errorMessage="";
        do{
            String stringInput=JOptionPane.showInputDialog(errorMessage+question
                    +"\nWartość musi należeć do przedziału ["+min+";"+max+"]");
            if(stringInput==null)
                return null;
            try{
                value[0]=Integer.parseInt(stringInput);
                if(value[0]>max||value[0]<min){
                    errorMessage="Wartość spoza przedziału!\n";
                    error=true;
                }
                else {
                    errorMessage="";
                    error=false;
                }
            }catch(NumberFormatException ex){
                    errorMessage="Zły format danych\n";
                error=true;
            }
        }while(error);
        return value;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        switch(state){
            case start:
                if(e.getSource()==resetButton){
                    path="images";
                    pathName.setText(path);
                }
                else if(e.getSource()==changePathButton){
                    path=pathName.getText();
                }
                else if(e.getSource()==acceptButton){
                    if(choices.getSelectedValue()==null)
                        return;
                    picturePath=path+="\\"+choices.getSelectedValue();
                    pictureName=""+choices.getSelectedValue();
                    state=State.actions;
                    kategoria=0;
                    choices=new JList();
                }
                else if(e.getSource()==helpButton){
                    JOptionPane.showMessageDialog(null,
                            "↺  -   Przywrócenie domyślnej ścieżki do folderu plików\n"+
                            "Pole Tekstowe  -   Relatywna lub bezwzględna ścieżka do folderu plików\n "+
                            "Zmień ścieżkę -   Aktualizuje liste plików na podstawie nowej ścieżki\n "+
                            "Dalej  -   Wybranie zaznaczonego pliku i przejście do kolejnego okna",
                            "Pomoc  -   Okno wyboru pliku",
                            JOptionPane.QUESTION_MESSAGE);
                    
                }
                break;
            case actions:
                if(e.getSource()==backButton){
                    path="images";
                    pathName.setText(path);
                    state=State.start;
                }
                else if(e.getSource()==acceptButton){
                    chosenOperation=(String) choices.getSelectedValue();
                    if(chosenOperation==null)
                        return;
                    else {
                        //sprawdzenie czy istnieje folder dla wyników
                        picture=new PCXPicture(picturePath,pictureName);
                        ///tutaj reakcja na operacje
                        switch(kategoria){
                            case 1: //operacje arytmetyczne
                                if(chosenOperation=="Sumowanie z liczbą"){
                                    int[] value=getInt("Podaj wartość, którą chcesz zsumować z obrazem",1,255);
                                    if(value==null)
                                        return;
                                    picture.addValue(value[0]);
                                    chosenOperation+="("+value[0]+")";
                                }
                                else if(chosenOperation=="Sumowanie z obrazem"){
                                    String tempPicturePath=getPicturePath();
                                    if(tempPicturePath==null)
                                        return;
                                    PCXPicture tempPicture=new PCXPicture(tempPicturePath,"sumowany");
                                    if(picture.getPictureType()!=tempPicture.getPictureType()){
                                        JOptionPane.showMessageDialog(null,
                                            "Obrazy nie są tego samego rodzaju",
                                            "",
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                    tempPicture.resize(picture.getWidth(),picture.getHeight());
                                    picture.addPictureValues(tempPicture.getData());
                                }
                                else if(chosenOperation=="Mnożenie przez Stałą"){
                                    int[] value=getInt("Podaj wartość, przez którą chcesz przemnożyć obraz",1,255);
                                    if(value==null)
                                        return;
                                    picture.multiplyByValue(value[0]);
                                    chosenOperation+="("+value[0]+")";
                                }
                                else if(chosenOperation=="Mnożenie przez obraz"){
                                    String tempPicturePath=getPicturePath();
                                    if(tempPicturePath==null)
                                        return;
                                    PCXPicture tempPicture=new PCXPicture(tempPicturePath,"mnożnik");
                                    if(picture.getPictureType()!=tempPicture.getPictureType()){
                                        JOptionPane.showMessageDialog(null,
                                            "Obrazy nie są tego samego rodzaju",
                                            "",
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                    tempPicture.resize(picture.getWidth(),picture.getHeight());
                                    picture.multiplyByPictureValues(tempPicture.getData());
                                }
                                else if(chosenOperation=="Mieszanie ze współczynnikiem"){
                                    String tempPicturePath=getPicturePath();
                                    if(tempPicturePath==null)
                                        return;
                                    double[] param=getDouble("Podaj wartość współczynnika dla pierwszego obrazu",0,100);
                                    if(param==null)
                                        return;
                                    param[0]/=100;
                                    PCXPicture tempPicture=new PCXPicture(tempPicturePath,"mieszany");
                                    if(picture.getPictureType()!=tempPicture.getPictureType()){
                                        JOptionPane.showMessageDialog(null,
                                            "Obrazy nie są tego samego rodzaju",
                                            "",
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                    tempPicture.resize(picture.getWidth(),picture.getHeight());
                                    picture.mix(tempPicture.getData(),param[0]);
                                    chosenOperation+="("+param[0]+")";
                                }
                                else if(chosenOperation=="Potęgowanie"){
                                    int[] value=getInt("Podaj stopień potęgi, do której chcesz podnieść obraz",1,5);
                                    if(value==null)
                                        return;
                                    picture.raiseToThePower(value[0]);
                                    chosenOperation+="("+value[0]+")";
                                }
                                else if(chosenOperation=="Dzielenie przez stałą"){
                                    int[] value;
                                    do{
                                        value=getInt("Podaj wartość, przez którą chcesz podzielić obraz",1,255);
                                        if(value==null)
                                        return;
                                    }while(value[0]==0);
                                    picture.divideByValue(value[0]);
                                    chosenOperation+="("+value[0]+")";
                                }
                                else if(chosenOperation=="Dzielenie obrazem"){
                                    String tempPicturePath=getPicturePath();
                                    if(tempPicturePath==null)
                                        return;
                                    PCXPicture tempPicture=new PCXPicture(tempPicturePath,"dzielnik");
                                    if(picture.getPictureType()!=tempPicture.getPictureType()){
                                        JOptionPane.showMessageDialog(null,
                                            "Obrazy nie są tego samego rodzaju",
                                            "",
                                            JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                    tempPicture.resize(picture.getWidth(),picture.getHeight());
                                    picture.divideByPictureValues(tempPicture.getData());
                                }
                                else if(chosenOperation=="Pierwiastkowanie"){
                                    int[] value=getInt("Podaj stopień pierwiastka",1,5);
                                    if(value==null)
                                        return;
                                    picture.rootValue(value[0]);
                                    chosenOperation+="("+value[0]+")";
                                }
                                else if(chosenOperation=="Logarytmowanie"){
                                    picture.logarithm();
                                }
                                break;
                            case 2: //operacje geometryczne
                                if(chosenOperation=="Przemieszczanie o wektor"){
                                    int[] moveX=getInt("Podaj przesunięcie względem osi X",-picture.getWidth(),picture.getWidth());
                                    if(moveX==null)
                                        return;
                                    int[] moveY=getInt("Podaj przesunięcie względem osi X",-picture.getHeight(),picture.getHeight());
                                    if(moveX==null)
                                        return;
                                    picture.moveBy(moveX[0],moveY[0]);
                                    chosenOperation+="("+moveX[0]+","+moveY[0]+")";
                                }
                                else if(chosenOperation=="Skalowanie jednorodne"){
                                    double[] scale=getDouble("Podaj wartość współczynnika skalowania",0.1,5);
                                    if(scale==null)
                                        return;
                                    picture.scale(scale[0],scale[0]);
                                    chosenOperation+="("+scale[0]+")";
                                }
                                else if(chosenOperation=="Skalowanie niejednorodne"){
                                    double[] scaleX=getDouble("Podaj wartość współczynnika skalowania współrzędnej X",0.1,5);
                                    if(scaleX==null)
                                        return;
                                    double[] scaleY=getDouble("Podaj wartość współczynnika skalowaniawspółrzędnej Y",0.1,5);
                                    if(scaleY==null)
                                        return;
                                    picture.scale(scaleX[0],scaleY[0]);
                                    chosenOperation+="("+scaleX[0]+","+scaleY[0]+")";
                                }
                                else if(chosenOperation=="Obracanie obrazu"){
                                    double[] angle=getDouble("Podaj wartość kąta(w stopniach) o który chcesz obrócić obraz",-180,180);
                                    if(angle==null)
                                        return;
                                    picture.rotate(angle[0]);
                                    chosenOperation+="("+angle[0]+"°)";
                                }
                                else if(chosenOperation=="Odbicie symetryczne względem krawędzi"){
                                    String[] buttons = { "X", "Y" };
                                    int rc = JOptionPane.showOptionDialog(null, "Według której osi odbić obraz?","",
                                            JOptionPane.YES_NO_OPTION, 0, null, buttons, buttons[1]);
                                    if(rc==-1)
                                        return;
                                    else if(rc==0)
                                        picture.flipAxis('x');
                                    else
                                        picture.flipAxis('y');
                                    chosenOperation+="("+buttons[rc]+")";
                                }
                                else if(chosenOperation=="Odbicie fragmentu obrazu"){
                                    String[] buttons = { "X", "Y" };
                                    int[] shift=null;
                                    int rc = JOptionPane.showOptionDialog(null, "Według której osi odbić obraz?", "",
                                            JOptionPane.YES_NO_OPTION, 0, null, buttons, buttons[1]);
                                    if(rc==-1)
                                        return;
                                    else if(rc==0){
                                        shift=getInt("Podaj przesunięcie względem górnej krawędzi",0,picture.getHeight());
                                        if(shift==null)
                                            return;
                                        picture.flipByShiftedAxis('x',shift[0]);
                                    }
                                    else{
                                        shift=getInt("Podaj przesunięcie względem lewej krawędzi",0,picture.getHeight());
                                        if(shift==null)
                                            return;
                                        picture.flipByShiftedAxis('y',shift[0]);
                                    }
                                    chosenOperation+="(względem "+buttons[rc]+"przesunięcie "+shift[0]+")";
                                }
                                else if(chosenOperation=="Wycinanie fragmentu"){
                                    int[] x=getInt("Podaj współrzędną x lewego górnego wierzchołka obszaru do wycięcia",0,picture.getWidth());
                                    if(x==null)
                                        return;
                                    int[] y=getInt("Podaj współrzędną y lewego górnego wierzchołka obszaru do wycięcia",0,picture.getHeight());
                                    if(y==null)
                                        return;
                                    int[] w=getInt("Podaj szerokość obszaru do wycięcia",1,picture.getWidth()-x[0]);
                                    if(w==null)
                                        return;
                                    int[] h=getInt("Podaj wysokość obszaru do wycięcia",1,picture.getHeight()-y[0]);
                                    if(h==null)
                                        return;
                                    File folder=new File("wyniki");
                                    if(!folder.exists()){
                                        JOptionPane.showMessageDialog(null,
                                                "Utworzono folder: "+folder.getAbsolutePath()+"",
                                                "",
                                                JOptionPane.INFORMATION_MESSAGE);

                                        folder.mkdir();
                                    }
                                    picture.cutFromPicture(x[0], y[0], w[0], h[0],"wyniki\\"+chosenOperation+"-produkt uboczny"+pictureName);
                                }
                                else if(chosenOperation=="Kopiowanie fragmentu"){
                                    int[] x=getInt("Podaj współrzędną x lewego górnego wierzchołka obszaru do skopiowania",0,picture.getWidth());
                                    if(x==null)
                                        return;
                                    int[] y=getInt("Podaj współrzędną y lewego górnego wierzchołka obszaru do skopiowania",0,picture.getHeight());
                                    if(y==null)
                                        return;
                                    int[] w=getInt("Podaj szerokość obszaru do skopiowania",1,picture.getWidth()-x[0]);
                                    if(w==null)
                                        return;
                                    int[] h=getInt("Podaj wysokość obszaru do skopiowania",1,picture.getHeight()-y[0]);
                                    if(h==null)
                                        return;
                                    File folder=new File("wyniki");
                                    if(!folder.exists()){
                                        JOptionPane.showMessageDialog(null,
                                                "Utworzono folder: "+folder.getAbsolutePath()+"",
                                                "",
                                                JOptionPane.INFORMATION_MESSAGE);

                                        folder.mkdir();
                                    }
                                    picture.copyFromPicture(x[0], y[0], w[0], h[0],"wyniki\\"+chosenOperation+"-Produkt uboczny"+pictureName);
                                }
                                break;
                            case 3: //operacje na histogramie
                                if(chosenOperation=="Obliczanie histogramu"){
                                    File folder=new File("wyniki");
                                    if(!folder.exists()){
                                        JOptionPane.showMessageDialog(null,
                                                "Utworzono folder: "+folder.getAbsolutePath()+"",
                                                "",
                                                JOptionPane.INFORMATION_MESSAGE);

                                        folder.mkdir();
                                    }
                                    picture.printHistogram("wyniki\\"+chosenOperation+"-Histogram.txt");
                                    JOptionPane.showMessageDialog(null,
                                            "plik: \""+chosenOperation+"-Histogram.txt"+"\" \nZapisano w folderze : "+"\""+folder.getPath()+"\"");
                                    return;
                                }
                                else if(chosenOperation=="Rozciąganie histogramu"){
                                    picture.stretchHistogram();
                                }
                                else if(chosenOperation=="Progowanie lokalne"){
                                    int value[]=getInt("Podaj zasięg maski progowania",1,min(picture.getHeight(),picture.getWidth()));
                                    if(value==null)
                                        return;
                                    double[] value2=getDouble("Podaj parametr progowania",0.5,20);
                                    if(value2==null)
                                        return;
                                    picture.localThresholding(value[0],value2[0]);
                                    chosenOperation+="(maska"+(value[0]*2+1)+"x"+(value[0]*2+1)+", parametr"+value2[0]+")";
                                }
                                else if(chosenOperation=="Progowanie globalne"){
                                    picture.globalThresholding();
                                }
                                else if(chosenOperation=="Progowanie 1 progowe"){
                                    int value[]=getInt("Podaj wartość progu",1,254);
                                    if(value==null)
                                        return;
                                    picture.thresholdWithParams(value);
                                }
                                else if(chosenOperation=="Progowanie n progowe"){
                                    int value[]=getInt("Podaj ilość progów, rozmieszczone zostaną w równych odstępach",2,20);
                                    if(value==null)
                                        return;
                                    int[] params=new int[value[0]];
                                    for(int i=0;i<params.length;i++)
                                        params[i]=(i+1)*256/(params.length+1) ;
                                    picture.thresholdWithParams(params);
                                    chosenOperation+="("+value[0]+"progów)";
                                }
                                break;
                            case 4://operacje morfologiczne
                                if(chosenOperation=="Okrawanie"){
                                    int value[]=getInt("Podaj stopień erozji",1,20);
                                    if(value==null)
                                        return;
                                    picture.erode(value[0]);
                                    chosenOperation+="("+value[0]+"st.)";
                                }
                                if(chosenOperation=="Dylatacja"){
                                    int value[]=getInt("Podaj stopień dylatacji",1,20);
                                    if(value==null)
                                        return;
                                    picture.dilate(value[0]);
                                    chosenOperation+="("+value[0]+"st.)";
                                }
                                if(chosenOperation=="Zamykanie"){
                                    int value[]=getInt("Podaj stopień zamknięcia",1,20);
                                    if(value==null)
                                        return;
                                    picture.binaryClose(value[0]);
                                    chosenOperation+="("+value[0]+"st.)";
                                }
                                if(chosenOperation=="Otwieranie"){
                                    int value[]=getInt("Podaj stopień otwarcia",1,20);
                                    if(value==null)
                                        return;
                                    picture.binaryOpen(value[0]);
                                    chosenOperation+="("+value[0]+"st.)";
                                }
                                break;
                            case 5: //filtry
                                if(chosenOperation=="Dolnoprzepustowe"){
                                    String[] buttons = { "Uśredniający","Kwadratowy","LP1","LP2","LP3", "Piramidalny",
                                        "Stożkowy","Gauss 1","Gauss 2", "Gauss 3"};
                                    String option=(String) JOptionPane.showInputDialog(null, "Którego filtra dolnoprzepustowego użyć?", "",
                                        JOptionPane.QUESTION_MESSAGE, null,buttons,null);
                                    if(option==null)
                                        return;
                                    int[][]square=null;
                                    if(option.equals("Uśredniający"))
                                        square=new int[][]{ {1,1,1},
                                                            {1,1,1},
                                                            {1,1,1},};
                                    else if(option.equals("Kwadratowy"))
                                        square=new int[][]{ {1,1,1,1,1},
                                                            {1,1,1,1,1},
                                                            {1,1,1,1,1},
                                                            {1,1,1,1,1},
                                                            {1,1,1,1,1}};
                                    else if(option.equals("LP1"))
                                        square=new int[][]{ {1,1,1},
                                                            {1,2,1},
                                                            {1,1,1},};
                                    else if(option.equals("LP2"))
                                        square=new int[][]{ {1,1,1},
                                                            {1,4,1},
                                                            {1,1,1},};
                                    else if(option.equals("LP3"))
                                        square=new int[][]{ {1,1,1},
                                                            {1,12,1},
                                                            {1,1,1},};
                                    else if(option.equals("Piramidalny"))
                                        square=new int[][]{ {1,2,3,2,1},
                                                            {2,4,6,4,2},
                                                            {3,6,9,6,3},
                                                            {2,4,6,4,2},
                                                            {1,2,3,2,1}};
                                    else if(option.equals("Stożkowy"))
                                        square=new int[][]{ {0,0,1,0,0},
                                                            {0,2,2,2,0},
                                                            {1,2,5,2,1},
                                                            {0,2,2,2,0},
                                                            {0,0,1,0,0}};
                                    else if(option.equals("Gauss 1"))
                                        square=new int[][]{ {1,2,1},
                                                            {2,4,2},
                                                            {1,2,1},};
                                    else if(option.equals("Gauss 2"))
                                        square=new int[][]{ {1,1,2,1,1},
                                                            {1,2,4,2,1},
                                                            {2,4,8,4,2},
                                                            {1,2,4,2,1},
                                                            {1,1,2,1,1}};
                                    else if(option.equals("Gauss 3"))
                                        square=new int[][]{ {0,1,2,1,0},
                                                            {1,4,8,4,1},
                                                            {2,8,16,8,2},
                                                            {1,4,8,4,1},
                                                            {0,1,2,1,0}};
                                    chosenOperation+="("+option+")";
                                    picture.linearLowPassFilter(square);
                                }
                                else if(chosenOperation=="Górnoprzepustowe"){
                                    String[] buttons = { "Maska Prewitta", "Maska Robertsa", "Maska Sobela","Usuń Średnią","HP1", "HP2"};
                                    String option=(String) JOptionPane.showInputDialog(null, "Wybierz kierunek filtra", "",
                                    JOptionPane.QUESTION_MESSAGE, null,buttons,null);
                                    if(option==null)
                                        return;
                                    int[][]square=null;
                                    if(option.equals("Maska Prewitta"))
                                        square=new int[][]{ {-1,-1,-1},
                                                            {0,0,0},
                                                            {1,1,1}};
                                    else if(option.equals("Maska Robertsa"))
                                        square=new int[][]{ {0,0,0},
                                                            {0,-1,0},
                                                            {0,0,1}};
                                    else if(option.equals("Maska Sobela"))
                                        square=new int[][]{ {1,2,1},
                                                            {0,0,0},
                                                            {-1,-2,-1}};
                                    else if(option.equals("Usuń Średnią"))
                                        square=new int[][]{ {-1,-1,-1},
                                                            {-1,9,-1},
                                                            {-1,-1,-1}};
                                    else if(option.equals("HP1"))
                                        square=new int[][]{ {0,-1,0},
                                                            {-1,5,-1},
                                                            {0,-1,0}};
                                    else if(option.equals("HP2"))
                                        square=new int[][]{ {1,-2,1},
                                                            {-2,5,-2},
                                                            {1,-2,1}};
                                    chosenOperation+="("+option+")";
                                    picture.linearHighPassFilter(square);
                                }
                                else if(chosenOperation=="Gradientowe"){
                                    int[][] square=null;
                                    String[] buttons = {"Płaskorzeźbowe kierunkowe",
                                        "Wektora kierunkowego VDG"};
                                    String option=(String) JOptionPane.showInputDialog(null, "Wybierz rodzaj gradientu", "",
                                        JOptionPane.QUESTION_MESSAGE, null,buttons,null);
                                    if(option==null)
                                        return;
                                    else if(option.equals("Płaskorzeźbowe kierunkowe")){    //uwypuklające
                                        option=null;
                                        buttons=new String[]{"Wschód","Południowy - Wschód","Południe","Południowy - Zachód",
                                        "Zachód","Północny - Zachód", "Północ","Północny - Wschód"};
                                        option=(String) JOptionPane.showInputDialog(null, "Wybierz kierunek filtra", "",
                                        JOptionPane.QUESTION_MESSAGE, null,buttons,null);
                                        if(option==null)
                                            return;
                                        else if(option.equals("Wschód"))
                                            square=new int[][]{ {-1,0,1},
                                                                {-1,1,1},
                                                                {-1,0,1}};
                                        else if(option.equals("Południowy - Wschód"))
                                            square=new int[][]{ {-1,-1,0},
                                                                {-1,1,1},
                                                                {0,1,1}};
                                        else if(option.equals("Południe"))
                                            square=new int[][]{ {-1,-1,-1},
                                                                {0,1,0},
                                                                {1,1,1}};
                                        else if(option.equals("Południowy - Zachód"))
                                            square=new int[][]{ {0,-1,-1},
                                                                {1,1,-1},
                                                                {1,1,0}};
                                        else if(option.equals("Zachód"))
                                            square=new int[][]{ {1,0,-1},
                                                                {1,1,-1},
                                                                {1,0,-1}};
                                        else if(option.equals("Północny - Zachód"))
                                            square=new int[][]{ {1,1,0},
                                                                {1,1,-1},
                                                                {0,-1,-1}};
                                        else if(option.equals("Północ"))
                                            square=new int[][]{ {1,1,1},
                                                                {0,1,0},
                                                                {-1,-1,-1}};
                                        else if(option.equals("Północny - Wschód"))
                                            square=new int[][]{ {0,1,1},
                                                                {-1,1,1},
                                                                {-1,-1,0}};
                                        
                                    }
                                    else if(option.equals("Wektora kierunkowego VDG")){ //wykrywające krawędzie
                                        option=null;
                                        buttons=new String[]{"Wschód","Południowy - Wschód","Południe","Południowy - Zachód",
                                        "Zachód","Północny - Zachód", "Północ","Północny - Wschód"};
                                        option=(String) JOptionPane.showInputDialog(null, "Wybierz kierunek filtra", "",
                                        JOptionPane.QUESTION_MESSAGE, null,buttons,null);
                                        if(option==null)
                                            return;
                                        else if(option.equals("Wschód"))
                                            square=new int[][]{ {-1,1,1},
                                                                {-1,-2,1},
                                                                {-1,1,1}};
                                        else if(option.equals("Południowy - Wschód"))
                                            square=new int[][]{ {-1,-1,1},
                                                                {-1,-2,1},
                                                                {1,1,1}};
                                        else if(option.equals("Południe"))
                                            square=new int[][]{ {-1,-1,-1},
                                                                {1,-2,1},
                                                                {1,1,1}};
                                        else if(option.equals("Południowy - Zachód"))
                                            square=new int[][]{ {1,-1,-1},
                                                                {1,-2,-1},
                                                                {1,1,1}};
                                        else if(option.equals("Zachód"))
                                            square=new int[][]{ {1,1,-1},
                                                                {1,-2,-1},
                                                                {1,1,-1}};
                                        else if(option.equals("Północny - Zachód"))
                                            square=new int[][]{ {1,1,1},
                                                                {1,-2,-1},
                                                                {1,-1,-1}};
                                        else if(option.equals("Północ"))
                                            square=new int[][]{ {1,1,1},
                                                                {1,-2,1},
                                                                {-1,-1,-1}};
                                        else if(option.equals("Północny - Wschód"))
                                            square=new int[][]{ {1,1,1},
                                                                {-1,-2,1},
                                                                {-1,-1,1}};
                                    }
                                    picture.linearHighPassFilter(square);
                                    chosenOperation+="("+option+")";
                                }
                                else if(chosenOperation=="Medianowe"){
                                    int value[]=getInt("Podaj zasięg maski",1,min(picture.getHeight(),picture.getWidth()));
                                    if(value==null)
                                        return;
                                    picture.statisticalFilter(value[0],"mediana");
                                }
                                else if(chosenOperation=="Ekstremalne"){
                                    String[] buttons = { "Minimum", "Maksimum"};
                                    int rc = JOptionPane.showOptionDialog(null, "Którego filtra ekstremalnego użyć?", "",
                                            JOptionPane.DEFAULT_OPTION, 0, null, buttons, null);
                                    if(rc==-1)
                                        return;
                                    String type=null;
                                    if(rc==0)   //minima
                                        type="exMin";
                                    else if(rc==1)  //maksyma
                                        type="exMax";
                                    int value[]=getInt("Podaj zasięg maski",1,min(picture.getHeight(),picture.getWidth()));
                                    if(value==null)
                                        return;
                                    chosenOperation+="("+buttons[rc]+")";
                                    picture.statisticalFilter(value[0], type);
                                }
                                break;
                            default:
                                break;
                        }
                        File folder=new File("wyniki");
                        if(!folder.exists()){
                            JOptionPane.showMessageDialog(null,
                                    "Utworzono folder: "+folder.getAbsolutePath()+"",
                                    "",
                                    JOptionPane.INFORMATION_MESSAGE);
                                    
                            folder.mkdir();
                        }
                        picture.makeAFile("wyniki\\"+chosenOperation+" - ");
                        JOptionPane.showMessageDialog(null,
                                "Zapisano w folderze : "+"\""+folder.getPath()+"\"");
                    }
                }
                else if(e.getSource()==helpButton){
                    JOptionPane.showMessageDialog(null,
                            "Wróć  -   Powrót do okna wyboru pliku\n"+
                            "Lista po prawej -   Wybranie rodzaju rządanej operacji\n "+
                            "Lista po lewej  -  Operacje na pliku dostepne w wybranej grupie\n"+
                            "Dalej  -   Wybranie operacji i przejście do kolejnego okna\n",
                            "Pomoc  -   Okno wyboru pliku",
                            JOptionPane.QUESTION_MESSAGE);
                    
                }
                else for(int i=0;i<5;i++)
                    if(e.getSource()==choiceButtons[i])
                        kategoria=i+1;
                break;
            default:
                System.exit(1);
                break;
        }
        initState();
    }
    
    public static void main(String[] args){
        new Window();
    }
}
