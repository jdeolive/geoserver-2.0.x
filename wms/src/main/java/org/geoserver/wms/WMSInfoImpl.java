package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.config.impl.ServiceInfoImpl;

public class WMSInfoImpl extends ServiceInfoImpl implements WMSInfo {

    List<String> srs = new ArrayList<String>();
    WatermarkInfo watermark = new WatermarkInfoImpl();
    WMSInterpolation interpolation;
    int maxBuffer;
    int maxRequestMemory;
    int maxRenderingTime;
    int maxRenderingErrors;
    
    public WMSInfoImpl() {
        setId( "wms" );
    }
    
    public int getMaxRequestMemory() {
        return maxRequestMemory;
    }

    public void setMaxRequestMemory(int maxRequestMemory) {
        this.maxRequestMemory = maxRequestMemory;
    }

    public WatermarkInfo getWatermark() {
        return watermark;
    }
    
    public void setWatermark(WatermarkInfo watermark) {
        this.watermark = watermark;
    }

    public void setInterpolation(WMSInterpolation interpolation) {
        this.interpolation = interpolation;
    }
    
    public WMSInterpolation getInterpolation() {
        return interpolation;
    }

    public List<String> getSRS() {
        return srs;
    }
    
    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    public int getMaxBuffer() {
        return maxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    public int getMaxRenderingTime() {
        return maxRenderingTime;
    }

    public void setMaxRenderingTime(int maxRenderingTime) {
        this.maxRenderingTime = maxRenderingTime;
    }

    public int getMaxRenderingErrors() {
        return maxRenderingErrors;
    }

    public void setMaxRenderingErrors(int maxRenderingErrors) {
        this.maxRenderingErrors = maxRenderingErrors;
    }

}
