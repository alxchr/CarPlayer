package ru.abch.carplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import ru.abch.carplayer.dummy.DummyContent.DummyItem;

import static ru.abch.carplayer.MainActivity.musicRoot;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ItemFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 4;
    private OnListFragmentInteractionListener mListener;
    private String TAG = "Folder fragment";
    ListView m_RootList;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ItemFragment newInstance(int columnCount) {
        ItemFragment fragment = new ItemFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_folder, container, false);
        m_RootList = view.findViewById(R.id.lvListRoot);
        Log.d(TAG, "Music root = " + musicRoot);
        MainActivity.rootPlayList = getDirFromRoot(musicRoot);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(DummyItem item);
    }
    boolean isMusic(File m_file){
        int m_lastIndex = m_file.getAbsolutePath().lastIndexOf(".");
        String m_filepath = m_file.getAbsolutePath();
        if(m_lastIndex > 0 && m_filepath.substring(m_lastIndex).equalsIgnoreCase(".mp3"))
        {
            return true;
        } else return false;
    }
    public String getDirFromRoot(String p_rootPath) {
        ArrayList<String> m_item, m_files, m_filesPath;
        final ArrayList<String>  m_path;
        String m_curDir;
        ListAdapter m_listAdapter;
        m_item = new ArrayList<String>();
        Boolean m_isRoot=true;
        m_path = new ArrayList<String>();
        m_files=new ArrayList<String>();
        m_filesPath=new ArrayList<String>();
        Log.d(TAG, "Get dir from root = " + p_rootPath);
        File m_file = new File(p_rootPath);
        File[] m_filesArray = m_file.listFiles();
        if(!p_rootPath.equals(musicRoot))
        {
            m_item.add("../");
            m_path.add(m_file.getParent());
            m_isRoot=false;
        }
        m_curDir=p_rootPath;
        Log.d(TAG, "N files in dir = " + m_filesArray.length);
        //sorting file list in alphabetical order
        Arrays.sort(m_filesArray);
        for(int i=0; i < m_filesArray.length; i++)
        {
            File file = m_filesArray[i];
            if(file.isDirectory())
            {
                m_item.add(file.getName());
                m_path.add(file.getPath());
            }
            else
            {
                m_files.add(file.getName());
                m_filesPath.add(file.getPath());
            }
        }
        for(String m_AddFile:m_files)
        {
            m_item.add(m_AddFile);
        }
        for(String m_AddPath:m_filesPath)
        {
            m_path.add(m_AddPath);
        }
        m_listAdapter=new ListAdapter(getActivity(),m_item,m_path,m_isRoot);
        m_RootList.setAdapter(m_listAdapter);

        m_RootList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String path = m_path.get(position);
                File m_isFile=new File(path);
                if(m_isFile.isDirectory())
                {
                    MainActivity.rootPlayList = getDirFromRoot(m_isFile.toString());
                }
                else if (isMusic(m_isFile)) {
                    int fileLength = (int)m_isFile.length();
                } else
                {
                    Toast.makeText(getActivity(), "This is File", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return m_curDir;
    }
}
