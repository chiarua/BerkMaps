import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer_mine {
    private static final double LonDPP0 = 0.00034332275390625;
    private static final double lrLon0 = -122.2119140625;
    private static final double ulLon0 = -122.2998046875;
    private static final double lrLat0 = 37.82280243352756;
    private static final double ulLat0 = 37.892195547244356;
    private static double lrlon, ullon, w, h, ullat, lrlat;
    private static double res_ullon, res_ullat, res_lrlon, res_lrlat;

    public Rasterer_mine() {
        // YOUR CODE HERE
    }

    private boolean paramsValidator(Map<String, Double> params) {
        double lrlon = params.get("lrlon");
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlat = params.get("lrlat");

        return !(lrlon < ullon || lrlat > ullat
                || lrlon < MapServer.ROOT_ULLON || ullon > MapServer.ROOT_LRLON
                || ullat < MapServer.ROOT_LRLAT || lrlat > MapServer.ROOT_ULLAT);
    }

    private int getScaling(){
        int n=0;
        double needed = (lrlon-ullon)/w;
        double now = LonDPP0;
        while(now>needed&&n<7){
            now/=2;
            n+=1;
        }
        return n;
    }

    private String[][] getPicRange(int scale){
        double stepW = (lrLon0 - ulLon0)/Math.pow(2,scale);
        double stepH = (ulLat0 - lrLat0)/Math.pow(2,scale);
        System.out.println(stepW);
        System.out.println(stepH);
        int wBegin = (int) Math.floor((ullon - ulLon0)/stepW);//需要小的
        int hBegin = (int) Math.floor((ulLat0 - ullat)/stepH);
        wBegin = Math.max(0, wBegin);
        hBegin = Math.max(0, hBegin);
        int wLen = (int) Math.ceil(w/256);//需要大的
        int hLen = (int) Math.ceil(h/256);
        wLen = (int) Math.min(Math.pow(2,scale)-1-wBegin, wLen);
        hLen = (int) Math.min(Math.pow(2,scale)-1-hBegin, hLen);
        String[][] ans = new String[hLen][wLen];
        res_ullon = ulLon0 + wBegin*stepW;//right
        res_lrlat = lrLat0 + hBegin*stepH;
        res_lrlon = ulLon0 + (wBegin+wLen)*stepW;
        res_ullat = lrLat0 + (hBegin+hLen)*stepH;
        for(int i=0;i<hLen;i++){
            for(int j=0;j<wLen;j++){
                ans[i][j] = "d"+ scale + "_" + "x" +(j+wBegin)  + "_" + "y" + (i+hBegin) + ".png";
            }
        }
        return ans;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        //params: {lrlon=-122.22638236999512, ullon=-122.27625, w=1162.0, h=902.0, ullat=37.88, lrlat=37.849437212611065}
        lrlon = params.get("lrlon");// right?
        ullon = params.get("ullon");
        w = params.get("w");
        h = params.get("h");
        ullat = params.get("ullat");
        lrlat = params.get("lrlat");
        System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        //1.放缩比例
        int scale = getScaling();
        //2.图像范围
        String[][] render_grid = getPicRange(scale);
        System.out.println(Arrays.deepToString(render_grid));
        results.put("render_grid", render_grid);
        results.put("raster_ul_lon", res_ullon);
        results.put("raster_ul_lat", res_ullat);
        results.put("raster_lr_lon", res_lrlon);
        results.put("raster_lr_lat", res_lrlat);
        results.put("depth", scale);
        System.out.println(res_ullat);
        System.out.println(res_lrlat);
        System.out.println(res_ullon);
        System.out.println(res_lrlon);
        results.put("query_success", paramsValidator(params));
        return results;
    }

}
