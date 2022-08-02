package io.rong.imkit.feature.location;

import android.text.TextUtils;

import org.json.JSONObject;

public class AMapLocationInfo {
    private double lon = 0.0d;
    private double lat = 0.0d;
    private float accuracy = 0.0f;
    private long time = 0L;
    private String retype = "";
    private String rdesc = "";
    private String citycode = "";
    private String desc = "";
    private String adcode = "";
    private String country = "";
    private String province = "";
    private String city = "";
    private String district = "";
    private String road = "";
    private String street = "";
    private String poiname = "";
    private String cens = null;
    private String poiid = "";
    private String floor = "";
    private int errorCode = 0;
    /*
     * 坐标系（-1代表未知，0代表原始坐标，1代表偏转坐标）
     */
    private int coord = -1;
    /*
     * 主基站
     */
    private String mcell = "";

    private String number = "";

    private String aoiname = "";

    private boolean isOffset = true;

    private boolean isReversegeo = true;
    private String errorInfo;

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }



    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }


    public boolean isOffset() {
        return isOffset;
    }

    public void setOffset(boolean isOffset) {
        this.isOffset = isOffset;
    }

    public boolean isReversegeo() {
        return isReversegeo;
    }

    public void setReversegeo(boolean isReversegeo) {
        this.isReversegeo = isReversegeo;
    }

    private JSONObject extra = null;

    public double getLng() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setLon(String strLon) {
        lon = Double.parseDouble(strLon);
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLat(String strLat) {
        lat = Double.parseDouble(strLat);
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        setAccuracy(String.valueOf(Math.round(accuracy)));
    }

    private void setAccuracy(String strAccu) {
        accuracy = Float.parseFloat(strAccu);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getRetype() {
        return retype;
    }

    public void setRetype(String retype) {
        this.retype = retype;
    }

    public String getRdesc() {
        return rdesc;
    }

    public void setRdesc(String rdesc) {
        this.rdesc = rdesc;
    }

    public String getCitycode() {
        return citycode;
    }

    public void setCitycode(String citycode) {
        this.citycode = citycode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getAdcode() {
        return adcode;
    }

    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getRoad() {
        return road;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * 获取门牌号
     *
     * @return 门牌号
     * @since 2.3.0
     *
     */
    public String getNumber() {
        return number;
    }

    /**
     * 设置门牌号
     *
     * @param number
     * @since 2.3.0
     *
     */
    public void setNumber(String number) {
        this.number = number;
    }

    public String getPoiname() {
        return poiname;
    }

    public void setPoiname(String poiname) {
        this.poiname = poiname;
    }

    /**
     * 获取aoiName
     * @since 2.4.0
     *
     * @return aoiName
     *
     */
    public String getAoiname() {
        return aoiname;
    }

    /**
     * 设置 aoiName
     *
     * @param aoiname
     * @since 2.4.0
     *
     */
    public void setAoiname(String aoiname) {
        this.aoiname = aoiname;
    }

    public String getCens() {
        return cens;
    }

    public void setCens(String cens) {
        if (TextUtils.isEmpty(cens)) {
            return;
        }
        String[] saCens = cens.split("\\*");
        String[] saCen;
        for (String str : saCens) {
            if (TextUtils.isEmpty(str)) {
                continue;
            }
            /*
             * 默认将首条有效记录作为定位结果
             */
            saCen = str.split(",");
            setLon(Double.parseDouble(saCen[0]));
            setLat(Double.parseDouble(saCen[1]));
            int iAccu = Integer.parseInt(saCen[2]);
            setAccuracy(iAccu);
            break;
        }
        this.cens = cens;
    }

    public String getPoiid() {
        return poiid;
    }

    public void setPoiid(String poiid) {
        this.poiid = poiid;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        if (!TextUtils.isEmpty(floor)) {
            floor = floor.replace("F", "");
            try {
                Integer.parseInt(floor);
            } catch (Throwable e) {
                floor = null;
            }
        }
        this.floor = floor;
    }

    public String getMcell() {
        return mcell;
    }

    public void setMcell(String mcell) {
        this.mcell = mcell;
    }

    public JSONObject getExtra() {
        return extra;
    }

    public void setExtra(JSONObject extra) {
        this.extra = extra;
    }

    public boolean hasAccuracy() {
        return accuracy > 0.0f;
    }

    public int getCoord() {
        return coord;
    }

    public void setCoord(String strCoord) {
        if (TextUtils.isEmpty(strCoord)) {
            coord = -1;
        } else if (strCoord.equals("0")) {
            coord = 0;
        } else if (strCoord.equals("1")) {
            coord = 1;
        } else {
            coord = -1;
        }
    }

    public void setCoord(int iCoord) {
        setCoord(String.valueOf(iCoord));
    }
}
