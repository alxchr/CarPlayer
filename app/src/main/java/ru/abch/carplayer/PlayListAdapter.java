package ru.abch.carplayer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static ru.abch.carplayer.MainActivity.rootPlayList;

public class PlayListAdapter extends BaseAdapter {
    private List<String> m_item;
    public ArrayList<Integer> m_selectedItem;
    Context m_context;
    public PlayListAdapter(Context p_context, List<String> p_item) {
        m_context=p_context;
        m_item=p_item;
        m_selectedItem=new ArrayList<Integer>();
    }
    @Override
    public int getCount() {
        return m_item.size();
    }

    @Override
    public Object getItem(int position) {
        return m_item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(final int p_position, View p_convertView, ViewGroup p_parent)
    {
        View m_view = null;
        final PlayListAdapter.ViewHolder m_viewHolder;
        if (p_convertView == null)
        {
            LayoutInflater m_inflater = LayoutInflater.from(m_context);
            m_view = m_inflater.inflate(R.layout.playlist_row, null);
            m_viewHolder = new PlayListAdapter.ViewHolder();
            m_viewHolder.m_tvFileName = (TextView) m_view.findViewById(R.id.pl_tvFileName);
//            m_viewHolder.m_ivIcon = (ImageView) m_view.findViewById(R.id.pl_ivFileIcon);
            m_viewHolder.playButton = m_view.findViewById(R.id.pl_PlayButton);
            m_viewHolder.pos = p_position;
            m_viewHolder.playButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    View parentRow = (View) v.getParent();
                    ListView listView = (ListView) parentRow.getParent();
                    final int position = listView.getPositionForView(parentRow);
                    // Your code that you want to execute on this button click
                    Log.d("In Play list", " track #" + position);
                    MainActivity.playButton();
                    Intent startIntent = new Intent(m_context, PlayService.class);
                    startIntent.putExtra("folder",rootPlayList);
                    startIntent.putExtra("track_num", position);
                    startIntent.putExtra("command", "play_track");
                    m_context.startService(startIntent);
                }

            });
            m_view.setTag(m_viewHolder);
        }
        else
        {
            m_view = p_convertView;
            m_viewHolder = ((PlayListAdapter.ViewHolder) m_view.getTag());
        }

        m_viewHolder.m_tvFileName.setText(m_item.get(p_position));
//        m_viewHolder.m_ivIcon.setImageResource(R.mipmap.icons8_musical_24);

        return m_view;
    }

    class ViewHolder
    {
//        ImageView m_ivIcon;
        TextView m_tvFileName;
        ImageButton playButton;
        int pos;
    }
}
