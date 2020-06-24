package ru.abch.carplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class PlayList {
    String rootDir;
    int curTrack;
    ArrayList<String> playList;
    int init(String r, int p) {
        rootDir = r;
        playList = getPlayList(rootDir);
        curTrack = p;
        return playList.size();
    }
    public ArrayList<String> get(){
        return playList;
    }

    String getNext() {
        if (playList.size() > 0) {
            String ret = playList.get(curTrack++);
            if (curTrack >= playList.size()) curTrack = 0;
            return ret;
        } else return "";
    }

    String getTrack(int p) {
        int s = playList.size();
        if (s > 0 && p >= 0 && p < s ) {
            return playList.get(p);
        } else return "";
    }
    private boolean isMusic(File m_file){
        int m_lastIndex=m_file.getAbsolutePath().lastIndexOf(".");
        String m_filepath=m_file.getAbsolutePath();
        if(m_lastIndex > 0 && m_filepath.substring(m_lastIndex).equalsIgnoreCase(".mp3"))
        {
            return true;
        } else return false;
    }
    private ArrayList<String> getPlayList(String root) {
        ArrayList<String> result = new ArrayList<>();
        File f = new File(root);
        int i;
        if (f.isDirectory()) {
            String[] sDirList = f.list();
            for(i = 0; i < sDirList.length; i++)
            {
                File f1 = new File(root +
                        File.separator + sDirList[i]);

                if(f1.isFile() && isMusic(f1))
                    result.add(root +
                            File.separator + sDirList[i]);
                else if (f1.isDirectory())
                {   ArrayList<String> sublist = getPlayList(root + File.separator + sDirList[i]);
                    if (sublist.size() > 0) {
                        for (int j = 0; j < sublist.size(); j++) result.add(sublist.get(j));
                    }
                }
            }
        }
        Collections.sort(result);
        return result;
    }
}
