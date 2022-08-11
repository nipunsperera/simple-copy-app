package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class MainFormController {
    public JFXButton btnOpen;
    public JFXButton btnDestination;

    public JFXButton btnCopy;
    public Rectangle lblPB;
    public Label lblPBSize;
    public JFXTextField txtOpenPath;
    public JFXTextField txtDesPath;
    public JFXTextField txtSize;
    public Rectangle lblFinalLength;
    public Label lblProgress;
    public JFXCheckBox chkDirectory;
    public JFXCheckBox chkFiles;

    private File[] srcFile;
    private File srcDir;
    private double totalFileSize;
    private long totalDirSize;
    private File directory;
    private ArrayList<File> fileReturn;





    public void initialize(){
        btnCopy.setDisable(true);
        btnDestination.setDisable(true);
        btnOpen.setDisable(true);


    }
    public void btnOpenOnAction(ActionEvent actionEvent) {
        if (chkFiles.isSelected()){
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File("/home/nipunperera/Desktop"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files (*.*)","*.*"));

            srcFile = fileChooser.showOpenMultipleDialog(btnOpen.getScene().getWindow()).toArray(new File[0]);

            totalFileSize=0;


            for (File file : srcFile) {
                txtOpenPath.appendText(file + "| ");
                totalFileSize = totalFileSize + file.length();
            }
            txtSize.setText(String.valueOf(formatNumber(totalFileSize/1024.0) + " kb"));
        } else if (chkDirectory.isSelected()) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File("/home/nipunperera/Desktop"));

            srcDir = directoryChooser.showDialog(btnDestination.getScene().getWindow());
            txtOpenPath.setText(String.valueOf(srcDir));
            for (File accessDir: srcDir.listFiles()){
                totalDirSize = totalDirSize + accessDir.length();
            }
            txtSize.setText(String.valueOf(totalDirSize/1024.0));

        }

        btnDestination.setDisable(false);

    }

    public void btnDestinationOnAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("/home/nipunperera/Desktop"));

        directory = directoryChooser.showDialog(btnDestination.getScene().getWindow());
        txtDesPath.setText(String.valueOf(directory));
        btnCopy.setDisable(false);
    }

    public void btnCopyOnAction(ActionEvent actionEvent) throws IOException {

        if(chkFiles.isSelected()){
            copyFile(srcFile,directory,totalFileSize);

        } else if (chkDirectory.isSelected()) {
            fileReturn = new ArrayList<>();
            getFiles(srcDir,fileReturn);
            File[] addToFiles = new File[fileReturn.size()];
            for (int i=0; i<fileReturn.size();i++){
                addToFiles[i] = fileReturn.get(i);
            }
            System.out.println(Arrays.toString(addToFiles));
            copyFile(addToFiles,directory,totalDirSize);



        }


    }

    public void chkDirectoryOnAction(ActionEvent actionEvent) {
        if (chkDirectory.isSelected()){
            chkFiles.setDisable(true);
            btnOpen.setDisable(false);

        } else if (!chkDirectory.isSelected()) {
            chkFiles.setDisable(false);
            btnOpen.setDisable(true);

        }
    }

    public void chkFilesOnAction(ActionEvent actionEvent) {
        if(chkFiles.isSelected()){
            chkDirectory.setDisable(true);
            btnOpen.setDisable(false);

        } else if (!chkFiles.isSelected()) {
            chkDirectory.setDisable(false);
            btnOpen.setDisable(true);

        }
    }
    private String formatNumber(double input){
        NumberFormat ni = NumberFormat.getNumberInstance();
        ni.setGroupingUsed(true);
        ni.setMaximumFractionDigits(2);
        ni.setMinimumFractionDigits(2);
        return ni.format(input);

    }

    private void copyFile(File[] sourceFile, File saveDirectory, double fileSizeOption){
        var task = new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                double totalRead = 0;

                for(File eachFile : sourceFile){

                    File destFile = new File(saveDirectory, eachFile.getName());
                    FileInputStream fis = new FileInputStream(eachFile);
                    FileOutputStream fos = new FileOutputStream(destFile);

                    BufferedInputStream bis = new BufferedInputStream(fis);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    byte[] buffer = new byte[1024*1];
                    while(true){
                        int read = bis.read(buffer);
                        totalRead+=read;

                        if (read == -1){
                            break;
                        }
                        bos.write(buffer,0,read);
                        updateProgress(totalRead,fileSizeOption);

                    }

                    updateProgress(fileSizeOption,fileSizeOption);
                    bos.close();
                    bis.close();

                }


                return null;
            }
        };


        task.workDoneProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number prevWork, Number curWork) {
                lblPB.setWidth(lblFinalLength.getWidth()/ task.getTotalWork() * curWork.doubleValue());
                lblPBSize.setText(formatNumber(task.getWorkDone()/1024.0) + "/" + formatNumber(fileSizeOption/1024.0) + " Kb");
                lblProgress.setText("Progress: " + formatNumber(curWork.doubleValue()/task.getTotalWork() * 100) + "%");

            }
        });

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                lblPB.setWidth(lblFinalLength.getWidth());
                new Alert(Alert.AlertType.INFORMATION,"Files has been copied successfully").showAndWait();
                txtOpenPath.setText("");
                txtDesPath.setText("");
                txtSize.setText("");
                lblPB.setWidth(0);
                lblPBSize.setText("0 / 0 Kb");
                lblProgress.setText("Progress: 0 %");
                btnCopy.setDisable(true);
                btnDestination.setDisable(true);

            }
        });
        new Thread(task).start();
    }
    private void getFiles(File inputFilePath, ArrayList<File> outPutFilePath){
        File[] fileNames = inputFilePath.listFiles();
        for(File eachFile: fileNames){
            if(!eachFile.isDirectory()){
                outPutFilePath.add(eachFile);
            } else if (eachFile.isDirectory()) {
                getFiles(eachFile,outPutFilePath);
            }
        }
    }



}
