package com.cmtech.android.bledevice.temphumid.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cmtech.android.bledevice.temphumid.model.TempHumidData;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.util.DateTimeUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TempHumidHistoryDataAdapter extends RecyclerView.Adapter<TempHumidHistoryDataAdapter.ViewHolder> {
    private List<TempHumidData> dataList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView historyTime;
        TextView historyTemp;
        TextView historyHumid;


        ViewHolder(View itemView) {
            super(itemView);
            historyTime = itemView.findViewById(R.id.tv_temphumid_historytime);
            historyTemp = itemView.findViewById(R.id.tv_temphumid_historytemp);
            historyHumid = itemView.findViewById(R.id.tv_temphumid_historyhumid);
        }
    }

    public TempHumidHistoryDataAdapter(List<TempHumidData> dataList) {
        this.dataList = dataList;
    }



    @NonNull
    @Override
    public TempHumidHistoryDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_item_history_temphumid, parent, false);
        return new TempHumidHistoryDataAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TempHumidHistoryDataAdapter.ViewHolder holder, final int position) {
        TempHumidData data = dataList.get(position);

        holder.historyTime.setText(DateTimeUtil.timeToShortStringFormat(data.getTime().getTimeInMillis()));
        holder.historyTemp.setText(String.format(Locale.getDefault(),"%.3f", data.getTemp()));
        holder.historyHumid.setText(String.valueOf(data.getHumid()));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

}
