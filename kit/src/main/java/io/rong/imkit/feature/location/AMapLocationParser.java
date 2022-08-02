package io.rong.imkit.feature.location;

import org.json.JSONObject;

import io.rong.common.RLog;

public class AMapLocationParser {
    private final static String TAG = "AMapLocationParser";

    public AMapLocationInfo parserApsJsonResp(String str) {
        if (str == null) {
            return null;
        }

        AMapLocationInfo amapLoc = new AMapLocationInfo();
        try {
            JSONObject respOBJ = new JSONObject(str);
            String status = respOBJ.optString("status");
            if ("0".equals(status)) {
                String errorInfo = respOBJ.optString("info");
                amapLoc.setErrorInfo(errorInfo);
                String infoCode = respOBJ.optString("infocode");
                int errorCode = Integer.parseInt(infoCode);
                amapLoc.setErrorCode(errorCode);
                return amapLoc;
            } else {
                amapLoc.setErrorCode(0);
                amapLoc.setErrorInfo("success");
            }
            String retype = respOBJ.optString("retype");
            amapLoc.setRetype(retype);
            String rdesc = respOBJ.optString("rdesc");
            amapLoc.setRdesc(rdesc);
            String adcode = respOBJ.optString("adcode");
            amapLoc.setAdcode(adcode);
            String citycode = respOBJ.optString("citycode");
            amapLoc.setCitycode(citycode);
            String coord = respOBJ.optString("coord");
            amapLoc.setCoord(coord);
            String desc = respOBJ.optString("desc");
            amapLoc.setDesc(desc);
            long apiTime = respOBJ.optLong("apiTime");
            amapLoc.setTime(apiTime);

            // lat、lon、radius
            JSONObject locationOBJ = respOBJ.optJSONObject("location");
            if (locationOBJ != null) {
                float radius = (float) locationOBJ.optDouble("radius");
                amapLoc.setAccuracy(radius);
                double lon = locationOBJ.optDouble("cenx");
                amapLoc.setLon(lon);
                double lat = locationOBJ.optDouble("ceny");
                amapLoc.setLat(lat);
            }

            // regeo
            JSONObject revergeoOBJ = respOBJ.optJSONObject("revergeo");
            if (revergeoOBJ != null) {
                String country = revergeoOBJ.optString("country");
                amapLoc.setCountry(country);
                String province = revergeoOBJ.optString("province");
                amapLoc.setProvince(province);
                String city = revergeoOBJ.optString("city");
                amapLoc.setCity(city);
                String district = revergeoOBJ.optString("district");
                amapLoc.setDistrict(district);
                String road = revergeoOBJ.optString("road");
                amapLoc.setRoad(road);
                String street = revergeoOBJ.optString("street");
                amapLoc.setStreet(street);
                String number = revergeoOBJ.optString("number");
                amapLoc.setNumber(number);
                String poiname = revergeoOBJ.optString("poiname");
                amapLoc.setPoiname(poiname);
                String aoiname = revergeoOBJ.optString("aoiname");
                amapLoc.setAoiname(aoiname);
            }

            // indoor
            JSONObject indoorOBJ = respOBJ.optJSONObject("indoor");
            if (indoorOBJ != null) {
                String pid = indoorOBJ.optString("pid");
                amapLoc.setPoiid(pid);
                String flr = indoorOBJ.optString("flr");
                amapLoc.setFloor(flr);
            }
        } catch (Exception e) {
            RLog.e(TAG, "parserApsJsonResp", e);
        }
        return amapLoc;
    }
}
