package xchova20.audiorecord;

import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Manipulation of device storage.
 */
public class Storage {
    public Sender sender;
    public String folder;
    public Boolean isAbsolute;
    private String sessionName;

    /**
     * Storage constructor for general purpose.
     *
     * @param sender sender
     * @param folder saving folder
     * @param isAbsolute absoluteness of path
     * @param sessionName session name
     */
    Storage(Sender sender, String folder, Boolean isAbsolute, String sessionName) {
        this.sender = sender;
        this.folder = folder;
        this.sessionName = sessionName;
        this.isAbsolute = isAbsolute;
    }

    /**
     * Storage constructor for basic functions.
     */
    Storage() { }

    /**
     * Sets log path
     * @param folder saving folder
     * @param isAbsolute absoluteness of path
     */
    public void setFolder(String folder, Boolean isAbsolute) {
        this.folder = folder;
        this.isAbsolute = isAbsolute;
    }

    /**
     * Creates a directory.
     *
     * @param path directory path
     * @param isAbsolute absoluteness of path
     * @return true if directory has been created, false if not
     */
    public boolean createDir(String path, Boolean isAbsolute) {
        File file;
        if (isAbsolute) file = new File(path);
        else file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                sender.saveLog(Constants.msg.get("DIR_FAIL") + path);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a file.
     *
     * @param path file path
     * @param isAbsolute absoluteness of path
     * @return true if file has been created, false if not
     */
    public boolean createFile(String path, Boolean isAbsolute) {
        File file;
        if (isAbsolute) file = new File(path);
        else file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                sender.saveLog(Constants.msg.get("FILE_FAIL") + path);
                return false;
            }
        }
        return true;
    }


    /**
     * Create everything storage-related needed by a new session.
     *
     * @return true if all was created, false if not
     */
    public boolean createSessionDirs() {
        String[] folder_parts = folder.split("/");
        // create all subfolders of main saving folder
        String full_folder;
        if (isAbsolute) full_folder = "";
        else full_folder = "/";
        for (String folder_part : folder_parts) {
            full_folder += (folder_part + "/");
            if (!createDir(full_folder, isAbsolute)) return false;
        }
        // current session folder
        String sessionFolder = folder + "/" + sessionName;
        if (!createDir(sessionFolder, isAbsolute)) return false;
        // recordings folder (for failed records)
        String recordingFolder = sessionFolder + "/recordings";
        if (!createDir(recordingFolder, isAbsolute)) return false;
        // file for logs
        String logFile = sessionFolder + "/log.txt";
        if (!createFile(logFile, isAbsolute))  return false;
        return true;
    }

    /**
     * Save packet into memory.
     *
     * @param data packet content
     * @param lastSavedPackage number of last saved packet
     * @return number of newly saved packet
     */
    public int save(final String data, int lastSavedPackage)  {
        int fileNameNum;
        // saving first package
        if (lastSavedPackage == -1) {
            fileNameNum = 1;
            lastSavedPackage = 1;
        } else {
            fileNameNum = lastSavedPackage + 1;
            lastSavedPackage++;
        }
        String file_path = folder + "/" + sessionName + "/recordings/" + fileNameNum;
        if (!this.createFile(file_path, isAbsolute)) sender.sendLog(Constants.msg.get("SAVE_UNSENT_ERR") + fileNameNum + ")");
        else {
            try {
                writeToFile(file_path, isAbsolute, data);
            } catch (IOException e) {
                sender.sendLog(Constants.msg.get("SAVE_UNSENT_ERR") + fileNameNum + ")");
            }
        }
        return  lastSavedPackage;
    }

    /**
     * Gets oldest unsent packet content.
     *
     * @return content of the packet
     */
    public String[] getNextUnsentPacket() {
        File[] files = getAllFilesFromDirectory(folder + "/" + sessionName + "/recordings", isAbsolute);
        Integer[] fileNameNums = new Integer[] {};
        for (File file : files) {
            fileNameNums = Arrays.copyOf(fileNameNums, fileNameNums.length + 1);
            fileNameNums[fileNameNums.length - 1] = Integer.parseInt(file.getName());
        }
        String fileName = null;
        String content = null;
        try {
            fileName = Collections.min(Arrays.asList(fileNameNums)).toString();
            content = readFromFile(folder + "/" + sessionName + "/recordings/" + fileName, isAbsolute);
        } catch (NoSuchElementException | IOException e) {
            // file has not been written into yet, try again in a while
        }
        return new String[] {content, fileName};
    }

    /**
     * Removed unsent packet, usually if it's successfully resent.
     *
     * @param fileName packet filename
     */
    public void removeUnsentPacket(String fileName) {
        File f;
        if (isAbsolute) f = new File(folder + "/" + sessionName + "/recordings/" + fileName);
        else f = new File(Environment.getExternalStorageDirectory(), folder + "/" + sessionName + "/recordings/" + fileName);
        f.delete();
    }

    /**
     * Gets all files from a directory.
     *
     * @param path directory path
     * @param absolute Absoluteness of directory path
     * @return files contained in the directory
     */
    public File[] getAllFilesFromDirectory(String path, Boolean absolute) {
        File dir;
        if (absolute) dir = new File(path);
        else dir = new File(Environment.getExternalStorageDirectory(), path);
        if (dir.exists() && dir.isDirectory()) {
            return dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });
        }
        File[] no_files = {};
        return no_files;
    }

    /**
     * Gets all directories from a directory.
     *
     * @param path directory path
     * @return directories contained in the directory
     */
    public File[] getAllDirectoriesFromDirectory(String path) {
        File dir = new File(Environment.getExternalStorageDirectory(), path);
        if (dir.exists() && dir.isDirectory()) {
        return dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
        }
        File[] no_files = {};
        return no_files;
    }

    /**
     * Checks whether are there any unsent packets.
     *
     * @return true if there are unsent packets, false if not
     */
    public boolean packetsUnsent() {
        File[] files = getAllFilesFromDirectory(folder + "/" + sessionName + "/recordings", isAbsolute);
        if (files == null || files.length == 0) return false;
        return true;
    }

    /**
     * Appends unsent packets together to create a complete recording.
     *
     * @param files list of files
     * @return string consisting of content of all files
     */
    public String appendFiles(File[] files) {
        String completeFile = "";
        for (File file : files) {
            try {
                completeFile +=  readFromFile(file.getAbsolutePath(), false);
            } catch (IOException e) {
                continue;
            }
        }
        return completeFile;
    }

    /**
     * Writes into a file, if the file does not exist, it is created.
     *
     * @param path file path
     * @param isAbsolute absoluteness of path
     * @param content content to be written
     * @throws IOException if the file creation was not successful
     */
    public void writeToFile(String path, Boolean isAbsolute, String content) throws IOException {
        File f;
        if (isAbsolute) f = new File(path);
        else f = new File(Environment.getExternalStorageDirectory(), path);
        if (!f.exists()) createFile(path, isAbsolute);
        FileOutputStream stream = new FileOutputStream(f);
        stream.write(content.getBytes());
        stream.close();
    }

    /**
     * Reads from a file. If file does not exist, throw exception.
     *
     * @param path file path
     * @param isAbsolute absoluteness of path
     * @throws IOException if the file does not exist
     * @returns File content
     */
    public String readFromFile(String path, Boolean isAbsolute) throws IOException {
        File file;
        if (isAbsolute) file = new File(path);
        else file = new File(Environment.getExternalStorageDirectory().toString(), path);
        return new Scanner(file).useDelimiter("\\Z").next();
    }
}
