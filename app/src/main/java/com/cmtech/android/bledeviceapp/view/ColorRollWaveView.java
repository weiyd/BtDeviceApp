package com.cmtech.android.bledeviceapp.view;

/**
 * ColorRollWaveView: 带标记颜色的卷轴滚动式的波形显示视图
 * Created by bme on 2019/2/26.
 */

public class ColorRollWaveView {

}

/*
public class ColorRollWaveView extends RollWaveView {
    protected static final int MARKED_WAVE_COLOR = Color.WHITE; // 缺省的标记波形颜色

    private List<Boolean> markerList = new ArrayList<>(); //要显示的信号数据是否处于标记中

    public ColorRollWaveView(Context context) {
        super(context);
    }

    public ColorRollWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void clearData() {
        super.clearData();
        markerList.clear();
    }

    @Override
    public void addData(Integer data) {
        addData(data, false);
    }

    public void addData(Integer data, boolean isMarked) {
        super.showData(data, false);
        markerList.add(isMarked);
    }

    public synchronized void showData(List<Integer> data, List<Boolean> markers) {
        viewData.addAll(data);
        markerList.addAll(markers);

        drawDataOnForeCanvas();
        invalidate();
    }

    @Override
    protected void drawDataOnForeCanvas()
    {
        foreCanvas.drawBitmap(backBitmap, 0, 0, null);

        Integer[] data = viewData.toArray(new Integer[0]);
        Boolean[] markers = markerList.toArray(new Boolean[0]);
        int dataNum = data.length;
        if(dataNum <= 1) {
            return;
        }

        int begin = dataNum - dataNumInView;
        if(begin < 0) {
            begin = 0;
        }

        clearData();
        addData(data[begin], markers[begin]);
        preX = initX;
        preY = initY - Math.round(data[begin]/ valuePerPixel);
        Path path = new Path();
        path.moveTo(preX, preY);
        forePaint.setColor((markers[begin]) ? MARKED_WAVE_COLOR : DEFAULT_WAVE_COLOR);
        for(int i = begin+1; i < dataNum; i++) {
            addData(data[i], markers[i]);
            preX += pixelPerData;
            preY = initY - Math.round(data[i]/ valuePerPixel);
            path.lineTo(preX, preY);
            if(markers[i] != markers[i-1]) {
                foreCanvas.drawPath(path, forePaint);
                path = new Path();
                path.moveTo(preX, preY);
                forePaint.setColor((markers[i]) ? MARKED_WAVE_COLOR : DEFAULT_WAVE_COLOR);
            }
        }
        foreCanvas.drawPath(path, forePaint);

    }
}
*/
